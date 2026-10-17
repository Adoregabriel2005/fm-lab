package br.local.fmlab;

import android.os.SystemClock;
import java.util.*;
import java.util.concurrent.Executor;

/** Sequential scan. One tuner, one transaction at a time, real measurements only. */
final class BandScanner {
  interface Listener {
    void tuning(int frequency);

    Boolean stereo();

    void progress(String text);

    void finished(List<Result> results, String error, boolean cancelled, boolean restored);
  }

  static final class Result {
    final int frequency;
    double rssi, sinr, sinrSquared;
    int rn, sn;
    String name = "", text = "";
    Boolean stereo;

    Result(int f) {
      frequency = f;
    }

    void add(Integer r, Integer s) {
      if (r != null) {
        rssi += r;
        rn++;
      }
      if (s != null) {
        sinr += s;
        sinrSquared += (double) s * s;
        sn++;
      }
    }

    double sinrSd() {
      return sn == 0
          ? Double.NaN
          : Math.sqrt(Math.max(0, sinrSquared / sn - sinrMean() * sinrMean()));
    }

    double rssiMean() {
      return rn == 0 ? Double.NaN : rssi / rn;
    }

    double sinrMean() {
      return sn == 0 ? Double.NaN : sinr / sn;
    }
  }

  interface Clock {
    long now();

    void sleep(long ms) throws InterruptedException;
  }

  private final Clock clock;

  BandScanner() {
    this(
        new Clock() {
          public long now() {
            return SystemClock.elapsedRealtime();
          }

          public void sleep(long ms) throws InterruptedException {
            Thread.sleep(ms);
          }
        });
  }

  BandScanner(Clock c) {
    clock = c;
  }

  private volatile boolean running = false, cancel = false, restore = true;

  boolean isRunning() {
    return running;
  }

  synchronized void cancel(boolean restoreFrequency) {
    if (!cancel) restore = restoreFrequency;
    cancel = true;
  }

  static void sort(List<Result> a) {
    Collections.sort(
        a,
        new Comparator<Result>() {
          public int compare(Result x, Result y) {
            double sx = x.sn == 0 ? -Double.MAX_VALUE : x.sinrMean(),
                sy = y.sn == 0 ? -Double.MAX_VALUE : y.sinrMean();
            int c = Double.compare(sy, sx);
            if (c != 0) return c;
            c =
                Double.compare(
                    x.sn == 0 ? Double.MAX_VALUE : x.sinrSd(),
                    y.sn == 0 ? Double.MAX_VALUE : y.sinrSd());
            if (c != 0) return c;
            double rx = x.rn == 0 ? -Double.MAX_VALUE : x.rssiMean(),
                ry = y.rn == 0 ? -Double.MAX_VALUE : y.rssiMean();
            return Double.compare(ry, rx);
          }
        });
  }

  synchronized void start(
      final RadioBridge radio,
      final int original,
      final int step,
      final int lower,
      final int upper,
      final Listener l,
      Executor executor) {
    if (running) return;
    if ((step != 100 && step != 200)
        || !BandPlan.valid(original)
        || !BandPlan.valid(lower)
        || !BandPlan.valid(upper)
        || lower > upper) throw new IllegalArgumentException("Scan band");
    running = true;
    cancel = false;
    restore = true;
    try {
      executor.execute(
          new Runnable() {
            public void run() {
              if (cancel) {
                running = false;
                l.finished(new ArrayList<Result>(), "", true, false);
                return;
              }
              scan(radio, original, step, lower, upper, l);
            }
          });
    } catch (RuntimeException e) {
      running = false;
      l.finished(new ArrayList<Result>(), "Fila do rádio ocupada", true, false);
    }
  }

  private Integer number(RadioBridge r, int code) {
    try {
      int v = r.number(code, null);
      return code == RadioBridge.SINR ? RadioBridge.sinrValue(v) : RadioBridge.valid(v) ? v : null;
    } catch (Exception e) {
      return null;
    }
  }

  private void waitMs(long ms) throws InterruptedException {
    long end = clock.now() + ms;
    while (!cancel) {
      long remaining = end - clock.now();
      if (remaining <= 0) return;
      clock.sleep(Math.min(50, remaining));
    }
  }

  private void scan(RadioBridge r, int original, int step, int lower, int upper, Listener l) {
    List<Result> all = new ArrayList<Result>();
    String error = "";
    boolean mutedByUs = false, restored = false;
    try {
      if (r.number(RadioBridge.IS_ON, null) == 0)
        throw new IllegalStateException("Ligue o FM antes de varrer");
      r.number(RadioBridge.STEREO, 1);
      if (r.number(RadioBridge.MUTED, null) == 0) mutedByUs = r.number(RadioBridge.MUTE, null) != 0;
      int total = BandPlan.count(lower, upper, step), index = 0;
      for (int f = lower; f <= upper && !cancel; f += step) {
        if (r.number(RadioBridge.IS_ON, null) == 0)
          throw new IllegalStateException("O rádio foi desligado durante a busca");
        l.progress(
            "Busca "
                + (++index)
                + "/"
                + total
                + " · "
                + String.format(Locale.US, "%.1f MHz", f / 1000.0));
        l.tuning(f);
        if (cancel) break;
        if (r.number(RadioBridge.TUNE, f) == 0) continue;
        waitMs(350);
        Result result = new Result(f);
        for (int i = 0; i < 3 && !cancel; i++) {
          result.add(number(r, RadioBridge.RSSI), number(r, RadioBridge.SINR));
          waitMs(80);
        }
        result.stereo = l.stereo();
        if (result.sn > 0 && result.sinrMean() >= 1) all.add(result);
      }
      sort(all);
      List<Result> refined = new ArrayList<Result>();
      int limit = Math.min(12, all.size());
      for (int i = 0; i < limit && !cancel; i++) {
        int f = all.get(i).frequency;
        l.progress(
            "Comparando candidato "
                + (i + 1)
                + "/"
                + limit
                + " · "
                + String.format(Locale.US, "%.1f MHz", f / 1000.0));
        l.tuning(f);
        if (cancel) break;
        if (r.number(RadioBridge.TUNE, f) == 0) continue;
        waitMs(1500);
        Result result = new Result(f);
        for (int j = 0; j < 8 && !cancel; j++) {
          result.add(number(r, RadioBridge.RSSI), number(r, RadioBridge.SINR));
          waitMs(100);
        }
        if (cancel) break;
        try {
          result.name = r.text(RadioBridge.PS);
          result.text = r.text(RadioBridge.RT);
        } catch (Exception ignored) {
        }
        result.stereo = l.stereo();
        refined.add(result);
      }
      if (!cancel) {
        all = refined;
        sort(all);
      }
    } catch (Exception e) {
      if (e instanceof InterruptedException) {
        cancel = true;
        Thread.currentThread().interrupt();
      }
      error = e.getClass().getSimpleName() + ": " + e.getMessage();
    } finally {
      if (restore && BandPlan.valid(original))
        try {
          if (r.number(RadioBridge.IS_ON, null) != 0) {
            l.tuning(original);
            restored = r.number(RadioBridge.TUNE, original) != 0;
          }
        } catch (Exception ignored) {
        }
      if (mutedByUs)
        try {
          r.number(RadioBridge.UNMUTE, null);
        } catch (Exception ignored) {
        }
      boolean wasCancelled = cancel;
      running = false;
      l.finished(all, error, wasCancelled, restored);
    }
  }
}

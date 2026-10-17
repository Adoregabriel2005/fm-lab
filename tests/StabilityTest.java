package br.local.fmlab;

import android.os.IBinder;
import java.util.*;
import java.util.concurrent.*;

public class StabilityTest {
  static int checks;

  static void check(boolean v) {
    checks++;
    if (!v) throw new AssertionError("check " + checks);
  }

  static final class Queue implements Executor {
    Runnable job;

    public void execute(Runnable r) {
      check(job == null);
      job = r;
    }

    void drain() {
      Runnable r = job;
      job = null;
      r.run();
    }
  }

  static final class Clock implements BandScanner.Clock {
    long time;
    Runnable action;

    public long now() {
      return time;
    }

    public void sleep(long ms) {
      if (ms <= 0 || ms > 50) throw new AssertionError("invalid sleep");
      time += ms;
      if (action != null) {
        Runnable r = action;
        action = null;
        r.run();
      }
    }
  }

  static final class Radio extends RadioBridge {
    int frequency = 98100, mute;
    final List<Integer> tunes = new ArrayList<Integer>();
    int calls;

    int number(int op, Integer arg) {
      calls++;
      switch (op) {
        case IS_ON:
          return 1;
        case STEREO:
          return 1;
        case MUTED:
          return mute;
        case MUTE:
          mute = 1;
          return 1;
        case UNMUTE:
          mute = 0;
          return 1;
        case TUNE:
          frequency = arg;
          tunes.add(arg);
          return 1;
        case RSSI:
          return 200;
        case SINR:
          return 12;
        default:
          throw new AssertionError("unexpected operation " + op);
      }
    }

    String text(int op) {
      return "test";
    }

    void registerEvents(IBinder b) {}
  }

  static final class Listener implements BandScanner.Listener {
    boolean finished, cancelled, restored;
    String error;
    int size;

    public void tuning(int f) {}

    public Boolean stereo() {
      return null;
    }

    public void progress(String s) {}

    public void finished(List<BandScanner.Result> r, String e, boolean c, boolean restored) {
      finished = true;
      cancelled = c;
      this.restored = restored;
      error = e;
      size = r.size();
    }
  }

  public static void main(String[] args) {
    PcmFrameBuffer pcm = new PcmFrameBuffer(2);
    short[] buffer = new short[8];
    check(pcm.prepare(buffer) == 0);
    buffer[0] = 1;
    buffer[1] = 2;
    buffer[2] = 3;
    check(pcm.complete(buffer, 3) == 2);
    check(pcm.prepare(buffer) == 1 && buffer[0] == 3);
    buffer[1] = 4;
    buffer[2] = 5;
    buffer[3] = 6;
    check(pcm.complete(buffer, 3) == 4);
    check(buffer[0] == 3 && buffer[3] == 6);
    check(pcm.prepare(buffer) == 0);
    buffer[0] = 7;
    check(pcm.complete(buffer, 1) == 0);
    check(pcm.prepare(buffer) == 1 && buffer[0] == 7);
    buffer[1] = 8;
    check(pcm.complete(buffer, 1) == 2);
    pcm = new PcmFrameBuffer(1);
    check(pcm.prepare(buffer) == 0);
    check(pcm.complete(buffer, 3) == 3);
    FrameGate gate = new FrameGate();
    gate.reset(1);
    check(gate.acquire(1));
    check(!gate.acquire(1));
    gate.reset(2);
    check(!gate.acquire(1));
    check(gate.acquire(2));
    gate.release(1);
    check(!gate.acquire(2));
    gate.release(2);
    check(gate.acquire(2));
    check(!gate.failed());
    gate.fail(1);
    check(!gate.failed());
    gate.fail(2);
    check(gate.failed());
    check(!gate.acquire(2));
    gate.reset(3);
    check(!gate.failed());
    check(gate.acquire(3));
    check(!ScreenAwakePolicy.keepAwake(false, false));
    check(ScreenAwakePolicy.keepAwake(true, false));
    check(ScreenAwakePolicy.keepAwake(false, true));
    check(ScreenAwakePolicy.keepAwake(true, true));
    AudioAnalysis.Analyzer analyzer = new AudioAnalysis.Analyzer();
    short[] tone = new short[1470];
    for (int i = 0; i < tone.length; i++)
      tone[i] = (short) (16000 * Math.sin(2 * Math.PI * 1000 * i / 44100));
    AudioAnalysis first = analyzer.analyze(tone, 44100);
    float[] before = first.bands.clone();
    AudioAnalysis silent = analyzer.analyze(new short[1470], 44100);
    check(silent.dominantHz == 0);
    check(Arrays.equals(first.bands, before));
    AudioAnalysis again = analyzer.analyze(tone, 44100);
    check(Arrays.equals(first.bands, again.bands));
    check(first.rmsDb == again.rmsDb);
    Clock clock = new Clock();
    BandScanner scan = new BandScanner(clock);
    Queue queue = new Queue();
    Radio radio = new Radio();
    Listener l = new Listener();
    scan.start(radio, 98100, 200, 87500, 87900, l, queue);
    check(scan.isRunning());
    check(radio.calls == 0);
    scan.cancel(true);
    queue.drain();
    check(l.finished && l.cancelled);
    check(radio.calls == 0);
    check(!scan.isRunning());
    for (final boolean restore : new boolean[] {true, false}) {
      clock = new Clock();
      final BandScanner s = new BandScanner(clock);
      queue = new Queue();
      radio = new Radio();
      l = new Listener();
      s.start(radio, 98100, 200, 87500, 87900, l, queue);
      clock.action =
          new Runnable() {
            public void run() {
              s.cancel(restore);
              s.cancel(!restore);
            }
          };
      queue.drain();
      check(l.finished && l.cancelled && l.error.length() == 0);
      check(l.restored == restore);
      check(radio.mute == 0);
      check(radio.frequency == (restore ? 98100 : 87500));
      check(!s.isRunning());
    }
    clock = new Clock();
    scan = new BandScanner(clock);
    queue = new Queue();
    radio = new Radio();
    l = new Listener();
    scan.start(radio, 98100, 200, 87500, 87900, l, queue);
    queue.drain();
    check(l.finished && !l.cancelled && l.error.length() == 0);
    check(l.size == 3);
    check(l.restored && radio.frequency == 98100 && radio.mute == 0);
    check(!scan.isRunning());
    l = new Listener();
    scan.start(
        radio,
        98100,
        200,
        87500,
        87900,
        l,
        new Executor() {
          public void execute(Runnable r) {
            throw new RejectedExecutionException();
          }
        });
    check(l.finished && l.cancelled);
    check(!scan.isRunning());
    boolean bad = false;
    try {
      scan.start(radio, 98100, 0, 87500, 87900, l, queue);
    } catch (IllegalArgumentException e) {
      bad = true;
    }
    check(bad);
    BandScanner jumping =
        new BandScanner(
            new BandScanner.Clock() {
              long t;

              public long now() {
                return t += 10000;
              }

              public void sleep(long ms) {
                throw new AssertionError("should not sleep after deadline");
              }
            });
    queue = new Queue();
    radio = new Radio();
    l = new Listener();
    jumping.start(radio, 98100, 200, 87500, 87900, l, queue);
    queue.drain();
    check(l.finished && l.error.length() == 0 && l.restored);
    BandScanner interrupted =
        new BandScanner(
            new BandScanner.Clock() {
              public long now() {
                return 0;
              }

              public void sleep(long ms) throws InterruptedException {
                throw new InterruptedException("test");
              }
            });
    queue = new Queue();
    radio = new Radio();
    l = new Listener();
    interrupted.start(radio, 98100, 200, 87500, 87900, l, queue);
    queue.drain();
    check(l.finished && l.cancelled && l.restored);
    check(radio.mute == 0);
    check(Thread.interrupted());
    short[] bass = new short[1470];
    for (int i = 0; i < bass.length; i++)
      bass[i] = (short) Math.round(16000 * Math.sin(2 * Math.PI * i / 1024));
    AudioAnalysis lowBand = analyzer.analyze(bass, 44100);
    check(lowBand.bands[0] > -10);
    check(lowBand.bands[0] == lowBand.bands[1]);
    check(analyzer.analyze(bass, 22050).bands[31] == -90);
    System.out.println(checks + " lifecycle, workspace and scan queue checks passed");
  }
}

package br.local.fmlab;

/** Ten-second raw SINR trial. No Android dependencies or RF unit conversion. */
final class ReceptionTrial {
  static final long DURATION = 10000, MAX_GAP = 1500;

  static final class Result {
    final int count, min, max;
    final double mean, sd;

    Result(int n, int lo, int hi, double avg, double sigma) {
      count = n;
      min = lo;
      max = hi;
      mean = avg;
      sd = sigma;
    }
  }

  private long start, last, first, maxGap;
  private int n, lo, hi;
  private double mean, m2;
  private boolean active;

  void start(long now) {
    start = now;
    last = now;
    first = -1;
    maxGap = 0;
    n = 0;
    lo = Integer.MAX_VALUE;
    hi = Integer.MIN_VALUE;
    mean = m2 = 0;
    active = true;
  }

  boolean active() {
    return active;
  }

  void cancel() {
    active = false;
  }

  long remaining(long now) {
    return Math.max(0, DURATION - (now - start));
  }

  void sample(Integer value, long now) {
    if (!active || value == null || now < start || now - start > DURATION || now < last) return;
    if (first < 0) first = now;
    maxGap = Math.max(maxGap, now - last);
    last = now;
    n++;
    lo = Math.min(lo, value);
    hi = Math.max(hi, value);
    double d = value - mean;
    mean += d / n;
    m2 += d * (value - mean);
  }

  Result finish(long now) {
    if (!active || remaining(now) > 0) return null;
    active = false;
    if (n < 10
        || first < 0
        || last - first < 8000
        || maxGap > MAX_GAP
        || start + DURATION - last > MAX_GAP) return null;
    return new Result(n, lo, hi, mean, Math.sqrt(Math.max(0, m2 / n)));
  }
}

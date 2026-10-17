package br.local.fmlab;

/** Display-only thresholds. Never writes tuner configuration. */
final class SignalQuality {
  static int classify(Integer value, int low, int high) {
    if (value == null || !RadioBridge.valid(value)) return -1;
    if (low >= high)
      throw new IllegalArgumentException("Os limites devem estar em ordem crescente");
    return value < low ? 0 : value < high ? 1 : 2;
  }

  static long delay(long targetMs, long elapsedMs) {
    return Math.max(16L, targetMs - elapsedMs);
  }

  static double rateHz(int intervals, long elapsedMs) {
    return elapsedMs <= 0 ? 0 : intervals * 1000.0 / elapsedMs;
  }
}

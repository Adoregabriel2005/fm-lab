package br.local.fmlab;

/** Zero rising trigger with a 2% of frame peak arming hysteresis. */
final class ScopeTrigger {
  static int start(short[] samples, int length) {
    if (length < 1 || length > samples.length) return 0;
    int peak = 0;
    for (short v : samples) peak = Math.max(peak, Math.abs((int) v));
    int hysteresis = Math.max(8, peak / 50);
    boolean armed = false;
    for (int i = 0; i + length <= samples.length; i++) {
      if (samples[i] <= -hysteresis) armed = true;
      if (armed && i > 0 && samples[i - 1] <= 0 && samples[i] > 0) return i;
    }
    return 0;
  }
}

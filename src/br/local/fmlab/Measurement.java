package br.local.fmlab;

/** Online statistics preserve absent readings instead of turning them into zero. */
final class Measurement {
  static final class Stats {
    int n = 0;
    double mean = 0, m2 = 0;
    int min = Integer.MAX_VALUE, max = Integer.MIN_VALUE;

    void add(Integer v) {
      if (v == null) return;
      n++;
      double delta = v - mean;
      mean += delta / n;
      m2 += delta * (v - mean);
      min = Math.min(min, v);
      max = Math.max(max, v);
    }

    double sd() {
      return n < 2 ? 0 : Math.sqrt(m2 / (n - 1));
    }
  }

  final Stats rssi = new Stats(), sinr = new Stats();
  int cycles = 0, poor = 0, classified = 0;
  final long started;
  final int frequency;
  final String name;
  final int source, low, high;

  Measurement(long start, int f, String label, int s, int l, int h) {
    started = start;
    frequency = f;
    name = label;
    source = s;
    low = l;
    high = h;
  }

  void add(Integer r, Integer s) {
    cycles++;
    rssi.add(r);
    sinr.add(s);
    int q = SignalQuality.classify(source == 0 ? s : r, low, high);
    if (q >= 0) {
      classified++;
      if (q == 0) poor++;
    }
  }
}

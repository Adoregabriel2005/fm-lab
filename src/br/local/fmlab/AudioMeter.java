package br.local.fmlab;

/**
 * Continuous digital PCM metering, independent of UI/FFT frame delivery. RMS300: one-pole mean
 * square, tau=300ms; sample peak, not intersample true peak.
 */
final class AudioMeter {
  static final class Reading {
    final int channels;
    final double[] rms300, blockRms, peak, hold;
    final long[] fullScale;
    final long frames;

    Reading(int c, double[] r, double[] b, double[] p, double[] h, long[] f, long n) {
      channels = c;
      rms300 = r;
      blockRms = b;
      peak = p;
      hold = h;
      fullScale = f;
      frames = n;
    }
  }

  private final double[] energy = new double[2],
      block = new double[2],
      peak = new double[2],
      held = new double[2];
  private final double[] sum = new double[2];
  private int cachedRate;
  private double alpha, release;
  private final long[] holdFrames = new long[2], fullScale = new long[2];
  private long frames;
  private int channels = 1;

  static double db(double amplitude) {
    return amplitude > 0 ? 20 * Math.log10(amplitude) : -120;
  }

  void process(short[] pcm, int length, int channelCount, int rate) {
    if (rate <= 0
        || (channelCount != 1 && channelCount != 2)
        || length < 0
        || length > pcm.length
        || length % channelCount != 0) throw new IllegalArgumentException("PCM format");
    channels = channelCount;
    int n = length / channels;
    if (n == 0) return;
    if (cachedRate != rate) {
      cachedRate = rate;
      alpha = Math.exp(-1.0 / (rate * .3));
      release = Math.pow(10, -1.0 / rate);
    }
    sum[0] = sum[1] = 0;
    for (int i = 0; i < n; i++)
      for (int ch = 0; ch < channels; ch++) {
        short raw = pcm[i * channels + ch];
        double v = raw / 32768.0, a = Math.abs(v);
        sum[ch] += v * v;
        energy[ch] = alpha * energy[ch] + (1 - alpha) * v * v;
        peak[ch] = Math.max(peak[ch], a);
        if (raw == 32767 || raw == -32768) fullScale[ch]++;
        if (a >= held[ch] && a > 0) {
          held[ch] = a;
          holdFrames[ch] = rate;
        } else if (holdFrames[ch] > 0) holdFrames[ch]--;
        else held[ch] *= release;
      }
    for (int ch = 0; ch < channels; ch++) block[ch] = Math.sqrt(sum[ch] / n);
    frames += n;
  }

  Reading snapshot() {
    double[] r = new double[channels],
        b = new double[channels],
        p = new double[channels],
        h = new double[channels];
    long[] f = new long[channels];
    for (int ch = 0; ch < channels; ch++) {
      r[ch] = db(Math.sqrt(energy[ch]));
      b[ch] = db(block[ch]);
      p[ch] = db(peak[ch]);
      h[ch] = db(held[ch]);
      f[ch] = fullScale[ch];
      peak[ch] = 0;
    }
    return new Reading(channels, r, b, p, h, f, frames);
  }
}

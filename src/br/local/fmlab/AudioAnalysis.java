package br.local.fmlab;

/** PCM analysis; no Android dependencies. Peak/RMS are digital levels, not RF power. */
final class AudioAnalysis {
  final AudioMeter.Reading meter;
  final float[] bands;
  final double rmsDb, peakPercent, clippedFraction, dominantHz;

  AudioAnalysis(float[] b, double r, double p, double c, double f) {
    this(b, r, p, c, f, null);
  }

  AudioAnalysis withMeter(AudioMeter.Reading m) {
    return new AudioAnalysis(bands, rmsDb, peakPercent, clippedFraction, dominantHz, m);
  }

  private AudioAnalysis(float[] b, double r, double p, double c, double f, AudioMeter.Reading m) {
    meter = m;
    bands = b;
    rmsDb = r;
    peakPercent = p;
    clippedFraction = c;
    dominantHz = f;
  }

  static AudioAnalysis analyze(short[] pcm, int rate) {
    return new Analyzer().analyze(pcm, rate);
  }

  /** One workspace per capture thread; returned frames never alias the workspace. */
  static final class Analyzer {
    private final double[] re = new double[1024], im = new double[1024], window = new double[1024];
    private final double windowSum;

    Analyzer() {
      double sum = 0;
      for (int i = 0; i < window.length; i++) {
        window[i] = .5 - .5 * Math.cos(2 * Math.PI * i / (window.length - 1));
        sum += window[i];
      }
      windowSum = sum;
    }

    AudioAnalysis analyze(short[] pcm, int rate) {
      if (rate <= 0) throw new IllegalArgumentException("Sample rate");
      int n = 1024;
      double energy = 0, peak = 0;
      int clipped = 0;
      for (short v : pcm) {
        double x = v / 32768.0;
        energy += x * x;
        peak = Math.max(peak, Math.abs(x));
        if (Math.abs((int) v) >= 32700) clipped++;
      }
      double rms = pcm.length == 0 ? 0 : Math.sqrt(energy / pcm.length);
      java.util.Arrays.fill(im, 0);
      double dc = 0;
      for (int i = 0; i < Math.min(n, pcm.length); i++) dc += pcm[i] / 32768.0;
      dc /= Math.max(1, Math.min(n, pcm.length));
      for (int i = 0; i < n; i++) {
        double win = window[i];
        re[i] = (i < pcm.length ? pcm[i] / 32768.0 - dc : 0) * win;
      }
      for (int i = 1, j = 0; i < n; i++) {
        int bit = n >> 1;
        for (; (j & bit) != 0; bit >>= 1) j ^= bit;
        j ^= bit;
        if (i < j) {
          double t = re[i];
          re[i] = re[j];
          re[j] = t;
        }
      }
      for (int len = 2; len <= n; len <<= 1) {
        double angle = -2 * Math.PI / len, wr0 = Math.cos(angle), wi0 = Math.sin(angle);
        for (int i = 0; i < n; i += len) {
          double wr = 1, wi = 0;
          for (int j = 0; j < len / 2; j++) {
            int a = i + j, b = a + len / 2;
            double vr = re[b] * wr - im[b] * wi, vi = re[b] * wi + im[b] * wr;
            re[b] = re[a] - vr;
            im[b] = im[a] - vi;
            re[a] += vr;
            im[a] += vi;
            double next = wr * wr0 - wi * wi0;
            wi = wr * wi0 + wi * wr0;
            wr = next;
          }
        }
      }
      float[] bands = new float[32];
      double strongest = 0, freq = 0;
      for (int k = 1; k < n / 2; k++) {
        double m = Math.hypot(re[k], im[k]) * 2 / windowSum;
        if (m > strongest) {
          strongest = m;
          freq = k * (double) rate / n;
        }
      }
      for (int b = 0; b < bands.length; b++) {
        double low = 43 * Math.pow(20000.0 / 43, b / 32.0),
            high = 43 * Math.pow(20000.0 / 43, (b + 1) / 32.0),
            max = 0;
        int start = Math.max(1, (int) Math.ceil(low * n / rate)),
            end = Math.min(n / 2, (int) Math.ceil(high * n / rate));
        if (low >= rate / 2.0) {
          bands[b] = -90;
          continue;
        }
        // A narrow log band may contain no bin center. Show its nearest resolved
        // FFT bin rather than implying silence; this does not add resolution.
        if (end <= start) {
          start =
              Math.max(1, Math.min(n / 2 - 1, (int) Math.round(Math.sqrt(low * high) * n / rate)));
          end = start + 1;
        }
        for (int k = start; k < end; k++)
          max = Math.max(max, Math.hypot(re[k], im[k]) * 2 / windowSum);
        bands[b] = (float) (max > 0 ? Math.max(-90, 20 * Math.log10(max)) : -90);
      }
      return new AudioAnalysis(
          bands,
          rms > 0 ? 20 * Math.log10(rms) : -120,
          peak * 100,
          pcm.length == 0 ? 0 : clipped / (double) pcm.length,
          strongest > .00001 ? freq : 0);
    }
  }
}

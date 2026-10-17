package br.local.fmlab;

public class AudioMeterTest {
  static int checks;

  static void check(boolean b) {
    checks++;
    if (!b) throw new AssertionError("check " + checks);
  }

  static void near(double a, double b, double e) {
    check(Math.abs(a - b) < e);
  }

  public static void main(String[] args) {
    AudioMeter m = new AudioMeter();
    short[] block = new short[4410];
    m.process(block, block.length, 1, 44100);
    AudioMeter.Reading r = m.snapshot();
    check(r.rms300[0] == -120);
    check(r.peak[0] == -120);
    check(r.fullScale[0] == 0);
    java.util.Arrays.fill(block, (short) 16384);
    m = new AudioMeter();
    for (int i = 0; i < 3; i++) m.process(block, block.length, 1, 44100);
    r = m.snapshot();
    near(r.rms300[0], 20 * Math.log10(.5 * Math.sqrt(1 - Math.exp(-1))), .00001);
    near(r.blockRms[0], -6.020599913, .00001);
    near(r.peak[0], -6.020599913, .00001);
    m = new AudioMeter();
    short[] sine = new short[44100];
    for (int i = 0; i < sine.length; i++)
      sine[i] = (short) Math.round(16384 * Math.sin(2 * Math.PI * 1000 * i / 44100));
    for (int i = 0; i < 3; i++) m.process(sine, sine.length, 1, 44100);
    r = m.snapshot();
    near(r.rms300[0], -9.0309, .02);
    near(r.blockRms[0], -9.0309, .002);
    m = new AudioMeter();
    m.process(new short[] {32767, -32768, 32766, -32767}, 4, 1, 44100);
    r = m.snapshot();
    check(r.fullScale[0] == 2);
    near(r.peak[0], 0, .000001);
    check(m.snapshot().peak[0] == -120);
    // Processed blocks not displayed by the UI still contribute to held peaks/counts.
    m.process(new short[] {-32768}, 1, 1, 44100);
    m.process(new short[4410], 4410, 1, 44100);
    r = m.snapshot();
    near(r.peak[0], 0, .000001);
    check(r.fullScale[0] == 3);
    m = new AudioMeter();
    m.process(new short[] {-32768}, 1, 1, 44100);
    m.process(new short[44100], 44100, 1, 44100);
    near(m.snapshot().hold[0], 0, .000001);
    m.process(new short[44100], 44100, 1, 44100);
    near(m.snapshot().hold[0], -20, .00001);
    // Anti-phase stereo is measured before mono downmix, so neither channel disappears.
    m = new AudioMeter();
    m.process(new short[] {16384, -16384, 16384, -16384}, 4, 2, 44100);
    r = m.snapshot();
    check(r.channels == 2);
    near(r.blockRms[0], -6.020599913, .00001);
    near(r.blockRms[1], -6.020599913, .00001);
    check(r.frames == 2);
    m = new AudioMeter();
    boolean error = false;
    try {
      m.process(new short[3], 3, 2, 44100);
    } catch (IllegalArgumentException e) {
      error = true;
    }
    check(error);
    short[] wave = {-200, -100, 100, 200, -200, -100, 100, 200};
    check(ScopeTrigger.start(wave, 4) == 2);
    check(ScopeTrigger.start(new short[] {1, 2, 3, 4}, 2) == 0);
    check(ScopeTrigger.start(new short[] {-1, 1, -1, 1}, 2) == 0);
    check(ScopeTrigger.start(wave, 20) == 0);
    System.out.println(checks + " continuous audio meter and scope trigger checks passed");
  }
}

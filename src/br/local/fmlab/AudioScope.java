package br.local.fmlab;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;

/**
 * Direct vendor RADIO_TUNER source, observed in the supplied native radio. Never falls back to MIC,
 * DEFAULT, VOICE_RECOGNITION or global output mix.
 */
final class AudioScope {
  interface Listener {
    void onFrame(short[] samples, int rate, AudioAnalysis analysis);

    void onStatus(String text);

    void onStopped();
  }

  private static final int RADIO_TUNER = 1998, RATE = 44100;
  private final Handler main = new Handler(Looper.getMainLooper());
  private final Listener listener;
  private final FrameGate frameGate = new FrameGate();
  private volatile boolean active = false;
  private volatile AudioRecord recorder;
  private Thread thread;
  private volatile int generation = 0;

  AudioScope(Listener l) {
    listener = l;
  }

  boolean isRunning() {
    return active;
  }

  synchronized void start() {
    if (thread != null) return;
    active = true;
    final int token = ++generation;
    frameGate.reset(token);
    status(token, "Abrindo a fonte de áudio FM do aparelho…");
    thread =
        new Thread(
            new Runnable() {
              public void run() {
                capture(token);
              }
            },
            "FM-Lab-Audio");
    thread.start();
  }

  private void status(final int token, final String s) {
    main.post(
        new Runnable() {
          public void run() {
            if (token == generation) listener.onStatus(s);
          }
        });
  }

  private AudioRecord create(int channel) {
    int min = AudioRecord.getMinBufferSize(RATE, channel, AudioFormat.ENCODING_PCM_16BIT);
    if (min <= 0) throw new IllegalStateException("Formato PCM não suportado");
    AudioRecord r =
        new AudioRecord(
            RADIO_TUNER, RATE, channel, AudioFormat.ENCODING_PCM_16BIT, Math.max(min * 2, 16384));
    if (r.getState() != AudioRecord.STATE_INITIALIZED) {
      r.release();
      throw new IllegalStateException("Fonte FM não inicializada pela ROM");
    }
    return r;
  }

  private void capture(final int token) {
    AudioRecord r = null;
    String error = null;
    boolean stereo = false;
    try {
      try {
        r = create(AudioFormat.CHANNEL_IN_MONO);
      } catch (IllegalStateException e) {
        r = create(AudioFormat.CHANNEL_IN_STEREO);
        stereo = true;
      }
      recorder = r;
      if (!active || token != generation) return;
      r.startRecording();
      if (r.getRecordingState() != AudioRecord.RECORDSTATE_RECORDING)
        throw new IllegalStateException("A ROM não iniciou a captura FM");
      status(token, "Fonte FM direta · PCM 44,1 kHz · " + (stereo ? "estéreo → mono" : "mono"));
      int frames = RATE / 30;
      short[] buffer = new short[(stereo ? frames * 2 : frames) + 1];
      long lastFrame = 0;
      AudioMeter meter = new AudioMeter();
      AudioAnalysis.Analyzer analyzer = new AudioAnalysis.Analyzer();
      PcmFrameBuffer pcmFrames = new PcmFrameBuffer(stereo ? 2 : 1);
      int emptyReads = 0;
      while (active && token == generation) {
        int offset = pcmFrames.prepare(buffer);
        int n = r.read(buffer, offset, stereo ? frames * 2 : frames);
        if (n < 0) {
          if (!active || token != generation) break;
          throw new IllegalStateException("Leitura FM recusada: " + n);
        }
        if (n == 0) {
          if (++emptyReads >= 100) throw new IllegalStateException("Fonte FM sem amostras");
          Thread.sleep(10);
          continue;
        }
        emptyReads = 0;
        n = pcmFrames.complete(buffer, n);
        if (n == 0) continue;
        meter.process(buffer, n, stereo ? 2 : 1, RATE);
        long now = SystemClock.elapsedRealtime();
        if (now - lastFrame < 30 || !frameGate.acquire(token)) continue;
        lastFrame = now;
        int length = stereo ? n / 2 : n;
        final short[] samples = new short[length];
        if (stereo) {
          for (int i = 0; i < length; i++)
            samples[i] = (short) (((int) buffer[i * 2] + buffer[i * 2 + 1]) / 2);
        } else System.arraycopy(buffer, 0, samples, 0, length);
        final AudioAnalysis analysis = analyzer.analyze(samples, RATE).withMeter(meter.snapshot());
        main.post(
            new Runnable() {
              public void run() {
                try {
                  if (active && token == generation) listener.onFrame(samples, RATE, analysis);
                } finally {
                  frameGate.release(token);
                }
              }
            });
      }
    } catch (Exception e) {
      if (active && token == generation)
        error = e.getClass().getSimpleName() + " · " + e.getMessage();
    } finally {
      recorder = null;
      if (r != null) {
        try {
          r.stop();
        } catch (Exception ignored) {
        }
        try {
          r.release();
        } catch (Exception ignored) {
        }
      }
      synchronized (this) {
        active = false;
        thread = null;
        frameGate.release(token);
      }
      if (error != null) {
        status(
            token, "Áudio FM indisponível: " + error + ". O sinal de rádio continua independente.");
        main.post(
            new Runnable() {
              public void run() {
                if (token == generation) listener.onStopped();
              }
            });
      }
    }
  }

  synchronized void stop() {
    if (!active) return;
    boolean wasActive = active;
    active = false;
    final int token = ++generation;
    frameGate.reset(token);
    final AudioRecord r = recorder;
    // Vendor stop() may block while releasing a read. Never run it on the UI thread.
    if (r != null)
      new Thread(
              new Runnable() {
                public void run() {
                  try {
                    r.stop();
                  } catch (Exception ignored) {
                  }
                }
              },
              "FM-Lab-AudioStop")
          .start();
    if (wasActive) status(token, "Captura FM desativada");
  }
}

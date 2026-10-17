package br.local.fmlab;

import android.content.Context;
import android.graphics.*;
import android.os.SystemClock;
import android.view.View;
import java.util.Locale;

final class ScopeView extends View {
  private boolean frozen;

  void freeze(boolean value) {
    frozen = value;
    invalidate();
  }

  private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
  private final Path path = new Path();
  private short[] samples;
  private int sampleRate = 44100, timebase = 10, windowStart = 0, windowLength = 0;
  private long timestamp = 0;
  private double rms = 0, peak = 0;
  private final float density;

  ScopeView(Context c) {
    super(c);
    density = getResources().getDisplayMetrics().density;
    setContentDescription(
        "Osciloscópio da fonte de áudio FM. Toque para mudar a janela entre 5, 10 e 20"
            + " milissegundos.");
    setOnClickListener(
        new OnClickListener() {
          public void onClick(View v) {
            timebase = timebase == 5 ? 10 : timebase == 10 ? 20 : 5;
            measureWindow();
            invalidate();
          }
        });
  }

  void update(short[] data, int rate) {
    samples = data;
    sampleRate = rate;
    timestamp = SystemClock.elapsedRealtime();
    measureWindow();
    invalidate();
  }

  private void measureWindow() {
    if (samples == null) return;
    windowLength = Math.min(samples.length, (int) (sampleRate * timebase / 1000.0));
    windowStart = ScopeTrigger.start(samples, windowLength);
    double energy = 0, p = 0;
    for (int i = 0; i < windowLength; i++) {
      double a = samples[windowStart + i] / 32768.0;
      energy += a * a;
      p = Math.max(p, Math.abs(a));
    }
    rms = Math.sqrt(energy / Math.max(1, windowLength));
    peak = p;
  }

  void clear() {
    samples = null;
    timestamp = 0;
    rms = 0;
    peak = 0;
    invalidate();
  }

  String summary() {
    if (samples == null || (!frozen && SystemClock.elapsedRealtime() - timestamp > 500))
      return "Sem amostras recentes da fonte FM";
    return String.format(
        Locale.US,
        "Janela: RMS %s dBFS · pico %.1f%% · %d ms · PCM 44,1 kHz",
        rms > 0 ? String.format(Locale.US, "%.1f", 20 * Math.log10(rms)) : "−∞",
        peak * 100,
        timebase);
  }

  protected void onDraw(Canvas c) {
    super.onDraw(c);
    float w = getWidth(),
        h = getHeight(),
        left = 12 * density,
        right = w - 12 * density,
        top = 24 * density,
        bottom = h - 24 * density,
        mid = (top + bottom) / 2;
    c.drawColor(0xff0b1624);
    paint.setStyle(Paint.Style.FILL);
    paint.setStrokeWidth(density * .5f);
    paint.setColor(0xff21354b);
    for (int i = 0; i <= 8; i++) {
      float x = left + (right - left) * i / 8;
      c.drawLine(x, top, x, bottom, paint);
    }
    for (int i = 0; i <= 4; i++) {
      float y = top + (bottom - top) * i / 4;
      c.drawLine(left, y, right, y, paint);
    }
    paint.setTextSize(10 * density);
    paint.setColor(0xff91a4be);
    c.drawText("ÁUDIO FM · PCM · amplitude digital", left, 13 * density, paint);
    c.drawText("−1 / 0 / +1 FS", left, h - 7 * density, paint);
    c.drawText(
        timebase + " ms · toque para mudar",
        Math.max(left, right - 150 * density),
        h - 7 * density,
        paint);
    if (samples == null || (!frozen && SystemClock.elapsedRealtime() - timestamp > 500)) {
      paint.setTextSize(12 * density);
      c.drawText("Aguardando áudio real da fonte FM", left, mid, paint);
      return;
    }
    int length = windowLength, start = windowStart;
    path.reset();
    for (int i = 0; i < length; i++) {
      float x = left + (right - left) * i / Math.max(1, length - 1),
          y = mid - samples[start + i] / 32768f * (bottom - top) / 2;
      if (i == 0) path.moveTo(x, y);
      else path.lineTo(x, y);
    }
    // Preserve sample extrema when multiple PCM samples map to one pixel.
    paint.setColor(0xff77b9ff);
    paint.setStrokeWidth(density);
    int pixels = Math.max(1, (int) (right - left));
    if (length > pixels)
      for (int px = 0; px < pixels; px++) {
        int a = px * length / pixels, b = Math.max(a + 1, (px + 1) * length / pixels);
        int lo = 32767, hi = -32768;
        for (int j = a; j < b; j++) {
          lo = Math.min(lo, samples[start + j]);
          hi = Math.max(hi, samples[start + j]);
        }
        float x = left + (right - left) * px / pixels;
        c.drawLine(
            x,
            mid - hi / 32768f * (bottom - top) / 2,
            x,
            mid - lo / 32768f * (bottom - top) / 2,
            paint);
      }
    paint.setColor(0xff77b9ff);
    paint.setStyle(Paint.Style.STROKE);
    paint.setStrokeWidth(1.5f * density);
    c.drawPath(path, paint);
    paint.setStyle(Paint.Style.FILL);
  }
}

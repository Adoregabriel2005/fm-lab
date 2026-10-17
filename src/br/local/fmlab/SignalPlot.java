package br.local.fmlab;

import android.content.Context;
import android.graphics.*;
import android.os.SystemClock;
import android.view.View;

/** Thirty-FPS scrolling renderer. Every stored point is a real driver sample. */
final class SignalPlot extends View {
  private final int[] values = new int[640];
  private final long[] times = new long[640];
  private int count = 0, head = 0;
  private boolean running = false;
  private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
  private final Path path = new Path();
  private final float density;
  private final Runnable frame =
      new Runnable() {
        public void run() {
          if (running) {
            invalidate();
            postDelayed(this, 33);
          }
        }
      };

  SignalPlot(Context c) {
    super(c);
    density = getResources().getDisplayMetrics().density;
    setContentDescription("Histórico RSSI, janela de 15 segundos, apenas amostras reais");
  }

  void setRunning(boolean enabled) {
    if (running == enabled) return;
    running = enabled;
    removeCallbacks(frame);
    if (enabled) post(frame);
  }

  void add(int value, long timestamp) {
    values[head] = value;
    times[head] = timestamp;
    head = (head + 1) % values.length;
    count = Math.min(values.length, count + 1);
    invalidate();
  }

  void clear() {
    count = 0;
    head = 0;
    invalidate();
  }

  protected void onDetachedFromWindow() {
    setRunning(false);
    super.onDetachedFromWindow();
  }

  protected void onDraw(Canvas c) {
    super.onDraw(c);
    float w = getWidth(),
        h = getHeight(),
        left = 38 * density,
        right = w - 10 * density,
        top = 24 * density,
        bottom = h - 25 * density;
    c.drawColor(Color.rgb(16, 27, 40));
    paint.setStyle(Paint.Style.FILL);
    paint.setTextSize(10 * density);
    paint.setColor(Color.rgb(131, 151, 172));
    long now = SystemClock.elapsedRealtime();
    int lo = Integer.MAX_VALUE, hi = Integer.MIN_VALUE, n = 0;
    for (int i = 0; i < count; i++) {
      int j = (head - count + i + values.length) % values.length;
      if (now - times[j] <= 15000) {
        lo = Math.min(lo, values[j]);
        hi = Math.max(hi, values[j]);
        n++;
      }
    }
    if (n == 0) {
      c.drawText("Aguardando sinal do rádio", 12 * density, h / 2, paint);
      return;
    }
    double range = Math.max(4.0, (double) hi - lo),
        lower = lo - (range - ((double) hi - lo)) / 2 - 1;
    range += 2;
    for (int i = 0; i <= 4; i++) {
      float y = top + (bottom - top) * i / 4;
      paint.setColor(Color.rgb(35, 49, 66));
      paint.setStrokeWidth(density * .5f);
      c.drawLine(left, y, right, y, paint);
      paint.setColor(Color.rgb(131, 151, 172));
      c.drawText(
          String.format(java.util.Locale.US, "%.0f", lower + range * (1 - i / 4.0)),
          2 * density,
          y + 3 * density,
          paint);
    }
    for (int i = 0; i <= 3; i++) {
      float x = left + (right - left) * i / 3;
      paint.setColor(Color.rgb(35, 49, 66));
      c.drawLine(x, top, x, bottom, paint);
      paint.setColor(Color.rgb(131, 151, 172));
      c.drawText(
          i == 3 ? "agora" : (15 - i * 5) + " s",
          Math.min(x, right - 26 * density),
          h - 7 * density,
          paint);
    }
    path.reset();
    boolean started = false;
    long previous = 0;
    for (int i = 0; i < count; i++) {
      int j = (head - count + i + values.length) % values.length;
      long age = now - times[j];
      if (age > 15000) continue;
      float x = left + (right - left) * (1 - age / 15000f),
          y = (float) (bottom - (bottom - top) * (values[j] - lower) / range);
      if (!started || times[j] - previous > 1000) path.moveTo(x, y);
      else path.lineTo(x, y);
      started = true;
      previous = times[j];
    }
    paint.setColor(Color.rgb(81, 226, 184));
    paint.setStrokeWidth(2 * density);
    paint.setStyle(Paint.Style.STROKE);
    c.drawPath(path, paint);
    paint.setStyle(Paint.Style.FILL);
    paint.setTextSize(10 * density);
    c.drawText("RSSI · escala automática", left, 13 * density, paint);
  }
}

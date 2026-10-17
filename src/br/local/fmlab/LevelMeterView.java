package br.local.fmlab;
import android.content.Context;
import android.graphics.*;
import android.os.SystemClock;
import android.view.View;
import java.util.Locale;
final class LevelMeterView extends View {
 private final Paint p=new Paint();private final float d;private AudioMeter.Reading data;private long timestamp;
 LevelMeterView(Context c){super(c);d=getResources().getDisplayMetrics().density;setContentDescription("Medidores digitais: RMS com resposta de 300 ms, pico de amostras com retenção e contagem de amostras no limite digital.");}
 void update(AudioMeter.Reading r){data=r;timestamp=SystemClock.elapsedRealtime();invalidate();}
 void clear(){data=null;invalidate();}
 private float x(double db,float left,float right){return left+(float)Math.max(0,Math.min(1,(db+60)/60))*(right-left);}
 protected void onDraw(Canvas c){super.onDraw(c);c.drawColor(0xff0b1624);p.setTextSize(10*d);float left=30*d,right=getWidth()-10*d;p.setColor(0xff91a4be);for(int db=-60;db<=0;db+=12)c.drawText(""+db,x(db,left,right)-7*d,13*d,p);
 if(data==null||SystemClock.elapsedRealtime()-timestamp>800){c.drawText("Aguardando níveis do PCM FM",left,50*d,p);return;}
 for(int ch=0;ch<data.channels;ch++){float top=(24+ch*53)*d,bottom=top+14*d;p.setColor(0xff91a4be);c.drawText(data.channels==1?"M":ch==0?"L":"R",4*d,bottom,p);p.setColor(0xff22364a);c.drawRect(left,top,right,bottom,p);p.setColor(data.rms300[ch]>=-1?0xffff6b7a:data.rms300[ch]>=-6?0xffffc568:0xff51e2b8);c.drawRect(left,top,x(data.rms300[ch],left,right),bottom,p);p.setColor(0xffffc568);float held=x(data.hold[ch],left,right);c.drawRect(held-1,top-2*d,held+1,bottom+2*d,p);p.setColor(0xffe6edf7);p.setTextSize(9*d);c.drawText(String.format(Locale.US,"RMS300 %.1f · pico %.1f · hold %.1f dBFS",data.rms300[ch],data.peak[ch],data.hold[ch]),left,bottom+13*d,p);c.drawText("Amostras no limite digital: "+data.fullScale[ch],left,bottom+25*d,p);p.setTextSize(10*d);}
 }
}

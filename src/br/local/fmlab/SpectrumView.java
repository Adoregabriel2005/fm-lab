package br.local.fmlab;

import android.content.Context;
import android.graphics.*;
import android.os.SystemClock;
import android.view.View;

final class SpectrumView extends View {
 private boolean frozen;
 void freeze(boolean value){frozen=value;invalidate();}

 private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
 private float[] bands;private long timestamp;private final float d;
 SpectrumView(Context c){super(c);d=getResources().getDisplayMetrics().density;setContentDescription("Espectro do áudio FM, de 43 Hz a 20 kHz, escala de amplitude em dBFS");}
 void update(AudioAnalysis a){bands=a.bands;timestamp=SystemClock.elapsedRealtime();invalidate();}
 void clear(){bands=null;invalidate();}
 protected void onDraw(Canvas c){super.onDraw(c);float w=getWidth(),h=getHeight(),left=30*d,right=w-8*d,top=18*d,bottom=h-22*d;c.drawColor(0xff0b1624);paint.setTextSize(9*d);
  for(int i=0;i<=3;i++){float y=top+(bottom-top)*i/3;paint.setColor(0xff22364a);c.drawLine(left,y,right,y,paint);paint.setColor(0xff91a4be);c.drawText(""+(-30*i),0,y+3*d,paint);}
  paint.setColor(0xff91a4be);c.drawText("43 Hz",left,h-5*d,paint);c.drawText("1 kHz",(left+right)/2,h-5*d,paint);c.drawText("20 kHz",right-35*d,h-5*d,paint);
  if(bands==null||(!frozen&&SystemClock.elapsedRealtime()-timestamp>800)){c.drawText("Ative a captura FM para analisar o áudio",left,(top+bottom)/2,paint);return;}
  float bw=(right-left)/bands.length;for(int i=0;i<bands.length;i++){float db=Math.max(-90,Math.min(0,bands[i]));float y=bottom-(bottom-top)*(db+90)/90;paint.setColor(i<20?0xff51e2b8:0xff77b9ff);c.drawRect(left+i*bw+1,y,left+(i+1)*bw-1,bottom,paint);}
 }
}

package br.local.fmlab;
import android.content.Context;
import android.graphics.*;
import android.os.SystemClock;
import android.view.View;
/** 8 seconds of 32-band audio FFT history, in 100 ms buckets. Not RF spectrum. */
final class SpectrogramView extends View {
 private final Paint p=new Paint();private final float[][] values=new float[80][32];private final long[] buckets=new long[80];private final float d;private boolean frozen;private long freezeTime;
 SpectrogramView(Context c){super(c);d=getResources().getDisplayMetrics().density;clear();setContentDescription("Espectrograma temporal do áudio FM: frequência vertical, últimos oito segundos na horizontal; brilho indica amplitude digital.");}
 void update(AudioAnalysis a){if(frozen)return;long b=SystemClock.elapsedRealtime()/100;int i=(int)(b%80);boolean changed=buckets[i]!=b;buckets[i]=b;System.arraycopy(a.bands,0,values[i],0,32);if(changed)invalidate();}
 void freeze(boolean f){frozen=f;freezeTime=SystemClock.elapsedRealtime();invalidate();}
 void clear(){for(int i=0;i<80;i++)buckets[i]=-1;frozen=false;invalidate();}
 protected void onDraw(Canvas c){super.onDraw(c);float left=40*d,right=getWidth()-6*d,top=18*d,bottom=getHeight()-23*d;c.drawColor(0xff0b1624);long now=(frozen?freezeTime:SystemClock.elapsedRealtime())/100;
 for(int x=0;x<80;x++){long b=now-79+x;if(b<0)continue;int i=(int)(b%80);if(buckets[i]!=b)continue;for(int k=0;k<32;k++){float v=Math.max(0,Math.min(1,(values[i][k]+90)/90));p.setColor(Color.rgb((int)(255*Math.max(0,(v-.65)/.35)),(int)(225*v),(int)(80+150*v)));float x0=left+(right-left)*x/80,y0=bottom-(bottom-top)*(k+1)/32;c.drawRect(x0,y0,left+(right-left)*(x+1)/80+1,bottom-(bottom-top)*k/32+1,p);}}
 p.setTextSize(9*d);p.setColor(0xff91a4be);c.drawText("20 kHz",0,top+7*d,p);c.drawText("1 kHz",0,(top+bottom)/2,p);c.drawText("43 Hz",0,bottom,p);c.drawText("−8 s",left,getHeight()-5*d,p);c.drawText(frozen?"congelado":"agora",right-50*d,getHeight()-5*d,p);c.drawText("ÁUDIO · escuro −90 / claro 0 dBFS",left,11*d,p);
 }
}

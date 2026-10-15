package br.local.fmlab;

import android.content.Context;
import android.graphics.*;
import android.os.SystemClock;
import android.view.View;
import java.util.Locale;

final class ScopeView extends View {
    private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path path=new Path();
    private short[] samples;
    private int sampleRate=44100,timebase=10;
    private long timestamp=0;
    private double rms=0,peak=0;
    private final float density;
    ScopeView(Context c){super(c);density=getResources().getDisplayMetrics().density;setContentDescription("Osciloscópio da fonte de áudio FM. Toque para mudar a janela entre 5, 10 e 20 milissegundos.");setOnClickListener(new OnClickListener(){public void onClick(View v){timebase=timebase==5?10:timebase==10?20:5;invalidate();}});}
    void update(short[] data,int rate){samples=data;sampleRate=rate;timestamp=SystemClock.elapsedRealtime();double energy=0,p=0;for(short value:data){double a=value/32768.0;energy+=a*a;p=Math.max(p,Math.abs(a));}rms=Math.sqrt(energy/Math.max(1,data.length));peak=p;invalidate();}
    void clear(){samples=null;timestamp=0;rms=0;peak=0;invalidate();}
    String summary(){if(samples==null||SystemClock.elapsedRealtime()-timestamp>500)return "Sem amostras recentes da fonte FM";return String.format(Locale.US,"RMS %s dBFS · pico %.1f%% · janela %d ms",rms>0?String.format(Locale.US,"%.1f",20*Math.log10(rms)):"−∞",peak*100,timebase);}
    protected void onDraw(Canvas c){
        super.onDraw(c);float w=getWidth(),h=getHeight(),left=12*density,right=w-12*density,top=24*density,bottom=h-24*density,mid=(top+bottom)/2;c.drawColor(0xff0b1624);paint.setStyle(Paint.Style.FILL);paint.setStrokeWidth(density*.5f);paint.setColor(0xff21354b);
        for(int i=0;i<=8;i++){float x=left+(right-left)*i/8;c.drawLine(x,top,x,bottom,paint);}for(int i=0;i<=4;i++){float y=top+(bottom-top)*i/4;c.drawLine(left,y,right,y,paint);}
        paint.setTextSize(10*density);paint.setColor(0xff91a4be);c.drawText("ÁUDIO FM · PCM · amplitude digital",left,13*density,paint);c.drawText("0",left,h-7*density,paint);c.drawText(timebase+" ms · toque para mudar",Math.max(left,right-150*density),h-7*density,paint);
        if(samples==null||SystemClock.elapsedRealtime()-timestamp>500){paint.setTextSize(12*density);c.drawText("Aguardando áudio real da fonte FM",left,mid,paint);return;}
        int length=Math.min(samples.length,(int)(sampleRate*timebase/1000.0)),start=0;
        for(int i=1;i+length<samples.length;i++){if(samples[i-1]<=0&&samples[i]>0){start=i;break;}}
        path.reset();for(int i=0;i<length;i++){float x=left+(right-left)*i/Math.max(1,length-1),y=mid-samples[start+i]/32768f*(bottom-top)/2;if(i==0)path.moveTo(x,y);else path.lineTo(x,y);}
        paint.setColor(0xff77b9ff);paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(1.5f*density);c.drawPath(path,paint);paint.setStyle(Paint.Style.FILL);
    }
}

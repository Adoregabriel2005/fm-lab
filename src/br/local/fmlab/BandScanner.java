package br.local.fmlab;
import android.os.SystemClock;
import java.util.*;
/** Sequential scan. One tuner, one transaction at a time, real measurements only. */
final class BandScanner {
 interface Listener {void tuning(int frequency);Boolean stereo();void progress(String text);void finished(List<Result> results,String error,boolean cancelled,boolean restored);}
 static final class Result {final int frequency;double rssi,sinr,sinrSquared;int rn,sn;String name="",text="";Boolean stereo;
  Result(int f){frequency=f;}void add(Integer r,Integer s){if(r!=null){rssi+=r;rn++;}if(s!=null){sinr+=s;sinrSquared+=(double)s*s;sn++;}}
  double sinrSd(){return sn==0?Double.NaN:Math.sqrt(Math.max(0,sinrSquared/sn-sinrMean()*sinrMean()));}double rssiMean(){return rn==0?Double.NaN:rssi/rn;}double sinrMean(){return sn==0?Double.NaN:sinr/sn;}}
 private volatile boolean running=false,cancel=false,restore=true;
 boolean isRunning(){return running;}
 void cancel(boolean restoreFrequency){restore=restoreFrequency;cancel=true;}
 static void sort(List<Result> a){Collections.sort(a,new Comparator<Result>(){public int compare(Result x,Result y){double sx=x.sn==0?-Double.MAX_VALUE:x.sinrMean(),sy=y.sn==0?-Double.MAX_VALUE:y.sinrMean();int c=Double.compare(sy,sx);if(c!=0)return c;c=Double.compare(x.sn==0?Double.MAX_VALUE:x.sinrSd(),y.sn==0?Double.MAX_VALUE:y.sinrSd());if(c!=0)return c;double rx=x.rn==0?-Double.MAX_VALUE:x.rssiMean(),ry=y.rn==0?-Double.MAX_VALUE:y.rssiMean();return Double.compare(ry,rx);}});}
 synchronized void start(final RadioBridge radio,final int original,final int step,final Listener l){if(running)return;running=true;cancel=false;restore=true;new Thread(new Runnable(){public void run(){scan(radio,original,step,l);}},"FM-Lab-Scan").start();}
 private Integer number(RadioBridge r,int code){try{int v=r.number(code,null);return RadioBridge.valid(v)?v:null;}catch(Exception e){return null;}}
 private void waitMs(long ms)throws InterruptedException{long end=SystemClock.elapsedRealtime()+ms;while(!cancel&&SystemClock.elapsedRealtime()<end)Thread.sleep(Math.min(50,end-SystemClock.elapsedRealtime()+1));}
 private void scan(RadioBridge r,int original,int step,Listener l){List<Result> all=new ArrayList<Result>();String error="";boolean mutedByUs=false,restored=false;try{
  if(r.number(RadioBridge.IS_ON,null)==0)throw new IllegalStateException("Ligue o FM antes de varrer");
  r.number(32,1);
  if(r.number(RadioBridge.MUTED,null)==0)mutedByUs=r.number(11,null)!=0;
  int total=(108000-87500)/step+1,index=0;
  for(int f=87500;f<=108000&&!cancel;f+=step){
   if(r.number(RadioBridge.IS_ON,null)==0)throw new IllegalStateException("O rádio foi desligado durante a busca");
   l.progress("Busca "+(++index)+"/"+total+" · "+String.format(Locale.US,"%.1f MHz",f/1000.0));l.tuning(f);
   if(r.number(RadioBridge.TUNE,f)==0)continue;waitMs(350);Result result=new Result(f);
   for(int i=0;i<3&&!cancel;i++){result.add(number(r,RadioBridge.RSSI),number(r,RadioBridge.SINR));waitMs(80);}
   result.stereo=l.stereo();if(result.sn>0&&result.sinrMean()>=1)all.add(result);
  }
  sort(all);List<Result> refined=new ArrayList<Result>();int limit=Math.min(12,all.size());
  for(int i=0;i<limit&&!cancel;i++){int f=all.get(i).frequency;l.progress("Comparando candidato "+(i+1)+"/"+limit+" · "+String.format(Locale.US,"%.1f MHz",f/1000.0));l.tuning(f);if(r.number(RadioBridge.TUNE,f)==0)continue;waitMs(1500);Result result=new Result(f);
   for(int j=0;j<8&&!cancel;j++){result.add(number(r,RadioBridge.RSSI),number(r,RadioBridge.SINR));waitMs(100);}
   if(cancel)break;try{result.name=r.text(RadioBridge.PS);result.text=r.text(RadioBridge.RT);}catch(Exception ignored){}result.stereo=l.stereo();refined.add(result);
  }
  if(!cancel){all=refined;sort(all);}
 }catch(Exception e){error=e.getClass().getSimpleName()+": "+e.getMessage();}
 finally{
  if(restore&&original>=87500&&original<=108000)try{if(r.number(RadioBridge.IS_ON,null)!=0){l.tuning(original);restored=r.number(RadioBridge.TUNE,original)!=0;}}catch(Exception ignored){}
  if(mutedByUs)try{r.number(13,null);}catch(Exception ignored){}
  running=false;l.finished(all,error,cancel,restored);
 }
 }
}

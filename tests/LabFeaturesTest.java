package br.local.fmlab;
public class LabFeaturesTest {
 static int count;
 static void check(boolean v,String text){count++;if(!v)throw new AssertionError(text);}
 public static void main(String[] args){
  short[] silence=new short[1470];AudioAnalysis a=AudioAnalysis.analyze(silence,44100);
  check(a.rmsDb==-120,"silence floor");check(a.dominantHz==0,"silence has no dominant tone");for(float b:a.bands)check(b==-90,"silent spectrum floor");
  short[] tone=new short[1470];double hz=23*44100.0/1024;
  for(int i=0;i<tone.length;i++)tone[i]=(short)Math.round(16384*Math.sin(2*Math.PI*hz*i/44100));
  a=AudioAnalysis.analyze(tone,44100);
  check(Math.abs(a.dominantHz-hz)<1,"FFT tone bin frequency");check(Math.abs(a.rmsDb+9.03)<.15,"half-scale sine RMS");check(Math.abs(a.peakPercent-50)<.1,"half-scale sine peak");check(a.clippedFraction==0,"clean tone not clipped");
  float peak=-90;for(float b:a.bands)peak=Math.max(peak,b);check(Math.abs(peak+6.02)<.15,"Hann coherent-gain normalization");
  short[] dc=new short[1470];java.util.Arrays.fill(dc,(short)32767);a=AudioAnalysis.analyze(dc,44100);
  check(a.clippedFraction==1,"near full-scale samples counted");check(a.dominantHz==0,"DC is removed from spectrum");
  Measurement m=new Measurement(0,107900,"Sala",0,5,15);m.add(10,4);m.add(null,null);m.add(20,5);m.add(30,15);
  check(m.cycles==4,"all RF cycles counted");check(m.rssi.n==3,"missing RSSI excluded");check(m.sinr.n==3,"missing SINR excluded");check(m.rssi.mean==20,"online mean");check(Math.abs(m.rssi.sd()-10)<.00001,"sample standard deviation");check(m.poor==1&&m.classified==3,"poor fraction only valid readings");check(m.rssi.min==10&&m.rssi.max==30,"range");
  check(LabFeatures.csvField("A;B").equals("\"A;B\""),"CSV separator quoted");check(LabFeatures.csvField("A\"B").equals("\"A\"\"B\""),"CSV quote escaped");check(LabFeatures.csvField("=1+1").equals("\"'=1+1\""),"CSV formula escaped");check(LabFeatures.csvField("x\ny").equals("\"x y\""),"CSV RDS newline normalized");
  System.out.println(count+" audio, measurement and CSV checks passed");
 }
}

package br.local.fmlab;
public class SignalQualityTest {
 private static void check(boolean result,String name){if(!result)throw new AssertionError(name);}
 public static void main(String[] args){
  check(SignalQuality.classify(null,5,15)==-1,"missing data");
  check(SignalQuality.classify(Integer.MAX_VALUE,5,15)==-1,"driver sentinel");
  check(SignalQuality.classify(-8,5,15)==0,"negative SINR");
  check(SignalQuality.classify(4,5,15)==0,"poor below boundary");
  check(SignalQuality.classify(5,5,15)==1,"regular boundary");
  check(SignalQuality.classify(14,5,15)==1,"regular upper range");
  check(SignalQuality.classify(15,5,15)==2,"good boundary");
  check(SignalQuality.classify(-110,-105,-90)==0,"custom RSSI poor");
  check(SignalQuality.classify(-100,-105,-90)==1,"custom RSSI regular");
  check(SignalQuality.classify(-85,-105,-90)==2,"custom RSSI good");
  boolean invalid=false;try{SignalQuality.classify(10,15,5);}catch(IllegalArgumentException e){invalid=true;}check(invalid,"reversed thresholds");
  check(SignalQuality.delay(100,30)==70,"10Hz period compensates read time");
  check(SignalQuality.delay(50,200)==16,"slow driver does not pile up immediate polls");
  check(Math.abs(SignalQuality.rateHz(10,1100)-9.090909)<.0001,"rate uses window count rather than average of reciprocals");
  check(SignalQuality.rateHz(10,0)==0,"zero rate window");
  System.out.println("15 quality and rate policy checks passed");
 }
}

package br.local.fmlab;
public class ReceptionTrialTest {
 static int checks;
 static void check(boolean b){checks++;if(!b)throw new AssertionError("check "+checks);}
 static void near(double a,double b){check(Math.abs(a-b)<1e-9);}
 public static void main(String[] args){
  ReceptionTrial t=new ReceptionTrial();check(!t.active());t.start(100);check(t.active());check(t.remaining(100)==10000);check(t.finish(500)==null);check(t.active());
  for(int i=0;i<100;i++)t.sample(i%2==0?-2:2,100+i*100);
  ReceptionTrial.Result r=t.finish(10100);check(r!=null);check(r.count==100);near(r.mean,0);near(r.sd,2);check(r.min==-2&&r.max==2);check(!t.active());check(t.finish(10200)==null);
  t.start(0);t.sample(12,0);check(t.finish(10000)==null);
  t.start(0);for(int i=0;i<100;i++)t.sample(null,i*100);check(t.finish(10000)==null);
  t.start(0);for(int i=0;i<100;i++)if(i<40||i>60)t.sample(10,i*100);check(t.finish(10000)==null);
  t.start(0);for(int i=0;i<70;i++)t.sample(10,i*100);check(t.finish(10000)==null);
  t.start(0);for(int i=30;i<100;i++)t.sample(10,i*100);check(t.finish(10000)==null);
  t.start(0);for(int i=0;i<100;i++)t.sample(10,i*100);r=t.finish(10000);check(r!=null);near(r.mean,10);near(r.sd,0);
  t.start(0);t.cancel();check(!t.active());check(t.finish(10000)==null);
  System.out.println(checks+" A/B reception trial checks passed");
 }
}

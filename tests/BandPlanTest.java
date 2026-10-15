package br.local.fmlab;
public class BandPlanTest {
 static void check(boolean v){if(!v)throw new AssertionError();}
 public static void main(String[] args){check(BandPlan.valid(76000));check(BandPlan.valid(76100));check(BandPlan.valid(87300));check(BandPlan.valid(108000));check(!BandPlan.valid(75900));check(!BandPlan.valid(108100));check(!BandPlan.valid(76150));check(BandPlan.lower(0,200)==76100);check(BandPlan.lower(0,100)==76000);check(BandPlan.lower(1,200)==87500);check(BandPlan.upper(2)==87300);check(BandPlan.count(76100,108000,200)==160);check(BandPlan.count(76000,108000,100)==321);check(BandPlan.count(76100,87300,200)==57);System.out.println("14 extended-band checks passed");}
}

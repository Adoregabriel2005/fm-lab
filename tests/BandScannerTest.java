package br.local.fmlab;

import java.util.*;

public class BandScannerTest {
  static void check(boolean b, String label) {
    if (!b) throw new AssertionError(label);
  }

  public static void main(String[] args) {
    BandScanner.Result weak = new BandScanner.Result(90100);
    weak.add(220, 7);
    weak.add(220, 9);
    BandScanner.Result strong = new BandScanner.Result(93100);
    strong.add(195, 20);
    strong.add(197, 22);
    BandScanner.Result tie = new BandScanner.Result(99900);
    tie.add(200, 21);
    BandScanner.Result absent = new BandScanner.Result(101100);
    absent.add(250, null);
    List<BandScanner.Result> a =
        new ArrayList<BandScanner.Result>(Arrays.asList(weak, absent, strong, tie));
    BandScanner.sort(a);
    check(a.get(0) == tie, "RSSI tie-break only after equal SINR");
    check(a.get(1) == strong, "stronger SINR beats higher raw RSSI");
    check(a.get(2) == weak, "lower SINR follows strong candidates");
    check(a.get(3) == absent, "missing SINR last");
    check(weak.sinrMean() == 8, "SINR average");
    check(strong.rssiMean() == 196, "RSSI average");
    check(Double.isNaN(absent.sinrMean()), "absent readings not zero");
    BandScanner.Result stableLowerRssi = new BandScanner.Result(103300);
    stableLowerRssi.add(180, 21);
    stableLowerRssi.add(180, 21);
    List<BandScanner.Result> stability =
        new ArrayList<BandScanner.Result>(Arrays.asList(strong, stableLowerRssi));
    BandScanner.sort(stability);
    check(stability.get(0) == stableLowerRssi, "lower SINR variation wins before higher RSSI");
    check(Math.abs(strong.sinrSd() - 1) < .00001, "SINR population deviation");
    check(
        RadioBridge.sinrValue(254) == -2, "unsigned 254 is negative SINR, not excellent reception");
    check(RadioBridge.sinrValue(128) == -128, "signed byte lower boundary");
    check(RadioBridge.sinrValue(127) == 127, "positive signed boundary");
    check(RadioBridge.sinrValue(255) == -1, "unsigned minus one");
    check(RadioBridge.sinrValue(-2) == -2, "already signed stays signed");
    check(RadioBridge.sinrValue(Integer.MAX_VALUE) == null, "unavailable SINR not wrapped");
    check(RadioBridge.sinrValue(300) == null, "out-of-range SINR rejected");
    System.out.println("16 scan ranking and SINR representation checks passed");
  }
}

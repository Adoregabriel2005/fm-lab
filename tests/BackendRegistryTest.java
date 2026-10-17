package br.local.fmlab;

public class BackendRegistryTest {
  static void check(boolean v) {
    if (!v) throw new AssertionError();
  }

  public static void main(String[] args) {
    check(BackendRegistry.supported("com.caf.fmradio", 25, "7.1.2"));
    check(!BackendRegistry.supported("com.caf.fmradio", 26, "7.1.2"));
    check(!BackendRegistry.supported("com.caf.fmradio", 25, "8.0"));
    check(!BackendRegistry.supported("com.motorola.fmplayer", 25, "7.1.2"));
    check(!BackendRegistry.supported(null, 25, null));
    check(BackendRegistry.service("com.sec.android.app.fm", 25, "7.1.2") == null);
    check(
        BackendRegistry.service("com.caf.fmradio", 25, "7.1.2")
            .equals("com.caf.fmradio.FMRadioService"));
    check(!BackendRegistry.supported("com.mediatek.fmradio", 25, "7.1.2"));
    System.out.println("8 backend selection checks passed");
  }
}

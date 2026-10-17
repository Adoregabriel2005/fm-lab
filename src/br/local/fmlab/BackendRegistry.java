package br.local.fmlab;

/** Pure selection policy: package names alone never authorize Binder calls. */
final class BackendRegistry {
  static final String[] PACKAGES = {
    "com.caf.fmradio",
    "com.android.fmradio",
    "com.mediatek.fmradio",
    "com.motorola.fmplayer",
    "com.sec.android.app.fm",
    "com.sonyericsson.fmradio",
    "com.sonyericsson.fmradio3",
    "com.miui.fm",
    "com.huawei.android.FMRadio"
  };

  static boolean supported(String pkg, int code, String version) {
    return "com.caf.fmradio".equals(pkg) && code == 25 && "7.1.2".equals(version);
  }

  static String service(String pkg, int code, String version) {
    return supported(pkg, code, version) ? "com.caf.fmradio.FMRadioService" : null;
  }
}

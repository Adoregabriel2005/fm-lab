package br.local.fmlab;

/** Requested bands only; the native service/firmware remains the final authority. */
final class BandPlan {
  static final int MIN = 76000, MAX = 108000;

  static boolean valid(int khz) {
    return khz >= MIN && khz <= MAX && khz % 100 == 0;
  }

  static int lower(int mode, int step) {
    return mode == 1 ? 87500 : mode == 2 ? 76100 : step == 200 ? 76100 : 76000;
  }

  static int upper(int mode) {
    return mode == 2 ? 87300 : MAX;
  }

  static int count(int low, int high, int step) {
    if (step != 100 && step != 200) throw new IllegalArgumentException("Passo inválido");
    return high < low ? 0 : (high - low) / step + 1;
  }

  static String label(int mode) {
    return mode == 1
        ? "FM convencional · 87,5–108 MHz"
        : mode == 2 ? "Somente FM estendido · 76,1–87,3 MHz" : "FM amplo · 76–108 MHz";
  }
}

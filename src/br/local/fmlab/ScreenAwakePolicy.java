package br.local.fmlab;

/** A running scan must keep the screen awake even if the user's default is off. */
final class ScreenAwakePolicy {
  static boolean keepAwake(boolean userPreference, boolean scanning) {
    return userPreference || scanning;
  }
}

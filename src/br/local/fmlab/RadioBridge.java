package br.local.fmlab;

import android.os.IBinder;
import android.os.RemoteException;

/**
 * Internal operation contract. Other adapters must map these operations to their own verified
 * transport; numeric values are not a universal Binder protocol. CAF-specific callback registration
 * remains transitional, not a vendor-neutral event API.
 */
abstract class RadioBridge {
  static final String DESCRIPTOR = "com.caf.fmradio.IFMRadioService";
  static final int ON = 1,
      OFF = 2,
      IS_ON = 4,
      ANALOG = 5,
      SPEAKER = 7,
      MUTE = 11,
      UNMUTE = 13,
      MUTED = 14,
      TUNE = 17,
      PS = 25,
      RT = 26,
      PTY = 27,
      PI = 28,
      STEREO = 32,
      ANTENNA = 33,
      RSSI = 36,
      IOC = 37,
      MPX = 38,
      INTERFERENCE = 39,
      SINR = 40,
      ECC = 50;

  abstract int number(int operation, Integer argument) throws RemoteException;

  abstract String text(int operation) throws RemoteException;

  abstract void registerEvents(IBinder callback) throws RemoteException;

  static Integer sinrValue(int value) {
    // Iris station SINR is an 8-bit signed quantity; this ARM kernel can
    // promote plain char as unsigned. Preserve already signed values.
    if (value >= -128 && value <= 127) return value;
    if (value >= 128 && value <= 255) return value - 256;
    return null;
  }

  static boolean valid(int value) {
    return value != Integer.MAX_VALUE && value != Integer.MIN_VALUE;
  }
}

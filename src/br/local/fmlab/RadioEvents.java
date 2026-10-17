package br.local.fmlab;

import android.os.*;
import java.lang.ref.WeakReference;

/** Native service has a single callback slot. Reclaim only while FM Lab is foreground. */
final class RadioEvents extends Binder {
  interface Listener {
    void stereoChanged(boolean stereo);
  }

  private static final String TOKEN = "com.caf.fmradio.IFMRadioServiceCallbacks";
  private final WeakReference<Listener> listener;

  RadioEvents(Listener l) {
    listener = new WeakReference<Listener>(l);
    attachInterface(null, TOKEN);
  }

  protected boolean onTransact(int code, Parcel data, Parcel reply, int flags)
      throws RemoteException {
    if (code == INTERFACE_TRANSACTION) {
      if (reply != null) reply.writeString(TOKEN);
      return true;
    }
    if (code < 1 || code > 30) return super.onTransact(code, data, reply, flags);
    data.enforceInterface(TOKEN);
    if (code == 12) {
      boolean stereo = data.readInt() != 0;
      Listener l = listener.get();
      if (l != null) l.stereoChanged(stereo);
    }
    if (reply != null) reply.writeNoException();
    return true;
  }
}

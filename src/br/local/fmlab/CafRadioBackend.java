package br.local.fmlab;

import android.os.IBinder;
import android.os.Parcel;
import android.os.RemoteException;

/** Protocol observed in the supplied eyltws.apk, version 25 / 7.1.2.
 * Uses the observed service interface without system identity or reflection.
 * Event registration uses the native service shared callback slot.
 */
final class CafRadioBackend extends RadioBridge {
    private final IBinder binder;
    CafRadioBackend(IBinder b) throws RemoteException {
        if (!DESCRIPTOR.equals(b.getInterfaceDescriptor())) throw new SecurityException("Serviço incompatível com o APK analisado");
        binder=b;
    }
    int number(int code, Integer argument) throws RemoteException {
        // fmOn initializes vendor objects on a remote Binder thread without a
        // Looper on this ROM. A Looper in our process cannot fix that thread.
        if (code==ON) throw new IllegalStateException("Ligue o FM pela tela do rádio original");
        Parcel in=Parcel.obtain(), out=Parcel.obtain();
        try {
            in.writeInterfaceToken(DESCRIPTOR);
            if(argument!=null) in.writeInt(argument);
            if(!binder.transact(code,in,out,0)) throw new RemoteException("Operação indisponível: "+code);
            out.readException();
            return out.readInt();
        } finally {out.recycle();in.recycle();}
    }
    String text(int code) throws RemoteException {
        Parcel in=Parcel.obtain(), out=Parcel.obtain();
        try {
            in.writeInterfaceToken(DESCRIPTOR);
            if(!binder.transact(code,in,out,0)) throw new RemoteException("RDS indisponível");
            out.readException();
            String value=out.readString();
            return value==null?"":value.trim();
        } finally {out.recycle();in.recycle();}
    }
    void registerEvents(IBinder callback)throws RemoteException{
        Parcel in=Parcel.obtain(),out=Parcel.obtain();try{in.writeInterfaceToken(DESCRIPTOR);in.writeStrongBinder(callback);if(!binder.transact(9,in,out,0))throw new RemoteException("Eventos indisponíveis");out.readException();}finally{out.recycle();in.recycle();}
    }
}

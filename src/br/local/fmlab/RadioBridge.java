package br.local.fmlab;

import android.os.IBinder;
import android.os.Parcel;
import android.os.RemoteException;

/** Protocol observed in the supplied eyltws.apk, version 25 / 7.1.2.
 * Uses the observed service interface without system identity or reflection.
 * Event registration uses the native service shared callback slot.
 */
final class RadioBridge {
    static final String DESCRIPTOR = "com.caf.fmradio.IFMRadioService";
    static final int ON=1, OFF=2, IS_ON=4, ANALOG=5, SPEAKER=7, MUTED=14, TUNE=17,
        PS=25, RT=26, PTY=27, PI=28, ANTENNA=33, RSSI=36, IOC=37, MPX=38, INTERFERENCE=39, SINR=40, ECC=50;
    private final IBinder binder;
    RadioBridge(IBinder b) throws RemoteException {
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
    static boolean valid(int value) {return value!=Integer.MAX_VALUE && value!=Integer.MIN_VALUE;}
    void registerEvents(IBinder callback)throws RemoteException{
        Parcel in=Parcel.obtain(),out=Parcel.obtain();try{in.writeInterfaceToken(DESCRIPTOR);in.writeStrongBinder(callback);if(!binder.transact(9,in,out,0))throw new RemoteException("Eventos indisponíveis");out.readException();}finally{out.recycle();in.recycle();}
    }
}

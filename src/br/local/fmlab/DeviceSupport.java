package br.local.fmlab;
import android.app.*;
import android.content.*;
import android.content.pm.*;
import android.os.Build;
import java.util.*;
/** Read-only inspection of a bounded list of known radio packages.
 * Never enumerates unrelated apps or probes unknown Binder transactions.
 */
final class DeviceSupport {
 private static PackageInfo info(Activity a,String p){try{return a.getPackageManager().getPackageInfo(p,PackageManager.GET_SERVICES);}catch(PackageManager.NameNotFoundException e){return null;}}
 static String report(Activity a){StringBuilder b=new StringBuilder("FM Lab compatibility report\n");b.append("Manufacturer: ").append(Build.MANUFACTURER).append("\nModel: ").append(Build.MODEL).append("\nDevice: ").append(Build.DEVICE).append("\nAndroid: ").append(Build.VERSION.RELEASE).append(" / API ").append(Build.VERSION.SDK_INT).append("\nBuild: ").append(Build.DISPLAY).append("\n\n");int found=0;
 for(String p:BackendRegistry.PACKAGES){PackageInfo i=info(a,p);if(i==null)continue;found++;b.append(p).append(" ").append(i.versionName).append(" (").append(i.versionCode).append(")\n");b.append("Adapter: ").append(BackendRegistry.supported(p,i.versionCode,i.versionName)?"implemented CAF profile; only GT-I9195 / LineageOS 14.1 physically verified":"not implemented; native app launch only").append("\n");
 b.append("Enabled: ").append(i.applicationInfo.enabled).append("\n");if(i.services!=null)for(ServiceInfo s:i.services)b.append("Service: ").append(s.name).append("; exported=").append(s.exported).append("; enabled=").append(s.enabled).append("; permission=").append(s.permission==null?"none declared":s.permission).append("\n");b.append("\n");}
 if(found==0)b.append("No installed radio package from the known candidate list was visible. This does not prove FM hardware is absent.\n");
 return b.append("Package/service presence does not prove access, compatible protocol, audio capture, RDS, stereo, metrics, or band support. No unknown service was called.\nNo device serial, account, location, or unrelated app inventory is included. Review before sharing.\n").toString();}
 static void show(final Activity a){final String text=report(a);new AlertDialog.Builder(a).setTitle("Compatibilidade do aparelho").setMessage(text).setPositiveButton("Copiar relatório",new DialogInterface.OnClickListener(){public void onClick(DialogInterface d,int w){((ClipboardManager)a.getSystemService(Context.CLIPBOARD_SERVICE)).setPrimaryClip(ClipData.newPlainText("FM Lab compatibility",text));}}).setNeutralButton("Abrir rádio nativo",new DialogInterface.OnClickListener(){public void onClick(DialogInterface d,int w){launch(a);}}).setNegativeButton("Fechar",null).show();}
 static void launch(final Activity a){final ArrayList<Intent> intents=new ArrayList<Intent>();ArrayList<String> labels=new ArrayList<String>();for(String p:BackendRegistry.PACKAGES){if(info(a,p)==null)continue;Intent i=a.getPackageManager().getLaunchIntentForPackage(p);if(i!=null){intents.add(i);labels.add(p);}}
 if(intents.isEmpty()){new AlertDialog.Builder(a).setMessage("Nenhum rádio nativo conhecido disponível. Isso não confirma ausência do chip FM.").setPositiveButton("Fechar",null).show();return;}new AlertDialog.Builder(a).setTitle("Abrir rádio instalado").setItems(labels.toArray(new String[0]),new DialogInterface.OnClickListener(){public void onClick(DialogInterface d,int which){try{a.startActivity(intents.get(which));}catch(Exception e){new AlertDialog.Builder(a).setMessage("Não foi possível abrir: "+e.getClass().getSimpleName()).setPositiveButton("Fechar",null).show();}}}).setNegativeButton("Cancelar",null).show();}
}

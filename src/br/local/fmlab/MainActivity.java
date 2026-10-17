package br.local.fmlab;

import android.Manifest;
import android.app.*;
import android.os.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.*;
import android.widget.*;
import android.text.InputType;
import android.util.Log;
import java.util.*;
import java.util.concurrent.*;

public final class MainActivity extends Activity implements RadioEvents.Listener {
    private static final int BG=0xff09121f,CARD=0xff101d2e,TEXT=0xffe6edf7,MUTED=0xff91a4be,ACCENT=0xff51e2b8;
    private final Handler ui=new Handler(Looper.getMainLooper());
    private final ExecutorService worker=Executors.newSingleThreadExecutor();
    private volatile RadioBridge radio;
    private volatile boolean foreground=false,destroyed=false;
    private boolean bound=false,connecting=false,polling=false,knownOn=false,wantScope=false;
    private TextView status,station,radioText,frequencyLabel,rssiLabel,sinrLabel,qualityLabel,qualityNote,rateLabel,statsLabel,details,scopeLabel,scopeStats;
    private View qualityDot;
    private EditText frequency;
    private SignalPlot plot;
    private ScopeView scopeView;
    private AudioScope scope;
    private Button scopeButton;
    private TextView stereoLabel,scanLabel;
    private volatile Boolean receivedStereo;
    private final BandScanner scanner=new BandScanner();
    private RadioEvents radioEvents;

    private SpectrumView spectrum;
    private SpectrogramView spectrogram;
    private LevelMeterView levelMeters;
    private boolean frozenAudio=false,comparisonB=false;
    private Button freezeButton;
    private TextView comparisonLabel;
    private final ReceptionTrial receptionTrial=new ReceptionTrial();
    private ReceptionTrial.Result reference;
    private int comparisonFrequency;

    private TextView audioAlert;
    private LabFeatures features;
    private AudioAnalysis audioAnalysis;
    private long silentSince=0,clipUntil=0,audioTimestamp=0;
    private ScrollView dashboard;
    private View connectionPanel, signalPanel;
    private Button muteButton, compactButton;
    private final ArrayDeque<String> rdsHistory=new ArrayDeque<String>();
    private String previousRds="";
    private boolean compact;
    private View audioPanel,testsPanel,dataPanel;

    private final StringBuilder log=new StringBuilder("FM Lab 0.9 | GT-I9195 / Android 7.1.2\n");
    private final ArrayDeque<Integer> history=new ArrayDeque<Integer>();
    private int lastRequest=0,generation=0,qualitySource=0,qualityLow=5,qualityHigh=15;
    private long targetMs=100,lastMetadata=0,lastSample=0,lastLog=0,rateWindow=0;
    private int rateIntervals=0;
    private double actualHz=0;
    private Integer lastRssi,lastSinr;
    private SharedPreferences prefs;
    private int dp(int n){return (int)(getResources().getDisplayMetrics().density*n+.5f);}
    private void record(String s){if(log.length()>24000)log.delete(0,12000);log.append(s).append('\n');Log.d("FMLab",s);}
    private void message(String s){set(status,s);record(s);}
    private void set(TextView v,String s){if(!v.getText().toString().equals(s))v.setText(s);}
    private GradientDrawable shape(int color,int radius){GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(dp(radius));return d;}
    private TextView text(String s,int size,int color){TextView t=new TextView(this);t.setText(s);t.setTextSize(size);t.setTextColor(color);t.setPadding(0,dp(4),0,dp(4));return t;}
    private TextView label(LinearLayout p,String s,int size,int color){TextView t=text(s,size,color);p.addView(t);return t;}
    private LinearLayout column(){LinearLayout p=new LinearLayout(this);p.setOrientation(1);return p;}
    private LinearLayout card(LinearLayout p,String caption){LinearLayout c=column();c.setPadding(dp(16),dp(12),dp(16),dp(14));c.setBackground(shape(CARD,18));LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.setMargins(0,0,0,dp(12));p.addView(c,lp);if(caption.length()>0){TextView t=label(c,caption,11,MUTED);t.setLetterSpacing(.12f);}return c;}
    private Button button(LinearLayout p,String name,boolean primary,View.OnClickListener click){Button b=new Button(this);b.setText(name);b.setAllCaps(false);b.setTextSize(13);b.setTextColor(primary?BG:TEXT);b.setBackground(shape(primary?ACCENT:0xff22334b,10));b.setMinHeight(dp(44));b.setPadding(dp(10),dp(6),dp(10),dp(6));b.setOnClickListener(click);LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(p.getOrientation()==0?0:-1,dp(46),p.getOrientation()==0?1:0);lp.setMargins(dp(3),dp(5),dp(3),dp(3));p.addView(b,lp);return b;}
    private LinearLayout row(LinearLayout p){LinearLayout r=new LinearLayout(this);r.setOrientation(0);p.addView(r);return r;}
    public void onCreate(Bundle saved){
        super.onCreate(saved);prefs=getSharedPreferences("fmlab",0);if(prefs.getInt("sinrEncoding",0)!=1)prefs.edit().remove("scanResults").putInt("sinrEncoding",1).apply();qualitySource=prefs.getInt("qualitySource",0);qualityLow=prefs.getInt("qualityLow",5);qualityHigh=prefs.getInt("qualityHigh",15);if(qualityLow>=qualityHigh){qualityLow=5;qualityHigh=15;}
        targetMs=prefs.getLong("interval",100);if(targetMs!=50&&targetMs!=100&&targetMs!=250)targetMs=100;
        getWindow().setStatusBarColor(BG);getWindow().setNavigationBarColor(BG);
        ScrollView scroll=new ScrollView(this);dashboard=scroll;scroll.setFillViewport(true);scroll.setBackgroundColor(BG);LinearLayout body=column();body.setPadding(dp(16),dp(16),dp(16),dp(24));scroll.addView(body);LinearLayout shell=column();shell.setBackgroundColor(BG);shell.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));setContentView(shell);
        LinearLayout header=row(body);TextView brand=text("FM LAB",28,ACCENT);brand.setTypeface(null,Typeface.BOLD);header.addView(brand,new LinearLayout.LayoutParams(0,-2,1));TextView chip=text("LIVE · 0.9",11,MUTED);chip.setGravity(Gravity.RIGHT|Gravity.CENTER_VERTICAL);header.addView(chip);
        label(body,"RECEPÇÃO • EMISSORA • ÁUDIO",10,MUTED).setLetterSpacing(.14f);
        LinearLayout navigation=row(shell);navigation.setPadding(dp(4),dp(4),dp(4),dp(4));navigation.setBackgroundColor(CARD);
        button(navigation,"Rádio",false,new View.OnClickListener(){public void onClick(View v){dashboard.smoothScrollTo(0,0);}});
        button(navigation,"Áudio",false,new View.OnClickListener(){public void onClick(View v){jump(audioPanel);}});
        button(navigation,"Testes",false,new View.OnClickListener(){public void onClick(View v){jump(testsPanel);}});
        button(navigation,"Dados",false,new View.OnClickListener(){public void onClick(View v){jump(dataPanel);}});
        LinearLayout connectionCard=card(body,"");status=label(connectionCard,"Conectando ao serviço do rádio…",13,TEXT);
        LinearLayout controls=row(connectionCard);connectionPanel=controls;button(controls,"Conectar",false,new View.OnClickListener(){public void onClick(View v){connect();}});button(controls,"Ligar no nativo ↗",true,new View.OnClickListener(){public void onClick(View v){openOriginalRadio();}});
        label(connectionCard,"Sem root · o serviço do rádio nativo precisa estar ativo.",11,MUTED);
        LinearLayout stationCard=card(body,"EMISSORA SINTONIZADA");station=label(stationCard,"Aguardando emissora",25,TEXT);station.setTypeface(null,Typeface.BOLD);
        frequencyLabel=label(stationCard,"Frequência atual não informada pelo serviço",12,MUTED);
        radioText=label(stationCard,"O texto RDS aparecerá quando recebido.",14,0xffbecce0);radioText.setMinLines(2);
        stereoLabel=label(stationCard,"Estéreo / mono · aguardando evento do receptor",13,ACCENT);
        button(stationCard,"Modo estéreo / mono",false,new View.OnClickListener(){public void onClick(View v){chooseStereo();}});
        LinearLayout quick=row(stationCard);
        muteButton=button(quick,"Mudo",false,new View.OnClickListener(){public void onClick(View v){toggleMute();}});
        button(quick,"Compartilhar",false,new View.OnClickListener(){public void onClick(View v){shareStation();}});
        LinearLayout stationTools=row(stationCard);
        button(stationTools,"Texto RDS",false,new View.OnClickListener(){public void onClick(View v){showRdsHistory();}});
        button(stationTools,"Recentes",false,new View.OnClickListener(){public void onClick(View v){showRecent();}});
        LinearLayout qualityRow=row(stationCard);qualityRow.setGravity(Gravity.CENTER_VERTICAL);qualityDot=new View(this);LinearLayout.LayoutParams dot=new LinearLayout.LayoutParams(dp(12),dp(12));dot.setMargins(0,0,dp(9),0);qualityRow.addView(qualityDot,dot);qualityLabel=text("Sinal indisponível",15,TEXT);qualityRow.addView(qualityLabel);
        qualityNote=label(stationCard,"Indicador estimado • limites ajustáveis",10,MUTED);qualityDot.setBackground(shape(0xff718096,20));
        LinearLayout signal=card(body,"RECEPÇÃO EM TEMPO REAL");signalPanel=signal;LinearLayout numbers=row(signal);
        LinearLayout rssBox=column(),sinBox=column();numbers.addView(rssBox,new LinearLayout.LayoutParams(0,-2,1));numbers.addView(sinBox,new LinearLayout.LayoutParams(0,-2,1));
        label(rssBox,"INTENSIDADE · RSSI",10,MUTED);rssiLabel=label(rssBox,"—",34,TEXT);label(sinBox,"QUALIDADE · SINR",10,MUTED);sinrLabel=label(sinBox,"—",34,ACCENT);
        label(signal,"Valores brutos do driver · escala ainda não calibrada",10,MUTED);
        plot=new SignalPlot(this);signal.addView(plot,new LinearLayout.LayoutParams(-1,dp(168)));
        statsLabel=label(signal,"Mínimo —   Média —   Máximo —",11,MUTED);rateLabel=label(signal,"Aguardando leituras · gráfico alvo 30 FPS",11,MUTED);
        LinearLayout tuning=card(body,"SINTONIA MANUAL");LinearLayout tuneRow=row(tuning);
        frequency=new EditText(this);frequency.setTextColor(TEXT);frequency.setTextSize(25);frequency.setSingleLine(true);frequency.setInputType(InputType.TYPE_CLASS_NUMBER|InputType.TYPE_NUMBER_FLAG_DECIMAL);frequency.setText(prefs.getString("frequency","98.1"));frequency.setContentDescription("Frequência desejada em MHz");tuneRow.addView(frequency,new LinearLayout.LayoutParams(0,dp(52),1));label(tuneRow,"MHz",15,MUTED);
        LinearLayout steps=row(tuning);button(steps,"− 0,1",false,new View.OnClickListener(){public void onClick(View v){tune(-100);}});button(steps,"Sintonizar",true,new View.OnClickListener(){public void onClick(View v){tune(0);}});button(steps,"+ 0,1",false,new View.OnClickListener(){public void onClick(View v){tune(100);}});
        features=new LabFeatures(this,prefs,new LabFeatures.Host(){
            public int frequency(){return lastRequest;}public boolean isOn(){return knownOn&&lastSample>0&&SystemClock.elapsedRealtime()-lastSample<1500;}
            public String station(){return station.getText().toString();}public int source(){return qualitySource;}public int low(){return qualityLow;}public int high(){return qualityHigh;}
            public void tune(int f){frequency.setText(String.format(Locale.US,"%.1f",f/1000.0));MainActivity.this.tune(0);}public void message(String s){MainActivity.this.message(s);}
        });
        testsPanel=features.addTo(body);
        LinearLayout antennaPanel=card(body,"ASSISTENTE DE ANTENA · COMPARAÇÃO A/B");
        comparisonLabel=label(antennaPanel,"1. Mantenha o cabo parado e meça A.\n2. Mude apenas a posição do cabo e meça B na mesma frequência.",12,TEXT);
        LinearLayout compareActions=row(antennaPanel);
        button(compareActions,"Medir A · 10 s",false,new View.OnClickListener(){public void onClick(View v){startComparison(false);}});
        button(compareActions,"Medir B · 10 s",false,new View.OnClickListener(){public void onClick(View v){startComparison(true);}});
        button(antennaPanel,"Cancelar coleta A/B",false,new View.OnClickListener(){public void onClick(View v){receptionTrial.cancel();set(comparisonLabel,"Coleta cancelada. A referência anterior, se houver, foi mantida.");}});
        label(antennaPanel,"Compara SINR bruto e estabilidade, sem estimar watts ou distância. Não mexa no cabo durante cada coleta. Trocar de sintonia invalida A.",10,MUTED);
        LinearLayout scanPanel=card(body,"BUSCA E COMPARAÇÃO DE EMISSORAS");
        scanLabel=label(scanPanel,"Varra a faixa FM e compare os sinais captados neste local.",12,MUTED);
        LinearLayout scanActions=row(scanPanel);
        button(scanPanel,"Selecionar faixa FM (76–108 / convencional / eFM)",false,new View.OnClickListener(){public void onClick(View v){chooseBand();}});
        button(scanActions,"Varrer FM",true,new View.OnClickListener(){public void onClick(View v){chooseScan();}});
        button(scanActions,"Resultados",false,new View.OnClickListener(){public void onClick(View v){showScan();}});
        button(scanActions,"Cancelar",false,new View.OnClickListener(){public void onClick(View v){scanner.cancel(true);}});
        LinearLayout audio=card(body,"OSCILOSCÓPIO · ÁUDIO DO SINTONIZADOR");audioPanel=audio;scopeLabel=label(audio,"Captura FM desativada",13,TEXT);scopeView=new ScopeView(this);audio.addView(scopeView,new LinearLayout.LayoutParams(-1,dp(165)));scopeStats=label(audio,"Forma de onda real · sem microfone",11,MUTED);
        scopeButton=button(audio,"Ativar captura de áudio FM",false,new View.OnClickListener(){public void onClick(View v){toggleScope();}});
        label(audio,"A captura usa a fonte FM do aparelho. Se a ROM bloquear o acesso, o motivo aparecerá aqui.",11,MUTED);
        label(audio,"NÍVEIS DIGITAIS · RMS300 / PICO · dBFS",11,MUTED);
        levelMeters=new LevelMeterView(this);audio.addView(levelMeters,new LinearLayout.LayoutParams(-1,dp(142)));
        label(audio,"RMS: resposta exponencial de 300 ms. Pico: amostras reais; retenção 1 s e queda 20 dB/s. Não mede true peak entre amostras nem VU analógico calibrado. Se PCM estéreo estiver disponível, L/R são medidos antes da mistura mono.",10,MUTED);
        label(audio,"ESPECTRO DO ÁUDIO · dBFS",11,MUTED);
        spectrum=new SpectrumView(this);audio.addView(spectrum,new LinearLayout.LayoutParams(-1,dp(145)));
        label(audio,"ESPECTROGRAMA · HISTÓRICO DO ÁUDIO",11,MUTED);
        spectrogram=new SpectrogramView(this);audio.addView(spectrogram,new LinearLayout.LayoutParams(-1,dp(175)));
        freezeButton=button(audio,"Congelar osciloscópio / espectros",false,new View.OnClickListener(){public void onClick(View v){if(!scope.isRunning()){message("Ative a captura FM primeiro");return;}frozenAudio=!frozenAudio;scopeView.freeze(frozenAudio);spectrum.freeze(frozenAudio);spectrogram.freeze(frozenAudio);freezeButton.setText(frozenAudio?"Retomar osciloscópio / espectros":"Congelar osciloscópio / espectros");}});
        label(audio,"Últimos 8 s · 32 faixas de áudio · 43 Hz–20 kHz. Congelar pausa os gráficos; não grava nem interrompe o som. Medidores e alertas continuam ao vivo.",10,MUTED);
        audioAlert=label(audio,"Ative a captura para analisar silêncio e picos digitais.",12,MUTED);
        label(audio,"Espectro mono com janela Hann / FFT de 1024 pontos. Mede o áudio recebido; não o espectro da portadora de rádio.",10,MUTED);
        LinearLayout technical=card(body,"DADOS DA ESTAÇÃO E DO RECEPTOR");dataPanel=technical;details=label(technical,"PI / PTY / ECC · aguardando RDS\nAntena / saída / áudio · aguardando serviço",12,0xffbecce0);details.setLineSpacing(dp(4),1);
        label(technical,"PI: identificação RDS · PTY: tipo de programa (código)\nECC: código estendido de país. Dados ausentes aparecem como —.",10,MUTED);
        LinearLayout settings=card(body,"AJUSTES DO PAINEL");
        compactButton=button(settings,"",false,new View.OnClickListener(){public void onClick(View v){compact=!compact;prefs.edit().putBoolean("compact",compact).apply();applyCompact();}});
        button(settings,"Compatibilidade / rádios do aparelho",false,new View.OnClickListener(){public void onClick(View v){DeviceSupport.show(MainActivity.this);}});
        LinearLayout opts=row(settings);button(opts,"Velocidade",false,new View.OnClickListener(){public void onClick(View v){chooseRate();}});button(opts,"Cores do sinal",false,new View.OnClickListener(){public void onClick(View v){configureQuality();}});
        LinearLayout actions=row(settings);button(actions,"Diagnóstico",false,new View.OnClickListener(){public void onClick(View v){ClipboardManager cb=(ClipboardManager)getSystemService(CLIPBOARD_SERVICE);cb.setPrimaryClip(ClipData.newPlainText("FM Lab",log.toString()));Toast.makeText(MainActivity.this,"Diagnóstico copiado",Toast.LENGTH_SHORT).show();}});button(actions,"Desligar FM",false,new View.OnClickListener(){public void onClick(View v){command(RadioBridge.OFF,null,"Desligar FM");}});
        label(body,"GT-I9195 · LineageOS 14.1\nO sintonizador é compartilhado com o rádio nativo. A frequência digitada é uma solicitação; o serviço não informa a frequência atual.",11,MUTED);
        scope=new AudioScope(new AudioScope.Listener(){public void onFrame(short[] samples,int sampleRate,AudioAnalysis analysis){if(!foreground)return;if(analysis.meter!=null)levelMeters.update(analysis.meter);if(!frozenAudio){scopeView.update(samples,sampleRate);spectrum.update(analysis);spectrogram.update(analysis);}audioAnalysis=analysis;long now=SystemClock.elapsedRealtime();audioTimestamp=now;if(analysis.rmsDb < -55){if(silentSince==0)silentSince=now;}else silentSince=0;if(analysis.clippedFraction>.001)clipUntil=now+2000;}
            public void onStatus(String s){if(destroyed)return;set(scopeLabel,s);record("Áudio: "+s);}
            public void onStopped(){if(destroyed)return;wantScope=false;scopeButton.setText("Ativar captura de áudio FM");scopeView.clear();spectrum.clear();clearAudioHistory();audioAnalysis=null;audioTimestamp=0;set(audioAlert,"Captura indisponível · veja o motivo acima");}});
        compact=prefs.getBoolean("compact",false);applyCompact();
        record("Dispositivo: "+Build.MODEL+" Android "+Build.VERSION.RELEASE);updateQuality();
    }
    private void clearAudioHistory(){if(levelMeters!=null)levelMeters.clear();frozenAudio=false;if(scopeView!=null)scopeView.freeze(false);if(spectrum!=null)spectrum.freeze(false);if(spectrogram!=null)spectrogram.clear();if(freezeButton!=null)freezeButton.setText("Congelar osciloscópio / espectros");}
    private void invalidateComparison(){boolean had=receptionTrial.active()||reference!=null;receptionTrial.cancel();reference=null;comparisonFrequency=0;if(had&&comparisonLabel!=null)set(comparisonLabel,"Comparação invalidada por mudança de sintonia, pausa ou estado. Meça A novamente.");}
    private void startComparison(boolean b){if(scanner.isRunning()||receptionTrial.active()){message("Aguarde ou cancele a coleta/varredura atual");return;}if(!knownOn||lastRequest==0||lastSample==0||SystemClock.elapsedRealtime()-lastSample>1500||lastSinr==null){message("Sintonize pelo FM Lab e aguarde leituras válidas de SINR");return;}if(b&&(reference==null||comparisonFrequency!=lastRequest)){message("Meça A primeiro na mesma frequência");return;}comparisonB=b;comparisonFrequency=lastRequest;receptionTrial.start(SystemClock.elapsedRealtime());set(comparisonLabel,"Medindo "+(b?"B":"A")+" · mantenha a posição por 10 s…");}
    private void updateComparison(){if(!receptionTrial.active())return;long now=SystemClock.elapsedRealtime();if(receptionTrial.remaining(now)>0){set(comparisonLabel,"Medindo "+(comparisonB?"B":"A")+" · faltam "+((receptionTrial.remaining(now)+999)/1000)+" s");return;}ReceptionTrial.Result result=receptionTrial.finish(now);if(result==null){set(comparisonLabel,"Coleta rejeitada: faltaram leituras recentes ou cobertura suficiente. Repita com o FM conectado.");return;}if(!comparisonB){reference=result;set(comparisonLabel,String.format(Locale.US,"A salva · SINR médio %.2f · desvio %.2f · %d leituras.\nAgora mude a posição e meça B.",result.mean,result.sd,result.count));return;}double delta=result.mean-reference.mean;set(comparisonLabel,String.format(Locale.US,"%.1f MHz · A/B na mesma sintonia solicitada\nSINR médio A %.2f → B %.2f · diferença %+.2f unidades brutas\nDesvio A %.2f → B %.2f (menor = menos variação)\nLeituras A %d / B %d. Repita para confirmar; o sinal varia com o tempo.",comparisonFrequency/1000.0,reference.mean,result.mean,delta,reference.sd,result.sd,reference.count,result.count));}
    private void applyCompact(){connectionPanel.setVisibility(compact?View.GONE:View.VISIBLE);signalPanel.setVisibility(compact?View.GONE:View.VISIBLE);compactButton.setText(compact?"Modo compacto ativo · mostrar painel completo":"Ativar modo compacto");}
    private void toggleMute(){final RadioBridge r=radio;if(r==null||!knownOn){message("Ligue e conecte o FM primeiro");return;}if(scanner.isRunning()){message("Aguarde o fim da varredura");return;}worker.execute(new Runnable(){public void run(){try{boolean muted=r.number(RadioBridge.MUTED,null)!=0;final boolean ok=r.number(muted?13:11,null)!=0;ui.post(new Runnable(){public void run(){if(!destroyed){lastMetadata=0;message(ok?"Solicitação de áudio enviada":"O serviço recusou a mudança de áudio");}}});}catch(Exception e){failure(e);}}});}
    private void rememberFrequency(int f){try{org.json.JSONArray old=new org.json.JSONArray(prefs.getString("recentTunes","[]")),next=new org.json.JSONArray();next.put(f);for(int i=0;i<old.length()&&next.length()<12;i++)if(old.getInt(i)!=f)next.put(old.getInt(i));prefs.edit().putString("recentTunes",next.toString()).apply();}catch(Exception e){record("Histórico: "+e.getMessage());}}
    private void showRecent(){try{final org.json.JSONArray a=new org.json.JSONArray(prefs.getString("recentTunes","[]"));if(a.length()==0){message("Sintonize pelo FM Lab para preencher os recentes");return;}String[] labels=new String[a.length()];for(int i=0;i<a.length();i++)labels[i]=String.format(Locale.US,"%.1f MHz",a.getInt(i)/1000.0);new AlertDialog.Builder(this).setTitle("Últimas sintonias solicitadas").setItems(labels,new DialogInterface.OnClickListener(){public void onClick(DialogInterface d,int i){frequency.setText(String.format(Locale.US,"%.1f",a.optInt(i)/1000.0));tune(0);}}).setNegativeButton("Fechar",null).show();}catch(Exception e){message("Histórico indisponível");}}
    private void rememberRds(String value){if(value.length()==0||value.equals(previousRds))return;previousRds=value;String when=new java.text.SimpleDateFormat("HH:mm:ss",Locale.getDefault()).format(new Date());rdsHistory.addFirst(when+" · "+(lastRequest>0?String.format(Locale.US,"%.1f MHz",lastRequest/1000.0):"frequência não informada")+"\n"+value);while(rdsHistory.size()>30)rdsHistory.removeLast();}
    private void showRdsHistory(){StringBuilder b=new StringBuilder();for(String entry:rdsHistory)b.append(entry).append("\n\n");new AlertDialog.Builder(this).setTitle("Texto RDS · esta sessão").setMessage(b.length()==0?"Ainda não foi recebido texto RDS. A emissora precisa transmitir esse dado.":b.toString()).setPositiveButton("Fechar",null).setNeutralButton("Limpar",new DialogInterface.OnClickListener(){public void onClick(DialogInterface d,int w){rdsHistory.clear();previousRds="";}}).show();}
    private void shareStation(){String f=lastRequest>0?String.format(Locale.US,"%.1f MHz (solicitada)",lastRequest/1000.0):"Frequência não informada";boolean fresh=knownOn&&lastSample>0&&SystemClock.elapsedRealtime()-lastSample<1500;String report="FM Lab · "+f+"\n"+station.getText()+"\n"+radioText.getText()+"\n"+stereoLabel.getText()+"\n"+(fresh?"RSSI bruto: "+shown(lastRssi)+" · SINR bruto: "+shown(lastSinr):"Métricas atuais indisponíveis")+"\n"+new java.text.SimpleDateFormat("dd/MM/yyyy HH:mm:ss",Locale.getDefault()).format(new Date());Intent i=new Intent(Intent.ACTION_SEND);i.setType("text/plain");i.putExtra(Intent.EXTRA_TEXT,report);try{startActivity(Intent.createChooser(i,"Compartilhar recepção"));}catch(ActivityNotFoundException e){message("Nenhum aplicativo disponível para compartilhar");}}
    private void jump(View v){if(v!=null)dashboard.smoothScrollTo(0,Math.max(0,v.getTop()-dp(8)));}
    private final ServiceConnection connection=new ServiceConnection(){
        public void onServiceConnected(ComponentName n,final IBinder b){if(destroyed)return;connecting=false;worker.execute(new Runnable(){public void run(){try{final RadioBridge next=new CafRadioBackend(b);radioEvents=new RadioEvents(MainActivity.this);try{next.registerEvents(radioEvents);}catch(Exception ignored){}ui.post(new Runnable(){public void run(){if(destroyed)return;radio=next;message("Serviço conectado · aguardando estado do FM");lastMetadata=0;if(foreground)pollNow();}});}catch(Exception e){failure(e);}}});}
        public void onServiceDisconnected(ComponentName n){radio=null;connecting=false;knownOn=false;resetSignal();stopScope();message("Serviço desconectado · reconecte ou abra o rádio nativo");}
    };
    private void connect(){if(destroyed||connecting)return;if(radio!=null){message("Serviço conectado · consultas ativas");return;}if(bound){unbindService(connection);bound=false;}
        try{android.content.pm.PackageInfo p=getPackageManager().getPackageInfo("com.caf.fmradio",0);record("Rádio: "+p.versionName+" / "+p.versionCode);
            if(!BackendRegistry.supported(p.packageName,p.versionCode,p.versionName)){message("Rádio detectado sem adaptador compatível · veja Compatibilidade");return;}
            Intent i=new Intent();i.setComponent(new ComponentName(p.packageName,BackendRegistry.service(p.packageName,p.versionCode,p.versionName)));connecting=true;bound=bindService(i,connection,BIND_AUTO_CREATE);
            if(!bound){connecting=false;message("Serviço ausente · abra o rádio nativo");}else{message("Conectando ao rádio nativo…");ui.postDelayed(new Runnable(){public void run(){if(!destroyed&&connecting){connecting=false;message("O serviço não respondeu · tente abrir o rádio nativo");}}},8000);}
        }catch(Exception e){connecting=false;message("Conexão indisponível · veja Compatibilidade nos ajustes. "+e.getClass().getSimpleName());record(Log.getStackTraceString(e));}}
    private void openOriginalRadio(){try{Intent i=getPackageManager().getLaunchIntentForPackage("com.caf.fmradio");if(i==null){DeviceSupport.launch(this);return;}features.interrupt("rádio nativo aberto");generation++;lastRequest=0;resetSignal();clearStation();set(frequencyLabel,"Frequência atual não informada pelo serviço");message("Ligue o rádio nativo e volte ao FM Lab");startActivity(i);}catch(Exception e){message("Não foi possível abrir: "+e.getMessage());}}
    private void tune(int step){try{double mhz=Double.parseDouble(frequency.getText().toString().replace(',','.'));if(Double.isNaN(mhz)||Double.isInfinite(mhz))throw new Exception();int khz=(int)Math.round(mhz*1000)+step;if(!BandPlan.valid(khz))throw new Exception();frequency.setText(String.format(Locale.US,"%.1f",khz/1000.0));command(RadioBridge.TUNE,khz,"Sintonia");}catch(Exception e){message("Use 76,0 a 108,0 MHz, em passos de 0,1");}}
    private void command(final int code,final Integer arg,final String name){if(scanner.isRunning()){message("Cancele a varredura antes de controlar a sintonia");return;}if(code==RadioBridge.ON){openOriginalRadio();return;}final RadioBridge r=radio;if(r==null){message("Conecte ao rádio primeiro");return;}worker.execute(new Runnable(){public void run(){try{
        if(code==RadioBridge.TUNE&&r.number(RadioBridge.IS_ON,null)==0){ui.post(new Runnable(){public void run(){message("Ligue o FM pelo rádio nativo antes de sintonizar");}});return;}
        if(code==RadioBridge.TUNE)receivedStereo=null;final boolean ok=r.number(code,arg)!=0;ui.post(new Runnable(){public void run(){if(destroyed)return;message(name+(ok?" · comando aceito":code==RadioBridge.TUNE&&arg!=null&&arg<87500?" · frequência estendida recusada pelo serviço; confira a região/faixa no rádio nativo":" · comando recusado"));if(ok){features.interrupt("sintonia / estado alterado");generation++;resetSignal();clearStation();lastMetadata=0;if(code==RadioBridge.TUNE){lastRequest=arg;set(frequencyLabel,String.format(Locale.US,"Solicitada: %.1f MHz · não confirmada pelo serviço",arg/1000.0));prefs.edit().putString("frequency",frequency.getText().toString()).apply();rememberFrequency(arg);}else if(code==RadioBridge.OFF){knownOn=false;stopScope();}}}});
        }catch(final Exception e){ui.post(new Runnable(){public void run(){if(!destroyed){record(Log.getStackTraceString(e));message(name+": "+e.getClass().getSimpleName()+" · "+e.getMessage());}}});}}});}
    private static String shown(Integer n){return n==null?"—":n.toString();}
    private Integer read(RadioBridge r,int code){try{int v=r.number(code,null);return code==RadioBridge.SINR?RadioBridge.sinrValue(v):RadioBridge.valid(v)?v:null;}catch(Exception e){return null;}}
    private String readText(RadioBridge r,int code){try{return r.text(code);}catch(Exception e){return "";}}
    private String bool(Integer v,String yes,String no){return v==null?"—":v!=0?yes:no;}
    private String hex(Integer v){return v==null||v==0?"—":String.format(Locale.US,"0x%04X",v);}
    private final Runnable ticker=new Runnable(){public void run(){pollNow();}};
    private void pollNow(){ui.removeCallbacks(ticker);final RadioBridge r=radio;if(!foreground||destroyed||r==null||polling||scanner.isRunning())return;polling=true;final int epoch=generation;final boolean meta=SystemClock.elapsedRealtime()-lastMetadata>=1000;
        worker.execute(new Runnable(){public void run(){final long started=SystemClock.elapsedRealtime();try{
            final boolean on=r.number(RadioBridge.IS_ON,null)!=0;final Integer rss=on?read(r,RadioBridge.RSSI):null,sin=on?read(r,RadioBridge.SINR):null;
            String stationName="",rt="",tech="";boolean antenna=false;
            if(meta){
                antenna=r.number(RadioBridge.ANTENNA,null)!=0;
                if(on){stationName=readText(r,RadioBridge.PS);rt=readText(r,RadioBridge.RT);
                    Integer pi=read(r,RadioBridge.PI),pty=read(r,RadioBridge.PTY),ecc=read(r,RadioBridge.ECC),speaker=read(r,RadioBridge.SPEAKER),mute=read(r,RadioBridge.MUTED),analog=read(r,RadioBridge.ANALOG);
                    tech="PI  "+hex(pi)+"     PTY  "+shown(pty)+"     ECC  "+hex(ecc)+"\nAntena  "+(antenna?"conectada":"não detectada")+"\nSaída  "+bool(speaker,"alto-falante","fone / outra rota")+"     Mudo  "+bool(mute,"sim","não")+"\nModo  "+bool(analog,"analógico","digital")+"\nIoC  "+shown(read(r,RadioBridge.IOC))+"     MPX DC  "+shown(read(r,RadioBridge.MPX))+"\nDetector de interferência  "+shown(read(r,RadioBridge.INTERFERENCE));
                }else tech="FM desligado\nAntena  "+(antenna?"conectada":"não detectada")+"\nPI / PTY / ECC / IoC / MPX · —";
            }
            final Integer muted=on?read(r,RadioBridge.MUTED):null;final String ps=stationName,radioTxt=rt,technical=tech;final boolean ant=antenna;final long ended=SystemClock.elapsedRealtime();
            ui.post(new Runnable(){public void run(){polling=false;if(destroyed)return;if(foreground&&radio==r&&generation==epoch&&!scanner.isRunning()){
                knownOn=on;if(meta)muteButton.setText(muted==null?"Mudo · ?":muted!=0?"Ouvir":"Mudo");lastRssi=rss;lastSinr=sin;set(rssiLabel,shown(rss));set(sinrLabel,shown(sin));
                if(rateWindow==0){rateWindow=ended;rateIntervals=0;}else rateIntervals++;
                if(ended-rateWindow>=1000){actualHz=SignalQuality.rateHz(rateIntervals,ended-rateWindow);rateWindow=ended;rateIntervals=0;}lastSample=ended;features.sample(rss,sin,ended,on);if(on)receptionTrial.sample(sin,ended);
                if(rss!=null){plot.add(rss,ended);history.add(rss);if(history.size()>100)history.removeFirst();updateStats();}
                else{plot.clear();history.clear();set(statsLabel,"Mínimo —   Média —   Máximo —");}
                set(rateLabel,String.format(Locale.US,"Leituras: %.1f Hz · alvo %d Hz · gráfico alvo 30 FPS",actualHz,1000/targetMs));updateQuality();
                if(meta){rememberRds(radioTxt);lastMetadata=ended;set(station,!on?"Rádio desligado":ps.length()>0?ps:"Sem identificação RDS");set(radioText,!on?"Ligue o FM no rádio nativo para receber a emissora.":radioTxt.length()>0?radioTxt:"Aguardando texto RDS da emissora…");set(details,technical);updateStereoLabel();set(status,!on?"Rádio nativo desligado · toque em Ligar no nativo ↗":!ant?"FM ativo · antena não detectada":"FM ativo · conectado ao serviço nativo");}
                if(!on){invalidateComparison();stopScope();plot.clear();}else if(wantScope&&!scope.isRunning())scope.start();
                if(ended-lastLog>=1000){lastLog=ended;record("FM="+on+" RSSI="+rss+" SINR="+sin+" Hz="+String.format(Locale.US,"%.1f",actualHz)+" solicitado_kHz="+lastRequest);}
            }
            if(foreground&&radio!=null)ui.postDelayed(ticker,SignalQuality.delay(targetMs,ended-started));}});
        }catch(Exception e){ui.post(new Runnable(){public void run(){polling=false;}});failure(e);}}});
    }

    public void stereoChanged(final boolean stereo){receivedStereo=stereo;ui.post(new Runnable(){public void run(){if(!destroyed&&foreground)updateStereoLabel();}});}
    private void updateStereoLabel(){if(stereoLabel!=null)set(stereoLabel,receivedStereo==null?"Estéreo / mono · aguardando evento do receptor":receivedStereo?"FM ESTÉREO · confirmado pelo receptor":"FM MONO · informado pelo receptor");}
    private void chooseStereo(){if(radio==null||!knownOn){message("Conecte e ligue o FM primeiro");return;}if(scanner.isRunning()){message("Aguarde o fim da varredura");return;}new AlertDialog.Builder(this).setTitle("Modo de recepção").setItems(new String[]{"Permitir estéreo automático","Forçar mono · pode reduzir ruído"},new DialogInterface.OnClickListener(){public void onClick(DialogInterface d,int which){final int mode=which==0?1:0;final RadioBridge r=radio;worker.execute(new Runnable(){public void run(){try{receivedStereo=null;final boolean ok=r.number(32,mode)!=0;ui.post(new Runnable(){public void run(){if(destroyed)return;updateStereoLabel();message(ok?"Modo solicitado · aguarde a confirmação do receptor":"O receptor recusou a mudança de modo");}});}catch(Exception e){failure(e);}}});}}).show();}
    private void chooseBand(){new AlertDialog.Builder(this).setTitle("Faixa solicitada ao receptor").setSingleChoiceItems(new String[]{BandPlan.label(0),BandPlan.label(1),BandPlan.label(2)},prefs.getInt("bandMode",0),new DialogInterface.OnClickListener(){public void onClick(DialogInterface d,int which){if(scanner.isRunning()){message("Aguarde o fim da varredura");return;}prefs.edit().putInt("bandMode",which).apply();message(BandPlan.label(which)+" · o serviço nativo precisa aceitar a faixa");d.dismiss();}}).setNegativeButton("Fechar",null).show();}
    private void chooseScan(){if(radio==null||!knownOn){message("Ligue e conecte o rádio antes da varredura");return;}if(scanner.isRunning()){message("Varredura já em andamento");return;}new AlertDialog.Builder(this).setTitle("Procurar sinais FM neste local").setMessage("Faixa: "+BandPlan.label(prefs.getInt("bandMode",0))+". O áudio será silenciado durante a busca. Ao terminar, a frequência solicitada será restaurada. Mantenha o telefone e o cabo de fone parados.").setPositiveButton("Rápida · passo 0,2 MHz",new DialogInterface.OnClickListener(){public void onClick(DialogInterface d,int w){startScan(200);}}).setNeutralButton("Completa · passo 0,1 MHz",new DialogInterface.OnClickListener(){public void onClick(DialogInterface d,int w){startScan(100);}}).setNegativeButton("Cancelar",null).show();}
    private void startScan(int step){final RadioBridge r=radio;if(r==null)return;int value=lastRequest;try{if(value==0)value=(int)Math.round(Double.parseDouble(frequency.getText().toString().replace(',','.'))*1000);}catch(Exception ignored){}if(!BandPlan.valid(value)){message("Informe uma frequência válida para restaurar após a busca");return;}final int restore=value;
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        features.interrupt("varredura iniciada");stopScope();generation++;resetSignal();clearStation();scanner.start(r,restore,step,BandPlan.lower(prefs.getInt("bandMode",0),step),BandPlan.upper(prefs.getInt("bandMode",0)),new BandScanner.Listener(){
        public void tuning(final int f){receivedStereo=null;ui.post(new Runnable(){public void run(){if(!destroyed){set(frequencyLabel,String.format(Locale.US,"Varredura · solicitada %.1f MHz",f/1000.0));updateStereoLabel();}}});}
        public Boolean stereo(){return receivedStereo;}
        public void progress(final String text){ui.post(new Runnable(){public void run(){if(!destroyed)set(scanLabel,text);}});}
        public void finished(final List<BandScanner.Result> result,final String error,final boolean cancelled,final boolean restored){ui.post(new Runnable(){public void run(){if(!prefs.getBoolean("keepScreenOn",false))getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);if(destroyed)return;try{org.json.JSONArray array=new org.json.JSONArray();for(BandScanner.Result item:result){org.json.JSONObject j=new org.json.JSONObject();j.put("frequency",item.frequency);j.put("sinr",item.sn==0?org.json.JSONObject.NULL:item.sinrMean());j.put("rssi",item.rn==0?org.json.JSONObject.NULL:item.rssiMean());j.put("stereo",item.stereo==null?org.json.JSONObject.NULL:item.stereo);j.put("sinr_sd",item.sn==0?org.json.JSONObject.NULL:item.sinrSd());j.put("samples",item.sn);j.put("name",item.name);j.put("text",item.text);array.put(j);Log.d("FMLabScan",j.toString());}prefs.edit().putString("scanResults",array.toString()).putBoolean("scanPartial",cancelled||error.length()>0).apply();}catch(Exception e){record("Falha ao salvar varredura: "+e.getMessage());}
        lastRequest=restored?restore:0;set(frequencyLabel,restored?String.format(Locale.US,"Restaurada: %.1f MHz · comando aceito",restore/1000.0):"Frequência atual não confirmada");lastMetadata=0;generation++;resetSignal();set(scanLabel,(cancelled?"Busca cancelada · resultados parciais":error.length()>0?error:"Busca concluída · "+result.size()+" candidatos comparados")+(restored?" · sintonia restaurada":""));if(foreground){pollNow();showScan();}}});}
        });}
    private void showScan(){try{final org.json.JSONArray a=new org.json.JSONArray(prefs.getString("scanResults","[]"));if(a.length()==0){message("Nenhum candidato salvo · execute uma varredura");return;}String[] labels=new String[a.length()];for(int i=0;i<a.length();i++){org.json.JSONObject j=a.getJSONObject(i);String name=j.optString("name");labels[i]=(i+1)+". "+String.format(Locale.US,"%.1f MHz",j.getInt("frequency")/1000.0)+(name.length()>0?" · "+name:"")+"\nSINR "+(j.isNull("sinr")?"—":String.format(Locale.US,"%.1f",j.getDouble("sinr")))+" · RSSI "+(j.isNull("rssi")?"—":String.format(Locale.US,"%.1f",j.getDouble("rssi")))+" · "+(j.isNull("stereo")?"estéreo não informado":j.getBoolean("stereo")?"estéreo":"mono");}
        new AlertDialog.Builder(this).setTitle(prefs.getBoolean("scanPartial",false)?"Sinais FM · busca parcial":"Melhores sinais FM neste local").setItems(labels,new DialogInterface.OnClickListener(){public void onClick(DialogInterface d,int which){org.json.JSONObject j=a.optJSONObject(which);frequency.setText(String.format(Locale.US,"%.1f",j.optInt("frequency")/1000.0));tune(0);}}).setNegativeButton("Fechar",null).show();
        }catch(Exception e){message("Resultados indisponíveis: "+e.getMessage());}}

    private void updateStats(){if(history.isEmpty())return;int lo=Integer.MAX_VALUE,hi=Integer.MIN_VALUE;long sum=0;for(int v:history){lo=Math.min(lo,v);hi=Math.max(hi,v);sum+=v;}set(statsLabel,String.format(Locale.US,"Mín %d   Média %.1f   Máx %d · últimas %d leituras",lo,sum/(double)history.size(),hi,history.size()));}
    private void resetSignal(){invalidateComparison();lastSample=0;rateWindow=0;rateIntervals=0;actualHz=0;lastRssi=null;lastSinr=null;history.clear();plot.clear();set(rssiLabel,"—");set(sinrLabel,"—");set(statsLabel,"Mínimo —   Média —   Máximo —");set(rateLabel,"Aguardando leituras · gráfico alvo 30 FPS");updateQuality();}
    private void clearStation(){clearAudioHistory();previousRds="";set(station,"Aguardando emissora");set(radioText,"Aguardando novos dados RDS");set(details,"PI / PTY / ECC / receptor · aguardando atualização");}
    private void updateQuality(){Integer value=qualitySource==0?lastSinr:lastRssi;boolean fresh=lastSample>0&&SystemClock.elapsedRealtime()-lastSample<1500;int q=knownOn&&fresh?SignalQuality.classify(value,qualityLow,qualityHigh):-1;int color=q==0?0xffff6b7a:q==1?0xffffc568:q==2?ACCENT:0xff718096;qualityDot.setBackground(shape(color,20));set(qualityLabel,q==0?"Sinal ruim · estimativa":q==1?"Sinal regular · estimativa":q==2?"Sinal bom · estimativa":"Sinal indisponível");qualityDot.setContentDescription(qualityLabel.getText());set(qualityNote,(qualitySource==0?"SINR":"RSSI")+" bruto · regular ≥ "+qualityLow+" · bom ≥ "+qualityHigh+" · ajustável");}
    private void chooseRate(){new AlertDialog.Builder(this).setTitle("Velocidade das leituras").setSingleChoiceItems(new String[]{"4 Hz · menor uso do serviço","10 Hz · padrão","20 Hz · rápido / experimental"},targetMs==250?0:targetMs==100?1:2,new DialogInterface.OnClickListener(){public void onClick(DialogInterface d,int which){targetMs=which==0?250:which==1?100:50;prefs.edit().putLong("interval",targetMs).apply();rateWindow=0;rateIntervals=0;actualHz=0;d.dismiss();}}).setNegativeButton("Fechar",null).show();}
    private void configureQuality(){final LinearLayout box=column();box.setPadding(dp(20),dp(8),dp(20),0);label(box,"Cores estimadas com valores brutos. Ajuste comparando uma estação com ruído e outra limpa. Esses limites só alteram o painel.",12,TEXT);final Spinner metric=new Spinner(this);metric.setAdapter(new ArrayAdapter<String>(this,android.R.layout.simple_spinner_dropdown_item,new String[]{"SINR","RSSI"}));metric.setSelection(qualitySource);box.addView(metric);final EditText low=new EditText(this),high=new EditText(this);low.setInputType(4098);high.setInputType(4098);low.setText(""+qualityLow);high.setText(""+qualityHigh);label(box,"Regular a partir de",12,TEXT);box.addView(low);label(box,"Bom a partir de",12,TEXT);box.addView(high);
        final AlertDialog dialog=new AlertDialog.Builder(this).setTitle("Indicador de recepção").setView(box).setPositiveButton("Salvar",null).setNegativeButton("Cancelar",null).create();dialog.setOnShowListener(new DialogInterface.OnShowListener(){public void onShow(DialogInterface d){dialog.getButton(-1).setOnClickListener(new View.OnClickListener(){public void onClick(View v){try{int a=Integer.parseInt(low.getText().toString()),b=Integer.parseInt(high.getText().toString());if(a>=b)throw new Exception();qualitySource=metric.getSelectedItemPosition();qualityLow=a;qualityHigh=b;prefs.edit().putInt("qualitySource",qualitySource).putInt("qualityLow",a).putInt("qualityHigh",b).apply();updateQuality();dialog.dismiss();}catch(Exception e){low.setError("Use números inteiros; regular deve ser menor que bom");}}});}});dialog.show();}
    private void toggleScope(){if(wantScope||scope.isRunning()){stopScope();return;}if(!knownOn){message("Ligue o FM no nativo antes de capturar o áudio");return;}if(Build.VERSION.SDK_INT>=23&&checkSelfPermission(Manifest.permission.RECORD_AUDIO)!=PackageManager.PERMISSION_GRANTED){requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO},41);return;}wantScope=true;scopeButton.setText("Parar captura FM");scope.start();}
    private void stopScope(){wantScope=false;if(scope!=null)scope.stop();if(scopeButton!=null)scopeButton.setText("Ativar captura de áudio FM");if(scopeView!=null)scopeView.clear();if(spectrum!=null)spectrum.clear();clearAudioHistory();audioAnalysis=null;silentSince=0;clipUntil=0;audioTimestamp=0;if(audioAlert!=null)set(audioAlert,"Captura parada · sem análise de áudio");}
    public void onRequestPermissionsResult(int request,String[] permissions,int[] results){super.onRequestPermissionsResult(request,permissions,results);if(request==41){if(results.length>0&&results[0]==PackageManager.PERMISSION_GRANTED)toggleScope();else set(scopeLabel,"Permissão de áudio negada · captura desativada");}}
    private final Runnable watchdog=new Runnable(){public void run(){if(!foreground||destroyed)return;if(lastSample>0&&SystemClock.elapsedRealtime()-lastSample>1500){updateQuality();set(rateLabel,"Dados atrasados · aguardando resposta do serviço");set(rssiLabel,"—");set(sinrLabel,"—");}if(features!=null)features.tick(SystemClock.elapsedRealtime());updateComparison();
        if(scopeView!=null&&scope.isRunning()){set(scopeStats,scopeView.summary());long now=SystemClock.elapsedRealtime();if(audioAnalysis==null||now-audioTimestamp>800){set(audioAlert,"Aguardando amostras recentes do áudio FM");if(!frozenAudio)spectrum.clear();}else if(now<clipUntil){set(audioAlert,"Picos próximos do limite digital · possível saturação");audioAlert.setTextColor(0xffff6b7a);}else if(silentSince>0&&now-silentSince>=2000){set(audioAlert,"Áudio muito baixo por 2 s ou mais · abaixo de −55 dBFS");audioAlert.setTextColor(0xffffc568);}else{set(audioAlert,String.format(Locale.US,"Áudio presente · pico espectral %.0f Hz",audioAnalysis.dominantHz));audioAlert.setTextColor(ACCENT);}}ui.postDelayed(this,500);}};
    private void failure(final Exception e){ui.post(new Runnable(){public void run(){if(destroyed)return;record(Log.getStackTraceString(e));radio=null;knownOn=false;resetSignal();stopScope();message("Serviço: "+e.getClass().getSimpleName()+" · "+e.getMessage());}});}
    protected void onResume(){super.onResume();foreground=true;if(radio!=null){final RadioBridge existing=radio;radioEvents=new RadioEvents(this);receivedStereo=null;worker.execute(new Runnable(){public void run(){try{existing.registerEvents(radioEvents);}catch(Exception ignored){}}});}if(plot!=null)plot.setRunning(true);ui.post(watchdog);if(radio==null)connect();else pollNow();}
    protected void onPause(){scanner.cancel(!((PowerManager)getSystemService(POWER_SERVICE)).isInteractive());foreground=false;invalidateComparison();ui.removeCallbacks(ticker);ui.removeCallbacks(watchdog);plot.setRunning(false);if(features!=null)features.pause();stopScope();super.onPause();}
    protected void onActivityResult(int request,int result,Intent data){super.onActivityResult(request,result,data);if(features!=null)features.onResult(request,result,data);}
    protected void onDestroy(){scanner.cancel(false);destroyed=true;foreground=false;radio=null;ui.removeCallbacksAndMessages(null);scope.stop();if(bound)unbindService(connection);worker.shutdownNow();super.onDestroy();}
}

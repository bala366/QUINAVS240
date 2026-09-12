package com.quina.campeao40;

import android.app.Activity;
import android.os.Bundle;
import android.os.Build;
import android.os.Environment;
import android.content.*;
import android.content.res.ColorStateList;
import android.graphics.*;
import android.graphics.drawable.GradientDrawable;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.provider.MediaStore;
import android.text.InputType;
import android.view.*;
import android.widget.*;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class MainActivity extends Activity {
    private static final int PICK_TXT=1001;
    private static final int PURPLE=Color.rgb(82,28,145);
    private static final int PURPLE_DARK=Color.rgb(52,12,98);
    private static final int PURPLE_LIGHT=Color.rgb(247,242,252);
    private static final int GREEN=Color.rgb(37,132,72);
    private static final int RED=Color.rgb(194,47,47);
    private static final int LAVENDER=Color.rgb(232,220,244);
    private static final int SOFT_RED=Color.rgb(249,220,220);

    private Button btnLoad,btnChampionFixed,btnDelayedFixed,btnChampionAlt,btnDelayedAlt,btnPdf;
    private TextView status,championView,delayedView,fixedView,currentFilter,progressText,detail,output;
    private ProgressBar progress;
    private EditText edRep,edEven,edPrime,edFib,edLow,edSumMin,edSumMax;

    private MotorCore.BaseData base;
    private MotorCore.Group40Result championGroup;
    private MotorCore.Group40Result delayedGroup;
    private MotorCore.FilterConfig fixedFilter;
    private MotorCore.GameResult championGame;
    private MotorCore.GameResult delayedGame;
    private long startMs;

    @Override public void onCreate(Bundle b){super.onCreate(b);buildUi();}
    private int dp(int v){return(int)(v*getResources().getDisplayMetrics().density+.5f);}
    private TextView text(String s,float sp){TextView t=new TextView(this);t.setText(s);t.setTextSize(sp);t.setTextColor(Color.rgb(35,25,42));return t;}
    private GradientDrawable round(int fill,int stroke,int radius){GradientDrawable g=new GradientDrawable();g.setColor(fill);g.setCornerRadius(dp(radius));if(stroke!=0)g.setStroke(dp(1),stroke);return g;}
    private Button button(String s){Button b=new Button(this);b.setText(s);b.setAllCaps(false);b.setTextSize(14);b.setTextColor(Color.WHITE);b.setTypeface(Typeface.DEFAULT_BOLD);b.setBackgroundResource(R.drawable.bg_button);LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,dp(58));lp.setMargins(0,dp(5),0,dp(5));b.setLayoutParams(lp);return b;}
    private LinearLayout card(){LinearLayout c=new LinearLayout(this);c.setOrientation(LinearLayout.VERTICAL);c.setPadding(dp(14),dp(14),dp(14),dp(14));c.setBackground(round(Color.WHITE,Color.rgb(220,202,235),16));LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.setMargins(0,dp(8),0,dp(8));c.setLayoutParams(lp);return c;}
    private EditText field(String hint,String value){EditText e=new EditText(this);e.setHint(hint);e.setText(value);e.setTextSize(14);e.setSingleLine(true);e.setInputType(InputType.TYPE_CLASS_TEXT);e.setPadding(dp(10),dp(7),dp(10),dp(7));e.setBackground(round(Color.WHITE,Color.rgb(190,160,215),10));LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,dp(50),1f);lp.setMargins(dp(3),dp(4),dp(3),dp(4));e.setLayoutParams(lp);return e;}
    private LinearLayout row(View a,View b){LinearLayout r=new LinearLayout(this);r.setOrientation(LinearLayout.HORIZONTAL);r.addView(a);r.addView(b);return r;}

    private void buildUi(){
        ScrollView scroll=new ScrollView(this);scroll.setFillViewport(true);
        LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setBackgroundColor(PURPLE_LIGHT);scroll.addView(root);

        FrameLayout hero=new FrameLayout(this);hero.setBackgroundColor(PURPLE);root.addView(hero,new LinearLayout.LayoutParams(-1,dp(220)));
        ImageView clover=new ImageView(this);clover.setImageResource(R.drawable.ic_clover_white);clover.setAlpha(.12f);clover.setScaleType(ImageView.ScaleType.CENTER_INSIDE);hero.addView(clover,new FrameLayout.LayoutParams(dp(190),dp(190),Gravity.CENTER));
        TextView title=text("QUINA CAMPEÃ\nGRUPO FIXO DE 40",28);title.setTextColor(Color.WHITE);title.setTypeface(Typeface.DEFAULT_BOLD);title.setGravity(Gravity.CENTER);title.setShadowLayer(3,0,2,PURPLE_DARK);hero.addView(title,new FrameLayout.LayoutParams(-1,-1));

        LinearLayout body=new LinearLayout(this);body.setOrientation(LinearLayout.VERTICAL);body.setPadding(dp(13),dp(12),dp(13),dp(34));root.addView(body);
        TextView sub=text("grupo campeão fixo + grupo forte mais atrasado → 658.008 jogos em cada → filtros → perímetro de duque",13);sub.setTextColor(PURPLE_DARK);sub.setGravity(Gravity.CENTER);sub.setTypeface(Typeface.DEFAULT_BOLD);sub.setPadding(0,0,0,dp(7));body.addView(sub);

        btnLoad=button("1. CARREGAR TXT DA QUINA");body.addView(btnLoad);

        LinearLayout c1=card();
        TextView t1=text("JOGO 1 — GRUPO CAMPEÃO FIXO DE 40",19);t1.setTextColor(PURPLE_DARK);t1.setTypeface(Typeface.DEFAULT_BOLD);c1.addView(t1);
        TextView d1=text("Este grupo já ficou fixado na V2. O aplicativo não procura outro campeão: ele apenas atualiza as estatísticas com o TXT carregado e pesca dentro das mesmas 40 dezenas.",13);d1.setPadding(0,dp(6),0,dp(8));c1.addView(d1);
        championView=text("GRUPO CAMPEÃO FIXO\n"+MotorCore.fmt(MotorCore.CHAMPION_GROUP40)+"\n\nCarregue o TXT para conferir as estatísticas atuais.",14);championView.setTypeface(Typeface.MONOSPACE);championView.setTextColor(PURPLE_DARK);championView.setPadding(dp(9),dp(9),dp(9),dp(9));championView.setBackground(round(Color.rgb(244,237,250),Color.rgb(207,182,226),10));c1.addView(championView);
        btnChampionFixed=button("GERAR JOGO 1 — GRUPO CAMPEÃO");btnChampionFixed.setEnabled(false);c1.addView(btnChampionFixed);body.addView(c1);

        LinearLayout c2=card();
        TextView t2=text("JOGO 2 — GRUPO 40 MAIS ATRASADO",19);t2.setTextColor(PURPLE_DARK);t2.setTypeface(Typeface.DEFAULT_BOLD);c2.addView(t2);
        TextView d2=text("O motor procura entre grupos historicamente fortes o que está há mais concursos seguidos sem conter as 5 dezenas. Na tela aparece claramente quantos concursos ele está falhando.",13);d2.setPadding(0,dp(6),0,dp(8));c2.addView(d2);
        delayedView=text("Grupo atrasado ainda não procurado.",14);delayedView.setTypeface(Typeface.MONOSPACE);delayedView.setTextColor(PURPLE_DARK);delayedView.setPadding(dp(9),dp(9),dp(9),dp(9));delayedView.setBackground(round(Color.rgb(252,246,246),Color.rgb(228,188,188),10));c2.addView(delayedView);
        btnDelayedFixed=button("PROCURAR GRUPO ATRASADO + GERAR JOGO 2");btnDelayedFixed.setEnabled(false);c2.addView(btnDelayedFixed);body.addView(c2);

        LinearLayout fcard=card();
        TextView ft=text("FILTRO PADRÃO FIXO",19);ft.setTextColor(PURPLE_DARK);ft.setTypeface(Typeface.DEFAULT_BOLD);fcard.addView(ft);
        fixedView=text("rep=0-1 | pares=2-4 | primos=0-2 | fib=0-1 | 01-40=2-4 | soma=155..249",14);fixedView.setPadding(0,dp(7),0,dp(7));fcard.addView(fixedView);
        TextView fx=text("Esse padrão fica congelado no aplicativo e é usado nos dois botões principais.",12);fx.setTextColor(Color.DKGRAY);fcard.addView(fx);body.addView(fcard);

        LinearLayout acard=card();acard.setBackground(round(Color.rgb(251,248,254),Color.rgb(176,135,208),16));
        TextView at=text("FILTRO ALTERNATIVO",19);at.setTextColor(PURPLE_DARK);at.setTypeface(Typeface.DEFAULT_BOLD);acard.addView(at);
        TextView ad=text("Se quiser mudar o padrão, digite um valor ou uma faixa. Os botões abaixo usam a mesma engrenagem, mudando somente o filtro.",13);ad.setPadding(0,dp(5),0,dp(7));acard.addView(ad);
        edRep=field("Repetidas","0-1");edEven=field("Pares","2-4");edPrime=field("Primos","0-2");edFib=field("Fibonacci","0-1");edLow=field("Dezenas 01-40","2-4");edSumMin=field("Soma mínima","155");edSumMax=field("Soma máxima","249");
        acard.addView(row(edRep,edEven));acard.addView(row(edPrime,edFib));acard.addView(row(edLow,spaceField()));acard.addView(row(edSumMin,edSumMax));
        btnChampionAlt=button("CAMPEÃO — GERAR COM FILTRO ALTERNATIVO");btnChampionAlt.setEnabled(false);acard.addView(btnChampionAlt);
        btnDelayedAlt=button("ATRASADO — PROCURAR E GERAR COM ALTERNATIVO");btnDelayedAlt.setEnabled(false);acard.addView(btnDelayedAlt);body.addView(acard);

        currentFilter=text("Filtro atual: padrão fixo",14);currentFilter.setTextColor(PURPLE_DARK);currentFilter.setTypeface(Typeface.DEFAULT_BOLD);currentFilter.setPadding(dp(11),dp(10),dp(11),dp(10));currentFilter.setBackground(round(Color.rgb(238,226,248),Color.rgb(205,177,226),12));body.addView(currentFilter);
        btnPdf=button("GERAR PDF DOS JOGOS");btnPdf.setEnabled(false);body.addView(btnPdf);

        status=text("Aguardando base histórica...",15);status.setPadding(dp(14),dp(14),dp(14),dp(14));status.setBackground(round(Color.WHITE,Color.rgb(220,202,235),14));LinearLayout.LayoutParams slp=new LinearLayout.LayoutParams(-1,-2);slp.setMargins(0,dp(9),0,dp(8));body.addView(status,slp);
        progressText=text("0%",22);progressText.setGravity(Gravity.CENTER);progressText.setTextColor(PURPLE);progressText.setTypeface(Typeface.DEFAULT_BOLD);body.addView(progressText);
        progress=new ProgressBar(this,null,android.R.attr.progressBarStyleHorizontal);progress.setMax(100);if(Build.VERSION.SDK_INT>=21)progress.setProgressTintList(ColorStateList.valueOf(PURPLE));body.addView(progress,new LinearLayout.LayoutParams(-1,dp(18)));
        detail=text("ETAPA: aguardando\nPROCESSAMENTO: 0 / 0\nTEMPO: 0 s",14);detail.setTypeface(Typeface.MONOSPACE);detail.setPadding(0,dp(8),0,dp(8));body.addView(detail);
        output=text("",15);output.setTypeface(Typeface.MONOSPACE);output.setTextIsSelectable(true);output.setPadding(0,dp(10),0,dp(20));body.addView(output);

        btnLoad.setOnClickListener(v->openTxt());
        btnChampionFixed.setOnClickListener(v->runChampion(fixedFilter));
        btnDelayedFixed.setOnClickListener(v->searchDelayedAndRun(fixedFilter));
        btnChampionAlt.setOnClickListener(v->{try{runChampion(readAlternative());}catch(Exception e){toast(e.getMessage());}});
        btnDelayedAlt.setOnClickListener(v->{try{searchDelayedAndRun(readAlternative());}catch(Exception e){toast(e.getMessage());}});
        btnPdf.setOnClickListener(v->generatePdf());
        setContentView(scroll);
    }

    private EditText spaceField(){EditText e=field(""," ");e.setVisibility(View.INVISIBLE);return e;}
    private void openTxt(){Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);i.addCategory(Intent.CATEGORY_OPENABLE);i.setType("*/*");startActivityForResult(i,PICK_TXT);}

    @Override protected void onActivityResult(int requestCode,int resultCode,Intent data){
        super.onActivityResult(requestCode,resultCode,data);
        if(requestCode!=PICK_TXT||resultCode!=RESULT_OK||data==null||data.getData()==null)return;
        try{
            base=MotorCore.parseText(readAll(data.getData()));
            fixedFilter=MotorCore.standardFixedFilter();
            championGroup=MotorCore.evaluateFixedGroup(base,MotorCore.CHAMPION_GROUP40);
            delayedGroup=null; championGame=null; delayedGame=null;
            fixedView.setText(fixedFilter.summary());
            championView.setText("GRUPO CAMPEÃO FIXO — V2\n"+MotorCore.groupReport(championGroup)+"\n\n[40 dezenas congeladas no aplicativo]");
            delayedView.setText("Grupo atrasado ainda não procurado para esta base.");
            currentFilter.setText("Filtro atual: "+fixedFilter.summary());
            status.setText("BASE CARREGADA\nConcursos: "+base.draws.size()+"\nÚltimo concurso: "+base.contests.get(base.contests.size()-1)+"\nÚltimo resultado: "+MotorCore.fmt(base.last));
            enableReady(true); btnPdf.setEnabled(false); output.setText(""); progress.setProgress(0);progressText.setText("0%");
        }catch(Exception e){toast("Erro ao ler TXT: "+e.getMessage());}
    }

    private String readAll(Uri u)throws IOException{InputStream in=getContentResolver().openInputStream(u);if(in==null)throw new IOException("Arquivo indisponível.");ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] buf=new byte[65536];int n;while((n=in.read(buf))>=0)out.write(buf,0,n);in.close();return new String(out.toByteArray(),StandardCharsets.UTF_8);}

    private void runChampion(MotorCore.FilterConfig filter){
        if(base==null||championGroup==null)return;
        disableWorking();startMs=System.currentTimeMillis();currentFilter.setText("JOGO 1 • "+filter.label+" • "+filter.summary());status.setText("Pescando nas 658.008 combinações do grupo campeão fixo...");
        new Thread(()->{try{
            MotorCore.GameResult r=MotorCore.generateGame(base,championGroup.group40,filter,(stage,done,total,pct,extra)->{
                int mapped=Math.max(0,Math.min(100,(pct-78)*100/22));
                updateProgress(stage,done,total,mapped,extra);
            });
            championGame=r;
            runOnUiThread(()->{restoreButtons();progress.setProgress(100);progressText.setText("100%");status.setText(r.best!=null?"CONCLUÍDO — Jogo 1 gerado no grupo campeão.":"Nenhum jogo do grupo campeão passou no filtro.");refreshOutput();updatePdfButton();});
        }catch(Exception e){runOnUiThread(()->{restoreButtons();status.setText("ERRO NO JOGO 1: "+e.getMessage());});}}).start();
    }

    private void searchDelayedAndRun(MotorCore.FilterConfig filter){
        if(base==null)return;
        disableWorking();startMs=System.currentTimeMillis();currentFilter.setText("JOGO 2 • "+filter.label+" • "+filter.summary());status.setText("Procurando grupo forte de 40 com maior atraso atual...");
        new Thread(()->{try{
            MotorCore.Group40Result g=MotorCore.searchMostDelayedStrongGroup40(base,600,0,(stage,done,total,pct,extra)->updateProgress(stage,done,total,pct,extra));
            delayedGroup=g;
            runOnUiThread(()->delayedView.setText("GRUPO 40 MAIS ATRASADO ENCONTRADO\n"+MotorCore.groupReport(g)+"\n\nCritério: maior atraso entre grupos historicamente fortes."));
            MotorCore.GameResult r=MotorCore.generateGame(base,g.group40,filter,(stage,done,total,pct,extra)->{
                int mapped=72+Math.max(0,Math.min(28,(pct-78)*28/22));
                updateProgress(stage,done,total,mapped,extra);
            });
            delayedGame=r;
            runOnUiThread(()->{restoreButtons();progress.setProgress(100);progressText.setText("100%");status.setText(r.best!=null?"CONCLUÍDO — grupo atrasado encontrado e Jogo 2 gerado.":"Grupo atrasado encontrado, mas nenhum jogo passou no filtro.");refreshOutput();updatePdfButton();});
        }catch(Exception e){runOnUiThread(()->{restoreButtons();status.setText("ERRO NO JOGO 2: "+e.getMessage());});}}).start();
    }

    private void refreshOutput(){
        StringBuilder s=new StringBuilder();
        if(championGame!=null){s.append("========== JOGO 1 — GRUPO CAMPEÃO ==========\n");s.append(MotorCore.gameReport(base,championGroup,championGame)).append("\n\n");}
        if(delayedGame!=null&&delayedGroup!=null){s.append("========== JOGO 2 — GRUPO MAIS ATRASADO ==========\n");s.append(MotorCore.gameReport(base,delayedGroup,delayedGame)).append("\n");}
        output.setText(s.toString());
    }

    private void updateProgress(String stage,int done,int total,int pct,String extra){long sec=(System.currentTimeMillis()-startMs)/1000;runOnUiThread(()->{int p=Math.max(0,Math.min(100,pct));progress.setProgress(p);progressText.setText(p+"%");detail.setText("ETAPA: "+stage+"\nPROCESSAMENTO: "+done+" / "+total+"\nTEMPO: "+sec+" s\n"+extra);});}
    private void disableWorking(){btnLoad.setEnabled(false);btnChampionFixed.setEnabled(false);btnDelayedFixed.setEnabled(false);btnChampionAlt.setEnabled(false);btnDelayedAlt.setEnabled(false);btnPdf.setEnabled(false);progress.setProgress(0);progressText.setText("0%");}
    private void restoreButtons(){btnLoad.setEnabled(true);enableReady(base!=null);updatePdfButton();}
    private void enableReady(boolean e){btnChampionFixed.setEnabled(e);btnDelayedFixed.setEnabled(e);btnChampionAlt.setEnabled(e);btnDelayedAlt.setEnabled(e);}
    private void updatePdfButton(){btnPdf.setEnabled((championGame!=null&&championGame.best!=null)||(delayedGame!=null&&delayedGame.best!=null));}

    private MotorCore.FilterConfig readAlternative(){MotorCore.FilterConfig f=new MotorCore.FilterConfig();f.label="FILTRO ALTERNATIVO";int[] r=parseRange(edRep.getText().toString(),"Repetidas");f.repMin=r[0];f.repMax=r[1];r=parseRange(edEven.getText().toString(),"Pares");f.evenMin=r[0];f.evenMax=r[1];r=parseRange(edPrime.getText().toString(),"Primos");f.primeMin=r[0];f.primeMax=r[1];r=parseRange(edFib.getText().toString(),"Fibonacci");f.fibMin=r[0];f.fibMax=r[1];r=parseRange(edLow.getText().toString(),"01-40");f.lowMin=r[0];f.lowMax=r[1];f.sumMin=parseInt(edSumMin,"Soma mínima");f.sumMax=parseInt(edSumMax,"Soma máxima");if(f.sumMin>f.sumMax)throw new IllegalArgumentException("Soma mínima não pode ser maior que a máxima.");validate0to5(f.repMin,f.repMax,"Repetidas");validate0to5(f.evenMin,f.evenMax,"Pares");validate0to5(f.primeMin,f.primeMax,"Primos");validate0to5(f.fibMin,f.fibMax,"Fibonacci");validate0to5(f.lowMin,f.lowMax,"01-40");return f;}
    private void validate0to5(int a,int b,String n){if(a<0||b>5)throw new IllegalArgumentException(n+" deve ficar entre 0 e 5.");}
    private int parseInt(EditText e,String n){try{return Integer.parseInt(e.getText().toString().trim());}catch(Exception x){throw new IllegalArgumentException("Valor inválido em "+n);}}
    private int[] parseRange(String raw,String name){String s=raw.trim().replace(" ","").replace("..","-");try{if(s.contains("-")){String[] p=s.split("-",2);int a=Integer.parseInt(p[0]),b=Integer.parseInt(p[1]);if(a>b){int t=a;a=b;b=t;}return new int[]{a,b};}int v=Integer.parseInt(s);return new int[]{v,v};}catch(Exception e){throw new IllegalArgumentException(name+": use 2 ou faixa 1-2.");}}

    private void generatePdf(){
        if(base==null)return;
        try{
            String name="QUINA_CAMPEA_V2_DOIS_GRUPOS_CONCURSO_"+(base.contests.get(base.contests.size()-1)+1)+".pdf";
            PdfDocument doc=new PdfDocument();int pageNo=1;
            if(championGame!=null&&championGame.best!=null)drawPdfPage(doc,pageNo++,"JOGO 1 — GRUPO CAMPEÃO FIXO",championGroup,championGame);
            if(delayedGame!=null&&delayedGame.best!=null)drawPdfPage(doc,pageNo,"JOGO 2 — GRUPO 40 MAIS ATRASADO",delayedGroup,delayedGame);
            savePdf(doc,name);doc.close();
        }catch(Exception e){toast("Erro ao gerar PDF: "+e.getMessage());}
    }

    private void drawPdfPage(PdfDocument doc,int pageNo,String title,MotorCore.Group40Result group,MotorCore.GameResult result){
        PdfDocument.PageInfo pi=new PdfDocument.PageInfo.Builder(595,842,pageNo).create();PdfDocument.Page page=doc.startPage(pi);Canvas c=page.getCanvas();Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setColor(PURPLE);c.drawRect(0,0,595,94,p);p.setColor(Color.WHITE);p.setTypeface(Typeface.DEFAULT_BOLD);p.setTextSize(19);c.drawText("QUINA CAMPEÃ — DOIS GRUPOS DE 40",25,32,p);p.setTextSize(12.5f);c.drawText(title,25,56,p);p.setTypeface(Typeface.DEFAULT);p.setTextSize(9.2f);c.drawText(result.filter.label+" | "+result.filter.summary(),25,78,p);
        p.setColor(Color.BLACK);p.setTypeface(Typeface.DEFAULT_BOLD);p.setTextSize(11);c.drawText("Grupo de 40:",28,116,p);p.setTypeface(Typeface.DEFAULT);p.setTextSize(9.2f);drawWrapped(c,p,MotorCore.fmt(group.group40),28,134,540,14);
        p.setTypeface(Typeface.DEFAULT_BOLD);p.setTextSize(12);c.drawText("VOLANTE 01-80",28,178,p);
        HashSet<Integer> gg=new HashSet<>();for(int n:group.group40)gg.add(n);HashSet<Integer> game=new HashSet<>();for(int n:result.best.game)game.add(n);
        float left=27,top=192,cw=49,ch=36,gap=3;p.setTextAlign(Paint.Align.CENTER);
        for(int n=1;n<=80;n++){int row=(n-1)/10,col=(n-1)%10;float x=left+col*(cw+gap),y=top+row*(ch+gap);if(game.contains(n))p.setColor(GREEN);else if(!gg.contains(n))p.setColor(SOFT_RED);else p.setColor(Color.WHITE);RectF box=new RectF(x,y,x+cw,y+ch);c.drawRoundRect(box,6,6,p);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(1.2f);p.setColor(gg.contains(n)?PURPLE:RED);c.drawRoundRect(box,6,6,p);p.setStyle(Paint.Style.FILL);p.setColor(game.contains(n)?Color.WHITE:(gg.contains(n)?PURPLE_DARK:RED));p.setTypeface(Typeface.DEFAULT_BOLD);p.setTextSize(13);c.drawText(String.format(Locale.US,"%02d",n),x+cw/2,y+23,p);}p.setTextAlign(Paint.Align.LEFT);
        float y=525;p.setColor(Color.BLACK);p.setTypeface(Typeface.DEFAULT_BOLD);p.setTextSize(13);c.drawText("JOGO FINAL — VERDE",28,y,p);y+=23;p.setColor(GREEN);p.setTextSize(21);c.drawText(MotorCore.fmt(result.best.game),28,y,p);y+=34;
        p.setColor(RED);p.setTypeface(Typeface.DEFAULT_BOLD);p.setTextSize(11);c.drawText("VERMELHO = 40 DEZENAS FORA DO GRUPO / FALHA",28,y,p);y+=25;
        p.setColor(Color.BLACK);p.setTypeface(Typeface.DEFAULT_BOLD);p.setTextSize(12);c.drawText("RESUMO",28,y,p);y+=19;p.setTypeface(Typeface.DEFAULT);p.setTextSize(10.5f);MotorCore.Candidate b=result.best;
        c.drawText("Repetidas: "+b.metrics.rep+" | Pares/Ímpares: "+b.metrics.even+"/"+b.metrics.odd+" | Primos: "+b.metrics.primes+" | Fibonacci: "+b.metrics.fib,28,y,p);y+=17;
        c.drawText("01-40 / 41-80: "+b.metrics.low+"/"+b.metrics.high+" | Soma: "+b.metrics.sum+" | Duque mais próximo: "+b.nearestDuqueGap,28,y,p);y+=17;
        c.drawText("Duques 30/100: "+b.duques30+"/"+b.duques100+" | Ausentes do ciclo no jogo: "+b.cycleAbsentCount,28,y,p);y+=17;
        c.drawText("5/5 históricos dentro do grupo: "+group.hits5+" | Falhou agora: "+group.currentDelay+" concurso(s)",28,y,p);y+=17;
        c.drawText("Intervalo médio: "+String.format(Locale.US,"%.2f",group.meanInterval)+" | Maior intervalo: "+group.maxInterval+" | Pressão: "+String.format(Locale.US,"%.3f",group.pressure),28,y,p);y+=17;
        c.drawText("C(40,5): 658.008 | classificados no filtro: "+result.totalClassified,28,y,p);
        p.setColor(Color.DKGRAY);p.setTextSize(8.7f);c.drawText("Verde = jogo final | vermelho = fora do grupo/falha | branco = restante do grupo de 40",28,791,p);c.drawText("Estudo estatístico; sorteios permanecem aleatórios e não há garantia de prêmio.",28,810,p);
        doc.finishPage(page);
    }

    private void drawWrapped(Canvas c,Paint p,String text,float x,float y,float maxWidth,float lineH){String[] w=text.split(" ");String line="";for(String z:w){String t=line.isEmpty()?z:line+" "+z;if(p.measureText(t)>maxWidth){c.drawText(line,x,y,p);y+=lineH;line=z;}else line=t;}if(!line.isEmpty())c.drawText(line,x,y,p);}
    private void savePdf(PdfDocument doc,String name)throws IOException{if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.Q){ContentValues cv=new ContentValues();cv.put(MediaStore.Downloads.DISPLAY_NAME,name);cv.put(MediaStore.Downloads.MIME_TYPE,"application/pdf");cv.put(MediaStore.Downloads.RELATIVE_PATH,Environment.DIRECTORY_DOWNLOADS);Uri uri=getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI,cv);if(uri==null)throw new IOException("Não foi possível criar PDF.");OutputStream os=getContentResolver().openOutputStream(uri);if(os==null)throw new IOException("Não foi possível gravar PDF.");doc.writeTo(os);os.close();toast("PDF salvo em Download/"+name);}else{File dir=getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS);if(dir==null)throw new IOException("Downloads indisponível.");File f=new File(dir,name);FileOutputStream os=new FileOutputStream(f);doc.writeTo(os);os.close();toast("PDF salvo em "+f.getAbsolutePath());}}
    private void toast(String s){Toast.makeText(this,s,Toast.LENGTH_LONG).show();}
}

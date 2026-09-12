package com.quina.campeao40;

import java.util.*;
import java.util.regex.*;

/**
 * QUINA CAMPEÃ - GRUPO 40
 *
 * Objetivo do motor:
 * 1) Ler todo o histórico da Quina.
 * 2) Procurar, por busca profunda determinística, um grupo de 40 dezenas que
 *    maximize a quantidade de concursos históricos cujas 5 dezenas ficaram
 *    totalmente dentro do grupo.
 * 3) Fixar esse grupo e gerar C(40,5)=658.008 jogos.
 * 4) Aplicar filtro histórico fixo ou filtro alternativo.
 * 5) Rankear os sobreviventes por força/talo/ciclo e perímetro de duque.
 *
 * Observação matemática: examinar todos C(80,40) grupos é impraticável.
 * Este motor usa múltiplos inícios + melhoria exata por troca 1x1 até ótimo
 * local. O resultado é o MELHOR GRUPO ENCONTRADO pela busca, sem alegar prova
 * de ótimo global.
 */
public final class MotorCore {
    private MotorCore() {}

    public static final int UNIVERSE = 80;
    public static final int GROUP_SIZE = 40;
    public static final int GAME_SIZE = 5;
    public static final int TOTAL_GAMES_GROUP40 = 658008;

    // Uma semente externa útil, NÃO tratada como vencedora. É apenas ponto de partida.
    public static final int[] MAZUSOFT_SEED40 = new int[]{
            2,3,5,6,8,9,12,13,14,15,16,17,18,19,20,22,24,25,26,27,
            28,29,30,34,36,38,39,41,42,46,47,50,55,56,57,61,63,65,66,78
    };

    // Grupo campeão encontrado na V1 e conferido pelo usuário no histórico:
    // 340 ocorrências de 5/5 até o concurso 7114 na base usada naquele estudo.
    // A V2 NÃO refaz esse grupo: ele já nasce fixo no aplicativo.
    public static final int[] CHAMPION_GROUP40 = new int[]{
            4,5,6,9,10,11,13,14,15,21,23,24,26,29,30,31,33,36,38,39,
            43,44,48,49,51,52,54,56,57,63,64,65,66,67,68,69,71,72,73,77
    };

    private static final boolean[] PRIMES = boolSet(
            2,3,5,7,11,13,17,19,23,29,31,37,41,43,47,53,59,61,67,71,73,79
    );
    private static final boolean[] FIB = boolSet(1,2,3,5,8,13,21,34,55);

    public interface ProgressCallback {
        void update(String stage, int done, int total, int overallPercent, String detail);
    }

    public static final class BaseData {
        public final List<Integer> contests;
        public final List<int[]> draws;
        public final int[] last;
        public BaseData(List<Integer> c, List<int[]> d) {
            contests = c;
            draws = d;
            last = d.get(d.size()-1).clone();
        }
    }

    public static final class Group40Result {
        public int[] group40;
        public int hits5;
        public Integer lastHitContest;
        public int currentDelay;
        public double meanInterval;
        public int maxInterval;
        public double pressure;
        public int restarts;
        public long elapsedMs;
    }

    public static final class FilterConfig {
        public String label;
        public int repMin, repMax;
        public int evenMin, evenMax;
        public int primeMin, primeMax;
        public int fibMin, fibMax;
        public int lowMin, lowMax; // 01..40
        public int sumMin, sumMax;

        public String summary() {
            return "rep="+range(repMin,repMax)+
                    " | pares="+range(evenMin,evenMax)+
                    " | primos="+range(primeMin,primeMax)+
                    " | fib="+range(fibMin,fibMax)+
                    " | 01-40="+range(lowMin,lowMax)+
                    " | soma="+sumMin+".."+sumMax;
        }
        private static String range(int a, int b) { return a==b ? String.valueOf(a) : a+"-"+b; }
    }

    public static final class Metrics {
        public int rep, even, odd, primes, fib, low, high, sum;
    }

    public static final class NumberInfo {
        public int n;
        public double hist, r100, r50, r20, weighted30, trend;
        public int delay;
        public double delayMean, pressure;
        public double strength;
    }

    public static final class Candidate {
        public int[] game;
        public Metrics metrics;
        public double baseScore;
        public double finalScore;
        public int nearestDuqueGap;
        public int duques30;
        public int duques100;
        public int cycleAbsentCount;
    }

    public static final class GameResult {
        public int[] fixedGroup40;
        public FilterConfig filter;
        public Candidate best;
        public List<Candidate> top20;
        public int totalGenerated;
        public int totalClassified;
        public Set<Integer> cycleAbsent;
        public NumberInfo[] numberMap;
    }

    private static boolean[] boolSet(int... values) {
        boolean[] a = new boolean[81];
        for (int v: values) if (v>=1 && v<=80) a[v]=true;
        return a;
    }

    // ------------------------------------------------------------------
    // PARSER
    // ------------------------------------------------------------------
    public static BaseData parseText(String text) {
        TreeMap<Integer,int[]> map = new TreeMap<>();
        String[] lines = text.split("\\R");
        Pattern contestP = Pattern.compile("(?i)concurso\\D*(\\d+)");
        Pattern numsP = Pattern.compile("(?i)(?:números|numeros|dezenas)\\D*([0-9\\s]+)");
        Pattern allP = Pattern.compile("\\d+");

        int fallbackContest = 1;
        for (String line: lines) {
            line = line.trim();
            if (line.isEmpty()) continue;

            Integer contest = null;
            Matcher cm = contestP.matcher(line);
            if (cm.find()) {
                try { contest = Integer.parseInt(cm.group(1)); } catch (Exception ignored) {}
            }

            ArrayList<Integer> nums = new ArrayList<>();
            Matcher nm = numsP.matcher(line);
            if (nm.find()) {
                Matcher m = allP.matcher(nm.group(1));
                while (m.find()) {
                    int v = Integer.parseInt(m.group());
                    if (v>=1 && v<=80) nums.add(v);
                }
            }

            if (nums.size() < 5) {
                ArrayList<Integer> vals = new ArrayList<>();
                Matcher m = allP.matcher(line);
                while (m.find()) vals.add(Integer.parseInt(m.group()));
                // Formato comum: concurso + 5 dezenas. Datas podem aparecer, então
                // preferimos as últimas 5 dezenas válidas.
                ArrayList<Integer> valid = new ArrayList<>();
                for (int v: vals) if (v>=1 && v<=80) valid.add(v);
                if (valid.size() >= 5) {
                    nums.clear();
                    for (int i=valid.size()-5;i<valid.size();i++) nums.add(valid.get(i));
                    if (contest == null && !vals.isEmpty() && vals.get(0) > 80) contest = vals.get(0);
                }
            }

            if (nums.size() >= 5) {
                int[] d = new int[5];
                HashSet<Integer> unique = new HashSet<>();
                int off = nums.size()-5;
                for (int i=0;i<5;i++) { d[i]=nums.get(off+i); unique.add(d[i]); }
                if (unique.size()==5) {
                    Arrays.sort(d);
                    if (contest == null) {
                        while (map.containsKey(fallbackContest)) fallbackContest++;
                        contest = fallbackContest++;
                    }
                    map.put(contest,d);
                }
            }
        }

        if (map.size() < 50) throw new IllegalArgumentException("Poucos concursos válidos da Quina foram identificados no TXT.");
        ArrayList<Integer> contests = new ArrayList<>(map.keySet());
        ArrayList<int[]> draws = new ArrayList<>();
        for (Integer c: contests) draws.add(map.get(c));
        return new BaseData(contests,draws);
    }

    // ------------------------------------------------------------------
    // ESTUDO E FIXAÇÃO DO GRUPO 40
    // ------------------------------------------------------------------
    public static Group40Result searchBestGroup40(BaseData base, int restarts, ProgressCallback cb) {
        if (restarts < 20) restarts = 20;
        long start = System.currentTimeMillis();
        int[] freq = frequency(base.draws);

        int[] bestGroup = null;
        int bestHits = -1;
        GroupStats bestStats = null;

        long seedBase = 20260912L + base.contests.get(base.contests.size()-1) * 1000003L + base.draws.size();
        Random rng = new Random(seedBase);

        for (int r=0;r<restarts;r++) {
            int[] seed;
            if (r==0) seed = MAZUSOFT_SEED40.clone();
            else if (r==1) seed = topFrequency40(freq);
            else if (r==2) seed = recentFrequency40(base.draws, 200);
            else seed = randomizedSeed(freq, rng, r);

            int[] local = improveBySwaps(seed, base.draws);
            GroupStats st = groupStats(local, base);

            if (st.hits5 > bestHits ||
                    (st.hits5 == bestHits && (bestStats==null || st.pressure > bestStats.pressure)) ||
                    (st.hits5 == bestHits && bestStats!=null && Math.abs(st.pressure-bestStats.pressure)<1e-12 && lexLess(local,bestGroup))) {
                bestHits = st.hits5;
                bestGroup = local.clone();
                bestStats = st;
            }

            if (cb != null && (r==0 || r==restarts-1 || r%Math.max(1,restarts/100)==0)) {
                int pct = (int)Math.round((r+1)*100.0/restarts);
                cb.update("Procurando grupo campeão de 40",r+1,restarts,Math.min(78,pct*78/100),
                        "Melhor encontrado: "+bestHits+" concursos com 5/5 dentro do grupo");
            }
        }

        Group40Result out = new Group40Result();
        out.group40 = bestGroup;
        out.hits5 = bestStats.hits5;
        out.lastHitContest = bestStats.lastHitContest;
        out.currentDelay = bestStats.currentDelay;
        out.meanInterval = bestStats.meanInterval;
        out.maxInterval = bestStats.maxInterval;
        out.pressure = bestStats.pressure;
        out.restarts = restarts;
        out.elapsedMs = System.currentTimeMillis()-start;
        return out;
    }

    /**
     * Procura um grupo de 40 historicamente forte que esteja atualmente mais
     * atrasado para conter 5/5. Para evitar um grupo artificialmente ruim que
     * quase nunca premia, primeiro cada semente é melhorada por ocorrências
     * históricas de 5/5; entre os grupos fortes resultantes vence o maior atraso.
     *
     * minHits é um piso de qualidade histórica. Se <=0, usa 60% das ocorrências
     * do grupo campeão fixo na própria base carregada.
     */
    public static Group40Result searchMostDelayedStrongGroup40(BaseData base, int restarts, int minHits, ProgressCallback cb) {
        if (restarts < 20) restarts = 20;
        long start = System.currentTimeMillis();
        int[] freq = frequency(base.draws);
        Group40Result champion = evaluateFixedGroup(base, CHAMPION_GROUP40);
        final int floor = minHits > 0 ? minHits : Math.max(30, (int)Math.floor(champion.hits5 * 0.60));

        int[] bestGroup = null;
        GroupStats bestStats = null;
        int[] fallbackGroup = null;
        GroupStats fallbackStats = null;

        long seedBase = 771140L + base.contests.get(base.contests.size()-1) * 2000003L + base.draws.size();
        Random rng = new Random(seedBase);

        for (int r=0; r<restarts; r++) {
            int[] seed;
            if (r==0) seed = CHAMPION_GROUP40.clone();
            else if (r==1) seed = topFrequency40(freq);
            else if (r==2) seed = recentFrequency40(base.draws, 200);
            else seed = randomizedSeed(freq, rng, r+9000);

            int[] local = improveBySwaps(seed, base.draws);
            GroupStats st = groupStats(local, base);

            // Fallback: melhor atraso encontrado, mesmo se o piso não for atingido.
            if (fallbackStats==null || delayedBetter(st, local, fallbackStats, fallbackGroup)) {
                fallbackStats = st; fallbackGroup = local.clone();
            }

            if (st.hits5 >= floor && (bestStats==null || delayedBetter(st, local, bestStats, bestGroup))) {
                bestStats = st; bestGroup = local.clone();
            }

            if (cb != null && (r==0 || r==restarts-1 || r%Math.max(1,restarts/100)==0)) {
                int pct=(int)Math.round((r+1)*100.0/restarts);
                GroupStats show = bestStats!=null ? bestStats : fallbackStats;
                String det = show==null ? "iniciando" :
                        "Maior atraso: "+show.currentDelay+" | 5/5 históricos: "+show.hits5+" | piso: "+floor;
                cb.update("Procurando grupo 40 mais atrasado", r+1, restarts, Math.min(72,pct*72/100), det);
            }
        }

        if (bestStats==null) { bestStats=fallbackStats; bestGroup=fallbackGroup; }
        if (bestStats==null || bestGroup==null) throw new IllegalStateException("Não foi possível formar grupo atrasado.");

        Group40Result out = new Group40Result();
        out.group40=bestGroup; out.hits5=bestStats.hits5; out.lastHitContest=bestStats.lastHitContest;
        out.currentDelay=bestStats.currentDelay; out.meanInterval=bestStats.meanInterval; out.maxInterval=bestStats.maxInterval;
        out.pressure=bestStats.pressure; out.restarts=restarts; out.elapsedMs=System.currentTimeMillis()-start;
        return out;
    }

    private static boolean delayedBetter(GroupStats a, int[] ga, GroupStats b, int[] gb) {
        if (a.currentDelay != b.currentDelay) return a.currentDelay > b.currentDelay;
        int q=Double.compare(a.pressure,b.pressure); if(q!=0) return q>0;
        if (a.hits5 != b.hits5) return a.hits5 > b.hits5;
        return lexLess(ga,gb);
    }

    private static int[] frequency(List<int[]> draws) {
        int[] f = new int[81];
        for (int[] d: draws) for (int n:d) f[n]++;
        return f;
    }

    private static int[] topFrequency40(int[] freq) {
        Integer[] ns = new Integer[80];
        for (int i=0;i<80;i++) ns[i]=i+1;
        Arrays.sort(ns,(a,b)-> freq[b]!=freq[a] ? Integer.compare(freq[b],freq[a]) : Integer.compare(a,b));
        int[] g = new int[40];
        for (int i=0;i<40;i++) g[i]=ns[i];
        Arrays.sort(g); return g;
    }

    private static int[] recentFrequency40(List<int[]> draws, int window) {
        int[] f = new int[81];
        int start = Math.max(0, draws.size()-window);
        for (int i=start;i<draws.size();i++) for (int n:draws.get(i)) f[n]++;
        return topFrequency40(f);
    }

    private static int[] randomizedSeed(int[] freq, Random rng, int restart) {
        ArrayList<Integer> pool = new ArrayList<>();
        for (int n=1;n<=80;n++) pool.add(n);
        // Chave aleatória com leve viés de frequência para gerar diversidade sem abandonar o histórico.
        final double[] key = new double[81];
        int min=Integer.MAX_VALUE,max=Integer.MIN_VALUE;
        for (int n=1;n<=80;n++){ min=Math.min(min,freq[n]); max=Math.max(max,freq[n]); }
        for (int n=1;n<=80;n++) {
            double nf = max==min ? .5 : (freq[n]-min)/(double)(max-min);
            key[n] = rng.nextDouble() + 0.28*nf + 0.000001*(80-n) + restart*1e-12;
        }
        Collections.sort(pool,(a,b)->Double.compare(key[b],key[a]));
        int[] g = new int[40];
        for (int i=0;i<40;i++) g[i]=pool.get(i);
        Arrays.sort(g); return g;
    }

    private static int[] improveBySwaps(int[] seed, List<int[]> draws) {
        boolean[] in = new boolean[81];
        for (int n: seed) in[n]=true;

        for (int iter=0;iter<200;iter++) {
            int[] inside = toArray(in,true);
            int[] outside = toArray(in,false);
            int[] loss = new int[81];
            int[][] gain = new int[81][81]; // [a fora?] usamos [a_in][b_out]

            for (int[] d: draws) {
                int missingCount=0, missing=0;
                for (int n:d) if (!in[n]) { missingCount++; missing=n; }
                if (missingCount==0) {
                    for (int n:d) loss[n]++;
                } else if (missingCount==1) {
                    int b=missing;
                    for (int a: inside) {
                        if (!contains5(d,a)) gain[a][b]++;
                    }
                }
            }

            int bestDelta=0,bestA=-1,bestB=-1;
            for (int a: inside) {
                for (int b: outside) {
                    int delta = gain[a][b]-loss[a];
                    if (delta > bestDelta || (delta==bestDelta && delta>0 && (a<bestA || (a==bestA && b<bestB)))) {
                        bestDelta=delta; bestA=a; bestB=b;
                    }
                }
            }
            if (bestDelta<=0) break;
            in[bestA]=false;
            in[bestB]=true;
        }
        int[] out = toArray(in,true);
        Arrays.sort(out);
        return out;
    }

    private static int[] toArray(boolean[] in, boolean value) {
        int count=0; for(int n=1;n<=80;n++) if(in[n]==value) count++;
        int[] a=new int[count]; int k=0;
        for(int n=1;n<=80;n++) if(in[n]==value) a[k++]=n;
        return a;
    }

    private static boolean contains5(int[] d, int n) {
        for (int x:d) if (x==n) return true;
        return false;
    }

    private static boolean lexLess(int[] a, int[] b) {
        if (b==null) return true;
        for (int i=0;i<Math.min(a.length,b.length);i++) {
            if (a[i]!=b[i]) return a[i]<b[i];
        }
        return a.length<b.length;
    }

    private static final class GroupStats {
        int hits5; Integer lastHitContest; int currentDelay; double meanInterval; int maxInterval; double pressure;
    }

    public static Group40Result evaluateFixedGroup(BaseData base, int[] group) {
        if (group == null || group.length != 40) throw new IllegalArgumentException("Grupo fixo precisa ter 40 dezenas.");
        int[] g = group.clone(); Arrays.sort(g);
        GroupStats st = groupStats(g, base);
        Group40Result out = new Group40Result();
        out.group40 = g; out.hits5 = st.hits5; out.lastHitContest = st.lastHitContest;
        out.currentDelay = st.currentDelay; out.meanInterval = st.meanInterval; out.maxInterval = st.maxInterval;
        out.pressure = st.pressure; out.restarts = 0; out.elapsedMs = 0;
        return out;
    }

    private static GroupStats groupStats(int[] group, BaseData base) {
        boolean[] in = new boolean[81]; for(int n:group) in[n]=true;
        ArrayList<Integer> idxs = new ArrayList<>();
        for (int i=0;i<base.draws.size();i++) {
            boolean ok=true; for(int n:base.draws.get(i)) if(!in[n]) {ok=false;break;}
            if(ok) idxs.add(i);
        }
        GroupStats s=new GroupStats();
        s.hits5=idxs.size();
        if(!idxs.isEmpty()) {
            int last=idxs.get(idxs.size()-1);
            s.lastHitContest=base.contests.get(last);
            s.currentDelay=(base.draws.size()-1)-last;
            long sum=0; int max=0;
            for(int i=1;i<idxs.size();i++){int g=idxs.get(i)-idxs.get(i-1);sum+=g;max=Math.max(max,g);}
            s.meanInterval=idxs.size()>1 ? sum/(double)(idxs.size()-1) : base.draws.size();
            s.maxInterval=idxs.size()>1 ? max : base.draws.size();
        } else {
            s.lastHitContest=null; s.currentDelay=base.draws.size(); s.meanInterval=base.draws.size(); s.maxInterval=base.draws.size();
        }
        s.pressure=s.currentDelay/Math.max(1.0,s.meanInterval);
        return s;
    }

    // ------------------------------------------------------------------
    // FILTRO PADRÃO FIXO - congelado a partir do estudo exibido na V1
    // ------------------------------------------------------------------
    public static FilterConfig standardFixedFilter() {
        FilterConfig f=new FilterConfig();
        f.label="FILTRO PADRÃO FIXO";
        f.repMin=0; f.repMax=1;
        f.evenMin=2; f.evenMax=4;
        f.primeMin=0; f.primeMax=2;
        f.fibMin=0; f.fibMax=1;
        f.lowMin=2; f.lowMax=4;
        f.sumMin=155; f.sumMax=249;
        return f;
    }

    // Mantido para auditoria/estudo, mas a V2 usa standardFixedFilter().
    public static FilterConfig learnFixedFilter(BaseData base) {
        ArrayList<Integer> reps=new ArrayList<>(), evens=new ArrayList<>(), primes=new ArrayList<>(), fibs=new ArrayList<>(), lows=new ArrayList<>(), sums=new ArrayList<>();
        for(int i=1;i<base.draws.size();i++) {
            int[] d=base.draws.get(i), prev=base.draws.get(i-1);
            Metrics m=metrics(d,prev);
            reps.add(m.rep); evens.add(m.even); primes.add(m.primes); fibs.add(m.fib); lows.add(m.low); sums.add(m.sum);
        }
        FilterConfig f=new FilterConfig();
        f.label="FILTRO FIXO HISTÓRICO";
        int[] r=smallestRange(reps,0.72); f.repMin=r[0]; f.repMax=r[1];
        r=smallestRange(evens,0.72); f.evenMin=r[0]; f.evenMax=r[1];
        r=smallestRange(primes,0.72); f.primeMin=r[0]; f.primeMax=r[1];
        r=smallestRange(fibs,0.72); f.fibMin=r[0]; f.fibMax=r[1];
        r=smallestRange(lows,0.72); f.lowMin=r[0]; f.lowMax=r[1];
        Collections.sort(sums);
        f.sumMin=percentile(sums,0.18);
        f.sumMax=percentile(sums,0.82);
        return f;
    }

    private static int[] smallestRange(List<Integer> vals, double cover) {
        if(vals.isEmpty()) return new int[]{0,5};
        int lo=Collections.min(vals), hi=Collections.max(vals);
        int need=(int)Math.ceil(vals.size()*cover);
        int[] cnt=new int[hi+1]; for(int v:vals) cnt[v]++;
        int bestL=lo,bestH=hi,bestW=999,bestCount=-1;
        for(int a=lo;a<=hi;a++) {
            int s=0;
            for(int b=a;b<=hi;b++) {
                s+=cnt[b];
                if(s>=need) {
                    int w=b-a;
                    if(w<bestW || (w==bestW && s>bestCount)) {bestW=w;bestL=a;bestH=b;bestCount=s;}
                    break;
                }
            }
        }
        return new int[]{bestL,bestH};
    }

    private static int percentile(List<Integer> sorted, double p) {
        if(sorted.isEmpty()) return 0;
        int idx=(int)Math.floor(p*(sorted.size()-1));
        return sorted.get(Math.max(0,Math.min(sorted.size()-1,idx)));
    }

    // ------------------------------------------------------------------
    // GERAÇÃO DOS 658.008 JOGOS
    // ------------------------------------------------------------------
    public static GameResult generateGame(BaseData base, int[] fixedGroup40, FilterConfig filter, ProgressCallback cb) {
        if(fixedGroup40==null || fixedGroup40.length!=40) throw new IllegalArgumentException("Grupo fixo precisa ter 40 dezenas.");
        int[] group=fixedGroup40.clone(); Arrays.sort(group);
        GameResult out=new GameResult();
        out.fixedGroup40=group; out.filter=filter; out.totalGenerated=TOTAL_GAMES_GROUP40;
        out.numberMap=buildNumberMap(base);
        out.cycleAbsent=cycleAbsent(base.draws);

        PriorityQueue<Candidate> heap=new PriorityQueue<>(Comparator.comparingDouble(c->c.baseScore));
        final int KEEP=500;
        int classified=0, processed=0;
        int[] last=base.last;

        for(int a=0;a<36;a++) for(int b=a+1;b<37;b++) for(int c=b+1;c<38;c++) for(int d=c+1;d<39;d++) for(int e=d+1;e<40;e++) {
            processed++;
            int[] g=new int[]{group[a],group[b],group[c],group[d],group[e]};
            Metrics m=metrics(g,last);
            if(!passes(m,filter)) continue;
            classified++;
            Candidate cand=new Candidate(); cand.game=g; cand.metrics=m;
            cand.cycleAbsentCount=0;
            double s=0;
            for(int n:g){ s += out.numberMap[n].strength; if(out.cycleAbsent.contains(n)) cand.cycleAbsentCount++; }
            s/=5.0;
            // Bônus pequeno por carregar ausente do ciclo, sem tornar obrigatório.
            s += Math.min(2,cand.cycleAbsentCount)*4.0;
            // Equilíbrio entre baixos/altos já está no filtro quando escolhido.
            cand.baseScore=s;
            if(heap.size()<KEEP) heap.add(cand);
            else if(cand.baseScore>heap.peek().baseScore){heap.poll();heap.add(cand);}

            if(cb!=null && processed%12000==0){
                int pct=78+(int)(processed*17.0/TOTAL_GAMES_GROUP40);
                cb.update("Gerando 658.008 jogos",processed,TOTAL_GAMES_GROUP40,Math.min(95,pct),"Passaram no filtro: "+classified);
            }
        }
        out.totalClassified=classified;

        ArrayList<Candidate> finalists=new ArrayList<>(heap);
        if(cb!=null) cb.update("Perímetro de duque",0,finalists.size(),96,"Avaliando finalistas mais fortes");
        int k=0;
        for(Candidate cand:finalists){
            evaluateDuque(cand,base);
            // Prioriza duque próximo/moderado e presença repetida de duques na vizinhança.
            double duq=0;
            if(cand.nearestDuqueGap>0) duq += 28.0/(1.0+Math.abs(cand.nearestDuqueGap-8)/8.0);
            duq += Math.min(12,cand.duques30)*1.4;
            duq += Math.min(30,cand.duques100)*0.25;
            cand.finalScore=cand.baseScore+duq;
            k++;
            if(cb!=null && (k%50==0 || k==finalists.size())) cb.update("Perímetro de duque",k,finalists.size(),96+(int)(k*4.0/Math.max(1,finalists.size())),"Finalizando ranking");
        }
        finalists.sort((x,y)->{
            int q=Double.compare(y.finalScore,x.finalScore); if(q!=0)return q;
            q=Double.compare(y.baseScore,x.baseScore); if(q!=0)return q;
            for(int i=0;i<5;i++){if(x.game[i]!=y.game[i])return Integer.compare(x.game[i],y.game[i]);}
            return 0;
        });
        out.top20=new ArrayList<>();
        for(int i=0;i<Math.min(20,finalists.size());i++) out.top20.add(finalists.get(i));
        out.best=out.top20.isEmpty()?null:out.top20.get(0);
        if(cb!=null) cb.update("Concluído",1,1,100,out.best==null?"Nenhum jogo passou no filtro":"Melhor jogo encontrado");
        return out;
    }

    private static boolean passes(Metrics m, FilterConfig f){
        return m.rep>=f.repMin && m.rep<=f.repMax &&
                m.even>=f.evenMin && m.even<=f.evenMax &&
                m.primes>=f.primeMin && m.primes<=f.primeMax &&
                m.fib>=f.fibMin && m.fib<=f.fibMax &&
                m.low>=f.lowMin && m.low<=f.lowMax &&
                m.sum>=f.sumMin && m.sum<=f.sumMax;
    }

    public static Metrics metrics(int[] game, int[] last){
        Metrics m=new Metrics();
        HashSet<Integer> prev=new HashSet<>(); for(int n:last)prev.add(n);
        for(int n:game){
            if(prev.contains(n))m.rep++;
            if(n%2==0)m.even++; else m.odd++;
            if(PRIMES[n])m.primes++;
            if(FIB[n])m.fib++;
            if(n<=40)m.low++; else m.high++;
            m.sum+=n;
        }
        return m;
    }

    private static NumberInfo[] buildNumberMap(BaseData base){
        NumberInfo[] arr=new NumberInfo[81];
        int N=base.draws.size();
        for(int n=1;n<=80;n++){
            NumberInfo x=new NumberInfo(); x.n=n;
            x.hist=rate(n,base.draws,0,N);
            x.r100=rate(n,base.draws,Math.max(0,N-100),N);
            x.r50=rate(n,base.draws,Math.max(0,N-50),N);
            x.r20=rate(n,base.draws,Math.max(0,N-20),N);
            x.weighted30=weightedRate(n,base.draws,Math.max(0,N-30),N);
            double prior20=rate(n,base.draws,Math.max(0,N-40),Math.max(0,N-20));
            x.trend=x.r20-prior20;
            x.delay=currentDelay(n,base.draws);
            x.delayMean=meanDelay(n,base.draws);
            x.pressure=x.delay/Math.max(1.0,x.delayMean);
            arr[n]=x;
        }
        double[] hist=norm(arr,z->z.hist), r100=norm(arr,z->z.r100), r50=norm(arr,z->z.r50), r20=norm(arr,z->z.r20), pond=norm(arr,z->z.weighted30), trend=norm(arr,z->z.trend), press=norm(arr,z->Math.min(3,z.pressure));
        for(int n=1;n<=80;n++){
            arr[n].strength=100*(0.12*hist[n]+0.15*r100[n]+0.15*r50[n]+0.20*r20[n]+0.18*pond[n]+0.08*trend[n]+0.12*press[n]);
        }
        return arr;
    }

    private interface Extract {double get(NumberInfo x);}
    private static double[] norm(NumberInfo[] a, Extract e){
        double lo=Double.POSITIVE_INFINITY,hi=Double.NEGATIVE_INFINITY;
        for(int n=1;n<=80;n++){double v=e.get(a[n]);lo=Math.min(lo,v);hi=Math.max(hi,v);}
        double[] out=new double[81];
        if(Math.abs(hi-lo)<1e-12){for(int n=1;n<=80;n++)out[n]=.5;return out;}
        for(int n=1;n<=80;n++)out[n]=(e.get(a[n])-lo)/(hi-lo);
        return out;
    }

    private static double rate(int n,List<int[]> d,int s,int e){
        if(e<=s)return 0; int c=0; for(int i=s;i<e;i++)if(contains5(d.get(i),n))c++;return c/(double)(e-s);
    }
    private static double weightedRate(int n,List<int[]> d,int s,int e){
        if(e<=s)return 0;double num=0,den=0;int w=1;for(int i=s;i<e;i++,w++){den+=w;if(contains5(d.get(i),n))num+=w;}return num/den;
    }
    private static int currentDelay(int n,List<int[]> d){int a=0;for(int i=d.size()-1;i>=0;i--){if(contains5(d.get(i),n))break;a++;}return a;}
    private static double meanDelay(int n,List<int[]> d){
        ArrayList<Integer> gaps=new ArrayList<>();int last=-1;
        for(int i=0;i<d.size();i++)if(contains5(d.get(i),n)){if(last>=0)gaps.add(i-last-1);last=i;}
        if(gaps.isEmpty())return d.size();long s=0;for(int g:gaps)s+=g;return s/(double)gaps.size();
    }

    public static Set<Integer> cycleAbsent(List<int[]> draws){
        HashSet<Integer> seen=new HashSet<>();
        for(int[] d:draws){
            for(int n:d)seen.add(n);
            if(seen.size()==80)seen.clear();
        }
        TreeSet<Integer> out=new TreeSet<>();for(int n=1;n<=80;n++)if(!seen.contains(n))out.add(n);return out;
    }

    private static void evaluateDuque(Candidate c,BaseData base){
        int nearest=0,d30=0,d100=0,N=base.draws.size();
        HashSet<Integer> g=new HashSet<>();for(int n:c.game)g.add(n);
        for(int i=N-1;i>=0;i--){
            int hits=0;for(int n:base.draws.get(i))if(g.contains(n))hits++;
            int gap=N-i;
            if(hits==2){
                if(nearest==0)nearest=gap;
                if(gap<=30)d30++;
                if(gap<=100)d100++;
            }
            if(gap>100 && nearest>0) break;
        }
        c.nearestDuqueGap=nearest;c.duques30=d30;c.duques100=d100;
    }

    public static String fmt(int[] a){
        if(a==null)return "-";StringBuilder s=new StringBuilder();for(int i=0;i<a.length;i++){if(i>0)s.append(' ');s.append(String.format(Locale.US,"%02d",a[i]));}return s.toString();
    }
    public static String fmtSet(Set<Integer> s){
        StringBuilder b=new StringBuilder();int i=0;for(int n:s){if(i++>0)b.append(' ');b.append(String.format(Locale.US,"%02d",n));}return b.toString();
    }

    public static String groupReport(Group40Result g){
        return "GRUPO 40 FIXADO\n"+
                fmt(g.group40)+"\n\n"+
                "Quinas históricas contidas: "+g.hits5+"\n"+
                "Último 5/5 dentro do grupo: "+(g.lastHitContest==null?"nenhum":g.lastHitContest)+"\n"+
                "Falhou agora: "+g.currentDelay+" concurso(s) seguido(s)\n"+
                "Intervalo médio: "+String.format(Locale.US,"%.2f",g.meanInterval)+"\n"+
                "Maior intervalo: "+g.maxInterval+"\n"+
                "Pressão atual: "+String.format(Locale.US,"%.3f",g.pressure)+"\n"+
                "Reinícios da busca: "+g.restarts+"\n"+
                "Tempo: "+String.format(Locale.US,"%.1f",g.elapsedMs/1000.0)+" s";
    }

    public static String gameReport(BaseData base, Group40Result group, GameResult r){
        StringBuilder s=new StringBuilder();
        s.append("QUINA CAMPEÃ - GRUPO 40\n");
        s.append("========================================\n");
        s.append("Base: ").append(base.draws.size()).append(" concursos\n");
        s.append("Último: ").append(base.contests.get(base.contests.size()-1)).append(" = ").append(fmt(base.last)).append("\n\n");
        s.append(groupReport(group)).append("\n\n");
        s.append(r.filter.label).append("\n").append(r.filter.summary()).append("\n\n");
        s.append("C(40,5) = ").append(TOTAL_GAMES_GROUP40).append("\n");
        s.append("Classificados: ").append(r.totalClassified).append("\n");
        s.append("Ausentes do ciclo: ").append(fmtSet(r.cycleAbsent)).append("\n\n");
        if(r.best==null){s.append("Nenhum jogo passou no filtro.\n");return s.toString();}
        Candidate b=r.best;
        s.append("MELHOR JOGO\n>>> ").append(fmt(b.game)).append(" <<<\n\n");
        s.append("Repetidas: ").append(b.metrics.rep).append("\n");
        s.append("Pares/Ímpares: ").append(b.metrics.even).append('/').append(b.metrics.odd).append("\n");
        s.append("Primos: ").append(b.metrics.primes).append(" | Fibonacci: ").append(b.metrics.fib).append("\n");
        s.append("01-40 / 41-80: ").append(b.metrics.low).append('/').append(b.metrics.high).append("\n");
        s.append("Soma: ").append(b.metrics.sum).append("\n");
        s.append("Duque mais próximo: ").append(b.nearestDuqueGap).append(" concurso(s) atrás\n");
        s.append("Duques últimos 30/100: ").append(b.duques30).append('/').append(b.duques100).append("\n");
        s.append("Ausentes do ciclo no jogo: ").append(b.cycleAbsentCount).append("\n");
        s.append("Score final: ").append(String.format(Locale.US,"%.3f",b.finalScore)).append("\n");
        return s.toString();
    }
}

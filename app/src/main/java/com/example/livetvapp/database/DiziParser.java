package com.example.livetvapp.database;
import com.example.livetvapp.Adapter.Bolum;
import com.example.livetvapp.Adapter.Dizi;
import com.example.livetvapp.Channel;
import com.example.livetvapp.Adapter.Sezon;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
public class DiziParser {
    private static final Pattern PATTERN_S_E = Pattern.compile(
            "(.*?)[\\s\\-]*[Ss](\\d{1,2})[\\s]*[Ee](\\d{1,3})[\\s\\-]*(.*)",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern PATTERN_SEZON_BOLUM = Pattern.compile(
            "(.*?)[\\s\\-]*[Ss]ezon[\\s]*(\\d+)[\\s\\-]*[Bb]ölüm[\\s]*(\\d+)[\\s\\-]*(.*)",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern PATTERN_TRAILING = Pattern.compile("[\\-_]+$");
    public static List<Dizi> parseM3UForDiziler(List<Channel> kanallar) {
        System.out.println("=== DİZİ PARSING BAŞLIYOR ===");
        if (kanallar == null || kanallar.isEmpty()) {
            System.out.println("⚠️ Kanal listesi boş");
            return new ArrayList<>();
        }
        Map<String, Dizi>                diziMap  = new LinkedHashMap<>();
        Map<String, Map<Integer, Sezon>> sezonMap = new HashMap<>();
        for (Channel channel : kanallar) {
            if (!"SERIES".equals(channel.getContentType())) continue;
            String kanalAdi = channel.getName();
            if (kanalAdi == null || kanalAdi.isEmpty()) continue;
            DiziBilgisi bilgi = extractDiziBilgisi(kanalAdi);
            if (bilgi.getDiziAdi() == null || bilgi.getDiziAdi().isEmpty()) continue;
            Dizi dizi = diziMap.get(bilgi.getDiziAdi());
            if (dizi == null) {
                dizi = new Dizi(bilgi.getDiziAdi(), channel.getLogo(), channel.getCategory());
                diziMap.put(bilgi.getDiziAdi(), dizi);
                sezonMap.put(bilgi.getDiziAdi(), new HashMap<>());
            }
            Map<Integer, Sezon> diziSezonMap = sezonMap.get(bilgi.getDiziAdi());
            Sezon sezon = diziSezonMap.get(bilgi.getSezonNo());
            if (sezon == null) {
                sezon = new Sezon(bilgi.getSezonNo(), "Sezon " + bilgi.getSezonNo());
                diziSezonMap.put(bilgi.getSezonNo(), sezon);
                dizi.sezonEkle(sezon);
            }
            sezon.bolumEkle(new Bolum(bilgi.getBolumNo(), bilgi.getBolumAdi(), channel.getUrl()));
        }
        List<Dizi> diziler = new ArrayList<>(diziMap.values());
        for (Dizi dizi : diziler) {
            Collections.sort(dizi.getSezonlar(), new Comparator<Sezon>() {
                @Override
                public int compare(Sezon a, Sezon b) {
                    return Integer.compare(a.getSezonNo(), b.getSezonNo());
                }
            });
            for (Sezon sezon : dizi.getSezonlar()) {
                Collections.sort(sezon.getBolumler(), new Comparator<Bolum>() {
                    @Override
                    public int compare(Bolum a, Bolum b) {
                        return Integer.compare(a.getBolumNo(), b.getBolumNo());
                    }
                });
            }
        }
        System.out.println("=== DİZİ PARSING TAMAMLANDI: " + diziler.size() + " dizi ===");
        return diziler;
    }
    public static List<Dizi> parseWithCache(String kategori, List<Channel> kanallar) {
        if (kategori != null) {
            List<Dizi> cached = DiziCache.getInstance().get(kategori);
            if (cached != null) return cached;
        }
        List<Dizi> diziler = parseM3UForDiziler(kanallar);
        if (kategori != null) {
            DiziCache.getInstance().put(kategori, diziler);
        }
        return diziler;
    }
    public static DiziBilgisi extractDiziBilgisi(String kanalAdi) {
        DiziBilgisi bilgi = new DiziBilgisi();
        String temiz = kanalAdi.trim();
        Matcher m1 = PATTERN_S_E.matcher(temiz);
        if (m1.find()) {
            bilgi.diziAdi = m1.group(1);
            bilgi.sezonNo = parseIntSafe(m1.group(2), 1);
            bilgi.bolumNo = parseIntSafe(m1.group(3), 1);
            String rest   = m1.groupCount() >= 4 ? nullToEmpty(m1.group(4)) : "";
            if (!rest.isEmpty() && !rest.matches("\\d+")) bilgi.bolumAdi = rest;
        } else {
            Matcher m2 = PATTERN_SEZON_BOLUM.matcher(temiz);
            if (m2.find()) {
                bilgi.diziAdi = m2.group(1);
                bilgi.sezonNo = parseIntSafe(m2.group(2), 1);
                bilgi.bolumNo = parseIntSafe(m2.group(3), 1);
                String rest   = m2.groupCount() >= 4 ? nullToEmpty(m2.group(4)) : "";
                if (!rest.isEmpty()) bilgi.bolumAdi = rest;
            } else {
                bilgi.diziAdi = temiz;
                bilgi.sezonNo = 1;
                bilgi.bolumNo = 1;
            }
        }
        if (bilgi.diziAdi != null) {
            bilgi.diziAdi = PATTERN_TRAILING.matcher(bilgi.diziAdi.trim()).replaceAll("").trim();
            if (!bilgi.diziAdi.isEmpty()) {
                bilgi.diziAdi = Character.toUpperCase(bilgi.diziAdi.charAt(0))
                        + bilgi.diziAdi.substring(1);
            }
        }
        if (bilgi.bolumAdi == null || bilgi.bolumAdi.isEmpty()) {
            bilgi.bolumAdi = bilgi.bolumNo + ". Bölüm";
        }
        return bilgi;
    }
    private static int parseIntSafe(String s, int def) {
        try { return Integer.parseInt(s); } catch (Exception e) { return def; }
    }
    private static String nullToEmpty(String s) {
        return s != null ? s.trim() : "";
    }
    public static class DiziBilgisi {
        private String diziAdi;
        private int    sezonNo  = 1;
        private int    bolumNo  = 1;
        private String bolumAdi;
        public String getDiziAdi()  { return diziAdi; }
        public int    getSezonNo()  { return sezonNo; }
        public int    getBolumNo()  { return bolumNo; }
        public String getBolumAdi() { return bolumAdi; }
    }
}
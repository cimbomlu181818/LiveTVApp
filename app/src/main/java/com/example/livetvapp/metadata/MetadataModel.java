package com.example.livetvapp.metadata;
import java.util.List;
public class MetadataModel {
    private String tmdbId;
    private String imdbId;
    private String baslik;
    private String orijinalBaslik;
    private String konu;
    private String posterUrl;
    private String backdropUrl;
    private String yil;
    private String sure;        
    private String ulke;
    private String yonetmen;
    private String imdbPuan;
    private String tmdbPuan;
    private String yasSiniri;
    private String sesDili;
    private String icerikTipi;
    private List<String> turler;
    private List<String> altyaziDilleri;
    private List<Oyuncu> oyuncular;
    public static class Oyuncu {
        private String ad;
        private String karakter;
        private String fotoUrl;
        public Oyuncu(String ad, String karakter, String fotoUrl) {
            this.ad        = ad;
            this.karakter  = karakter;
            this.fotoUrl   = fotoUrl;
        }
        public String getAd()       { return ad; }
        public String getFotoUrl()  { return fotoUrl; }
    }
    public MetadataModel() {}
    public void setTmdbId(String v)     { this.tmdbId = v; }
    public void setImdbId(String v)     { this.imdbId = v; }
    public String getBaslik()           { return baslik; }
    public void setBaslik(String v)     { this.baslik = v; }
    public String getOrijinalBaslik()         { return orijinalBaslik; }
    public void setOrijinalBaslik(String v)   { this.orijinalBaslik = v; }
    public String getKonu()             { return konu; }
    public void setKonu(String v)       { this.konu = v; }
    public void setPosterUrl(String v)  { this.posterUrl = v; }
    public void setBackdropUrl(String v)    { this.backdropUrl = v; }
    public String getYil()              { return yil; }
    public void setYil(String v)        { this.yil = v; }
    public String getSure()             { return sure; }
    public void setSure(String v)       { this.sure = v; }
    public String getUlke()             { return ulke; }
    public void setUlke(String v)       { this.ulke = v; }
    public String getYonetmen()             { return yonetmen; }
    public void setYonetmen(String v)       { this.yonetmen = v; }
    public String getImdbPuan()             { return imdbPuan; }
    public void setImdbPuan(String v)       { this.imdbPuan = v; }
    public String getTmdbPuan()             { return tmdbPuan; }
    public void setTmdbPuan(String v)       { this.tmdbPuan = v; }
    public String getYasSiniri()            { return yasSiniri; }
    public void setYasSiniri(String v)      { this.yasSiniri = v; }
    public void setSesDili(String v)        { this.sesDili = v; }
    public void setIcerikTipi(String v)     { this.icerikTipi = v; }
    public void setTurler(List<String> v)       { this.turler = v; }
    public List<String> getAltyaziDilleri()         { return altyaziDilleri; }
    public void setAltyaziDilleri(List<String> v)   { this.altyaziDilleri = v; }
    public List<Oyuncu> getOyuncular()          { return oyuncular; }
    public void setOyuncular(List<Oyuncu> v)    { this.oyuncular = v; }
    public String getPosterTamUrl() {
        if (posterUrl == null || posterUrl.isEmpty()) return null;
        if (posterUrl.startsWith("http")) return posterUrl;
        return "https://image.tmdb.org/t/p/w300" + posterUrl;
    }
    public String getBackdropTamUrl() {
        if (backdropUrl == null || backdropUrl.isEmpty()) return null;
        if (backdropUrl.startsWith("http")) return backdropUrl;
        return "https://image.tmdb.org/t/p/w780" + backdropUrl;
    }
    public String getTurlerMetni() {
        if (turler == null || turler.isEmpty()) return null;
        return String.join(" · ", turler);
    }
}
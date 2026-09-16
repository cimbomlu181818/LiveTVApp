package com.example.livetvapp.Adapter;
public class Bolum {
    private int bolumNo;
    private String ad;
    private String url;
    public Bolum(int bolumNo, String ad, String url) {
        this.bolumNo = bolumNo;
        this.ad = ad;
        this.url = url;
    }
    public int getBolumNo()       { return bolumNo; }
    public String getAd()         { return ad; }
    public String getUrl()        { return url; }
    public String getTamAd() {
        if (ad != null && !ad.isEmpty() && !ad.matches("\\d+\\.\\s*Bölüm.*")) {
            return bolumNo + ". Bölüm: " + ad;
        }
        return bolumNo + ". Bölüm";
    }


}
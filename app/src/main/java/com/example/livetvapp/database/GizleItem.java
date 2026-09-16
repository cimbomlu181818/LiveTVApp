package com.example.livetvapp.database;
public class GizleItem {
    public static final int TIP_M3U      = 0;
    public static final int TIP_KATEGORI = 1;
    public static final int TIP_KANAL    = 2;
    private final int     tip;
    private final String  ad;          
    private final String  rawAd;       
    private final String  m3uAdi;      
    private final String  kategoriAdi; 
    private final String  url;         
    private       boolean gizli;
    private       boolean acik;        
    public static GizleItem m3u(String ad, boolean gizli) {
        return new GizleItem(TIP_M3U, ad, ad, ad, null, null, gizli, false);
    }
    public static GizleItem kategori(String adGoster, String rawAd, String m3uAdi, boolean gizli) {
        return new GizleItem(TIP_KATEGORI, adGoster, rawAd, m3uAdi, adGoster, null, gizli, false);
    }
    public static GizleItem kategori(String ad, String m3uAdi, boolean gizli) {
        return new GizleItem(TIP_KATEGORI, ad, ad, m3uAdi, ad, null, gizli, false);
    }
    public static GizleItem kanal(String ad, String m3uAdi, String kategoriAdi,
                                  String url, boolean gizli) {
        return new GizleItem(TIP_KANAL, ad, ad, m3uAdi, kategoriAdi, url, gizli, false);
    }
    private GizleItem(int tip, String ad, String rawAd, String m3uAdi, String kategoriAdi,
                      String url, boolean gizli, boolean acik) {
        this.tip         = tip;
        this.ad          = ad;
        this.rawAd       = rawAd != null ? rawAd : ad;
        this.m3uAdi      = m3uAdi;
        this.kategoriAdi = kategoriAdi;
        this.url         = url;
        this.gizli       = gizli;
        this.acik        = acik;
    }
    public int     getTip()         { return tip; }
    public String  getAd()          { return ad; }
    public String  getRawAd()       { return rawAd; }
    public String  getM3uAdi()      { return m3uAdi; }
    public String  getKategoriAdi() { return kategoriAdi; }
    public String  getUrl()         { return url; }
    public boolean isGizli()        { return gizli; }
    public boolean isAcik()         { return acik; }
    public void setGizli(boolean gizli) { this.gizli = gizli; }
    public void setAcik(boolean acik)   { this.acik  = acik; }
}
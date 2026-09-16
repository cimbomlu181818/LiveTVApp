package com.example.livetvapp.database;
public class SiralamaItem {
    public static final int TIP_TUR      = 0;
    public static final int TIP_KATEGORI = 1;
    public static final int TIP_KANAL    = 2;
    public static final int TIP_DIZI     = 3;
    private int    tip;
    private String ad;
    private String tur;
    private String kategoriAdi;
    private String m3uAdi;
    private String url;
    private long   kanalId;
    private int    pozisyon;
    private int    siraNo;
    private int    sezonSayisi;
    private boolean secili = false;
    private boolean acik   = false;
    public static SiralamaItem turOlustur(String ad, String tur) {
        SiralamaItem item = new SiralamaItem();
        item.tip = TIP_TUR;
        item.ad  = ad;
        item.tur = tur;
        return item;
    }
    public static SiralamaItem kategoriOlustur(String ad, String tur, String m3uAdi) {
        SiralamaItem item = new SiralamaItem();
        item.tip       = TIP_KATEGORI;
        item.ad        = ad;
        item.tur       = tur;
        item.m3uAdi    = m3uAdi;
        return item;
    }
    public static SiralamaItem diziOlustur(String ad, String tur,
                                           String kategoriAdi, int sezonSayisi) {
        SiralamaItem item = new SiralamaItem();
        item.tip         = TIP_DIZI;
        item.ad          = ad;
        item.tur         = tur;
        item.kategoriAdi = kategoriAdi;
        item.sezonSayisi = sezonSayisi;
        return item;
    }
    public static SiralamaItem kanalOlustur(String ad, String url, long kanalId,
                                            int pozisyon, String kategoriAdi, String tur) {
        SiralamaItem item = new SiralamaItem();
        item.tip         = TIP_KANAL;
        item.ad          = ad;
        item.url         = url;
        item.kanalId     = kanalId;
        item.pozisyon    = pozisyon;
        item.kategoriAdi = kategoriAdi;
        item.tur         = tur;
        return item;
    }
    public String getGoruntuMetni() {
        switch (tip) {
            case TIP_TUR:
                return (acik ? "▼ " : "▶ ") + ad;
            case TIP_KATEGORI:
                String m3uEtiket = (m3uAdi != null && !m3uAdi.isEmpty()) ? " (" + m3uAdi + ")" : "";
                return "  " + (acik ? "▼ " : "▶ ") + ad + m3uEtiket;
            case TIP_DIZI:
                String sezonEtiket = sezonSayisi > 0 ? "  (" + sezonSayisi + " Sezon)" : "";
                return "    " + ad + sezonEtiket;
            case TIP_KANAL:
                return "    " + siraNo + " - " + ad;
            default:
                return ad;
        }
    }
    public int     getTip()              { return tip; }
    public String  getAd()               { return ad; }
    public void    setAd(String ad)      { this.ad = ad; }
    public String  getTur()              { return tur; }
    public String  getKategoriAdi()      { return kategoriAdi; }
    public String  getM3uAdi()           { return m3uAdi; }
    public String  getUrl()              { return url; }
    public long    getKanalId()          { return kanalId; }
    public int     getPozisyon()         { return pozisyon; }
    public void    setPozisyon(int p)    { this.pozisyon = p; }
    public int     getSiraNo()            { return siraNo; }
    public void    setSiraNo(int siraNo)  { this.siraNo = siraNo; }
    public int     getSezonSayisi()      { return sezonSayisi; }
    public boolean isSecili()            { return secili; }
    public void    setSecili(boolean s)  { this.secili = s; }
    public boolean isAcik()              { return acik; }
    public void    setAcik(boolean a)    { this.acik = a; }
}
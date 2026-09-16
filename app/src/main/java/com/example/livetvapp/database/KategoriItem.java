package com.example.livetvapp.database;
public class KategoriItem {
    private final String kategoriAdi;
    private final String kaynakAdi;   
    public KategoriItem(String kategoriAdi, String kaynakAdi) {
        this.kategoriAdi = kategoriAdi != null ? kategoriAdi : "";
        this.kaynakAdi   = kaynakAdi   != null ? kaynakAdi   : "";
    }
    public String getKategoriAdi() { return kategoriAdi; }
    public String getKaynakAdi()   { return kaynakAdi; }
    public String getGorunumMetni() {
        if (kaynakAdi.isEmpty()) return kategoriAdi;
        return kategoriAdi + "\n(" + kaynakAdi + ")";
    }
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof KategoriItem)) return false;
        KategoriItem other = (KategoriItem) o;
        return kategoriAdi.equals(other.kategoriAdi) && kaynakAdi.equals(other.kaynakAdi);
    }
    @Override
    public int hashCode() {
        return 31 * kategoriAdi.hashCode() + kaynakAdi.hashCode();
    }
    @Override
    public String toString() {
        return "KategoriItem{" + kategoriAdi + " / " + kaynakAdi + "}";
    }
}
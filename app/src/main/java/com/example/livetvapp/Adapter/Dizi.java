package com.example.livetvapp.Adapter;
import java.util.ArrayList;
import java.util.List;
public class Dizi {
    private String ad;
    private String logo;
    private String kategori;
    private List<Sezon> sezonlar;
    public Dizi(String ad, String logo, String kategori) {
        this.ad       = ad;
        this.logo     = logo;
        this.kategori = kategori;
        this.sezonlar = new ArrayList<>();
    }
    public void sezonEkle(Sezon sezon) { sezonlar.add(sezon); }
    public String getAd()              { return ad; }
    public String getKategori()        { return kategori; }
    public List<Sezon> getSezonlar()   { return sezonlar; }
    public void setSezonlar(List<Sezon> sezonlar) {
        this.sezonlar = sezonlar != null ? sezonlar : new ArrayList<>();
    }
}
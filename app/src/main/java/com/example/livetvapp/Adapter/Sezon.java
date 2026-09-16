package com.example.livetvapp.Adapter;
import java.util.ArrayList;
import java.util.List;
public class Sezon {
    private int sezonNo;
    private String ad;
    private List<Bolum> bolumler;
    public Sezon(int sezonNo, String ad) {
        this.sezonNo  = sezonNo;
        this.ad       = ad;
        this.bolumler = new ArrayList<>();
    }
    public void bolumEkle(Bolum bolum) { bolumler.add(bolum); }
    public int getSezonNo()         { return sezonNo; }
    public String getAd()           { return ad; }
    public List<Bolum> getBolumler() { return bolumler; }
}
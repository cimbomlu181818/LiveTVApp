package com.example.livetvapp.database;
import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.ColumnInfo;
import androidx.annotation.NonNull;
import java.util.Date;
@Entity(tableName = "m3u_items")
public class M3UItem {
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    private long id;
    @NonNull
    @ColumnInfo(name = "name")
    private String name;
    @ColumnInfo(name = "filePath")
    private String filePath;
    @ColumnInfo(name = "url")
    private String url;
    @ColumnInfo(name = "channelCount")
    private int channelCount;
    @ColumnInfo(name = "addedDate")
    private Date addedDate;
    @ColumnInfo(name = "isLocalFile")
    private boolean isLocalFile;
    @ColumnInfo(name = "isHidden")
    private boolean isHidden = false;
    public M3UItem() {
        this.name      = "";
        this.addedDate = new Date();
        this.isHidden  = false;
    }
    public M3UItem(@NonNull String name, String filePath, String url,
                   int channelCount, boolean isLocalFile) {
        this.name         = name;
        this.filePath     = filePath;
        this.url          = url;
        this.channelCount = channelCount;
        this.addedDate    = new Date();
        this.isLocalFile  = isLocalFile;
        this.isHidden     = false;
    }
    public long getId()               { return id; }
    public void setId(long id)        { this.id = id; }
    @NonNull
    public String getName()           { return name; }
    public void setName(@NonNull String name) { this.name = name; }
    public String getFilePath()       { return filePath; }
    public void setFilePath(String filePath) { this.filePath = filePath; }
    public String getUrl()            { return url; }
    public void setUrl(String url)    { this.url = url; }
    public int getChannelCount()      { return channelCount; }
    public void setChannelCount(int channelCount) { this.channelCount = channelCount; }
    public Date getAddedDate()        { return addedDate; }
    public void setAddedDate(Date addedDate) { this.addedDate = addedDate; }
    public boolean isLocalFile()      { return isLocalFile; }
    public void setLocalFile(boolean localFile) { isLocalFile = localFile; }
    public boolean isHidden()         { return isHidden; }
    public void setHidden(boolean hidden) { isHidden = hidden; }
    @Override
    public String toString() {
        return name + " (" + channelCount + " kanal)" + (isHidden ? " [GİZLİ]" : "");
    }
}
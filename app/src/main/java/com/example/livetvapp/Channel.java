package com.example.livetvapp;
import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.ColumnInfo;
import androidx.annotation.NonNull;
@Entity(tableName = "channels")
public class Channel {
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    private long id;
    @NonNull
    @ColumnInfo(name = "name")
    private String name;
    @NonNull
    @ColumnInfo(name = "url")
    private String url;
    @ColumnInfo(name = "category")
    private String category;
    @ColumnInfo(name = "logo")
    private String logo;
    @NonNull
    @ColumnInfo(name = "sourceName")
    private String sourceName;
    @ColumnInfo(name = "contentType")
    private String contentType; 
    @ColumnInfo(name = "position")
    private int position;
    @ColumnInfo(name = "tmdbId")
    private String tmdbId;
    public Channel() {
        this.name = "";
        this.url = "";
        this.sourceName = "Default";
        this.contentType = "LIVE";
    }
    public Channel(@NonNull String name, @NonNull String url,
                   String category, String logo) {
        this.name = name;
        this.url = url;
        this.category = category;
        this.logo = logo;
        this.sourceName = "Default";
        this.contentType = detectContentTypeStatic(url, null);
    }
    public Channel(@NonNull String name, @NonNull String url,
                   String category, String logo,
                   @NonNull String sourceName) {
        this.name = name;
        this.url = url;
        this.category = category;
        this.logo = logo;
        this.sourceName = sourceName != null ? sourceName : "Default";
        this.contentType = detectContentTypeStatic(url, null);
    }
    public static String detectContentTypeStatic(String url, String extinfLine) {
        if (url == null || url.trim().isEmpty()) {
            return "LIVE";
        }
        String lowerUrl = url.toLowerCase().trim();
        String urlNoParams = lowerUrl.split("\\?")[0].split("#")[0];
        String[] parts = urlNoParams.split("/");
        for (int i = parts.length - 1; i >= 0; i--) {
            String part = parts[i].trim();
            if (!part.isEmpty()) {
                if (part.matches("\\d+")) {
                    return "LIVE";
                }
                break; 
            }
        }
        if (lowerUrl.contains("/movie/")
                || lowerUrl.contains("/movies/")
                || lowerUrl.contains("/vod/")
                || lowerUrl.contains("/film/")
                || lowerUrl.contains("/films/")) {
            return "MOVIE";
        }
        if (lowerUrl.contains("/series/")
                || lowerUrl.contains("/serie/")
                || lowerUrl.contains("/show/")
                || lowerUrl.contains("/shows/")
                || lowerUrl.contains("/tvshow/")) {
            return "SERIES";
        }
        if (extinfLine != null && !extinfLine.isEmpty()) {
            String tvgType = extractAttributeValue(extinfLine, "tvg-type");
            if (tvgType != null) {
                String lt = tvgType.toLowerCase().trim();
                if (lt.equals("movie") || lt.equals("vod") || lt.equals("film"))
                    return "MOVIE";
                if (lt.equals("series") || lt.equals("serie") || lt.equals("show"))
                    return "SERIES";
                if (lt.equals("live") || lt.equals("livetv"))
                    return "LIVE";
            }
        }
        if (extinfLine != null && !extinfLine.isEmpty()) {
            String groupTitle = extractAttributeValue(extinfLine, "group-title");
            if (groupTitle != null) {
                String lt = groupTitle.toLowerCase();
                if (lt.contains("film")
                        || lt.contains("movie")
                        || lt.contains("vod")
                        || lt.contains("sinema")
                        || lt.contains("cinema")) {
                    return "MOVIE";
                }
                if (lt.contains("dizi")
                        || lt.contains("series")
                        || lt.contains("serie")
                        || lt.contains("show")
                        || lt.contains("serial")
                        || lt.contains("diziler")) {
                    return "SERIES";
                }
            }
        }
        if (urlNoParams.endsWith(".mp4")) {
            return "MOVIE";
        }
        if (urlNoParams.endsWith(".mkv")) {
            return "SERIES";
        }
        return "LIVE";
    }
    private static String extractAttributeValue(String line, String attrName) {
        try {
            String search = attrName + "=\"";
            int start = line.indexOf(search);
            if (start == -1) return null;
            start += search.length();
            int end = line.indexOf("\"", start);
            if (end == -1) return null;
            String val = line.substring(start, end).trim();
            return val.isEmpty() ? null : val;
        } catch (Exception e) {
            return null;
        }
    }
    public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    @NonNull
    public String getName() { return name; }
    public void setName(@NonNull String name) { this.name = name; }
    @NonNull
    public String getUrl() { return url; }
    public void setUrl(@NonNull String url) { this.url = url; }
    public String getCategory() {
        return category != null ? category : "Diğer";
    }
    public void setCategory(String category) {
        this.category = category;
    }
    public String getLogo() { return logo; }
    public void setLogo(String logo) { this.logo = logo; }
    @NonNull
    public String getSourceName() {
        return sourceName != null ? sourceName : "Default";
    }
    public void setSourceName(@NonNull String sourceName) {
        this.sourceName = sourceName != null ? sourceName : "Default";
    }
    public String getContentType() {
        return contentType != null ? contentType : "LIVE";
    }
    public void setContentType(String contentType) {
        this.contentType = contentType;
    }
    public int getPosition() { return position; }
    public void setPosition(int position) { this.position = position; }
    public String getTmdbId() { return tmdbId; }
    public void setTmdbId(String tmdbId) { this.tmdbId = tmdbId; }
}
package com.example.livetvapp;
import android.content.Context;
import android.content.SharedPreferences;
import com.example.livetvapp.database.AppDatabase;
import com.example.livetvapp.database.DatabaseHelper;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;
public class KanalListesi {
    private static final String CHANNEL_CACHE_KEY = "cached_channels";
    private static final String PREFS_NAME = "ChannelCache";
    private static final String M3U_CHANNEL_PREFIX = "channels_m3u_";
    public static void saveChannelsForM3U(Context context, List<Channel> channels, String m3uName) {
        try {
            
            AppDatabase db = AppDatabase.getInstance(context);
            db.channelDao().deleteChannelsByM3U(m3uName);
            System.out.println("🗑️ Eski kanallar silindi: " + m3uName);

            
            SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = prefs.edit();
            Gson gson = new Gson();
            String json = gson.toJson(channels);
            String key = M3U_CHANNEL_PREFIX + m3uName.hashCode();
            editor.putString(key, json);
            editor.putString(CHANNEL_CACHE_KEY, json);
            editor.apply();

            
            DatabaseHelper dbHelper = new DatabaseHelper(context);
            dbHelper.insertChannels(channels, new DatabaseHelper.OnCompleteListener() {
                @Override
                public void onSuccess() {
                    System.out.println("✅ Kanallar database'e kaydedildi: " + channels.size());
                    dbHelper.updateChannelCount(m3uName, channels.size(), new DatabaseHelper.OnCompleteListener() {
                        @Override public void onSuccess() { System.out.println("✅ M3U kanal sayısı güncellendi"); }
                        @Override public void onError(Exception e) { System.out.println("❌ " + e.getMessage()); }
                    });
                }
                @Override
                public void onError(Exception e) {
                    System.out.println("❌ Database kayıt hatası: " + e.getMessage());
                    e.printStackTrace();
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    public static void clearAllM3UChannels(Context context) {
        try {
            SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = prefs.edit();
            Map<String, ?> allPrefs = prefs.getAll();
            for (String key : allPrefs.keySet()) {
                if (key.startsWith(M3U_CHANNEL_PREFIX)) editor.remove(key);
            }
            editor.remove(CHANNEL_CACHE_KEY);
            editor.apply();
            AppDatabase database = AppDatabase.getInstance(context);
            new Thread(() -> {
                database.channelDao().deleteAll();
                System.out.println("✅ Tüm kanallar database'den silindi");
            }).start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    public static List<Channel> getChannelsFromUrl(String urlString) {
        List<Channel> channels = new ArrayList<>();
        try {
            URL url = new URL(urlString);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(10000);
            connection.setReadTimeout(10000);
            if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                InputStream inputStream = connection.getInputStream();
                BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
                String sourceName = extractSourceNameFromUrl(urlString);
                channels = parseM3UFromReader(reader, sourceName);
                reader.close();
                inputStream.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return channels;
    }
    public static List<Channel> parseM3U(String m3uContent, String sourceName) {
        List<Channel> channels = new ArrayList<>();
        if (m3uContent == null || m3uContent.isEmpty()) return channels;
        String[] lines = m3uContent.split("\n");
        Channel currentChannel = null;
        String currentExtinfLine = null;
        int position = 0;
        for (String line : lines) {
            line = line.trim();
            if (line.startsWith("#EXTINF:")) {
                String name     = extractChannelName(line);
                String logo     = extractLogo(line);
                String category = extractCategory(line);
                currentChannel   = new Channel(name, "", category, logo, sourceName);
                currentExtinfLine = line;
            } else if (!line.isEmpty() && !line.startsWith("#") && currentChannel != null) {
                currentChannel.setUrl(line);
                currentChannel.setPosition(position++);
                String contentType = Channel.detectContentTypeStatic(line, currentExtinfLine);
                currentChannel.setContentType(contentType);
                channels.add(currentChannel);
                currentChannel   = null;
                currentExtinfLine = null;
            }
        }
        return channels;
    }
    public static List<Channel> parseM3UFromReader(BufferedReader reader, String sourceName) throws Exception {
        List<Channel> channels = new ArrayList<>();
        String firstLine = reader.readLine();
        if (firstLine == null || !firstLine.trim().startsWith("#EXTM3U")) {
            return channels; 
        }
        Channel currentChannel = null;
        String currentExtinfLine = null;
        int position = 0;
        String line;
        while ((line = reader.readLine()) != null) {
            line = line.trim();
            if (line.startsWith("#EXTINF:")) {
                String name     = extractChannelName(line);
                String logo     = extractLogo(line);
                String category = extractCategory(line);
                currentChannel    = new Channel(name, "", category, logo, sourceName);
                currentExtinfLine = line;
            } else if (!line.isEmpty() && !line.startsWith("#") && currentChannel != null) {
                currentChannel.setUrl(line);
                currentChannel.setPosition(position++);
                String contentType = Channel.detectContentTypeStatic(line, currentExtinfLine);
                currentChannel.setContentType(contentType);
                channels.add(currentChannel);
                currentChannel    = null;
                currentExtinfLine = null;
            }
        }
        return channels;
    }
    private static String extractChannelName(String extinfLine) {
        try {
            int lastComma = extinfLine.lastIndexOf(",");
            if (lastComma != -1 && lastComma + 1 < extinfLine.length()) {
                return extinfLine.substring(lastComma + 1).trim();
            }
            if (extinfLine.contains("tvg-name=")) {
                int start = extinfLine.indexOf("tvg-name=\"") + 10;
                int end   = extinfLine.indexOf("\"", start);
                if (start != -1 && end != -1 && start < end) return extinfLine.substring(start, end).trim();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "Bilinmeyen Kanal";
    }
    private static String extractLogo(String extinfLine) {
        try {
            if (extinfLine.contains("tvg-logo=")) {
                int start = extinfLine.indexOf("tvg-logo=\"") + 10;
                int end   = extinfLine.indexOf("\"", start);
                if (start != -1 && end != -1 && start < end) return extinfLine.substring(start, end).trim();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }
    private static String extractCategory(String extinfLine) {
        try {
            if (extinfLine.contains("group-title=")) {
                int start = extinfLine.indexOf("group-title=\"") + 13;
                int end   = extinfLine.indexOf("\"", start);
                if (start != -1 && end != -1 && start < end) return extinfLine.substring(start, end).trim();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "Genel";
    }
    private static String extractSourceNameFromUrl(String urlString) {
        try {
            URL url = new URL(urlString);
            String host = url.getHost();
            if (host != null) {
                if (host.startsWith("www.")) host = host.substring(4);
                return host;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "URL Source";
    }
}
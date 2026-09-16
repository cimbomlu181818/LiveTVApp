package com.example.livetvapp.database;
import android.os.AsyncTask;
import com.example.livetvapp.Channel;
import com.example.livetvapp.KanalListesi;
import java.util.List;
public class M3UParser extends AsyncTask<String, Integer, List<Channel>> {
    private final String m3uContent;
    private final String sourceName;
    private final OnParseCompleteListener listener;
    private Exception parseException;
    public M3UParser(String m3uContent, String sourceName, OnParseCompleteListener listener) {
        this.m3uContent = m3uContent;
        this.sourceName = sourceName;
        this.listener = listener;
    }
    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        if (listener != null) {
            listener.onParseStarted();
        }
    }
    @Override
    protected List<Channel> doInBackground(String... params) {
        try {
            List<Channel> channels = KanalListesi.parseM3U(m3uContent, sourceName);
            int totalChannels = channels.size();
            for (int i = 0; i < totalChannels; i++) {
                if (i % 100 == 0) {
                    int progress = (int) ((i / (float) totalChannels) * 100);
                    publishProgress(progress);
                }
            }
            return channels;
        } catch (Exception e) {
            parseException = e;
            return null;
        }
    }
    @Override
    protected void onProgressUpdate(Integer... values) {
        super.onProgressUpdate(values);
        if (listener != null && values.length > 0) {
            listener.onParseProgress(values[0]);
        }
    }
    @Override
    protected void onPostExecute(List<Channel> channels) {
        super.onPostExecute(channels);
        if (parseException != null) {
            if (listener != null) {
                listener.onParseError(parseException);
            }
        } else {
            if (listener != null) {
                listener.onParseComplete(channels);
            }
        }
    }
    public interface OnParseCompleteListener {
        void onParseStarted();
        void onParseProgress(int progress);
        void onParseComplete(List<Channel> channels);
        void onParseError(Exception e);
    }
}
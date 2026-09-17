package com.example.livetvapp;

import android.os.AsyncTask;
import org.json.JSONObject;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.security.MessageDigest;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.List;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509ExtendedTrustManager;
import android.util.Base64;

public class ApiHelper {

    private static final String BASE_URL = "https://153.56.184.247.nip.io/api/";

    // Sunucu sertifikasının ve yedek (ara CA) sertifikasının SHA-256 SPKI pin'leri.
    // Sertifika yenilenirse burayı güncellemek gerekebilir.
    private static final List<String> PINNED_HASHES = Arrays.asList(
            "gTBRshpGjQfs/89Eoq4czeFM6NXDuofqHRMRp5dXEIw=", // sunucu sertifikası (leaf)
            "nWN7PSep5XDQdge5zK24CnCRXHr3KvzhKEGxsdqCX9E="  // ara CA sertifikası (yedek)
    );

    private static SSLContext pinliSslContext;

    public interface ApiListener {
        void onBasarili(JSONObject sonuc);
        void onHata(String hata);
    }

    /**
     * Normal sistem güvenini (PKI) kullanan, ama EK olarak sertifikanın
     * bizim beklediğimiz pin'lerden biriyle eşleşmesini zorunlu kılan SSLContext oluşturur.
     * X509ExtendedTrustManager kullanılıyor çünkü network_security_config.xml içinde
     * domain-config tanımlı olduğu için Android hostname-aware trust manager istiyor.
     */
    private static synchronized SSLContext getPinliSslContext() throws Exception {
        if (pinliSslContext != null) {
            return pinliSslContext;
        }

        TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        tmf.init((java.security.KeyStore) null);

        final X509ExtendedTrustManager varsayilanTrustManager =
                (X509ExtendedTrustManager) tmf.getTrustManagers()[0];

        X509ExtendedTrustManager pinliTrustManager = new X509ExtendedTrustManager() {
            private void pinKontrolEt(X509Certificate[] chain) throws CertificateException {
                boolean eslesmeVar = false;
                for (X509Certificate cert : chain) {
                    String hash = sertifikaPinHesapla(cert);
                    if (hash != null && PINNED_HASHES.contains(hash)) {
                        eslesmeVar = true;
                        break;
                    }
                }
                if (!eslesmeVar) {
                    throw new CertificateException("SSL Pinning hatasi: sertifika beklenen pin ile eslesmiyor.");
                }
            }

            @Override
            public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
                varsayilanTrustManager.checkClientTrusted(chain, authType);
            }

            @Override
            public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
                varsayilanTrustManager.checkServerTrusted(chain, authType);
                pinKontrolEt(chain);
            }

            @Override
            public void checkClientTrusted(X509Certificate[] chain, String authType, java.net.Socket socket) throws CertificateException {
                varsayilanTrustManager.checkClientTrusted(chain, authType, socket);
            }

            @Override
            public void checkServerTrusted(X509Certificate[] chain, String authType, java.net.Socket socket) throws CertificateException {
                varsayilanTrustManager.checkServerTrusted(chain, authType, socket);
                pinKontrolEt(chain);
            }

            @Override
            public void checkClientTrusted(X509Certificate[] chain, String authType, SSLEngine engine) throws CertificateException {
                varsayilanTrustManager.checkClientTrusted(chain, authType, engine);
            }

            @Override
            public void checkServerTrusted(X509Certificate[] chain, String authType, SSLEngine engine) throws CertificateException {
                varsayilanTrustManager.checkServerTrusted(chain, authType, engine);
                pinKontrolEt(chain);
            }

            @Override
            public X509Certificate[] getAcceptedIssuers() {
                return varsayilanTrustManager.getAcceptedIssuers();
            }
        };

        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(null, new TrustManager[]{pinliTrustManager}, null);
        pinliSslContext = sslContext;
        return pinliSslContext;
    }

    private static String sertifikaPinHesapla(X509Certificate cert) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] spkiBytes = cert.getPublicKey().getEncoded();
            byte[] hash = md.digest(spkiBytes);
            return Base64.encodeToString(hash, Base64.NO_WRAP);
        } catch (Exception e) {
            return null;
        }
    }

    private void istekGonder(String endpoint, JSONObject veri, ApiListener listener) {
        new AsyncTask<Void, Void, String>() {
            String hataMesaji = null;

            @Override
            protected String doInBackground(Void... voids) {
                try {
                    URL url = new URL(BASE_URL + endpoint);
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    if (conn instanceof HttpsURLConnection) {
                        ((HttpsURLConnection) conn).setSSLSocketFactory(getPinliSslContext().getSocketFactory());
                    }
                    conn.setRequestMethod("POST");
                    conn.setRequestProperty("Content-Type", "application/json");
                    conn.setDoOutput(true);
                    conn.setConnectTimeout(10000);
                    conn.setReadTimeout(10000);

                    OutputStream os = conn.getOutputStream();
                    os.write(veri.toString().getBytes("UTF-8"));
                    os.close();

                    int kod = conn.getResponseCode();
                    BufferedReader br;
                    if (kod >= 200 && kod < 300) {
                        br = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"));
                    } else {
                        br = new BufferedReader(new InputStreamReader(conn.getErrorStream(), "UTF-8"));
                    }

                    StringBuilder sb = new StringBuilder();
                    String satir;
                    while ((satir = br.readLine()) != null) {
                        sb.append(satir);
                    }
                    br.close();

                    return sb.toString();
                } catch (Exception e) {
                    hataMesaji = "Bağlantı hatası: " + e.getMessage();
                    return null;
                }
            }

            @Override
            protected void onPostExecute(String sonucMetni) {
                if (hataMesaji != null) {
                    listener.onHata(hataMesaji);
                    return;
                }
                try {
                    JSONObject json = new JSONObject(sonucMetni);
                    if (json.has("hata")) {
                        listener.onHata(json.getString("hata"));
                    } else {
                        listener.onBasarili(json);
                    }
                } catch (Exception e) {
                    listener.onHata("Sunucu yanıtı okunamadı.");
                }
            }
        }.execute();
    }

    public void kayitOl(String email, String sifre, ApiListener listener) {
        try {
            JSONObject veri = new JSONObject();
            veri.put("email", email);
            veri.put("sifre", sifre);
            istekGonder("kayit/", veri, listener);
        } catch (Exception e) {
            listener.onHata("Veri hazırlama hatası.");
        }
    }
    public void sifremiUnuttum(String email, ApiListener listener) {
        JSONObject veri = new JSONObject();
        try {
            veri.put("email", email);
        } catch (Exception e) {
            listener.onHata("Beklenmeyen bir hata oluştu.");
            return;
        }
        istekGonder("sifremi-unuttum/", veri, listener);
    }

    public void sifreSifirla(String email, String kod, String yeniSifre, ApiListener listener) {
        JSONObject veri = new JSONObject();
        try {
            veri.put("email", email);
            veri.put("kod", kod);
            veri.put("yeni_sifre", yeniSifre);
        } catch (Exception e) {
            listener.onHata("Beklenmeyen bir hata oluştu.");
            return;
        }
        istekGonder("sifre-sifirla/", veri, listener);
    }
    public void dogrula(String email, String kod, ApiListener listener) {
        try {
            JSONObject veri = new JSONObject();
            veri.put("email", email);
            veri.put("kod", kod);
            istekGonder("dogrula/", veri, listener);
        } catch (Exception e) {
            listener.onHata("Veri hazırlama hatası.");
        }
    }

    public void girisYap(String email, String sifre, String cihazId, ApiListener listener) {
        try {
            JSONObject veri = new JSONObject();
            veri.put("email", email);
            veri.put("sifre", sifre);
            veri.put("cihaz_id", cihazId);
            istekGonder("giris/", veri, listener);
        } catch (Exception e) {
            listener.onHata("Veri hazırlama hatası.");
        }
    }

    public void erisimKontrol(String email, String cihazId, ApiListener listener) {
        try {
            JSONObject veri = new JSONObject();
            veri.put("email", email);
            veri.put("cihaz_id", cihazId);
            veri.put("marka", android.os.Build.MANUFACTURER);
            veri.put("model", android.os.Build.MODEL);
            istekGonder("erisim/", veri, listener);
        } catch (Exception e) {
            listener.onHata("Veri hazırlama hatası.");
        }
    }

    /**
     * Kullanıcı "Çıkış Yap" dediğinde çağrılır.
     * Backend'e bu cihazın kaydını silmesi için istek gönderir.
     * Başarılı olursa callback ile "basarili" JSON'u döner.
     */

    public void cihazCikisYap(String email, String cihazId, ApiListener listener) {
        try {
            JSONObject veri = new JSONObject();
            veri.put("email", email);
            veri.put("cihaz_id", cihazId);
            istekGonder("cihaz-cikis/", veri, listener);
        } catch (Exception e) {
            listener.onHata("Veri hazırlama hatası.");
        }
    }
}
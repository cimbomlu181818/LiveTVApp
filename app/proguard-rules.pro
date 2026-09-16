# ═══════════════════════════════════════════════════════════════════════════════
#  proguard-rules.pro  —  LiveTV App
# ═══════════════════════════════════════════════════════════════════════════════

# ── Stack trace okuyabilmek için (release build'de de hata ayıklamak istersen) ─
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# ── Annotation'ları koru (Room, Gson, Firebase için gerekli) ─────────────────
-keepattributes *Annotation*
-keepattributes Signature
-keepattributes Exceptions
-keepattributes InnerClasses
-keepattributes EnclosingMethod

# ── Firebase Auth ─────────────────────────────────────────────────────────────
-keep class com.google.firebase.auth.** { *; }
-keep class com.google.firebase.auth.internal.** { *; }
-dontwarn com.google.firebase.auth.**

# ── Firebase Firestore ────────────────────────────────────────────────────────
-keep class com.google.firebase.firestore.** { *; }
-keep class com.google.firebase.firestore.model.** { *; }
-dontwarn com.google.firebase.firestore.**

# ── Firebase Installations (FID — cihaz kimliği için kullanıyoruz) ───────────
-keep class com.google.firebase.installations.** { *; }
-dontwarn com.google.firebase.installations.**

# ── Firebase genel ───────────────────────────────────────────────────────────
-keep class com.google.firebase.** { *; }
-dontwarn com.google.firebase.**

# ── Google Play Services / Sign-In ───────────────────────────────────────────
-keep class com.google.android.gms.** { *; }
-dontwarn com.google.android.gms.**
-keep class com.google.android.gms.auth.api.signin.** { *; }
-keep class com.google.android.gms.common.** { *; }
-keep class com.google.android.gms.tasks.** { *; }

# ── Room Database ─────────────────────────────────────────────────────────────
-keep class androidx.room.** { *; }
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao class * { *; }
-keep @androidx.room.Database class * { *; }
-keepclassmembers class * {
    @androidx.room.* <methods>;
    @androidx.room.* <fields>;
}
-dontwarn androidx.room.**

# ── VLC ───────────────────────────────────────────────────────────────────────
-keep class org.videolan.** { *; }
-keep class org.videolan.libvlc.** { *; }
-dontwarn org.videolan.**

# ── Glide ─────────────────────────────────────────────────────────────────────
-keep public class * implements com.bumptech.glide.module.GlideModule
-keep class * extends com.bumptech.glide.module.AppGlideModule {
    <init>(...);
}
-keep public enum com.bumptech.glide.load.ImageHeaderParser$** {
    **[] $VALUES;
    public *;
}
-keep class com.bumptech.glide.load.data.ParcelFileDescriptorRewinder$InternalRewinder {
    *** rewind();
}
-dontwarn com.bumptech.glide.**

# ── RxJava 3 ──────────────────────────────────────────────────────────────────
-keep class io.reactivex.rxjava3.** { *; }
-keepclassmembers class io.reactivex.rxjava3.** { *; }
-dontwarn java.util.concurrent.Flow*
-dontwarn io.reactivex.rxjava3.**

# ── Gson ──────────────────────────────────────────────────────────────────────
-keep class com.google.gson.** { *; }
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer
-keepclassmembers,allowobfuscation class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

# ── AndroidX / AppCompat / Material ──────────────────────────────────────────
-keep class androidx.appcompat.** { *; }
-keep class com.google.android.material.** { *; }
-dontwarn androidx.**
-dontwarn com.google.android.material.**

# ── AndroidX Leanback (TV desteği) ───────────────────────────────────────────
-keep class androidx.leanback.** { *; }
-dontwarn androidx.leanback.**

# ── Paging & Lifecycle ────────────────────────────────────────────────────────
-keep class androidx.paging.** { *; }
-keep class androidx.lifecycle.** { *; }
-keepclassmembers class * extends androidx.lifecycle.ViewModel {
    <init>();
}
-dontwarn androidx.paging.**
-dontwarn androidx.lifecycle.**

# ── Kendi uygulama paketimiz ──────────────────────────────────────────────────
# Activity isimleri AndroidManifest.xml'de kayıtlı olduğu için sadece
# sınıf adını koru, içindeki tüm metod/field isimleri obfuscate edilsin.
-keep public class com.example.livetvapp.MainActivity
-keep public class com.example.livetvapp.LoginActivity

# FirebaseHelper: iç interface'ler (SonucListener, ErisimListener vb.)
# reflection ile çağrıldığı için metodlarını koru, sınıf içi mantık gizlenir.
-keepclassmembers class com.example.livetvapp.FirebaseHelper$* {
    public <methods>;
}

# Firestore'dan @DocumentId veya alan adıyla okunan model sınıfları varsa
# alan isimlerini koru (yoksa bu satırı sil):
# -keepclassmembers class com.example.livetvapp.model.** { <fields>; }

# ── Native kütüphanelerin JNI metodlarını koru ───────────────────────────────
-keepclasseswithmembernames class * {
    native <methods>;
}

# ── Serializable sınıflar ─────────────────────────────────────────────────────
-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}

# ── Parcelable ────────────────────────────────────────────────────────────────
-keepclassmembers class * implements android.os.Parcelable {
    public static final ** CREATOR;
}

# ── Enum'lar ──────────────────────────────────────────────────────────────────
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# ── Genel uyarı bastırma ─────────────────────────────────────────────────────
-dontwarn javax.annotation.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**
# Gson TypeToken için generic signature koruma
-keepattributes Signature
-keepattributes *Annotation*

# Gson'ın kendisini koru
-keep class com.google.gson.** { *; }
-keep class com.google.gson.reflect.TypeToken { *; }
-keep class * extends com.google.gson.reflect.TypeToken

# Uygulamanızdaki Gson kullanılan sınıflar varsa onları da koru (varsa)
# -keep class com.example.livetvapp.model.** { *; }
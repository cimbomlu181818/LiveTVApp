from django.db import models
from django.contrib.auth.hashers import make_password, check_password


class Kullanici(models.Model):
    email = models.EmailField(unique=True, verbose_name="E-posta")
    sifre = models.CharField(max_length=255, verbose_name="Şifre")

    mail_dogrulandi = models.BooleanField(default=False, verbose_name="E-posta Doğrulandı")
    dogrulama_kodu = models.CharField(max_length=64, blank=True, null=True, verbose_name="Doğrulama Kodu")
    dogrulama_kodu_zamani = models.DateTimeField(blank=True, null=True, verbose_name="Doğrulama Kodu Zamanı")

    trial_baslangic = models.DateTimeField(auto_now_add=True, verbose_name="Deneme Başlangıcı")
    trial_bitis = models.DateTimeField(null=True, blank=True, verbose_name="Deneme Bitişi")

    premium_mi = models.BooleanField(default=False, verbose_name="Premium mi")

    olusturma_tarihi = models.DateTimeField(auto_now_add=True, verbose_name="Oluşturma Tarihi")

    def sifre_belirle(self, ham_sifre):
        self.sifre = make_password(ham_sifre)

    def sifre_dogru_mu(self, ham_sifre):
        return check_password(ham_sifre, self.sifre)

    def __str__(self):
        return self.email

    class Meta:
        verbose_name = "Kullanıcı"
        verbose_name_plural = "Kullanıcılar"


class Ayarlar(models.Model):
    bakim_modu_acik = models.BooleanField(default=False, verbose_name="Bakım Modu Açık")
    bakim_mesaji = models.CharField(
        max_length=255,
        default="Uygulama bakımda, birazdan döneceğiz.",
        verbose_name="Bakım Mesajı"
    )

    def __str__(self):
        return "Genel Ayarlar"

    class Meta:
        verbose_name = "Ayar"
        verbose_name_plural = "Genel Ayarlar"


class Cihaz(models.Model):
    kullanici = models.ForeignKey(Kullanici, on_delete=models.CASCADE, related_name="cihazlar", verbose_name="Kullanıcı")
    cihaz_id = models.CharField(max_length=255, verbose_name="Cihaz Kimliği")
    marka = models.CharField(max_length=100, blank=True, default="", verbose_name="Marka")
    model = models.CharField(max_length=100, blank=True, default="", verbose_name="Model")
    eklenme_tarihi = models.DateTimeField(auto_now_add=True, verbose_name="Eklenme Tarihi")

    def __str__(self):
        return f"{self.marka} {self.model} ({self.kullanici.email})"

    class Meta:
        verbose_name = "Cihaz"
        verbose_name_plural = "Cihazlar"

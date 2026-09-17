import json
import random
import datetime
from django.http import JsonResponse
from django.views.decorators.csrf import csrf_exempt
from django.core.mail import send_mail
from django.db import IntegrityError
from django.utils import timezone
from .models import Kullanici, Ayarlar, Cihaz
from django_ratelimit.decorators import ratelimit

@csrf_exempt
@ratelimit(key='ip', rate='5/m', method='POST', block=True)
def kayit_ol(request):
    if request.method != 'POST':
        return JsonResponse({'hata': 'Sadece POST istekleri kabul edilir.'}, status=405)

    try:
        veri = json.loads(request.body)
    except json.JSONDecodeError:
        return JsonResponse({'hata': 'Geçersiz veri formatı.'}, status=400)

    email = veri.get('email', '').strip().lower()
    sifre = veri.get('sifre', '')

    if not email or not sifre:
        return JsonResponse({'hata': 'Email ve şifre zorunludur.'}, status=400)

    if len(sifre) < 6:
        return JsonResponse({'hata': 'Şifre en az 6 karakter olmalıdır.'}, status=400)

    mevcut_kullanici = Kullanici.objects.filter(email=email).first()

    if mevcut_kullanici is not None:
        if mevcut_kullanici.mail_dogrulandi:
            return JsonResponse({'hata': 'Bu email zaten kayıtlı.'}, status=400)

        # Doğrulanmamış hesap: art arda kod isteğini (email bombing) engellemek için 60 sn bekleme.
        if mevcut_kullanici.dogrulama_kodu_zamani is not None:
            gecen_saniye = (timezone.now() - mevcut_kullanici.dogrulama_kodu_zamani).total_seconds()
            if gecen_saniye < 60:
                kalan = int(60 - gecen_saniye)
                return JsonResponse({'hata': f'Lütfen yeni kod için {kalan} saniye bekleyin.'}, status=429)

        dogrulama_kodu = str(random.randint(100000, 999999))
        mevcut_kullanici.sifre_belirle(sifre)
        mevcut_kullanici.dogrulama_kodu = dogrulama_kodu
        mevcut_kullanici.dogrulama_kodu_zamani = timezone.now()
        mevcut_kullanici.trial_bitis = timezone.now() + datetime.timedelta(hours=24)
        mevcut_kullanici.save()

        send_mail(
            subject='Doğrulama Kodunuz',
            message=f'Merhaba,\n\nHesabınızı doğrulamak için kodunuz: {dogrulama_kodu}',
            from_email=None,
            recipient_list=[email],
            fail_silently=True,
        )

        return JsonResponse({'basarili': True, 'mesaj': 'Kayıt başarılı. Doğrulama kodu email adresinize gönderildi.'})

    dogrulama_kodu = str(random.randint(100000, 999999))
    trial_bitis = timezone.now() + datetime.timedelta(hours=24)

    yeni_kullanici = Kullanici(email=email, dogrulama_kodu=dogrulama_kodu, dogrulama_kodu_zamani=timezone.now(), trial_bitis=trial_bitis)
    yeni_kullanici.sifre_belirle(sifre)
    yeni_kullanici.save()

    send_mail(
        subject='Doğrulama Kodunuz',
        message=f'Merhaba,\n\nHesabınızı doğrulamak için kodunuz: {dogrulama_kodu}',
        from_email=None,
        recipient_list=[email],
        fail_silently=True,
    )

    return JsonResponse({'basarili': True, 'mesaj': 'Kayıt başarılı. Doğrulama kodu email adresinize gönderildi.'})


@csrf_exempt
@ratelimit(key='ip', rate='5/m', method='POST', block=True)
def dogrula(request):
    if request.method != 'POST':
        return JsonResponse({'hata': 'Sadece POST istekleri kabul edilir.'}, status=405)

    try:
        veri = json.loads(request.body)
    except json.JSONDecodeError:
        return JsonResponse({'hata': 'Geçersiz veri formatı.'}, status=400)

    email = veri.get('email', '').strip().lower()
    kod = veri.get('kod', '').strip()

    if not email or not kod:
        return JsonResponse({'hata': 'Email ve kod zorunludur.'}, status=400)

    try:
        kullanici = Kullanici.objects.get(email=email)
    except Kullanici.DoesNotExist:
        return JsonResponse({'hata': 'Kullanıcı bulunamadı.'}, status=404)

    if kullanici.mail_dogrulandi:
        return JsonResponse({'hata': 'Bu hesap zaten doğrulanmış.'}, status=400)

    if kullanici.dogrulama_kodu_zamani is None or (timezone.now() - kullanici.dogrulama_kodu_zamani).total_seconds() > 900:
        return JsonResponse({'hata': 'Kodun süresi dolmuş, lütfen tekrar kayıt olup yeni kod isteyin.'}, status=400)
    if kullanici.dogrulama_kodu != kod:
        return JsonResponse({'hata': 'Kod hatalı.'}, status=400)

    kullanici.mail_dogrulandi = True
    kullanici.dogrulama_kodu = None
    kullanici.save()

    return JsonResponse({'basarili': True, 'mesaj': 'Hesap doğrulandı.'})


@csrf_exempt
@ratelimit(key='ip', rate='5/m', method='POST', block=True)
def sifremi_unuttum(request):
    if request.method != 'POST':
        return JsonResponse({'hata': 'Sadece POST istekleri kabul edilir.'}, status=405)

    try:
        veri = json.loads(request.body)
    except json.JSONDecodeError:
        return JsonResponse({'hata': 'Geçersiz veri formatı.'}, status=400)

    email = veri.get('email', '').strip().lower()

    if not email:
        return JsonResponse({'hata': 'Email zorunludur.'}, status=400)

    try:
        kullanici = Kullanici.objects.get(email=email)
    except Kullanici.DoesNotExist:
        # Hesabın var olup olmadığını belli etmemek için aynı başarılı mesajı döndürüyoruz.
        return JsonResponse({'basarili': True, 'mesaj': 'Eğer bu email kayıtlıysa, şifre sıfırlama kodu gönderildi.'})

    # Art arda kod isteğini (email bombing) engellemek için 60 sn bekleme.
    if kullanici.sifirlama_kodu_zamani is not None:
        gecen_saniye = (timezone.now() - kullanici.sifirlama_kodu_zamani).total_seconds()
        if gecen_saniye < 60:
            kalan = int(60 - gecen_saniye)
            return JsonResponse({'hata': f'Lütfen yeni kod için {kalan} saniye bekleyin.'}, status=429)

    sifirlama_kodu = str(random.randint(100000, 999999))
    kullanici.sifirlama_kodu = sifirlama_kodu
    kullanici.sifirlama_kodu_zamani = timezone.now()
    kullanici.save()

    send_mail(
        subject='Şifre Sıfırlama Kodunuz',
        message=f'Merhaba,\n\nŞifrenizi sıfırlamak için kodunuz: {sifirlama_kodu}\n\nBu kodu siz istemediyseniz bu emaili görmezden gelebilirsiniz.',
        from_email=None,
        recipient_list=[email],
        fail_silently=True,
    )

    return JsonResponse({'basarili': True, 'mesaj': 'Eğer bu email kayıtlıysa, şifre sıfırlama kodu gönderildi.'})


@csrf_exempt
@ratelimit(key='ip', rate='5/m', method='POST', block=True)
def sifre_sifirla(request):
    if request.method != 'POST':
        return JsonResponse({'hata': 'Sadece POST istekleri kabul edilir.'}, status=405)

    try:
        veri = json.loads(request.body)
    except json.JSONDecodeError:
        return JsonResponse({'hata': 'Geçersiz veri formatı.'}, status=400)

    email = veri.get('email', '').strip().lower()
    kod = veri.get('kod', '').strip()
    yeni_sifre = veri.get('yeni_sifre', '')

    if not email or not kod or not yeni_sifre:
        return JsonResponse({'hata': 'Email, kod ve yeni şifre zorunludur.'}, status=400)

    if len(yeni_sifre) < 6:
        return JsonResponse({'hata': 'Şifre en az 6 karakter olmalıdır.'}, status=400)

    try:
        kullanici = Kullanici.objects.get(email=email)
    except Kullanici.DoesNotExist:
        return JsonResponse({'hata': 'Kod hatalı veya süresi dolmuş.'}, status=400)

    if kullanici.sifirlama_kodu_zamani is None or (timezone.now() - kullanici.sifirlama_kodu_zamani).total_seconds() > 900:
        return JsonResponse({'hata': 'Kodun süresi dolmuş, lütfen tekrar şifre sıfırlama isteği gönderin.'}, status=400)
    if not kullanici.sifirlama_kodu or kullanici.sifirlama_kodu != kod:
        return JsonResponse({'hata': 'Kod hatalı veya süresi dolmuş.'}, status=400)

    kullanici.sifre_belirle(yeni_sifre)
    kullanici.sifirlama_kodu = None
    kullanici.sifirlama_kodu_zamani = None
    # Şifre başarıyla sıfırlandığı için varsa hesap kilidini de kaldırıyoruz.
    kullanici.basarisiz_giris_sayisi = 0
    kullanici.kilit_bitis_zamani = None
    kullanici.save()

    return JsonResponse({'basarili': True, 'mesaj': 'Şifreniz başarıyla sıfırlandı.'})


KILIT_DENEME_LIMITI = 5
KILIT_SURESI_DAKIKA = 15


@csrf_exempt
@ratelimit(key='ip', rate='5/m', method='POST', block=True)
def giris_yap(request):
    if request.method != 'POST':
        return JsonResponse({'hata': 'Sadece POST istekleri kabul edilir.'}, status=405)

    try:
        veri = json.loads(request.body)
    except json.JSONDecodeError:
        return JsonResponse({'hata': 'Geçersiz veri formatı.'}, status=400)

    email = veri.get('email', '').strip().lower()
    sifre = veri.get('sifre', '')
    cihaz_id = veri.get('cihaz_id', '').strip()
    marka = veri.get('marka', '').strip()
    model_adi = veri.get('model', '').strip()

    if not email or not sifre or not cihaz_id:
        return JsonResponse({'hata': 'Email, şifre ve cihaz_id zorunludur.'}, status=400)

    try:
        kullanici = Kullanici.objects.get(email=email)
    except Kullanici.DoesNotExist:
        return JsonResponse({'hata': 'Email veya şifre hatalı.'}, status=400)

    if kullanici.kilit_bitis_zamani and timezone.now() < kullanici.kilit_bitis_zamani:
        kalan_dk = int((kullanici.kilit_bitis_zamani - timezone.now()).total_seconds() // 60) + 1
        return JsonResponse({'hata': f'Çok fazla hatalı deneme. Hesap {kalan_dk} dakika kilitli.'}, status=403)

    if not kullanici.sifre_dogru_mu(sifre):
        kullanici.basarisiz_giris_sayisi += 1
        if kullanici.basarisiz_giris_sayisi >= KILIT_DENEME_LIMITI:
            kullanici.kilit_bitis_zamani = timezone.now() + datetime.timedelta(minutes=KILIT_SURESI_DAKIKA)
            kullanici.basarisiz_giris_sayisi = 0
        kullanici.save()
        return JsonResponse({'hata': 'Email veya şifre hatalı.'}, status=400)

    if kullanici.basarisiz_giris_sayisi or kullanici.kilit_bitis_zamani:
        kullanici.basarisiz_giris_sayisi = 0
        kullanici.kilit_bitis_zamani = None
        kullanici.save()

    if not kullanici.mail_dogrulandi:
        return JsonResponse({'hata': 'Hesabınız henüz doğrulanmamış.'}, status=403)

    mevcut_cihaz = Cihaz.objects.filter(kullanici=kullanici, cihaz_id=cihaz_id).first()

    if mevcut_cihaz is None:
        if Cihaz.objects.filter(kullanici=kullanici).count() >= 2:
            return JsonResponse({'hata': 'Bu hesap zaten 2 cihazda aktif.'}, status=403)
        try:
            Cihaz.objects.create(kullanici=kullanici, cihaz_id=cihaz_id, marka=marka, model=model_adi)
        except IntegrityError:
            pass

    return JsonResponse({'basarili': True, 'mesaj': 'Giriş başarılı.'})


@csrf_exempt
@ratelimit(key='ip', rate='30/m', method='POST', block=True)
def erisim_kontrol(request):
    if request.method != 'POST':
        return JsonResponse({'hata': 'Sadece POST istekleri kabul edilir.'}, status=405)

    try:
        veri = json.loads(request.body)
    except json.JSONDecodeError:
        return JsonResponse({'hata': 'Geçersiz veri formatı.'}, status=400)

    ayarlar = Ayarlar.objects.first()
    if ayarlar and ayarlar.bakim_modu_acik:
        return JsonResponse({'bakim_modu': True, 'mesaj': ayarlar.bakim_mesaji})

    email = veri.get('email', '').strip().lower()
    cihaz_id = veri.get('cihaz_id', '').strip()
    marka = veri.get('marka', '').strip()
    model_adi = veri.get('model', '').strip()

    if not email:
        return JsonResponse({'hata': 'Email zorunludur.'}, status=400)

    try:
        kullanici = Kullanici.objects.get(email=email)
    except Kullanici.DoesNotExist:
        return JsonResponse({'hata': 'Kullanıcı bulunamadı.'}, status=404)

    if not kullanici.mail_dogrulandi:
        return JsonResponse({'bakim_modu': False, 'erisim': False, 'durum': 'mail_dogrulanmadi'})

    # Cihaz senkronizasyonu: admin panelden cihaz silinmiş olabilir.
    # Uygulama email zaten kayıtlıysa giris/ akışına hiç uğramadan doğrudan
    # buraya düşüyor, bu yüzden cihaz kaydı burada da güncellenmeli.
    if cihaz_id:
        mevcut_cihaz = Cihaz.objects.filter(kullanici=kullanici, cihaz_id=cihaz_id).first()
        if mevcut_cihaz is None:
            return JsonResponse({'bakim_modu': False, 'erisim': False, 'durum': 'cihaz_kayitli_degil'}, status=403)
        else:
            # Cihaz zaten kayıtlı; marka/model değişmişse güncelle,
            # her erişim kontrolünde tarihi (son görülme) tazelemek için
            # değişiklik olmasa bile mutlaka save() çağır.
            if marka and mevcut_cihaz.marka != marka:
                mevcut_cihaz.marka = marka
            if model_adi and mevcut_cihaz.model != model_adi:
                mevcut_cihaz.model = model_adi
            mevcut_cihaz.save()

    if kullanici.premium_mi:
        return JsonResponse({'bakim_modu': False, 'erisim': True, 'durum': 'premium'})

    if kullanici.trial_bitis and timezone.now() < kullanici.trial_bitis:
        return JsonResponse({'bakim_modu': False, 'erisim': True, 'durum': 'trial'})

    return JsonResponse({'bakim_modu': False, 'erisim': False, 'durum': 'trial_bitti'})

@csrf_exempt
@ratelimit(key='ip', rate='10/m', method='POST', block=True)
def cihaz_cikis(request):
    if request.method != 'POST':
        return JsonResponse({'hata': 'Sadece POST istekleri kabul edilir.'}, status=405)
    try:
        veri = json.loads(request.body)
    except json.JSONDecodeError:
        return JsonResponse({'hata': 'Geçersiz veri formatı.'}, status=400)

    email = veri.get('email', '').strip().lower()
    cihaz_id = veri.get('cihaz_id', '').strip()

    if not email or not cihaz_id:
        return JsonResponse({'hata': 'Email ve cihaz_id zorunludur.'}, status=400)

    try:
        kullanici = Kullanici.objects.get(email=email)
    except Kullanici.DoesNotExist:
        return JsonResponse({'basarili': True, 'mesaj': 'Çıkış işlemi tamamlandı.'})

    silinen_sayisi, _ = Cihaz.objects.filter(kullanici=kullanici, cihaz_id=cihaz_id).delete()

    return JsonResponse({
        'basarili': True,
        'mesaj': 'Cihaz kaydı silindi.',
        'silinen': silinen_sayisi
    })

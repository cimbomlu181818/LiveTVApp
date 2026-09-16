import datetime
from django.contrib import admin
from django.shortcuts import render, redirect
from django.urls import path
from django.utils import timezone
from django.contrib import messages
from .models import Kullanici, Ayarlar, Cihaz


class CihazInline(admin.TabularInline):
    model = Cihaz
    extra = 0
    fields = ('marka', 'model', 'cihaz_id', 'eklenme_tarihi')
    readonly_fields = ('cihaz_id', 'eklenme_tarihi')


@admin.action(description="Trial süresini uzat")
def trial_uzat_action(modeladmin, request, queryset):
    secili_idler = list(queryset.values_list('id', flat=True))
    request.session['trial_uzat_idler'] = secili_idler
    return redirect('admin:trial_uzat_ekrani')


@admin.register(Kullanici)
class KullaniciAdmin(admin.ModelAdmin):
    list_display = ('email', 'mail_dogrulandi', 'premium_mi', 'trial_bitis', 'olusturma_tarihi', 'cihaz_sayisi')
    search_fields = ('email',)
    list_filter = ('premium_mi', 'mail_dogrulandi')
    inlines = [CihazInline]
    actions = [trial_uzat_action]

    def cihaz_sayisi(self, obj):
        return obj.cihazlar.count()
    cihaz_sayisi.short_description = "Cihaz Sayısı"

    def get_urls(self):
        urls = super().get_urls()
        ozel_urls = [
            path('trial-uzat/', self.admin_site.admin_view(self.trial_uzat_ekrani), name='trial_uzat_ekrani'),
        ]
        return ozel_urls + urls

    def trial_uzat_ekrani(self, request):
        idler = request.session.get('trial_uzat_idler', [])
        kullanicilar = Kullanici.objects.filter(id__in=idler)

        if request.method == 'POST':
            secenek = request.POST.get('secenek')
            secili_idler = request.POST.getlist('secili_id')
            hedef_kullanicilar = Kullanici.objects.filter(id__in=secili_idler)

            if secenek == 'bitir':
                hedef_kullanicilar.update(trial_bitis=timezone.now())
                messages.success(request, f"{hedef_kullanicilar.count()} kullanıcının trial süresi anında bitirildi.")
            elif secenek == 'ozel':
                ozel_tarih = request.POST.get('ozel_tarih')
                if ozel_tarih:
                    tarih = datetime.datetime.strptime(ozel_tarih, '%Y-%m-%d')
                    tarih = timezone.make_aware(
                        tarih.replace(hour=23, minute=59, second=59)
                    )
                    hedef_kullanicilar.update(trial_bitis=tarih)
                    messages.success(request, f"{hedef_kullanicilar.count()} kullanıcının trial süresi {ozel_tarih} tarihine kadar uzatıldı.")
            else:
                gun_sayisi = int(secenek)
                yeni_tarih = timezone.now() + datetime.timedelta(days=gun_sayisi)
                hedef_kullanicilar.update(trial_bitis=yeni_tarih)
                messages.success(request, f"{hedef_kullanicilar.count()} kullanıcının trial süresi {gun_sayisi} gün uzatıldı.")

            request.session.pop('trial_uzat_idler', None)
            return redirect('admin:kullanicilar_kullanici_changelist')

        context = dict(
            self.admin_site.each_context(request),
            kullanicilar=kullanicilar,
        )
        return render(request, 'admin/trial_uzat.html', context)


@admin.register(Ayarlar)
class AyarlarAdmin(admin.ModelAdmin):
    list_display = ('bakim_modu_acik', 'bakim_mesaji')

    def has_add_permission(self, request):
        if Ayarlar.objects.exists():
            return False
        return True
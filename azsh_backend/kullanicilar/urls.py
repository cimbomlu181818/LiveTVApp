from django.urls import path
from . import views

urlpatterns = [
    path('kayit/', views.kayit_ol, name='kayit_ol'),
    path('dogrula/', views.dogrula, name='dogrula'),
    path('giris/', views.giris_yap, name='giris_yap'),
    path('erisim/', views.erisim_kontrol, name='erisim_kontrol'),
    path('cihaz-cikis/', views.cihaz_cikis, name='cihaz_cikis'),
    path('sifremi-unuttum/', views.sifremi_unuttum, name='sifremi_unuttum'),
    path('sifre-sifirla/', views.sifre_sifirla, name='sifre_sifirla'),
]

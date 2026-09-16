from django.urls import path
from . import views

urlpatterns = [
    path('kayit/', views.kayit_ol, name='kayit_ol'),
    path('dogrula/', views.dogrula, name='dogrula'),
    path('giris/', views.giris_yap, name='giris_yap'),
    path('erisim/', views.erisim_kontrol, name='erisim_kontrol'),
]
# Uploat-V

Android uchun yengil video va audio yuklovchi. Dvigatel: [yt-dlp](https://github.com/yt-dlp/yt-dlp).

Ikkita ekran bor: **Bosh sahifa** va **Sozlamalar**.

## Imkoniyatlar

- Havola orqali video yoki audio yuklash (boshqa ilovadan «Ulashish» ham ishlaydi)
- Sifat (360p–1080p, eng yaxshi) va audio format (M4A, MP3) tanlash
- Playlistlarni yuklash
- Fon rejimida yuklash (bildirishnoma bilan), bir vaqtda 2 tagacha
- Fayllar `Download/Uploat-V` papkasiga saqlanadi
- Material 3, dinamik ranglar, yorug'/qorong'i mavzu
- Sozlamalardan yt-dlp'ni yangilash

## APK olish

`main` branch'ga push qilinganda GitHub Actions APK yig'adi va **Releases → latest** ga joylaydi.
Boshqa branch'lar uchun APK Actions → Artifacts bo'limida.

## Lokal build

```
./gradlew assembleRelease
```

JDK 17 va Android SDK 35 kerak. Faqat `arm64-v8a` qurilmalar qo'llab-quvvatlanadi, minimal Android 10.

<p align="center">
  <img src="docs/logo.png" width="120" alt="Uploat-V logosi">
</p>

<h1 align="center">Uploat-V</h1>

<p align="center">
  Android uchun yengil, ishonchli <b>video yuklovchi</b>.<br>
  Dvigatel: <a href="https://github.com/yt-dlp/yt-dlp">yt-dlp</a> · Kotlin · Jetpack Compose
</p>

<p align="center">
  <a href="../../actions/workflows/build.yml"><img alt="Build" src="../../actions/workflows/build.yml/badge.svg"></a>
  <img alt="Android 10+" src="https://img.shields.io/badge/Android-10%2B-3DDC84?logo=android&logoColor=white">
</p>

---

## Imkoniyatlar

- Havola orqali video yuklash, boshqa ilovadan «Ulashish» ham ishlaydi
- Sifatni tanlash (360p dan 1080p gacha), fayl hajmi oldindan ko'rinadi
- Playlistlar va fon rejimida yuklash (bir vaqtda 2 tagacha)
- Uzilgan yuklash davom etadi, internet qaytganda avtomatik qayta urinadi
- Tarmoq rejimi: Wi-Fi va mobil internet / faqat Wi-Fi / faqat mobil internet
- Yuklash tarixini avtomatik tozalash (cheksiz, 30 kun, 7 kun yoki 24 soat)
- Yorug' va qorong'i mavzu

## Tillar

- O'zbekcha: mavjud
- English: tez orada

## O'rnatish

1. [**Releases**](../../releases/latest) bo'limidan `Uploat-V.apk` ni yuklab oling.
2. APK'ni oching va «Noma'lum manbalardan o'rnatish» ruxsatini bering.
3. Birinchi ochilganda ilova bildirishnoma va xotira ruxsatlarini so'raydi.

Talablar: Android 10 va undan yuqori, arm64 qurilma.

**Yangilash:** yangi APK'ni yuklab, eskisining ustiga o'rnating. Tarix va sozlamalar saqlanadi.

Yuklangan videolar `Download/Uploat-V` papkasida bo'ladi.

## Dasturchilar uchun

```bash
./gradlew testDebugUnitTest   # testlar
./gradlew assembleRelease     # APK
```

JDK 17 kerak. Yangi versiya chiqarish: Actions → Build APK → Run workflow, versiyani yozing (masalan `1.0.1`).

## Litsenziya

[GPL-3.0](LICENSE)

## Minnatdorchilik

[yt-dlp](https://github.com/yt-dlp/yt-dlp), [youtubedl-android](https://github.com/JunkFood02/youtubedl-android), [Haze](https://github.com/chrisbanes/haze), [Coil](https://github.com/coil-kt/coil), [Seal](https://github.com/JunkFood02/Seal) (ilhom manbai).

⭐ Ilova yoqqan bo'lsa, loyihaga **Star** bosib qo'llab-quvvatlang. Oldindan rahmat!

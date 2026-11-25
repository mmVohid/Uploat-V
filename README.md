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
  <img alt="arm64" src="https://img.shields.io/badge/ABI-arm64--v8a-555">
</p>

---

## Imkoniyatlar

**Yuklash**
- Havola orqali video yuklash; boshqa ilovadan «Ulashish» ham ishlaydi
- Sifat tanlash (360p dan 1080p gacha va eng yaxshi), fayl hajmi oldindan ko'rinadi
- Playlistlar, bir vaqtda 2 tagacha yuklash, fon rejimida bildirishnoma bilan
- Fayllar `Download/Uploat-V` papkasiga (yoki o'zingiz tanlagan papkaga) saqlanadi

**Ishonchlilik**
- Uzilgan yuklashni davom ettirish (boshidan boshlamaydi)
- Internet uzilsa, qaytganda avtomatik qayta urinadi
- Ilova tizim tomonidan yopilsa ham yuklashlar tiklanadi
- Yuklashdan oldin bo'sh joy va tarmoq tekshiriladi
- Bir xil havola qayta kiritilsa, tasdiq so'raydi
- Tushunarli xato xabarlari va nusxalash mumkin bo'lgan xatolar jurnali

**Sozlamalar**
- Tarmoq: Wi-Fi va mobil internet / faqat Wi-Fi / faqat mobil internet
- Fon rejimi (batareya cheklovisiz) va bildirishnomani ilova ichidan boshqarish
- Tarix saqlash muddati: cheksiz, 30 kun, 7 kun yoki 24 soat
- yt-dlp ni yangilash (barqaror yoki nightly) va bosh sahifada tezkor yangilash kartochkasi
- Yorug', qorong'i va tizim mavzusi; oq mavzu va qora aksent

## O'rnatish

1. [**Releases**](../../releases) bo'limidan oxirgi `Uploat-V.apk` ni yuklab oling.
2. Telefonda «Noma'lum manbalardan o'rnatish» ruxsatini bering va APK'ni oching.
3. Birinchi ochilganda ilova bildirishnoma va xotira ruxsatlarini so'raydi.

Talablar: Android 10 (API 29) va undan yuqori, arm64 qurilma.

## Build qilish

```bash
./gradlew testDebugUnitTest   # unit testlar
./gradlew assembleRelease     # APK: app/build/outputs/apk/release/
```

JDK 17 kerak. Loyiha GitHub Actions'da avtomatik yig'iladi (`.github/workflows/build.yml`): avval testlar, so'ng APK.

## Branch va release

| Branch | Natija |
|---|---|
| `main` | Har push uchun alohida release (`v1.0.<build>`), `latest` belgisi bilan |
| `feature/home-settings` | Yagona `beta` pre-release, har push'da yangilanadi |
| boshqalari | APK faqat Actions → Artifacts bo'limida |

## Tuzilma

```
app/src/main/java/uz/vohid/uploatv/
├── data/       sozlamalar (DataStore) va modellar
├── download/   yuklash menejeri, servis, yt-dlp bilan ishlash, xatolar
└── ui/         ekranlar (Compose), mavzu
```

## Minnatdorchilik

- [yt-dlp](https://github.com/yt-dlp/yt-dlp) va [youtubedl-android](https://github.com/JunkFood02/youtubedl-android)
- [Seal](https://github.com/JunkFood02/Seal), ilhom manbai
- [Haze](https://github.com/chrisbanes/haze) (xira shisha effekti), [Coil](https://github.com/coil-kt/coil)

Faqat o'zingizga ruxsat etilgan yoki erkin tarqatiladigan kontentni yuklang. Platformalarning foydalanish shartlariga rioya qilish foydalanuvchi zimmasida.

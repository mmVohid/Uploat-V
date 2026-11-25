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

1. [**Releases → Latest**](../../releases/latest) dan `Uploat-V.apk` ni yuklab oling.
2. Telefonda «Noma'lum manbalardan o'rnatish» ruxsatini bering va APK'ni oching.
3. Birinchi ochilganda ilova bildirishnoma va xotira ruxsatlarini so'raydi.

Talablar: Android 10 (API 29) va undan yuqori, arm64 qurilma.

### Yangilash

Ilova doimiy imzo kaliti bilan imzolanadi, shuning uchun yangi versiyani eskisining **ustiga o'rnatish** mumkin: ilovani o'chirish shart emas, tarix va sozlamalar saqlanadi. Yangisini yuklab, APK'ni oching va «Yangilash» ni bosing.

> **Eslatma:** doimiy kalit bilan imzolangan birinchi versiya `v1.0.0` dir. Undan oldingi sinov build'lari (masalan, `v1.0.95`) boshqa kalit bilan imzolangan. Ularni o'rnatgan bo'lsangiz, ilovani **bir marta o'chirib**, `v1.0.0` ni o'rnating (tarix va sozlamalar o'chadi). Shundan keyin hamma yangilanishlar ustiga o'rnatiladi.

**Barqaror versiya: [v1.0.0](../../releases/tag/v1.0.0).** Dasturchi yangi o'zgarishlarni sinab ko'rayotgan paytda ham sizga shu sinalgan versiya beriladi. Yangisi tayyor bo'lgach, alohida versiya (v1.0.1, v1.1.0...) chiqadi va Releases'da ko'rinadi.

## Build qilish

```bash
./gradlew testDebugUnitTest   # unit testlar
./gradlew assembleRelease     # APK: app/build/outputs/apk/release/
```

JDK 17 kerak. Loyiha GitHub Actions'da avtomatik yig'iladi (`.github/workflows/build.yml`): avval testlar, so'ng APK.

## Versiyalar va release

- `main` ga push qilinganda GitHub Actions testlarni o'tkazadi va APK yig'adi. APK **Actions → Artifacts** bo'limida bo'ladi, release chiqmaydi.
- Release faqat qo'lda chiqariladi: **Actions → Build APK → Run workflow** da versiyani yozing (masalan `1.0.1`), yoki `git tag v1.0.1 && git push origin v1.0.1`. Kiritilgan versiya ilova versiyasiga aylanadi.
- Imzo kaliti GitHub Secrets'da saqlanadi (`KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`). Kalit yo'qolsa yoki almashtirilsa, foydalanuvchilar ilovani o'chirib qayta o'rnatishga majbur bo'ladi, shuning uchun kalit fayli zaxirada turishi kerak.
- Versiya raqamlari: `v1.0.1` kichik tuzatish, `v1.1.0` yangi imkoniyat, `v2.0.0` katta o'zgarish.

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

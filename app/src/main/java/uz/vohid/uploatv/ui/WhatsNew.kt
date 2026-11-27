package uz.vohid.uploatv.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Vibration
import androidx.compose.material.icons.rounded.SwapVert
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentPaste
import androidx.compose.material.icons.rounded.PlaylistAddCheck
import androidx.compose.material.icons.rounded.SystemUpdate
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.TabletAndroid
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.PaddingValues

data class ChangeItem(val icon: ImageVector, val title: String, val text: String)

data class ChangeEntry(val id: String, val title: String, val date: String?, val items: List<ChangeItem>)

/** "Yangiliklar" ro'yxati. Yangi yozuv qo'shilganda [LATEST_ID] o'zgaradi va belgi (nuqta) yana chiqadi. */
object Changelog {
    val entries: List<ChangeEntry> = listOf(
        ChangeEntry(
            id = "v1.1.0",
            title = "Versiya 1.1.0: yangi imkoniyatlar",
            date = "28-noyabr 2026",
            items = listOf(
                ChangeItem(
                    Icons.Rounded.PlayCircle,
                    "Ilova ichida pleer",
                    "Yuklangan videoni galereyaga o'tmay, Yuklashlar sahifasidagi «Ko'rish» tugmasi bilan ko'ring.",
                ),
                ChangeItem(
                    Icons.Rounded.PlayCircle,
                    "Boyitilgan pleer",
                    "Ikki marta bosib 10 soniya oldinga yoki orqaga suring, tezlikni 0,5× dan 2× gacha tanlang. Ekran ustida ijro (PiP) va gorizontal to'liq ekran ham bor.",
                ),
                ChangeItem(
                    Icons.Rounded.PlaylistAddCheck,
                    "Playlistdan tanlab yuklash",
                    "Playlistdagi kerakli videolarni belgilab, faqat shularni yuklang.",
                ),
                ChangeItem(
                    Icons.Rounded.Link,
                    "Playlist: faqat yangilari",
                    "Playlistni qayta qo'ysangiz, avval yuklangan videolar o'tkazib yuboriladi. Sozlamalar → Yuklash bo'limida boshqariladi.",
                ),
                ChangeItem(
                    Icons.Rounded.ContentPaste,
                    "Ko'p havolani birdan joylash",
                    "Bir nechta havola qo'yilsa, ro'yxat chiqadi. Rejimni Sozlamalar → Yuklash bo'limidan tanlang.",
                ),
                ChangeItem(
                    Icons.Rounded.SwapVert,
                    "Navbatni boshqarish",
                    "Kutayotgan yuklashlarni sudrab tartiblang yoki «Birinchi» tugmasi bilan oldinga olib chiqing.",
                ),
                ChangeItem(
                    Icons.Rounded.ErrorOutline,
                    "Xatolar tushunarli",
                    "Xato sababi oddiy tilda yoziladi, yonida esa kerakli joyga olib boradigan tugma chiqadi.",
                ),
                ChangeItem(
                    Icons.Rounded.Link,
                    "Toza havolalar",
                    "Havoladagi kuzatuv qismlari olib tashlanadi, bir xil video qayta yuklanmaydi.",
                ),
                ChangeItem(
                    Icons.Rounded.SystemUpdate,
                    "Yangi versiya haqida xabar",
                    "Yangi reliz chiqsa, bosh sahifada bildiriladi.",
                ),
                ChangeItem(
                    Icons.Rounded.Notifications,
                    "Yangiliklar oynasi",
                    "Sarlavhadagi qo'ng'iroqcha va Ilova haqida bo'limida o'zgarishlar ro'yxati.",
                ),
            ),
        ),
        ChangeEntry(
            id = "v1.1.0-improve",
            title = "Versiya 1.1.0: qulaylik va ko'rinish",
            date = null,
            items = listOf(
                ChangeItem(
                    Icons.Rounded.Palette,
                    "Aksent rang va AMOLED",
                    "Tashqi ko'rinishda ilova rangini tanlang va qorong'i rejimda sof qora fonni yoqing.",
                ),
                ChangeItem(
                    Icons.Rounded.Vibration,
                    "Tugash tebranishi",
                    "Hamma yuklashlar tugaganda telefon tebranadi. Sozlamalar → Yuklash bo'limida sozlanadi.",
                ),
                ChangeItem(
                    Icons.Rounded.Speed,
                    "Yuklash tezligi",
                    "Parcha oqimlarini 4, 8 yoki 16 qilib tanlang. Bir vaqtda 1–3 ta yuklash va tezlik chegarasi ham bor.",
                ),
                ChangeItem(
                    Icons.Rounded.Tune,
                    "Fayl nomi shabloni",
                    "Saqlanadigan fayl nomini o'zingizga moslang. Yuklangan faylni ulashish va o'chirish ham mumkin.",
                ),
                ChangeItem(
                    Icons.Rounded.TabletAndroid,
                    "Katta ekran va gorizontal ko'rinish",
                    "Planshet, katlanuvchi telefon va yotiq holatda kontent markazga yig'iladi.",
                ),
                ChangeItem(
                    Icons.Rounded.Palette,
                    "Yangilangan Sozlamalar",
                    "Tartibli bo'limlar, ikonkali qatorlar, mavzu ko'rinishi va yuklovchi holat kartasi.",
                ),
                ChangeItem(
                    Icons.Rounded.ContentPaste,
                    "Qulayroq bosh sahifa",
                    "Yuklash tugmasi pastda, bitta joylash tugmasi, yuklanish paytida skelet va toza sarlavha.",
                ),
            ),
        ),
        ChangeEntry(
            id = "v1.1.0-tech",
            title = "Versiya 1.1.0: tezlik va ishonchlilik",
            date = null,
            items = listOf(
                ChangeItem(
                    Icons.Rounded.Bolt,
                    "Tezroq va yengilroq",
                    "Ilova silliqroq ishlaydi, o'rnatish fayli ancha kichraydi.",
                ),
                ChangeItem(
                    Icons.Rounded.Shield,
                    "Play Protect bilan muammo yo'q",
                    "Maxsus imkoniyatlar xizmati olib tashlandi: faqat plitka va doimiy tugma qoldi.",
                ),
                ChangeItem(
                    Icons.Rounded.Bolt,
                    "Barqaror ishlash",
                    "Tarix xavfsiz yoziladi, ro'yxatlar kamroq qayta chiziladi, surish foni faqat surish paytida ko'rinadi.",
                ),
            ),
        ),
    )

    val LATEST_ID: String = entries.first().id
}

/** Sarlavhadagi "Yangiliklar" tugmasi: yumshoq shisha doira, yangilik bo'lsa kichik nuqta bilan. */
@Composable
fun NewsButton(hasNews: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    val source = remember { MutableInteractionSource() }
    Box(modifier.size(44.dp)) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .pressScale(source)
                .glass(CircleShape, tint = scheme.primaryContainer, tintAlpha = 0.4f)
                .clickable(interactionSource = source, indication = null, role = Role.Button, onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Rounded.Notifications,
                contentDescription = "Xabarlar",
                tint = scheme.primary,
                modifier = Modifier.size(22.dp),
            )
        }
        if (hasNews) {
            Box(
                Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 1.dp, end = 1.dp)
                    .size(12.dp)
                    .background(scheme.surface, CircleShape)
                    .padding(2.dp)
                    .background(scheme.primary, CircleShape),
            )
        }
    }
}

/** Elementlar birin-ketin yumshoq paydo bo'ladi (faqat bir marta, yengil). */
@Composable
private fun Modifier.softReveal(index: Int): Modifier {
    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) { progress.animateTo(1f, tween(durationMillis = 420, delayMillis = 60 + index * 70)) }
    return this.graphicsLayer {
        alpha = progress.value
        translationY = (1f - progress.value) * 24.dp.toPx()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WhatsNewSheet(onSeen: () -> Unit, onDismiss: () -> Unit) {
    LaunchedEffect(Unit) { onSeen() }
    val scheme = MaterialTheme.colorScheme
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = scheme.surface,
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
        dragHandle = null,
    ) {
        Column(Modifier.fillMaxWidth()) {
            // Yumshoq rangli sarlavha
            Box(
                Modifier
                    .fillMaxWidth()
                    .background(Brush.verticalGradient(listOf(scheme.primaryContainer.copy(alpha = 0.45f), Color.Transparent)))
                    // Oyna to'liq ochilganda holat paneli (soat, batareya) ustiga chiqib ketmasligi uchun
                    .statusBarsPadding()
                    .padding(start = 24.dp, end = 16.dp, top = 16.dp, bottom = 12.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier.size(52.dp).clip(RoundedCornerShape(18.dp)).background(scheme.primaryContainer),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Rounded.Notifications, contentDescription = null, tint = scheme.onPrimaryContainer, modifier = Modifier.size(28.dp))
                    }
                    Spacer(Modifier.width(16.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Yangiliklar", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold)
                        Text(
                            "Ilovada nimalar o'zgardi",
                            style = MaterialTheme.typography.bodyMedium,
                            color = scheme.onSurfaceVariant,
                        )
                    }
                    Box(
                        Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(scheme.onSurface.copy(alpha = 0.07f))
                            .clickable(role = Role.Button, onClick = onDismiss),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            Icons.Rounded.Close,
                            contentDescription = "Yopish",
                            tint = scheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }
            }

            LazyColumn(
                modifier = Modifier.weight(1f, fill = false),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                itemsIndexed(Changelog.entries, key = { _, e -> e.id }, contentType = { _, _ -> "entry" }) { index, entry ->
                    EntryCard(entry, isLatest = entry.id == Changelog.LATEST_ID, modifier = Modifier.softReveal(index))
                }
            }

            FilledTonalButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
                    .height(52.dp),
            ) {
                Text("Tushunarli", style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}

@Composable
private fun EntryCard(entry: ChangeEntry, isLatest: Boolean, modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    Column(
        modifier = modifier
            .fillMaxWidth()
            .glass(RoundedCornerShape(24.dp), tintAlpha = 0.6f)
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(entry.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                entry.date?.let {
                    Text(it, style = MaterialTheme.typography.labelMedium, color = scheme.onSurfaceVariant)
                }
            }
            if (isLatest) {
                Spacer(Modifier.width(10.dp))
                Text(
                    "Yangi",
                    style = MaterialTheme.typography.labelMedium,
                    color = scheme.onPrimaryContainer,
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(scheme.primaryContainer)
                        .padding(horizontal = 10.dp, vertical = 3.dp),
                )
            }
        }
        entry.items.forEach { item ->
            Row(verticalAlignment = Alignment.Top) {
                Box(
                    Modifier.size(40.dp).clip(RoundedCornerShape(14.dp)).background(scheme.primary.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(item.icon, contentDescription = null, tint = scheme.primary, modifier = Modifier.size(22.dp))
                }
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(item.title, style = MaterialTheme.typography.titleSmall)
                    Spacer(Modifier.height(2.dp))
                    Text(item.text, style = MaterialTheme.typography.bodyMedium, color = scheme.onSurfaceVariant)
                }
            }
        }
    }
}

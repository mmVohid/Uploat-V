package uz.vohid.uploatv.ui

import android.app.Activity
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import kotlinx.coroutines.delay
import uz.vohid.uploatv.download.DownloadManager

/** Yuklangan videoni ilova ichida ijro etadi (galereyaga o'tmasdan). */
@Composable
fun PlayerScreen(id: String, onBack: () -> Unit) {
    val items by DownloadManager.items.collectAsStateWithLifecycle()
    val item = items.firstOrNull { it.id == id }
    val context = LocalContext.current
    val view = LocalView.current

    // Qora fonda holat paneli belgilari doim och rangda bo'lsin; chiqishda mavzuga qaytariladi
    DisposableEffect(Unit) {
        val window = (view.context as? Activity)?.window
        val controller = window?.let { WindowCompat.getInsetsController(it, view) }
        val prevStatus = controller?.isAppearanceLightStatusBars
        val prevNav = controller?.isAppearanceLightNavigationBars
        controller?.isAppearanceLightStatusBars = false
        controller?.isAppearanceLightNavigationBars = false
        onDispose {
            prevStatus?.let { controller.isAppearanceLightStatusBars = it }
            prevNav?.let { controller.isAppearanceLightNavigationBars = it }
        }
    }

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        val uri = item?.savedUri
        if (item == null || uri == null) {
            PlayerMessage("Video topilmadi", null, onBack)
        } else {
            var failed by remember { mutableStateOf(false) }
            // Ekran aylanganda joyidan davom etadi
            var savedPosition by rememberSaveable(id) { mutableStateOf(0L) }
            val player = remember(id) {
                ExoPlayer.Builder(context).build().apply {
                    setMediaItem(MediaItem.fromUri(Uri.parse(uri)))
                    seekTo(savedPosition)
                    prepare()
                    playWhenReady = true
                }
            }
            DisposableEffect(player) {
                val listener = object : Player.Listener {
                    override fun onPlayerError(error: PlaybackException) {
                        failed = true
                    }
                }
                player.addListener(listener)
                onDispose {
                    savedPosition = player.currentPosition
                    player.removeListener(listener)
                    player.release()
                }
            }
            LaunchedEffect(player) {
                while (true) {
                    savedPosition = player.currentPosition
                    delay(500)
                }
            }
            LifecycleEventEffect(Lifecycle.Event.ON_PAUSE) { player.pause() }

            if (failed) {
                PlayerMessage("Videoni ijro etib bo'lmadi", "Boshqa ilovada ochish") { openFile(context, item) }
            } else {
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { ctx ->
                        PlayerView(ctx).apply {
                            this.player = player
                            setShowNextButton(false)
                            setShowPreviousButton(false)
                            keepScreenOn = true
                        }
                    },
                    update = { it.player = player },
                )
            }

            // Yuqorida orqaga tugmasi va video nomi (yengil qora gradient ustida)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Brush.verticalGradient(listOf(Color(0xB3000000), Color.Transparent)))
                    .statusBarsPadding()
                    .padding(start = 4.dp, end = 16.dp, top = 4.dp, bottom = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Orqaga", tint = Color.White)
                }
                Text(
                    text = item.title,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = androidx.compose.material3.MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun PlayerMessage(text: String, actionLabel: String?, onAction: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().statusBarsPadding().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center,
    ) {
        Text(text, color = Color.White, style = androidx.compose.material3.MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(16.dp))
        Button(onClick = onAction) { Text(actionLabel ?: "Orqaga") }
    }
}

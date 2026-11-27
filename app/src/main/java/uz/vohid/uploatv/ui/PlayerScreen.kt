package uz.vohid.uploatv.ui

import android.app.Activity
import android.app.PictureInPictureParams
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.net.Uri
import android.os.Build
import android.util.Rational
import android.view.GestureDetector
import android.view.MotionEvent
import androidx.activity.ComponentActivity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.PictureInPictureAlt
import androidx.compose.material.icons.rounded.ScreenRotation
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.app.PictureInPictureModeChangedInfo
import androidx.core.util.Consumer
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.VideoSize
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import kotlinx.coroutines.delay
import uz.vohid.uploatv.download.DownloadManager

private const val SEEK_MS = 10_000L
private val SPEEDS = listOf(0.5f, 0.75f, 1f, 1.25f, 1.5f, 2f)

/** Ekran ustida ijro (Picture-in-Picture) uchun umumiy holat: MainActivity ham shundan foydalanadi. */
object PlayerPip {
    @Volatile var active = false
    @Volatile var playing = false
    @Volatile var aspect: Rational = Rational(16, 9)

    fun params(autoEnter: Boolean = false): PictureInPictureParams =
        PictureInPictureParams.Builder().setAspectRatio(aspect).apply {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) setAutoEnterEnabled(autoEnter)
        }.build()

    fun enter(activity: Activity) {
        runCatching { activity.enterPictureInPictureMode(params()) }
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

private fun speedLabel(s: Float) = (if (s % 1f == 0f) s.toInt().toString() else s.toString()) + "×"

/**
 * Yuklangan videoni ilova ichida ijro etadi: ikki marta bosib ±10 soniya, tezlik tanlash,
 * ekran ustida ijro (PiP) va gorizontal rejimda to'liq ekran.
 */
@Composable
fun PlayerScreen(id: String, onBack: () -> Unit) {
    val items by DownloadManager.items.collectAsStateWithLifecycle()
    val item = items.firstOrNull { it.id == id }
    val context = LocalContext.current
    val view = LocalView.current
    val activity = remember(context) { context.findActivity() }
    val landscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE

    var inPip by remember { mutableStateOf(activity?.isInPictureInPictureMode == true) }
    DisposableEffect(activity) {
        val componentActivity = activity as? ComponentActivity
        val listener = Consumer<PictureInPictureModeChangedInfo> { inPip = it.isInPictureInPictureMode }
        componentActivity?.addOnPictureInPictureModeChangedListener(listener)
        onDispose { componentActivity?.removeOnPictureInPictureModeChangedListener(listener) }
    }

    // Qora fonda holat paneli belgilari och rangda; chiqishda hammasi mavzuga qaytariladi
    val insetsController = remember(activity, view) {
        activity?.window?.let { WindowCompat.getInsetsController(it, view) }
    }
    DisposableEffect(insetsController) {
        val prevStatus = insetsController?.isAppearanceLightStatusBars
        val prevNav = insetsController?.isAppearanceLightNavigationBars
        insetsController?.isAppearanceLightStatusBars = false
        insetsController?.isAppearanceLightNavigationBars = false
        onDispose {
            insetsController?.show(WindowInsetsCompat.Type.systemBars())
            prevStatus?.let { insetsController.isAppearanceLightStatusBars = it }
            prevNav?.let { insetsController.isAppearanceLightNavigationBars = it }
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }

    // Gorizontal holatda (yoki PiP'da) tizim panellari yashiriladi: to'liq ekran
    LaunchedEffect(landscape, inPip, insetsController) {
        insetsController?.let { c ->
            if (landscape || inPip) {
                c.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                c.hide(WindowInsetsCompat.Type.systemBars())
            } else {
                c.show(WindowInsetsCompat.Type.systemBars())
            }
        }
    }

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        val uri = item?.savedUri
        if (item == null || uri == null) {
            PlayerMessage("Video topilmadi", null, onBack)
        } else {
            var failed by remember { mutableStateOf(false) }
            var playing by remember { mutableStateOf(false) }
            var speed by rememberSaveable { mutableFloatStateOf(1f) }
            var forceLandscape by rememberSaveable { mutableStateOf(false) }
            var speedMenu by remember { mutableStateOf(false) }
            var seekFlash by remember { mutableIntStateOf(0) } // -1 orqaga, 1 oldinga, 0 yo'q
            var flashKey by remember { mutableIntStateOf(0) }
            var savedPosition by rememberSaveable(id) { mutableStateOf(0L) }

            val player = remember(id) {
                ExoPlayer.Builder(context)
                    .setSeekBackIncrementMs(SEEK_MS)
                    .setSeekForwardIncrementMs(SEEK_MS)
                    .build().apply {
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

                    override fun onIsPlayingChanged(isPlaying: Boolean) {
                        playing = isPlaying
                        PlayerPip.playing = isPlaying
                    }

                    override fun onVideoSizeChanged(videoSize: VideoSize) {
                        if (videoSize.width > 0 && videoSize.height > 0) {
                            val ratio = (videoSize.width * videoSize.pixelWidthHeightRatio / videoSize.height).coerceIn(0.42f, 2.38f)
                            PlayerPip.aspect = Rational((ratio * 1000).toInt(), 1000)
                        }
                    }
                }
                player.addListener(listener)
                PlayerPip.active = true
                onDispose {
                    PlayerPip.active = false
                    PlayerPip.playing = false
                    savedPosition = player.currentPosition
                    player.removeListener(listener)
                    player.release()
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        runCatching { activity?.setPictureInPictureParams(PlayerPip.params(autoEnter = false)) }
                    }
                }
            }
            // Android 12+: ijro paytida ilovadan chiqilsa, video o'zi ekran ustiga o'tadi
            LaunchedEffect(playing) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    runCatching { activity?.setPictureInPictureParams(PlayerPip.params(autoEnter = playing)) }
                }
            }
            LaunchedEffect(speed, player) { player.setPlaybackSpeed(speed) }
            LaunchedEffect(forceLandscape) {
                activity?.requestedOrientation = if (forceLandscape) {
                    ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                } else {
                    ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                }
            }
            LaunchedEffect(player) {
                while (true) {
                    savedPosition = player.currentPosition
                    delay(500)
                }
            }
            LaunchedEffect(flashKey) {
                if (seekFlash != 0) {
                    delay(700)
                    seekFlash = 0
                }
            }
            // Ilova ko'rinmay qolsa (PiP bo'lmasa) video to'xtaydi
            LifecycleEventEffect(Lifecycle.Event.ON_STOP) {
                if (activity?.isInPictureInPictureMode != true) player.pause()
            }

            val onDoubleTapSeek by rememberUpdatedState<(Boolean) -> Unit> { forward ->
                if (forward) player.seekForward() else player.seekBack()
                seekFlash = if (forward) 1 else -1
                flashKey++
            }

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
                            // Bo'sh joyni ikki marta bosish: chap yarmi orqaga, o'ng yarmi oldinga; bir marta bosish: boshqaruv
                            val view = this
                            val detector = GestureDetector(
                                ctx,
                                object : GestureDetector.SimpleOnGestureListener() {
                                    override fun onDown(e: MotionEvent) = true

                                    override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
                                        if (view.isControllerFullyVisible) view.hideController() else view.showController()
                                        return true
                                    }

                                    override fun onDoubleTap(e: MotionEvent): Boolean {
                                        onDoubleTapSeek(e.x >= view.width / 2f)
                                        return true
                                    }
                                },
                            )
                            @Suppress("ClickableViewAccessibility")
                            setOnTouchListener { _, event -> detector.onTouchEvent(event) }
                        }
                    },
                    update = {
                        it.player = player
                        it.useController = !inPip
                    },
                )
            }

            // ±10 soniya belgisi
            AnimatedVisibility(
                visible = seekFlash != 0 && !inPip,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier.align(if (seekFlash < 0) Alignment.CenterStart else Alignment.CenterEnd),
            ) {
                Text(
                    text = if (seekFlash < 0) "−10 s" else "+10 s",
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier
                        .padding(horizontal = 32.dp)
                        .clip(RoundedCornerShape(50))
                        .background(Color(0x99000000))
                        .padding(horizontal = 18.dp, vertical = 10.dp),
                )
            }

            // Yuqori panel: orqaga, nom, tezlik, ekran ustida ijro, to'liq ekran
            if (!inPip) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Brush.verticalGradient(listOf(Color(0xB3000000), Color.Transparent)))
                        .statusBarsPadding()
                        .padding(start = 4.dp, end = 4.dp, top = 4.dp, bottom = 16.dp),
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
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.weight(1f),
                    )
                    Box {
                        TextButton(onClick = { speedMenu = true }) {
                            Text(speedLabel(speed), color = Color.White)
                        }
                        DropdownMenu(expanded = speedMenu, onDismissRequest = { speedMenu = false }) {
                            SPEEDS.forEach { s ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            speedLabel(s),
                                            color = if (s == speed) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                        )
                                    },
                                    onClick = {
                                        speed = s
                                        speedMenu = false
                                    },
                                )
                            }
                        }
                    }
                    IconButton(onClick = { activity?.let { PlayerPip.enter(it) } }) {
                        Icon(Icons.Rounded.PictureInPictureAlt, contentDescription = "Ekran ustida ijro", tint = Color.White)
                    }
                    IconButton(onClick = { forceLandscape = !forceLandscape }) {
                        Icon(
                            Icons.Rounded.ScreenRotation,
                            contentDescription = "To'liq ekran (gorizontal)",
                            tint = if (forceLandscape) MaterialTheme.colorScheme.primary else Color.White,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PlayerMessage(text: String, actionLabel: String?, onAction: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().statusBarsPadding().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(text, color = Color.White, style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(16.dp))
        Button(onClick = onAction) { Text(actionLabel ?: "Orqaga") }
    }
}

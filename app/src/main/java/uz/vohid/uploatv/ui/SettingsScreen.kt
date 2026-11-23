package uz.vohid.uploatv.ui

import android.os.Build
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import uz.vohid.uploatv.BuildConfig
import uz.vohid.uploatv.data.AudioFormat
import uz.vohid.uploatv.data.Quality
import uz.vohid.uploatv.data.ThemeMode

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SettingsSheet(vm: SettingsViewModel, onDismiss: () -> Unit) {
    val settings by vm.settings.collectAsStateWithLifecycle()
    val engine by vm.engine.collectAsStateWithLifecycle()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Text(
            text = "Sozlamalar",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(start = 24.dp, end = 24.dp, bottom = 8.dp),
        )
        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            item {
                Section("Ko'rinish") {
                    LabeledRow("Mavzu") {
                        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                            ThemeMode.entries.forEachIndexed { i, mode ->
                                SegmentedButton(
                                    selected = settings.themeMode == mode,
                                    onClick = { vm.setTheme(mode) },
                                    shape = SegmentedButtonDefaults.itemShape(i, ThemeMode.entries.size),
                                ) { Text(mode.label) }
                            }
                        }
                    }
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        SwitchRow(
                            title = "Dinamik ranglar",
                            subtitle = "Rangni telefon fon rasmiga moslash",
                            checked = settings.dynamicColor,
                            onChange = vm::setDynamicColor,
                        )
                    }
                }
            }

            item {
                Section("Yuklash") {
                    LabeledRow("Standart tur") {
                        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                            SegmentedButton(
                                selected = !settings.defaultAudio,
                                onClick = { vm.setDefaultAudio(false) },
                                shape = SegmentedButtonDefaults.itemShape(0, 2),
                            ) { Text("Video") }
                            SegmentedButton(
                                selected = settings.defaultAudio,
                                onClick = { vm.setDefaultAudio(true) },
                                shape = SegmentedButtonDefaults.itemShape(1, 2),
                            ) { Text("Audio") }
                        }
                    }
                    LabeledRow("Standart video sifati") {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Quality.entries.forEach { q ->
                                FilterChip(
                                    selected = settings.defaultQuality == q,
                                    onClick = { vm.setDefaultQuality(q) },
                                    label = { Text(q.label) },
                                )
                            }
                        }
                    }
                    LabeledRow("Standart audio format") {
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            AudioFormat.entries.forEach { f ->
                                FilterChip(
                                    selected = settings.audioFormat == f,
                                    onClick = { vm.setAudioFormat(f) },
                                    label = { Text(f.label) },
                                )
                            }
                        }
                    }
                    InfoRow("Saqlash joyi", "Download/Uploat-V")
                }
            }

            item {
                Section("Dvigatel") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text("yt-dlp", style = MaterialTheme.typography.bodyLarge)
                            Text(
                                text = engine.message ?: "Versiya: ${engine.version ?: "…"}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        if (engine.updating) {
                            CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.5.dp)
                        } else {
                            FilledTonalButton(onClick = vm::updateEngine) { Text("Yangilash") }
                        }
                    }
                    Text(
                        text = "Sayt o'zgarib video yuklanmay qolsa, avval dvigatelni yangilang",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                    )
                }
            }

            item {
                Section("Ilova haqida") {
                    InfoRow("Uploat-V", "Versiya ${BuildConfig.VERSION_NAME}")
                }
            }
        }
    }
}

@Composable
private fun Section(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 8.dp, bottom = 8.dp),
        )
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        ) {
            Column(
                modifier = Modifier.padding(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                content = content,
            )
        }
    }
}

@Composable
private fun LabeledRow(label: String, content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge)
        content()
    }
}

@Composable
private fun SwitchRow(title: String, subtitle: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
private fun InfoRow(title: String, value: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Text(title, style = MaterialTheme.typography.bodyLarge)
        Text(
            value,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

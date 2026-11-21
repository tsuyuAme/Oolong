package oolong.client.ui.page.settings.catalog

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import kotlinx.collections.immutable.persistentListOf
import oolong.client.ui.common.theme.compose.Corners
import oolong.client.ui.common.theme.compose.ExtendedTheme
import oolong.client.ui.common.theme.compose.Spacing
import oolong.client.ui.common.theme.compose.TextStyles
import oolong.client.ui.navigation.Navigator
import oolong.client.ui.page.LocalNavigator
import oolong.client.ui.widgets.compose.BackNavigationIcon
import oolong.client.ui.widgets.compose.Button
import oolong.client.ui.widgets.compose.Chip
import oolong.client.ui.widgets.compose.GlassScaffold
import oolong.client.ui.widgets.compose.GlassTopBar
import oolong.client.ui.widgets.compose.GlassTopBarTitle
import oolong.client.ui.widgets.compose.SecondaryButton
import oolong.client.ui.widgets.compose.SegmentedControl
import oolong.client.ui.widgets.compose.Slider
import oolong.client.ui.widgets.compose.Stepper
import oolong.client.ui.widgets.compose.Switch
import oolong.client.ui.widgets.compose.TextButton
import oolong.client.ui.widgets.compose.verticalScrollUnderBar
import kotlin.math.roundToInt

/**
 * Every control of the design system on one page, in each of its states, to check them together
 * on a device (design-system-plan.md §4, step 4). Debug builds only, so its copy is not translated.
 */
@Composable
fun ControlCatalogPage(
    navigator: Navigator = LocalNavigator.current,
) {
    GlassScaffold(
        backgroundColor = Color.Transparent,
        topBar = {
            GlassTopBar(
                title = { GlassTopBarTitle(text = "控件目录") },
                navigationIcon = { BackNavigationIcon(onBackPressed = { navigator.navigateUp() }) },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .verticalScrollUnderBar()
                .padding(padding)
                .padding(horizontal = Spacing.md, vertical = Spacing.xs),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            ControlCatalog()
        }
    }
}

@Composable
fun ColumnScope.ControlCatalog() {
    Section("按钮") {
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
            Button(onClick = {}) { Text("主要") }
            SecondaryButton(onClick = {}) { Text("次要") }
            TextButton(onClick = {}) { Text("文字") }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
            Button(onClick = {}, enabled = false) { Text("主要") }
            SecondaryButton(onClick = {}, enabled = false) { Text("次要") }
            TextButton(onClick = {}, enabled = false) { Text("文字") }
        }
    }
    Section("标签") {
        var picked by remember { mutableIntStateOf(0) }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.xs), verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
            listOf("全部", "精品", "最新").forEachIndexed { index, label ->
                Chip(text = label, invertColor = picked == index, onClick = { picked = index })
            }
            Chip(text = "不可点按")
        }
    }
    Section("开关") {
        var on by remember { mutableStateOf(true) }
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.md), verticalAlignment = Alignment.CenterVertically) {
            Switch(checked = on, onCheckedChange = { on = it })
            Switch(checked = !on, onCheckedChange = { on = !it })
            Switch(checked = true, onCheckedChange = {}, enabled = false)
            Switch(checked = false, onCheckedChange = {}, enabled = false)
        }
    }
    Section("滑块") {
        var free by remember { mutableFloatStateOf(0.4f) }
        var stepped by remember { mutableFloatStateOf(2f) }
        Label("连续 · ${(free * 100).roundToInt()}%")
        Slider(value = free, onValueChange = { free = it })
        Label("分档 · ${stepped.roundToInt()}")
        Slider(value = stepped, onValueChange = { stepped = it }, valueRange = 0f..6f, steps = 5)
        Label("禁用")
        Slider(value = 0.6f, onValueChange = {}, enabled = false)
    }
    Section("分段控件") {
        var selected by remember { mutableIntStateOf(0) }
        SegmentedControl(options = persistentListOf("浅色", "深色", "跟随系统"), selected = selected, onSelect = { selected = it })
    }
    Section("步进器") {
        var level by remember { mutableIntStateOf(4) }
        Stepper(value = level, range = 1..18, text = "$level 级", down = "减少", up = "增加", onChange = { level = it })
    }
}

@Composable
private fun Section(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(ExtendedTheme.colors.card, Corners.md)
            .padding(Spacing.md),
        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        Text(text = title, style = TextStyles.nameCompact, color = ExtendedTheme.colors.textSecondary)
        content()
    }
}

@Composable
private fun Label(text: String) {
    Text(text = text, style = TextStyles.caption, color = ExtendedTheme.colors.textSecondary)
}

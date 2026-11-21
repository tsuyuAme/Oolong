package oolong.client.ui.common.prefs

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.key
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import oolong.client.ui.common.theme.compose.ExtendedTheme
import oolong.client.ui.common.theme.compose.Radius
import oolong.client.ui.common.theme.compose.SmoothCornerShape
import oolong.client.ui.widgets.compose.Container
import oolong.client.ui.widgets.compose.GlassScaffold
import oolong.client.ui.widgets.compose.maskTo
import oolong.client.ui.widgets.compose.verticalScrollUnderBar

lateinit var LocalPrefsDataStore: ProvidableCompositionLocal<DataStore<Preferences>>

/**
 * Main preference screen which holds [PrefsListItem]s
 *
 * @param dataStore DataStore which will be used to save all the preferences
 * @param modifier Modifier applied to the column holding the list of Prefs
 * @param contentPadding padding around the list, such as the bars of a [GlassScaffold]
 *
 */
@Composable
fun PrefsScreen(
    dataStore: DataStore<Preferences>,
    modifier: Modifier = Modifier,
    dividerThickness: Dp = 1.dp, // 0 for no divider
    dividerIndent: Dp = 0.dp, // indents on both sides
    contentPadding: PaddingValues = PaddingValues(0.dp),
    scrollState: ScrollState = rememberScrollState(),
    content: PrefsScope.() -> Unit
) {
    LocalPrefsDataStore = staticCompositionLocalOf { dataStore }
    val prefsScope = PrefsScopeImpl().apply(content)

    // Now the dataStore can be accessed by calling LocalPrefsDataStore.current from any child Pref
    CompositionLocalProvider(LocalPrefsDataStore provides dataStore) {
        Container {
            // A plain column rather than a lazy list: settings pages are short, and with every row
            // laid out a search result can scroll to any of them. Long data lists don't belong here.
            Column(
                modifier = modifier
                    .fillMaxSize()
                    .verticalScrollUnderBar(scrollState)
                    .padding(contentPadding),
            ) {
                val items = prefsScope.prefsItems
                items.forEachIndexed { index, item ->
                    key(index) {
                        if (item.isSetting) {
                            SettingInGroup(items, index, dividerThickness, dividerIndent) { prefsScope.getPrefsItem(index)() }
                        } else {
                            prefsScope.getPrefsItem(index)()
                        }
                    }
                }
            }
        }
    }
}

/**
 * The setting at [index] of [items]: settings next to each other form one group, set apart by
 * headers and gaps, and sit in one grey block.
 */
@Composable
private fun SettingInGroup(
    items: List<PrefsItem>,
    index: Int,
    dividerThickness: Dp,
    dividerIndent: Dp,
    content: @Composable () -> Unit,
) {
    val first = index == 0 || !items[index - 1].isSetting
    val last = index == items.lastIndex || !items[index + 1].isSetting
    val shape = SmoothCornerShape(CardRadius, top = first, bottom = last)
    Box(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .padding(top = if (first && index == 0) 8.dp else 0.dp)
            .background(ExtendedTheme.colors.chip, shape)
            .then(if (first || last) Modifier.maskTo(shape) else Modifier.clipToBounds())
            .padding(top = if (first) 4.dp else 0.dp, bottom = if (last) 4.dp else 0.dp)
    ) {
        content()
    }
    if (dividerThickness != 0.dp && !last) {
        Divider(thickness = dividerThickness, indent = dividerIndent)
    }
}

private val CardRadius = Radius.md

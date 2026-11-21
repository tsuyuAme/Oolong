package oolong.client.ui.page.settings.update

import android.text.format.Formatter
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.ProvideTextStyle
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState
import oolong.client.BuildConfig
import oolong.client.R
import oolong.client.repository.InstalledIsPreRelease
import oolong.client.repository.Release
import oolong.client.repository.UpdateChannel
import oolong.client.repository.UpdateCheck
import oolong.client.repository.UpdateDownload
import oolong.client.repository.UpdateVerificationException
import oolong.client.repository.notesFor
import oolong.client.ui.common.prefs.PrefsChoiceValue
import oolong.client.ui.common.theme.compose.ExtendedTheme
import oolong.client.ui.common.theme.compose.PressFeedback
import oolong.client.ui.common.theme.compose.Radius
import oolong.client.ui.common.theme.compose.SmoothCornerShape
import oolong.client.ui.common.theme.compose.TextStyles
import oolong.client.ui.common.theme.compose.pressScale
import oolong.client.ui.navigation.Navigator
import oolong.client.ui.page.LocalNavigator
import oolong.client.ui.page.settings.about.AboutMark
import oolong.client.ui.page.settings.about.LensOverlay
import oolong.client.ui.page.settings.about.MarkLens
import oolong.client.ui.widgets.compose.BackNavigationIcon
import oolong.client.ui.widgets.compose.GlassScaffold
import oolong.client.ui.widgets.compose.GlassTopBar
import oolong.client.ui.widgets.compose.GlassTopBarTitle
import oolong.client.ui.widgets.compose.Refocus
import oolong.client.ui.widgets.compose.Switch
import oolong.client.ui.widgets.compose.TextButton
import oolong.client.ui.widgets.compose.bottomEdgeGlass
import oolong.client.ui.widgets.compose.choiceMenuItems
import oolong.client.ui.widgets.compose.clickMenu
import oolong.client.ui.widgets.compose.maskTo
import oolong.client.ui.widgets.compose.menuAnchor
import oolong.client.ui.widgets.compose.rememberMenuTriggerState

/**
 * Software update, a page of its own (design-system.md §9.5): the app and where its version stands,
 * what the release offered changes, the update settings, and one button at the bottom that checks,
 * downloads (showing how far it has got) and installs.
 */
@Composable
fun UpdatePage(
    navigator: Navigator = LocalNavigator.current,
    viewModel: UpdateViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val scroll = rememberScrollState()

    GlassScaffold(
        backgroundColor = Color.Transparent,
        // Over the page, so its scroll edge can blur the page under it
        bottomBar = {
            Dock(
                state = state,
                onCheck = viewModel::check,
                onDownload = viewModel::download,
                onInstall = viewModel::install,
                contentLeft = { scroll.maxValue - scroll.value },
            )
        },
        topBar = {
            GlassTopBar(
                title = { GlassTopBarTitle(text = stringResource(id = R.string.title_software_update)) },
                navigationIcon = { BackNavigationIcon(onBackPressed = { navigator.navigateUp() }) },
            )
        },
    ) { contentPaddings ->
        val lens = remember { MarkLens() }
        val page = rememberHazeState()
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .hazeSource(page)
                    .verticalScroll(scroll)
                    .padding(contentPaddings)
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Hero(lens, state)
                // The new version's notes when there is one; else what this version brought
                val context = LocalContext.current
                val locale = LocalConfiguration.current.locales[0]
                val offered = state.offered
                if (offered != null) {
                    Notes(
                        version = offered.versionName,
                        details = "${Formatter.formatShortFileSize(context, offered.size)} · ${offered.publishedAt}",
                        groups = offered.notesFor(locale),
                    )
                } else if (state.installedNotes.isNotEmpty()) {
                    Notes(version = BuildConfig.VERSION_NAME, groups = state.installedNotes.notesFor(locale))
                }
                val busy = state.download is UpdateDownload.Running || state.download is UpdateDownload.Ready
                if (offered != null && !offered.required && !busy) {
                    TextButton(onClick = { viewModel.skip(offered) }, color = ExtendedTheme.colors.textSecondary) {
                        Text(text = stringResource(id = R.string.update_skip), style = TextStyles.caption)
                    }
                }
                Settings(state, onAutoCheck = viewModel::setAutoCheck, onChannel = viewModel::setChannel)
            }
            LensOverlay(lens = lens, page = page, markSize = MarkSize)
        }
    }
}

/** The mark, the name, the version and where it stands. */
@Composable
private fun Hero(lens: MarkLens, state: UpdateState) {
    val colors = ExtendedTheme.colors
    Column(
        modifier = Modifier.padding(top = 12.dp, bottom = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        AboutMark(lens = lens, size = MarkSize)
        Text(
            text = stringResource(id = R.string.app_name),
            modifier = Modifier.padding(top = 16.dp),
            style = Name.copy(color = colors.text),
        )
        Text(
            text = stringResource(id = R.string.tip_about, BuildConfig.VERSION_NAME),
            modifier = Modifier.padding(top = 2.dp),
            style = TextStyles.caption.copy(color = colors.textSecondary),
        )
        val status = when (val check = state.check) {
            UpdateCheck.Idle -> ""
            UpdateCheck.Checking -> stringResource(id = R.string.update_status_checking)
            is UpdateCheck.Failed -> stringResource(id = R.string.update_status_failed)
            is UpdateCheck.Done -> if (check.latest.isNewer) {
                stringResource(id = R.string.update_status_found, check.latest.versionName)
            } else {
                stringResource(id = R.string.update_status_latest)
            }
        }
        Refocus(targetState = status, modifier = Modifier.padding(top = 12.dp).heightIn(min = 20.dp)) {
            Text(
                text = it,
                style = TextStyles.caption.copy(
                    color = if (state.offered != null) colors.emphasis else colors.textSecondary,
                    fontWeight = if (state.offered != null) FontWeight.Medium else FontWeight.Normal,
                ),
            )
        }
    }
}

/** What [release] changes, grouped as its manifest groups it. */
@Composable
private fun Notes(version: String, groups: Map<String, List<String>>, details: String? = null) {
    val colors = ExtendedTheme.colors
    Column(
        modifier = Modifier
            .widthIn(max = 600.dp)
            .fillMaxWidth()
            .background(colors.chip, CardShape)
            .padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 4.dp),
    ) {
        Row(modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp)) {
            Text(
                text = version,
                style = TextStyles.caption.copy(color = colors.textSecondary),
                modifier = Modifier.weight(1f),
            )
            if (details != null) Text(text = details, style = TextStyles.caption.copy(color = colors.textSecondary))
        }
        groups.forEach { (group, items) ->
            // Above its items, which may have titles of their own
            Text(text = group, style = TextStyles.name.copy(color = colors.text))
            Column(modifier = Modifier.padding(top = 2.dp, bottom = 20.dp)) {
                items.forEach { item ->
                    // One kind of point for all: "Title\nwhat it does for you" sets the title as the point's
                    // words and what it does under them, a plain item has the words alone
                    val title = item.substringBefore('\n', missingDelimiterValue = "")
                    Row(modifier = Modifier.padding(top = 6.dp)) {
                        Text(text = "·", style = TextStyles.bodyCompact.copy(color = colors.textSecondary), modifier = Modifier.padding(end = 8.dp))
                        Column {
                            if (title.isNotEmpty()) {
                                Text(text = title, style = TextStyles.bodyCompact.copy(color = colors.text, fontWeight = FontWeight.Medium))
                                Text(text = item.substringAfter('\n'), style = TextStyles.caption.copy(color = colors.textSecondary))
                            } else {
                                Text(text = item, style = TextStyles.bodyCompact.copy(color = colors.text))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Settings(state: UpdateState, onAutoCheck: (Boolean) -> Unit, onChannel: (UpdateChannel) -> Unit) {
    val colors = ExtendedTheme.colors
    Text(
        text = stringResource(id = R.string.update_settings),
        style = TextStyles.caption.copy(color = colors.textSecondary),
        modifier = Modifier.widthIn(max = 600.dp).fillMaxWidth().padding(start = 16.dp, top = 20.dp, bottom = 8.dp),
    )
    Column(
        modifier = Modifier
            .widthIn(max = 600.dp)
            .fillMaxWidth()
            .background(colors.chip, CardShape)
            .maskTo(CardShape),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp)
                .toggleable(
                    value = state.autoCheck,
                    interactionSource = null,
                    indication = PressFeedback.Overlay,
                    role = Role.Switch,
                    onValueChange = onAutoCheck,
                )
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(text = stringResource(id = R.string.update_auto_check), style = TextStyles.bodyCompact.copy(color = colors.text), modifier = Modifier.weight(1f))
            Switch(checked = state.autoCheck, onCheckedChange = onAutoCheck, modifier = Modifier.clearAndSetSemantics {})
        }
        // Chosen in a menu growing out of the value, as settings' choices are
        val menu = rememberMenuTriggerState()
        val channels = UpdateChannel.entries.map { it to stringResource(id = it.label) }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp)
                .clickMenu(items = { choiceMenuItems(channels, state.channel, onChannel) }, indication = PressFeedback.Overlay, state = menu)
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(text = stringResource(id = R.string.update_channel), style = TextStyles.bodyCompact.copy(color = colors.text), modifier = Modifier.weight(1f))
            ProvideTextStyle(TextStyles.bodyCompact.copy(color = colors.textSecondary)) {
                PrefsChoiceValue(text = stringResource(id = state.channel.label), modifier = Modifier.menuAnchor(menu))
            }
        }
        // Android can't go back to an older version, so stable has nothing for a pre-release until it passes it
        if (state.channel == UpdateChannel.Stable && InstalledIsPreRelease) {
            Text(
                text = stringResource(id = R.string.update_channel_stable_note),
                style = TextStyles.caption.copy(color = colors.textSecondary),
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 14.dp),
            )
        }
    }
}

/**
 * The button at the bottom, over the page faded out behind it while [contentLeft] pixels of the
 * page are still to come: it checks, downloads and installs, and is the download's progress bar
 * meanwhile. Above it, why it couldn't go on, when it couldn't.
 */
@Composable
private fun Dock(
    state: UpdateState,
    onCheck: () -> Unit,
    onDownload: (Release) -> Unit,
    onInstall: (UpdateDownload.Ready) -> Unit,
    contentLeft: () -> Int,
    modifier: Modifier = Modifier,
) {
    val colors = ExtendedTheme.colors
    Column(
        modifier = modifier
            .fillMaxWidth()
            // The page fades out under the button, as it does under the bar at the top
            .bottomEdgeGlass(contentLeft)
            .navigationBarsPadding()
            .padding(start = 16.dp, end = 16.dp, top = 24.dp, bottom = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        val download = state.download
        val note = when {
            download is UpdateDownload.Ready && state.installing -> stringResource(id = R.string.update_installing_note)
            download is UpdateDownload.Failed -> stringResource(
                id = if (download.error is UpdateVerificationException) R.string.update_verify_failed else R.string.update_download_failed,
            )
            else -> null
        }
        if (note != null) {
            Text(text = note, style = TextStyles.caption.copy(color = colors.textSecondary), textAlign = TextAlign.Center)
        }
        val offered = state.offered
        when {
            download is UpdateDownload.Running -> ProgressButton(
                text = stringResource(id = R.string.update_downloading, (download.progress * 100).toInt()),
                progress = download.progress,
                onClick = {},
            )
            download is UpdateDownload.Ready && state.installing ->
                ProgressButton(text = stringResource(id = R.string.update_installing), enabled = false, onClick = {})
            download is UpdateDownload.Ready -> ProgressButton(text = stringResource(id = R.string.update_install), onClick = { onInstall(download) })
            download is UpdateDownload.Failed ->
                ProgressButton(text = stringResource(id = R.string.button_retry), onClick = { onDownload(download.release) })
            offered != null -> ProgressButton(text = stringResource(id = R.string.update_download), onClick = { onDownload(offered) })
            state.check == UpdateCheck.Checking ->
                ProgressButton(text = stringResource(id = R.string.update_status_checking), enabled = false, onClick = {})
            else -> ProgressButton(text = stringResource(id = R.string.update_check), onClick = onCheck)
        }
    }
}

/**
 * The capsule button, filled with the emphasis colour; given a [progress], only that much of it is
 * filled, from the left, over a tint of the same colour, the label turning over where the fill is.
 */
@Composable
private fun ProgressButton(text: String, onClick: () -> Unit, progress: Float? = null, enabled: Boolean = true) {
    val colors = ExtendedTheme.colors
    val interactionSource = remember { MutableInteractionSource() }
    val label = TextStyles.label.copy(fontSize = 16.sp)
    Box(
        modifier = Modifier
            .widthIn(max = 480.dp)
            .fillMaxWidth()
            .height(52.dp)
            .pressScale(interactionSource)
            .background(if (progress != null) colors.emphasis.copy(alpha = 0.18f) else colors.emphasis, SmoothCornerShape.Capsule)
            .clickable(
                interactionSource = interactionSource,
                indication = PressFeedback.overlay(SmoothCornerShape.Capsule),
                enabled = enabled && progress == null,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        if (progress == null) {
            Text(text = text, style = label.copy(color = colors.onEmphasis))
        } else {
            Text(text = text, style = label.copy(color = colors.emphasis))
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .drawWithContent { clipRect(right = size.width * progress) { this@drawWithContent.drawContent() } }
                    .background(colors.emphasis, SmoothCornerShape.Capsule),
                contentAlignment = Alignment.Center,
            ) {
                Text(text = text, style = label.copy(color = colors.onEmphasis))
            }
        }
    }
}

private val UpdateChannel.label: Int
    get() = when (this) {
        UpdateChannel.Stable -> R.string.update_channel_stable
        UpdateChannel.Beta -> R.string.update_channel_beta
    }

private val MarkSize = 88.dp

/** The name as the About page sets it. */
private val Name = TextStyle(fontSize = 28.sp, lineHeight = 36.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-0.3).sp)

/** As settings groups are drawn. */
private val CardShape = SmoothCornerShape(Radius.md)

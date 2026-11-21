package oolong.client

import android.annotation.SuppressLint
import android.app.job.JobInfo
import android.app.job.JobScheduler
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Process
import android.os.SystemClock
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withLink
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toDrawable
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.window.layout.FoldingFeature
import androidx.window.layout.WindowInfoTracker
import coil3.compose.AsyncImage
import dagger.hilt.android.AndroidEntryPoint
import dev.chrisbanes.haze.rememberHazeState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import oolong.client.api.retrofit.exception.userMessage
import oolong.client.arch.AppContent
import oolong.client.arch.GlobalEvent
import oolong.client.arch.emitGlobalEvent
import oolong.client.arch.onGlobalEvent
import oolong.client.components.ClipBoardForumLink
import oolong.client.components.ClipBoardLink
import oolong.client.components.ClipBoardLinkDetector
import oolong.client.components.ClipBoardThreadLink
import oolong.client.components.CrashLogs
import oolong.client.components.CrashReports
import oolong.client.components.UsageStats
import oolong.client.repository.AccountRepository
import oolong.client.services.AutoSign
import oolong.client.services.NotifyJobService
import oolong.client.ui.common.LocalNow
import oolong.client.ui.common.navigation.PageMorphHost
import oolong.client.ui.common.rememberMinuteClock
import oolong.client.ui.common.theme.compose.ExtendedTheme
import oolong.client.ui.common.theme.compose.SmoothCornerShape
import oolong.client.ui.navigation.AppNavDisplay
import oolong.client.ui.navigation.BackStackNavigator
import oolong.client.ui.navigation.CrashLogsKey
import oolong.client.ui.navigation.ForumKey
import oolong.client.ui.navigation.Key
import oolong.client.ui.navigation.Navigator
import oolong.client.ui.navigation.Page
import oolong.client.ui.navigation.StorageKey
import oolong.client.ui.navigation.ThreadKey
import oolong.client.ui.navigation.WebViewKey
import oolong.client.ui.navigation.rememberAppBackStack
import oolong.client.ui.navigation.toDeepLinkKey
import oolong.client.ui.page.LocalNavigator
import oolong.client.ui.page.landing.LandingHost
import oolong.client.ui.page.landing.UsageStatsContentDialog
import oolong.client.ui.page.settings.block.BlockShortcuts
import oolong.client.ui.page.settings.block.BlockShortcutsHost
import oolong.client.ui.page.settings.block.LocalBlockShortcuts
import oolong.client.ui.page.settings.theme.TranslucentWallpaper
import oolong.client.ui.page.settings.theme.redrawOutdatedWallpaper
import oolong.client.ui.page.settings.theme.rememberWallpaperDim
import oolong.client.ui.page.settings.theme.repairWallpaper
import oolong.client.ui.utils.DevicePosture
import oolong.client.ui.utils.isBookPosture
import oolong.client.ui.utils.isSeparating
import oolong.client.ui.widgets.compose.AlertDialog
import oolong.client.ui.widgets.compose.AppDialogHost
import oolong.client.ui.widgets.compose.Avatar
import oolong.client.ui.widgets.compose.AvatarIcon
import oolong.client.ui.widgets.compose.CopySheetHost
import oolong.client.ui.widgets.compose.CopySheets
import oolong.client.ui.widgets.compose.Dialog
import oolong.client.ui.widgets.compose.DialogNegativeButton
import oolong.client.ui.widgets.compose.DialogPositiveButton
import oolong.client.ui.widgets.compose.LocalCopySheets
import oolong.client.ui.widgets.compose.LocalWallpaperGlass
import oolong.client.ui.widgets.compose.Sizes
import oolong.client.ui.widgets.compose.rememberDialogState
import oolong.client.ui.widgets.compose.wallpaperGlassSource
import oolong.client.utils.ClientUtils
import oolong.client.utils.JobServiceUtil
import oolong.client.utils.PermissionUtils
import oolong.client.utils.PickMediasRequest
import oolong.client.utils.QuickPreviewUtil
import oolong.client.utils.ThemeUtil
import oolong.client.utils.TiebaUtil
import oolong.client.utils.appPreferences
import oolong.client.utils.compose.LaunchActivityForResult
import oolong.client.utils.compose.LaunchActivityRequest
import oolong.client.utils.isIgnoringBatteryOptimizations
import oolong.client.utils.newIntentFilter
import oolong.client.utils.registerPickMediasLauncher
import oolong.client.utils.requestIgnoreBatteryOptimizations
import oolong.client.utils.requestPermission
import oolong.client.utils.toTiebaKey
import java.io.File
import java.util.Locale
import javax.inject.Inject

val LocalNotificationCountFlow =
    staticCompositionLocalOf<Flow<Int>> { throw IllegalStateException("not allowed here!") }
val LocalDevicePosture =
    staticCompositionLocalOf<State<DevicePosture>> { throw IllegalStateException("not allowed here!") }

// Tieba links from outside the app, opened natively when they have a counterpart
private val EXTERNAL_LINK_SCHEMES = setOf("http", "https", "tbfrs", "tbpb", "com.baidu.tieba")

// Length of the splash icon animation in ic_splash_screen_anim
internal const val SPLASH_ICON_MILLIS = 400L

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    @Inject
    lateinit var accountRepository: AccountRepository

    @Inject
    lateinit var blockShortcuts: BlockShortcuts

    private val handler = Handler(Looper.getMainLooper())
    private val newMessageReceiver: BroadcastReceiver = NewMessageReceiver()

    private val notificationCountFlow: MutableSharedFlow<Int> =
        MutableSharedFlow(replay = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)

    private val pickMediasLauncher =
        registerPickMediasLauncher {
            lifecycleScope.emitGlobalEvent(GlobalEvent.SelectedImages(it.id, it.uris))
        }

    private val mLaunchActivityForResultLauncher = registerForActivityResult(
        LaunchActivityForResult()
    ) {
        lifecycleScope.emitGlobalEvent(GlobalEvent.ActivityResult(it.requesterId, it.resultCode, it.intent))
    }

    private val devicePostureFlow: StateFlow<DevicePosture> by lazy {
        WindowInfoTracker.getOrCreate(this)
            .windowLayoutInfo(this)
            .flowWithLifecycle(lifecycle)
            .map { layoutInfo ->
                val foldingFeature =
                    layoutInfo.displayFeatures
                        .filterIsInstance<FoldingFeature>()
                        .firstOrNull()
                when {
                    isBookPosture(foldingFeature) ->
                        DevicePosture.BookPosture(foldingFeature.bounds)

                    isSeparating(foldingFeature) ->
                        DevicePosture.Separating(foldingFeature.bounds, foldingFeature.orientation)

                    else -> DevicePosture.NormalPosture
                }
            }
            .stateIn(
                scope = lifecycleScope,
                started = SharingStarted.Eagerly,
                initialValue = DevicePosture.NormalPosture
            )
    }

    private var navigator: Navigator? = null

    /** A page linked to before the pages are shown, which then start with it over the first. */
    private var linkedKey: Key? = null

    private fun navigate(key: Key) {
        navigator?.navigate(key) ?: run { linkedKey = key }
    }

    /** Opens the page [intent] links to, over the pages already open. */
    private fun checkIntent(intent: Intent) {
        // The system's app details, managing the app's space
        if (intent.component?.className == "${MainActivity::class.java.`package`?.name}.ManageSpace") {
            return navigate(StorageKey)
        }
        val uri = intent.data ?: return
        val key = when (val scheme = uri.scheme?.lowercase()) {
            "oolong" -> uri.toDeepLinkKey()
            in EXTERNAL_LINK_SCHEMES -> uri.toTiebaKey()
                ?: if (scheme == "http" || scheme == "https") WebViewKey(uri.toString()) else null

            else -> null
        }
        key?.let { navigate(it) }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        checkIntent(intent)
    }

    private fun fetchAccount() {
        val account = accountRepository.current.value ?: return
        lifecycleScope.launch {
            try {
                accountRepository.refresh(account)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                toastShort(e.userMessage(this@MainActivity))
                e.printStackTrace()
            }
        }
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && accountRepository.current.value != null) {
            requestPermission {
                permissions = listOf(PermissionUtils.POST_NOTIFICATIONS)
                description = getString(R.string.desc_permission_post_notifications)
            }
        }
    }

    // A day whose alarm never went off is signed now, while the app can start the signing itself
    private fun initAutoSign() {
        runCatching {
            AutoSign.schedule(this)
            AutoSign.catchUp(this, foreground = true)
        }
    }

    override fun onStart() {
        super.onStart()
        ClipBoardLinkDetector.onStart()
        runCatching {
            ContextCompat.registerReceiver(
                this,
                newMessageReceiver,
                newIntentFilter(NotifyJobService.ACTION_NEW_MESSAGE),
                ContextCompat.RECEIVER_NOT_EXPORTED
            )
            startService(Intent(this, NotifyJobService::class.java))
            val builder = JobInfo.Builder(
                JobServiceUtil.getJobId(this),
                ComponentName(this, NotifyJobService::class.java)
            )
                .setPersisted(true)
                .setPeriodic(30 * 60 * 1000L)
                .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
            val jobScheduler = getSystemService(Context.JOB_SCHEDULER_SERVICE) as JobScheduler
            jobScheduler.schedule(builder.build())
        }
        handler.postDelayed({
            requestNotificationPermission()
        }, 100)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        // Let the icon finish bubbling (it starts with the process), but never wait past that
        val iconDone = Process.getStartUptimeMillis() + SPLASH_ICON_MILLIS
        installSplashScreen().setKeepOnScreenCondition { SystemClock.uptimeMillis() < iconDone }
        super.onCreate(savedInstanceState)
        // Formatting that reads the default locale, such as counts, follows the app's language,
        // which before Android 13 only reaches the activity's configuration
        resources.configuration.locales[0]?.let { Locale.setDefault(it) }
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContent { AppContent(this, accountRepository) { Content() } }
        // The condition is only checked when a frame is drawn
        window.decorView.postDelayed(
            { window.decorView.invalidate() },
            (iconDone - SystemClock.uptimeMillis()).coerceAtLeast(0)
        )
        window.decorView.setBackgroundColor(0)
        window.setBackgroundDrawable(0.toDrawable())
        lifecycleScope.launch {
            ClientUtils.setActiveTimestamp()
        }
        // Once per activity: called from Content, these would add a listener on every recomposition
        lifecycleScope.onGlobalEvent<GlobalEvent.StartSelectImages> {
            pickMediasLauncher.launch(PickMediasRequest(it.id, it.maxCount))
        }
        lifecycleScope.onGlobalEvent<GlobalEvent.StartActivityForResult> {
            mLaunchActivityForResultLauncher.launch(LaunchActivityRequest(it.requesterId, it.intent))
        }
        // A recreated activity restores its pages, the linked one among them
        if (savedInstanceState == null) intent?.let { checkIntent(it) }
        fetchAccount()
        initAutoSign()
        // Notifications are about the account's messages, so they are asked for once one logs in
        lifecycleScope.launch {
            accountRepository.current.map { it != null }.distinctUntilChanged().drop(1).filter { it }.collect {
                requestNotificationPermission()
            }
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        // Only a focused window is given the clipboard
        if (hasFocus) ClipBoardLinkDetector.check(this)
    }

    private fun openClipBoardLink(link: ClipBoardLink) {
        when (link) {
            is ClipBoardThreadLink -> {
                link.threadId.toLongOrNull()?.let { navigate(ThreadKey(it)) }
            }

            is ClipBoardForumLink -> {
                navigate(ForumKey(link.forumName))
            }

            else -> {
//                launchUrl(this, link.url)
            }
        }
    }

    @Composable
    private fun ClipBoardDetectDialog() {
        val previewInfo by ClipBoardLinkDetector.previewInfoStateFlow.collectAsState()

        val dialogState = rememberDialogState()

        LaunchedEffect(previewInfo) {
            if (previewInfo != null) {
                dialogState.show()
            }
        }
        // Answered or dismissed, it is not offered again, even if the activity is recreated
        LaunchedEffect(dialogState) {
            snapshotFlow { dialogState.show }.drop(1).filter { !it }.collect { ClipBoardLinkDetector.consume() }
        }

        Dialog(
            dialogState = dialogState,
            title = {
                Text(text = stringResource(id = R.string.title_dialog_clip_board_tieba_url))
            },
            buttons = {
                DialogPositiveButton(text = stringResource(id = R.string.button_open)) {
                    previewInfo?.let {
                        openClipBoardLink(it.clipBoardLink)
                    }
                }
                DialogNegativeButton(text = stringResource(id = R.string.btn_close))
            },
        ) {
            previewInfo?.let {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                    border = BorderStroke(1.dp, ExtendedTheme.colors.divider),
                    shape = SmoothCornerShape(6.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.padding(16.dp)
                    ) {
                        it.icon?.let { icon ->
                            if (icon.type == QuickPreviewUtil.Icon.TYPE_DRAWABLE_RES) {
                                AvatarIcon(
                                    resId = icon.res,
                                    size = Sizes.Medium,
                                    contentDescription = null
                                )
                            } else {
                                Avatar(
                                    data = icon.url,
                                    size = Sizes.Medium,
                                    contentDescription = null
                                )
                            }
                        }
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            it.title?.let { title ->
                                Text(text = title, style = MaterialTheme.typography.subtitle1)
                            }
                            it.subtitle?.let { subtitle ->
                                Text(text = subtitle, style = MaterialTheme.typography.body2)
                            }
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun Content() {
        val okSignAlertDialogState = rememberDialogState()
        ClipBoardDetectDialog()
        AlertDialog(
            dialogState = okSignAlertDialogState,
            title = { Text(text = stringResource(id = R.string.title_dialog_oksign_battery_optimization)) },
            content = { Text(text = stringResource(id = R.string.message_dialog_oksign_battery_optimization)) },
            buttons = {
                DialogPositiveButton(
                    text = stringResource(id = R.string.button_go_to_ignore_battery_optimization),
                    onClick = {
                        requestIgnoreBatteryOptimizations()
                    }
                )
                DialogNegativeButton(
                    text = stringResource(id = R.string.button_cancel)
                )
                DialogNegativeButton(
                    text = stringResource(id = R.string.button_dont_remind_again),
                    onClick = {
                        appPreferences.ignoreBatteryOptimizationsDialog = true
                    }
                )
            },
        )
        LaunchedEffect(Unit) {
            if (appPreferences.autoSign && !isIgnoringBatteryOptimizations() && !appPreferences.ignoreBatteryOptimizationsDialog) {
                okSignAlertDialogState.show()
            }
        }
        // After the app went down, once: what happened is kept and can be read, and sent when the
        // person agrees (see CrashReports)
        val crashDialogState = rememberDialogState()
        var crash by remember { mutableStateOf<CrashLogs.Entry?>(null) }
        var askToSend by remember { mutableStateOf(false) }
        AlertDialog(
            dialogState = crashDialogState,
            title = { Text(text = stringResource(id = R.string.title_dialog_crashed)) },
            content = {
                Text(text = stringResource(id = if (askToSend) R.string.message_dialog_crashed_send else R.string.message_dialog_crashed))
            },
            buttons = {
                if (askToSend) {
                    DialogPositiveButton(
                        text = stringResource(id = R.string.button_send_crash_log),
                        onClick = {
                            val entry = crash ?: return@DialogPositiveButton
                            lifecycleScope.launch(Dispatchers.IO) { CrashReports.send(this@MainActivity, entry) }
                        },
                    )
                    DialogNegativeButton(
                        text = stringResource(id = R.string.button_view_crash_log),
                        onClick = { navigator?.navigate(CrashLogsKey) },
                    )
                    DialogNegativeButton(text = stringResource(id = R.string.button_dont_send_crash_log))
                } else {
                    DialogPositiveButton(
                        text = stringResource(id = R.string.button_view_crash_log),
                        onClick = { navigator?.navigate(CrashLogsKey) },
                    )
                    DialogNegativeButton(text = stringResource(id = R.string.btn_close))
                }
            },
        )
        // Asked once of those who used the app before usage statistics came in; new users answer on the welcome
        val usageStatsDialogState = rememberDialogState()
        val usageStatsContentState = rememberDialogState()
        UsageStatsContentDialog(usageStatsContentState)
        AlertDialog(
            dialogState = usageStatsDialogState,
            title = { Text(text = stringResource(id = R.string.title_dialog_usage_stats)) },
            content = {
                Text(
                    text = buildAnnotatedString {
                        append(stringResource(id = R.string.message_dialog_usage_stats))
                        withLink(LinkAnnotation.Clickable("view", TextLinkStyles(SpanStyle(color = ExtendedTheme.colors.accent))) { usageStatsContentState.show() }) {
                            append(stringResource(id = R.string.usage_stats_view))
                        }
                    },
                )
            },
            buttons = {
                DialogPositiveButton(
                    text = stringResource(id = R.string.button_send_usage_stats),
                    onClick = {
                        appPreferences.usageStats = true
                        UsageStats.sendSoon(this@MainActivity)
                    },
                )
                DialogNegativeButton(text = stringResource(id = R.string.button_dont_send_usage_stats))
            },
        )
        LaunchedEffect(Unit) {
            // Sends that failed before, and today's usage statistics, go on the side
            lifecycleScope.launch(Dispatchers.IO) {
                CrashReports.sendWaiting(this@MainActivity)
                UsageStats.sendIfDue(this@MainActivity)
            }
            val entry = withContext(Dispatchers.IO) {
                CrashLogs.collectExits(this@MainActivity)
                CrashLogs.takeUnnoticed(this@MainActivity)
            }
            if (entry == null) {
                if (appPreferences.landingShown && !appPreferences.usageStatsAsked) {
                    appPreferences.usageStatsAsked = true
                    usageStatsDialogState.show()
                }
                return@LaunchedEffect
            }
            val sendable = withContext(Dispatchers.IO) { CrashReports.canSend(this@MainActivity, entry) }
            when (if (sendable) CrashReports.mode(this@MainActivity) else CrashReports.Mode.Never) {
                CrashReports.Mode.Always -> withContext(Dispatchers.IO) { CrashReports.send(this@MainActivity, entry) }
                CrashReports.Mode.Ask -> {
                    crash = entry
                    askToSend = true
                    crashDialogState.show()
                }
                CrashReports.Mode.Never -> crashDialogState.show()
            }
        }
        TiebaLiteLocalProvider {
            TranslucentThemeBackground {
                val backStack = rememberAppBackStack(linked = linkedKey)
                LaunchedEffect(backStack) {
                    snapshotFlow { (backStack.lastOrNull() as? Page)?.key }.collect { key ->
                        // By its serial name, which shrinking leaves as written
                        CrashLogs.page = key?.let {
                            runCatching {
                                Json.encodeToJsonElement(Key.serializer(), it).jsonObject["type"]?.jsonPrimitive?.content?.substringAfterLast('.')
                            }.getOrNull()
                        }
                    }
                }
                val navigator = remember(backStack) { BackStackNavigator(backStack) }
                val copySheets = remember { CopySheets() }
                val now = rememberMinuteClock()
                CompositionLocalProvider(
                    LocalNow provides now,
                    LocalNavigator provides navigator,
                    LocalBlockShortcuts provides blockShortcuts,
                    LocalCopySheets provides copySheets,
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        // Around the menus as well as the pages: a page can open out of what a menu previews
                        PageMorphHost { AppNavDisplay(backStack = backStack) }
                        BlockShortcutsHost(blockShortcuts)
                        CopySheetHost(copySheets)
                        AppDialogHost()
                        // Over everything, on first launch and after an import
                        LandingHost()
                    }
                }

                SideEffect {
                    this.navigator = navigator
                    linkedKey = null
                }
            }
        }
    }

    @Composable
    private fun TranslucentThemeBackground(
        modifier: Modifier = Modifier,
        content: @Composable () -> Unit,
    ) {
        Surface(
            color = ExtendedTheme.colors.background,
            modifier = modifier
        ) {
            val translucent = ThemeUtil.isTranslucentTheme(ExtendedTheme.colors.theme)
            // Drawn once under every page and captured for their chrome to blur
            val wallpaperGlass = rememberHazeState()
            if (translucent) {
                val backgroundPath by rememberPreferenceAsMutableState(
                    key = stringPreferencesKey("translucent_theme_background_path"),
                    defaultValue = ""
                )
                val backgroundFile by remember { derivedStateOf { File(backgroundPath) } }
                // A wallpaper from before dark text stopped fading it to white is drawn again
                val context = LocalContext.current
                LaunchedEffect(Unit) { context.redrawOutdatedWallpaper() }
                // Darker at night, under the glass too
                val nightOverlay = if (ThemeUtil.isDark()) TranslucentWallpaper.nightOverlay(rememberWallpaperDim()) else 0f
                Box(modifier = Modifier.fillMaxSize().wallpaperGlassSource(wallpaperGlass)) {
                    // A drawn wallpaper that can't be shown, its file gone, is drawn again once
                    val scope = rememberCoroutineScope()
                    var repaired by remember { mutableStateOf<String?>(null) }
                    AsyncImage(
                        model = backgroundFile,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                        onError = {
                            if (repaired != backgroundPath) {
                                repaired = backgroundPath
                                scope.launch { context.repairWallpaper(backgroundPath) }
                            }
                        },
                    )
                    if (nightOverlay > 0f) {
                        Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = nightOverlay)))
                    }
                }
            }
            // Content stays in one place whatever the theme: moving it would rebuild every page
            CompositionLocalProvider(LocalWallpaperGlass provides wallpaperGlass.takeIf { translucent }, content = content)
        }
    }

    @Composable
    fun TiebaLiteLocalProvider(content: @Composable () -> Unit) {
        CompositionLocalProvider(
            LocalNotificationCountFlow provides notificationCountFlow,
            LocalDevicePosture provides devicePostureFlow.collectAsState(),
        ) {
            content()
        }
    }

    private inner class NewMessageReceiver : BroadcastReceiver() {
        @SuppressLint("RestrictedApi")
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == NotifyJobService.ACTION_NEW_MESSAGE) {
                val channel = intent.getStringExtra("channel")
                val count = intent.getIntExtra("count", 0)
                if (channel != null && channel == NotifyJobService.CHANNEL_TOTAL) {
                    lifecycleScope.launch {
                        notificationCountFlow.emit(count)
                    }
                }
            }
        }
    }
}

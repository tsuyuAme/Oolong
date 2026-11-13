package oolong.client.ui.page.main

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.graphics.ExperimentalAnimationGraphicsApi
import androidx.compose.animation.graphics.res.rememberAnimatedVectorPainter
import androidx.compose.animation.graphics.vector.AnimatedImageVector
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.onLongClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.fastForEachIndexed
import kotlinx.collections.immutable.ImmutableList
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import oolong.client.R
import oolong.client.ui.common.theme.compose.ExtendedColors
import oolong.client.ui.common.theme.compose.ExtendedTheme
import oolong.client.ui.common.theme.compose.Motion
import oolong.client.ui.common.theme.compose.SmoothCornerShape
import oolong.client.ui.common.theme.compose.Symbols
import oolong.client.ui.common.theme.compose.White
import oolong.client.ui.common.theme.compose.pressScale
import oolong.client.ui.widgets.compose.Avatar
import oolong.client.ui.widgets.compose.ChromeStyle
import oolong.client.ui.widgets.compose.ContextMenuItem
import oolong.client.ui.widgets.compose.LocalChromeStyle
import oolong.client.ui.widgets.compose.LocalContextMenus
import oolong.client.ui.widgets.compose.OwnAvatar
import oolong.client.ui.widgets.compose.classicBarGlass
import oolong.client.ui.widgets.compose.floatingGlass
import oolong.client.ui.widgets.compose.glassLens
import oolong.client.ui.widgets.compose.liquidLensOptics
import oolong.client.ui.widgets.compose.longPressMenu
import kotlin.math.abs
import kotlin.math.roundToInt
import androidx.compose.ui.util.lerp as lerpFloat

/**
 * The main navigation on wider windows: the floating pill stood upright beside the panes. It
 * stays while pages are open, highlighting the tab they were opened from; choosing a tab goes
 * back to the main page on that tab, and choosing the shown one again refreshes it. Labels go
 * where the window is too short for them.
 */
@OptIn(ExperimentalAnimationGraphicsApi::class)
@Composable
fun NavigationRail(
    currentPosition: Int,
    onSelect: (position: Int) -> Unit,
    navigationItems: ImmutableList<NavigationItem>,
    showLabels: Boolean,
    modifier: Modifier = Modifier,
    themeColors: ExtendedColors = ExtendedTheme.colors,
) {
    val itemWidth = if (showLabels) NavItemWidth else NavItemCollapsedWidth
    val itemHeight = if (showLabels) NavItemHeight else NavItemCollapsedWidth
    val indicator = remember { Animatable(currentPosition.toFloat()) }
    LaunchedEffect(currentPosition) { indicator.animateTo(currentPosition.toFloat(), IndicatorSpring) }
    Box(
        modifier = modifier
            .fillMaxHeight()
            .windowInsetsPadding(
                WindowInsets.systemBars.union(WindowInsets.displayCutout)
                    .only(WindowInsetsSides.Vertical + WindowInsetsSides.Start)
            )
            .padding(horizontal = NavPillMargin),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .floatingGlass(NavRailShape)
                .padding(NavPillPadding),
        ) {
            Box(
                modifier = Modifier
                    .offset { IntOffset(0, (indicator.value * itemHeight.toPx()).roundToInt()) }
                    .size(width = itemWidth, height = itemHeight)
                    .background(ExtendedTheme.colors.emphasis.copy(alpha = 0.12f), NavPillShape)
            )
            Column {
                navigationItems.fastForEachIndexed { index, navigationItem ->
                    val selected = index == currentPosition
                    val title = navigationItem.title(selected)
                    val contentColor =
                        if (selected) ExtendedTheme.colors.emphasis else themeColors.textSecondary
                    val menu = navigationItem.menu
                    Column(
                        modifier = Modifier
                            .size(width = itemWidth, height = itemHeight)
                            .clip(NavPillShape)
                            .then(
                                if (menu == null) {
                                    Modifier.selectable(
                                        selected = selected,
                                        role = Role.Tab,
                                        onClick = { onSelect(index) },
                                    )
                                } else {
                                    Modifier
                                        .semantics {
                                            role = Role.Tab
                                            this.selected = selected
                                        }
                                        .longPressMenu(items = menu, onClick = { onSelect(index) })
                                }
                            ),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        NavItemIcon(navigationItem, selected, title, contentColor)
                        if (showLabels) {
                            Text(
                                text = title,
                                color = contentColor,
                                fontSize = 11.sp,
                                lineHeight = 14.sp,
                                maxLines = 1,
                            )
                        }
                    }
                }
            }
        }
    }
}

/** An item's icon, animating to its selected form, with the item's badge on its corner. */
@OptIn(ExperimentalAnimationGraphicsApi::class)
@Composable
private fun NavItemIcon(
    navigationItem: NavigationItem,
    selected: Boolean,
    title: String,
    tint: Color,
) {
    Box {
        if (navigationItem.avatar != null) {
            // Ringed in the selected colour when chosen, as the icons are tinted
            // The snap spring overshoots: a width going to none would dip below zero
            val ringAnimated by animateDpAsState(if (selected) AvatarRing else 0.dp, Motion.snap(), label = "ring")
            val ring = ringAnimated.coerceAtLeast(0.dp)
            OwnAvatar(
                data = navigationItem.avatar,
                contentDescription = title,
                modifier = Modifier
                    .size(24.dp)
                    .border(ring, tint, CircleShape)
                    .padding(ring * 2),
            )
        } else {
            Icon(
                painter = rememberAnimatedVectorPainter(
                    animatedImageVector = navigationItem.icon(),
                    atEnd = selected
                ),
                contentDescription = title,
                tint = tint,
                modifier = Modifier.size(24.dp),
            )
        }
        if (navigationItem.badge) {
            Text(
                textAlign = TextAlign.Center,
                fontSize = 10.sp,
                color = White,
                text = navigationItem.badgeText ?: "",
                modifier = Modifier
                    .size(14.dp)
                    .clip(CircleShape)
                    .align(Alignment.TopEnd)
                    .background(
                        color = ExtendedTheme.colors.emphasis,
                        shape = CircleShape
                    ),
            )
        }
    }
}

private val AvatarRing = 1.5.dp

private val NavPillShape = SmoothCornerShape.Capsule
private val NavPillPadding = 6.dp
private val NavItemWidth = 68.dp
private val NavItemCollapsedWidth = 52.dp
private val NavItemHeight = 52.dp
private val NavItemCollapsedHeight = 40.dp
internal val NavPillMargin = 12.dp

/**
 * The rail's ends, concentric with the item's capsule in from them: rounded like a capsule across
 * the rail's width, they would leave a wider gap at the ends than along the sides.
 */
private val NavRailShape = SmoothCornerShape(NavItemHeight / 2 + NavPillPadding)
private val SearchButtonGap = 8.dp

/** Height the pill and its margins take above the navigation bar, expanded. */
val FloatingNavBarHeight = NavItemHeight + NavPillPadding * 2 + NavPillMargin

/**
 * The main navigation as a pill floating over the content, above the system navigation bar.
 * Each item shows its icon and a short label; while [collapsedFraction] goes to 1 (the content
 * is being scrolled forward) the labels fade out and the pill shrinks to its icons.
 *
 * The selection indicator slides between items. While the pill is pressed the indicator lifts
 * off as a clear glass lens, larger than the pill, that magnifies the item under it; dragging
 * moves the lens with the finger and releasing selects the item it ends on. Held still on an item
 * with a menu, the lens settles back and the menu opens under the finger, which can slide onto
 * an item of it and lift to pick it.
 *
 * The bar always measures at its expanded height so content padding does not change while it
 * collapses.
 *
 * With [onSearchButtonPlaced] the bar leaves a round space beside the pill, the pill's height and
 * never collapsing away, for the search button drawn over it; its items narrow where the window
 * is too narrow for both. While [searching] the pill is left out of what glass sees, so the
 * search field over it doesn't show it.
 */
@OptIn(ExperimentalAnimationGraphicsApi::class)
@Composable
fun FloatingNavBar(
    currentPosition: Int,
    onChangePosition: (position: Int) -> Unit,
    onReselected: (position: Int) -> Unit,
    navigationItems: ImmutableList<NavigationItem>,
    collapsedFraction: () -> Float,
    modifier: Modifier = Modifier,
    themeColors: ExtendedColors = ExtendedTheme.colors,
    onSearchButtonPlaced: ((boundsInRoot: Rect) -> Unit)? = null,
    searching: Boolean = false,
    onSearch: () -> Unit = {},
    swipePosition: () -> Float? = { null },
) {
    if (LocalChromeStyle.current == ChromeStyle.Classic) {
        ClassicNavBar(currentPosition, onChangePosition, onReselected, navigationItems, modifier, themeColors, onSearchButtonPlaced, onSearch)
        return
    }
    val coroutineScope = rememberCoroutineScope()
    val indicator = remember { Animatable(currentPosition.toFloat()) }
    val press = remember { Animatable(0f) }
    var indicatorDragged by remember { mutableStateOf(false) }
    LaunchedEffect(currentPosition) {
        if (!indicatorDragged) indicator.animateTo(currentPosition.toFloat(), IndicatorSpring)
    }
    // While the pages are swiped, the indicator moves with them, and springs to the tab they settle on
    val shownTab by rememberUpdatedState(currentPosition)
    LaunchedEffect(Unit) {
        var swiping = false
        snapshotFlow(swipePosition).collect { position ->
            if (indicatorDragged) return@collect
            if (position != null) {
                swiping = true
                indicator.snapTo(position)
            } else if (swiping) {
                swiping = false
                indicator.animateTo(shownTab.toFloat(), IndicatorSpring)
            }
        }
    }
    val menus = LocalContextMenus.current
    val haptic = LocalHapticFeedback.current
    val items by rememberUpdatedState(navigationItems)
    // The gesture outlives recompositions, so it reads the shown tab through this
    val shown by rememberUpdatedState(currentPosition)
    var pill by remember { mutableStateOf<LayoutCoordinates?>(null) }
    val itemBounds = remember { mutableMapOf<Int, Rect>() }
    val select by rememberUpdatedState { index: Int ->
        if (index == currentPosition) {
            onReselected(index)
        } else {
            onChangePosition(index)
        }
        navigationItems[index].onClick?.invoke()
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .windowInsetsPadding(
                WindowInsets.navigationBars.union(WindowInsets.displayCutout)
                    .only(WindowInsetsSides.Bottom + WindowInsetsSides.Horizontal)
            )
            .height(FloatingNavBarHeight),
        contentAlignment = Alignment.TopCenter,
    ) {
        val searchSpace = if (onSearchButtonPlaced != null) NavItemHeight + NavPillPadding * 2 + SearchButtonGap else 0.dp
        val fullItemWidth = minOf(
            NavItemWidth,
            (maxWidth - NavPillMargin * 2 - searchSpace - NavPillPadding * 2) / navigationItems.size,
        )
        val fraction = collapsedFraction()
        val itemWidth = lerp(fullItemWidth, NavItemCollapsedWidth, fraction)
        val itemHeight = lerp(NavItemHeight, NavItemCollapsedHeight, fraction)
        val density = LocalDensity.current
        val itemWidthPx = with(density) { itemWidth.toPx() }
        // Changes while the bar collapses, which a press during a scroll overlaps, so the
        // gesture reads it rather than restarting on it
        val gestureItemWidthPx by rememberUpdatedState(itemWidthPx)
        // The item under the indicator, so colours follow it while it is dragged.
        val highlighted = indicator.value.roundToInt()
        Row(verticalAlignment = Alignment.Bottom) {
            Box(modifier = Modifier.padding(top = (NavItemHeight - itemHeight))) {
                Box(
                    modifier = Modifier
                        .floatingGlass(NavPillShape, lensTarget = !searching)
                        .padding(NavPillPadding)
                        .onGloballyPositioned { pill = it }
                        .pointerInput(navigationItems.size, menus) {
                            val last = navigationItems.size - 1
                            fun indexAt(x: Float) = (x / gestureItemWidthPx).toInt().coerceIn(0, last)
                            fun positionAt(x: Float) = (x / gestureItemWidthPx - 0.5f).coerceIn(0f, last.toFloat())
                            awaitEachGesture {
                                val down = awaitFirstDown(requireUnconsumed = false)
                                try {
                                    coroutineScope.launch { press.animateTo(1f, PressSpring) }
                                    coroutineScope.launch {
                                        indicator.animateTo(
                                            positionAt(down.position.x).roundToInt().toFloat(),
                                            IndicatorSpring
                                        )
                                    }
                                    var dragging = false
                                    var lastX = down.position.x
                                    var released = false
                                    var releasedAt = 0L
                                    var lost = false
                                    fun follow(event: PointerEvent) {
                                        val change = event.changes.firstOrNull { it.id == down.id }
                                        if (change == null) {
                                            lost = true
                                            return
                                        }
                                        if (!change.pressed) {
                                            released = true
                                            releasedAt = change.uptimeMillis
                                            return
                                        }
                                        lastX = change.position.x
                                        if (!dragging &&
                                            abs(lastX - down.position.x) > viewConfiguration.touchSlop
                                        ) {
                                            dragging = true
                                            indicatorDragged = true
                                        }
                                        if (dragging) {
                                            change.consume()
                                            coroutineScope.launch { indicator.snapTo(positionAt(lastX)) }
                                        }
                                    }
                                    val menu = items[indexAt(down.position.x)].menu
                                    // Until the finger moves off or lifts, a menu waits for the press to last
                                    val held = menu != null && menus != null &&
                                        withTimeoutOrNull(viewConfiguration.longPressTimeoutMillis) {
                                            while (!dragging && !released && !lost) follow(awaitPointerEvent())
                                        } == null
                                    if (held && menu != null && menus != null) {
                                        coroutineScope.launch { press.animateTo(0f, PressSpring) }
                                        coroutineScope.launch {
                                            indicator.animateTo(shown.toFloat(), IndicatorSpring)
                                        }
                                        val coordinates = pill ?: return@awaitEachGesture
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        menus.show(menu(), anchor = Rect(coordinates.localToWindow(down.position), Size.Zero))
                                        while (true) {
                                            val change = awaitPointerEvent().changes
                                                .firstOrNull { it.id == down.id } ?: break
                                            change.consume()
                                            val position = coordinates.localToWindow(change.position)
                                            if (!change.pressed) {
                                                menus.liftFinger(position)
                                                break
                                            }
                                            menus.moveFinger(position)
                                        }
                                        return@awaitEachGesture
                                    }
                                    while (!released && !lost) follow(awaitPointerEvent())
                                    coroutineScope.launch { press.animateTo(0f, PressSpring) }
                                    indicatorDragged = false
                                    if (released) {
                                        val target = if (dragging) {
                                            positionAt(lastX).roundToInt()
                                        } else {
                                            indexAt(down.position.x)
                                        }
                                        coroutineScope.launch {
                                            indicator.animateTo(target.toFloat(), IndicatorSpring)
                                        }
                                        // Only a tap on the shown tab asks it again; a lens dragged
                                        // or held there and let go just goes back
                                        val tapped = !dragging &&
                                            releasedAt - down.uptimeMillis < viewConfiguration.longPressTimeoutMillis
                                        if (target != shown || tapped) select(target)
                                    } else {
                                        coroutineScope.launch {
                                            indicator.animateTo(shown.toFloat(), IndicatorSpring)
                                        }
                                    }
                                } catch (e: CancellationException) {
                                    // Restarted or taken off mid-press, the gesture would leave the lens up
                                    coroutineScope.launch { press.animateTo(0f, PressSpring) }
                                    coroutineScope.launch { indicator.animateTo(shown.toFloat(), IndicatorSpring) }
                                    indicatorDragged = false
                                    throw e
                                }
                            }
                        },
                ) {
                    Box(
                        modifier = Modifier
                            .offset { IntOffset((indicator.value * itemWidthPx).roundToInt(), 0) }
                            .size(width = itemWidth, height = itemHeight)
                            .graphicsLayer { alpha = 1f - press.value }
                            .background(ExtendedTheme.colors.emphasis.copy(alpha = 0.12f), NavPillShape)
                    )
                    Row {
                        navigationItems.fastForEachIndexed { index, navigationItem ->
                            val selected = index == currentPosition
                            val title = navigationItem.title(selected)
                            val contentColor =
                                if (index == highlighted) ExtendedTheme.colors.emphasis
                                else themeColors.textSecondary
                            Column(
                                modifier = Modifier
                                    .size(width = itemWidth, height = itemHeight)
                                    .onGloballyPositioned { itemBounds[index] = it.boundsInWindow() }
                                    .semantics(mergeDescendants = true) {
                                        role = Role.Tab
                                        this.selected = selected
                                        onClick(label = title) {
                                            select(index)
                                            true
                                        }
                                        val menu = navigationItem.menu
                                        if (menu != null && menus != null) {
                                            onLongClick(label = navigationItem.menuLabel) {
                                                val bounds = itemBounds[index] ?: return@onLongClick false
                                                menus.show(menu(), anchor = bounds)
                                                true
                                            }
                                        }
                                    },
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center,
                            ) {
                                NavItemIcon(navigationItem, selected, title, contentColor)
                                if (fraction < 1f) {
                                    Text(
                                        text = title,
                                        color = contentColor,
                                        fontSize = 11.sp,
                                        lineHeight = 14.sp,
                                        maxLines = 1,
                                        modifier = Modifier
                                            .height(14.dp * (1f - fraction))
                                            .graphicsLayer { alpha = 1f - fraction * 2f },
                                    )
                                }
                            }
                        }
                    }
                }
                // Lifts off the pill while it is pressed: drawn above it, so it magnifies the pill too.
                // Grows by layout rather than a layer scale so the glass samples the area it covers,
                // but outside the pill's size, which would otherwise grow with it and push the
                // search button down.
                val lensScale = lerpFloat(1f, LensScale, press.value)
                val lensWidth = itemWidth * lensScale
                val lensHeight = itemHeight * lensScale
                val lensOptics = remember(itemHeight) { liquidLensOptics(itemHeight * LensScale) }
                Box(modifier = Modifier.matchParentSize()) {
                    Box(
                        modifier = Modifier
                            .wrapContentSize(Alignment.TopStart, unbounded = true)
                            .offset {
                                val padding = NavPillPadding.roundToPx()
                                val grownX = ((lensWidth - itemWidth) / 2).roundToPx()
                                val grownY = ((lensHeight - itemHeight) / 2).roundToPx()
                                IntOffset(
                                    (indicator.value * itemWidthPx).roundToInt() + padding - grownX,
                                    padding - grownY,
                                )
                            }
                            .size(width = lensWidth, height = lensHeight)
                            .graphicsLayer { alpha = press.value }
                            .glassLens(NavPillShape, lensOptics)
                    )
                }
            }
            if (onSearchButtonPlaced != null) {
                Spacer(modifier = Modifier.width(SearchButtonGap))
                Box(
                    modifier = Modifier
                        .size(itemHeight + NavPillPadding * 2)
                        .onGloballyPositioned { onSearchButtonPlaced(it.boundsInRoot()) }
                )
            }
        }
    }
}

/**
 * The main navigation as a classic bar: a band along the bottom of the window, above and behind
 * the system navigation bar, its items sharing it, the chosen one's icon on a pill. With
 * [onSearchButtonPlaced], search is the last item, and the field grows out of its pill.
 */
@Composable
private fun ClassicNavBar(
    currentPosition: Int,
    onChangePosition: (position: Int) -> Unit,
    onReselected: (position: Int) -> Unit,
    navigationItems: ImmutableList<NavigationItem>,
    modifier: Modifier,
    themeColors: ExtendedColors,
    onSearchButtonPlaced: ((boundsInRoot: Rect) -> Unit)?,
    onSearch: () -> Unit,
) {
    val menus = LocalContextMenus.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .classicBarGlass(top = false)
            .windowInsetsPadding(
                WindowInsets.navigationBars.union(WindowInsets.displayCutout)
                    .only(WindowInsetsSides.Bottom + WindowInsetsSides.Horizontal)
            )
            .height(ClassicNavBarHeight)
            .selectableGroup(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        navigationItems.fastForEachIndexed { index, item ->
            val selected = index == currentPosition
            val title = item.title(selected)
            var bounds by remember { mutableStateOf(Rect.Zero) }
            val menu = item.menu
            ClassicNavItem(
                selected = selected,
                title = title,
                color = if (selected) ExtendedTheme.colors.emphasis else themeColors.textSecondary,
                onClick = {
                    if (selected) onReselected(index) else onChangePosition(index)
                    item.onClick?.invoke()
                },
                onLongClick = if (menu != null && menus != null) {
                    { menus.show(menu(), anchor = bounds) }
                } else null,
                modifier = Modifier.onGloballyPositioned { bounds = it.boundsInWindow() },
            ) { tint -> NavItemIcon(item, selected, title, tint) }
        }
        if (onSearchButtonPlaced != null) {
            val title = stringResource(id = R.string.title_search)
            ClassicNavItem(
                selected = false,
                title = title,
                color = themeColors.textSecondary,
                onClick = onSearch,
                onLongClick = null,
                pillModifier = Modifier.onGloballyPositioned { onSearchButtonPlaced(it.boundsInRoot()) },
            ) { tint -> Icon(imageVector = Symbols.Search, contentDescription = null, tint = tint, modifier = Modifier.size(24.dp)) }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun RowScope.ClassicNavItem(
    selected: Boolean,
    title: String,
    color: Color,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
    pillModifier: Modifier = Modifier,
    icon: @Composable (tint: Color) -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pill by animateFloatAsState(if (selected) 1f else 0f, Motion.snap(), label = "classicPill")
    val emphasis = ExtendedTheme.colors.emphasis
    Column(
        modifier = modifier
            .weight(1f)
            .fillMaxHeight()
            .combinedClickable(
                interactionSource = interactionSource,
                indication = null,
                role = Role.Tab,
                onClickLabel = title,
                onLongClick = onLongClick,
                onClick = onClick,
            )
            .semantics { this.selected = selected },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier = Modifier
                .then(pillModifier)
                .pressScale(interactionSource)
                .size(width = ClassicPillWidth, height = ClassicPillHeight)
                .drawBehind { drawRoundRect(emphasis.copy(alpha = 0.12f * pill), cornerRadius = CornerRadius(size.height / 2)) },
            contentAlignment = Alignment.Center,
        ) { icon(color) }
        Text(text = title, color = color, fontSize = 11.sp, lineHeight = 14.sp, maxLines = 1)
    }
}

private val ClassicNavBarHeight = 64.dp
private val ClassicPillWidth = 56.dp
private val ClassicPillHeight = 30.dp

private val IndicatorSpring = spring<Float>(dampingRatio = 0.72f, stiffness = 380f)
private val PressSpring = spring<Float>(dampingRatio = 0.6f, stiffness = Spring.StiffnessMedium)

/** How much the lens outgrows the item it lifts off from. */
private const val LensScale = 1.4f

@Immutable
data class NavigationItem @OptIn(ExperimentalAnimationGraphicsApi::class) constructor(
    val id: String,
    val icon: @Composable () -> AnimatedImageVector,
    val title: @Composable (selected: Boolean) -> String,
    val badge: Boolean = false,
    val badgeText: String? = null,
    val onClick: (() -> Unit)? = null,
    /** Shown in place of the icon, such as the signed-in account's avatar. */
    val avatar: String? = null,
    /** What a long press on it offers, labelled [menuLabel] for accessibility; null for nothing. */
    val menu: (() -> List<ContextMenuItem>)? = null,
    val menuLabel: String? = null,
)

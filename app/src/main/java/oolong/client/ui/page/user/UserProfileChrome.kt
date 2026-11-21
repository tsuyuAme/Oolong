package oolong.client.ui.page.user

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.collections.immutable.ImmutableList
import kotlinx.coroutines.launch
import oolong.client.R
import oolong.client.model.UserProfile
import oolong.client.models.PhotoViewData
import oolong.client.models.PicItem
import oolong.client.ui.common.navigation.pageHeroTarget
import oolong.client.ui.common.shownIpLocation
import oolong.client.ui.common.theme.compose.ExtendedTheme
import oolong.client.ui.common.theme.compose.PressFeedback
import oolong.client.ui.common.theme.compose.SmoothCornerShape
import oolong.client.ui.common.theme.compose.Symbols
import oolong.client.ui.navigation.UserFollowsKey
import oolong.client.ui.page.LocalNavigator
import oolong.client.ui.widgets.compose.Avatar
import oolong.client.ui.widgets.compose.GlassTab
import oolong.client.ui.widgets.compose.GlassTabRow
import oolong.client.ui.widgets.compose.NetworkImage
import oolong.client.ui.widgets.compose.Sizes
import oolong.client.ui.widgets.compose.matchedGeometry
import oolong.client.ui.widgets.compose.matchedGeometryAnchor
import oolong.client.ui.widgets.compose.onPageAccent
import oolong.client.ui.widgets.compose.pageAccentOrPrimary
import oolong.client.ui.widgets.compose.rememberScrollProgress
import oolong.client.utils.StringUtil
import oolong.client.utils.StringUtil.getShortNumString

/** The keys the header's avatar and name, and the bar's copies that dock from them, match by. */
internal object ProfileAvatarAnchor
internal object ProfileNameAnchor

/**
 * The user's front: their avatar above their name, what they moderate or are known for, their
 * intro, how many they follow, follow them and liked their posts, a full-width button to follow
 * them or edit one's own, and their ID, IP location and years on Tieba in one line.
 *
 * With [docking], the bar draws the name ([UserProfileBarTitle]) and docks it and the avatar as
 * the header scrolls up under it; the avatar here hands over to the bar's once it starts to move,
 * and is the one tapped and landed on till then.
 *
 * @param onButton what the button does, or null for no button, as when no one is logged in
 */
@Composable
internal fun UserProfileHeader(
    user: UserProfile,
    isSelf: Boolean,
    onButton: (() -> Unit)?,
    onCopyId: () -> Unit,
    modifier: Modifier = Modifier,
    docking: Boolean = true,
) {
    val avatarDock = rememberScrollProgress(ProfileAvatarAnchor)
    val nameDock = rememberScrollProgress(ProfileNameAnchor)
    Column(modifier = modifier.padding(HeaderPadding)) {
        ProfileAvatar(
            user = user,
            modifier = Modifier
                .pageHeroTarget()
                .size(ProfileAvatarSize)
                .matchedGeometryAnchor(ProfileAvatarAnchor)
                .graphicsLayer { if (docking && avatarDock.value > 0f) alpha = 0f },
        )
        Spacer(modifier = Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = user.displayName,
                fontSize = NameFontSize,
                lineHeight = 30.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .weight(1f, fill = false)
                    .matchedGeometryAnchor(ProfileNameAnchor)
                    .graphicsLayer { if (docking) alpha = 0f },
            )
            SexMark(
                sex = user.sex,
                modifier = Modifier
                    .padding(start = 6.dp)
                    .graphicsLayer { alpha = 1f - nameDock.fraction(0f..MarkFade) },
            )
        }
        val verified = user.bazhuTitle ?: user.godField?.let { stringResource(id = R.string.text_god_verify, it) }
        if (verified != null) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 2.dp)) {
                Icon(
                    imageVector = Symbols.Verified,
                    contentDescription = null,
                    modifier = Modifier.size(15.dp),
                    tint = pageAccentOrPrimary(),
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(text = verified, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = pageAccentOrPrimary())
            }
        }
        Spacer(modifier = Modifier.height(SectionGap))
        Text(
            text = user.intro.ifEmpty { stringResource(id = R.string.tip_no_intro) },
            style = MaterialTheme.typography.body2,
            color = if (user.intro.isEmpty()) ExtendedTheme.colors.textSecondary else ExtendedTheme.colors.text,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(modifier = Modifier.height(SectionGap))
        ProfileStats(user)
        if (onButton != null) {
            Spacer(modifier = Modifier.height(SectionGap))
            ProfileButton(user = user, isSelf = isSelf, onClick = onButton)
        }
        Spacer(modifier = Modifier.height(10.dp))
        ProfileFacts(user, onCopyId)
    }
}

/** The avatar, which opens in the picture viewer, where it can be saved. */
@Composable
private fun ProfileAvatar(user: UserProfile, modifier: Modifier = Modifier) {
    val avatar = StringUtil.getBigAvatarUrl(user.portrait)
    Box(modifier = modifier.clip(CircleShape)) {
        // Over the small avatar lists show, already at hand, until the big one loads; where an
        // avatar the profile was opened from lands
        Avatar(data = StringUtil.getAvatarUrl(user.portrait), contentDescription = null, modifier = Modifier.matchParentSize(), decodedAt = Sizes.Small)
        NetworkImage(
            imageUri = avatar,
            contentDescription = null,
            modifier = Modifier.matchParentSize(),
            photoViewData = remember(avatar) {
                PhotoViewData(
                    picItems = listOf(
                        PicItem(picId = "avatar:${user.portrait}", picIndex = 1, url = avatar, originUrl = avatar, showOriginBtn = false, originSize = 0),
                    ),
                )
            },
            contentScale = ContentScale.Crop,
            skipNetworkCheck = true,
        )
    }
}

/**
 * The user's avatar and name in the bar. Each is drawn at its place in the header
 * ([UserProfileHeader]) until it scrolls up into the bar's row, and docks there as it does.
 */
@Composable
internal fun UserProfileBarTitle(user: UserProfile) {
    val avatarDock = rememberScrollProgress(ProfileAvatarAnchor)
    val nameDock = rememberScrollProgress(ProfileNameAnchor)
    Row(verticalAlignment = Alignment.CenterVertically) {
        // Decoded at the header's size, so it stays sharp drawn as large as that
        Avatar(
            data = StringUtil.getBigAvatarUrl(user.portrait),
            contentDescription = null,
            decodedAt = ProfileAvatarSize,
            modifier = Modifier
                .size(DockedAvatarSize)
                .matchedGeometry(ProfileAvatarAnchor, avatarDock)
                // The header's own shows till it moves
                .graphicsLayer { if (avatarDock.value <= 0f) alpha = 0f },
        )
        Spacer(modifier = Modifier.width(DockedNameGap))
        Text(
            text = user.displayName,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.matchedGeometry(ProfileNameAnchor, nameDock),
        )
    }
}

@Composable
private fun SexMark(sex: Int, modifier: Modifier = Modifier) {
    val (mark, color) = when (sex) {
        1 -> "♂" to Color(0xFF3B82F6)
        2 -> "♀" to Color(0xFFEC4899)
        else -> return
    }
    Text(text = mark, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = color, modifier = modifier)
}

/** Following and followers, which open their lists, and likes received. */
@Composable
private fun ProfileStats(user: UserProfile) {
    val navigator = LocalNavigator.current
    val name = user.displayName
    Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
        ProfileStat(
            value = user.followCount.getShortNumString(),
            label = stringResource(id = R.string.text_stat_follow),
            onClick = { navigator.navigate(UserFollowsKey(user.id, fans = false, name = name)) },
        )
        ProfileStat(
            value = user.fanCount.getShortNumString(),
            label = stringResource(id = R.string.text_stat_fans),
            onClick = { navigator.navigate(UserFollowsKey(user.id, fans = true, name = name)) },
        )
        ProfileStat(value = user.agreeCount.getShortNumString(), label = stringResource(id = R.string.text_stat_agrees))
    }
}

@Composable
private fun ProfileStat(value: String, label: String, onClick: (() -> Unit)? = null) {
    Column(modifier = if (onClick != null) Modifier.clickable(role = Role.Button, onClick = onClick) else Modifier) {
        Text(text = value, fontSize = 20.sp, lineHeight = 24.sp, fontWeight = FontWeight.Bold, color = ExtendedTheme.colors.text)
        Text(text = label, style = MaterialTheme.typography.caption, color = ExtendedTheme.colors.textSecondary)
    }
}

/**
 * Follow, in the page's colour; unfollow and edit one's own profile, tonal, so a profile the
 * viewer already follows or owns does not call for a tap.
 */
@Composable
private fun ProfileButton(user: UserProfile, isSelf: Boolean, onClick: () -> Unit) {
    val accent = pageAccentOrPrimary()
    val filled = !isSelf && !user.followed
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(ButtonHeight)
            .background(if (filled) accent else accent.copy(alpha = if (ExtendedTheme.colors.isNightMode) 0.2f else 0.12f), SmoothCornerShape.Capsule)
            .clickable(interactionSource = null, indication = PressFeedback.overlay(SmoothCornerShape.Capsule), role = Role.Button, onClick = onClick),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val color = if (filled) onPageAccent() else accent
        val icon = when {
            isSelf -> Symbols.Edit
            filled -> Symbols.Add
            else -> null
        }
        if (icon != null) {
            Icon(imageVector = icon, contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
        }
        Text(
            text = stringResource(
                id = when {
                    isSelf -> R.string.menu_edit_info
                    filled -> R.string.button_follow
                    else -> R.string.button_unfollow
                }
            ),
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = color,
        )
    }
}

/** ID, which tapping copies, IP location and years on Tieba, in one quiet line. */
@Composable
private fun ProfileFacts(user: UserProfile, onCopyId: () -> Unit) {
    val color = ExtendedTheme.colors.textSecondary
    val style = MaterialTheme.typography.caption
    Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.clickable(role = Role.Button, onClick = onCopyId),
        ) {
            Text(text = stringResource(id = R.string.text_profile_user_id, user.tiebaUid), style = style, color = color)
            Spacer(modifier = Modifier.width(3.dp))
            Icon(imageVector = Symbols.ContentCopy, contentDescription = null, tint = color, modifier = Modifier.size(12.dp))
        }
        shownIpLocation(user.ipLocation)?.let { ipLocation ->
            Text(text = stringResource(id = R.string.text_profile_ip_location, ipLocation), style = style, color = color, maxLines = 1)
        }
        if (user.tbAge.isNotEmpty()) {
            Text(text = stringResource(id = R.string.text_profile_tb_age, user.tbAge), style = style, color = color, maxLines = 1)
        }
    }
}

/** The profile's tabs on a glass capsule across the width, each with how many it holds. */
@Composable
internal fun UserProfileTabRow(
    user: UserProfile,
    pages: ImmutableList<UserProfilePageData>,
    pagerState: PagerState,
    modifier: Modifier = Modifier,
) {
    val coroutineScope = rememberCoroutineScope()
    GlassTabRow(selectedTabIndex = pagerState.currentPage, pagerState = pagerState, modifier = modifier, fill = true) {
        pages.forEachIndexed { index, page ->
            GlassTab(
                selected = pagerState.currentPage == index,
                onClick = { coroutineScope.launch { pagerState.animateScrollToPage(index) } },
                text = {
                    Text(
                        text = page.title(user),
                        style = MaterialTheme.typography.button.copy(fontWeight = FontWeight.Bold, fontSize = 13.sp, letterSpacing = 0.sp),
                    )
                },
            )
        }
    }
}

/** Where the header starts, which a profile still loading puts its avatar at too. */
internal val HeaderPadding = PaddingValues(start = 16.dp, top = 8.dp, end = 16.dp, bottom = 12.dp)
internal val ProfileAvatarSize = 80.dp
private val NameFontSize = 23.sp
private val SectionGap = 12.dp
private val ButtonHeight = 38.dp
private val DockedAvatarSize = 28.dp
private val DockedNameGap = 10.dp

/** How much of its docking the name spends taking the sex mark with it. */
private const val MarkFade = 0.5f

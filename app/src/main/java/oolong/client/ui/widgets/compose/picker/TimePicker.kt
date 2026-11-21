package oolong.client.ui.widgets.compose.picker

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import oolong.client.ui.common.theme.compose.Corners
import oolong.client.ui.common.theme.compose.ExtendedTheme
import oolong.client.ui.common.theme.compose.TextStyles

/**
 * Picking a time on wheels of hours and minutes, and of AM and PM when not [is24TimeFormat], with
 * a band behind the middle row across them all, as the time picked.
 */
@Composable
fun TimePicker(
    currentTime: String,
    onTimeChanged: (String) -> Unit,
    is24TimeFormat: Boolean,
    modifier: Modifier = Modifier,
) {
    val colors = ExtendedTheme.colors
    val pickerTime = remember { PickerTime.parse(currentTime, is24TimeFormat = is24TimeFormat) }
    val hours = remember(is24TimeFormat) { (if (is24TimeFormat) (0..23) else (1..12)).toList() }
    val minutes = remember { (0..59).toList() }
    // Digits of one width, so the numbers don't shift as the wheels turn
    val style = TextStyles.title.copy(fontSize = 22.sp, fontWeight = FontWeight.Medium, color = colors.text, fontFeatureSettings = "tnum")

    Box(modifier = modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(WheelItemHeight)
                .background(colors.chip, Corners.sm),
        )
        Row(horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
            WheelPicker(
                items = hours,
                selectedItem = pickerTime.hours,
                onItemChanged = {
                    pickerTime.hours = it
                    onTimeChanged(pickerTime.toString())
                },
                textStyle = style,
                itemToString = { if (is24TimeFormat) "%02d".format(it) else it.toString() },
                modifier = Modifier.width(WheelWidth),
            )
            Text(text = ":", style = style)
            WheelPicker(
                items = minutes,
                selectedItem = pickerTime.minutes,
                onItemChanged = {
                    pickerTime.minutes = it
                    onTimeChanged(pickerTime.toString())
                },
                textStyle = style,
                itemToString = { "%02d".format(it) },
                modifier = Modifier.width(WheelWidth),
            )
            if (!is24TimeFormat) {
                WheelPicker(
                    items = TimesOfDay.entries,
                    selectedItem = pickerTime.timesOfDay!!,
                    onItemChanged = {
                        pickerTime.timesOfDay = it
                        onTimeChanged(pickerTime.toString())
                    },
                    textStyle = style,
                    cyclic = false,
                    itemToString = { it.string },
                    modifier = Modifier.width(WheelWidth),
                )
            }
        }
    }
}

private val WheelWidth = 72.dp

enum class TimesOfDay(val string: String) {
    AM("AM"),
    PM("PM")
}

@Stable
data class PickerTime(
    var hours: Int,
    var minutes: Int,
    var timesOfDay: TimesOfDay? = null
) {
    companion object {
        fun parse(string: String, is24TimeFormat: Boolean): PickerTime {
            val is24 =
                listOf(TimesOfDay.AM, TimesOfDay.PM).none { string.endsWith(it.string, true) }
            val hours: Int
            val minutes: Int
            if (is24) {
                hours = string.split(":")[0].toInt()
                minutes = string.split(":")[1].toInt()
            } else {
                val timesOfDay =
                    TimesOfDay.values().first { it.string == string.split(":")[1].split(" ")[1] }
                hours = string.split(":")[0].toInt() + if (timesOfDay == TimesOfDay.PM) 12 else 0
                minutes = string.split(":")[1].split(" ")[0].toInt()
            }
            return if (is24TimeFormat) PickerTime(hours, minutes) else {
                val hours12 = if (hours == 0) 12 else if (hours > 12) hours - 12 else hours
                val timesOfDay = if (hours == 0 || hours > 12) TimesOfDay.PM else TimesOfDay.AM
                PickerTime(hours12, minutes, timesOfDay)
            }
        }
    }

    override fun toString(): String {
        return "${String.format("%02d", hours)}:${
            String.format(
                "%02d",
                minutes
            )
        }${" ".takeIf { timesOfDay != null }.orEmpty()}${timesOfDay?.string.orEmpty()}"
    }
}

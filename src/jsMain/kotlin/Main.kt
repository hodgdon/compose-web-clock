@file:OptIn(ExperimentalTime::class)
@file:Suppress("FunctionName")

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.UtcOffset
import kotlinx.datetime.offsetAt
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.web.attributes.selected
import org.jetbrains.compose.web.css.paddingLeft
import org.jetbrains.compose.web.css.paddingRight
import org.jetbrains.compose.web.css.px
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.H1
import org.jetbrains.compose.web.dom.Label
import org.jetbrains.compose.web.dom.OptGroup
import org.jetbrains.compose.web.dom.Option
import org.jetbrains.compose.web.dom.Select
import org.jetbrains.compose.web.dom.Text
import org.jetbrains.compose.web.renderComposable
import kotlin.time.ExperimentalTime

// Import JodaTime so we get timezone info
@JsModule("@js-joda/timezone")
@JsNonModule
external object JsJodaTimeZoneModule

@Suppress("unused")
private val jsJodaTz = JsJodaTimeZoneModule

fun main() {
    val systemDefaultTimeZone = TimeZone.currentSystemDefault().id
    val selectedTimeZoneIdMutableState: MutableState<String> = mutableStateOf(systemDefaultTimeZone)
    val timeZonesByOffset: Map<UtcOffset, List<String>> = timeZonesByOffset()
    renderComposable(rootElementId = "root") {
        AppWindow(selectedTimeZoneIdMutableState, timeZonesByOffset)
    }
}

@Composable
private fun AppWindow(
    selectedTimeZoneIdMutableState: MutableState<String>,
    timeZonesByOffset: Map<UtcOffset, List<String>>
) {
    LocalTimeText(selectedTimeZoneIdMutableState)
    TimeZonePicker(selectedTimeZoneIdMutableState, timeZonesByOffset)
}

// Seems like kotlin multiplatform should have a String.format() type thing?
private fun Int.toStringPadZero(digits: Int): String {
    val toString = toString()
    val missingDigits = digits - toString.length
    return if (digits <= 0) toString else {
        "0".repeat(missingDigits) + toString
    }
}

// Should dig through kotlinx.datetime to see if it has this kind of formatting.
private fun Instant.formatHMmSsTime(timeZoneId: String): String {
    val localDateTime = toLocalDateTime(TimeZone.of(timeZoneId))
    val hour = localDateTime.hour.toString()
    val minute = localDateTime.minute.toStringPadZero(2)
    val second = localDateTime.second.toStringPadZero(2)
    return "$hour:$minute:$second"
}

/**
 * Local time in the selected timezone in "h:mm:ss" format
 */
@Composable
private fun LocalTimeText(selectedTimeZoneIdState: State<String>) {
    val nowInstantMutableState: MutableState<Instant?> = mutableStateOf(null)
    val timerCoroutineScope = rememberCoroutineScope()
    timerCoroutineScope.launch {
        while (true) {
            nowInstantMutableState.value = Clock.System.now()
            delay(1000)
        }
    }
    val now by nowInstantMutableState
    val selectedTimeZoneId: String by selectedTimeZoneIdState
    Div({
        style {
            paddingLeft(25.px)
            paddingRight(25.px)
        }
    }) {
        H1 {
            val localTimeString = now?.formatHMmSsTime(selectedTimeZoneId)
            if (localTimeString != null) {
                Text(localTimeString)
            }
        }
    }
}

private fun timeZonesByOffset(referenceTimeForUtcOffset: Instant = Clock.System.now()): Map<UtcOffset, List<String>> {
    return TimeZone.availableZoneIds.map { zoneId ->
        TimeZone.of(zoneId)
    }.groupBy({ timeZone ->
        timeZone.offsetAt(referenceTimeForUtcOffset)
    }, { timeZone ->
        timeZone.id
    })
}

enum class PickerStyle {
    BROWSER_DEFAULT,
    MATERIAL,
}

/**
 * A spinner / drop down menu which shows timezones grouped by UTC offset
 */
@Composable
private fun TimeZonePicker(
    selectedTimeZoneIdMutableState: MutableState<String>,
    timeZonesByOffset: Map<UtcOffset, List<String>>,
    pickerStyle: PickerStyle = PickerStyle.BROWSER_DEFAULT,
) {
    var selectedTimeZoneId: String by selectedTimeZoneIdMutableState
    Div(
        attrs = {
            style {
                paddingLeft(25.px)
                paddingRight(25.px)
            }
            classes("input-field", "col", "s12")
        }) {
        Select(multiple = false, attrs = {
            if(pickerStyle == PickerStyle.BROWSER_DEFAULT) {
                classes("browser-default")
            }
            id("zone-selector")
            onChange {
                val onChangeTimeZoneId = it.value
                if (onChangeTimeZoneId != null) {
                    selectedTimeZoneId = onChangeTimeZoneId
                }
            }
        }) {
            timeZonesByOffset.forEach { (utcOffset, ids) ->
                OptGroup(label = utcOffset.toString()) {
                    ids.forEach { id ->
                        Option(value = id, attrs = {
                            style {
                                if (id == selectedTimeZoneId) {
                                    selected()
                                }
                            }
                        }) {
                            Text(id)
                        }
                    }
                }
            }
        }
        if(pickerStyle == PickerStyle.MATERIAL) {
            Label(forId = "zone-selector") {
                Text("Time Zone")
            }
        }
    }
}
package io.github.fedeonline.fridged.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.Preferences
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.LocalContext
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.currentState
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import io.github.fedeonline.fridged.MainActivity
import io.github.fedeonline.fridged.R
import io.github.fedeonline.fridged.ui.format.formatCelsius
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/** Home-screen widget: temperature and a one-line summary; tapping it opens the app. */
class FridgeWidget : GlanceAppWidget() {
    override val stateDefinition = PreferencesGlanceStateDefinition
    override val sizeMode = SizeMode.Single

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            GlanceTheme { Content(WidgetData.read(currentState<Preferences>())) }
        }
    }
}

class FridgeWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = FridgeWidget()

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        WidgetRefreshWorker.schedule(context)
    }

    override fun onUpdate(context: Context, appWidgetManager: android.appwidget.AppWidgetManager, appWidgetIds: IntArray) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)
        WidgetRefreshWorker.refreshNow(context)
    }

    override fun onDisabled(context: Context) {
        super.onDisabled(context)
        WidgetRefreshWorker.cancel(context)
    }
}

@Composable
private fun Content(data: WidgetData?) {
    val context = LocalContext.current
    val locale = context.resources.configuration.locales[0]
    val colors = GlanceTheme.colors
    val big = TextStyle(fontSize = 30.sp, fontWeight = FontWeight.Medium, color = colors.onSurface)
    val small = TextStyle(fontSize = 13.sp, color = colors.onSurfaceVariant)

    val (title, line, warning) = when (data) {
        null -> Triple(context.getString(R.string.app_name), "…", false)
        WidgetData.SignedOut -> Triple(context.getString(R.string.app_name), context.getString(R.string.widget_signed_out), true)
        WidgetData.NoData -> Triple(context.getString(R.string.app_name), context.getString(R.string.widget_no_data), false)
        is WidgetData.Status -> {
            val time = DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)
                .withLocale(locale).withZone(ZoneId.systemDefault()).format(data.time)
            val text = when (data.line) {
                WidgetData.Line.COMPRESSOR_ON -> R.string.widget_compressor_on
                WidgetData.Line.COMPRESSOR_OFF -> R.string.widget_compressor_off
                WidgetData.Line.SAFE -> R.string.widget_safe
                WidgetData.Line.OVERRUN -> R.string.widget_overrun
                WidgetData.Line.STALE -> R.string.widget_stale
            }
            Triple(data.temperature?.let { formatCelsius(it, locale) } ?: "—", context.getString(text, time), data.isWarning)
        }
    }

    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(colors.widgetBackground)
            .cornerRadius(16.dp)
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clickable(actionStartActivity<MainActivity>()),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, style = big, maxLines = 1)
        Text(line, style = if (warning) small.copy(color = colors.error) else small, maxLines = 2)
    }
}

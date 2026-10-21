package io.github.fedeonline.fridged.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.Preferences
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.LocalSize
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
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import io.github.fedeonline.fridged.MainActivity
import io.github.fedeonline.fridged.R
import io.github.fedeonline.fridged.ui.format.formatBand
import io.github.fedeonline.fridged.ui.format.formatCelsius
import io.github.fedeonline.fridged.ui.theme.Palette
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/** Home-screen widget: temperature and a one-line summary; tapping it opens the app. */
class FridgeWidget : GlanceAppWidget() {
    override val stateDefinition = PreferencesGlanceStateDefinition
    // Exact: the layout adapts to the widget's real size (see Content).
    override val sizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            Content(WidgetData.read(currentState<Preferences>()))
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
    val onSurface = ColorProvider(Palette.OnSurface)
    val muted = ColorProvider(Palette.Muted)

    Row(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(ColorProvider(Palette.Background))
            .cornerRadius(28.dp)
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clickable(actionStartActivity<MainActivity>()),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        when (data) {
            null, WidgetData.SignedOut, WidgetData.NoData -> Column {
                Text(context.getString(R.string.app_name), style = TextStyle(fontSize = 22.sp, fontWeight = FontWeight.Bold, color = onSurface))
                val message = when (data) {
                    WidgetData.SignedOut -> context.getString(R.string.widget_signed_out)
                    WidgetData.NoData -> context.getString(R.string.widget_no_data)
                    else -> "…"
                }
                Text(message, style = TextStyle(fontSize = 13.sp, color = if (data == WidgetData.SignedOut) ColorProvider(Palette.Peach) else muted), maxLines = 2)
            }
            is WidgetData.Status -> {
                // A 2×1 widget is often narrower than the dial and the text need: shrink, drop the band.
                val wide = LocalSize.current.width >= WIDE
                val dial = if (wide) 84.dp else 56.dp
                val dialPx = (dial.value * context.resources.displayMetrics.density).toInt()
                Image(
                    provider = ImageProvider(DialBitmap.render(dialPx, data.band, data.temperature, data.compressorOn)),
                    contentDescription = null,
                    modifier = GlanceModifier.size(dial),
                )
                Spacer(GlanceModifier.width(if (wide) 14.dp else 10.dp))
                val time = DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)
                    .withLocale(locale).withZone(ZoneId.systemDefault()).format(data.time)
                val (text, color) = when (data.line) {
                    WidgetData.Line.COMPRESSOR_ON -> R.string.widget_compressor_on to Palette.Blue
                    WidgetData.Line.COMPRESSOR_OFF -> R.string.widget_compressor_off to Palette.Muted
                    WidgetData.Line.SAFE -> R.string.widget_safe to Palette.Lilac
                    WidgetData.Line.OVERRUN -> R.string.widget_overrun to Palette.Red
                    WidgetData.Line.STALE -> R.string.widget_stale to Palette.Peach
                }
                Column {
                    Text(
                        data.temperature?.let { formatCelsius(it, locale) } ?: "—",
                        style = TextStyle(fontSize = if (wide) 32.sp else 24.sp, fontWeight = FontWeight.Bold, color = onSurface),
                        maxLines = 1,
                    )
                    Text(
                        context.getString(text, time),
                        style = TextStyle(fontSize = if (wide) 13.sp else 12.sp, fontWeight = FontWeight.Medium, color = ColorProvider(color)),
                        maxLines = 2,
                    )
                    data.band?.takeIf { wide }?.let {
                        Text(formatBand(it, locale), style = TextStyle(fontSize = 12.sp, color = muted), maxLines = 1)
                    }
                }
            }
        }
    }
}

/** From this width the widget shows the full layout. */
private val WIDE = 240.dp

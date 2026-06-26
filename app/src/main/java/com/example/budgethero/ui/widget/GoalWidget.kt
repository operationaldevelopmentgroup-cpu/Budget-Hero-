package com.example.budgethero.ui.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.DpSize
import androidx.glance.LocalSize
import androidx.glance.appwidget.SizeMode
import androidx.glance.ColorFilter
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.Action
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.appWidgetBackground
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.background
import androidx.glance.currentState
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.fillMaxHeight
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.state.GlanceStateDefinition
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.compose.ui.unit.Density
import com.example.budgethero.MainActivity
import com.example.budgethero.R
import com.example.budgethero.ui.dashboard.TimeScale
import com.example.budgethero.ui.dashboard.DashboardState
import com.example.budgethero.ui.dashboard.DashboardStateUtils
import androidx.glance.GlanceTheme
import androidx.glance.color.ColorProvider as DayNightColorProvider
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import com.example.budgethero.data.repository.BudgetRepository
import kotlinx.coroutines.flow.first
import java.util.Locale

class GoalWidget : GlanceAppWidget() {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface WidgetEntryPoint {
        fun repository(): BudgetRepository
    }

    override var stateDefinition: GlanceStateDefinition<*> = PreferencesGlanceStateDefinition
    override val sizeMode: SizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val entryPoint = EntryPointAccessors.fromApplication(context, WidgetEntryPoint::class.java)
        val repository = entryPoint.repository()
        
        // Fetch data
        val bills = repository.getAllBills().first()
        val workDays = repository.getAllWorkDays().first()
        val lineItems = repository.getAllLineItems().first()
        val incomeEntries = repository.getAllIncome().first()

        provideContent {
            val prefs = currentState<Preferences>()
            val timeScaleName = prefs[stringPreferencesKey("time_scale")] ?: TimeScale.DAILY.name
            val timeScale = TimeScale.valueOf(timeScaleName)
            
            val dashboardState = DashboardStateUtils.calculateDashboardState(
                bills = bills,
                workDays = workDays,
                lineItems = lineItems,
                incomeEntries = incomeEntries,
                timeScale = timeScale
            )
            
            GlanceTheme(colors = WidgetTheme.colors) {
                GoalWidgetContent(dashboardState)
            }
        }
    }
}

@Composable
private fun GoalWidgetContent(state: DashboardState) {
    val size = LocalSize.current
    val width = size.width
    val height = size.height

    // Responsive Flags
    val isNarrow = width < 180.dp
    val isShort = height < 200.dp
    val isTiny = width < 120.dp || height < 120.dp
    
    val remaining = state.remainingAmount
    val isSurplus = remaining < 0
    val amountText = if (isSurplus) {
        String.format(Locale.US, "+$%.0f", Math.abs(remaining))
    } else {
        String.format(Locale.US, "$%.0f", remaining)
    }

    val primaryColor = GlanceTheme.colors.primary
    val surfaceVariant = GlanceTheme.colors.surfaceVariant
    
    // Vibrant Color Palette
    val targetColor = DayNightColorProvider(day = Color(0xFF007AFF), night = Color(0xFF0A84FF)) // Electric Blue
    val earnedColor = DayNightColorProvider(day = Color(0xFF34C759), night = Color(0xFF30D158)) // Neon Green
    val spentColor = DayNightColorProvider(day = Color(0xFFFF3B30), night = Color(0xFFFF453A)) // Sunset Red
    val workColor = DayNightColorProvider(day = Color(0xFF5856D6), night = Color(0xFF5E5CE6)) // Indigo
    val avgColor = DayNightColorProvider(day = Color(0xFFFF9500), night = Color(0xFFFF9F0A)) // Orange
    
    val successColor = earnedColor
    val errorColor = spentColor

    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .appWidgetBackground()
            .background(GlanceTheme.colors.surface)
            .padding(if (isTiny) 6.dp else 12.dp)
            .clickable(actionRunCallback<ToggleTimeScaleAction>()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalAlignment = Alignment.Top
    ) {
        // Header: Time Scale
        Box(
            modifier = GlanceModifier
                .background(surfaceVariant)
                .padding(horizontal = 16.dp, vertical = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = state.timeScale.name,
                style = TextStyle(
                    fontWeight = FontWeight.Bold,
                    fontSize = if (isTiny) 10.sp else 12.sp,
                    color = primaryColor
                )
            )
        }

        Spacer(modifier = GlanceModifier.height(if (isTiny) 6.dp else 12.dp))

        // Main Metric Section (Horizontal Rows for efficiency)
        Column(
            modifier = GlanceModifier.fillMaxWidth().defaultWeight(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // High Visibility Primary Row
            MetricBar(
                label = if (isSurplus) "SURPLUS" else "REMAINING",
                value = amountText,
                color = if (isSurplus) earnedColor else GlanceTheme.colors.onSurface,
                isLarge = true,
                isTiny = isTiny
            )
            
            Spacer(modifier = GlanceModifier.height(if (isTiny) 6.dp else 10.dp))

            if (isNarrow) {
                // Single column for narrow widgets
                MetricBar(label = "TARGET", value = String.format(Locale.US, "$%.0f", state.targetAmount), color = targetColor, isTiny = isTiny)
                Spacer(modifier = GlanceModifier.height(if (isTiny) 4.dp else 8.dp))
                MetricBar(label = "EARNED", value = String.format(Locale.US, "$%.0f", state.earnedIncome), color = earnedColor, isTiny = isTiny)
                Spacer(modifier = GlanceModifier.height(if (isTiny) 4.dp else 8.dp))
                MetricBar(label = "SPENT", value = String.format(Locale.US, "$%.0f", state.notebookSpentAmount), color = spentColor, isTiny = isTiny)
                if (!isShort) {
                    Spacer(modifier = GlanceModifier.height(if (isTiny) 4.dp else 8.dp))
                    MetricBar(label = "WORK", value = state.formattedWorkTime, color = workColor, isTiny = isTiny)
                }
            } else {
                // Two-column horizontal bar layout for efficiency
                Row(modifier = GlanceModifier.fillMaxWidth()) {
                    MetricBar(label = "TARGET", value = String.format(Locale.US, "$%.0f", state.targetAmount), color = targetColor, modifier = GlanceModifier.defaultWeight(), isTiny = isTiny)
                    Spacer(modifier = GlanceModifier.width(8.dp))
                    MetricBar(label = "EARNED", value = String.format(Locale.US, "$%.0f", state.earnedIncome), color = earnedColor, modifier = GlanceModifier.defaultWeight(), isTiny = isTiny)
                }
                Spacer(modifier = GlanceModifier.height(if (isTiny) 4.dp else 8.dp))
                Row(modifier = GlanceModifier.fillMaxWidth()) {
                    MetricBar(label = "SPENT", value = String.format(Locale.US, "$%.0f", state.notebookSpentAmount), color = spentColor, modifier = GlanceModifier.defaultWeight(), isTiny = isTiny)
                    Spacer(modifier = GlanceModifier.width(8.dp))
                    MetricBar(label = "DAILY", value = String.format(Locale.US, "$%.0f", state.dailyGoalAverage), color = avgColor, modifier = GlanceModifier.defaultWeight(), isTiny = isTiny)
                }
                if (!isShort) {
                    Spacer(modifier = GlanceModifier.height(if (isTiny) 4.dp else 8.dp))
                    MetricBar(label = "REMAINING WORK TIME", value = state.formattedWorkTime, color = workColor, isTiny = isTiny)
                }
            }
        }

        Spacer(modifier = GlanceModifier.height(if (isTiny) 8.dp else 12.dp))

        // Bottom Section: Actions
        Row(modifier = GlanceModifier.fillMaxWidth()) {
            WidgetActionButton(
                label = "INCOME",
                color = successColor,
                action = actionStartActivity<MainActivity>(actionParametersOf(ActionParameters.Key<String>("action") to "add_income")),
                modifier = GlanceModifier.defaultWeight().height(if (isShort) 36.dp else 44.dp)
            )
            Spacer(modifier = GlanceModifier.width(if (isTiny) 6.dp else 12.dp))
            WidgetActionButton(
                label = "SPENDING",
                color = errorColor,
                action = actionStartActivity<MainActivity>(actionParametersOf(ActionParameters.Key<String>("action") to "add_spending")),
                modifier = GlanceModifier.defaultWeight().height(if (isShort) 36.dp else 44.dp)
            )
        }
    }
}

@Composable
private fun MetricBar(
    label: String,
    value: String,
    color: ColorProvider,
    modifier: GlanceModifier = GlanceModifier,
    isLarge: Boolean = false,
    isTiny: Boolean
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(GlanceTheme.colors.surfaceVariant)
            .padding(horizontal = if (isLarge) 12.dp else 8.dp, vertical = if (isLarge) 10.dp else 6.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalAlignment = Alignment.Start
        ) {
            // Colored Accent strip
            Box(
                modifier = GlanceModifier
                    .width(4.dp)
                    .height(if (isLarge) 24.dp else 16.dp)
                    .background(color)
            ) {}
            
            Spacer(modifier = GlanceModifier.width(8.dp))
            
            Text(
                text = label,
                modifier = GlanceModifier.defaultWeight(),
                style = TextStyle(
                    fontSize = if (isTiny) 8.sp else 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = GlanceTheme.colors.onSurfaceVariant
                )
            )
            
            Text(
                text = value,
                style = TextStyle(
                    fontSize = if (isLarge) 18.sp else 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = color
                )
            )
        }
    }
}

@Composable
private fun WidgetActionButton(
    label: String,
    color: ColorProvider,
    action: Action,
    modifier: GlanceModifier = GlanceModifier
) {
    Box(
        modifier = modifier
            .background(color)
            .clickable(action),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = TextStyle(
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                color = DayNightColorProvider(day = Color.White, night = Color.White)
            )
        )
    }
}


class ToggleTimeScaleAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        updateAppWidgetState(context, PreferencesGlanceStateDefinition, glanceId) { prefs ->
            val current = prefs[stringPreferencesKey("time_scale")] ?: TimeScale.DAILY.name
            val next = when (TimeScale.valueOf(current)) {
                TimeScale.DAILY -> TimeScale.WEEKLY
                TimeScale.WEEKLY -> TimeScale.MONTHLY
                TimeScale.MONTHLY -> TimeScale.YEARLY
                TimeScale.YEARLY -> TimeScale.DAILY
            }
            prefs.toMutablePreferences().apply {
                this[stringPreferencesKey("time_scale")] = next.name
            }
        }
        GoalWidget().update(context, glanceId)
    }
}

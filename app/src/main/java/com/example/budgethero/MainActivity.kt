package com.example.budgethero

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfoV2
import androidx.compose.material3.adaptive.layout.calculatePaneScaffoldDirective
import androidx.compose.material3.adaptive.navigation3.ListDetailSceneStrategy
import androidx.compose.material3.adaptive.navigation3.rememberListDetailSceneStrategy
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.example.budgethero.ui.calendar.CalendarScreen
import com.example.budgethero.ui.calendar.CalendarViewModel
import com.example.budgethero.ui.dashboard.DashboardScreen
import com.example.budgethero.ui.dashboard.DashboardViewModel
import com.example.budgethero.ui.earnings.EarningsScreen
import com.example.budgethero.ui.earnings.EarningsViewModel
import com.example.budgethero.ui.notebook.NotebookScreen
import com.example.budgethero.ui.notebook.NotebookViewModel
import com.example.budgethero.ui.navigation.HeroNavKey
import com.example.budgethero.ui.splash.SplashScreen
import com.example.budgethero.ui.theme.BudgetHeroTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val _widgetAction = mutableStateOf<String?>(null)

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        _widgetAction.value = intent.getStringExtra("action")
    }

    @OptIn(ExperimentalMaterial3AdaptiveApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        _widgetAction.value = intent.getStringExtra("action")
        enableEdgeToEdge()
        setContent {
                    BudgetHeroTheme {
                        Surface(
                            modifier = Modifier.fillMaxSize(),
                            color = MaterialTheme.colorScheme.background,
                        ) {
                            val backStack = rememberNavBackStack(HeroNavKey.Splash as NavKey)

                            // Handle Widget Deep Links
                            val action by _widgetAction
                            LaunchedEffect(action) {
                                when (action) {
                                    "add_income" -> {
                                        if (!backStack.contains(HeroNavKey.Earnings)) {
                                            backStack.add(HeroNavKey.Earnings)
                                        }
                                        _widgetAction.value = null
                                    }
                                    "add_spending" -> {
                                        if (!backStack.contains(HeroNavKey.Notebook)) {
                                            backStack.add(HeroNavKey.Notebook)
                                        }
                                        _widgetAction.value = null
                                    }
                                }
                            }

                            val windowAdaptiveInfo = currentWindowAdaptiveInfoV2()
                            val directive = remember(windowAdaptiveInfo) {
                        calculatePaneScaffoldDirective(windowAdaptiveInfo)
                            .copy(horizontalPartitionSpacerSize = 0.dp)
                    }
                    val listDetailStrategy = rememberListDetailSceneStrategy<NavKey>(directive = directive)

                    NavDisplay(
                        backStack = backStack,
                        onBack = {
                            if (backStack.size > 1) {
                                backStack.removeLastOrNull()
                            } else {
                                finish()
                            }
                        },
                        sceneStrategies = listOf(listDetailStrategy),
                        entryProvider = { key ->
                            when (key) {
                                is HeroNavKey.Splash -> NavEntry(
                                    key = key,
                                    metadata = ListDetailSceneStrategy.listPane()
                                ) {
                                    SplashScreen(
                                        onTimeout = {
                                            backStack.add(HeroNavKey.Dashboard)
                                            backStack.remove(HeroNavKey.Splash)
                                        }
                                    )
                                }
                                is HeroNavKey.Dashboard -> NavEntry(
                                    key = key,
                                    metadata = ListDetailSceneStrategy.listPane(
                                        detailPlaceholder = {
                                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                                Text("Select an action or view calendar")
                                            }
                                        }
                                    )
                                ) {
                                    val viewModel: DashboardViewModel = hiltViewModel()
                                    DashboardScreen(
                                        viewModel = viewModel,
                                        onNavigateToCalendar = {
                                            if (!backStack.contains(HeroNavKey.Calendar)) {
                                                backStack.add(HeroNavKey.Calendar)
                                            }
                                        },
                                        onNavigateToNotebook = {
                                            if (!backStack.contains(HeroNavKey.Notebook)) {
                                                backStack.add(HeroNavKey.Notebook)
                                            }
                                        },
                                        onNavigateToEarnings = {
                                            if (!backStack.contains(HeroNavKey.Earnings)) {
                                                backStack.add(HeroNavKey.Earnings)
                                            }
                                        },
                                        onNavigateToBilling = {
                                            if (!backStack.contains(HeroNavKey.Billing)) {
                                                backStack.add(HeroNavKey.Billing)
                                            }
                                        }
                                    )
                                }
                                is HeroNavKey.Billing -> NavEntry(
                                    key = key,
                                    metadata = ListDetailSceneStrategy.detailPane()
                                ) {
                                    val viewModel: DashboardViewModel = hiltViewModel()
                                    com.example.budgethero.ui.dashboard.BillingLogScreen(
                                        viewModel = viewModel,
                                        onBack = { backStack.removeLastOrNull() }
                                    )
                                }
                                is HeroNavKey.Notebook -> NavEntry(
                                    key = key,
                                    metadata = ListDetailSceneStrategy.detailPane()
                                ) {
                                    val viewModel: NotebookViewModel = hiltViewModel()
                                    NotebookScreen(
                                        viewModel = viewModel,
                                        onBack = { backStack.removeLastOrNull() }
                                    )
                                }
                                is HeroNavKey.Earnings -> NavEntry(
                                    key = key,
                                    metadata = ListDetailSceneStrategy.detailPane()
                                ) {
                                    val viewModel: EarningsViewModel = hiltViewModel()
                                    EarningsScreen(
                                        viewModel = viewModel,
                                        onBack = { backStack.removeLastOrNull() }
                                    )
                                }
                                is HeroNavKey.Calendar -> NavEntry(
                                    key = key,
                                    metadata = ListDetailSceneStrategy.detailPane()
                                ) {
                                    val viewModel: CalendarViewModel = hiltViewModel()
                                    CalendarScreen(
                                        viewModel = viewModel,
                                        onBack = { backStack.removeLastOrNull() }
                                    )
                                }
                                else -> error("Unknown key: $key")
                            }
                        }
                    )
                }
            }
        }
    }
}

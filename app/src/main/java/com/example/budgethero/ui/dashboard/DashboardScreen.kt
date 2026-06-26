package com.example.budgethero.ui.dashboard

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.example.budgethero.R
import com.example.budgethero.data.model.BillFrequency
import com.example.budgethero.ui.components.ActionPill
import com.example.budgethero.ui.components.AddBillDialog
import com.example.budgethero.ui.theme.BudgetHeroTheme
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    onNavigateToCalendar: () -> Unit,
    onNavigateToNotebook: () -> Unit,
    onNavigateToEarnings: () -> Unit,
    onNavigateToBilling: () -> Unit
) {
    val dashboardState by viewModel.dashboardState.collectAsState()
    val weeklyData by viewModel.weeklyDailyData.collectAsState()
    val currentTimeOffset by viewModel.currentTimeOffset.collectAsState()
    var showAddIncomeDialog by remember { mutableStateOf(false) }
    var showAddSpendingDialog by remember { mutableStateOf(false) }
    var showAddBillDialog by remember { mutableStateOf(false) }
    var showExportDialog by remember { mutableStateOf(false) }
    var showPrivacyPolicyDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current

    val createDocumentLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/pdf") // Default, will change if CSV
    ) { uri ->
        uri?.let {
            val exportData = viewModel.getExportData()
            context.contentResolver.openOutputStream(it)?.use { outputStream ->
                if (it.toString().endsWith(".csv", ignoreCase = true) || 
                    context.contentResolver.getType(it)?.contains("csv") == true) {
                    ExportUtils.generateCsv(outputStream, exportData)
                } else {
                    ExportUtils.generatePdf(outputStream, exportData)
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_name).uppercase(), fontWeight = FontWeight.Black, letterSpacing = 2.sp) },
                actions = {
                    IconButton(
                        onClick = onNavigateToEarnings
                    ) {
                        Icon(Icons.Default.Payments, contentDescription = "Earnings")
                    }
                    IconButton(
                        onClick = onNavigateToNotebook
                    ) {
                        Icon(Icons.Default.EditNote, contentDescription = "Notebook")
                    }
                    IconButton(
                        onClick = onNavigateToCalendar
                    ) {
                        Icon(Icons.Default.CalendarMonth, contentDescription = "Calendar")
                    }
                }
            )
        },
        contentWindowInsets = WindowInsets.systemBars
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(top = 16.dp, bottom = 120.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    ProgressCard(
                        state = dashboardState,
                        onNextTimeScale = { viewModel.nextTimeScale() },
                        onPreviousPeriod = { viewModel.decrementPeriod() },
                        onNextPeriod = { viewModel.incrementPeriod() },
                        nextEnabled = currentTimeOffset < 0
                    )
                }

                item {
                    ActivityCalendarCard(
                        data = weeklyData,
                        onExportClick = { showExportDialog = true }
                    )
                }

                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onNavigateToBilling() },
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                        ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    ) {
                        Row(
                            modifier = Modifier
                                .padding(24.dp)
                                .fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.ReceiptLong,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.secondary
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    "Billing",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    String.format(java.util.Locale.US, "$%.2f", dashboardState.totalBilling),
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Icon(
                                    Icons.Default.ChevronRight,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.secondary
                                )
                            }
                        }
                    }
                }
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 48.dp, bottom = 32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        OperationalDevelopmentStamp()
                        
                        Text(
                            text = stringResource(R.string.privacy_policy_link),
                            style = MaterialTheme.typography.labelMedium.copy(
                                textDecoration = TextDecoration.Underline,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                            ),
                            modifier = Modifier.clickable { showPrivacyPolicyDialog = true }
                        )
                    }
                }
            }
        }
    }

    if (showPrivacyPolicyDialog) {
        AlertDialog(
            onDismissRequest = { showPrivacyPolicyDialog = false },
            title = { Text(stringResource(R.string.privacy_policy_title)) },
            text = {
                val scrollState = rememberScrollState()
                val annotatedString = androidx.compose.ui.text.AnnotatedString.Builder().apply {
                    val content = stringResource(R.string.privacy_policy_content)
                    // Simple parsing for <b> tags if desired, or just use plain text
                    // For now, using standard text display but could use an HTML text component
                    append(content.replace("<b>", "").replace("</b>", "").replace("<br>", "\n").replace("\\n", "\n"))
                }.toAnnotatedString()

                Box(modifier = Modifier.heightIn(max = 400.dp)) {
                    Text(
                        text = annotatedString,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.verticalScroll(scrollState)
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showPrivacyPolicyDialog = false }) {
                    Text("Close")
                }
            },
            dismissButton = {
                val webUrl = stringResource(R.string.privacy_policy_web_url)
                TextButton(onClick = {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(webUrl))
                    context.startActivity(intent)
                }) {
                    Text(stringResource(R.string.privacy_policy_view_online))
                }
            }
        )
    }

    if (showAddIncomeDialog) {
        com.example.budgethero.ui.components.BudgetEntryModal(
            title = "Log Income",
            isSpending = false,
            dailyGoalAverage = dashboardState.dailyGoalAverage,
            onDismiss = { showAddIncomeDialog = false },
            onConfirm = { desc, amount, date, uri, _ ->
                viewModel.addIncome(amount, desc, date, uri)
                showAddIncomeDialog = false
            }
        )
    }

    if (showAddSpendingDialog) {
        com.example.budgethero.ui.components.BudgetEntryModal(
            title = "Log Spending",
            isSpending = true,
            dailyGoalAverage = dashboardState.dailyGoalAverage,
            onDismiss = { showAddSpendingDialog = false },
            onConfirm = { desc, amount, date, uri, isBudget ->
                viewModel.addLineItem(desc, amount, date, uri, isBudget)
                showAddSpendingDialog = false
            }
        )
    }

    if (showAddBillDialog) {
        AddBillDialog(
            onDismiss = { showAddBillDialog = false },
            onConfirm = { name, amount, freq, endDate ->
                viewModel.addBill(name, amount, freq, endDate)
                showAddBillDialog = false
            }
        )
    }

    if (showExportDialog) {
        AlertDialog(
            onDismissRequest = { showExportDialog = false },
            title = { Text("Export Activity") },
            text = { Text("Choose a format to export your entire directory of activity.") },
            confirmButton = {
                TextButton(onClick = {
                    showExportDialog = false
                    createDocumentLauncher.launch("BudgetHero_Export_${LocalDate.now()}.pdf")
                }) {
                    Text("PDF")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showExportDialog = false
                    createDocumentLauncher.launch("BudgetHero_Export_${LocalDate.now()}.csv")
                }) {
                    Text("CSV")
                }
            }
        )
    }
}

@Composable
fun ActivityCalendarCard(
    data: List<DailyMetrics>,
    onExportClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "WEEKLY ACTIVITY",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                
                Surface(
                    onClick = onExportClick,
                    shape = RoundedCornerShape(50),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ) {
                    Text(
                        "Export",
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
            
            val maxAmount = (data.maxOfOrNull { maxOf(it.income, it.budgetSpending, it.nonBudgetSpending, Math.abs(it.delta)) } ?: 100.0).coerceAtLeast(1.0)
            
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                // Y-Axis
                Column(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(32.dp)
                        .padding(bottom = 20.dp), // Align with bars above labels
                    verticalArrangement = Arrangement.SpaceBetween,
                    horizontalAlignment = Alignment.End
                ) {
                    Text(String.format("$%.0f", maxAmount), style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(String.format("$%.0f", maxAmount / 2), style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("$0", style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }

                Spacer(modifier = Modifier.width(4.dp))

                // Chart Area
                Row(
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    data.forEach { metrics ->
                        Column(
                            modifier = Modifier.weight(1f),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Bottom
                        ) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth()
                                    .padding(horizontal = 2.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.BottomCenter
                            ) {
                                // Grid Lines
                                Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
                                    HorizontalDivider(modifier = Modifier.fillMaxWidth(), thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.1f))
                                    HorizontalDivider(modifier = Modifier.fillMaxWidth(), thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.1f))
                                    HorizontalDivider(modifier = Modifier.fillMaxWidth(), thickness = 1.dp, color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                                }

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 1.dp),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.Bottom
                                ) {
                                    // Income Bar (Green)
                                    if (metrics.income > 0) {
                                        Box(
                                            modifier = Modifier
                                                .width(7.dp)
                                                .fillMaxHeight((metrics.income / maxAmount).toFloat().coerceIn(0.01f, 1f))
                                                .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                                                .background(
                                                    Brush.verticalGradient(
                                                        colors = listOf(Color(0xFF81C784), Color(0xFF4CAF50))
                                                    )
                                                )
                                        )
                                        Spacer(modifier = Modifier.width(2.dp))
                                    }
                                    
                                    // Budget Spending (Blue)
                                    if (metrics.budgetSpending > 0) {
                                        Box(
                                            modifier = Modifier
                                                .width(7.dp)
                                                .fillMaxHeight((metrics.budgetSpending / maxAmount).toFloat().coerceIn(0.01f, 1f))
                                                .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                                                .background(
                                                    Brush.verticalGradient(
                                                        colors = listOf(MaterialTheme.colorScheme.primary.copy(alpha = 0.7f), MaterialTheme.colorScheme.primary)
                                                    )
                                                )
                                        )
                                        Spacer(modifier = Modifier.width(2.dp))
                                    }
                                    
                                    // Non-Budget Spending (Red)
                                    if (metrics.nonBudgetSpending > 0) {
                                        Box(
                                            modifier = Modifier
                                                .width(7.dp)
                                                .fillMaxHeight((metrics.nonBudgetSpending / maxAmount).toFloat().coerceIn(0.01f, 1f))
                                                .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                                                .background(
                                                    Brush.verticalGradient(
                                                        colors = listOf(MaterialTheme.colorScheme.error.copy(alpha = 0.7f), MaterialTheme.colorScheme.error)
                                                    )
                                                )
                                        )
                                        Spacer(modifier = Modifier.width(2.dp))
                                    }

                                    // Delta Dot (Indicator of surplus/deficit)
                                    Box(
                                        modifier = Modifier
                                            .size(7.dp)
                                            .clip(CircleShape)
                                            .background(if (metrics.delta >= 0) Color(0xFF4CAF50) else MaterialTheme.colorScheme.error)
                                            .border(1.dp, MaterialTheme.colorScheme.surface, CircleShape)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                metrics.date.dayOfWeek.name.take(3),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ProgressCard(
    state: DashboardState,
    onNextTimeScale: () -> Unit,
    onPreviousPeriod: () -> Unit,
    onNextPeriod: () -> Unit,
    nextEnabled: Boolean,
    modifier: Modifier = Modifier
) {
    val progress by animateFloatAsState(targetValue = state.progress, label = "progress")
    val dateRangeText = remember(state.periodStart, state.periodEnd, state.timeScale) {
        when (state.timeScale) {
            TimeScale.DAILY -> state.periodStart.format(DateTimeFormatter.ofPattern("MMM dd"))
            TimeScale.WEEKLY -> {
                val formatter = DateTimeFormatter.ofPattern("MMM dd")
                "${state.periodStart.format(formatter)} - ${state.periodEnd.format(formatter)}"
            }
            TimeScale.MONTHLY -> state.periodStart.format(DateTimeFormatter.ofPattern("MMMM yyyy"))
            TimeScale.YEARLY -> state.periodStart.year.toString()
        }
    }
    
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    onClick = onNextTimeScale,
                    color = Color.Transparent,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        "LONG RUN GOAL: ${state.timeScale.name}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        modifier = Modifier.padding(vertical = 4.dp, horizontal = 4.dp)
                    )
                }
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onPreviousPeriod,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            Icons.Default.ChevronLeft,
                            contentDescription = "Previous Period",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    
                    Text(
                        dateRangeText,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                    
                    IconButton(
                        onClick = onNextPeriod,
                        enabled = nextEnabled,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            Icons.Default.ChevronRight,
                            contentDescription = "Next Period",
                            tint = if (nextEnabled) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    progress = { 1f },
                    modifier = Modifier.size(200.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    strokeWidth = 16.dp,
                )
                CircularProgressIndicator(
                    progress = { progress.coerceIn(0f, 1f) },
                    modifier = Modifier.size(200.dp),
                    color = when {
                        state.netEarned >= state.onTrackAmount -> Color(0xFF4CAF50) // Green if ahead/on-track
                        progress > 0.15f -> MaterialTheme.colorScheme.primary
                        else -> MaterialTheme.colorScheme.error
                    },
                    strokeWidth = 16.dp,
                    strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
                )
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    val remaining = state.remainingAmount
                    Text(
                        if (remaining >= 0) String.format("$%.2f", remaining) else String.format("+$%.2f", Math.abs(remaining)),
                        style = MaterialTheme.typography.headlineLarge.copy(fontSize = 32.sp),
                        fontWeight = FontWeight.Black,
                        color = if (remaining >= 0) MaterialTheme.colorScheme.onSurface else Color(0xFF4CAF50)
                    )
                    Text(
                        if (remaining >= 0) "REMAINING" else "SURPLUS",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f))
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Total Income",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.secondary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        String.format("$%.2f", state.earnedIncome),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Total Spending",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        String.format("$%.2f", state.totalSpending),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Period Delta",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        if (state.totalDelta >= 0) String.format("+$%.2f", state.totalDelta) else String.format("$%.2f", state.totalDelta),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Black,
                        color = if (state.totalDelta >= 0) Color(0xFF4CAF50) else MaterialTheme.colorScheme.error
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(modifier = Modifier.fillMaxWidth(), thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Weekly Progress",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        if (state.weeklyDelta >= 0) String.format("+$%.2f", state.weeklyDelta) else String.format("$%.2f", state.weeklyDelta),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (state.weeklyDelta >= 0) Color(0xFF4CAF50) else MaterialTheme.colorScheme.error
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Monthly Running Delta",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        if (state.monthlyDelta >= 0) String.format("+$%.2f", state.monthlyDelta) else String.format("$%.2f", state.monthlyDelta),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (state.monthlyDelta >= 0) Color(0xFF4CAF50) else MaterialTheme.colorScheme.error
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Cumulative Delta (YTD)",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        if (state.cumulativeDelta >= 0) String.format("+$%.2f", state.cumulativeDelta) else String.format("$%.2f", state.cumulativeDelta),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (state.cumulativeDelta >= 0) Color(0xFF4CAF50) else MaterialTheme.colorScheme.error
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Daily Average Goal",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        String.format("$%.2f", state.dailyGoalAverage),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Notebook Spending",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        String.format("$%.2f", state.notebookSpentAmount),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Budget Fulfilled",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        String.format("$%.2f", state.budgetSpentAmount),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
@Composable
fun OperationalDevelopmentStamp(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = stringResource(R.string.designed_and_operated_by),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
            letterSpacing = 1.5.sp
        )
        Text(
            text = stringResource(R.string.operational_development_group),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
        )
        Text(
            text = stringResource(R.string.location_stamp),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
        )
    }
}

@Preview(showBackground = true)
@Composable
fun PrivacyPolicyLinkPreview() {
    BudgetHeroTheme {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 32.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                OperationalDevelopmentStamp()
                Text(
                    text = "Privacy Policy",
                    style = MaterialTheme.typography.labelMedium.copy(
                        textDecoration = TextDecoration.Underline,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                    )
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PrivacyPolicyDialogPreview() {
    BudgetHeroTheme {
        AlertDialog(
            onDismissRequest = { },
            title = { Text(stringResource(R.string.privacy_policy_title)) },
            text = {
                val content = stringResource(R.string.privacy_policy_content)
                val annotatedString = androidx.compose.ui.text.AnnotatedString.Builder().apply {
                    append(content.replace("<b>", "").replace("</b>", "").replace("<br>", "\n").replace("\\n", "\n"))
                }.toAnnotatedString()

                Box(modifier = Modifier.heightIn(max = 400.dp)) {
                    Text(
                        text = annotatedString,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.verticalScroll(rememberScrollState())
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { }) {
                    Text("Close")
                }
            }
        )
    }
}
    @Preview(showBackground = true)
@Composable
fun ActivityCalendarCardPreview() {
    com.example.budgethero.ui.theme.BudgetHeroTheme {
        Box(modifier = Modifier.padding(16.dp)) {
            ActivityCalendarCard(
                data = listOf(
                    DailyMetrics(java.time.LocalDate.now().minusDays(6), 100.0, 50.0, 20.0, 30.0),
                    DailyMetrics(java.time.LocalDate.now().minusDays(5), 0.0, 80.0, 10.0, -10.0),
                    DailyMetrics(java.time.LocalDate.now().minusDays(4), 150.0, 30.0, 0.0, 120.0),
                    DailyMetrics(java.time.LocalDate.now().minusDays(3), 200.0, 100.0, 50.0, 50.0),
                    DailyMetrics(java.time.LocalDate.now().minusDays(2), 50.0, 20.0, 10.0, 20.0),
                    DailyMetrics(java.time.LocalDate.now().minusDays(1), 0.0, 0.0, 0.0, 0.0),
                    DailyMetrics(java.time.LocalDate.now(), 120.0, 40.0, 15.0, 105.0),
                ),
                onExportClick = {}
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ProgressCardPreview() {
    BudgetHeroTheme {
        ProgressCard(
            state = DashboardState(
                totalBilling = 2000.0,
                billingFulfilled = 1500.0,
                targetAmount = 1500.0,
                earnedIncome = 1200.0,
                notebookSpentAmount = 50.0,
                timeScale = TimeScale.MONTHLY,
                remainingWorkHours = 80.0,
                formattedWorkTime = "80.0h",
                onTrackAmount = 1000.0,
                weeklyDelta = 50.0,
                monthlyDelta = 200.0,
                cumulativeDelta = 500.0,
                dailyGoalAverage = 100.0
            ),
            onNextTimeScale = {},
            onPreviousPeriod = {},
            onNextPeriod = {},
            nextEnabled = false
        )
    }
}

@Preview(showBackground = true)
@Composable
fun DashboardPreview() {
    BudgetHeroTheme {
        Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(top = 16.dp, bottom = 120.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    ProgressCard(
                        state = DashboardState(
                            targetAmount = 1500.0,
                            earnedIncome = 1200.0,
                            timeScale = TimeScale.MONTHLY,
                            formattedWorkTime = "80h"
                        ),
                        onNextTimeScale = {},
                        onPreviousPeriod = {},
                        onNextPeriod = {},
                        nextEnabled = false
                    )
                }

                item {
                    ActivityCalendarCard(data = emptyList(), onExportClick = {})
                }

                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                        ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    ) {
                        Row(
                            modifier = Modifier.padding(24.dp).fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.ReceiptLong, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                                Spacer(modifier = Modifier.width(12.dp))
                                Text("Billing", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
                            }
                            Text("$1000.00", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }

                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Privacy Policy",
                            style = MaterialTheme.typography.labelMedium.copy(
                                textDecoration = TextDecoration.Underline,
                                color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.6f)
                            )
                        )
                    }
                }
            }
        }
    }
}

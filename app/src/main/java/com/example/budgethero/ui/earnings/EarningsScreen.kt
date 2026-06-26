package com.example.budgethero.ui.earnings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.budgethero.data.model.Income
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import androidx.compose.material.icons.filled.Edit
import com.example.budgethero.ui.components.ActionPill
import com.example.budgethero.ui.components.HierarchicalLogList

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EarningsScreen(
    viewModel: EarningsViewModel,
    onBack: () -> Unit
) {
    val incomeItems by viewModel.incomeItems.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    var editingIncome by remember { mutableStateOf<Income?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Earnings", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    ActionPill(
                        text = "Add",
                        icon = Icons.Default.Add,
                        onClick = { showAddDialog = true },
                        modifier = Modifier.padding(end = 8.dp)
                    )
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text(
                    "Income Records",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Black,
                    modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
                )
            }

            item {
                HierarchicalLogList(
                    items = incomeItems,
                    getDate = { it.date },
                    itemContent = { item ->
                        EarningsItem(
                            item = item,
                            onEdit = { editingIncome = item },
                            onDelete = { viewModel.deleteIncome(item) }
                        )
                    }
                )
            }
            
            item { Spacer(modifier = Modifier.height(80.dp)) }
        }
    }

    if (showAddDialog || editingIncome != null) {
        val dashboardViewModel: com.example.budgethero.ui.dashboard.DashboardViewModel = androidx.hilt.navigation.compose.hiltViewModel()
        val dashboardState by dashboardViewModel.dashboardState.collectAsState()

        com.example.budgethero.ui.components.BudgetEntryModal(
            title = if (editingIncome == null) "Log Income" else "Edit Income",
            initialDescription = editingIncome?.description ?: "",
            initialAmount = editingIncome?.amount,
            initialDate = editingIncome?.date ?: LocalDate.now(),
            initialImageUri = editingIncome?.imageUri,
            isSpending = false,
            dailyGoalAverage = dashboardState.dailyGoalAverage,
            onDismiss = { 
                showAddDialog = false
                editingIncome = null
            },
            onConfirm = { desc, amt, date, uri, _ ->
                if (editingIncome != null) {
                    viewModel.updateIncome(editingIncome!!.copy(
                        description = desc,
                        amount = amt,
                        date = date,
                        imageUri = uri
                    ))
                } else {
                    viewModel.addIncome(desc, amt, date, uri)
                }
                showAddDialog = false
                editingIncome = null
            }
        )
    }
}

@Composable
fun EarningsItem(
    item: Income,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column {
            Row(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(item.description, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(
                        item.date.format(DateTimeFormatter.ofPattern("MMM dd, yyyy")),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
                Text(
                    String.format(java.util.Locale.US, "$%.2f", item.amount),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Black,
                    color = Color(0xFF4CAF50),
                    modifier = Modifier.padding(horizontal = 12.dp)
                )
                Row {
                    IconButton(onClick = onEdit) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit", tint = MaterialTheme.colorScheme.secondary)
                    }
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                    }
                }
            }
            
            if (item.imageUri != null) {
                AsyncImage(
                    model = item.imageUri,
                    contentDescription = "Income Receipt",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .clip(RoundedCornerShape(12.dp)),
                    contentScale = ContentScale.Crop
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

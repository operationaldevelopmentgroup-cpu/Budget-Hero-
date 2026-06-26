package com.example.budgethero.ui.notebook

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.budgethero.data.model.LineItem
import java.time.format.DateTimeFormatter

import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Remove
import com.example.budgethero.ui.components.ActionPill
import com.example.budgethero.ui.components.HierarchicalLogList
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotebookScreen(
    viewModel: NotebookViewModel,
    onBack: () -> Unit
) {
    val lineItems by viewModel.lineItems.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    var editingItem by remember { mutableStateOf<LineItem?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Spending Notebook", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    ActionPill(
                        text = "Add",
                        icon = Icons.Default.Remove,
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
                    "Recent Activity",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Black,
                    modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
                )
            }

            item {
                HierarchicalLogList(
                    items = lineItems,
                    getDate = { it.date },
                    itemContent = { item ->
                        NotebookItem(
                            item = item,
                            onEdit = { editingItem = item },
                            onDelete = { viewModel.deleteLineItem(item) }
                        )
                    }
                )
            }
            
            item { Spacer(modifier = Modifier.height(80.dp)) }
        }
    }

    if (showAddDialog || editingItem != null) {
        val dashboardViewModel: com.example.budgethero.ui.dashboard.DashboardViewModel = androidx.hilt.navigation.compose.hiltViewModel()
        val dashboardState by dashboardViewModel.dashboardState.collectAsState()
        
        com.example.budgethero.ui.components.BudgetEntryModal(
            title = if (editingItem == null) "Quick Note" else "Edit Note",
            initialDescription = editingItem?.description ?: "",
            initialAmount = editingItem?.amount,
            initialDate = editingItem?.date ?: LocalDate.now(),
            initialImageUri = editingItem?.imageUri,
            initialIsBudgetItem = editingItem?.isBudgetItem ?: false,
            isSpending = true,
            dailyGoalAverage = dashboardState.dailyGoalAverage,
            onDismiss = { 
                showAddDialog = false 
                editingItem = null
            },
            onConfirm = { desc, amount, date, uri, isBudget ->
                if (editingItem != null) {
                    viewModel.updateLineItem(editingItem!!.copy(
                        description = desc,
                        amount = amount,
                        date = date,
                        imageUri = uri,
                        isBudgetItem = isBudget
                    ))
                } else {
                    viewModel.addLineItem(desc, amount, date, uri, isBudget)
                }
                showAddDialog = false
                editingItem = null
            }
        )
    }
}

@Composable
fun NotebookItem(
    item: LineItem,
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
                    color = if (item.isBudgetItem) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
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
                    contentDescription = "Receipt",
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


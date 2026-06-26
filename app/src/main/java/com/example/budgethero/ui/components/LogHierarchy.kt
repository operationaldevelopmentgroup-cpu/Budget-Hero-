package com.example.budgethero.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.WeekFields
import java.util.*

@Composable
fun <T> HierarchicalLogList(
    items: List<T>,
    getDate: (T) -> LocalDate,
    itemContent: @Composable (T) -> Unit
) {
    if (items.isEmpty()) return

    val daysCount = items.map { getDate(it) }.distinct().size
    
    if (daysCount <= 1) {
        items.forEach { item ->
            itemContent(item)
            Spacer(modifier = Modifier.height(12.dp))
        }
        return
    }

    val now = LocalDate.now()
    val locale = LocalContext.current.resources.configuration.locales[0]
    val weekFields = WeekFields.of(locale)
    
    val groupedByMonth = items.groupBy { 
        val date = getDate(it)
        YearMonth(date.year, date.monthValue)
    }

    groupedByMonth.forEach { (month, monthItems) ->
        val isCurrentMonth = month.year == now.year && month.month == now.monthValue
        var monthExpanded by remember { mutableStateOf(isCurrentMonth) }

        ExpandableSection(
            title = month.format(),
            isExpanded = monthExpanded,
            onToggle = { monthExpanded = !monthExpanded },
            level = 0
        ) {
            val groupedByWeek = monthItems.groupBy { 
                val date = getDate(it)
                date.get(weekFields.weekOfWeekBasedYear())
            }

            groupedByWeek.forEach { (weekNumber, weekItems) ->
                val isCurrentWeek = isCurrentMonth && weekNumber == now.get(weekFields.weekOfWeekBasedYear())
                var weekExpanded by remember { mutableStateOf(isCurrentWeek) }
                val firstDayOfWeek = weekItems.minByOrNull { getDate(it) }?.let { getDate(it) }
                val lastDayOfWeek = weekItems.maxByOrNull { getDate(it) }?.let { getDate(it) }
                
                val weekTitle = if (firstDayOfWeek != null && lastDayOfWeek != null) {
                    "Week of ${firstDayOfWeek.format(DateTimeFormatter.ofPattern("MMM dd"))}"
                } else "Week"

                ExpandableSection(
                    title = weekTitle,
                    isExpanded = weekExpanded,
                    onToggle = { weekExpanded = !weekExpanded },
                    level = 1
                ) {
                    val groupedByDay = weekItems.groupBy { getDate(it) }
                    
                    groupedByDay.forEach { (day, dayItems) ->
                        val isCurrentDay = isCurrentWeek && day == now
                        var dayExpanded by remember { mutableStateOf(isCurrentDay) }
                        
                        ExpandableSection(
                            title = day.format(DateTimeFormatter.ofPattern("EEEE, MMM dd")),
                            isExpanded = dayExpanded,
                            onToggle = { dayExpanded = !dayExpanded },
                            level = 2
                        ) {
                            dayItems.forEach { item ->
                                itemContent(item)
                                Spacer(modifier = Modifier.height(8.dp))
                            }
                        }
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Composable
private fun ExpandableSection(
    title: String,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    level: Int,
    content: @Composable () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onToggle() }
                .padding(vertical = 8.dp, horizontal = (level * 8).dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                if (isExpanded) Icons.Default.KeyboardArrowDown else Icons.Default.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = title,
                style = when(level) {
                    0 -> MaterialTheme.typography.titleMedium
                    1 -> MaterialTheme.typography.titleSmall
                    else -> MaterialTheme.typography.bodyMedium
                },
                fontWeight = if (level < 2) FontWeight.Bold else FontWeight.Medium,
                color = if (level == 0) MaterialTheme.colorScheme.primary else Color.Unspecified
            )
        }
        AnimatedVisibility(visible = isExpanded) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = ((level + 1) * 8).dp)
            ) {
                content()
            }
        }
    }
}

private data class YearMonth(val year: Int, val month: Int) {
    fun format(): String {
        return LocalDate.of(year, month, 1).format(DateTimeFormatter.ofPattern("MMMM yyyy"))
    }
}

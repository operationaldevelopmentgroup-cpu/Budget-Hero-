package com.example.budgethero

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.budgethero.ui.theme.BudgetHeroTheme

@Preview(showBackground = true)
@Composable
fun BrandingPreview() {
    BudgetHeroTheme {
        Box(
            modifier = Modifier
                .size(width = 200.dp, height = 80.dp)
                .background(Color(0xFF0A0A0B)),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_launcher_foreground),
                contentDescription = "App Icon",
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

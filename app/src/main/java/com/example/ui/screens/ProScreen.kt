package com.example.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.ui.viewmodel.GameLingoViewModel

@Composable
fun ProScreen(
    viewModel: GameLingoViewModel,
    modifier: Modifier = Modifier
) {
    PremiumScreen(
        viewModel = viewModel,
        onBack = null,
        modifier = modifier
    )
}

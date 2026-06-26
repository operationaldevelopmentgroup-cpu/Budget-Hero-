package com.example.budgethero.ui.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
sealed class HeroNavKey : NavKey {
    @Serializable
    data object Splash : HeroNavKey()

    @Serializable
    data object Dashboard : HeroNavKey()
    
    @Serializable
    data object Calendar : HeroNavKey()

    @Serializable
    data object Notebook : HeroNavKey()

    @Serializable
    data object Earnings : HeroNavKey()

    @Serializable
    data object Billing : HeroNavKey()
}

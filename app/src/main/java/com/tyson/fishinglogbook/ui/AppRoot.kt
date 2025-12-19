package com.tyson.fishinglogbook.ui

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.tyson.fishinglogbook.ui.screens.HomeScreen

@Composable
fun AppRoot() {
    val nav = rememberNavController()
    NavHost(navController = nav, startDestination = "home") {
        composable("home") {
            HomeScreen(
                onStartTrip = { nav.navigate("home") } // placeholder for next batch
            )
        }
    }
}

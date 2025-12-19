package com.tyson.fishinglogbook.ui

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.tyson.fishinglogbook.ui.screens.AddTripScreen
import com.tyson.fishinglogbook.ui.screens.HomeScreen
import com.tyson.fishinglogbook.ui.screens.TripScreen

@Composable
fun AppRoot() {
    val nav = rememberNavController()

    NavHost(navController = nav, startDestination = "home") {
        composable("home") {
            HomeScreen(
                onStartTrip = { nav.navigate("trip_add") },
                onOpenActiveTrip = { nav.navigate("trip_active") }
            )
        }
        composable("trip_add") {
            AddTripScreen(onDone = { nav.popBackStack() })
        }
        composable("trip_active") {
            TripScreen(onBack = { nav.popBackStack() })
        }
    }
}

package com.tyson.fishinglogbook.ui

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.tyson.fishinglogbook.ui.screens.*

@Composable
fun AppRoot() {
    val nav = rememberNavController()

    NavHost(navController = nav, startDestination = "home") {
        composable("home") {
            HomeScreen(
                onStartTrip = { nav.navigate("trip_add") },
                onOpenActiveTrip = { nav.navigate("trip_active") },
                onAddCatch = { nav.navigate("catch_add") },
                onViewCatches = { nav.navigate("catch_list") },
                onOpenMap = { nav.navigate("map") }
            )
        }

        composable("map") {
            CatchMapScreen(
                onBack = { nav.popBackStack() },
                onOpenCatch = { id: Long -> nav.navigate("catch/$id") }
            )
        }

        composable("trip_add") { AddTripScreen(onDone = { nav.popBackStack() }) }
        composable("trip_active") { TripScreen(onBack = { nav.popBackStack() }) }

        composable("catch_add") { AddCatchScreen(onDone = { nav.popBackStack() }) }

        composable("catch_list") {
            CatchListScreen(
                onBack = { nav.popBackStack() },
                onOpenCatch = { id: Long -> nav.navigate("catch/$id") }
            )
        }

        composable("catch/{id}") { back ->
            val id = back.arguments?.getString("id")?.toLongOrNull() ?: 0L
            CatchDetailScreen(catchId = id, onBack = { nav.popBackStack() })
        }
    }
}

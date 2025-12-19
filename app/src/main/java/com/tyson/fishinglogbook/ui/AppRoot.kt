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
                onOpenCatch = { id -> nav.navigate("catch/$id") }
            )
        }

        composable("trip_add") { AddTripScreen { nav.popBackStack() } }
        composable("trip_active") { TripScreen { nav.popBackStack() } }
        composable("catch_add") { AddCatchScreen { nav.popBackStack() } }
        composable("catch_list") {
            CatchListScreen(
                onBack = { nav.popBackStack() },
                onOpenCatch = { id -> nav.navigate("catch/$id") }
            )
        }
        composable("catch/{id}") {
            val id = it.arguments?.getString("id")!!.toLong()
            CatchDetailScreen(id) { nav.popBackStack() }
        }
    }
}

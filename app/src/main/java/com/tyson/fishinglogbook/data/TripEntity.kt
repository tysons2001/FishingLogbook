package com.tyson.fishinglogbook.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "trips")
data class TripEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val startMillis: Long,
    val endMillis: Long? = null,
    val name: String? = null,
    val waterway: String? = null,
    val notes: String? = null
)

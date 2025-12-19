package com.tyson.fishinglogbook.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "catches",
    foreignKeys = [
        ForeignKey(
            entity = TripEntity::class,
            parentColumns = ["id"],
            childColumns = ["tripId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("tripId")]
)
data class CatchEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val tripId: Long?,
    val timestampMillis: Long,
    val species: String,
    val lengthCm: Double?,
    val weightKg: Double?,
    val lure: String?,
    val notes: String?,
    val latitude: Double?,
    val longitude: Double?,
    val accuracyM: Float?,
    val photoUri: String?,

    // Weather
    val weatherTempC: Double?,
    val weatherPressureHpa: Double?,
    val weatherFetchedAtMillis: Long?,

    // Moon
    val moonPhaseName: String?,
    val moon

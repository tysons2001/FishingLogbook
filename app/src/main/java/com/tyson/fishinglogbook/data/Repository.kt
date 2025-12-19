package com.tyson.fishinglogbook.data

class Repository(private val dao: AppDao) {
    fun observeTrips() = dao.observeTrips()
    fun observeActiveTrip() = dao.observeActiveTrip()
    fun observeAllCatches() = dao.observeAllCatches()

    suspend fun startTrip(name: String?, waterway: String?, notes: String?): Long {
        return dao.insertTrip(
            TripEntity(
                startMillis = System.currentTimeMillis(),
                endMillis = null,
                name = name,
                waterway = waterway,
                notes = notes
            )
        )
    }

    suspend fun endTrip(id: Long) {
        dao.endTrip(id, System.currentTimeMillis())
    }

    suspend fun addCatch(item: CatchEntity) = dao.insertCatch(item)
}

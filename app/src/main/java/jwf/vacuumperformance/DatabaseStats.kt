package jwf.vacuumperformance

interface VacuumTestTarget {
    suspend fun vacuum()
}

interface DatabaseTestTarget {
    suspend fun getStatistics(): DatabaseStats
}

data class DatabaseStats(
    val journalMode: String,
    val freelistPageCount: Int,
    val sizeBytes: Long,
    val cacheSizePages: Long,
    val pageSizeBytes: Long
)

package jwf.vacuumperformance

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class TestDatabase(
    context: Context,
    private val config: Config,
    private val onCreateCb: (db: SQLiteDatabase) -> Unit
) : SQLiteOpenHelper(context, config.fileName, VERSION, config.toOpenParams()) {
    override fun onCreate(db: SQLiteDatabase) {
        config.toPragmaStatements().forEach { db.rawQuery(it, emptyArray()).use { } }
        onCreateCb(db)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit

    data class Config(
        val fileName: String,
        val journalMode: JournalMode,
        val synchronousMode: SynchronousMode,
        val cacheSizePages: Int,
        val secureDeleteMode: SecureDeleteMode,
        val walAutoCheckpointPageLimit: Int
    ) {
        fun toOpenParams(): SQLiteDatabase.OpenParams {
            return SQLiteDatabase.OpenParams.Builder()
                .apply {
                    setJournalMode(journalMode.name)
                    setSynchronousMode(synchronousMode.name)
                }
                .build()
        }

        fun toPragmaStatements(): Array<String> = arrayOf(
            "PRAGMA cache_size=${cacheSizePages}",
            "PRAGMA secure_delete=${secureDeleteMode.name}",
        )

        enum class SynchronousMode { EXTRA, FULL, NORMAL, OFF }
        enum class JournalMode { DELETE, TRUNCATE, PERSIST, MEMORY, WAL, OFF }
        enum class AutoVacuumMode(val value: Int) { NONE(0), FULL(1), INCREMENTAL(2) }
        enum class SecureDeleteMode { TRUE, FALSE, FAST }
    }

    companion object {
        private const val VERSION = 1
    }
}

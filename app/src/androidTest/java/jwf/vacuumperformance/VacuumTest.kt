package jwf.vacuumperformance

import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.Executors
import kotlin.random.Random
import kotlin.system.measureNanoTime

/**
 * Instrumented test, which will execute on an Android device.
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
@RunWith(AndroidJUnit4::class)
class VacuumTest {
    @Test
    fun evaluateVacuum() = runBlocking<Unit> {

        // Context of the app under test.
        val appContext =
            InstrumentationRegistry.getInstrumentation().targetContext.applicationContext as App

        // Tweak these values to try different database configurations.
        val config = TestDatabase.Config(
            fileName = "wal_fullSynchronous_autovacuum_pageSize1024_secureDelete_autoCheckpoint1",
            journalMode = TestDatabase.Config.JournalMode.WAL,
            synchronousMode = TestDatabase.Config.SynchronousMode.FULL,
            cacheSizePages = 1,
            secureDeleteMode = TestDatabase.Config.SecureDeleteMode.TRUE,
            walAutoCheckpointPageLimit = 1
        )

        // Define an executor upon which database accesses will occur.
        val executor = Executors.newFixedThreadPool(4)

        // Initialize the database.
        val dbAccess = SQLiteDatabaseAccess(executor)
        val db = appContext.openDatabase(config) {
            it.execSQL(
                """
                CREATE TABLE my_table (
                    name TEXT NOT NULL,
                    blob BLOB NOT NULL
                )
                """.trimIndent()
            )
        }

        suspend fun getStats(): DatabaseStats {
            dbAccess.query(db.readableDatabase, "PRAGMA wal_checkpoint") { }
            return DatabaseStats(
                dbAccess.query(db.readableDatabase, "PRAGMA journal_mode") { getString(0) }.first(),
                dbAccess.query(db.readableDatabase, "PRAGMA freelist_count") { getInt(0) }.first(),
                appContext.getDatabasePath(config.fileName).length(),
                dbAccess.query(db.readableDatabase, "PRAGMA cache_size") { getLong(0) }.first(),
                dbAccess.query(db.readableDatabase, "PRAGMA page_size") { getLong(0) }.first(),
            )
        }

        log("Before: ${getStats()}")

        val random = Random(0L)
        val mutex = Mutex()
        val existing = mutableSetOf<Int>()

        val insertionTime = measureNanoTime {
            dbAccess.transact(db.writableDatabase) {
                val bytes = ByteArray(1024)
                repeat(10000) {
                    random.nextBytes(bytes)
                    execute(
                        "INSERT INTO my_table VALUES (?, ?)",
                        listOf(
                            "item_$it",
                            bytes.toString(Charsets.UTF_8)
                        )
                    )
                    existing.add(it)
                }
            }
        }


        log("After Inserting (took ${insertionTime / 1000000.0}ms): ${getStats()}")

        repeat(3000) {
            launch {
                var toDelete = -1
                mutex.withLock {
                    while (toDelete !in existing) {
                        toDelete = random.nextInt(0, 10000)
                    }
                    existing.remove(toDelete)
                }
                dbAccess.execute(
                    db.writableDatabase,
                    "DELETE FROM my_table WHERE name = ?",
                    listOf("item_$toDelete")
                )
                log("Deleted the $it'd item")
            }
        }

        log("After Deleting: ${getStats()}")

        val vacuumTime = measureNanoTime {
            dbAccess.cleanup(db.writableDatabase)
        }

        log("After VACUUMing (took ${vacuumTime / 1000000.0}ms): ${getStats()}")
    }

    private fun log(message: String) = Log.i("TEST_RESULTS", message)
}

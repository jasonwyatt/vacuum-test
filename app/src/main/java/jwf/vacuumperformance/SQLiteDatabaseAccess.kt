package jwf.vacuumperformance

import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.os.CancellationSignal
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.concurrent.ExecutorService
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

class SQLiteDatabaseAccess(
    private val dbAccessExecutor: ExecutorService,
) : DatabaseAccess<SQLiteDatabase> {
    override suspend fun <Result> query(
        db: SQLiteDatabase,
        query: String,
        args: List<String>,
        resultMapper: DatabaseAccess.Record.() -> Result
    ): List<Result> = suspendCancellableCoroutine { continuation ->
        val cancellationSignal = CancellationSignal()
        continuation.invokeOnCancellation { cancellationSignal.cancel() }

        dbAccessExecutor.execute {
            val results = mutableListOf<Result>()

            try {
                db.rawQuery(query, args.toTypedArray(), cancellationSignal).use {
                    val recordAccess = SQLiteRecord(it)
                    while (it.moveToNext()) {
                        results.add(recordAccess.resultMapper())
                    }
                }

                continuation.resume(results)
            } catch (e: Throwable) {
                continuation.resumeWithException(e)
            }
        }
    }

    suspend fun cleanup(db: SQLiteDatabase) = suspendCoroutine<Unit> {
        dbAccessExecutor.execute {
            db.execSQL("VACUUM")
            it.resume(Unit)
        }
    }

    override suspend fun execute(db: SQLiteDatabase, statement: String, args: List<String>) =
        transact(db) { execute(statement, args) }

    override suspend fun <T> transact(
        db: SQLiteDatabase,
        block: DatabaseAccess.Transaction.() -> T
    ): T = suspendCancellableCoroutine { continuation ->
        val cancellationSignal = CancellationSignal()
        continuation.invokeOnCancellation { cancellationSignal.cancel() }

        dbAccessExecutor.execute {
            db.beginTransaction()
            try {
                val transaction = SQLiteTransaction(db, cancellationSignal)
                val result = transaction.block()
                db.setTransactionSuccessful()
                continuation.resume(result)
            } catch (e: Throwable) {
                continuation.resumeWithException(e)
            } finally {
                db.endTransaction()
            }
        }
    }

    private class SQLiteRecord(private val cursor: Cursor) : DatabaseAccess.Record {
        override fun getBlob(fieldIndex: Int): ByteArray = cursor.getBlob(fieldIndex)
        override fun getString(fieldIndex: Int): String = cursor.getString(fieldIndex)
        override fun getShort(fieldIndex: Int): Short = cursor.getShort(fieldIndex)
        override fun getInt(fieldIndex: Int): Int = cursor.getInt(fieldIndex)
        override fun getLong(fieldIndex: Int): Long = cursor.getLong(fieldIndex)
        override fun getFloat(fieldIndex: Int): Float = cursor.getFloat(fieldIndex)
        override fun getDouble(fieldIndex: Int): Double = cursor.getDouble(fieldIndex)
        override fun getType(fieldIndex: Int): Int = cursor.getType(fieldIndex)
        override fun isNull(fieldIndex: Int): Boolean = cursor.isNull(fieldIndex)
    }

    private class SQLiteTransaction(
        private val db: SQLiteDatabase,
        private val cancellationSignal: CancellationSignal
    ) : DatabaseAccess.Transaction {
        override fun <Result> query(
            query: String,
            args: List<String>,
            resultMapper: DatabaseAccess.Record.() -> Result
        ): List<Result> {
            val results = mutableListOf<Result>()
            db.rawQuery(query, args.toTypedArray(), cancellationSignal).use {
                val recordAccess = SQLiteRecord(it)
                while (it.moveToNext()) {
                    results.add(recordAccess.resultMapper())
                }
            }
            return results
        }

        override fun execute(statement: String, args: List<String>) {
            db.execSQL(statement, args.toTypedArray())
        }
    }
}

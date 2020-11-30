package jwf.vacuumperformance

import android.app.Application
import android.database.sqlite.SQLiteDatabase

class App : Application() {

    fun openDatabase(
        config: TestDatabase.Config,
        onCreate: (db: SQLiteDatabase) -> Unit
    ): TestDatabase = TestDatabase(this, config, onCreate)
}

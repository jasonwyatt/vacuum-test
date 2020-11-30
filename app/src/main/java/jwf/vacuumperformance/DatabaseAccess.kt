package jwf.vacuumperformance

interface DatabaseAccess<DatabaseConnection> {
    suspend fun <Result> query(
        db: DatabaseConnection,
        query: String,
        args: List<String> = emptyList(),
        resultMapper: Record.() -> Result
    ): List<Result>

    suspend fun execute(
        db: DatabaseConnection,
        statement: String,
        args: List<String> = emptyList()
    )

    suspend fun <T> transact(
        db: DatabaseConnection,
        block: Transaction.() -> T
    ): T

    interface Record {
        fun getBlob(fieldIndex: Int): ByteArray
        fun getString(fieldIndex: Int): String
        fun getShort(fieldIndex: Int): Short
        fun getInt(fieldIndex: Int): Int
        fun getLong(fieldIndex: Int): Long
        fun getFloat(fieldIndex: Int): Float
        fun getDouble(fieldIndex: Int): Double
        fun getType(fieldIndex: Int): Int
        fun isNull(fieldIndex: Int): Boolean
    }

    interface Transaction {
        fun <Result> query(
            query: String,
            args: List<String> = emptyList(),
            resultMapper: Record.() -> Result
        ): List<Result>

        fun execute(statement: String, args: List<String> = emptyList())
    }
}

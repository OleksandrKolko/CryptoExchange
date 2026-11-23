package com.example.cryptoexchange.data.local

import android.content.Context
import androidx.room3.Dao
import androidx.room3.Database
import androidx.room3.Entity
import androidx.room3.PrimaryKey
import androidx.room3.Query
import androidx.room3.Room
import androidx.room3.RoomDatabase
import androidx.room3.Transaction
import androidx.room3.Upsert
import androidx.sqlite.driver.AndroidSQLiteDriver
import kotlinx.coroutines.flow.Flow

/** Room Entity — окремий тип від DTO та domain-моделі */
@Entity(tableName = "coins")
data class CoinEntity(
    @PrimaryKey val id: String,        // стабільний ID активу з API ("bitcoin") → без дублікатів
    val symbol: String,
    val name: String,
    val logoUrl: String?,
    val priceUsd: Double?,
    val change24hPercent: Double?,
    val high24hUsd: Double?,
    val low24hUsd: Double?,
    val marketCapUsd: Double?,
    val volume24hUsd: Double?,
    val rank: Int,
    val updatedAtMillis: Long
)

@Dao
abstract class CoinDao {

    @Query("SELECT * FROM coins ORDER BY `rank` ASC")
    abstract fun observeCoins(): Flow<List<CoinEntity>>

    @Query("SELECT MAX(updatedAtMillis) FROM coins")
    abstract fun observeLastUpdate(): Flow<Long?>

    @Query("SELECT COUNT(*) FROM coins")
    abstract suspend fun count(): Int

    @Upsert
    abstract suspend fun upsertAll(coins: List<CoinEntity>)

    @Query("DELETE FROM coins WHERE id NOT IN (:keepIds)")
    abstract suspend fun deleteNotIn(keepIds: List<String>)

    /**
     * Атомарна заміна рейтингу в ОДНІЙ транзакції:
     * валюти, що вийшли з топ-30, видаляються; решта оновлюється без дублікатів.
     * Викликається лише ПІСЛЯ успішної відповіді API.
     */
    @Transaction
    open suspend fun replaceTopList(coins: List<CoinEntity>) {
        deleteNotIn(coins.map { it.id })
        upsertAll(coins)
    }
}

@Database(entities = [CoinEntity::class], version = 1, exportSchema = false)
abstract class CoinDatabase : RoomDatabase() {

    abstract fun coinDao(): CoinDao

    companion object {
        @Volatile
        private var INSTANCE: CoinDatabase? = null

        fun getDatabase(context: Context): CoinDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE?.let { return it }
                Room.databaseBuilder(
                    context.applicationContext,
                    CoinDatabase::class.java,
                    "crypto.db"
                )
                    .setDriver(AndroidSQLiteDriver())
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}

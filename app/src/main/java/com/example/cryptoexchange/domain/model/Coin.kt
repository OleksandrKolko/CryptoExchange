package com.example.cryptoexchange.domain.model

/**
 * Модель криптовалюти предметної області.
 * Не залежить від Room, Retrofit чи Android UI.
 */
data class Coin(
    val id: String,                   // стабільний ID активу з API ("bitcoin")
    val symbol: String,               // BTC
    val name: String,                 // Bitcoin
    val logoUrl: String?,             // може бути відсутнім
    val priceUsd: Double?,            // null → «Немає даних», а не 0
    val change24hPercent: Double?,    // зміна за 24 год, %
    val high24hUsd: Double?,          // максимум за 24 год
    val low24hUsd: Double?,           // мінімум за 24 год
    val marketCapUsd: Double?,        // ринкова капіталізація
    val volume24hUsd: Double?,        // обсяг торгів за 24 год
    val rank: Int,                    // місце в рейтингу (1..30)
    val updatedAtMillis: Long         // час успішного збереження
)

/** Результат синхронізації — без прив'язки до HTTP чи WorkManager */
sealed interface SyncResult {
    data class Success(val count: Int) : SyncResult
    data object NetworkError : SyncResult                     // тимчасова → повторити
    data class ServerError(val code: Int) : SyncResult        // 5xx → повторити
    data class RateLimited(val retryAfterSeconds: Long?) : SyncResult
    data object Unauthorized : SyncResult                     // ключ / права → НЕ повторювати
    data class InvalidResponse(val reason: String) : SyncResult
}

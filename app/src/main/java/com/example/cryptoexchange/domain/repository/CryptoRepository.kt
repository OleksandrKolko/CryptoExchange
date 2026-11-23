package com.example.cryptoexchange.domain.repository

import com.example.cryptoexchange.domain.model.Coin
import com.example.cryptoexchange.domain.model.SyncResult
import kotlinx.coroutines.flow.Flow

/** Контракт репозиторію. Реалізація — у шарі data. */
interface CryptoRepository {

    /** Збережений рейтинг (джерело — локальна база), відсортований за місцем */
    fun observeCoins(): Flow<List<Coin>>

    /** Час останнього успішного збереження, null — даних ще немає */
    fun observeLastUpdate(): Flow<Long?>

    /** Завантажити топ-30 з API та атомарно замінити збережений рейтинг */
    suspend fun syncTopCoins(): SyncResult
}

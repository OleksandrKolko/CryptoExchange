package com.example.cryptoexchange.domain.usecase

import com.example.cryptoexchange.domain.model.Coin
import com.example.cryptoexchange.domain.model.SyncResult
import com.example.cryptoexchange.domain.repository.CryptoRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

/** Сценарій: отримати збережені валюти з пошуком (⭐) серед локальних даних */
class ObserveCoinsUseCase(private val repository: CryptoRepository) {

    operator fun invoke(query: Flow<String>): Flow<List<Coin>> =
        combine(repository.observeCoins(), query) { coins, q ->
            val text = q.trim()
            if (text.isEmpty()) coins
            else coins.filter {
                it.name.contains(text, ignoreCase = true) || it.symbol.contains(text, ignoreCase = true)
            }
        }
}

/** Сценарій: час останнього успішного оновлення */
class ObserveLastUpdateUseCase(private val repository: CryptoRepository) {
    operator fun invoke(): Flow<Long?> = repository.observeLastUpdate()
}

/** Сценарій синхронізації: викликається з Worker */
class SyncCoinsUseCase(private val repository: CryptoRepository) {
    suspend operator fun invoke(): SyncResult = repository.syncTopCoins()
}

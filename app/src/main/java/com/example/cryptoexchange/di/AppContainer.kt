package com.example.cryptoexchange.di

import android.content.Context
import com.example.cryptoexchange.BuildConfig
import com.example.cryptoexchange.data.local.CoinDatabase
import com.example.cryptoexchange.data.remote.CoinGeckoNetwork
import com.example.cryptoexchange.data.repository.CryptoRepositoryImpl
import com.example.cryptoexchange.domain.repository.CryptoRepository
import com.example.cryptoexchange.domain.usecase.ObserveCoinsUseCase
import com.example.cryptoexchange.domain.usecase.ObserveLastUpdateUseCase
import com.example.cryptoexchange.domain.usecase.SyncCoinsUseCase
import com.example.cryptoexchange.worker.SyncScheduler

/**
 * Ручне впровадження залежностей: один спільний контейнер на застосунок.
 * Використовують і ViewModel, і Worker.
 */
class AppContainer(context: Context) {

    // Порожній ключ допустимий: CoinGecko відповідає і без нього, але з меншим лімітом
    private val apiKey: String = BuildConfig.COINGECKO_API_KEY

    private val repository: CryptoRepository by lazy {
        CryptoRepositoryImpl(
            api = CoinGeckoNetwork.createApi(apiKey),
            dao = CoinDatabase.getDatabase(context).coinDao()
        )
    }

    val observeCoinsUseCase by lazy { ObserveCoinsUseCase(repository) }
    val observeLastUpdateUseCase by lazy { ObserveLastUpdateUseCase(repository) }
    val syncCoinsUseCase by lazy { SyncCoinsUseCase(repository) }

    val syncScheduler by lazy { SyncScheduler(context) }
}

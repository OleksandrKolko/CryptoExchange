package com.example.cryptoexchange.data.repository

import android.util.Log
import com.example.cryptoexchange.data.local.CoinDao
import com.example.cryptoexchange.data.mapper.toDomain
import com.example.cryptoexchange.data.mapper.toEntity
import com.example.cryptoexchange.data.remote.CoinGeckoApi
import com.example.cryptoexchange.domain.model.Coin
import com.example.cryptoexchange.domain.model.SyncResult
import com.example.cryptoexchange.domain.repository.CryptoRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.io.IOException

class CryptoRepositoryImpl(
    private val api: CoinGeckoApi,
    private val dao: CoinDao,
    private val clock: () -> Long = System::currentTimeMillis
) : CryptoRepository {

    override fun observeCoins(): Flow<List<Coin>> =
        dao.observeCoins().map { list -> list.map { it.toDomain() } }

    override fun observeLastUpdate(): Flow<Long?> = dao.observeLastUpdate()

    override suspend fun syncTopCoins(): SyncResult {
        val response = try {
            api.getTopCoins()          // таблицю НЕ чіпаємо до завершення запиту
        } catch (e: CancellationException) {
            throw e
        } catch (e: IOException) {
            Log.w(TAG, "Мережева помилка: ${e.javaClass.simpleName}")
            return SyncResult.NetworkError
        } catch (e: Exception) {
            Log.e(TAG, "Некоректна відповідь: ${e.javaClass.simpleName}")
            return SyncResult.InvalidResponse("Не вдалося розібрати відповідь")
        }

        when (response.code()) {
            401, 403 -> return SyncResult.Unauthorized
            429 -> return SyncResult.RateLimited(
                response.headers()["Retry-After"]?.toLongOrNull()
            )
        }
        if (response.code() >= 500) return SyncResult.ServerError(response.code())
        if (!response.isSuccessful) return SyncResult.InvalidResponse("HTTP ${response.code()}")

        val items = response.body() ?: return SyncResult.InvalidResponse("Порожня відповідь")

        val now = clock()
        val entities = items.mapIndexedNotNull { index, dto ->
            dto.toEntity(rank = index + 1, updatedAtMillis = now)
        }.distinctBy { it.id }

        if (entities.isEmpty()) {
            // Некоректна / порожня відповідь: зберігаємо попередні записи
            return SyncResult.InvalidResponse("Сервер не повернув жодної валюти")
        }

        dao.replaceTopList(entities)   // одна транзакція
        Log.d(TAG, "Збережено в Room: ${entities.size} валют")
        return SyncResult.Success(entities.size)
    }

    companion object {
        private const val TAG = "CryptoSync"
    }
}

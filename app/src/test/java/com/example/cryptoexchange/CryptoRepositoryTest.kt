package com.example.cryptoexchange

import com.example.cryptoexchange.data.local.CoinDao
import com.example.cryptoexchange.data.local.CoinEntity
import com.example.cryptoexchange.data.mapper.toEntity
import com.example.cryptoexchange.data.remote.CoinGeckoApi
import com.example.cryptoexchange.data.remote.CoinMarketDto
import com.example.cryptoexchange.data.repository.CryptoRepositoryImpl
import com.example.cryptoexchange.domain.model.SyncResult
import com.example.cryptoexchange.presentation.components.formatPrice
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.Response
import java.io.IOException

/** Фейковий DAO в пам'яті */
private class FakeCoinDao : CoinDao() {
    val table = MutableStateFlow<List<CoinEntity>>(emptyList())

    override fun observeCoins(): Flow<List<CoinEntity>> = table.map { it.sortedBy(CoinEntity::rank) }
    override fun observeLastUpdate(): Flow<Long?> = table.map { list -> list.maxOfOrNull { it.updatedAtMillis } }
    override suspend fun count(): Int = table.value.size
    override suspend fun upsertAll(coins: List<CoinEntity>) {
        val byId = table.value.associateBy { it.id }.toMutableMap()
        coins.forEach { byId[it.id] = it }
        table.value = byId.values.toList()
    }
    override suspend fun deleteNotIn(keepIds: List<String>) {
        table.value = table.value.filter { it.id in keepIds }
    }
}

/** Фейковий API: віддає заздалегідь задану відповідь або кидає помилку */
private class FakeApi : CoinGeckoApi {
    var next: () -> Response<List<CoinMarketDto>> = { error("not set") }

    override suspend fun getTopCoins(
        vsCurrency: String, order: String, perPage: Int, page: Int
    ): Response<List<CoinMarketDto>> = next()
}

private fun asset(symbol: String, price: Double? = 1.0, logo: String? = "https://x/$symbol.png") =
    CoinMarketDto(symbol.lowercase(), symbol.lowercase(), "$symbol coin", logo, price, null, null, null, null, null)

private fun ok(vararg assets: CoinMarketDto) = Response.success(assets.toList())

class CryptoRepositoryTest {

    private val dao = FakeCoinDao()
    private val api = FakeApi()
    private var now = 1_000L
    private val repo = CryptoRepositoryImpl(api, dao, clock = { now })

    @Test
    fun `повторна синхронізація не створює дублікатів`() = runBlocking {
        api.next = { ok(asset("BTC"), asset("ETH")) }
        repo.syncTopCoins()
        repo.syncTopCoins()
        assertEquals(2, dao.count())
    }

    @Test
    fun `новий рейтинг замінює попередній`() = runBlocking {
        api.next = { ok(asset("BTC"), asset("ETH"), asset("XRP")) }
        repo.syncTopCoins()

        api.next = { ok(asset("ETH"), asset("SOL")) }   // BTC та XRP вийшли з топу
        repo.syncTopCoins()

        assertEquals(listOf("ETH", "SOL"), dao.table.value.sortedBy { it.rank }.map { it.symbol })
        assertEquals(1, dao.table.value.first { it.symbol == "ETH" }.rank)
    }

    @Test
    fun `невдала синхронізація зберігає кеш`() = runBlocking {
        api.next = { ok(asset("BTC")) }
        repo.syncTopCoins()
        val before = dao.table.value

        now = 2_000L
        api.next = { throw IOException("offline") }
        assertEquals(SyncResult.NetworkError, repo.syncTopCoins())

        api.next = { Response.error(500, "".toResponseBody()) }
        assertTrue(repo.syncTopCoins() is SyncResult.ServerError)

        api.next = { ok() }                                    // порожня відповідь
        assertTrue(repo.syncTopCoins() is SyncResult.InvalidResponse)

        assertEquals(before, dao.table.value)                  // і час оновлення не змінився
    }

    @Test
    fun `помилка ключа розпізнається окремо`() = runBlocking {
        api.next = { Response.error(401, "".toResponseBody()) }
        assertEquals(SyncResult.Unauthorized, repo.syncTopCoins())
    }

    @Test
    fun `DTO без ціни та логотипу не перетворюється на нуль`() {
        val entity = asset("PEPE", price = null, logo = null).toEntity(rank = 1, updatedAtMillis = 0)!!
        assertNull(entity.priceUsd)
        assertNull(entity.logoUrl)
        assertEquals("Немає даних", formatPrice(entity.priceUsd))
    }

    @Test
    fun `DTO без ID пропускається`() {
        assertNull(CoinMarketDto(null, "x", "X", null, 1.0, null, null, null, null, null).toEntity(1, 0))
    }

    @Test
    fun `малі ціни не округлюються до нуля`() {
        assertEquals("$0.00001234", formatPrice(0.000012341))
        assertEquals("$98,123.46", formatPrice(98123.456))
    }
}

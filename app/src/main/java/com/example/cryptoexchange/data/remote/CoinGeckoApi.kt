package com.example.cryptoexchange.data.remote

import com.google.gson.annotations.SerializedName
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response as OkHttpResponse
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

/**
 * CoinGecko Demo API — список монет з ринковими даними.
 * GET https://api.coingecko.com/api/v3/coins/markets
 *     ?vs_currency=usd&order=market_cap_desc&per_page=30&page=1
 */
interface CoinGeckoApi {

    @GET("coins/markets")
    suspend fun getTopCoins(
        @Query("vs_currency") vsCurrency: String = "usd",
        @Query("order") order: String = "market_cap_desc",
        @Query("per_page") perPage: Int = 30,
        @Query("page") page: Int = 1
    ): Response<List<CoinMarketDto>>
}

// ================= DTO (за структурою JSON відповіді) =================

/** Один елемент масиву відповіді /coins/markets */
data class CoinMarketDto(
    @SerializedName("id") val id: String?,                       // "bitcoin" — стабільний ID
    @SerializedName("symbol") val symbol: String?,               // "btc"
    @SerializedName("name") val name: String?,                   // "Bitcoin"
    @SerializedName("image") val image: String?,                 // URL логотипа
    @SerializedName("current_price") val currentPrice: Double?,
    @SerializedName("market_cap") val marketCap: Double?,
    @SerializedName("total_volume") val totalVolume: Double?,
    @SerializedName("high_24h") val high24h: Double?,
    @SerializedName("low_24h") val low24h: Double?,
    @SerializedName("price_change_percentage_24h") val priceChangePercentage24h: Double?
)

// ================= Мережевий модуль =================

/**
 * Додає заголовок x-cg-demo-api-key, якщо ключ задано.
 * Без ключа CoinGecko теж відповідає, але з меншим лімітом за IP.
 */
class ApiKeyInterceptor(private val apiKey: String) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): OkHttpResponse {
        val request = if (apiKey.isNotBlank()) {
            chain.request().newBuilder()
                .header("x-cg-demo-api-key", apiKey)
                .build()
        } else {
            chain.request()
        }
        return chain.proceed(request)
    }
}

object CoinGeckoNetwork {

    private const val BASE_URL = "https://api.coingecko.com/api/v3/"

    fun createApi(apiKey: String): CoinGeckoApi {
        val client = OkHttpClient.Builder()
            .addInterceptor(ApiKeyInterceptor(apiKey))   // ключ НЕ логуємо
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build()

        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(CoinGeckoApi::class.java)
    }
}

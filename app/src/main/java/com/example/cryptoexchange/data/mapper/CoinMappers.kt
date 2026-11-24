package com.example.cryptoexchange.data.mapper

import com.example.cryptoexchange.data.local.CoinEntity
import com.example.cryptoexchange.data.remote.CoinMarketDto
import com.example.cryptoexchange.domain.model.Coin

/**
 * DTO → Entity. Записи без ID чи символу пропускаються (повертається null).
 * Відсутні ціна/логотип залишаються null — НЕ підміняються нулем.
 */
fun CoinMarketDto.toEntity(rank: Int, updatedAtMillis: Long): CoinEntity? {
    val safeId = id?.takeIf { it.isNotBlank() } ?: return null
    val safeSymbol = symbol?.takeIf { it.isNotBlank() }?.uppercase() ?: return null
    return CoinEntity(
        id = safeId,
        symbol = safeSymbol,
        name = name?.takeIf { it.isNotBlank() } ?: safeSymbol,
        logoUrl = image?.takeIf { it.isNotBlank() },
        priceUsd = currentPrice,
        change24hPercent = priceChangePercentage24h,
        high24hUsd = high24h,
        low24hUsd = low24h,
        marketCapUsd = marketCap,
        volume24hUsd = totalVolume,
        rank = rank,
        updatedAtMillis = updatedAtMillis
    )
}

/** Entity → domain */
fun CoinEntity.toDomain(): Coin = Coin(
    id = id,
    symbol = symbol,
    name = name,
    logoUrl = logoUrl,
    priceUsd = priceUsd,
    change24hPercent = change24hPercent,
    high24hUsd = high24hUsd,
    low24hUsd = low24hUsd,
    marketCapUsd = marketCapUsd,
    volume24hUsd = volume24hUsd,
    rank = rank,
    updatedAtMillis = updatedAtMillis
)

package com.example.cryptoexchange.presentation.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.example.cryptoexchange.R
import com.example.cryptoexchange.domain.model.Coin

private val PositiveColor = Color(0xFF2E7D32)
private val NegativeColor = Color(0xFFC62828)

/** Логотип через Coil 3: заглушка на час завантаження та при помилці / відсутності URL */
@Composable
fun CoinLogo(url: String?, size: Dp, modifier: Modifier = Modifier) {
    val placeholder = painterResource(R.drawable.ic_coin_placeholder)
    AsyncImage(
        model = url,
        contentDescription = null,
        placeholder = placeholder,
        error = placeholder,
        fallback = placeholder,        // url == null
        modifier = modifier
            .size(size)
            .clip(CircleShape)
    )
}

/** Картка у списку (вигляд — як у відеоприкладі: «BTC / USD», ціна, час оновлення) */
@Composable
fun CoinCard(coin: Coin, isSelected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer
            else MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(
                "${coin.rank}",
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.width(28.dp),
                textAlign = TextAlign.Center
            )
            CoinLogo(coin.logoUrl, 40.dp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Row {
                    Text(coin.symbol, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                    Text(" / USD", color = PositiveColor, fontWeight = FontWeight.Bold)
                }
                Text(coin.name, style = MaterialTheme.typography.bodySmall)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    formatPrice(coin.priceUsd),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                coin.change24hPercent?.let {
                    Text(
                        formatPercent(it),
                        color = if (it >= 0) PositiveColor else NegativeColor,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}

/** Деталі валюти (⭐) */
@Composable
fun CoinDetails(coin: Coin, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CoinLogo(coin.logoUrl, 96.dp)
        Spacer(Modifier.height(12.dp))
        Row {
            Text(coin.symbol, color = MaterialTheme.colorScheme.primary, fontSize = 28.sp, fontWeight = FontWeight.Bold)
            Text(" / USD", color = PositiveColor, fontSize = 28.sp, fontWeight = FontWeight.Bold)
        }
        Text(coin.name, style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(16.dp))

        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            DetailRow("Місце в рейтингу", "#${coin.rank}")
            DetailRow("Ціна", formatPrice(coin.priceUsd))
            DetailRow(
                "Зміна за 24 год",
                formatPercent(coin.change24hPercent),
                valueColor = coin.change24hPercent?.let { if (it >= 0) PositiveColor else NegativeColor }
            )
            DetailRow("Мін. за 24 год", formatPrice(coin.low24hUsd), valueColor = NegativeColor)
            DetailRow("Макс. за 24 год", formatPrice(coin.high24hUsd), valueColor = PositiveColor)
            DetailRow("Капіталізація", formatLargeUsd(coin.marketCapUsd))
            DetailRow("Обсяг за 24 год", formatLargeUsd(coin.volume24hUsd))
            DetailRow("Оновлено", formatDateTime(coin.updatedAtMillis))
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String, valueColor: Color? = null) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge)
        Text(
            value,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold,
            color = valueColor ?: MaterialTheme.colorScheme.onSurface
        )
    }
    HorizontalDivider()
}

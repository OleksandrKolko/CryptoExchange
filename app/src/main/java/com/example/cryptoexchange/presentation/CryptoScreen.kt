package com.example.cryptoexchange.presentation

import android.content.res.Configuration
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.cryptoexchange.presentation.components.CoinCard
import com.example.cryptoexchange.presentation.components.CoinDetails
import com.example.cryptoexchange.presentation.components.formatDateTime
import com.example.cryptoexchange.worker.SyncState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CryptoScreen(viewModel: CryptoViewModel = viewModel(factory = CryptoViewModel.Factory)) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    // rememberLazyListState зберігає прокрутку при повороті екрана
    val listState = rememberLazyListState()
    val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
    val selected = state.selectedCoin

    // У портреті деталі відкриваються на весь екран; «Назад» повертає до списку
    BackHandler(enabled = !isLandscape && selected != null) { viewModel.onCoinSelected(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(if (!isLandscape && selected != null) selected.name else "Біржа криптовалют")
                },
                navigationIcon = {
                    if (!isLandscape && selected != null) {
                        TextButton(onClick = { viewModel.onCoinSelected(null) }) { Text("←") }
                    }
                }
            )
        }
    ) { padding ->
        Box(
            Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when {
                // Ландшафт: список і деталі поруч
                isLandscape -> Row(Modifier.fillMaxSize()) {
                    CoinListPane(state, listState, viewModel, Modifier.weight(1f))
                    VerticalDivider()
                    Box(
                        Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    ) {
                        if (selected != null) {
                            CoinDetails(selected, Modifier.verticalScroll(rememberScrollState()))
                        } else {
                            Text(
                                "Оберіть валюту зі списку",
                                Modifier.align(Alignment.Center)
                            )
                        }
                    }
                }

                selected != null -> CoinDetails(
                    selected,
                    Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                )

                else -> CoinListPane(state, listState, viewModel, Modifier.fillMaxSize())
            }
        }
    }
}

@Composable
private fun CoinListPane(
    state: CryptoUiState,
    listState: LazyListState,
    viewModel: CryptoViewModel,
    modifier: Modifier = Modifier
) {
    Column(modifier.padding(horizontal = 12.dp)) {
        SyncStatusBar(state)

        Row(verticalAlignment = Alignment.CenterVertically) {
            // ⭐ Пошук серед локально збережених валют
            OutlinedTextField(
                value = state.searchQuery,
                onValueChange = viewModel::onSearchQueryChanged,
                placeholder = { Text("Пошук: назва або символ") },
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.width(8.dp))
            Button(onClick = viewModel::refresh, enabled = !state.isRefreshing) {
                Text("Оновити")
            }
        }
        Spacer(Modifier.size(8.dp))

        val coins = state.coins
        Box(Modifier.fillMaxSize()) {
            when {
                coins == null -> CircularProgressIndicator(Modifier.align(Alignment.Center))

                !state.hasAnyData -> EmptyState(
                    text = if (state.isRefreshing) "Завантажуємо перші дані…"
                    else "Даних ще немає.\nНатисніть «Оновити», щоб завантажити топ-30 криптовалют.",
                    Modifier.align(Alignment.Center)
                )

                coins.isEmpty() -> EmptyState(
                    "Нічого не знайдено за запитом «${state.searchQuery}»",
                    Modifier.align(Alignment.Center)
                )

                else -> LazyColumn(
                    state = listState,
                    contentPadding = PaddingValues(bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Стабільний ID активу як key → правильна анімація та прокрутка
                    items(coins, key = { it.id }) { coin ->
                        CoinCard(
                            coin = coin,
                            isSelected = coin.id == state.selectedCoin?.id,
                            onClick = { viewModel.onCoinSelected(coin.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SyncStatusBar(state: CryptoUiState) {
    val (text, color) = when (val sync = state.syncState) {
        SyncState.WaitingForNetwork -> "⏳ Очікує мережі…" to Color.Gray
        SyncState.Running -> "🔄 Оновлення…" to Color(0xFF1565C0)
        is SyncState.Retrying -> "⚠️ Помилка мережі. Повторна спроба №${sync.attempt} очікує…" to Color(0xFFEF6C00)
        is SyncState.Failed ->
            (if (sync.isAuthError) "🔑 ${sync.message}" else "❌ ${sync.message}") to Color(0xFFC62828)
        is SyncState.Succeeded -> "✅ ${sync.message}" to Color(0xFF2E7D32)
        SyncState.Idle ->
            (if (state.periodicRunning) "🔄 Фонове оновлення…" else "") to Color(0xFF1565C0)
    }

    Column(Modifier.padding(vertical = 6.dp)) {
        Text(
            "Останнє оновлення: ${formatDateTime(state.lastUpdateMillis)}",
            style = MaterialTheme.typography.bodyMedium
        )
        if (text.isNotEmpty()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (state.isRefreshing) {
                    CircularProgressIndicator(Modifier.size(14.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.width(6.dp))
                }
                Text(text, color = color, style = MaterialTheme.typography.bodySmall)
            }
        }
        if (state.syncState is SyncState.Failed && state.hasAnyData) {
            Text(
                "Показано раніше збережені дані",
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray
            )
        }
    }
}

@Composable
private fun EmptyState(text: String, modifier: Modifier = Modifier) {
    Text(text, modifier = modifier.padding(24.dp), textAlign = TextAlign.Center)
}

/** Тема: світла / темна автоматично за системними налаштуваннями */
@Composable
fun CryptoTheme(content: @Composable () -> Unit) {
    val dark = androidx.compose.foundation.isSystemInDarkTheme()
    MaterialTheme(
        colorScheme = if (dark) androidx.compose.material3.darkColorScheme()
        else androidx.compose.material3.lightColorScheme()
    ) {
        Surface(Modifier.fillMaxSize(), content = content)
    }
}


package com.example.cryptoexchange.presentation

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.cryptoexchange.CryptoApp
import com.example.cryptoexchange.domain.model.Coin
import com.example.cryptoexchange.domain.usecase.ObserveCoinsUseCase
import com.example.cryptoexchange.domain.usecase.ObserveLastUpdateUseCase
import com.example.cryptoexchange.worker.SyncScheduler
import com.example.cryptoexchange.worker.SyncState
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn

/** Стан екрана */
data class CryptoUiState(
    val coins: List<Coin>? = null,            // null — ще читаємо Room
    val hasAnyData: Boolean = false,          // чи є в базі хоч щось (без урахування пошуку)
    val lastUpdateMillis: Long? = null,
    val syncState: SyncState = SyncState.Idle,
    val periodicRunning: Boolean = false,
    val searchQuery: String = "",
    val selectedCoin: Coin? = null
) {
    val isRefreshing: Boolean
        get() = periodicRunning || syncState is SyncState.Running ||
            syncState is SyncState.WaitingForNetwork || syncState is SyncState.Retrying
}

/**
 * ViewModel не виконує SQL чи HTTP: читає Room через use case,
 * а оновлення доручає WorkManager через SyncScheduler.
 */
class CryptoViewModel(
    observeCoins: ObserveCoinsUseCase,
    observeLastUpdate: ObserveLastUpdateUseCase,
    private val scheduler: SyncScheduler,
    private val savedState: SavedStateHandle
) : ViewModel() {

    // Пошук і обрана валюта — у SavedStateHandle (переживають поворот і смерть процесу)
    private val searchQuery = savedState.getStateFlow(KEY_QUERY, "")
    private val selectedId = savedState.getStateFlow<String?>(KEY_SELECTED, null)

    private val allCoins = observeCoins(flowOf(""))

    val uiState: StateFlow<CryptoUiState> = combine(
        observeCoins(searchQuery),
        allCoins,
        observeLastUpdate(),
        combine(scheduler.observeManualSync(), scheduler.observePeriodicRunning()) { s, p -> s to p },
        combine(searchQuery, selectedId) { q, id -> q to id }
    ) { filtered, all, lastUpdate, (sync, periodic), (query, id) ->
        CryptoUiState(
            coins = filtered,
            hasAnyData = all.isNotEmpty(),
            lastUpdateMillis = lastUpdate,
            syncState = sync,
            periodicRunning = periodic,
            searchQuery = query,
            selectedCoin = all.firstOrNull { it.id == id }   // ціна оновиться після синхронізації
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), CryptoUiState())

    /** Кнопка «Оновити»: лише ставить одноразову задачу WorkManager */
    fun refresh() = scheduler.syncNow()

    fun onSearchQueryChanged(query: String) {
        savedState[KEY_QUERY] = query
    }

    fun onCoinSelected(id: String?) {
        savedState[KEY_SELECTED] = id
    }

    // onCleared() НЕ скасовує WorkManager: фонова синхронізація має власний життєвий цикл

    companion object {
        private const val KEY_QUERY = "search_query"
        private const val KEY_SELECTED = "selected_coin_id"

        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as CryptoApp
                CryptoViewModel(
                    observeCoins = app.container.observeCoinsUseCase,
                    observeLastUpdate = app.container.observeLastUpdateUseCase,
                    scheduler = app.container.syncScheduler,
                    savedState = createSavedStateHandle()
                )
            }
        }
    }
}

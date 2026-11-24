package com.example.cryptoexchange.worker

import android.content.Context
import android.util.Log
import androidx.lifecycle.asFlow
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.concurrent.TimeUnit

/** Стан фонового оновлення для UI */
sealed interface SyncState {
    data object Idle : SyncState
    data object WaitingForNetwork : SyncState
    data object Running : SyncState
    data class Retrying(val attempt: Int) : SyncState
    data class Succeeded(val message: String) : SyncState
    data class Failed(val message: String, val isAuthError: Boolean) : SyncState
}

/**
 * Постановка задач WorkManager — окремо від composable та ViewModel.
 */
class SyncScheduler(context: Context) {

    private val workManager = WorkManager.getInstance(context.applicationContext)

    private val constraints = Constraints.Builder()
        .setRequiredNetworkType(NetworkType.CONNECTED)
        .build()

    /** Періодичне оновлення раз на годину (CoinGecko Demo: 10 000 запитів/міс); стале ім'я + KEEP */
    fun schedulePeriodic() {
        val request = PeriodicWorkRequestBuilder<CryptoSyncWorker>(SYNC_INTERVAL_HOURS, TimeUnit.HOURS)
            .setConstraints(constraints)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .addTag(TAG_SYNC)
            .build()

        workManager.enqueueUniquePeriodicWork(PERIODIC_WORK, ExistingPeriodicWorkPolicy.KEEP, request)
        Log.d(CryptoSyncWorker.TAG, "Періодичне оновлення зареєстровано: $PERIODIC_WORK (KEEP)")
    }

    /** Ручне оновлення; KEEP не дублює незавершену задачу */
    fun syncNow() {
        val request = OneTimeWorkRequestBuilder<CryptoSyncWorker>()
            .setConstraints(constraints)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .addTag(TAG_SYNC)
            .build()

        workManager.enqueueUniqueWork(MANUAL_WORK, ExistingWorkPolicy.KEEP, request)
        Log.d(CryptoSyncWorker.TAG, "Ручне оновлення поставлено: $MANUAL_WORK (KEEP)")
    }

    /** Стан ручного оновлення */
    fun observeManualSync(): Flow<SyncState> =
        workManager.getWorkInfosForUniqueWorkLiveData(MANUAL_WORK).asFlow()
            .map { infos -> infos.firstOrNull().toSyncState() }

    /** Чи виконується зараз періодичне оновлення */
    fun observePeriodicRunning(): Flow<Boolean> =
        workManager.getWorkInfosForUniqueWorkLiveData(PERIODIC_WORK).asFlow()
            .map { infos -> infos.any { it.state == WorkInfo.State.RUNNING } }

    private fun WorkInfo?.toSyncState(): SyncState = when (this?.state) {
        null -> SyncState.Idle
        WorkInfo.State.ENQUEUED, WorkInfo.State.BLOCKED ->
            if (runAttemptCount > 0) SyncState.Retrying(runAttemptCount) else SyncState.WaitingForNetwork
        WorkInfo.State.RUNNING -> SyncState.Running
        WorkInfo.State.SUCCEEDED ->
            SyncState.Succeeded(outputData.getString(CryptoSyncWorker.KEY_MESSAGE) ?: "Оновлено")
        WorkInfo.State.FAILED -> SyncState.Failed(
            message = outputData.getString(CryptoSyncWorker.KEY_MESSAGE) ?: "Помилка оновлення",
            isAuthError = outputData.getBoolean(CryptoSyncWorker.KEY_AUTH, false)
        )
        WorkInfo.State.CANCELLED -> SyncState.Failed("Оновлення скасовано", false)
    }

    companion object {
        const val PERIODIC_WORK = "crypto_sync_periodic"
        const val MANUAL_WORK = "crypto_sync_manual"
        const val TAG_SYNC = "crypto_sync"
        const val SYNC_INTERVAL_HOURS = 1L
    }
}

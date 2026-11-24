package com.example.cryptoexchange.worker

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.example.cryptoexchange.CryptoApp
import com.example.cryptoexchange.domain.model.SyncResult

/**
 * Інфраструктурний компонент: лише викликає сценарій синхронізації
 * і перетворює SyncResult на Result WorkManager.
 * Не має посилань на Activity / ViewModel.
 */
class CryptoSyncWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val container = (applicationContext as CryptoApp).container
        Log.d(TAG, "doWork: старт, спроба №${runAttemptCount + 1}")

        // CoroutineWorker чекає завершення запиту ТА запису в Room
        return when (val result = container.syncCoinsUseCase()) {
            is SyncResult.Success ->
                Result.success(workDataOf(KEY_MESSAGE to "Оновлено ${result.count} валют"))

            SyncResult.NetworkError -> retryOrFail("Немає з'єднання з сервером")

            is SyncResult.ServerError -> retryOrFail("Сервер тимчасово недоступний (HTTP ${result.code})")

            // Ліміт API: повторюємо пізніше (backoff), але не нескінченно
            is SyncResult.RateLimited -> retryOrFail("Перевищено ліміт запитів API")

            // Помилки ключа / прав — НЕ повторюємо
            SyncResult.Unauthorized ->
                Result.failure(workDataOf(KEY_MESSAGE to "Невірний API-ключ або немає доступу", KEY_AUTH to true))

            is SyncResult.InvalidResponse ->
                Result.failure(workDataOf(KEY_MESSAGE to "Некоректна відповідь: ${result.reason}"))
        }.also { Log.d(TAG, "doWork: результат = $it") }
    }

    private fun retryOrFail(message: String): Result =
        if (runAttemptCount < MAX_ATTEMPTS) {
            Log.w(TAG, "$message → retry з backoff")
            Result.retry()
        } else {
            Result.failure(workDataOf(KEY_MESSAGE to message))
        }

    companion object {
        const val TAG = "CryptoWorker"
        const val KEY_MESSAGE = "message"
        const val KEY_AUTH = "auth_error"
        private const val MAX_ATTEMPTS = 5
    }
}

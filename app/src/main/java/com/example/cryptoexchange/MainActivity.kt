package com.example.cryptoexchange

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.cryptoexchange.presentation.CryptoScreen
import com.example.cryptoexchange.presentation.CryptoTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // Запит у мережу тут НЕ запускаємо: нова Activity після повороту
        // лише підписується на стан тієї самої ViewModel
        setContent {
            CryptoTheme { CryptoScreen() }
        }
    }
}

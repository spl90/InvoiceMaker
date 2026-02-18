package com.example.invoicegen

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.invoicegen.ui.navigation.AppNavigation
import com.example.invoicegen.ui.theme.InvoiceMakerTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            InvoiceMakerTheme {
                AppNavigation()
            }
        }
    }
}

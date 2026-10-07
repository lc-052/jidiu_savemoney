package com.jidiu.companion

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge

class MainActivity : ComponentActivity() {
    private lateinit var store: CompanionStore
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        store = CompanionStore(applicationContext)
        setContent { CompanionApp(store) }
    }
    override fun onDestroy() {
        super.onDestroy()
        store.close()
    }
}

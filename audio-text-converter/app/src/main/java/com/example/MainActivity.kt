package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import com.example.data.AppDatabase
import com.example.data.TranscriptRepository
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.RecordScreen
import com.example.ui.screens.SyncSettingsScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.AudioScribeViewModel
import com.example.ui.viewmodel.AudioScribeViewModelFactory
import com.example.ui.viewmodel.Screen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val database = AppDatabase.getDatabase(applicationContext)
        val repository = TranscriptRepository(database.transcriptDao())
        val factory = AudioScribeViewModelFactory(application, repository)
        val viewModel = ViewModelProvider(this, factory)[AudioScribeViewModel::class.java]

        setContent {
            MyApplicationTheme {
                MainAppContent(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun MainAppContent(viewModel: AudioScribeViewModel) {
    val currentScreen by viewModel.currentScreen.collectAsState()

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp
            ) {
                NavigationBarItem(
                    selected = currentScreen == Screen.Home,
                    onClick = { viewModel.setScreen(Screen.Home) },
                    icon = { Icon(Icons.Default.Description, contentDescription = "My Notes Tab") },
                    label = { Text("Notes") }
                )
                NavigationBarItem(
                    selected = currentScreen == Screen.Record,
                    onClick = { viewModel.setScreen(Screen.Record) },
                    icon = { Icon(Icons.Default.Mic, contentDescription = "Record Voice Tab") },
                    label = { Text("Record") }
                )
                NavigationBarItem(
                    selected = currentScreen == Screen.Settings,
                    onClick = { viewModel.setScreen(Screen.Settings) },
                    icon = { Icon(Icons.Default.Sync, contentDescription = "Cloud Sync Tab") },
                    label = { Text("Cloud Sync") }
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentScreen) {
                Screen.Home -> HomeScreen(
                    viewModel = viewModel,
                    onNavigateToRecord = { viewModel.setScreen(Screen.Record) }
                )
                Screen.Record -> RecordScreen(viewModel = viewModel)
                Screen.Settings -> SyncSettingsScreen(viewModel = viewModel)
            }
        }
    }
}

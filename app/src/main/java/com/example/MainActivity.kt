package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.screens.GameScreen
import com.example.ui.screens.LevelSelectScreen
import com.example.ui.screens.ShopScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.GameViewModel

enum class AppScreen {
    GAME,
    LEVEL_SELECT,
    SHOP
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                val viewModel: GameViewModel = viewModel()
                var currentScreen by remember { mutableStateOf(AppScreen.GAME) }

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color(0xFF121212)
                ) {
                    when (currentScreen) {
                        AppScreen.GAME -> {
                            GameScreen(
                                viewModel = viewModel,
                                onNavigateToMap = { currentScreen = AppScreen.LEVEL_SELECT },
                                onNavigateToShop = { currentScreen = AppScreen.SHOP }
                            )
                        }
                        AppScreen.LEVEL_SELECT -> {
                            BackHandler {
                                currentScreen = AppScreen.GAME
                            }
                            LevelSelectScreen(
                                viewModel = viewModel,
                                onSelectLevel = { level ->
                                    viewModel.loadLevel(level)
                                    currentScreen = AppScreen.GAME
                                },
                                onNavigateToShop = { currentScreen = AppScreen.SHOP },
                                onBack = { currentScreen = AppScreen.GAME }
                            )
                        }
                        AppScreen.SHOP -> {
                            BackHandler {
                                currentScreen = AppScreen.GAME
                            }
                            ShopScreen(
                                viewModel = viewModel,
                                onBack = { currentScreen = AppScreen.GAME }
                            )
                        }
                    }
                }
            }
        }
    }
}

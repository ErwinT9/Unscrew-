package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.model.BiomeTheme
import com.example.model.LevelData
import com.example.ui.components.BoardCanvas
import com.example.ui.components.ScrewBoxRow
import com.example.ui.components.ToolBarView
import com.example.viewmodel.GameViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameScreen(
    viewModel: GameViewModel,
    onNavigateToMap: () -> Unit,
    onNavigateToShop: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val levelData = uiState.levelData ?: return

    val biome = levelData.biome
    val bgGradient = Brush.verticalGradient(
        colors = listOf(
            Color(biome.bgGradientStart),
            Color(biome.bgGradientEnd)
        )
    )

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .background(bgGradient),
        containerColor = Color.Transparent,
        contentWindowInsets = WindowInsets.safeDrawing
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // 1. Top Header Bar
                GameTopHeader(
                    currentLevel = uiState.currentLevel,
                    levelTitle = levelData.title,
                    biome = biome,
                    timeRemaining = uiState.timeRemaining,
                    isFrozen = uiState.isTimerFrozen,
                    freezeSeconds = uiState.freezeSecondsLeft,
                    totalCoins = uiState.totalCoins,
                    onPauseClick = { viewModel.pauseGame() },
                    onRestartClick = { viewModel.restartCurrentLevel() },
                    onMapClick = onNavigateToMap,
                    onShopClick = onNavigateToShop
                )

                Spacer(modifier = Modifier.height(4.dp))

                // 2. Active Screw Boxes Shelf
                ScrewBoxRow(
                    activeBoxes = uiState.activeBoxes,
                    queuedBoxesCount = uiState.queuedBoxes.size
                )

                // 3. Main Puzzle Board Canvas
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    BoardCanvas(
                        levelData = levelData,
                        planks = uiState.planks,
                        holes = uiState.holes,
                        screws = uiState.screws,
                        activeFlights = uiState.activeFlights,
                        onScrewTapped = { viewModel.onScrewTapped(it) }
                    )

                    // Floating Combo Banner
                    androidx.compose.animation.AnimatedVisibility(
                        visible = uiState.comboMessage != null,
                        enter = fadeIn() + scaleIn(),
                        exit = fadeOut() + scaleOut(),
                        modifier = Modifier.align(Alignment.Center)
                    ) {
                        uiState.comboMessage?.let { msg ->
                            Box(
                                modifier = Modifier
                                    .shadow(8.dp, RoundedCornerShape(20.dp))
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(
                                        Brush.horizontalGradient(
                                            listOf(Color(0xFFFF6F00), Color(0xFFFFD54F))
                                        )
                                    )
                                    .padding(horizontal = 20.dp, vertical = 10.dp)
                            ) {
                                Text(
                                    text = msg,
                                    color = Color.Black,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Black
                                )
                            }
                        }
                    }
                }

                // 4. Message Banner Toast (if active)
                androidx.compose.animation.AnimatedVisibility(
                    visible = uiState.messageBanner != null,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    uiState.messageBanner?.let { banner ->
                        Text(
                            text = banner,
                            color = Color(0xFFFFD54F),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp)
                        )
                    }
                }

                // 5. Bottom Power-Up Tools Bar
                ToolBarView(
                    currentLevel = uiState.currentLevel,
                    toolInventory = uiState.toolInventory,
                    onUseTool = { viewModel.activateTool(it) }
                )
            }

            // Dialogs
            if (uiState.isVictory) {
                VictoryDialog(
                    level = uiState.currentLevel,
                    stars = uiState.starsEarned,
                    coins = uiState.coinsEarned,
                    timeRemaining = uiState.timeRemaining,
                    onNextLevel = { viewModel.nextLevel() },
                    onReplay = { viewModel.restartCurrentLevel() },
                    onMap = onNavigateToMap
                )
            }

            if (uiState.isGameOver) {
                GameOverDialog(
                    level = uiState.currentLevel,
                    coins = uiState.totalCoins,
                    onRevive = {
                        if (viewModel.prefs.spendCoins(30)) {
                            viewModel.showBanner("Revived with +30s!")
                            viewModel.loadLevel(uiState.currentLevel)
                        } else {
                            viewModel.showBanner("Not enough coins to revive!")
                        }
                    },
                    onRestart = { viewModel.restartCurrentLevel() },
                    onMap = onNavigateToMap
                )
            }

            if (uiState.isPaused) {
                PauseDialog(
                    onResume = { viewModel.resumeGame() },
                    onRestart = { viewModel.restartCurrentLevel() },
                    onMap = onNavigateToMap,
                    isSoundOn = viewModel.prefs.isSoundEnabled(),
                    isHapticsOn = viewModel.prefs.isHapticsEnabled(),
                    onToggleSound = { viewModel.toggleSound() },
                    onToggleHaptics = { viewModel.toggleHaptics() }
                )
            }
        }
    }
}

@Composable
fun GameTopHeader(
    currentLevel: Int,
    levelTitle: String,
    biome: BiomeTheme,
    timeRemaining: Int,
    isFrozen: Boolean,
    freezeSeconds: Int,
    totalCoins: Int,
    onPauseClick: () -> Unit,
    onRestartClick: () -> Unit,
    onMapClick: () -> Unit,
    onShopClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color.Black.copy(alpha = 0.4f))
            .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(16.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Level & Biome info
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onMapClick,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Map,
                        contentDescription = "Map",
                        tint = Color(0xFFFFD54F)
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = biome.iconName,
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Lv $currentLevel",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                    Text(
                        text = levelTitle,
                        color = Color(0xFFD7CCC8),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Controls & Coins
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Coins Pill
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF2E7D32).copy(alpha = 0.7f))
                        .border(1.dp, Color(0xFFFFD54F), RoundedCornerShape(12.dp))
                        .clickable { onShopClick() }
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = "🪙", fontSize = 12.sp)
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = "$totalCoins",
                            color = Color(0xFFFFD54F),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                IconButton(
                    onClick = onRestartClick,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Refresh,
                        contentDescription = "Restart",
                        tint = Color.White
                    )
                }

                IconButton(
                    onClick = onPauseClick,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Pause,
                        contentDescription = "Pause",
                        tint = Color.White
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Timer Bar
        val timerColor = when {
            isFrozen -> Color(0xFF00E5FF)
            timeRemaining <= 15 -> Color(0xFFFF1744)
            timeRemaining <= 30 -> Color(0xFFFF9100)
            else -> Color(0xFF00E676)
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (isFrozen) Icons.Default.AcUnit else Icons.Default.Timer,
                contentDescription = "Timer",
                tint = timerColor,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))

            // Time Bar
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color.White.copy(alpha = 0.15f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth((timeRemaining / 100f).coerceIn(0f, 1f))
                        .clip(RoundedCornerShape(4.dp))
                        .background(timerColor)
                )
            }

            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = if (isFrozen) "FROZEN (${freezeSeconds}s)" else "${timeRemaining}s",
                color = timerColor,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun VictoryDialog(
    level: Int,
    stars: Int,
    coins: Int,
    timeRemaining: Int,
    onNextLevel: () -> Unit,
    onReplay: () -> Unit,
    onMap: () -> Unit
) {
    Dialog(onDismissRequest = {}) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E130D)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .border(2.dp, Color(0xFFFFD54F), RoundedCornerShape(24.dp))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "🎉 LEVEL CLEARED! 🎉",
                    color = Color(0xFFFFD54F),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Stars Row
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    for (i in 1..3) {
                        Text(
                            text = if (i <= stars) "⭐" else "☆",
                            fontSize = 32.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Stats breakdown
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.Black.copy(alpha = 0.4f))
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "Remaining Time:", color = Color.White, fontSize = 13.sp)
                        Text(text = "${timeRemaining}s", color = Color(0xFF00E676), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "Coins Awarded:", color = Color.White, fontSize = 13.sp)
                        Text(text = "+$coins 🪙", color = Color(0xFFFFD54F), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Action Buttons
                Button(
                    onClick = onNextLevel,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFB300)),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text(text = if (level < 50) "Next Level ➔" else "Victory Complete! 🏆", color = Color.Black, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onReplay,
                        modifier = Modifier
                            .weight(1f)
                            .height(42.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Replay", color = Color.White)
                    }
                    OutlinedButton(
                        onClick = onMap,
                        modifier = Modifier
                            .weight(1f)
                            .height(42.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Level Map", color = Color(0xFFFFD54F))
                    }
                }
            }
        }
    }
}

@Composable
fun GameOverDialog(
    level: Int,
    coins: Int,
    onRevive: () -> Unit,
    onRestart: () -> Unit,
    onMap: () -> Unit
) {
    Dialog(onDismissRequest = {}) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF261010)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .border(2.dp, Color(0xFFE53935), RoundedCornerShape(24.dp))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "⏰ TIME'S UP!",
                    color = Color(0xFFFF5252),
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Black
                )

                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "The puzzle locked down. Don't give up!",
                    color = Color(0xFFD7CCC8),
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(18.dp))

                Button(
                    onClick = onRevive,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF43A047)),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text("Revive +30s (30 🪙)", color = Color.White, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = onRestart,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE53935)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Restart Level", color = Color.White, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(6.dp))

                TextButton(onClick = onMap) {
                    Text("Return to Level Map", color = Color(0xFFFFD54F))
                }
            }
        }
    }
}

@Composable
fun PauseDialog(
    onResume: () -> Unit,
    onRestart: () -> Unit,
    onMap: () -> Unit,
    isSoundOn: Boolean,
    isHapticsOn: Boolean,
    onToggleSound: () -> Unit,
    onToggleHaptics: () -> Unit
) {
    var soundState by remember { mutableStateOf(isSoundOn) }
    var hapticsState by remember { mutableStateOf(isHapticsOn) }

    Dialog(onDismissRequest = onResume) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1B1B1B)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .border(2.dp, Color(0xFFFFD54F).copy(alpha = 0.5f), RoundedCornerShape(24.dp))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "⏸️ PAUSED",
                    color = Color.White,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Black
                )

                Spacer(modifier = Modifier.height(18.dp))

                // Sound & Haptics Toggles
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.Black.copy(alpha = 0.4f))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "Sound FX", color = Color.White, fontSize = 14.sp)
                    Switch(
                        checked = soundState,
                        onCheckedChange = {
                            soundState = !soundState
                            onToggleSound()
                        }
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.Black.copy(alpha = 0.4f))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "Haptics Vibration", color = Color.White, fontSize = 14.sp)
                    Switch(
                        checked = hapticsState,
                        onCheckedChange = {
                            hapticsState = !hapticsState
                            onToggleHaptics()
                        }
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = onResume,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFB300)),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text("Resume Game", color = Color.Black, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedButton(
                    onClick = onRestart,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Restart Level", color = Color.White)
                }

                Spacer(modifier = Modifier.height(8.dp))

                TextButton(onClick = onMap) {
                    Text("Exit to Level Map", color = Color(0xFFFFD54F))
                }
            }
        }
    }
}

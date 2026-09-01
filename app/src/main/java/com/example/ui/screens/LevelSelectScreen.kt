package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.model.BiomeTheme
import com.example.viewmodel.GameViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LevelSelectScreen(
    viewModel: GameViewModel,
    onSelectLevel: (Int) -> Unit,
    onNavigateToShop: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val unlockedLevel = uiState.unlockedLevel
    val totalStars = uiState.totalStars
    val totalCoins = uiState.totalCoins

    val biomes = BiomeTheme.values().toList()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Level Journey (100 Levels)",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFFFFD54F)
                        )
                        Text(
                            text = "10 Mechanical Biomes • 10 Levels Each",
                            fontSize = 11.sp,
                            color = Color(0xFFD7CCC8)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                actions = {
                    // Stars badge
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .padding(end = 6.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.Black.copy(alpha = 0.5f))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(text = "⭐", fontSize = 13.sp)
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = "$totalStars/300",
                            color = Color(0xFFFFD54F),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Shop Button
                    Button(
                        onClick = onNavigateToShop,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(text = "🪙 $totalCoins", color = Color(0xFFFFD54F), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF140D09)
                )
            )
        },
        containerColor = Color(0xFF100A07)
    ) { innerPadding ->
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 14.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            biomes.forEachIndexed { biomeIdx, biome ->
                val startLevel = biomeIdx * 10 + 1
                val endLevel = startLevel + 9

                item {
                    BiomeChapterCard(
                        biome = biome,
                        chapterIndex = biomeIdx + 1,
                        startLevel = startLevel,
                        endLevel = endLevel,
                        unlockedLevel = unlockedLevel,
                        getStars = { viewModel.prefs.getStarsForLevel(it) },
                        onSelectLevel = onSelectLevel
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
fun BiomeChapterCard(
    biome: BiomeTheme,
    chapterIndex: Int,
    startLevel: Int,
    endLevel: Int,
    unlockedLevel: Int,
    getStars: (Int) -> Int,
    onSelectLevel: (Int) -> Unit
) {
    val isBiomeUnlocked = unlockedLevel >= startLevel

    val bgBrush = Brush.verticalGradient(
        colors = listOf(
            Color(biome.bgGradientStart),
            Color(biome.boardBgColor)
        )
    )

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        modifier = Modifier
            .fillMaxWidth()
            .shadow(6.dp, RoundedCornerShape(20.dp))
            .clip(RoundedCornerShape(20.dp))
            .background(bgBrush)
            .border(
                width = 1.5.dp,
                color = if (isBiomeUnlocked) Color(biome.accentColor).copy(alpha = 0.6f) else Color.White.copy(alpha = 0.15f),
                shape = RoundedCornerShape(20.dp)
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            // Biome Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = biome.iconName, fontSize = 22.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "World $chapterIndex: ${biome.title}",
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = biome.subtitle,
                            color = Color(biome.accentColor),
                            fontSize = 11.sp
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color.Black.copy(alpha = 0.4f)
                ) {
                    Text(
                        text = "Lv $startLevel - $endLevel",
                        color = Color.White.copy(alpha = 0.85f),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // First Row of 5 Levels
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                for (lvl in startLevel until startLevel + 5) {
                    val isLvlUnlocked = unlockedLevel >= lvl
                    val stars = getStars(lvl)

                    LevelNodeButton(
                        levelNumber = lvl,
                        isUnlocked = isLvlUnlocked,
                        stars = stars,
                        accentColor = Color(biome.accentColor),
                        onClick = {
                            if (isLvlUnlocked) {
                                onSelectLevel(lvl)
                            }
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Second Row of 5 Levels
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                for (lvl in (startLevel + 5)..endLevel) {
                    val isLvlUnlocked = unlockedLevel >= lvl
                    val stars = getStars(lvl)

                    LevelNodeButton(
                        levelNumber = lvl,
                        isUnlocked = isLvlUnlocked,
                        stars = stars,
                        accentColor = Color(biome.accentColor),
                        onClick = {
                            if (isLvlUnlocked) {
                                onSelectLevel(lvl)
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun LevelNodeButton(
    levelNumber: Int,
    isUnlocked: Boolean,
    stars: Int,
    accentColor: Color,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(enabled = isUnlocked) { onClick() }
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .shadow(if (isUnlocked) 4.dp else 1.dp, CircleShape)
                .clip(CircleShape)
                .background(
                    if (isUnlocked) {
                        Brush.radialGradient(
                            listOf(accentColor, Color(0xFF212121))
                        )
                    } else {
                        Brush.radialGradient(
                            listOf(Color(0xFF2C2C2C), Color(0xFF141414))
                        )
                    }
                )
                .border(
                    width = 1.5.dp,
                    color = if (isUnlocked) Color.White.copy(alpha = 0.8f) else Color.White.copy(alpha = 0.15f),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            if (isUnlocked) {
                Text(
                    text = "$levelNumber",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Black
                )
            } else {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = "Locked",
                    tint = Color.White.copy(alpha = 0.4f),
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(2.dp))

        // Stars mini row
        if (isUnlocked) {
            Row(horizontalArrangement = Arrangement.spacedBy(1.dp)) {
                for (s in 1..3) {
                    Text(
                        text = if (s <= stars) "⭐" else "·",
                        fontSize = 8.sp
                    )
                }
            }
        } else {
            Text(text = "·", fontSize = 8.sp, color = Color.White.copy(alpha = 0.3f))
        }
    }
}

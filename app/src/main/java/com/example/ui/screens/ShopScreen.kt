package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.ShoppingCart
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
import com.example.model.CoinPack
import com.example.model.StoreBundle
import com.example.model.ToolType
import com.example.viewmodel.GameViewModel

enum class ShopTab(val title: String, val icon: @Composable () -> Unit) {
    BUNDLES("Bundles", { Icon(Icons.Default.LocalOffer, contentDescription = null, modifier = Modifier.size(18.dp)) }),
    COINS("Coins", { Icon(Icons.Default.MonetizationOn, contentDescription = null, modifier = Modifier.size(18.dp)) }),
    TOOLS("Armory", { Icon(Icons.Default.Build, contentDescription = null, modifier = Modifier.size(18.dp)) })
}

data class IapNoticeInfo(
    val title: String,
    val price: String,
    val description: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShopScreen(
    viewModel: GameViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val coins = uiState.totalCoins
    val currentLevel = uiState.unlockedLevel
    var selectedTab by remember { mutableStateOf(ShopTab.BUNDLES) }
    val canClaim = viewModel.canClaimDailyReward()

    // State for Amazon IAP Notice Dialog
    var pendingIapNotice by remember { mutableStateOf<IapNoticeInfo?>(null) }

    val toolPrices: Map<ToolType, Int> = mapOf(
        ToolType.UNDO to ToolType.UNDO.coinCost,
        ToolType.FREEZE_TIME to ToolType.FREEZE_TIME.coinCost,
        ToolType.MAGNET to ToolType.MAGNET.coinCost,
        ToolType.DRILL to ToolType.DRILL.coinCost,
        ToolType.AUTO_SCREW to ToolType.AUTO_SCREW.coinCost,
        ToolType.HAMMER to ToolType.HAMMER.coinCost
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ShoppingCart,
                            contentDescription = "Tool Workshop",
                            tint = Color(0xFFFFD54F)
                        )
                        Text(
                            text = "Workshop & Shop",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFFD54F)
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
                    Surface(
                        shape = CircleShape,
                        color = Color(0xFF2C1E14),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFFD54F)),
                        modifier = Modifier.padding(end = 12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(text = "🪙 $coins", color = Color(0xFFFFD54F), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF141414)
                )
            )
        },
        containerColor = Color(0xFF121212),
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Daily Workshop Supply Banner (Only 1 claim per day!)
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (canClaim) Color(0xFF2E1C0A) else Color(0xFF1C1C1C)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        1.5.dp,
                        if (canClaim) Color(0xFFFFB300) else Color.White.copy(alpha = 0.15f),
                        RoundedCornerShape(16.dp)
                    )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CardGiftcard,
                            contentDescription = "Daily Reward",
                            tint = if (canClaim) Color(0xFFFFD54F) else Color(0xFF888888),
                            modifier = Modifier.size(30.dp)
                        )
                        Column {
                            Text(
                                text = "Daily Workshop Supply",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                            Text(
                                text = if (canClaim) "Claim +150 🪙 & +1 Auto Screwdriver" else "Claimed today! Resets tomorrow",
                                color = if (canClaim) Color(0xFFBCAAA4) else Color(0xFF888888),
                                fontSize = 11.sp
                            )
                        }
                    }

                    Button(
                        onClick = { viewModel.claimDailyReward() },
                        enabled = canClaim,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFFFB300),
                            disabledContainerColor = Color(0xFF2B2B2B)
                        ),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = if (canClaim) "Claim" else "Claimed ✓",
                            color = if (canClaim) Color.Black else Color(0xFF888888),
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                }
            }

            // Tab Navigation Bar
            PrimaryTabRow(
                selectedTabIndex = selectedTab.ordinal,
                containerColor = Color(0xFF1E1E1E),
                contentColor = Color(0xFFFFD54F),
                modifier = Modifier.clip(RoundedCornerShape(12.dp))
            ) {
                ShopTab.values().forEach { tab ->
                    Tab(
                        selected = selectedTab == tab,
                        onClick = { selectedTab = tab },
                        text = {
                            Text(
                                text = tab.title,
                                fontWeight = if (selectedTab == tab) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 13.sp
                            )
                        },
                        icon = tab.icon
                    )
                }
            }

            // Tab Content
            when (selectedTab) {
                ShopTab.BUNDLES -> {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        item {
                            AmazonIapHeaderBanner(title = "Store Bundles require payment via Amazon In-App Purchasing.")
                        }

                        items(StoreBundle.DEFAULT_BUNDLES) { bundle ->
                            StoreBundleCard(
                                bundle = bundle,
                                onBuy = {
                                    pendingIapNotice = IapNoticeInfo(
                                        title = bundle.title,
                                        price = bundle.priceUsd,
                                        description = "${bundle.coins} Coins + ${bundle.autoScrewdrivers} Auto-Screwdrivers + ${bundle.plankBreakers} Plank Breakers + ${bundle.drillHoles} Extra Drills"
                                    )
                                }
                            )
                        }
                    }
                }
                ShopTab.COINS -> {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        item {
                            AmazonIapHeaderBanner(title = "Coin Packs require payment via Amazon In-App Purchasing.")
                        }

                        items(CoinPack.DEFAULT_PACKS) { pack ->
                            CoinPackCard(
                                pack = pack,
                                onBuy = {
                                    pendingIapNotice = IapNoticeInfo(
                                        title = pack.title,
                                        price = pack.priceUsd,
                                        description = "${pack.coins + pack.bonusCoins} Gold Coins"
                                    )
                                }
                            )
                        }
                    }
                }
                ShopTab.TOOLS -> {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 145.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(ToolType.values()) { tool ->
                            val isUnlocked = currentLevel >= tool.unlockLevel
                            val price = toolPrices[tool] ?: 50
                            val ownedCount = uiState.toolInventory[tool] ?: 0

                            ShopToolCard(
                                tool = tool,
                                isUnlocked = isUnlocked,
                                price = price,
                                ownedCount = ownedCount,
                                onBuy = { viewModel.purchaseTool(tool, price) }
                            )
                        }
                    }
                }
            }
        }
    }

    // Amazon IAP Notice Dialog
    pendingIapNotice?.let { notice ->
        Dialog(onDismissRequest = { pendingIapNotice = null }) {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .border(1.5.dp, Color(0xFFFF9800), RoundedCornerShape(20.dp))
                    .shadow(10.dp, RoundedCornerShape(20.dp))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .clip(CircleShape)
                            .background(Brush.radialGradient(listOf(Color(0xFFFFB74D), Color(0xFFE65100)))),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.ShoppingCart,
                            contentDescription = "Amazon IAP",
                            tint = Color.Black,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    Text(
                        text = "Amazon IAP Coming Soon",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFF2B2014),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFFB74D).copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "${notice.title} (${notice.price})",
                                color = Color(0xFFFFD54F),
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Text(
                                text = notice.description,
                                color = Color(0xFFBCAAA4),
                                fontSize = 11.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    Text(
                        text = "This item requires payment via Amazon In-App Purchasing (Amazon IAP). Real-money purchases are currently locked until the Amazon IAP billing system is fully connected.",
                        fontSize = 12.sp,
                        color = Color(0xFFD7CCC8),
                        textAlign = TextAlign.Center,
                        lineHeight = 16.sp
                    )

                    Button(
                        onClick = { pendingIapNotice = null },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF9800)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                    ) {
                        Text(
                            text = "Understood",
                            color = Color.Black,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AmazonIapHeaderBanner(title: String) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = Color(0xFF1F1A14),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFF9800).copy(alpha = 0.3f)),
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = "Notice",
                tint = Color(0xFFFFB74D),
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = "$title Amazon IAP gateway integration in progress.",
                color = Color(0xFFFFCC80),
                fontSize = 11.sp,
                lineHeight = 14.sp
            )
        }
    }
}

@Composable
fun StoreBundleCard(
    bundle: StoreBundle,
    onBuy: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = if (bundle.isPopular) 1.5.dp else 1.dp,
                color = if (bundle.isPopular) Color(0xFFFFD54F) else Color.White.copy(alpha = 0.12f),
                shape = RoundedCornerShape(16.dp)
            )
            .shadow(if (bundle.isPopular) 6.dp else 2.dp, RoundedCornerShape(16.dp))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(text = bundle.badge, fontSize = 24.sp)
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = bundle.title,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                            if (bundle.isPopular) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = Color(0xFFFF6F00)
                                ) {
                                    Text(
                                        text = "POPULAR",
                                        color = Color.White,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Black,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                        Text(
                            text = bundle.description,
                            color = Color(0xFFB0BEC5),
                            fontSize = 11.sp
                        )
                    }
                }

                Button(
                    onClick = onBuy,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (bundle.isPopular) Color(0xFFFFB300) else Color(0xFFE65100)
                    ),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = bundle.priceUsd,
                        color = if (bundle.isPopular) Color.Black else Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
            }

            // Bundle item chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                BundleChip(label = "${bundle.coins} 🪙", bg = Color(0xFF2C220E), textColor = Color(0xFFFFD54F))
                BundleChip(label = "${bundle.autoScrewdrivers}x 🪛 Screwdrivers", bg = Color(0xFF102838), textColor = Color(0xFF81D4FA))
                BundleChip(label = "${bundle.plankBreakers}x 🔨 Breakers", bg = Color(0xFF381515), textColor = Color(0xFFFF8A80))
                BundleChip(label = "${bundle.drillHoles}x 🔩 Drills", bg = Color(0xFF1C2D1C), textColor = Color(0xFFA5D6A7))
            }
        }
    }
}

@Composable
fun BundleChip(label: String, bg: Color, textColor: Color) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = bg,
        border = androidx.compose.foundation.BorderStroke(0.8.dp, textColor.copy(alpha = 0.4f))
    ) {
        Text(
            text = label,
            color = textColor,
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
        )
    }
}

@Composable
fun CoinPackCard(
    pack: CoinPack,
    onBuy: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A1A)),
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = if (pack.isBestValue) 1.5.dp else 1.dp,
                color = if (pack.isBestValue) Color(0xFFFFD54F) else Color.White.copy(alpha = 0.1f),
                shape = RoundedCornerShape(14.dp)
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                listOf(Color(0xFFFFD54F), Color(0xFFFF8F00))
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "🪙", fontSize = 20.sp)
                }

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = pack.title,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                        if (pack.bonusLabel != null) {
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = Color(0xFF00C853)
                            ) {
                                Text(
                                    text = pack.bonusLabel,
                                    color = Color.White,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                )
                            }
                        }
                    }
                    Text(
                        text = "+${pack.coins} Gold Coins",
                        color = Color(0xFFFFD54F),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Button(
                onClick = onBuy,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (pack.isBestValue) Color(0xFFFFB300) else Color(0xFF2E7D32)
                ),
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
            ) {
                Text(
                    text = pack.priceUsd,
                    color = if (pack.isBestValue) Color.Black else Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            }
        }
    }
}

@Composable
fun ShopToolCard(
    tool: ToolType,
    isUnlocked: Boolean,
    price: Int,
    ownedCount: Int,
    onBuy: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isUnlocked) Color(0xFF1E1E1E) else Color(0xFF161616)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = if (isUnlocked) Color(0xFFFFD54F).copy(alpha = 0.35f) else Color.White.copy(alpha = 0.08f),
                shape = RoundedCornerShape(14.dp)
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Icon Badge
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(
                        if (isUnlocked) Brush.radialGradient(listOf(Color(0xFF37474F), Color(0xFF212121)))
                        else Brush.radialGradient(listOf(Color(0xFF262626), Color(0xFF141414)))
                    )
                    .border(1.dp, if (isUnlocked) Color(0xFFFFD54F) else Color.Gray, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(text = tool.icon, fontSize = 22.sp)
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = tool.title,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                textAlign = TextAlign.Center
            )

            Text(
                text = tool.description,
                color = Color(0xFFAAAAAA),
                fontSize = 10.sp,
                textAlign = TextAlign.Center,
                lineHeight = 13.sp,
                modifier = Modifier
                    .padding(vertical = 3.dp)
                    .height(32.dp)
            )

            Text(
                text = "Owned: $ownedCount",
                color = Color(0xFFFFD54F),
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(6.dp))

            if (isUnlocked) {
                Button(
                    onClick = onBuy,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(34.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE65100)),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(2.dp)
                ) {
                    Text(
                        text = "Buy 1 ($price 🪙)",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            } else {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color.Black.copy(alpha = 0.4f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(34.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = "Unlocks at Lv ${tool.unlockLevel}",
                            color = Color(0xFF888888),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

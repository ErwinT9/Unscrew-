package com.example.model

import androidx.compose.ui.graphics.Color

enum class ScrewColor(
    val displayName: String,
    val primaryColor: Long,
    val darkColor: Long,
    val lightColor: Long,
    val symbol: String
) {
    RED("Ruby Red", 0xFFE53935, 0xFFB71C1C, 0xFFFFCDD2, "+"),
    BLUE("Sapphire", 0xFF1E88E5, 0xFF0D47A1, 0xFFBBDEFB, "×"),
    GREEN("Emerald", 0xFF43A047, 0xFF1B5E20, 0xFFC8E6C9, "★"),
    YELLOW("Golden", 0xFFFDD835, 0xFFF57F17, 0xFFFFF9C4, "✦"),
    PURPLE("Amethyst", 0xFF8E24AA, 0xFF4A148C, 0xFFE1BEE7, "◆"),
    ORANGE("Topaz", 0xFFFB8C00, 0xFFE65100, 0xFFFFE0B2, "▲"),
    PINK("Rose Quartz", 0xFFE91E63, 0xFF880E4F, 0xFFF8BBD0, "♥"),
    CYAN("Diamond", 0xFF00ACC1, 0xFF006064, 0xFFB2EBF2, "●")
}

enum class BiomeTheme(
    val title: String,
    val subtitle: String,
    val bgGradientStart: Long,
    val bgGradientEnd: Long,
    val boardBgColor: Long,
    val boardBorderColor: Long,
    val plankBaseTexture: PlankTexture,
    val accentColor: Long,
    val iconName: String
) {
    WORKSHOP(
        "Carpenter Workshop",
        "Classic Timber & Brass",
        0xFF2E1A11,
        0xFF180D08,
        0xFF3E2723,
        0xFFD7CCC8,
        PlankTexture.WOOD_OAK,
        0xFFFFB300,
        "🪚"
    ),
    ARABIAN_PALACE(
        "Persian Mandala",
        "Royal Purple & Silk Jewels",
        0xFF311B92,
        0xFF1A0A4A,
        0xFF4A148C,
        0xFFFFD54F,
        PlankTexture.JEWELED_GOLD,
        0xFFFFCA28,
        "🕌"
    ),
    STEAMPUNK_FORGE(
        "Clockwork Forge",
        "Bronze Gears & Rivets",
        0xFF263238,
        0xFF10171A,
        0xFF37474F,
        0xFFFF8A65,
        PlankTexture.BRONZE_STEEL,
        0xFFFF7043,
        "⚙️"
    ),
    ZEN_GARDEN(
        "Zen Bamboo Haven",
        "Emerald Canes & River Stone",
        0xFF1B5E20,
        0xFF0A2E0E,
        0xFF2E7D32,
        0xFFA5D6A7,
        PlankTexture.BAMBOO_JADE,
        0xFF81C784,
        "🎋"
    ),
    CYBER_NEON(
        "Cyberpunk 2099",
        "Holo-Grids & Laser Bolts",
        0xFF0D1B2A,
        0xFF050B14,
        0xFF1B263B,
        0xFF00E5FF,
        PlankTexture.NEON_HOLO,
        0xFF00E5FF,
        "⚡"
    ),
    ALCHEMY_LAB(
        "Alchemist Sanctuary",
        "Mystic Glass & Potion Flasks",
        0xFF3E2723,
        0xFF1A0B2E,
        0xFF311B92,
        0xFFBA68C8,
        PlankTexture.CRYSTAL_GLASS,
        0xFFCE93D8,
        "🧪"
    ),
    PHARAOH_TOMB(
        "Pharaoh's Vault",
        "Ancient Gold & Lapis Lazuli",
        0xFF3E2005,
        0xFF1F1002,
        0xFF4E2C0B,
        0xFFFFD700,
        PlankTexture.GOLDEN_STONE,
        0xFFFFD700,
        "🏺"
    ),
    ATLANTIS_DEEP(
        "Abyssal Atlantis",
        "Bioluminescent Sea-Glass",
        0xFF00293C,
        0xFF001520,
        0xFF004D66,
        0xFF4DD0E1,
        PlankTexture.SEA_GLASS,
        0xFF26C6DA,
        "🌊"
    ),
    CANDY_KINGDOM(
        "Sugar Candy Realm",
        "Wafer Planks & Sweet Glazes",
        0xFF880E4F,
        0xFF4A0028,
        0xFFAD1457,
        0xFFF48FB1,
        PlankTexture.CANDY_WAFER,
        0xFFFF4081,
        "🍭"
    ),
    COSMIC_GALAXY(
        "Cosmic Singularity",
        "Quantum Starlight & Nebula",
        0xFF0B001A,
        0xFF000000,
        0xFF1A0033,
        0xFFE040FB,
        PlankTexture.COSMIC_STARS,
        0xFFE040FB,
        "🌌"
    )
}

enum class PlankTexture {
    WOOD_OAK,
    JEWELED_GOLD,
    BRONZE_STEEL,
    BAMBOO_JADE,
    NEON_HOLO,
    CRYSTAL_GLASS,
    GOLDEN_STONE,
    SEA_GLASS,
    CANDY_WAFER,
    COSMIC_STARS
}

enum class PlankShape {
    RECTANGLE,
    ROUNDED_BAR,
    CIRCULAR_RING,
    MANDALA_PETAL,
    CROSS_BAR,
    DIAGONAL_BEAM,
    BOTTLE_BODY,
    TRIANGLE_PLATE,
    GEAR_DISC,
    STAR_PLATE
}

data class Point2D(val x: Float, val y: Float)

data class Screw(
    val id: String,
    val x: Float,
    val y: Float,
    val color: ScrewColor,
    var holeId: String,
    var isUnscrewing: Boolean = false,
    var isFlying: Boolean = false,
    var flightProgress: Float = 0f,
    var isCleared: Boolean = false
)

data class Hole(
    val id: String,
    val x: Float,
    val y: Float,
    val isReservedSpare: Boolean = false,
    var occupiedScrewId: String? = null,
    val isUnlocked: Boolean = true
)

data class Plank(
    val id: String,
    val shape: PlankShape,
    val points: List<Point2D>, // 2 points for bar (start, end) or multiple for polygon/center+radius
    val width: Float = 28f,
    val radius: Float = 40f,
    val angle: Float = 0f,
    val zIndex: Int = 0,
    val colorHex: Long,
    val secondaryColorHex: Long = 0xFFFFFFFF,
    val holeIds: List<String>,
    var isFalling: Boolean = false,
    var fallProgress: Float = 0f,
    var fallOffsetY: Float = 0f,
    var fallRotation: Float = 0f,
    var fallAlpha: Float = 1f,
    var isCleared: Boolean = false
)

data class ScrewBox(
    val id: String,
    val color: ScrewColor,
    val capacity: Int = 3,
    val filledScrews: MutableList<String> = mutableListOf(),
    var isFull: Boolean = false,
    var isDisappearing: Boolean = false
)

enum class ToolType(
    val title: String,
    val description: String,
    val icon: String,
    val unlockLevel: Int,
    val coinCost: Int
) {
    DRILL("Drill Extra Hole", "Adds an extra temporary screw parking hole on the board", "🔩", 3, 50),
    AUTO_SCREW("Auto Screwdriver", "Instantly unscrews and matches a blocking screw", "🪛", 7, 75),
    FREEZE_TIME("Time Freeze", "Pauses the countdown timer for 20 seconds", "⏳", 12, 60),
    MAGNET("Magnetic Pull", "Instantly collects 3 matching screws from board into boxes", "🧲", 18, 100),
    HAMMER("Plank Breaker", "Shatters any obstructing plank and frees its pins", "🔨", 25, 120),
    UNDO("Undo Wrench", "Reverses your previous screw move", "↩️", 1, 40)
}

data class LevelData(
    val levelNumber: Int,
    val title: String,
    val biome: BiomeTheme,
    val timeLimitSeconds: Int,
    val targetBoxes: List<ScrewBox>,
    val holes: List<Hole>,
    val screws: List<Screw>,
    val planks: List<Plank>,
    val freeHolesCount: Int = 3,
    val star3TimeRemaining: Int = 30,
    val star2TimeRemaining: Int = 10,
    val coinReward: Int = 100
)

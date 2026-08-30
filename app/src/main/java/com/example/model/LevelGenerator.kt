package com.example.model

import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.PI

object LevelGenerator {

    fun getBiomeForLevel(level: Int): BiomeTheme {
        return when ((level - 1) / 5) {
            0 -> BiomeTheme.WORKSHOP
            1 -> BiomeTheme.ARABIAN_PALACE
            2 -> BiomeTheme.STEAMPUNK_FORGE
            3 -> BiomeTheme.ZEN_GARDEN
            4 -> BiomeTheme.CYBER_NEON
            5 -> BiomeTheme.ALCHEMY_LAB
            6 -> BiomeTheme.PHARAOH_TOMB
            7 -> BiomeTheme.ATLANTIS_DEEP
            8 -> BiomeTheme.CANDY_KINGDOM
            else -> BiomeTheme.COSMIC_GALAXY
        }
    }

    fun getLevel(levelNumber: Int): LevelData {
        val clampedLevel = levelNumber.coerceIn(1, 50)
        val biome = getBiomeForLevel(clampedLevel)
        
        // Base parameters scaling with level
        val baseTime = when {
            clampedLevel <= 5 -> 100
            clampedLevel <= 15 -> 90
            clampedLevel <= 25 -> 80
            clampedLevel <= 35 -> 75
            clampedLevel <= 45 -> 70
            else -> 65
        }
        val timeLimit = baseTime + (if (clampedLevel % 5 == 0) 15 else 0) // Boss/milestone levels get slightly more time

        val colorsPool = when {
            clampedLevel <= 4 -> listOf(ScrewColor.RED, ScrewColor.BLUE)
            clampedLevel <= 10 -> listOf(ScrewColor.RED, ScrewColor.BLUE, ScrewColor.YELLOW)
            clampedLevel <= 20 -> listOf(ScrewColor.RED, ScrewColor.BLUE, ScrewColor.GREEN, ScrewColor.YELLOW)
            clampedLevel <= 35 -> listOf(ScrewColor.RED, ScrewColor.BLUE, ScrewColor.GREEN, ScrewColor.YELLOW, ScrewColor.PURPLE)
            else -> listOf(ScrewColor.RED, ScrewColor.BLUE, ScrewColor.GREEN, ScrewColor.YELLOW, ScrewColor.PURPLE, ScrewColor.ORANGE, ScrewColor.CYAN)
        }

        return generateLayoutForLevel(clampedLevel, biome, timeLimit, colorsPool)
    }

    private fun generateLayoutForLevel(
        level: Int,
        biome: BiomeTheme,
        timeLimit: Int,
        colorsPool: List<ScrewColor>
    ): LevelData {
        val holes = mutableListOf<Hole>()
        val screws = mutableListOf<Screw>()
        val planks = mutableListOf<Plank>()
        val levelIndexInBiome = (level - 1) % 5 // 0..4
        val biomeIndex = (level - 1) / 5

        // Free spare holes count
        val freeHolesCount = if (level <= 5) 3 else if (level <= 20) 4 else if (level <= 35) 4 else 5
        // Add reserved spare holes along the top shelf of the board canvas
        for (i in 0 until freeHolesCount) {
            val hx = 60f + i * (280f / (freeHolesCount - 1).coerceAtLeast(1))
            val hy = 35f
            holes.add(Hole(id = "free_hole_$i", x = hx, y = hy, isReservedSpare = true))
        }

        // Layout presets per biome and level index
        when (biomeIndex) {
            0 -> buildWorkshopLevels(levelIndexInBiome, holes, screws, planks, colorsPool, level)
            1 -> buildMandalaLevels(levelIndexInBiome, holes, screws, planks, colorsPool, level)
            2 -> buildClockworkLevels(levelIndexInBiome, holes, screws, planks, colorsPool, level)
            3 -> buildZenGardenLevels(levelIndexInBiome, holes, screws, planks, colorsPool, level)
            4 -> buildCyberLevels(levelIndexInBiome, holes, screws, planks, colorsPool, level)
            5 -> buildAlchemyLevels(levelIndexInBiome, holes, screws, planks, colorsPool, level)
            6 -> buildEgyptianLevels(levelIndexInBiome, holes, screws, planks, colorsPool, level)
            7 -> buildAtlantisLevels(levelIndexInBiome, holes, screws, planks, colorsPool, level)
            8 -> buildCandyLevels(levelIndexInBiome, holes, screws, planks, colorsPool, level)
            else -> buildCosmicLevels(levelIndexInBiome, holes, screws, planks, colorsPool, level)
        }

        // Generate Target Boxes based on the actual screws count of each color
        val colorCounts = screws.groupBy { it.color }
        val boxes = mutableListOf<ScrewBox>()
        var boxIdCounter = 0
        colorCounts.forEach { (color, screwList) ->
            var remaining = screwList.size
            while (remaining > 0) {
                val cap = remaining.coerceAtMost(3)
                boxes.add(ScrewBox(id = "box_${boxIdCounter++}", color = color, capacity = cap))
                remaining -= cap
            }
        }
        // Shuffle box order slightly for strategic planning challenge, but ensure first color is immediately visible
        val orderedBoxes = boxes.shuffled().toMutableList()
        // If possible, ensure the top box matches a top-layer screw
        val topLayerScrewColors = screws.take(3).map { it.color }.toSet()
        val firstMatchIdx = orderedBoxes.indexOfFirst { it.color in topLayerScrewColors }
        if (firstMatchIdx > 0) {
            val matchBox = orderedBoxes.removeAt(firstMatchIdx)
            orderedBoxes.add(0, matchBox)
        }

        val titles = listOf(
            "Introductory Joint", "Cross Timber", "Tripod Scaffold", "Dual Trusses", "Workshop Lattice",
            "Arabesque Ring", "Jeweled Diamond", "Palace Sunburst", "Sultan's Mandala", "Grand Mosque Dome",
            "Twin Cogs", "Scissor Linkage", "Piston Matrix", "Interlocking Gears", "Chronos Dial",
            "Bamboo Raft", "Torii Portal", "Lotus Petals", "Stepping Stones", "Yin-Yang Harmony",
            "Laser Diagonal", "Silicon Chip", "Hexagon Nexus", "Quantum Scaffolding", "Cyber Core",
            "Potion Flask", "Alembic Spiral", "Philosopher's Seal", "Hourglass Vessel", "Grand Crucible",
            "Gold Pyramid", "Ankh of Life", "Scarab Brooch", "Eye of Horus", "Pharaoh's Gate",
            "Nautical Anchor", "Starfish Prism", "Nautilus Shell", "Trident Cage", "Poseidon Crown",
            "Lollipop Wheel", "Wafer Lattice", "Candy Pretzel", "Sugar Sandwich", "Gingerbread Keep",
            "Orbital Ring", "Pulsar Beam", "Hypercube Lattice", "Infinity Coil", "Cosmic Singularity"
        )

        val title = titles.getOrElse(level - 1) { "Level $level Challenge" }

        return LevelData(
            levelNumber = level,
            title = title,
            biome = biome,
            timeLimitSeconds = timeLimit,
            targetBoxes = orderedBoxes,
            holes = holes,
            screws = screws,
            planks = planks,
            freeHolesCount = freeHolesCount,
            coinReward = 50 + level * 10
        )
    }

    // Helper functions to construct various geometry patterns
    private fun addBarPlank(
        id: String,
        p1: Point2D,
        p2: Point2D,
        zIndex: Int,
        colorHex: Long,
        screwColors: List<ScrewColor>,
        holes: MutableList<Hole>,
        screws: MutableList<Screw>,
        planks: MutableList<Plank>,
        width: Float = 32f
    ) {
        val h1Id = "${id}_h1"
        val h2Id = "${id}_h2"
        
        // Check if holes already exist at these approximate points
        val existingH1 = holes.find { Math.hypot((it.x - p1.x).toDouble(), (it.y - p1.y).toDouble()) < 15 }
        val actualH1Id = existingH1?.id ?: h1Id
        if (existingH1 == null) {
            holes.add(Hole(id = h1Id, x = p1.x, y = p1.y))
        }

        val existingH2 = holes.find { Math.hypot((it.x - p2.x).toDouble(), (it.y - p2.y).toDouble()) < 15 }
        val actualH2Id = existingH2?.id ?: h2Id
        if (existingH2 == null) {
            holes.add(Hole(id = h2Id, x = p2.x, y = p2.y))
        }

        // Assign screws if not already occupied
        val h1Obj = holes.find { it.id == actualH1Id }
        if (h1Obj?.occupiedScrewId == null && screwColors.isNotEmpty()) {
            val sc1 = Screw(id = "screw_${screws.size}", x = p1.x, y = p1.y, color = screwColors[0], holeId = actualH1Id)
            screws.add(sc1)
            h1Obj?.occupiedScrewId = sc1.id
        }

        val h2Obj = holes.find { it.id == actualH2Id }
        if (h2Obj?.occupiedScrewId == null && screwColors.size > 1) {
            val sc2 = Screw(id = "screw_${screws.size}", x = p2.x, y = p2.y, color = screwColors[1], holeId = actualH2Id)
            screws.add(sc2)
            h2Obj?.occupiedScrewId = sc2.id
        }

        planks.add(
            Plank(
                id = id,
                shape = PlankShape.ROUNDED_BAR,
                points = listOf(p1, p2),
                width = width,
                zIndex = zIndex,
                colorHex = colorHex,
                holeIds = listOf(actualH1Id, actualH2Id)
            )
        )
    }

    private fun add3HoleBar(
        id: String,
        p1: Point2D,
        p2: Point2D,
        p3: Point2D,
        zIndex: Int,
        colorHex: Long,
        screwColors: List<ScrewColor>,
        holes: MutableList<Hole>,
        screws: MutableList<Screw>,
        planks: MutableList<Plank>,
        width: Float = 32f
    ) {
        val hIds = mutableListOf<String>()
        val pts = listOf(p1, p2, p3)
        pts.forEachIndexed { idx, pt ->
            val existing = holes.find { Math.hypot((it.x - pt.x).toDouble(), (it.y - pt.y).toDouble()) < 15 }
            val hid = existing?.id ?: "${id}_h$idx"
            if (existing == null) {
                holes.add(Hole(id = hid, x = pt.x, y = pt.y))
            }
            hIds.add(hid)
            val hObj = holes.find { it.id == hid }
            if (hObj?.occupiedScrewId == null && idx < screwColors.size) {
                val sc = Screw(id = "screw_${screws.size}", x = pt.x, y = pt.y, color = screwColors[idx], holeId = hid)
                screws.add(sc)
                hObj?.occupiedScrewId = sc.id
            }
        }

        planks.add(
            Plank(
                id = id,
                shape = PlankShape.ROUNDED_BAR,
                points = listOf(p1, p3),
                width = width,
                zIndex = zIndex,
                colorHex = colorHex,
                holeIds = hIds
            )
        )
    }

    private fun addRingPlank(
        id: String,
        center: Point2D,
        radius: Float,
        holeAnglesDeg: List<Float>,
        zIndex: Int,
        colorHex: Long,
        screwColors: List<ScrewColor>,
        holes: MutableList<Hole>,
        screws: MutableList<Screw>,
        planks: MutableList<Plank>,
        width: Float = 30f
    ) {
        val hIds = mutableListOf<String>()
        holeAnglesDeg.forEachIndexed { idx, deg ->
            val rad = deg * PI.toFloat() / 180f
            val hx = center.x + radius * cos(rad)
            val hy = center.y + radius * sin(rad)
            val existing = holes.find { Math.hypot((it.x - hx).toDouble(), (it.y - hy).toDouble()) < 15 }
            val hid = existing?.id ?: "${id}_h$idx"
            if (existing == null) {
                holes.add(Hole(id = hid, x = hx, y = hy))
            }
            hIds.add(hid)
            val hObj = holes.find { it.id == hid }
            if (hObj?.occupiedScrewId == null && idx < screwColors.size) {
                val sc = Screw(id = "screw_${screws.size}", x = hx, y = hy, color = screwColors[idx], holeId = hid)
                screws.add(sc)
                hObj?.occupiedScrewId = sc.id
            }
        }

        planks.add(
            Plank(
                id = id,
                shape = PlankShape.CIRCULAR_RING,
                points = listOf(center),
                radius = radius,
                width = width,
                zIndex = zIndex,
                colorHex = colorHex,
                holeIds = hIds
            )
        )
    }

    // --- BIOME BUILDERS ---

    private fun buildWorkshopLevels(
        subLevel: Int,
        holes: MutableList<Hole>,
        screws: MutableList<Screw>,
        planks: MutableList<Plank>,
        colors: List<ScrewColor>,
        level: Int
    ) {
        val oak = 0xFFD7A15C
        val walnut = 0xFF8D5B34
        val pine = 0xFFF5DEB3
        val mahogany = 0xFF5D2E17

        when (subLevel) {
            0 -> { // 2 simple crossing planks
                addBarPlank("plank_1", Point2D(90f, 180f), Point2D(310f, 360f), 0, walnut, listOf(colors[0], colors[1 % colors.size]), holes, screws, planks)
                addBarPlank("plank_2", Point2D(310f, 180f), Point2D(90f, 360f), 1, oak, listOf(colors[0], colors[1 % colors.size]), holes, screws, planks)
            }
            1 -> { // 3 planks Z shape
                addBarPlank("plank_top", Point2D(80f, 160f), Point2D(320f, 160f), 0, oak, listOf(colors[0], colors[1 % colors.size]), holes, screws, planks)
                addBarPlank("plank_bot", Point2D(80f, 380f), Point2D(320f, 380f), 0, pine, listOf(colors[1 % colors.size], colors[0]), holes, screws, planks)
                addBarPlank("plank_diag", Point2D(300f, 160f), Point2D(100f, 380f), 1, walnut, listOf(colors[0], colors[1 % colors.size]), holes, screws, planks)
            }
            2 -> { // Triangle Frame
                addBarPlank("side_left", Point2D(200f, 140f), Point2D(90f, 370f), 0, walnut, listOf(colors[0], colors[1 % colors.size]), holes, screws, planks)
                addBarPlank("side_right", Point2D(200f, 140f), Point2D(310f, 370f), 1, oak, listOf(colors[1 % colors.size], colors[0]), holes, screws, planks)
                addBarPlank("side_bot", Point2D(90f, 370f), Point2D(310f, 370f), 2, mahogany, listOf(colors[0], colors[1 % colors.size]), holes, screws, planks)
            }
            3 -> { // Scaffolding 4 bars
                addBarPlank("vert_1", Point2D(120f, 140f), Point2D(120f, 390f), 0, oak, listOf(colors[0], colors[1 % colors.size]), holes, screws, planks)
                addBarPlank("vert_2", Point2D(280f, 140f), Point2D(280f, 390f), 0, pine, listOf(colors[1 % colors.size], colors[0]), holes, screws, planks)
                addBarPlank("diag_1", Point2D(120f, 140f), Point2D(280f, 390f), 1, walnut, listOf(colors[0], colors[1 % colors.size]), holes, screws, planks)
                addBarPlank("diag_2", Point2D(280f, 140f), Point2D(120f, 390f), 2, mahogany, listOf(colors[1 % colors.size], colors[0]), holes, screws, planks)
            }
            else -> { // Ladder Matrix 5 bars (Level 5 Boss)
                addBarPlank("ladder_L", Point2D(110f, 130f), Point2D(110f, 410f), 0, walnut, listOf(colors[0], colors[1 % colors.size]), holes, screws, planks)
                addBarPlank("ladder_R", Point2D(290f, 130f), Point2D(290f, 410f), 0, oak, listOf(colors[1 % colors.size], colors[0]), holes, screws, planks)
                addBarPlank("rung_1", Point2D(110f, 180f), Point2D(290f, 180f), 1, pine, listOf(colors[0], colors[1 % colors.size]), holes, screws, planks)
                addBarPlank("rung_2", Point2D(110f, 270f), Point2D(290f, 270f), 2, mahogany, listOf(colors[1 % colors.size], colors[0]), holes, screws, planks)
                addBarPlank("rung_3", Point2D(110f, 360f), Point2D(290f, 360f), 1, oak, listOf(colors[0], colors[1 % colors.size]), holes, screws, planks)
            }
        }
    }

    private fun buildMandalaLevels(
        subLevel: Int,
        holes: MutableList<Hole>,
        screws: MutableList<Screw>,
        planks: MutableList<Plank>,
        colors: List<ScrewColor>,
        level: Int
    ) {
        val purple = 0xFF7B1FA2
        val gold = 0xFFFFD700
        val magenta = 0xFFC2185B
        val indigo = 0xFF3F51B5

        val c1 = colors[0]
        val c2 = colors[1 % colors.size]
        val c3 = colors[2 % colors.size]

        when (subLevel) {
            0 -> { // Concentric Ring + Cross
                addRingPlank("mandala_ring", Point2D(200f, 270f), 110f, listOf(0f, 90f, 180f, 270f), 0, purple, listOf(c1, c2, c1, c2), holes, screws, planks)
                addBarPlank("bar_h", Point2D(90f, 270f), Point2D(310f, 270f), 1, gold, listOf(c3, c3), holes, screws, planks)
                addBarPlank("bar_v", Point2D(200f, 160f), Point2D(200f, 380f), 2, magenta, listOf(c2, c1), holes, screws, planks)
            }
            1 -> { // Diamond Rhombus
                addBarPlank("d_top_L", Point2D(200f, 130f), Point2D(80f, 270f), 0, gold, listOf(c1, c2), holes, screws, planks)
                addBarPlank("d_top_R", Point2D(200f, 130f), Point2D(320f, 270f), 1, magenta, listOf(c2, c3), holes, screws, planks)
                addBarPlank("d_bot_L", Point2D(80f, 270f), Point2D(200f, 410f), 0, purple, listOf(c3, c1), holes, screws, planks)
                addBarPlank("d_bot_R", Point2D(320f, 270f), Point2D(200f, 410f), 1, indigo, listOf(c1, c2), holes, screws, planks)
                addRingPlank("center_disc", Point2D(200f, 270f), 45f, listOf(45f, 135f, 225f, 315f), 2, gold, listOf(c2, c3, c1, c2), holes, screws, planks, width = 22f)
            }
            2 -> { // Palace Star
                for (i in 0 until 4) {
                    val deg = i * 45f
                    val rad = deg * PI.toFloat() / 180f
                    val p1 = Point2D(200f - 110f * cos(rad), 270f - 110f * sin(rad))
                    val p2 = Point2D(200f + 110f * cos(rad), 270f + 110f * sin(rad))
                    val col = if (i % 2 == 0) purple else gold
                    val scCol = listOf(colors[i % colors.size], colors[(i + 1) % colors.size])
                    addBarPlank("star_ray_$i", p1, p2, i, col, scCol, holes, screws, planks)
                }
            }
            3 -> { // Ornate Mandala Dome
                addRingPlank("outer_ring", Point2D(200f, 270f), 120f, listOf(30f, 90f, 150f, 210f, 270f, 330f), 0, indigo, listOf(c1, c2, c3, c1, c2, c3), holes, screws, planks)
                addRingPlank("inner_ring", Point2D(200f, 270f), 65f, listOf(0f, 120f, 240f), 1, gold, listOf(c2, c3, c1), holes, screws, planks)
                addBarPlank("dome_cross_1", Point2D(90f, 210f), Point2D(310f, 330f), 2, magenta, listOf(c3, c2), holes, screws, planks)
                addBarPlank("dome_cross_2", Point2D(90f, 330f), Point2D(310f, 210f), 3, purple, listOf(c1, c3), holes, screws, planks)
            }
            else -> { // Level 10 Sultan's Grand Mandala Master
                addRingPlank("m_outer", Point2D(200f, 270f), 130f, listOf(0f, 60f, 120f, 180f, 240f, 300f), 0, purple, listOf(c1, c2, c3, c1, c2, c3), holes, screws, planks)
                addRingPlank("m_mid", Point2D(200f, 270f), 80f, listOf(30f, 90f, 150f, 210f, 270f, 330f), 1, gold, listOf(c2, c3, c1, c2, c3, c1), holes, screws, planks)
                addBarPlank("m_b1", Point2D(120f, 190f), Point2D(280f, 350f), 2, magenta, listOf(c1, c2), holes, screws, planks)
                addBarPlank("m_b2", Point2D(280f, 190f), Point2D(120f, 350f), 3, indigo, listOf(c3, c1), holes, screws, planks)
                addRingPlank("m_center", Point2D(200f, 270f), 35f, listOf(45f, 225f), 4, gold, listOf(c2, c3), holes, screws, planks, width = 20f)
            }
        }
    }

    private fun buildClockworkLevels(
        subLevel: Int,
        holes: MutableList<Hole>,
        screws: MutableList<Screw>,
        planks: MutableList<Plank>,
        colors: List<ScrewColor>,
        level: Int
    ) {
        val bronze = 0xFFCD7F32
        val copper = 0xFFB87333
        val steel = 0xFF78909C
        val brass = 0xFFE6C229

        val c1 = colors[0]
        val c2 = colors[1 % colors.size]
        val c3 = colors[2 % colors.size]
        val c4 = colors[3 % colors.size]

        when (subLevel) {
            0 -> { // Dual Intermeshed Gears
                addRingPlank("gear_1", Point2D(140f, 240f), 65f, listOf(0f, 90f, 180f, 270f), 0, bronze, listOf(c1, c2, c3, c4), holes, screws, planks)
                addRingPlank("gear_2", Point2D(260f, 300f), 65f, listOf(45f, 135f, 225f, 315f), 1, copper, listOf(c2, c3, c4, c1), holes, screws, planks)
                addBarPlank("linkage", Point2D(140f, 240f), Point2D(260f, 300f), 2, brass, listOf(c3, c2), holes, screws, planks)
            }
            1 -> { // Scissor Linkage
                addBarPlank("sc_1a", Point2D(90f, 150f), Point2D(310f, 270f), 0, steel, listOf(c1, c2), holes, screws, planks)
                addBarPlank("sc_1b", Point2D(310f, 150f), Point2D(90f, 270f), 0, bronze, listOf(c3, c4), holes, screws, planks)
                addBarPlank("sc_2a", Point2D(90f, 270f), Point2D(310f, 390f), 1, copper, listOf(c2, c1), holes, screws, planks)
                addBarPlank("sc_2b", Point2D(310f, 270f), Point2D(90f, 390f), 1, brass, listOf(c4, c3), holes, screws, planks)
                addBarPlank("sc_tie", Point2D(200f, 150f), Point2D(200f, 390f), 2, steel, listOf(c1, c3), holes, screws, planks)
            }
            2 -> { // Piston Matrix
                addBarPlank("p_h1", Point2D(70f, 180f), Point2D(330f, 180f), 0, bronze, listOf(c1, c2), holes, screws, planks)
                addBarPlank("p_h2", Point2D(70f, 360f), Point2D(330f, 360f), 0, copper, listOf(c3, c4), holes, screws, planks)
                addBarPlank("p_v1", Point2D(120f, 130f), Point2D(120f, 410f), 1, steel, listOf(c2, c3), holes, screws, planks)
                addBarPlank("p_v2", Point2D(280f, 130f), Point2D(280f, 410f), 1, brass, listOf(c4, c1), holes, screws, planks)
                addBarPlank("p_diag", Point2D(70f, 180f), Point2D(330f, 360f), 2, bronze, listOf(c1, c4), holes, screws, planks)
            }
            3 -> { // 3 Gears Complex
                addRingPlank("g_top", Point2D(200f, 170f), 55f, listOf(0f, 120f, 240f), 0, brass, listOf(c1, c2, c3), holes, screws, planks)
                addRingPlank("g_left", Point2D(130f, 310f), 55f, listOf(30f, 150f, 270f), 1, bronze, listOf(c2, c3, c4), holes, screws, planks)
                addRingPlank("g_right", Point2D(270f, 310f), 55f, listOf(90f, 210f, 330f), 2, copper, listOf(c3, c4, c1), holes, screws, planks)
                addBarPlank("triangle_tie", Point2D(200f, 170f), Point2D(130f, 310f), 3, steel, listOf(c4, c1), holes, screws, planks)
                addBarPlank("base_tie", Point2D(130f, 310f), Point2D(270f, 310f), 4, brass, listOf(c2, c3), holes, screws, planks)
            }
            else -> { // Level 15 Chronos Dial
                addRingPlank("chronos_outer", Point2D(200f, 270f), 125f, listOf(0f, 45f, 90f, 135f, 180f, 225f, 270f, 315f), 0, steel, listOf(c1, c2, c3, c4, c1, c2, c3, c4), holes, screws, planks)
                addBarPlank("hand_hour", Point2D(200f, 270f), Point2D(200f, 150f), 1, brass, listOf(c1, c2), holes, screws, planks)
                addBarPlank("hand_min", Point2D(200f, 270f), Point2D(300f, 270f), 2, copper, listOf(c3, c4), holes, screws, planks)
                addBarPlank("hand_sec", Point2D(200f, 270f), Point2D(120f, 350f), 3, bronze, listOf(c2, c1), holes, screws, planks)
                addRingPlank("chronos_hub", Point2D(200f, 270f), 35f, listOf(60f, 240f), 4, brass, listOf(c4, c3), holes, screws, planks, width = 20f)
            }
        }
    }

    private fun buildZenGardenLevels(
        subLevel: Int,
        holes: MutableList<Hole>,
        screws: MutableList<Screw>,
        planks: MutableList<Plank>,
        colors: List<ScrewColor>,
        level: Int
    ) {
        val bambooGreen = 0xFF4CAF50
        val darkJade = 0xFF1B5E20
        val riverStone = 0xFF607D8B
        val cherryPink = 0xFFF06292

        val c1 = colors[0]
        val c2 = colors[1 % colors.size]
        val c3 = colors[2 % colors.size]
        val c4 = colors[3 % colors.size]

        when (subLevel) {
            0 -> { // Bamboo Raft
                for (i in 0 until 4) {
                    val y = 160f + i * 70f
                    addBarPlank("bamboo_$i", Point2D(80f, y), Point2D(320f, y), 0, if (i % 2 == 0) bambooGreen else darkJade, listOf(colors[i % colors.size], colors[(i + 1) % colors.size]), holes, screws, planks)
                }
                addBarPlank("raft_tie_1", Point2D(130f, 140f), Point2D(130f, 390f), 1, riverStone, listOf(c3, c4), holes, screws, planks)
                addBarPlank("raft_tie_2", Point2D(270f, 140f), Point2D(270f, 390f), 1, cherryPink, listOf(c1, c2), holes, screws, planks)
            }
            1 -> { // Torii Gate
                addBarPlank("torii_post_L", Point2D(110f, 160f), Point2D(110f, 410f), 0, darkJade, listOf(c1, c2), holes, screws, planks)
                addBarPlank("torii_post_R", Point2D(290f, 160f), Point2D(290f, 410f), 0, darkJade, listOf(c3, c4), holes, screws, planks)
                addBarPlank("torii_lintel_top", Point2D(60f, 150f), Point2D(340f, 150f), 1, cherryPink, listOf(c2, c1), holes, screws, planks)
                addBarPlank("torii_lintel_mid", Point2D(80f, 210f), Point2D(320f, 210f), 2, bambooGreen, listOf(c4, c3), holes, screws, planks)
                addBarPlank("torii_bracket", Point2D(110f, 260f), Point2D(290f, 260f), 1, riverStone, listOf(c1, c4), holes, screws, planks)
            }
            2 -> { // Lotus Petals
                for (i in 0 until 5) {
                    val deg = i * 72f
                    val rad = deg * PI.toFloat() / 180f
                    val p1 = Point2D(200f, 270f)
                    val p2 = Point2D(200f + 115f * cos(rad), 270f + 115f * sin(rad))
                    addBarPlank("petal_$i", p1, p2, i, if (i % 2 == 0) cherryPink else bambooGreen, listOf(colors[i % colors.size], colors[(i + 1) % colors.size]), holes, screws, planks)
                }
                addRingPlank("lotus_heart", Point2D(200f, 270f), 40f, listOf(36f, 180f), 5, darkJade, listOf(c2, c3), holes, screws, planks, width = 22f)
            }
            3 -> { // Stepping Stone Grid
                addBarPlank("ss_1", Point2D(90f, 160f), Point2D(310f, 240f), 0, riverStone, listOf(c1, c2), holes, screws, planks)
                addBarPlank("ss_2", Point2D(310f, 240f), Point2D(90f, 320f), 1, bambooGreen, listOf(c3, c4), holes, screws, planks)
                addBarPlank("ss_3", Point2D(90f, 320f), Point2D(310f, 400f), 2, darkJade, listOf(c2, c1), holes, screws, planks)
                addBarPlank("ss_v", Point2D(200f, 140f), Point2D(200f, 420f), 3, cherryPink, listOf(c4, c3), holes, screws, planks)
            }
            else -> { // Level 20 Yin-Yang Harmony
                addRingPlank("yy_outer", Point2D(200f, 270f), 120f, listOf(0f, 60f, 120f, 180f, 240f, 300f), 0, riverStone, listOf(c1, c2, c3, c4, c1, c2), holes, screws, planks)
                addRingPlank("yy_top", Point2D(200f, 210f), 50f, listOf(0f, 180f), 1, darkJade, listOf(c3, c4), holes, screws, planks)
                addRingPlank("yy_bot", Point2D(200f, 330f), 50f, listOf(0f, 180f), 1, cherryPink, listOf(c1, c2), holes, screws, planks)
                addBarPlank("yy_spine", Point2D(200f, 150f), Point2D(200f, 390f), 2, bambooGreen, listOf(c4, c1), holes, screws, planks)
            }
        }
    }

    private fun buildCyberLevels(
        subLevel: Int,
        holes: MutableList<Hole>,
        screws: MutableList<Screw>,
        planks: MutableList<Plank>,
        colors: List<ScrewColor>,
        level: Int
    ) {
        val neonCyan = 0xFF00E5FF
        val neonPink = 0xFFFF007F
        val neonYellow = 0xFFFFEA00
        val cyberPurple = 0xFF7C4DFF

        val c1 = colors[0]
        val c2 = colors[1 % colors.size]
        val c3 = colors[2 % colors.size]
        val c4 = colors[3 % colors.size]
        val c5 = colors[4 % colors.size]

        when (subLevel) {
            0 -> { // Laser Diagonal Grid
                addBarPlank("laser_1", Point2D(70f, 160f), Point2D(270f, 360f), 0, neonCyan, listOf(c1, c2), holes, screws, planks)
                addBarPlank("laser_2", Point2D(130f, 160f), Point2D(330f, 360f), 0, neonPink, listOf(c3, c4), holes, screws, planks)
                addBarPlank("laser_3", Point2D(330f, 160f), Point2D(130f, 360f), 1, neonYellow, listOf(c2, c3), holes, screws, planks)
                addBarPlank("laser_4", Point2D(270f, 160f), Point2D(70f, 360f), 1, cyberPurple, listOf(c4, c5), holes, screws, planks)
            }
            1 -> { // Silicon Chip
                addBarPlank("chip_top", Point2D(100f, 170f), Point2D(300f, 170f), 0, neonCyan, listOf(c1, c2), holes, screws, planks)
                addBarPlank("chip_bot", Point2D(100f, 370f), Point2D(300f, 370f), 0, neonPink, listOf(c3, c4), holes, screws, planks)
                addBarPlank("chip_L", Point2D(100f, 170f), Point2D(100f, 370f), 1, neonYellow, listOf(c2, c5), holes, screws, planks)
                addBarPlank("chip_R", Point2D(300f, 170f), Point2D(300f, 370f), 1, cyberPurple, listOf(c4, c1), holes, screws, planks)
                addBarPlank("chip_diag1", Point2D(100f, 170f), Point2D(300f, 370f), 2, neonCyan, listOf(c5, c3), holes, screws, planks)
                addBarPlank("chip_diag2", Point2D(300f, 170f), Point2D(100f, 370f), 3, neonPink, listOf(c1, c4), holes, screws, planks)
            }
            2 -> { // Hexagon Nexus
                for (i in 0 until 6) {
                    val deg1 = i * 60f
                    val deg2 = (i + 1) * 60f
                    val r = 110f
                    val p1 = Point2D(200f + r * cos(deg1 * PI.toFloat() / 180f), 270f + r * sin(deg1 * PI.toFloat() / 180f))
                    val p2 = Point2D(200f + r * cos(deg2 * PI.toFloat() / 180f), 270f + r * sin(deg2 * PI.toFloat() / 180f))
                    val col = if (i % 2 == 0) neonCyan else neonPink
                    addBarPlank("hex_side_$i", p1, p2, i % 2, col, listOf(colors[i % colors.size], colors[(i + 1) % colors.size]), holes, screws, planks)
                }
                addRingPlank("hex_core", Point2D(200f, 270f), 45f, listOf(0f, 180f), 3, neonYellow, listOf(c3, c5), holes, screws, planks)
            }
            3 -> { // Quantum Scaffolding
                addBarPlank("q_1", Point2D(80f, 140f), Point2D(320f, 220f), 0, cyberPurple, listOf(c1, c2), holes, screws, planks)
                addBarPlank("q_2", Point2D(320f, 220f), Point2D(80f, 300f), 1, neonCyan, listOf(c3, c4), holes, screws, planks)
                addBarPlank("q_3", Point2D(80f, 300f), Point2D(320f, 380f), 2, neonPink, listOf(c5, c1), holes, screws, planks)
                addBarPlank("q_v1", Point2D(120f, 130f), Point2D(120f, 400f), 3, neonYellow, listOf(c2, c3), holes, screws, planks)
                addBarPlank("q_v2", Point2D(280f, 130f), Point2D(280f, 400f), 3, cyberPurple, listOf(c4, c5), holes, screws, planks)
            }
            else -> { // Level 25 Cyber Core Boss
                addRingPlank("core_ring_1", Point2D(200f, 270f), 125f, listOf(0f, 90f, 180f, 270f), 0, neonCyan, listOf(c1, c2, c3, c4), holes, screws, planks)
                addRingPlank("core_ring_2", Point2D(200f, 270f), 80f, listOf(45f, 135f, 225f, 315f), 1, neonPink, listOf(c2, c3, c4, c5), holes, screws, planks)
                addBarPlank("core_b1", Point2D(80f, 270f), Point2D(320f, 270f), 2, neonYellow, listOf(c5, c1), holes, screws, planks)
                addBarPlank("core_b2", Point2D(200f, 150f), Point2D(200f, 390f), 3, cyberPurple, listOf(c3, c2), holes, screws, planks)
                addRingPlank("core_hub", Point2D(200f, 270f), 35f, listOf(30f, 210f), 4, neonCyan, listOf(c4, c5), holes, screws, planks, width = 20f)
            }
        }
    }

    private fun buildAlchemyLevels(
        subLevel: Int,
        holes: MutableList<Hole>,
        screws: MutableList<Screw>,
        planks: MutableList<Plank>,
        colors: List<ScrewColor>,
        level: Int
    ) {
        val glassTeal = 0xFF4DD0E1
        val mysticPurple = 0xFFAB47BC
        val elixirAmber = 0xFFFFB74D
        val potionRuby = 0xFFEC407A

        val c1 = colors[0]
        val c2 = colors[1 % colors.size]
        val c3 = colors[2 % colors.size]
        val c4 = colors[3 % colors.size]
        val c5 = colors[4 % colors.size]

        when (subLevel) {
            0 -> { // Potion Flask Contour
                addBarPlank("flask_neck_L", Point2D(170f, 140f), Point2D(170f, 200f), 0, glassTeal, listOf(c1, c2), holes, screws, planks)
                addBarPlank("flask_neck_R", Point2D(230f, 140f), Point2D(230f, 200f), 0, glassTeal, listOf(c3, c4), holes, screws, planks)
                addBarPlank("flask_body_L", Point2D(170f, 200f), Point2D(90f, 370f), 1, mysticPurple, listOf(c2, c3), holes, screws, planks)
                addBarPlank("flask_body_R", Point2D(230f, 200f), Point2D(310f, 370f), 1, potionRuby, listOf(c4, c5), holes, screws, planks)
                addBarPlank("flask_base", Point2D(90f, 370f), Point2D(310f, 370f), 2, elixirAmber, listOf(c5, c1), holes, screws, planks)
            }
            1 -> { // Alembic Spiral
                addRingPlank("alembic_globe", Point2D(150f, 220f), 60f, listOf(0f, 120f, 240f), 0, glassTeal, listOf(c1, c2, c3), holes, screws, planks)
                addRingPlank("alembic_receiver", Point2D(270f, 320f), 50f, listOf(30f, 150f, 270f), 1, mysticPurple, listOf(c2, c4, c5), holes, screws, planks)
                addBarPlank("alembic_pipe", Point2D(150f, 160f), Point2D(270f, 320f), 2, elixirAmber, listOf(c3, c1), holes, screws, planks)
                addBarPlank("alembic_stand", Point2D(90f, 380f), Point2D(310f, 380f), 3, potionRuby, listOf(c4, c2), holes, screws, planks)
            }
            2 -> { // Philosopher's Seal (Triangle inside Circle)
                addRingPlank("seal_circle", Point2D(200f, 270f), 115f, listOf(0f, 90f, 180f, 270f), 0, mysticPurple, listOf(c1, c2, c3, c4), holes, screws, planks)
                addBarPlank("seal_t1", Point2D(200f, 155f), Point2D(100f, 330f), 1, elixirAmber, listOf(c2, c5), holes, screws, planks)
                addBarPlank("seal_t2", Point2D(200f, 155f), Point2D(300f, 330f), 1, glassTeal, listOf(c3, c1), holes, screws, planks)
                addBarPlank("seal_t3", Point2D(100f, 330f), Point2D(300f, 330f), 2, potionRuby, listOf(c4, c2), holes, screws, planks)
                addRingPlank("seal_heart", Point2D(200f, 270f), 35f, listOf(60f, 240f), 3, elixirAmber, listOf(c5, c3), holes, screws, planks)
            }
            3 -> { // Hourglass Vessel
                addBarPlank("hg_top", Point2D(100f, 140f), Point2D(300f, 140f), 0, glassTeal, listOf(c1, c2), holes, screws, planks)
                addBarPlank("hg_bot", Point2D(100f, 400f), Point2D(300f, 400f), 0, glassTeal, listOf(c3, c4), holes, screws, planks)
                addBarPlank("hg_diag1", Point2D(100f, 140f), Point2D(300f, 400f), 1, mysticPurple, listOf(c2, c5), holes, screws, planks)
                addBarPlank("hg_diag2", Point2D(300f, 140f), Point2D(100f, 400f), 2, potionRuby, listOf(c4, c1), holes, screws, planks)
                addRingPlank("hg_neck", Point2D(200f, 270f), 30f, listOf(0f, 180f), 3, elixirAmber, listOf(c5, c3), holes, screws, planks)
            }
            else -> { // Level 30 Grand Crucible Boss
                addRingPlank("crucible_rim", Point2D(200f, 180f), 90f, listOf(0f, 90f, 180f, 270f), 0, glassTeal, listOf(c1, c2, c3, c4), holes, screws, planks)
                addBarPlank("crucible_L", Point2D(110f, 180f), Point2D(140f, 380f), 1, mysticPurple, listOf(c2, c5), holes, screws, planks)
                addBarPlank("crucible_R", Point2D(290f, 180f), Point2D(260f, 380f), 1, potionRuby, listOf(c3, c1), holes, screws, planks)
                addBarPlank("crucible_base", Point2D(140f, 380f), Point2D(260f, 380f), 2, elixirAmber, listOf(c4, c2), holes, screws, planks)
                addBarPlank("crucible_stir", Point2D(90f, 130f), Point2D(270f, 370f), 3, glassTeal, listOf(c5, c3), holes, screws, planks)
            }
        }
    }

    private fun buildEgyptianLevels(
        subLevel: Int,
        holes: MutableList<Hole>,
        screws: MutableList<Screw>,
        planks: MutableList<Plank>,
        colors: List<ScrewColor>,
        level: Int
    ) {
        val pharaohGold = 0xFFFFD700
        val lapisBlue = 0xFF1565C0
        val turquoise = 0xFF26A69A
        val terracotta = 0xFFD84315

        val c1 = colors[0]
        val c2 = colors[1 % colors.size]
        val c3 = colors[2 % colors.size]
        val c4 = colors[3 % colors.size]
        val c5 = colors[4 % colors.size]

        when (subLevel) {
            0 -> { // Golden Pyramid
                addBarPlank("pyr_base", Point2D(70f, 390f), Point2D(330f, 390f), 0, pharaohGold, listOf(c1, c2), holes, screws, planks)
                addBarPlank("pyr_left", Point2D(200f, 140f), Point2D(70f, 390f), 1, lapisBlue, listOf(c2, c3), holes, screws, planks)
                addBarPlank("pyr_right", Point2D(200f, 140f), Point2D(330f, 390f), 1, turquoise, listOf(c3, c4), holes, screws, planks)
                addBarPlank("pyr_mid", Point2D(135f, 265f), Point2D(265f, 265f), 2, terracotta, listOf(c4, c5), holes, screws, planks)
                addBarPlank("pyr_spine", Point2D(200f, 140f), Point2D(200f, 390f), 3, pharaohGold, listOf(c5, c1), holes, screws, planks)
            }
            1 -> { // Ankh of Life
                addRingPlank("ankh_head", Point2D(200f, 175f), 45f, listOf(0f, 90f, 180f, 270f), 0, pharaohGold, listOf(c1, c2, c3, c4), holes, screws, planks)
                addBarPlank("ankh_cross", Point2D(100f, 240f), Point2D(300f, 240f), 1, lapisBlue, listOf(c2, c5), holes, screws, planks)
                addBarPlank("ankh_stem", Point2D(200f, 220f), Point2D(200f, 410f), 2, turquoise, listOf(c3, c1), holes, screws, planks)
                addBarPlank("ankh_bracket_L", Point2D(120f, 240f), Point2D(160f, 330f), 3, terracotta, listOf(c4, c2), holes, screws, planks)
                addBarPlank("ankh_bracket_R", Point2D(280f, 240f), Point2D(240f, 330f), 3, pharaohGold, listOf(c5, c3), holes, screws, planks)
            }
            2 -> { // Scarab Brooch
                addRingPlank("scarab_body", Point2D(200f, 270f), 65f, listOf(30f, 90f, 150f, 210f, 270f, 330f), 0, lapisBlue, listOf(c1, c2, c3, c4, c5, c1), holes, screws, planks)
                addBarPlank("wing_TL", Point2D(200f, 270f), Point2D(80f, 160f), 1, pharaohGold, listOf(c2, c3), holes, screws, planks)
                addBarPlank("wing_TR", Point2D(200f, 270f), Point2D(320f, 160f), 1, pharaohGold, listOf(c4, c5), holes, screws, planks)
                addBarPlank("wing_BL", Point2D(200f, 270f), Point2D(80f, 380f), 2, turquoise, listOf(c3, c1), holes, screws, planks)
                addBarPlank("wing_BR", Point2D(200f, 270f), Point2D(320f, 380f), 2, terracotta, listOf(c5, c2), holes, screws, planks)
            }
            3 -> { // Eye of Horus
                addRingPlank("eye_pupil", Point2D(200f, 250f), 35f, listOf(0f, 180f), 0, lapisBlue, listOf(c1, c2), holes, screws, planks)
                addBarPlank("eye_upper", Point2D(90f, 250f), Point2D(310f, 250f), 1, pharaohGold, listOf(c3, c4), holes, screws, planks)
                addBarPlank("eye_arch_top", Point2D(100f, 240f), Point2D(200f, 170f), 2, turquoise, listOf(c2, c5), holes, screws, planks)
                addBarPlank("eye_arch_topR", Point2D(200f, 170f), Point2D(300f, 240f), 2, turquoise, listOf(c4, c1), holes, screws, planks)
                addBarPlank("eye_tear", Point2D(200f, 250f), Point2D(240f, 380f), 3, terracotta, listOf(c5, c3), holes, screws, planks)
            }
            else -> { // Level 35 Pharaoh's Gate Boss
                addBarPlank("gate_col_L", Point2D(100f, 140f), Point2D(100f, 410f), 0, pharaohGold, listOf(c1, c2), holes, screws, planks)
                addBarPlank("gate_col_R", Point2D(300f, 140f), Point2D(300f, 410f), 0, pharaohGold, listOf(c3, c4), holes, screws, planks)
                addBarPlank("gate_lintel", Point2D(60f, 150f), Point2D(340f, 150f), 1, lapisBlue, listOf(c2, c5), holes, screws, planks)
                addBarPlank("gate_cross1", Point2D(100f, 170f), Point2D(300f, 390f), 2, turquoise, listOf(c4, c1), holes, screws, planks)
                addBarPlank("gate_cross2", Point2D(300f, 170f), Point2D(100f, 390f), 2, terracotta, listOf(c5, c3), holes, screws, planks)
                addRingPlank("gate_seal", Point2D(200f, 280f), 45f, listOf(90f, 270f), 3, pharaohGold, listOf(c1, c4), holes, screws, planks)
            }
        }
    }

    private fun buildAtlantisLevels(
        subLevel: Int,
        holes: MutableList<Hole>,
        screws: MutableList<Screw>,
        planks: MutableList<Plank>,
        colors: List<ScrewColor>,
        level: Int
    ) {
        val coralCyan = 0xFF00E5FF
        val deepBlue = 0xFF0277BD
        val seaPearl = 0xFF80DEEA
        val abaloneTeal = 0xFF00897B

        val c1 = colors[0]
        val c2 = colors[1 % colors.size]
        val c3 = colors[2 % colors.size]
        val c4 = colors[3 % colors.size]
        val c5 = colors[4 % colors.size]
        val c6 = colors[5 % colors.size]

        when (subLevel) {
            0 -> { // Nautical Anchor
                addBarPlank("anchor_shank", Point2D(200f, 140f), Point2D(200f, 380f), 0, deepBlue, listOf(c1, c2), holes, screws, planks)
                addRingPlank("anchor_ring", Point2D(200f, 140f), 35f, listOf(0f, 180f), 1, seaPearl, listOf(c3, c4), holes, screws, planks)
                addBarPlank("anchor_stock", Point2D(110f, 200f), Point2D(290f, 200f), 2, coralCyan, listOf(c2, c5), holes, screws, planks)
                addBarPlank("anchor_fluke_L", Point2D(90f, 320f), Point2D(200f, 380f), 3, abaloneTeal, listOf(c4, c1), holes, screws, planks)
                addBarPlank("anchor_fluke_R", Point2D(310f, 320f), Point2D(200f, 380f), 3, abaloneTeal, listOf(c5, c3), holes, screws, planks)
            }
            1 -> { // Starfish Prism
                for (i in 0 until 5) {
                    val deg = i * 72f - 90f
                    val rad = deg * PI.toFloat() / 180f
                    val p1 = Point2D(200f, 270f)
                    val p2 = Point2D(200f + 120f * cos(rad), 270f + 120f * sin(rad))
                    addBarPlank("star_arm_$i", p1, p2, i % 3, if (i % 2 == 0) coralCyan else seaPearl, listOf(colors[i % colors.size], colors[(i + 1) % colors.size]), holes, screws, planks)
                }
                addRingPlank("star_hub", Point2D(200f, 270f), 40f, listOf(0f, 120f, 240f), 4, deepBlue, listOf(c4, c5, c6), holes, screws, planks)
            }
            2 -> { // Nautilus Spiral
                addRingPlank("naut_1", Point2D(200f, 270f), 120f, listOf(0f, 120f, 240f), 0, deepBlue, listOf(c1, c2, c3), holes, screws, planks)
                addRingPlank("naut_2", Point2D(210f, 260f), 80f, listOf(60f, 180f, 300f), 1, coralCyan, listOf(c4, c5, c6), holes, screws, planks)
                addRingPlank("naut_3", Point2D(220f, 250f), 45f, listOf(90f, 270f), 2, seaPearl, listOf(c2, c4), holes, screws, planks)
                addBarPlank("naut_cross", Point2D(100f, 270f), Point2D(300f, 270f), 3, abaloneTeal, listOf(c5, c1), holes, screws, planks)
            }
            3 -> { // Trident Cage
                addBarPlank("tri_stem", Point2D(200f, 200f), Point2D(200f, 410f), 0, deepBlue, listOf(c1, c2), holes, screws, planks)
                addBarPlank("tri_bar", Point2D(90f, 230f), Point2D(310f, 230f), 1, seaPearl, listOf(c3, c4), holes, screws, planks)
                addBarPlank("tri_prong_L", Point2D(90f, 140f), Point2D(90f, 230f), 2, coralCyan, listOf(c2, c5), holes, screws, planks)
                addBarPlank("tri_prong_M", Point2D(200f, 130f), Point2D(200f, 200f), 2, abaloneTeal, listOf(c4, c6), holes, screws, planks)
                addBarPlank("tri_prong_R", Point2D(310f, 140f), Point2D(310f, 230f), 2, coralCyan, listOf(c6, c1), holes, screws, planks)
                addBarPlank("tri_cage_diag", Point2D(90f, 230f), Point2D(310f, 380f), 3, deepBlue, listOf(c5, c3), holes, screws, planks)
            }
            else -> { // Level 40 Poseidon Crown Boss
                addRingPlank("crown_circlet", Point2D(200f, 310f), 110f, listOf(180f, 225f, 270f, 315f, 0f), 0, deepBlue, listOf(c1, c2, c3, c4, c5), holes, screws, planks)
                addBarPlank("crown_point_L", Point2D(90f, 310f), Point2D(130f, 150f), 1, coralCyan, listOf(c2, c6), holes, screws, planks)
                addBarPlank("crown_point_M", Point2D(200f, 310f), Point2D(200f, 130f), 1, abaloneTeal, listOf(c4, c1), holes, screws, planks)
                addBarPlank("crown_point_R", Point2D(310f, 310f), Point2D(270f, 150f), 1, seaPearl, listOf(c5, c3), holes, screws, planks)
                addBarPlank("crown_tie", Point2D(130f, 150f), Point2D(270f, 150f), 2, coralCyan, listOf(c6, c2), holes, screws, planks)
                addRingPlank("crown_jewel", Point2D(200f, 220f), 35f, listOf(45f, 225f), 3, deepBlue, listOf(c3, c5), holes, screws, planks)
            }
        }
    }

    private fun buildCandyLevels(
        subLevel: Int,
        holes: MutableList<Hole>,
        screws: MutableList<Screw>,
        planks: MutableList<Plank>,
        colors: List<ScrewColor>,
        level: Int
    ) {
        val bubblePink = 0xFFFF4081
        val candyMint = 0xFF69F0AE
        val lemonDrop = 0xFFFFD740
        val grapeJelly = 0xFFE040FB

        val c1 = colors[0]
        val c2 = colors[1 % colors.size]
        val c3 = colors[2 % colors.size]
        val c4 = colors[3 % colors.size]
        val c5 = colors[4 % colors.size]
        val c6 = colors[5 % colors.size]

        when (subLevel) {
            0 -> { // Lollipop Wheel
                addRingPlank("lolly_candy", Point2D(200f, 210f), 80f, listOf(0f, 60f, 120f, 180f, 240f, 300f), 0, bubblePink, listOf(c1, c2, c3, c4, c5, c6), holes, screws, planks)
                addBarPlank("lolly_stick", Point2D(200f, 210f), Point2D(200f, 410f), 1, candyMint, listOf(c2, c5), holes, screws, planks)
                addRingPlank("lolly_spiral", Point2D(200f, 210f), 45f, listOf(45f, 225f), 2, lemonDrop, listOf(c3, c1), holes, screws, planks)
            }
            1 -> { // Wafer Lattice (4x4 Criss-Cross)
                for (i in 0 until 3) {
                    val y = 180f + i * 80f
                    addBarPlank("wafer_h_$i", Point2D(80f, y), Point2D(320f, y), 0, if (i % 2 == 0) lemonDrop else grapeJelly, listOf(colors[i % colors.size], colors[(i + 1) % colors.size]), holes, screws, planks)
                }
                for (j in 0 until 3) {
                    val x = 110f + j * 90f
                    addBarPlank("wafer_v_$j", Point2D(x, 150f), Point2D(x, 390f), 1, if (j % 2 == 0) bubblePink else candyMint, listOf(colors[(j + 2) % colors.size], colors[(j + 3) % colors.size]), holes, screws, planks)
                }
            }
            2 -> { // Candy Pretzel
                addRingPlank("pretzel_L", Point2D(150f, 240f), 60f, listOf(90f, 180f, 270f), 0, bubblePink, listOf(c1, c2, c3), holes, screws, planks)
                addRingPlank("pretzel_R", Point2D(250f, 240f), 60f, listOf(90f, 0f, 270f), 0, candyMint, listOf(c4, c5, c6), holes, screws, planks)
                addBarPlank("pretzel_cross1", Point2D(100f, 320f), Point2D(280f, 180f), 1, grapeJelly, listOf(c2, c4), holes, screws, planks)
                addBarPlank("pretzel_cross2", Point2D(300f, 320f), Point2D(120f, 180f), 1, lemonDrop, listOf(c5, c1), holes, screws, planks)
            }
            3 -> { // Sugar Sandwich Grid
                addBarPlank("sand_top", Point2D(80f, 160f), Point2D(320f, 160f), 0, lemonDrop, listOf(c1, c2), holes, screws, planks)
                addBarPlank("sand_bot", Point2D(80f, 380f), Point2D(320f, 380f), 0, lemonDrop, listOf(c3, c4), holes, screws, planks)
                addBarPlank("sand_diag1", Point2D(80f, 160f), Point2D(320f, 380f), 1, bubblePink, listOf(c2, c5), holes, screws, planks)
                addBarPlank("sand_diag2", Point2D(320f, 160f), Point2D(80f, 380f), 1, candyMint, listOf(c4, c6), holes, screws, planks)
                addBarPlank("sand_mid", Point2D(80f, 270f), Point2D(320f, 270f), 2, grapeJelly, listOf(c5, c3), holes, screws, planks)
            }
            else -> { // Level 45 Gingerbread Keep Boss
                addBarPlank("keep_L", Point2D(100f, 160f), Point2D(100f, 400f), 0, lemonDrop, listOf(c1, c2), holes, screws, planks)
                addBarPlank("keep_R", Point2D(300f, 160f), Point2D(300f, 400f), 0, lemonDrop, listOf(c3, c4), holes, screws, planks)
                addBarPlank("keep_roof_L", Point2D(80f, 180f), Point2D(200f, 130f), 1, bubblePink, listOf(c2, c5), holes, screws, planks)
                addBarPlank("keep_roof_R", Point2D(200f, 130f), Point2D(320f, 180f), 1, candyMint, listOf(c4, c6), holes, screws, planks)
                addBarPlank("keep_beam", Point2D(100f, 280f), Point2D(300f, 280f), 2, grapeJelly, listOf(c5, c1), holes, screws, planks)
                addRingPlank("keep_crest", Point2D(200f, 280f), 45f, listOf(45f, 135f, 225f, 315f), 3, lemonDrop, listOf(c6, c2, c3, c5), holes, screws, planks)
            }
        }
    }

    private fun buildCosmicLevels(
        subLevel: Int,
        holes: MutableList<Hole>,
        screws: MutableList<Screw>,
        planks: MutableList<Plank>,
        colors: List<ScrewColor>,
        level: Int
    ) {
        val starlightViolet = 0xFFE040FB
        val quantumCyan = 0xFF18FFFF
        val supernovaGold = 0xFFFFD700
        val nebulaRose = 0xFFFF4081

        val c1 = colors[0]
        val c2 = colors[1 % colors.size]
        val c3 = colors[2 % colors.size]
        val c4 = colors[3 % colors.size]
        val c5 = colors[4 % colors.size]
        val c6 = colors[5 % colors.size]
        val c7 = colors[6 % colors.size]

        when (subLevel) {
            0 -> { // Orbital Satellite Ring
                addRingPlank("orbit_ring", Point2D(200f, 270f), 120f, listOf(0f, 60f, 120f, 180f, 240f, 300f), 0, starlightViolet, listOf(c1, c2, c3, c4, c5, c6), holes, screws, planks)
                addBarPlank("sat_beam_1", Point2D(80f, 270f), Point2D(320f, 270f), 1, quantumCyan, listOf(c2, c5), holes, screws, planks)
                addBarPlank("sat_beam_2", Point2D(200f, 150f), Point2D(200f, 390f), 1, nebulaRose, listOf(c4, c7), holes, screws, planks)
                addRingPlank("sat_core", Point2D(200f, 270f), 40f, listOf(45f, 225f), 2, supernovaGold, listOf(c6, c1), holes, screws, planks)
            }
            1 -> { // Pulsar Starburst
                for (i in 0 until 6) {
                    val deg = i * 60f
                    val rad = deg * PI.toFloat() / 180f
                    val p1 = Point2D(200f, 270f)
                    val p2 = Point2D(200f + 125f * cos(rad), 270f + 125f * sin(rad))
                    addBarPlank("pulsar_$i", p1, p2, i % 3, if (i % 2 == 0) quantumCyan else nebulaRose, listOf(colors[i % colors.size], colors[(i + 1) % colors.size]), holes, screws, planks)
                }
                addRingPlank("pulsar_eye", Point2D(200f, 270f), 45f, listOf(0f, 120f, 240f), 3, supernovaGold, listOf(c3, c5, c7), holes, screws, planks)
            }
            2 -> { // Hypercube Tesseract
                addBarPlank("tess_top", Point2D(100f, 170f), Point2D(300f, 170f), 0, starlightViolet, listOf(c1, c2), holes, screws, planks)
                addBarPlank("tess_bot", Point2D(100f, 370f), Point2D(300f, 370f), 0, starlightViolet, listOf(c3, c4), holes, screws, planks)
                addBarPlank("tess_L", Point2D(100f, 170f), Point2D(100f, 370f), 0, quantumCyan, listOf(c2, c5), holes, screws, planks)
                addBarPlank("tess_R", Point2D(300f, 170f), Point2D(300f, 370f), 0, quantumCyan, listOf(c4, c6), holes, screws, planks)
                addRingPlank("tess_inner", Point2D(200f, 270f), 55f, listOf(45f, 135f, 225f, 315f), 1, supernovaGold, listOf(c5, c7, c1, c3), holes, screws, planks)
                addBarPlank("tess_diag1", Point2D(100f, 170f), Point2D(300f, 370f), 2, nebulaRose, listOf(c6, c2), holes, screws, planks)
                addBarPlank("tess_diag2", Point2D(300f, 170f), Point2D(100f, 370f), 2, nebulaRose, listOf(c7, c4), holes, screws, planks)
            }
            3 -> { // Infinity Coil
                addRingPlank("inf_L", Point2D(140f, 270f), 65f, listOf(0f, 90f, 180f, 270f), 0, quantumCyan, listOf(c1, c2, c3, c4), holes, screws, planks)
                addRingPlank("inf_R", Point2D(260f, 270f), 65f, listOf(0f, 90f, 180f, 270f), 0, nebulaRose, listOf(c4, c5, c6, c7), holes, screws, planks)
                addBarPlank("inf_cross1", Point2D(80f, 210f), Point2D(320f, 330f), 1, supernovaGold, listOf(c2, c6), holes, screws, planks)
                addBarPlank("inf_cross2", Point2D(80f, 330f), Point2D(320f, 210f), 1, starlightViolet, listOf(c5, c1), holes, screws, planks)
                addBarPlank("inf_core", Point2D(140f, 270f), Point2D(260f, 270f), 2, quantumCyan, listOf(c3, c7), holes, screws, planks)
            }
            else -> { // Level 50 FINAL BOSS: Cosmic Singularity Master
                addRingPlank("sing_outer", Point2D(200f, 270f), 135f, listOf(0f, 45f, 90f, 135f, 180f, 225f, 270f, 315f), 0, starlightViolet, listOf(c1, c2, c3, c4, c5, c6, c7, c1), holes, screws, planks)
                addRingPlank("sing_mid", Point2D(200f, 270f), 85f, listOf(30f, 90f, 150f, 210f, 270f, 330f), 1, quantumCyan, listOf(c2, c4, c6, c1, c3, c5), holes, screws, planks)
                addBarPlank("sing_x1", Point2D(90f, 160f), Point2D(310f, 380f), 2, supernovaGold, listOf(c3, c7), holes, screws, planks)
                addBarPlank("sing_x2", Point2D(310f, 160f), Point2D(90f, 380f), 2, nebulaRose, listOf(c5, c2), holes, screws, planks)
                addBarPlank("sing_h", Point2D(70f, 270f), Point2D(330f, 270f), 3, quantumCyan, listOf(c6, c4), holes, screws, planks)
                addBarPlank("sing_v", Point2D(200f, 140f), Point2D(200f, 400f), 3, starlightViolet, listOf(c7, c1), holes, screws, planks)
                addRingPlank("sing_center", Point2D(200f, 270f), 35f, listOf(0f, 180f), 4, supernovaGold, listOf(c1, c5), holes, screws, planks, width = 20f)
            }
        }
    }
}

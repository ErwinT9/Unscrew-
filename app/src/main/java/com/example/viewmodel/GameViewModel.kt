package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.audio.HapticManager
import com.example.audio.SoundManager
import com.example.data.GamePreferences
import com.example.model.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ActiveFlight(
    val screwId: String,
    val color: ScrewColor,
    val startX: Float,
    val startY: Float,
    val targetX: Float,
    val targetY: Float,
    val progress: Float = 0f
)

data class GameUiState(
    val currentLevel: Int = 1,
    val levelData: LevelData? = null,
    val timeRemaining: Int = 90,
    val isTimerFrozen: Boolean = false,
    val freezeSecondsLeft: Int = 0,
    val isPaused: Boolean = false,
    val isVictory: Boolean = false,
    val isGameOver: Boolean = false,
    val starsEarned: Int = 0,
    val coinsEarned: Int = 0,
    val totalCoins: Int = 0,
    val totalStars: Int = 0,
    val unlockedLevel: Int = 1,
    val comboStreak: Int = 0,
    val comboMessage: String? = null,
    val activeBoxes: List<ScrewBox> = emptyList(),
    val queuedBoxes: List<ScrewBox> = emptyList(),
    val holes: List<Hole> = emptyList(),
    val screws: List<Screw> = emptyList(),
    val planks: List<Plank> = emptyList(),
    val activeFlights: List<ActiveFlight> = emptyList(),
    val toolInventory: Map<ToolType, Int> = emptyMap(),
    val selectedToolForTarget: ToolType? = null,
    val messageBanner: String? = null
)

class GameViewModel(application: Application) : AndroidViewModel(application) {
    val prefs = GamePreferences(application)
    val soundManager = SoundManager(application)
    val hapticManager = HapticManager(application)

    private val _uiState = MutableStateFlow(GameUiState())
    val uiState: StateFlow<GameUiState> = _uiState.asStateFlow()

    private var timerJob: Job? = null
    private var physicsJob: Job? = null
    private var previousMovesStack = mutableListOf<MoveHistory>()

    data class MoveHistory(
        val screwId: String,
        val originHoleId: String,
        val originPos: Point2D,
        val targetType: String, // "BOX" or "FREE_HOLE"
        val targetId: String
    )

    init {
        soundManager.isSoundEnabled = prefs.isSoundEnabled()
        hapticManager.isHapticsEnabled = prefs.isHapticsEnabled()
        refreshMetaState()
        loadLevel(prefs.getUnlockedLevel())
    }

    fun refreshMetaState() {
        val toolsMap = ToolType.values().associateWith { prefs.getToolCount(it) }
        _uiState.value = _uiState.value.copy(
            totalCoins = prefs.getCoins(),
            totalStars = prefs.getTotalStars(),
            unlockedLevel = prefs.getUnlockedLevel(),
            toolInventory = toolsMap
        )
    }

    fun loadLevel(levelNum: Int) {
        timerJob?.cancel()
        physicsJob?.cancel()
        previousMovesStack.clear()

        val rawLevel = LevelGenerator.getLevel(levelNum)
        val allBoxes = rawLevel.targetBoxes.map { it.copy(filledScrews = it.filledScrews.toMutableList()) }
        val activeBoxes = allBoxes.take(4)
        val queuedBoxes = allBoxes.drop(4)

        _uiState.value = _uiState.value.copy(
            currentLevel = levelNum,
            levelData = rawLevel,
            timeRemaining = rawLevel.timeLimitSeconds,
            isTimerFrozen = false,
            freezeSecondsLeft = 0,
            isPaused = false,
            isVictory = false,
            isGameOver = false,
            starsEarned = 0,
            coinsEarned = 0,
            comboStreak = 0,
            comboMessage = null,
            activeBoxes = activeBoxes,
            queuedBoxes = queuedBoxes,
            holes = rawLevel.holes.map { it.copy() },
            screws = rawLevel.screws.map { it.copy() },
            planks = rawLevel.planks.map { it.copy() },
            activeFlights = emptyList(),
            selectedToolForTarget = null,
            messageBanner = null
        )

        refreshMetaState()
        startTimer()
        startPhysicsLoop()
    }

    private fun startTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (true) {
                delay(1000)
                val state = _uiState.value
                if (state.isPaused || state.isVictory || state.isGameOver) continue

                if (state.isTimerFrozen) {
                    val remainingFreeze = state.freezeSecondsLeft - 1
                    if (remainingFreeze <= 0) {
                        _uiState.value = _uiState.value.copy(
                            isTimerFrozen = false,
                            freezeSecondsLeft = 0
                        )
                    } else {
                        _uiState.value = _uiState.value.copy(freezeSecondsLeft = remainingFreeze)
                    }
                } else {
                    val nextTime = state.timeRemaining - 1
                    if (nextTime <= 0) {
                        _uiState.value = _uiState.value.copy(timeRemaining = 0, isGameOver = true)
                        soundManager.playGameOver()
                        hapticManager.heavy()
                        break
                    } else {
                        _uiState.value = _uiState.value.copy(timeRemaining = nextTime)
                    }
                }
            }
        }
    }

    private fun startPhysicsLoop() {
        physicsJob?.cancel()
        physicsJob = viewModelScope.launch {
            while (true) {
                delay(16) // ~60fps
                updatePhysicsAndAnimations()
            }
        }
    }

    private fun updatePhysicsAndAnimations() {
        val state = _uiState.value
        if (state.planks.none { it.isFalling } && state.activeFlights.isEmpty()) return

        // Update flights
        val updatedFlights = mutableListOf<ActiveFlight>()
        state.activeFlights.forEach { flight ->
            val nextProg = flight.progress + 0.08f
            if (nextProg < 1f) {
                updatedFlights.add(flight.copy(progress = nextProg))
            }
        }

        // Update falling planks
        var anyPlankDroppedOff = false
        val updatedPlanks = state.planks.map { plank ->
            if (plank.isFalling && !plank.isCleared) {
                val nextOffsetY = plank.fallOffsetY + 18f
                val nextRot = plank.fallRotation + 3.5f
                val nextAlpha = (plank.fallAlpha - 0.035f).coerceAtLeast(0f)
                if (nextOffsetY > 700f || nextAlpha <= 0f) {
                    anyPlankDroppedOff = true
                    plank.copy(isCleared = true, fallOffsetY = nextOffsetY, fallAlpha = 0f)
                } else {
                    plank.copy(fallOffsetY = nextOffsetY, fallRotation = nextRot, fallAlpha = nextAlpha)
                }
            } else {
                plank
            }
        }

        _uiState.value = _uiState.value.copy(
            activeFlights = updatedFlights,
            planks = updatedPlanks
        )

        if (anyPlankDroppedOff) {
            checkWinCondition()
        }
    }

    fun onScrewTapped(screw: Screw) {
        val state = _uiState.value
        if (state.isPaused || state.isVictory || state.isGameOver || screw.isCleared || screw.isUnscrewing || screw.isFlying) return

        // Check if hammer tool is currently active
        if (state.selectedToolForTarget == ToolType.HAMMER) {
            applyHammerOnScrew(screw)
            return
        }

        soundManager.playClick()
        hapticManager.tick()

        // 1. Try to find an active box matching the screw color
        val matchingBoxIdx = state.activeBoxes.indexOfFirst { it.color == screw.color && !it.isFull }

        if (matchingBoxIdx != -1) {
            // MATCHED TO ACTIVE BOX!
            val targetBox = state.activeBoxes[matchingBoxIdx]
            processScrewToBox(screw, targetBox, matchingBoxIdx)
        } else {
            // 2. No matching box -> Check for an empty free spare hole!
            val emptyHole = state.holes.find { it.isReservedSpare && it.isUnlocked && it.occupiedScrewId == null }
            if (emptyHole != null) {
                processScrewToFreeHole(screw, emptyHole)
            } else {
                // NO ROOM!
                showBanner("⚠️ No free slots or matching box! Use a Drill or clear top boxes.")
                hapticManager.medium()
            }
        }
    }

    private fun processScrewToBox(screw: Screw, targetBox: ScrewBox, boxIdx: Int) {
        soundManager.playUnscrew()
        hapticManager.medium()

        // Calculate visual targets
        val targetBoxX = 50f + boxIdx * 80f
        val targetBoxY = -60f // Above canvas

        // Save move history for Undo
        previousMovesStack.add(
            MoveHistory(
                screwId = screw.id,
                originHoleId = screw.holeId,
                originPos = Point2D(screw.x, screw.y),
                targetType = "BOX",
                targetId = targetBox.id
            )
        )

        // Free origin hole
        val updatedHoles = _uiState.value.holes.map { h ->
            if (h.id == screw.holeId) h.copy(occupiedScrewId = null) else h
        }

        // Add flight animation
        val flight = ActiveFlight(
            screwId = screw.id,
            color = screw.color,
            startX = screw.x,
            startY = screw.y,
            targetX = targetBoxX,
            targetY = targetBoxY
        )

        // Update target box
        val newFilled = targetBox.filledScrews.toMutableList()
        newFilled.add(screw.id)
        val isBoxNowFull = newFilled.size >= targetBox.capacity
        val updatedBox = targetBox.copy(
            filledScrews = newFilled,
            isFull = isBoxNowFull
        )

        var newActiveBoxes = _uiState.value.activeBoxes.toMutableList()
        var newQueuedBoxes = _uiState.value.queuedBoxes.toMutableList()

        if (isBoxNowFull) {
            soundManager.playBoxMatch()
            hapticManager.heavy()
            // Replace full box with next queue box
            newActiveBoxes[boxIdx] = updatedBox
            viewModelScope.launch {
                delay(250)
                if (newQueuedBoxes.isNotEmpty()) {
                    val nextInQueue = newQueuedBoxes.removeAt(0)
                    newActiveBoxes[boxIdx] = nextInQueue
                } else {
                    newActiveBoxes.removeAt(boxIdx)
                }
                _uiState.value = _uiState.value.copy(
                    activeBoxes = newActiveBoxes.toList(),
                    queuedBoxes = newQueuedBoxes.toList()
                )
                checkWinCondition()
            }
        } else {
            newActiveBoxes[boxIdx] = updatedBox
        }

        // Update screws
        val updatedScrews = _uiState.value.screws.map { sc ->
            if (sc.id == screw.id) sc.copy(isCleared = true, isFlying = true) else sc
        }

        // Combo system
        val nextStreak = _uiState.value.comboStreak + 1
        val comboMsg = if (nextStreak >= 2) "Combo x$nextStreak! 🔥" else null

        _uiState.value = _uiState.value.copy(
            holes = updatedHoles,
            screws = updatedScrews,
            activeBoxes = newActiveBoxes,
            queuedBoxes = newQueuedBoxes,
            activeFlights = _uiState.value.activeFlights + flight,
            comboStreak = nextStreak,
            comboMessage = comboMsg
        )

        // Trigger physics check for planks!
        evaluatePlankPhysics(updatedHoles)
    }

    private fun processScrewToFreeHole(screw: Screw, targetHole: Hole) {
        soundManager.playUnscrew()
        hapticManager.tick()

        previousMovesStack.add(
            MoveHistory(
                screwId = screw.id,
                originHoleId = screw.holeId,
                originPos = Point2D(screw.x, screw.y),
                targetType = "FREE_HOLE",
                targetId = targetHole.id
            )
        )

        // Free origin hole & occupy target hole
        val updatedHoles = _uiState.value.holes.map { h ->
            when (h.id) {
                screw.holeId -> h.copy(occupiedScrewId = null)
                targetHole.id -> h.copy(occupiedScrewId = screw.id)
                else -> h
            }
        }

        // Move screw to free hole position
        val updatedScrews = _uiState.value.screws.map { sc ->
            if (sc.id == screw.id) sc.copy(x = targetHole.x, y = targetHole.y, holeId = targetHole.id) else sc
        }

        _uiState.value = _uiState.value.copy(
            holes = updatedHoles,
            screws = updatedScrews
        )

        evaluatePlankPhysics(updatedHoles)
    }

    private fun evaluatePlankPhysics(currentHoles: List<Hole>) {
        val occupiedHoleIds = currentHoles.filter { it.occupiedScrewId != null }.map { it.id }.toSet()
        var plankDropped = false

        val updatedPlanks = _uiState.value.planks.map { plank ->
            if (!plank.isFalling && !plank.isCleared) {
                val remainingHoldingScrews = plank.holeIds.count { it in occupiedHoleIds }
                if (remainingHoldingScrews == 0) {
                    plankDropped = true
                    plank.copy(isFalling = true)
                } else {
                    plank
                }
            } else {
                plank
            }
        }

        if (plankDropped) {
            soundManager.playPlankDrop()
            hapticManager.medium()
        }

        _uiState.value = _uiState.value.copy(planks = updatedPlanks)
        checkWinCondition()
    }

    private fun checkWinCondition() {
        val state = _uiState.value
        val allScrewsCleared = state.screws.all { it.isCleared }
        val allPlanksCleared = state.planks.all { it.isCleared || it.isFalling }

        if ((allScrewsCleared || allPlanksCleared) && !state.isVictory) {
            val lvl = state.currentLevel
            val levelData = state.levelData ?: return
            
            // Calculate Stars
            val timeBonus = state.timeRemaining
            val stars = when {
                timeBonus >= levelData.star3TimeRemaining -> 3
                timeBonus >= levelData.star2TimeRemaining -> 2
                else -> 1
            }

            val earnedCoins = levelData.coinReward + (stars * 20) + (timeBonus * 2)
            prefs.addCoins(earnedCoins)
            prefs.setStarsForLevel(lvl, stars)
            if (lvl < 50) {
                prefs.setUnlockedLevel(lvl + 1)
            }

            soundManager.playVictory()
            hapticManager.heavy()

            _uiState.value = _uiState.value.copy(
                isVictory = true,
                starsEarned = stars,
                coinsEarned = earnedCoins
            )
            refreshMetaState()
        }
    }

    // --- POWER-UP TOOLS ---

    fun activateTool(tool: ToolType) {
        val count = prefs.getToolCount(tool)
        if (count <= 0) {
            showBanner("You don't have any ${tool.title}! Purchase more in the Shop.")
            return
        }

        when (tool) {
            ToolType.DRILL -> {
                if (prefs.useTool(tool)) {
                    soundManager.playToolUse()
                    hapticManager.heavy()
                    // Add a new reserved free hole on the shelf
                    val currentFreeHoles = _uiState.value.holes.filter { it.isReservedSpare }
                    val newIdx = currentFreeHoles.size
                    val newX = 40f + newIdx * 48f
                    val newHole = Hole(id = "extra_drill_hole_$newIdx", x = newX, y = 35f, isReservedSpare = true)
                    _uiState.value = _uiState.value.copy(
                        holes = _uiState.value.holes + newHole,
                        messageBanner = "Drill deployed! Extra parking slot added."
                    )
                    refreshMetaState()
                }
            }
            ToolType.AUTO_SCREW -> {
                // Find first screw on board that can match an active box
                val matchScrew = _uiState.value.screws.firstOrNull { sc ->
                    !sc.isCleared && _uiState.value.activeBoxes.any { it.color == sc.color && !it.isFull }
                } ?: _uiState.value.screws.firstOrNull { !it.isCleared }

                if (matchScrew != null && prefs.useTool(tool)) {
                    soundManager.playToolUse()
                    onScrewTapped(matchScrew)
                    showBanner("⚡ Electric Screwdriver auto-unscrewed a pin!")
                    refreshMetaState()
                } else {
                    showBanner("No valid screw to auto-unscrew!")
                }
            }
            ToolType.FREEZE_TIME -> {
                if (prefs.useTool(tool)) {
                    soundManager.playToolUse()
                    hapticManager.heavy()
                    _uiState.value = _uiState.value.copy(
                        isTimerFrozen = true,
                        freezeSecondsLeft = 20,
                        messageBanner = "⏳ Time Frozen for 20 seconds!"
                    )
                    refreshMetaState()
                }
            }
            ToolType.MAGNET -> {
                // Collect up to 3 matching screws
                val activeColors = _uiState.value.activeBoxes.filter { !it.isFull }.map { it.color }.toSet()
                val targetScrews = _uiState.value.screws.filter { !it.isCleared && it.color in activeColors }.take(3)
                if (targetScrews.isNotEmpty() && prefs.useTool(tool)) {
                    soundManager.playToolUse()
                    hapticManager.heavy()
                    viewModelScope.launch {
                        targetScrews.forEachIndexed { i, sc ->
                            delay(i * 120L)
                            onScrewTapped(sc)
                        }
                    }
                    showBanner("🧲 Magnetic Pull gathered ${targetScrews.size} screws!")
                    refreshMetaState()
                } else {
                    showBanner("No active box matches found on board for Magnet!")
                }
            }
            ToolType.HAMMER -> {
                // Put user into target selection mode or auto shatter top plank
                val topPlank = _uiState.value.planks.filter { !it.isFalling && !it.isCleared }.maxByOrNull { it.zIndex }
                if (topPlank != null && prefs.useTool(tool)) {
                    soundManager.playToolUse()
                    hapticManager.heavy()
                    // Shatter plank
                    val updatedPlanks = _uiState.value.planks.map {
                        if (it.id == topPlank.id) it.copy(isFalling = true) else it
                    }
                    _uiState.value = _uiState.value.copy(
                        planks = updatedPlanks,
                        messageBanner = "🔨 Hammer shattered an obstructive plank!"
                    )
                    refreshMetaState()
                } else {
                    showBanner("No planks left to break!")
                }
            }
            ToolType.UNDO -> {
                if (previousMovesStack.isNotEmpty() && prefs.useTool(tool)) {
                    soundManager.playToolUse()
                    hapticManager.medium()
                    val lastMove = previousMovesStack.removeAt(previousMovesStack.lastIndex)
                    undoMove(lastMove)
                    showBanner("↩️ Move reversed!")
                    refreshMetaState()
                } else {
                    showBanner("No previous move to undo!")
                }
            }
        }
    }

    private fun undoMove(move: MoveHistory) {
        // Return screw to original hole
        val updatedHoles = _uiState.value.holes.map { h ->
            when (h.id) {
                move.originHoleId -> h.copy(occupiedScrewId = move.screwId)
                move.targetId -> if (move.targetType == "FREE_HOLE") h.copy(occupiedScrewId = null) else h
                else -> h
            }
        }

        val updatedScrews = _uiState.value.screws.map { sc ->
            if (sc.id == move.screwId) sc.copy(
                x = move.originPos.x,
                y = move.originPos.y,
                holeId = move.originHoleId,
                isCleared = false,
                isFlying = false
            ) else sc
        }

        // If it was in a box, remove from box
        val updatedActiveBoxes = _uiState.value.activeBoxes.map { box ->
            if (box.id == move.targetId) {
                val list = box.filledScrews.toMutableList()
                list.remove(move.screwId)
                box.copy(filledScrews = list, isFull = false)
            } else box
        }

        _uiState.value = _uiState.value.copy(
            holes = updatedHoles,
            screws = updatedScrews,
            activeBoxes = updatedActiveBoxes
        )

        evaluatePlankPhysics(updatedHoles)
    }

    private fun applyHammerOnScrew(screw: Screw) {
        _uiState.value = _uiState.value.copy(selectedToolForTarget = null)
        onScrewTapped(screw)
    }

    fun buyTool(tool: ToolType): Boolean {
        if (prefs.spendCoins(tool.coinCost)) {
            prefs.addTool(tool, 1)
            soundManager.playClick()
            hapticManager.tick()
            refreshMetaState()
            return true
        }
        return false
    }

    fun showBanner(message: String) {
        _uiState.value = _uiState.value.copy(messageBanner = message)
        viewModelScope.launch {
            delay(2800)
            if (_uiState.value.messageBanner == message) {
                _uiState.value = _uiState.value.copy(messageBanner = null)
            }
        }
    }

    fun pauseGame() {
        _uiState.value = _uiState.value.copy(isPaused = true)
    }

    fun resumeGame() {
        _uiState.value = _uiState.value.copy(isPaused = false)
    }

    fun restartCurrentLevel() {
        loadLevel(_uiState.value.currentLevel)
    }

    fun nextLevel() {
        val next = (_uiState.value.currentLevel + 1).coerceAtMost(50)
        loadLevel(next)
    }

    fun toggleSound(): Boolean {
        val next = !prefs.isSoundEnabled()
        prefs.setSoundEnabled(next)
        soundManager.isSoundEnabled = next
        return next
    }

    fun toggleHaptics(): Boolean {
        val next = !prefs.isHapticsEnabled()
        prefs.setHapticsEnabled(next)
        hapticManager.isHapticsEnabled = next
        return next
    }

    fun purchaseTool(tool: ToolType, cost: Int): Boolean {
        if (prefs.spendCoins(cost)) {
            prefs.addTool(tool, 1)
            soundManager.playToolUse()
            hapticManager.medium()
            refreshMetaState()
            showBanner("Purchased 1x ${tool.title}!")
            return true
        } else {
            soundManager.playClick()
            hapticManager.tick()
            showBanner("Not enough coins!")
            return false
        }
    }

    fun claimDailyReward(): Boolean {
        prefs.addCoins(100)
        soundManager.playBoxMatch()
        hapticManager.medium()
        refreshMetaState()
        showBanner("Claimed +100 Daily 🪙!")
        return true
    }
}

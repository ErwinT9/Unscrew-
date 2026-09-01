package com.example.data

import android.content.Context
import android.content.SharedPreferences
import com.example.model.ToolType
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class GamePreferences(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("unscrew_game_prefs", Context.MODE_PRIVATE)

    fun getUnlockedLevel(): Int {
        return prefs.getInt("unlocked_level", 1)
    }

    fun setUnlockedLevel(level: Int) {
        val current = getUnlockedLevel()
        if (level > current) {
            prefs.edit().putInt("unlocked_level", level.coerceIn(1, 100)).apply()
        }
    }

    fun getStarsForLevel(level: Int): Int {
        return prefs.getInt("stars_level_$level", 0)
    }

    fun setStarsForLevel(level: Int, stars: Int) {
        val current = getStarsForLevel(level)
        if (stars > current) {
            prefs.edit().putInt("stars_level_$level", stars.coerceIn(0, 3)).apply()
        }
    }

    fun getTotalStars(): Int {
        var total = 0
        for (i in 1..100) {
            total += getStarsForLevel(i)
        }
        return total
    }

    fun getCoins(): Int {
        return prefs.getInt("coins_balance", 0) // Starts at 0 for new installs
    }

    fun addCoins(amount: Int) {
        val current = getCoins()
        prefs.edit().putInt("coins_balance", (current + amount).coerceAtLeast(0)).apply()
    }

    fun spendCoins(amount: Int): Boolean {
        val current = getCoins()
        if (current >= amount) {
            prefs.edit().putInt("coins_balance", current - amount).apply()
            return true
        }
        return false
    }

    fun getToolCount(tool: ToolType): Int {
        // Starting tool count: 1 free undo wrench, 0 for specialty armory tools
        val defaultCount = if (tool == ToolType.UNDO) 1 else 0
        return prefs.getInt("tool_count_${tool.name}", defaultCount)
    }

    fun addTool(tool: ToolType, count: Int = 1) {
        val current = getToolCount(tool)
        prefs.edit().putInt("tool_count_${tool.name}", (current + count).coerceAtLeast(0)).apply()
    }

    fun useTool(tool: ToolType): Boolean {
        val current = getToolCount(tool)
        if (current > 0) {
            prefs.edit().putInt("tool_count_${tool.name}", current - 1).apply()
            return true
        }
        return false
    }

    private fun getTodayDateString(): String {
        return SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
    }

    fun canClaimDaily(): Boolean {
        val lastClaimed = prefs.getString("last_daily_claim_date", "") ?: ""
        return lastClaimed != getTodayDateString()
    }

    fun markDailyClaimed() {
        prefs.edit().putString("last_daily_claim_date", getTodayDateString()).apply()
    }

    fun isSoundEnabled(): Boolean {
        return prefs.getBoolean("sound_enabled", true)
    }

    fun setSoundEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("sound_enabled", enabled).apply()
    }

    fun isHapticsEnabled(): Boolean {
        return prefs.getBoolean("haptics_enabled", true)
    }

    fun setHapticsEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("haptics_enabled", enabled).apply()
    }

    fun resetAllProgress() {
        prefs.edit().clear().apply()
    }
}

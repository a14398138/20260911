package com.example.game

import android.content.Context
import android.content.SharedPreferences

class GamePreferences(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("stellar_strike_prefs", Context.MODE_PRIVATE)

    var highScore: Int
        get() = prefs.getInt("key_high_score", 0)
        set(value) = prefs.edit().putInt("key_high_score", value).apply()

    var totalCrystals: Int
        get() = prefs.getInt("key_total_crystals", 50) // start with 50 crystals for a friendly head start
        set(value) = prefs.edit().putInt("key_total_crystals", value).apply()

    var selectedShip: ShipType
        get() {
            val name = prefs.getString("key_selected_ship", ShipType.VOID_STRIKER.name)
            return try {
                ShipType.valueOf(name ?: ShipType.VOID_STRIKER.name)
            } catch (_: Exception) {
                ShipType.VOID_STRIKER
            }
        }
        set(value) = prefs.edit().putString("key_selected_ship", value.name).apply()

    var shieldUpgradeLevel: Int
        get() = prefs.getInt("key_shield_upgrade", 1)
        set(value) = prefs.edit().putInt("key_shield_upgrade", value).apply()

    var blasterUpgradeLevel: Int
        get() = prefs.getInt("key_blaster_upgrade", 1)
        set(value) = prefs.edit().putInt("key_blaster_upgrade", value).apply()

    var fireRateUpgradeLevel: Int
        get() = prefs.getInt("key_firerate_upgrade", 1)
        set(value) = prefs.edit().putInt("key_firerate_upgrade", value).apply()

    var bombChargeUpgradeLevel: Int
        get() = prefs.getInt("key_bomb_upgrade", 1)
        set(value) = prefs.edit().putInt("key_bomb_upgrade", value).apply()

    var isSoundEnabled: Boolean
        get() = prefs.getBoolean("key_sound_enabled", true)
        set(value) = prefs.edit().putBoolean("key_sound_enabled", value).apply()

    var isHapticsEnabled: Boolean
        get() = prefs.getBoolean("key_haptics_enabled", true)
        set(value) = prefs.edit().putBoolean("key_haptics_enabled", value).apply()

    fun isShipUnlocked(ship: ShipType): Boolean {
        if (ship.unlockCost == 0) return true
        val set = prefs.getStringSet("key_unlocked_ships", null) ?: emptySet()
        return set.contains(ship.name)
    }

    fun unlockShip(ship: ShipType) {
        val set = prefs.getStringSet("key_unlocked_ships", null)?.toMutableSet() ?: mutableSetOf()
        set.add(ship.name)
        prefs.edit().putStringSet("key_unlocked_ships", set).apply()
    }
}

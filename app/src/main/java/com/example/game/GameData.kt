package com.example.game

import androidx.compose.ui.graphics.Color

enum class ShipType(
    val title: String,
    val description: String,
    val primaryColor: Color,
    val accentColor: Color,
    val hpMultiplier: Float,
    val fireRateMultiplier: Float,
    val damageMultiplier: Float,
    val unlockCost: Int
) {
    VOID_STRIKER(
        title = "Void Striker",
        description = "Balanced multi-role fighter. High agility and rapid twin plasma cannons.",
        primaryColor = Color(0xFF00E5FF),
        accentColor = Color(0xFF0077B6),
        hpMultiplier = 1.0f,
        fireRateMultiplier = 1.0f,
        damageMultiplier = 1.0f,
        unlockCost = 0
    ),
    PHOENIX_AEGIS(
        title = "Phoenix Aegis",
        description = "Heavy armored interceptor. Massive shield capacity with plasma blast waves.",
        primaryColor = Color(0xFFFF5722),
        accentColor = Color(0xFFFFD600),
        hpMultiplier = 1.5f,
        fireRateMultiplier = 0.85f,
        damageMultiplier = 1.25f,
        unlockCost = 150
    ),
    QUANTUM_PHANTOM(
        title = "Quantum Phantom",
        description = "Experimental warp ship. Hyper-speed fire rate and rapid bomb charging.",
        primaryColor = Color(0xFFE040FB),
        accentColor = Color(0xFF7C4DFF),
        hpMultiplier = 0.85f,
        fireRateMultiplier = 1.35f,
        damageMultiplier = 1.1f,
        unlockCost = 350
    )
}

enum class GameState {
    MENU,
    PLAYING,
    PAUSED,
    GAME_OVER,
    VICTORY,
    HANGAR,
    HOW_TO_PLAY
}

enum class EnemyType {
    DRONE,
    ASTEROID,
    SCOUT,
    HEAVY_CRUISER,
    NEBULA_BOSS
}

enum class PowerUpType(val symbol: String, val color: Color, val label: String) {
    TRIPLE_SHOT("⚡", Color(0xFF00E5FF), "Triple Laser"),
    RAPID_FIRE("🔥", Color(0xFFFF9100), "Hyper Fire"),
    SHIELD_RECHARGE("🛡️", Color(0xFF00E676), "Shield Boost"),
    QUANTUM_BOMB("💣", Color(0xFFFF1744), "Nova Bomb"),
    STAR_CRYSTAL("💎", Color(0xFFFFD700), "Crystals +5")
}

data class Star(
    var x: Float,
    var y: Float,
    val speed: Float,
    val size: Float,
    var alpha: Float,
    val color: Color
)

data class NebulaCloud(
    var x: Float,
    var y: Float,
    val radius: Float,
    val color: Color,
    val alpha: Float
)

data class Player(
    var x: Float = 0f,
    var y: Float = 0f,
    var targetX: Float = 0f,
    var targetY: Float = 0f,
    var radius: Float = 36f,
    var hp: Float = 100f,
    var maxHp: Float = 100f,
    var bombCharge: Float = 0f, // 0f..100f
    var rapidFireTimer: Float = 0f,
    var tripleShotTimer: Float = 0f,
    var invulnerableTimer: Float = 0f
)

data class Bullet(
    var x: Float,
    var y: Float,
    var vx: Float,
    var vy: Float,
    val isEnemy: Boolean,
    val damage: Float,
    val color: Color,
    val radius: Float
)

data class Enemy(
    val id: Long,
    val type: EnemyType,
    var x: Float,
    var y: Float,
    var vx: Float,
    var vy: Float,
    var hp: Float,
    val maxHp: Float,
    val radius: Float,
    val scoreValue: Int,
    val crystalValue: Int,
    var shootTimer: Float,
    val color: Color,
    var angle: Float = 0f,
    var rotationSpeed: Float = 0f
)

data class PowerUp(
    val type: PowerUpType,
    var x: Float,
    var y: Float,
    var vy: Float = 120f,
    val radius: Float = 26f,
    var pulsePhase: Float = 0f
)

data class Particle(
    var x: Float,
    var y: Float,
    var vx: Float,
    var vy: Float,
    var alpha: Float,
    val color: Color,
    var radius: Float,
    val decay: Float
)

data class FloatingText(
    var x: Float,
    var y: Float,
    val text: String,
    val color: Color,
    var alpha: Float = 1f,
    var vy: Float = -60f
)

data class GameStats(
    var score: Int = 0,
    var wave: Int = 1,
    var enemiesDefeated: Int = 0,
    var crystalsCollected: Int = 0,
    var comboCount: Int = 0,
    var comboMultiplier: Int = 1,
    var comboTimer: Float = 0f
)

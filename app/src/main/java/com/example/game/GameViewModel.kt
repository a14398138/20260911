package com.example.game

import android.app.Application
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin
import kotlin.random.Random

class GameViewModel(application: Application) : AndroidViewModel(application) {

    val prefs = GamePreferences(application)
    val sound = SoundManager(application)
    val haptics = HapticsManager(application)

    private val _gameState = MutableStateFlow(GameState.MENU)
    val gameState: StateFlow<GameState> = _gameState.asStateFlow()

    private val _stats = MutableStateFlow(GameStats())
    val stats: StateFlow<GameStats> = _stats.asStateFlow()

    private val _player = MutableStateFlow(Player())
    val player: StateFlow<Player> = _player.asStateFlow()

    private val _boss = MutableStateFlow<Enemy?>(null)
    val boss: StateFlow<Enemy?> = _boss.asStateFlow()

    private val _warningBanner = MutableStateFlow<String?>(null)
    val warningBanner: StateFlow<String?> = _warningBanner.asStateFlow()

    private val _totalCrystals = MutableStateFlow(prefs.totalCrystals)
    val totalCrystals: StateFlow<Int> = _totalCrystals.asStateFlow()

    private val _highScore = MutableStateFlow(prefs.highScore)
    val highScore: StateFlow<Int> = _highScore.asStateFlow()

    private val _selectedShip = MutableStateFlow(prefs.selectedShip)
    val selectedShip: StateFlow<ShipType> = _selectedShip.asStateFlow()

    // Screen dimensions
    var screenWidth: Float = 1080f
    var screenHeight: Float = 2400f

    // Live Game entities (modified on game tick)
    val stars = mutableListOf<Star>()
    val nebulae = mutableListOf<NebulaCloud>()
    val bullets = mutableListOf<Bullet>()
    val enemies = mutableListOf<Enemy>()
    val powerUps = mutableListOf<PowerUp>()
    val particles = mutableListOf<Particle>()
    val floatingTexts = mutableListOf<FloatingText>()

    var screenShake: Float = 0f
    private var shootCooldown: Float = 0f
    private var enemySpawnTimer: Float = 0f
    private var waveProgressTimer: Float = 0f
    private var bannerTimer: Float = 0f
    private var nextEnemyId: Long = 1L
    private var isBossWaveActive: Boolean = false

    init {
        sound.isMuted = !prefs.isSoundEnabled
        haptics.isHapticsEnabled = prefs.isHapticsEnabled
        initStarfield(1080f, 2400f)
    }

    fun initStarfield(width: Float, height: Float) {
        screenWidth = width.coerceAtLeast(400f)
        screenHeight = height.coerceAtLeast(600f)
        if (stars.isEmpty()) {
            val starColors = listOf(
                Color(0xFFFFFFFF),
                Color(0xFF80D8FF),
                Color(0xFFFF80AB),
                Color(0xFFFFD54F)
            )
            for (i in 0 until 120) {
                stars.add(
                    Star(
                        x = Random.nextFloat() * screenWidth,
                        y = Random.nextFloat() * screenHeight,
                        speed = Random.nextFloat() * 120f + 40f,
                        size = Random.nextFloat() * 2.5f + 1f,
                        alpha = Random.nextFloat() * 0.7f + 0.3f,
                        color = starColors.random()
                    )
                )
            }
            nebulae.clear()
            nebulae.add(NebulaCloud(screenWidth * 0.3f, screenHeight * 0.25f, 220f, Color(0x227C4DFF), 0.15f))
            nebulae.add(NebulaCloud(screenWidth * 0.75f, screenHeight * 0.65f, 260f, Color(0x2200E5FF), 0.15f))
            nebulae.add(NebulaCloud(screenWidth * 0.4f, screenHeight * 0.85f, 200f, Color(0x22FF1744), 0.12f))
        }
    }

    fun startMission() {
        val ship = _selectedShip.value
        val baseMaxHp = 100f * ship.hpMultiplier * (1f + (prefs.shieldUpgradeLevel - 1) * 0.15f)

        _player.value = Player(
            x = screenWidth / 2f,
            y = screenHeight * 0.82f,
            targetX = screenWidth / 2f,
            targetY = screenHeight * 0.82f,
            radius = 36f,
            hp = baseMaxHp,
            maxHp = baseMaxHp,
            bombCharge = 35f,
            invulnerableTimer = 1.5f
        )

        _stats.value = GameStats(wave = 1)
        _boss.value = null
        _warningBanner.value = "MISSION START: DEFEND THE SECTOR"
        bannerTimer = 2.5f

        bullets.clear()
        enemies.clear()
        powerUps.clear()
        particles.clear()
        floatingTexts.clear()

        isBossWaveActive = false
        enemySpawnTimer = 1.0f
        waveProgressTimer = 0f
        shootCooldown = 0f
        screenShake = 0f

        _gameState.value = GameState.PLAYING
    }

    fun pauseGame() {
        if (_gameState.value == GameState.PLAYING) {
            _gameState.value = GameState.PAUSED
        }
    }

    fun resumeGame() {
        if (_gameState.value == GameState.PAUSED) {
            _gameState.value = GameState.PLAYING
        }
    }

    fun goToMenu() {
        _gameState.value = GameState.MENU
        _boss.value = null
        _warningBanner.value = null
    }

    fun openHangar() {
        _gameState.value = GameState.HANGAR
    }

    fun openHowToPlay() {
        _gameState.value = GameState.HOW_TO_PLAY
    }

    fun selectShip(ship: ShipType) {
        if (prefs.isShipUnlocked(ship)) {
            prefs.selectedShip = ship
            _selectedShip.value = ship
        }
    }

    fun buyShip(ship: ShipType): Boolean {
        val currentCrystals = _totalCrystals.value
        if (currentCrystals >= ship.unlockCost) {
            prefs.totalCrystals = currentCrystals - ship.unlockCost
            _totalCrystals.value = prefs.totalCrystals
            prefs.unlockShip(ship)
            selectShip(ship)
            sound.playPowerUp()
            return true
        }
        return false
    }

    fun upgradeStat(type: String): Boolean {
        val currentCrystals = _totalCrystals.value
        val cost = when (type) {
            "shield" -> prefs.shieldUpgradeLevel * 40
            "blaster" -> prefs.blasterUpgradeLevel * 40
            "firerate" -> prefs.fireRateUpgradeLevel * 40
            "bomb" -> prefs.bombChargeUpgradeLevel * 50
            else -> 9999
        }
        if (currentCrystals >= cost) {
            prefs.totalCrystals = currentCrystals - cost
            _totalCrystals.value = prefs.totalCrystals
            when (type) {
                "shield" -> prefs.shieldUpgradeLevel += 1
                "blaster" -> prefs.blasterUpgradeLevel += 1
                "firerate" -> prefs.fireRateUpgradeLevel += 1
                "bomb" -> prefs.bombChargeUpgradeLevel += 1
            }
            sound.playPowerUp()
            return true
        }
        return false
    }

    fun toggleSound(): Boolean {
        val newState = !prefs.isSoundEnabled
        prefs.isSoundEnabled = newState
        sound.isMuted = !newState
        return newState
    }

    fun toggleHaptics(): Boolean {
        val newState = !prefs.isHapticsEnabled
        prefs.isHapticsEnabled = newState
        haptics.isHapticsEnabled = newState
        return newState
    }

    fun updatePlayerTarget(touchX: Float, touchY: Float) {
        if (_gameState.value != GameState.PLAYING) return
        val p = _player.value
        p.targetX = touchX.coerceIn(p.radius, screenWidth - p.radius)
        p.targetY = touchY.coerceIn(p.radius * 2f, screenHeight - p.radius * 2f)
    }

    fun triggerQuantumBomb() {
        if (_gameState.value != GameState.PLAYING) return
        val p = _player.value
        if (p.bombCharge < 100f) return

        p.bombCharge = 0f
        sound.playBomb()
        haptics.vibrateBomb()
        screenShake = 22f

        // Clear all enemy bullets
        bullets.removeAll { it.isEnemy }

        // Spawn massive ring shockwave particles
        for (i in 0 until 60) {
            val angle = (i / 60f) * 2.0 * Math.PI
            val speed = Random.nextFloat() * 500f + 250f
            particles.add(
                Particle(
                    x = p.x,
                    y = p.y,
                    vx = (cos(angle) * speed).toFloat(),
                    vy = (sin(angle) * speed).toFloat(),
                    alpha = 1f,
                    color = Color(0xFF00E5FF),
                    radius = Random.nextFloat() * 4f + 2f,
                    decay = 1.2f
                )
            )
        }

        // Damage all active enemies
        val currentStats = _stats.value
        val damage = 350f
        val iterator = enemies.iterator()
        while (iterator.hasNext()) {
            val enemy = iterator.next()
            enemy.hp -= damage
            if (enemy.hp <= 0) {
                iterator.remove()
                onEnemyDestroyed(enemy, currentStats)
            } else {
                floatingTexts.add(
                    FloatingText(
                        x = enemy.x,
                        y = enemy.y,
                        text = "-${damage.toInt()}",
                        color = Color(0xFFFF5252)
                    )
                )
            }
        }
    }

    /** Primary game loop tick called on each frame */
    fun update(dt: Float) {
        val clampedDt = dt.coerceIn(0.001f, 0.05f)

        // Starfield & Parallax background always move
        updateStarfield(clampedDt)

        if (_gameState.value != GameState.PLAYING) {
            updateParticles(clampedDt)
            updateFloatingTexts(clampedDt)
            return
        }

        // Screen shake decay
        if (screenShake > 0) {
            screenShake = (screenShake - clampedDt * 25f).coerceAtLeast(0f)
        }

        // Banner timer
        if (bannerTimer > 0) {
            bannerTimer -= clampedDt
            if (bannerTimer <= 0) {
                _warningBanner.value = null
            }
        }

        val p = _player.value
        val s = _stats.value
        val ship = _selectedShip.value

        // Player movement interpolation (smooth follow touch)
        p.x += (p.targetX - p.x) * 12f * clampedDt
        p.y += (p.targetY - p.y) * 12f * clampedDt

        // Timers
        if (p.invulnerableTimer > 0) p.invulnerableTimer -= clampedDt
        if (p.rapidFireTimer > 0) p.rapidFireTimer -= clampedDt
        if (p.tripleShotTimer > 0) p.tripleShotTimer -= clampedDt

        // Thruster flame particles behind player
        particles.add(
            Particle(
                x = p.x + (Random.nextFloat() * 12f - 6f),
                y = p.y + p.radius * 0.7f,
                vx = Random.nextFloat() * 40f - 20f,
                vy = Random.nextFloat() * 150f + 120f,
                alpha = 0.8f,
                color = if (p.rapidFireTimer > 0) Color(0xFFFF9100) else ship.primaryColor,
                radius = Random.nextFloat() * 3.5f + 1.5f,
                decay = 2.5f
            )
        )

        // Combo timer
        if (s.comboTimer > 0) {
            s.comboTimer -= clampedDt
            if (s.comboTimer <= 0) {
                s.comboCount = 0
                s.comboMultiplier = 1
            }
        }

        // Auto-firing weapons
        val baseFireInterval = 0.22f / (ship.fireRateMultiplier * (1f + (prefs.fireRateUpgradeLevel - 1) * 0.12f))
        val actualFireInterval = if (p.rapidFireTimer > 0) baseFireInterval * 0.5f else baseFireInterval

        shootCooldown -= clampedDt
        if (shootCooldown <= 0) {
            shootCooldown = actualFireInterval
            firePlayerWeapons(p, ship)
        }

        // Wave management & Enemy spawning
        waveProgressTimer += clampedDt
        enemySpawnTimer -= clampedDt
        if (enemySpawnTimer <= 0 && !isBossWaveActive) {
            spawnEnemiesForWave(s.wave)
            enemySpawnTimer = (2.2f - (s.wave * 0.15f)).coerceAtLeast(0.85f)
        }

        // Check if wave threshold reached for boss (every 5 waves)
        if (s.wave % 5 == 0 && !isBossWaveActive && enemies.none { it.type == EnemyType.NEBULA_BOSS }) {
            spawnBoss(s.wave)
        } else if (!isBossWaveActive && waveProgressTimer > 35f && enemies.isEmpty()) {
            // Wave cleared!
            advanceWave()
        }

        // Update Projectiles
        updateBullets(clampedDt, p, s)

        // Update Enemies
        updateEnemies(clampedDt, p, s)

        // Update PowerUps
        updatePowerUps(clampedDt, p, s)

        // Update Particles
        updateParticles(clampedDt)

        // Update Floating Texts
        updateFloatingTexts(clampedDt)

        // Passive Bomb charge generation
        val bombChargeRate = 1.2f * (1f + (prefs.bombChargeUpgradeLevel - 1) * 0.2f)
        p.bombCharge = (p.bombCharge + clampedDt * bombChargeRate).coerceAtMost(100f)

        // Sync boss state for UI health bar
        _boss.value = enemies.firstOrNull { it.type == EnemyType.NEBULA_BOSS }
    }

    private fun updateStarfield(dt: Float) {
        val speedFactor = if (_gameState.value == GameState.PLAYING) 1.0f else 0.4f
        for (star in stars) {
            star.y += star.speed * speedFactor * dt
            if (star.y > screenHeight) {
                star.y = -5f
                star.x = Random.nextFloat() * screenWidth
            }
        }
    }

    private fun firePlayerWeapons(p: Player, ship: ShipType) {
        val baseDamage = 25f * ship.damageMultiplier * (1f + (prefs.blasterUpgradeLevel - 1) * 0.15f)
        val bulletSpeed = 1100f

        sound.playLaser()

        if (p.tripleShotTimer > 0) {
            // Center
            bullets.add(Bullet(p.x, p.y - p.radius, 0f, -bulletSpeed, false, baseDamage, Color(0xFF00E5FF), 5f))
            // Left spread
            bullets.add(Bullet(p.x - 14f, p.y - p.radius * 0.8f, -220f, -bulletSpeed * 0.95f, false, baseDamage * 0.85f, Color(0xFF00E5FF), 4.5f))
            // Right spread
            bullets.add(Bullet(p.x + 14f, p.y - p.radius * 0.8f, 220f, -bulletSpeed * 0.95f, false, baseDamage * 0.85f, Color(0xFF00E5FF), 4.5f))
        } else {
            // Twin parallel lasers
            bullets.add(Bullet(p.x - 16f, p.y - p.radius * 0.6f, 0f, -bulletSpeed, false, baseDamage, ship.primaryColor, 5f))
            bullets.add(Bullet(p.x + 16f, p.y - p.radius * 0.6f, 0f, -bulletSpeed, false, baseDamage, ship.primaryColor, 5f))
        }
    }

    private fun spawnEnemiesForWave(wave: Int) {
        val spawnX = Random.nextFloat() * (screenWidth - 120f) + 60f
        val id = nextEnemyId++

        val rand = Random.nextFloat()
        when {
            wave >= 3 && rand < 0.25f -> {
                // Heavy Cruiser
                enemies.add(
                    Enemy(
                        id = id,
                        type = EnemyType.HEAVY_CRUISER,
                        x = spawnX,
                        y = -70f,
                        vx = if (Random.nextBoolean()) 60f else -60f,
                        vy = 80f,
                        hp = 180f + wave * 25f,
                        maxHp = 180f + wave * 25f,
                        radius = 42f,
                        scoreValue = 350,
                        crystalValue = 4,
                        shootTimer = 1.2f,
                        color = Color(0xFFFF5252)
                    )
                )
            }
            wave >= 2 && rand < 0.55f -> {
                // Scout Raider
                enemies.add(
                    Enemy(
                        id = id,
                        type = EnemyType.SCOUT,
                        x = spawnX,
                        y = -50f,
                        vx = if (Random.nextBoolean()) 120f else -120f,
                        vy = 140f,
                        hp = 60f + wave * 15f,
                        maxHp = 60f + wave * 15f,
                        radius = 28f,
                        scoreValue = 180,
                        crystalValue = 2,
                        shootTimer = 1.6f,
                        color = Color(0xFFFF4081)
                    )
                )
            }
            rand < 0.80f -> {
                // Drone
                enemies.add(
                    Enemy(
                        id = id,
                        type = EnemyType.DRONE,
                        x = spawnX,
                        y = -40f,
                        vx = 0f,
                        vy = 210f + wave * 10f,
                        hp = 35f + wave * 8f,
                        maxHp = 35f + wave * 8f,
                        radius = 24f,
                        scoreValue = 100,
                        crystalValue = 1,
                        shootTimer = 999f,
                        color = Color(0xFFFFA000)
                    )
                )
            }
            else -> {
                // Asteroid
                enemies.add(
                    Enemy(
                        id = id,
                        type = EnemyType.ASTEROID,
                        x = spawnX,
                        y = -50f,
                        vx = Random.nextFloat() * 40f - 20f,
                        vy = 120f + Random.nextFloat() * 60f,
                        hp = 90f + wave * 12f,
                        maxHp = 90f + wave * 12f,
                        radius = 34f,
                        scoreValue = 80,
                        crystalValue = 2,
                        shootTimer = 999f,
                        color = Color(0xFFB0BEC5),
                        rotationSpeed = Random.nextFloat() * 90f - 45f
                    )
                )
            }
        }
    }

    private fun spawnBoss(wave: Int) {
        isBossWaveActive = true
        _warningBanner.value = "⚠️ DANGER: NEBULA TITAN DETECTED ⚠️"
        bannerTimer = 3.5f
        sound.playExplosion(large = true)
        haptics.vibrateExplosion()

        val bossHp = 1200f + wave * 300f
        enemies.add(
            Enemy(
                id = nextEnemyId++,
                type = EnemyType.NEBULA_BOSS,
                x = screenWidth / 2f,
                y = -120f,
                vx = 80f,
                vy = 60f,
                hp = bossHp,
                maxHp = bossHp,
                radius = 70f,
                scoreValue = 5000,
                crystalValue = 35,
                shootTimer = 1.0f,
                color = Color(0xFFFF1744)
            )
        )
    }

    private fun updateBullets(dt: Float, p: Player, s: GameStats) {
        val bIterator = bullets.iterator()
        while (bIterator.hasNext()) {
            val b = bIterator.next()
            b.x += b.vx * dt
            b.y += b.vy * dt

            // Off screen check
            if (b.y < -30f || b.y > screenHeight + 30f || b.x < -30f || b.x > screenWidth + 30f) {
                bIterator.remove()
                continue
            }

            if (!b.isEnemy) {
                // Check collision with enemies
                var bulletConsumed = false
                val eIterator = enemies.iterator()
                while (eIterator.hasNext()) {
                    val enemy = eIterator.next()
                    val dist = hypot(b.x - enemy.x, b.y - enemy.y)
                    if (dist < b.radius + enemy.radius) {
                        bulletConsumed = true
                        enemy.hp -= b.damage

                        // Small hit sparks
                        for (i in 0 until 4) {
                            particles.add(
                                Particle(
                                    x = b.x,
                                    y = b.y,
                                    vx = Random.nextFloat() * 120f - 60f,
                                    vy = Random.nextFloat() * 120f - 60f,
                                    alpha = 0.9f,
                                    color = b.color,
                                    radius = 2.5f,
                                    decay = 3.5f
                                )
                            )
                        }

                        if (enemy.hp <= 0) {
                            eIterator.remove()
                            onEnemyDestroyed(enemy, s)
                        }
                        break
                    }
                }
                if (bulletConsumed) {
                    bIterator.remove()
                }
            } else {
                // Enemy bullet vs Player
                val dist = hypot(b.x - p.x, b.y - p.y)
                if (dist < b.radius + p.radius * 0.7f) {
                    bIterator.remove()
                    damagePlayer(b.damage, p, s)
                }
            }
        }
    }

    private fun updateEnemies(dt: Float, p: Player, s: GameStats) {
        val eIterator = enemies.iterator()
        while (eIterator.hasNext()) {
            val enemy = eIterator.next()

            if (enemy.type == EnemyType.NEBULA_BOSS) {
                // Boss movement pattern
                if (enemy.y < 220f) {
                    enemy.y += enemy.vy * dt
                } else {
                    enemy.x += enemy.vx * dt
                    if (enemy.x > screenWidth - enemy.radius) {
                        enemy.x = screenWidth - enemy.radius
                        enemy.vx = -enemy.vx
                    } else if (enemy.x < enemy.radius) {
                        enemy.x = enemy.radius
                        enemy.vx = -enemy.vx
                    }
                }

                // Boss attack pattern
                enemy.shootTimer -= dt
                if (enemy.shootTimer <= 0) {
                    enemy.shootTimer = if (enemy.hp < enemy.maxHp * 0.4f) 0.65f else 1.1f
                    // Multi-laser barrage
                    val bulletCount = 7
                    for (i in 0 until bulletCount) {
                        val angle = (Math.PI / 6.0) + (i.toDouble() / (bulletCount - 1)) * (2.0 * Math.PI / 3.0)
                        val speed = 360f
                        bullets.add(
                            Bullet(
                                x = enemy.x,
                                y = enemy.y + enemy.radius * 0.6f,
                                vx = (cos(angle) * speed).toFloat(),
                                vy = (sin(angle) * speed).toFloat(),
                                isEnemy = true,
                                damage = 22f,
                                color = Color(0xFFFF1744),
                                radius = 6f
                            )
                        )
                    }
                    sound.playLaser()
                }
            } else {
                // Normal enemy movement
                enemy.x += enemy.vx * dt
                enemy.y += enemy.vy * dt
                enemy.angle += enemy.rotationSpeed * dt

                // Wall bounce for scouts & cruisers
                if (enemy.x < enemy.radius || enemy.x > screenWidth - enemy.radius) {
                    enemy.vx = -enemy.vx
                }

                // Normal enemy shooting
                if (enemy.type == EnemyType.SCOUT || enemy.type == EnemyType.HEAVY_CRUISER) {
                    enemy.shootTimer -= dt
                    if (enemy.shootTimer <= 0 && enemy.y > 0 && enemy.y < screenHeight * 0.7f) {
                        enemy.shootTimer = 2.0f
                        bullets.add(
                            Bullet(
                                x = enemy.x,
                                y = enemy.y + enemy.radius,
                                vx = 0f,
                                vy = 340f,
                                isEnemy = true,
                                damage = if (enemy.type == EnemyType.HEAVY_CRUISER) 24f else 16f,
                                color = Color(0xFFFF5252),
                                radius = 5f
                            )
                        )
                    }
                }

                // Remove enemy if flies off bottom
                if (enemy.y > screenHeight + enemy.radius * 2f) {
                    eIterator.remove()
                    continue
                }
            }

            // Body collision with Player
            val dist = hypot(enemy.x - p.x, enemy.y - p.y)
            if (dist < enemy.radius + p.radius * 0.8f) {
                if (enemy.type != EnemyType.NEBULA_BOSS) {
                    eIterator.remove()
                    onEnemyDestroyed(enemy, s)
                }
                damagePlayer(35f, p, s)
            }
        }
    }

    private fun damagePlayer(damage: Float, p: Player, s: GameStats) {
        if (p.invulnerableTimer > 0) return

        p.hp = (p.hp - damage).coerceAtLeast(0f)
        p.invulnerableTimer = 0.8f
        screenShake = 12f
        haptics.vibrateHit()
        sound.playShieldHit()

        // Reset combo
        s.comboCount = 0
        s.comboMultiplier = 1

        // Damage indicator
        floatingTexts.add(
            FloatingText(
                x = p.x,
                y = p.y - p.radius,
                text = "-${damage.toInt()}",
                color = Color(0xFFFF1744)
            )
        )

        // Shield hit sparks
        for (i in 0 until 10) {
            particles.add(
                Particle(
                    x = p.x,
                    y = p.y,
                    vx = Random.nextFloat() * 160f - 80f,
                    vy = Random.nextFloat() * 160f - 80f,
                    alpha = 0.9f,
                    color = Color(0xFFFF1744),
                    radius = 3f,
                    decay = 2.5f
                )
            )
        }

        if (p.hp <= 0f) {
            triggerGameOver()
        }
    }

    private fun onEnemyDestroyed(enemy: Enemy, s: GameStats) {
        sound.playExplosion(large = enemy.type == EnemyType.NEBULA_BOSS || enemy.type == EnemyType.HEAVY_CRUISER)
        haptics.vibrateExplosion()

        // Increase combo
        s.comboCount++
        s.comboMultiplier = when {
            s.comboCount >= 15 -> 5
            s.comboCount >= 10 -> 4
            s.comboCount >= 5 -> 3
            s.comboCount >= 2 -> 2
            else -> 1
        }
        s.comboTimer = 2.8f

        // Increase score & stats
        val earnedScore = enemy.scoreValue * s.comboMultiplier
        s.score += earnedScore
        s.enemiesDefeated++

        // Check & update high score
        if (s.score > _highScore.value) {
            _highScore.value = s.score
            prefs.highScore = s.score
        }

        // Add floating score
        floatingTexts.add(
            FloatingText(
                x = enemy.x,
                y = enemy.y,
                text = "+$earnedScore",
                color = if (s.comboMultiplier > 1) Color(0xFFFFD700) else Color.White
            )
        )

        // Bomb charge bonus
        val p = _player.value
        p.bombCharge = (p.bombCharge + (if (enemy.type == EnemyType.NEBULA_BOSS) 35f else 4.5f)).coerceAtMost(100f)

        // Explosion particles
        val pCount = if (enemy.type == EnemyType.NEBULA_BOSS) 45 else 18
        for (i in 0 until pCount) {
            val angle = Random.nextFloat() * 2f * Math.PI.toFloat()
            val speed = Random.nextFloat() * 220f + 60f
            particles.add(
                Particle(
                    x = enemy.x,
                    y = enemy.y,
                    vx = cos(angle) * speed,
                    vy = sin(angle) * speed,
                    alpha = 1f,
                    color = enemy.color,
                    radius = Random.nextFloat() * 4f + 2f,
                    decay = 1.8f
                )
            )
        }

        // Drops: Crystals and PowerUps
        val earnedCrystals = enemy.crystalValue
        if (earnedCrystals > 0) {
            s.crystalsCollected += earnedCrystals
            prefs.totalCrystals += earnedCrystals
            _totalCrystals.value = prefs.totalCrystals
        }

        // Power-up chance (15% from normal, 100% from boss or heavy)
        val powerUpRoll = Random.nextFloat()
        if (enemy.type == EnemyType.NEBULA_BOSS) {
            powerUps.add(PowerUp(PowerUpType.QUANTUM_BOMB, enemy.x - 40f, enemy.y))
            powerUps.add(PowerUp(PowerUpType.SHIELD_RECHARGE, enemy.x + 40f, enemy.y))
            isBossWaveActive = false
            advanceWave()
        } else if (enemy.type == EnemyType.HEAVY_CRUISER || powerUpRoll < 0.16f) {
            val type = when {
                powerUpRoll < 0.04f -> PowerUpType.QUANTUM_BOMB
                powerUpRoll < 0.08f -> PowerUpType.SHIELD_RECHARGE
                powerUpRoll < 0.12f -> PowerUpType.TRIPLE_SHOT
                else -> PowerUpType.RAPID_FIRE
            }
            powerUps.add(PowerUp(type, enemy.x, enemy.y))
        }
    }

    private fun updatePowerUps(dt: Float, p: Player, s: GameStats) {
        val pIterator = powerUps.iterator()
        while (pIterator.hasNext()) {
            val pu = pIterator.next()
            pu.y += pu.vy * dt
            pu.pulsePhase += dt * 5f

            if (pu.y > screenHeight + 40f) {
                pIterator.remove()
                continue
            }

            // Collect check with Player
            val dist = hypot(pu.x - p.x, pu.y - p.y)
            if (dist < pu.radius + p.radius) {
                pIterator.remove()
                sound.playPowerUp()

                when (pu.type) {
                    PowerUpType.TRIPLE_SHOT -> {
                        p.tripleShotTimer = 10f
                        floatingTexts.add(FloatingText(p.x, p.y - 20f, "TRIPLE LASER!", pu.type.color))
                    }
                    PowerUpType.RAPID_FIRE -> {
                        p.rapidFireTimer = 8f
                        floatingTexts.add(FloatingText(p.x, p.y - 20f, "RAPID FIRE!", pu.type.color))
                    }
                    PowerUpType.SHIELD_RECHARGE -> {
                        p.hp = (p.hp + p.maxHp * 0.45f).coerceAtMost(p.maxHp)
                        floatingTexts.add(FloatingText(p.x, p.y - 20f, "+SHIELD REPAIRED", pu.type.color))
                    }
                    PowerUpType.QUANTUM_BOMB -> {
                        p.bombCharge = 100f
                        floatingTexts.add(FloatingText(p.x, p.y - 20f, "NOVA BOMB READY!", pu.type.color))
                    }
                    PowerUpType.STAR_CRYSTAL -> {
                        s.crystalsCollected += 5
                        prefs.totalCrystals += 5
                        _totalCrystals.value = prefs.totalCrystals
                        floatingTexts.add(FloatingText(p.x, p.y - 20f, "+5 CRYSTALS", pu.type.color))
                    }
                }
            }
        }
    }

    private fun updateParticles(dt: Float) {
        val iterator = particles.iterator()
        while (iterator.hasNext()) {
            val p = iterator.next()
            p.x += p.vx * dt
            p.y += p.vy * dt
            p.alpha -= p.decay * dt
            if (p.alpha <= 0f) {
                iterator.remove()
            }
        }
    }

    private fun updateFloatingTexts(dt: Float) {
        val iterator = floatingTexts.iterator()
        while (iterator.hasNext()) {
            val ft = iterator.next()
            ft.y += ft.vy * dt
            ft.alpha -= 0.9f * dt
            if (ft.alpha <= 0f) {
                iterator.remove()
            }
        }
    }

    private fun advanceWave() {
        val s = _stats.value
        s.wave++
        waveProgressTimer = 0f
        _warningBanner.value = "SECTOR SECURED! ENTERING WAVE ${s.wave}"
        bannerTimer = 3.0f
        sound.playPowerUp()

        // Wave completion bonus
        val bonus = s.wave * 250
        s.score += bonus
        floatingTexts.add(
            FloatingText(
                x = screenWidth / 2f,
                y = screenHeight * 0.35f,
                text = "WAVE BONUS +$bonus",
                color = Color(0xFF00E5FF)
            )
        )
    }

    private fun triggerGameOver() {
        _gameState.value = GameState.GAME_OVER
        sound.playExplosion(large = true)
        haptics.vibrateBomb()
        screenShake = 20f
        _boss.value = null
    }
}

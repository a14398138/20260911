package com.example.game

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.VolumeMute
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun GameOverlayUI(
    viewModel: GameViewModel,
    modifier: Modifier = Modifier
) {
    val gameState by viewModel.gameState.collectAsState()
    val stats by viewModel.stats.collectAsState()
    val player by viewModel.player.collectAsState()
    val boss by viewModel.boss.collectAsState()
    val warningBanner by viewModel.warningBanner.collectAsState()
    val totalCrystals by viewModel.totalCrystals.collectAsState()
    val highScore by viewModel.highScore.collectAsState()
    val selectedShip by viewModel.selectedShip.collectAsState()

    Box(modifier = modifier.fillMaxSize()) {
        when (gameState) {
            GameState.PLAYING -> {
                InGameHUD(
                    stats = stats,
                    player = player,
                    boss = boss,
                    warningBanner = warningBanner,
                    selectedShip = selectedShip,
                    isSoundMuted = viewModel.sound.isMuted,
                    isHapticsOn = viewModel.haptics.isHapticsEnabled,
                    onPause = { viewModel.pauseGame() },
                    onToggleSound = { viewModel.toggleSound() },
                    onToggleHaptics = { viewModel.toggleHaptics() },
                    onTriggerBomb = { viewModel.triggerQuantumBomb() }
                )
            }

            GameState.PAUSED -> {
                InGameHUD(
                    stats = stats,
                    player = player,
                    boss = boss,
                    warningBanner = null,
                    selectedShip = selectedShip,
                    isSoundMuted = viewModel.sound.isMuted,
                    isHapticsOn = viewModel.haptics.isHapticsEnabled,
                    onPause = {},
                    onToggleSound = { viewModel.toggleSound() },
                    onToggleHaptics = { viewModel.toggleHaptics() },
                    onTriggerBomb = {}
                )
                PauseOverlay(
                    onResume = { viewModel.resumeGame() },
                    onRestart = { viewModel.startMission() },
                    onQuit = { viewModel.goToMenu() }
                )
            }

            GameState.GAME_OVER -> {
                GameOverOverlay(
                    stats = stats,
                    highScore = highScore,
                    onRetry = { viewModel.startMission() },
                    onHangar = { viewModel.openHangar() },
                    onMenu = { viewModel.goToMenu() }
                )
            }

            GameState.MENU -> {
                MainMenuOverlay(
                    highScore = highScore,
                    totalCrystals = totalCrystals,
                    selectedShip = selectedShip,
                    isSoundMuted = viewModel.sound.isMuted,
                    isHapticsOn = viewModel.haptics.isHapticsEnabled,
                    onStart = { viewModel.startMission() },
                    onOpenHangar = { viewModel.openHangar() },
                    onOpenHowToPlay = { viewModel.openHowToPlay() },
                    onToggleSound = { viewModel.toggleSound() },
                    onToggleHaptics = { viewModel.toggleHaptics() }
                )
            }

            GameState.HANGAR -> {
                HangarOverlay(
                    viewModel = viewModel,
                    totalCrystals = totalCrystals,
                    selectedShip = selectedShip,
                    onBack = { viewModel.goToMenu() }
                )
            }

            GameState.HOW_TO_PLAY -> {
                HowToPlayOverlay(
                    onBack = { viewModel.goToMenu() }
                )
            }

            GameState.VICTORY -> {
                GameOverOverlay(
                    stats = stats,
                    highScore = highScore,
                    onRetry = { viewModel.startMission() },
                    onHangar = { viewModel.openHangar() },
                    onMenu = { viewModel.goToMenu() }
                )
            }
        }
    }
}

@Composable
fun InGameHUD(
    stats: GameStats,
    player: Player,
    boss: Enemy?,
    warningBanner: String?,
    selectedShip: ShipType,
    isSoundMuted: Boolean,
    isHapticsOn: Boolean,
    onPause: () -> Unit,
    onToggleSound: () -> Unit,
    onToggleHaptics: () -> Unit,
    onTriggerBomb: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // TOP HUD BAR
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Score & Wave
                Column {
                    Text(
                        text = "SCORE: ${stats.score}",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color(0x3300E5FF),
                            modifier = Modifier.border(1.dp, Color(0xFF00E5FF), RoundedCornerShape(6.dp))
                        ) {
                            Text(
                                text = "WAVE ${stats.wave}",
                                color = Color(0xFF00E5FF),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "💎 ${stats.crystalsCollected}",
                            color = Color(0xFFFFD700),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                // Combo Indicator
                if (stats.comboMultiplier > 1) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0x55FF9100),
                        modifier = Modifier.border(1.5.dp, Color(0xFFFF9100), RoundedCornerShape(12.dp))
                    ) {
                        Text(
                            text = "COMBO x${stats.comboMultiplier}!",
                            color = Color(0xFFFFD600),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Black,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }

                // Header Action Buttons
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onToggleSound,
                        modifier = Modifier.size(38.dp).testTag("hud_sound_btn")
                    ) {
                        Icon(
                            imageVector = if (isSoundMuted) Icons.Default.VolumeMute else Icons.Default.VolumeUp,
                            contentDescription = "Sound toggle",
                            tint = Color.White.copy(alpha = 0.8f)
                        )
                    }
                    IconButton(
                        onClick = onPause,
                        modifier = Modifier.size(38.dp).testTag("hud_pause_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Pause,
                            contentDescription = "Pause",
                            tint = Color.White
                        )
                    }
                }
            }

            // Boss Health Bar
            if (boss != null) {
                Spacer(modifier = Modifier.height(10.dp))
                val hpPct = (boss.hp / boss.maxHp).coerceIn(0f, 1f)
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0x881A0026)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.5.dp, Color(0xFFFF1744), RoundedCornerShape(8.dp))
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "⚠️ NEBULA TITAN",
                                color = Color(0xFFFF5252),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Black
                            )
                            Text(
                                text = "${(hpPct * 100).toInt()}%",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        LinearProgressIndicator(
                            progress = { hpPct },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = Color(0xFFFF1744),
                            trackColor = Color(0x55000000)
                        )
                    }
                }
            }

            // Banner notifications
            AnimatedVisibility(
                visible = warningBanner != null,
                enter = fadeIn() + slideInVertically(),
                exit = fadeOut() + slideOutVertically()
            ) {
                if (warningBanner != null) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xCC0D1432),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 10.dp)
                            .border(1.dp, Color(0xFF00E5FF), RoundedCornerShape(8.dp))
                    ) {
                        Text(
                            text = warningBanner,
                            color = Color(0xFF80D8FF),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(vertical = 6.dp, horizontal = 12.dp)
                        )
                    }
                }
            }
        }

        // BOTTOM HUD: SHIELD & NOVA BOMB
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Shield HP
                Column(modifier = Modifier.weight(1f).padding(end = 16.dp)) {
                    val hpPct = (player.hp / player.maxHp).coerceIn(0f, 1f)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Shield,
                                contentDescription = "Shield",
                                tint = if (hpPct > 0.35f) Color(0xFF00E5FF) else Color(0xFFFF5252),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "SHIELD SYSTEM",
                                color = Color.White.copy(alpha = 0.9f),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(
                            text = "${player.hp.toInt()} / ${player.maxHp.toInt()}",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    LinearProgressIndicator(
                        progress = { hpPct },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(10.dp)
                            .clip(RoundedCornerShape(5.dp)),
                        color = if (hpPct > 0.4f) Color(0xFF00E5FF) else Color(0xFFFF1744),
                        trackColor = Color(0x66000000)
                    )
                }

                // Nova Bomb Button
                val isBombReady = player.bombCharge >= 100f
                Button(
                    onClick = onTriggerBomb,
                    enabled = isBombReady,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isBombReady) Color(0xFFFF1744) else Color(0x44FFFFFF),
                        contentColor = Color.White
                    ),
                    modifier = Modifier
                        .height(46.dp)
                        .testTag("bomb_action_btn")
                        .then(
                            if (isBombReady) {
                                Modifier.border(2.dp, Color(0xFFFFD600), RoundedCornerShape(12.dp))
                            } else Modifier
                        )
                ) {
                    Text(
                        text = if (isBombReady) "💣 NOVA BOMB" else "💣 ${player.bombCharge.toInt()}%",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }
        }
    }
}

@Composable
fun MainMenuOverlay(
    highScore: Int,
    totalCrystals: Int,
    selectedShip: ShipType,
    isSoundMuted: Boolean,
    isHapticsOn: Boolean,
    onStart: () -> Unit,
    onOpenHangar: () -> Unit,
    onOpenHowToPlay: () -> Unit,
    onToggleSound: () -> Unit,
    onToggleHaptics: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xBB040510))
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            // Title Header
            Text(
                text = "STELLAR STRIKE",
                fontSize = 38.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 2.sp
            )
            Text(
                text = "COSMIC VANGUARD",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF00E5FF),
                letterSpacing = 6.sp,
                modifier = Modifier.padding(bottom = 28.dp)
            )

            // Current Ship Preview Card
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0x550A1128)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.5.dp, selectedShip.primaryColor.copy(alpha = 0.7f), RoundedCornerShape(16.dp))
                    .padding(horizontal = 4.dp)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "READY FOR DEPLOYMENT",
                        fontSize = 11.sp,
                        color = Color.White.copy(alpha = 0.6f),
                        letterSpacing = 2.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = selectedShip.title,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = selectedShip.primaryColor
                    )
                    Text(
                        text = selectedShip.description,
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.8f),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 4.dp)
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("HIGH SCORE", fontSize = 10.sp, color = Color.Gray)
                            Text("$highScore", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFFD700))
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("CRYSTALS", fontSize = 10.sp, color = Color.Gray)
                            Text("💎 $totalCrystals", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = Color(0xFF00E5FF))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Launch Button
            Button(
                onClick = onStart,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF00E5FF),
                    contentColor = Color(0xFF060919)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .testTag("start_mission_btn")
            ) {
                Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(24.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "DEPLOY MISSION",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.sp
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Ship Hangar & Upgrades Button
            OutlinedButton(
                onClick = onOpenHangar,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("hangar_btn")
            ) {
                Text(
                    text = "🛠️ SHIP HANGAR & UPGRADES",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // How To Play Button
            OutlinedButton(
                onClick = onOpenHowToPlay,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("how_to_play_btn")
            ) {
                Icon(imageVector = Icons.Default.Info, contentDescription = null, tint = Color(0xFF80D8FF), modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "HOW TO PLAY",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF80D8FF)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Sound & Haptic Toggles
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onToggleSound, modifier = Modifier.testTag("menu_sound_btn")) {
                    Icon(
                        imageVector = if (isSoundMuted) Icons.Default.VolumeMute else Icons.Default.VolumeUp,
                        contentDescription = "Sound toggle",
                        tint = Color.White.copy(alpha = 0.7f)
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                IconButton(onClick = onToggleHaptics, modifier = Modifier.testTag("menu_haptics_btn")) {
                    Icon(
                        imageVector = Icons.Default.Vibration,
                        contentDescription = "Haptics toggle",
                        tint = if (isHapticsOn) Color(0xFF00E5FF) else Color.Gray
                    )
                }
            }
        }
    }
}

@Composable
fun HangarOverlay(
    viewModel: GameViewModel,
    totalCrystals: Int,
    selectedShip: ShipType,
    onBack: () -> Unit
) {
    val scrollState = rememberScrollState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFA050716))
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack, modifier = Modifier.testTag("hangar_back_btn")) {
                    Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                }
                Text(
                    text = "SHIP HANGAR",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    fontFamily = FontFamily.Monospace
                )
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0x3300E5FF),
                    modifier = Modifier.border(1.dp, Color(0xFF00E5FF), RoundedCornerShape(10.dp))
                ) {
                    Text(
                        text = "💎 $totalCrystals",
                        color = Color(0xFFFFD700),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Ships list
            Text(
                text = "FLEET SELECTION",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF00E5FF),
                letterSpacing = 1.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            for (ship in ShipType.values()) {
                val isSelected = ship == selectedShip
                val isUnlocked = viewModel.prefs.isShipUnlocked(ship)

                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected) Color(0x4400E5FF) else Color(0x221A237E)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp)
                        .border(
                            width = if (isSelected) 2.dp else 1.dp,
                            color = if (isSelected) ship.primaryColor else Color(0x44FFFFFF),
                            shape = RoundedCornerShape(12.dp)
                        )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = ship.title,
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = ship.primaryColor
                                )
                                if (isSelected) {
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = ship.primaryColor
                                    ) {
                                        Text(
                                            text = "ACTIVE",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.Black,
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                            Text(
                                text = ship.description,
                                fontSize = 11.sp,
                                color = Color.White.copy(alpha = 0.8f),
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        if (isUnlocked) {
                            Button(
                                onClick = { viewModel.selectShip(ship) },
                                enabled = !isSelected,
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = ship.primaryColor,
                                    contentColor = Color.Black
                                )
                            ) {
                                Text(if (isSelected) "SELECTED" else "SELECT", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        } else {
                            Button(
                                onClick = { viewModel.buyShip(ship) },
                                enabled = totalCrystals >= ship.unlockCost,
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFFFFD700),
                                    contentColor = Color.Black
                                )
                            ) {
                                Icon(imageVector = Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("💎 ${ship.unlockCost}", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Upgrades Section
            Text(
                text = "SYSTEM UPGRADES",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF00E5FF),
                letterSpacing = 1.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            UpgradeItem(
                title = "Shield Capacity",
                subtitle = "Boosts starship hull points by +15%",
                level = viewModel.prefs.shieldUpgradeLevel,
                cost = viewModel.prefs.shieldUpgradeLevel * 40,
                canAfford = totalCrystals >= viewModel.prefs.shieldUpgradeLevel * 40,
                onUpgrade = { viewModel.upgradeStat("shield") }
            )

            UpgradeItem(
                title = "Plasma Blaster Power",
                subtitle = "Increases laser damage by +15%",
                level = viewModel.prefs.blasterUpgradeLevel,
                cost = viewModel.prefs.blasterUpgradeLevel * 40,
                canAfford = totalCrystals >= viewModel.prefs.blasterUpgradeLevel * 40,
                onUpgrade = { viewModel.upgradeStat("blaster") }
            )

            UpgradeItem(
                title = "Fire Rate Accelerator",
                subtitle = "Enhances auto-cannon firing frequency by +12%",
                level = viewModel.prefs.fireRateUpgradeLevel,
                cost = viewModel.prefs.fireRateUpgradeLevel * 40,
                canAfford = totalCrystals >= viewModel.prefs.fireRateUpgradeLevel * 40,
                onUpgrade = { viewModel.upgradeStat("firerate") }
            )

            UpgradeItem(
                title = "Nova Bomb Reactor",
                subtitle = "Accelerates bomb passive charging by +20%",
                level = viewModel.prefs.bombChargeUpgradeLevel,
                cost = viewModel.prefs.bombChargeUpgradeLevel * 50,
                canAfford = totalCrystals >= viewModel.prefs.bombChargeUpgradeLevel * 50,
                onUpgrade = { viewModel.upgradeStat("bomb") }
            )

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun UpgradeItem(
    title: String,
    subtitle: String,
    level: Int,
    cost: Int,
    canAfford: Boolean,
    onUpgrade: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0x33101E3C)),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp)
            .border(1.dp, Color(0x3300E5FF), RoundedCornerShape(12.dp))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = "$title (Lv. $level)", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Text(text = subtitle, fontSize = 11.sp, color = Color.White.copy(alpha = 0.7f))
            }
            Spacer(modifier = Modifier.width(12.dp))
            Button(
                onClick = onUpgrade,
                enabled = canAfford,
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF00E5FF),
                    contentColor = Color.Black
                )
            ) {
                Text(text = "💎 $cost", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun PauseOverlay(
    onResume: () -> Unit,
    onRestart: () -> Unit,
    onQuit: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xDD040612)),
        contentAlignment = Alignment.Center
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0D142E)),
            shape = RoundedCornerShape(18.dp),
            modifier = Modifier
                .padding(32.dp)
                .fillMaxWidth()
                .border(1.5.dp, Color(0xFF00E5FF), RoundedCornerShape(18.dp))
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "MISSION PAUSED",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    fontFamily = FontFamily.Monospace
                )
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = onResume,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF), contentColor = Color.Black),
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) {
                    Text("RESUME MISSION", fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedButton(
                    onClick = onRestart,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) {
                    Text("RESTART WAVE", color = Color.White)
                }
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedButton(
                    onClick = onQuit,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) {
                    Text("RETURN TO BASE", color = Color(0xFFFF5252))
                }
            }
        }
    }
}

@Composable
fun GameOverOverlay(
    stats: GameStats,
    highScore: Int,
    onRetry: () -> Unit,
    onHangar: () -> Unit,
    onMenu: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xEE060714)),
        contentAlignment = Alignment.Center
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F1532)),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier
                .padding(28.dp)
                .fillMaxWidth()
                .border(2.dp, Color(0xFFFF1744), RoundedCornerShape(20.dp))
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "MISSION TERMINATED",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Black,
                    color = Color(0xFFFF5252),
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = "SHIP HULL CRITICAL FAILURE",
                    fontSize = 11.sp,
                    color = Color.White.copy(alpha = 0.6f),
                    letterSpacing = 2.sp,
                    modifier = Modifier.padding(bottom = 20.dp)
                )

                // Stats breakdown
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0x44000000)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        StatRow(label = "FINAL SCORE", value = "${stats.score}", highlight = true)
                        StatRow(label = "HIGH SCORE", value = "$highScore")
                        StatRow(label = "WAVES SURVIVED", value = "${stats.wave}")
                        StatRow(label = "ENEMIES DESTROYED", value = "${stats.enemiesDefeated}")
                        StatRow(label = "CRYSTALS SALVAGED", value = "💎 ${stats.crystalsCollected}")
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = onRetry,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF), contentColor = Color.Black),
                    modifier = Modifier.fillMaxWidth().height(50.dp).testTag("game_over_retry_btn")
                ) {
                    Icon(imageVector = Icons.Default.Refresh, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("DEPLOY AGAIN", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedButton(
                    onClick = onHangar,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) {
                    Text("🛠️ UPGRADE IN HANGAR", color = Color.White)
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedButton(
                    onClick = onMenu,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) {
                    Text("MAIN MENU", color = Color.White.copy(alpha = 0.8f))
                }
            }
        }
    }
}

@Composable
fun StatRow(label: String, value: String, highlight: Boolean = false) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, fontSize = 12.sp, color = Color.Gray, fontWeight = FontWeight.SemiBold)
        Text(
            text = value,
            fontSize = if (highlight) 18.sp else 14.sp,
            fontWeight = FontWeight.Bold,
            color = if (highlight) Color(0xFFFFD700) else Color.White
        )
    }
}

@Composable
fun HowToPlayOverlay(
    onBack: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xF506081B))
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                }
                Text(
                    text = "PILOT MANUAL",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    fontFamily = FontFamily.Monospace
                )
                Spacer(modifier = Modifier.size(48.dp))
            }

            Spacer(modifier = Modifier.height(16.dp))

            ManualSection(
                title = "🎮 CONTROLS",
                content = "Touch and drag anywhere on the screen to maneuver your starfighter. Your ship continuously auto-fires twin plasma blasters."
            )

            ManualSection(
                title = "⚡ POWER-UPS",
                content = "Destroyed enemies drop glowing power orbs:\n• ⚡ TRIPLE LASER: Tri-directional spread blasters\n• 🔥 HYPER FIRE: Double weapon firing rate\n• 🛡️ SHIELD REPAIR: Restores 45% hull barrier\n• 💣 NOVA BOMB: Instantly charges superweapon\n• 💎 STAR CRYSTALS: Currency for hangar upgrades"
            )

            ManualSection(
                title = "💣 NOVA BOMB",
                content = "Passive reactor and enemy kills charge the Nova Bomb. When charged to 100%, tap the Nova Bomb button to trigger a screen-wide EMP that vaporizes all enemy projectiles and deals 350+ damage to all hostiles!"
            )

            ManualSection(
                title = "🔥 COMBO SYSTEM",
                content = "Vaporizing enemies in rapid succession increments your combo multiplier up to 5x for massive score boosts!"
            )

            ManualSection(
                title = "👾 BOSS TITANS",
                content = "Every 5 waves, an armored Nebula Titan warship enters the sector with multi-phase laser barrages. Defeat the Titan for huge crystal rewards!"
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = onBack,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF), contentColor = Color.Black),
                modifier = Modifier.fillMaxWidth().height(50.dp)
            ) {
                Text("UNDERSTOOD, PILOT", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun ManualSection(title: String, content: String) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0x33101E42)),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .border(1.dp, Color(0x3300E5FF), RoundedCornerShape(12.dp))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(text = title, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF00E5FF))
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = content, fontSize = 12.sp, color = Color.White.copy(alpha = 0.85f), lineHeight = 18.sp)
        }
    }
}

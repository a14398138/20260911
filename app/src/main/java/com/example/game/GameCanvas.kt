package com.example.game

import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun GameCanvas(
    viewModel: GameViewModel,
    modifier: Modifier = Modifier
) {
    val textPaint = Paint().apply {
        isAntiAlias = true
        textAlign = Paint.Align.CENTER
        textSize = 42f
        isFakeBoldText = true
    }

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = { offset ->
                        viewModel.updatePlayerTarget(offset.x, offset.y)
                    }
                )
            }
            .pointerInput(Unit) {
                detectDragGestures { change, _ ->
                    change.consume()
                    viewModel.updatePlayerTarget(change.position.x, change.position.y)
                }
            }
    ) {
        val width = size.width
        val height = size.height
        viewModel.initStarfield(width, height)

        val shakeOffset = if (viewModel.screenShake > 0) {
            Offset(
                (Math.random().toFloat() * 2f - 1f) * viewModel.screenShake,
                (Math.random().toFloat() * 2f - 1f) * viewModel.screenShake
            )
        } else {
            Offset.Zero
        }

        // Draw deep cosmic space background
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color(0xFF060714),
                    Color(0xFF0C0F28),
                    Color(0xFF080918)
                )
            )
        )

        // Draw Nebulae
        for (nebula in viewModel.nebulae) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        nebula.color.copy(alpha = nebula.alpha),
                        Color.Transparent
                    ),
                    center = Offset(nebula.x, nebula.y) + shakeOffset,
                    radius = nebula.radius
                ),
                radius = nebula.radius,
                center = Offset(nebula.x, nebula.y) + shakeOffset
            )
        }

        // Draw Starfield
        for (star in viewModel.stars) {
            drawCircle(
                color = star.color.copy(alpha = star.alpha),
                radius = star.size,
                center = Offset(star.x, star.y) + shakeOffset
            )
        }

        // Draw Particles (thrusters, explosions, sparks)
        for (particle in viewModel.particles) {
            drawCircle(
                color = particle.color.copy(alpha = particle.alpha.coerceIn(0f, 1f)),
                radius = particle.radius,
                center = Offset(particle.x, particle.y) + shakeOffset
            )
        }

        // Draw PowerUps
        for (pu in viewModel.powerUps) {
            val center = Offset(pu.x, pu.y) + shakeOffset
            val pulse = (sin(pu.pulsePhase.toDouble()).toFloat() * 0.2f + 1.0f)
            val r = pu.radius * pulse

            // Glowing aura
            drawCircle(
                color = pu.type.color.copy(alpha = 0.35f),
                radius = r * 1.5f,
                center = center
            )
            // Ring
            drawCircle(
                color = pu.type.color,
                radius = r,
                center = center,
                style = Stroke(width = 4f)
            )
            // Core
            drawCircle(
                color = Color.White.copy(alpha = 0.9f),
                radius = r * 0.45f,
                center = center
            )
        }

        // Draw Bullets
        for (b in viewModel.bullets) {
            val pos = Offset(b.x, b.y) + shakeOffset
            // Outer glow
            drawCircle(
                color = b.color.copy(alpha = 0.4f),
                radius = b.radius * 2.2f,
                center = pos
            )
            // Laser bolt capsule
            drawLine(
                color = b.color,
                start = Offset(b.x, b.y - b.radius * 2f) + shakeOffset,
                end = Offset(b.x, b.y + b.radius * 2f) + shakeOffset,
                strokeWidth = b.radius * 1.4f
            )
            // Inner hot core
            drawLine(
                color = Color.White,
                start = Offset(b.x, b.y - b.radius) + shakeOffset,
                end = Offset(b.x, b.y + b.radius) + shakeOffset,
                strokeWidth = b.radius * 0.7f
            )
        }

        // Draw Enemies
        for (enemy in viewModel.enemies) {
            drawEnemy(enemy, shakeOffset)
        }

        // Draw Player Ship if playing
        if (viewModel.gameState.value == GameState.PLAYING) {
            drawPlayer(viewModel.player.value, viewModel.selectedShip.value, shakeOffset)
        }

        // Draw Floating Texts
        for (ft in viewModel.floatingTexts) {
            textPaint.color = android.graphics.Color.argb(
                (ft.alpha.coerceIn(0f, 1f) * 255).toInt(),
                (ft.color.red * 255).toInt(),
                (ft.color.green * 255).toInt(),
                (ft.color.blue * 255).toInt()
            )
            drawContext.canvas.nativeCanvas.drawText(
                ft.text,
                ft.x + shakeOffset.x,
                ft.y + shakeOffset.y,
                textPaint
            )
        }
    }
}

private fun DrawScope.drawPlayer(player: Player, ship: ShipType, shake: Offset) {
    val px = player.x + shake.x
    val py = player.y + shake.y
    val r = player.radius

    // Invulnerability shield bubble
    if (player.invulnerableTimer > 0) {
        val shieldColor = Color(0xFF00E5FF).copy(alpha = (sin(player.invulnerableTimer * 20) * 0.2f + 0.35f).toFloat())
        drawCircle(
            color = shieldColor,
            radius = r * 1.6f,
            center = Offset(px, py)
        )
        drawCircle(
            color = Color(0xFF80D8FF),
            radius = r * 1.6f,
            center = Offset(px, py),
            style = Stroke(width = 3f)
        )
    }

    // Engine thruster glow
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(Color(0xFFFF9100), Color.Transparent),
            center = Offset(px, py + r * 0.7f),
            radius = r * 0.9f
        ),
        radius = r * 0.9f,
        center = Offset(px, py + r * 0.7f)
    )

    // Ship Hull Path
    val path = Path().apply {
        when (ship) {
            ShipType.PHOENIX_AEGIS -> {
                // Broad armored hull
                moveTo(px, py - r * 1.2f)
                lineTo(px + r * 0.7f, py - r * 0.3f)
                lineTo(px + r * 1.3f, py + r * 0.9f)
                lineTo(px + r * 0.4f, py + r * 0.7f)
                lineTo(px, py + r * 0.5f)
                lineTo(px - r * 0.4f, py + r * 0.7f)
                lineTo(px - r * 1.3f, py + r * 0.9f)
                lineTo(px - r * 0.7f, py - r * 0.3f)
                close()
            }
            ShipType.QUANTUM_PHANTOM -> {
                // Needle stealth hull
                moveTo(px, py - r * 1.4f)
                lineTo(px + r * 0.4f, py - r * 0.2f)
                lineTo(px + r * 1.2f, py + r * 0.7f)
                lineTo(px + r * 0.2f, py + r * 0.5f)
                lineTo(px, py + r * 0.8f)
                lineTo(px - r * 0.2f, py + r * 0.5f)
                lineTo(px - r * 1.2f, py + r * 0.7f)
                lineTo(px - r * 0.4f, py - r * 0.2f)
                close()
            }
            else -> {
                // Void Striker - Classic delta fighter
                moveTo(px, py - r * 1.3f)
                lineTo(px + r * 0.5f, py)
                lineTo(px + r * 1.1f, py + r * 0.9f)
                lineTo(px + r * 0.3f, py + r * 0.6f)
                lineTo(px, py + r * 0.4f)
                lineTo(px - r * 0.3f, py + r * 0.6f)
                lineTo(px - r * 1.1f, py + r * 0.9f)
                lineTo(px - r * 0.5f, py)
                close()
            }
        }
    }

    // Hull fill
    drawPath(
        path = path,
        brush = Brush.verticalGradient(
            colors = listOf(
                Color.White,
                ship.primaryColor,
                ship.accentColor
            ),
            startY = py - r * 1.3f,
            endY = py + r * 0.9f
        )
    )

    // Hull outline
    drawPath(
        path = path,
        color = Color(0xFFFFFFFF).copy(alpha = 0.8f),
        style = Stroke(width = 2.5f)
    )

    // Cockpit canopy
    drawOval(
        color = Color(0xFFFFF176),
        topLeft = Offset(px - r * 0.18f, py - r * 0.6f),
        size = Size(r * 0.36f, r * 0.75f)
    )
}

private fun DrawScope.drawEnemy(enemy: Enemy, shake: Offset) {
    val ex = enemy.x + shake.x
    val ey = enemy.y + shake.y
    val r = enemy.radius

    when (enemy.type) {
        EnemyType.ASTEROID -> {
            // Rough faceted asteroid
            val path = Path()
            val points = 8
            for (i in 0 until points) {
                val angle = (i.toFloat() / points) * 2f * Math.PI.toFloat() + (enemy.angle * Math.PI.toFloat() / 180f)
                val variation = if (i % 2 == 0) r else r * 0.75f
                val px = ex + cos(angle) * variation
                val py = ey + sin(angle) * variation
                if (i == 0) path.moveTo(px, py) else path.lineTo(px, py)
            }
            path.close()

            drawPath(
                path = path,
                brush = Brush.linearGradient(
                    colors = listOf(Color(0xFF78909C), Color(0xFF37474F)),
                    start = Offset(ex - r, ey - r),
                    end = Offset(ex + r, ey + r)
                )
            )
            drawPath(path = path, color = Color(0xFFB0BEC5), style = Stroke(width = 2.5f))
            // Craters
            drawCircle(Color(0xFF263238), r * 0.22f, Offset(ex - r * 0.3f, ey - r * 0.2f))
            drawCircle(Color(0xFF263238), r * 0.18f, Offset(ex + r * 0.25f, ey + r * 0.3f))
        }

        EnemyType.DRONE -> {
            // Fast arrowhead drone with glowing red eye
            val path = Path().apply {
                moveTo(ex, ey + r)
                lineTo(ex - r * 0.8f, ey - r * 0.8f)
                lineTo(ex, ey - r * 0.3f)
                lineTo(ex + r * 0.8f, ey - r * 0.8f)
                close()
            }
            drawPath(
                path = path,
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0xFFFFB300), Color(0xFFFF6F00)),
                    startY = ey - r,
                    endY = ey + r
                )
            )
            drawPath(path = path, color = Color.White, style = Stroke(width = 2f))
            // Glowing core
            drawCircle(Color(0xFFFF1744), r * 0.3f, Offset(ex, ey))
        }

        EnemyType.SCOUT -> {
            // Twin-wing scout
            val path = Path().apply {
                moveTo(ex, ey + r * 1.1f)
                lineTo(ex + r * 0.9f, ey)
                lineTo(ex + r * 1.1f, ey - r * 0.9f)
                lineTo(ex, ey - r * 0.5f)
                lineTo(ex - r * 1.1f, ey - r * 0.9f)
                lineTo(ex - r * 0.9f, ey)
                close()
            }
            drawPath(
                path = path,
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0xFFFF4081), Color(0xFFC2185B)),
                    startY = ey - r,
                    endY = ey + r
                )
            )
            drawPath(path = path, color = Color(0xFFFF80AB), style = Stroke(width = 2.5f))
            drawCircle(Color(0xFF00E5FF), r * 0.25f, Offset(ex, ey))
        }

        EnemyType.HEAVY_CRUISER -> {
            // Armored dreadnought
            val path = Path().apply {
                moveTo(ex, ey + r * 1.2f)
                lineTo(ex + r * 0.6f, ey + r * 0.4f)
                lineTo(ex + r * 1.2f, ey - r * 0.6f)
                lineTo(ex + r * 0.7f, ey - r * 1.1f)
                lineTo(ex - r * 0.7f, ey - r * 1.1f)
                lineTo(ex - r * 1.2f, ey - r * 0.6f)
                lineTo(ex - r * 0.6f, ey + r * 0.4f)
                close()
            }
            drawPath(
                path = path,
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0xFFE53935), Color(0xFFB71C1C), Color(0xFF4A148C)),
                    startY = ey - r * 1.1f,
                    endY = ey + r * 1.2f
                )
            )
            drawPath(path = path, color = Color(0xFFFFCDD2), style = Stroke(width = 3f))
            // Cannons
            drawRect(Color(0xFFFFEB3B), Offset(ex - r * 0.8f, ey - r * 0.2f), Size(r * 0.25f, r * 0.6f))
            drawRect(Color(0xFFFFEB3B), Offset(ex + r * 0.55f, ey - r * 0.2f), Size(r * 0.25f, r * 0.6f))
            // Bridge
            drawCircle(Color(0xFF00E5FF), r * 0.3f, Offset(ex, ey - r * 0.2f))
        }

        EnemyType.NEBULA_BOSS -> {
            // Colossal Boss: Nebula Titan
            // Outer energy aura
            drawCircle(
                color = Color(0xFFFF1744).copy(alpha = 0.22f),
                radius = r * 1.35f,
                center = Offset(ex, ey)
            )

            // Main boss chassis
            val path = Path().apply {
                moveTo(ex, ey + r * 1.15f)
                lineTo(ex + r * 0.5f, ey + r * 0.6f)
                lineTo(ex + r * 1.35f, ey + r * 0.1f)
                lineTo(ex + r * 1.2f, ey - r * 0.8f)
                lineTo(ex + r * 0.6f, ey - r * 1.1f)
                lineTo(ex, ey - r * 0.7f)
                lineTo(ex - r * 0.6f, ey - r * 1.1f)
                lineTo(ex - r * 1.2f, ey - r * 0.8f)
                lineTo(ex - r * 1.35f, ey + r * 0.1f)
                lineTo(ex - r * 0.5f, ey + r * 0.6f)
                close()
            }
            drawPath(
                path = path,
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0xFFFF1744), Color(0xFFD50000), Color(0xFF4A148C), Color(0xFF1A0033)),
                    startY = ey - r * 1.1f,
                    endY = ey + r * 1.2f
                )
            )
            drawPath(path = path, color = Color(0xFFFF8A80), style = Stroke(width = 4f))

            // Pulsing reactor core
            drawCircle(Color(0xFFFFD600), r * 0.42f, Offset(ex, ey))
            drawCircle(Color.White, r * 0.22f, Offset(ex, ey))

            // Weapon pods
            drawCircle(Color(0xFF00E5FF), r * 0.22f, Offset(ex - r * 0.9f, ey + r * 0.2f))
            drawCircle(Color(0xFF00E5FF), r * 0.22f, Offset(ex + r * 0.9f, ey + r * 0.2f))
        }
    }

    // Health bar above enemy (for heavy cruisers and boss)
    if (enemy.hp < enemy.maxHp && enemy.type != EnemyType.NEBULA_BOSS) {
        val barWidth = r * 1.6f
        val barHeight = 6f
        val hpPct = (enemy.hp / enemy.maxHp).coerceIn(0f, 1f)
        val barLeft = ex - barWidth / 2f
        val barTop = ey - r - 16f

        drawRect(Color(0x88000000), Offset(barLeft, barTop), Size(barWidth, barHeight))
        drawRect(
            color = if (hpPct > 0.5f) Color(0xFF00E676) else Color(0xFFFF1744),
            topLeft = Offset(barLeft, barTop),
            size = Size(barWidth * hpPct, barHeight)
        )
    }
}

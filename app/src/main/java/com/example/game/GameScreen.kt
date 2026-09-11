package com.example.game

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun GameScreen(
    viewModel: GameViewModel = viewModel(),
    modifier: Modifier = Modifier
) {
    val gameState by viewModel.gameState.collectAsState()

    // 60fps Game loop tied to display sync
    LaunchedEffect(Unit) {
        var lastTime = withFrameNanos { it }
        while (true) {
            withFrameNanos { now ->
                val dt = ((now - lastTime) / 1_000_000_000f).coerceIn(0.001f, 0.05f)
                lastTime = now
                viewModel.update(dt)
            }
        }
    }

    // Android back handler
    BackHandler(enabled = gameState != GameState.MENU) {
        when (gameState) {
            GameState.PLAYING -> viewModel.pauseGame()
            GameState.PAUSED -> viewModel.goToMenu()
            GameState.HANGAR, GameState.HOW_TO_PLAY, GameState.GAME_OVER, GameState.VICTORY -> viewModel.goToMenu()
            GameState.MENU -> { /* default exit app */ }
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        GameCanvas(viewModel = viewModel)
        GameOverlayUI(viewModel = viewModel)
    }
}

# Stellar Strike: Cosmic Vanguard 🚀

[![Build and Release APK](https://github.com/a14398138/20260911/actions/workflows/build-and-release.yml/badge.svg)](https://github.com/a14398138/20260911/actions/workflows/build-and-release.yml)

A retro-futuristic arcade space combat Android game built with **Kotlin** and **Jetpack Compose**.

---

## 🌟 Game Highlights

- **Dynamic Touch Flight**: Smooth, responsive touch-drag piloting with continuous twin plasma blasters.
- **60 FPS Compose Canvas**: Multi-layered parallax cosmic starfield, nebula particle clouds, glowing laser trajectories, and animated explosions.
- **Procedural Retro Synthesizer Audio**: Zero external audio dependencies! Waveforms synthesized via Android `AudioTrack` for instant, low-latency sound effects.
- **Haptic Feedback**: Dynamic vibration impulses on impacts, explosions, and bomb shockwaves.
- **Formidable Hostiles & Boss Titans**:
  - *Drones*: Agile interceptors with crimson optics.
  - *Asteroids*: Tumbling space rocks that fracture and explode.
  - *Scouts*: Armed raiders with twin forward cannons.
  - *Heavy Dreadnoughts*: High-armor gunboats firing heavy salvos.
  - *Nebula Titan (Boss)*: Enters every 5 waves with multi-phase laser rings, segmented armor, and rage phases.
- **Power-Ups**:
  - ⚡ **Triple Laser**: Tri-directional spread blasters.
  - 🔥 **Hyper Fire**: Doubles auto-firing frequency.
  - 🛡️ **Shield Repair**: Restores hull barriers.
  - 💣 **Nova Bomb**: Screen-wide EMP blast that clears all hostile projectiles and heavily damages enemies.
  - 💎 **Star Crystals**: Salvaged materials used for hangar upgrades.
- **Ship Hangar & Systems Upgrades**:
  - Unlock new starfighters: *Void Striker*, *Phoenix Aegis*, and *Quantum Phantom*.
  - Upgrade Shield Capacity, Laser Power, Firing Rate, and Nova Bomb Recharging.
- **Leaderboard Persistence**: Saves high scores, crystal balances, and system levels locally.

---

## 📦 Releases & APK Downloads

Pre-built uncompressed Android APK files are available directly on the **[Releases](https://github.com/a14398138/20260911/releases)** page:
- **`StellarStrike-v1.0.0-release.apk`**: Release build.
- **`StellarStrike-v1.0.0-debug.apk`**: Debug build for immediate testing.

---

## 🛠️ Build & CI/CD

Built automatically via GitHub Actions upon push to `main` and release tags (`v*`).

```bash
# Build locally
./gradlew assembleRelease assembleDebug
```

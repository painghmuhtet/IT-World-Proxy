package com.itworld.vpn

import android.content.Context
import com.wireguard.android.backend.GoBackend
import com.wireguard.android.backend.Tunnel
import com.wireguard.config.Config
import java.io.ByteArrayInputStream

class WireGuardController(context: Context) {
    private val appContext = context.applicationContext
    private val backend = GoBackend(appContext)

    private val tunnel = object : Tunnel {
        override fun getName(): String = "itworld"

        override fun onStateChange(newState: Tunnel.State) {
            // State is read from the backend by the controller.
        }
    }

    fun isVpnPermissionRequired(): Boolean =
        android.net.VpnService.prepare(appContext) != null

    fun connect(location: String) {
        val configText = loadConfig(location)
        val config = Config.parse(ByteArrayInputStream(configText.toByteArray(Charsets.UTF_8)))
        backend.setState(tunnel, Tunnel.State.UP, config)
    }

    fun disconnect() {
        backend.setState(tunnel, Tunnel.State.DOWN, null)
    }

    fun isConnected(): Boolean =
        backend.getState(tunnel) == Tunnel.State.UP

    private fun loadConfig(location: String): String {
        val assetName = when (location) {
            "Singapore" -> "singapore.conf"
            "Malaysia" -> "malaysia.conf"
            else -> throw IllegalArgumentException("Unknown location: $location")
        }

        return try {
            appContext.assets.open(assetName).bufferedReader().use { it.readText() }
        } catch (_: Exception) {
            throw IllegalStateException(
                "No WireGuard configuration installed for $location yet."
            )
        }
    }
}

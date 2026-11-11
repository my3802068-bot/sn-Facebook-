package com.example.model

import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.InetSocketAddress
import java.net.Proxy
import java.net.Socket
import java.net.URL

enum class ProxyProtocol(val displayName: String) {
    SOCKS5("SOCKS5"),
    HTTP_TLS("HTTP + TLS"),
    HTTP("HTTP")
}

data class ProxySettings(
    val enabled: Boolean = false,
    val protocol: ProxyProtocol = ProxyProtocol.SOCKS5,
    val host: String = "",
    val port: String = "",
    val username: String = "",
    val password: String = "",
    val isVerified: Boolean = false,
    val lastPingMs: Long = -1L
) {
    val displaySummary: String
        get() {
            if (!enabled || host.isBlank()) return "Proxy Disabled"
            val portStr = if (port.isNotBlank()) ":$port" else ""
            val pingStr = if (lastPingMs > 0) " (${lastPingMs}ms)" else ""
            return "${protocol.displayName}: $host$portStr$pingStr"
        }
}

object ProxyTester {
    suspend fun checkProxy(
        protocol: ProxyProtocol,
        host: String,
        portText: String,
        username: String = "",
        password: String = ""
    ): Result<Long> = withContext(Dispatchers.IO) {
        val port = portText.toIntOrNull()
            ?: return@withContext Result.failure(IllegalArgumentException("Invalid port number. Port must be between 1 and 65535."))

        if (port !in 1..65535) {
            return@withContext Result.failure(IllegalArgumentException("Port must be between 1 and 65535."))
        }

        if (host.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("Host / Server cannot be empty."))
        }

        val startTime = System.currentTimeMillis()

        try {
            when (protocol) {
                ProxyProtocol.SOCKS5 -> {
                    val socket = Socket()
                    socket.connect(InetSocketAddress(host, port), 4500)
                    socket.soTimeout = 4500
                    socket.close()
                    val ping = System.currentTimeMillis() - startTime
                    Result.success(ping.coerceAtLeast(12L))
                }
                ProxyProtocol.HTTP, ProxyProtocol.HTTP_TLS -> {
                    val proxy = Proxy(Proxy.Type.HTTP, InetSocketAddress(host, port))
                    val testUrl = if (protocol == ProxyProtocol.HTTP_TLS) {
                        URL("https://www.google.com")
                    } else {
                        URL("http://www.google.com")
                    }
                    val conn = (testUrl.openConnection(proxy) as HttpURLConnection).apply {
                        connectTimeout = 4500
                        readTimeout = 4500
                        requestMethod = "HEAD"
                        if (username.isNotBlank()) {
                            val cred = Base64.encodeToString(
                                "$username:$password".toByteArray(),
                                Base64.NO_WRAP
                            )
                            setRequestProperty("Proxy-Authorization", "Basic $cred")
                        }
                    }
                    conn.connect()
                    val responseCode = conn.responseCode
                    conn.disconnect()
                    val ping = System.currentTimeMillis() - startTime

                    if (responseCode in 200..399 || responseCode == 407 || responseCode == 404) {
                        Result.success(ping.coerceAtLeast(15L))
                    } else {
                        Result.failure(Exception("Proxy returned HTTP response $responseCode"))
                    }
                }
            }
        } catch (e: Exception) {
            val message = when {
                e.message?.contains("timed out", ignoreCase = true) == true -> "Connection timed out (Proxy is offline)"
                e.message?.contains("refused", ignoreCase = true) == true -> "Connection refused (Port closed or unreachable)"
                e.message?.contains("UnknownHost", ignoreCase = true) == true -> "Unknown host (Check server address)"
                else -> e.localizedMessage ?: "Failed to reach proxy server"
            }
            Result.failure(Exception(message))
        }
    }

    fun applySystemProxy(settings: ProxySettings) {
        if (!settings.enabled || settings.host.isBlank() || settings.port.isBlank()) {
            System.clearProperty("http.proxyHost")
            System.clearProperty("http.proxyPort")
            System.clearProperty("https.proxyHost")
            System.clearProperty("https.proxyPort")
            System.clearProperty("socksProxyHost")
            System.clearProperty("socksProxyPort")
            java.net.Authenticator.setDefault(null)
            return
        }

        when (settings.protocol) {
            ProxyProtocol.SOCKS5 -> {
                System.setProperty("socksProxyHost", settings.host)
                System.setProperty("socksProxyPort", settings.port)
                System.clearProperty("http.proxyHost")
                System.clearProperty("https.proxyHost")
            }
            ProxyProtocol.HTTP -> {
                System.setProperty("http.proxyHost", settings.host)
                System.setProperty("http.proxyPort", settings.port)
                System.clearProperty("socksProxyHost")
            }
            ProxyProtocol.HTTP_TLS -> {
                System.setProperty("http.proxyHost", settings.host)
                System.setProperty("http.proxyPort", settings.port)
                System.setProperty("https.proxyHost", settings.host)
                System.setProperty("https.proxyPort", settings.port)
                System.clearProperty("socksProxyHost")
            }
        }

        if (settings.username.isNotBlank()) {
            java.net.Authenticator.setDefault(object : java.net.Authenticator() {
                override fun getPasswordAuthentication(): java.net.PasswordAuthentication {
                    return java.net.PasswordAuthentication(
                        settings.username,
                        settings.password.toCharArray()
                    )
                }
            })
        } else {
            java.net.Authenticator.setDefault(null)
        }
    }
}

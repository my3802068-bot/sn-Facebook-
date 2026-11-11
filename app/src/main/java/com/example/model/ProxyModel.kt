package com.example.model

import android.util.Base64
import androidx.webkit.ProxyConfig
import androidx.webkit.ProxyController
import androidx.webkit.WebViewFeature
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

data class GeoIpInfo(
    val ip: String = "",
    val country: String = "",
    val city: String = "",
    val flag: String = "🌐",
    val org: String = "",
    val isProxy: Boolean = false,
    val fetchedAt: Long = System.currentTimeMillis()
) {
    val displayLocation: String
        get() {
            if (ip.isBlank()) return "Unknown IP"
            val loc = if (city.isNotBlank()) "$city, $country" else country
            return if (loc.isNotBlank() && loc != "Unknown") "$flag $loc" else "$flag $ip"
        }
}

data class ProxySettings(
    val enabled: Boolean = false,
    val protocol: ProxyProtocol = ProxyProtocol.SOCKS5,
    val host: String = "",
    val port: String = "",
    val username: String = "",
    val password: String = "",
    val isVerified: Boolean = false,
    val lastPingMs: Long = -1L,
    val detectedIp: String = "",
    val detectedCountry: String = "",
    val detectedFlag: String = "🌐"
) {
    val displaySummary: String
        get() {
            if (!enabled || host.isBlank()) return "Proxy Disabled (Direct)"
            val portStr = if (port.isNotBlank()) ":$port" else ""
            val pingStr = if (lastPingMs > 0) " (${lastPingMs}ms)" else ""
            val countryStr = if (detectedCountry.isNotBlank()) " • $detectedFlag $detectedCountry" else ""
            return "${protocol.displayName}: $host$portStr$pingStr$countryStr"
        }
}

object ProxyTester {
    suspend fun checkProxy(
        protocol: ProxyProtocol,
        host: String,
        portText: String,
        username: String = "",
        password: String = ""
    ): Result<Pair<Long, GeoIpInfo?>> = withContext(Dispatchers.IO) {
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
                    val ping = (System.currentTimeMillis() - startTime).coerceAtLeast(15L)

                    // Attempt to fetch GeoIP via proxy
                    val tempSettings = ProxySettings(
                        enabled = true,
                        protocol = protocol,
                        host = host,
                        port = portText,
                        username = username,
                        password = password
                    )
                    val geo = fetchCurrentPublicIp(tempSettings).getOrNull()
                    Result.success(Pair(ping, geo))
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
                    val ping = (System.currentTimeMillis() - startTime).coerceAtLeast(15L)

                    if (responseCode in 200..399 || responseCode == 407 || responseCode == 404) {
                        val tempSettings = ProxySettings(
                            enabled = true,
                            protocol = protocol,
                            host = host,
                            port = portText,
                            username = username,
                            password = password
                        )
                        val geo = fetchCurrentPublicIp(tempSettings).getOrNull()
                        Result.success(Pair(ping, geo))
                    } else {
                        Result.failure(Exception("Proxy returned HTTP response $responseCode"))
                    }
                }
            }
        } catch (e: Exception) {
            val message = when {
                e.message?.contains("timed out", ignoreCase = true) == true -> "Connection timed out (Proxy server is offline)"
                e.message?.contains("refused", ignoreCase = true) == true -> "Connection refused (Port closed or unreachable)"
                e.message?.contains("UnknownHost", ignoreCase = true) == true -> "Unknown host (Check server address)"
                else -> e.localizedMessage ?: "Failed to connect to proxy server"
            }
            Result.failure(Exception(message))
        }
    }

    suspend fun fetchCurrentPublicIp(settings: ProxySettings?): Result<GeoIpInfo> = withContext(Dispatchers.IO) {
        try {
            val url = URL("https://ipwho.is/")
            val connection = if (settings != null && settings.enabled && settings.host.isNotBlank() && settings.port.isNotBlank()) {
                val port = settings.port.toIntOrNull() ?: 80
                val proxy = when (settings.protocol) {
                    ProxyProtocol.SOCKS5 -> Proxy(Proxy.Type.SOCKS, InetSocketAddress(settings.host, port))
                    ProxyProtocol.HTTP, ProxyProtocol.HTTP_TLS -> Proxy(Proxy.Type.HTTP, InetSocketAddress(settings.host, port))
                }
                url.openConnection(proxy) as HttpURLConnection
            } else {
                url.openConnection() as HttpURLConnection
            }

            connection.connectTimeout = 5000
            connection.readTimeout = 5000
            connection.setRequestProperty("User-Agent", "Mozilla/5.0")
            connection.connect()

            val json = connection.inputStream.bufferedReader().use { it.readText() }
            connection.disconnect()

            val ip = json.substringAfter("\"ip\":\"", "").substringBefore("\"")
            val country = json.substringAfter("\"country\":\"", "").substringBefore("\"")
            val city = json.substringAfter("\"city\":\"", "").substringBefore("\"")
            val countryCode = json.substringAfter("\"country_code\":\"", "").substringBefore("\"")
            val org = json.substringAfter("\"isp\":\"", "").substringBefore("\"")

            val flag = if (countryCode.length == 2) {
                try {
                    val firstChar = Character.codePointAt(countryCode, 0) - 0x41 + 0x1F1E6
                    val secondChar = Character.codePointAt(countryCode, 1) - 0x41 + 0x1F1E6
                    String(Character.toChars(firstChar)) + String(Character.toChars(secondChar))
                } catch (e: Exception) {
                    "🌐"
                }
            } else "🌐"

            Result.success(
                GeoIpInfo(
                    ip = if (ip.isNotBlank()) ip else "Unknown",
                    country = if (country.isNotBlank()) country else "Global",
                    city = city,
                    flag = flag,
                    org = org,
                    isProxy = settings?.enabled == true
                )
            )
        } catch (e: Exception) {
            // Fallback to api.ipify.org
            try {
                val ipifyUrl = URL("https://api.ipify.org")
                val conn = (ipifyUrl.openConnection() as HttpURLConnection).apply {
                    connectTimeout = 4000
                    readTimeout = 4000
                }
                val ip = conn.inputStream.bufferedReader().use { it.readText().trim() }
                conn.disconnect()
                Result.success(GeoIpInfo(ip = ip, country = "Online", flag = "🌐"))
            } catch (e2: Exception) {
                Result.failure(e)
            }
        }
    }

    /**
     * Applies the proxy directly to Android WebView using ProxyController.
     * This ensures all Facebook and web page traffic routes through the proxy,
     * genuinely changing the user's IP and country on the website!
     */
    fun applyWebViewProxy(settings: ProxySettings, onApplied: (() -> Unit)? = null) {
        if (WebViewFeature.isFeatureSupported(WebViewFeature.PROXY_OVERRIDE)) {
            if (!settings.enabled || settings.host.isBlank() || settings.port.isBlank()) {
                ProxyController.getInstance().clearProxyOverride({ it.run() }) {
                    onApplied?.invoke()
                }
            } else {
                val rule = when (settings.protocol) {
                    ProxyProtocol.SOCKS5 -> "socks://${settings.host}:${settings.port}"
                    ProxyProtocol.HTTP -> "http://${settings.host}:${settings.port}"
                    ProxyProtocol.HTTP_TLS -> "https://${settings.host}:${settings.port}"
                }
                val config = ProxyConfig.Builder()
                    .addProxyRule(rule)
                    .build()

                ProxyController.getInstance().setProxyOverride(config, { it.run() }) {
                    onApplied?.invoke()
                }
            }
        } else {
            onApplied?.invoke()
        }
    }

    /**
     * Configures Java system properties for background HTTP / socket connections
     */
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

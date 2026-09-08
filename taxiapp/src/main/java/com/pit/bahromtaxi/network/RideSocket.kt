package com.pit.bahromtaxi.network

import okhttp3.Request
import okhttp3.WebSocket
import okhttp3.WebSocketListener

/**
 * Live-обновления статуса поездок с backend (новый заказ виден online-водителям,
 * смена статуса виден пассажиру) без опроса сервера по таймеру.
 */
class RideSocket(private val onEvent: (WsEvent) -> Unit) {

    private val adapter = ApiClient.moshi.adapter(WsEvent::class.java)
    private var socket: WebSocket? = null

    fun connect() {
        val request = Request.Builder().url(NetworkConfig.WS_URL).build()
        socket = ApiClient.okHttpClient.newWebSocket(request, object : WebSocketListener() {
            override fun onMessage(webSocket: WebSocket, text: String) {
                runCatching { adapter.fromJson(text) }.getOrNull()?.let(onEvent)
            }
        })
    }

    fun close() {
        socket?.close(1000, null)
        socket = null
    }
}

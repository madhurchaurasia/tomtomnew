package com.example.poconnbandtomtom

import android.content.Context
import android.content.Intent
import android.widget.Toast
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONObject

class WebSocketClient (private val context: Context): WebSocketListener() {

    override fun onOpen(webSocket: WebSocket, response: Response) {
        super.onOpen(webSocket, response)
        println("WebSocket opened: $response")
    }

    override fun onMessage(webSocket: WebSocket, text: String) {
        super.onMessage(webSocket, text)
        if(text !="Logged order J4 with status delivered")
        {
            val jsonString = text
            val jsonObject = JSONObject(jsonString)
            val routeId = jsonObject.getString("route_id")

            val routeNumber = when (routeId) {
                "route-1" -> 1
                "route-2" -> 2
                "route-3" -> 3
                "route-4" -> 4
                else -> -1 // fallback for unknown route

            }
            println("Route number: $routeNumber")
            val intent = Intent("com.example.poconnbandtomtom.START_NAVIGATION")
            intent.putExtra("route", routeNumber)
            intent.putExtra("isRoute", true)
            context.sendBroadcast(intent)
            println("Message received: $text")



        }
    }

    override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
        super.onClosing(webSocket, code, reason)
        println("WebSocket closing: $reason")
    }

    override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
        super.onClosed(webSocket, code, reason)
        println("WebSocket closed: $reason")
    }

    override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
        super.onFailure(webSocket, t, response)
        t.printStackTrace()
    }
}
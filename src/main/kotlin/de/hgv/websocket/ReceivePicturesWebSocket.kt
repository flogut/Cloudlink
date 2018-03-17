package de.hgv.websocket

import com.github.salomonbrys.kodein.Kodein
import com.github.salomonbrys.kodein.conf.global
import com.github.salomonbrys.kodein.instance
import io.javalin.embeddedserver.jetty.websocket.WsSession
import org.eclipse.jetty.websocket.api.Session
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketConnect
import org.eclipse.jetty.websocket.api.annotations.WebSocket

@WebSocket(maxIdleTime = 1000 * 60 * 60 * 24)
class ReceivePicturesWebSocket {

    private val receivePicturesSessions: MutableList<WsSession> = Kodein.global.instance("receivePicturesSessions")

    @OnWebSocketConnect
    fun onConnect(session: Session) {
        val wsSession = WsSession(session)
        receivePicturesSessions.add(wsSession)
    }

}
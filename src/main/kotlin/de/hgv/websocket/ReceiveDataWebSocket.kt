package de.hgv.websocket

import com.github.salomonbrys.kodein.Kodein
import com.github.salomonbrys.kodein.conf.global
import com.github.salomonbrys.kodein.instance
import io.javalin.embeddedserver.jetty.websocket.WsSession
import org.eclipse.jetty.websocket.api.Session
import org.eclipse.jetty.websocket.api.StatusCode
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketConnect
import org.eclipse.jetty.websocket.api.annotations.WebSocket

@WebSocket(maxIdleTime = 1000 * 60 * 60 * 24)
class ReceiveDataWebSocket {

    private val receiveDataSessions: MutableList<WsSession> = Kodein.global.instance("receiveDataSessions")
    private val receiveTokens: List<String> = Kodein.global.instance("receiveTokens")

    @OnWebSocketConnect
    fun onConnect(session: Session) {
        val wsSession = WsSession(session)
        if (wsSession.queryString() == null || wsSession.anyQueryParamNull("token") || !receiveTokens.contains(
                wsSession.queryParam("token")
            )
        ) {
            wsSession.close(StatusCode.POLICY_VIOLATION, "Unauthorized")
            wsSession.disconnect()
        } else {
            receiveDataSessions.add(wsSession)
        }
    }

}
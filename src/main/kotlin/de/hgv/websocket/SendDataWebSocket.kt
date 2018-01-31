package de.hgv.websocket

import com.github.salomonbrys.kodein.Kodein
import com.github.salomonbrys.kodein.conf.global
import com.github.salomonbrys.kodein.instance
import de.hgv.broadcastMessage
import de.hgv.model.Data
import de.hgv.model.DataType
import de.hgv.transaction
import io.javalin.embeddedserver.jetty.websocket.WsSession
import io.javalin.translator.json.JavalinJacksonPlugin
import org.apache.logging.log4j.LogManager
import org.eclipse.jetty.websocket.api.Session
import org.eclipse.jetty.websocket.api.StatusCode
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketConnect
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketMessage
import org.eclipse.jetty.websocket.api.annotations.WebSocket
import org.hibernate.SessionFactory
import java.util.*

@WebSocket
class SendDataWebSocket {

    private val sessionFactory: SessionFactory = Kodein.global.instance()
    private val sendSessions: MutableList<WsSession> = Kodein.global.instance("sendSessions")
    private val sendTokens: List<String> = Kodein.global.instance("sendTokens")
    private val receiveSessions: MutableList<WsSession> = Kodein.global.instance("receiveDataSessions")

    @OnWebSocketConnect
    fun onConnect(session: Session) {
        val wsSession = WsSession(session)
        if (wsSession.queryString() == null || wsSession.anyQueryParamNull("token") || !sendTokens.contains(
                wsSession.queryParam("token")
            )
        ) {
            wsSession.close(StatusCode.POLICY_VIOLATION, "Unauthorized")
            wsSession.disconnect()
        } else {
            sendSessions.add(wsSession)
        }
    }

    @OnWebSocketMessage
    fun onMessage(text: String) {
        val cmd = text.split(" ")
        if (cmd.size == 1) {
            throw IllegalArgumentException("$cmd is no valid message. Format: <data type> <value>")
        }

        val type = when (cmd[0]) {
            "height" -> DataType.HEIGHT
            "temperature" -> DataType.TEMPERATURE
            else -> throw IllegalArgumentException("${cmd[0]} is no valid type")
        }

        val value = cmd[1].toDoubleOrNull() ?: throw IllegalArgumentException("${cmd[1]} is no double")

        val data = Data(UUID.randomUUID().toString(), type, value, Date())
        storeData(data)

        broadcastMessage(receiveSessions, JavalinJacksonPlugin.toJson(data))
    }

    private fun storeData(data: Data) {
        try {
            transaction(sessionFactory) {
                save(data)
            }
        } catch (exception: Exception) {
            LOGGER.error(exception)
        }
    }

    companion object {

        private val LOGGER = LogManager.getLogger(SendDataWebSocket::class.java)

    }

}
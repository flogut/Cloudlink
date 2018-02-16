package de.hgv.websocket

import com.github.salomonbrys.kodein.Kodein
import com.github.salomonbrys.kodein.conf.global
import com.github.salomonbrys.kodein.instance
import de.hgv.broadcastMessage
import de.hgv.model.Picture
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
import java.io.File
import java.util.*

@WebSocket(maxBinaryMessageSize = 5 * 1024 * 1024, maxIdleTime = 1000 * 60 * 60 * 24)
class SendPictureWebSocket {

    private val sessionFactory: SessionFactory = Kodein.global.instance()
    private val sendSessions: MutableList<WsSession> = Kodein.global.instance("sendSessions")
    private val sendTokens: List<String> = Kodein.global.instance("sendTokens")
    private val receiveSessions: MutableList<WsSession> = Kodein.global.instance("receivePicturesSessions")

    private val picturesDirectory: File = Kodein.global.instance("pictures")

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
    fun onMessage(byteArray: ByteArray, offset: Int, length: Int) {
        val imageBytes = byteArray.sliceArray(offset..(offset + length + 1))

        //Save image
        val time = Date()
        val id = UUID.randomUUID().toString()

        val imageFile = File("${picturesDirectory.path}\\$id.jpg")
        imageFile.writeBytes(imageBytes)

        val picture = Picture(id, time, "jpg")
        storePicture(picture)

        //Broadcast image URL
        broadcastMessage(receiveSessions, JavalinJacksonPlugin.toJson(picture))
    }


    private fun storePicture(picture: Picture) {
        try {
            transaction(sessionFactory) {
                save(picture)
            }
        } catch (exception: Exception) {
            LOGGER.error(exception)
        }
    }

    companion object {

        private val LOGGER = LogManager.getLogger(SendPictureWebSocket::class.java)

    }

}
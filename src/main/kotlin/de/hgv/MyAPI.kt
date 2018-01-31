package de.hgv

import com.github.salomonbrys.kodein.Kodein
import com.github.salomonbrys.kodein.bind
import com.github.salomonbrys.kodein.conf.global
import com.github.salomonbrys.kodein.instance
import de.hgv.model.Data
import de.hgv.model.MyRole
import de.hgv.model.Picture
import de.hgv.model.User
import de.hgv.rest.DataController
import de.hgv.rest.PictureController
import de.hgv.websocket.ReceiveDataWebSocket
import de.hgv.websocket.ReceivePicturesWebSocket
import de.hgv.websocket.SendDataWebSocket
import de.hgv.websocket.SendPictureWebSocket
import io.javalin.Context
import io.javalin.Javalin
import io.javalin.builder.CookieBuilder
import io.javalin.embeddedserver.jetty.websocket.WsSession
import org.apache.logging.log4j.LogManager
import org.hibernate.SessionFactory
import org.hibernate.cfg.Configuration
import java.io.File
import java.nio.charset.Charset
import java.security.MessageDigest
import java.util.*

class MyAPI(private val picturesDirectory: File) {

    private val sessionFactory: SessionFactory
    private val server: Javalin

    private val sendSessions = mutableListOf<WsSession>()
    private val sendTokens = mutableListOf<String>()
    private val receiveDataSessions = mutableListOf<WsSession>()
    private val receivePicturesSessions = mutableListOf<WsSession>()
    private val receiveTokens = mutableListOf<String>()

    init {
        if (!picturesDirectory.exists()) {
            picturesDirectory.mkdirs()
        }

        //Create session factory
        sessionFactory = createSessionFactory()

        //Store stuff for dependency injection
        Kodein.global.addConfig {
            bind<SessionFactory>() with instance(sessionFactory)
            bind<MyAPI>() with instance(this@MyAPI)
            bind<MutableList<WsSession>>("sendSessions") with instance(sendSessions)
            bind<List<String>>("sendTokens") with instance(sendTokens)
            bind<MutableList<WsSession>>("receiveDataSessions") with instance(receiveDataSessions)
            bind<MutableList<WsSession>>("receivePicturesSessions") with instance(receivePicturesSessions)
            bind<List<String>>("receiveTokens") with instance(receiveTokens)
            bind<File>("pictures") with instance(picturesDirectory)
        }

        //Configure Server
        server = Javalin.create().apply {
            port(7000)
            //TODO Remove (Debugging)
            this.enableStandardRequestLogging()
        }

        //Configure WebSockets
        server.ws("/sendData", SendDataWebSocket::class.java)
        server.ws("/sendPictures", SendPictureWebSocket::class.java)
        server.ws("/receiveData", ReceiveDataWebSocket::class.java)
        server.ws("/receivePictures", ReceivePicturesWebSocket::class.java)

        server.start()

        //Handle exceptions
        server.exception(IllegalArgumentException::class.java) { e, ctx ->
            ctx.status(400)
            ctx.result(e.localizedMessage)
        }

        //Control login
        server.accessManager { handler, ctx, permittedRoles ->
            val userRole = getUserRole(ctx)
            if (userRole in permittedRoles) {
                handler.handle(ctx)
            } else {
                ctx.status(401).result("Unauthorized")
            }
        }

        //REST
        val pictureController = PictureController()
        val dataController = DataController()

        server.get("/login", this::login, listOf(MyRole.READ, MyRole.WRITE))
        server.get("/picture", pictureController::getNewestPicture, listOf(MyRole.READ, MyRole.WRITE))
        server.get("/pictures/:id", pictureController::getPicture, listOf(MyRole.READ, MyRole.WRITE))
        server.get("/pictures", pictureController::getAllPictures, listOf(MyRole.READ, MyRole.WRITE))
        server.get("/data", dataController::getData, listOf(MyRole.READ, MyRole.WRITE))

        server.post("/data", dataController::postData, listOf(MyRole.WRITE))
        server.post("/picture", pictureController::postPicture, listOf(MyRole.WRITE))
    }

    private fun login(ctx: Context) {
        val role = getUserRole(ctx)
        if (role == null) {
            ctx.response().status = 401
        }

        val token = UUID.randomUUID().toString()

        if (role == MyRole.WRITE) {
            sendTokens.add(token)
            receiveTokens.add(token)
        } else if (role == MyRole.READ) {
            receiveTokens.add(token)
        }

        //TODO Set secureFlag after implementing an SSL server
        ctx.cookie(CookieBuilder(name = "token", value = token, maxAge = -1, secure = false))
    }

    private fun getUserRole(ctx: Context) = ctx.basicAuthCredentials()?.let { getUserRole(it.username, it.password) }

    private fun getUserRole(username: String, password: String) = try {
        transaction(sessionFactory) {
            val query = createQuery("SELECT U.role FROM User U WHERE U.username = :username AND password = :password")
            query.setParameter("username", username)
            query.setParameter("password", password.hash("SHA-512"))

            val roles = query.list().map { it as MyRole }
            if (roles.isNotEmpty()) roles[0] else null
        }
    } catch (exception: Exception) {
        LOGGER.error(exception)
        null
    }

    private fun createSessionFactory(): SessionFactory {
        val properties = Properties().apply {
            setProperty("hibernate.dialect", "org.hibernate.dialect.MySQLDialect")
            setProperty(
                "hibernate.connection.url",
                "jdbc:mysql://localhost:3306/hibernate?useUnicode=true&useJDBCCompliantTimezoneShift=true&useLegacyDatetimeCode=false&serverTimezone=Europe/Berlin"
            )
            setProperty("hibernate.connection.username", "hibernate")
            setProperty("hibernate.connection.password", "hibernatePassword")
        }

        return Configuration().addProperties(properties)
            .addAnnotatedClass(User::class.java)
            .addAnnotatedClass(Data::class.java)
            .addAnnotatedClass(Picture::class.java)
            .buildSessionFactory()
    }

    companion object {

        private val LOGGER = LogManager.getLogger(MyAPI::class.java)

    }
}

fun broadcastMessage(sessions: List<WsSession>, message: String) = sessions.filter { it.isOpen }.forEach {
    it.send(message)
}

fun String.hash(algorithm: String): String {
    val hexChars = "0123456789ABCDEF"
    val bytes = MessageDigest.getInstance(algorithm).digest(toByteArray(Charset.forName("UTF-8")))
    val result = StringBuilder(bytes.size * 2)

    bytes.map { it.toInt() }.forEach {
        result.append(hexChars[it shr 4 and 0x0f])
        result.append(hexChars[it and 0x0f])
    }

    return result.toString()
}
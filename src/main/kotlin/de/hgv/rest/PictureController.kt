package de.hgv.rest

import com.github.salomonbrys.kodein.Kodein
import com.github.salomonbrys.kodein.conf.global
import com.github.salomonbrys.kodein.instance
import de.hgv.broadcastMessage
import de.hgv.model.Picture
import de.hgv.transaction
import io.javalin.Context
import io.javalin.embeddedserver.jetty.websocket.WsSession
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.message.SimpleMessage
import org.hibernate.SessionFactory
import java.io.File
import java.util.*

class PictureController {

    private val picturesDirectory: File = Kodein.global.instance("pictures")
    private val sessionFactory: SessionFactory = Kodein.global.instance()
    private val receivePicturesSessions: MutableList<WsSession> = Kodein.global.instance("receivePicturesSessions")

    /**
     * Responds with the Base64 encoded image
     */
    fun getPicture(ctx: Context) = getPicture(ctx, ctx.param("id"))

    fun getNewestPicture(ctx: Context) {
        try {
            val image = transaction(sessionFactory) {
                createQuery("From Picture p ORDER BY p.time DESC").setMaxResults(1).uniqueResult() as Picture
            }
            getPicture(ctx, image.id)
        } catch (exception: Exception) {
            ctx.status(500)
            ctx.result("Database error")
            LOGGER.error(exception)
        }

    }

    fun getAllPictures(ctx: Context) {
        try {
            val pictures = transaction(sessionFactory) {
                createQuery("FROM Picture").list().map { it as Picture }
            }

            ctx.json(pictures)
        } catch (exception: Exception) {
            ctx.status(500)
            ctx.result("Database error")
            LOGGER.error(exception)
        }
    }

    fun postPicture(ctx: Context) {
        val uploadedFile = ctx.uploadedFile("picture")

        if (uploadedFile == null) {
            ctx.status(400)
            ctx.result("Missing parameter")
            LOGGER.debug { SimpleMessage("Request missing parameter \"picture\"") }

            return
        }

        val id = UUID.randomUUID().toString()
        val extension = uploadedFile.extension.removePrefix(".")
        val content = uploadedFile.content.readBytes()

        File("${picturesDirectory.path}\\$id.$extension").writeBytes(content)

        val picture = Picture(id, Date(), extension)

        try {
            transaction(sessionFactory) {
                save(picture)
            }
        } catch (exception: Exception) {
            ctx.status(500)
            ctx.result("Database error")
            LOGGER.error(exception)

            return
        }

        broadcastMessage(receivePicturesSessions, Base64.getEncoder().encodeToString(content))
    }

    private fun getPicture(ctx: Context, id: String?) {
        if (id == null) {
            ctx.status(404)
            ctx.result("Not found")
            return
        }

        val type = ctx.queryParam("type") ?: "jpg"

        if (picturesDirectory.listFiles().any { it.nameWithoutExtension == id }) {
            val img = File("${picturesDirectory.path}\\$id.$type")
            val contentType = when (type) {
                "jpg" -> "image/jpeg"
                "png" -> "image/png"
                else -> "image/jpeg"
            }

            ctx.contentType(contentType)
            ctx.result(img.inputStream())
        } else {
            ctx.status(404)
            ctx.result("Not found")
        }
    }

    companion object {
        private val LOGGER = LogManager.getLogger(PictureController::class.java)
    }
}
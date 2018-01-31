package de.hgv.rest

import com.github.salomonbrys.kodein.Kodein
import com.github.salomonbrys.kodein.conf.global
import com.github.salomonbrys.kodein.instance
import de.hgv.broadcastMessage
import de.hgv.model.Data
import de.hgv.model.DataType
import de.hgv.transaction
import io.javalin.Context
import io.javalin.embeddedserver.jetty.websocket.WsSession
import org.apache.logging.log4j.LogManager
import org.hibernate.HibernateException
import org.hibernate.SessionFactory
import java.text.SimpleDateFormat
import java.util.*

class DataController {

    private val sessionFactory: SessionFactory = Kodein.global.instance()
    private val receiveDataSessions: MutableList<WsSession> = Kodein.global.instance("receiveDataSessions")

    fun getData(ctx: Context) {
        val time = ctx.queryParam("time")?.let { SimpleDateFormat("yyyy-mm-dd hh:mm:ss").parse(it) }
        val type = when (ctx.queryParam("type")) {
            "height" -> DataType.HEIGHT
            "temperature" -> DataType.TEMPERATURE
            else -> null
        }

        try {
            val data = transaction(sessionFactory) {
                val query = when {
                    time == null && type == null -> createQuery("FROM Data")
                    time == null && type != null -> createQuery("FROM Data d WHERE d.type=:type").setParameter(
                        "type",
                        type
                    )
                    time != null && type == null -> createQuery("FROM Data d WHERE d.time>:time").setParameter(
                        "time",
                        time
                    )
                    time != null && type != null -> createQuery(
                        "FROM Data d WHERE d.type=:type AND d.time>:time"
                    ).setParameter("type", type).setParameter(
                        "time", time
                    )
                    else -> createQuery("FROM Data")
                }

                query.list().map { it as Data }
            }

            ctx.json(data)
        } catch (exception: Exception) {
            ctx.status(500)
            ctx.result("Database error")
            LOGGER.error(exception)
        }
    }

    fun postData(ctx: Context) {
        val type = when (ctx.queryParam("type")) {
            "height" -> DataType.HEIGHT
            "temperature" -> DataType.TEMPERATURE
            else -> null
        }
        val value = ctx.queryParam("value")?.toDouble()

        if (type == null || value == null) {
            ctx.status(400)
            ctx.result("Missing parameter")

            return
        }

        val data = Data(UUID.randomUUID().toString(), type, value, Date())

        try {
            transaction(sessionFactory) {
                save(data)
            }
        } catch (exception: HibernateException) {
            ctx.status(500)
            ctx.result("Database error")
            LOGGER.error(exception)

            return
        }

        broadcastMessage(receiveDataSessions, "${ctx.queryParam("type")} $value")
    }

    companion object {

        private val LOGGER = LogManager.getLogger(DataController::class.java)

    }

}
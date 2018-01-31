package de.hgv

import org.hibernate.HibernateException
import org.hibernate.Session
import org.hibernate.SessionFactory
import org.hibernate.Transaction
import javax.persistence.PersistenceException

inline fun <T> transaction(sessionFactory: SessionFactory, op: Session.() -> T): T {
    val session = sessionFactory.openSession()
    var transaction: Transaction? = null
    val result: T

    try {
        transaction = session.beginTransaction()

        result = session.op()

        transaction.commit()
    } catch (e: HibernateException) {
        transaction?.rollback()
        throw e
    } catch (e: PersistenceException) {
        transaction?.rollback()
        throw e
    } finally {
        session.close()
    }

    return result
}
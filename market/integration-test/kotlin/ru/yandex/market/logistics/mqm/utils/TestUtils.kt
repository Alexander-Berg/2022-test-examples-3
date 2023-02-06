package ru.yandex.market.logistics.mqm.utils

import org.hibernate.SessionFactory
import org.hibernate.stat.Statistics
import javax.persistence.EntityManagerFactory

fun gatherHibernateStatistic(entityManagerFactory: EntityManagerFactory, block: () -> Unit): Statistics {
    val statistics = entityManagerFactory.unwrap(SessionFactory::class.java).statistics
    statistics.clear()
    statistics.isStatisticsEnabled = true
    block()
    statistics.isStatisticsEnabled = false
    return statistics
}

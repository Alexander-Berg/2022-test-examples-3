package ru.yandex.market.doctor.testutils

import org.slf4j.LoggerFactory
import ru.yandex.market.mboc.common.utils.PGaaSZonkyInitializer

/**
 * @author yuramalinov
 * @created 24.02.2020
 */
class PgTestInitializer : PGaaSZonkyInitializer() {
    override fun exportProperties(config: ConnectionParameters, map: MutableMap<String, Any>) {
        map["doctor.postgresql.url"] = config.url
        map["doctor.postgresql.username"] = config.userName
        map["doctor.postgresql.password"] = config.password
        map["doctor.postgresql.properties"] = ""
    }

    override fun getPortFile() = "doctor-pg.port"

    companion object {
        val log = LoggerFactory.getLogger(PgTestInitializer::class.java)
    }
}

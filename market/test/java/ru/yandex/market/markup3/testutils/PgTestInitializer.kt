package ru.yandex.market.markup3.testutils

import org.slf4j.LoggerFactory
import ru.yandex.market.mboc.common.utils.PGaaSZonkyInitializer

/**
 * @author yuramalinov
 * @created 24.02.2020
 */
class PgTestInitializer : PGaaSZonkyInitializer() {
    override fun exportProperties(config: ConnectionParameters, map: MutableMap<String, Any>) {
        map["markup3.postgresql.url"] = config.url
        map["markup3.postgresql.username"] = config.userName
        map["markup3.postgresql.password"] = config.password
        map["markup3.postgresql.properties"] = ""
    }

    override fun getPortFile() = "content-mapping-pg.port"

    companion object {
        val log = LoggerFactory.getLogger(PgTestInitializer::class.java)
    }
}

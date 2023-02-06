package ru.yandex.market.contentmapping.testutils

import ru.yandex.market.mboc.common.utils.PGaaSZonkyInitializer
import javax.annotation.Nonnull

/**
 * @author yuramalinov
 * @created 24.02.2020
 */
class PgTestInitializer : PGaaSZonkyInitializer() {
    override fun exportProperties(config: ConnectionParameters, map: MutableMap<String, Any>) {
        map["sql.url"] = config.url
        map["market-content-mapping.postgresql.username"] = config.userName
        map["market-content-mapping.postgresql.password"] = config.password
        map["liquibase.tables.schema"] = "default"
        map["sql.pgRootCert"] = ""
    }

    override fun getPortFile() = "content-mapping-pg.port"
}

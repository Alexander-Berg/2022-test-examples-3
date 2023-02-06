package ru.yandex.market.mdm.lib.testutils

import ru.yandex.market.mboc.common.utils.PGaaSOtjInitializer

class PgTestInitializer : PGaaSOtjInitializer() {

    override fun getPortFile() = "http.port"
}

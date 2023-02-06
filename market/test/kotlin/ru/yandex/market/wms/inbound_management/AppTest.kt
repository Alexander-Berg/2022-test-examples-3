package ru.yandex.market.wms.inbound_management

import org.junit.jupiter.api.Test
import ru.yandex.market.wms.common.spring.IntegrationTest

class AppTest : IntegrationTest() {
    @Test
    fun initContext() {
        println("Hello Inbound Management!")
    }
}

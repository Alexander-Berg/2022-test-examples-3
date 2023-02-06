package ru.yandex.market.wms.consolidation.modules.consolidation.websocket

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.ApplicationContext
import ru.yandex.market.wms.common.spring.IntegrationTest
import ru.yandex.market.wms.consolidation.modules.consolidation.websocket.util.ConsolidationWebSocket

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
abstract class WebSocketIntegrationTest : IntegrationTest() {

    @Autowired
    protected lateinit var context: ApplicationContext

    protected fun createSocket(): ConsolidationWebSocket = context.getBean(ConsolidationWebSocket::class.java)

}

package ru.yandex.market.wms.taskrouter.config

import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.context.annotation.Profile
import ru.yandex.market.wms.shared.libs.async.mq.MqAdminClient
import ru.yandex.market.wms.shared.libs.env.conifg.Profiles
import ru.yandex.market.wms.taskrouter.configuration.NodeConfig
import ru.yandex.market.wms.taskrouter.monitoring.service.SessionPerUserMonitor
import ru.yandex.market.wms.taskrouter.notification.websocket.config.auth.WebSocketAuthInboundChannelInterceptor
import javax.annotation.PostConstruct

@TestConfiguration
@Profile(ru.yandex.market.wms.shared.libs.env.conifg.Profiles.TEST)
class TestConfig {

    @Autowired
    @MockBean
    private lateinit var nodeConfig: NodeConfig

    @Autowired
    @MockBean
    private lateinit var mqAdminClient: MqAdminClient

    @MockBean
    private lateinit var webSocketAuthInboundChannelInterceptor: WebSocketAuthInboundChannelInterceptor

    @MockBean
    private lateinit var sessionPerUserMonitor: SessionPerUserMonitor

    @PostConstruct
    fun init() {
        Mockito.`when`(mqAdminClient.fetchKnownDestinations()).thenReturn(listOf())
        Mockito.`when`(nodeConfig.nodeName).thenReturn("testNode")
    }
}

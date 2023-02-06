package ru.yandex.market.wms.consolidation.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.context.annotation.Profile
import org.springframework.core.annotation.Order
import org.springframework.messaging.Message
import org.springframework.messaging.MessageChannel
import org.springframework.messaging.simp.stomp.StompHeaderAccessor
import org.springframework.messaging.support.MessageHeaderAccessor
import ru.yandex.market.wms.auth.core.model.InforAuthentication
import ru.yandex.market.wms.auth.core.websocket.interceptor.AuthInboundChannelInterceptor
import ru.yandex.market.wms.common.model.enums.InforRole
import ru.yandex.market.wms.common.spring.TestSecurityDataProvider
import ru.yandex.market.wms.shared.libs.env.conifg.Profiles

@Configuration
@Profile(ru.yandex.market.wms.shared.libs.env.conifg.Profiles.TEST)
@Order(-1)
class WebSocketTestConfig {
    companion object {
        const val TOKEN = "TEST_TOKEN"
    }

    @Bean
    @Primary
    fun authInboundChannelInterceptor(securityDataProvider: TestSecurityDataProvider): AuthInboundChannelInterceptor =
        object : AuthInboundChannelInterceptor {
            override fun preSend(message: Message<*>, channel: MessageChannel): Message<*> {
                MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor::class.java)?.let { accessor ->
                    accessor.login?.let { user ->
                        val principal = InforAuthentication(user, TOKEN, InforRole.ALL_ROLES)
                        principal.isAuthenticated = true
                        if (accessor.isMutable) {
                            accessor.user = principal
                            securityDataProvider.user = user
                        }
                    }
                }
                return message
            }
        }
}

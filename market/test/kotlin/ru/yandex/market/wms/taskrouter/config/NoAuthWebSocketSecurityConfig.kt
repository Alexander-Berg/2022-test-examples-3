package ru.yandex.market.wms.taskrouter.config

import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.core.annotation.Order
import org.springframework.security.config.annotation.web.socket.AbstractSecurityWebSocketMessageBrokerConfigurer
import ru.yandex.market.wms.shared.libs.env.conifg.Profiles

@Order(1)
@Configuration
@Profile(ru.yandex.market.wms.shared.libs.env.conifg.Profiles.TEST)
class NoAuthWebSocketSecurityConfig : AbstractSecurityWebSocketMessageBrokerConfigurer() {
    override fun sameOriginDisabled(): Boolean {
        return true
    }
}

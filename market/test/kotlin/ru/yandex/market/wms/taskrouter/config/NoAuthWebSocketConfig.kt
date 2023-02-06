package ru.yandex.market.wms.taskrouter.config

import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.messaging.simp.config.MessageBrokerRegistry
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker
import org.springframework.web.socket.config.annotation.StompEndpointRegistry
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer
import ru.yandex.market.wms.shared.libs.env.conifg.Profiles
import ru.yandex.market.wms.taskrouter.notification.websocket.config.WebSocketConfig

@Configuration
@EnableWebSocketMessageBroker
@Profile(ru.yandex.market.wms.shared.libs.env.conifg.Profiles.TEST)
class NoAuthWebSocketConfig : WebSocketMessageBrokerConfigurer {

    override fun configureMessageBroker(config: MessageBrokerRegistry) {
        config.setApplicationDestinationPrefixes(APPLICATION_ENDPOINT)
    }

    override fun registerStompEndpoints(registry: StompEndpointRegistry) = WebSocketConfig.ENDPOINTS.forEach {
        registry
            .addEndpoint(it)
            .setAllowedOriginPatterns("*")
        registry
            .addEndpoint(it)
            .setAllowedOriginPatterns("*")
            .withSockJS()
            .setSessionCookieNeeded(false)
    }

    companion object {
        const val APPLICATION_ENDPOINT = "/app"
    }
}

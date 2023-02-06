package ru.yandex.market.contentmapping.testutils

import org.mockserver.client.MockServerClient
import org.mockserver.integration.ClientAndServer
import org.springframework.context.ApplicationContextInitializer
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.context.event.ContextStoppedEvent
import org.springframework.context.support.GenericApplicationContext
import org.springframework.core.env.MapPropertySource
import java.util.function.Supplier

class MockServerInitializer : ApplicationContextInitializer<ConfigurableApplicationContext> {
    override fun initialize(applicationContext: ConfigurableApplicationContext) {
        val clientAndServer = ClientAndServer.startClientAndServer()
        applicationContext.addApplicationListener { e ->
            if (e is ContextStoppedEvent) {
                clientAndServer.stop()
            }
        }

        (applicationContext as GenericApplicationContext).registerBean(
                "mockServerClient", MockServerClient::class.java, Supplier { clientAndServer })

        applicationContext.environment.propertySources.addFirst(MapPropertySource("mock-server-props", mapOf(
                "mock-server.port" to clientAndServer.localPort
        )))
    }
}

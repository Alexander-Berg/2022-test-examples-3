package ru.yandex.market.mbi.orderservice.api.config

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.testcontainers.containers.FixedHostPortGenericContainer
import org.testcontainers.containers.output.Slf4jLogConsumer
import org.testcontainers.containers.wait.strategy.Wait
import org.testcontainers.junit.jupiter.Testcontainers
import ru.yandex.market.mbi.helpers.LocalEnvOnly
import ru.yandex.market.mbi.helpers.YtCleaner
import ru.yandex.market.mbi.orderservice.common.service.yt.dynamic.TableBindingHolder
import ru.yandex.market.mbi.orderservice.common.util.injectedLogger
import ru.yandex.market.yt.client.YtClientProxy
import ru.yandex.market.yt.client.YtClientProxySource
import ru.yandex.market.yt.client.YtDynamicTableClientFactory
import java.time.Duration

@Configuration
@Profile("localYt")
@Testcontainers
open class LocalYTConfig {

    private val log by injectedLogger()

    @Bean(initMethod = "start")
    @LocalEnvOnly
    open fun ytContainer(): FixedHostPortGenericContainer<*> {
        return FixedHostPortGenericContainer<Nothing>("registry.yandex.net/yt/yt:stable")
            .apply {
                withFixedExposedPort(8002, 80) // http
                withFixedExposedPort(8003, 8003) // rpc
                waitingFor(Wait.forLogMessage(".*Local YT started.*", 1))
                withStartupTimeout(Duration.ofMinutes(2))
                withCommand("--proxy-config {address_resolver={enable_ipv4=%true;enable_ipv6=%false;};coordinator={public_fqdn=\"localhost:8002\"}} --rpc-proxy-count 1 --rpc-proxy-port 8003")
                withLogConsumer(Slf4jLogConsumer(log))
                addExposedPort(8003)
            }
    }

    @Bean(name = ["ytHost"])
    open fun ytProxyHost(@Autowired(required = false) ytContainer: FixedHostPortGenericContainer<*>?): String {
        val proxyFromEnv = System.getenv("YT_PROXY")
        if (proxyFromEnv != null) {
            // using recipe
            return proxyFromEnv
        }

        // assuming that we are running testcontainers
        return "localhost:8002"
    }

    @Bean
    open fun tableMigrations(
        tableBindingStorage: TableBindingHolder,
        clientFactory: YtDynamicTableClientFactory,
        ytHost: String
    ): YtDynamicTableClientFactory.OnDemandInitializer {
        return clientFactory.makeTablesInitializer(
            tableBindingStorage.getAsList(),
            ytHost,
            null
        )
    }

    @Bean
    open fun readOnlyClient(readWriteClient: YtClientProxy, ): YtClientProxySource {
        return YtClientProxySource.singleSource(readWriteClient)
    }

    @Bean
    open fun ytCleaner(rwClient: YtClientProxy, tableBindingHolder: TableBindingHolder): YtCleaner {
        return YtCleaner(rwClient, tableBindingHolder)
    }
}

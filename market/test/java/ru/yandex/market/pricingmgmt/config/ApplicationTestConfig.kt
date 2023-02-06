package ru.yandex.market.pricingmgmt.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import ru.yandex.inside.passport.blackbox2.Blackbox2
import ru.yandex.inside.passport.blackbox2.BlackboxRawRequestExecutor
import ru.yandex.inside.passport.blackbox2.BlackboxRequestExecutorWithRetries
import ru.yandex.market.javaframework.yamlproperties.openapi.OpenApiYaml
import ru.yandex.market.javaframework.yamlproperties.openapi.servers.ServerSettings
import ru.yandex.market.javaframework.yamlproperties.serviceyaml.ServiceYaml
import ru.yandex.market.javaframework.yamlproperties.serviceyaml.clients.ClientServiceProperties
import ru.yandex.market.javaframework.yamlproperties.serviceyaml.javaservice.JavaService
import ru.yandex.misc.io.http.Timeout;

@TestConfiguration
open class ApplicationTestConfig {

    @Value("\${market.pricing-mgmt.blackbox.test.api.url}")
    private val localBlackBoxUrl: String? = null

    // FIXME Удалить. Теперь клиенты генерируются для тестов автоматически если в тестовом ya.make подключено
    //  INCLUDE(${ARCADIA_ROOT}/market/infra/java-application/mj/${MJ_VERSION}/mj.test.ya.make)
    // @Bean
    // open fun clientServicesProperties() : Map<String, ClientServiceProperties> {
    //     val clientServicesProps = HashMap<String, ClientServiceProperties>()
    //
    //     val clientServicesProperties =  ClientServiceProperties()
    //
    //     clientServicesProperties.serviceYaml = ServiceYaml()
    //     clientServicesProperties.serviceYaml.javaService = JavaService()
    //     clientServicesProperties.serviceYaml.javaService.serviceName = "promoboss"
    //     clientServicesProperties.openApiYaml = OpenApiYaml()
    //     clientServicesProperties.openApiYaml.servers = ServerSettings()
    //     clientServicesProperties.openApiYaml.servers.url = "http://localhost:8080"
    //
    //     clientServicesProps["promoservice"] = clientServicesProperties
    //
    //     return clientServicesProps
    // }

    @Bean
    open fun blackboxClient(): Blackbox2 {
        return Blackbox2(
            BlackboxRequestExecutorWithRetries(
                BlackboxRawRequestExecutor(localBlackBoxUrl, Timeout.milliseconds(200)),
                3
            )
        )
    }
}

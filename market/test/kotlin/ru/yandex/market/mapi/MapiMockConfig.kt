package ru.yandex.market.mapi

import org.mockito.Mockito.anyString
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.boot.autoconfigure.data.web.SpringDataWebAutoConfiguration
import org.springframework.boot.autoconfigure.freemarker.FreeMarkerAutoConfiguration
import org.springframework.boot.autoconfigure.gson.GsonAutoConfiguration
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration
import org.springframework.boot.autoconfigure.liquibase.LiquibaseAutoConfiguration
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration
import org.springframework.boot.autoconfigure.quartz.QuartzAutoConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import ru.yandex.market.mapi.client.antirobot.ValidationServiceClient
import ru.yandex.market.mapi.client.cms.TemplatorClient
import ru.yandex.market.mapi.client.fapi.FapiClient
import ru.yandex.market.mapi.client.passport.BlackboxClient
import ru.yandex.market.mapi.client.uaas.UaasClient
import ru.yandex.market.mapi.config.ClientSelector
import ru.yandex.market.mapi.core.MapiContext
import ru.yandex.market.mapi.core.MockContext
import ru.yandex.market.mapi.core.NonEngineConfig
import ru.yandex.market.mapi.core.UserExpInfo
import ru.yandex.passport.tvmauth.TvmClient
import java.util.concurrent.CompletableFuture
import java.util.function.Supplier

/**
 * Mock configuration
 * @author Ilya Kislitsyn / ilyakis@ / 17.01.2022
 */
@EnableAutoConfiguration(
    exclude = [
        DataSourceAutoConfiguration::class, HibernateJpaAutoConfiguration::class,
        LiquibaseAutoConfiguration::class, GsonAutoConfiguration::class, SpringDataWebAutoConfiguration::class,
        FreeMarkerAutoConfiguration::class, QuartzAutoConfiguration::class
    ]
)
@ComponentScan(
    basePackageClasses = [MapiLauncher::class],
    excludeFilters = [ComponentScan.Filter(
        classes = [NonEngineConfig::class]
    )]
)
@Configuration
open class MapiMockConfig {

    @Bean
    open fun tvmClient() = MockContext.registerMock<TvmClient>()

    @Bean
    open fun templatorClient() = MockContext.registerMock<TemplatorClient> { client ->
        whenever(client.getCmsPageTemplateSimple(anyString(), anyString())).then {
            CompletableFuture.completedFuture(Supplier { null })
        }
    }

    @Bean
    open fun fapiClient() = MockContext.registerMock<FapiClient> { client ->
        whenever(client.callResolver(any(), any())).then {
            throw RuntimeException("Fapi exception message here")
        }
    }

    @Bean
    open fun fapiCLientSelector(fapiClient: FapiClient) =
        MockContext.registerMock<ClientSelector<FapiClient>> { selector ->
            whenever(selector.invoke(any())).then {
                fapiClient
            }
        }

    @Bean
    open fun blackboxClientSelector(blackboxClient: BlackboxClient) =
        MockContext.registerMock<ClientSelector<BlackboxClient>> { selector ->
            whenever(selector.invoke(any())).then {
                blackboxClient
            }
        }

    @Bean
    open fun blackboxClient() = MockContext.registerMock<BlackboxClient> { client ->
        whenever(client.checkOauth(any(), any())).then {
            CompletableFuture.failedFuture<Void>(RuntimeException("unmocked blackbox call"))
        }
    }

    @Bean
    open fun validationServiceClient() = MockContext.registerMock<ValidationServiceClient> { client ->
        whenever(client.generateNonce(any())).then {
            CompletableFuture.failedFuture<Void>(RuntimeException("unmocked validation service call"))
        }

        whenever(client.authenticateIos(any())).then {
            CompletableFuture.failedFuture<Void>(RuntimeException("unmocked validation service call"))
        }

        whenever(client.authenticateAndroid(any())).then {
            CompletableFuture.failedFuture<Void>(RuntimeException("unmocked validation service call"))
        }
    }

    @Bean
    open fun uaasClient() = MockContext.registerMock<UaasClient> { client ->
        whenever(client.resolveExps(any(), any())).then {
            Supplier {
                UserExpInfo(
                    version = "test-version",
                    uaasRearrs = linkedSetOf("test-rearr1=0"),
                    forcedTestIds = MapiContext.get().forcedTestIds,
                    forcedRearrs = MapiContext.get().forcedRearrs
                )
            }
        }
    }
}

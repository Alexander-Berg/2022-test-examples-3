package ru.yandex.market.logistics.logistrator.configuration

import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.boot.test.mock.mockito.SpyBean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.EnableAspectJAutoProxy
import org.springframework.context.annotation.Import
import ru.yandex.market.logistics.dbqueue.DbQueueService
import ru.yandex.market.logistics.management.client.LMSClient
import ru.yandex.market.logistics.test.integration.db.zonky.EnableZonkyEmbeddedPostgres
import java.time.Clock

@Configuration
@EnableAspectJAutoProxy(proxyTargetClass = true)
@EnableZonkyEmbeddedPostgres
@EnableAutoConfiguration
@SpyBean(DbQueueService::class)
@MockBean(LMSClient::class, Clock::class)
@Import(WebApplicationConfiguration::class)
class IntegrationTestConfiguration

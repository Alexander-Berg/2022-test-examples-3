package ru.yandex.market.adv.incut.executor

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import org.dbunit.database.DatabaseConfig
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.adv.incut.AbstractFunctionalTest
import ru.yandex.market.adv.incut.integration.saas.service.IncutSaasLogbrokerEvent
import ru.yandex.market.adv.incut.tms.executor.RetrySaasIncutSynchronizationExecutor
import ru.yandex.market.adv.incut.utils.time.toInstantAtUtc3
import ru.yandex.market.common.test.db.DbUnitDataBaseConfig
import ru.yandex.market.common.test.db.DbUnitDataSet
import ru.yandex.market.logbroker.LogbrokerEventPublisher
import java.time.Clock
import java.time.LocalDateTime
import java.time.Month
import java.util.concurrent.CompletableFuture

@DbUnitDataBaseConfig(
    DbUnitDataBaseConfig.Entry(name = DatabaseConfig.FEATURE_ALLOW_EMPTY_FIELDS, value = "true")
)
class RetrySaasIncutSynchronizationExecutorFunctionalTest(
    @Autowired private val vendorPartnerMock: WireMockServer,
    @Autowired private val incutSaasLogbrokerEventPublisher: LogbrokerEventPublisher<IncutSaasLogbrokerEvent>,
    @Autowired private val retrySaasIncutSynchronizationExecutor: RetrySaasIncutSynchronizationExecutor,
    @Autowired private val clock: Clock
) : AbstractFunctionalTest() {

    @DbUnitDataSet(
        before = ["/ru/yandex/market/adv/incut/executor/RetrySaasIncutSynchronizationExecutorFunctionalTest/retrySync/before.csv"],
        after = ["/ru/yandex/market/adv/incut/executor/RetrySaasIncutSynchronizationExecutorFunctionalTest/retrySync/after.csv"]
    )
    @Test
    fun `retry sync`() {
        Mockito.`when`(clock.instant())
            .thenReturn(
                LocalDateTime.of(
                    2021, Month.DECEMBER,
                    17, 15, 8, 0
                ).toInstantAtUtc3()
            )

        Mockito.`when`(incutSaasLogbrokerEventPublisher.publishEventAsync(Mockito.any()))
            .thenReturn(CompletableFuture.completedFuture(null))

        vendorPartnerMock.stubFor(
            WireMock.post(
                "/vendors/19708/models?uid=1186962236"
            ).willReturn(WireMock.okJson(getStringResource("retrySync/models.json")))
        )

        vendorPartnerMock.stubFor(
            WireMock.get(
                "/categories?categoryIds=91491&categoryIds=10498025&onlyWithModels=false"
            ).willReturn(WireMock.okJson(getStringResource("retrySync/categories.json")))
        )

        vendorPartnerMock.stubFor(
            WireMock.get(
                "/vendors/list?vendorId=19708&page=1&size=1"
            ).willReturn(WireMock.okJson(getStringResource("retrySync/vendorsList.json")))
        )

        retrySaasIncutSynchronizationExecutor.doJob(null)
    }

}

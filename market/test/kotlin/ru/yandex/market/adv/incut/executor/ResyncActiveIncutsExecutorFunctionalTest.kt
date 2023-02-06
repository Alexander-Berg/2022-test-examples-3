package ru.yandex.market.adv.incut.executor

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import org.dbunit.database.DatabaseConfig
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.adv.incut.AbstractFunctionalTest
import ru.yandex.market.adv.incut.integration.saas.service.IncutSaasLogbrokerEvent
import ru.yandex.market.adv.incut.tms.executor.ResyncActiveIncutsExecutor
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
class ResyncActiveIncutsExecutorFunctionalTest(
    @Autowired private val vendorPartnerMock: WireMockServer,
    @Autowired private val incutSaasLogbrokerEventPublisher: LogbrokerEventPublisher<IncutSaasLogbrokerEvent>,
    @Autowired private val resyncActiveIncutsExecutor: ResyncActiveIncutsExecutor,
    @Autowired private val clock: Clock
) : AbstractFunctionalTest() {

    @DbUnitDataSet(
        before = ["/ru/yandex/market/adv/incut/executor/ResyncActiveIncutsExecutorFunctionalTest/testActiveSyncExecutor/before.csv"],
        after = ["/ru/yandex/market/adv/incut/executor/ResyncActiveIncutsExecutorFunctionalTest/testActiveSyncExecutor/after.csv"]
    )
    @Test
    fun `test active sync executor`() {
        Mockito.`when`(clock.instant())
            .thenReturn(
                LocalDateTime.of(
                    2022, Month.MARCH,
                    3, 15, 0, 0
                ).toInstantAtUtc3()
            )

        Mockito.`when`(incutSaasLogbrokerEventPublisher.publishEventAsync(Mockito.any()))
            .thenReturn(CompletableFuture.completedFuture(null))

        vendorPartnerMock.stubFor(
            WireMock.get(
                "/vendors/list?vendorId=1&page=1&size=1"
            ).willReturn(WireMock.okJson(getStringResource("/testActiveSyncExecutor/vendorsList.json")))
        )

        resyncActiveIncutsExecutor.doJob(null)

        Mockito.`when`(clock.instant())
            .thenReturn(
                LocalDateTime.of(
                    2022, Month.MARCH,
                    3, 15, 10, 0
                ).toInstantAtUtc3()
            )

        resyncActiveIncutsExecutor.doJob(null)
    }
}

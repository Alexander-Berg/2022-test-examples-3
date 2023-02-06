package ru.yandex.market.adv.incut.executor

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import org.dbunit.database.DatabaseConfig
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.adv.incut.AbstractFunctionalTest
import ru.yandex.market.adv.incut.tms.executor.SendToModerationExecutor
import ru.yandex.market.adv.incut.utils.time.toInstantAtUtc3
import ru.yandex.market.common.test.db.DbUnitDataBaseConfig
import ru.yandex.market.common.test.db.DbUnitDataSet
import java.time.Clock
import java.time.LocalDateTime
import java.time.Month

@DbUnitDataBaseConfig(
    DbUnitDataBaseConfig.Entry(name = DatabaseConfig.FEATURE_ALLOW_EMPTY_FIELDS, value = "true")
)
class SendToModerationExecutorFunctionalTest(
    @Autowired private val sendToModerationExecutor: SendToModerationExecutor,
    @Autowired private val vendorPartnerMock: WireMockServer,
    @Autowired private val clock: Clock
) : AbstractFunctionalTest() {

    @DbUnitDataSet(
        before = ["/ru/yandex/market/adv/incut/executor/SendToModerationExecutorFunctionalTest/doJob/before.csv"],
        after = ["/ru/yandex/market/adv/incut/executor/SendToModerationExecutorFunctionalTest/doJob/after.csv"]
    )
    @Test
    fun `do job`() {
        // mock creation datetime
        Mockito.`when`(clock.instant())
            .thenReturn(
                LocalDateTime.of(
                    2021, Month.DECEMBER,
                    17, 0, 0, 0
                ).toInstantAtUtc3()
            )

        // mock vendorIntegrationService.getCategories
        vendorPartnerMock.stubFor(
            WireMock.get(
                "/categories?categoryIds=55&categoryIds=33&categoryIds=44&onlyWithModels=false"
            ).willReturn(WireMock.okJson(getStringResource("doJob/categories.json")))
        )

        // mock vendorIntegrationService.getModels
        vendorPartnerMock.stubFor(
            WireMock.post(
                "/vendors/2/models/report"
            ).willReturn(WireMock.okJson(getStringResource("doJob/models.json")))
        )

        // mock vendorIntegrationService.getVendors
        vendorPartnerMock.stubFor(
            WireMock.get(
                "/vendors/list?vendorId=2&page=1&size=1"
            ).willReturn(WireMock.okJson(getStringResource("doJob/vendors.json")))
        )

        // mock ticket creation: startrekSession.issues().create
        vendorPartnerMock.stubFor(
            WireMock.post(
                "/v2/issues?fields="
            ).willReturn(WireMock.okJson(getStringResource("doJob/issue.json")))
        )

        sendToModerationExecutor.doJob(null)
    }
}


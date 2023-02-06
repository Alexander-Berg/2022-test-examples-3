package ru.yandex.market.adv.incut.executor

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import org.dbunit.database.DatabaseConfig
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.adv.incut.AbstractFunctionalTest
import ru.yandex.market.adv.incut.tms.executor.CheckModeratedExecutor
import ru.yandex.market.adv.incut.utils.time.toInstantAtUtc3
import ru.yandex.market.common.test.db.DbUnitDataBaseConfig
import ru.yandex.market.common.test.db.DbUnitDataSet
import java.time.Clock
import java.time.LocalDateTime
import java.time.Month

@DbUnitDataBaseConfig(
    DbUnitDataBaseConfig.Entry(name = DatabaseConfig.FEATURE_ALLOW_EMPTY_FIELDS, value = "true")
)
class CheckModeratedExecutorTest(
    @Autowired private val checkModeratedExecutor: CheckModeratedExecutor,
    @Autowired private val vendorPartnerMock: WireMockServer,
    @Autowired private val clock: Clock
) : AbstractFunctionalTest() {

    @DbUnitDataSet(
        before = ["/ru/yandex/market/adv/incut/executor/CheckModeratedExecutorTest/doJob/before.csv"],
        after = ["/ru/yandex/market/adv/incut/executor/CheckModeratedExecutorTest/doJob/after.csv"]
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

        // mock ticket reading: startrekSession.issues().find
        vendorPartnerMock.stubFor(
            WireMock.post(
                "/v2/issues/_search?fields="
            ).willReturn(WireMock.okJson(getStringResource("doJob/issues.json")))
        )

        checkModeratedExecutor.doJob(null)
    }
}

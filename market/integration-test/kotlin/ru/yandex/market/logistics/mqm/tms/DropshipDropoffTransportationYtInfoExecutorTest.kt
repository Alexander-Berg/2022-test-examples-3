package ru.yandex.market.logistics.mqm.tms

import com.github.springtestdbunit.annotation.DatabaseSetup
import com.github.springtestdbunit.annotation.ExpectedDatabase
import com.github.springtestdbunit.assertion.DatabaseAssertionMode
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.anyOrNull
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.whenever
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.logistics.mqm.AbstractContextualTest
import ru.yandex.market.logistics.mqm.configuration.properties.DropshipDropoffTransportationYtInfoExecutorProperties
import ru.yandex.market.logistics.mqm.service.DropshipDropoffTransportationInfoService
import ru.yandex.market.logistics.mqm.service.yt.DropshipDropoffTransportationInfoDto
import ru.yandex.market.logistics.mqm.service.yt.YtDropshipDropoffTransportationInfoService
import ru.yandex.market.logistics.mqm.service.yt.YtService
import java.time.LocalDate

@DisplayName("Тест обновление информации об отгрузках")
class DropshipDropoffTransportationYtInfoExecutorTest : AbstractContextualTest() {

    @Autowired
    private lateinit var ytDropshipDropoffTransportationInfoService: YtDropshipDropoffTransportationInfoService

    @Autowired
    private lateinit var dropshipDropoffTransportationInfoService: DropshipDropoffTransportationInfoService

    @Autowired
    private lateinit var properties: DropshipDropoffTransportationYtInfoExecutorProperties

    @Autowired
    private lateinit var ytService: YtService

    private lateinit var executor: DropshipDropoffTransportationYtInfoExecutor

    @BeforeEach
    fun setup() {
        executor = DropshipDropoffTransportationYtInfoExecutor(
            ytDropshipDropoffTransportationInfoService,
            dropshipDropoffTransportationInfoService,
            properties
        )
    }

    @Test
    @DatabaseSetup("/tms/processDropshipDropoffTransportationYtInfoExecutor/before/success.xml")
    @ExpectedDatabase(
        value = "/tms/processDropshipDropoffTransportationYtInfoExecutor/after/success.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @DisplayName("Успешное обновление информации об отгрузке")
    fun successTest() {
        doReturn(2L).whenever(ytService).getRowCount(any())
        doReturn(
            listOf(
                DropshipDropoffTransportationInfoDto(
                    transportationId = 11,
                    dropshipPartnerId = 12,
                    dropoffLogisticPointId = 13,
                    transportationDate = LocalDate.of(2022, 3, 22)
                ),
                DropshipDropoffTransportationInfoDto(
                    transportationId = 111,
                    dropshipPartnerId = 122,
                    dropoffLogisticPointId = 133,
                    transportationDate = LocalDate.of(2022, 3, 22)
                )
            )
        ).whenever(ytService).readTableFromRowToRow(
            any(),
            eq(DropshipDropoffTransportationInfoDto::class.java),
            any(),
            anyOrNull(),
            any()
        )

        executor.run()
    }
}

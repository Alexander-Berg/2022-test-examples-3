package ru.yandex.market.mbi.orderservice.tms.service.command

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.kotlin.argThat
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.mbi.orderservice.common.commandInvocationTest
import ru.yandex.market.mbi.orderservice.common.persistence.repository.yt.HistoricalReturnEventRepository
import ru.yandex.market.mbi.orderservice.common.persistence.repository.yt.YtReturnRepository
import ru.yandex.market.mbi.orderservice.tms.FunctionalTest
import ru.yandex.market.mbi.orderservice.tms.service.mbi.MbiHistoricalReturnDataService

class GenerateHistoricalLogisticReturnEventsCommandTest : FunctionalTest() {

    @Autowired
    lateinit var returnRepository: YtReturnRepository

    @Autowired
    lateinit var historicalReturnEventRepository: HistoricalReturnEventRepository

    @Test
    fun commandTest() {
        val mbiHistoricalReturnDataService: MbiHistoricalReturnDataService = mock { }
        val command = GenerateHistoricalLogisticReturnEventsCommand(
            returnRepository,
            historicalReturnEventRepository,
            mbiHistoricalReturnDataService
        )

        commandInvocationTest(
            command,
            """generate-return-events --fromOrderId=10 --toOrderId=400"""
        )

        verify(mbiHistoricalReturnDataService)
            .generateLogisticEventsByHistoricalData(argThat { from ->
                assertThat(from).isEqualTo(10)
                true
            }, argThat { to ->
                assertThat(to).isEqualTo(400)
                true
            }, argThat { event ->
                assertThat(event.get()).isEqualTo(Long.MAX_VALUE / 2)
                true
            }, argThat { returnId ->
                assertThat(returnId.get()).isEqualTo(Long.MAX_VALUE / 2)
                true
            })
    }
}

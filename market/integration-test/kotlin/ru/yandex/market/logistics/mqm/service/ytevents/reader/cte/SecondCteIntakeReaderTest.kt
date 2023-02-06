package ru.yandex.market.logistics.mqm.service.ytevents.reader.cte

import com.github.springtestdbunit.annotation.DatabaseSetup
import com.github.springtestdbunit.annotation.ExpectedDatabase
import com.github.springtestdbunit.assertion.DatabaseAssertionMode
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.whenever
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.logistics.mqm.AbstractContextualTest
import ru.yandex.market.logistics.mqm.service.yt.YtService
import ru.yandex.market.logistics.mqm.service.yt.dto.SecondCteIntakeYtRow

@DisplayName("Тесты для SecondCteIntakeReader")
class SecondCteIntakeReaderTest: AbstractContextualTest() {

    @Autowired
    lateinit var ytReader: SecondCteIntakeReader

    @Autowired
    lateinit var ytService: YtService

    @Test
    @DisplayName("Обновление состояния ридера")
    @DatabaseSetup("/service/ytevents/reader/cte/SecondCteIntakeReader/before/setUpEvents.xml")
    @ExpectedDatabase(
        value = "/service/ytevents/reader/cte/SecondCteIntakeReader/after/successProcessorStateSave.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun updateProcessorState() {
        val ytRow = SecondCteIntakeYtRow(
            id = 1234,
            orderId = "235",
            warehouseId = "172",
            intakeTime = "2021-12-16 11:55:00.000000",
        )
        doReturn(listOf(ytRow))
            .whenever(ytService).readTableFromIdToId(any(), any<Class<SecondCteIntakeYtRow>>(), any(), any(), any())
        ytReader.run()
    }

    @Test
    @DisplayName("Не обновлять статус ридера, если прочитано 0 записей")
    @DatabaseSetup("/service/ytevents/reader/cte/SecondCteIntakeReader/before/setUpEvents.xml")
    @ExpectedDatabase(
        value = "/service/ytevents/reader/cte/SecondCteIntakeReader/after/notChangeState.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun doNotUpdateProcessorStateIfReaderZeroRows() {
        doReturn(listOf<SecondCteIntakeYtRow>())
            .whenever(ytService).readTableFromIdToId(any(), any<Class<SecondCteIntakeYtRow>>(), any(), any(), any())
        ytReader.run()
    }
}

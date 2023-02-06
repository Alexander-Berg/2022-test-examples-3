package ru.yandex.market.logistics.mqm.service.ytevents.reader.cte

import com.github.springtestdbunit.annotation.DatabaseSetup
import com.github.springtestdbunit.annotation.ExpectedDatabase
import com.github.springtestdbunit.assertion.DatabaseAssertionMode
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.whenever
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.RowMapper
import ru.yandex.market.logistics.mqm.AbstractContextualTest
import ru.yandex.market.logistics.mqm.service.ytevents.row.FirstCteIntakeYtDto
import java.time.Instant

@DisplayName("Тесты для FirstCteIntakeReader")
internal class FirstCteIntakeReaderTest: AbstractContextualTest() {

    @Autowired
    lateinit var ytReader: FirstCteIntakeReader

    @Autowired
    @Qualifier("yqlJdbcTemplate")
    lateinit var yqlJdbcTemplate: JdbcTemplate

    @Test
    @DisplayName("Обновление состояния ридера")
    @DatabaseSetup("/service/ytevents/reader/cte/FirstCteIntakeReader/before/setUpEvents.xml")
    @ExpectedDatabase(
        value = "/service/ytevents/reader/cte/FirstCteIntakeReader/after/successProcessorStateSave.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun updateProcessorState() {
        val ytDto = FirstCteIntakeYtDto(
            id = 123,
            orderId = "235",
            finishTime = FINISH_TIME,
            reader = FirstCteIntakeReader::class
        )
        whenever(yqlJdbcTemplate.query(any<String>(), any<RowMapper<FirstCteIntakeYtDto>>())).thenReturn(listOf(ytDto))
        ytReader.run()
    }

    @Test
    @DisplayName("Не обновлять статус ридера, если прочитано 0 записей")
    @DatabaseSetup("/service/ytevents/reader/cte/FirstCteIntakeReader/before/setUpEvents.xml")
    @ExpectedDatabase(
        value = "/service/ytevents/reader/cte/FirstCteIntakeReader/after/notChangeState.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun doNotUpdateProcessorStateIfReaderZeroRows() {
        whenever(yqlJdbcTemplate.query(any<String>(), any<RowMapper<FirstCteIntakeYtDto>>())).thenReturn(listOf())
        ytReader.run()
    }

    companion object {
        private val FINISH_TIME = Instant.parse("2021-12-08T19:00:00.00Z")
    }

}

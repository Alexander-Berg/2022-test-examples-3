package ru.yandex.market.logistics.mqm.service.ytevents.reader.courier

import com.github.springtestdbunit.annotation.DatabaseSetup
import com.github.springtestdbunit.annotation.ExpectedDatabase
import com.github.springtestdbunit.assertion.DatabaseAssertionMode
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.anyOrNull
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.whenever
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.mockito.stubbing.Answer
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.inside.yt.kosher.ytree.YTreeMapNode
import ru.yandex.market.logistics.mqm.AbstractContextualTest
import ru.yandex.market.logistics.mqm.service.yt.YtService
import ru.yandex.market.logistics.mqm.service.ytevents.reader.EVENT_TIME_COLUMN
import ru.yandex.market.logistics.mqm.service.ytevents.reader.SHIFT_START_TIME_COLUMN
import ru.yandex.market.logistics.mqm.service.ytevents.reader.USER_SHIFT_RECORD_ID_COLUMN
import ru.yandex.market.logistics.mqm.utils.createYtRow
import java.util.function.Function

@DisplayName("Тесты для YtCourierShiftFinishTimeReader")
class YtCourierShiftFinishTimeReaderTest: AbstractContextualTest() {

    @Autowired
    lateinit var ytReader: YtCourierShiftFinishTimeReader

    @Autowired
    lateinit var ytService: YtService

    @BeforeEach
    fun setUp() {
        doReturn("YtCourierShiftFinishTimeReader")
            .whenever(ytReader).getProcessorStateKey()
    }

    private fun setReadFromYtRows(ytRows: Iterable<YTreeMapNode>) {
        val answer = Answer { invocation ->
            val generateEventsFunction = invocation.getArgument<Function<Iterator<YTreeMapNode>, Pair<Int, Long>>>(2)
            val ytIterator: Iterator<YTreeMapNode> = ytRows.iterator()
            generateEventsFunction.apply(ytIterator)
        }
        Mockito.doAnswer(answer)
            .whenever(ytService)
            .selectRowsFromTable<Any>(any(), anyOrNull(), any())
    }

    @Test
    @DisplayName("Ридер правильно обновляет события, и создает задачи")
    @DatabaseSetup("/service/ytevents/courier/shiftfinishreader/before/setUpEvents.xml")
    @ExpectedDatabase(
        value = "/service/ytevents/courier/shiftfinishreader/after/successProcessorStateSave.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun updateProcessorState() {
        val ytData = listOf(
            mapOf(
                USER_SHIFT_RECORD_ID_COLUMN to 101L,
                SHIFT_START_TIME_COLUMN to "2021-07-29T04:00:00.123456+03:00",
                EVENT_TIME_COLUMN to "2021-07-30T04:00:00.123456+03:00"
            ),
            mapOf(
                USER_SHIFT_RECORD_ID_COLUMN to 102L,
                SHIFT_START_TIME_COLUMN to "2021-07-29T04:00:00.123456+03:00",
                EVENT_TIME_COLUMN to "2021-07-30T13:00:00.123456+03:00"
            )
        )
            .map { createYtRow(it) }
        setReadFromYtRows(ytData)
        ytReader.run()
    }
}


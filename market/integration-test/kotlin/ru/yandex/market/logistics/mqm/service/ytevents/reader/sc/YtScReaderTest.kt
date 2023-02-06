package ru.yandex.market.logistics.mqm.service.ytevents.reader.sc


import com.github.springtestdbunit.annotation.DatabaseSetup
import com.github.springtestdbunit.annotation.ExpectedDatabase
import com.github.springtestdbunit.assertion.DatabaseAssertionMode
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.anyOrNull
import com.nhaarman.mockitokotlin2.whenever
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.Mockito.doAnswer
import org.mockito.stubbing.Answer
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.transaction.support.TransactionOperations
import ru.yandex.inside.yt.kosher.ytree.YTreeMapNode
import ru.yandex.market.logistics.mqm.AbstractContextualTest
import ru.yandex.market.logistics.mqm.configuration.properties.YtProperties
import ru.yandex.market.logistics.mqm.entity.YtEvent
import ru.yandex.market.logistics.mqm.repository.ProcessorStateRepository
import ru.yandex.market.logistics.mqm.repository.YtEventRepository
import ru.yandex.market.logistics.mqm.service.logging.LogService
import ru.yandex.market.logistics.mqm.service.yt.YtService
import ru.yandex.market.logistics.mqm.service.ytevents.reader.RECORD_ID_COLUMN
import ru.yandex.market.logistics.mqm.service.ytevents.row.DynamicYtDto
import ru.yandex.market.logistics.mqm.utils.createYtRow
import java.util.function.Function

@DisplayName("Тесты для YtScReader")
class YtScReaderTest: AbstractContextualTest() {

    lateinit var ytScReader: YtScReader

    @Autowired
    lateinit var ytService: YtService

    @Autowired
    fun initReader(
        logService: LogService,
        ytEventRepository: YtEventRepository,
        ytProperties: YtProperties,
        processorStateRepository: ProcessorStateRepository,
        transactionTemplate: TransactionOperations,
    ) {
        val answer = Answer { invocation ->
            val argument = invocation.getArgument<Function<Iterator<YTreeMapNode>, List<DynamicYtDto>>>(2)
            val ytIterator = listOf(createYtRow(mapOf(RECORD_ID_COLUMN to 80000001L))).iterator()
            argument.apply(ytIterator)
        }
        doAnswer(answer)
            .whenever(ytService)
            .selectRowsFromTable<Any>(any(), anyOrNull(), any())

        ytScReader = YtScReader(
            listOf(),
            listOf(),
            ytEventRepository,
            transactionTemplate,
            processorStateRepository,
            ytService,
            ytProperties
        )
    }

    @Test
    @DisplayName("Обновление состояния ридера")
    @DatabaseSetup("/service/ytevents/reader/sc/before/setUpEvents.xml")
    @ExpectedDatabase(
        value = "/service/ytevents/reader/sc/after/successProcessorStateSave.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun updateProcessorState() {
        ytScReader.run()
    }


    @Test
    @DisplayName("Не обновлять статус ридера, если прочитано 0 записей")
    @DatabaseSetup("/service/ytevents/reader/sc/before/setUpEvents.xml")
    @ExpectedDatabase(
        value = "/service/ytevents/reader/sc/after/notChangeState.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun doNotUpdateProcessorStateIfReaderZeroRows() {
        val answer = Answer { invocation ->
            val argument = invocation.getArgument<Function<Iterator<YTreeMapNode>, List<DynamicYtDto>>>(2)
            val ytIterator = listOf<YTreeMapNode>().iterator()
            argument.apply(ytIterator)
        }
        doAnswer(answer)
            .whenever(ytService)
            .selectRowsFromTable<YtEvent<*>>(any(), anyOrNull(), any())
        ytScReader.run()
    }
}

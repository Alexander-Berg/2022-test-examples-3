package ru.yandex.market.logistics.mqm.service.ytevents.reader.courier

import com.github.springtestdbunit.annotation.DatabaseSetup
import com.github.springtestdbunit.annotation.ExpectedDatabase
import com.github.springtestdbunit.assertion.DatabaseAssertionMode
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.anyOrNull
import com.nhaarman.mockitokotlin2.whenever
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.Mockito
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

@DisplayName("Тесты для YtCourierMaxIdReader")
internal class YtCourierMaxIdReaderTest: AbstractContextualTest() {

    lateinit var ytReader: YtCourierMaxIdReader

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
            val generateEventsFunction = invocation.getArgument<Function<Iterator<YTreeMapNode>, List<DynamicYtDto>>>(2)
            val ytIterator = listOf(createYtRow(mapOf(RECORD_ID_COLUMN to 80000001L))).iterator()
            generateEventsFunction.apply(ytIterator)
        }
        Mockito.doAnswer(answer)
            .whenever(ytService)
            .selectRowsFromTable<Any>(any(), anyOrNull(), any())

        ytReader = object: YtCourierMaxIdReader(
            listOf(),
            listOf(),
            ytEventRepository,
            transactionTemplate,
            processorStateRepository,
            ytService,
            ytProperties
        ) {
            override fun getQuery(minId: Long) = "select something"

            override fun getProcessorStateKey() = "TestClassName"

        }
    }

    @Test
    @DisplayName("Ридер корректно обновляет свое стостояние и завершает работу")
    @DatabaseSetup("/service/ytevents/courier/maxidreader/before/setUpEvents.xml")
    @ExpectedDatabase(
        value = "/service/ytevents/courier/maxidreader/after/successProcessorStateSave.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun updateProcessorState() {
        ytReader.run()
    }

    @Test
    @DisplayName("Не обновлять статус ридера, если процитано 0 записей")
    @DatabaseSetup("/service/ytevents/courier/maxidreader/before/setUpEvents.xml")
    @ExpectedDatabase(
        value = "/service/ytevents/courier/maxidreader/before/setUpEvents.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun doNotUpdateProcessorStateIfReaderZeroRows() {
        val answer = Answer { invocation ->
            val argument = invocation.getArgument<Function<Iterator<YTreeMapNode>, List<DynamicYtDto>>>(2)
            val ytIterator = listOf<YTreeMapNode>().iterator()
            argument.apply(ytIterator)
        }
        Mockito.doAnswer(answer)
            .whenever(ytService)
            .selectRowsFromTable<YtEvent<*>>(any(), anyOrNull(), any())
        ytReader.run()
    }
}


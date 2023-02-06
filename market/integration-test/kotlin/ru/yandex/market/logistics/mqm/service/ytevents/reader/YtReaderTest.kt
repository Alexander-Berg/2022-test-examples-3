package ru.yandex.market.logistics.mqm.service.ytevents.reader

import com.github.springtestdbunit.annotation.DatabaseSetup
import com.github.springtestdbunit.annotation.ExpectedDatabase
import com.github.springtestdbunit.assertion.DatabaseAssertionMode
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.argumentCaptor
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import io.kotest.assertions.assertSoftly
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.Mock
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.transaction.support.TransactionOperations
import ru.yandex.market.logistics.mqm.AbstractContextualTest
import ru.yandex.market.logistics.mqm.entity.YtEvent
import ru.yandex.market.logistics.mqm.entity.processorstate.ProcessorState
import ru.yandex.market.logistics.mqm.entity.processorstate.state.MaxProcessedIdPayload
import ru.yandex.market.logistics.mqm.entity.yteventpayload.ReturnScIntakeYtEventPayload
import ru.yandex.market.logistics.mqm.repository.ProcessorStateRepository
import ru.yandex.market.logistics.mqm.repository.YtEventRepository
import ru.yandex.market.logistics.mqm.service.enums.YtEventType.RETURN_SC_INTAKE
import ru.yandex.market.logistics.mqm.service.ytevents.generator.YtEventGenerator
import ru.yandex.market.logistics.mqm.service.ytevents.postprocessor.YtEventPostProcessor
import ru.yandex.market.logistics.mqm.service.ytevents.row.DynamicYtDto
import ru.yandex.market.logistics.mqm.utils.createYtRow


@DisplayName("Тесты для YtReader")
class YtReaderTest: AbstractContextualTest() {

    @Autowired
    lateinit var ytEventRepository: YtEventRepository

    @Autowired
    lateinit var transactionTemplate: TransactionOperations

    @Autowired
    lateinit var processorStateRepository: ProcessorStateRepository

    private lateinit var ytReader: YtReader<DynamicYtDto>

    @Mock
    lateinit var generator: YtEventGenerator<DynamicYtDto>

    @Mock
    lateinit var postProcessor: YtEventPostProcessor

    @BeforeEach
    fun setUp() {
        ytReader = TestingYtReader(
            generator = generator,
            ytEventRepository = ytEventRepository,
            transactionTemplate = transactionTemplate,
            processorStateRepository = processorStateRepository,
            postProcessor = postProcessor,
        )
        whenever(generator.generateEvent(any()))
            .thenReturn(
                YtEvent<ReturnScIntakeYtEventPayload>(
                    type = RETURN_SC_INTAKE,
                    uniqueKey = "key1"
                ),
                YtEvent<ReturnScIntakeYtEventPayload>(
                    type = RETURN_SC_INTAKE,
                    uniqueKey = "key2"
                ),
                YtEvent<ReturnScIntakeYtEventPayload>(
                    type = RETURN_SC_INTAKE,
                    uniqueKey = "key3"
                ),
            )
    }

    @Test
    @DisplayName("Успешное создание и запись событий")
    @DatabaseSetup("/service/ytevents/reader/before/setUpState.xml")
    @ExpectedDatabase(
        value = "/service/ytevents/reader/after/successEventWrite.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun successEventWrite() {
        ytReader.run()
    }


    @Test
    @DisplayName("Не генерировать уже существующие события")
    @DatabaseSetup("/service/ytevents/reader/before/setUpEvents.xml")
    @ExpectedDatabase(
        value = "/service/ytevents/reader/after/successEventWrite.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun doNotCreateExistingEvents() {
        ytReader.run()
    }


    @Test
    @DisplayName("Не генерировать события с одинаковыми ключами в одном батче")
    @DatabaseSetup("/service/ytevents/reader/before/setUpState.xml")
    @ExpectedDatabase(
        value = "/service/ytevents/reader/after/oneEvent.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun doNotCreateNotUnicueEventsInOneBatch() {
        whenever(generator.generateEvent(any()))
            .thenReturn(
                YtEvent<ReturnScIntakeYtEventPayload>(
                    type = RETURN_SC_INTAKE,
                    uniqueKey = "key1"
                ),
                YtEvent<ReturnScIntakeYtEventPayload>(
                    type = RETURN_SC_INTAKE,
                    uniqueKey = "key1"
                ),
                YtEvent<ReturnScIntakeYtEventPayload>(
                    type = RETURN_SC_INTAKE,
                    uniqueKey = "key1"
                ),
            )
        ytReader.run()
    }

    @Test
    @DisplayName("Вызывать postProcess для всех новых событий")
    @DatabaseSetup("/service/ytevents/reader/before/setUpEvents.xml")
    fun callPostProcess() {
        whenever(generator.generateEvent(any()))
            .thenReturn(
                YtEvent<ReturnScIntakeYtEventPayload>(
                    type = RETURN_SC_INTAKE,
                    uniqueKey = "key1"
                ),
                YtEvent<ReturnScIntakeYtEventPayload>(
                    type = RETURN_SC_INTAKE,
                    uniqueKey = "key2"
                ),
                YtEvent<ReturnScIntakeYtEventPayload>(
                    type = RETURN_SC_INTAKE,
                    uniqueKey = "key3"
                ),
            )
        val eventCaptor = argumentCaptor<YtEvent<*>>()
        ytReader.run()
        assertSoftly {
            verify(postProcessor).postProcessYtEvent(eventCaptor.capture())
            eventCaptor.firstValue.uniqueKey shouldBe "key3"
        }
    }

    private class TestingYtReader(
        generator: YtEventGenerator<DynamicYtDto>,
        ytEventRepository: YtEventRepository,
        transactionTemplate: TransactionOperations,
        processorStateRepository: ProcessorStateRepository,
        postProcessor: YtEventPostProcessor,
    ): YtReader<DynamicYtDto>(
        eventGenerators = listOf(generator),
        ytEventRepository = ytEventRepository,
        transactionTemplate = transactionTemplate,
        processorStateRepository = processorStateRepository,
        eventPostProcessors = listOf(postProcessor),
    ) {

        override fun getProcessorStateKey() = "YtReaderTest"

        override fun readAndProcessYtBatch(processorState: ProcessorState): BatchProcessResult {
            val emptyYtRows = listOf(
                DynamicYtDto(createYtRow(mapOf()), TestingYtReader::class),
                DynamicYtDto(createYtRow(mapOf()), TestingYtReader::class),
                DynamicYtDto(createYtRow(mapOf()), TestingYtReader::class),
            )
            (processorState.payload as MaxProcessedIdPayload).maxProcessedId = 1
            generateEvents(emptyYtRows)
            return BatchProcessResult(
                processedRowsCount = 3,
                newProcessorState = processorState,
                hasNextBatch = false
            )
        }
    }

}

package ru.yandex.market.logistics.mqm.monitoringevent.consumer

import com.fasterxml.jackson.databind.ObjectMapper
import com.nhaarman.mockitokotlin2.argumentCaptor
import com.nhaarman.mockitokotlin2.verify
import io.kotest.assertions.assertSoftly
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mock
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.logistics.mqm.AbstractContextualTest
import ru.yandex.market.logistics.mqm.model.enums.ProcessingOrderErrorType
import ru.yandex.market.logistics.mqm.monitoringevent.base.consumer.MdbProcessingOrderErrorConsumer
import ru.yandex.market.logistics.mqm.monitoringevent.base.producer.CreateStartrekIssueForClaimEventProducer
import ru.yandex.market.logistics.mqm.monitoringevent.payload.CreateStartrekIssueForClaimPayload
import ru.yandex.market.logistics.mqm.monitoringevent.payload.MdbProcessingOrderErrorPayload
import ru.yandex.market.logistics.mqm.monitoringevent.processorconfig.MdbProcessingOrderErrorProcessorConfig
import ru.yandex.market.logistics.mqm.queue.base.QueueRegister
import ru.yandex.market.logistics.mqm.repository.MonitoringEventProcessorRepository
import ru.yandex.money.common.dbqueue.api.EnqueueParams

class MdbProcessingOrderErrorConsumerTest : AbstractContextualTest() {

    @Mock
    private lateinit var producer: CreateStartrekIssueForClaimEventProducer

    @Autowired
    private lateinit var queueRegister: QueueRegister

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Autowired
    private lateinit var processorRepository: MonitoringEventProcessorRepository

    private lateinit var consumer: MdbProcessingOrderErrorConsumer

    @BeforeEach
    fun setUp() {
        consumer = MdbProcessingOrderErrorConsumer(queueRegister, objectMapper, processorRepository, producer)
    }

    @Test
    fun createOrderErrorIssueTest() {
        consumer.processPayload(
            MdbProcessingOrderErrorPayload(
                ProcessingOrderErrorType.CREATE_ORDER,
                "NPE",
                "NPE at line 12",
                "DropshipCreate",
                "34134",
                "14325"
            ),
            PROCESSOR_CONFIG
        )

        val argumentCaptor = argumentCaptor<EnqueueParams<CreateStartrekIssueForClaimPayload>>()
        verify(producer).enqueue(argumentCaptor.capture())

        val argument = argumentCaptor.firstValue
        checkCommonFields(argument)

        assertSoftly {
            argument.payload!!.queue shouldBe "CREATEQUEUE"
        }
    }

    @Test
    fun createUpdateErrorIssueTest() {
        consumer.processPayload(
            MdbProcessingOrderErrorPayload(
                ProcessingOrderErrorType.UPDATE_ORDER,
                "NPE",
                "NPE at line 12",
                "DropshipCreate",
                "34134",
                "14325"
            ),
            PROCESSOR_CONFIG
        )

        val argumentCaptor = argumentCaptor<EnqueueParams<CreateStartrekIssueForClaimPayload>>()
        verify(producer).enqueue(argumentCaptor.capture())

        val argument = argumentCaptor.firstValue

        checkCommonFields(argument)

        assertSoftly {
            argument.payload!!.queue shouldBe "UPDATEQUEUE"
        }
    }

    private fun checkCommonFields(argument: EnqueueParams<CreateStartrekIssueForClaimPayload>) {
        val summary = argument.payload!!.summary
        val description = argument.payload!!.description
        val fields = argument.payload!!.fields

        assertSoftly {
            summary shouldBe "[MDB] Ошибка в MDB при обработке события DropshipCreate 34134 заказа 14325"
            description shouldBe "Выброшенное исключение %%(bash)NPE%%\n<{trace\n%%(bash)NPE at line 12%%\n}>"
            fields shouldBe mapOf(
                "tags" to "DropshipCreate",
                "components" to 101548L
            )
        }
    }

    companion object {
        private val PROCESSOR_CONFIG = MdbProcessingOrderErrorProcessorConfig("CREATEQUEUE", "UPDATEQUEUE", 101548L);
    }
}

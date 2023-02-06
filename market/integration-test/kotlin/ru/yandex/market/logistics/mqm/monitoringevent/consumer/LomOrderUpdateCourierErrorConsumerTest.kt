package ru.yandex.market.logistics.mqm.monitoringevent.consumer

import com.fasterxml.jackson.databind.ObjectMapper
import com.nhaarman.mockitokotlin2.argumentCaptor
import com.nhaarman.mockitokotlin2.verify
import freemarker.template.Configuration
import io.kotest.assertions.assertSoftly
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mock
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.bolts.collection.impl.ArrayListF
import ru.yandex.market.common.util.DateTimeUtils
import ru.yandex.market.logistics.mqm.AbstractContextualTest
import ru.yandex.market.logistics.mqm.monitoringevent.base.consumer.LomOrderUpdateCourierErrorConsumer
import ru.yandex.market.logistics.mqm.monitoringevent.base.producer.CreateStartrekIssueEventProducer
import ru.yandex.market.logistics.mqm.monitoringevent.payload.CreateStartrekIssuePayload
import ru.yandex.market.logistics.mqm.monitoringevent.payload.LomOrderUpdateCourierErrorPayload
import ru.yandex.market.logistics.mqm.monitoringevent.processorconfig.LomOrderUpdateCourierErrorProcessorConfig
import ru.yandex.market.logistics.mqm.queue.base.QueueRegister
import ru.yandex.market.logistics.mqm.repository.MonitoringEventProcessorRepository
import ru.yandex.money.common.dbqueue.api.EnqueueParams
import java.time.Instant

class LomOrderUpdateCourierErrorConsumerTest : AbstractContextualTest() {

    @Autowired
    private lateinit var queueRegister: QueueRegister

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Autowired
    private lateinit var processorRepository: MonitoringEventProcessorRepository

    @Autowired
    private lateinit var freemarkerConfiguration: Configuration

    @Mock
    private lateinit var producer: CreateStartrekIssueEventProducer

    private lateinit var consumer: LomOrderUpdateCourierErrorConsumer

    @BeforeEach
    fun setUp() {
        clock.setFixed(Instant.parse("2021-09-09T10:00:00.00Z"), DateTimeUtils.MOSCOW_ZONE)

        consumer = LomOrderUpdateCourierErrorConsumer(
            queueRegister,
            objectMapper,
            processorRepository,
            freemarkerConfiguration,
            producer
        )
    }

    @Test
    fun createIssueTest() {
        consumer.processPayload(PAYLOAD, PROCESSOR_CONFIG)

        val argumentCaptor = argumentCaptor<EnqueueParams<CreateStartrekIssuePayload>>()

        verify(producer).enqueue(argumentCaptor.capture())

        val arguments = argumentCaptor.firstValue

        assertSoftly {
            arguments shouldBe EnqueueParams.create(
                CreateStartrekIssuePayload(
                    QUEUE,
                    SUMMARY,
                    DESCRIPTION,
                    tags(TAGS),
                )
            )
        }
    }

    companion object {
        private const val QUEUE = "MQMU"
        private const val BARCODE = "barcode-1"
        private const val LOM_ORDER_ID = 101L
        private const val BUSINESS_PROCESS_ID = 12345L
        private const val PARTNER_ID = 222L
        private const val PARTNER_NAME = "Партнер"
        private const val SUMMARY = "Для заказа $BARCODE произошла ошибка при обновлении данных курьера"

        private val DESCRIPTION = """
            https://abo.market.yandex-team.ru/order/$BARCODE
            https://ow.market.yandex-team.ru/order/$BARCODE
            https://lms-admin.market.yandex-team.ru/lom/orders/$LOM_ORDER_ID
            https://lms-admin.market.yandex-team.ru/lom/business-processes/$BUSINESS_PROCESS_ID
            
            Идентификатор заказа: $BARCODE
        """.trimIndent()
        private val PAYLOAD = LomOrderUpdateCourierErrorPayload(
            orderId = BARCODE,
            lomOrderId = LOM_ORDER_ID,
            businessProcessId = BUSINESS_PROCESS_ID,
            partnerId = PARTNER_ID,
            partnerName = PARTNER_NAME
        )
        private val PROCESSOR_CONFIG = LomOrderUpdateCourierErrorProcessorConfig(QUEUE)

        private fun tags(tags: List<String>): Map<String, Any> {
            val items = mutableMapOf<String, Any>()
            items["tags"] = tags
            return items
        }

        private val TAGS = listOf(
            "processId:${BUSINESS_PROCESS_ID}",
            "$PARTNER_NAME:$PARTNER_ID"
        )
    }
}

package ru.yandex.market.logistics.mqm.monitoringevent.consumer

import com.nhaarman.mockitokotlin2.argumentCaptor
import com.nhaarman.mockitokotlin2.verify
import io.kotest.assertions.assertSoftly
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mock
import ru.yandex.market.logistics.mqm.monitoringevent.base.consumer.LomOrderUpdateRecipientErrorConsumer
import ru.yandex.market.logistics.mqm.monitoringevent.base.producer.CreateStartrekIssueEventProducer
import ru.yandex.market.logistics.mqm.monitoringevent.payload.CreateStartrekIssuePayload
import ru.yandex.market.logistics.mqm.monitoringevent.payload.LomOrderUpdateRecipientErrorPayload
import ru.yandex.market.logistics.mqm.monitoringevent.processorconfig.LomOrderUpdateRecipientErrorProcessorConfig
import ru.yandex.money.common.dbqueue.api.EnqueueParams

class LomOrderUpdateRecipientErrorConsumerTest : LomConsumerTest() {

    private lateinit var consumer: LomOrderUpdateRecipientErrorConsumer

    @Mock
    private lateinit var producer: CreateStartrekIssueEventProducer

    @BeforeEach
    fun setUp() {
        consumer = LomOrderUpdateRecipientErrorConsumer(
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
                    description(),
                    tags(TAGS),
                )
            )
        }
    }

    private fun description() = DESCRIPTION

    companion object {
        private val PAYLOAD = LomOrderUpdateRecipientErrorPayload(
            orderId = BARCODE,
            lomOrderId = LOM_ORDER_ID,
            businessProcessesId = BUSINESS_PROCESS_ID
        )

        private val PROCESSOR_CONFIG = LomOrderUpdateRecipientErrorProcessorConfig(QUEUE)

        private const val SUMMARY = "Для заказа $BARCODE произошла ошибка при обновлении данных получателя"
        private const val DESCRIPTION =
            "https://abo.market.yandex-team.ru/order/$BARCODE\n" +
                    "https://ow.market.yandex-team.ru/order/$BARCODE\n" +
                    "https://lms-admin.market.yandex-team.ru/lom/orders/$LOM_ORDER_ID\n" +
                    "https://lms-admin.market.yandex-team.ru/lom/business-processes/$BUSINESS_PROCESS_ID\n" +
                    "\n" +
                    "Идентификатор заказа: $BARCODE"

        private val TAGS = listOf("processId:${BUSINESS_PROCESS_ID}")
    }
}

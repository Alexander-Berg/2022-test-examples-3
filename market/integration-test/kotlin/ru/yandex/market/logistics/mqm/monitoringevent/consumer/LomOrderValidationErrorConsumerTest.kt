package ru.yandex.market.logistics.mqm.monitoringevent.consumer

import com.nhaarman.mockitokotlin2.argumentCaptor
import com.nhaarman.mockitokotlin2.verify
import io.kotest.assertions.assertSoftly
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.Mock
import ru.yandex.market.common.util.DateTimeUtils
import ru.yandex.market.logistics.mqm.monitoringevent.base.consumer.LomOrderValidationErrorConsumer
import ru.yandex.market.logistics.mqm.monitoringevent.base.producer.CreateStartrekIssueEventProducer
import ru.yandex.market.logistics.mqm.monitoringevent.payload.CreateStartrekIssuePayload
import ru.yandex.market.logistics.mqm.monitoringevent.payload.LomOrderValidationErrorPayload
import ru.yandex.market.logistics.mqm.monitoringevent.processorconfig.LomOrderValidationErrorProcessorConfig
import ru.yandex.money.common.dbqueue.api.EnqueueParams
import java.time.Instant

@DisplayName("Обработка ошибки валидации заказа в LOM")
class LomOrderValidationErrorConsumerTest : LomConsumerTest() {

    private lateinit var consumer: LomOrderValidationErrorConsumer

    @Mock
    private lateinit var producer: CreateStartrekIssueEventProducer

    @BeforeEach
    fun setUp() {
        clock.setFixed(Instant.parse("2021-09-09T10:00:00.00Z"), DateTimeUtils.MOSCOW_ZONE)

        consumer = LomOrderValidationErrorConsumer(
            queueRegister,
            objectMapper,
            processorRepository,
            freemarkerConfiguration,
            producer
        )
    }

    @Test
    @DisplayName("Тикет создается")
    fun createIssue() {
        consumer.processPayload(PAYLOAD, PROCESSOR_CONFIG)

        val argumentCaptor = argumentCaptor<EnqueueParams<CreateStartrekIssuePayload>>()

        verify(producer).enqueue(argumentCaptor.capture())

        val arguments = argumentCaptor.firstValue

        assertSoftly {
            arguments shouldBe EnqueueParams.create(
                CreateStartrekIssuePayload(
                    QUEUE,
                    SUMMARY,
                    description(DESCRIPTION),
                    tags(TAGS),
                )
            )
        }
    }

    companion object {
        private val PAYLOAD = LomOrderValidationErrorPayload(
            orderId = BARCODE,
            lomOrderId = LOM_ORDER_ID,
            orderCreationDate = INSTANT,
            businessProcessesId = BUSINESS_PROCESS_ID
        )

        private val PROCESSOR_CONFIG = LomOrderValidationErrorProcessorConfig(QUEUE)

        private const val SUMMARY = "Заказ $BARCODE перешел в статус VALIDATION_ERROR"
        private const val DESCRIPTION =
            "https://abo.market.yandex-team.ru/order/$BARCODE\n" +
                    "https://ow.market.yandex-team.ru/order/$BARCODE\n" +
                    "https://lms-admin.market.yandex-team.ru/lom/orders/$LOM_ORDER_ID\n" +
                    "https://lms-admin.market.yandex-team.ru/lom/business-processes/$BUSINESS_PROCESS_ID\n" +
                    "\n" +
                    "Идентификатор заказа: $BARCODE\n" +
                    "Дата создания заказа: %s"

        private val TAGS = listOf("processId:$BUSINESS_PROCESS_ID")
    }
}

package ru.yandex.market.logistics.mqm.monitoringevent.consumer

import com.nhaarman.mockitokotlin2.argumentCaptor
import com.nhaarman.mockitokotlin2.verify
import io.kotest.assertions.assertSoftly
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.Mock
import ru.yandex.market.logistics.mqm.monitoringevent.base.consumer.LomCreateOrderErrorConsumer
import ru.yandex.market.logistics.mqm.monitoringevent.base.producer.CreateStartrekIssueEventProducer
import ru.yandex.market.logistics.mqm.monitoringevent.payload.CreateStartrekIssuePayload
import ru.yandex.market.logistics.mqm.monitoringevent.payload.LomCreateOrderErrorPayload
import ru.yandex.market.logistics.mqm.monitoringevent.processorconfig.LomCreateOrderErrorProcessorConfig
import ru.yandex.money.common.dbqueue.api.EnqueueParams

@DisplayName("Обработка ошибки заказа в LOM")
class LomCreateOrderErrorConsumerTest : LomConsumerTest() {

    private lateinit var consumer: LomCreateOrderErrorConsumer

    @Mock
    private lateinit var producer: CreateStartrekIssueEventProducer

    @BeforeEach
    fun setUp() {
        consumer = LomCreateOrderErrorConsumer(
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

        private val PAYLOAD = LomCreateOrderErrorPayload(
            orderId = BARCODE,
            partnerId = PARTNER_ID,
            partnerName = PARTNER_NAME,
            errorCode = ERROR_CODE,
            errorMessage = ERROR_MESSAGE,
            partnerType = PARTNER_TYPE,
            segmentId = SEGMENT_ID,
            express = EXPRESS,
            lomOrderId = LOM_ORDER_ID,
            orderCreationDate = INSTANT,
            cause = CAUSE,
            businessProcessesId = BUSINESS_PROCESS_ID
        )

        private val PROCESSOR_CONFIG = LomCreateOrderErrorProcessorConfig(QUEUE)

        private const val SUMMARY = "Ошибка при обновлении заказа $BARCODE"
        private const val DESCRIPTION =
            "https://abo.market.yandex-team.ru/order/$BARCODE\n" +
                    "https://ow.market.yandex-team.ru/order/$BARCODE\n" +
                    "https://lms-admin.market.yandex-team.ru/lom/orders/$LOM_ORDER_ID\n" +
                    "https://lms-admin.market.yandex-team.ru/lom/business-processes/$BUSINESS_PROCESS_ID\n" +
                    "\n" +
                    "Номер заказа: $BARCODE\n" +
                    "Дата создания заказа: %s\n" +
                    "Партнер: $PARTNER_TYPE/$PARTNER_NAME/$PARTNER_ID.\n" +
                    "\n" +
                    "Код: $ERROR_CODE\n" +
                    "Сообщение: $ERROR_MESSAGE"

        private val TAGS = listOf(
            PARTNER_TYPE,
            "$PARTNER_NAME:$PARTNER_ID",
            CAUSE,
            EXPRESS_TAG,
            "processId:3023"
        )
    }
}

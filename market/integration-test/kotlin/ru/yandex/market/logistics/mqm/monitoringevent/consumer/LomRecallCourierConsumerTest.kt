package ru.yandex.market.logistics.mqm.monitoringevent.consumer

import com.nhaarman.mockitokotlin2.argumentCaptor
import com.nhaarman.mockitokotlin2.verify
import io.kotest.assertions.assertSoftly
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.Mock
import ru.yandex.market.logistics.mqm.entity.enums.IssueLinkEntityType
import ru.yandex.market.logistics.mqm.entity.enums.IssueLinkReason
import ru.yandex.market.logistics.mqm.model.enums.RecallCourierReason
import ru.yandex.market.logistics.mqm.monitoringevent.base.consumer.LomRecallCourierConsumer
import ru.yandex.market.logistics.mqm.monitoringevent.base.producer.CreateStartrekIssueEventProducer
import ru.yandex.market.logistics.mqm.monitoringevent.payload.BaseCreateStartrekIssuePayload
import ru.yandex.market.logistics.mqm.monitoringevent.payload.CreateStartrekIssuePayload
import ru.yandex.market.logistics.mqm.monitoringevent.payload.LomRecallCourierPayload
import ru.yandex.market.logistics.mqm.monitoringevent.processorconfig.LomRecallCourierConfig
import ru.yandex.money.common.dbqueue.api.EnqueueParams

@DisplayName("Обработка необходимости перевызова курьера")
class LomRecallCourierConsumerTest : LomConsumerTest() {
    private lateinit var consumer: LomRecallCourierConsumer

    @Mock
    private lateinit var producer: CreateStartrekIssueEventProducer

    @BeforeEach
    fun setup() {
        consumer = LomRecallCourierConsumer(
            queueRegister,
            objectMapper,
            processorRepository,
            freemarkerConfiguration,
            producer
        )
    }

    @Test
    @DisplayName("Неизвестная причина")
    fun unknown() {
        consume(PAYLOAD, DESCRIPTION, TAGS)
    }

    @Test
    @DisplayName("Протухшая заявка на вызов курьера")
    fun rottenCallCourier() {
        consume(PAYLOAD_ROTTEN, DESCRIPTION_ROTTEN, TAGS_ROTTEN)
    }

    private fun consume(payload: LomRecallCourierPayload, description: String, tags: List<String>) {
        consumer.processPayload(payload, PROCESSOR_CONFIG)

        val argumentCaptor = argumentCaptor<EnqueueParams<CreateStartrekIssuePayload>>()

        verify(producer).enqueue(argumentCaptor.capture())

        val arguments = argumentCaptor.firstValue

        assertSoftly {
            arguments shouldBe EnqueueParams.create(
                CreateStartrekIssuePayload(
                    QUEUE,
                    SUMMARY,
                    description(description),
                    tags(tags).plus(Pair("components", listOf(LOM_COMPONENT))),
                    entities = setOf(
                        BaseCreateStartrekIssuePayload.Entity(
                            payload.orderId,
                            IssueLinkEntityType.ORDER,
                            IssueLinkReason.RECALL_COURIER
                        )
                    )
                )
            )
        }
    }

    companion object {
        private val PAYLOAD = LomRecallCourierPayload(
            orderId = BARCODE,
            lomOrderId = LOM_ORDER_ID,
            orderCreationDate = INSTANT,
            reason = RecallCourierReason.UNKNOWN
        )

        private val PAYLOAD_ROTTEN = LomRecallCourierPayload(
            orderId = BARCODE,
            lomOrderId = LOM_ORDER_ID,
            orderCreationDate = INSTANT,
            reason = RecallCourierReason.ROTTEN_CALL_COURIER
        )

        private val PROCESSOR_CONFIG = LomRecallCourierConfig(QUEUE, LOM_COMPONENT)

        private const val SUMMARY = "Ошибка при вызове курьера для заказа $BARCODE"
        private const val DESCRIPTION =
            "https://abo.market.yandex-team.ru/order/barcode-1\n" +
                    "https://ow.market.yandex-team.ru/order/barcode-1\n" +
                    "https://lms-admin.market.yandex-team.ru/lom/orders/101\n" +
                    "\n" +
                    "Номер заказа: barcode-1\n" +
                    "Дата создания заказа: 09-09-2021\n" +
                    "\n" +
                    "Инструкция: https://nda.ya.ru/t/nbD03jvV57kMb8\n" +
                    "Причина: Неизвестно"
        private const val DESCRIPTION_ROTTEN =
            "https://abo.market.yandex-team.ru/order/barcode-1\n" +
                    "https://ow.market.yandex-team.ru/order/barcode-1\n" +
                    "https://lms-admin.market.yandex-team.ru/lom/orders/101\n" +
                    "\n" +
                    "Номер заказа: barcode-1\n" +
                    "Дата создания заказа: 09-09-2021\n" +
                    "\n" +
                    "Инструкция: https://nda.ya.ru/t/nbD03jvV57kMb8\n" +
                    "Причина: Протухла заявка на вызов курьера"
        private val TAGS = listOf(EXPRESS_TAG, "ds-call-courier")
        private val TAGS_ROTTEN = listOf(EXPRESS_TAG, "ds-call-courier", "47")
    }
}

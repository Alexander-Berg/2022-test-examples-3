package ru.yandex.market.logistics.mqm.queue.consumer

import com.fasterxml.jackson.databind.ObjectMapper
import com.nhaarman.mockitokotlin2.argumentCaptor
import com.nhaarman.mockitokotlin2.verify
import io.kotest.assertions.assertSoftly
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.springframework.transaction.support.TransactionOperations
import ru.yandex.market.logistics.les.base.Event
import ru.yandex.market.logistics.les.dto.CourierDto
import ru.yandex.market.logistics.les.dto.PersonDto
import ru.yandex.market.logistics.les.tpl.CourierReceivedPickupReturnEvent
import ru.yandex.market.logistics.les.tpl.TplCourierDeliveredReturnToScEvent
import ru.yandex.market.logistics.mqm.entity.courier.CourierDeliveredToScEventPayload
import ru.yandex.market.logistics.mqm.entity.courier.CourierEventHistory
import ru.yandex.market.logistics.mqm.entity.courier.CourierReceivedPickupEventPayload
import ru.yandex.market.logistics.mqm.entity.enums.courier.CourierStatus
import ru.yandex.market.logistics.mqm.queue.base.QueueRegister
import ru.yandex.market.logistics.mqm.queue.dto.ProcessLesEventDto
import ru.yandex.market.logistics.mqm.queue.producer.CourierStatusChangedProducer
import ru.yandex.market.logistics.mqm.service.FailedQueueTaskService
import ru.yandex.market.logistics.mqm.service.les.Producer


@ExtendWith(MockitoExtension::class)
class ProcessLesEventConsumerTest {

    private lateinit var consumer: ProcessLesEventConsumer

    @Mock
    private lateinit var failedQueueTaskService: FailedQueueTaskService

    @Mock
    private lateinit var objectMapper: ObjectMapper

    @Mock
    private lateinit var transactionTemplate: TransactionOperations

    @Mock
    private lateinit var courierStatusChangedProducer: CourierStatusChangedProducer

    @BeforeEach
    fun setup() {
        consumer = ProcessLesEventConsumer(
            QueueRegister(mapOf()),
            objectMapper,
            failedQueueTaskService,
            transactionTemplate,
            courierStatusChangedProducer
        )
    }

    @Test
    fun testCourierReceived() {
        consumer.processPayload(
            ProcessLesEventDto(
                Event(
                    "aboba",
                    "1",
                    null,
                    Producer.EVENT_TYPE,
                    CourierReceivedPickupReturnEvent(
                        boxExternalId = "box-ext-id",
                        sortingCenterId = 1,
                        courier = CourierDto(
                            id = 2,
                            partnerId = 3,
                            person = PersonDto(
                                name = "Nik",
                                surname = "Ricnorr"
                            )
                        )
                    ),
                    "Тестовое событие"
                )
            )
        )
        val captor = argumentCaptor<CourierEventHistory>()
        verify(courierStatusChangedProducer).produceTask(captor.capture())
        val history = captor.firstValue
        assertSoftly {
            history.externalBoxId shouldBe "box-ext-id"
            history.status shouldBe CourierStatus.RECEIVED_PICKUP
            history.payload shouldBe CourierReceivedPickupEventPayload(
                sortingCenterId = 1,
                courier = CourierReceivedPickupEventPayload.CourierDto(
                    id = 2,
                    partnerId = 3,
                    person = CourierReceivedPickupEventPayload.PersonDto(
                        name = "Nik",
                        surname = "Ricnorr"
                    ),
                    phone = CourierReceivedPickupEventPayload.PhoneDto(),
                    car = CourierReceivedPickupEventPayload.CarDto(),
                    legalEntity = CourierReceivedPickupEventPayload.LegalEntityDto()
                )
            )
        }
    }

    @Test
    fun testCourierDeliveredToSc() {
        consumer.processPayload(
            ProcessLesEventDto(
                Event(
                    "aboba",
                    "1",
                    null,
                    Producer.EVENT_TYPE,
                    TplCourierDeliveredReturnToScEvent(
                        returnId = "return-id",
                        boxExternalId = "box-ext-id",
                        scLogisticPointId = "sc-point-id"
                    ),
                    "Тестовое событие"
                )
            )
        )
        val captor = argumentCaptor<CourierEventHistory>()
        verify(courierStatusChangedProducer).produceTask(captor.capture())
        val history = captor.firstValue
        assertSoftly {
            history.externalBoxId shouldBe "box-ext-id"
            history.status shouldBe CourierStatus.DELIVERED_TO_SC
            history.payload shouldBe CourierDeliveredToScEventPayload(
                returnId = "return-id",
                scLogisticPointId = "sc-point-id"
            )
        }
    }
}

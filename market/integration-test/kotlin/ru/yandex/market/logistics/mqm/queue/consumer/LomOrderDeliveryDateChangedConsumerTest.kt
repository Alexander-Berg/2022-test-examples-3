package ru.yandex.market.logistics.mqm.queue.consumer

import com.github.springtestdbunit.annotation.DatabaseSetup
import com.github.springtestdbunit.annotation.ExpectedDatabase
import com.github.springtestdbunit.assertion.DatabaseAssertionMode
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.transaction.support.TransactionOperations
import ru.yandex.market.common.util.DateTimeUtils
import ru.yandex.market.logistics.mqm.AbstractContextualTest
import ru.yandex.market.logistics.mqm.entity.PlanFact
import ru.yandex.market.logistics.mqm.queue.dto.LomOrderDeliveryDateChangedDto
import ru.yandex.market.logistics.test.integration.logging.BackLogCaptor
import java.time.Instant
import javax.persistence.EntityManager

@DisplayName("Тест обработки изменения даты доставки заказа")
class LomOrderDeliveryDateChangedConsumerTest: AbstractContextualTest() {

    @RegisterExtension
    @JvmField
    final val backLogCaptor = BackLogCaptor()

    @Autowired
    private lateinit var entityManager: EntityManager

    @Autowired
    lateinit var consumer: LomOrderDeliveryDateChangedConsumer

    @Autowired
    private lateinit var transactionOperations: TransactionOperations

    @Test
    @DatabaseSetup("/queue/consumer/before/process_order_delivery_date_change/setup.xml")
    @ExpectedDatabase(
        value = "/queue/consumer/after/process_order_delivery_date_change/processed.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @DisplayName("Успешный сценарий, план-факт RecipientTransmitPlanFactProcessor пересчитан")
    fun testDeliveryDateChange() {
        resetSequenceIdGeneratorCache(PlanFact::class.java, entityManager)
        clock.setFixed(Instant.parse("2021-07-01T09:00:00.00Z"), DateTimeUtils.MOSCOW_ZONE)
        transactionOperations.executeWithoutResult {
            consumer.processPayload(LomOrderDeliveryDateChangedDto(1L))
        }
    }

    @Test
    @DatabaseSetup("/queue/consumer/before/process_order_delivery_date_change/setup_last_mile_go.xml")
    @ExpectedDatabase(
        value = "/queue/consumer/after/process_order_delivery_date_change/processed_go.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @DisplayName("Успешный сценарий пересчета план-факта для сегмента GO_PLATFORM")
    fun testDeliveryDateChangeForGo() {
        resetSequenceIdGeneratorCache(PlanFact::class.java, entityManager)
        clock.setFixed(Instant.parse("2021-07-01T09:00:00.00Z"), DateTimeUtils.MOSCOW_ZONE)
        transactionOperations.executeWithoutResult {
            consumer.processPayload(LomOrderDeliveryDateChangedDto(1L))
        }
    }

    @Test
    @DisplayName("Успешный сценарий вызова обработки изменения даты доставки только при передачи идентификатора")
    @DatabaseSetup("/queue/consumer/before/process_order_delivery_date_change/setup_dd.xml")
    fun successProcessLomOrderDeliveryDateChanged() {
        resetSequenceIdGeneratorCache(PlanFact::class.java, entityManager)
        clock.setFixed(Instant.parse("2021-07-01T09:00:00.00Z"), DateTimeUtils.MOSCOW_ZONE)

        val transformer = consumer.getPayloadTransformer()

        val payload = transformer.toObject("""{"lomOrderId":1}""")!!

        transactionOperations.executeWithoutResult {
            consumer.processPayload(payload)
        }
    }
}

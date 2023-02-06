package ru.yandex.market.logistics.mqm.queue.consumer

import com.github.springtestdbunit.annotation.DatabaseSetup
import com.github.springtestdbunit.annotation.ExpectedDatabase
import com.github.springtestdbunit.assertion.DatabaseAssertionMode
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.transaction.support.TransactionOperations
import ru.yandex.market.common.util.DateTimeUtils
import ru.yandex.market.logistics.mqm.AbstractContextualTest
import ru.yandex.market.logistics.mqm.queue.dto.LomOrderIdDto
import java.time.Instant

@DisplayName("Тест обработки создания новой заявки на отмену заказа")
class LomCancellationRequestCreatedConsumerTest : AbstractContextualTest() {
    @Autowired
    lateinit var consumer: LomCancellationRequestCreatedConsumer

    @Autowired
    private lateinit var transactionOperations: TransactionOperations

    @Test
    @DatabaseSetup("/queue/consumer/before/process_cancellation_request_created/setup.xml")
    @ExpectedDatabase(
        value = "/queue/consumer/after/process_cancellation_request_created/marked_not_actual.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @DisplayName("Обработка создания новой заявки на отмену")
    fun testConsumerC() {
        clock.setFixed(Instant.parse("2021-07-01T09:00:00.00Z"), DateTimeUtils.MOSCOW_ZONE)
        transactionOperations.executeWithoutResult {
            consumer.processPayload(LomOrderIdDto(1L))
        }
    }
}

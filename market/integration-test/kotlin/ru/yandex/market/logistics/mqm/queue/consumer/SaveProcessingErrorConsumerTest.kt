package ru.yandex.market.logistics.mqm.queue.consumer

import com.github.springtestdbunit.annotation.DatabaseSetup
import com.github.springtestdbunit.annotation.ExpectedDatabase
import com.github.springtestdbunit.assertion.DatabaseAssertionMode
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.transaction.support.TransactionTemplate
import ru.yandex.market.logistics.mqm.AbstractContextualTest
import ru.yandex.market.logistics.mqm.exception.planfact.WaybillPlanFactsNotFoundException
import ru.yandex.market.logistics.mqm.model.enums.ProcessType
import ru.yandex.market.logistics.mqm.queue.dto.ProcessingErrorDto

@DisplayName("Тест обработки получения нового статуса заказа")
class SaveProcessingErrorConsumerTest: AbstractContextualTest() {
    @Autowired
    lateinit var consumer: SaveProcessingErrorConsumer

    @Autowired
    lateinit var transactionTemplate: TransactionTemplate

    @Test
    @DatabaseSetup(
        value = [
            "/queue/consumer/before/process_save_processing_error/order.xml",
            "/queue/consumer/before/process_save_processing_error/plan_fact.xml"
        ]
    )
    @ExpectedDatabase(
        value = "/queue/consumer/after/process_save_processing_error/saved.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @DisplayName("Сохранение ошибки")
    fun testConsumer() {
        transactionTemplate.executeWithoutResult {
            consumer.processPayload(createPayload())
        }
    }

    @Test
    @DatabaseSetup("/queue/consumer/before/process_save_processing_error/order.xml")
    @DisplayName("Не найдено ПФ для сегмента")
    fun testConsumerNotFound() {
        assertThrows<WaybillPlanFactsNotFoundException> {
            transactionTemplate.executeWithoutResult {
                consumer.processPayload(createPayload())
            }
        }
    }

    @Test
    @DatabaseSetup(
        value = [
            "/queue/consumer/before/process_save_processing_error/order.xml",
            "/queue/consumer/before/process_save_processing_error/plan_fact.xml",
            "/queue/consumer/before/process_save_processing_error/cancellation_request.xml"
        ]
    )
    @ExpectedDatabase(
        value = "/queue/consumer/before/process_save_processing_error/plan_fact.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @DisplayName("У заказа есть cancellation request")
    fun orderHasCancellationRequest() {
        transactionTemplate.executeWithoutResult {
            consumer.processPayload(createPayload())
        }
    }

    private fun createPayload() = ProcessingErrorDto(
        ProcessType.LOM_ORDER_CREATE,
        1L,
        123456L,
        1000,
        "Code: null, message: code 9999: Неверно указан город получателя",
    )
}

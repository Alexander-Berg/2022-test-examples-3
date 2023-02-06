package ru.yandex.market.logistics.mqm.queue.producer

import com.github.springtestdbunit.annotation.ExpectedDatabase
import com.github.springtestdbunit.assertion.DatabaseAssertionMode
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.logistics.mqm.AbstractContextualTest
import java.time.Instant

internal class HandlePlanFactProducerTest: AbstractContextualTest() {

    @Autowired
    private lateinit var producer: HandlePlanFactProducer

    @Test
    @DisplayName("Проверка сохранения колонки request_id в БД")
    @ExpectedDatabase(
        value = "/queue/producer/after/handle_plan_fact/save_request_id.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun saveRequestIdToNewColumn() {
        producer.produceTask(listOf(1L), TEST_TIME)
    }

    companion object {
        private val TEST_TIME = Instant.parse("2021-10-18T17:00:00.00Z")
    }
}

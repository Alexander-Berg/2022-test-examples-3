package ru.yandex.market.logistics.mqm.service.logbroker

import com.fasterxml.jackson.databind.ObjectMapper
import com.github.springtestdbunit.annotation.ExpectedDatabase
import com.github.springtestdbunit.assertion.DatabaseAssertionMode
import com.nhaarman.mockitokotlin2.whenever
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.logistics.mqm.AbstractContextualTest
import ru.yandex.market.logistics.mqm.configuration.properties.CheckouterOrderHistoryEventConsumerProperties
import ru.yandex.market.logistics.mqm.utils.queue.extractFileContent

@DisplayName("Проверка чтения событий из Checkouter")
class CheckouterOrderHistoryEventConsumerTest: AbstractContextualTest() {

    @Autowired
    private lateinit var consumer: CheckouterOrderHistoryEventConsumer

    @Autowired
    private lateinit var consumerProperties: CheckouterOrderHistoryEventConsumerProperties

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @DisplayName("Успешная обработка события")
    @Test
    @ExpectedDatabase(
        value = "/service/logbroker/checkouter_order_history_event_success_consume.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun successConsume() {
        val entity = extractFileContent("service/logbroker/checkouter_order_history_event.json")
        val delivered = extractFileContent(
            "service/logbroker/checkouter_order_history_event_delivered.json"
        )
        val parser = createCheckouterOrderHistoryEventLbParser()
        val dto = parser.parseLine(consumerProperties.entityType, entity)
        val deliveredEvent = parser.parseLine(consumerProperties.entityType, delivered)
        consumer.accept(listOf(dto, deliveredEvent))
    }

    @DisplayName("Не создавать таски при выключенной настройке processingSaveEnabled")
    @Test
    @ExpectedDatabase(
        value = "/service/logbroker/checkouter_order_history_event_disabled_property.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun disabledProcessingSaveEnabledProperty() {
        whenever(consumerProperties.processingEnabled).thenReturn(false)
        val entity = extractFileContent("service/logbroker/checkouter_order_history_event.json")
        val parser = createCheckouterOrderHistoryEventLbParser()
        val dto = parser.parseLine(consumerProperties.entityType, entity)
        consumer.accept(listOf(dto))
    }

    private fun createCheckouterOrderHistoryEventLbParser() =
        CheckouterOrderHistoryEventLbParser(consumerProperties.entityType, objectMapper)

}

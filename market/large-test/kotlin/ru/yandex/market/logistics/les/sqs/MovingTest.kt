package ru.yandex.market.logistics.les.sqs

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import ru.yandex.market.logistics.les.AbstractLargeContextualTest
import ru.yandex.market.logistics.les.OrderDamagedEvent
import ru.yandex.market.logistics.les.base.Event
import ru.yandex.market.logistics.les.model.dto.request.MoveEventsRequest

class MovingTest : AbstractLargeContextualTest() {
    private var fromQueueName = "from_queue"
    private var toQueueName = "to_queue"
    private var source = "test_source"
    private var eventType = "test_event_type"
    private var mapper = jacksonObjectMapper()

    private var event = Event(
        source,
        "event_id",
        0,
        eventType,
        OrderDamagedEvent("123"),
        null
    )

    @BeforeEach
    fun setUp() {
        jmsTemplate.receiveTimeout = 10000
        client.createQueue(fromQueueName)
        client.createQueue(toQueueName)
    }

    @Test
    fun moveWithNumberOfMessages() {
        val number = 10

        fillQueue(number)
        val request = MoveEventsRequest(fromQueueName, toQueueName, number)
        sendRequest(request)
        checkOutQueue(number)
        queueIsEmpty()
    }

    @Test
    fun moveWithTimeout() {
        val number = 5

        fillQueue(number)
        val request = MoveEventsRequest(fromQueueName, toQueueName, number + 1)
        sendRequest(request)
        checkOutQueue(number)
        queueIsEmpty()
    }

    private fun fillQueue(number: Int) {
        repeat(number) {
            jmsTemplate.convertAndSend(fromQueueName, event)
        }
    }

    private fun checkOutQueue(number: Int) {
        repeat(number) {
            val resEvent1 = jmsTemplate.receiveAndConvert(toQueueName) as Event
            assertEquals(event, resEvent1)
        }
    }

    private fun sendRequest(request: MoveEventsRequest) {
        val requestData = mapper.writeValueAsString(request)

        mockMvc.perform(MockMvcRequestBuilders.post("/events/move")
            .contentType(MediaType.APPLICATION_JSON)
            .content(requestData))
            .andExpect(MockMvcResultMatchers.status().isOk)
    }

    private fun queueIsEmpty() {
        val message = jmsTemplate.receive(fromQueueName)
        assertNull(message)
    }
}

package ru.yandex.market.logistics.les.controller

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.github.springtestdbunit.annotation.DatabaseSetup
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import ru.yandex.market.logistics.les.AbstractLargeContextualTest
import ru.yandex.market.logistics.les.model.dto.request.SubscriptionRequest
import ru.yandex.market.logistics.les.model.dto.response.SubscriptionResponse


open class SubscriptionTest : AbstractLargeContextualTest() {

    private val subscriber = "test_subscriber"
    private val queueName = "${subscriber}_in"
    private val source = "test_source"
    private val eventType = "test_event_type"

    private var mapper = jacksonObjectMapper()

    @Test
    @DatabaseSetup("/controller/setup.xml")
    fun createQueue() {
        val request = SubscriptionRequest(subscriber, source, eventType)
        val requestData = mapper.writeValueAsString(request)

        val result = mockMvc.perform(post("/subscribe")
            .contentType(MediaType.APPLICATION_JSON)
            .content(requestData))
            .andExpect(status().isOk)
            .andReturn()

        val response: SubscriptionResponse = mapper.readValue(result.response.contentAsString)
        assertEquals(queueName, response.queueName)

        val url = "${lesSqsProperties.endpointHost}/queue/${queueName}"
        assertEquals(url, response.queueUrl)

        val queueUrls = client.listQueues().queueUrls
        assertTrue(queueUrls.contains(response.queueUrl))
    }
}

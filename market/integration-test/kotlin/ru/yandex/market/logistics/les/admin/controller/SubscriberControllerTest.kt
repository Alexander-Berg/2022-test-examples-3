package ru.yandex.market.logistics.les.admin.controller

import com.amazonaws.services.sqs.model.CreateQueueResult
import com.github.springtestdbunit.annotation.DatabaseSetup
import com.github.springtestdbunit.annotation.ExpectedDatabase
import com.github.springtestdbunit.assertion.DatabaseAssertionMode
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.verifyNoMoreInteractions
import com.nhaarman.mockitokotlin2.whenever
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.DisplayName
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import ru.yandex.market.logistics.les.AbstractContextualTest
import ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.jsonContent

@DisplayName("Получение информации о подписчиках")
class SubscriberControllerTest : AbstractContextualTest() {

    @Test
    @DatabaseSetup("/admin/subscriber/before/subscribers.xml")
    fun getSubscribers() {
        mockMvc.perform(get("/admin/subscribers"))
            .andExpect(status().isOk)
            .andExpect(jsonContent("admin/subscriber/response/subscribers.json", false))
    }

    @Test
    @DatabaseSetup("/admin/subscriber/before/subscribers.xml")
    fun getSubscriberOptionsWithoutCurrentValue() {
        mockMvc.perform(get("/admin/subscriber-options"))
            .andExpect(status().isOk)
            .andExpect(jsonContent("admin/subscriber/response/subscriber-options-without-value.json", false))
    }

    @Test
    @DatabaseSetup("/admin/subscriber/before/subscribers.xml")
    fun getSubscriberOptionsWithCurrentValue() {
        mockMvc.perform(get("/admin/subscriber-options").param("subscriberId", "2"))
            .andExpect(status().isOk)
            .andExpect(jsonContent("admin/subscriber/response/subscriber-options-with-value.json", false))
    }

    @Test
    @ExpectedDatabase(
        "/admin/subscriber/after/new_subscriber.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun createSubscriber() {
        val commonQueueUrl = "http://localhost:1234/queue/new_sub_in"
        whenever(sqsClient.createQueue("new_sub_in")).thenReturn(CreateQueueResult().apply {
            queueUrl = commonQueueUrl
        })
        whenever(sqsClient.createQueue("new_sub_in_dlq")).thenReturn(CreateQueueResult().apply {
            queueUrl = "${commonQueueUrl}_dlq"
        })

        mockMvc.perform(
            post("/admin/create-subscriber")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\": \"new_sub\"}")
        )
            .andExpect(status().isOk)

        verify(sqsClient).createQueue("new_sub_in")
        verify(sqsClient).createQueue("new_sub_in_dlq")
    }

    @Test
    @DatabaseSetup("/admin/subscriber/before/subscribers.xml")
    @ExpectedDatabase(
        "/admin/subscriber/before/subscribers.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun doNotCreateExistingSubscriber() {
        mockMvc.perform(
            post("/admin/create-subscriber")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\": \"never\"}")
        )
            .andExpect(status().isOk)

        verifyNoMoreInteractions(sqsClient)
    }
}

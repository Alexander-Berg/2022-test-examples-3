package ru.yandex.market.logistics.logistrator.controller

import com.github.springtestdbunit.annotation.DatabaseSetup
import com.github.springtestdbunit.annotation.ExpectedDatabase
import com.github.springtestdbunit.assertion.DatabaseAssertionMode
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.verifyNoMoreInteractions
import net.javacrumbs.jsonunit.spring.JsonUnitResultMatchers.json
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

import ru.yandex.market.logistics.logistrator.AbstractContextualTest
import ru.yandex.market.logistics.logistrator.queue.payload.RequestIdPayload
import ru.yandex.market.logistics.logistrator.queue.processor.RequestValidationProcessor
import ru.yandex.market.logistics.logistrator.queue.processor.sc_or_ff_creation.ScOrFfPartnerCreationProcessor
import ru.yandex.market.logistics.logistrator.utils.REQUEST_ID
import ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.extractFileContent

@DisplayName("Работа с состоянием запросов")
internal class RequestControllerTest : AbstractContextualTest() {

    @AfterEach
    private fun tearDown() {
        verifyNoMoreInteractions(dbQueueService)
    }

    @Test
    @DatabaseSetup("/db/request/before/ready.xml")
    @ExpectedDatabase("/db/request/after/ready.xml", assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    @DisplayName("Получение данных о запросе")
    fun testGetRequest() {
        mockMvc.perform(get("/requests/$REQUEST_ID"))
            .andExpect(status().isOk)
            .andExpect(json().isEqualTo(extractFileContent("response/ready.json")))
    }

    @Test
    @DatabaseSetup("/db/request/before/draft.xml")
    @ExpectedDatabase(
        "/db/request/after/sent_to_validation.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @DisplayName("Перевод запроса из статуса драфта в статус созданного запроса")
    fun testCommitDraft() {
        mockMvc.perform(post("/requests/$REQUEST_ID/commit"))
            .andExpect(status().isOk)
            .andExpect(json().isEqualTo(extractFileContent("response/sent_to_validation.json")))

        verify(dbQueueService).produceTask(eq(RequestValidationProcessor::class.java), eq(RequestIdPayload(REQUEST_ID)))
    }

    @Test
    @DatabaseSetup("/db/request/before/error.xml")
    @ExpectedDatabase("/db/request/after/retried.xml", assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    @DisplayName("Возобновление обработки запроса, упавшего с ошибкой")
    fun testRetryFailedRequestHandling() {
        mockMvc.perform(post("/requests/$REQUEST_ID/retry"))
            .andExpect(status().isOk)
            .andExpect(json().isEqualTo(extractFileContent("response/retried.json")))

        verify(dbQueueService)
            .produceTask(eq(ScOrFfPartnerCreationProcessor::class.java), eq(RequestIdPayload(REQUEST_ID)))
    }
}

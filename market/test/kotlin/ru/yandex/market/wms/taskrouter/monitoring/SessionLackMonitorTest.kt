package ru.yandex.market.wms.taskrouter.monitoring

import io.netty.handler.codec.http.HttpResponseStatus.SERVICE_UNAVAILABLE
import okhttp3.mockwebserver.MockResponse
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultHandlers
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import ru.yandex.market.wms.taskrouter.config.BaseTest
import ru.yandex.market.wms.taskrouter.monitoring.repository.SessionPerUserRepository
import ru.yandex.market.wms.taskrouter.notification.websocket.queue.job.CrossNodeNotifier
import ru.yandex.market.wms.taskrouter.notification.websocket.queue.job.NotConnectedCleaner
import ru.yandex.market.wms.taskrouter.task.service.TaskManagementService

class SessionLackMonitorTest : BaseTest() {

    @MockBean
    @Autowired
    private lateinit var sessionPerUserRepository: SessionPerUserRepository

    @MockBean
    private lateinit var notConnectedCleaner: NotConnectedCleaner

    @MockBean
    private lateinit var taskManagementService: TaskManagementService

    @MockBean
    private lateinit var crossNodeNotifier: CrossNodeNotifier

    @Test
    fun `Test ok with retries`() {
        Mockito.`when`(sessionPerUserRepository.getActiveUsers()).thenReturn(270)

        ttsServer.enqueue(MockResponse().setResponseCode(SERVICE_UNAVAILABLE.code()))
        ttsServer.enqueue(MockResponse().setResponseCode(SERVICE_UNAVAILABLE.code()))
        ttsServer.enqueue(MockResponse().setResponseCode(SERVICE_UNAVAILABLE.code()))
        ttsServer.enqueue(
            MockResponse()
                .addHeader("Content-Type", "application/json")
                .setBody("""{"limit":0,"offset":0,"total":135}""")
        )

        mockMvc.perform(MockMvcRequestBuilders.get("/monitoring/session-lack"))
            .andDo(MockMvcResultHandlers.print())
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(
                MockMvcResultMatchers.content().string(
                    "0;There are sessions for 270 usersin task router. There are 135 users in TTS. This is fine."
                )
            )
    }

    @Test
    fun `Test not ok with retries`() {
        Mockito.`when`(sessionPerUserRepository.getActiveUsers()).thenReturn(270)

        ttsServer.enqueue(MockResponse().setResponseCode(SERVICE_UNAVAILABLE.code()))
        ttsServer.enqueue(MockResponse().setResponseCode(SERVICE_UNAVAILABLE.code()))
        ttsServer.enqueue(MockResponse().setResponseCode(SERVICE_UNAVAILABLE.code()))
        ttsServer.enqueue(MockResponse().setResponseCode(SERVICE_UNAVAILABLE.code()))

        mockMvc.perform(MockMvcRequestBuilders.get("/monitoring/session-lack"))
            .andDo(MockMvcResultHandlers.print())
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(
                MockMvcResultMatchers.content().string(
                    "1;TTS is currently ill. Cannot obtain users online."
                )
            )
    }

    companion object {

        @BeforeAll
        @JvmStatic
        fun setUpServer() {
            try {
                ttsServer.start()
            } catch (_: IllegalArgumentException) {
            }
        }

        @AfterAll
        @JvmStatic
        fun tearDownServer() {
            ttsServer.shutdown()
        }
    }
}

package ru.yandex.market.wms.core.controller.planned

import com.fasterxml.jackson.databind.ObjectMapper
import com.github.springtestdbunit.annotation.DatabaseSetup
import com.github.springtestdbunit.annotation.ExpectedDatabase
import com.github.springtestdbunit.assertion.DatabaseAssertionMode
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import ru.yandex.market.wms.common.spring.IntegrationTest
import ru.yandex.market.wms.core.base.request.IndirectActivityRequest
import java.time.LocalDateTime

class CreatePlannedIndirectActivityControllerTest : IntegrationTest() {

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Test
    @DatabaseSetup("/controller/planned-indirect-activities/db/before-startIndirectActivity-list-no-duration.xml")
    @ExpectedDatabase(
        value = "/controller/planned-indirect-activities/db/after-startIndirectActivity-list-start-time-no-end.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    fun `startIndirectActivityList planned indirect activity no endTime`() {
        mockMvc.perform(
            defaultPostRequest(
                objectMapper.writeValueAsString(
                    IndirectActivityRequest(
                        users = listOf(
                            "anonymousUser",
                            "anonymousUserSecond"
                        ),
                        startTime = LocalDateTime.of(2021, 12, 10, 10, 0),
                        endTime = null
                    )
                )
            )
        ).andExpect(MockMvcResultMatchers.status().isOk)
    }

    @Test
    @DatabaseSetup("/controller/planned-indirect-activities/db/before-startIndirectActivity-list.xml")
    @ExpectedDatabase(
        value = "/controller/planned-indirect-activities/db/after-startIndirectActivity-list-start-time.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    fun `startIndirectActivityList planned indirect activity`() {
        mockMvc.perform(
            defaultPostRequest(
                objectMapper.writeValueAsString(
                    IndirectActivityRequest(
                        users = listOf(
                            "anonymousUser",
                            "anonymousUserSecond"
                        ),
                        startTime = LocalDateTime.of(2021, 12, 10, 10, 0),
                        endTime = LocalDateTime.of(2021, 12, 10, 12, 0),
                    )
                )
            )
        ).andExpect(MockMvcResultMatchers.status().isOk)
    }

    @Test
    @DatabaseSetup("/controller/planned-indirect-activities/db/before-startIndirectActivity-list.xml")
    @ExpectedDatabase(
        value = "/controller/planned-indirect-activities/db/after-startIndirectActivity-list-start-time-with-duration.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    fun `startIndirectActivityList planned indirect activity no endTime but using duration`() {
        mockMvc.perform(
            defaultPostRequest(
                objectMapper.writeValueAsString(
                    IndirectActivityRequest(
                        users = listOf(
                            "anonymousUser",
                            "anonymousUserSecond"
                        ),
                        startTime = LocalDateTime.of(2021, 12, 10, 10, 0),
                        endTime = null
                    )
                )
            )
        ).andExpect(MockMvcResultMatchers.status().isOk)
    }

    @Test
    fun `exception when time is not future`() {
        mockMvc.perform(
            defaultPostRequest(
                objectMapper.writeValueAsString(
                    IndirectActivityRequest(
                        users = listOf(
                            "anonymousUser"
                        ),
                        startTime = LocalDateTime.of(2019, 12, 10, 10, 0),
                        endTime = null
                    )
                )
            )
        ) .andExpect(MockMvcResultMatchers.status().isBadRequest)
    }

    @Test
    @DatabaseSetup("/controller/indirect-activities/db/before-startIndirectActivity.xml")
    @ExpectedDatabase(
        value = "/controller/indirect-activities/db/after-startIndirectActivity-planned.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    fun `startIndirectActivityList planned but in the last 30 minutes`() {
        mockMvc.perform(
            defaultPostRequest(
                objectMapper.writeValueAsString(
                    IndirectActivityRequest(
                        users = listOf(
                            "anonymousUser"
                        ),
                        startTime = LocalDateTime.of(2020, 4, 1, 12, 15),
                        endTime = null,
                    )
                )
            )
        ).andExpect(MockMvcResultMatchers.status().isOk)
    }



    private fun defaultPostRequest(content: String) = MockMvcRequestBuilders
        .post("/indirect-activities/test_activity/start/list")
        .contentType(MediaType.APPLICATION_JSON_VALUE)
        .accept(MediaType.APPLICATION_JSON)
        .content(content)
}

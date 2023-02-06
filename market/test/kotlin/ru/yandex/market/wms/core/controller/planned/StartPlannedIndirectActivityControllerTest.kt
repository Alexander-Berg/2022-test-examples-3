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
import ru.yandex.market.wms.core.base.request.StartPlannedIndirectActivitiesRequest
import java.time.LocalDateTime

class StartPlannedIndirectActivityControllerTest : IntegrationTest() {

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Test
    @DatabaseSetup("/controller/planned-indirect-activities/db/before-startPlannedActivity.xml")
    @ExpectedDatabase(
        value = "/controller/planned-indirect-activities/db/after-startPlannedActivity.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    fun `start planned indirect activity`() {
        mockMvc.perform(
            MockMvcRequestBuilders.post("/indirect-activities/planned/start")
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .accept(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsString(
                        StartPlannedIndirectActivitiesRequest(
                            curTime = LocalDateTime.of(2022, 4, 1, 22, 0)
                        )
                    )
                )

        ).andExpect(MockMvcResultMatchers.status().isOk)
    }

    @Test
    @DatabaseSetup("/controller/planned-indirect-activities/db/before-startPlannedActivity-no-endtime.xml")
    @ExpectedDatabase(
        value = "/controller/planned-indirect-activities/db/after-startPlannedActivity-no-endtime.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    fun `start planned indirect activity no endtime`() {
        mockMvc.perform(
            MockMvcRequestBuilders.post("/indirect-activities/planned/start")
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .accept(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsString(
                        StartPlannedIndirectActivitiesRequest(
                            curTime = LocalDateTime.of(2022, 4, 1, 22, 0)
                        )
                    )
                )

        ).andExpect(MockMvcResultMatchers.status().isOk)
    }

    @Test
    @DatabaseSetup("/controller/planned-indirect-activities/db/before-startPlannedActivity.xml")
    @ExpectedDatabase(
        value = "/controller/planned-indirect-activities/db/after-startPlannedActivity-nothing-to-start.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    fun `start planned indirect activity when nothing to start`() {
        mockMvc.perform(
            MockMvcRequestBuilders.post("/indirect-activities/planned/start")
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .accept(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsString(
                        StartPlannedIndirectActivitiesRequest(
                            curTime = LocalDateTime.of(2022, 4, 1, 21, 0)
                        )
                    )
                )

        ).andExpect(MockMvcResultMatchers.status().isOk)
    }

    @Test
    @DatabaseSetup("/controller/planned-indirect-activities/db/before-startPlannedActivity.xml")
    @ExpectedDatabase(
        value = "/controller/planned-indirect-activities/db/after-startPlannedActivity-multiple.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    fun `start multiple planned indirect activity`() {
        mockMvc.perform(
            MockMvcRequestBuilders.post("/indirect-activities/planned/start")
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .accept(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsString(
                        StartPlannedIndirectActivitiesRequest(
                            curTime = LocalDateTime.of(2022, 4, 1, 22, 30)
                        )
                    )
                )

        ).andExpect(MockMvcResultMatchers.status().isOk)
    }

    @Test
    @DatabaseSetup("/controller/planned-indirect-activities/db/before-startPlannedActivity-by-id.xml")
    @ExpectedDatabase(
        value = "/controller/planned-indirect-activities/db/after-startPlannedActivity-by-id.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    fun `start planned indirect activity by id`() {
        mockMvc.perform(
            MockMvcRequestBuilders.post("/indirect-activities/planned/start/1")

        ).andExpect(MockMvcResultMatchers.status().isOk)
    }
}

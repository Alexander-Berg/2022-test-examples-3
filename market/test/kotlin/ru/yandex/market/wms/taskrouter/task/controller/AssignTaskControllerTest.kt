package ru.yandex.market.wms.taskrouter.task.controller

import com.github.springtestdbunit.annotation.DatabaseSetup
import com.github.springtestdbunit.annotation.DatabaseSetups
import com.github.springtestdbunit.annotation.ExpectedDatabase
import com.github.springtestdbunit.assertion.DatabaseAssertionMode
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import ru.yandex.market.wms.taskrouter.config.BaseTest

internal class AssignTaskControllerTest : BaseTest(){

    @Test
    @DatabaseSetups(
        DatabaseSetup("/taskmanagement/db/common.xml")
    )
    @ExpectedDatabase(
        value = "/taskmanagement/db/assigntask/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun assignUserToZone() {
        mockMvc.perform(
            MockMvcRequestBuilders.post("/task/assign")
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .accept(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                        "user":"some_user",
                        "zone":"MEZONIN_2",
                        "process":"PICKING"
                    }
                    """
                )
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
    }

    @Test
    @DatabaseSetups(
        DatabaseSetup("/taskmanagement/db/common.xml")
    )
    @ExpectedDatabase(
        value = "/taskmanagement/db/assigntask/after-expected-end-time.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun assignUserToZoneWithExpectedEndTime() {
        mockMvc.perform(
            MockMvcRequestBuilders.post("/task/assign")
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .accept(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                        "user":"some_user",
                        "zone":"MEZONIN_2",
                        "process":"PICKING",
                        "expectedEndTime": "2022-04-25T15:00:00"
                    }
                    """
                )
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
    }

    @Test
    @DatabaseSetups(
        DatabaseSetup("/taskmanagement/db/common.xml")
    )
    fun assignUserToInvalidZone() {
        mockMvc.perform(
            MockMvcRequestBuilders.post("/task/assign")
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .accept(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                        "user":"some_user",
                        "zone":"NOT_EXISTS",
                        "process":"PICKING"
                    }
                    """
                )
        )
            .andExpect(MockMvcResultMatchers.status().is4xxClientError)
    }

    @Test
    @DatabaseSetups(
        DatabaseSetup("/taskmanagement/db/common.xml")
    )
    @ExpectedDatabase(
        value = "/taskmanagement/db/assigntask/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun assignUserToZoneTwice() {
        mockMvc.perform(
            MockMvcRequestBuilders.post("/task/assign")
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .accept(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                        "user":"some_user",
                        "zone":"MEZONIN_1",
                        "process":"PICKING"
                    }
                    """
                )
        )
            .andExpect(MockMvcResultMatchers.status().isOk)

        mockMvc.perform(
            MockMvcRequestBuilders.post("/task/assign")
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .accept(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                        "user":"some_user",
                        "zone":"MEZONIN_2",
                        "process":"PICKING"
                    }
                    """
                )
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
    }

    @Test
    @DatabaseSetups(
        DatabaseSetup("/taskmanagement/db/common.xml"),
        DatabaseSetup("/taskmanagement/db/assigntask-remove/before.xml")
    )
    @ExpectedDatabase(
        value = "/taskmanagement/db/assigntask-remove/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun removeAssignZone() {
        mockMvc.perform(
            MockMvcRequestBuilders.post("/task/assign/remove")
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .accept(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                        "expectedEndTime":"2022-04-25T15:00:00"
                    }
                    """
                )
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
    }

    @Test
    @DatabaseSetups(
        DatabaseSetup("/taskmanagement/db/common.xml"),
        DatabaseSetup("/taskmanagement/db/assigntask-remove/before.xml")
    )
    @ExpectedDatabase(
        value = "/taskmanagement/db/assigntask-remove/after-time-user.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun removeAssignZoneTimeAndUser() {
        mockMvc.perform(
            MockMvcRequestBuilders.post("/task/assign/remove")
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .accept(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                        "user": "user1",
                        "expectedEndTime":"2022-04-25T15:00:00"
                    }
                    """
                )
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
    }

    @Test
    @DatabaseSetups(
        DatabaseSetup("/taskmanagement/db/common.xml"),
        DatabaseSetup("/taskmanagement/db/assigntask-remove/before.xml")
    )
    @ExpectedDatabase(
        value = "/taskmanagement/db/assigntask-remove/after-user.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun removeAssignZoneOnlyUser() {
        mockMvc.perform(
            MockMvcRequestBuilders.post("/task/assign/remove")
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .accept(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                        "user": "user3"
                    }
                    """
                )
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
    }
}

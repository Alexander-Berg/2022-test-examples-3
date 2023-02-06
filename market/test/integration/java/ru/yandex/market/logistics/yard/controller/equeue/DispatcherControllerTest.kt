package ru.yandex.market.logistics.yard.controller.equeue

import com.github.springtestdbunit.annotation.DatabaseSetup
import com.github.springtestdbunit.annotation.ExpectedDatabase
import com.github.springtestdbunit.assertion.DatabaseAssertionMode
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultHandlers
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import ru.yandex.market.logistics.yard.base.AbstractSecurityMockedContextualTest
import ru.yandex.market.logistics.yard.service.UserService
import ru.yandex.market.logistics.yard.util.FileContentUtils.getFileContent

class DispatcherControllerTest : AbstractSecurityMockedContextualTest() {

    @MockBean
    val userService: UserService? = null

    @BeforeEach
    fun setup() {
        Mockito.`when`(userService!!.getPrincipalLogin()).thenReturn("test_login")
    }

    @Test
    @DatabaseSetup(value = ["classpath:fixtures/controller/dispatcher/before.xml"])
    fun getQueueInfo() {
        mockMvc.perform(
            MockMvcRequestBuilders.get("/window/1/queue")
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(content().json(
                getFileContent("classpath:fixtures/controller/dispatcher/result.json")
            ))
            .andExpect(MockMvcResultMatchers.status().is2xxSuccessful)
    }

    @Test
    @DatabaseSetup(value = ["classpath:fixtures/controller/dispatcher/test-initialWindowNum/before.xml"])
    fun testInitialWindowNum() {
        mockMvc.perform(
            MockMvcRequestBuilders.get("/window/1/queue")
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(content().json(
                getFileContent("classpath:fixtures/controller/dispatcher/test-initialWindowNum/response.json")
            ))
            .andExpect(MockMvcResultMatchers.status().is2xxSuccessful)
    }

    @Test
    @DatabaseSetup(value = ["classpath:fixtures/controller/dispatcher/queue-with-postponed/before.xml"])
    fun getQueueInfoWithPostponed() {
        mockMvc.perform(
            MockMvcRequestBuilders.get("/window/1/queue")
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(
                content().json(
                    getFileContent("classpath:fixtures/controller/dispatcher/queue-with-postponed/response.json")
                )
            )
            .andExpect(MockMvcResultMatchers.status().is2xxSuccessful)
    }

    @Test
    @DatabaseSetup(value = ["classpath:fixtures/controller/dispatcher/queue-with-trip-info/before.xml"])
    fun getQueueInfoWithTripInfo() {
        mockMvc.perform(
            MockMvcRequestBuilders.get("/window/1/queue")
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andDo(MockMvcResultHandlers.print())
            .andExpect(
                content().json(
                    getFileContent("classpath:fixtures/controller/dispatcher/queue-with-trip-info/response.json")
                )
            )
            .andExpect(MockMvcResultMatchers.status().is2xxSuccessful)
    }

    @Test
    @DatabaseSetup(value = ["classpath:fixtures/controller/dispatcher/before_with_assigned.xml"])
    fun testQueueInfoWhenClientAssigned() {
        mockMvc.perform(
            MockMvcRequestBuilders.get("/window/1/queue")
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(content().json("{\"count\":2,\"assignedClientId\":1,\"client\":null,\"postponedTickets\":[]}"))
            .andExpect(MockMvcResultMatchers.status().is2xxSuccessful)
    }

    @Test
    @DatabaseSetup(value = ["classpath:fixtures/controller/dispatcher/before.xml"])
    @ExpectedDatabase(
        value = "classpath:fixtures/controller/dispatcher/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun changeUserInfo() {
        mockMvc.perform(
            MockMvcRequestBuilders.get("/window/1/queue")
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(content().json("{\"count\":1,\"client\":{\"clientId\":1,\"meta\":{\"ticketCode\":\"Р043\",\"requestType\":\"SHIPMENT\",\"takeAwayPallets\":false,\"takeAwayReturns\":false},\"phone\":\"123123123\",\"licencePlate\":\"E105TM53\",\"requiredGateType\":\"big test car\"}}"))
            .andExpect(MockMvcResultMatchers.status().is2xxSuccessful)

        mockMvc.perform(
            MockMvcRequestBuilders.put("/window/1/client")
                .contentType(MediaType.APPLICATION_JSON)
                .content(getFileContent("classpath:fixtures/controller/dispatcher/change.json"))
        )
            .andExpect(MockMvcResultMatchers.status().is2xxSuccessful)
    }

    @Test
    @DatabaseSetup(value = ["classpath:fixtures/controller/dispatcher/before.xml"])
    fun getDispatcherInfoWhenClientProcessing() {
        mockMvc.perform(
            MockMvcRequestBuilders.get("/window/1")
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(content().json("{\"id\":1,\"capacityId\":100,\"readableName\":\"test_capacity_unit\",\"status\":\"PROCESSING\",\"siteName\":\"capacity\",\"serviceName\":\"service\"}"))
            .andExpect(MockMvcResultMatchers.status().is2xxSuccessful)
    }

    @Test
    @DatabaseSetup(value = ["classpath:fixtures/controller/dispatcher/before_with_assigned_with_type.xml"])
    fun getDispatcherInfoWithType() {
        mockMvc.perform(
            MockMvcRequestBuilders.get("/window/1")
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(content().json("{\"id\":1,\"capacityId\":100,\"readableName\":\"test_capacity_unit\",\"status\":\"ON_LINE\",\"siteName\":\"capacity\",\"serviceName\":\"service\",\"serviceType\":\"SORTING_CENTER\"}"))
            .andExpect(MockMvcResultMatchers.status().is2xxSuccessful)
    }

    @Test
    @DatabaseSetup(value = ["classpath:fixtures/controller/dispatcher/before_with_waiting.xml"])
    fun getDispatcherInfoWhenClientWaiting() {
        mockMvc.perform(
            MockMvcRequestBuilders.get("/window/1")
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(content().json("{\"id\":1,\"capacityId\":100,\"readableName\":\"test_capacity_unit\",\"status\":\"WAITING\",\"siteName\":\"capacity\",\"serviceName\":\"service\"}"))
            .andExpect(MockMvcResultMatchers.status().is2xxSuccessful)
    }

    @Test
    @DatabaseSetup(value = ["classpath:fixtures/controller/dispatcher/before.xml"])
    fun changeDispatcherStatusLogin() {

        Mockito.`when`(userService?.getPrincipalLogin()).thenReturn("test")

        mockMvc.perform(
            MockMvcRequestBuilders.post("/window/2/login")
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andDo(MockMvcResultHandlers.print())
            .andExpect(content().json("{\"id\":2,\"capacityId\":100,\"readableName\":\"test_capacity_unit_2\",\"status\":\"PAUSED\",\"siteName\":\"capacity\",\"serviceName\":\"service\"}"))
            .andExpect(MockMvcResultMatchers.status().is2xxSuccessful)
    }

    @Test
    @DatabaseSetup(value = ["classpath:fixtures/controller/dispatcher/before.xml"])
    fun changeDispatcherStatus() {

        Mockito.`when`(userService?.getPrincipalLogin()).thenReturn("test")

        mockMvc.perform(
            MockMvcRequestBuilders.post("/window/2/login")
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(MockMvcResultMatchers.status().is2xxSuccessful)

        mockMvc.perform(
            MockMvcRequestBuilders.post("/window/2/start")
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andDo(MockMvcResultHandlers.print())
            .andExpect(content().json("{\"id\":2,\"capacityId\":100,\"readableName\":\"test_capacity_unit_2\",\"status\":\"ON_LINE\",\"siteName\":\"capacity\",\"serviceName\":\"service\"}"))
            .andExpect(MockMvcResultMatchers.status().is2xxSuccessful)
    }

    @Test
    @DatabaseSetup(value = ["classpath:fixtures/controller/dispatcher/before.xml"])
    fun changeDispatcherStatusToOnLine() {

        Mockito.`when`(userService?.getPrincipalLogin()).thenReturn("test")

        mockMvc.perform(
            MockMvcRequestBuilders.post("/window/2/start")
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andDo(MockMvcResultHandlers.print())
            .andExpect(content().json("{\"id\":2,\"capacityId\":100,\"readableName\":\"test_capacity_unit_2\",\"status\":\"ON_LINE\",\"siteName\":\"capacity\",\"serviceName\":\"service\"}"))
            .andExpect(MockMvcResultMatchers.status().is2xxSuccessful)
    }

    @Test
    @DatabaseSetup(value = ["classpath:fixtures/controller/dispatcher/before_logout.xml"])
    @ExpectedDatabase(
        value = "classpath:fixtures/controller/dispatcher/after_logout.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun changeDispatcherStatusToOfLine() {

        Mockito.`when`(userService?.getPrincipalLogin()).thenReturn("test")

        mockMvc.perform(
            MockMvcRequestBuilders.post("/window/1/exit")
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andDo(MockMvcResultHandlers.print())
            .andExpect(MockMvcResultMatchers.status().is2xxSuccessful)
    }

    @Test
    @DatabaseSetup(value = ["classpath:fixtures/controller/dispatcher/before_recall.xml"])
    @ExpectedDatabase(
        value = "classpath:fixtures/controller/dispatcher/after_version_update.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun recall() {
        mockMvc.perform(
            MockMvcRequestBuilders.post("/window/1/recall")
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andDo(MockMvcResultHandlers.print())
            .andExpect(content().json("{\"id\":1,\"capacityId\":100,\"readableName\":\"test_capacity_unit\",\"status\":\"WAITING\",\"siteName\":\"capacity\",\"serviceName\":\"service\"}"))
            .andExpect(MockMvcResultMatchers.status().is2xxSuccessful)
    }

    @Test
    @DatabaseSetup(value = ["classpath:fixtures/controller/dispatcher/push/1/before.xml"])
    @ExpectedDatabase(
        value = "classpath:fixtures/controller/dispatcher/push/1/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun push() {
        mockMvc.perform(
            MockMvcRequestBuilders.post("/window/1/push")
                .contentType(MediaType.APPLICATION_JSON)
                .content(getFileContent("classpath:fixtures/controller/dispatcher/push/1/request.json"))
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(
                content().json(
                    getFileContent("classpath:fixtures/controller/dispatcher/push/1/result.json")
                )
            )
    }

    @Test
    @DatabaseSetup(value = ["classpath:fixtures/controller/dispatcher/push/4/before.xml"])
    @ExpectedDatabase(
        value = "classpath:fixtures/controller/dispatcher/push/4/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun pushWithCapacityUnitFreeze() {
        mockMvc.perform(
            MockMvcRequestBuilders.post("/window/1/push")
                .contentType(MediaType.APPLICATION_JSON)
                .content(getFileContent("classpath:fixtures/controller/dispatcher/push/4/request.json"))
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(
                content().json(
                    getFileContent("classpath:fixtures/controller/dispatcher/push/4/result.json")
                )
            )
    }

    @Test
    @DatabaseSetup(value = ["classpath:fixtures/controller/dispatcher/push/5/before.xml"])
    @ExpectedDatabase(
        value = "classpath:fixtures/controller/dispatcher/push/5/before.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun pushWhileConcreteCapacityUnitIsAlreadyOccupied() {
        mockMvc.perform(
            MockMvcRequestBuilders.post("/window/1/push")
                .contentType(MediaType.APPLICATION_JSON)
                .content(getFileContent("classpath:fixtures/controller/dispatcher/push/5/request.json"))
        )
            .andExpect(
                content().string("Allowed edge not found")
            )
    }

    @Test
    @DatabaseSetup(value = ["classpath:fixtures/controller/dispatcher/submit/before.xml"])
    @ExpectedDatabase(
        value = "classpath:fixtures/controller/dispatcher/submit/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun submit() {
        mockMvc.perform(
            MockMvcRequestBuilders.post("/window/1/submit")
                .contentType(MediaType.APPLICATION_JSON)
                .content(getFileContent("classpath:fixtures/controller/dispatcher/submit/request.json"))
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(
                content().json(
                    getFileContent("classpath:fixtures/controller/dispatcher/submit/result.json")
                )
            )
    }

    @Test
    @DatabaseSetup(value = ["classpath:fixtures/controller/dispatcher/submit/submit_empty/before.xml"])
    @ExpectedDatabase(
        value = "classpath:fixtures/controller/dispatcher/submit/submit_empty/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun submitEmpty() {
        mockMvc.perform(
            MockMvcRequestBuilders.post("/window/1/submit")
                .contentType(MediaType.APPLICATION_JSON)
                .content(getFileContent("classpath:fixtures/controller/dispatcher/submit/submit_empty/request.json"))
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(
                content().json(
                    getFileContent("classpath:fixtures/controller/dispatcher/submit/result.json")
                )
            )
    }

    @Test
    @DatabaseSetup(value = ["classpath:fixtures/controller/dispatcher/submit/second_submit/before.xml"])
    @ExpectedDatabase(
        value = "classpath:fixtures/controller/dispatcher/submit/second_submit/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun submitAnotherRequests() {
        mockMvc.perform(
            MockMvcRequestBuilders.post("/window/1/submit")
                .contentType(MediaType.APPLICATION_JSON)
                .content(getFileContent("classpath:fixtures/controller/dispatcher/submit/second_submit/request.json"))
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(
                content().json(
                    getFileContent("classpath:fixtures/controller/dispatcher/submit/result.json")
                )
            )
    }

    @Test
    @DatabaseSetup(value = ["classpath:fixtures/controller/dispatcher/submit/before.xml"])
    @ExpectedDatabase(
        value = "classpath:fixtures/controller/dispatcher/submit/root/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun submitOnlyRootId() {
        mockMvc.perform(
            MockMvcRequestBuilders.post("/window/1/submit")
                .contentType(MediaType.APPLICATION_JSON)
                .content(getFileContent("classpath:fixtures/controller/dispatcher/submit/root/request.json"))
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(
                content().json(
                    getFileContent("classpath:fixtures/controller/dispatcher/submit/root/result.json")
                )
            )
    }

    @Test
    @DatabaseSetup(value = ["classpath:fixtures/controller/dispatcher/push/3/before.xml"])
    @ExpectedDatabase(
        value = "classpath:fixtures/controller/dispatcher/push/3/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun tryPushWithoutAllowedEdge() {
        mockMvc.perform(
            MockMvcRequestBuilders.post("/window/1/push")
                .contentType(MediaType.APPLICATION_JSON)
                .content(getFileContent("classpath:fixtures/controller/dispatcher/push/3/request.json"))
        )
            .andExpect(MockMvcResultMatchers.status().isConflict)
            .andExpect(content().string("Allowed edge not found"))
    }

    @Test
    @DatabaseSetup(value = ["classpath:fixtures/controller/dispatcher/cancel/before.xml"])
    @ExpectedDatabase(
        value = "classpath:fixtures/controller/dispatcher/cancel/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun cancel() {
        mockMvc.perform(
            MockMvcRequestBuilders.post("/window/1/cancel")
                .contentType(MediaType.APPLICATION_JSON)
                .content(getFileContent("classpath:fixtures/controller/dispatcher/cancel/request.json"))
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
    }

    @Test
    @DatabaseSetup(value = ["classpath:fixtures/controller/dispatcher/cancel/deactivate/before.xml"])
    @ExpectedDatabase(
        value = "classpath:fixtures/controller/dispatcher/cancel/deactivate/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun testDeactivateCancel() {
        mockMvc.perform(
            MockMvcRequestBuilders.post("/window/1/submit")
                .contentType(MediaType.APPLICATION_JSON)
                .content(getFileContent("classpath:fixtures/controller/dispatcher/cancel/deactivate/request.json"))
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
    }

    @Test
    @DatabaseSetup(value = ["classpath:fixtures/controller/dispatcher/postpone/before.xml"])
    @ExpectedDatabase(
        value = "classpath:fixtures/controller/dispatcher/postpone/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun postpone() {
        mockMvc.perform(
            MockMvcRequestBuilders.post("/window/1/push")
                .contentType(MediaType.APPLICATION_JSON)
                .content(getFileContent("classpath:fixtures/controller/dispatcher/postpone/request.json"))
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
    }

    @Test
    @DatabaseSetup(value = ["classpath:fixtures/controller/dispatcher/postpone/second-time/before.xml"])
    @ExpectedDatabase(
        value = "classpath:fixtures/controller/dispatcher/postpone/second-time/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun postponeSecondTime() {
        mockMvc.perform(
            MockMvcRequestBuilders.post("/window/1/push")
                .contentType(MediaType.APPLICATION_JSON)
                .content(getFileContent("classpath:fixtures/controller/dispatcher/postpone/second-time/request.json"))
        )
        .andExpect(MockMvcResultMatchers.status().isOk)
    }
}

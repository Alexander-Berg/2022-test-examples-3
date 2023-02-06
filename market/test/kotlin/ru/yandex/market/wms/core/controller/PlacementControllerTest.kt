package ru.yandex.market.wms.core.controller

import com.github.springtestdbunit.annotation.DatabaseSetup
import com.github.springtestdbunit.annotation.ExpectedDatabase
import com.github.springtestdbunit.assertion.DatabaseAssertionMode
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import ru.yandex.market.wms.common.model.enums.EmptyToteAction
import ru.yandex.market.wms.common.spring.IntegrationTest
import ru.yandex.market.wms.common.spring.utils.FileContentUtils
import ru.yandex.market.wms.transportation.client.TransportationClient

class PlacementControllerTest : IntegrationTest() {
    @MockBean
    @Autowired
    private lateinit var transportationClient: TransportationClient

    @Test
    @DatabaseSetup("/controller/placement/moveToCart/1/before.xml")
    @ExpectedDatabase(
        "/controller/placement/moveToCart/1/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `Moving nested ids from tote to cart starts empty tote replenishment`() {
        Mockito.`when`(transportationClient.makeToteEmpty(Mockito.any())).thenReturn(EmptyToteAction.PUT_ON_CONVEYOR)
        mockMvc.perform(
            MockMvcRequestBuilders.post("/placement/moveToCart")
                .contentType(MediaType.APPLICATION_JSON)
                .content(FileContentUtils.getFileContent("controller/placement/moveToCart/request.json"))
        )
            .andExpect(MockMvcResultMatchers.status().`is`(HttpStatus.OK.value()))
            .andExpect(
                MockMvcResultMatchers.content().json(
                    FileContentUtils
                        .getFileContent("controller/placement/moveToCart/response-put-on-conveyor.json"), true
                )
            )

        checkMakeToteEmptyWasCalled("TM01")
    }

    @Test
    @DatabaseSetup("/controller/placement/moveToCart/2/before.xml")
    @ExpectedDatabase(
        "/controller/placement/moveToCart/2/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `Moving serials from tote to cart starts empty tote replenishment`() {
        Mockito.`when`(transportationClient.makeToteEmpty(Mockito.any())).thenReturn(EmptyToteAction.PUT_ON_CONVEYOR)
        mockMvc.perform(
            MockMvcRequestBuilders.post("/placement/moveToCart")
                .contentType(MediaType.APPLICATION_JSON)
                .content(FileContentUtils.getFileContent("controller/placement/moveToCart/request.json"))
        )
            .andExpect(MockMvcResultMatchers.status().`is`(HttpStatus.OK.value()))
            .andExpect(
                MockMvcResultMatchers.content().json(
                    FileContentUtils
                        .getFileContent("controller/placement/moveToCart/response-put-on-conveyor.json"), true
                )
            )

        checkMakeToteEmptyWasCalled("TM01")
    }

    @Test
    @DatabaseSetup("/controller/placement/moveToCart/3/before.xml")
    @ExpectedDatabase(
        "/controller/placement/moveToCart/3/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `Moving nested ids from BL to cart doesn't start empty tote replenishment`() {
        Mockito.`when`(transportationClient.makeToteEmpty(Mockito.any())).thenReturn(EmptyToteAction.PUT_ON_CONVEYOR)
        mockMvc.perform(
            MockMvcRequestBuilders.post("/placement/moveToCart")
                .contentType(MediaType.APPLICATION_JSON)
                .content(FileContentUtils.getFileContent("controller/placement/moveToCart/3/request.json"))
        )
            .andExpect(MockMvcResultMatchers.status().`is`(HttpStatus.OK.value()))
            .andExpect(
                MockMvcResultMatchers.content().json(
                    FileContentUtils
                        .getFileContent("controller/placement/moveToCart/response-no-action.json"), true
                )
            )

        Mockito.verify(transportationClient, Mockito.never()).makeToteEmpty("BL01")
    }

    @Test
    fun `Moving nested ids from cart to cart throws exception`() {
        mockMvc.perform(
            MockMvcRequestBuilders.post("/placement/moveToCart")
                .contentType(MediaType.APPLICATION_JSON)
                .content(FileContentUtils.getFileContent("controller/placement/moveToCart/4/request.json"))
        )
            .andExpect(MockMvcResultMatchers.status().`is`(HttpStatus.BAD_REQUEST.value()))
            .andExpect(
                MockMvcResultMatchers.content().json(
                    FileContentUtils
                        .getFileContent("controller/placement/moveToCart/4/response.json"), true
                )
            )
    }

    @Test
    fun `Moving nested ids from tote to tote throws exception`() {
        mockMvc.perform(
            MockMvcRequestBuilders.post("/placement/moveToCart")
                .contentType(MediaType.APPLICATION_JSON)
                .content(FileContentUtils.getFileContent("controller/placement/moveToCart/5/request.json"))
        )
            .andExpect(MockMvcResultMatchers.status().`is`(HttpStatus.BAD_REQUEST.value()))
            .andExpect(
                MockMvcResultMatchers.content().json(
                    FileContentUtils
                        .getFileContent("controller/placement/moveToCart/5/response.json"), true
                )
            )
    }

    @Test
    fun `Check container - id doesn't exist`() {
        mockMvc.perform(
            MockMvcRequestBuilders.get("/placement/checkContainer?containerId=TM12345")
        )
            .andExpect(MockMvcResultMatchers.status().`is`(HttpStatus.NOT_FOUND.value()))
            .andExpect(
                MockMvcResultMatchers.content().json(
                    FileContentUtils
                        .getFileContent("controller/placement/moveToCart/6/response.json"), true
                )
            )
    }

    @Test
    @DatabaseSetup("/controller/placement/moveToCart/7/before.xml")
    fun `Check container - id is empty`() {
        mockMvc.perform(
            MockMvcRequestBuilders.get("/placement/checkContainer?containerId=TM12345")
        )
            .andExpect(MockMvcResultMatchers.status().`is`(HttpStatus.BAD_REQUEST.value()))
            .andExpect(
                MockMvcResultMatchers.content().json(
                    FileContentUtils
                        .getFileContent("controller/placement/moveToCart/7/response.json"), true
                )
            )
    }

    @Test
    @DatabaseSetup("/controller/placement/moveToCart/8/before.xml")
    fun `Moving nested ids from tote to cart with serials causes exception`() {
        mockMvc.perform(
            MockMvcRequestBuilders.post("/placement/moveToCart")
                .contentType(MediaType.APPLICATION_JSON)
                .content(FileContentUtils.getFileContent("controller/placement/moveToCart/request.json"))
        )
            .andExpect(MockMvcResultMatchers.status().`is`(HttpStatus.BAD_REQUEST.value()))
            .andExpect(
                MockMvcResultMatchers.content().json(
                    FileContentUtils
                        .getFileContent("controller/placement/moveToCart/8/response.json"), true
                )
            )
    }

    @Test
    @DatabaseSetup("/controller/placement/moveToCart/9/before.xml")
    fun `Moving BL to cart with serials causes exception`() {
        mockMvc.perform(
            MockMvcRequestBuilders.post("/placement/moveToCart")
                .contentType(MediaType.APPLICATION_JSON)
                .content(FileContentUtils.getFileContent("controller/placement/moveToCart/3/request.json"))
        )
            .andExpect(MockMvcResultMatchers.status().`is`(HttpStatus.BAD_REQUEST.value()))
            .andExpect(
                MockMvcResultMatchers.content().json(
                    FileContentUtils
                        .getFileContent("controller/placement/moveToCart/11/response.json"), true
                )
            )
    }

    @Test
    @DatabaseSetup("/controller/placement/moveToCart/10/before.xml")
    fun `Moving serials from tote to cart with nested ids causes exception`() {
        mockMvc.perform(
            MockMvcRequestBuilders.post("/placement/moveToCart")
                .contentType(MediaType.APPLICATION_JSON)
                .content(FileContentUtils.getFileContent("controller/placement/moveToCart/request.json"))
        )
            .andExpect(MockMvcResultMatchers.status().`is`(HttpStatus.BAD_REQUEST.value()))
            .andExpect(
                MockMvcResultMatchers.content().json(
                    FileContentUtils
                        .getFileContent("controller/placement/moveToCart/10/response.json"), true
                )
            )

    }

    @Test
    @DatabaseSetup("/controller/placement/moveToCart/12/before.xml")
    fun `Moving serials from tote to cart with buffer validation exception`() {
        mockMvc.perform(
            MockMvcRequestBuilders.post("/placement/moveToCart")
                .contentType(MediaType.APPLICATION_JSON)
                .content(FileContentUtils.getFileContent("controller/placement/moveToCart/request-w-buf-loc.json"))
        )
            .andExpect(MockMvcResultMatchers.status().`is`(HttpStatus.BAD_REQUEST.value()))
            .andExpect(
                MockMvcResultMatchers.content().json(
                    FileContentUtils
                        .getFileContent("controller/placement/moveToCart/12/response.json"), true
                )
            )
    }

    @Test
    @DatabaseSetup("/controller/placement/moveToCart/13/before.xml")
    @ExpectedDatabase(
        "/controller/placement/moveToCart/13/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `Moving serials from tote to cart with moving to buffer`() {
        Mockito.`when`(transportationClient.makeToteEmpty(Mockito.any())).thenReturn(EmptyToteAction.PUT_ON_CONVEYOR)
        mockMvc.perform(
            MockMvcRequestBuilders.post("/placement/moveToCart")
                .contentType(MediaType.APPLICATION_JSON)
                .content(FileContentUtils.getFileContent("controller/placement/moveToCart/request-w-buf-loc.json"))
        )
            .andExpect(MockMvcResultMatchers.status().`is`(HttpStatus.OK.value()))
            .andExpect(
                MockMvcResultMatchers.content().json(
                    FileContentUtils
                        .getFileContent("controller/placement/moveToCart/response-put-on-conveyor.json"), true
                )
            )

        checkMakeToteEmptyWasCalled("TM01")
    }

    @Test
    @DatabaseSetup("/controller/placement/moveToCart/14/before.xml")
    @ExpectedDatabase(
        "/controller/placement/moveToCart/14/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `Moving serials from tote to cart with not happened moving to buffer`() {
        Mockito.`when`(transportationClient.makeToteEmpty(Mockito.any())).thenReturn(EmptyToteAction.PUT_ON_CONVEYOR)
        mockMvc.perform(
            MockMvcRequestBuilders.post("/placement/moveToCart")
                .contentType(MediaType.APPLICATION_JSON)
                .content(FileContentUtils.getFileContent("controller/placement/moveToCart/request-w-buf-loc.json"))
        )
            .andExpect(MockMvcResultMatchers.status().`is`(HttpStatus.OK.value()))
            .andExpect(
                MockMvcResultMatchers.content().json(
                    FileContentUtils
                        .getFileContent("controller/placement/moveToCart/response-put-on-conveyor.json"), true
                )
            )

        checkMakeToteEmptyWasCalled("TM01")
    }

    @Test
    @DatabaseSetup("/controller/placement/moveToCart/15/before.xml")
    @ExpectedDatabase(
        "/controller/placement/moveToCart/15/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `Moving fast serials from tote to slow cart, turnover check disabled because cart not in fast zone`() {
        Mockito.`when`(transportationClient.makeToteEmpty(Mockito.any())).thenReturn(EmptyToteAction.PUT_ON_CONVEYOR)
        mockMvc.perform(
            MockMvcRequestBuilders.post("/placement/moveToCart")
                .contentType(MediaType.APPLICATION_JSON)
                .content(FileContentUtils.getFileContent("controller/placement/moveToCart/request-w-buf-loc.json"))
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(
                MockMvcResultMatchers.content().json(
                    FileContentUtils
                        .getFileContent("controller/placement/moveToCart/response-put-on-conveyor.json"), true
                )
            )

        checkMakeToteEmptyWasCalled("TM01")
    }

    @Test
    @DatabaseSetup("/controller/placement/moveToCart/16/before.xml")
    @ExpectedDatabase(
        "/controller/placement/moveToCart/16/before.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `Moving fast serials from tote to slow cart, turnover check returns error`() {
        mockMvc.perform(
            MockMvcRequestBuilders.post("/placement/moveToCart")
                .contentType(MediaType.APPLICATION_JSON)
                .content(FileContentUtils.getFileContent("controller/placement/moveToCart/request-w-buf-loc.json"))
        )
            .andExpect(MockMvcResultMatchers.status().isBadRequest)
            .andExpect(
                MockMvcResultMatchers.content().json(
                    FileContentUtils.getFileContent("controller/placement/moveToCart/16/response.json"), true
                )
            )
    }

    @Test
    @DatabaseSetup("/controller/placement/moveToCart/17/before.xml")
    @ExpectedDatabase(
        "/controller/placement/moveToCart/17/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `Moving fast serials from tote to fast cart with turnover check`() {
        Mockito.`when`(transportationClient.makeToteEmpty(Mockito.any())).thenReturn(EmptyToteAction.PUT_ON_CONVEYOR)
        mockMvc.perform(
            MockMvcRequestBuilders.post("/placement/moveToCart")
                .contentType(MediaType.APPLICATION_JSON)
                .content(FileContentUtils.getFileContent("controller/placement/moveToCart/request-w-buf-loc.json"))
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(
                MockMvcResultMatchers.content().json(
                    FileContentUtils
                        .getFileContent("controller/placement/moveToCart/response-put-on-conveyor.json"), true
                )
            )

        checkMakeToteEmptyWasCalled("TM01")
    }

    @Test
    @DatabaseSetup("/controller/placement/moveToCart/18/before.xml")
    @ExpectedDatabase(
        "/controller/placement/moveToCart/18/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `Moving slow nested ids to slow cart with turnover check`() {
        Mockito.`when`(transportationClient.makeToteEmpty(Mockito.any())).thenReturn(EmptyToteAction.PUT_ON_CONVEYOR)
        mockMvc.perform(
            MockMvcRequestBuilders.post("/placement/moveToCart")
                .contentType(MediaType.APPLICATION_JSON)
                .content(FileContentUtils.getFileContent("controller/placement/moveToCart/request.json"))
        )
            .andExpect(MockMvcResultMatchers.status().`is`(HttpStatus.OK.value()))
            .andExpect(
                MockMvcResultMatchers.content().json(
                    FileContentUtils
                        .getFileContent("controller/placement/moveToCart/response-put-on-conveyor.json"), true
                )
            )

        checkMakeToteEmptyWasCalled("TM01")
    }

    @Test
    @DatabaseSetup("/controller/placement/moveToCart/19/before.xml")
    @ExpectedDatabase(
        "/controller/placement/moveToCart/19/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `Moving nested from tote to cart with moving to buffer`() {
        Mockito.`when`(transportationClient.makeToteEmpty(Mockito.any())).thenReturn(EmptyToteAction.PUT_ON_CONVEYOR)
        mockMvc.perform(
            MockMvcRequestBuilders.post("/placement/moveToCart")
                .contentType(MediaType.APPLICATION_JSON)
                .content(FileContentUtils.getFileContent("controller/placement/moveToCart/request-w-buf-loc.json"))
        )
            .andExpect(MockMvcResultMatchers.status().`is`(HttpStatus.OK.value()))
            .andExpect(
                MockMvcResultMatchers.content().json(
                    FileContentUtils
                        .getFileContent("controller/placement/moveToCart/response-put-on-conveyor.json"), true
                )
            )

        checkMakeToteEmptyWasCalled("TM01")
    }

    private fun checkMakeToteEmptyWasCalled(containerId: String) {
        Mockito.verify(transportationClient, Mockito.atLeastOnce()).makeToteEmpty(containerId)
    }

    @Test
    @DatabaseSetup("/controller/placement/moveIdToOutBuf/ok/before.xml")
    @ExpectedDatabase("/controller/placement/moveIdToOutBuf/ok/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    fun `Moving id w serials to st out buf`() {
        mockMvc.perform(
            MockMvcRequestBuilders.post("/placement/moveIdToOutBuf")
                .contentType(MediaType.APPLICATION_JSON)
                .content(FileContentUtils.getFileContent("controller/placement/moveIdToOutBuf/ok/request.json"))
        )
            .andExpect(MockMvcResultMatchers.status().`is`(HttpStatus.OK.value()))
    }

    @Test
    @DatabaseSetup("/controller/placement/moveIdToOutBuf/ok-already-in-buf/before.xml")
    @ExpectedDatabase("/controller/placement/moveIdToOutBuf/ok-already-in-buf/before.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    fun `Moving id w serials to st out buf already in out buf`() {
        mockMvc.perform(
            MockMvcRequestBuilders.post("/placement/moveIdToOutBuf")
                .contentType(MediaType.APPLICATION_JSON)
                .content(FileContentUtils.getFileContent("controller/placement/moveIdToOutBuf/ok/request.json"))
        )
            .andExpect(MockMvcResultMatchers.status().`is`(HttpStatus.OK.value()))
    }

    @Test
    @DatabaseSetup("/controller/placement/moveIdToOutBuf/ok-nesting/before.xml")
    @ExpectedDatabase("/controller/placement/moveIdToOutBuf/ok-nesting/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    fun `Moving id w nesting to st out buf`() {
        mockMvc.perform(
            MockMvcRequestBuilders.post("/placement/moveIdToOutBuf")
                .contentType(MediaType.APPLICATION_JSON)
                .content(FileContentUtils.getFileContent("controller/placement/moveIdToOutBuf/ok-nesting/request.json"))
        )
            .andExpect(MockMvcResultMatchers.status().`is`(HttpStatus.OK.value()))
    }

    @Test
    @DatabaseSetup("/controller/placement/moveIdToOutBuf/error-few-zones/before.xml")
    fun `Moving id w serials error few zones in id`() {
        mockMvc.perform(
            MockMvcRequestBuilders.post("/placement/moveIdToOutBuf")
                .contentType(MediaType.APPLICATION_JSON)
                .content(FileContentUtils.getFileContent(
                    "controller/placement/moveIdToOutBuf/error-few-zones/request.json"))
        )
            .andExpect(MockMvcResultMatchers.status().`is`(HttpStatus.BAD_REQUEST.value()))
            .andExpect(
                MockMvcResultMatchers.content().json(
                    FileContentUtils.getFileContent(
                        "controller/placement/moveIdToOutBuf/error-few-zones/response.json"), true
                )
            )
    }

    @Test
    @DatabaseSetup("/controller/placement/moveIdToOutBuf/error-no-buf/before.xml")
    fun `Moving id w serials error no buf in zone`() {
        mockMvc.perform(
            MockMvcRequestBuilders.post("/placement/moveIdToOutBuf")
                .contentType(MediaType.APPLICATION_JSON)
                .content(FileContentUtils.getFileContent(
                    "controller/placement/moveIdToOutBuf/error-no-buf/request.json"))
        )
            .andExpect(MockMvcResultMatchers.status().`is`(HttpStatus.BAD_REQUEST.value()))
            .andExpect(
                MockMvcResultMatchers.content().json(
                    FileContentUtils.getFileContent(
                        "controller/placement/moveIdToOutBuf/error-no-buf/response.json"), true
                )
            )
    }
}

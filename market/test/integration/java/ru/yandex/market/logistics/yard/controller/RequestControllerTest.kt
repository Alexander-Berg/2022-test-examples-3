package ru.yandex.market.logistics.yard.controller

import com.github.springtestdbunit.annotation.DatabaseSetup
import com.nhaarman.mockitokotlin2.argumentCaptor
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultHandlers
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import ru.yandex.market.ff.client.FulfillmentWorkflowClientApi
import ru.yandex.market.ff.client.dto.ShopRequestYardDTOContainer
import ru.yandex.market.ff.client.dto.ShopRequestYardFilterDTO
import ru.yandex.market.ff.client.enums.RequestItemAttribute
import ru.yandex.market.logistics.yard.base.AbstractSecurityMockedContextualTest
import ru.yandex.market.logistics.yard.util.FileContentUtils
import ru.yandex.market.tpl.common.dsm.client.model.DriverPersonalDataDto
import java.time.LocalDate

class RequestControllerTest : AbstractSecurityMockedContextualTest() {

    @MockBean
    val ffWorkflowApiClient: FulfillmentWorkflowClientApi? = null

    @Test
    fun testAttributes() {
        val captor = argumentCaptor<ShopRequestYardFilterDTO>()

        Mockito.`when`(
            ffWorkflowApiClient!!.getRequestsForYard(captor.capture())
        ).thenReturn(ShopRequestYardDTOContainer())

        mockMvc.perform(
            MockMvcRequestBuilders.get("/requests")
                .param("requestDateFrom", "2016-01-01")
                .param("requestDateTo", "2016-12-06")
                .param("creationDateFrom", "2016-01-01")
                .param("types", "0", "1")
                .param("statuses", "0", "1")
                .param("shopIds", "1", "2")
                .param("attributes", "CTM")
                .param("page", "1")
                .param("size", "2")
                .param("sort", "comment,desc")
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
        assertions().assertThat(captor.firstValue.attributes).isEqualTo(setOf(RequestItemAttribute.CTM))
    }

    @Test
    @Throws(
        Exception::class
    )
    fun findRequests() {
        mockMvc.perform(
            MockMvcRequestBuilders.get("/requests")
                .param("requestDateFrom", "2016-01-01")
                .param("requestDateTo", "2016-12-06")
                .param("creationDateFrom", "2016-01-01")
                .param("types", "0", "1")
                .param("statuses", "0", "1")
                .param("shopIds", "1", "2")
                .param("page", "1")
                .param("size", "2")
                .param("sort", "comment,desc")
        ).andDo(MockMvcResultHandlers.print())
            .andReturn()
    }

    @Test
    @DatabaseSetup("classpath:fixtures/controller/request/before.xml")
    fun testGetTagsByParentId() {
        val expected = FileContentUtils.getFileContent(
            "classpath:fixtures/controller/request/response-by-parent-id.json"
        )
        mockMvc.perform(
            MockMvcRequestBuilders.get("/tags-filter?parentId=1")
        )
            .andDo(MockMvcResultHandlers.print())
            .andExpect(MockMvcResultMatchers.content().json(expected))
    }

    @Test
    @DatabaseSetup("classpath:fixtures/controller/request/service-filter/before-service-list.xml")
    fun getAllAvailableForSearchServices() {
        val expected = FileContentUtils.getFileContent(
            "classpath:fixtures/controller/request/service-filter/response-services-for-filter.json"
        )
        mockMvc.perform(
            MockMvcRequestBuilders.get("/services-for-filter")
        )
            .andDo(MockMvcResultHandlers.print())
            .andExpect(MockMvcResultMatchers.content().json(expected))
    }

    @Test
    @DatabaseSetup("classpath:fixtures/controller/request/before.xml")
    fun testGetTagsByGroupId() {
        val expected = FileContentUtils.getFileContent(
            "classpath:fixtures/controller/request/response-by-group-id.json"
        )
        mockMvc.perform(
            MockMvcRequestBuilders.get("/tags-filter?groupId=2")
        )
            .andDo(MockMvcResultHandlers.print())
            .andExpect(MockMvcResultMatchers.content().json(expected))
    }

    @Test
    @DatabaseSetup("classpath:fixtures/controller/request/before.xml")
    fun testGetTagsByParentIdOrGroupId() {
        val expected = FileContentUtils.getFileContent(
            "classpath:fixtures/controller/request/response-by-parent-id-or-group-id.json"
        )
        mockMvc.perform(
            MockMvcRequestBuilders.get("/tags-filter?parentId=2&groupId=1")
        )
            .andDo(MockMvcResultHandlers.print())
            .andExpect(MockMvcResultMatchers.content().json(expected))
    }

    @Test
    @DatabaseSetup("classpath:fixtures/controller/request/before.xml")
    fun testGetTagsByParentGroupId() {
        val expected = FileContentUtils.getFileContent(
            "classpath:fixtures/controller/request/response-by-parent-group-id.json"
        )
        mockMvc.perform(
            MockMvcRequestBuilders.get("/tags-filter?parentGroupId=1")
        )
            .andDo(MockMvcResultHandlers.print())
            .andExpect(MockMvcResultMatchers.content().json(expected))
    }
    @Test
    @DatabaseSetup("classpath:fixtures/controller/request/search-transportation-partners/before.xml")
    fun testGetTransportationPartners() {
        val expectedById = FileContentUtils.getFileContent(
            "classpath:fixtures/controller/request/search-transportation-partners/response-by-id.json"
        )
        val expectedByName = FileContentUtils.getFileContent(
            "classpath:fixtures/controller/request/search-transportation-partners/response-by-name.json"
        )
        mockMvc.perform(
            MockMvcRequestBuilders.post("/transportation-partners")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"id\": 138585}")
        )
            .andExpect(MockMvcResultMatchers.content().json(expectedById))

        mockMvc.perform(
            MockMvcRequestBuilders.post("/transportation-partners")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\": \"ооо\"}")
        )
            .andExpect(MockMvcResultMatchers.content().json(expectedByName))
    }

    @Test
    fun getPersonalInfo() {
        val courierUid = 123

        Mockito.`when`(driverApi.driverByUidUidPersonalDataGet(courierUid.toString())).thenReturn(
            DriverPersonalDataDto()
                .firstName("firstName")
                .lastName("lastName")
                .passportNumber("passportNumber")
                .issuer("issuer")
                .issuedAt(LocalDate.parse("2022-01-01"))
                .birthday(LocalDate.parse("2000-01-01"))
        )
        val expected = FileContentUtils.getFileContent(
            "classpath:fixtures/controller/request/personal-info/response.json"
        )
        mockMvc.perform(
            MockMvcRequestBuilders.get("/personal-info?courierUid=$courierUid")
        )
            .andExpect(MockMvcResultMatchers.content().json(expected))
    }

    @Test
    @DatabaseSetup("classpath:fixtures/controller/request/search-by-trip-points/before.xml")
    fun testSearchByTripPoints() {
        mockMvc.perform(
            MockMvcRequestBuilders.get("/requests")
                .param("requestDateFrom", "2022-05-25")
                .param("requestDateTo", "2022-05-25")
                .param("transportationPartnerId", "15072021")
                .param("serviceIds", "304")
                .param("page", "0")
                .param("size", "10")
        )
            .andDo(MockMvcResultHandlers.print())
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(
                MockMvcResultMatchers.content().json(
                    FileContentUtils.getFileContent(
                        "classpath:fixtures/controller/request/search-by-trip-points/response.json"
                    )
                )
            )
    }
}

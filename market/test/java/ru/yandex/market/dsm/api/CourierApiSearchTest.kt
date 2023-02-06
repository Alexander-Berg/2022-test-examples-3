package ru.yandex.market.dsm.api

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.web.servlet.MvcResult
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import org.springframework.util.LinkedMultiValueMap
import ru.yandex.market.dsm.config.DsmConstants
import ru.yandex.market.dsm.core.test.AbstractDsmApiTest
import ru.yandex.market.dsm.domain.courier.test.CourierTestFactory
import ru.yandex.market.dsm.domain.employer.EmployersTestFactory
import ru.yandex.market.tpl.common.util.TplObjectMappers
import ru.yandex.mj.generated.server.model.CouriersSearchResultDto

class CourierApiSearchTest : AbstractDsmApiTest() {

    @Autowired
    private lateinit var employersFactory: EmployersTestFactory

    @Autowired
    private lateinit var courierTestFactory: CourierTestFactory

    @Test
    fun `couriersGet - success`() {
        //given
        val employer = employersFactory.createAndSave()
        val courier = courierTestFactory.create(employer.id, "uid")

        //when
        val response = performCouriersRequest(
            pageNumber = 0,
            pageSize = 50,
            params = mapOf()
        )

        //then
        val result = parseResponse(response)

        assertThat(result.pageNumber).isEqualTo(0)
        assertThat(result.pageSize).isEqualTo(50)
        assertThat(result.totalPages).isEqualTo(1)

        val content = result.content
        assertThat(content.size).isEqualTo(1)
        assertThat(content.first().id).isEqualTo(courier.id)
    }

    @Test
    fun `couriersGet - paging`() {
        val employer = employersFactory.createAndSave()
        courierTestFactory.create(employer.id, "uid-1")
        courierTestFactory.create(employer.id, "uid-2")

        val responsePage0 = performCouriersRequest(
            pageNumber = 0,
            pageSize = 1,
            params = mapOf()
        )

        val resultPage0 = parseResponse(responsePage0)

        assertThat(resultPage0.pageNumber).isEqualTo(0)
        assertThat(resultPage0.pageSize).isEqualTo(1)
        assertThat(resultPage0.totalPages).isEqualTo(2)
        assertThat(resultPage0.content.size).isEqualTo(1)

        val responsePage1 = performCouriersRequest(
            pageNumber = 1,
            pageSize = 1,
            params = mapOf()
        )

        val resultPage1 = parseResponse(responsePage1)

        assertThat(resultPage1.pageNumber).isEqualTo(1)
        assertThat(resultPage1.pageSize).isEqualTo(1)
        assertThat(resultPage1.totalPages).isEqualTo(2)
    }

    @Test
    fun `couriersGet - search by employer`() {
        //given
        val employer1 = employersFactory.createAndSave()
        val employer2 = employersFactory.createAndSave()
        val courier1 = courierTestFactory.create(employer1.id, "uid-1")
        courierTestFactory.create(employer2.id, "uid-2")

        //when
        val response = performCouriersRequest(
            pageNumber = 0,
            pageSize = 50,
            params = mapOf(Pair("employerId", employer1.id))
        )

        //then
        val result = parseResponse(response)

        assertThat(result.pageNumber).isEqualTo(0)
        assertThat(result.pageSize).isEqualTo(50)
        assertThat(result.totalPages).isEqualTo(1)

        val content = result.content
        assertThat(content.size).isEqualTo(1)
        assertThat(content.first().id).isEqualTo(courier1.id)
    }

    @Test
    fun `couriersGet - search by uid`() {
        //given
        val employer = employersFactory.createAndSave()
        val courier1 = courierTestFactory.create(employer.id, "uid-1", "email-1")
        courierTestFactory.create(employer.id, "uid-2", "email-2")

        //when
        val response = performCouriersRequest(
            pageNumber = 0,
            pageSize = 50,
            params = mapOf(Pair("uid", courier1.uid))
        )

        //then
        val result = parseResponse(response)

        assertThat(result.totalPages).isEqualTo(1)

        val content = result.content
        assertThat(content.size).isEqualTo(1)
        assertThat(content.first().id).isEqualTo(courier1.id)
    }

    @Test
    fun `couriersGet - search by email`() {
        //given
        val employer = employersFactory.createAndSave()
        val courier1 = courierTestFactory.create(employer.id, "uid-1", "email-1")
        courierTestFactory.create(employer.id, "uid-2", "email-2")

        //when
        val response = performCouriersRequest(
            pageNumber = 0,
            pageSize = 50,
            params = mapOf(Pair("email", courier1.personalData.email))
        )

        //then
        val result = parseResponse(response)

        assertThat(result.pageNumber).isEqualTo(0)
        assertThat(result.pageSize).isEqualTo(50)
        assertThat(result.totalPages).isEqualTo(1)

        val content = result.content
        assertThat(content.size).isEqualTo(1)
        assertThat(content.first().id).isEqualTo(courier1.id)
    }

    @Test
    fun `couriersGet - search by deleted`() {
        //given
        val employer = employersFactory.createAndSave()
        val courier1 = courierTestFactory.create(employerId = employer.id, uid = "uid-1")
        courierTestFactory.create(employerId = employer.id, uid = "uid-2", deleted = true)

        //when
        val response = performCouriersRequest(
            pageNumber = 0,
            pageSize = 50,
            params = mapOf(Pair("deleted", "false"))
        )

        //then
        val result = parseResponse(response)

        assertThat(result.pageNumber).isEqualTo(0)
        assertThat(result.pageSize).isEqualTo(50)
        assertThat(result.totalPages).isEqualTo(1)

        val content = result.content
        assertThat(content.size).isEqualTo(1)
        assertThat(content.first().id).isEqualTo(courier1.id)
    }

    private fun performCouriersRequest(
        pageNumber: Int,
        pageSize: Int,
        params: Map<String, String>
    ) = mockMvc
        .perform(
            MockMvcRequestBuilders.get("/couriers")
                .params(LinkedMultiValueMap(params.mapValues { mutableListOf(it.value) }))
                .param("pageNumber", pageNumber.toString())
                .param("pageSize", pageSize.toString())
                .header(DsmConstants.TVM.TVM_HEADER, "TVM_TICKET")
        )
        .andExpect(MockMvcResultMatchers.status().isOk)
        .andReturn()

    private fun parseResponse(response: MvcResult) =
        TplObjectMappers.TPL_API_OBJECT_MAPPER.readValue(
            response.response.contentAsString,
            CouriersSearchResultDto::class.java
        )
}

package ru.yandex.market.dsm.api

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.web.servlet.MvcResult
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import org.springframework.util.LinkedMultiValueMap
import ru.yandex.market.dsm.config.DsmConstants
import ru.yandex.market.dsm.core.test.AbstractDsmApiTest
import ru.yandex.market.dsm.domain.driver.test.DriverTestFactory
import ru.yandex.market.dsm.domain.employer.EmployersTestFactory
import ru.yandex.market.tpl.common.util.TplObjectMappers
import ru.yandex.mj.generated.server.model.DriverSearchResultDto
import java.time.LocalDate

class DriverApiSearchTest : AbstractDsmApiTest() {

    @Autowired
    private lateinit var employersFactory: EmployersTestFactory

    @Autowired
    private lateinit var driverTestFactory: DriverTestFactory

    @Test
    fun `driversGet - success`() {
        //given
        val employer = employersFactory.createAndSave()
        val courier = driverTestFactory.create(employer.id, "uid")

        //when
        val response = getSearchResponse(
            pageNumber = 0,
            pageSize = 50,
            params = mapOf()
        )

        //then
        Assertions.assertThat(response.pageNumber).isEqualTo(0)
        Assertions.assertThat(response.pageSize).isEqualTo(50)
        Assertions.assertThat(response.totalPages).isEqualTo(1)

        val content = response.content
        Assertions.assertThat(content.size).isEqualTo(1)
        Assertions.assertThat(content.first().id).isEqualTo(courier.id)
    }

    @Test
    fun `driverGet - paging`() {
        val employer = employersFactory.createAndSave()
        driverTestFactory.create(employer.id, "uid-1")
        driverTestFactory.create(employer.id, "uid-2")

        val resultPage0 = getSearchResponse(
            pageNumber = 0,
            pageSize = 1,
            params = mapOf()
        )

        Assertions.assertThat(resultPage0.pageNumber).isEqualTo(0)
        Assertions.assertThat(resultPage0.pageSize).isEqualTo(1)
        Assertions.assertThat(resultPage0.totalPages).isEqualTo(2)
        Assertions.assertThat(resultPage0.content.size).isEqualTo(1)

        val resultPage1 = getSearchResponse(
            pageNumber = 1,
            pageSize = 1,
            params = mapOf()
        )

        Assertions.assertThat(resultPage1.pageNumber).isEqualTo(1)
        Assertions.assertThat(resultPage1.pageSize).isEqualTo(1)
        Assertions.assertThat(resultPage1.totalPages).isEqualTo(2)
    }

    @Test
    fun `driverGet - search by employer`() {
        //given
        val employer1 = employersFactory.createAndSave()
        val employer2 = employersFactory.createAndSave()
        val driver1 = driverTestFactory.create(employer1.id, "uid-1")
        driverTestFactory.create(employer2.id, "uid-2")

        //when
        val result = getSearchResponse(
            pageNumber = 0,
            pageSize = 50,
            params = mapOf(Pair("employerId", employer1.id))
        )

        //then
        Assertions.assertThat(result.pageNumber).isEqualTo(0)
        Assertions.assertThat(result.pageSize).isEqualTo(50)
        Assertions.assertThat(result.totalPages).isEqualTo(1)

        val content = result.content
        Assertions.assertThat(content.size).isEqualTo(1)
        Assertions.assertThat(content.first().id).isEqualTo(driver1.id)
    }

    @Test
    fun `driverGet - search by single employer - driver has single employer`() {
        //given
        val employer1 = employersFactory.createAndSave()
        val employer2 = employersFactory.createAndSave()
        val driver1 = driverTestFactory.create(employer1.id, "uid-1")
        driverTestFactory.create(employer2.id, "uid-2")

        //when
        val result = getSearchResponse(
            pageNumber = 0,
            pageSize = 50,
            params = mapOf(Pair("employerIds", employer1.id))
        )

        //then
        Assertions.assertThat(result.pageNumber).isEqualTo(0)
        Assertions.assertThat(result.pageSize).isEqualTo(50)
        Assertions.assertThat(result.totalPages).isEqualTo(1)

        val content = result.content
        Assertions.assertThat(content.size).isEqualTo(1)
        Assertions.assertThat(content.first().id).isEqualTo(driver1.id)
    }

    @Test
    fun `driverGet - search by several employers - each driver has single employer`() {
        //given
        val employer1 = employersFactory.createAndSave()
        val employer2 = employersFactory.createAndSave()
        val driver1 = driverTestFactory.create(employer1.id, "uid-1")
        val driver2 = driverTestFactory.create(employer2.id, "uid-2")

        //when
        val result = getSearchResponse(
            pageNumber = 0,
            pageSize = 50,
            params = mapOf(Pair("employerIds", employer1.id + "," + employer2.id))
        )

        //then
        Assertions.assertThat(result.pageNumber).isEqualTo(0)
        Assertions.assertThat(result.pageSize).isEqualTo(50)
        Assertions.assertThat(result.totalPages).isEqualTo(1)

        val content = result.content
        Assertions.assertThat(content.size).isEqualTo(2)
        Assertions.assertThat(content.map { it.id }.toList()).containsExactlyInAnyOrder(driver1.id, driver2.id)
    }

    @Test
    fun `driverGet - search by single employer - driver has several employers`() {
        //given
        val employer1 = employersFactory.createAndSave()
        val employer2 = employersFactory.createAndSave()

        val driver1 = driverTestFactory.create(employer1.id, "uid-1")
        driverTestFactory.addLinkToEmployer(driver1.id, employer2.id)

        val employer3 = employersFactory.createAndSave()
        driverTestFactory.create(employer3.id, "uid-2")

        //when
        val result = getSearchResponse(
            pageNumber = 0,
            pageSize = 50,
            params = mapOf(Pair("employerIds", employer1.id))
        )

        //then
        Assertions.assertThat(result.pageNumber).isEqualTo(0)
        Assertions.assertThat(result.pageSize).isEqualTo(50)
        Assertions.assertThat(result.totalPages).isEqualTo(1)

        val content = result.content
        Assertions.assertThat(content.size).isEqualTo(1)
        Assertions.assertThat(content.first().id).isEqualTo(driver1.id)
    }


    @Test
    fun `driverGet - search by several employers - driver has several employers`() {
        //given
        val employer1 = employersFactory.createAndSave()
        val employer2 = employersFactory.createAndSave()
        val employer3 = employersFactory.createAndSave()

        val driver1 = driverTestFactory.create(employer1.id, "uid-1")
        driverTestFactory.addLinkToEmployer(driver1.id, employer2.id)

        val driver2 = driverTestFactory.create(employer2.id, "uid-2")

        driverTestFactory.create(employer3.id, "uid-3")


        //when
        val result = getSearchResponse(
            pageNumber = 0,
            pageSize = 50,
            params = mapOf(Pair("employerIds", employer1.id + "," + employer2.id))
        )

        //then
        Assertions.assertThat(result.pageNumber).isEqualTo(0)
        Assertions.assertThat(result.pageSize).isEqualTo(50)
        Assertions.assertThat(result.totalPages).isEqualTo(1)

        val content = result.content
        Assertions.assertThat(content.size).isEqualTo(2)
        Assertions.assertThat(content.map { it.id }.toList()).containsExactlyInAnyOrder(driver1.id, driver2.id)
    }


    @Test
    fun `driverGet - search by several employers - each driver has several employers`() {
        //given
        val employer1 = employersFactory.createAndSave()
        val employer2 = employersFactory.createAndSave()
        val employer3 = employersFactory.createAndSave()

        val driver1 = driverTestFactory.create(employer1.id, "uid-1")
        driverTestFactory.addLinkToEmployer(driver1.id, employer2.id)

        val driver2 = driverTestFactory.create(employer1.id, "uid-2")
        driverTestFactory.addLinkToEmployer(driver2.id, employer2.id)

        driverTestFactory.create(employer3.id, "uid-3")


        //when
        val result = getSearchResponse(
            pageNumber = 0,
            pageSize = 50,
            params = mapOf(Pair("employerIds", employer1.id + "," + employer2.id))
        )

        //then
        Assertions.assertThat(result.pageNumber).isEqualTo(0)
        Assertions.assertThat(result.pageSize).isEqualTo(50)
        Assertions.assertThat(result.totalPages).isEqualTo(1)

        val content = result.content
        Assertions.assertThat(content.size).isEqualTo(2)
        Assertions.assertThat(content.map { it.id }.toList()).containsExactlyInAnyOrder(driver1.id, driver2.id)
    }

    @Test
    fun `driverGet - search by multiple id`() {
        //given
        val employer = employersFactory.createAndSave()
        val driver1 = driverTestFactory.create(employer.id, "uid-1", "email-1")
        val driver2 = driverTestFactory.create(employer.id, "uid-2", "email-2")
        driverTestFactory.create(employer.id, "uid-3", "email-3")

        //when
        val result = getSearchResponse(
            pageNumber = 0,
            pageSize = 50,
            params = mapOf(Pair("id", driver1.id + "," + driver2.id))
        )

        //then
        Assertions.assertThat(result.totalPages).isEqualTo(1)

        val content = result.content
        Assertions.assertThat(content.size).isEqualTo(2)
        Assertions.assertThat(result.content.map {it.id}.toList()).containsExactlyInAnyOrder(driver1.id, driver2.id)
    }

    @Test
    fun `driverGet - search by multiple uid`() {
        //given
        val employer = employersFactory.createAndSave()
        val driver1 = driverTestFactory.create(employer.id, "uid-1", "email-1")
        val driver2 = driverTestFactory.create(employer.id, "uid-2", "email-2")
        driverTestFactory.create(employer.id, "uid-3", "email-3")

        //when
        val result = getSearchResponse(
            pageNumber = 0,
            pageSize = 50,
            params = mapOf(Pair("uid", driver1.uid + "," + driver2.uid))
        )

        //then
        Assertions.assertThat(result.totalPages).isEqualTo(1)

        val content = result.content
        Assertions.assertThat(content.size).isEqualTo(2)
        Assertions.assertThat(result.content.map {it.id}.toList()).containsExactlyInAnyOrder(driver1.id, driver2.id)
    }

    @Test
    fun `driverGet - search by name part`() {
        // given
        val employer = employersFactory.createAndSave()
        val driver1 = driverTestFactory.create(
            employerId = employer.id,
            uid = "uid-1",
            lastName = "Петров",
            firstName = "Иван"
        )
        driverTestFactory.create(
            employerId = employer.id,
            uid = "uid-2",
            lastName = "Лебедев",
            firstName = "Артемий"
        )

        //when
        val result = getSearchResponse(
            pageNumber = 0,
            pageSize = 50,
            params = mapOf(Pair("namePart", "ЕТРОВ"))
        )

        //then
        Assertions.assertThat(result.totalPages).isEqualTo(1)

        val content = result.content
        Assertions.assertThat(content.size).isEqualTo(1)
        Assertions.assertThat(result.content.first().id).isEqualTo(driver1.id)
    }

    @Test
    fun `driverGet - search by phone part`() {
        // given
        val employer = employersFactory.createAndSave()
        val driver1 = driverTestFactory.create(
            employerId = employer.id,
            uid = "uid-1",
            phone = "+79998881234"
        )
        driverTestFactory.create(
            employerId = employer.id,
            uid = "uid-2",
            lastName = "Лебедев",
            phone = "+79998882345"
        )

        //when
        val result = getSearchResponse(
            pageNumber = 0,
            pageSize = 50,
            params = mapOf(Pair("phonePart", "8123"))
        )

        //then
        Assertions.assertThat(result.totalPages).isEqualTo(1)

        val content = result.content
        Assertions.assertThat(content.size).isEqualTo(1)
        Assertions.assertThat(result.content.first().id).isEqualTo(driver1.id)
    }

    @Test
    fun `driverGet - search by birthday`() {
        // given
        val employer = employersFactory.createAndSave()
        val driver1 = driverTestFactory.create(
            employerId = employer.id,
            uid = "uid-1",
            passportNumber = "1234567891",
            birthday = LocalDate.of(2000, 1, 1)
        )
        driverTestFactory.create(
            employerId = employer.id,
            uid = "uid-2",
            passportNumber = "1234567892",
            birthday = LocalDate.of(1999, 1, 1)
        )

        //when
        val result = getSearchResponse(
            pageNumber = 0,
            pageSize = 50,
            params = mapOf(Pair("birthday", "2000-01-01"))
        )

        //then
        Assertions.assertThat(result.totalPages).isEqualTo(1)

        val content = result.content
        Assertions.assertThat(content.size).isEqualTo(1)
        Assertions.assertThat(result.content.first().id).isEqualTo(driver1.id)
    }

    @Test
    fun `driverGet - search by blacklisted`() {
        // given
        val employer = employersFactory.createAndSave()
        val driver1 = driverTestFactory.create(employerId = employer.id, uid = "uid-1")
        val driver2 = driverTestFactory.create(employerId = employer.id, uid = "uid-2")

        driverTestFactory.addToBlackList(driver1.id)

        //when
        val result1 = getSearchResponse(
            pageNumber = 0,
            pageSize = 50,
            params = mapOf(Pair("blackListed", "true"))
        )

        //then
        Assertions.assertThat(result1.totalPages).isEqualTo(1)

        val content1 = result1.content
        Assertions.assertThat(content1.size).isEqualTo(1)
        Assertions.assertThat(result1.content.first().id).isEqualTo(driver1.id)

        //when
        val result2 = getSearchResponse(
            pageNumber = 0,
            pageSize = 50,
            params = mapOf(Pair("blackListed", "false"))
        )

        //then
        Assertions.assertThat(result2.totalPages).isEqualTo(1)

        val content2 = result2.content
        Assertions.assertThat(content2.size).isEqualTo(1)
        Assertions.assertThat(result2.content.first().id).isEqualTo(driver2.id)
    }

    @Test
    fun `driverGet - sort`() {
        val employer = employersFactory.createAndSave()
        val driver1 = driverTestFactory.create(
            employerId = employer.id,
            uid = "uid-1",
            lastName = "1"
        )
        val driver2 = driverTestFactory.create(
            employerId = employer.id,
            uid = "uid-2",
            lastName = "2"
        )
        val driver3 = driverTestFactory.create(
            employerId = employer.id,
            uid = "uid-3",
            lastName = "3"
        )

        val resultPage1 = getSearchResponse(
            pageNumber = 0,
            pageSize = 50,
            params = mapOf(Pair("sort", "name"))
        )

        Assertions.assertThat(resultPage1.content.map { it.uid }.toList())
            .containsExactly(driver1.uid, driver2.uid, driver3.uid)


        val resultPage2 = getSearchResponse(
            pageNumber = 0,
            pageSize = 50,
            params = mapOf(Pair("sort", "name,asc"))
        )

        Assertions.assertThat(resultPage2.content.map { it.uid }.toList())
            .containsExactly(driver1.uid, driver2.uid, driver3.uid)


        val resultPage3 = getSearchResponse(
            pageNumber = 0,
            pageSize = 50,
            params = mapOf(Pair("sort", "name,desc"))
        )


        Assertions.assertThat(resultPage3.content.map { it.uid }.toList())
            .containsExactly(driver3.uid, driver2.uid, driver1.uid)
    }

    @Test
    fun `driverGet - sort with employerId filter`() {
        val employer = employersFactory.createAndSave()
        val driver1 = driverTestFactory.create(
            employerId = employer.id,
            uid = "uid-1",
            lastName = "1"
        )
        val driver2 = driverTestFactory.create(
            employerId = employer.id,
            uid = "uid-2",
            lastName = "2"
        )

        val resultPage1 = getSearchResponse(
            pageNumber = 0,
            pageSize = 50,
            params = mapOf(
                Pair("sort", "name,desc"),
                Pair("employerId", employer.id)
            )
        )

        Assertions.assertThat(resultPage1.content.map { it.uid }.toList())
            .containsExactly(driver2.uid, driver1.uid)
    }

    @Test
    fun `driverGet - sort with employer filter and several other filters at the same time`() {
        val employer = employersFactory.createAndSave()
        val driver1 = driverTestFactory.create(
            employerId = employer.id,
            uid = "uid-1",
            lastName = "1",
            birthday = LocalDate.of(1999, 1, 1)
        )
        val driver2 = driverTestFactory.create(
            employerId = employer.id,
            uid = "uid-2",
            lastName = "2",
            birthday = LocalDate.of(1999, 1, 1)
        )

        val resultPage1 = getSearchResponse(
            pageNumber = 0,
            pageSize = 50,
            params = mapOf(
                Pair("sort", "name,desc"),
                Pair("employerId", employer.id),
                Pair("blackListed", "false"),
                Pair("birthday", "1999-01-01")
            )
        )

        Assertions.assertThat(resultPage1.content.map { it.uid }.toList())
            .containsExactly(driver2.uid, driver1.uid)
    }

    private fun getSearchResponse(
        pageNumber: Int,
        pageSize: Int,
        params: Map<String, String>
    ): DriverSearchResultDto {
        val response = mockMvc
            .perform(
                MockMvcRequestBuilders.get("/drivers")
                    .params(LinkedMultiValueMap(params.mapValues { mutableListOf(it.value) }))
                    .param("pageNumber", pageNumber.toString())
                    .param("pageSize", pageSize.toString())
                    .header(DsmConstants.TVM.TVM_HEADER, "TVM_TICKET")
            )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andReturn()
        return parseResponse(response)
    }

    private fun parseResponse(response: MvcResult) =
        TplObjectMappers.TPL_API_OBJECT_MAPPER.readValue(
            response.response.contentAsString,
            DriverSearchResultDto::class.java
        )
}

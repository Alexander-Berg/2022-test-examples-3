package ru.yandex.market.dsm.api

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.test.context.jdbc.Sql
import org.springframework.test.web.servlet.ResultMatcher
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import ru.yandex.market.dsm.config.DsmConstants.TVM.Companion.TVM_HEADER
import ru.yandex.market.dsm.core.test.AbstractDsmApiTest
import ru.yandex.market.dsm.domain.employer.EmployersTestFactory
import ru.yandex.market.dsm.domain.employer.mapper.EmployerMapper
import ru.yandex.market.dsm.domain.employer.service.EmployerQueryService
import ru.yandex.market.dsm.test.AssertsEmployerFactory
import ru.yandex.market.dsm.test.TestUtil
import ru.yandex.market.tpl.common.util.TplObjectMappers
import ru.yandex.mj.generated.server.model.EmployerDto
import ru.yandex.mj.generated.server.model.EmployerUpsertDto
import ru.yandex.mj.generated.server.model.EmployersSearchResultDto
import java.util.UUID
import java.util.stream.IntStream

class EmployerApiMvcTest : AbstractDsmApiTest() {
    @Autowired
    private lateinit var employerQueryService: EmployerQueryService
    @Autowired
    private lateinit var employersFactory: EmployersTestFactory
    @Autowired
    private lateinit var employerMapper: EmployerMapper

    @Test
    fun `employersIdGet - success`() {
        //given
        val employer = employersFactory.createAndSave()

        //when
        val response = mockMvc
            .perform(
                MockMvcRequestBuilders.get("/employers/${employer.id}")
                    .header(TVM_HEADER, "TVM_TICKET")
            )
            .andExpect(status().isOk)
            .andReturn()

        //then
        val result = TplObjectMappers.TPL_API_OBJECT_MAPPER.readValue(
            response.response.contentAsString,
            EmployerDto::class.java
        )
        AssertsEmployerFactory.asserts(result, employer)
    }

    @Test
    fun `employersIdGet - not found`() {
        mockMvc
            .perform(
                MockMvcRequestBuilders.get("/employers/not-exists")
                    .header(TVM_HEADER, "TVM_TICKET")
            )
            .andExpect(status().isNotFound)
    }

    @Test
    fun `employersPut - create`() {
        //given
        val createDto = TestUtil.OBJECT_GENERATOR.nextObject(EmployerUpsertDto::class.java)
        createDto.id = null

        //when
        val response = performPutRequest(createDto, status().isCreated)

        //then
        assertThat(response.response.contentAsString).isNotBlank
        val createdEmployer = TplObjectMappers.TPL_API_OBJECT_MAPPER.readValue(
            response.response.contentAsString,
            EmployerDto::class.java
        )
        AssertsEmployerFactory.asserts(createdEmployer, createDto)
    }

    @Test
    fun `employersPut - conflict on create`() {
        //given
        val employer = employersFactory.createAndSave()
        val createDto = TestUtil.OBJECT_GENERATOR.nextObject(EmployerUpsertDto::class.java)
        createDto.id = null
        createDto.login = employer.login

        //when
        val response = performPutRequest(createDto, status().isOk)

        //then
        assertThat(response.response.contentAsString).isNotBlank
        val resultDto = TplObjectMappers.TPL_API_OBJECT_MAPPER.readValue(
            response.response.contentAsString,
            EmployerDto::class.java
        )
        AssertsEmployerFactory.asserts(resultDto, employer)
    }

    @Test
    fun `employersPut - update`() {
        //given
        val createdEmployer = employersFactory.createAndSave()

        val updateDto = TestUtil.OBJECT_GENERATOR.nextObject(EmployerUpsertDto::class.java)
        updateDto.id = createdEmployer.id

        //when
        val response = performPutRequest(updateDto, status().isOk)

        //then
        assertThat(response.response.contentAsString).isNotBlank
        val updatedEmployer = TplObjectMappers.TPL_API_OBJECT_MAPPER.readValue(
            response.response.contentAsString,
            EmployerDto::class.java
        )
        AssertsEmployerFactory.asserts(updatedEmployer, updateDto)
    }

    @Sql("classpath:truncate.sql")
    @Test
    fun `search all Employer`() {
        //given
        IntStream.range(1, 12).forEach { employersFactory.createAndSave() }

        //when
        val response = mockMvc.perform(
            MockMvcRequestBuilders.get("/employers")
                .param("pageNumber", "0")
                .param("pageSize", "10")
                .header(TVM_HEADER, "TVM_TICKET")
        )
            .andExpect(status().is2xxSuccessful)
            .andReturn()


        //then
        assertThat(response.response.contentAsString).isNotBlank
        val searchResult = TplObjectMappers.TPL_API_OBJECT_MAPPER.readValue(
            response.response.contentAsString,
            EmployersSearchResultDto::class.java
        )

        assertThat(searchResult.totalPages).isEqualTo(2)
        assertThat(searchResult.content).hasSize(10)
    }

    @Test
    fun `search Employer - empty result`() {
        //given
        IntStream.range(1, 9).forEach { employersFactory.createAndSave() }

        //when
        val response = mockMvc.perform(
            MockMvcRequestBuilders.get("/employers")
                .param("pageNumber", "0")
                .param("pageSize", "10")
                .param("name", UUID.randomUUID().toString())
                .header(TVM_HEADER, "TVM_TICKET")
        )
            .andExpect(status().is2xxSuccessful)
            .andReturn()


        //then
        assertThat(response.response.contentAsString).isNotBlank
        val searchResult = TplObjectMappers.TPL_API_OBJECT_MAPPER.readValue(
            response.response.contentAsString,
            EmployersSearchResultDto::class.java
        )

        assertThat(searchResult.totalPages).isEqualTo(0)
        assertThat(searchResult.content).hasSize(0)
    }

    @Test
    fun `search Employer - by name`() {
        //given
        val createdEmployer = employersFactory.createAndSave()
        IntStream.range(1, 9).forEach { employersFactory.createAndSave() }

        //when
        val response = mockMvc.perform(
            MockMvcRequestBuilders.get("/employers")
                .param("pageNumber", "0")
                .param("pageSize", "10")
                .param("name", createdEmployer.name)
                .header(TVM_HEADER, "TVM_TICKET")
        )
            .andExpect(status().is2xxSuccessful)
            .andReturn()


        //then
        assertThat(response.response.contentAsString).isNotBlank
        val searchResult = TplObjectMappers.TPL_API_OBJECT_MAPPER.readValue(
            response.response.contentAsString,
            EmployersSearchResultDto::class.java
        )

        assertThat(searchResult.totalPages).isEqualTo(1)
        assertThat(searchResult.content).hasSize(1)
        assertThat(searchResult.content).containsExactly(employerMapper.mapToDto(createdEmployer))
    }

    private fun performPutRequest(
        upsertDto: EmployerUpsertDto, resultStatusMatcher: ResultMatcher
    ) = mockMvc.perform(
        MockMvcRequestBuilders.put("/employers")
            .header(TVM_HEADER, "TVM_TICKET")
            .contentType(MediaType.APPLICATION_JSON_UTF8)
            .content(TplObjectMappers.TPL_API_OBJECT_MAPPER.writeValueAsString(upsertDto))
    )
        .andExpect(resultStatusMatcher)
        .andReturn()
}

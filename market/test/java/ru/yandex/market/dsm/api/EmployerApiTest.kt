package ru.yandex.market.dsm.api

import com.fasterxml.jackson.databind.ObjectMapper
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.AssertionsForClassTypes
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.jdbc.Sql
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import org.springframework.transaction.support.TransactionTemplate
import org.springframework.validation.FieldError
import ru.yandex.market.dsm.config.DsmConstants
import ru.yandex.market.dsm.core.ddd.DsmCommandService
import ru.yandex.market.dsm.core.exception.BadRequestException
import ru.yandex.market.dsm.core.test.AbstractDsmApiTest
import ru.yandex.market.dsm.dbqueue.DbQueueTestUtil
import ru.yandex.market.dsm.dbqueue.model.DsmDbQueue
import ru.yandex.market.dsm.domain.employer.EmployersTestFactory
import ru.yandex.market.dsm.domain.employer.db.EmployerDboRepository
import ru.yandex.market.dsm.domain.employer.mapper.EmployerMapper
import ru.yandex.market.dsm.domain.employer.model.EmployerBalanceRegistrationStatus
import ru.yandex.market.dsm.domain.employer.service.EmployerQueryService
import ru.yandex.market.dsm.test.AssertsEmployerFactory
import ru.yandex.market.dsm.test.TestUtil
import ru.yandex.mj.generated.server.model.EmployerUpsertDto
import ru.yandex.mj.generated.server.model.EmployersSearchResultDto
import java.util.UUID
import java.util.stream.IntStream

class EmployerApiTest : AbstractDsmApiTest() {
    @Autowired
    private lateinit var employerApi: EmployerApi
    @Autowired
    private lateinit var employersTestFactory: EmployersTestFactory
    @Autowired
    private lateinit var employerMapper: EmployerMapper
    @Autowired
    private lateinit var dsmCommandService: DsmCommandService
    @Autowired
    private lateinit var dbQueueTestUtil: DbQueueTestUtil
    @Autowired
    private lateinit var employerDboRepository: EmployerDboRepository
    @Autowired
    private lateinit var transactionTemplate: TransactionTemplate
    @Autowired
    private lateinit var employerQueryService: EmployerQueryService
    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Test
    fun `createEmployer`() {
        //given
        val upsertDto = TestUtil.OBJECT_GENERATOR.nextObject(EmployerUpsertDto::class.java)
        upsertDto.id = null

        //when
        val putResponse = employerApi.employersPut(upsertDto)

        //then
        assertThat(putResponse.body).isNotNull
        AssertsEmployerFactory.asserts(putResponse.body!!, upsertDto)
    }

    @Sql("classpath:truncate.sql")
    @Test
    fun `search all employers`() {
        //given
        IntStream.range(0, 20).forEach { i -> employersTestFactory.createAndSave() }

        //when
        val searchResponse = employerApi.employersGet(
            pageNumber = 0,
            pageSize = 10,
            ids = null,
            name = null,
            nameSubstring = null,
            login = null,
            cabinetMbiId = null,
            type = null,
            active = null
        )

        //then
        assertThat(searchResponse.body).isNotNull
        assertThat(searchResponse.body!!.totalPages).isEqualTo(2)
        assertThat(searchResponse.body!!.content).hasSize(10)
    }

    @Sql("classpath:truncate.sql")
    @Test
    fun `search all employers via request`() {
        //given
        IntStream.range(0, 20).forEach { i -> employersTestFactory.createAndSave() }

        //when
        val searchResult = mockMvc.perform(
            MockMvcRequestBuilders.get("/employers?pageNumber=0&pageSize=10")
                .header(DsmConstants.TVM.TVM_HEADER, "TVM_TICKET")
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andReturn().response.contentAsString

        val searchResultDto = objectMapper.readValue(searchResult, EmployersSearchResultDto::class.java)

        //then
        assertThat(searchResultDto).isNotNull
        assertThat(searchResultDto.totalPages).isEqualTo(2)
        assertThat(searchResultDto.content).hasSize(10)
    }

    @Test
    fun `search employers - empty result`() {
        //given
        IntStream.range(0, 20).forEach { i -> employersTestFactory.createAndSave() }

        //when
        val searchResponse = employerApi.employersGet(
            pageNumber = 0,
            pageSize = 10,
            ids = null,
            name = UUID.randomUUID().toString(),
            nameSubstring = null,
            login = null,
            cabinetMbiId = null,
            type = null,
            active = null
        )

        //then
        assertThat(searchResponse.body).isNotNull
        assertThat(searchResponse.body!!.totalPages).isEqualTo(0)
        assertThat(searchResponse.body!!.content).hasSize(0)
    }

    @Test
    fun `search employers by name`() {
        //given
        val createdEmployer = employersTestFactory.createAndSave()
        IntStream.range(0, 10).forEach { i -> employersTestFactory.createAndSave() }

        //when
        val searchResponse = employerApi.employersGet(
            pageNumber = 0,
            pageSize = 10,
            ids = null,
            name = createdEmployer.name,
            nameSubstring = null,
            login = null,
            cabinetMbiId = null,
            type = null,
            active = null
        )

        //then
        assertThat(searchResponse.body).isNotNull
        assertThat(searchResponse.body!!.totalPages).isEqualTo(1)
        assertThat(searchResponse.body!!.content).hasSize(1)
        assertThat(searchResponse.body!!.content).containsExactly(employerMapper.mapToDto(createdEmployer))
    }

    @Test
    fun `createEmployer_idempotence`() {
        //given
        val upsertDto = TestUtil.OBJECT_GENERATOR.nextObject(EmployerUpsertDto::class.java)
        upsertDto.id = null

        //when
        val putResponse1 = employerApi.employersPut(upsertDto)
        val id = upsertDto.id
        upsertDto.id = null
        val putResponse2 = employerApi.employersPut(upsertDto)

        //then
        assertThat(putResponse1.body).isNotNull
        assertThat(putResponse1.body).isEqualTo(putResponse2.body)
        upsertDto.id = id
        AssertsEmployerFactory.asserts(putResponse1.body!!, upsertDto)
    }

    @Test
    fun `create employer - bad request`() {
        //given
        val upsertDto = TestUtil.OBJECT_GENERATOR.nextObject(EmployerUpsertDto::class.java)
        upsertDto.id = null
        upsertDto.type = null

        //when
        val ex = assertThrows<BadRequestException> {
            employerApi.employersPut(upsertDto)
        }

        //then
        assertThat(ex.message).isNotNull
        assertThat(ex.errors).isNotNull
        assertThat(ex.errors!!.allErrors.map { it as FieldError }.count { it.field == "type" }).isEqualTo(1)
    }

    @Test
    fun `update employer - bad request`() {
        //given
        val createDto = TestUtil.OBJECT_GENERATOR.nextObject(EmployerUpsertDto::class.java)
        createDto.id = null
        val createdDto = employerApi.employersPut(createDto)

        val upsertDto = TestUtil.OBJECT_GENERATOR.nextObject(EmployerUpsertDto::class.java)
        upsertDto.id = createdDto.body!!.id
        upsertDto.type = null

        //when
        val ex = assertThrows<BadRequestException> {
            employerApi.employersPut(upsertDto)
        }

        //then
        assertThat(ex.message).isNotNull
        assertThat(ex.errors).isNotNull
        assertThat(ex.errors!!.allErrors.map { it as FieldError }.count { it.field == "type" }).isEqualTo(1)
    }

    @Test
    fun `updateEmployer`() {
        //given
        val upsertDto = TestUtil.OBJECT_GENERATOR.nextObject(EmployerUpsertDto::class.java)
        upsertDto.id = null

        //when
        val createResponse = employerApi.employersPut(upsertDto)
        assertThat(createResponse.body!!.id).isNotNull

        val updatedDto = TestUtil.OBJECT_GENERATOR.nextObject(EmployerUpsertDto::class.java)
        updatedDto.id(createResponse.body!!.id)
        val updateResponse = employerApi.employersPut(updatedDto)

        //then
        AssertsEmployerFactory.asserts(updateResponse.body!!, updatedDto)
    }

    @Test
    fun  manualEmployersIdEmployerRegistrationInBalancePut() {
        val employerCommand = employersTestFactory.createCreateCommand()
        employerCommand.balanceRegistrationStatus = null
        dsmCommandService.handle(employerCommand)

        mockMvc.perform(
            MockMvcRequestBuilders.put("/manual/employers/${employerCommand.id}/employer-registration-in-balance")
                .header(DsmConstants.TVM.TVM_HEADER, "TVM_TICKET")
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andReturn()
        dbQueueTestUtil.executeAllQueueItems(DsmDbQueue.COMPANY_REGISTRATION_IN_BALANCE)
        val employer = employerQueryService.getById(employerCommand.id)
        AssertionsForClassTypes.assertThat(employer.balanceRegistrationStatus)
            .isEqualTo(EmployerBalanceRegistrationStatus.REGISTERED)

        //Регистрация завершена, получаем ошибку
        mockMvc.perform(
            MockMvcRequestBuilders.put("/manual/employers/${employerCommand.id}/employer-registration-in-balance")
                .header(DsmConstants.TVM.TVM_HEADER, "TVM_TICKET")
        )
            .andExpect(MockMvcResultMatchers.status().is4xxClientError)
            .andReturn()

        //Необходимое поле null, получаем ошибку
        transactionTemplate.execute {
            val employer2 = employerDboRepository.getOne(employerCommand.id)
            employer2.balanceRegistrationStatus = null
            employer2.account = null
            employerDboRepository.save(employer2)
        }
        mockMvc.perform(
            MockMvcRequestBuilders.put("/manual/employers/${employerCommand.id}/employer-registration-in-balance")
                .header(DsmConstants.TVM.TVM_HEADER, "TVM_TICKET")
        )
            .andExpect(MockMvcResultMatchers.status().is4xxClientError)
            .andReturn()

        dbQueueTestUtil.clear(DsmDbQueue.SELF_EMPLOYED_REGISTRATION_IN_BALANCE)
    }
}

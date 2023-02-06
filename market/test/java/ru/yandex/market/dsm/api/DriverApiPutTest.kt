package ru.yandex.market.dsm.api

import one.util.streamex.StreamEx
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.ResultMatcher
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import ru.yandex.market.dsm.config.DsmConstants
import ru.yandex.market.dsm.core.test.AbstractDsmApiTest
import ru.yandex.market.dsm.domain.driver.service.DriverQueryService
import ru.yandex.market.dsm.domain.driver.test.AssertDriverFactory
import ru.yandex.market.dsm.domain.driver.test.DriverTestFactory
import ru.yandex.market.dsm.domain.employer.EmployersTestFactory
import ru.yandex.market.dsm.domain.nationality.db.NationalityDbo
import ru.yandex.market.dsm.domain.nationality.db.NationalityDboRepository
import ru.yandex.market.tpl.common.util.TplObjectMappers
import ru.yandex.mj.generated.server.model.DriverDto
import ru.yandex.mj.generated.server.model.DriverUpsertDto
import ru.yandex.mj.generated.server.model.PersonalDataUpsertDto

class DriverApiPutTest : AbstractDsmApiTest() {

    @Autowired
    private lateinit var employersFactory: EmployersTestFactory

    @Autowired
    private lateinit var driverTestFactory: DriverTestFactory

    @Autowired
    private lateinit var driverQueryService: DriverQueryService

    @Autowired
    private lateinit var nationalityDboRepository: NationalityDboRepository

    @BeforeEach
    fun init() {
        nationalityDboRepository.save(
            NationalityDbo(
                "10283759",
                "RUS"
            )
        )
    }

    @Test
    fun `driverPut - create`() {
        //given
        val employer = employersFactory.createAndSave()
        val upsertDto = DriverTestFactory.getValidDriverUpsertDto()
        upsertDto.id = null
        upsertDto.employerId = employer.id

        //when
        val response = performPutRequest(upsertDto, MockMvcResultMatchers.status().isCreated)

        //then
        Assertions.assertThat(response.response.contentAsString).isNotBlank

        val result = TplObjectMappers.TPL_API_OBJECT_MAPPER.readValue(
            response.response.contentAsString,
            DriverDto::class.java
        )
        assertsEquals(result, upsertDto)
    }

    @Test
    fun `driverPut - create driver without Employer`() {
        //given
        val upsertDto = DriverTestFactory.getValidDriverUpsertDto()
        upsertDto.id = null
        upsertDto.employerId = null

        //when
        val response = performPutRequest(upsertDto, MockMvcResultMatchers.status().isCreated)

        //then
        Assertions.assertThat(response.response.contentAsString).isNotBlank

        val result = TplObjectMappers.TPL_API_OBJECT_MAPPER.readValue(
            response.response.contentAsString,
            DriverDto::class.java
        )
        assertsEquals(result, upsertDto)
    }

    @Test
    fun `driverPut - create with same passport`() {
        val employer = employersFactory.createAndSave()
        val upsertDto1 = DriverTestFactory.getValidDriverUpsertDto()
        upsertDto1.id = null
        upsertDto1.employerId = employer.id
        val upsertDto2 = DriverTestFactory.getValidDriverUpsertDto()
        upsertDto2.id = null
        upsertDto2.uid = upsertDto2.uid + "_2"
        upsertDto2.employerId = employer.id

        val response1 = performPutRequest(upsertDto1, MockMvcResultMatchers.status().isCreated)
        val response2 = performPutRequest(upsertDto2, MockMvcResultMatchers.status().isCreated)

        val result1 = TplObjectMappers.TPL_API_OBJECT_MAPPER.readValue(
            response1.response.contentAsString,
            DriverDto::class.java
        )
        val result2 = TplObjectMappers.TPL_API_OBJECT_MAPPER.readValue(
            response2.response.contentAsString,
            DriverDto::class.java
        )

        Assertions.assertThat(
            driverQueryService.findByIdOrThrow(result1.id).personalData.passportData.id
        ).isEqualTo(
            driverQueryService.findByIdOrThrow(result2.id).personalData.passportData.id
        )
    }

    @Test
    fun `driverPut - create with only required attrs`() {
        //given
        val employer = employersFactory.createAndSave()
        val upsertDto = DriverUpsertDto().apply {
            this.uid = "uid"
            this.employerId = employer.id
            this.personalData = PersonalDataUpsertDto().apply {
                this.email = "email"
                this.lastName = "lastName"
                this.firstName = "firstName"
            }
        }

        //when
        val response = performPutRequest(upsertDto, MockMvcResultMatchers.status().isCreated)

        //then
        Assertions.assertThat(response.response.contentAsString).isNotBlank

        val result = TplObjectMappers.TPL_API_OBJECT_MAPPER.readValue(
            response.response.contentAsString,
            DriverDto::class.java
        )
        assertsEquals(result, upsertDto)
    }

    @Test
    fun `driverPut - conflict on create`() {
        //given
        val employer = employersFactory.createAndSave()
        val driver = driverTestFactory.create(employerId = employer.id, uid = "uid-1")
        val upsertDto = DriverTestFactory.getValidDriverUpsertDto()
        upsertDto.id = null
        upsertDto.uid = driver.uid
        upsertDto.employerId = employer.id

        //when
        val response = performPutRequest(upsertDto, MockMvcResultMatchers.status().isOk)

        //then
        val result = TplObjectMappers.TPL_API_OBJECT_MAPPER.readValue(
            response.response.contentAsString,
            DriverDto::class.java
        )
        AssertDriverFactory.assertEquals(result, driver)
    }

    @Test
    fun `driverPut - update`() {
        //given
        val employer = employersFactory.createAndSave()
        val courier = driverTestFactory.create(employerId = employer.id, uid = "uid-1")
        val upsertDto = DriverTestFactory.getValidDriverUpsertDto()
        upsertDto.id = courier.id
        upsertDto.employerId = employer.id

        //when
        val response = performPutRequest(upsertDto, MockMvcResultMatchers.status().isOk)

        //then
        Assertions.assertThat(response.response.contentAsString).isNotBlank

        val result = TplObjectMappers.TPL_API_OBJECT_MAPPER.readValue(
            response.response.contentAsString,
            DriverDto::class.java
        )
        Assertions.assertThat(result.id).isEqualTo(courier.id)
        assertsEquals(result, upsertDto)
    }

    @Test
    fun `driverPut - employer doesnt exist`() {
        //given
        val upsertDto = DriverTestFactory.getValidDriverUpsertDto()
        upsertDto.id = null
        upsertDto.employerId = "not-exists"

        //when
        performPutRequest(upsertDto, MockMvcResultMatchers.status().isBadRequest)
    }

    @ParameterizedTest
    @MethodSource("driverPutInvalidParamsSource")
    fun `driverPut - validation fail`(upsertDto: DriverUpsertDto) {
        //given
        val employer = employersFactory.createAndSave()
        upsertDto.id = null
        upsertDto.employerId = employer.id

        //when
        performPutRequest(upsertDto, MockMvcResultMatchers.status().isBadRequest)
    }

    private fun performPutRequest(
        upsertDto: DriverUpsertDto, resultStatusMatcher: ResultMatcher
    ) = mockMvc.perform(
        MockMvcRequestBuilders.put("/drivers")
            .header(DsmConstants.TVM.TVM_HEADER, "TVM_TICKET")
            .contentType(MediaType.APPLICATION_JSON_UTF8)
            .content(TplObjectMappers.TPL_API_OBJECT_MAPPER.writeValueAsString(upsertDto))
    )
        .andExpect(resultStatusMatcher)
        .andReturn()

    private fun assertsEquals(
        driverDto: DriverDto,
        upsertDto: DriverUpsertDto
    ) {
        Assertions.assertThat(driverDto.id).isNotEmpty
        if (upsertDto.id != null) Assertions.assertThat(upsertDto.id).isEqualTo(driverDto.id)
        Assertions.assertThat(upsertDto.uid).isEqualTo(driverDto.uid)

        if (upsertDto.employerId != null) {
            Assertions.assertThat(driverDto.employerIds).contains(upsertDto.employerId)
        }

        if (upsertDto.personalData.name != null) {
            Assertions.assertThat(upsertDto.personalData.name).isEqualTo(driverDto.name)
        } else {
            Assertions.assertThat(upsertDto.personalData.lastName + " " + upsertDto.personalData.firstName)
                .isEqualTo(driverDto.name)
        }
    }

    companion object {

        @JvmStatic
        fun driverPutInvalidParamsSource() = StreamEx.of(
            Arguments.of(
                DriverTestFactory.getValidDriverUpsertDto().apply { this.uid = null }
            ),
            Arguments.of(
                DriverTestFactory.getValidDriverUpsertDto().apply { this.personalData = null }
            ),
            Arguments.of(
                DriverTestFactory.getValidDriverUpsertDto().apply { this.personalData.email = null }
            ),
            Arguments.of(
                DriverTestFactory.getValidDriverUpsertDto().apply { this.personalData.lastName = null }
            ),
            Arguments.of(
                DriverTestFactory.getValidDriverUpsertDto().apply { this.personalData.firstName = null }
            ),
        )
    }
}

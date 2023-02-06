package ru.yandex.market.dsm.api

import one.util.streamex.StreamEx
import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.assertThat
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
import ru.yandex.market.dsm.domain.courier.service.CourierQueryService
import ru.yandex.market.dsm.domain.courier.test.AssertsCourierFactory
import ru.yandex.market.dsm.domain.courier.test.CourierTestFactory
import ru.yandex.market.dsm.domain.employer.EmployersTestFactory
import ru.yandex.market.dsm.domain.nationality.db.NationalityDbo
import ru.yandex.market.dsm.domain.nationality.db.NationalityDboRepository
import ru.yandex.market.tpl.common.util.TplObjectMappers
import ru.yandex.mj.generated.server.model.CourierDto
import ru.yandex.mj.generated.server.model.CourierRegistrationStatusDto
import ru.yandex.mj.generated.server.model.CourierStatusDto
import ru.yandex.mj.generated.server.model.CourierTypeDto
import ru.yandex.mj.generated.server.model.CourierUpsertDto
import ru.yandex.mj.generated.server.model.PersonalDataUpsertDto
import java.time.LocalDate

internal class CourierApiPutTest : AbstractDsmApiTest() {

    @Autowired
    private lateinit var employersFactory: EmployersTestFactory
    @Autowired
    private lateinit var courierTestFactory: CourierTestFactory
    @Autowired
    private lateinit var courierQueryService: CourierQueryService
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
    fun checkUnrecognizedFieldIgnore() {
        val employer = employersFactory.createAndSave()
        val upsertDto = getValidCourierUpsertDto()
        upsertDto.id = null
        upsertDto.employerId = employer.id

        var strRequest = TplObjectMappers.TPL_API_OBJECT_MAPPER.writeValueAsString(upsertDto)
        strRequest = strRequest.substring(0, strRequest.length - 1)
        strRequest += ",\"badField\":\"badValue\"}"

        val response = mockMvc.perform(
            MockMvcRequestBuilders.put("/couriers")
                .header(DsmConstants.TVM.TVM_HEADER, "TVM_TICKET")
                .contentType(MediaType.APPLICATION_JSON_UTF8)
                .content(strRequest)
        )
            .andExpect(MockMvcResultMatchers.status().isCreated)
            .andReturn()

        assertThat(response.response.contentAsString).isNotBlank
        val result = TplObjectMappers.TPL_API_OBJECT_MAPPER.readValue(
            response.response.contentAsString,
            CourierDto::class.java
        )
        assertsEquals(result, upsertDto)
    }

    @Test
    fun `couriersPut - create`() {
        //given
        val employer = employersFactory.createAndSave()
        val upsertDto = getValidCourierUpsertDto()
        upsertDto.id = null
        upsertDto.employerId = employer.id
        upsertDto.courierType = null

        //when
        val response = performPutRequest(upsertDto, MockMvcResultMatchers.status().isCreated)

        //then
        Assertions.assertThat(response.response.contentAsString).isNotBlank

        val result = TplObjectMappers.TPL_API_OBJECT_MAPPER.readValue(
            response.response.contentAsString,
            CourierDto::class.java
        )
        assertsEquals(result, upsertDto)
    }

    @Test
    fun `couriersPut - create with same passport`() {
        val employer = employersFactory.createAndSave()
        val upsertDto1 = getValidCourierUpsertDto()
        upsertDto1.id = null
        upsertDto1.employerId = employer.id
        val upsertDto2 = getValidCourierUpsertDto()
        upsertDto2.id = null
        upsertDto2.uid = upsertDto2.uid + "_2"
        upsertDto2.employerId = employer.id

        val response1 = performPutRequest(upsertDto1, MockMvcResultMatchers.status().isCreated)
        val response2 = performPutRequest(upsertDto2, MockMvcResultMatchers.status().isCreated)

        val result1 = TplObjectMappers.TPL_API_OBJECT_MAPPER.readValue(
            response1.response.contentAsString,
            CourierDto::class.java
        )
        val result2 = TplObjectMappers.TPL_API_OBJECT_MAPPER.readValue(
            response2.response.contentAsString,
            CourierDto::class.java
        )

        assertThat(
            courierQueryService.getById(result1.id).personalData.passportData.id
        ).isEqualTo(
            courierQueryService.getById(result2.id).personalData.passportData.id
        )
    }

    @Test
    fun `couriersPut - create with only required attrs`() {
        //given
        val employer = employersFactory.createAndSave()
        val upsertDto = CourierUpsertDto().apply {
            this.uid = "uid"
            this.employerId = employer.id
            this.courierType = CourierTypeDto.PARTNER
            this.courierRegistrationStatus = CourierRegistrationStatusDto.REGISTERED
            this.personalData = PersonalDataUpsertDto().apply {
                this.name = "test name"
                this.email = "email"
                this.lastName = "lastName"
                this.firstName = "firstName"
            }
        }

        //when
        val response = performPutRequest(upsertDto, MockMvcResultMatchers.status().isCreated)

        //then
        assertThat(response.response.contentAsString).isNotBlank

        val result = TplObjectMappers.TPL_API_OBJECT_MAPPER.readValue(
            response.response.contentAsString,
            CourierDto::class.java
        )
        assertsEquals(result, upsertDto)
    }

    @Test
    fun `couriersPut - conflict on create`() {
        //given
        val employer = employersFactory.createAndSave()
        val courier = courierTestFactory.create(employerId = employer.id, uid = "uid-1")
        val upsertDto = getValidCourierUpsertDto()
        upsertDto.id = null
        upsertDto.uid = courier.uid
        upsertDto.employerId = employer.id

        //when
        val response = performPutRequest(upsertDto, MockMvcResultMatchers.status().isOk)

        //then
        val result = TplObjectMappers.TPL_API_OBJECT_MAPPER.readValue(
            response.response.contentAsString,
            CourierDto::class.java
        )
        AssertsCourierFactory.assertsEquals(result, courier)
    }

    @Test
    fun `couriersPut - update`() {
        //given
        val employer = employersFactory.createAndSave()
        val courier = courierTestFactory.create(employerId = employer.id, uid = "uid-1")
        val upsertDto = getValidCourierUpsertDto()
        upsertDto.id = courier.id
        upsertDto.courierType = null
        upsertDto.employerId = employer.id

        //when
        val response = performPutRequest(upsertDto, MockMvcResultMatchers.status().isOk)

        //then
        Assertions.assertThat(response.response.contentAsString).isNotBlank

        val result = TplObjectMappers.TPL_API_OBJECT_MAPPER.readValue(
            response.response.contentAsString,
            CourierDto::class.java
        )
        assertThat(result.id).isEqualTo(courier.id)
        assertsEquals(result, upsertDto)
    }

    @Test
    fun `couriersPut - delete via update`() {
        //given
        val employer = employersFactory.createAndSave()
        val courier = courierTestFactory.create(employerId = employer.id, uid = "uid-1")
        val upsertDto = getValidCourierUpsertDto()
        upsertDto.id = courier.id
        upsertDto.employerId = employer.id
        upsertDto.deleted = true

        //when
        val response = performPutRequest(upsertDto, MockMvcResultMatchers.status().isOk)

        //then
        assertThat(response.response.contentAsString).isNotBlank

        val result = TplObjectMappers.TPL_API_OBJECT_MAPPER.readValue(
            response.response.contentAsString,
            CourierDto::class.java
        )
        assertThat(result.id).isEqualTo(courier.id)
        assertsEquals(result, upsertDto)
    }

    @Test
    fun `couriersPut - employer doesnt exist`() {
        //given
        val upsertDto = getValidCourierUpsertDto()
        upsertDto.id = null
        upsertDto.employerId = "not-exists"

        //when
        performPutRequest(upsertDto, MockMvcResultMatchers.status().isBadRequest)
    }

    @Test
    fun `couriersIdPatch - update`() {
        //given
        val employer = employersFactory.createAndSave()
        val employer2 = employersFactory.createAndSave()
        val courier = courierTestFactory.create(employerId = employer.id, uid = "uid-1")

        val postfix = "_updated"
        val courierExpected = courier.copy(
            id = courier.id,
            uid = courier.uid + postfix,
            employerId = employer2.id,
            routingId = courier.routingId + postfix,
            workplaceNumber = courier.workplaceNumber + postfix,
            personalData = courier.personalData.copy(
                id = courier.personalData.id,
                email = courier.personalData.email + postfix,
                name = courier.personalData.name + postfix,
                phone = courier.personalData.phone + postfix,
                telegramLogin = courier.personalData.telegramLogin + postfix,
                passportData = courier.personalData.passportData.copy(
                    id = courier.personalData.passportData.id,
                    lastName = courier.personalData.passportData.lastName + postfix,
                    firstName = courier.personalData.passportData.firstName + postfix,
                    patronymicName = courier.personalData.passportData.patronymicName + postfix,
                    passportNumber = courier.personalData.passportData.passportNumber + postfix,
                    expiredAt = courier.personalData.passportData.expiredAt?.plusDays(1),
                    nationality = "RUS",
                    birthday = courier.personalData.passportData.birthday?.plusDays(1),
                ),
                vaccinated = !courier.personalData.vaccinated,
                vaccinationLink = courier.personalData.vaccinationLink,
                vaccinationExpiredAt = courier.personalData.vaccinationExpiredAt?.plusDays(1),
            )
        )

        val body = mutableMapOf<String, Any?>().apply {
            this["uid"] = courierExpected.uid
            this["employerId"] = courierExpected.employerId
            this["routingId"] = courierExpected.routingId
            this["workplaceNumber"] = courierExpected.workplaceNumber

            this["personalData"] = mutableMapOf<String, Any?>().apply {
                this["email"] = courierExpected.personalData.email
                this["name"] = courierExpected.personalData.name
                this["lastName"] = courierExpected.personalData.passportData.lastName
                this["firstName"] = courierExpected.personalData.passportData.firstName
                this["patronymicName"] = courierExpected.personalData.passportData.patronymicName
                this["passportNumber"] = courierExpected.personalData.passportData.passportNumber
                this["expiredAt"] = courierExpected.personalData.passportData.expiredAt
                this["nationality"] = "RUS"
                this["birthday"] = courierExpected.personalData.passportData.birthday
                this["phone"] = courierExpected.personalData.phone
                this["telegramLogin"] = courierExpected.personalData.telegramLogin
                this["vaccinated"] = courierExpected.personalData.vaccinated
                this["vaccinationLink"] = courierExpected.personalData.vaccinationLink
                this["vaccinationExpiredAt"] = courierExpected.personalData.vaccinationExpiredAt
            }
        }
        //when
        val response = mockMvc.perform(
            MockMvcRequestBuilders.patch("/couriers/${courier.id}")
                .header(DsmConstants.TVM.TVM_HEADER, "TVM_TICKET")
                .contentType(MediaType.APPLICATION_JSON_UTF8)
                .content(TplObjectMappers.TPL_API_OBJECT_MAPPER.writeValueAsString(body))
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andReturn()

        //then
        assertThat(response.response.contentAsString).isNotBlank

        val result = courierQueryService.getById(courier.id)

        assertThat(result)
            .usingRecursiveComparison()
            .ignoringFields("updatedAt")
            .isEqualTo(courierExpected)
    }

    @ParameterizedTest
    @MethodSource("couriersPutInvalidParamsSource")
    fun `couriersPut - validation fail`(upsertDto: CourierUpsertDto) {
        //given
        val employer = employersFactory.createAndSave()
        upsertDto.id = null
        upsertDto.employerId = employer.id

        //when
        performPutRequest(upsertDto, MockMvcResultMatchers.status().isBadRequest)
    }

    private fun assertsEquals(
        courierDto: CourierDto,
        upsertDto: CourierUpsertDto
    ) {
        assertThat(courierDto.id).isNotEmpty
        if (upsertDto.id != null) assertThat(upsertDto.id).isEqualTo(courierDto.id)
        assertThat(CourierStatusDto.NOT_ACTIVE).isEqualTo(courierDto.status)
        assertThat(upsertDto.uid).isEqualTo(courierDto.uid)
        assertThat(upsertDto.employerId).isEqualTo(courierDto.employerId)
        assertThat(upsertDto.routingId).isEqualTo(courierDto.routingId)
        assertThat(upsertDto.workplaceNumber).isEqualTo(courierDto.workplaceNumber)
        assertThat(upsertDto.personalData.email).isEqualTo(courierDto.email)
        assertThat(upsertDto.personalData.name).isEqualTo(courierDto.name)
        if (upsertDto.personalData.name != null) {
            assertThat(upsertDto.personalData.name).isEqualTo(courierDto.name)
        } else {
            assertThat(upsertDto.personalData.lastName + " " + upsertDto.personalData.firstName)
                .isEqualTo(courierDto.name)
        }
        assertThat(upsertDto.deleted ?: false).isEqualTo(courierDto.deleted)
    }

    private fun performPutRequest(
        upsertDto: CourierUpsertDto, resultStatusMatcher: ResultMatcher
    ) = mockMvc.perform(
        MockMvcRequestBuilders.put("/couriers")
            .header(DsmConstants.TVM.TVM_HEADER, "TVM_TICKET")
            .contentType(MediaType.APPLICATION_JSON_UTF8)
            .content(TplObjectMappers.TPL_API_OBJECT_MAPPER.writeValueAsString(upsertDto))
    )
        .andExpect(resultStatusMatcher)
        .andReturn()

    companion object {
        @JvmStatic
        fun couriersPutInvalidParamsSource() = StreamEx.of(
            Arguments.of(
                getValidCourierUpsertDto().apply { this.uid = null }
            ),
            Arguments.of(
                getValidCourierUpsertDto().apply { this.personalData = null }
            ),
            Arguments.of(
                getValidCourierUpsertDto().apply { this.personalData.email = null }
            ),
            Arguments.of(
                getValidCourierUpsertDto().apply { this.personalData.lastName = null }
            ),
            Arguments.of(
                getValidCourierUpsertDto().apply { this.personalData.firstName = null }
            ),
        )

        private fun getValidCourierUpsertDto() = CourierUpsertDto().apply {
            this.id = "id"
            this.uid = "uid"
            this.employerId = "employerId"
            this.routingId = "routingId"
            this.workplaceNumber = "workplaceNumber"
            this.courierRegistrationStatus = CourierRegistrationStatusDto.REGISTERED
            this.courierType = CourierTypeDto.PARTNER
            this.personalData = PersonalDataUpsertDto().apply {
                this.email = "email"
                this.name = "name"
                this.lastName = "lastName"
                this.firstName = "firstName"
                this.patronymicName = "patronymicName"
                this.passportNumber = "passportNumber"
                this.expiredAt = LocalDate.EPOCH
                this.nationality = null
                this.birthday = LocalDate.EPOCH
                this.phone = "phone"
                this.telegramLogin = "telegramLogin"
                this.vaccinated = true
                this.vaccinationLink = "vaccinationLink"
                this.vaccinationExpiredAt = LocalDate.EPOCH
                this.vaccinationInfos = listOf()
            }
        }
    }

}

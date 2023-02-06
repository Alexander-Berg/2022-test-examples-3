package ru.yandex.market.dsm.api

import one.util.streamex.StreamEx
import org.assertj.core.api.Assertions.assertThat
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
import ru.yandex.market.dsm.domain.courier.test.CourierTestFactory
import ru.yandex.market.dsm.domain.employer.EmployersTestFactory
import ru.yandex.market.tpl.common.util.TplObjectMappers
import ru.yandex.mj.generated.server.model.CourierPersonalDataDto
import ru.yandex.mj.generated.server.model.PersonalDataUpsertDto
import ru.yandex.mj.generated.server.model.VaccinationInfoDto
import java.time.Clock
import java.time.LocalDate

internal class CouriersIdPersonalDataPutTest : AbstractDsmApiTest() {

    @Autowired
    private lateinit var employersFactory: EmployersTestFactory

    @Autowired
    private lateinit var courierTestFactory: CourierTestFactory

    @Autowired
    private lateinit var courierQueryService: CourierQueryService

    @Autowired
    private lateinit var clock: Clock

    @Test
    fun `couriersIdPersonalDataPut - update`() {
        val employer = employersFactory.createAndSave()
        val courier = courierTestFactory.create(employerId = employer.id, uid = "uid-1")
        val upsertDto = getValidPersonalDataUpsertDto()
        upsertDto.vaccinationInfos = listOf(
            VaccinationInfoDto().apply {
                this.date = LocalDate.now(clock)
            }
        )

        val response = performPutRequest(courier.id, upsertDto, MockMvcResultMatchers.status().isOk)

        assertThat(response.response.contentAsString).isNotBlank

        val result = TplObjectMappers.TPL_API_OBJECT_MAPPER.readValue(
            response.response.contentAsString,
            CourierPersonalDataDto::class.java
        )
        assertsEquals(result, upsertDto)
    }

    @ParameterizedTest
    @MethodSource("couriersIdPersonalDataPutInvalidParamsSource")
    fun `couriersIdPersonalDataPut - validation fail`(upsertDto: PersonalDataUpsertDto) {
        upsertDto.vaccinationInfos = listOf(
            VaccinationInfoDto().apply {
                this.date = LocalDate.now(clock)
            }
        )
        val employer = employersFactory.createAndSave()
        val courier = courierTestFactory.create(employerId = employer.id, uid = "uid-1")

        performPutRequest(courier.id, upsertDto, MockMvcResultMatchers.status().isBadRequest)
    }

    private fun assertsEquals(
        dto: CourierPersonalDataDto,
        upsertDto: PersonalDataUpsertDto,
    ) {
        assertThat(dto.id).isNotEmpty
        assertThat(dto.email).isEqualTo(upsertDto.email)
        if (upsertDto.name != null) {
            assertThat(dto.name).isEqualTo(upsertDto.name)
        } else {
            assertThat(upsertDto.lastName + " " + upsertDto.firstName)
                .isEqualTo(dto.name)
        }
        assertThat(dto.lastName).isEqualTo(upsertDto.lastName)
        assertThat(dto.firstName).isEqualTo(upsertDto.firstName)
        assertThat(dto.patronymicName).isEqualTo(upsertDto.patronymicName)
        assertThat(dto.passportNumber).isEqualTo(upsertDto.passportNumber)
        assertThat(dto.expiredAt).isEqualTo(upsertDto.expiredAt)
        assertThat(dto.nationality).isEqualTo(upsertDto.nationality)
        assertThat(dto.birthday).isEqualTo(upsertDto.birthday)
        assertThat(dto.phone).isEqualTo(upsertDto.phone)
        assertThat(dto.telegramLogin).isEqualTo(upsertDto.telegramLogin)
        assertThat(dto.vaccinated).isEqualTo(upsertDto.vaccinated)
        assertThat(dto.vaccinationLink).isEqualTo(upsertDto.vaccinationLink)
        assertThat(dto.vaccinationExpiredAt).isEqualTo(upsertDto.vaccinationExpiredAt)
    }

    private fun performPutRequest(
        courierId: String,
        upsertDto: PersonalDataUpsertDto,
        resultStatusMatcher: ResultMatcher,
    ) = mockMvc.perform(
        MockMvcRequestBuilders.put("/couriers/$courierId/personal-data")
            .header(DsmConstants.TVM.TVM_HEADER, "TVM_TICKET")
            .contentType(MediaType.APPLICATION_JSON_UTF8)
            .content(TplObjectMappers.TPL_API_OBJECT_MAPPER.writeValueAsString(upsertDto))
    )
        .andExpect(resultStatusMatcher)
        .andReturn()

    companion object {
        @JvmStatic
        fun couriersIdPersonalDataPutInvalidParamsSource() = StreamEx.of(
            Arguments.of(
                getValidPersonalDataUpsertDto().apply { this.email = null }
            ),
            Arguments.of(
                getValidPersonalDataUpsertDto().apply { this.lastName = null }
            ),
            Arguments.of(
                getValidPersonalDataUpsertDto().apply { this.firstName = null }
            ),
            Arguments.of(
                getValidPersonalDataUpsertDto().apply { this.firstName = null }
            ),
        )

        private fun getValidPersonalDataUpsertDto() = PersonalDataUpsertDto().apply {
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
            this.vaccinationInfos = listOf(
                VaccinationInfoDto().apply {
                    this.date = LocalDate.now()
                }
            )
        }
    }
}

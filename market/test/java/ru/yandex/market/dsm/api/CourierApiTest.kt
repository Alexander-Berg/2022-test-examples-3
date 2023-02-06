package ru.yandex.market.dsm.api

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import ru.yandex.market.dsm.config.DsmConstants
import ru.yandex.market.dsm.core.test.AbstractDsmApiTest
import ru.yandex.market.dsm.domain.courier.service.CourierQueryService
import ru.yandex.market.dsm.domain.courier.test.AssertsCourierFactory
import ru.yandex.market.dsm.domain.courier.test.CourierTestFactory
import ru.yandex.market.dsm.domain.employer.EmployersTestFactory
import ru.yandex.market.tpl.common.util.TplObjectMappers
import ru.yandex.mj.generated.server.model.CourierDto
import ru.yandex.mj.generated.server.model.CourierPersonalDataDto

internal class CourierApiTest : AbstractDsmApiTest() {

    @Autowired
    private lateinit var employersFactory: EmployersTestFactory
    @Autowired
    private lateinit var courierTestFactory: CourierTestFactory
    @Autowired
    private lateinit var courierQueryService: CourierQueryService

    @Test
    fun `couriersIdGet - success`() {
        //given
        val employer = employersFactory.createAndSave()
        val courier = courierTestFactory.create(employer.id, "uid")

        //when
        val response = mockMvc
            .perform(
                MockMvcRequestBuilders.get("/couriers/${courier.id}")
                    .header(DsmConstants.TVM.TVM_HEADER, "TVM_TICKET")
            )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andReturn()

        //then
        val result = TplObjectMappers.TPL_API_OBJECT_MAPPER.readValue(
            response.response.contentAsString,
            CourierDto::class.java
        )
        AssertsCourierFactory.assertsEquals(result, courier)
    }

    @Test
    fun `couriersIdGet - not found`() {
        mockMvc
            .perform(
                MockMvcRequestBuilders.get("/couriers/not-exists")
                    .header(DsmConstants.TVM.TVM_HEADER, "TVM_TICKET")
            )
            .andExpect(MockMvcResultMatchers.status().isNotFound)
    }

    @Test
    fun `couriersIdPersonalDataGet - success`() {
        //given
        val employer = employersFactory.createAndSave()
        val courier = courierTestFactory.create(employer.id, "uid")

        //when
        val response = mockMvc
            .perform(
                MockMvcRequestBuilders.get("/couriers/${courier.id}/personal-data")
                    .header(DsmConstants.TVM.TVM_HEADER, "TVM_TICKET")
            )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andReturn()

        //then
        val result = TplObjectMappers.TPL_API_OBJECT_MAPPER.readValue(
            response.response.contentAsString,
            CourierPersonalDataDto::class.java
        )

        assertThat(result.id).isEqualTo(courier.personalData.id)
        assertThat(result.email).isEqualTo(courier.personalData.email)
        assertThat(result.name).isEqualTo(courier.personalData.name)
        assertThat(result.lastName).isEqualTo(courier.personalData.passportData.lastName)
        assertThat(result.firstName).isEqualTo(courier.personalData.passportData.firstName)
        assertThat(result.patronymicName).isEqualTo(courier.personalData.passportData.patronymicName)
        assertThat(result.passportNumber).isEqualTo(courier.personalData.passportData.passportNumber)
        assertThat(result.expiredAt).isEqualTo(courier.personalData.passportData.expiredAt)
        assertThat(result.nationality).isEqualTo(courier.personalData.passportData.nationality)
        assertThat(result.birthday).isEqualTo(courier.personalData.passportData.birthday)
        assertThat(result.phone).isEqualTo(courier.personalData.phone)
        assertThat(result.telegramLogin).isEqualTo(courier.personalData.telegramLogin)
        assertThat(result.vaccinated).isEqualTo(courier.personalData.vaccinated)
        assertThat(result.vaccinationLink).isEqualTo(courier.personalData.vaccinationLink)
        assertThat(result.vaccinationInfos?.size).isEqualTo(courier.personalData.vaccinationsDates?.size)
    }

    @Test
    fun `couriersIdPersonalDataGet - not found`() {
        mockMvc
            .perform(
                MockMvcRequestBuilders.get("/couriers/not-exists/personal-data")
                    .header(DsmConstants.TVM.TVM_HEADER, "TVM_TICKET")
            )
            .andExpect(MockMvcResultMatchers.status().isNotFound)
    }

    @Test
    fun `couriersIdDelete - success`() {
        //given
        val employer = employersFactory.createAndSave()
        val courier = courierTestFactory.create(employer.id, "uid")

        //when
        val response = mockMvc
            .perform(
                MockMvcRequestBuilders.delete("/couriers/${courier.id}")
                    .header(DsmConstants.TVM.TVM_HEADER, "TVM_TICKET")
            )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andReturn()

        //then
        val result = TplObjectMappers.TPL_API_OBJECT_MAPPER.readValue(
            response.response.contentAsString,
            CourierDto::class.java
        )
        assertThat(result.deleted).isEqualTo(true)
        AssertsCourierFactory.assertsEquals(result, courierQueryService.getById(courier.id))
    }

}

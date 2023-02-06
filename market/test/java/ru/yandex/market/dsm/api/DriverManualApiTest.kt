package ru.yandex.market.dsm.api

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import ru.yandex.market.dsm.config.DsmConstants
import ru.yandex.market.dsm.core.test.AbstractDsmApiTest
import ru.yandex.market.dsm.domain.driver.service.DriverQueryService
import ru.yandex.market.dsm.domain.driver.test.DriverTestFactory
import ru.yandex.market.dsm.domain.employer.EmployersTestFactory
import ru.yandex.market.tpl.common.util.TplObjectMappers
import ru.yandex.mj.generated.server.model.DriverDto

class DriverManualApiTest : AbstractDsmApiTest() {

    @Autowired
    private lateinit var employersFactory: EmployersTestFactory
    @Autowired
    private lateinit var driverQueryService: DriverQueryService

    @Test
    fun `driver - manual delete`() {
        //given
        val employer = employersFactory.createAndSave()

        val upsertDto = DriverTestFactory.getValidDriverUpsertDto()
        upsertDto.id = null
        upsertDto.employerId = employer.id

        val response = mockMvc.perform(
            MockMvcRequestBuilders.put("/drivers")
                .header(DsmConstants.TVM.TVM_HEADER, "TVM_TICKET")
                .contentType(MediaType.APPLICATION_JSON_UTF8)
                .content(TplObjectMappers.TPL_API_OBJECT_MAPPER.writeValueAsString(upsertDto)))
            .andExpect(MockMvcResultMatchers.status().isCreated)
            .andReturn()

        val result = TplObjectMappers.TPL_API_OBJECT_MAPPER.readValue(
            response.response.contentAsString,
            DriverDto::class.java
        )
        Assertions.assertThat(upsertDto.uid).isEqualTo(result.uid)

        //when
        mockMvc.perform(
            MockMvcRequestBuilders.delete("/manual/drivers/${result.id}")
                .header(DsmConstants.TVM.TVM_HEADER, "TVM_TICKET")
        )
            .andExpect(MockMvcResultMatchers.status().isOk)

        Assertions.assertThat(driverQueryService.findById(result.id)).isEmpty
    }

}

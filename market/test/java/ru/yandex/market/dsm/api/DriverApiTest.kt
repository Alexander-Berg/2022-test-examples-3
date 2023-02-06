package ru.yandex.market.dsm.api

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import ru.yandex.market.dsm.config.DsmConstants
import ru.yandex.market.dsm.core.test.AbstractDsmApiTest
import ru.yandex.market.dsm.domain.driver.service.DriverQueryService
import ru.yandex.market.dsm.domain.driver.test.AssertDriverFactory
import ru.yandex.market.dsm.domain.driver.test.DriverTestFactory
import ru.yandex.market.dsm.domain.employer.EmployersTestFactory
import ru.yandex.market.tpl.common.util.TplObjectMappers
import ru.yandex.mj.generated.server.model.DriverDto
import ru.yandex.mj.generated.server.model.DriverPersonalDataDto

class DriverApiTest : AbstractDsmApiTest() {

    @Autowired
    private lateinit var employersFactory: EmployersTestFactory

    @Autowired
    private lateinit var driverTestFactory: DriverTestFactory

    @Autowired
    private lateinit var driverQueryService: DriverQueryService

    @Test
    fun `driverGetId - success`() {
        val employer = employersFactory.createAndSave()
        val driver = driverTestFactory.create(employer.id, "uid-1")

        val response = mockMvc
            .perform(
                MockMvcRequestBuilders.get("/drivers/${driver.id}")
                    .header(DsmConstants.TVM.TVM_HEADER, "TVM_TICKET")
            )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andReturn()

        val result = TplObjectMappers.TPL_API_OBJECT_MAPPER.readValue(
            response.response.contentAsString,
            DriverDto::class.java
        )

        AssertDriverFactory.assertEquals(result, driver)
    }

    @Test
    fun `driverGetId - not found`() {
        mockMvc
            .perform(
                MockMvcRequestBuilders.get("/drivers/not-exists")
                    .header(DsmConstants.TVM.TVM_HEADER, "TVM_TICKET")
            )
            .andExpect(MockMvcResultMatchers.status().isNotFound)
    }

    @Test
    fun `driverIdPersonalDataGet - success`() {
        val employer = employersFactory.createAndSave()
        val driver = driverTestFactory.create(employer.id, "uid-1")

        val response = mockMvc
            .perform(
                MockMvcRequestBuilders.get("/drivers/${driver.id}/personal-data")
                    .header(DsmConstants.TVM.TVM_HEADER, "TVM_TICKET")
            )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andReturn()

        val result = TplObjectMappers.TPL_API_OBJECT_MAPPER.readValue(
            response.response.contentAsString,
            DriverPersonalDataDto::class.java
        )
        AssertDriverFactory.assertEquals(result, driver)
    }

    @Test
    fun `driverIdPersonalDataGet - not found`() {
        mockMvc
            .perform(
                MockMvcRequestBuilders.get("/drivers/not-exists/personal-data")
                    .header(DsmConstants.TVM.TVM_HEADER, "TVM_TICKET")
            )
            .andExpect(MockMvcResultMatchers.status().isNotFound)
    }

    @Test
    fun `driverGetUid - success`() {
        val employer = employersFactory.createAndSave()
        val driver = driverTestFactory.create(employer.id, "uid-1")

        val response = mockMvc
            .perform(
                MockMvcRequestBuilders.get("/driver-by-uid/${driver.uid}")
                    .header(DsmConstants.TVM.TVM_HEADER, "TVM_TICKET")
            )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andReturn()

        val result = TplObjectMappers.TPL_API_OBJECT_MAPPER.readValue(
            response.response.contentAsString,
            DriverDto::class.java
        )

        AssertDriverFactory.assertEquals(result, driver)
    }

    @Test
    fun `driverGetUid - not found`() {
        mockMvc
            .perform(
                MockMvcRequestBuilders.get("/driver-by-uid/not-exists")
                    .header(DsmConstants.TVM.TVM_HEADER, "TVM_TICKET")
            )
            .andExpect(MockMvcResultMatchers.status().isNotFound)
    }

    @Test
    fun `driverUidPersonalDataGet - success`() {
        val employer = employersFactory.createAndSave()
        val driver = driverTestFactory.create(employer.id, "uid-1")

        val response = mockMvc
            .perform(
                MockMvcRequestBuilders.get("/driver-by-uid/${driver.uid}/personal-data")
                    .header(DsmConstants.TVM.TVM_HEADER, "TVM_TICKET")
            )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andReturn()

        val result = TplObjectMappers.TPL_API_OBJECT_MAPPER.readValue(
            response.response.contentAsString,
            DriverPersonalDataDto::class.java
        )
        AssertDriverFactory.assertEquals(result, driver)
    }

    @Test
    fun `driverUidPersonalDataGet - not found`() {
        mockMvc
            .perform(
                MockMvcRequestBuilders.get("/driver-by-uid/not-exists/personal-data")
                    .header(DsmConstants.TVM.TVM_HEADER, "TVM_TICKET")
            )
            .andExpect(MockMvcResultMatchers.status().isNotFound)
    }

    @Test
    fun `driverByPassportSerialNumberGet - success`() {
        val employer = employersFactory.createAndSave()
        val driver = driverTestFactory.create(employer.id, "uid-1")

        val response = mockMvc
            .perform(
                MockMvcRequestBuilders
                    .get("/driver-by-passport/${driver.personalData.passportData.passportNumber}")
                    .header(DsmConstants.TVM.TVM_HEADER, "TVM_TICKET")
            )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andReturn()

        val result = TplObjectMappers.TPL_API_OBJECT_MAPPER.readValue(
            response.response.contentAsString,
            DriverDto::class.java
        )

        AssertDriverFactory.assertEquals(result, driver)
    }

    @Test
    fun `driverByPassportSerialNumberGet - not found`() {
        mockMvc
            .perform(
                MockMvcRequestBuilders.get("/driver-by-passport/not-exists")
                    .header(DsmConstants.TVM.TVM_HEADER, "TVM_TICKET")
            )
            .andExpect(MockMvcResultMatchers.status().isNotFound)
    }

    @Test
    fun `driversIdUpdateUidUidPut - success`() {
        val employer = employersFactory.createAndSave()
        val driver = driverTestFactory.create(employer.id, "uid-1")
        val newUid = "-101"

        val response = mockMvc
            .perform(
                MockMvcRequestBuilders
                    .put("/drivers/${driver.id}/updateUid/$newUid")
                    .header(DsmConstants.TVM.TVM_HEADER, "TVM_TICKET")
            )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andReturn()

        val result = TplObjectMappers.TPL_API_OBJECT_MAPPER.readValue(
            response.response.contentAsString,
            DriverDto::class.java
        )

        Assertions.assertThat(result.uid).isEqualTo(newUid)
        Assertions.assertThat(driverQueryService.findByIdOrThrow(driver.id).uid).isEqualTo(newUid)
    }

    @Test
    fun `driversIdUpdateUidUidPut - not found`() {
        mockMvc
            .perform(
                MockMvcRequestBuilders.put("/drivers/not-exists/updateUid/101")
                    .header(DsmConstants.TVM.TVM_HEADER, "TVM_TICKET")
            )
            .andExpect(MockMvcResultMatchers.status().isNotFound)
    }

    @Test
    fun `driversIdBlackListed - successfully added and deleted from black list`() {
        val employer = employersFactory.createAndSave()
        val driver = driverTestFactory.create(employer.id, "uid-1")

        mockMvc
            .perform(
                MockMvcRequestBuilders
                    .put("/drivers/${driver.id}/blackListed")
                    .header(DsmConstants.TVM.TVM_HEADER, "TVM_TICKET")
            )
            .andExpect(MockMvcResultMatchers.status().isOk)

        Assertions.assertThat(driverQueryService.findByIdOrThrow(driver.id).blackListed).isTrue

        mockMvc
            .perform(
                MockMvcRequestBuilders
                    .delete("/drivers/${driver.id}/blackListed")
                    .header(DsmConstants.TVM.TVM_HEADER, "TVM_TICKET")
            )
            .andExpect(MockMvcResultMatchers.status().isOk)

        Assertions.assertThat(driverQueryService.findByIdOrThrow(driver.id).blackListed).isFalse
    }

    @Test
    fun `driversIdBlackListedPut - not found`() {
        mockMvc
            .perform(
                MockMvcRequestBuilders
                    .put("/drivers/not-exists/blackListed")
                    .header(DsmConstants.TVM.TVM_HEADER, "TVM_TICKET")
            )
            .andExpect(MockMvcResultMatchers.status().isNotFound)
    }

    @Test
    fun `driversIdEmployersEmployerId - successfully added and deleted employer from driver`() {
        val employer1 = employersFactory.createAndSave()
        val employer2 = employersFactory.createAndSave()
        val driver = driverTestFactory.create(employer1.id, "uid-1")

        Assertions.assertThat(driverQueryService.findByIdOrThrow(driver.id).employerIds)
            .containsExactlyInAnyOrder(employer1.id)

        mockMvc
            .perform(
                MockMvcRequestBuilders
                    .put("/drivers/${driver.id}/employers/${employer2.id}")
                    .header(DsmConstants.TVM.TVM_HEADER, "TVM_TICKET")
            )
            .andExpect(MockMvcResultMatchers.status().isOk)

        Assertions.assertThat(driverQueryService.findByIdOrThrow(driver.id).employerIds)
            .containsExactlyInAnyOrder(employer1.id, employer2.id)

        mockMvc
            .perform(
                MockMvcRequestBuilders
                    .delete("/drivers/${driver.id}/employers/${employer1.id}")
                    .header(DsmConstants.TVM.TVM_HEADER, "TVM_TICKET")
            )
            .andExpect(MockMvcResultMatchers.status().isOk)

        Assertions.assertThat(driverQueryService.findByIdOrThrow(driver.id).employerIds)
            .containsExactlyInAnyOrder(employer2.id)
    }

    @Test
    fun `driversIdEmployersEmployerId - not found`() {
        mockMvc
            .perform(
                MockMvcRequestBuilders
                    .put("/drivers/not-exists/employers/not-exists")
                    .header(DsmConstants.TVM.TVM_HEADER, "TVM_TICKET")
            )
            .andExpect(MockMvcResultMatchers.status().isNotFound)
    }

}

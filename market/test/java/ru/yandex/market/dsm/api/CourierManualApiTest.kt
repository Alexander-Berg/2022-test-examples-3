package ru.yandex.market.dsm.api

import org.assertj.core.api.AssertionsForClassTypes
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import ru.yandex.market.dsm.config.DsmConstants
import ru.yandex.market.dsm.core.ddd.DsmCommandService
import ru.yandex.market.dsm.core.test.AbstractDsmApiTest
import ru.yandex.market.dsm.domain.courier.model.CourierRegistrationStatus
import ru.yandex.market.dsm.domain.courier.service.CourierQueryService
import ru.yandex.market.dsm.domain.courier.test.CourierTestFactory
import ru.yandex.market.dsm.domain.employer.service.EmployerService
import ru.yandex.market.dsm.test.TestUtil
import ru.yandex.mj.generated.server.model.EmployerUpsertDto

class CourierManualApiTest : AbstractDsmApiTest() {
    @Autowired
    private lateinit var employerService: EmployerService

    @Autowired
    private lateinit var courierTestFactory: CourierTestFactory

    @Autowired
    private lateinit var commandService: DsmCommandService

    @Autowired
    private lateinit var courierQueryService: CourierQueryService

    @Test
    fun manualCourierIdCourierRegistrationStatusPatch() {
        val upsertEmployer = TestUtil.OBJECT_GENERATOR.nextObject(EmployerUpsertDto::class.java)
        employerService.createEmployer(upsertEmployer)

        val courierCreate1 = courierTestFactory.createCommand(upsertEmployer.id, "12137458", "test33593845")
        courierCreate1.courierRegistrationStatus = CourierRegistrationStatus.SELF_EMPLOYED_REGISTRATION_PROCESSING

        commandService.handle(
            courierCreate1
        )

        val courier = courierQueryService.getByUid(courierCreate1.uid)
        AssertionsForClassTypes.assertThat(courier!!.courierRegistrationStatus).isEqualTo(
            CourierRegistrationStatus.SELF_EMPLOYED_REGISTRATION_PROCESSING
        )
        val id = courier.id

        val response = mockMvc.perform(
            MockMvcRequestBuilders.patch(
                "/manual/selfemployed/$id/courierRegistrationStatus?value=SELF_EMPLOYED_REGISTRATION_AWAITING"
            )
                .header(DsmConstants.TVM.TVM_HEADER, "TVM_TICKET")
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andReturn()
        val courierNew = courierQueryService.getByUid(courierCreate1.uid)
        AssertionsForClassTypes.assertThat(courierNew!!.courierRegistrationStatus).isEqualTo(
            CourierRegistrationStatus.SELF_EMPLOYED_REGISTRATION_AWAITING
        )
    }

    @Test
    fun manualCourierUidCourierRegistrationInProPut() {
        val response = mockMvc.perform(
            MockMvcRequestBuilders.put(
                "/manual/courier/47698554/courier-registration-in-pro"
            )
                .header(DsmConstants.TVM.TVM_HEADER, "TVM_TICKET")
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andReturn()
    }
}


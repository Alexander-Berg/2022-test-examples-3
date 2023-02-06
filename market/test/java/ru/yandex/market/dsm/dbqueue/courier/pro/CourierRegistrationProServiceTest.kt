package ru.yandex.market.dsm.dbqueue.courier.pro

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.Mockito.`when`
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.dsm.core.test.AbstractTest
import ru.yandex.market.dsm.domain.courier.model.CourierType
import ru.yandex.market.dsm.domain.courier.service.CourierQueryService
import ru.yandex.market.dsm.domain.courier.test.CourierTestFactory
import ru.yandex.market.dsm.domain.employer.EmployersTestFactory
import ru.yandex.market.dsm.domain.employer.model.EmployerType
import ru.yandex.market.dsm.external.pro.ProProfileIntegrationService
import ru.yandex.mj.generated.client.cp_profiles_draft.model.ProfileDraftResponse
import ru.yandex.mj.generated.client.cp_sync_profile.model.ContractorCreateResponse

class CourierRegistrationProServiceTest : AbstractTest() {
    @Autowired
    private lateinit var courierRegistrationProService: CourierRegistrationProService
    @Autowired
    private lateinit var proProfileIntegrationService: ProProfileIntegrationService
    @Autowired
    private lateinit var courierQueryService: CourierQueryService
    @Autowired
    private lateinit var courierTestFactory: CourierTestFactory
    @Autowired
    private lateinit var employersTestFactory: EmployersTestFactory

    @Test
    fun registration_selfEmployed() {
        val proProfileId = "122454_4576456"
        val employer = employersTestFactory.createAndSave(
            "485767446",
            "NAME3748495",
            "LOGIN4754435887",
            "EPVYVBTKTSRUO",
            EmployerType.SUPPLY,
        )

        val courier = courierTestFactory.create(
            employerId = employer.id,
            uid = "478543578",
            email = "email6643404",
            courierType = CourierType.SELF_EMPLOYED,
        )

        `when`(
            proProfileIntegrationService.upsertSelfEmployedCourier(courier)
        ).thenReturn(
            ProfileDraftResponse().apply {
                this.profileId = proProfileId
            }
        )

        courierRegistrationProService.processPayload(
            CourierRegistrationInProPayload(
                requestId = null,
                id = courier.id,
            )
        )

        courierQueryService.getById(courier.id).let {
            assertThat(it.yaProId).isEqualTo(proProfileId)
        }
    }

    @Test
    fun registration_partner() {
        val proContractorId = "457453_346579385"
        val employer = employersTestFactory.createAndSave(
            "485767445",
            "NAME3748494",
            "LOGIN4754435890",
            "EPVYVBTKTSRUO",
            EmployerType.SUPPLY,
        )

        val courier = courierTestFactory.create(
            employerId = employer.id,
            uid = "478543577",
            email = "email6643404",
            courierType = CourierType.PARTNER,
        )

        `when`(
            proProfileIntegrationService.createPartnerCourier(courier)
        ).thenReturn(
            ContractorCreateResponse().apply {
                this.contractorId = proContractorId
            }
        )

        courierRegistrationProService.processPayload(
            CourierRegistrationInProPayload(
                requestId = null,
                id = courier.id,
            )
        )

        courierQueryService.getById(courier.id).let {
            assertThat(it.yaProId).isEqualTo(proContractorId)
        }
    }
}

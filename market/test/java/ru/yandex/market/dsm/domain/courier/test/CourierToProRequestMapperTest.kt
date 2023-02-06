package ru.yandex.market.dsm.domain.courier.test

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.dsm.core.test.AbstractTest
import ru.yandex.market.dsm.domain.courier.mapper.CourierToProRequestMapper
import ru.yandex.market.dsm.domain.courier.model.Courier
import ru.yandex.market.dsm.domain.employer.EmployersTestFactory
import ru.yandex.market.dsm.domain.employer.model.EmployerType
import ru.yandex.mj.generated.client.cp_sync_profile.model.ExternalProfileModel

class CourierToProRequestMapperTest : AbstractTest() {
    @Autowired
    private lateinit var courierTestFactory: CourierTestFactory

    @Autowired
    private lateinit var courierToProRequestMapper: CourierToProRequestMapper

    @Autowired
    private lateinit var employersTestFactory: EmployersTestFactory

    @Test
    fun toCreate() {
        val employer = employersTestFactory.createAndSave(
            id = "4857676789", name = "NAME37484973789",
            login = "LOGIN475489487", type = EmployerType.SUPPLY, active = true
        )
        val courier = courierTestFactory.create(
            employerId = employer.id, uid = "47858898677",
            email = "email4892503404", deleted = false
        )
        val result = courierToProRequestMapper.mapToCreate(courier).getContractor()
        assert(result, courier)
    }

    @Test
    fun toUpdate() {
        val employer = employersTestFactory.createAndSave(
            id = "4857676734649", name = "NAME37484935435789",
            login = "LOGIN47548935357", type = EmployerType.SUPPLY, active = true
        )
        val courier = courierTestFactory.create(
            employerId = employer.id, uid = "47858898464377",
            email = "email4846462503404", deleted = false
        )
        val result = courierToProRequestMapper.mapToUpdate(courier).getContractor()
        assert(result, courier)
    }

    @Test
    fun toProfileDraftRequest() {
        val employer = employersTestFactory.createAndSave(
            id = "4857676734649",
            name = "NAME37484935435789",            login = "LOGIN47548935357",
            type = EmployerType.SUPPLY,
        )
        val courier = courierTestFactory.create(
            employerId = employer.id,
            uid = "47858898464377",
            email = "email4846462503404",
        )

        courierToProRequestMapper.mapToProfileDraftRequest(courier, false).let {
            assertThat(it.passportUid).isEqualTo(courier.uid)
            assertThat(it.phone).isNull()
            assertThat(it.details.externalId).isEqualTo(courier.id)
            assertThat(it.selfemployed).isTrue
        }

        courierToProRequestMapper.mapToProfileDraftRequest(courier, true).let {
            assertThat(it.passportUid).isNull()
            assertThat(it.phone).isEqualTo(courier.personalData.phone)
            assertThat(it.details.externalId).isEqualTo(courier.id)
            assertThat(it.selfemployed).isTrue
        }
    }

    private fun assert(contractor: ExternalProfileModel, courier: Courier) {
        assertThat(contractor.passportUid).isEqualTo(courier.uid)
        assertThat(contractor.phone).isEqualTo(courier.personalData.phone)
        assertThat(contractor.details.externalId).isEqualTo(courier.id)
        assertThat(contractor.details.profession).isEqualTo(courierToProRequestMapper.PARTNER_INFO_PROFESSION)
        assertThat(contractor.fullName.firstName).isEqualTo(courier.personalData.passportData.firstName)
        assertThat(contractor.fullName.lastName).isEqualTo(courier.personalData.passportData.lastName)
        assertThat(contractor.fullName.middleName).isEqualTo(courier.personalData.passportData.patronymicName)
    }
}

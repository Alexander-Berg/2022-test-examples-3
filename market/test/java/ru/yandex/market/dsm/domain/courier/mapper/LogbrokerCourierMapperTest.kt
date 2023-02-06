package ru.yandex.market.dsm.domain.courier.mapper

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.dsm.core.ddd.DsmCommandService
import ru.yandex.market.dsm.core.test.AbstractTest
import ru.yandex.market.dsm.domain.courier.model.CourierStatus
import ru.yandex.market.dsm.domain.courier.model.CourierType
import ru.yandex.market.dsm.domain.courier.test.CourierTestFactory
import ru.yandex.market.dsm.domain.employer.service.EmployerService
import ru.yandex.market.dsm.test.TestUtil
import ru.yandex.mj.generated.server.model.EmployerUpsertDto
import ru.yandex.mj.generated.server.model.LogbrokerCourierStatus
import ru.yandex.mj.generated.server.model.LogbrokerCourierTypeDto

internal class LogbrokerCourierMapperTest : AbstractTest() {

    @Autowired
    private lateinit var employerService: EmployerService
    @Autowired
    private lateinit var courierTestFactory: CourierTestFactory
    @Autowired
    private lateinit var commandService: DsmCommandService
    @Autowired
    private lateinit var logbrokerCourierMapper: LogbrokerCourierMapper

    private lateinit var employerId: String

    @BeforeEach
    fun init() {
        val upsertEmployer = TestUtil.OBJECT_GENERATOR.nextObject(EmployerUpsertDto::class.java)
        employerId = employerService.createEmployer(upsertEmployer)
    }

    @Test
    fun `map - success`() {
        val courier = courierTestFactory.create(employerId, "12134237")

        val result = logbrokerCourierMapper.map(courier)

        assertThat(result.id).isEqualTo(courier.id)
        assertThat(result.version).isEqualTo(courier.updatedAt.toEpochMilli())
        assertThat(result.uid).isEqualTo(courier.uid)
        assertThat(result.employerId).isEqualTo(courier.employerId)
        assertThat(result.deleted).isEqualTo(courier.deleted)
        assertThat(result.status).isEqualTo(mapToLogbrokerCourierStatus(courier.status))
        assertThat(result.courierType).isEqualTo(mapToLogbrokerCourierTypeDto(courier.courierType))
        assertThat(result.yaProId).isEqualTo(courier.yaProId)
    }

    private fun mapToLogbrokerCourierStatus(
        courierStatus: CourierStatus
    ) = when (courierStatus) {
        CourierStatus.REVIEW -> LogbrokerCourierStatus.REVIEW
        CourierStatus.INTERNSHIP -> LogbrokerCourierStatus.INTERNSHIP
        CourierStatus.NEWBIE -> LogbrokerCourierStatus.NEWBIE
        CourierStatus.ACTIVE -> LogbrokerCourierStatus.ACTIVE
        CourierStatus.FIRED -> LogbrokerCourierStatus.FIRED
        CourierStatus.IN_PROCESS_OF_FIRING -> LogbrokerCourierStatus.IN_PROCESS_OF_FIRING
        CourierStatus.NOT_ACTIVE -> LogbrokerCourierStatus.NOT_ACTIVE
    }

    private fun mapToLogbrokerCourierTypeDto(courierType: CourierType) =
        when(courierType) {
            CourierType.PARTNER -> LogbrokerCourierTypeDto.PARTNER
            CourierType.SELF_EMPLOYED -> LogbrokerCourierTypeDto.SELF_EMPLOYED
        }
}

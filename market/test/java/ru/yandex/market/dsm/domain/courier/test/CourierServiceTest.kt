package ru.yandex.market.dsm.domain.courier.test

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.dsm.core.ddd.DsmCommandService
import ru.yandex.market.dsm.core.exception.BadRequestException
import ru.yandex.market.dsm.core.test.AbstractTest
import ru.yandex.market.dsm.domain.courier.db.CourierDboRepository
import ru.yandex.market.dsm.domain.courier.model.CourierRegistrationStatus
import ru.yandex.market.dsm.domain.courier.model.CourierStatus
import ru.yandex.market.dsm.domain.courier.model.CourierType
import ru.yandex.market.dsm.domain.courier.service.CourierQueryService
import ru.yandex.market.dsm.domain.courier.service.CourierService
import ru.yandex.market.dsm.domain.employer.service.EmployerService
import ru.yandex.market.dsm.test.TestUtil
import ru.yandex.mj.generated.server.model.CourierPatchIdDto
import ru.yandex.mj.generated.server.model.CourierPatchIdsDto
import ru.yandex.mj.generated.server.model.EmployerUpsertDto
import java.time.Clock
import java.time.LocalDate

class CourierServiceTest() : AbstractTest() {
    @Autowired
    private lateinit var courierService: CourierService
    @Autowired
    private lateinit var employerService: EmployerService
    @Autowired
    private lateinit var courierTestFactory: CourierTestFactory
    @Autowired
    private lateinit var courierQueryService: CourierQueryService
    @Autowired
    private lateinit var commandService: DsmCommandService
    @Autowired
    private lateinit var courierDboRepository: CourierDboRepository
    @Autowired
    private lateinit var clock: Clock

    @Test
    fun bulkUpdateCertState() {
        val upsertEmployer = TestUtil.OBJECT_GENERATOR.nextObject(EmployerUpsertDto::class.java)
        employerService.createEmployer(upsertEmployer)

        val courierCreate1 = courierTestFactory.
        createCommand(upsertEmployer.id, "12134237", "test364564")
        courierCreate1.vaccinationExpiredAt = LocalDate.now(clock)
        courierCreate1.vaccinated = true

        val courierCreate2 = courierTestFactory.
        createCommand(upsertEmployer.id, "12134238", "test364565")
        courierCreate2.vaccinationExpiredAt = LocalDate.now(clock).plusDays(1)
        courierCreate2.vaccinated = true

        commandService.handle(
            courierCreate1
        )
        commandService.handle(
            courierCreate2
        )

        courierService.bulkUpdateCertState()

        val courier1 = courierQueryService.getByUid("12134237")
        val courier2 = courierQueryService.getByUid("12134238")

        assertThat(courier1!!.personalData.vaccinated).isFalse
        assertThat(courier2!!.personalData.vaccinated).isTrue
    }

    @Test
    fun updateWithCourierPatchIdsDto() {
        val upsertEmployer = TestUtil.OBJECT_GENERATOR.nextObject(EmployerUpsertDto::class.java)
        employerService.createEmployer(upsertEmployer)

        val courier1 = courierTestFactory.
        createCommand(upsertEmployer.id, "12134893", "test36453284")
        courier1.status = CourierStatus.NOT_ACTIVE
        commandService.handle(
            courier1
        )
        val courier1Patch = CourierPatchIdDto()
        courier1Patch.id = courierDboRepository.findByUid(courier1.uid)!!.id
        courier1Patch.params["status"] = "ACTIVE"

        val courier2 = courierTestFactory.
        createCommand(upsertEmployer.id, "12134201", "test364565")
        courier2.workplaceNumber = "342508"
        commandService.handle(
            courier2
        )
        val courier2Patch = CourierPatchIdDto()
        courier2Patch.id = courierDboRepository.findByUid(courier2.uid)!!.id
        courier2Patch.params["workplaceNumber"] = "11101"
        courier2Patch.params["courierRegistrationStatus"] = "READY_TO_BE_SELF_EMPLOYED"
        courier2Patch.params["courierType"] = "SELF_EMPLOYED"

        val courierPatchIdsDto = CourierPatchIdsDto()
        courierPatchIdsDto.addParamsItem(courier1Patch)
        courierPatchIdsDto.addParamsItem(courier2Patch)
        courierService.update(courierPatchIdsDto)

        val updateCourier1 = courierQueryService.getByUid(courier1.uid)
        val updateCourier2 = courierQueryService.getByUid(courier2.uid)
        assertThat(updateCourier1!!.status).isEqualTo(CourierStatus.ACTIVE)
        assertThat(updateCourier2!!.workplaceNumber).isEqualTo("11101")
        assertThat(updateCourier2.courierRegistrationStatus).isEqualTo(CourierRegistrationStatus.READY_TO_BE_SELF_EMPLOYED)
        assertThat(updateCourier2.courierType).isEqualTo(CourierType.SELF_EMPLOYED)
    }

    @Test
    fun testBadRequestForStatuses() {
        val upsertEmployer = TestUtil.OBJECT_GENERATOR.nextObject(EmployerUpsertDto::class.java)
        employerService.createEmployer(upsertEmployer)

        val courier1 = courierTestFactory.
        createCommand(upsertEmployer.id, "12134895", "test36453286")
        commandService.handle(
            courier1
        )
        val courierId = courierDboRepository.findByUid(courier1.uid)!!.id;

        val courier1Patch = CourierPatchIdDto()
        courier1Patch.id = courierId
        courier1Patch.params["status"] = "ACTIV"

        val courier2Patch = CourierPatchIdDto()
        courier2Patch.id = courierId
        courier2Patch.params["courierType"] = "PARTNE"

        val courier3Patch = CourierPatchIdDto()
        courier3Patch.id = courierId
        courier3Patch.params["courierRegistrationStatus"] = "READY_TO_BE_SELF_EMPLOYE"

        var courierPatchIdsDto = CourierPatchIdsDto()
        courierPatchIdsDto.addParamsItem(courier1Patch)
        assertThrows<BadRequestException> {
            courierService.update(courierPatchIdsDto)
        }

        courierPatchIdsDto = CourierPatchIdsDto()
        courierPatchIdsDto.addParamsItem(courier2Patch)
        assertThrows<BadRequestException> {
            courierService.update(courierPatchIdsDto)
        }

        courierPatchIdsDto = CourierPatchIdsDto()
        courierPatchIdsDto.addParamsItem(courier3Patch)
        assertThrows<BadRequestException> {
            courierService.update(courierPatchIdsDto)
        }
    }
}

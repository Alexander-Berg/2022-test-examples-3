package ru.yandex.market.dsm.domain.courier.test

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import ru.yandex.market.dsm.core.ddd.DsmCommandService
import ru.yandex.market.dsm.domain.courier.command.CourierBaseCommand
import ru.yandex.market.dsm.domain.courier.db.CourierDboRepository
import ru.yandex.market.dsm.domain.courier.model.Courier
import ru.yandex.market.dsm.domain.courier.model.CourierRegistrationStatus
import ru.yandex.market.dsm.domain.courier.model.CourierType
import ru.yandex.market.dsm.domain.courier.service.CourierQueryService
import ru.yandex.market.dsm.external.tracker.selfemployed.model.SelfemployedTrackerTicketInfo
import ru.yandex.market.dsm.test.TestUtil
import java.time.Clock
import java.time.LocalDate
import java.util.UUID

@Service
class CourierTestFactory(
    private val dsmCommandService: DsmCommandService,
    private val courierDboRepository: CourierDboRepository,
    private val courierQueryService: CourierQueryService,
    private val clock: Clock,
) {

    @Transactional
    fun create(
        employerId: String?,
        uid: String,
        email: String = "test@test.ru",
        courierType: CourierType = CourierType.PARTNER,
        deleted: Boolean = false,
    ) = dsmCommandService.handle(
        createCommand(
            employerId = employerId,
            uid = uid,
            email = email,
            courierType = courierType
        )
    ).let {
        if (deleted) {
            courierDboRepository.getOne(it).apply {
                this.deleted = true
                courierDboRepository.save(this)
            }
        }
        courierQueryService.getById(it)
    }

    fun createFromTicket() = TestUtil.OBJECT_GENERATOR.nextObject(SelfemployedTrackerTicketInfo::class.java)

    fun generateCreateCommand(): CourierBaseCommand.Create {
        val result = TestUtil.OBJECT_GENERATOR.nextObject(CourierBaseCommand.Create::class.java)
        result.employerId = null
        result.yaProId = result.yaProId + "_" + "132423436"
        return result
    }

    fun generateCourier(): Courier {
        val command = generateCreateCommand()
        dsmCommandService.handle(command)
        return courierQueryService.getById(command.id)
    }

    fun createCommand(
        employerId: String?,
        uid: String,
        email: String = "test@test.ru",
        courierType: CourierType = CourierType.PARTNER,
    ) = CourierBaseCommand.Create(
        id = UUID.randomUUID().toString(),
        status = null,
        uid = uid,
        employerId = employerId,
        routingId = null,
        workplaceNumber = null,
        deleted = false,
        email = email,
        name = "null",
        lastName = "lastName",
        firstName = "firstName",
        patronymicName = "partro",
        passportNumber = "1234 12345",
        expiredAt = LocalDate.now(clock),
        nationality = null,
        birthday = null,
        phone = null,
        telegramLogin = null,
        vaccinated = true,
        vaccinationLink = "test-link",
        vaccinationsDates = null,
        vaccinationExpiredAt = LocalDate.now(clock),
        courierType = courierType,
        courierRegistrationStatus = CourierRegistrationStatus.REGISTERED,
        yaProId = null,
        balancePersonId = null,
        balanceClientId = null,
        balanceContractId = null,
        createTicket = null,
        passportDateOfIssue = null,
        passportIssuedByName = null,
        passportIssuedByCode = null,
        firedReason = null
    )

}

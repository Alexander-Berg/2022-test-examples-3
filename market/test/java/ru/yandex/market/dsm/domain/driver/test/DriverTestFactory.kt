package ru.yandex.market.dsm.domain.driver.test

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import ru.yandex.market.dsm.core.ddd.DsmCommandService
import ru.yandex.market.dsm.domain.driver.command.DriverBaseCommand
import ru.yandex.market.dsm.domain.driver.service.DriverQueryService
import ru.yandex.mj.generated.server.model.DriverUpsertDto
import ru.yandex.mj.generated.server.model.PersonalDataUpsertDto
import java.time.LocalDate
import java.util.UUID

@Service
class DriverTestFactory(
    private val dsmCommandService: DsmCommandService,
    private val driverQueryService: DriverQueryService,
) {

    @Transactional
    fun create(
        employerId: String?,
        uid: String,
        email: String = "test@test.ru",
        phone: String? = null,
        passportNumber: String? = "1234 12345",
        birthday: LocalDate? = null,
        lastName: String = "lastName",
        firstName: String = "firstName",
        patronymicName: String = "partro",
    ) = dsmCommandService.handle(
        DriverBaseCommand.Create(
            id = UUID.randomUUID().toString(),
            uid = uid,
            employerId = employerId,
            email = email,
            name = "$firstName $lastName $patronymicName",
            lastName = lastName,
            firstName = firstName,
            patronymicName = patronymicName,
            passportNumber = passportNumber,
            issuedAt = LocalDate.EPOCH,
            issuer = "issuer",
            nationality = null,
            birthday = birthday,
            phone = phone,
            telegramLogin = null,
        )
    ).let {
        driverQueryService.findByIdOrThrow(it)
    }

    @Transactional
    fun addLinkToEmployer(driverId: String, employerId: String) {
        dsmCommandService.handle(DriverBaseCommand.AddLinkToEmployer(driverId, employerId))
    }

    @Transactional
    fun addToBlackList(id: String) {
        dsmCommandService.handle(DriverBaseCommand.AddToBlackList(id))
    }

    companion object {

        @JvmStatic
        fun getValidDriverUpsertDto() = DriverUpsertDto().apply {
            this.id = "id"
            this.uid = "uid"
            this.employerId = "employerId"
            this.personalData = PersonalDataUpsertDto().apply {
                this.email = "email"
                this.name = "name"
                this.lastName = "lastName"
                this.firstName = "firstName"
                this.patronymicName = "patronymicName"
                this.passportNumber = "passportNumber"
                this.issuedAt = LocalDate.EPOCH
                this.issuer = "issuer"
                this.nationality = "RUS"
                this.birthday = LocalDate.EPOCH
                this.phone = "phone"
                this.telegramLogin = "telegramLogin"
            }
        }
    }

}

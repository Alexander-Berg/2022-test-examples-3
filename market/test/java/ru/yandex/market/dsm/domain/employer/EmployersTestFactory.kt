package ru.yandex.market.dsm.domain.employer

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import ru.yandex.market.dsm.core.ddd.DsmCommandService
import ru.yandex.market.dsm.domain.employer.command.EmployerBaseCommand
import ru.yandex.market.dsm.domain.employer.model.EmployerBalanceRegistrationStatus
import ru.yandex.market.dsm.domain.employer.model.EmployerType
import ru.yandex.market.dsm.domain.employer.service.EmployerQueryService
import ru.yandex.market.dsm.test.TestUtil
import java.util.Optional
import java.util.UUID

@Service
class EmployersTestFactory {

    @Autowired
    private lateinit var dsmCommandService: DsmCommandService
    @Autowired
    private lateinit var employerQueryService: EmployerQueryService

    @Transactional
    fun createAndSave(
        id: String? = null,
        name: String? = null,
        login: String? = null,
        companyCabinetMbiId: String? = null,
        type: EmployerType? = null,
        active: Boolean = true
    ) = Optional.of(dsmCommandService.handle(createCreateCommand(
        id = id,
        name = name,
        login = login,
        companyCabinetMbiId = companyCabinetMbiId,
        type = type,
        active = active
    )))
        .flatMap { Optional.of(employerQueryService.getById(it)) }
        .orElseThrow()

    fun createCreateCommand(
        id: String? = null,
        name: String? = null,
        login: String? = null,
        companyCabinetMbiId: String? = null,
        type: EmployerType? = null,
        active: Boolean? = null
    ): EmployerBaseCommand.Create {
        val createDto = TestUtil.OBJECT_GENERATOR.nextObject(EmployerBaseCommand.Create::class.java)
        createDto.id = UUID.randomUUID().toString()
        createDto.balanceRegistrationStatus = EmployerBalanceRegistrationStatus.REGISTERED

        Optional.ofNullable(id).map { v -> createDto.id = v }
        Optional.ofNullable(name).map { v -> createDto.name = v }
        Optional.ofNullable(login).map { v -> createDto.login = v }
        Optional.ofNullable(companyCabinetMbiId).map { v -> createDto.companyCabinetMbiId = v }
        Optional.ofNullable(type).map { v -> createDto.type = v }
        Optional.ofNullable(active).map { v -> createDto.isActive = v }

        return createDto
    }
}

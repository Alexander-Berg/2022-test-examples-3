package ru.yandex.market.dsm.dbqueue.balance.company

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.dsm.core.ddd.DsmCommandService
import ru.yandex.market.dsm.core.test.AbstractTest
import ru.yandex.market.dsm.dbqueue.balance.company.handlers.CompanyCreateClientInBalanceHandler
import ru.yandex.market.dsm.dbqueue.balance.company.handlers.CompanyCreateContractInBalanceHandler
import ru.yandex.market.dsm.dbqueue.balance.company.handlers.CompanyCreatePersonInBalanceHandler
import ru.yandex.market.dsm.dbqueue.balance.company.handlers.CompanyLinkIntegrationBalanceHandler
import ru.yandex.market.dsm.domain.employer.EmployersTestFactory
import ru.yandex.market.dsm.domain.employer.model.EmployerBalanceRegistrationStatus
import ru.yandex.market.dsm.domain.employer.service.EmployerQueryService

class CompanyBalanceHandlersTest : AbstractTest() {
    @Autowired
    lateinit var employersTestFactory: EmployersTestFactory
    @Autowired
    lateinit var dsmCommandService: DsmCommandService
    @Autowired
    lateinit var companyCreateClientInBalanceHandler: CompanyCreateClientInBalanceHandler
    @Autowired
    lateinit var employerQueryService: EmployerQueryService
    @Autowired
    lateinit var companyCreateContractInBalanceHandler: CompanyCreateContractInBalanceHandler
    @Autowired
    lateinit var companyCreatePersonInBalanceHandler: CompanyCreatePersonInBalanceHandler
    @Autowired
    lateinit var companyLinkIntegrationBalanceHandler: CompanyLinkIntegrationBalanceHandler

    @Test
    fun createClient() {
        val employerCommand = employersTestFactory.createCreateCommand()
        employerCommand.balanceClientId = null
        employerCommand.balanceRegistrationStatus = EmployerBalanceRegistrationStatus.CREATE_CLIENT
        dsmCommandService.handle(employerCommand)

        companyCreateClientInBalanceHandler.handle(employerQueryService.getById(employerCommand.id))

        val updateEmployer = employerQueryService.getById(employerCommand.id)
        Assertions.assertThat(updateEmployer.balanceClientId).isNotNull
        Assertions.assertThat(updateEmployer.balanceRegistrationStatus).isEqualTo(EmployerBalanceRegistrationStatus.LINK_CONFIGURATION)
    }


    @Test
    fun linkConfiguration() {
        val employerCommand = employersTestFactory.createCreateCommand()
        employerCommand.balanceRegistrationStatus = EmployerBalanceRegistrationStatus.LINK_CONFIGURATION
        dsmCommandService.handle(employerCommand)

        companyLinkIntegrationBalanceHandler.handle(employerQueryService.getById(employerCommand.id))

        val updateEmployer = employerQueryService.getById(employerCommand.id)
        Assertions.assertThat(updateEmployer.balanceRegistrationStatus).isEqualTo(EmployerBalanceRegistrationStatus.CREATE_PERSON)
    }

    @Test
    fun createPerson() {
        val employerCommand = employersTestFactory.createCreateCommand()
        employerCommand.balancePersonId = null
        employerCommand.balanceRegistrationStatus = EmployerBalanceRegistrationStatus.CREATE_PERSON
        dsmCommandService.handle(employerCommand)

        companyCreatePersonInBalanceHandler.handle(employerQueryService.getById(employerCommand.id))

        val updateEmployer = employerQueryService.getById(employerCommand.id)
        Assertions.assertThat(updateEmployer.balancePersonId).isNotNull
        Assertions.assertThat(updateEmployer.balanceRegistrationStatus).isEqualTo(EmployerBalanceRegistrationStatus.CREATE_CONTRACT)
    }

    @Test
    fun createContract() {
        val employerCommand = employersTestFactory.createCreateCommand()
        employerCommand.balanceContractId = null
        employerCommand.balanceRegistrationStatus = EmployerBalanceRegistrationStatus.CREATE_CONTRACT
        dsmCommandService.handle(employerCommand)

        companyCreateContractInBalanceHandler.handle(employerQueryService.getById(employerCommand.id))

        val updateEmployer = employerQueryService.getById(employerCommand.id)
        Assertions.assertThat(updateEmployer.balanceContractId).isNotNull
        Assertions.assertThat(updateEmployer.balanceRegistrationStatus).isEqualTo(EmployerBalanceRegistrationStatus.REGISTERED)
    }

    @Test
    fun updateContract() {
        val employerCommand = employersTestFactory.createCreateCommand()
        val contractId = 5L
        employerCommand.balanceContractId = contractId
        employerCommand.balanceRegistrationStatus = EmployerBalanceRegistrationStatus.CREATE_CONTRACT
        dsmCommandService.handle(employerCommand)

        companyCreateContractInBalanceHandler.handle(employerQueryService.getById(employerCommand.id))

        val updateEmployer = employerQueryService.getById(employerCommand.id)
        Assertions.assertThat(updateEmployer.balanceContractId).isEqualTo(contractId)
        Assertions.assertThat(updateEmployer.balanceRegistrationStatus).isEqualTo(EmployerBalanceRegistrationStatus.REGISTERED)
    }
}

package ru.yandex.market.dsm.dbqueue.balance.selfemployed

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.dsm.core.ddd.DsmCommandService
import ru.yandex.market.dsm.core.test.AbstractTest
import ru.yandex.market.dsm.dbqueue.balance.selfemployed.handlers.SelfEmployedCreateClientInBalanceHandler
import ru.yandex.market.dsm.dbqueue.balance.selfemployed.handlers.SelfEmployedCreateContractInBalanceHandler
import ru.yandex.market.dsm.dbqueue.balance.selfemployed.handlers.SelfEmployedCreatePersonInBalanceHandler
import ru.yandex.market.dsm.dbqueue.balance.selfemployed.handlers.SelfEmployedLinkIntegrationBalanceHandler
import ru.yandex.market.dsm.domain.courier.db.CourierDboRepository
import ru.yandex.market.dsm.domain.courier.test.CourierBalanceDataTestFactory
import ru.yandex.market.dsm.domain.courier.test.CourierTestFactory
import ru.yandex.market.dsm.domain.courier.balance.db.CourierBalanceRegistrationStatus
import ru.yandex.market.dsm.domain.courier.balance.db.CourierBalancePaymentReceiver
import ru.yandex.market.dsm.domain.courier.balance.service.CourierBalanceQueryService
import ru.yandex.market.dsm.domain.employer.EmployersTestFactory
import ru.yandex.market.dsm.domain.employer.service.EmployerService
import ru.yandex.market.dsm.test.TestUtil
import ru.yandex.mj.generated.server.model.EmployerUpsertDto

class SelfEmployedCreateInBalanceHandlerTest : AbstractTest() {
    @Autowired
    private lateinit var selfEmployedCreateClientInBalanceHandler: SelfEmployedCreateClientInBalanceHandler

    @Autowired
    private lateinit var courierBalanceDataTestFactory: CourierBalanceDataTestFactory

    @Autowired
    private lateinit var dsmCommandService: DsmCommandService

    @Autowired
    private lateinit var courierBalanceQueryService: CourierBalanceQueryService

    @Autowired
    private lateinit var selfEmployedLinkIntegrationBalanceHandler: SelfEmployedLinkIntegrationBalanceHandler

    @Autowired
    private lateinit var selfEmployedCreatePersonInBalanceHandler: SelfEmployedCreatePersonInBalanceHandler

    @Autowired
    private lateinit var selfEmployedCreateContractInBalanceHandler: SelfEmployedCreateContractInBalanceHandler

    @Autowired
    private lateinit var courierTestFactory: CourierTestFactory

    @Autowired
    private lateinit var employerTestFactory: EmployersTestFactory

    @Autowired
    private lateinit var courierDboRepository: CourierDboRepository

    @Autowired
    private lateinit var employerService: EmployerService

    @Test
    fun testClientCreate() {
        val upsertEmployer = TestUtil.OBJECT_GENERATOR.nextObject(EmployerUpsertDto::class.java)
        employerService.createEmployer(upsertEmployer)
        val courier = courierTestFactory.createCommand(upsertEmployer.id, "1213749735", "test3359384357390")
        courier.balanceClientId = null
        dsmCommandService.handle(courier)

        val courierCandidateCommand = courierBalanceDataTestFactory.createCommand()
        courierCandidateCommand.balanceRegistrationStatus = CourierBalanceRegistrationStatus.CREATE_CLIENT
        courierCandidateCommand.courierId = courier.id
        val id = dsmCommandService.handle(courierCandidateCommand)

        val courierCandidate = courierBalanceQueryService.getById(id)
        selfEmployedCreateClientInBalanceHandler.handle(courierCandidate)

        val courierCandidateUpdate = courierBalanceQueryService.getById(id)
        assertThat(courierCandidateUpdate.balanceRegistrationStatus)
            .isEqualTo(CourierBalanceRegistrationStatus.LINK_CONFIGURATION)
        assertThat(courierCandidateUpdate.balanceClientId).isNotNull
    }

    @Test
    fun testLinkIntegration() {
        val upsertEmployer = TestUtil.OBJECT_GENERATOR.nextObject(EmployerUpsertDto::class.java)
        employerService.createEmployer(upsertEmployer)
        val courier = courierTestFactory.createCommand(upsertEmployer.id, "12137497351", "test33593843573901")
        courier.balanceClientId = 1L
        dsmCommandService.handle(courier)

        val courierCandidateCommand = courierBalanceDataTestFactory.createCommand()
        courierCandidateCommand.balanceRegistrationStatus = CourierBalanceRegistrationStatus.LINK_CONFIGURATION
        courierCandidateCommand.courierId = courier.id
        val id = dsmCommandService.handle(courierCandidateCommand)

        val courierCandidate = courierBalanceQueryService.getById(id)
        selfEmployedLinkIntegrationBalanceHandler.handle(courierCandidate)

        val courierCandidateUpdate = courierBalanceQueryService.getById(id)
        assertThat(courierCandidateUpdate.balanceRegistrationStatus)
            .isEqualTo(CourierBalanceRegistrationStatus.CREATE_PERSON)
    }

    @Test
    fun testPersonCreate() {
        val upsertEmployer = TestUtil.OBJECT_GENERATOR.nextObject(EmployerUpsertDto::class.java)
        employerService.createEmployer(upsertEmployer)
        val courier = courierTestFactory.createCommand(upsertEmployer.id, "121374973511", "test335938435739011")
        courier.balanceClientId = 1L
        dsmCommandService.handle(courier)

        val courierCandidateCommand = courierBalanceDataTestFactory.createCommand()
        courierCandidateCommand.balanceRegistrationStatus = CourierBalanceRegistrationStatus.CREATE_PERSON
        courierCandidateCommand.courierId = courier.id
        courierCandidateCommand.paymentReceiver = CourierBalancePaymentReceiver.BANK
        val id = dsmCommandService.handle(courierCandidateCommand)

        val courierCandidate = courierBalanceQueryService.getById(id)
        selfEmployedCreatePersonInBalanceHandler.handle(courierCandidate)

        val courierCandidateUpdate = courierBalanceQueryService.getById(id)
        assertThat(courierCandidateUpdate.balanceRegistrationStatus)
            .isEqualTo(CourierBalanceRegistrationStatus.CREATE_CONTRACT)
        assertThat(courierCandidateUpdate.balancePersonId).isNotNull
    }


    @Test
    fun testContractCreate() {
        val uid = "35649777359"
        val balanceClientId = 24363463L
        val balancePersonId = 3465363L
        val employer = employerTestFactory.createAndSave()
        val courierCommand = courierTestFactory.createCommand(employer.id, uid, "TEST-AAAghjfs")
        courierCommand.balanceClientId = balanceClientId
        courierCommand.balancePersonId = balancePersonId
        courierCommand.balanceContractId = null
        val courierId = dsmCommandService.handle(courierCommand)

        val courierCandidateCommand = courierBalanceDataTestFactory.createCommand()
        courierCandidateCommand.balanceRegistrationStatus = CourierBalanceRegistrationStatus.CREATE_CONTRACT
        courierCandidateCommand.courierId = courierId
        val id = dsmCommandService.handle(courierCandidateCommand)

        val courierCandidate = courierBalanceQueryService.getById(id)
        selfEmployedCreateContractInBalanceHandler.handle(courierCandidate)

        val courierCandidateUpdate = courierBalanceQueryService.getById(id)
        assertThat(courierCandidateUpdate.balanceRegistrationStatus)
            .isEqualTo(CourierBalanceRegistrationStatus.REGISTERED)
        assertThat(courierCandidateUpdate.balanceContractId).isNotNull

        val courier = courierDboRepository.findById(courierId).orElseThrow()
        assertThat(courier.balancePersonId).isEqualTo(balancePersonId)
        assertThat(courier.balanceClientId).isEqualTo(balanceClientId)
        assertThat(courier.balanceContractId).isEqualTo(courierCandidateUpdate.balanceContractId)
    }


    @Test
    fun testContractUpdate() {
        val balanceContractId = 111L
        val upsertEmployer = TestUtil.OBJECT_GENERATOR.nextObject(EmployerUpsertDto::class.java)
        employerService.createEmployer(upsertEmployer)
        val courierFirst = courierTestFactory.createCommand(upsertEmployer.id, "12137567", "test3359385677556")
        courierFirst.balanceClientId = 1L
        courierFirst.balancePersonId = 1L
        courierFirst.balanceContractId = balanceContractId
        dsmCommandService.handle(courierFirst)

        val courierCandidateCommand = courierBalanceDataTestFactory.createCommand()
        courierCandidateCommand.balanceRegistrationStatus = CourierBalanceRegistrationStatus.CREATE_CONTRACT
        courierCandidateCommand.courierId = courierFirst.id
        val id = dsmCommandService.handle(courierCandidateCommand)

        val courierCandidate = courierBalanceQueryService.getById(id)
        selfEmployedCreateContractInBalanceHandler.handle(courierCandidate)

        val courierCandidateUpdate = courierBalanceQueryService.getById(id)
        assertThat(courierCandidateUpdate.balanceRegistrationStatus)
            .isEqualTo(CourierBalanceRegistrationStatus.REGISTERED)
        assertThat(courierCandidateUpdate.balanceContractId).isEqualTo(balanceContractId)
    }
}



package ru.yandex.market.dsm.domain.courier.balance

import org.assertj.core.api.AssertionsForClassTypes.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.dsm.core.ddd.DsmCommandService
import ru.yandex.market.dsm.core.test.AbstractTest
import ru.yandex.market.dsm.domain.courier.balance.db.CourierBalanceDataDboRepository
import ru.yandex.market.dsm.domain.courier.balance.db.CourierBalancePaymentReceiver
import ru.yandex.market.dsm.domain.courier.test.CourierBalanceDataTestFactory
import ru.yandex.market.dsm.domain.courier.test.CourierTestFactory
import ru.yandex.market.dsm.domain.employer.service.EmployerService
import ru.yandex.market.dsm.test.TestUtil
import ru.yandex.mj.generated.server.model.EmployerUpsertDto

class CreateCourierBalanceDataHandlerTest : AbstractTest() {
    @Autowired
    private lateinit var dsmCommandService: DsmCommandService

    @Autowired
    private lateinit var courierBalanceDataDboRepository: CourierBalanceDataDboRepository

    @Autowired
    private lateinit var courierBalanceDataTestFactory: CourierBalanceDataTestFactory

    @Autowired
    private lateinit var employerService: EmployerService

    @Autowired
    private lateinit var courierTestFactory: CourierTestFactory

    @Test
    fun testCreateCourierCandidate() {
        val upsertEmployer = TestUtil.OBJECT_GENERATOR.nextObject(EmployerUpsertDto::class.java)
        employerService.createEmployer(upsertEmployer)
        val courierFirst = courierTestFactory.generateCreateCommand()
        dsmCommandService.handle(courierFirst)

        val createCommand = courierBalanceDataTestFactory.createCommand()
        createCommand.courierId = courierFirst.id
        dsmCommandService.handle(createCommand)

        val courierCandidateOpt = courierBalanceDataDboRepository.findById(createCommand.id)
        assertThat(courierCandidateOpt).isPresent

        val courierCandidate = courierCandidateOpt.get()
        assertThat(courierCandidate.id).isEqualTo(createCommand.id)
    }

    @Test
    fun testUpdateBalanceData() {
        val upsertEmployer = TestUtil.OBJECT_GENERATOR.nextObject(EmployerUpsertDto::class.java)
        employerService.createEmployer(upsertEmployer)
        val courierFirst = courierTestFactory.generateCreateCommand()
        dsmCommandService.handle(courierFirst)

        val createCommand = courierBalanceDataTestFactory.createCommand()
        createCommand.courierId = courierFirst.id
        dsmCommandService.handle(createCommand)

        val updateBalanceData = courierBalanceDataTestFactory.updateBalanceData()
        updateBalanceData.id = createCommand.id
        updateBalanceData.paymentReceiver = CourierBalancePaymentReceiver.SBERBANK

        dsmCommandService.handle(
            updateBalanceData
        )

        val updateCandidateOpt = courierBalanceDataDboRepository.findById(createCommand.id)
        assertThat(updateCandidateOpt).isPresent
        val updateCandidate = updateCandidateOpt.get()

        assertThat(updateCandidate.pfr).isEqualTo(updateBalanceData.pfr)
        assertThat(updateCandidate.inn).isEqualTo(updateBalanceData.inn)
        assertThat(updateCandidate.personBankAccount).isEqualTo(updateBalanceData.personBankAccount)

        assertThat(updateCandidate.legalAddressPostCode).isEqualTo(updateBalanceData.legalAddressPostCode)
        assertThat(updateCandidate.legalAddressGni).isEqualTo(updateBalanceData.legalAddressGni)
        assertThat(updateCandidate.legalAddressRegion).isEqualTo(updateBalanceData.legalAddressRegion)
        assertThat(updateCandidate.legalAddressCity).isEqualTo(updateBalanceData.legalAddressCity)
        assertThat(updateCandidate.legalAddressStreet).isEqualTo(updateBalanceData.legalAddressStreet)
        assertThat(updateCandidate.legalAddressHome).isEqualTo(updateBalanceData.legalAddressHome)
        assertThat(updateCandidate.legalFiasGuid).isEqualTo(updateBalanceData.legalFiasGuid)

        assertThat(updateCandidate.addressGni).isEqualTo(updateBalanceData.addressGni)
        assertThat(updateCandidate.addressRegion).isEqualTo(updateBalanceData.addressRegion)
        assertThat(updateCandidate.postCode).isEqualTo(updateBalanceData.postCode)
        assertThat(updateCandidate.addressCode).isEqualTo(updateBalanceData.addressCode)

        assertThat(updateCandidate.bankBik).isEqualTo(updateBalanceData.bankBik)

        assertThat(updateCandidate.paymentReceiver).isEqualTo(updateBalanceData.paymentReceiver)
    }
}

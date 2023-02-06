package ru.yandex.market.dsm.domain.courier.balance

import org.assertj.core.api.AssertionsForClassTypes
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.dsm.core.ddd.DsmCommandService
import ru.yandex.market.dsm.core.test.AbstractTest
import ru.yandex.market.dsm.domain.courier.command.CourierBaseCommand
import ru.yandex.market.dsm.domain.courier.test.CourierBalanceDataTestFactory
import ru.yandex.market.dsm.domain.courier.test.CourierTestFactory
import ru.yandex.market.dsm.domain.courier.balance.command.CourierBalanceDataBaseCommand
import ru.yandex.market.dsm.domain.courier.balance.model.CourierBalanceData
import ru.yandex.market.dsm.domain.courier.balance.service.CourierBalanceQueryService
import ru.yandex.market.dsm.domain.employer.service.EmployerService
import ru.yandex.market.dsm.test.TestUtil
import ru.yandex.mj.generated.server.model.EmployerUpsertDto
import java.time.format.DateTimeFormatter

class CourierBalanceQueryServiceTest : AbstractTest() {
    @Autowired
    private lateinit var courierBalanceQueryService: CourierBalanceQueryService

    @Autowired
    private lateinit var courierBalanceDataTestFactory: CourierBalanceDataTestFactory

    @Autowired
    private lateinit var dsmCommandService: DsmCommandService

    @Autowired
    private lateinit var employerService: EmployerService

    @Autowired
    private lateinit var courierTestFactory: CourierTestFactory

    private val FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    @Test
    fun testCourierCandidateFind() {
        val upsertEmployer = TestUtil.OBJECT_GENERATOR.nextObject(EmployerUpsertDto::class.java)
        employerService.createEmployer(upsertEmployer)
        val courierFirst = courierTestFactory.generateCreateCommand()
        dsmCommandService.handle(courierFirst)

        val createCommand = courierBalanceDataTestFactory.createCommand()
        createCommand.courierId = courierFirst.id
        dsmCommandService.handle(createCommand)

        val courierBalance = courierBalanceQueryService.getById(createCommand.id)

        assertCourierBalance(courierBalance, courierFirst, createCommand)
    }

    @Test
    fun testCourierCandidateFindByCourierId() {
        val upsertEmployer = TestUtil.OBJECT_GENERATOR.nextObject(EmployerUpsertDto::class.java)
        employerService.createEmployer(upsertEmployer)
        val courierFirst = courierTestFactory.generateCreateCommand()
        dsmCommandService.handle(courierFirst)

        val createCommand = courierBalanceDataTestFactory.createCommand()
        createCommand.courierId = courierFirst.id
        dsmCommandService.handle(createCommand)

        val courierBalance = courierBalanceQueryService.getByCourierId(createCommand.courierId)

        assertCourierBalance(courierBalance!!, courierFirst, createCommand)
    }

    private fun assertCourierBalance(
        courierBalance: CourierBalanceData, courier: CourierBaseCommand.Create,
        create: CourierBalanceDataBaseCommand.Create
    ) {
        AssertionsForClassTypes.assertThat(courierBalance.id).isEqualTo(create.id)
        AssertionsForClassTypes.assertThat(courierBalance.uid).isEqualTo(courier.uid)
        AssertionsForClassTypes.assertThat(courierBalance.email).isEqualTo(courier.email)
        AssertionsForClassTypes.assertThat(courierBalance.lastName).isEqualTo(courier.lastName)
        AssertionsForClassTypes.assertThat(courierBalance.firstName).isEqualTo(courier.firstName)
        AssertionsForClassTypes.assertThat(courierBalance.patronymicName).isEqualTo(courier.patronymicName)
        AssertionsForClassTypes.assertThat(courierBalance.passportNumber).isEqualTo(courier.passportNumber)
        AssertionsForClassTypes.assertThat(courierBalance.phone).isEqualTo(courier.phone)

        AssertionsForClassTypes.assertThat(courierBalance.pfr).isEqualTo(create.pfr)
        AssertionsForClassTypes.assertThat(courierBalance.inn).isEqualTo(create.inn)
        AssertionsForClassTypes.assertThat(courierBalance.personBankAccount).isEqualTo(create.personBankAccount)

        AssertionsForClassTypes.assertThat(courierBalance.legalAddressPostCode).isEqualTo(create.legalAddressPostCode)
        AssertionsForClassTypes.assertThat(courierBalance.legalAddressGni).isEqualTo(create.legalAddressGni)
        AssertionsForClassTypes.assertThat(courierBalance.legalAddressRegion).isEqualTo(create.legalAddressRegion)
        AssertionsForClassTypes.assertThat(courierBalance.legalAddressCity).isEqualTo(create.legalAddressCity)
        AssertionsForClassTypes.assertThat(courierBalance.legalAddressStreet).isEqualTo(create.legalAddressStreet)
        AssertionsForClassTypes.assertThat(courierBalance.legalAddressHome).isEqualTo(create.legalAddressHome)
        AssertionsForClassTypes.assertThat(courierBalance.legalFiasGuid).isEqualTo(create.legalFiasGuid)

        AssertionsForClassTypes.assertThat(courierBalance.addressGni).isEqualTo(create.addressGni)
        AssertionsForClassTypes.assertThat(courierBalance.addressRegion).isEqualTo(create.addressRegion)
        AssertionsForClassTypes.assertThat(courierBalance.postCode).isEqualTo(create.postCode)
        AssertionsForClassTypes.assertThat(courierBalance.addressCode).isEqualTo(create.addressCode)

        AssertionsForClassTypes.assertThat(courierBalance.birthday).isEqualTo(courier.birthday?.format(FORMATTER))

        AssertionsForClassTypes.assertThat(courierBalance.balanceClientId).isEqualTo(courier.balanceClientId)
        AssertionsForClassTypes.assertThat(courierBalance.balancePersonId).isEqualTo(courier.balancePersonId)
        AssertionsForClassTypes.assertThat(courierBalance.balanceContractId).isEqualTo(courier.balanceContractId)


        AssertionsForClassTypes.assertThat(courierBalance.paymentReceiver).isEqualTo(create.paymentReceiver)
    }
}


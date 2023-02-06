package ru.yandex.market.dsm.domain.courier.test

import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.dsm.core.ddd.DsmCommandService
import ru.yandex.market.dsm.core.test.AbstractTest
import ru.yandex.market.dsm.domain.courier.command.CourierBaseCommand
import ru.yandex.market.dsm.domain.courier.db.CourierDboRepository

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import ru.yandex.market.dsm.domain.employer.EmployersTestFactory

class CourierBalanceIdUpdateTest(): AbstractTest() {
    @Autowired
    private lateinit var dsmCommandService: DsmCommandService

    @Autowired
    private lateinit var courierTestFactory: CourierTestFactory

    @Autowired
    private lateinit var employersTestFactory: EmployersTestFactory

    @Autowired
    private lateinit var courierDboRepository: CourierDboRepository

    @Test
    fun updateBalanceId() {
        val employer = employersTestFactory.createAndSave()

        val courierCommand = courierTestFactory.createCommand(employer.id, "27365455", "email")
        courierCommand.balanceClientId = null
        courierCommand.balanceContractId = null
        courierCommand.balancePersonId = null
        val id = dsmCommandService.handle(courierCommand)

        val balanceClientId = 94935464439L
        val balanceContractId = 23798459L
        val balancePersonId = 42738592L
        dsmCommandService.handle(CourierBaseCommand.UpdateBalanceId(id, balanceClientId, balancePersonId, balanceContractId))

        val updateCourier = courierDboRepository.findById(id).orElseThrow()
        assertThat(balanceClientId).isEqualTo(updateCourier.balanceClientId)
        assertThat(balanceContractId).isEqualTo(updateCourier.balanceContractId)
        assertThat(balancePersonId).isEqualTo(updateCourier.balancePersonId)
    }
}

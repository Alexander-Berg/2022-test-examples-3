package ru.yandex.market.dsm.domain.courier.test

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.dsm.core.ddd.DsmCommandService
import ru.yandex.market.dsm.core.test.AbstractTest
import ru.yandex.market.dsm.domain.courier.command.CourierBaseCommand
import ru.yandex.market.dsm.domain.courier.db.CourierDboRepository

import org.assertj.core.api.Assertions.assertThat
import ru.yandex.market.dsm.domain.employer.EmployersTestFactory
import ru.yandex.market.dsm.domain.employer.model.EmployerType

class UpdateCourierYaProIdHandlerTest : AbstractTest() {
    @Autowired
    private lateinit var dsmCommandService: DsmCommandService

    @Autowired
    private lateinit var courierTestFactory: CourierTestFactory

    @Autowired
    private lateinit var employerTestFactory: EmployersTestFactory

    @Autowired
    private lateinit var courierDboRepository: CourierDboRepository

    @Test
    fun updateYaProId() {
        val employerId = "478532956729"
        employerTestFactory.createAndSave(id = employerId, name = "685912345", login = "37549027502",
        type = EmployerType.SUPPLY, active = false)
        val courier =
            courierTestFactory.create(employerId = employerId, uid = "97848257", email = "4856789788",
                deleted = false)
        val yaProId = "747398567_5875"
        dsmCommandService.handle(CourierBaseCommand.UpdateYaProId(courier.id, yaProId))
        val updateCourierOpt = courierDboRepository.findById(courier.id)
        assertThat(updateCourierOpt).isPresent
        val updateCourier = updateCourierOpt.get()
        assertThat(updateCourier.yaProId).isEqualTo(yaProId)
    }
}


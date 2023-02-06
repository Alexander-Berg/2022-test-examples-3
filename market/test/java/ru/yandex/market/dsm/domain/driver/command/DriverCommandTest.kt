package ru.yandex.market.dsm.domain.driver.command

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.dsm.core.ddd.DsmCommandService
import ru.yandex.market.dsm.core.test.AbstractTest
import ru.yandex.market.dsm.domain.employer.EmployersTestFactory
import ru.yandex.market.dsm.domain.employer.db.EmployerDboRepository
import ru.yandex.market.dsm.domain.employer.model.EmployerType

class DriverCommandTest: AbstractTest() {

    @Autowired
    private lateinit var repository: EmployerDboRepository

    @Autowired
    private lateinit var dsmCommandService: DsmCommandService

    @Autowired
    private lateinit var employersTestFactory: EmployersTestFactory

    @Test
    fun `create driver`() {
        val employer = employersTestFactory.createAndSave(
            id = "4857676789", name = "NAME37484973789",
            login = "LOGIN475489487", type = EmployerType.SUPPLY, active = true
        )

    }
}

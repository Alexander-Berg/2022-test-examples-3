package ru.yandex.market.dsm.domain.employer.command

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.dsm.core.test.AbstractTest
import ru.yandex.market.dsm.domain.employer.db.EmployerDboRepository
import ru.yandex.market.dsm.domain.employer.model.EmployerLegalForm
import ru.yandex.market.dsm.domain.employer.model.EmployerType
import ru.yandex.market.dsm.core.ddd.DsmCommandService
import ru.yandex.market.dsm.domain.employer.EmployersTestFactory
import java.util.UUID

class EmployerBaseCommandTest : AbstractTest() {

    @Autowired
    private lateinit var repository: EmployerDboRepository

    @Autowired
    private lateinit var dsmCommandService: DsmCommandService

    @Autowired
    private lateinit var employersTestFactory: EmployersTestFactory

    @Test
    fun `create Employer - success`() {
        //when
        val command = employersTestFactory.createCreateCommand()
        command.name = "test-3243901";
        command.login = "login-test-23013201"

        val entityId = dsmCommandService.handle(command)

        //then
        val resultOpt = repository.findById(command.id)

        Assertions.assertThat(resultOpt.isPresent).isTrue
        val result = resultOpt.get()

        Assertions.assertThat(result.id).isEqualTo(command.id)
        Assertions.assertThat(result.createdAt).isNotNull
        Assertions.assertThat(result.updatedAt).isNotNull
        Assertions.assertThat(result.name).isEqualTo(command.name)
        Assertions.assertThat(entityId).isEqualTo(command.id)
    }


    @Test
    fun `update Employer - success`() {
        val command = employersTestFactory.createCreateCommand()
        command.name = "test-324390";
        command.login = "login-test-2301320"

        dsmCommandService.handle(command)

        val expectedNewName = "new name"
        Assertions.assertThat(command.name).isNotEqualTo(expectedNewName);

        //when
        dsmCommandService.handle(EmployerBaseCommand.UpdateName(command.id, expectedNewName))

        //then
        val resultOpt = repository.findById(command.id)

        Assertions.assertThat(resultOpt.isPresent).isTrue
        val result = resultOpt.get()
        Assertions.assertThat(result.name).isEqualTo(expectedNewName)
    }
}

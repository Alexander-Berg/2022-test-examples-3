package ru.yandex.market.dsm.core.ddd

import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.dsm.core.test.AbstractTest
import ru.yandex.market.dsm.domain.employer.db.EmployerDbo
import ru.yandex.market.dsm.external.ddd.BaseCommandWithId
import ru.yandex.market.dsm.external.ddd.exception.CommandHandlerNotFoundException

class DsmCommandServiceTest : AbstractTest() {
    @Autowired
    private lateinit var dsmCommandService: DsmCommandService

    @Test
    fun `Unsupported command (handler not found) - failure`() {
        assertThrows(
            CommandHandlerNotFoundException::class.java
        ) { dsmCommandService.handle(UnsupportedCommand()) }
    }

    class UnsupportedCommand : BaseCommandWithId<EmployerDbo> {
        override fun getEntityType() = EmployerDbo::class.java
        override fun getAggregateId() = "id"
    }
}

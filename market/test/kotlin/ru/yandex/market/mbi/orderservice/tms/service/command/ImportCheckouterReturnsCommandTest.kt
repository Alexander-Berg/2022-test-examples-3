package ru.yandex.market.mbi.orderservice.tms.service.command

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.kotlin.argThat
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.mbi.orderservice.common.commandInvocationTest
import ru.yandex.market.mbi.orderservice.tms.FunctionalTest
import ru.yandex.market.mbi.orderservice.common.service.pg.EnvironmentService
import ru.yandex.market.mbi.orderservice.tms.service.yt.events.CheckouterReturnYtConsumer

class ImportCheckouterReturnsCommandTest : FunctionalTest() {

    @Autowired
    lateinit var environmentService: EnvironmentService

    @Test
    fun commandTest() {
        val checkouterConsumer = mock<CheckouterReturnYtConsumer> {}
        val importCheckouterReturnsCommand =
            ImportCheckouterReturnsCommand(environmentService, checkouterConsumer)

        commandInvocationTest(
            importCheckouterReturnsCommand,
            """checkouter-returns-import --fromReturnId=13 --toReturnId=13"""
        )

        verify(checkouterConsumer)
            .importCheckouterReturns(argThat {
                assertThat(fromReturnId).isEqualTo(13)
                assertThat(toReturnId).isEqualTo(13)
                true
            })
    }
}

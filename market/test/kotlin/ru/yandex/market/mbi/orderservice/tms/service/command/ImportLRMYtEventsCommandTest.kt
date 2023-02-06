package ru.yandex.market.mbi.orderservice.tms.service.command

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.kotlin.argThat
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.mbi.orderservice.common.commandInvocationTest
import ru.yandex.market.mbi.orderservice.common.persistence.repository.yt.YtReturnRepository
import ru.yandex.market.mbi.orderservice.common.service.pg.EnvironmentService
import ru.yandex.market.mbi.orderservice.tms.FunctionalTest
import ru.yandex.market.mbi.orderservice.tms.service.yt.events.BATCH_SIZE
import ru.yandex.market.mbi.orderservice.tms.service.yt.events.LogisticDeadLetterQueueProcessor

class ImportLRMYtEventsCommandTest : FunctionalTest() {

    @Autowired
    lateinit var environmentService: EnvironmentService

    @Autowired
    lateinit var returnRepository: YtReturnRepository

    @Test
    fun commandTest() {
        val logisticReturnEventsImport = mock<LogisticDeadLetterQueueProcessor> { }
        val importLRMYtEventsCommand =
            ImportLRMYtEventsCommand(
                environmentService,
                logisticDeadLetterQueueProcessor = logisticReturnEventsImport
            )

        commandInvocationTest(
            importLRMYtEventsCommand,
            """lrm-import-events --fromOrderId=13 --toOrderId=13"""
        )

        verify(logisticReturnEventsImport)
            .runImport(argThat {
                assertThat(fromOrderId).isEqualTo(13)
                assertThat(toOrderId).isEqualTo(13)
                assertThat(batchSize).isEqualTo(BATCH_SIZE)
                true
            })
    }
}

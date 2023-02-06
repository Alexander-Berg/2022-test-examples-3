package ru.yandex.market.abo.core.call.transcription

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.transaction.support.TransactionTemplate
import ru.yandex.EmptyTest
import ru.yandex.market.abo.bpmn.client.AboBpmnClient
import ru.yandex.market.abo.bpmn.client.AboBpmnProcessClient
import ru.yandex.market.abo.core.communication.proxy.CommunicationProxyService
import ru.yandex.market.abo.generated.client.communication_proxy.model.CallInfoDto
import ru.yandex.market.abo.generated.client.communication_proxy.model.CallsResponse
import ru.yandex.market.abo.util.db.batch.PgBatchUpdater
import ru.yandex.market.util.db.ConfigurationService
import java.time.OffsetDateTime

/**
 * @author zilzilok
 */
class CallTranscriptionTaskManagerTest @Autowired constructor(
    private val callTranscriptionTaskBatchUpdater: PgBatchUpdater<CallTranscriptionTask>,
    private val callTranscriptionTaskRepo: CallTranscriptionTaskRepo,
    transactionTemplate: TransactionTemplate
) : EmptyTest() {

    private val communicationProxyService: CommunicationProxyService = mock()
    private val coreCounterService: ConfigurationService = mock()
    private val aboBpmnProcessClient: AboBpmnProcessClient = mock()
    private val aboBpmnClient = AboBpmnClient(aboBpmnProcessClient, mock())
    private val callTranscriptionManager = CallTranscriptionTaskManager(
        communicationProxyService,
        callTranscriptionTaskBatchUpdater,
        coreCounterService,
        transactionTemplate,
        callTranscriptionTaskRepo,
        aboBpmnClient
    )

    @Test
    fun loadAndCreateNewTasks() {
        whenever(communicationProxyService.getCalls(any()))
            .thenReturn(
                CallsResponse()
                    .calls(listOf(
                        CallInfoDto().orderId(1L).recordId(RECORD_ID_1).started(STARTED).ended(STARTED.plusSeconds(40)),
                        CallInfoDto().orderId(2L).recordId(RECORD_ID_1).started(STARTED).ended(STARTED.plusSeconds(40)),
                        CallInfoDto().orderId(2L).recordId(RECORD_ID_2).started(STARTED).ended(STARTED.plusSeconds(20)),
                    ))
                .pageNum(0)
                .pageSize(3)
                .totalPages(1)
                .totalElements(3)
        )
        callTranscriptionTaskBatchUpdater.insertWithoutUpdate(listOf(CallTranscriptionTask(RECORD_ID_1, 1L, 40)))

        callTranscriptionManager.loadAndCreateNewTasks()

        val tasks = callTranscriptionTaskRepo.findAll()

        assertEquals(1, tasks.size)
        assertTrue(tasks.map { it.recordId }.containsAll(listOf(RECORD_ID_1)))
    }

    @Test
    fun executeTasks() {
        callTranscriptionTaskBatchUpdater.insertWithoutUpdate(listOf(CallTranscriptionTask(RECORD_ID_1, 1L, 40)))

        callTranscriptionManager.executeTasks(1)
        flushAndClear()

        verify(aboBpmnProcessClient).startProcess(any())
        assertNotNull(callTranscriptionTaskRepo.findAll().first().processedTime)
    }

    companion object {
        private const val RECORD_ID_1 = "00000000-0000-0000-0000-000000000000"
        private const val RECORD_ID_2 = "11111111-1111-1111-1111-111111111111"
        private val STARTED = OffsetDateTime.now()
    }
}

package ru.yandex.market.logistics.mqm.queue.consumer

import com.fasterxml.jackson.databind.ObjectMapper
import com.github.springtestdbunit.annotation.ExpectedDatabase
import com.github.springtestdbunit.assertion.DatabaseAssertionMode
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.transaction.support.TransactionOperations
import ru.yandex.market.logistics.mqm.AbstractContextualTest
import ru.yandex.market.logistics.mqm.queue.base.QueueRegister
import ru.yandex.market.logistics.mqm.queue.dto.LomOrderIdBarcodeDto
import ru.yandex.market.logistics.mqm.queue.task.TaskType
import ru.yandex.market.logistics.mqm.service.FailedQueueTaskService
import ru.yandex.money.common.dbqueue.api.Task
import ru.yandex.money.common.dbqueue.config.QueueShardId

@DisplayName("Проверка сохранения упавших тасок в БД")
class ConsumerSavingTasksTest : AbstractContextualTest() {
    @Autowired
    private lateinit var queueRegister: QueueRegister

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Autowired
    private lateinit var failedQueueTaskService: FailedQueueTaskService

    @Autowired
    private lateinit var transactionOperations: TransactionOperations

    @ExpectedDatabase(
        value = "/queue/consumer/after/consumer_saving_tasks_test/with_logged_queue_task.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @Test
    @DisplayName("Проверка сохранения в БД")
    fun savingFailedTaskToDbTest() {
        ConsumerWithException(
            queueRegister,
            objectMapper,
            failedQueueTaskService,
            transactionOperations
        ).execute(
            Task.builder<LomOrderIdBarcodeDto>(QueueShardId("QUEUE_SHARD_ID"))
                .withAttemptsCount(10)
                .withTotalAttemptsCount(10)
                .withPayload(LomOrderIdBarcodeDto(123L, "orderId"))
                .build()
        )
    }

    class ConsumerWithException(
        queueRegister: QueueRegister,
        objectMapper: ObjectMapper,
        failedQueueTaskService: FailedQueueTaskService,
        transactionOperations: TransactionOperations,
    ) : BaseQueueConsumer<LomOrderIdBarcodeDto>(
        queueRegister,
        objectMapper,
        TaskType.COURIER_SHIFT_FINISHED_EVENT,
        failedQueueTaskService,
        transactionOperations,
    ) {
        override fun processPayload(payload: LomOrderIdBarcodeDto) = throw Exception()
    }
}

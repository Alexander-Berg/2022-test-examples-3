package ru.yandex.market.logistics.cte.dbqueue.consumer

import com.fasterxml.jackson.databind.ObjectMapper
import com.github.springtestdbunit.annotation.DatabaseSetup
import com.github.springtestdbunit.annotation.ExpectedDatabase
import com.github.springtestdbunit.annotation.ExpectedDatabases
import com.github.springtestdbunit.assertion.DatabaseAssertionMode
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.logistics.cte.base.IntegrationTest
import ru.yandex.market.logistics.cte.dbqueue.payload.GenerateAscEnrichmentTasksPayload
import ru.yandex.market.logistics.cte.dbqueue.producer.AscEnrichOrderReceiptProducer
import ru.yandex.market.logistics.cte.service.ServiceCenterItemsToSendService
import ru.yandex.market.logistics.dbqueue.registry.DbQueueConfigRegistry
import ru.yandex.market.logistics.dbqueue.service.DbQueueLogService
import ru.yandex.money.common.dbqueue.api.Task
import ru.yandex.money.common.dbqueue.config.QueueShard
import java.time.Clock
import java.time.ZonedDateTime

internal class GenerateAscEnrichmentTasksConsumerTest(
    @Autowired private val dbQueueConfigRegistry: DbQueueConfigRegistry,
    @Autowired private val objectMapper: ObjectMapper,
    @Autowired private val dbQueueLogService: DbQueueLogService,
    @Autowired private val ascEnrichOrderReceiptProducer: AscEnrichOrderReceiptProducer,
    @Autowired private val serviceCenterItemsToSendService: ServiceCenterItemsToSendService,
    @Autowired private val queueShard: QueueShard,
    @Autowired private val clock: Clock,
): IntegrationTest() {
    private val generateAscEnrichmentTasksConsumer: GenerateAscEnrichmentTasksConsumer =
        GenerateAscEnrichmentTasksConsumer(
            dbQueueConfigRegistry, objectMapper, dbQueueLogService,
            serviceCenterItemsToSendService, listOf(ascEnrichOrderReceiptProducer)
        )

    @Test
    @DatabaseSetup(
        value = ["classpath:dbqueue/consumer/generate-asc-enrichment-tasks/before.xml"],
        connection = "dbqueueDatabaseConnection"

    )
    @ExpectedDatabases(
        ExpectedDatabase(
            value = "classpath:dbqueue/consumer/generate-asc-enrichment-tasks/after.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED,
            connection = "dbqueueDatabaseConnection"
        ),
        ExpectedDatabase(
            value = "classpath:dbqueue/consumer/generate-asc-enrichment-tasks/after_enrichment_task.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED,
            connection = "dbUnitDatabaseConnection"
        ),
    )
    fun shouldSuccessfullyGenerateTasks() {
        val task = Task.builder<GenerateAscEnrichmentTasksPayload>(queueShard.shardId)
            .withCreatedAt(ZonedDateTime.now(clock))
            .withPayload(GenerateAscEnrichmentTasksPayload(1L, listOf(1L, 2L)))
            .build()
        generateAscEnrichmentTasksConsumer.execute(task)
    }

}

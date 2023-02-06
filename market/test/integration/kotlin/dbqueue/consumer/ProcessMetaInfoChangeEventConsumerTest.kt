package ru.yandex.market.logistics.calendaring.dbqueue.consumer

import com.github.springtestdbunit.annotation.DatabaseSetup
import com.github.springtestdbunit.annotation.DatabaseSetups
import com.github.springtestdbunit.annotation.ExpectedDatabase
import com.github.springtestdbunit.assertion.DatabaseAssertionMode
import org.junit.Assert.assertNotNull
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.logistics.calendaring.base.AbstractContextualTest
import ru.yandex.market.logistics.calendaring.dbqueue.payload.ProcessMetaInfoChangeEventPayload
import ru.yandex.market.logistics.calendaring.repository.MetaInfoChangeEventRepository
import ru.yandex.market.logistics.calendaring.repository.MetaInfoRepository
import ru.yandex.market.logistics.test.integration.jpa.JpaQueriesCount
import ru.yandex.money.common.dbqueue.api.Task
import ru.yandex.money.common.dbqueue.config.QueueShardId

open class ProcessMetaInfoChangeEventConsumerTest(
    @Autowired private val metaInfoChangeEventConsumer: ProcessMetaInfoChangeEventConsumer,
    @Autowired private val metaInfoRepository: MetaInfoRepository,
    @Autowired private val metaInfoChangeEventRepository: MetaInfoChangeEventRepository,

    ): AbstractContextualTest() {

    val EXTERNAL_ID_VALID = "538440"
    val SOURCE_FFWF = "FFWF"

    @Test
    @DatabaseSetup(value = ["classpath:fixtures/dbqueue/consumer/process-meta-info-events/create-one-booking-meta/before.xml"])
    @ExpectedDatabase(
        value = "classpath:fixtures/dbqueue/consumer/process-meta-info-events/create-one-booking-meta/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT,
    )
    fun createOneBookingMeta() {
        val payload = ProcessMetaInfoChangeEventPayload(EXTERNAL_ID_VALID, SOURCE_FFWF, null, "1")

        val task: Task<ProcessMetaInfoChangeEventPayload> = Task.builder<ProcessMetaInfoChangeEventPayload>(QueueShardId("test"))
            .withPayload(payload)
            .build()

        metaInfoChangeEventConsumer.execute(task)
    }

    @Test
    @DatabaseSetups(DatabaseSetup(value = ["classpath:fixtures/dbqueue/consumer/process-meta-info-events/create-one-booking-meta/before.xml"]))
    @ExpectedDatabase(
        value = "classpath:fixtures/dbqueue/consumer/process-meta-info-events/create-one-booking-meta/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT,
    )
    fun createOneBookingMetaByBookingIdDefaultMapper() {
        val payload = ProcessMetaInfoChangeEventPayload("538440", "FFWF", 1, "1")

        val task: Task<ProcessMetaInfoChangeEventPayload> = Task.builder<ProcessMetaInfoChangeEventPayload>(QueueShardId("test"))
            .withPayload(payload)
            .build()
        metaInfoChangeEventConsumer.execute(task)
    }

    @Test
    @DatabaseSetups(
        DatabaseSetup(value = ["classpath:fixtures/value-mapper.xml"]),
        DatabaseSetup(value = ["classpath:fixtures/dbqueue/consumer/process-meta-info-events/create-one-booking-meta-custom-map/before.xml"])
    )
    @ExpectedDatabase(
        value = "classpath:fixtures/dbqueue/consumer/process-meta-info-events/create-one-booking-meta-custom-map/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT,
    )
    fun createOneBookingMetaByBookingIdCustomMapper() {
        val payload = ProcessMetaInfoChangeEventPayload("538440", "FFWF", 1, "1")

        val task: Task<ProcessMetaInfoChangeEventPayload> = Task.builder<ProcessMetaInfoChangeEventPayload>(QueueShardId("test"))
            .withPayload(payload)
            .build()

        metaInfoChangeEventConsumer.execute(task)
    }



    @Test
    @DatabaseSetup(value = ["classpath:fixtures/dbqueue/consumer/process-meta-info-events/create-one-booking-meta/different-time/before.xml"])
    @ExpectedDatabase(
        value = "classpath:fixtures/dbqueue/consumer/process-meta-info-events/create-one-booking-meta/different-time/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT,
    )
    fun createOneBookingMetaWithDiffrentUpdateTime() {
        val payload = ProcessMetaInfoChangeEventPayload(EXTERNAL_ID_VALID, SOURCE_FFWF, null, "1")

        val task: Task<ProcessMetaInfoChangeEventPayload> = Task.builder<ProcessMetaInfoChangeEventPayload>(QueueShardId("test"))
            .withPayload(payload)
            .build()

        metaInfoChangeEventConsumer.execute(task)
    }

    @Test
    @DatabaseSetup(value = ["classpath:fixtures/dbqueue/consumer/process-meta-info-events/mark-meta-as-processed-without-creating-meta/before.xml"])
    fun markMetaAsProcessedWithoutCreatingMeta() {
        val payload = ProcessMetaInfoChangeEventPayload(EXTERNAL_ID_VALID, SOURCE_FFWF, null, "1")

        val task: Task<ProcessMetaInfoChangeEventPayload> = Task.builder<ProcessMetaInfoChangeEventPayload>(QueueShardId("test"))
            .withPayload(payload)
            .build()

        metaInfoChangeEventConsumer.execute(task)

        softly.assertThat(0).isEqualTo(metaInfoRepository.findAll().size)

        metaInfoChangeEventRepository.findAll().forEach { assertNotNull(it.processedTime) }
    }

    @Test
    @JpaQueriesCount(5)
    @DatabaseSetup(value = ["classpath:fixtures/dbqueue/consumer/process-meta-info-events/events-already-processed/before.xml"])
    fun finishTaskCauseOfProcessedEvents() {
        val payload = ProcessMetaInfoChangeEventPayload(EXTERNAL_ID_VALID, SOURCE_FFWF, null, "1")

        val task: Task<ProcessMetaInfoChangeEventPayload> = Task.builder<ProcessMetaInfoChangeEventPayload>(QueueShardId("test"))
            .withPayload(payload)
            .build()

        metaInfoChangeEventConsumer.execute(task)

    }

    @Test
    @DatabaseSetup(value = ["classpath:fixtures/dbqueue/consumer/process-meta-info-events/update-existing-meta/before.xml"])
    @ExpectedDatabase(
        value = "classpath:fixtures/dbqueue/consumer/process-meta-info-events/update-existing-meta/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT,
    )
    fun updateExistingMeta() {
        val payload = ProcessMetaInfoChangeEventPayload(EXTERNAL_ID_VALID, SOURCE_FFWF, null, "1")

        val task: Task<ProcessMetaInfoChangeEventPayload> = Task.builder<ProcessMetaInfoChangeEventPayload>(QueueShardId("test"))
            .withPayload(payload)
            .build()

        metaInfoChangeEventConsumer.execute(task)
    }

    @Test
    @DatabaseSetup(value = ["classpath:fixtures/dbqueue/consumer/process-meta-info-events/update-existing-meta/before.xml"])
    @ExpectedDatabase(
        value = "classpath:fixtures/dbqueue/consumer/process-meta-info-events/update-existing-meta/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT,
    )
    fun updateExistingMetaForCancelledBooking() {
        val payload = ProcessMetaInfoChangeEventPayload(EXTERNAL_ID_VALID, SOURCE_FFWF, null, "1")

        val task: Task<ProcessMetaInfoChangeEventPayload> = Task.builder<ProcessMetaInfoChangeEventPayload>(QueueShardId("test"))
            .withPayload(payload)
            .build()

        metaInfoChangeEventConsumer.execute(task)
    }

    @Test
    @DatabaseSetups(DatabaseSetup(value = ["classpath:fixtures/dbqueue/consumer/process-meta-info-events/update-existing-meta/before.xml"]))
    @ExpectedDatabase(
        value = "classpath:fixtures/dbqueue/consumer/process-meta-info-events/update-existing-meta/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT,
    )
    fun updateExistingMetaByBookingId() {
        val payload = ProcessMetaInfoChangeEventPayload("538440", "FFWF", 1, "1")

        val task: Task<ProcessMetaInfoChangeEventPayload> = Task.builder<ProcessMetaInfoChangeEventPayload>(QueueShardId("test"))
            .withPayload(payload)
            .build()

        metaInfoChangeEventConsumer.execute(task)
    }

    @Test
    @DatabaseSetups(DatabaseSetup(value = ["classpath:fixtures/dbqueue/consumer/process-meta-info-events/update-existing-meta-by-previous-external-id/before.xml"]))
    @ExpectedDatabase(
        value = "classpath:fixtures/dbqueue/consumer/process-meta-info-events/update-existing-meta/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT,
    )
    fun updateExistingMetaByPreviousExternalId() {
        val payload = ProcessMetaInfoChangeEventPayload("538439", "FFWF", null, "1")

        val task: Task<ProcessMetaInfoChangeEventPayload> =
            Task.builder<ProcessMetaInfoChangeEventPayload>(QueueShardId("test"))
                .withPayload(payload)
                .build()

        metaInfoChangeEventConsumer.execute(task)
    }

}

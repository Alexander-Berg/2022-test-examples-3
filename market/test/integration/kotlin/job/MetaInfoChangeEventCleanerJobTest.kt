package ru.yandex.market.logistics.calendaring.job

import com.github.springtestdbunit.annotation.DatabaseSetup
import com.github.springtestdbunit.annotation.ExpectedDatabase
import com.github.springtestdbunit.assertion.DatabaseAssertionMode
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.logistics.calendaring.base.AbstractContextualTest
import ru.yandex.market.logistics.calendaring.model.entity.MetaInfoChangeEventEntity
import ru.yandex.market.logistics.calendaring.repository.MetaInfoChangeEventRepository
import ru.yandex.market.logistics.calendaring.service.system.SystemPropertyService
import ru.yandex.market.logistics.calendaring.task.MetaInfoChangeEventCleanerJob
import java.time.LocalDateTime

open class MetaInfoChangeEventCleanerJobTest(
    @Autowired private val metaInfoChangeEventRepository: MetaInfoChangeEventRepository,
    @Autowired private val systemPropertyService: SystemPropertyService

): AbstractContextualTest() {
    val metaInfoChangeEventCleaner = MetaInfoChangeEventCleanerJob(systemPropertyService, metaInfoChangeEventRepository)

    @Test
    @DatabaseSetup(value = ["classpath:fixtures/job/meta-info-change-event-cleaner/remove-old-events/before.xml"])
    @ExpectedDatabase(
        value = "classpath:fixtures/job/meta-info-change-event-cleaner/remove-old-events/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT,
    )
    fun removeOldEvents(){
        addFreshEvent()

        metaInfoChangeEventCleaner.getTaskToRun().run()
    }

    @Test
    @DatabaseSetup(value = ["classpath:fixtures/job/meta-info-change-event-cleaner/remove-only-processed-events/before.xml"])
    @ExpectedDatabase(
        value = "classpath:fixtures/job/meta-info-change-event-cleaner/remove-only-processed-events/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT,
    )
    fun removeOnlyProcessedEvents(){
        metaInfoChangeEventCleaner.getTaskToRun().run()
    }

    private fun addFreshEvent() {
        val metaInfoChangeEventEntity = MetaInfoChangeEventEntity().apply {
            newMeta = mapOf("id" to "123", "status" to "100", "itemsCount" to "9");
            externalId = "45"
            externalUpdateTime = LocalDateTime.now()
            processedTime = LocalDateTime.now()
            source = "FFWF"
        }

        metaInfoChangeEventRepository.save(metaInfoChangeEventEntity)
    }
}

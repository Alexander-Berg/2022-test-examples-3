package ru.yandex.market.contentmapping.services.datacamp

import Market.DataCamp.DataCampUnitedOffer
import com.nhaarman.mockitokotlin2.any
import org.assertj.core.api.Assertions
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatchers.anyList
import org.mockito.Mock
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.transaction.support.TransactionTemplate
import ru.yandex.market.contentmapping.services.datacamp.offer.export.BusinessSkuKey
import ru.yandex.market.contentmapping.services.datacamp.offer.processor.DatacampOfferProcessorInterface
import ru.yandex.market.contentmapping.testutils.BaseAppTestClass
import ru.yandex.market.mbo.taskqueue.TaskQueueRegistrator
import ru.yandex.market.mbo.taskqueue.TaskQueueRepository
import ru.yandex.market.mbo.taskqueue.TaskRecord
import java.time.Instant

/**
 * @author apluhin
 * @created 1/31/22
 */
internal class ReimportOfferTaskHandlerTest : BaseAppTestClass() {

    @Mock
    private lateinit var dataCampService: DataCampService

    @Mock
    private lateinit var datacampOfferProcessService: DatacampOfferProcessorInterface

    @Autowired
    private lateinit var newTransactionTemplate: TransactionTemplate

    @Autowired
    private lateinit var taskQueueRepository: TaskQueueRepository

    @Autowired
    private lateinit var taskQueueRegistrator: TaskQueueRegistrator

    private lateinit var reimportOfferTaskHandler: ReimportOfferTaskHandler

    @Before
    fun setUp() {
        reimportOfferTaskHandler = ReimportOfferTaskHandler(
            dataCampService, datacampOfferProcessService,
            newTransactionTemplate, taskQueueRepository, taskQueueRegistrator
        )
    }

    @Test
    fun `test reimport offer from queue`() {
        val stubTask = createTaskRecord(6)
        val id = taskQueueRepository.insert(stubTask)
        stubTask.id = id

        val businessSkuKeys = listOf(
            BusinessSkuKey(1, "key"),
            BusinessSkuKey(2, "key")
        )
        Mockito.`when`(dataCampService.getUnitedOffers(anyList())).thenReturn(buildUnitedOffers(businessSkuKeys))
        Mockito.`when`(datacampOfferProcessService.process(any(), any(), any())).thenThrow(RuntimeException::class.java)

        reimportOfferTaskHandler.handle(
            ReimportOfferTask(businessSkuKeys),
            stubTask
        )
        Assertions.assertThat(taskQueueRepository.runningTasks.size).isEqualTo(2)
        Assertions.assertThat(taskQueueRepository.findById(stubTask.id).taskState).isEqualTo(TaskRecord.TaskState.DONE)
    }

    @Test(expected = RuntimeException::class)
    fun `test don't reimport single offer from queue`() {
        val stubTask = createTaskRecord(6)
        val id = taskQueueRepository.insert(stubTask)
        stubTask.id = id

        val businessSkuKeys = listOf(
            BusinessSkuKey(1, "key"),
        )
        Mockito.`when`(dataCampService.getUnitedOffers(anyList())).thenReturn(buildUnitedOffers(businessSkuKeys))
        Mockito.`when`(datacampOfferProcessService.process(any(), any(), any())).thenThrow(RuntimeException::class.java)

        reimportOfferTaskHandler.handle(
            ReimportOfferTask(businessSkuKeys),
            stubTask
        )
    }

    @Test
    fun `test handle fresher by batch`() {
        val stubTask = createTaskRecord(2)
        val id = taskQueueRepository.insert(stubTask)
        stubTask.id = id

        val businessSkuKeys = listOf(
            BusinessSkuKey(1, "key"),
            BusinessSkuKey(2, "key")
        )
        Mockito.`when`(dataCampService.getUnitedOffers(anyList())).thenReturn(buildUnitedOffers(businessSkuKeys))
        Mockito.`when`(datacampOfferProcessService.process(any(), any(), any())).thenThrow(RuntimeException::class.java)

        try {
            reimportOfferTaskHandler.handle(
                ReimportOfferTask(businessSkuKeys),
                stubTask
            )
        } catch (ex: Exception) {
            //
        }
        Assertions.assertThat(taskQueueRepository.runningTasks.size).isEqualTo(1)
        Assertions.assertThat(taskQueueRepository.findById(stubTask.id).taskState)
            .isEqualTo(TaskRecord.TaskState.ACTIVE)
    }


    private fun buildUnitedOffer(key: BusinessSkuKey): DataCampUnitedOffer.UnitedOffer {
        val identifiers = DataCampUnitedOffer.UnitedOffer.newBuilder().basicBuilder
            .identifiersBuilder.setBusinessId(key.businessId.toInt()).setOfferId(key.shopSku)
        val basic = DataCampUnitedOffer.UnitedOffer.newBuilder().basicBuilder.setIdentifiers(identifiers.build())
        return DataCampUnitedOffer.UnitedOffer.newBuilder().setBasic(basic).build()
    }

    private fun buildUnitedOffers(keys: List<BusinessSkuKey>): List<DataCampUnitedOffer.UnitedOffer> {
        return keys.map { buildUnitedOffer(it) }
    }

    private fun createTaskRecord(attempts: Int): TaskRecord {
        val taskRecord = TaskRecord()
        taskRecord.attempts = attempts
        taskRecord.taskState = TaskRecord.TaskState.ACTIVE
        taskRecord.nextRun = Instant.now()
        taskRecord.taskType = ""
        taskRecord.taskData = "{}"
        return taskRecord
    }

}

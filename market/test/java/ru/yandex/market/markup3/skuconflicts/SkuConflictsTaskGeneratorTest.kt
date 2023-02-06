package ru.yandex.market.markup3.skuconflicts

import io.kotest.inspectors.forAll
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.longs.shouldBeGreaterThan
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.markup3.core.repositories.TaskDbService
import ru.yandex.market.markup3.skuconflicts.repository.SkuConflictsAssignmentRepository
import ru.yandex.market.markup3.skuconflicts.repository.SkuConflictsAssignmentRow
import ru.yandex.market.markup3.skuconflicts.repository.SkuConflictsAssignmentStatus
import ru.yandex.market.markup3.testutils.BaseAppTest
import ru.yandex.market.mbo.storage.StorageKeyValueService
import java.time.Instant
import java.time.temporal.ChronoUnit

class SkuConflictsTaskGeneratorTest : BaseAppTest() {

    @Autowired
    lateinit var skuConflictsAssignmentRepository: SkuConflictsAssignmentRepository

    @Autowired
    lateinit var storageKeyValueService: StorageKeyValueService

    @Autowired
    lateinit var skuConflictsTaskGenerator: SkuConflictsTaskGenerator

    @Autowired
    lateinit var taskDbService: TaskDbService

    @Test
    fun `creates tasks for ready assignments in separate categories`() {
        val assignmentModel1 = assignment().copy(categoryId = 1, conflictCardId = 1)
        val assignmentModel2 = assignment().copy(categoryId = 1, conflictCardId = 2)

        // model1, model2 in one category, model3 - in separate
        val assignmentModel3 = assignment().copy(categoryId = 2, conflictCardId = 3)

        skuConflictsAssignmentRepository.insertBatchOnlyNewConflicts(listOf(
            assignmentModel1.copy(offerId = 1, parameterId = 1),
            assignmentModel1.copy(offerId = 2, parameterId = 2),

            assignmentModel2.copy(offerId = 3, parameterId = 1),

            assignmentModel3.copy(offerId = 4, parameterId = 3),
        ))

        makeReadyInQueue(skuConflictsAssignmentRepository.findAll())

        skuConflictsTaskGenerator.generateTasks()

        val assignments = skuConflictsAssignmentRepository.findAll()
        assignments shouldHaveSize 4
        assignments.forAll {
            it.status shouldBe SkuConflictsAssignmentStatus.IN_YANG
            it.markupTaskId shouldNotBe null
            it.markupTaskId!! shouldBeGreaterThan 0
        }

        val tasks = taskDbService.findAll()
        tasks shouldHaveSize 2
    }

    @Test
    fun `don't create tasks if out of limit`() {
        storageKeyValueService.putValue(SkuConflictsTaskGenerator.MAX_ACTIVE_MODELS_KEY, 2)
        storageKeyValueService.invalidateCache()

        val inYangAssignment = assignment().copy(status = SkuConflictsAssignmentStatus.IN_YANG)

        for (i in 1L..4L) {
            skuConflictsAssignmentRepository.insert(inYangAssignment.copy(parameterId = i, conflictCardId = i))
        }

        skuConflictsAssignmentRepository.insert(
            assignment().copy(status = SkuConflictsAssignmentStatus.NEW, conflictCardId = 100, parameterId = 100)
        )

        makeReadyInQueue(skuConflictsAssignmentRepository.findAll())

        skuConflictsTaskGenerator.generateTasks()

        val assignments = skuConflictsAssignmentRepository.findAll()
        assignments.filter { it.status == SkuConflictsAssignmentStatus.IN_YANG } shouldHaveSize 4
        assignments.filter { it.status == SkuConflictsAssignmentStatus.NEW }.forAll {
            it.conflictCardId shouldBe 100
            it.markupTaskId shouldBe null
        }
    }

    @Test
    fun `split created tasks by model limit in task and correctly prioritized`() {
        storageKeyValueService.putValue(SkuConflictsTaskGenerator.MIN_BATCH_SIZE_KEY, 1)
        storageKeyValueService.putValue(SkuConflictsTaskGenerator.MAX_ACTIVE_MODELS_KEY, 11)
        storageKeyValueService.putValue(SkuConflictsTaskGenerator.MAX_MODELS_IN_TASK_KEY, 3)
        storageKeyValueService.invalidateCache()

        // make 3 categories of (5, 6, 3) models in each
        for (conflictCardId in 1L..14L) {
            val paramId = conflictCardId
            val categoryId = conflictCardId / 6 + 1

            skuConflictsAssignmentRepository.insert(assignment().copy(
                offerId = conflictCardId,
                categoryId = categoryId,
                conflictCardId = conflictCardId,
                parameterId = paramId,
            ))
        }

        makeReadyInQueue(skuConflictsAssignmentRepository.findAll())

        // category 2 should be assigned last and not full
        skuConflictsAssignmentRepository.updateBatch(skuConflictsAssignmentRepository.findAll().onEach {
            if (it.categoryId == 2L) {
                it.created = Instant.now().minus(3, ChronoUnit.HOURS)
                it.lastProcessed = Instant.now().minus(2, ChronoUnit.HOURS)
                it.lastError = "test error"
            }
        })

        skuConflictsTaskGenerator.generateTasks()

        val assignments = skuConflictsAssignmentRepository.findAll().groupBy { it.categoryId }

        assignments[1]!! shouldHaveSize 5
        assignments[1]!!.forAll { it.status shouldBe SkuConflictsAssignmentStatus.IN_YANG }

        val assignedCategory2 = assignments[2]!!
        assignedCategory2.filter { it.status == SkuConflictsAssignmentStatus.IN_YANG } shouldHaveSize 3
        assignedCategory2.filter { it.status == SkuConflictsAssignmentStatus.NEW } shouldHaveSize 3

        assignments[3]!! shouldHaveSize 3
        assignments[3]!!.forAll { it.status shouldBe SkuConflictsAssignmentStatus.IN_YANG }

        val tasks = taskDbService.findAll()
        tasks shouldHaveSize 4
    }

    @Test
    fun `Write task id for failed unique keys`() {
        for (i in 1L..4L) {
            skuConflictsAssignmentRepository.insert(assignment().copy(parameterId = i, conflictCardId = i))
        }

        makeReadyInQueue(skuConflictsAssignmentRepository.findAll())
        skuConflictsTaskGenerator.generateTasks()

        var assignments = skuConflictsAssignmentRepository.findAll()
        assignments.forAll { it.status shouldBe SkuConflictsAssignmentStatus.IN_YANG }

        skuConflictsAssignmentRepository.updateBatch(assignments.onEach { it.apply {
            status = SkuConflictsAssignmentStatus.NEW
            markupTaskId = null
        }})

        for (i in 5L..8L) {
            skuConflictsAssignmentRepository.insert(assignment().copy(parameterId = i, conflictCardId = i))
        }

        makeReadyInQueue(skuConflictsAssignmentRepository.findAll().filter { it.conflictCardId > 4 })
        skuConflictsTaskGenerator.generateTasks()

        assignments = skuConflictsAssignmentRepository.findAll()
        assignments.forAll {
            if (it.conflictCardId <= 4) {
                it.status shouldBe SkuConflictsAssignmentStatus.IN_YANG
                it.markupTaskId shouldNotBe null
                it.markupTaskId!! shouldBeGreaterThan 0
            } else {
                it.status shouldBe SkuConflictsAssignmentStatus.NEW
                it.markupTaskId shouldBe null
                it.lastError shouldNotBe null
            }
        }
    }

    private inline fun assignment() = SkuConflictsAssignmentRow(
            status = SkuConflictsAssignmentStatus.NEW,
            categoryId = 1,
            conflictCardId = 1,
            mappingSkuId = null,
            parentModelId = null,
            parameterId = 1,
            offerId = 1,
            parameterValue = listOf("test"),
        )

    private fun makeReadyInQueue(rows: List<SkuConflictsAssignmentRow>) {
        skuConflictsAssignmentRepository.updateBatch(rows.onEach {
            it.apply {
                created = Instant.now().minus(20, ChronoUnit.MINUTES)
            }
        })
    }
}

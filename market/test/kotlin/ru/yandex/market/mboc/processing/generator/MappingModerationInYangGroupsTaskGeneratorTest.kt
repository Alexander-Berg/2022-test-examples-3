package ru.yandex.market.mboc.processing.generator

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.argWhere
import com.nhaarman.mockitokotlin2.atLeastOnce
import com.nhaarman.mockitokotlin2.verify
import io.kotest.matchers.shouldBe
import org.junit.Test
import org.mockito.Mockito
import ru.yandex.market.mboc.common.offers.model.Offer
import ru.yandex.market.mboc.common.offers.model.OfferProcessingAssignment
import ru.yandex.market.mboc.common.services.category.CategoryCachingServiceMock
import ru.yandex.market.mboc.processing.OfferId
import ru.yandex.market.mboc.processing.ProcessingStrategyType
import ru.yandex.market.mboc.processing.moderation.generator.MappingModerationInYangTaskGenerator
import ru.yandex.market.mboc.processing.task.AbstractTaskGenerator
import java.time.LocalDateTime
import kotlin.random.Random

class MappingModerationInYangGroupsTaskGeneratorTest : AbstractTaskGeneratorTest(
    ProcessingStrategyType.YANG_MAPPING_MODERATION
) {

    private val random = Random(100)
    override var categoryCachingServiceMock = Mockito.mock(CategoryCachingServiceMock::class.java)

    override fun setUp() {
        Mockito.doAnswer { invocation -> "abc" }.`when`(categoryCachingServiceMock).getCategoryName(any())
        super.setUp()
    }

    override fun initGenerator(): AbstractTaskGenerator {
        val generator = MappingModerationInYangTaskGenerator(
            skv,
            offerProcessingTaskRepository,
            offerProcessingAssignmentAndTaskRepository,
            markup3ApiServiceMock,
            categoryCachingServiceMock
        )

        skv.putValue(generator.useCategoryGroupsKey, true)
        skv.invalidateCache()

        return generator
    }

    @Test
    fun `generator should correctly process several category groups`() {
        initOkMarkupApi()
        setUpOptimalGroupBoundaries(min = 1, max = 10)
        val existingAssignments = generateSeveralGroups(10, 25)

        generator.generate()

        val existingTasks = offerProcessingTaskRepository.findAll()
        checkTasksCorrectness(existingAssignments, existingTasks)
        checkTasksStrategyTypes(existingTasks)

        val taskByOffer = existingTasks.associate { it.offerId to it.taskId }
        val assignmentsByTask = existingAssignments.groupBy { taskByOffer[it.offerId]!! }

        assignmentsByTask.entries.sortedBy { it.key }.forEach { (taskId, assignments) ->
            assignments.groupBy { it.categoryGroupId }.count() shouldBe 1
        }
    }

    @Test
    fun `generator should correctly process not leaf categories`() {
        initOkMarkupApi()
        setUpOptimalGroupBoundaries(min = 1, max = 10)
        val existingAssignments = generateNotLeafAssignments(1, 15)

        generator.generate()

        val existingTasks = offerProcessingTaskRepository.findAll()
        checkTasksCorrectness(existingAssignments, existingTasks)
        checkTasksStrategyTypes(existingTasks)

        val taskByOffer = existingTasks.associate { it.offerId to it.taskId }
        val assignmentsByTask = existingAssignments.groupBy { taskByOffer[it.offerId]!! }

        assignmentsByTask.entries.sortedBy { it.key }.forEach { (taskId, assignments) ->
            assignments.groupBy { it.categoryGroupId }.count() shouldBe 1
            assignments.groupBy { it.categoryId }.count() shouldBe 1
        }

        verify(markup3ApiServiceMock, atLeastOnce())
            .createTask(argWhere {
                it.tasksList.all { t ->
                    t.input.yangMappingModerationInput.categoryGroupIdsCount == 0
                        && t.input.yangMappingModerationInput.categoryId > 0
                }
            })
    }

    override fun generateOneGroupAssignments(from: OfferId, to: OfferId): List<OfferProcessingAssignment> {
        val businessId = random.nextInt()
        var categoryId = random.nextInt().toLong()
        var categoryGroupId = random.nextLong()
        val skuType = Offer.SkuType.values().toList().shuffled().first()
        return (from..to).map {
            if (it % 3 == 0L) { categoryId = random.nextInt().toLong() }
            if (it % 14 == 0L) { categoryGroupId = random.nextLong() }

            OfferProcessingAssignment.builder().apply {
                offerId(it)
                businessId(businessId)
                categoryId(categoryId)
                categoryGroupId(categoryGroupId)
                processingTicketId(123)
                priority(1)
                target(strategy.offerTarget)
                type(strategy.offerProcessingType)
                skuType(skuType)
                targetSkuId(Random.nextLong())
                assignedTs(LocalDateTime.now().plusSeconds(10))
            }.build()
        }.let { offerProcessingAssignmentRepository.insertOrUpdateAll(it) }
    }

    private fun generateNotLeafAssignments(from: OfferId, to: OfferId): List<OfferProcessingAssignment> {
        val businessId = random.nextInt()
        var categoryId = random.nextInt().toLong()
        var categoryGroupId = -categoryId
        val skuType = Offer.SkuType.values().toList().shuffled().first()
        return (from..to).map {
            if (it % 5 == 0L) {
                categoryId = random.nextInt().toLong()
                categoryGroupId = -categoryId
            }

            OfferProcessingAssignment.builder().apply {
                offerId(it)
                businessId(businessId)
                categoryId(categoryId)
                categoryGroupId(categoryGroupId)
                processingTicketId(123)
                priority(1)
                target(strategy.offerTarget)
                type(strategy.offerProcessingType)
                skuType(skuType)
                targetSkuId(Random.nextLong())
                assignedTs(LocalDateTime.now().plusSeconds(10))
            }.build()
        }.let { offerProcessingAssignmentRepository.insertOrUpdateAll(it) }
    }
}

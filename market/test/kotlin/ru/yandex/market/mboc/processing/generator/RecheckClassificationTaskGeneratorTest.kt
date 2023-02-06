package ru.yandex.market.mboc.processing.generator

import ru.yandex.market.mboc.common.offers.model.OfferProcessingAssignment
import ru.yandex.market.mboc.common.services.category.CategoryCachingServiceMock
import ru.yandex.market.mboc.processing.OfferId
import ru.yandex.market.mboc.processing.ProcessingStrategyType
import ru.yandex.market.mboc.processing.blueclassification.generator.RecheckClassificationTaskGenerator
import ru.yandex.market.mboc.processing.task.AbstractTaskGenerator
import java.time.LocalDateTime
import kotlin.random.Random

class RecheckClassificationTaskGeneratorTest : AbstractTaskGeneratorTest(
    ProcessingStrategyType.YANG_RECHECK_CLASSIFICATION
) {

    private val random = Random(100)

    override fun setUp() {
        super.setUp()
        buildTestCategoryTree()
    }

    override fun initGenerator(): AbstractTaskGenerator =
        RecheckClassificationTaskGenerator(
            skv,
            offerProcessingTaskRepository,
            offerProcessingAssignmentAndTaskRepository,
            markup3ApiServiceMock,
            categoryCachingServiceMock
        )

    override fun `generator should regenerate only optimal by size failed groups`() {
        initUniqFailMarkupApi()
        setUpOptimalGroupBoundaries(min = 12, max = 20)
        // create only one group because classification will work only with one big group and small subgroups.
        // before all opa were splited by supplier in first step and it was working
        generateSeveralGroups(1, 20)
        // could not recreate tasks because of boundaries
        val expectingNotFailedAssignments = emptyList<OfferProcessingAssignment>()

        generator.generate()

        val existingTasks = offerProcessingTaskRepository.findAll()
        checkTasksCorrectness(expectingNotFailedAssignments, existingTasks)
    }

    override fun generateOneGroupAssignments(from: OfferId, to: OfferId): List<OfferProcessingAssignment> {
        val categoryTree = categoryCachingServiceMock.categoryTree
        val leafs = categoryTree.allCategoryIdsInTree.filter { categoryTree.getByCategoryId(it).isLeaf }

        val businessId = random.nextInt()
        val categoryId = leafs[random.nextInt(0, leafs.size - 1)]
        return (from..to).map {
            OfferProcessingAssignment.builder().apply {
                offerId(it)
                businessId(businessId)
                categoryId(categoryId.toLong())
                processingTicketId(123)
                priority(1)
                target(strategy.offerTarget)
                type(strategy.offerProcessingType)
                assignedTs(LocalDateTime.now().plusSeconds(10))
            }.build()
        }.let { offerProcessingAssignmentRepository.insertOrUpdateAll(it) }
    }
}

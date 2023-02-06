package ru.yandex.market.mboc.processing.generator

import org.junit.Test
import org.springframework.test.util.ReflectionTestUtils
import ru.yandex.market.mboc.common.offers.model.OfferProcessingAssignment
import ru.yandex.market.mboc.processing.OfferId
import ru.yandex.market.mboc.processing.ProcessingStrategyType
import ru.yandex.market.mboc.processing.blueclassification.generator.BlueClassificationTaskGenerator
import ru.yandex.market.mboc.processing.task.AbstractTaskGenerator
import java.time.LocalDateTime
import kotlin.random.Random

class BlueClassificationTaskGeneratorTest : AbstractTaskGeneratorTest(ProcessingStrategyType.BLUE_CLASSIFICATION) {

    private val random = Random(100)

    override fun initGenerator(): AbstractTaskGenerator =
        BlueClassificationTaskGenerator(
            skv,
            offerProcessingTaskRepository,
            offerProcessingAssignmentAndTaskRepository,
            markup3ApiServiceMock,
            categoryCachingServiceMock
        )

    override fun setUp() {
        super.setUp()
        buildTestCategoryTree()
    }

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

    @Test
    fun `should create groups with closest assigments by category`() {
        initOkMarkupApi()
        setUpOptimalGroupBoundaries(min = 19, max = 20)

        val chargerCategory = categoryCachingServiceMock.categoryTree.getByCategoryId(11)
        val chargerAssignments = generateOneGroupAssignments(1, 10).map { it.setCategoryId(chargerCategory.categoryId) }
        offerProcessingAssignmentRepository.updateBatch(chargerAssignments)

        val androidCategory = categoryCachingServiceMock.categoryTree.getByCategoryId(8)
        val androidAssignments = generateOneGroupAssignments(11, 15).map { it.setCategoryId(androidCategory.categoryId) }
        offerProcessingAssignmentRepository.updateBatch(androidAssignments)

        val iphoneCategory = categoryCachingServiceMock.categoryTree.getByCategoryId(7)
        val iphoneAssignments = generateOneGroupAssignments(16, 20).map { it.setCategoryId(iphoneCategory.categoryId) }
        offerProcessingAssignmentRepository.updateBatch(iphoneAssignments)

        val shoesCategory = categoryCachingServiceMock.categoryTree.getByCategoryId(3)
        val shoesAssignments = generateOneGroupAssignments(21, 30).map { it.setCategoryId(shoesCategory.categoryId) }
        offerProcessingAssignmentRepository.updateBatch(shoesAssignments)

        // only closest offers to charger (charger 10, andro 5, iphone 5)
        val expectingTasksFor = listOf(chargerAssignments, androidAssignments, iphoneAssignments).flatten()

        generator.generate()

        val existingTasks = offerProcessingTaskRepository.findAll()
        checkTasksCorrectness(expectingTasksFor, existingTasks)
    }

    @Test
    fun `should create groups with closest assigments by category with restrict tree root`() {
        initOkMarkupApi()
        setUpOptimalGroupBoundaries(min = 11, max = 20)

        // restrict search tree
        val categoryRelationLevel = ReflectionTestUtils.getField(generator, "categoryRelationLevel") as String
        skv.putValue(categoryRelationLevel, 1)

        // can not pass because not enough opa (min 11) in closest categories with categoryRelationLevel 1
        val chargerCategory = categoryCachingServiceMock.categoryTree.getByCategoryId(11)
        val chargerAssignments = generateOneGroupAssignments(1, 10).map { it.setCategoryId(chargerCategory.categoryId) }
        offerProcessingAssignmentRepository.updateBatch(chargerAssignments)

        // can pass because of it has additionally 6 iphones in closest category with level 1 ("Телефоны")
        val androidCategory = categoryCachingServiceMock.categoryTree.getByCategoryId(8)
        val androidAssignments = generateOneGroupAssignments(11, 15).map { it.setCategoryId(androidCategory.categoryId) }
        offerProcessingAssignmentRepository.updateBatch(androidAssignments)

        val iphoneCategory = categoryCachingServiceMock.categoryTree.getByCategoryId(7)
        val iphoneAssignments = generateOneGroupAssignments(16, 21).map { it.setCategoryId(iphoneCategory.categoryId) }
        offerProcessingAssignmentRepository.updateBatch(iphoneAssignments)

        // can not pass because not enough opa (min 11) in closest categories with categoryRelationLevel 1
        val shoesCategory = categoryCachingServiceMock.categoryTree.getByCategoryId(3)
        val shoesAssignments = generateOneGroupAssignments(22, 30).map { it.setCategoryId(shoesCategory.categoryId) }
        offerProcessingAssignmentRepository.updateBatch(shoesAssignments)

        // only closest offers with count >= 11 (andro 5, iphone 6)
        val expectingTasksFor = listOf(androidAssignments, iphoneAssignments).flatten()

        generator.generate()

        val existingTasks = offerProcessingTaskRepository.findAll()
        checkTasksCorrectness(expectingTasksFor, existingTasks)
    }

    @Test
    fun `should create groups with with the same category on categoryRelationLevel 0`() {
        val maxTasksInGroup = 8
        initOkMarkupApi()
        setUpOptimalGroupBoundaries(min = 6, max = maxTasksInGroup)

        // restrict search tree
        val categoryRelationLevel = ReflectionTestUtils.getField(generator, "categoryRelationLevel") as String
        // only in the same category
        skv.putValue(categoryRelationLevel, 0)

        val androidCategory = categoryCachingServiceMock.categoryTree.getByCategoryId(8)
        val androidAssignments = generateOneGroupAssignments(1, 10).map { it.setCategoryId(androidCategory.categoryId) }
        offerProcessingAssignmentRepository.updateBatch(androidAssignments)

        val iphoneCategory = categoryCachingServiceMock.categoryTree.getByCategoryId(7)
        val iphoneAssignments = generateOneGroupAssignments(11, 15).map { it.setCategoryId(iphoneCategory.categoryId) }
        offerProcessingAssignmentRepository.updateBatch(iphoneAssignments)

        // only first 8 androids
        // not enough iphones for group restriction min = 6
        // and no androids could be with iphones in the same group for create second pool
        val expectingTasksFor = androidAssignments.take(maxTasksInGroup)

        generator.generate()

        val existingTasks = offerProcessingTaskRepository.findAll()
        checkTasksCorrectness(expectingTasksFor, existingTasks)
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

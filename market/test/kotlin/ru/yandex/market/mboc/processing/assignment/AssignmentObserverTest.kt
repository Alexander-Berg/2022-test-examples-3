package ru.yandex.market.mboc.processing.assignment

import com.google.protobuf.Int64Value
import com.nhaarman.mockitokotlin2.any
import org.assertj.core.api.Assertions.assertThat
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.markup3.api.Markup3Api
import ru.yandex.market.mboc.common.categorygroups.CategoryGroup
import ru.yandex.market.mboc.common.categorygroups.CategoryGroupRepository
import ru.yandex.market.mboc.common.categorygroups.CategoryGroupService
import ru.yandex.market.mboc.common.db.jooq.generated.mbo_category.enums.OfferProcessingType
import ru.yandex.market.mboc.common.db.jooq.generated.mbo_category.enums.OfferTarget
import ru.yandex.market.mboc.common.dict.SupplierRepository
import ru.yandex.market.mboc.common.offers.model.Offer
import ru.yandex.market.mboc.common.offers.model.OfferProcessingAssignment
import ru.yandex.market.mboc.common.offers.repository.OfferRepository
import ru.yandex.market.mboc.common.services.category.CategoryCachingServiceMock
import ru.yandex.market.mboc.common.services.category.CategoryTree
import ru.yandex.market.mboc.common.services.category.models.Category
import ru.yandex.market.mboc.common.utils.OfferTestUtils
import ru.yandex.market.mboc.processing.BaseOfferProcessingTest
import ru.yandex.market.mboc.processing.Markup3ApiService
import ru.yandex.market.mboc.processing.blueclassification.generator.BlueClassificationTaskGenerator
import ru.yandex.market.mboc.processing.task.OfferProcessingAssignmentAndTaskRepository
import ru.yandex.market.mboc.processing.task.OfferProcessingTaskRepository
import java.time.LocalDateTime

class AssignmentObserverTest : BaseOfferProcessingTest() {
    @Autowired
    private lateinit var offerRepository: OfferRepository

    @Autowired
    private lateinit var supplierRepository: SupplierRepository

    @Autowired
    private lateinit var assignmentRepository: OfferProcessingAssignmentRepository

    @Autowired
    private lateinit var categoryGroupRepository: CategoryGroupRepository

    @Autowired
    private lateinit var categoryGroupService: CategoryGroupService

    private lateinit var blueClassificationTaskGenerator: BlueClassificationTaskGenerator

    @Autowired
    private lateinit var offerProccessingTaskRepository: OfferProcessingTaskRepository

    @Autowired
    private lateinit var offerProcessingAssignmentAndTaskRepository: OfferProcessingAssignmentAndTaskRepository

    private lateinit var categoryCachingServiceMock: CategoryCachingServiceMock

    @Autowired
    private lateinit var markup3ApiServiceMock: Markup3ApiService

    @Before
    fun setup() {
        categoryCachingServiceMock = Mockito.spy(CategoryCachingServiceMock())
        blueClassificationTaskGenerator = BlueClassificationTaskGenerator(
            storageKeyValueService,
            offerProccessingTaskRepository,
            offerProcessingAssignmentAndTaskRepository,
            markup3ApiServiceMock,
            categoryCachingServiceMock
        )
        supplierRepository.insert(OfferTestUtils.simpleSupplier())
    }

    @Test
    fun newOfferInClassification() {
        val offer = OfferTestUtils.simpleOkOffer()
            .setCategoryIdForTests(OfferTestUtils.TEST_CATEGORY_INFO_ID, Offer.BindingKind.SUGGESTED)
            .setModelId(100L)
            .setProcessingTicketId(1000)
            .updateProcessingStatusIfValid(Offer.ProcessingStatus.IN_CLASSIFICATION)
        offerRepository.insertOffer(offer)
        val assignment = assignmentRepository.findById(offer.id)
        assertEqualContent(offer, assignment)
        assertNotAssigned(assignment)
    }

    @Test
    fun offerEntersClassification() {
        var offer = OfferTestUtils.simpleOkOffer()
            .setCategoryIdForTests(OfferTestUtils.TEST_CATEGORY_INFO_ID, Offer.BindingKind.SUGGESTED)
            .setModelId(100L)
            .setProcessingTicketId(1000)
            .updateProcessingStatusIfValid(Offer.ProcessingStatus.OPEN)
        offerRepository.insertOffer(offer)
        assertTrue(assignmentRepository.findAll().isEmpty())
        offer = offerRepository.getOfferById(offer.id)
        offer.updateProcessingStatusIfValid(Offer.ProcessingStatus.IN_CLASSIFICATION)
        offerRepository.updateOffer(offer)
        val assignment = assignmentRepository.findById(offer.id)
        assertEqualContent(offer, assignment)
        assertNotAssigned(assignment)
    }

    @Test
    fun offerLeavesClassification() {
        var offer = OfferTestUtils.simpleOkOffer()
            .setCategoryIdForTests(OfferTestUtils.TEST_CATEGORY_INFO_ID, Offer.BindingKind.SUGGESTED)
            .setModelId(100L)
            .setProcessingTicketId(1000)
            .updateProcessingStatusIfValid(Offer.ProcessingStatus.IN_CLASSIFICATION)
        offerRepository.insertOffer(offer)
        val assignment = assignmentRepository.findById(offer.id)
        assertEqualContent(offer, assignment)
        assertNotAssigned(assignment)
        offer = offerRepository.getOfferById(offer.id)
        offer.updateProcessingStatusIfValid(Offer.ProcessingStatus.PROCESSED)
        offerRepository.updateOffer(offer)
        val assignments = assignmentRepository.findByIds(listOf(offer.id))
        assertTrue(assignments.isEmpty())
    }

    @Test
    fun offerRemoval() {
        var offer = OfferTestUtils.simpleOkOffer()
            .setCategoryIdForTests(OfferTestUtils.TEST_CATEGORY_INFO_ID, Offer.BindingKind.SUGGESTED)
            .setModelId(100L)
            .setProcessingTicketId(1000)
            .updateProcessingStatusIfValid(Offer.ProcessingStatus.IN_CLASSIFICATION)
        offerRepository.insertOffer(offer)
        val assignment = assignmentRepository.findById(offer.id)
        assertEqualContent(offer, assignment)
        assertNotAssigned(assignment)
        offer = offerRepository.getOfferById(offer.id)
        offerRepository.removeOffer(offer)
        val assignments = assignmentRepository.findByIds(listOf(offer.id))
        assertTrue(assignments.isEmpty())
    }

    @Test
    fun offerUpdatedWithReassign() {
        var offer = OfferTestUtils.simpleOkOffer()
            .setCategoryIdForTests(OfferTestUtils.TEST_CATEGORY_INFO_ID, Offer.BindingKind.SUGGESTED)
            .setModelId(100L)
            .setProcessingTicketId(1000)
            .updateProcessingStatusIfValid(Offer.ProcessingStatus.IN_CLASSIFICATION)
        offerRepository.insertOffer(offer)
        var assignment = assignmentRepository.findById(offer.id)
        assertEqualContent(offer, assignment)
        assertNotAssigned(assignment)
        assignment.target = OfferTarget.YANG
        assignment.type = OfferProcessingType.IN_CLASSIFICATION
        assignmentRepository.update(assignment)
        offer = offerRepository.getOfferById(offer.id)
        offer.ticketCritical = true
        offerRepository.updateOffer(offer)
        assignment = assignmentRepository.findById(offer.id)
        assertEqualContent(offer, assignment)
        assertMarkedToReassign(assignment)
    }

    @Test
    fun offerUpdatedWithoutReassign() {
        var offer = OfferTestUtils.simpleOkOffer()
            .setCategoryIdForTests(OfferTestUtils.TEST_CATEGORY_INFO_ID, Offer.BindingKind.SUGGESTED)
            .setModelId(100L)
            .setProcessingTicketId(1000)
            .updateProcessingStatusIfValid(Offer.ProcessingStatus.IN_CLASSIFICATION)
        offerRepository.insertOffer(offer)
        var assignment = assignmentRepository.findById(offer.id)
        assertEqualContent(offer, assignment)
        assertNotAssigned(assignment)
        assignment.target = OfferTarget.YANG
        assignment.type = OfferProcessingType.IN_CLASSIFICATION
        assignmentRepository.update(assignment)
        offer = offerRepository.getOfferById(offer.id)
        offer.ticketCritical = true
        offerRepository.updateOffer(offer)
        assignment = assignmentRepository.findById(offer.id)
        assertEqualContent(offer, assignment)
        assertAssigned(assignment)
    }

    @Test
    fun targetSkuIdTest() {
//      should return null if not IN_MODERATION
        var offer = OfferTestUtils.simpleOkOffer()
            .setCategoryIdForTests(OfferTestUtils.TEST_CATEGORY_INFO_ID, Offer.BindingKind.SUGGESTED)
            .setModelId(100L)
            .setProcessingTicketId(1000)
            .updateProcessingStatusIfValid(Offer.ProcessingStatus.IN_CLASSIFICATION)
        offerRepository.insertOffer(offer)
        var assignment = assignmentRepository.findById(offer.id)
        assertEqualContent(offer, assignment)
        assertEquals(null, assignment.targetSkuId)
        offerRepository.deleteAllInTest()

//      should return mappingId if had SupplierMapping
        var mappingId = 1L
        offer = OfferTestUtils.simpleOkOffer()
            .setCategoryIdForTests(OfferTestUtils.TEST_CATEGORY_INFO_ID, Offer.BindingKind.SUGGESTED)
            .setModelId(100L)
            .setProcessingTicketId(1000)
            .updateProcessingStatusIfValid(Offer.ProcessingStatus.IN_MODERATION)
            .setSupplierSkuMapping(Offer.Mapping(mappingId, LocalDateTime.now(), Offer.SkuType.PARTNER20))
        offerRepository.insertOffer(offer)
        assignment = assignmentRepository.findById(offer.id)
        assertEqualContent(offer, assignment)
        assertEquals(mappingId, assignment.targetSkuId)
        assertEquals(Offer.SkuType.PARTNER20, assignment.skuType)
        offerRepository.deleteAllInTest()

//      should return SuggestMapping if hadn't SupplierMapping
        mappingId = 2L
        offer = OfferTestUtils.simpleOkOffer()
            .setCategoryIdForTests(OfferTestUtils.TEST_CATEGORY_INFO_ID, Offer.BindingKind.SUGGESTED)
            .setModelId(100L)
            .setProcessingTicketId(1000)
            .updateProcessingStatusIfValid(Offer.ProcessingStatus.IN_MODERATION)
            .setSuggestSkuMapping(Offer.Mapping(mappingId, LocalDateTime.now(), Offer.SkuType.MARKET))
        offerRepository.insertOffer(offer)
        assignment = assignmentRepository.findById(offer.id)
        assertEqualContent(offer, assignment)
        assertEquals(mappingId, assignment.targetSkuId)
        assertEquals(Offer.SkuType.MARKET, assignment.skuType)
        offerRepository.deleteAllInTest()
    }

    @Test
    fun correctSkyTypeIfModelDeleted() {
        val mappingId = 1L
        val offer = OfferTestUtils.simpleOkOffer()
            .setCategoryIdForTests(OfferTestUtils.TEST_CATEGORY_INFO_ID, Offer.BindingKind.SUGGESTED)
            .setModelId(100L)
            .setProcessingTicketId(1000)
            .updateProcessingStatusIfValid(Offer.ProcessingStatus.IN_MODERATION)
            .setSupplierSkuMapping(Offer.Mapping(0L, LocalDateTime.now(), Offer.SkuType.PARTNER20))
            .setSuggestSkuMapping(Offer.Mapping(mappingId, LocalDateTime.now(), Offer.SkuType.MARKET))
        offerRepository.insertOffer(offer)
        val assignment = assignmentRepository.findById(offer.id)
        assertEqualContent(offer, assignment)
        assertEquals(mappingId, assignment.targetSkuId)
        assertEquals(Offer.SkuType.MARKET, assignment.skuType)
        offerRepository.deleteAllInTest()
    }

    @Test
    fun doNotModerateFastSku() {
        val mappingId = 1L
        val offer = OfferTestUtils.simpleOkOffer()
            .setCategoryIdForTests(OfferTestUtils.TEST_CATEGORY_INFO_ID, Offer.BindingKind.SUGGESTED)
            .setModelId(100L)
            .setProcessingTicketId(1000)
            .updateProcessingStatusIfValid(Offer.ProcessingStatus.IN_MODERATION)
            .setSupplierSkuMapping(Offer.Mapping(1232342L, LocalDateTime.now(), Offer.SkuType.FAST_SKU))
            .setSuggestSkuMapping(Offer.Mapping(mappingId, LocalDateTime.now(), Offer.SkuType.MARKET))
        offerRepository.insertOffer(offer)
        val assignment = assignmentRepository.findById(offer.id)
        assertEqualContent(offer, assignment)
        assertEquals(mappingId, assignment.targetSkuId)
        assertEquals(Offer.SkuType.MARKET, assignment.skuType)
        offerRepository.deleteAllInTest()
    }

    @Test
    fun shouldAssignRecheckClassification() {
        val recheckCategoryId = OfferTestUtils.TEST_CATEGORY_INFO_ID
        val offer = OfferTestUtils.simpleOkOffer()
            .setRecheckCategoryId(recheckCategoryId)
            .setCategoryIdForTests(recheckCategoryId, Offer.BindingKind.APPROVED)
            .setProcessingStatusInternal(Offer.ProcessingStatus.IN_RECHECK_CLASSIFICATION)
            .setRecheckClassificationStatus(Offer.RecheckClassificationStatus.ON_RECHECK)
            .setRecheckClassificationSource(Offer.RecheckClassificationSource.PARTNER)
            .setApprovedSkuMappingConfidence(Offer.MappingConfidence.CONTENT)
            .updateApprovedSkuMapping(Offer.Mapping(1232342L, LocalDateTime.now(), Offer.SkuType.FAST_SKU))
            .setSupplierSkuMapping(Offer.Mapping(1232342L, LocalDateTime.now(), Offer.SkuType.FAST_SKU))
            .setSuggestSkuMapping(Offer.Mapping(1232342L, LocalDateTime.now(), Offer.SkuType.FAST_SKU))
        offerRepository.insertOffer(offer)
        val assignment = assignmentRepository.findById(offer.id)
        assertNotNull(assignment)
        assertEquals(assignment.processingStatus, Offer.ProcessingStatus.IN_RECHECK_CLASSIFICATION)
        assertEquals(assignment.categoryId, offer.recheckCategoryId)
        assertEquals(assignment.processingTicketId, Integer.valueOf(0))
    }

    @Test
    fun shouldAssignDsbsModerationWithoutPriority() {
        val recheckCategoryId = OfferTestUtils.TEST_CATEGORY_INFO_ID
        val offer = OfferTestUtils.simpleOkOffer(OfferTestUtils.dsbsSupplierUnderBiz())
            .setCategoryIdForTests(recheckCategoryId, Offer.BindingKind.APPROVED)
            .setSuggestSkuMapping(OfferTestUtils.mapping(1L, Offer.SkuType.MARKET))
            .updateProcessingStatusIfValid(Offer.ProcessingStatus.IN_MODERATION)
        offerRepository.insertOffer(offer)
        val assignment = assignmentRepository.findById(offer.id)
        assertThat(assignment).isNotNull
        assertThat(assignment.processingStatus).isEqualTo(Offer.ProcessingStatus.IN_MODERATION)
        assertThat(assignment.targetSkuId).isEqualTo(offer.suggestedSkuId)
        assertTrue(assignment.priority == null)
    }

    @Test
    fun shouldAssignDsbsRecheckModerationWithPriority() {
        val recheckCategoryId = OfferTestUtils.TEST_CATEGORY_INFO_ID
        val offer = OfferTestUtils.simpleOkOffer(OfferTestUtils.dsbsSupplierUnderBiz())
            .setCategoryIdForTests(recheckCategoryId, Offer.BindingKind.APPROVED)
            .updateProcessingStatusIfValid(Offer.ProcessingStatus.IN_RECHECK_MODERATION)
            .setRecheckSkuMapping(OfferTestUtils.mapping(1L))
            .setRecheckMappingStatus(Offer.RecheckMappingStatus.ON_RECHECK)
            .setRecheckMappingSource(Offer.RecheckMappingSource.FRIEND)
        offerRepository.insertOffer(offer)
        val assignment = assignmentRepository.findById(offer.id)
        assertThat(assignment).isNotNull
        assertThat(assignment.processingStatus).isEqualTo(Offer.ProcessingStatus.IN_RECHECK_MODERATION)
        assertThat(assignment.targetSkuId).isEqualTo(offer.recheckSkuId)
        assertThat(assignment.priority).isPositive
    }

    @Test
    fun mappingModerationOfferPriorityWithMapping2FastSkuShouldBeSmaller() {
        storageKeyValueService.putValue(AssignmentObserver.MM_DELTA_FOR_OFFERS_WITH_MAPPING_TO_FAST, 5000)
        val recheckCategoryId = OfferTestUtils.TEST_CATEGORY_INFO_ID
        val offer = OfferTestUtils.simpleOkOffer(OfferTestUtils.dsbsSupplierUnderBiz())
            .setOfferDestination(Offer.MappingDestination.BLUE)
            .setCategoryIdForTests(recheckCategoryId, Offer.BindingKind.APPROVED)
            .updateProcessingStatusIfValid(Offer.ProcessingStatus.IN_MODERATION)
        val offer1 = OfferTestUtils.simpleOkOffer(OfferTestUtils.dsbsSupplierUnderBiz())
            .setOfferDestination(Offer.MappingDestination.BLUE)
            .setCategoryIdForTests(recheckCategoryId, Offer.BindingKind.APPROVED)
            .updateProcessingStatusIfValid(Offer.ProcessingStatus.IN_MODERATION)
            .setShopSku("new-shop-sku")
            .setSupplierSkuMapping(Offer.Mapping(100500, LocalDateTime.now(), Offer.SkuType.FAST_SKU))
        offerRepository.insertOffers(offer, offer1)
        var assignment = assignmentRepository.findById(offer.id)
        assertThat(assignment).isNotNull
        assertThat(assignment.processingStatus).isEqualTo(Offer.ProcessingStatus.IN_MODERATION)
        val priority = assignment.priority
        assignment = assignmentRepository.findById(offer1.id)
        assertThat(assignment).isNotNull
        assertThat(assignment.processingStatus).isEqualTo(Offer.ProcessingStatus.IN_MODERATION)
        assertTrue(assignment.priority < priority)
    }

    @Test
    fun blueLogsOfferPriorityWithMapping2FastSkuShouldBeSmaller() {
        storageKeyValueService.putValue(AssignmentObserver.BL_DELTA_FOR_OFFERS_WITH_MAPPING_TO_FAST, 5000)
        val recheckCategoryId = OfferTestUtils.TEST_CATEGORY_INFO_ID
        val offer = OfferTestUtils.simpleOkOffer(OfferTestUtils.dsbsSupplierUnderBiz())
            .setOfferDestination(Offer.MappingDestination.BLUE)
            .setCategoryIdForTests(recheckCategoryId, Offer.BindingKind.APPROVED)
            .updateProcessingStatusIfValid(Offer.ProcessingStatus.IN_PROCESS)
        val offer1 = OfferTestUtils.simpleOkOffer(OfferTestUtils.dsbsSupplierUnderBiz())
            .setOfferDestination(Offer.MappingDestination.BLUE)
            .setCategoryIdForTests(recheckCategoryId, Offer.BindingKind.APPROVED)
            .updateProcessingStatusIfValid(Offer.ProcessingStatus.IN_PROCESS)
            .setShopSku("new-shop-sku")
            .setSupplierSkuMapping(Offer.Mapping(100500, LocalDateTime.now(), Offer.SkuType.FAST_SKU))
        offerRepository.insertOffers(offer, offer1)
        var assignment = assignmentRepository.findById(offer.id)
        assertThat(assignment).isNotNull
        assertThat(assignment.processingStatus).isEqualTo(Offer.ProcessingStatus.IN_PROCESS)
        val priority = assignment.priority
        assignment = assignmentRepository.findById(offer1.id)
        assertThat(assignment).isNotNull
        assertThat(assignment.processingStatus).isEqualTo(Offer.ProcessingStatus.IN_PROCESS)
        assertTrue(assignment.priority < priority)
    }

    @Test
    fun partnerPriorityShouldBeBiggerForRecheckMappingModeration() {
        storageKeyValueService.putValue("DeltaRecheckMappingModerationForPartnerSource", 2000)
        val recheckCategoryId = OfferTestUtils.TEST_CATEGORY_INFO_ID
        val offer = OfferTestUtils.simpleOkOffer(OfferTestUtils.dsbsSupplierUnderBiz())
            .setCategoryIdForTests(recheckCategoryId, Offer.BindingKind.APPROVED)
            .updateProcessingStatusIfValid(Offer.ProcessingStatus.IN_RECHECK_MODERATION)
            .setRecheckSkuMapping(OfferTestUtils.mapping(1L))
            .setRecheckMappingStatus(Offer.RecheckMappingStatus.ON_RECHECK)
            .setRecheckMappingSource(Offer.RecheckMappingSource.FRIEND)
        val offer1 = OfferTestUtils.simpleOkOffer()
            .setShopSku("new-shop-sku")
            .setCategoryIdForTests(recheckCategoryId, Offer.BindingKind.APPROVED)
            .updateProcessingStatusIfValid(Offer.ProcessingStatus.IN_RECHECK_MODERATION)
            .setRecheckSkuMapping(OfferTestUtils.mapping(1L))
            .setRecheckMappingStatus(Offer.RecheckMappingStatus.ON_RECHECK)
            .setRecheckMappingSource(Offer.RecheckMappingSource.PARTNER)
        offerRepository.insertOffers(listOf(offer, offer1))
        var assignment = assignmentRepository.findById(offer.id)
        assertThat(assignment).isNotNull
        assertThat(assignment.processingStatus).isEqualTo(Offer.ProcessingStatus.IN_RECHECK_MODERATION)
        assertThat(assignment.targetSkuId).isEqualTo(offer.recheckSkuId)
        val priority = assignment.priority
        assertThat(priority).isPositive

        assignment = assignmentRepository.findById(offer1.id)
        assertThat(assignment).isNotNull
        assertThat(assignment.processingStatus).isEqualTo(Offer.ProcessingStatus.IN_RECHECK_MODERATION)
        assertThat(assignment.targetSkuId).isEqualTo(offer.recheckSkuId)
        assertThat(assignment.priority).isPositive
        assertTrue(assignment.priority > priority)
    }

    @Test
    fun categoryGroupIdSetForNewAssignment() {
        categoryGroupRepository.insertBatch(
            listOf(
                CategoryGroup(null, null, listOf(OfferTestUtils.TEST_CATEGORY_INFO_ID), "", "")
            )
        )
        categoryGroupService.invalidateCaches()

        val categoryGroup = categoryGroupRepository.findAll()[0]

        val offer = OfferTestUtils.simpleOkOffer()
            .setCategoryIdForTests(OfferTestUtils.TEST_CATEGORY_INFO_ID, Offer.BindingKind.SUGGESTED)
            .setModelId(100L)
            .updateProcessingStatusIfValid(Offer.ProcessingStatus.IN_MODERATION)

        val offer2 = OfferTestUtils.simpleOkOffer()
            .setShopSku(offer.shopSku + "-2")
            .setCategoryIdForTests(OfferTestUtils.TEST_CATEGORY_INFO_ID + 5, Offer.BindingKind.SUGGESTED)
            .setModelId(100L)
            .updateProcessingStatusIfValid(Offer.ProcessingStatus.IN_MODERATION)

        offerRepository.insertOffers(offer, offer2)

        val assignment = assignmentRepository.findById(offer.id)
        assertEqualContent(offer, assignment)
        assertNotAssigned(assignment)
        assertEquals(categoryGroup.id, assignment.categoryGroupId)

        val assignment2 = assignmentRepository.findById(offer2.id)
        assertEqualContent(offer2, assignment2)
        assertNotAssigned(assignment2)
        assertEquals(-assignment2.categoryId, assignment2.categoryGroupId)
    }

    @Test
    fun offerDestinationClassificationTest() {
        buildTestCategoryTree()
        initOkMarkupApi()
        var offer = OfferTestUtils.simpleOkOffer()
            .setCategoryIdForTests(OfferTestUtils.TEST_CATEGORY_INFO_ID, Offer.BindingKind.SUGGESTED)
            .setProcessingTicketId(1000)
            .updateProcessingStatusIfValid(Offer.ProcessingStatus.IN_CLASSIFICATION)
        offerRepository.insertOffer(offer)
        var assignment = assignmentRepository.findById(offer.id)
        assertEqualContent(offer, assignment)
        assertNotNull(assignment.priority)
        assertNotAssigned(assignment)

        assignmentRepository.assign(listOf(offer.id), OfferTarget.YANG, OfferProcessingType.IN_CLASSIFICATION)
        blueClassificationTaskGenerator.generate()
        var tasks = offerProccessingTaskRepository.findByOfferId(offer.id)
        assertNotNull(tasks)
        assertFalse(tasks.isEmpty())
        assertEquals(offer.id, tasks.first().offerId)

        offer = OfferTestUtils.simpleOkOffer()
            .setOfferDestination(Offer.MappingDestination.WHITE)
            .setCategoryIdForTests(OfferTestUtils.TEST_CATEGORY_INFO_ID, Offer.BindingKind.SUGGESTED)
            .setModelId(100L)
            .setProcessingTicketId(1000)
            .updateProcessingStatusIfValid(Offer.ProcessingStatus.IN_CLASSIFICATION)
        offerRepository.deleteAllInTest()
        offerRepository.insertOffer(offer)
        assignment = assignmentRepository.findById(offer.id)
        assertEqualContent(offer, assignment)
        assertNotNull(assignment.priority)
        assertNotAssigned(assignment)

        assignmentRepository.assign(listOf(offer.id), OfferTarget.YANG, OfferProcessingType.IN_CLASSIFICATION)
        blueClassificationTaskGenerator.generate()
        tasks = offerProccessingTaskRepository.findByOfferId(offer.id)
        assertNotNull(tasks)
        assertFalse(tasks.isEmpty())
        assertEquals(offer.id, tasks.first().offerId)

        offer = OfferTestUtils.simpleOkOffer()
            .setOfferDestination(Offer.MappingDestination.DSBS)
            .setCategoryIdForTests(OfferTestUtils.TEST_CATEGORY_INFO_ID, Offer.BindingKind.SUGGESTED)
            .setModelId(100L)
            .setProcessingTicketId(1000)
            .updateProcessingStatusIfValid(Offer.ProcessingStatus.IN_CLASSIFICATION)
        offerRepository.deleteAllInTest()
        offerRepository.insertOffer(offer)
        assignment = assignmentRepository.findById(offer.id)
        assertEqualContent(offer, assignment)
        assertNotNull(assignment.priority)
        assertNotAssigned(assignment)

        assignmentRepository.assign(listOf(offer.id), OfferTarget.YANG, OfferProcessingType.IN_CLASSIFICATION)
        blueClassificationTaskGenerator.generate()
        tasks = offerProccessingTaskRepository.findByOfferId(offer.id)
        assertNotNull(tasks)
        assertFalse(tasks.isEmpty())
        assertEquals(offer.id, tasks.first().offerId)
    }

    private fun assertEqualContent(offer: Offer, assignment: OfferProcessingAssignment) {
        assertEquals(assignment.categoryId, offer.categoryId)
        assertEquals(assignment.modelId, offer.modelId)
        assertEquals(assignment.processingTicketId, offer.processingTicketId)
        assertNotNull(assignment.ticketDeadline)
    }

    private fun assertNotAssigned(assignment: OfferProcessingAssignment) {
        assertNull(assignment.target)
        assertNull(assignment.type)
    }

    private fun assertMarkedToReassign(assignment: OfferProcessingAssignment) {
        assertAssigned(assignment)
        assertTrue(assignment.isToReset)
    }

    private fun assertAssigned(assignment: OfferProcessingAssignment) {
        assertNotNull(assignment.target)
        assertNotNull(assignment.type)
    }

    private fun buildTestCategoryTree() {
        val rootCategoryId = CategoryTree.ROOT_CATEGORY_ID
        val fashionNodeCategory = Category()
            .setCategoryId(2)
            .setParentCategoryId(rootCategoryId)
            .setName("Одежда")
        val deviceNodeCategory = Category()
            .setCategoryId(5)
            .setParentCategoryId(rootCategoryId)
            .setName("Мобильные устройства")
        val cellphoneNodeCategory = Category()
            .setCategoryId(6)
            .setParentCategoryId(deviceNodeCategory.categoryId)
            .setName("Телефоны")
        val accessoryNodeCategory = Category()
            .setCategoryId(9)
            .setParentCategoryId(deviceNodeCategory.categoryId)
            .setName("Аксессуары")
        categoryCachingServiceMock.addCategories(
            fashionNodeCategory,
            Category()
                .setCategoryId(3)
                .setParentCategoryId(fashionNodeCategory.categoryId)
                .setName("Тапки")
                .setLeaf(true),
            Category()
                .setCategoryId(4)
                .setParentCategoryId(fashionNodeCategory.categoryId)
                .setName("Штаны")
                .setLeaf(true),
            deviceNodeCategory,
            cellphoneNodeCategory,
            Category()
                .setCategoryId(OfferTestUtils.TEST_CATEGORY_INFO_ID)
                .setParentCategoryId(cellphoneNodeCategory.categoryId)
                .setName("IPhone")
                .setLeaf(true),
            Category()
                .setCategoryId(8)
                .setParentCategoryId(cellphoneNodeCategory.categoryId)
                .setName("Android")
                .setLeaf(true),
            accessoryNodeCategory,
            Category()
                .setCategoryId(11)
                .setParentCategoryId(accessoryNodeCategory.categoryId)
                .setName("Зарядки")
                .setLeaf(true),
            Category()
                .setCategoryId(10)
                .setParentCategoryId(accessoryNodeCategory.categoryId)
                .setName("Чехлы")
                .setLeaf(true)
        )
    }

    private fun initOkMarkupApi() {
        var idx = 0L

        Mockito.doAnswer { invocation ->
            val request = invocation.arguments[0] as Markup3Api.CreateTasksRequest
            return@doAnswer Markup3Api.CreateTasksResponse.newBuilder().apply {
                request.tasksList.map { task ->
                    Markup3Api.CreateTaskResponseItem.newBuilder().apply {
                        externalKey = task?.externalKey
                        taskId = Int64Value.of(++idx)
                        result = Markup3Api.CreateTaskResponseItem.CreateTaskResult.OK
                    }.build()
                }.let { addAllResponseItems(it) }
            }.build()
        }.`when`(markup3ApiServiceMock).createTask(any())
    }
}

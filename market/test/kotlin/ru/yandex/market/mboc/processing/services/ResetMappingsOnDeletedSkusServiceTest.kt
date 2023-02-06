package ru.yandex.market.mboc.processing.services

import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.comparables.shouldBeEqualComparingTo
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.mboc.common.availability.msku.MskuRepository
import ru.yandex.market.mboc.common.db.jooq.generated.mbo_category.tables.pojos.Msku
import ru.yandex.market.mboc.common.dict.Supplier
import ru.yandex.market.mboc.common.dict.SupplierRepository
import ru.yandex.market.mboc.common.dict.SupplierService
import ru.yandex.market.mboc.common.honestmark.OfferCategoryRestrictionCalculator
import ru.yandex.market.mboc.common.offers.mapping.service.FastSkuMappingsService
import ru.yandex.market.mboc.common.offers.model.Offer
import ru.yandex.market.mboc.common.offers.repository.AntiMappingRepository
import ru.yandex.market.mboc.common.offers.repository.OfferBatchProcessor
import ru.yandex.market.mboc.common.offers.repository.OfferRepository
import ru.yandex.market.mboc.common.offers.repository.search.OffersFilter
import ru.yandex.market.mboc.common.services.books.BooksService
import ru.yandex.market.mboc.common.services.category.CategoryCachingServiceMock
import ru.yandex.market.mboc.common.services.category_info.knowledges.CategoryKnowledgeServiceMock
import ru.yandex.market.mboc.common.services.modelstorage.ModelStorageCachingServiceMock
import ru.yandex.market.mboc.common.services.offers.mapping.LegacyOfferMappingActionService
import ru.yandex.market.mboc.common.services.offers.mapping.OfferMappingActionService
import ru.yandex.market.mboc.common.services.offers.mapping.RetrieveMappingSkuTypeService
import ru.yandex.market.mboc.common.services.offers.processing.NeedContentStatusService
import ru.yandex.market.mboc.common.services.offers.processing.OffersProcessingStatusService
import ru.yandex.market.mboc.common.services.storage.StorageKeyValueServiceMock
import ru.yandex.market.mboc.processing.BaseOfferProcessingTest
import java.time.Instant
import java.time.LocalDateTime

class ResetMappingsOnDeletedSkusServiceTest: BaseOfferProcessingTest() {
    @Autowired
    private lateinit var offerBatchProcessor: OfferBatchProcessor
    @Autowired
    private lateinit var offerRepository: OfferRepository
    @Autowired
    private lateinit var mskuRepository: MskuRepository
    @Autowired
    private lateinit var supplierRepository: SupplierRepository
    @Autowired
    private lateinit var antiMappingRepository: AntiMappingRepository

    private lateinit var resetMappingsOnDeletedSkusService: ResetMappingsOnDeletedSkusService

    @Before
    fun setUp() {
        val supplierService = SupplierService(supplierRepository)
        val categoryCachingService = CategoryCachingServiceMock()
        val needContentStatusService = NeedContentStatusService(
            categoryCachingService, supplierService,
            BooksService(categoryCachingService, emptySet())
        )
        val legacyOfferMappingActionService = LegacyOfferMappingActionService(
            needContentStatusService,
            Mockito.mock(OfferCategoryRestrictionCalculator::class.java),
            offerDestinationCalculator,
            storageKeyValueService
        )
        val offerMappingActionService = OfferMappingActionService(legacyOfferMappingActionService)
        val categoryKnowledgeService = CategoryKnowledgeServiceMock()
        val modelStorageCachingService = ModelStorageCachingServiceMock()
        val retrieveMappingSkuTypeService = RetrieveMappingSkuTypeService(
            modelStorageCachingService, offerBatchProcessor, supplierRepository
        )
        val offersProcessingStatusService = OffersProcessingStatusService(
            offerBatchProcessor, needContentStatusService,
            supplierService, categoryKnowledgeService, retrieveMappingSkuTypeService,
            offerMappingActionService, categoryInfoRepository, antiMappingRepository, offerDestinationCalculator,
            StorageKeyValueServiceMock(), FastSkuMappingsService(needContentStatusService), false, false, 3,
            categoryInfoCache
        )

        resetMappingsOnDeletedSkusService = ResetMappingsOnDeletedSkusService(offerBatchProcessor, mskuRepository,
            offerMappingActionService, offersProcessingStatusService)
        supplierRepository.insert(Supplier().apply {
            id = 0
            name = "vasya"
        })
        offerRepository.deleteAllInTest()
    }

    @Test
    fun `should correctly reset suggest sm mappings`() {
        val generatedOffers = generateOffersWithSuggestMappings(idsTo = 20)
        generateDeletedMskus(idsTo = 20)

        resetMappingsOnDeletedSkusService.resetMappings()

        val offerIds = generatedOffers.map{ it.id }
        val resetOffers = offerRepository.findOffers(OffersFilter().setOfferIds(offerIds))
        resetOffers shouldHaveSize offerIds.size
        resetOffers.forEach {
            it.hasSuggestSkuMapping() shouldBeEqualComparingTo false
            println(it.processingStatus)
        }
    }

    @Test
    fun `should correctly reset suplier mappings`() {
        val generatedOffers = generateOffersWithSuppliersMappings(idsTo = 20)
        generateDeletedMskus(idsTo = 20)

        resetMappingsOnDeletedSkusService.resetMappings()

        val offerIds = generatedOffers.map{ it.id }
        val resetOffers = offerRepository.findOffers(OffersFilter().setOfferIds(offerIds))
        resetOffers shouldHaveSize offerIds.size
        resetOffers.forEach {
            it.hasSupplierSkuMapping() shouldBeEqualComparingTo false
        }
    }

    @Test
    fun `should correctly reset both mapping types`() {
        generateOffersWithSuppliersMappings(idsTo = 8)
        generateOffersWithSupplierAndSuggestMappings(idsFrom = 9, idsTo = 10)
        generateOffersWithSuggestMappings(idsFrom = 11, idsTo = 18)
        generateDeletedMskus(idsFrom = 2, idsTo = 9)
        generateDeletedMskus(idsFrom = 11, idsTo = 17)

        resetMappingsOnDeletedSkusService.resetMappings()

        val offerIds = (1L..18L).toList()
        val resetOffers = offerRepository.findOffers(OffersFilter().setOfferIds(offerIds))
        resetOffers shouldHaveSize offerIds.size
        resetOffers.forEach{
            if (it.id == 1L || it.id == 10L) {
                it.supplierSkuMapping.mappingId shouldBeEqualComparingTo it.id
            }
            if (it.id == 18L || it.id == 10L) {
                it.suggestSkuMapping.mappingId shouldBeEqualComparingTo it.id
            }
            if (it.id != 1L && it.id != 10L && it.id != 18L){
                it.hasSupplierSkuMapping() shouldBeEqualComparingTo false
                it.hasSuggestSkuMapping() shouldBeEqualComparingTo false
            }
        }
    }

    private fun generateOffersWithSuggestMappings(idsFrom: Int = 1, idsTo: Int): List<Offer> =
        (idsFrom..idsTo).map {
            Offer.builder()
                .id(it.toLong())
                .shopSku("testSku${it}")
                .suggestMappingSource(Offer.SuggestMappingSource.SMART_MATCHER)
                .suggestSkuMapping(Offer.Mapping(it.toLong(), LocalDateTime.now()))
                .title("a")
                .shopCategoryName("scratches")
                .processingStatus(Offer.ProcessingStatus.IN_MODERATION)
                .build()
        }.toList().also { offerRepository.insertOffers(it) }

    private fun generateOffersWithSuppliersMappings(idsFrom: Int = 1, idsTo: Int): List<Offer> =
        (idsFrom..idsTo).map {
            Offer.builder()
                .id(it.toLong())
                .shopSku("testSku${it}")
                .supplierSkuMapping(Offer.Mapping(it.toLong(), LocalDateTime.now()))
                .title("a")
                .shopCategoryName("scratches")
                .processingStatus(Offer.ProcessingStatus.IN_MODERATION)
                .build()
        }.toList().also { offerRepository.insertOffers(it) }

    private fun generateOffersWithSupplierAndSuggestMappings(idsFrom: Int = 1, idsTo: Int): List<Offer> =
        (idsFrom..idsTo).map {
            Offer.builder()
                .id(it.toLong())
                .shopSku("testSku${it}")
                .supplierSkuMapping(Offer.Mapping(it.toLong(), LocalDateTime.now()))
                .suggestMappingSource(Offer.SuggestMappingSource.SMART_MATCHER)
                .suggestSkuMapping(Offer.Mapping(it.toLong(), LocalDateTime.now()))
                .title("a")
                .shopCategoryName("scratches")
                .processingStatus(Offer.ProcessingStatus.IN_MODERATION)
                .build()
        }.toList().also { offerRepository.insertOffers(it) }

    private fun generateDeletedMskus(idsFrom: Int = 1, idsTo: Int): List<Msku> =
        (idsFrom..idsTo).map{
            Msku().apply {
                deleted = true
                marketSkuId = it.toLong()
                parentModelId = (it * 1000).toLong()
                categoryId = 1
                vendorId = 2L
                creationTs = Instant.now()
                modificationTs = Instant.now()
            }.let {
                mskuRepository.save(it)
            }
        }.toList()
}

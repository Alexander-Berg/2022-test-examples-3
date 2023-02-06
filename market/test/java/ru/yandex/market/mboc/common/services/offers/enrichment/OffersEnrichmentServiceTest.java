package ru.yandex.market.mboc.common.services.offers.enrichment;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import org.assertj.core.api.Assertions;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ru.yandex.market.ir.http.Classifier;
import ru.yandex.market.ir.http.UltraController;
import ru.yandex.market.ir.http.UltraControllerService;
import ru.yandex.market.mbo.http.SkuBDApi.SkutchType;
import ru.yandex.market.mboc.common.assertions.MbocAssertions;
import ru.yandex.market.mboc.common.dict.Supplier;
import ru.yandex.market.mboc.common.dict.SupplierRepositoryMock;
import ru.yandex.market.mboc.common.dict.SupplierService;
import ru.yandex.market.mboc.common.golden.GoldenMatrixService;
import ru.yandex.market.mboc.common.honestmark.HonestMarkClassificationCounterService;
import ru.yandex.market.mboc.common.honestmark.HonestMarkClassificationService;
import ru.yandex.market.mboc.common.honestmark.HonestMarkDepartment;
import ru.yandex.market.mboc.common.honestmark.HonestMarkDepartmentService;
import ru.yandex.market.mboc.common.honestmark.HonestMarkTestUtils;
import ru.yandex.market.mboc.common.honestmark.OfferCategoryRestrictionCalculator;
import ru.yandex.market.mboc.common.offers.DefaultOfferDestinationCalculator;
import ru.yandex.market.mboc.common.offers.OfferDestinationCalculator;
import ru.yandex.market.mboc.common.offers.model.Offer;
import ru.yandex.market.mboc.common.services.books.BooksService;
import ru.yandex.market.mboc.common.services.category.CategoryCachingServiceMock;
import ru.yandex.market.mboc.common.services.category.models.Category;
import ru.yandex.market.mboc.common.services.category_info.info.CategoryInfo;
import ru.yandex.market.mboc.common.services.category_info.info.CategoryInfoCache;
import ru.yandex.market.mboc.common.services.category_info.knowledges.CategoryKnowledgeServiceMock;
import ru.yandex.market.mboc.common.services.offers.mapping.LegacyOfferMappingActionService;
import ru.yandex.market.mboc.common.services.offers.mapping.OfferMappingActionService;
import ru.yandex.market.mboc.common.services.offers.processing.NeedContentStatusService;
import ru.yandex.market.mboc.common.services.storage.StorageKeyValueServiceMock;
import ru.yandex.market.mboc.common.services.ultracontroller.UltraControllerServiceImpl;
import ru.yandex.market.mboc.common.utils.OfferTestUtils;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;
import static ru.yandex.market.mboc.common.honestmark.OfferCategoryRestrictionCalculator.DEFAULT_CLASSIFIER_TRUST_THRESHOLD;
import static ru.yandex.market.mboc.common.offers.model.Offer.MappingConfidence.CONTENT;
import static ru.yandex.market.mboc.common.offers.model.Offer.MappingConfidence.PARTNER;
import static ru.yandex.market.mboc.common.offers.model.Offer.MappingConfidence.PARTNER_FAST;
import static ru.yandex.market.mboc.common.offers.model.Offer.MappingConfidence.PARTNER_SELF;

@SuppressWarnings("checkstyle:magicnumber")
public class OffersEnrichmentServiceTest {
    private static final Logger log = LoggerFactory.getLogger(OffersEnrichmentServiceTest.class);

    private static final int SUPPLIER_ID = 1;

    private static final long BOOTS_RELATED_HID = 5L;
    private static final long DRUGS_RELATED_HID = 8L;
    private static final long OTHER_HID = 11;
    private static final HonestMarkDepartment BOOTS_GROUP = new HonestMarkDepartment(
        HonestMarkDepartmentService.BOOTS_DEPARTMENT_ID,
        HonestMarkDepartmentService.BOOTS_DEPARTMENT_NAME,
        Set.of(BOOTS_RELATED_HID));
    private static final HonestMarkDepartment DRUGS_GROUP = new HonestMarkDepartment(
        HonestMarkDepartmentService.DRUGS_DEPARTMENT_ID,
        HonestMarkDepartmentService.DRUGS_DEPARTMENT_NAME,
        Set.of(DRUGS_RELATED_HID));

    private GoldenMatrixService goldenMatrixService;
    private UltraControllerService ultraControllerServiceRemote;
    private CategoryCachingServiceMock categoryCachingServiceMock;
    private OfferMappingActionService offerMappingActionService;
    private OffersEnrichmentService offersEnrichmentService;
    private SupplierRepositoryMock supplierRepository;
    private SupplierService supplierService;

    private NeedContentStatusService needContentStatusService;
    private CategoryKnowledgeServiceMock categoryKnowledgeService;
    private HonestMarkDepartmentService honestMarkDepartmentService;
    private OfferDestinationCalculator offerDestinationCalculator;
    private OfferCategoryRestrictionCalculator offerCategoryRestrictionCalculator;

    Supplier newPipelineSupplier = OfferTestUtils.simpleSupplier()
        .setId(SUPPLIER_ID)
        .setBusinessId(OfferTestUtils.BIZ_ID_SUPPLIER)
        .setNewContentPipeline(true);

    private static boolean hasKnowledge(Long categoryId, Set<Long> categoriesWithKnowledge) {
        return categoriesWithKnowledge.contains(categoryId);
    }

    @Before
    public void setup() {
        supplierRepository = new SupplierRepositoryMock();
        supplierRepository.insert(OfferTestUtils.simpleSupplier());
        supplierRepository.insert(newPipelineSupplier);
        supplierRepository.insert(OfferTestUtils.fmcgSupplier());
        supplierRepository.insert(OfferTestUtils.businessSupplier());

        supplierService = new SupplierService(supplierRepository);

        categoryCachingServiceMock = new CategoryCachingServiceMock().enableAuto().setGoodContentDefault(true);
        categoryKnowledgeService = new CategoryKnowledgeServiceMock();

        var categoryInfoCache = Mockito.mock(CategoryInfoCache.class);
        when(categoryInfoCache.getCategoryInfoById(anyLong()))
            .thenReturn(Optional.empty());
        CategoryInfo categoryInfoWithAllowFlag = new CategoryInfo(100501)
            .setAllowChange(true);

        when(categoryInfoCache.getCategoryInfoById(100501L))
            .thenReturn(Optional.of(categoryInfoWithAllowFlag));

        honestMarkDepartmentService = Mockito.mock(HonestMarkDepartmentService.class);

        offerCategoryRestrictionCalculator =
            new OfferCategoryRestrictionCalculator(honestMarkDepartmentService, categoryInfoCache);

        goldenMatrixService = mock(GoldenMatrixService.class);
        ultraControllerServiceRemote = mock(UltraControllerService.class);
        UltraControllerServiceImpl ultraControllerService = new UltraControllerServiceImpl(
            ultraControllerServiceRemote,
            UltraControllerServiceImpl.DEFAULT_RETRY_COUNT,
            UltraControllerServiceImpl.DEFAULT_RETRY_SLEEP_MS);
        needContentStatusService = new NeedContentStatusService(categoryCachingServiceMock, supplierService,
            new BooksService(categoryCachingServiceMock, Collections.emptySet()));
        offerDestinationCalculator = new DefaultOfferDestinationCalculator();
        var legacyOfferMappingActionService = new LegacyOfferMappingActionService(needContentStatusService,
            Mockito.mock(OfferCategoryRestrictionCalculator.class), offerDestinationCalculator,
            new StorageKeyValueServiceMock());
        offerMappingActionService = new OfferMappingActionService(legacyOfferMappingActionService);
        BooksService booksService = new BooksService(categoryCachingServiceMock, Collections.emptySet());

        HonestMarkClassificationService honestMarkClassificationService = new HonestMarkClassificationService(
            Collections.emptySet(),
            categoryCachingServiceMock,
            needContentStatusService,
            offerCategoryRestrictionCalculator);

        offersEnrichmentService = new OffersEnrichmentService(goldenMatrixService, ultraControllerService,
            offerMappingActionService, supplierService, categoryKnowledgeService, honestMarkClassificationService,
            Mockito.mock(HonestMarkClassificationCounterService.class), booksService, offerDestinationCalculator,
            categoryInfoCache);

        categoryKnowledgeService.addCategory(BOOTS_RELATED_HID);
        categoryKnowledgeService.addCategory(OTHER_HID);

        Mockito.when(honestMarkDepartmentService.getDepartmentById(eq(BOOTS_GROUP.getId())))
            .thenReturn(Optional.of(BOOTS_GROUP));
        Mockito.when(honestMarkDepartmentService.getDepartmentById(eq(DRUGS_GROUP.getId())))
            .thenReturn(Optional.of(DRUGS_GROUP));
        Mockito.when(honestMarkDepartmentService.getDepartmentById(eq(HonestMarkDepartment.OTHER.getId())))
            .thenReturn(Optional.of(HonestMarkDepartment.OTHER));
    }

    @Test
    public void testMainResponse() {
        Offer offer1 = OfferTestUtils.simpleOffer()
            .setCategoryIdForTests(1L, Offer.BindingKind.SUGGESTED)
            .setModelId(10L)
            .setSuggestSkuMapping(OfferTestUtils.mapping(100))
            .markLoadedContent();
        // Not updated since it has SKU mapping
        Offer offer2 = OfferTestUtils.simpleOffer()
            .setCategoryIdForTests(1L, Offer.BindingKind.SUGGESTED)
            .setModelId(10L)
            .setSuggestSkuMapping(OfferTestUtils.mapping(200))
            .setSuggestSkuMappingType(SkutchType.SKUTCH_BY_PARAMETERS)
            .updateApprovedSkuMapping(OfferTestUtils.mapping(200), CONTENT)
            .markLoadedContent();
        Offer offer3 = OfferTestUtils.simpleOffer() // странный кейс, тут все уже заполнено
            .setCategoryIdForTests(1L, Offer.BindingKind.APPROVED)
            .setModelId(20L)
            .setSuggestSkuMapping(OfferTestUtils.mapping(200))
            .setSuggestSkuMappingType(SkutchType.SKUTCH_BY_PARAMETERS)
            .setMarketModelName("test")
            .setSuggestMappingSource(Offer.SuggestMappingSource.ULTRA_CONTROLLER)
            .markLoadedContent();
        Offer offer4 = OfferTestUtils.simpleOffer()
            .setCategoryIdForTests(1L, Offer.BindingKind.APPROVED)
            .setModelId(10L)
            .setSuggestSkuMapping(OfferTestUtils.mapping(100))
            .setSuggestSkuMappingType(SkutchType.SKUTCH_BY_PARAMETERS)
            .markLoadedContent();
        Offer offer5 = OfferTestUtils.simpleOffer()
            .setBusinessId(SUPPLIER_ID)
            .setProcessingStatusInternal(Offer.ProcessingStatus.IN_MODERATION)
            .setCategoryIdForTests(2L, Offer.BindingKind.SUGGESTED)
            .setModelId(2L)
            .setSuggestSkuMapping(OfferTestUtils.mapping(100))
            .setSuggestSkuMappingType(SkutchType.SKUTCH_BY_PARAMETERS)
            .markLoadedContent();
        Offer offer6 = OfferTestUtils.simpleOffer()
            .setBusinessId(OfferTestUtils.FMCG_SUPPLIER_ID)
            .setCategoryIdForTests(2L, Offer.BindingKind.SUGGESTED)
            .setModelId(2L)
            .setSuggestSkuMapping(OfferTestUtils.mapping(100))
            .setSuggestSkuMappingType(SkutchType.SKUTCH_BY_PARAMETERS)
            .markLoadedContent();
        Offer offer7 = OfferTestUtils.simpleOffer()
            .setBusinessId(SUPPLIER_ID)
            .setProcessingStatusInternal(Offer.ProcessingStatus.OPEN)
            .setCategoryIdForTests(2L, Offer.BindingKind.SUGGESTED)
            .setModelId(2L)
            .setSuggestSkuMapping(OfferTestUtils.mapping(100))
            .setSuggestSkuMappingType(SkutchType.SKUTCH_BY_PARAMETERS)
            .markLoadedContent();

        Mockito.when(ultraControllerServiceRemote.enrich(any())).then(__ ->
            UltraController.DataResponse.newBuilder()
                .addOffers(ucResponse(
                    UltraController.EnrichedOffer.EnrichType.MAIN, 2, 20, 200, SkutchType.SKUTCH_BY_PARAMETERS
                ))
                .addOffers(ucResponse(
                    UltraController.EnrichedOffer.EnrichType.MAIN, 2, 20, 200, SkutchType.SKUTCH_BY_PARAMETERS
                ))
                .addOffers(ucResponse(
                    UltraController.EnrichedOffer.EnrichType.MAIN, 2, 20, 200, SkutchType.SKUTCH_BY_PARAMETERS
                ))
                .addOffers(ucResponse(
                    UltraController.EnrichedOffer.EnrichType.MAIN, 1, 11, 111, SkutchType.VENDOR_CODE_SKUTCH
                ))
                .addOffers(ucResponse(
                    UltraController.EnrichedOffer.EnrichType.MAIN, 1, -1, -1, SkutchType.NO_SKUTCH))
                .addOffers(ucResponse(
                    UltraController.EnrichedOffer.EnrichType.MAIN, 1, -1, -1, SkutchType.NO_SKUTCH))
                .addOffers(ucResponse(
                    UltraController.EnrichedOffer.EnrichType.MAIN, 1, 3, -1, SkutchType.NO_SKUTCH))
                .build());

        List<Offer> offers = Arrays.asList(new Offer(offer1), new Offer(offer2), new Offer(offer3),
            new Offer(offer4), new Offer(offer5), new Offer(offer6), new Offer(offer7));
        offersEnrichmentService.doUltraControllerEnrichment(offers, log::info);

        Offer expectedOffer1 = expectedOffer()
            .setCategoryIdForTests(2L, Offer.BindingKind.SUGGESTED)
            .setModelId(20L)
            .setSuggestSkuMapping(OfferTestUtils.mapping(200))
            .setSuggestMappingSource(Offer.SuggestMappingSource.ULTRA_CONTROLLER)
            .setSuggestSkuMappingType(SkutchType.SKUTCH_BY_PARAMETERS)
            .setSuggestCategoryMappingId(2L)
            .setSuggestModelMappingId(20L)
            .setSuggestMarketModelName("test");
        Offer expectedOffer2 = expectedOffer()
            .setCategoryIdForTests(2L, Offer.BindingKind.SUGGESTED)
            .setModelId(20L)
            .setSuggestSkuMapping(OfferTestUtils.mapping(200))
            .setSuggestMappingSource(Offer.SuggestMappingSource.ULTRA_CONTROLLER)
            .setSuggestSkuMappingType(SkutchType.SKUTCH_BY_PARAMETERS)
            .setSuggestCategoryMappingId(2L)
            .setSuggestModelMappingId(20L)
            .setSuggestMarketModelName("test")
            .updateApprovedSkuMapping(OfferTestUtils.mapping(200), CONTENT);
        Offer expectedOffer3 = offer3.copy()
            .setSuggestModelMappingId(20L)
            .setSuggestMarketModelName("test")
            .setSuggestCategoryMappingId(2L);
        Offer expectedOffer4 = expectedOffer()
            .setCategoryIdForTests(1L, Offer.BindingKind.APPROVED)
            .setModelId(11L)
            .setSuggestSkuMapping(OfferTestUtils.mapping(111))
            .setSuggestMappingSource(Offer.SuggestMappingSource.ULTRA_CONTROLLER)
            .setSuggestSkuMappingType(SkutchType.VENDOR_CODE_SKUTCH)
            .setSuggestCategoryMappingId(1L)
            .setSuggestModelMappingId(11L)
            .setSuggestMarketModelName("test");
        Offer expectedOffer5 = expectedOffer()
            .setBusinessId(SUPPLIER_ID)
            .setCategoryIdForTests(1L, Offer.BindingKind.SUGGESTED)
            .setSuggestSkuMapping(null)
            .setMarketModelName(null)
            .setSuggestCategoryMappingId(1L)
            .setProcessingStatusInternal(Offer.ProcessingStatus.IN_MODERATION);
        Offer expectedOffer6 = expectedOffer()
            .setBusinessId(OfferTestUtils.FMCG_SUPPLIER_ID)
            .setCategoryIdForTests(1L, Offer.BindingKind.SUGGESTED)
            .setSuggestSkuMapping(null)
            .setMarketModelName(null)
            .setSuggestCategoryMappingId(1L)
            .setProcessingStatusInternal(Offer.ProcessingStatus.OPEN);
        Offer expectedOffer7 = expectedOffer()
            .setBusinessId(SUPPLIER_ID)
            .setCategoryIdForTests(1L, Offer.BindingKind.SUGGESTED)
            .setSuggestSkuMapping(null)
            .setSuggestCategoryMappingId(1L)
            .setSuggestModelMappingId(3L)
            .setSuggestMarketModelName("test")
            .setModelId(3L)
            .setProcessingStatusInternal(Offer.ProcessingStatus.OPEN);
        MbocAssertions.assertThat(offers.get(0)).isEqualTo(expectedOffer1);
        MbocAssertions.assertThat(offers.get(1)).isEqualToWithoutYtStamp(expectedOffer2);
        MbocAssertions.assertThat(offers.get(2)).isEqualTo(expectedOffer3);
        MbocAssertions.assertThat(offers.get(3)).isEqualTo(expectedOffer4);
        MbocAssertions.assertThat(offers.get(4)).isEqualTo(expectedOffer5);
        MbocAssertions.assertThat(offers.get(5)).isEqualTo(expectedOffer6);
        MbocAssertions.assertThat(offers.get(6)).isEqualTo(expectedOffer7);
    }

    @Test
    public void ignoreUcResultWithNotPublishedModel() {
        Offer offer = OfferTestUtils.simpleOffer()
            .setProcessingStatusInternal(Offer.ProcessingStatus.IN_MODERATION)
            .setCategoryIdForTests(2L, Offer.BindingKind.SUGGESTED)
            .setModelId(2L)
            .setSuggestSkuMapping(OfferTestUtils.mapping(100))
            .setSuggestSkuMappingType(SkutchType.SKUTCH_BY_PARAMETERS)
            .markLoadedContent();

        Mockito.when(ultraControllerServiceRemote.enrich(any())).then(__ ->
            UltraController.DataResponse.newBuilder()
                .addOffers(ucResponse(
                    UltraController.EnrichedOffer.EnrichType.MAIN, 1, 1, 112, SkutchType.VENDOR_CODE_SKUTCH,
                    false, false))
                .build());

        offersEnrichmentService.doUltraControllerEnrichment(List.of(offer), log::info);

        Offer expectedOffer = expectedOffer()
            .setCategoryIdForTests(1L, Offer.BindingKind.SUGGESTED)
            .setSuggestSkuMapping(null)
            .setMarketModelName(null)
            .setSuggestCategoryMappingId(null)
            .setProcessingStatusInternal(Offer.ProcessingStatus.IN_MODERATION);

        MbocAssertions.assertThat(offer).isEqualTo(expectedOffer);
    }

    @Test
    public void shouldChangeBindedCategoryWhenCategoryAllowsAndSkuMappingChanged() {
        Offer.Mapping skuMapping = new Offer.Mapping(157L, LocalDateTime.now(), Offer.SkuType.FAST_SKU);
        Offer offer1 = OfferTestUtils.simpleOffer()
            .setCategoryIdForTests(100501L, Offer.BindingKind.APPROVED)
            .markLoadedContent()
            .setAutomaticClassification(true)
            .setMappedCategoryId(null)
            .setModelId(10L)
            .setApprovedSkuMappingInternal(skuMapping);

        Mockito.when(ultraControllerServiceRemote.enrich(any())).then(__ ->
            UltraController.DataResponse.newBuilder()
                .addOffers(ucResponse(
                    UltraController.EnrichedOffer.EnrichType.APPROVED_SKU, 2L, 1, 112,
                    SkutchType.VENDOR_CODE_SKUTCH,
                    false, false))
                .build());

        offersEnrichmentService.doUltraControllerEnrichment(List.of(offer1), log::info);

        Offer expectedOffer1 = expectedOffer()
            .setCategoryIdForTests(2L, Offer.BindingKind.SUGGESTED)
            .setSuggestSkuMapping(null)
            .setModelId(10L)
            .setMarketModelName(null)
            .setSuggestCategoryMappingId(2L)
            .setAutomaticClassification(false)
            .setApprovedSkuMappingInternal(skuMapping);

        Assertions.assertThat(offer1).usingRecursiveComparison()
            .ignoringFields("acceptanceStatusModified",
                "contentChangedTs",
                "contentProcessingStatusModified",
                "created",
                "processingStatus",
                "processingStatusModified",
                "updated")
            .isEqualTo(expectedOffer1);
    }

    @Test
    public void shouldChangeBindedCategoryWhenCategoryAllowsAndSkuMappingChangedFromDataCamp() {
        Offer.Mapping skuMapping = new Offer.Mapping(157L, LocalDateTime.now(), Offer.SkuType.FAST_SKU);
        Offer offer1 = OfferTestUtils.simpleOffer()
            .setCategoryIdForTests(100501L, Offer.BindingKind.APPROVED)
            .markLoadedContent()
            .setAutomaticClassification(true)
            .setMappedCategoryId(null)
            .setMarketModelName("test")
            .setModelId(1L)
            .setApprovedSkuMappingInternal(skuMapping);

        var enrichedOfferBuilder = ucResponse(UltraController.EnrichedOffer.EnrichType.APPROVED_SKU, 2L, 1, 1,
            SkutchType.SKUTCH_BY_PARAMETERS);
        var enriched = enrichedOfferBuilder.build();
        var businessSkuKey = offer1.getBusinessSkuKey();

        offersEnrichmentService.doUltraControllerEnrichment(List.of(offer1), log::info, true, Map.of(businessSkuKey,
            enriched));

        Offer expectedOffer1 = expectedOffer()
            .setCategoryIdForTests(2L, Offer.BindingKind.SUGGESTED)
            .setSuggestSkuMapping(null)
            .setSuggestCategoryMappingId(2L)
            .setAutomaticClassification(false)
            .setApprovedSkuMappingInternal(skuMapping);

        Assertions.assertThat(offer1).usingRecursiveComparison()
            .ignoringFields("acceptanceStatusModified",
                "contentChangedTs",
                "contentProcessingStatusModified",
                "created",
                "processingStatus",
                "processingStatusModified",
                "updated",
                "suggestSkuMapping",
                "suggestSkuMappingType",
                "suggestMarketModelName",
                "suggestModelMappingId",
                "marketModelName",
                "modelId",
                "vendorId")
            .isEqualTo(expectedOffer1);
    }

    @Test
    public void shouldNotChangeBindedCategoryWhenCategoryAllowsAndApprovedCategory() {
        Offer.Mapping skuMapping = new Offer.Mapping(157L, LocalDateTime.now(), Offer.SkuType.FAST_SKU);
        Offer offer1 = OfferTestUtils.simpleOffer()
            .setCategoryIdForTests(100501L, Offer.BindingKind.APPROVED)
            .markLoadedContent()
            .setAutomaticClassification(true)
            .setMappedCategoryId(null)
            .setModelId(10L)
            .setApprovedSkuMappingInternal(skuMapping);

        Mockito.when(ultraControllerServiceRemote.enrich(any())).then(__ ->
            UltraController.DataResponse.newBuilder()
                .addOffers(ucResponse(
                    UltraController.EnrichedOffer.EnrichType.APPROVED_CATEGORY, 2L, 1, 112,
                    SkutchType.VENDOR_CODE_SKUTCH,
                    false, false))
                .build());

        offersEnrichmentService.doUltraControllerEnrichment(List.of(offer1), log::info);

        Offer expectedOffer1 = expectedOffer()
            .setCategoryIdForTests(100501L, Offer.BindingKind.APPROVED)
            .setSuggestSkuMapping(null)
            .setModelId(10L)
            .setMarketModelName(null)
            .setAutomaticClassification(true)
            .setApprovedSkuMappingInternal(skuMapping);

        Assertions.assertThat(offer1).usingRecursiveComparison()
            .ignoringFields("acceptanceStatusModified",
                "contentChangedTs",
                "contentProcessingStatusModified",
                "created",
                "processingStatus",
                "processingStatusModified",
                "marketVendorName",
                "vendorId",
                "updated")
            .isEqualTo(expectedOffer1);
    }

    @Test
    public void shouldNotChangeBindedCategoryWhenCategoryAllowsAndApprovedCategoryFromDataCamp() {
        Offer.Mapping skuMapping = new Offer.Mapping(157L, LocalDateTime.now(), Offer.SkuType.FAST_SKU);
        Offer offer1 = OfferTestUtils.simpleOffer()
            .setCategoryIdForTests(100501L, Offer.BindingKind.APPROVED)
            .markLoadedContent()
            .setAutomaticClassification(true)
            .setMappedCategoryId(null)
            .setModelId(10L)
            .setApprovedSkuMappingInternal(skuMapping);

        var enrichedOfferBuilder = ucResponse(UltraController.EnrichedOffer.EnrichType.APPROVED_CATEGORY, 2L, 1, 1,
            SkutchType.SKUTCH_BY_PARAMETERS);
        var enriched = enrichedOfferBuilder.build();
        var businessSkuKey = offer1.getBusinessSkuKey();

        offersEnrichmentService.doUltraControllerEnrichment(List.of(offer1), log::info, true, Map.of(businessSkuKey,
            enriched));

        Offer expectedOffer1 = expectedOffer()
            .setCategoryIdForTests(100501L, Offer.BindingKind.APPROVED)
            .setSuggestSkuMapping(null)
            .setModelId(10L)
            .setMarketVendorName(null)
            .setVendorId(null)
            .setMarketModelName(null)
            .setAutomaticClassification(true)
            .setApprovedSkuMappingInternal(skuMapping);

        Assertions.assertThat(offer1).usingRecursiveComparison()
            .ignoringFields("acceptanceStatusModified",
                "contentChangedTs",
                "contentProcessingStatusModified",
                "created",
                "processingStatus",
                "processingStatusModified",
                "updated",
                "suggestSkuMapping",
                "suggestSkuMappingType")
            .isEqualTo(expectedOffer1);
    }

    @Test
    public void shouldRemoveAutoApproveWhenCategoryChangesFromDataCamp() {
        Offer.Mapping skuMapping = new Offer.Mapping(157L, LocalDateTime.now(), Offer.SkuType.FAST_SKU);
        Offer offer1 = OfferTestUtils.simpleOffer()
            .setCategoryIdForTests(100501L, Offer.BindingKind.APPROVED)
            .markLoadedContent()
            .setAutomaticClassification(true)
            .setMappedCategoryId(null)
            .setModelId(10L)
            .setApprovedSkuMappingInternal(skuMapping);

        var enrichedOfferBuilder =
            ucResponse(UltraController.EnrichedOffer.EnrichType.MAIN, 2L, 1, 1,
                SkutchType.SKUTCH_BY_PARAMETERS);
        var enriched = enrichedOfferBuilder.build();
        var businessSkuKey = offer1.getBusinessSkuKey();

        offersEnrichmentService.doUltraControllerEnrichment(List.of(offer1), log::info, true, Map.of(businessSkuKey,
            enriched));

        Offer expectedOffer1 = expectedOffer()
            .setCategoryIdForTests(2L, Offer.BindingKind.SUGGESTED)
            .setAutomaticClassification(false)
            .setSuggestCategoryMappingId(2L)
            .setSuggestMappingSource(Offer.SuggestMappingSource.ULTRA_CONTROLLER)
            .setApprovedSkuMappingInternal(skuMapping);

        Assertions.assertThat(offer1).usingRecursiveComparison()
            .ignoringFields("acceptanceStatusModified",
                "contentChangedTs",
                "contentProcessingStatusModified",
                "created",
                "processingStatus",
                "processingStatusModified",
                "updated",
                "suggestSkuMapping",
                "suggestSkuMappingType",
                "suggestMarketModelName",
                "marketModelName",
                "suggestModelMappingId",
                "vendorId",
                "marketVendorName",
                "modelId")
            .isEqualTo(expectedOffer1);
    }

    @Test
    public void shouldChangeBindedCategoryWhenCategoryAllowsAndApprovedModel() {
        Offer.Mapping skuMapping = new Offer.Mapping(157L, LocalDateTime.now(), Offer.SkuType.FAST_SKU);
        Offer offer1 = OfferTestUtils.simpleOffer()
            .setCategoryIdForTests(100501L, Offer.BindingKind.APPROVED)
            .markLoadedContent()
            .setAutomaticClassification(true)
            .setMappedCategoryId(null)
            .setMappedModelId(1L)
            .setModelId(1L)
            .setApprovedSkuMappingInternal(skuMapping);

        Mockito.when(ultraControllerServiceRemote.enrich(any())).then(__ ->
            UltraController.DataResponse.newBuilder()
                .addOffers(ucResponse(
                    UltraController.EnrichedOffer.EnrichType.APPROVED_MODEL, 2L, 1, 112,
                    SkutchType.VENDOR_CODE_SKUTCH,
                    false, false))
                .build());

        offersEnrichmentService.doUltraControllerEnrichment(List.of(offer1), log::info);

        Offer expectedOffer1 = expectedOffer()
            .setCategoryIdForTests(2L, Offer.BindingKind.SUGGESTED)
            .setSuggestSkuMapping(null)
            .setModelId(1L)
            .setMappedModelId(1L)
            .setMarketModelName(null)
            .setAutomaticClassification(false)
            .setApprovedSkuMappingInternal(skuMapping);

        Assertions.assertThat(offer1).usingRecursiveComparison()
            .ignoringFields("acceptanceStatusModified",
                "contentChangedTs",
                "contentProcessingStatusModified",
                "created",
                "processingStatus",
                "processingStatusModified",
                "updated")
            .isEqualTo(expectedOffer1);
    }

    @Test
    public void shouldChangeBindedCategoryWhenCategoryAllowsAndDefaultUK() {
        Offer.Mapping skuMapping = new Offer.Mapping(157L, LocalDateTime.now(), Offer.SkuType.FAST_SKU);
        Offer offer1 = OfferTestUtils.simpleOffer()
            .setCategoryIdForTests(100501L, Offer.BindingKind.APPROVED)
            .markLoadedContent()
            .setAutomaticClassification(true)
            .setMappedCategoryId(null)
            .setMappedModelId(1L)
            .setModelId(1L)
            .setApprovedSkuMappingInternal(skuMapping);

        Mockito.when(ultraControllerServiceRemote.enrich(any())).then(__ ->
            UltraController.DataResponse.newBuilder()
                .addOffers(ucResponse(
                    UltraController.EnrichedOffer.EnrichType.MAIN, 2L, 1, 112,
                    SkutchType.VENDOR_CODE_SKUTCH,
                    false, false))
                .build());

        offersEnrichmentService.doUltraControllerEnrichment(List.of(offer1), log::info);

        Offer expectedOffer1 = expectedOffer()
            .setCategoryIdForTests(2L, Offer.BindingKind.SUGGESTED)
            .setSuggestSkuMapping(null)
            .setModelId(1L)
            .setMappedModelId(1L)
            .setMarketModelName(null)
            .setAutomaticClassification(false)
            .setApprovedSkuMappingInternal(skuMapping);

        Assertions.assertThat(offer1).usingRecursiveComparison()
            .ignoringFields("acceptanceStatusModified",
                "contentChangedTs",
                "contentProcessingStatusModified",
                "created",
                "processingStatus",
                "processingStatusModified",
                "updated")
            .isEqualTo(expectedOffer1);
    }

    @Test
    public void shouldChangeBindedCategoryWhenCategoryAllowsAndApprovedModelFromDataCamp() {
        Offer.Mapping skuMapping = new Offer.Mapping(157L, LocalDateTime.now(), Offer.SkuType.FAST_SKU);
        Offer offer1 = OfferTestUtils.simpleOffer()
            .setCategoryIdForTests(100501L, Offer.BindingKind.APPROVED)
            .markLoadedContent()
            .setAutomaticClassification(true)
            .setMappedCategoryId(null)
            .setMappedModelId(1L)
            .setModelId(1L)
            .setApprovedSkuMappingInternal(skuMapping);

        var enrichedOfferBuilder = ucResponse(UltraController.EnrichedOffer.EnrichType.APPROVED_MODEL, 2L, 1, 1,
            SkutchType.SKUTCH_BY_PARAMETERS);
        var enriched = enrichedOfferBuilder.build();
        var businessSkuKey = offer1.getBusinessSkuKey();

        offersEnrichmentService.doUltraControllerEnrichment(List.of(offer1), log::info, true, Map.of(businessSkuKey,
            enriched));

        Offer expectedOffer1 = expectedOffer()
            .setCategoryIdForTests(2L, Offer.BindingKind.SUGGESTED)
            .setSuggestSkuMapping(null)
            .setModelId(1L)
            .setMappedModelId(1L)
            .setMarketModelName("test")
            .setSuggestCategoryMappingId(2L)
            .setSuggestMappingSource(Offer.SuggestMappingSource.ULTRA_CONTROLLER)
            .setSuggestMarketModelName("test")
            .setSuggestModelMappingId(1L)
            .setAutomaticClassification(false)
            .setApprovedSkuMappingInternal(skuMapping);

        Assertions.assertThat(offer1).usingRecursiveComparison()
            .ignoringFields("acceptanceStatusModified",
                "contentChangedTs",
                "contentProcessingStatusModified",
                "created",
                "processingStatus",
                "processingStatusModified",
                "updated",
                "suggestSkuMapping",
                "suggestSkuMappingType")
            .isEqualTo(expectedOffer1);
    }

    @Test
    public void shouldChangeBindedCategoryWhenCategoryAllowsAndDefaultFromDataCamp() {
        Offer.Mapping skuMapping = new Offer.Mapping(157L, LocalDateTime.now(), Offer.SkuType.FAST_SKU);
        Offer offer1 = OfferTestUtils.simpleOffer()
            .setCategoryIdForTests(100501L, Offer.BindingKind.APPROVED)
            .markLoadedContent()
            .setAutomaticClassification(true)
            .setMappedCategoryId(null)
            .setMappedModelId(1L)
            .setModelId(1L)
            .setApprovedSkuMappingInternal(skuMapping);
        var enrichedOfferBuilder = ucResponse(UltraController.EnrichedOffer.EnrichType.MAIN, 2L, 1, 1,
            SkutchType.SKUTCH_BY_PARAMETERS);
        var enriched = enrichedOfferBuilder.build();
        var businessSkuKey = offer1.getBusinessSkuKey();

        offersEnrichmentService.doUltraControllerEnrichment(List.of(offer1), log::info, true, Map.of(businessSkuKey,
            enriched));

        Offer expectedOffer1 = expectedOffer()
            .setCategoryIdForTests(2L, Offer.BindingKind.SUGGESTED)
            .setSuggestSkuMapping(null)
            .setModelId(1L)
            .setMappedModelId(1L)
            .setMarketModelName("test")
            .setSuggestCategoryMappingId(2L)
            .setSuggestMappingSource(Offer.SuggestMappingSource.ULTRA_CONTROLLER)
            .setSuggestMarketModelName("test")
            .setSuggestModelMappingId(1L)
            .setAutomaticClassification(false)
            .setApprovedSkuMappingInternal(skuMapping);

        Assertions.assertThat(offer1).usingRecursiveComparison()
            .ignoringFields("acceptanceStatusModified",
                "contentChangedTs",
                "contentProcessingStatusModified",
                "created",
                "processingStatus",
                "processingStatusModified",
                "updated",
                "suggestSkuMapping",
                "suggestSkuMappingType")
            .isEqualTo(expectedOffer1);
    }

    @Test
    public void shouldNotChangeBindedCategoryWhenMappedCategoryIdIsSetFromDataCamp() {
        Offer.Mapping skuMapping = new Offer.Mapping(157L, LocalDateTime.now(), Offer.SkuType.FAST_SKU);
        Offer offer1 = OfferTestUtils.simpleOffer()
            .setCategoryIdForTests(100501L, Offer.BindingKind.APPROVED)
            .markLoadedContent()
            .setAutomaticClassification(true)
            .setMappedCategoryId(100501L)
            .setMappedModelId(1L)
            .setModelId(1L)
            .setApprovedSkuMappingInternal(skuMapping);
        var enrichedOfferBuilder = ucResponse(UltraController.EnrichedOffer.EnrichType.MAIN, 2L, 1, 1,
            SkutchType.SKUTCH_BY_PARAMETERS);
        var enriched = enrichedOfferBuilder.build();
        var businessSkuKey = offer1.getBusinessSkuKey();

        offersEnrichmentService.doUltraControllerEnrichment(List.of(offer1), log::info, true, Map.of(businessSkuKey,
            enriched));

        Offer expectedOffer1 = OfferTestUtils.simpleOffer()
            .setCategoryIdForTests(100501L, Offer.BindingKind.APPROVED)
            .setSuggestSkuMapping(null)
            .setModelId(1L)
            .setMappedModelId(1L)
            .setMappedCategoryId(100501L)
            .setAutomaticClassification(true)
            .setApprovedSkuMappingInternal(skuMapping);

        Assertions.assertThat(offer1).usingRecursiveComparison()
            .ignoringFields("acceptanceStatusModified",
                "contentChangedTs",
                "contentProcessingStatusModified",
                "created",
                "processingStatus",
                "processingStatusModified",
                "updated",
                "suggestSkuMapping",
                "suggestSkuMappingType")
            .isEqualTo(expectedOffer1);
    }

    @Test
    public void shouldNotChangeWhenMappedCategoryIsSet() {
        Offer.Mapping skuMapping = new Offer.Mapping(157L, LocalDateTime.now(), Offer.SkuType.FAST_SKU);
        Offer offer1 = OfferTestUtils.simpleOffer()
            .setCategoryIdForTests(100501L, Offer.BindingKind.APPROVED)
            .markLoadedContent()
            .setAutomaticClassification(true)
            .setMappedCategoryId(100501L)
            .setMappedModelId(1L)
            .setModelId(1L)
            .setApprovedSkuMappingInternal(skuMapping);

        Mockito.when(ultraControllerServiceRemote.enrich(any())).then(__ ->
            UltraController.DataResponse.newBuilder()
                .addOffers(ucResponse(
                    UltraController.EnrichedOffer.EnrichType.MAIN, 2L, 1, 112,
                    SkutchType.VENDOR_CODE_SKUTCH,
                    false, false))
                .build());

        offersEnrichmentService.doUltraControllerEnrichment(List.of(offer1), log::info);

        Offer expectedOffer1 = OfferTestUtils.simpleOffer()
            .setCategoryIdForTests(100501L, Offer.BindingKind.APPROVED)
            .setSuggestSkuMapping(null)
            .setModelId(1L)
            .setMappedCategoryId(100501L)
            .setMappedModelId(1L)
            .setMarketModelName(null)
            .setAutomaticClassification(true)
            .setApprovedSkuMappingInternal(skuMapping);

        Assertions.assertThat(offer1).usingRecursiveComparison()
            .ignoringFields("acceptanceStatusModified",
                "contentChangedTs",
                "contentProcessingStatusModified",
                "created",
                "processingStatus",
                "processingStatusModified",
                "updated")
            .isEqualTo(expectedOffer1);
    }

    @Test
    public void whenUCReturnsApprovedClassificationTypeUseMaxClassifierConfidence() {
        Offer offer1 = OfferTestUtils.simpleOffer()
            .setCategoryIdForTests(1L, Offer.BindingKind.SUGGESTED)
            .markLoadedContent();

        categoryKnowledgeService.addCategory(2);

        Mockito.when(ultraControllerServiceRemote.enrich(any())).then(__ ->
            UltraController.DataResponse.newBuilder()
                .addOffers(ucResponse(UltraController.EnrichedOffer.EnrichType.MAIN, 2, 20, 30,
                    SkutchType.SKUTCH_BY_PARAMETERS)
                    .setClassifierCategoryId(2)
                    .setClassificationTypeValue(UltraController.EnrichedOffer.ClassificationType.APPROVED)
                    .setClassifierConfidentTopPrecision(0.0d))
                .build());

        List<Offer> offers = Collections.singletonList(new Offer(offer1));
        offersEnrichmentService.doUltraControllerEnrichment(offers, log::info);

        Offer expectedOffer1 = expectedOffer()
            .setCategoryIdForTests(2L, Offer.BindingKind.APPROVED)
            .setAutomaticClassification(true)
            .setClassifierCategoryId(2L, OffersEnrichmentService.MAX_CONFIDENCE)
            .setModelId(20L)
            .setSuggestSkuMapping(OfferTestUtils.mapping(30))
            .setSuggestSkuMappingType(SkutchType.SKUTCH_BY_PARAMETERS)
            .setSuggestMappingSource(Offer.SuggestMappingSource.ULTRA_CONTROLLER)
            .setSuggestModelMappingId(20L)
            .setSuggestMarketModelName("test")
            .setSuggestCategoryMappingId(2L);
        MbocAssertions.assertThat(offers.get(0)).isEqualTo(expectedOffer1);
    }

    @Test
    public void testApprovedMappingCategoryChangeTriggersYtStampUpdate() {
        Supplier supplier = OfferTestUtils.simpleSupplier();
        supplierRepository.insert(supplier);

        Offer offer = OfferTestUtils.simpleOffer()
            .setCategoryIdForTests(1L, Offer.BindingKind.SUGGESTED)
            .setModelId(10L)
            .setSuggestSkuMapping(OfferTestUtils.mapping(100))
            .setSuggestSkuMappingType(SkutchType.SKUTCH_BY_PARAMETERS)
            .updateApprovedSkuMapping(OfferTestUtils.mapping(100), CONTENT);

        Mockito.when(ultraControllerServiceRemote.enrich(any())).then(__ ->
            UltraController.DataResponse.newBuilder()
                .addOffers(ucResponse(
                    UltraController.EnrichedOffer.EnrichType.MAIN, 2, 20, 100, SkutchType.SKUTCH_BY_PARAMETERS
                ))
                .build());

        List<Offer> offers = List.of(new Offer(offer));

        offersEnrichmentService.doUltraControllerEnrichment(offers, log::info);

        Offer expectedOffer = expectedOffer()
            .setCategoryIdForTests(2L, Offer.BindingKind.SUGGESTED) // <-- category change
            .setModelId(20L)
            .setSuggestSkuMapping(OfferTestUtils.mapping(100))
            .setSuggestMappingSource(Offer.SuggestMappingSource.ULTRA_CONTROLLER)
            .setSuggestSkuMappingType(SkutchType.SKUTCH_BY_PARAMETERS)
            .setSuggestModelMappingId(20L)
            .setSuggestMarketModelName("test")
            .setSuggestCategoryMappingId(2L)
            .updateApprovedSkuMapping(OfferTestUtils.mapping(100), CONTENT);

        Offer actualOffer = offers.get(0);
        MbocAssertions.assertThat(actualOffer).isEqualToWithoutYtStamp(expectedOffer);
    }

    @Test
    public void testApprovedMappingVendorIdChangeTriggersYtStampUpdate() {
        Supplier supplier = OfferTestUtils.simpleSupplier();
        supplierRepository.insert(supplier);

        Offer offer = OfferTestUtils.simpleOffer()
            .setCategoryIdForTests(1L, Offer.BindingKind.SUGGESTED)
            .setModelId(10L)
            .setSuggestSkuMapping(OfferTestUtils.mapping(100))
            .setSuggestSkuMappingType(SkutchType.SKUTCH_BY_PARAMETERS)
            .setMarketVendorName("test")
            .setVendorId(10)
            .updateApprovedSkuMapping(OfferTestUtils.mapping(200), CONTENT)
            .markLoadedContent();

        Mockito.when(ultraControllerServiceRemote.enrich(any())).then(__ ->
            UltraController.DataResponse.newBuilder()
                .addOffers(ucResponse(
                    UltraController.EnrichedOffer.EnrichType.MAIN, 1, 20, 200, SkutchType.SKUTCH_BY_PARAMETERS
                ))
                .build());

        List<Offer> offers = List.of(new Offer(offer));

        offersEnrichmentService.doUltraControllerEnrichment(offers, log::info);

        Offer expectedOffer = expectedOffer()
            .setCategoryIdForTests(1L, Offer.BindingKind.SUGGESTED)
            .setModelId(20L)
            .setSuggestSkuMapping(OfferTestUtils.mapping(200))
            .setSuggestSkuMappingType(SkutchType.SKUTCH_BY_PARAMETERS)
            .setSuggestMappingSource(Offer.SuggestMappingSource.ULTRA_CONTROLLER)
            .setSuggestModelMappingId(20L)
            .setSuggestMarketModelName("test")
            .setSuggestCategoryMappingId(1L)
            .updateApprovedSkuMapping(OfferTestUtils.mapping(200), CONTENT);

        Offer actualOffer = offers.get(0);
        MbocAssertions.assertThat(actualOffer).isEqualToWithoutYtStamp(expectedOffer);
    }

    @Test
    public void testApprovedMappingMarketVendorNameChangeTriggersYtStampUpdate() {
        Supplier supplier = OfferTestUtils.simpleSupplier();
        supplierRepository.insert(supplier);

        Offer offer = OfferTestUtils.simpleOffer()
            .setCategoryIdForTests(1L, Offer.BindingKind.SUGGESTED)
            .setModelId(10L)
            .setSuggestSkuMapping(OfferTestUtils.mapping(100))
            .setSuggestSkuMappingType(SkutchType.SKUTCH_BY_PARAMETERS)
            .setMarketVendorName("other name")
            .setVendorId(300)
            .updateApprovedSkuMapping(OfferTestUtils.mapping(200), CONTENT)
            .markLoadedContent();

        Mockito.when(ultraControllerServiceRemote.enrich(any())).then(__ ->
            UltraController.DataResponse.newBuilder()
                .addOffers(ucResponse(
                    UltraController.EnrichedOffer.EnrichType.MAIN, 1, 20, 200, SkutchType.SKUTCH_BY_PARAMETERS
                ))
                .build());

        List<Offer> offers = List.of(new Offer(offer));

        offersEnrichmentService.doUltraControllerEnrichment(offers, log::info);

        Offer expectedOffer = expectedOffer()
            .setCategoryIdForTests(1L, Offer.BindingKind.SUGGESTED)
            .setModelId(20L)
            .setSuggestSkuMapping(OfferTestUtils.mapping(200))
            .setSuggestSkuMappingType(SkutchType.SKUTCH_BY_PARAMETERS)
            .setSuggestMappingSource(Offer.SuggestMappingSource.ULTRA_CONTROLLER)
            .setSuggestModelMappingId(20L)
            .setSuggestMarketModelName("test")
            .setSuggestCategoryMappingId(1L)
            .updateApprovedSkuMapping(OfferTestUtils.mapping(200), CONTENT);

        Offer actualOffer = offers.get(0);
        MbocAssertions.assertThat(actualOffer).isEqualToWithoutYtStamp(expectedOffer);
    }


    @Test
    public void testAutomaticClassification() {
        final String departmentName1 = "perfume";
        final String departmentName2 = "drugs";
        final double probability1 = 0.5;
        final double probability2 = 0.1;
        final double probability3 = 0.0;
        Offer offer1 = OfferTestUtils.simpleOffer()
            .setCategoryIdForTests(1L, Offer.BindingKind.SUGGESTED)
            .setModelId(10L)
            .setSuggestSkuMapping(OfferTestUtils.mapping(100))
            .markLoadedContent();
        Offer offer2 = OfferTestUtils.simpleOffer()
            .setCategoryIdForTests(1L, Offer.BindingKind.SUGGESTED)
            .setModelId(10L)
            .setSuggestSkuMapping(OfferTestUtils.mapping(100))
            .markLoadedContent();
        Offer offer3 = OfferTestUtils.simpleOffer()
            .setCategoryIdForTests(1L, Offer.BindingKind.SUGGESTED)
            .setModelId(10L)
            .setSuggestSkuMapping(OfferTestUtils.mapping(100))
            .setMappingDestination(Offer.MappingDestination.FMCG)
            .markLoadedContent();
        Offer offer4 = OfferTestUtils.simpleOffer()
            .setCategoryIdForTests(1L, Offer.BindingKind.SUGGESTED)
            .setModelId(10L)
            .setSuggestSkuMapping(OfferTestUtils.mapping(100))
            .markLoadedContent();

        UltraController.EnrichedOffer.Builder enrichedOfferDraft = defaultUcResponse();

        UltraController.EnrichedOffer enrichedOffer1 = enrichedOfferDraft
            .setClassifierConfidentTopPrecision(DEFAULT_CLASSIFIER_TRUST_THRESHOLD)
            .setClassifierCategoryId((int) BOOTS_RELATED_HID)
            .addAllHonestMarkDepartments(
                Arrays.asList(
                    Classifier.HonestMarkDepartmentProbability.newBuilder()
                        .setName(departmentName1).setProbability(probability1).build(),
                    Classifier.HonestMarkDepartmentProbability.newBuilder()
                        .setName(departmentName2).setProbability(probability2).build()
                )
            )
            .build();
        enrichedOfferDraft.clearHonestMarkDepartments();
        UltraController.EnrichedOffer enrichedOffer2 = enrichedOfferDraft
            .setClassifierConfidentTopPrecision(DEFAULT_CLASSIFIER_TRUST_THRESHOLD - 0.0001)
            .setClassifierCategoryId((int) BOOTS_RELATED_HID)
            .addAllHonestMarkDepartments(
                Collections.singletonList(
                    Classifier.HonestMarkDepartmentProbability.newBuilder()
                        .setName(departmentName1).setProbability(probability3).build()
                )
            )
            .build();
        enrichedOfferDraft.clearHonestMarkDepartments();
        UltraController.EnrichedOffer enrichedOffer3 = enrichedOfferDraft
            .setClassifierConfidentTopPrecision(1.0)
            .setClassifierCategoryId((int) BOOTS_RELATED_HID)
            .addAllHonestMarkDepartments(
                // empty
                Collections.singletonList(Classifier.HonestMarkDepartmentProbability.newBuilder().build())
            )
            .build();
        enrichedOfferDraft.clearHonestMarkDepartments();
        UltraController.EnrichedOffer enrichedOffer4 = enrichedOfferDraft
            .setClassifierConfidentTopPrecision(DEFAULT_CLASSIFIER_TRUST_THRESHOLD)
            .setClassifierCategoryId(3)
            .build();

        Mockito.when(ultraControllerServiceRemote.enrich(any())).then(__ ->
            UltraController.DataResponse.newBuilder()
                .addOffers(enrichedOffer1)
                .addOffers(enrichedOffer2)
                .addOffers(enrichedOffer3)
                .addOffers(enrichedOffer4)
                .build());

        List<Offer> offers = Stream.of(offer1, offer2, offer3, offer4)
            .map(Offer::new)
            .collect(Collectors.toList());
        offersEnrichmentService.doUltraControllerEnrichment(offers, log::info);

        Offer expectedOffer1 = expectedOffer()
            .setCategoryIdForTests(BOOTS_RELATED_HID, Offer.BindingKind.APPROVED)
            .setModelId(20L)
            .setSuggestSkuMapping(OfferTestUtils.mapping(200))
            .setSuggestMappingSource(Offer.SuggestMappingSource.ULTRA_CONTROLLER)
            .setSuggestSkuMappingType(SkutchType.SKUTCH_BY_PARAMETERS)
            .setSuggestModelMappingId(20L)
            .setSuggestMarketModelName("test")
            .setSuggestCategoryMappingId(BOOTS_RELATED_HID)
            .setAutomaticClassification(true)
            .setClassifierCategoryId((long) enrichedOffer1.getClassifierCategoryId(),
                OffersEnrichmentService.getClassifierConfidence(enrichedOffer1).get())
            .setHonestMarkDepartmentId(1)
            .setHonestMarkDepartmentProbability(probability1);
        Offer expectedOffer2 = expectedOffer()
            .setCategoryIdForTests(BOOTS_RELATED_HID, Offer.BindingKind.SUGGESTED)
            .setModelId(20L)
            .setSuggestSkuMapping(OfferTestUtils.mapping(200))
            .setSuggestMappingSource(Offer.SuggestMappingSource.ULTRA_CONTROLLER)
            .setSuggestSkuMappingType(SkutchType.SKUTCH_BY_PARAMETERS)
            .setSuggestModelMappingId(20L)
            .setSuggestMarketModelName("test")
            .setSuggestCategoryMappingId(BOOTS_RELATED_HID)
            .setAutomaticClassification(false)
            .setClassifierCategoryId((long) enrichedOffer2.getClassifierCategoryId(),
                OffersEnrichmentService.getClassifierConfidence(enrichedOffer2).get());
        Offer expectedOffer3 = expectedOffer()
            .setCategoryIdForTests(BOOTS_RELATED_HID, Offer.BindingKind.APPROVED)
            .setModelId(20L)
            .setSuggestSkuMapping(OfferTestUtils.mapping(200))
            .setSuggestMappingSource(Offer.SuggestMappingSource.ULTRA_CONTROLLER)
            .setSuggestSkuMappingType(SkutchType.SKUTCH_BY_PARAMETERS)
            .setSuggestModelMappingId(20L)
            .setSuggestMarketModelName("test")
            .setSuggestCategoryMappingId(BOOTS_RELATED_HID)
            .setMappingDestination(Offer.MappingDestination.FMCG)
            .setAutomaticClassification(true)
            .setClassifierCategoryId((long) enrichedOffer3.getClassifierCategoryId(),
                OffersEnrichmentService.getClassifierConfidence(enrichedOffer3).get());
        Offer expectedOffer4 = expectedOffer()
            .setCategoryIdForTests(BOOTS_RELATED_HID, Offer.BindingKind.SUGGESTED)
            .setModelId(20L)
            .setSuggestSkuMapping(OfferTestUtils.mapping(200))
            .setSuggestMappingSource(Offer.SuggestMappingSource.ULTRA_CONTROLLER)
            .setSuggestSkuMappingType(SkutchType.SKUTCH_BY_PARAMETERS)
            .setSuggestModelMappingId(20L)
            .setSuggestMarketModelName("test")
            .setSuggestCategoryMappingId(BOOTS_RELATED_HID)
            .setAutomaticClassification(false)
            .setClassifierCategoryId((long) enrichedOffer4.getClassifierCategoryId(),
                OffersEnrichmentService.getClassifierConfidence(enrichedOffer4).get());

        MbocAssertions.assertThat(offers.get(0)).isEqualTo(expectedOffer1);
        MbocAssertions.assertThat(offers.get(1)).isEqualTo(expectedOffer2);
        MbocAssertions.assertThat(offers.get(2)).isEqualTo(expectedOffer3);
        MbocAssertions.assertThat(offers.get(3)).isEqualTo(expectedOffer4);
    }

    @Test
    public void testHonestMarkClassificationForNotGoodContent() {
        Offer offer1 = OfferTestUtils.simpleOffer()
            .setCategoryIdForTests(BOOTS_RELATED_HID, Offer.BindingKind.SUGGESTED)
            .setModelId(10L)
            .setSuggestSkuMapping(OfferTestUtils.mapping(100));
        Offer offer2 = OfferTestUtils.simpleOffer()
            .setCategoryIdForTests(BOOTS_RELATED_HID, Offer.BindingKind.SUGGESTED)
            .setModelId(10L)
            .setSuggestSkuMapping(OfferTestUtils.mapping(100));
        Offer offer3 = OfferTestUtils.simpleOffer()
            .setCategoryIdForTests(OTHER_HID, Offer.BindingKind.SUGGESTED)
            .setModelId(10L)
            .setSuggestSkuMapping(OfferTestUtils.mapping(100));
        Offer offer4 = OfferTestUtils.simpleOffer()
            .setCategoryIdForTests(OTHER_HID, Offer.BindingKind.APPROVED)
            .setModelId(10L)
            .setSuggestSkuMapping(OfferTestUtils.mapping(100));
        Offer offer5 = OfferTestUtils.simpleOffer()
            .setCategoryIdForTests(OTHER_HID, Offer.BindingKind.SUGGESTED)
            .setModelId(10L)
            .setSuggestSkuMapping(OfferTestUtils.mapping(100));

        UltraController.EnrichedOffer.Builder enrichedOfferDraft = defaultUcResponse();

        UltraController.EnrichedOffer enrichedOffer1 = enrichedOfferDraft.clone()
            .setClassifierConfidentTopPrecision(1 - HonestMarkTestUtils.CONFIDENT_CLASSIFIER)
            .setClassifierCategoryId((int) BOOTS_RELATED_HID)
            .addAllHonestMarkDepartments(
                Collections.singletonList(
                    Classifier.HonestMarkDepartmentProbability.newBuilder()
                        .setName(BOOTS_GROUP.getName())
                        .setProbability(HonestMarkTestUtils.CONFIDENT_DEP).build()
                )
            )
            .build();

        UltraController.EnrichedOffer enrichedOffer2 = enrichedOfferDraft.clone()
            .setClassifierConfidentTopPrecision(HonestMarkTestUtils.CONFIDENT_CLASSIFIER)
            .setClassifierCategoryId((int) BOOTS_RELATED_HID)
            .build();

        UltraController.EnrichedOffer enrichedOffer3 = enrichedOfferDraft.clone()
            .setClassifierConfidentTopPrecision(1 - HonestMarkTestUtils.CONFIDENT_CLASSIFIER)
            .setClassifierCategoryId((int) BOOTS_RELATED_HID)
            .addAllHonestMarkDepartments(Collections.emptyList())
            .build();

        UltraController.EnrichedOffer enrichedOffer4 = enrichedOfferDraft.clone()
            .setClassifierConfidentTopPrecision(HonestMarkTestUtils.CONFIDENT_CLASSIFIER)
            .setCategoryId((int) OTHER_HID)
            .setClassifierCategoryId((int) OTHER_HID)
            .build();
        UltraController.EnrichedOffer enrichedOffer5 = enrichedOfferDraft.clone()
            .setClassifierConfidentTopPrecision(1 - HonestMarkTestUtils.CONFIDENT_CLASSIFIER)
            .setClassifierCategoryId((int) OTHER_HID)
            .setCategoryId((int) OTHER_HID)
            .addAllHonestMarkDepartments(
                Collections.singletonList(
                    Classifier.HonestMarkDepartmentProbability.newBuilder()
                        .setName(HonestMarkDepartment.OTHER.getName())
                        .setProbability(HonestMarkTestUtils.CONFIDENT_DEP).build()
                )
            )
            .build();

        Mockito.when(ultraControllerServiceRemote.enrich(any())).then(__ ->
            UltraController.DataResponse.newBuilder()
                .addOffers(enrichedOffer1)
                .addOffers(enrichedOffer2)
                .addOffers(enrichedOffer3)
                .addOffers(enrichedOffer4)
                .addOffers(enrichedOffer5)
                .build());

        categoryCachingServiceMock.addCategory(new Category().setCategoryId(BOOTS_RELATED_HID)
            .setAcceptGoodContent(false));
        categoryCachingServiceMock.addCategory(new Category().setCategoryId(OTHER_HID).setAcceptGoodContent(false));

        List<Offer> offers = Stream.of(offer1, offer2, offer3, offer4, offer5)
            .map(Offer::new)
            .collect(Collectors.toList());
        offersEnrichmentService.doUltraControllerEnrichment(offers, log::info);

        // we can just check whether the offer is autoclassified or not:
        Assertions.assertThat(offers.stream().map(Offer::isAutomaticClassification).collect(Collectors.toList()))
            .containsExactly(
                false,
                true,
                false,
                false,
                false
            );
    }

    @Test
    public void testAutoClassificationIsFalseWhenUcAndRcUnconfident() {
        Offer offer = OfferTestUtils.simpleOffer()
            .setBusinessId(newPipelineSupplier.getId())
            .setCategoryIdForTests(BOOTS_RELATED_HID, Offer.BindingKind.SUGGESTED)
            .setModelId(10L)
            .setSuggestSkuMapping(OfferTestUtils.mapping(100));

        UltraController.EnrichedOffer enrichedOffer = defaultUcResponse()
            .setClassifierConfidentTopPrecision(HonestMarkTestUtils.NOT_CONFIDENT_CLASSIFIER)
            .setClassifierCategoryId((int) BOOTS_RELATED_HID)
            .addAllHonestMarkDepartments(
                Arrays.asList(
                    Classifier.HonestMarkDepartmentProbability.newBuilder()
                        .setName(BOOTS_GROUP.getName())
                        .setProbability(HonestMarkTestUtils.NOT_CONFIDENT_DEP).build(),
                    Classifier.HonestMarkDepartmentProbability.newBuilder()
                        .setName(DRUGS_GROUP.getName())
                        .setProbability(1 - HonestMarkTestUtils.NOT_CONFIDENT_DEP).build()
                )
            )
            .build();

        Mockito.when(ultraControllerServiceRemote.enrich(any())).then(__ ->
            UltraController.DataResponse.newBuilder()
                .addOffers(enrichedOffer)
                .build());

        testHonestMarkClassification(offer, enrichedOffer, false);
    }

    @Test
    public void testAutoClassificationIsTrueWhenUcConfidentAndRcUnconfident() {
        Offer offer = OfferTestUtils.simpleOffer()
            .setCategoryIdForTests(BOOTS_RELATED_HID, Offer.BindingKind.SUGGESTED)
            .setModelId(10L)
            .setSuggestSkuMapping(OfferTestUtils.mapping(100));

        UltraController.EnrichedOffer enrichedOffer = defaultUcResponse()
            .setClassifierConfidentTopPrecision(HonestMarkTestUtils.CONFIDENT_CLASSIFIER)
            .setClassifierCategoryId((int) BOOTS_RELATED_HID)
            .addAllHonestMarkDepartments(
                Collections.singletonList(
                    Classifier.HonestMarkDepartmentProbability.newBuilder()
                        .setName(BOOTS_GROUP.getName())
                        .setProbability(HonestMarkTestUtils.NOT_CONFIDENT_DEP).build()
                )
            )
            .build();

        Mockito.when(ultraControllerServiceRemote.enrich(any())).then(__ ->
            UltraController.DataResponse.newBuilder()
                .addOffers(enrichedOffer)
                .build());

        testHonestMarkClassification(offer, enrichedOffer, true);
    }

    @Test
    public void testAutoClassificationIsTrueWhenUcUnconfidentAndRcConfidentWithRelatedDept() {
        Offer offer = OfferTestUtils.simpleOffer()
            .setBusinessId(newPipelineSupplier.getId())
            .setCategoryIdForTests(BOOTS_RELATED_HID, Offer.BindingKind.SUGGESTED)
            .setModelId(10L)
            .setSuggestSkuMapping(OfferTestUtils.mapping(100))
            .setMappingDestination(Offer.MappingDestination.FMCG);

        UltraController.EnrichedOffer enrichedOffer = defaultUcResponse()
            .setClassifierConfidentTopPrecision(HonestMarkTestUtils.NOT_CONFIDENT_CLASSIFIER)
            .setClassifierCategoryId((int) BOOTS_RELATED_HID)
            .addAllHonestMarkDepartments(
                Collections.singletonList(
                    Classifier.HonestMarkDepartmentProbability.newBuilder()
                        .setName(BOOTS_GROUP.getName())
                        .setProbability(HonestMarkTestUtils.CONFIDENT_DEP).build()
                )
            )
            .build();

        Mockito.when(ultraControllerServiceRemote.enrich(any())).then(__ ->
            UltraController.DataResponse.newBuilder()
                .addOffers(enrichedOffer)
                .build());

        testHonestMarkClassification(offer, enrichedOffer, true);
    }

    @Test
    public void testAutoClassificationIsTrueWhenUcAndRcConfindent() {
        Offer offer = OfferTestUtils.simpleOffer()
            .setBusinessId(newPipelineSupplier.getId())
            .setCategoryIdForTests(BOOTS_RELATED_HID, Offer.BindingKind.SUGGESTED)
            .setModelId(10L)
            .setSuggestSkuMapping(OfferTestUtils.mapping(100));

        UltraController.EnrichedOffer enrichedOffer = defaultUcResponse()
            .setClassifierConfidentTopPrecision(HonestMarkTestUtils.CONFIDENT_CLASSIFIER)
            .setClassifierCategoryId((int) BOOTS_RELATED_HID)
            .addAllHonestMarkDepartments(
                Collections.singletonList(
                    Classifier.HonestMarkDepartmentProbability.newBuilder()
                        .setName(BOOTS_GROUP.getName())
                        .setProbability(HonestMarkTestUtils.CONFIDENT_DEP).build()
                )
            )
            .build();

        Mockito.when(ultraControllerServiceRemote.enrich(any())).then(__ ->
            UltraController.DataResponse.newBuilder()
                .addOffers(enrichedOffer)
                .build());

        testHonestMarkClassification(offer, enrichedOffer, true);
    }

    @Test
    public void testAutoClassificationIsFalseWhenUcUnconfidentAndRcConfidentNoRelatedDept() {
        Offer offer = OfferTestUtils.simpleOffer()
            .setBusinessId(newPipelineSupplier.getId())
            .setCategoryIdForTests(BOOTS_RELATED_HID, Offer.BindingKind.SUGGESTED)
            .setModelId(10L)
            .setSuggestSkuMapping(OfferTestUtils.mapping(100));

        UltraController.EnrichedOffer enrichedOffer = defaultUcResponse()
            .setClassifierConfidentTopPrecision(HonestMarkTestUtils.NOT_CONFIDENT_CLASSIFIER)
            .setClassifierCategoryId((int) BOOTS_RELATED_HID)
            .addAllHonestMarkDepartments(
                Collections.singletonList(
                    Classifier.HonestMarkDepartmentProbability.newBuilder()
                        .setName(DRUGS_GROUP.getName())
                        .setProbability(HonestMarkTestUtils.CONFIDENT_DEP).build()
                )
            )
            .build();

        Mockito.when(ultraControllerServiceRemote.enrich(any())).then(__ ->
            UltraController.DataResponse.newBuilder()
                .addOffers(enrichedOffer)
                .build());

        testHonestMarkClassification(offer, enrichedOffer, false);
    }

    @Test
    public void testAutoClassificationIsTrueWhenUcUnconfidentRcConfidentWithOtherDept() {
        Offer offer = OfferTestUtils.simpleOffer()
            .setBusinessId(newPipelineSupplier.getId())
            .setCategoryIdForTests(OTHER_HID, Offer.BindingKind.SUGGESTED)
            .setModelId(10L)
            .setSuggestSkuMapping(OfferTestUtils.mapping(100));

        UltraController.EnrichedOffer enrichedOffer = defaultUcResponse()
            .setClassifierConfidentTopPrecision(HonestMarkTestUtils.NOT_CONFIDENT_CLASSIFIER)
            .setClassifierCategoryId((int) OTHER_HID)
            .setCategoryId((int) OTHER_HID)
            .addAllHonestMarkDepartments(
                Collections.singletonList(
                    Classifier.HonestMarkDepartmentProbability.newBuilder()
                        .setName(HonestMarkDepartment.OTHER.getName())
                        .setProbability(HonestMarkTestUtils.CONFIDENT_DEP).build()
                )
            )
            .build();

        Mockito.when(ultraControllerServiceRemote.enrich(any())).then(__ ->
            UltraController.DataResponse.newBuilder()
                .addOffers(enrichedOffer)
                .build());

        testHonestMarkClassification(offer, enrichedOffer, true);
    }

    @Test
    public void testAutoClassificationIsTrueWhenUcConfidentRcHasNoDepartments() {
        Offer offer = OfferTestUtils.simpleOffer()
            .setBusinessId(newPipelineSupplier.getId())
            .setCategoryIdForTests(BOOTS_RELATED_HID, Offer.BindingKind.SUGGESTED)
            .setModelId(10L)
            .setSuggestSkuMapping(OfferTestUtils.mapping(100));

        UltraController.EnrichedOffer enrichedOffer = defaultUcResponse()
            .setClassifierConfidentTopPrecision(HonestMarkTestUtils.CONFIDENT_CLASSIFIER)
            .setClassifierCategoryId((int) BOOTS_RELATED_HID)
            .build();

        Mockito.when(ultraControllerServiceRemote.enrich(any())).then(__ ->
            UltraController.DataResponse.newBuilder()
                .addOffers(enrichedOffer)
                .build());

        testHonestMarkClassification(offer, enrichedOffer, true);
    }

    @Test
    public void testAutoClassificationIsFalseWhenDepartmentDoesNotExist() {
        Offer offer = OfferTestUtils.simpleOffer()
            .setBusinessId(newPipelineSupplier.getId())
            .setCategoryIdForTests(BOOTS_RELATED_HID, Offer.BindingKind.SUGGESTED)
            .setModelId(10L)
            .setSuggestSkuMapping(OfferTestUtils.mapping(100));

        UltraController.EnrichedOffer enrichedOffer = defaultUcResponse()
            .setClassifierConfidentTopPrecision(HonestMarkTestUtils.NOT_CONFIDENT_CLASSIFIER)
            .setClassifierCategoryId((int) BOOTS_RELATED_HID)
            .addAllHonestMarkDepartments(
                Collections.singletonList(
                    Classifier.HonestMarkDepartmentProbability.newBuilder()
                        .setName("not_existing")
                        .setProbability(HonestMarkTestUtils.CONFIDENT_DEP).build()
                )
            )
            .build();

        Mockito.when(ultraControllerServiceRemote.enrich(any())).then(__ ->
            UltraController.DataResponse.newBuilder()
                .addOffers(enrichedOffer)
                .build());

        testHonestMarkClassification(offer, enrichedOffer, false);
    }

    @Test
    public void testAutoClassificationIsTrueWhenCategoryIdIsNullUcUnconfidentRcConfidentWithOtherDept() {
        Offer offer = OfferTestUtils.simpleOffer()
            .setBusinessId(newPipelineSupplier.getId())
            .setCategoryIdForTests(null, null)
            .setModelId(10L)
            .setSuggestSkuMapping(OfferTestUtils.mapping(100));

        UltraController.EnrichedOffer enrichedOffer = defaultUcResponse()
            .setClassifierConfidentTopPrecision(HonestMarkTestUtils.NOT_CONFIDENT_CLASSIFIER)
            .setClassifierCategoryId((int) OTHER_HID)
            .setCategoryId((int) OTHER_HID)
            .addAllHonestMarkDepartments(
                Collections.singletonList(
                    Classifier.HonestMarkDepartmentProbability.newBuilder()
                        .setName(HonestMarkDepartment.OTHER.getName())
                        .setProbability(HonestMarkTestUtils.CONFIDENT_DEP).build()
                )
            )
            .build();

        Mockito.when(ultraControllerServiceRemote.enrich(any())).then(__ ->
            UltraController.DataResponse.newBuilder()
                .addOffers(enrichedOffer)
                .build());

        testHonestMarkClassification(offer, enrichedOffer, true);
    }

    public void testHonestMarkClassification(Offer offer,
                                             UltraController.EnrichedOffer enrichedOffer,
                                             boolean expectedIsAutoClassification
    ) {
        Mockito.when(ultraControllerServiceRemote.enrich(any())).then(__ ->
            UltraController.DataResponse.newBuilder()
                .addOffers(enrichedOffer)
                .build());

        offersEnrichmentService.doUltraControllerEnrichment(List.of(offer), log::info);

        Assertions.assertThat(offer.isAutomaticClassification()).isEqualTo(expectedIsAutoClassification);
    }

    @Test
    public void testAutomaticClassificationForSupplierCategory() {
        Offer offer1 = OfferTestUtils.simpleOffer(1)
            .setCategoryIdForTests(1L, Offer.BindingKind.SUPPLIER)
            .setModelId(20L)
            .setSuggestSkuMapping(OfferTestUtils.mapping(200))
            .setSuggestSkuMappingType(SkutchType.SKUTCH_BY_PARAMETERS)
            .setMarketModelName("test")
            .setSuggestMappingSource(Offer.SuggestMappingSource.ULTRA_CONTROLLER);
        Offer offer2 = OfferTestUtils.simpleOffer(2)
            .setCategoryIdForTests(1L, Offer.BindingKind.SUPPLIER)
            .setModelId(20L)
            .setSuggestSkuMapping(OfferTestUtils.mapping(200))
            .setSuggestSkuMappingType(SkutchType.SKUTCH_BY_PARAMETERS)
            .setMarketModelName("test")
            .setSuggestMappingSource(Offer.SuggestMappingSource.ULTRA_CONTROLLER);
        Offer offer3 = OfferTestUtils.simpleOffer(3)
            .setCategoryIdForTests(BOOTS_RELATED_HID, Offer.BindingKind.SUPPLIER)
            .setModelId(10L)
            .setSuggestSkuMapping(OfferTestUtils.mapping(200))
            .setSuggestSkuMappingType(SkutchType.SKUTCH_BY_PARAMETERS)
            .setSuggestMappingSource(Offer.SuggestMappingSource.ULTRA_CONTROLLER);

        UltraController.EnrichedOffer.Builder enrichedOfferDraft = defaultUcResponse();

        UltraController.EnrichedOffer enrichedOffer1 = enrichedOfferDraft
            .setClassifierConfidentTopPrecision(1.0)
            .setClassifierCategoryId((int) BOOTS_RELATED_HID)
            .build();
        UltraController.EnrichedOffer enrichedOffer2 = enrichedOfferDraft
            .setClassifierConfidentTopPrecision(0.0)
            .setClassifierCategoryId((int) BOOTS_RELATED_HID)
            .build();
        UltraController.EnrichedOffer enrichedOffer3 = enrichedOfferDraft
            .setClassifierConfidentTopPrecision(0.0)
            .setClassifierCategoryId((int) BOOTS_RELATED_HID)
            .build();

        Mockito.when(ultraControllerServiceRemote.enrich(any())).then(__ ->
            UltraController.DataResponse.newBuilder()
                .addOffers(enrichedOffer1)
                .addOffers(enrichedOffer2)
                .addOffers(enrichedOffer3)
                .build());

        List<Offer> offers = Stream.of(offer1, offer2, offer3)
            .map(Offer::new)
            .collect(Collectors.toList());
        offersEnrichmentService.doUltraControllerEnrichment(offers, log::info);


        // nothing changed
        MbocAssertions.assertThat(offers.get(0)).isEqualTo(offer1.copy()
            .setSuggestModelMappingId(20L)
            .setSuggestMarketModelName("test")
            .setSuggestCategoryMappingId(BOOTS_RELATED_HID)
            .setAutomaticClassification(false)
            .setClassifierCategoryId((long) enrichedOffer3.getClassifierCategoryId(), 1.0));
        MbocAssertions.assertThat(offers.get(1)).isEqualTo(offer2.copy()
            .setSuggestModelMappingId(20L)
            .setSuggestMarketModelName("test")
            .setSuggestCategoryMappingId(BOOTS_RELATED_HID)
            .setAutomaticClassification(false)
            .setClassifierCategoryId((long) enrichedOffer3.getClassifierCategoryId(), 0.0));
        // change for same category
        MbocAssertions.assertThat(offers.get(2)).isEqualTo(expectedOffer(3)
            .setCategoryIdForTests(BOOTS_RELATED_HID, Offer.BindingKind.SUPPLIER)
            .setModelId(20L)
            .setSuggestSkuMapping(OfferTestUtils.mapping(200))
            .setSuggestSkuMappingType(SkutchType.SKUTCH_BY_PARAMETERS)
            .setSuggestMappingSource(Offer.SuggestMappingSource.ULTRA_CONTROLLER)
            .setSuggestModelMappingId(20L)
            .setSuggestMarketModelName("test")
            .setSuggestCategoryMappingId(BOOTS_RELATED_HID)
            .setAutomaticClassification(false)
            .setClassifierCategoryId((long) enrichedOffer3.getClassifierCategoryId(), 0.0)
        );
    }

    @Test
    public void testAutomaticClassificationWontSetIfCategoryHasNoKnowledge() {
        Offer offer = OfferTestUtils.simpleOffer()
            .setCategoryIdForTests(1L, Offer.BindingKind.SUGGESTED)
            .setModelId(10L)
            .setSuggestSkuMapping(OfferTestUtils.mapping(100))
            .markLoadedContent();

        Mockito.when(ultraControllerServiceRemote.enrich(any())).then(__ ->
            UltraController.DataResponse.newBuilder()
                .addOffers(ucResponse(
                    UltraController.EnrichedOffer.EnrichType.MAIN, 2, 20, 200, SkutchType
                        .SKUTCH_BY_PARAMETERS).setClassifierConfidentTopPrecision(DEFAULT_CLASSIFIER_TRUST_THRESHOLD))
                .build());

        offersEnrichmentService.doUltraControllerEnrichment(Collections.singletonList(offer), log::info);

        MbocAssertions.assertThat(offer).hasAutomaticClassification(false);
    }

    @Test
    public void testClassifierCategoryAndPrecisionOverwritten() {
        Offer offer = OfferTestUtils.simpleOffer()
            .setCategoryIdForTests(1L, Offer.BindingKind.SUGGESTED)
            .setModelId(10L)
            .setSuggestSkuMapping(OfferTestUtils.mapping(100));

        UltraController.EnrichedOffer.Builder enrichedOffer = ucResponse(UltraController.EnrichedOffer.EnrichType.MAIN,
            2, 20, 200, SkutchType.SKUTCH_BY_PARAMETERS)
            .setClassifierConfidentTopPrecision(DEFAULT_CLASSIFIER_TRUST_THRESHOLD)
            .setClassifierCategoryId(2);

        Mockito.when(ultraControllerServiceRemote.enrich(any())).then(__ ->
            UltraController.DataResponse.newBuilder()
                .addOffers(enrichedOffer)
                .build());

        // first call
        List<Offer> offers = List.of(offer);
        offersEnrichmentService.doUltraControllerEnrichment(offers, log::info);

        MbocAssertions.assertThat(offer).hasClassifierCategoryId(2);
        MbocAssertions.assertThat(offer).hasClassifierConfidence(DEFAULT_CLASSIFIER_TRUST_THRESHOLD);

        final int newClassifierCategory = 3;
        final double newClassifierConfidence = DEFAULT_CLASSIFIER_TRUST_THRESHOLD - 0.01;
        Mockito.when(ultraControllerServiceRemote.enrich(any())).then(__ ->
            UltraController.DataResponse.newBuilder()
                .addOffers(enrichedOffer
                    .setCategoryId(3)
                    .setClassifierCategoryId(newClassifierCategory)
                    .setClassifierConfidentTopPrecision(newClassifierConfidence)
                ).build());

        // second call, with ClassifierCategoryId and ClassifierConfidentTopPrecision changed
        offersEnrichmentService.doUltraControllerEnrichment(offers, log::info);

        // assert classifier is updated
        MbocAssertions.assertThat(offer).hasClassifierCategoryId(newClassifierCategory);
        MbocAssertions.assertThat(offer).hasClassifierConfidence(newClassifierConfidence);

        // check that it does not update partially
        final int newPartialClassifierCategory = 4;
        Mockito.when(ultraControllerServiceRemote.enrich(any())).then(__ ->
            UltraController.DataResponse.newBuilder()
                .addOffers(enrichedOffer
                    .setCategoryId(3)
                    .setClassifierCategoryId(newPartialClassifierCategory)
                    // without confidence
                    .clearClassifierConfidentTopPrecision()
                ).build());

        offersEnrichmentService.doUltraControllerEnrichment(offers, log::info);

        // assert nothing changed
        MbocAssertions.assertThat(offer).hasClassifierCategoryId(newClassifierCategory);
        MbocAssertions.assertThat(offer).hasClassifierConfidence(newClassifierConfidence);
    }

    @Test
    public void testFMCGOffersHasNewSuggestedMapping() {
        Offer offer1 = OfferTestUtils.simpleOffer()
            .setCategoryIdForTests(1L, Offer.BindingKind.SUGGESTED)
            .setModelId(10L)
            .setSuggestSkuMapping(OfferTestUtils.mapping(100))
            .setMappingDestination(Offer.MappingDestination.FMCG);
        Offer offer2 = OfferTestUtils.simpleOffer()
            .setCategoryIdForTests(1L, Offer.BindingKind.SUGGESTED)
            .setModelId(10L)
            .setSuggestSkuMapping(OfferTestUtils.mapping(100))
            .updateApprovedSkuMapping(OfferTestUtils.mapping(100), CONTENT)
            .setMappingDestination(Offer.MappingDestination.FMCG);
        Offer offer3 = OfferTestUtils.simpleOffer() // странный кейс, тут все уже заполнено
            .setCategoryIdForTests(1L, Offer.BindingKind.APPROVED)
            .setModelId(20L)
            .setSuggestSkuMapping(OfferTestUtils.mapping(200))
            .setSuggestSkuMappingType(SkutchType.BARCODE_SKUTCH)
            .setMappingDestination(Offer.MappingDestination.FMCG)
            .setMarketModelName("test")
            .setSuggestMappingSource(Offer.SuggestMappingSource.ULTRA_CONTROLLER);
        Offer offer4 = OfferTestUtils.simpleOffer()
            .setCategoryIdForTests(1L, Offer.BindingKind.APPROVED)
            .setModelId(10L)
            .setSuggestSkuMapping(OfferTestUtils.mapping(100))
            .setSuggestSkuMappingType(SkutchType.SKUTCH_BY_PARAMETERS)
            .setMappingDestination(Offer.MappingDestination.FMCG);
        Offer offer5 = OfferTestUtils.simpleOffer()
            .setCategoryIdForTests(2L, Offer.BindingKind.SUGGESTED)
            .setModelId(2L)
            .setSuggestSkuMapping(OfferTestUtils.mapping(100))
            .setSuggestSkuMappingType(SkutchType.SKUTCH_BY_PARAMETERS)
            .setMappingDestination(Offer.MappingDestination.FMCG);

        Mockito.when(ultraControllerServiceRemote.enrich(any())).then(__ ->
            UltraController.DataResponse.newBuilder()
                .addOffers(ucResponse(
                    UltraController.EnrichedOffer.EnrichType.MAIN, 2, 20, 200, SkutchType.BARCODE_SKUTCH
                ))
                .addOffers(ucResponse(
                    UltraController.EnrichedOffer.EnrichType.MAIN, 2, 20, 100, SkutchType.BARCODE_SKUTCH
                ))
                .addOffers(ucResponse(
                    UltraController.EnrichedOffer.EnrichType.MAIN, 2, 20, 200, SkutchType.BARCODE_SKUTCH
                ))
                .addOffers(ucResponse(
                    UltraController.EnrichedOffer.EnrichType.MAIN, 1, 11, 111, SkutchType.BARCODE_SKUTCH
                ))
                .addOffers(ucResponse(
                    UltraController.EnrichedOffer.EnrichType.MAIN, 1, 1, 112, SkutchType.VENDOR_CODE_SKUTCH,
                    true, false
                ))
                .build());
        List<Offer> offers = Arrays.asList(new Offer(offer1), new Offer(offer2), new Offer(offer3),
            new Offer(offer4), new Offer(offer5));
        offersEnrichmentService.doUltraControllerEnrichment(offers, log::info);

        Offer expectedOffer1 = expectedOffer()
            .setCategoryIdForTests(2L, Offer.BindingKind.SUGGESTED)
            .setModelId(20L)
            .setSuggestSkuMapping(OfferTestUtils.mapping(200))
            .setSuggestMappingSource(Offer.SuggestMappingSource.ULTRA_CONTROLLER)
            .setSuggestSkuMappingType(SkutchType.BARCODE_SKUTCH)
            .setSuggestModelMappingId(20L)
            .setSuggestMarketModelName("test")
            .setSuggestCategoryMappingId(2L)
            .setMappingDestination(Offer.MappingDestination.FMCG);
        Offer expectedOffer2 = expectedOffer()
            .setCategoryIdForTests(2L, Offer.BindingKind.SUGGESTED)
            .setModelId(20L)
            .setSuggestSkuMapping(OfferTestUtils.mapping(100))
            .setSuggestMappingSource(Offer.SuggestMappingSource.ULTRA_CONTROLLER)
            .setSuggestSkuMappingType(SkutchType.BARCODE_SKUTCH)
            .setSuggestModelMappingId(20L)
            .setSuggestMarketModelName("test")
            .setSuggestCategoryMappingId(2L)
            .updateApprovedSkuMapping(OfferTestUtils.mapping(100), CONTENT)
            .setMappingDestination(Offer.MappingDestination.FMCG);
        Offer expectedOffer3 = offer3.copy()
            .setSuggestModelMappingId(20L)
            .setSuggestMarketModelName("test")
            .setSuggestCategoryMappingId(2L);
        Offer expectedOffer4 = expectedOffer()
            .setCategoryIdForTests(1L, Offer.BindingKind.APPROVED)
            .setModelId(11L)
            .setSuggestSkuMapping(OfferTestUtils.mapping(111))
            .setSuggestMappingSource(Offer.SuggestMappingSource.ULTRA_CONTROLLER)
            .setSuggestSkuMappingType(SkutchType.BARCODE_SKUTCH)
            .setSuggestModelMappingId(11L)
            .setSuggestMarketModelName("test")
            .setSuggestCategoryMappingId(1L)
            .setMappingDestination(Offer.MappingDestination.FMCG);
        Offer expectedOffer5 = expectedOffer()
            .setCategoryIdForTests(1L, Offer.BindingKind.SUGGESTED)
            .setMarketModelName(null)
            .setMappingDestination(Offer.MappingDestination.FMCG);
        MbocAssertions.assertThat(offers.get(0)).isEqualTo(expectedOffer1);
        MbocAssertions.assertThat(offers.get(1)).isEqualToWithoutYtStamp(expectedOffer2);
        MbocAssertions.assertThat(offers.get(2)).isEqualTo(expectedOffer3);
        MbocAssertions.assertThat(offers.get(3)).isEqualTo(expectedOffer4);
        MbocAssertions.assertThat(offers.get(4)).isEqualTo(expectedOffer5);
    }

    @Test
    public void testFMCGOffersApplyAllSuggestedMappingTypes() {
        for (SkutchType skutchType : SkutchType.values()) {
            Offer offer = OfferTestUtils.simpleOffer()
                .setCategoryIdForTests(1L, Offer.BindingKind.SUGGESTED)
                .setModelId(10L)
                .setSuggestSkuMapping(OfferTestUtils.mapping(100))
                .setSuggestMappingSource(Offer.SuggestMappingSource.ULTRA_CONTROLLER)
                .setSuggestSkuMappingType(SkutchType.BARCODE_SKUTCH)
                .setMappingDestination(Offer.MappingDestination.FMCG)
                .markLoadedContent();

            Mockito.when(ultraControllerServiceRemote.enrich(any())).then(__ ->
                UltraController.DataResponse.newBuilder()
                    .addOffers(ucResponse(
                        UltraController.EnrichedOffer.EnrichType.MAIN, 2, 20, 200, skutchType
                    ))
                    .build());
            List<Offer> offers = Collections.singletonList(new Offer(offer));
            offersEnrichmentService.doUltraControllerEnrichment(offers, log::info);

            Offer expectedOffer = expectedOffer()
                .setCategoryIdForTests(2L, Offer.BindingKind.SUGGESTED)
                .setModelId(20L)
                .setSuggestSkuMapping(OfferTestUtils.mapping(200))
                .setSuggestMappingSource(Offer.SuggestMappingSource.ULTRA_CONTROLLER)
                .setSuggestSkuMappingType(skutchType)
                .setSuggestModelMappingId(20L)
                .setSuggestMarketModelName("test")
                .setSuggestCategoryMappingId(2L)
                .setMappingDestination(Offer.MappingDestination.FMCG);

            MbocAssertions.assertThat(offers.get(0)).isEqualTo(expectedOffer);
        }
    }

    @Test
    public void testMinusOneSku() {
        Offer offer1 = OfferTestUtils.simpleOffer()
            .setBindingKind(Offer.BindingKind.SUGGESTED)
            .markLoadedContent();
        Offer offer2 = OfferTestUtils.simpleOffer()
            .setCategoryIdForTests(1L, Offer.BindingKind.SUGGESTED)
            .setModelId(10L)
            .setSuggestSkuMapping(OfferTestUtils.mapping(100))
            .setSuggestSkuMappingType(SkutchType.SKUTCH_BY_PARAMETERS)
            .markLoadedContent();
        Offer offer3 = OfferTestUtils.simpleOffer()
            .setCategoryIdForTests(1L, Offer.BindingKind.SUGGESTED)
            .setModelId(10L)
            .setSuggestSkuMapping(OfferTestUtils.mapping(100))
            .setSuggestSkuMappingType(SkutchType.SKUTCH_BY_PARAMETERS)
            .markLoadedContent();

        Mockito.when(ultraControllerServiceRemote.enrich(any())).then(__ ->
            UltraController.DataResponse.newBuilder()
                .addOffers(ucResponse(UltraController.EnrichedOffer.EnrichType.MAIN, 2, -1, -1, null))
                .addOffers(ucResponse(UltraController.EnrichedOffer.EnrichType.MAIN, 1, -1, -1, null))
                .addOffers(ucResponse(UltraController.EnrichedOffer.EnrichType.MAIN, 1, 10, -1, null))
                .build());

        List<Offer> offers = Arrays.asList(offer1.copy(), offer2.copy(), offer3.copy());
        offersEnrichmentService.doUltraControllerEnrichment(offers, log::info);

        Offer expectedOffer1 = expectedOffer()
            .setCategoryIdForTests(2L, Offer.BindingKind.SUGGESTED)
            .setSuggestCategoryMappingId(2L)
            .setMarketModelName(null);
        Offer expectedOffer2 = expectedOffer()
            .setCategoryIdForTests(1L, Offer.BindingKind.SUGGESTED)
            .setSuggestCategoryMappingId(1L)
            .setMarketModelName(null);
        Offer expectedOffer3 = expectedOffer()
            .setCategoryIdForTests(1L, Offer.BindingKind.SUGGESTED)
            .setSuggestCategoryMappingId(1L)
            .setSuggestModelMappingId(10L)
            .setSuggestMarketModelName("test")
            .setModelId(10L);

        MbocAssertions.assertThat(offers.get(0)).isEqualTo(expectedOffer1);
        MbocAssertions.assertThat(offers.get(1)).isEqualTo(expectedOffer2);
        MbocAssertions.assertThat(offers.get(2)).isEqualTo(expectedOffer3);
    }

    @Test
    public void testApprovedSkuResponse() {
        Offer offer1 = OfferTestUtils.simpleOffer()
            .setCategoryIdForTests(1L, Offer.BindingKind.SUGGESTED)
            .setModelId(10L)
            .setSuggestSkuMapping(OfferTestUtils.mapping(100))
            .setSuggestSkuMappingType(SkutchType.SKUTCH_BY_PARAMETERS);
        Offer offer2 = OfferTestUtils.simpleOffer()
            .setCategoryIdForTests(1L, Offer.BindingKind.APPROVED)
            .setModelId(10L)
            .setSuggestSkuMapping(OfferTestUtils.mapping(100));
        Offer offer3 = OfferTestUtils.simpleOffer()
            .setCategoryIdForTests(1L, Offer.BindingKind.APPROVED)
            .setModelId(10L)
            .setSuggestSkuMapping(OfferTestUtils.mapping(100))
            .updateApprovedSkuMapping(OfferTestUtils.mapping(200), CONTENT);

        Mockito.when(ultraControllerServiceRemote.enrich(any())).then(__ ->
            UltraController.DataResponse.newBuilder()
                .addOffers(ucResponse(
                    UltraController.EnrichedOffer.EnrichType.APPROVED_SKU, 2, 20, 200, SkutchType.SKUTCH_BY_PARAMETERS
                ))
                .addOffers(ucResponse(
                    UltraController.EnrichedOffer.EnrichType.APPROVED_SKU, 2, 20, 200, SkutchType.SKUTCH_BY_PARAMETERS
                ))
                .addOffers(ucResponse(
                    UltraController.EnrichedOffer.EnrichType.APPROVED_SKU, 2, 20, 200, SkutchType.SKUTCH_BY_PARAMETERS
                ))
                .build());

        List<Offer> offersEnrich = Arrays.asList(new Offer(offer1), new Offer(offer2), new Offer(offer3));
        offersEnrichmentService.doUltraControllerEnrichment(offersEnrich, log::info);

        Offer expectedOffer3 = expectedOffer()
            .setCategoryIdForTests(2L, Offer.BindingKind.APPROVED)
            .setModelId(20L)
            .setSuggestSkuMapping(OfferTestUtils.mapping(100))
            .setSuggestModelMappingId(20L)
            .setSuggestMarketModelName("test")
            .setSuggestCategoryMappingId(2L)
            .updateApprovedSkuMapping(OfferTestUtils.mapping(200), CONTENT);
        MbocAssertions.assertThat(offersEnrich)
            .usingElementComparatorIgnoringFields("isOfferContentPresent")
            .containsExactly(offer1, offer2, expectedOffer3);
    }

    @Test
    public void testApprovedModelResponse() {
        Offer offer1 = OfferTestUtils.simpleOffer()
            .setCategoryIdForTests(1L, Offer.BindingKind.SUGGESTED)
            .setModelId(10L)
            .setSuggestSkuMapping(OfferTestUtils.mapping(100));
        Offer offer2 = OfferTestUtils.simpleOffer()
            .setCategoryIdForTests(1L, Offer.BindingKind.SUPPLIER)
            .setModelId(10L)
            .setSuggestSkuMapping(OfferTestUtils.mapping(100));
        Offer offer3 = OfferTestUtils.simpleOffer()
            .setCategoryIdForTests(1L, Offer.BindingKind.APPROVED)
            .setModelId(10L)
            .setSuggestSkuMapping(OfferTestUtils.mapping(100));

        List<Offer> offersEnrich = Stream.of(offer1, offer2, offer3)
            .map(Offer::copy)
            .collect(Collectors.toList());

        Mockito.when(ultraControllerServiceRemote.enrich(any())).then(__ ->
            UltraController.DataResponse.newBuilder()
                .addAllOffers(offersEnrich.stream()
                    .map(offer -> ucResponse(
                        UltraController.EnrichedOffer.EnrichType.APPROVED_MODEL, 2, 20, 200,
                        SkutchType.SKUTCH_BY_PARAMETERS
                    ).build())
                    .collect(Collectors.toList()))
                .build());

        offersEnrichmentService.doUltraControllerEnrichment(offersEnrich, log::info);

        // nothing changed
        MbocAssertions.assertThat(offersEnrich).containsExactly(offer1, offer2, offer3);
    }

    @Test
    public void testApprovedCategoryResponse() {
        Offer offer1 = OfferTestUtils.simpleOffer(1)
            .setMappedCategoryId(2L)
            .setCategoryIdForTests(1L, Offer.BindingKind.SUGGESTED)
            .setModelId(10L)
            .setSuggestSkuMapping(OfferTestUtils.mapping(100));
        Offer offer2 = OfferTestUtils.simpleOffer(2)
            .setMappedCategoryId(2L)
            .setCategoryIdForTests(1L, Offer.BindingKind.SUPPLIER)
            .setModelId(10L)
            .setSuggestSkuMapping(OfferTestUtils.mapping(100));
        Offer offer3 = OfferTestUtils.simpleOffer(3)
            .setMappedCategoryId(2L)
            .setCategoryIdForTests(1L, Offer.BindingKind.APPROVED)
            .setModelId(10L)
            .setSuggestSkuMapping(OfferTestUtils.mapping(100));

        Offer offer4 = OfferTestUtils.simpleOffer(4)
            .setMappedCategoryId(1L)
            .setCategoryIdForTests(2L, Offer.BindingKind.SUGGESTED)
            .setModelId(10L)
            .setSuggestSkuMapping(OfferTestUtils.mapping(100));
        Offer offer5 = OfferTestUtils.simpleOffer(5)
            .setMappedCategoryId(1L)
            .setCategoryIdForTests(2L, Offer.BindingKind.SUPPLIER)
            .setModelId(10L)
            .setSuggestSkuMapping(OfferTestUtils.mapping(100));
        Offer offer6 = OfferTestUtils.simpleOffer(6)
            .setMappedCategoryId(1L)
            .setCategoryIdForTests(2L, Offer.BindingKind.APPROVED)
            .setModelId(10L)
            .setSuggestSkuMapping(OfferTestUtils.mapping(100));

        Offer offer7 = OfferTestUtils.simpleOffer(7)
            .setMappedCategoryId(2L)
            .setCategoryIdForTests(2L, Offer.BindingKind.SUGGESTED)
            .setModelId(10L)
            .setSuggestSkuMapping(OfferTestUtils.mapping(100));
        Offer offer8 = OfferTestUtils.simpleOffer(8)
            .setMappedCategoryId(2L)
            .setCategoryIdForTests(2L, Offer.BindingKind.SUPPLIER)
            .setModelId(10L)
            .setSuggestSkuMapping(OfferTestUtils.mapping(100));
        Offer offer9 = OfferTestUtils.simpleOffer(9)
            .setMappedCategoryId(2L)
            .setCategoryIdForTests(2L, Offer.BindingKind.APPROVED)
            .setModelId(10L)
            .setSuggestSkuMapping(OfferTestUtils.mapping(100));

        List<Offer> offersEnrich = Stream.of(offer1, offer2, offer3, offer4, offer5, offer6, offer7, offer8, offer9)
            .map(Offer::copy)
            .collect(Collectors.toList());

        Mockito.when(ultraControllerServiceRemote.enrich(any())).then(__ ->
            UltraController.DataResponse.newBuilder()
                .addAllOffers(offersEnrich.stream()
                    .map(offer -> ucResponse(
                        UltraController.EnrichedOffer.EnrichType.APPROVED_CATEGORY, 2, null, null, null)
                    )
                    .map(UltraController.EnrichedOffer.Builder::build)
                    .collect(Collectors.toList())
                )
                .build());

        offersEnrichmentService.doUltraControllerEnrichment(offersEnrich, log::info);

        MbocAssertions.assertThat(offersEnrich.get(0)).isEqualTo(
            // updated with same mappedCategoryId and SUGGEST
            expectedOffer(1)
                .setMarketModelName(null)
                .setMappedCategoryId(2L)
                .setSuggestCategoryMappingId(2L)
                .setCategoryIdForTests(2L, Offer.BindingKind.SUGGESTED));
        // nothing changed, another category, suggest mapping removed
        MbocAssertions.assertThat(offersEnrich.get(1)).isEqualTo(
            offer2
                .setSuggestSkuMapping(null)
                .setSuggestCategoryMappingId(2L));
        MbocAssertions.assertThat(offersEnrich.get(2)).isEqualTo(
            offer3
                .setSuggestSkuMapping(null)
                .setSuggestCategoryMappingId(2L));
        // nothing changed, another mapped category
        MbocAssertions.assertThat(offersEnrich.get(3)).isEqualTo(offer4);
        MbocAssertions.assertThat(offersEnrich.get(4)).isEqualTo(offer5);
        MbocAssertions.assertThat(offersEnrich.get(5)).isEqualTo(offer6);
        // updated with same mappedCategoryId and SUGGEST
        MbocAssertions.assertThat(offersEnrich.get(6)).isEqualTo(expectedOffer(7)
            .setMarketModelName(null)
            .setMappedCategoryId(2L)
            .setSuggestCategoryMappingId(2L)
            .setCategoryIdForTests(2L, Offer.BindingKind.SUGGESTED));
        // updated with same categoryId and mappedCategoryId
        MbocAssertions.assertThat(offersEnrich.get(7)).isEqualTo(expectedOffer(8)
            .setMarketModelName(null)
            .setModelId(10L)
            .setMappedCategoryId(2L)
            .setSuggestCategoryMappingId(2L)
            .setCategoryIdForTests(2L, Offer.BindingKind.SUPPLIER));
        MbocAssertions.assertThat(offersEnrich.get(8)).isEqualTo(expectedOffer(9)
            .setMarketModelName(null)
            .setModelId(10L)
            .setMappedCategoryId(2L)
            .setSuggestCategoryMappingId(2L)
            .setCategoryIdForTests(2L, Offer.BindingKind.APPROVED));
    }

    @Test
    public void testCategoryBooksResponse() {
        Offer offer1 = OfferTestUtils.simpleOffer(1)
            .setMappedCategoryId(2L)
            .setCategoryIdForTests(1L, Offer.BindingKind.SUGGESTED)
            .setModelId(10L)
            .setBarCode("9753161484100")
            .setSuggestSkuMapping(OfferTestUtils.mapping(100));
        Offer offer2 = OfferTestUtils.simpleOffer(2)
            .setMappedCategoryId(2L)
            .setCategoryIdForTests(1L, Offer.BindingKind.SUGGESTED)
            .setModelId(10L)
            .setBarCode("9783161484100")
            .setSuggestSkuMapping(OfferTestUtils.mapping(100));

        categoryKnowledgeService.addCategory(2L);

        List<Offer> offersEnrich = Stream.of(offer1, offer2)
            .map(Offer::copy)
            .collect(Collectors.toList());

        Mockito.when(ultraControllerServiceRemote.enrich(any())).then(__ ->
            UltraController.DataResponse.newBuilder()
                .addAllOffers(offersEnrich.stream()
                    .map(offer ->
                        ucResponse(
                            UltraController.EnrichedOffer.EnrichType.MAIN, 2, null, null, null
                        ).setClassifierConfidentTopPrecision(DEFAULT_CLASSIFIER_TRUST_THRESHOLD)
                            .setClassifierCategoryId(2)
                    )
                    .map(UltraController.EnrichedOffer.Builder::build)
                    .collect(Collectors.toList())
                )
                .build());

        offersEnrichmentService.doUltraControllerEnrichment(offersEnrich, log::info);

        Assert.assertTrue(offersEnrich.get(0).isAutomaticClassification());
        Assert.assertEquals(Offer.BindingKind.APPROVED, offersEnrich.get(0).getBindingKind());
        Assert.assertFalse(offersEnrich.get(1).isAutomaticClassification());
        Assert.assertEquals(Offer.BindingKind.SUGGESTED, offersEnrich.get(1).getBindingKind());
    }


    @Test
    public void testGoldenMatrixService() {
        Offer offer1 = OfferTestUtils.simpleOffer()
            .setCategoryIdForTests(1L, Offer.BindingKind.SUGGESTED)
            .setModelId(1L)
            .setSuggestSkuMapping(OfferTestUtils.mapping(100));
        Offer offer2 = OfferTestUtils.simpleOffer()
            .setCategoryIdForTests(1L, Offer.BindingKind.APPROVED)
            .setModelId(2L)
            .setSuggestSkuMapping(OfferTestUtils.mapping(100))
            .setSuggestSkuMappingType(SkutchType.BARCODE_SKUTCH);
        Offer offer3 = OfferTestUtils.simpleOffer()
            .setCategoryIdForTests(1L, Offer.BindingKind.APPROVED)
            .setModelId(3L)
            .setSuggestSkuMapping(OfferTestUtils.mapping(100))
            .setSuggestSkuMappingType(SkutchType.SKUTCH_BY_PARAMETERS);

        Mockito.when(goldenMatrixService.isModelInGoldenMatrix(anyLong())).then(i -> {
            long modelId = i.getArgument(0);
            return modelId > 1;
        });

        offersEnrichmentService.checkInGoldenMatrix(Lists.newArrayList(offer1, offer2, offer3));

        Assertions.assertThat(offer1.getGolden()).isNull();
        Assertions.assertThat(offer2.getGolden()).isTrue();
        Assertions.assertThat(offer3.getGolden()).isTrue();
    }

    @Test
    public void testEnrichmentCallsUCandGoldenMatrix() {
        Mockito.when(ultraControllerServiceRemote.enrich(any())).then(__ ->
            UltraController.DataResponse.newBuilder()
                .addOffers(ucResponse(
                    UltraController.EnrichedOffer.EnrichType.MAIN, 2, 20, 200, SkutchType.SKUTCH_BY_PARAMETERS
                ))
                .build());

        List<Offer> offers = Collections.singletonList(
            OfferTestUtils.simpleOffer()
                .setId(42)
                .setCategoryIdForTests(42L, Offer.BindingKind.APPROVED)
                .setModelId(34L)
                .setSuggestSkuMapping(OfferTestUtils.mapping(0))
                .setSuggestSkuMappingType(SkutchType.SKUTCH_BY_PARAMETERS)
                .updateApprovedSkuMapping(OfferTestUtils.mapping(0), CONTENT)
                .markLoadedContent()
        );
        offersEnrichmentService.enrichOffers(offers, s -> {
        });

        Mockito.verify(ultraControllerServiceRemote, times(1))
            .enrich(Mockito.any());
        Mockito.verify(goldenMatrixService, times(1))
            .isModelInGoldenMatrix(Mockito.anyLong());
    }

    @Test
    public void testEnrichModelIdForSuggestIfNotSkuchedOffer() {
        Offer offer1 = OfferTestUtils.simpleOffer()
            .setCategoryIdForTests(1L, Offer.BindingKind.SUGGESTED)
            .setModelId(10L);

        Mockito.when(ultraControllerServiceRemote.enrich(any())).then(__ ->
            UltraController.DataResponse.newBuilder()
                .addOffers(ucResponse(UltraController.EnrichedOffer.EnrichType.MAIN, 2, 20, null, null))
                .build());

        List<Offer> offers = Collections.singletonList(new Offer(offer1));
        offersEnrichmentService.doUltraControllerEnrichment(offers, log::info);

        Offer expectedOffer1 = expectedOffer()
            .setCategoryIdForTests(2L, Offer.BindingKind.SUGGESTED)
            .setSuggestModelMappingId(20L)
            .setSuggestMarketModelName("test")
            .setSuggestCategoryMappingId(2L)
            .setModelId(20L);
        MbocAssertions.assertThat(offers.get(0)).isEqualTo(expectedOffer1);
    }

    @Test
    public void testSuggestedMappingFromUCHasMarketSkuType() {
        Offer offer1 = OfferTestUtils.simpleOffer()
            .setCategoryIdForTests(1L, Offer.BindingKind.SUGGESTED)
            .markLoadedContent();

        Mockito.when(ultraControllerServiceRemote.enrich(any())).then(__ ->
            UltraController.DataResponse.newBuilder()
                .addOffers(ucResponse(UltraController.EnrichedOffer.EnrichType.MAIN, 2, 20, 30,
                    SkutchType.SKUTCH_BY_PARAMETERS))
                .build());

        List<Offer> offers = Collections.singletonList(new Offer(offer1));
        offersEnrichmentService.doUltraControllerEnrichment(offers, log::info);

        Offer expectedOffer1 = expectedOffer()
            .setCategoryIdForTests(2L, Offer.BindingKind.SUGGESTED)
            .setModelId(20L)
            .setSuggestSkuMapping(OfferTestUtils.mapping(30))
            .setSuggestSkuMappingType(SkutchType.SKUTCH_BY_PARAMETERS)
            .setSuggestMappingSource(Offer.SuggestMappingSource.ULTRA_CONTROLLER)
            .setSuggestModelMappingId(20L)
            .setSuggestMarketModelName("test")
            .setSuggestCategoryMappingId(2L);
        MbocAssertions.assertThat(offers.get(0)).isEqualTo(expectedOffer1);
    }

    @Test
    public void testApprovedMappingKeepSkuType() {
        Offer offer1 = OfferTestUtils.simpleOffer()
            .setCategoryIdForTests(2L, Offer.BindingKind.APPROVED)
            .setModelId(20L)
            .setSuggestSkuMapping(OfferTestUtils.mapping(30))
            .setSuggestSkuMappingType(SkutchType.SKUTCH_BY_SKU_ID)
            .setVendorId(300)
            .setMarketVendorName("test")
            .updateApprovedSkuMapping(OfferTestUtils.mapping(30), CONTENT);

        Mockito.when(ultraControllerServiceRemote.enrich(any())).then(__ ->
            UltraController.DataResponse.newBuilder()
                .addOffers(ucResponse(UltraController.EnrichedOffer.EnrichType.APPROVED_SKU, 2, 20, 30,
                    SkutchType.SKUTCH_BY_PARAMETERS))
                .build());

        List<Offer> offers = Collections.singletonList(new Offer(offer1));
        offersEnrichmentService.doUltraControllerEnrichment(offers, log::info);

        Offer expectedOffer1 = expectedOffer()
            .setCategoryIdForTests(2L, Offer.BindingKind.APPROVED)
            .setModelId(20L)
            .setSuggestSkuMapping(OfferTestUtils.mapping(30))
            .setSuggestSkuMappingType(SkutchType.SKUTCH_BY_SKU_ID)
            .setSuggestModelMappingId(20L)
            .setSuggestMarketModelName("test")
            .setSuggestCategoryMappingId(2L)
            .updateApprovedSkuMapping(OfferTestUtils.mapping(30), CONTENT);
        MbocAssertions.assertThat(offers.get(0)).isEqualTo(expectedOffer1);
    }

    @Test
    public void testApprovedMappingKeepContentMappedCategoryWithBetterConfidence() {
        Offer offer = OfferTestUtils.simpleOffer()
            .setCategoryIdForTests(1L, Offer.BindingKind.APPROVED)
            .setModelId(20L)
            .setSuggestSkuMapping(OfferTestUtils.mapping(30))
            .setSuggestSkuMappingType(SkutchType.SKUTCH_BY_SKU_ID)
            .setVendorId(300)
            .setMarketVendorName("test")
            .updateApprovedSkuMapping(OfferTestUtils.mapping(30), PARTNER_SELF)
            .setMappedCategoryId(1L, CONTENT);

        Mockito.when(ultraControllerServiceRemote.enrich(any())).then(__ ->
            UltraController.DataResponse.newBuilder()
                .addOffers(ucResponse(UltraController.EnrichedOffer.EnrichType.APPROVED_SKU, 2, 22, 30,
                    SkutchType.SKUTCH_BY_PARAMETERS))
                .build());

        List<Offer> offers = Collections.singletonList(new Offer(offer));
        offersEnrichmentService.doUltraControllerEnrichment(offers, log::info);

        Offer expectedOffer = expectedOffer()
            .setCategoryIdForTests(1L, Offer.BindingKind.APPROVED)
            .setMappedCategoryId(1L, CONTENT)
            .setModelId(20L)
            .setMarketModelName(null)
            .setSuggestSkuMapping(OfferTestUtils.mapping(30))
            .setSuggestSkuMappingType(SkutchType.SKUTCH_BY_SKU_ID)
            .updateApprovedSkuMapping(OfferTestUtils.mapping(30), PARTNER_SELF);
        MbocAssertions.assertThat(offers.get(0)).isEqualTo(expectedOffer);
    }

    @Test
    public void testApprovedMappingUpdatedWithSameConfidence() {
        Offer offer = OfferTestUtils.simpleOffer()
            .setCategoryIdForTests(1L, Offer.BindingKind.APPROVED)
            .setModelId(20L)
            .setSuggestSkuMapping(OfferTestUtils.mapping(30))
            .setSuggestSkuMappingType(SkutchType.SKUTCH_BY_SKU_ID)
            .setVendorId(300)
            .setMarketVendorName("test")
            .updateApprovedSkuMapping(OfferTestUtils.mapping(30), CONTENT)
            .setMappedCategoryId(1L, CONTENT);

        Mockito.when(ultraControllerServiceRemote.enrich(any())).then(__ ->
            UltraController.DataResponse.newBuilder()
                .addOffers(ucResponse(UltraController.EnrichedOffer.EnrichType.APPROVED_SKU, 2, 22, 30,
                    SkutchType.SKUTCH_BY_PARAMETERS))
                .build());

        List<Offer> offers = Collections.singletonList(new Offer(offer));
        offersEnrichmentService.doUltraControllerEnrichment(offers, log::info);

        Offer expectedOffer = expectedOffer()
            .setCategoryIdForTests(2L, Offer.BindingKind.APPROVED)
            .setMappedCategoryId(1L, CONTENT)
            .setModelId(22L)
            .setSuggestSkuMapping(OfferTestUtils.mapping(30))
            .setSuggestSkuMappingType(SkutchType.SKUTCH_BY_SKU_ID)
            .setSuggestModelMappingId(22L)
            .setSuggestMarketModelName("test")
            .setSuggestCategoryMappingId(2L)
            .updateApprovedSkuMapping(OfferTestUtils.mapping(30), CONTENT);
        MbocAssertions.assertThat(offers.get(0)).isEqualTo(expectedOffer);
    }

    @Test
    public void testApprovedMappingUpdatedWithLowerConfidence() {
        Offer offer = OfferTestUtils.simpleOffer()
            .setCategoryIdForTests(1L, Offer.BindingKind.APPROVED)
            .setModelId(20L)
            .setSuggestSkuMapping(OfferTestUtils.mapping(30))
            .setSuggestSkuMappingType(SkutchType.SKUTCH_BY_SKU_ID)
            .setVendorId(300)
            .setMarketVendorName("test")
            .updateApprovedSkuMapping(OfferTestUtils.mapping(30), CONTENT)
            .setMappedCategoryId(1L, PARTNER);

        Mockito.when(ultraControllerServiceRemote.enrich(any())).then(__ ->
            UltraController.DataResponse.newBuilder()
                .addOffers(ucResponse(UltraController.EnrichedOffer.EnrichType.APPROVED_SKU, 2, 22, 30,
                    SkutchType.SKUTCH_BY_PARAMETERS))
                .build());

        List<Offer> offers = Collections.singletonList(new Offer(offer));
        offersEnrichmentService.doUltraControllerEnrichment(offers, log::info);

        Offer expectedOffer = expectedOffer()
            .setCategoryIdForTests(2L, Offer.BindingKind.APPROVED)
            .setMappedCategoryId(1L, PARTNER)
            .setModelId(22L)
            .setSuggestSkuMapping(OfferTestUtils.mapping(30))
            .setSuggestSkuMappingType(SkutchType.SKUTCH_BY_SKU_ID)
            .setSuggestCategoryMappingId(2L)
            .setSuggestModelMappingId(22L)
            .setSuggestMarketModelName("test")
            .updateApprovedSkuMapping(OfferTestUtils.mapping(30), CONTENT);
        MbocAssertions.assertThat(offers.get(0)).isEqualTo(expectedOffer);
    }

    @Test
    public void whenOfferWasSetManuallyShouldNotUpdateVendorFields() {
        Offer offer1 = Mockito.spy(OfferTestUtils.simpleOffer()
            .setCategoryIdForTests(1L, Offer.BindingKind.SUGGESTED)
            .setVendorId(2)
            .setModelId(10L)
            .setSuggestSkuMapping(OfferTestUtils.mapping(100))
            .setManualVendor(true)
            .setSuggestSkuMappingType(SkutchType.SKUTCH_BY_PARAMETERS));

        Offer offer2 = Mockito.spy(OfferTestUtils.simpleOffer()
            .setCategoryIdForTests(1L, Offer.BindingKind.APPROVED)
            .setVendorId(3)
            .setManualVendor(true)
            .setModelId(10L)
            .setSuggestSkuMapping(OfferTestUtils.mapping(100)));

        Offer offer3 = Mockito.spy(OfferTestUtils.simpleOffer()
            .setVendorId(4)
            .setCategoryIdForTests(1L, Offer.BindingKind.APPROVED)
            .setModelId(10L)
            .setSuggestSkuMapping(OfferTestUtils.mapping(100))
            .updateApprovedSkuMapping(OfferTestUtils.mapping(200), CONTENT));

        Mockito.when(ultraControllerServiceRemote.enrich(any())).then(__ ->
            UltraController.DataResponse.newBuilder()
                .addOffers(ucResponse(
                    UltraController.EnrichedOffer.EnrichType.APPROVED_SKU, 2, 20, 200, SkutchType.SKUTCH_BY_PARAMETERS
                ))
                .addOffers(ucResponse(
                    UltraController.EnrichedOffer.EnrichType.APPROVED_SKU, 2, 20, 200, SkutchType.SKUTCH_BY_PARAMETERS
                ))
                .addOffers(ucResponse(
                    UltraController.EnrichedOffer.EnrichType.APPROVED_SKU, 2, 20, 200, SkutchType.SKUTCH_BY_PARAMETERS
                ))
                .build());

        List<Offer> offersEnrich = Arrays.asList(offer1, offer2, offer3);
        offersEnrichmentService.doUltraControllerEnrichment(offersEnrich, log::info);

        Assertions.assertThat(offer1.getVendorId()).isEqualTo(2);
        Assertions.assertThat(offer1.getMarketVendorName()).isNull();
        Assertions.assertThat(offer2.getVendorId()).isEqualTo(3);
        Assertions.assertThat(offer2.getMarketVendorName()).isNull();
        Assertions.assertThat(offer3.getVendorId()).isEqualTo(300);
        Assertions.assertThat(offer3.getMarketVendorName()).isEqualTo("test");

    }

    @Test
    public void whenOfferWasSetManuallyShouldNotUpdateVendorFieldsAndMappings() {
        Offer isNotManualOffer = OfferTestUtils.simpleOffer()
            .setCategoryIdForTests(null, Offer.BindingKind.APPROVED)
            .setVendorId(300)
            .setManualVendor(false)
            .setMarketVendorName("not manual vendor name")
            .setModelId(10L)
            .setSuggestSkuMapping(OfferTestUtils.mapping(100));

        Offer manualWithSameVendorIfOffer = OfferTestUtils.simpleOffer()
            .setCategoryIdForTests(1L, Offer.BindingKind.APPROVED)
            .setManualVendor(true)
            .setVendorId(300)
            .setMarketVendorName("vendor name1")
            .setModelId(10L)
            .setSuggestSkuMapping(OfferTestUtils.mapping(100))
            .updateApprovedSkuMapping(OfferTestUtils.mapping(200), CONTENT);
        Offer manualOfferWithDifferentVendorId = OfferTestUtils.simpleOffer()
            .setCategoryIdForTests(1L, Offer.BindingKind.SUGGESTED)
            .setManualVendor(true)
            .setVendorId(4)
            .setModelId(10L)
            .setSuggestSkuMapping(OfferTestUtils.mapping(100))
            .updateApprovedSkuMapping(OfferTestUtils.mapping(200), CONTENT);

        Mockito.when(ultraControllerServiceRemote.enrich(any())).then(__ ->
            UltraController.DataResponse.newBuilder()
                .addOffers(ucResponse(
                    UltraController.EnrichedOffer.EnrichType.MAIN, 2, 20, 200, SkutchType.SKUTCH_BY_PARAMETERS,
                    true, true
                ))
                .addOffers(ucResponse(
                    UltraController.EnrichedOffer.EnrichType.MAIN, 1L, 20, 200, SkutchType.SKUTCH_BY_PARAMETERS,
                    true, true
                ))
                .addOffers(ucResponse(
                    UltraController.EnrichedOffer.EnrichType.MAIN, 1L, 20, 200, SkutchType.SKUTCH_BY_PARAMETERS,
                    true, true
                ))
                .build());

        List<Offer> offersEnrich = Arrays.asList(isNotManualOffer, manualWithSameVendorIfOffer,
            manualOfferWithDifferentVendorId);
        offersEnrichmentService.doUltraControllerEnrichment(offersEnrich, log::info);

        Assertions.assertThat(isNotManualOffer.getVendorId()).isEqualTo(300);
        Assertions.assertThat(isNotManualOffer.getCategoryId()).isEqualTo(2);
        Assertions.assertThat(isNotManualOffer.getMarketVendorName()).isEqualTo("test");
        Assertions.assertThat(isNotManualOffer.getModelId()).isEqualTo(20);

        Assertions.assertThat(manualWithSameVendorIfOffer.getVendorId()).isEqualTo(300);
        Assertions.assertThat(manualWithSameVendorIfOffer.getMarketVendorName()).isEqualTo("vendor name1");
        Assertions.assertThat(manualWithSameVendorIfOffer.getModelId()).isEqualTo(20L);
        Assertions.assertThat(manualWithSameVendorIfOffer.getSuggestSkuMapping().getMappingId())
            .isEqualTo(200);

        Assertions.assertThat(manualOfferWithDifferentVendorId.getSuggestSkuMappingType()).isNull();
        Assertions.assertThat(manualOfferWithDifferentVendorId.getSuggestSkuMapping()).isNull();

    }

    @Test
    public void whenUCMatchesSKUForOfferWithFastSkuMappingThenSuggestSkuMappingIsUpdated() {
        Offer offerFastSku = OfferTestUtils.simpleOffer()
            .updateApprovedSkuMapping(OfferTestUtils.mapping(100, Offer.SkuType.FAST_SKU), PARTNER_FAST)
            .markLoadedContent()
            .setCategoryIdForTests(1L, Offer.BindingKind.SUGGESTED)
            .setModelId(null)
            .setSuggestSkuMapping(null);

        Mockito.when(ultraControllerServiceRemote.enrich(any())).then(__ ->
            UltraController.DataResponse.newBuilder()
                .addOffers(ucResponse(
                    UltraController.EnrichedOffer.EnrichType.MAIN, 2, 20, 200, SkutchType.SKUTCH_BY_PARAMETERS
                ))
                .build());

        offersEnrichmentService.doUltraControllerEnrichment(List.of(offerFastSku), log::info);

        Offer expectedOfferSkutched = expectedOffer()
            .updateApprovedSkuMapping(OfferTestUtils.mapping(100, Offer.SkuType.FAST_SKU), PARTNER_FAST)
            .markLoadedContent()
            .setCategoryIdForTests(2L, Offer.BindingKind.SUGGESTED)
            .setModelId(20L)
            .setSuggestSkuMapping(OfferTestUtils.mapping(200))
            .setSuggestMappingSource(Offer.SuggestMappingSource.ULTRA_CONTROLLER)
            .setSuggestSkuMappingType(SkutchType.SKUTCH_BY_PARAMETERS)
            .setSuggestModelMappingId(20L)
            .setSuggestMarketModelName("test")
            .setSuggestCategoryMappingId(2L);

        MbocAssertions.assertThat(offerFastSku).isEqualTo(expectedOfferSkutched);
    }

    private Offer expectedOffer() {
        return OfferTestUtils.simpleOffer()
            .setMarketModelName("test")
            .setMarketVendorName("test")
            .setVendorId(300);
    }

    private Offer expectedOffer(long id) {
        return OfferTestUtils.simpleOffer(id)
            .setMarketModelName("test")
            .setMarketVendorName("test")
            .setVendorId(300);
    }

    private UltraController.EnrichedOffer.Builder defaultUcResponse() {
        return ucResponse(
            UltraController.EnrichedOffer.EnrichType.MAIN,
            BOOTS_RELATED_HID, 20, 200, SkutchType.SKUTCH_BY_PARAMETERS);
    }

    private UltraController.EnrichedOffer.Builder ucResponse(
        UltraController.EnrichedOffer.EnrichType enrichType, long categoryId, Integer matchedId, Integer skuMapping,
        SkutchType skutchType) {
        return ucResponse(enrichType, categoryId, matchedId, skuMapping, skutchType, true, true);
    }

    private UltraController.EnrichedOffer.Builder ucResponse(
        UltraController.EnrichedOffer.EnrichType enrichType, long categoryId, Integer matchedId, Integer skuMapping,
        SkutchType skutchType,
        boolean publishedOnBlue, boolean publishedOnWhite) {
        UltraController.EnrichedOffer.Builder builder = UltraController.EnrichedOffer.newBuilder();
        builder.setVendorId(300);
        builder.setMarketVendorName("vendor");
        builder.setMarketCategoryName("test");
        builder.setMarketModelName("test");
        builder.setMarketVendorName("test");
        builder.setEnrichType(enrichType);
        builder.setCategoryId((int) categoryId);
        if (matchedId != null) {
            builder.setMatchedId(matchedId);
        }
        if (skuMapping != null) {
            Preconditions.checkNotNull(matchedId, "MatchedId should be set if you want to set skuMapping");
            builder.setMarketSkuId(skuMapping);
            builder.setMarketSkuName(OfferTestUtils.defaultMappingName(skuMapping));
            if (skuMapping != -1) {
                Preconditions.checkNotNull(skutchType, "SkutchType should be set if you want to set skuMapping");
                builder.setSkutchType(skutchType);
            }
        }
        builder.setMarketSkuPublishedOnBlueMarket(publishedOnBlue);
        builder.setMarketSkuPublishedOnMarket(publishedOnWhite);
        return builder;
    }
}

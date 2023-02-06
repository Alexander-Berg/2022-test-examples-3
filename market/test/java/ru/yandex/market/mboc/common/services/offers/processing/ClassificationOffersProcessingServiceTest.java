package ru.yandex.market.mboc.common.services.offers.processing;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.assertj.core.api.SoftAssertions;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.mboc.common.dict.Supplier;
import ru.yandex.market.mboc.common.dict.SupplierRepository;
import ru.yandex.market.mboc.common.dict.SupplierService;
import ru.yandex.market.mboc.common.honestmark.OfferCategoryRestrictionCalculator;
import ru.yandex.market.mboc.common.offers.ClassifierOffer;
import ru.yandex.market.mboc.common.offers.model.ContentComment;
import ru.yandex.market.mboc.common.offers.model.ContentCommentType;
import ru.yandex.market.mboc.common.offers.model.Offer;
import ru.yandex.market.mboc.common.services.books.BooksService;
import ru.yandex.market.mboc.common.services.category.CategoryCachingServiceMock;
import ru.yandex.market.mboc.common.services.category.models.Category;
import ru.yandex.market.mboc.common.services.category_info.info.CategoryInfo;
import ru.yandex.market.mboc.common.services.offers.mapping.LegacyOfferMappingActionService;
import ru.yandex.market.mboc.common.services.offers.mapping.OfferMappingActionService;
import ru.yandex.market.mboc.common.services.offers.mapping.RetrieveMappingSkuTypeService;
import ru.yandex.market.mboc.common.utils.BaseDbTestClass;
import ru.yandex.market.mboc.common.utils.DateTimeUtils;
import ru.yandex.market.mboc.common.utils.OfferTestUtils;
import ru.yandex.market.mboc.http.SupplierOffer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anySet;

/**
 * @author danfertev
 * @since 27.03.2019
 */
public class ClassificationOffersProcessingServiceTest extends BaseDbTestClass {
    private static final String TEXT_COMMENT = "TEXT_COMMENT";
    private static final long CATEGORY_ID = 1;
    private static final long FIXED_CATEGORY_ID = 2;
    private static final long GOOD_CATEGORY_ID = 3;
    private static final long FIXED_GOOD_CATEGORY_ID = 4;
    private static final long NOT_EXISTING_CATEGORY_ID = 5;
    private static final long MODEL_ID = 10L;
    private static final long MSKU_ID = 10000001L;

    @Autowired
    private SupplierRepository supplierRepository;
    private SupplierService supplierService;

    private ClassificationOffersProcessingService classificationOffersProcessingService;
    private CategoryCachingServiceMock categoryCachingServiceMock;
    private OfferMappingActionService offerMappingActionService;

    private Supplier supplier;

    @Before
    public void setUp() {
        supplierService = new SupplierService(supplierRepository);
        categoryCachingServiceMock = new CategoryCachingServiceMock();
        var needContentStatusService = new NeedContentStatusService(categoryCachingServiceMock, supplierService,
            new BooksService(categoryCachingServiceMock, Collections.emptySet()));
        var legacyOfferMappingActionService = new LegacyOfferMappingActionService(
            needContentStatusService,
            Mockito.mock(OfferCategoryRestrictionCalculator.class),
            offerDestinationCalculator,
            storageKeyValueService
        );
        var offerMappingActionServiceV2Instance = new OfferMappingActionService(legacyOfferMappingActionService);
        offerMappingActionService = Mockito.spy(offerMappingActionServiceV2Instance);

        var retrieveMappingSkuTypeService = Mockito.mock(RetrieveMappingSkuTypeService.class);
        Mockito.when(retrieveMappingSkuTypeService.retrieveMappingSkuType(anyCollection(), anySet(), any()))
            .thenAnswer(invocation -> invocation.getArgument(0));

        classificationOffersProcessingService = new ClassificationOffersProcessingService(
            categoryCachingServiceMock, offerMappingActionService, offerDestinationCalculator);
        supplier = OfferTestUtils.simpleSupplier();
        supplierRepository.insertBatch(supplier);

        categoryCachingServiceMock.addCategories(
            new Category().setCategoryId(CATEGORY_ID).setHasKnowledge(true),
            new Category().setCategoryId(FIXED_CATEGORY_ID).setHasKnowledge(true),
            new Category().setCategoryId(GOOD_CATEGORY_ID).setHasKnowledge(true).setAcceptGoodContent(true),
            new Category().setCategoryId(FIXED_GOOD_CATEGORY_ID).setHasKnowledge(true).setAcceptGoodContent(true)
        );
        categoryInfoRepository.insertOrUpdateAll(List.of(
            new CategoryInfo(CATEGORY_ID).setModerationInYang(true),
            new CategoryInfo(FIXED_CATEGORY_ID).setModerationInYang(true),
            new CategoryInfo(GOOD_CATEGORY_ID).setModerationInYang(true),
            new CategoryInfo(FIXED_GOOD_CATEGORY_ID).setModerationInYang(true)
        ));
    }

    @Test
    public void testUpdateCommentsFromClassifierOffer() {
        Offer offer = OfferTestUtils.simpleOffer(supplier)
            .updateAcceptanceStatusForTests(Offer.AcceptanceStatus.OK);
        ClassifierOffer classifierOffer = new ClassifierOffer();
        classifierOffer.setId(offer.getId());
        classifierOffer.setContentComment(TEXT_COMMENT);
        classifierOffer.setContentCommentType1(ContentCommentType.WRONG_CATEGORY);
        classifierOffer.setContentCommentItems1("items1");
        classifierOffer.setContentCommentType2(ContentCommentType.NEED_INFORMATION);
        classifierOffer.setContentCommentItems2("items2");

        classificationOffersProcessingService.updateOfferContentComments(offer, classifierOffer);

        assertThat(offer.getContentComment()).isEqualTo(TEXT_COMMENT);
        assertThat(offer.getContentComments().stream().map(ContentComment::getType).collect(Collectors.toList()))
            .containsExactlyInAnyOrder(ContentCommentType.WRONG_CATEGORY, ContentCommentType.NEED_INFORMATION);
    }

    @Test
    public void testUpdateCommentsFromClassifierTaskResult() {
        Offer offer = OfferTestUtils.simpleOffer(supplier)
            .updateAcceptanceStatusForTests(Offer.AcceptanceStatus.OK)
            .setContentComment("SOMETHING");
        SupplierOffer.ClassificationTaskResult taskResult = SupplierOffer.ClassificationTaskResult.newBuilder()
            .addContentComment(SupplierOffer.ContentComment.newBuilder()
                .setType(ContentCommentType.WRONG_CATEGORY.toString())
                .addItems("item1")
                .build())
            .addContentComment(SupplierOffer.ContentComment.newBuilder()
                .setType(ContentCommentType.NEED_INFORMATION.toString())
                .addItems("item2")
                .build())
            .build();
        classificationOffersProcessingService.updateOfferContentComments(offer, taskResult);

        assertThat(offer.getContentComment()).isNullOrEmpty();
        assertThat(offer.getContentComments().stream().map(ContentComment::getType).collect(Collectors.toList()))
            .containsExactlyInAnyOrder(ContentCommentType.WRONG_CATEGORY, ContentCommentType.NEED_INFORMATION);
    }

    @Test
    public void testProcessOfferSetNeedInfoNoFixedCategory() {
        Offer offer = OfferTestUtils.simpleOffer(supplier)
            .updateAcceptanceStatusForTests(Offer.AcceptanceStatus.OK)
            .setContentComments(new ContentComment(ContentCommentType.NO_KNOWLEDGE));

        classificationOffersProcessingService.processOffer(supplier, offer, null);

        assertThat(offer.getProcessingStatus()).isEqualTo(Offer.ProcessingStatus.NEED_INFO);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testProcessOfferSetErrorIfNoCommentAndNoFixedCategory() {
        Offer offer = OfferTestUtils.simpleOffer()
            .updateAcceptanceStatusForTests(Offer.AcceptanceStatus.OK);

        classificationOffersProcessingService.processOffer(supplier, offer, null);
    }

    @Test
    public void testProcessOfferCategoryNotChanged() {
        Offer offer = OfferTestUtils.simpleOffer(supplier)
            .updateAcceptanceStatusForTests(Offer.AcceptanceStatus.OK)
            .setCategoryIdForTests(CATEGORY_ID, Offer.BindingKind.SUGGESTED);

        classificationOffersProcessingService.processOffer(supplier, offer, CATEGORY_ID);

        assertThat(offer.getProcessingStatus()).isEqualTo(Offer.ProcessingStatus.CLASSIFIED);
        assertThat(offer.getBindingKind()).isEqualTo(Offer.BindingKind.APPROVED);
        assertThat(offer.getCategoryId()).isEqualTo(CATEGORY_ID);
    }

    @Test
    public void testProcessOfferCategoryChanged() {
        Offer offer = OfferTestUtils.simpleOffer(supplier)
            .updateAcceptanceStatusForTests(Offer.AcceptanceStatus.OK)
            .setCategoryIdForTests(CATEGORY_ID, Offer.BindingKind.SUGGESTED)
            .setModelId(MODEL_ID)
            .setAutomaticClassification(true);

        classificationOffersProcessingService.processOffer(supplier, offer, FIXED_CATEGORY_ID);

        assertThat(offer.getProcessingStatus()).isEqualTo(Offer.ProcessingStatus.CLASSIFIED);
        assertThat(offer.getBindingKind()).isEqualTo(Offer.BindingKind.APPROVED);
        assertThat(offer.getCategoryId()).isEqualTo(FIXED_CATEGORY_ID);
        assertThat(offer.isAutomaticClassification()).isEqualTo(false);
        assertThat(offer.getModelId()).isNull();
    }

    @Test
    public void testProcessOfferNoKnowledge() {
        Offer offer = OfferTestUtils.simpleOffer(supplier)
            .updateAcceptanceStatusForTests(Offer.AcceptanceStatus.OK)
            .setCategoryIdForTests(CATEGORY_ID, Offer.BindingKind.SUGGESTED)
            .setModelId(MODEL_ID);

        categoryCachingServiceMock.setCategoryHasKnowledge(FIXED_CATEGORY_ID, false);

        classificationOffersProcessingService.processOffer(supplier, offer, FIXED_CATEGORY_ID);

        assertThat(offer.getProcessingStatus()).isEqualTo(Offer.ProcessingStatus.CLASSIFIED);
        assertThat(offer.getBindingKind()).isEqualTo(Offer.BindingKind.APPROVED);
        assertThat(offer.getCategoryId()).isEqualTo(FIXED_CATEGORY_ID);
        assertThat(offer.getModelId()).isNull();
    }

    @Test
    public void testReturnOfferFromNeedContentToContentProcessing() {
        Offer offer = OfferTestUtils.simpleOffer()
            .updateAcceptanceStatusForTests(Offer.AcceptanceStatus.OK)
            .setMappedCategoryId(GOOD_CATEGORY_ID)
            .setModelId(MODEL_ID)
            .setReprocessRequested(true);
        Supplier newPipelineSupplier = OfferTestUtils.simpleSupplier().setNewContentPipeline(true);
        classificationOffersProcessingService.processOffer(newPipelineSupplier, offer, GOOD_CATEGORY_ID);

        assertThat(offer.getProcessingStatus()).isEqualTo(Offer.ProcessingStatus.CLASSIFIED);
        assertThat(offer.isReprocessRequested()).isEqualTo(false);
    }

    @Test
    public void testDontReturnOfferFromNeedContentToContentProcessing() {
        Offer offer = OfferTestUtils.simpleOffer()
            .updateAcceptanceStatusForTests(Offer.AcceptanceStatus.OK)
            .setMappedCategoryId(GOOD_CATEGORY_ID)
            .setModelId(MODEL_ID)
            .setReprocessRequested(false);
        Supplier newPipelineSupplier = OfferTestUtils.simpleSupplier().setNewContentPipeline(true);
        classificationOffersProcessingService.processOffer(newPipelineSupplier, offer, GOOD_CATEGORY_ID);

        assertThat(offer.getProcessingStatus()).isEqualTo(Offer.ProcessingStatus.CLASSIFIED);
        assertThat(offer.isReprocessRequested()).isEqualTo(false);
    }

    @Test
    public void testReturnOfferFromNeedContentToContentProcessingCommentsCase() {
        Offer offer = OfferTestUtils.simpleOffer()
            .updateAcceptanceStatusForTests(Offer.AcceptanceStatus.OK)
            .setCategoryIdForTests(GOOD_CATEGORY_ID, Offer.BindingKind.APPROVED)
            .setModelId(MODEL_ID)
            .setReprocessRequested(true)
            .setContentComments(new ContentComment(ContentCommentType.CANCELLED));
        Supplier newPipelineSupplier = OfferTestUtils.simpleSupplier().setNewContentPipeline(true);

        classificationOffersProcessingService.processOffer(newPipelineSupplier, offer, null);

        assertThat(offer.getProcessingStatus()).isEqualTo(Offer.ProcessingStatus.NEED_INFO);
        assertThat(offer.isReprocessRequested()).isEqualTo(false);
    }

    @Test
    public void testShouldKeepProcessedStatuses() {
        Set<Offer.ProcessingStatus> processedStatuses = Set.of(
            Offer.ProcessingStatus.PROCESSED,
            Offer.ProcessingStatus.AUTO_PROCESSED
        );

        Set<Offer.ProcessingStatus> otherStatuses = Arrays.stream(Offer.ProcessingStatus.values())
            .filter(Predicate.not(processedStatuses::contains))
            .filter(Predicate.not(Offer.ProcessingStatus.INVALID::equals))
            .collect(Collectors.toSet());

        List<Offer> processedOffers = processedStatuses.stream()
            .map(status -> OfferTestUtils.simpleOffer()
                .updateAcceptanceStatusForTests(Offer.AcceptanceStatus.OK)
                .updateProcessingStatusIfValid(status))
            .collect(Collectors.toList());

        List<Offer> otherOffers = otherStatuses.stream()
            .map(status -> OfferTestUtils.simpleOffer()
                .updateAcceptanceStatusForTests(Offer.AcceptanceStatus.OK)
                .updateProcessingStatusIfValid(status))
            .collect(Collectors.toList());

        Offer invalidOffer = OfferTestUtils.simpleOffer()
            .updateAcceptanceStatusForTests(Offer.AcceptanceStatus.OK)
            .updateProcessingStatusIfValid(Offer.ProcessingStatus.INVALID);

        Stream.of(
            processedOffers.stream(),
            Stream.of(invalidOffer),
            otherOffers.stream()
        )
            .flatMap(Function.identity())
            .forEach(offer ->
                classificationOffersProcessingService.processOffer(supplier, offer, FIXED_CATEGORY_ID)
            );

        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(processedOffers)
                .as("processed offers get classification results and keep original statuses")
                .usingElementComparatorOnFields(
                    "processingStatus",
                    "categoryId", "bindingKind",
                    "mappedCategoryId", "mappedCategoryConfidence")
                .containsExactlyInAnyOrderElementsOf(
                    processedStatuses.stream()
                        .map(status -> OfferTestUtils.simpleOffer()
                            .updateProcessingStatusIfValid(status)
                            .setCategoryIdForTests(FIXED_CATEGORY_ID, Offer.BindingKind.APPROVED)
                            .setMappedCategoryId(FIXED_CATEGORY_ID, Offer.MappingConfidence.CONTENT))
                        .collect(Collectors.toList())
                );

            softly.assertThat(List.of(invalidOffer))
                .as("invalid offers get classification results and keep original statuses")
                .usingElementComparatorOnFields(
                    "processingStatus",
                    "categoryId", "bindingKind",
                    "mappedCategoryId", "mappedCategoryConfidence")
                .containsExactlyInAnyOrderElementsOf(List.of(
                    OfferTestUtils.simpleOffer()
                        .updateProcessingStatusIfValid(Offer.ProcessingStatus.INVALID)
                        .setCategoryIdForTests(FIXED_CATEGORY_ID, Offer.BindingKind.APPROVED)
                        .setMappedCategoryId(FIXED_CATEGORY_ID, Offer.MappingConfidence.CONTENT))
                );

            softly.assertThat(otherOffers)
                .as("other offers get classification results and IN_PROCESS status")
                .usingElementComparatorOnFields(
                    "processingStatus",
                    "categoryId", "bindingKind",
                    "mappedCategoryId", "mappedCategoryConfidence")
                .containsExactlyInAnyOrderElementsOf(
                    otherOffers.stream()
                        .map(status -> OfferTestUtils.simpleOffer()
                            .updateProcessingStatusIfValid(Offer.ProcessingStatus.CLASSIFIED)
                            .setCategoryIdForTests(FIXED_CATEGORY_ID, Offer.BindingKind.APPROVED)
                            .setMappedCategoryId(FIXED_CATEGORY_ID, Offer.MappingConfidence.CONTENT))
                        .collect(Collectors.toList())
                );
        });
    }

    @Test
    public void testProcessAcceptedCategoryMapping() {
        Offer offer = OfferTestUtils.simpleOffer()
            .updateAcceptanceStatusForTests(Offer.AcceptanceStatus.OK)
            .setShopSku("offer")
            .setSupplierCategoryId(GOOD_CATEGORY_ID)
            .setSupplierCategoryMappingStatus(Offer.MappingStatus.NEW);
        Supplier newPipelineSupplier = OfferTestUtils.simpleSupplier().setNewContentPipeline(true);

        classificationOffersProcessingService.processOffer(newPipelineSupplier, offer, GOOD_CATEGORY_ID);

        assertThat(offer.getProcessingStatus()).isEqualTo(Offer.ProcessingStatus.CLASSIFIED);
        assertThat(offer.getSupplierCategoryMappingStatus()).isEqualTo(Offer.MappingStatus.ACCEPTED);
    }

    @Test
    public void testProcessRejectedCategoryMapping() {
        Offer offer = OfferTestUtils.simpleOffer().setShopSku("offer")
            .updateAcceptanceStatusForTests(Offer.AcceptanceStatus.OK)
            .setSupplierCategoryId(GOOD_CATEGORY_ID)
            .setSupplierCategoryMappingStatus(Offer.MappingStatus.NEW);
        Supplier newPipelineSupplier = OfferTestUtils.simpleSupplier().setNewContentPipeline(true);

        classificationOffersProcessingService.processOffer(newPipelineSupplier, offer, FIXED_GOOD_CATEGORY_ID);

        assertThat(offer.getProcessingStatus()).isEqualTo(Offer.ProcessingStatus.CLASSIFIED);
        assertThat(offer.getSupplierCategoryMappingStatus()).isEqualTo(Offer.MappingStatus.REJECTED);
    }

    @Test
    public void testProcessNoCategoryMapping() {
        Offer offer = OfferTestUtils.simpleOffer()
            .updateAcceptanceStatusForTests(Offer.AcceptanceStatus.OK)
            .setShopSku("offer")
            .setSupplierCategoryId(NOT_EXISTING_CATEGORY_ID)
            .setSupplierCategoryMappingStatus(Offer.MappingStatus.ACCEPTED);
        Supplier newPipelineSupplier = OfferTestUtils.simpleSupplier().setNewContentPipeline(true);

        classificationOffersProcessingService.processOffer(newPipelineSupplier, offer, GOOD_CATEGORY_ID);

        assertThat(offer.getProcessingStatus()).isEqualTo(Offer.ProcessingStatus.CLASSIFIED);
        // SupplierCategoryId (NOT_EXISTING_CATEGORY_ID) != CONTENT id (GOOD_CATEGORY_ID)
        assertThat(offer.getSupplierCategoryMappingStatus()).isEqualTo(Offer.MappingStatus.REJECTED);
    }

    @Test
    public void testOfferGotMappingInClassification() {
        Supplier newPipelineSupplier = OfferTestUtils.simpleSupplier().setNewContentPipeline(true);

        // case 1: classification result category is the same as suggested in offer
        Offer offer = OfferTestUtils.simpleOffer(newPipelineSupplier)
            .updateAcceptanceStatusForTests(Offer.AcceptanceStatus.OK)
            .setCategoryIdForTests(GOOD_CATEGORY_ID, Offer.BindingKind.SUGGESTED)
            .setModelId(MODEL_ID)
            .setSuggestSkuMapping(new Offer.Mapping(MSKU_ID, DateTimeUtils.dateTimeNow(), Offer.SkuType.MARKET));

        classificationOffersProcessingService.processOffer(newPipelineSupplier, offer, GOOD_CATEGORY_ID);

        assertThat(offer.getProcessingStatus()).isEqualTo(Offer.ProcessingStatus.CLASSIFIED);
        assertThat(offer.getBindingKind()).isEqualTo(Offer.BindingKind.APPROVED);
        assertThat(offer.getCategoryId()).isEqualTo(GOOD_CATEGORY_ID);

        // case 2: classification result category differs, suggested model and sku mapping should be cleared
        Offer offer2 = OfferTestUtils.simpleOffer(newPipelineSupplier)
            .updateAcceptanceStatusForTests(Offer.AcceptanceStatus.OK)
            .setCategoryIdForTests(CATEGORY_ID, Offer.BindingKind.SUGGESTED)
            .setModelId(MODEL_ID)
            .setSuggestSkuMapping(new Offer.Mapping(MSKU_ID, DateTimeUtils.dateTimeNow()));

        classificationOffersProcessingService.processOffer(newPipelineSupplier, offer2, GOOD_CATEGORY_ID);

        assertThat(offer2.getProcessingStatus()).isEqualTo(Offer.ProcessingStatus.CLASSIFIED);
        assertThat(offer2.getBindingKind()).isEqualTo(Offer.BindingKind.APPROVED);
        assertThat(offer2.getCategoryId()).isEqualTo(GOOD_CATEGORY_ID);

        // case 3: classification result category is the same, offer send to classification by gutgin
        Offer offer3 = OfferTestUtils.simpleOffer(newPipelineSupplier)
            .updateAcceptanceStatusForTests(Offer.AcceptanceStatus.OK)
            .setCategoryIdForTests(GOOD_CATEGORY_ID, Offer.BindingKind.SUGGESTED)
            .setModelId(MODEL_ID)
            .setSuggestSkuMapping(new Offer.Mapping(MSKU_ID, DateTimeUtils.dateTimeNow()))
            .setReprocessRequested(true);

        classificationOffersProcessingService.processOffer(newPipelineSupplier, offer3, GOOD_CATEGORY_ID);

        assertThat(offer3.getProcessingStatus()).isEqualTo(Offer.ProcessingStatus.CLASSIFIED);
        assertThat(offer3.getBindingKind()).isEqualTo(Offer.BindingKind.APPROVED);
        assertThat(offer3.getCategoryId()).isEqualTo(GOOD_CATEGORY_ID);
    }

    @Test
    public void testProcessOfferCategoryNotChangedOnRecheckClassification() {
        Offer offer = OfferTestUtils.simpleOffer(supplier)
            .updateAcceptanceStatusForTests(Offer.AcceptanceStatus.OK)
            .setCategoryIdForTests(CATEGORY_ID, Offer.BindingKind.APPROVED)
            .setRecheckCategoryId(CATEGORY_ID)
            .setProcessingStatusInternal(Offer.ProcessingStatus.IN_RECHECK_CLASSIFICATION)
            .setRecheckClassificationSource(Offer.RecheckClassificationSource.PARTNER)
            .setRecheckClassificationStatus(Offer.RecheckClassificationStatus.ON_RECHECK)
            .setSupplierCategoryId(FIXED_CATEGORY_ID)
            .setSupplierCategoryMappingStatus(Offer.MappingStatus.NEW);

        classificationOffersProcessingService.processOffer(supplier, offer, CATEGORY_ID);

        assertThat(offer.getProcessingStatus())
            .isEqualTo(Offer.ProcessingStatus.CLASSIFIED);
        assertThat(offer.getBindingKind())
            .isEqualTo(Offer.BindingKind.APPROVED);
        assertThat(offer.getCategoryId())
            .isEqualTo(CATEGORY_ID);
        assertThat(offer.getRecheckClassificationStatus())
            .isEqualTo(Offer.RecheckClassificationStatus.CONFIRMED);
        assertThat(offer.getSupplierCategoryMappingStatus())
            .isEqualTo(Offer.MappingStatus.REJECTED);
    }

    @Test
    public void testProcessOfferCategoryChangedOnRecheckClassification() {
        var offerSupplierCategoryConfirmed = OfferTestUtils.simpleOffer(supplier)
            .updateAcceptanceStatusForTests(Offer.AcceptanceStatus.OK)
            .setCategoryIdForTests(CATEGORY_ID, Offer.BindingKind.APPROVED)
            .setModelId(MODEL_ID)
            .setAutomaticClassification(true)
            .setRecheckCategoryId(CATEGORY_ID)
            .setProcessingStatusInternal(Offer.ProcessingStatus.IN_RECHECK_CLASSIFICATION)
            .setRecheckClassificationSource(Offer.RecheckClassificationSource.PARTNER)
            .setRecheckClassificationStatus(Offer.RecheckClassificationStatus.ON_RECHECK)
            .setSupplierCategoryId(FIXED_CATEGORY_ID)
            .setSupplierCategoryMappingStatus(Offer.MappingStatus.NEW);

        var offerSupplierCategoryRejected = offerSupplierCategoryConfirmed.copy()
                .setSupplierCategoryId(GOOD_CATEGORY_ID);

        classificationOffersProcessingService.processOffer(supplier, offerSupplierCategoryConfirmed, FIXED_CATEGORY_ID);
        classificationOffersProcessingService.processOffer(supplier, offerSupplierCategoryRejected, FIXED_CATEGORY_ID);

        Consumer<Offer> categoryChangedValid = offer -> {
            assertThat(offer.getRecheckClassificationStatus())
                .isEqualTo(Offer.RecheckClassificationStatus.REJECTED);
            assertThat(offer.getProcessingStatus())
                .isEqualTo(Offer.ProcessingStatus.CLASSIFIED);
            assertThat(offer.getBindingKind())
                .isEqualTo(Offer.BindingKind.APPROVED);
            assertThat(offer.getCategoryId())
                .isEqualTo(FIXED_CATEGORY_ID);
            assertThat(offer.isAutomaticClassification())
                .isEqualTo(false);
            assertThat(offer.getModelId())
                    .isNull();
        };

        // operator change category to supplier one
        assertThat(offerSupplierCategoryConfirmed.getSupplierCategoryMappingStatus())
            .isEqualTo(Offer.MappingStatus.ACCEPTED);
        categoryChangedValid.accept(offerSupplierCategoryConfirmed);

        // operator change category to unexpected one
        assertThat(offerSupplierCategoryRejected.getSupplierCategoryMappingStatus())
            .isEqualTo(Offer.MappingStatus.REJECTED);
        categoryChangedValid.accept(offerSupplierCategoryRejected);
    }

    @Test
    public void testProcessOfferSetNeedInfoOnRecheckClassification() {
        Offer offer = OfferTestUtils.simpleOffer(supplier)
            .updateAcceptanceStatusForTests(Offer.AcceptanceStatus.OK)
            .setContentComments(new ContentComment(ContentCommentType.NO_KNOWLEDGE))
            .setCategoryIdForTests(CATEGORY_ID, Offer.BindingKind.APPROVED)
            .setModelId(MODEL_ID)
            .setAutomaticClassification(true)
            .setRecheckCategoryId(CATEGORY_ID)
            .setProcessingStatusInternal(Offer.ProcessingStatus.IN_RECHECK_CLASSIFICATION)
            .setRecheckClassificationSource(Offer.RecheckClassificationSource.PARTNER)
            .setRecheckClassificationStatus(Offer.RecheckClassificationStatus.ON_RECHECK)
            .setSupplierCategoryId(FIXED_CATEGORY_ID)
            .setSupplierCategoryMappingStatus(Offer.MappingStatus.NEW);

        classificationOffersProcessingService.processOffer(supplier, offer, null);

        assertThat(offer.getProcessingStatus())
            .isEqualTo(Offer.ProcessingStatus.NEED_INFO);
        assertThat(offer.getRecheckClassificationStatus())
            .isEqualTo(Offer.RecheckClassificationStatus.ON_RECHECK);
        assertThat(offer.getSupplierCategoryMappingStatus())
            .isEqualTo(Offer.MappingStatus.NEW);
    }
}

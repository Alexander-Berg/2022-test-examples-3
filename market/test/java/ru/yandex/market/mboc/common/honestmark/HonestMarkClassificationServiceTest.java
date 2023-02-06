package ru.yandex.market.mboc.common.honestmark;

import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import ru.yandex.market.mboc.common.dict.Supplier;
import ru.yandex.market.mboc.common.offers.model.Offer;
import ru.yandex.market.mboc.common.services.category.CategoryCachingServiceMock;
import ru.yandex.market.mboc.common.services.category_info.info.CategoryInfoCacheImpl;
import ru.yandex.market.mboc.common.services.category_info.info.CategoryInfoRepositoryMock;
import ru.yandex.market.mboc.common.services.mbousers.MboUsersRepositoryMock;
import ru.yandex.market.mboc.common.services.offers.processing.NeedContentStatusService;
import ru.yandex.market.mboc.common.utils.OfferTestUtils;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static ru.yandex.market.mboc.common.honestmark.HonestMarkTestUtils.BOOTS_ID;
import static ru.yandex.market.mboc.common.honestmark.HonestMarkTestUtils.BOOTS_NAME;
import static ru.yandex.market.mboc.common.honestmark.HonestMarkTestUtils.CONFIDENT_CLASSIFIER;
import static ru.yandex.market.mboc.common.honestmark.HonestMarkTestUtils.CONFIDENT_DEP;
import static ru.yandex.market.mboc.common.honestmark.HonestMarkTestUtils.HID_1;
import static ru.yandex.market.mboc.common.honestmark.HonestMarkTestUtils.NOT_CONFIDENT_CLASSIFIER;
import static ru.yandex.market.mboc.common.honestmark.HonestMarkTestUtils.NOT_CONFIDENT_DEP;
import static ru.yandex.market.mboc.common.honestmark.HonestMarkTestUtils.OTHER_HID;

public class HonestMarkClassificationServiceTest {

    private Set<Long> noGCAutoClassificationCategories;
    private NeedContentStatusService needContentStatusService;
    private HonestMarkDepartmentService honestMarkDepartmentService;
    private HonestMarkClassificationService honestMarkClassificationService;

    @Before
    public void setUp() throws Exception {
        CategoryCachingServiceMock categoryCachingService = new CategoryCachingServiceMock();
        needContentStatusService = Mockito.mock(NeedContentStatusService.class);
        honestMarkDepartmentService = Mockito.mock(HonestMarkDepartmentService.class);
        noGCAutoClassificationCategories = new HashSet<>();

        var categoryInfoRepositoryMock = new CategoryInfoRepositoryMock(new MboUsersRepositoryMock());
        var categoryInfoCache = new CategoryInfoCacheImpl(categoryInfoRepositoryMock);
        OfferCategoryRestrictionCalculator offerCategoryRestrictionCalculator =
            new OfferCategoryRestrictionCalculator(honestMarkDepartmentService, categoryInfoCache);

        honestMarkClassificationService = new HonestMarkClassificationService(
            noGCAutoClassificationCategories,
            categoryCachingService,
            needContentStatusService,
            offerCategoryRestrictionCalculator);
    }

    @Test
    public void getClassificationResultForBothUnconfident() {
        Supplier supplier = OfferTestUtils.simpleSupplier()
            .setNewContentPipeline(true);
        // сначала проверяем не для гудконтента
        Mockito.when(needContentStatusService.isGoodContentOffer(any(Offer.class), any(Supplier.class), anyLong()))
            .thenReturn(false);
        Offer offer = Offer.builder()
            .classifierCategoryId(HID_1)
            .classifierConfidence(NOT_CONFIDENT_CLASSIFIER)
            .honestMarkDepartmentId((int) HonestMarkDepartment.OTHER.getId())
            .honestMarkDepartmentProbability(NOT_CONFIDENT_DEP)
            .build();
        Set<Long> categoriesWithKnowledge = Set.of(HID_1);

        AutoClassificationResult classificationResult = honestMarkClassificationService.getClassificationResult(
            offer, offer.getClassifierCategoryId(),
            supplier, categoriesWithKnowledge, noGCAutoClassificationCategories);

        Assertions.assertThat(classificationResult.isAutoClassifiable()).isEqualTo(false);

        // затем для гудконтента
        Mockito.when(needContentStatusService.isGoodContentOffer(any(Offer.class), any(Supplier.class), anyLong()))
            .thenReturn(true);
        classificationResult = honestMarkClassificationService.getClassificationResult(
            offer, offer.getClassifierCategoryId(),
            supplier, categoriesWithKnowledge, noGCAutoClassificationCategories);

        Assertions.assertThat(classificationResult.isAutoClassifiable()).isEqualTo(false);
        Assertions.assertThat(classificationResult.getGcResult()).isPresent();
        Assertions.assertThat(classificationResult.getGcResult().get())
            .isEqualTo(GcClassificationResult.BOTH_UNCONFIDENT);
    }

    @Test
    public void getClassificationResultForUcHidConfident() {
        Supplier supplier = OfferTestUtils.simpleSupplier()
            .setNewContentPipeline(true);
        // сначала проверяем не для гудконтента
        Mockito.when(needContentStatusService.isGoodContentOffer(any(Offer.class), any(Supplier.class), anyLong()))
            .thenReturn(false);
        Set<Long> categoriesWithKnowledge = Set.of(HID_1);
        Offer offer = Offer.builder()
            .classifierCategoryId(HID_1)
            .classifierConfidence(CONFIDENT_CLASSIFIER)
            .honestMarkDepartmentId((int) HonestMarkDepartment.OTHER.getId())
            .honestMarkDepartmentProbability(NOT_CONFIDENT_DEP)
            .build();

        AutoClassificationResult classificationResult = honestMarkClassificationService.getClassificationResult(
            offer, offer.getClassifierCategoryId(), supplier,
            categoriesWithKnowledge, noGCAutoClassificationCategories);

        Assertions.assertThat(classificationResult.isAutoClassifiable()).isEqualTo(true);

        // затем для гудконтента
        Mockito.when(needContentStatusService.isGoodContentOffer(any(Offer.class), any(Supplier.class), anyLong()))
            .thenReturn(true);
        classificationResult = honestMarkClassificationService.getClassificationResult(
            offer, offer.getClassifierCategoryId(), supplier,
            categoriesWithKnowledge, noGCAutoClassificationCategories);

        Assertions.assertThat(classificationResult.isAutoClassifiable()).isEqualTo(true);
        Assertions.assertThat(classificationResult.getGcResult()).isPresent();
        Assertions.assertThat(classificationResult.getGcResult().get())
            .isEqualTo(GcClassificationResult.CONFIDENT_FOR_CLASSIFICATION);
    }

    @Test
    public void getClassificationResultForUcHidConfidentNoGcAutoClassification() {
        Supplier supplier = OfferTestUtils.simpleSupplier()
            .setNewContentPipeline(true);
        // сначала проверяем не для гудконтента
        Mockito.when(needContentStatusService.isGoodContentOffer(any(Offer.class), any(Supplier.class), anyLong()))
            .thenReturn(false);
        Set<Long> categoriesWithKnowledge = Set.of(HID_1);
        Offer offer = Offer.builder()
            .classifierCategoryId(HID_1)
            .classifierConfidence(CONFIDENT_CLASSIFIER)
            .honestMarkDepartmentId((int) HonestMarkDepartment.OTHER.getId())
            .honestMarkDepartmentProbability(NOT_CONFIDENT_DEP)
            .build();
        noGCAutoClassificationCategories.add(HID_1);

        AutoClassificationResult classificationResult = honestMarkClassificationService.getClassificationResult(
            offer, offer.getClassifierCategoryId(), supplier,
            categoriesWithKnowledge, noGCAutoClassificationCategories);

        Assertions.assertThat(classificationResult.isAutoClassifiable()).isEqualTo(true);

        // затем для гудконтента
        Mockito.when(needContentStatusService.isGoodContentOffer(any(Offer.class), any(Supplier.class), anyLong()))
            .thenReturn(true);
        classificationResult = honestMarkClassificationService.getClassificationResult(
            offer, offer.getClassifierCategoryId(), supplier,
            categoriesWithKnowledge, noGCAutoClassificationCategories);

        Assertions.assertThat(classificationResult.isAutoClassifiable()).isEqualTo(false);
        Assertions.assertThat(classificationResult.getGcResult()).isPresent();
        Assertions.assertThat(classificationResult.getGcResult().get())
            .isEqualTo(GcClassificationResult.ALWAYS_NEED_CLASSIFICATION);
    }

    @Test
    public void getClassificationResultForUcHidConfidentDepConflict() {
        Supplier supplier = OfferTestUtils.simpleSupplier()
            .setNewContentPipeline(true);
        Mockito.when(honestMarkDepartmentService.getDepartmentById((long) BOOTS_ID))
            .thenReturn(Optional.of(new HonestMarkDepartment(BOOTS_ID, BOOTS_NAME,
                Collections.singleton(OTHER_HID))));
        Offer offer = Offer.builder()
            .classifierCategoryId(HID_1)
            .classifierConfidence(NOT_CONFIDENT_CLASSIFIER)
            .honestMarkDepartmentId(BOOTS_ID)
            .honestMarkDepartmentProbability(CONFIDENT_DEP)
            .build();

        // сначала проверяем не для гудконтента
        Mockito.when(needContentStatusService.isGoodContentOffer(any(Offer.class), any(Supplier.class), anyLong()))
            .thenReturn(false);
        Set<Long> categoriesWithKnowledge = Set.of(HID_1);
        AutoClassificationResult classificationResult = honestMarkClassificationService.getClassificationResult(
            offer, offer.getClassifierCategoryId(),
            supplier, categoriesWithKnowledge, noGCAutoClassificationCategories);

        Assertions.assertThat(classificationResult.isAutoClassifiable()).isEqualTo(false);

        // затем для гудконтента
        Mockito.when(needContentStatusService.isGoodContentOffer(any(Offer.class), any(Supplier.class), anyLong()))
            .thenReturn(true);
        classificationResult = honestMarkClassificationService.getClassificationResult(
            offer, offer.getClassifierCategoryId(),
            supplier, categoriesWithKnowledge, noGCAutoClassificationCategories);

        Assertions.assertThat(classificationResult.isAutoClassifiable()).isEqualTo(false);
        Assertions.assertThat(classificationResult.getGcResult()).isPresent();
        Assertions.assertThat(classificationResult.getGcResult().get())
            .isEqualTo(GcClassificationResult.CONFIDENT_FOR_DEPARTMENT_WITH_CONFLICT);
    }

    @Test
    public void getClassificationResultForUcHidConfidentDepOk() {
        Supplier supplier = OfferTestUtils.simpleSupplier()
            .setNewContentPipeline(true);
        Mockito.when(honestMarkDepartmentService.getDepartmentById((long) BOOTS_ID))
            .thenReturn(Optional.of(new HonestMarkDepartment(BOOTS_ID, BOOTS_NAME,
                Collections.singleton(HID_1))));

        // сначала проверяем не для гудконтента
        Mockito.when(needContentStatusService.isGoodContentOffer(any(Offer.class), any(Supplier.class), anyLong()))
            .thenReturn(false);
        Set<Long> categoriesWithKnowledge = Set.of(HID_1);
        Offer offer = Offer.builder()
            .classifierCategoryId(HID_1)
            .classifierConfidence(NOT_CONFIDENT_CLASSIFIER)
            .honestMarkDepartmentId(BOOTS_ID)
            .honestMarkDepartmentProbability(CONFIDENT_DEP)
            .build();

        AutoClassificationResult classificationResult = honestMarkClassificationService.getClassificationResult(
            offer, offer.getClassifierCategoryId(),
            supplier, categoriesWithKnowledge, noGCAutoClassificationCategories);

        Assertions.assertThat(classificationResult.isAutoClassifiable()).isEqualTo(false);

        // затем для гудконтента
        Mockito.when(needContentStatusService.isGoodContentOffer(any(Offer.class), any(Supplier.class), anyLong()))
            .thenReturn(true);
        classificationResult = honestMarkClassificationService.getClassificationResult(
            offer, offer.getClassifierCategoryId(),
            supplier, categoriesWithKnowledge, noGCAutoClassificationCategories);

        Assertions.assertThat(classificationResult.isAutoClassifiable()).isEqualTo(true);
        Assertions.assertThat(classificationResult.getGcResult()).isPresent();
        Assertions.assertThat(classificationResult.getGcResult().get())
            .isEqualTo(GcClassificationResult.CONFIDENT_FOR_DEPARTMENT_WITHOUT_CONFLICT);
    }

    @Test
    public void getClassificationResultForUcHidConfidentDepOkNoGcAutoClassification() {
        Supplier supplier = OfferTestUtils.simpleSupplier()
            .setNewContentPipeline(true);
        Mockito.when(honestMarkDepartmentService.getDepartmentByName(BOOTS_NAME))
            .thenReturn(Optional.of(new HonestMarkDepartment(BOOTS_ID, BOOTS_NAME,
                Collections.singleton(HID_1))));

        // сначала проверяем не для гудконтента
        Mockito.when(needContentStatusService.isGoodContentOffer(any(Offer.class), any(Supplier.class), anyLong()))
            .thenReturn(false);
        Set<Long> categoriesWithKnowledge = Set.of(HID_1);
        Offer offer = Offer.builder()
            .classifierCategoryId(HID_1)
            .classifierConfidence(NOT_CONFIDENT_CLASSIFIER)
            .honestMarkDepartmentId(BOOTS_ID)
            .honestMarkDepartmentProbability(CONFIDENT_DEP)
            .build();
        noGCAutoClassificationCategories.add(HID_1);

        AutoClassificationResult classificationResult = honestMarkClassificationService.getClassificationResult(
            offer, offer.getClassifierCategoryId(),
            supplier, categoriesWithKnowledge, noGCAutoClassificationCategories);

        Assertions.assertThat(classificationResult.isAutoClassifiable()).isEqualTo(false);

        // затем для гудконтента
        Mockito.when(needContentStatusService.isGoodContentOffer(any(Offer.class), any(Supplier.class), anyLong()))
            .thenReturn(true);
        classificationResult = honestMarkClassificationService.getClassificationResult(
            offer, offer.getClassifierCategoryId(),
            supplier, categoriesWithKnowledge, noGCAutoClassificationCategories);

        Assertions.assertThat(classificationResult.isAutoClassifiable()).isEqualTo(false);
        Assertions.assertThat(classificationResult.getGcResult()).isPresent();
        Assertions.assertThat(classificationResult.getGcResult().get())
            .isEqualTo(GcClassificationResult.ALWAYS_NEED_CLASSIFICATION);
    }

    @Test
    public void getClassificationResultForGoodContentWithOther() {
        Supplier supplier = OfferTestUtils.simpleSupplier()
            .setNewContentPipeline(true);
        Mockito.when(honestMarkDepartmentService.getDepartmentById(HonestMarkDepartment.OTHER.getId()))
            .thenReturn(Optional.of(HonestMarkDepartment.OTHER));

        // сначала проверяем не для гудконтента
        Mockito.when(needContentStatusService.isGoodContentOffer(any(Offer.class), any(Supplier.class), anyLong()))
            .thenReturn(false);
        Set<Long> categoriesWithKnowledge = Set.of(HID_1);
        Offer offer = Offer.builder()
            .classifierCategoryId(HID_1)
            .classifierConfidence(NOT_CONFIDENT_CLASSIFIER)
            .honestMarkDepartmentId((int) HonestMarkDepartment.OTHER.getId())
            .honestMarkDepartmentProbability(CONFIDENT_DEP)
            .build();

        AutoClassificationResult classificationResult = honestMarkClassificationService.getClassificationResult(
            offer, offer.getClassifierCategoryId(),
            supplier, categoriesWithKnowledge, noGCAutoClassificationCategories);

        Assertions.assertThat(classificationResult.isAutoClassifiable()).isEqualTo(false);

        // затем для гудконтента
        Mockito.when(needContentStatusService.isGoodContentOffer(any(Offer.class), any(Supplier.class), anyLong()))
            .thenReturn(true);
        classificationResult = honestMarkClassificationService.getClassificationResult(
            offer, offer.getClassifierCategoryId(),
            supplier, categoriesWithKnowledge, noGCAutoClassificationCategories);

        Assertions.assertThat(classificationResult.isAutoClassifiable()).isEqualTo(true);
        Assertions.assertThat(classificationResult.getGcResult()).isPresent();
        Assertions.assertThat(classificationResult.getGcResult().get())
            .isEqualTo(GcClassificationResult.CONFIDENT_FOR_DEPARTMENT_OTHER_WITHOUT_CONFLICT);
    }

    @Test
    public void getClassificationResultForGoodContentWithOtherNoGCAutoClassification() {
        Supplier supplier = OfferTestUtils.simpleSupplier()
            .setNewContentPipeline(true);
        Mockito.when(honestMarkDepartmentService.getDepartmentByName(HonestMarkDepartment.OTHER.getName()))
            .thenReturn(Optional.of(HonestMarkDepartment.OTHER));

        // сначала проверяем не для гудконтента
        Mockito.when(needContentStatusService.isGoodContentOffer(any(Offer.class), any(Supplier.class), anyLong()))
            .thenReturn(false);
        Set<Long> categoriesWithKnowledge = Set.of(HID_1);
        Offer offer = Offer.builder()
            .classifierCategoryId(HID_1)
            .classifierConfidence(NOT_CONFIDENT_CLASSIFIER)
            .honestMarkDepartmentId((int) HonestMarkDepartment.OTHER.getId())
            .honestMarkDepartmentProbability(CONFIDENT_DEP)
            .build();
        noGCAutoClassificationCategories.add(HID_1);

        AutoClassificationResult classificationResult = honestMarkClassificationService.getClassificationResult(
            offer, offer.getClassifierCategoryId(),
            supplier, categoriesWithKnowledge, noGCAutoClassificationCategories);

        Assertions.assertThat(classificationResult.isAutoClassifiable()).isEqualTo(false);

        // затем для гудконтента
        Mockito.when(needContentStatusService.isGoodContentOffer(any(Offer.class), any(Supplier.class), anyLong()))
            .thenReturn(true);
        classificationResult = honestMarkClassificationService.getClassificationResult(
            offer, offer.getClassifierCategoryId(),
            supplier, categoriesWithKnowledge, noGCAutoClassificationCategories);

        Assertions.assertThat(classificationResult.isAutoClassifiable()).isEqualTo(false);
        Assertions.assertThat(classificationResult.getGcResult()).isPresent();
        Assertions.assertThat(classificationResult.getGcResult().get())
            .isEqualTo(GcClassificationResult.ALWAYS_NEED_CLASSIFICATION);
    }
}

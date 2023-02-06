package ru.yandex.market.mboc.common.honestmark;

import java.util.Collections;
import java.util.Optional;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import ru.yandex.market.mboc.common.offers.model.Offer;
import ru.yandex.market.mboc.common.services.category_info.info.CategoryInfo;
import ru.yandex.market.mboc.common.services.category_info.info.CategoryInfoCache;
import ru.yandex.market.mboc.common.utils.OfferTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static ru.yandex.market.mboc.common.honestmark.HonestMarkTestUtils.ALLOW_GC_THRESHOLD;
import static ru.yandex.market.mboc.common.honestmark.HonestMarkTestUtils.ALLOW_GC_THRESHOLD_ZERO;
import static ru.yandex.market.mboc.common.honestmark.HonestMarkTestUtils.BOOTS_NAME;
import static ru.yandex.market.mboc.common.honestmark.HonestMarkTestUtils.CONFIDENT_CLASSIFIER;
import static ru.yandex.market.mboc.common.honestmark.HonestMarkTestUtils.CONFIDENT_DEP;
import static ru.yandex.market.mboc.common.honestmark.HonestMarkTestUtils.DEFAULT_CLASSIFIER;
import static ru.yandex.market.mboc.common.honestmark.HonestMarkTestUtils.HID_1;
import static ru.yandex.market.mboc.common.honestmark.HonestMarkTestUtils.NOT_CONFIDENT_CLASSIFIER;
import static ru.yandex.market.mboc.common.honestmark.HonestMarkTestUtils.NOT_CONFIDENT_CLASSIFIER_NO_GC;
import static ru.yandex.market.mboc.common.honestmark.HonestMarkTestUtils.NOT_CONFIDENT_DEP;
import static ru.yandex.market.mboc.common.honestmark.HonestMarkTestUtils.NULL_HID;
import static ru.yandex.market.mboc.common.honestmark.HonestMarkTestUtils.OTHER_HID;

public class OfferCategoryRestrictionCalculatorTest {

    private HonestMarkDepartmentService honestMarkDepartmentService;
    private OfferCategoryRestrictionCalculator calculator;
    private CategoryInfoCache categoryInfoCache;

    @Before
    public void setUp() throws Exception {
        honestMarkDepartmentService = Mockito.mock(HonestMarkDepartmentService.class);
        categoryInfoCache = Mockito.mock(CategoryInfoCache.class);
        CategoryInfo cachedCategoryInfo = new CategoryInfo()
            .setClassifierTrustThreshold((float) DEFAULT_CLASSIFIER)
            .setAllowGcBlueThreshold((float) ALLOW_GC_THRESHOLD_ZERO)
            .setAllowGcDsbsThreshold((float) ALLOW_GC_THRESHOLD_ZERO)
            .setAllowGcOtherThreshold((float) ALLOW_GC_THRESHOLD_ZERO);
        Mockito.when(categoryInfoCache.getCategoryInfoById(anyLong())).thenReturn(Optional.of(cachedCategoryInfo));
        calculator = new OfferCategoryRestrictionCalculator(honestMarkDepartmentService, categoryInfoCache);
    }

    @Test
    public void calculateRestrictionForConfidentUc() {
        Offer offer = OfferTestUtils.simpleOffer().setCategoryIdForTests(HID_1, Offer.BindingKind.APPROVED)
            .setAutomaticClassification(true)
            .setClassifierCategoryId(HID_1, CONFIDENT_CLASSIFIER);
        CategoryRestriction categoryRestriction = calculator.calculateRestriction(offer).orElseThrow();
        assertThat(categoryRestriction).isInstanceOf(SingleCategoryRestriction.class);
        assertThat(categoryRestriction.getType()).isEqualTo(AllowedCategoryType.SINGLE);
        assertThat(categoryRestriction.getAllowedCategoryId()).isEqualTo(HID_1);
    }

    @Test
    public void calculateRestrictionForConfidentUcNullHid() {
        Offer offer = OfferTestUtils.simpleOffer().setCategoryIdForTests(NULL_HID, Offer.BindingKind.SUGGESTED)
            .setClassifierCategoryId(NULL_HID, CONFIDENT_CLASSIFIER);
        Optional<CategoryRestriction> categoryRestriction = calculator.calculateRestriction(offer);
        assertThat(categoryRestriction).isNotPresent();
    }

    @Test
    public void calculateRestrictionForNotConfidentUcAndAbsentDepartments() {
        Offer offer = OfferTestUtils.simpleOffer().setCategoryIdForTests(OTHER_HID, Offer.BindingKind.SUGGESTED)
            .setClassifierCategoryId(NULL_HID, NOT_CONFIDENT_CLASSIFIER);
        Optional<CategoryRestriction> categoryRestriction = calculator.calculateRestriction(offer);
        assertThat(categoryRestriction).isNotPresent();
    }

    @Test
    public void calculateRestrictionForNotConfidentUcAndNotConfidentDepartments() {
        Offer offer = OfferTestUtils.simpleOffer().setCategoryIdForTests(OTHER_HID, Offer.BindingKind.SUGGESTED)
            .setClassifierCategoryId(NULL_HID, NOT_CONFIDENT_CLASSIFIER)
            .setHonestMarkDepartmentId(2)
            .setHonestMarkDepartmentProbability(NOT_CONFIDENT_DEP);
        Optional<CategoryRestriction> categoryRestriction = calculator.calculateRestriction(offer);
        assertThat(categoryRestriction).isNotPresent();
    }

    @Test
    public void calculateRestrictionForNotConfidentUcAndDisagreeDepartments() {
        final int drugs = 2;
        final long bootsRelatedHid = 11;
        Mockito.when(honestMarkDepartmentService.getDepartmentById((long) drugs))
            .thenReturn(Optional.of(new HonestMarkDepartment(2, "drugs", Collections.singleton(OTHER_HID))));
        Offer offer = OfferTestUtils.simpleOffer().setCategoryIdForTests(OTHER_HID, Offer.BindingKind.SUGGESTED)
            .setClassifierCategoryId(bootsRelatedHid, NOT_CONFIDENT_CLASSIFIER)
            .setHonestMarkDepartmentId(2)
            .setHonestMarkDepartmentProbability(CONFIDENT_DEP);
        Optional<CategoryRestriction> categoryRestriction = calculator.calculateRestriction(offer);
        assertThat(categoryRestriction).isNotPresent();
    }

    @Test
    public void calculateRestrictionForNotConfidentUcAndAgreeDepartment() {
        final long bootsId = 1;
        final long bootRelatedHid = 11;
        Offer offer = OfferTestUtils.simpleOffer().setCategoryIdForTests(OTHER_HID, Offer.BindingKind.SUGGESTED)
            .setClassifierCategoryId(bootRelatedHid, NOT_CONFIDENT_CLASSIFIER)
            .setHonestMarkDepartmentId((int) bootsId)
            .setHonestMarkDepartmentProbability(CONFIDENT_DEP);
        Mockito.when(honestMarkDepartmentService.getDepartmentById(bootsId))
            .thenReturn(Optional.of(new HonestMarkDepartment(bootsId, BOOTS_NAME,
                Collections.singleton(bootRelatedHid))));
        CategoryRestriction categoryRestriction = calculator.calculateRestriction(offer).orElseThrow();
        assertThat(categoryRestriction).isInstanceOf(GroupCategoryRestriction.class);
        assertThat(categoryRestriction.getType()).isEqualTo(AllowedCategoryType.GROUP);
        assertThat(categoryRestriction.getAllowedGroupId()).isEqualTo(bootsId);
    }

    @Test
    public void calculateRestrictionForConfidentUcCheckCategoryIdIsIgnored() {
        final long bootRelatedHid = 11;
        // если категория approved и отличается от уверенной классификаторной
        Offer offer = OfferTestUtils.simpleOffer().setCategoryIdForTests(bootRelatedHid + 1, Offer.BindingKind.APPROVED)
            .setClassifierCategoryId(bootRelatedHid, CONFIDENT_CLASSIFIER)
            .setAutomaticClassification(true);
        CategoryRestriction categoryRestriction = calculator.calculateRestriction(offer).orElseThrow();
        assertThat(categoryRestriction).isInstanceOf(SingleCategoryRestriction.class);
        assertThat(categoryRestriction.getType()).isEqualTo(AllowedCategoryType.SINGLE);
        assertThat(categoryRestriction.getAllowedCategoryId()).isEqualTo(bootRelatedHid);
    }

    @Test
    public void calculateRestrictionForNotConfidentUcCheckCategoryIdIsIgnored() {
        final long bootRelatedHid = 11;
        final long bootsId = 1;
        Offer offer = OfferTestUtils.simpleOffer().setCategoryIdForTests(bootRelatedHid + 1, Offer.BindingKind.APPROVED)
            .setClassifierCategoryId(bootRelatedHid, NOT_CONFIDENT_CLASSIFIER)
            .setAutomaticClassification(true)
            .setHonestMarkDepartmentId((int) bootsId)
            .setHonestMarkDepartmentProbability(CONFIDENT_DEP);
        Mockito.when(honestMarkDepartmentService.getDepartmentById(bootsId))
            .thenReturn(Optional.of(new HonestMarkDepartment(bootsId, BOOTS_NAME,
                Collections.singleton(bootRelatedHid))));
        CategoryRestriction categoryRestriction = calculator.calculateRestriction(offer)
            .orElseThrow();
        assertThat(categoryRestriction).isInstanceOf(GroupCategoryRestriction.class);
        assertThat(categoryRestriction.getType()).isEqualTo(AllowedCategoryType.GROUP);
        assertThat(categoryRestriction.getAllowedGroupId())
            .isEqualTo(bootsId);
    }

    @Test
    public void calculateRestrictionForUcHidIsNull() {
        Offer offer = OfferTestUtils.simpleOffer()
            .setAutomaticClassification(true)
            .setClassifierCategoryId(null, CONFIDENT_CLASSIFIER);
        Optional<CategoryRestriction> categoryRestriction = calculator.calculateRestriction(offer);
        assertThat(categoryRestriction).isNotPresent();
    }

    @Test
    public void calculateRestrictionOverriddenByContent() {
        Offer offer = OfferTestUtils.simpleOffer().setCategoryIdForTests(OTHER_HID, Offer.BindingKind.APPROVED)
            .setAutomaticClassification(true)
            .setClassifierCategoryId(OTHER_HID, CONFIDENT_CLASSIFIER);
        offer.setMappedCategoryId(HID_1, Offer.MappingConfidence.CONTENT);
        CategoryRestriction categoryRestriction = calculator.calculateRestriction(offer).orElseThrow();
        assertThat(categoryRestriction).isInstanceOf(SingleCategoryRestriction.class);
        assertThat(categoryRestriction.getType()).isEqualTo(AllowedCategoryType.SINGLE);
        assertThat(categoryRestriction.getAllowedCategoryId()).isEqualTo(HID_1);
    }

    @Test
    public void testClassificationResultForConfidentClassifier() {
        Offer offer = OfferTestUtils.simpleOffer().setCategoryIdForTests(OTHER_HID, Offer.BindingKind.APPROVED)
            .setAutomaticClassification(true)
            .setClassifierCategoryId(OTHER_HID, CONFIDENT_CLASSIFIER);

        boolean isConfident = calculator.calculateClassificationResult(offer.getCategoryId(), offer).isConfident();
        assertThat(isConfident).isTrue();
    }

    @Test
    public void testClassificationResultForNotConfidentClassifier() {
        Offer offer = OfferTestUtils.simpleOffer().setCategoryIdForTests(OTHER_HID, Offer.BindingKind.APPROVED)
            .setAutomaticClassification(true)
            .setClassifierCategoryId(OTHER_HID, NOT_CONFIDENT_CLASSIFIER);

        boolean isConfident = calculator.calculateClassificationResult(offer.getCategoryId(), offer).isConfident();
        assertThat(isConfident).isFalse();
    }

    @Test
    public void whenUnconfidentAboveThresholdThenAllowGcClassification() {
        Offer blueOffer = OfferTestUtils.simpleOffer().setCategoryIdForTests(OTHER_HID, Offer.BindingKind.APPROVED)
            .setAutomaticClassification(true)
            .setClassifierCategoryId(OTHER_HID, NOT_CONFIDENT_CLASSIFIER);

        categoryInfoCache.getCategoryInfoById(OTHER_HID).get()
            .setAllowGcBlueThreshold((float) ALLOW_GC_THRESHOLD);

        ClassificationResult classificationResult =
            calculator.calculateClassificationResult(blueOffer.getCategoryId(), blueOffer);

        assertThat(classificationResult.isConfident()).isFalse();
        assertThat(classificationResult.isAllowGcClassification()).isTrue();
    }

    @Test
    public void whenUnconfidentBelowThresholdThenNotAllowGcClassification() {
        Offer blueOffer = OfferTestUtils.simpleOffer().setCategoryIdForTests(OTHER_HID, Offer.BindingKind.APPROVED)
            .setAutomaticClassification(true)
            .setClassifierCategoryId(OTHER_HID, NOT_CONFIDENT_CLASSIFIER_NO_GC);

        categoryInfoCache.getCategoryInfoById(OTHER_HID).get()
            .setAllowGcBlueThreshold((float) ALLOW_GC_THRESHOLD);

        ClassificationResult classificationResult =
            calculator.calculateClassificationResult(blueOffer.getCategoryId(), blueOffer);

        assertThat(classificationResult.isConfident()).isFalse();
        assertThat(classificationResult.isAllowGcClassification()).isFalse();
    }

    @Test
    public void whenUnconfidentAboveThresholdThenRestrictionIsNotEmpty() {
        Offer blueOffer = OfferTestUtils.simpleOffer().setCategoryIdForTests(OTHER_HID, Offer.BindingKind.APPROVED)
            .setAutomaticClassification(true)
            .setClassifierCategoryId(OTHER_HID, NOT_CONFIDENT_CLASSIFIER)
            .setHonestMarkDepartmentId(1)
            .setHonestMarkDepartmentProbability(CONFIDENT_DEP);
        Mockito.when(honestMarkDepartmentService.getDepartmentById(1L))
            .thenReturn(Optional.of(new HonestMarkDepartment(1L, BOOTS_NAME, Collections.singleton(OTHER_HID))));

        categoryInfoCache.getCategoryInfoById(OTHER_HID).get()
            .setAllowGcBlueThreshold((float) ALLOW_GC_THRESHOLD);

        Optional<CategoryRestriction> categoryRestriction = calculator.calculateRestriction(blueOffer);

        assertThat(categoryRestriction).isNotEmpty();
    }

    @Test
    public void whenUnconfidentBelowThresholdThenRestrictionIsNotEmpty() {
        Offer blueOffer = OfferTestUtils.simpleOffer().setCategoryIdForTests(OTHER_HID, Offer.BindingKind.APPROVED)
            .setAutomaticClassification(true)
            .setClassifierCategoryId(OTHER_HID, NOT_CONFIDENT_CLASSIFIER_NO_GC)
            .setHonestMarkDepartmentId(1)
            .setHonestMarkDepartmentProbability(CONFIDENT_DEP);
        Mockito.when(honestMarkDepartmentService.getDepartmentById(1L))
            .thenReturn(Optional.of(new HonestMarkDepartment(1L, BOOTS_NAME, Collections.singleton(OTHER_HID))));

        categoryInfoCache.getCategoryInfoById(OTHER_HID).get()
            .setAllowGcBlueThreshold((float) ALLOW_GC_THRESHOLD);

        Optional<CategoryRestriction> categoryRestriction = calculator.calculateRestriction(blueOffer);

        assertThat(categoryRestriction).isEmpty();
    }
}

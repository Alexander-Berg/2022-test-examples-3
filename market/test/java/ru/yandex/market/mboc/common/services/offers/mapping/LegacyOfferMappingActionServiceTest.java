package ru.yandex.market.mboc.common.services.offers.mapping;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import ru.yandex.market.mbo.storage.StorageKeyValueService;
import ru.yandex.market.mboc.common.MbocErrors;
import ru.yandex.market.mboc.common.assertions.MbocAssertions;
import ru.yandex.market.mboc.common.assertions.custom.OfferAssertions;
import ru.yandex.market.mboc.common.dict.Supplier;
import ru.yandex.market.mboc.common.honestmark.EmptyCategoryRestriction;
import ru.yandex.market.mboc.common.honestmark.GroupCategoryRestriction;
import ru.yandex.market.mboc.common.honestmark.OfferCategoryRestrictionCalculator;
import ru.yandex.market.mboc.common.honestmark.SingleCategoryRestriction;
import ru.yandex.market.mboc.common.offers.DefaultOfferDestinationCalculator;
import ru.yandex.market.mboc.common.offers.OfferDestinationCalculator;
import ru.yandex.market.mboc.common.offers.model.Offer;
import ru.yandex.market.mboc.common.offers.model.Offer.MappingStatus;
import ru.yandex.market.mboc.common.services.category.models.Category;
import ru.yandex.market.mboc.common.services.modelstorage.ModelStorageCachingService;
import ru.yandex.market.mboc.common.services.modelstorage.models.Model;
import ru.yandex.market.mboc.common.services.modelstorage.models.SimpleModel;
import ru.yandex.market.mboc.common.services.offers.mapping.context.CategoryMappingContext;
import ru.yandex.market.mboc.common.services.offers.mapping.context.SkuMappingContext;
import ru.yandex.market.mboc.common.services.offers.mapping.context.SupplierSkuMappingContext;
import ru.yandex.market.mboc.common.services.offers.processing.NeedContentStatusService;
import ru.yandex.market.mboc.common.services.storage.StorageKeyValueServiceMock;
import ru.yandex.market.mboc.common.utils.OfferTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;

public class LegacyOfferMappingActionServiceTest {

    private static final long CATEGORY_1 = 1;
    private static final long CATEGORY_2 = 2;

    private NeedContentStatusService needContentStatusService;
    private OfferMappingActionService offerMappingActionService;
    private OfferMappingActionService offerMappingActionServiceEnabledHm;
    private OfferCategoryRestrictionCalculator categoryRestrictionCalculator;
    private final OfferDestinationCalculator offerDestinationCalculator = new DefaultOfferDestinationCalculator();
    private StorageKeyValueService storageKeyValueService;

    @Before
    public void setUp() throws Exception {
        storageKeyValueService = new StorageKeyValueServiceMock();
        needContentStatusService = Mockito.mock(NeedContentStatusService.class);
        categoryRestrictionCalculator = Mockito.mock(OfferCategoryRestrictionCalculator.class);
        var legacyOfferMappingActionService = new LegacyOfferMappingActionService(needContentStatusService,
            categoryRestrictionCalculator, offerDestinationCalculator, storageKeyValueService);
        offerMappingActionService = new OfferMappingActionService(legacyOfferMappingActionService);
        var legacyOfferMappingActionServiceEnabledHm = new LegacyOfferMappingActionService(needContentStatusService,
            categoryRestrictionCalculator, offerDestinationCalculator, storageKeyValueService);
        offerMappingActionServiceEnabledHm =
            new OfferMappingActionService(legacyOfferMappingActionServiceEnabledHm);
    }

    public void setCategoryMappingWhenContentProcessingCheck(boolean enableChangeCategory) {
        Offer offer = OfferTestUtils.simpleOffer()
            .setDataCampOffer(true)
            .setCategoryIdForTests(CATEGORY_1, Offer.BindingKind.SUGGESTED)
            .updateProcessingStatusIfValid(Offer.ProcessingStatus.CONTENT_PROCESSING);
        Supplier supplier = OfferTestUtils.simpleSupplier().setNewContentPipeline(true);
        Category category = new Category().setCategoryId(CATEGORY_2)
            .setAcceptContentFromWhiteShops(true);
        var context = new CategoryMappingContext()
            .setSupplier(supplier)
            .setCategory(category);
        offerMappingActionService.PARTNER.setCategoryMapping(offer, context);
        if (enableChangeCategory) {
            OfferAssertions.assertThat(offer)
                .hasCategoryId(CATEGORY_2)
                .hasSupplierCategoryId(CATEGORY_2)
                .hasSupplierCategoryMappingStatus(MappingStatus.ACCEPTED)
                .hasMappedCategoryId(CATEGORY_2);
        } else {
            OfferAssertions.assertThat(offer)
                .hasCategoryId(CATEGORY_1)
                .hasSupplierCategoryId(CATEGORY_2)
                .hasSupplierCategoryMappingStatus(MappingStatus.NEW)
                .doesNotHaveMappedCategoryId();
        }
        // make category no goodcontent, for this case supplier category should be rejected
        category.setAcceptContentFromWhiteShops(false);
        offerMappingActionService.PARTNER.setCategoryMapping(offer, context);
        if (enableChangeCategory) {
            OfferAssertions.assertThat(offer)
                .hasCategoryId(CATEGORY_2)
                .hasSupplierCategoryId(CATEGORY_2)
                .hasSupplierCategoryMappingStatus(MappingStatus.ACCEPTED)
                .hasMappedCategoryId(CATEGORY_2);
        } else {
            OfferAssertions.assertThat(offer)
                .hasSupplierCategoryId(CATEGORY_2)
                .hasSupplierCategoryMappingStatus(MappingStatus.REJECTED)
                .hasCategoryId(CATEGORY_1)
                .doesNotHaveMappedCategoryId();
            assertThat(offer.getContentStatusActiveError().render())
                .contains("В выбранной категории не разрешен прием гудконтента (оффер 'shop-sku')");
        }

        // when activated new functionality, now there should be error message too
        Offer offer2 = OfferTestUtils.simpleOffer()
            .setCategoryIdForTests(CATEGORY_1, Offer.BindingKind.SUGGESTED)
            .updateProcessingStatusIfValid(Offer.ProcessingStatus.CONTENT_PROCESSING);
        Category categoryNonGc = new Category().setCategoryId(CATEGORY_2)
            .setAcceptGoodContent(false);
        context
            .setCategory(categoryNonGc);
        offerMappingActionServiceEnabledHm.PARTNER.setCategoryMapping(offer2, context);
        OfferAssertions.assertThat(offer2)
            .hasSupplierCategoryId(CATEGORY_2)
            .hasSupplierCategoryMappingStatus(MappingStatus.REJECTED)
            .hasCategoryId(CATEGORY_1)
            .doesNotHaveMappedCategoryId();
        assertThat(offer2.getContentStatusActiveError().getErrorCode())
            .isEqualTo("mboc.error.category-mapping.category-does-not-accept-goodcontent");
    }

    @Test
    public void setCategoryMappingWhenContentProcessingOnFeatureFlag() {
        setCategoryMappingWhenContentProcessingCheck(false);
        storageKeyValueService.putValue(LegacyOfferMappingActionService.ENABLE_CHANGE_PSKU_CATEGORY_KEY, true);
        setCategoryMappingWhenContentProcessingCheck(true);
    }

    public void setPartnerCategoryMappingWillOverwriteCategoryCheck(boolean enableChangeCategory) {
        Offer offer = OfferTestUtils.simpleOffer()
            .setCategoryIdForTests(CATEGORY_1, Offer.BindingKind.APPROVED)
            .setMappedCategoryId(CATEGORY_1, Offer.MappingConfidence.CONTENT);
        Offer autoclassifiedOffer = OfferTestUtils.simpleOffer().setCategoryIdForTests(CATEGORY_1,
            Offer.BindingKind.APPROVED);
        Offer autoclassifiedOffer2 = autoclassifiedOffer.copy();
        Supplier supplier = OfferTestUtils.simpleSupplier();
        Category category = new Category().setCategoryId(CATEGORY_2);
        var context = new CategoryMappingContext()
            .setSupplier(supplier)
            .setCategory(category);
        offerMappingActionService.PARTNER.setCategoryMapping(offer, context);
        var expectedStatus = enableChangeCategory ? MappingStatus.ACCEPTED : MappingStatus.REJECTED;
       OfferAssertions.assertThat(offer)
            .hasSupplierCategoryId(CATEGORY_2)
            .hasSupplierCategoryMappingStatus(expectedStatus);
        // autoclassified offers:
        context = new CategoryMappingContext()
            .setSupplier(supplier)
            .setCategory(category);
        offerMappingActionService.PARTNER.setCategoryMapping(autoclassifiedOffer, context);
        OfferAssertions.assertThat(autoclassifiedOffer)
            .hasSupplierCategoryId(CATEGORY_2)
            .hasSupplierCategoryMappingStatus(MappingStatus.ACCEPTED);
        context = new CategoryMappingContext()
            .setSupplier(supplier)
            .setCategory(category);
        offerMappingActionService.PARTNER.setCategoryMapping(autoclassifiedOffer2, context);
        OfferAssertions.assertThat(autoclassifiedOffer2)
            .hasSupplierCategoryId(CATEGORY_2)
            .hasSupplierCategoryMappingStatus(MappingStatus.ACCEPTED);
    }

    @Test
    public void setPartnerCategoryMappingOverwriteCategoryOnFeatureFlag() {
        setPartnerCategoryMappingWillOverwriteCategoryCheck(false);
        storageKeyValueService.putValue(LegacyOfferMappingActionService.ENABLE_CHANGE_PSKU_CATEGORY_KEY, true);
        setPartnerCategoryMappingWillOverwriteCategoryCheck(true);
    }

    @Test
    public void setCategoryMappingWhenOfferHasModelOrSku() {
        Supplier supplier = OfferTestUtils.simpleSupplier();
        Category category = new Category().setCategoryId(CATEGORY_2)
            .setAcceptGoodContent(true);
        // case 1: has model
        Offer offerWithModel = OfferTestUtils.simpleOffer()
            .setCategoryIdForTests(CATEGORY_1, Offer.BindingKind.SUGGESTED)
            .setModelId(1L);
        var context = new CategoryMappingContext()
            .setSupplier(supplier)
            .setCategory(category);
        offerMappingActionService.PARTNER.setCategoryMapping(offerWithModel, context);
        OfferAssertions.assertThat(offerWithModel)
            .hasCategoryId(CATEGORY_1)
            .hasSupplierCategoryId(CATEGORY_2)
            .hasSupplierCategoryMappingStatus(MappingStatus.REJECTED)
            .doesNotHaveMappedCategoryId();
        // case 2: has sku mapping
        Offer offerWithSku = OfferTestUtils.simpleOffer()
            .setCategoryIdForTests(CATEGORY_1, Offer.BindingKind.SUGGESTED)
            .updateApprovedSkuMapping(Offer.Mapping.fromSku(ModelStorageCachingService.NEED_CONTENT_MODEL),
                Offer.MappingConfidence.PARTNER);
        Offer offerForMsg = offerWithSku.copy();
        context = new CategoryMappingContext()
            .setSupplier(supplier)
            .setCategory(category);
        offerMappingActionService.PARTNER.setCategoryMapping(offerWithSku, context);
        OfferAssertions.assertThat(offerWithSku)
            .hasCategoryId(CATEGORY_1)
            .hasSupplierCategoryId(CATEGORY_2)
            .hasSupplierCategoryMappingStatus(MappingStatus.REJECTED)
            .doesNotHaveMappedCategoryId();
        // check error msg
        context = new CategoryMappingContext()
            .setSupplier(supplier)
            .setCategory(category);
        offerMappingActionServiceEnabledHm.PARTNER.setCategoryMapping(offerForMsg, context);
        OfferAssertions.assertThat(offerForMsg)
            .hasCategoryId(CATEGORY_1)
            .hasSupplierCategoryId(CATEGORY_2)
            .hasSupplierCategoryMappingStatus(MappingStatus.REJECTED)
            .doesNotHaveMappedCategoryId();
        assertThat(offerForMsg.getContentStatusActiveError().getErrorCode())
            .isEqualTo("mboc.error.category-mapping.offer-has-model-or-sku");
    }

    @Test
    public void setCategoryMappingWhenResetCategory() {
        Offer offer = OfferTestUtils.simpleOffer()
            .setCategoryIdForTests(CATEGORY_1, Offer.BindingKind.SUGGESTED)
            .setDataCampOffer(true);
        Supplier supplier = OfferTestUtils.simpleSupplier();
        Category category = null;
        var context = new CategoryMappingContext()
            .setSupplier(supplier)
            .setCategory(category);
        offerMappingActionService.PARTNER.setCategoryMapping(offer, context);
        OfferAssertions.assertThat(offer)
            .hasCategoryId(CATEGORY_1)
            .doesNotHaveSupplierCategoryId()
            .doesNotHaveMappedCategoryId();
    }

    public void setCategoryMappingWhenMovePskuCheck(boolean enableChangeCategory) {
        Offer offer = OfferTestUtils.simpleOffer()
            .setCategoryIdForTests(CATEGORY_1, Offer.BindingKind.SUGGESTED)
            .updateApprovedSkuMapping(Offer.Mapping.fromSku(ModelStorageCachingService.NEED_CONTENT_MODEL),
                Offer.MappingConfidence.PARTNER_SELF)
            .setDataCampOffer(true);
        Supplier supplier = OfferTestUtils.simpleSupplier().setNewContentPipeline(true);
        Category category = new Category().setCategoryId(CATEGORY_2)
            .setAcceptContentFromWhiteShops(true);
        var context = new CategoryMappingContext()
            .setSupplier(supplier)
            .setCategory(category);
        offerMappingActionService.PARTNER.setCategoryMapping(offer, context);
        if (enableChangeCategory) {
            OfferAssertions.assertThat(offer)
                .hasCategoryId(CATEGORY_2)
                .hasSupplierCategoryId(CATEGORY_2)
                .hasSupplierCategoryMappingStatus(MappingStatus.ACCEPTED)
                .hasMappedCategoryId(CATEGORY_2);
        } else {
            OfferAssertions.assertThat(offer)
                .hasCategoryId(CATEGORY_1)
                .hasSupplierCategoryId(CATEGORY_2)
                .hasSupplierCategoryMappingStatus(MappingStatus.NEW)
                .doesNotHaveMappedCategoryId();
        }
    }

    @Test
    public void setCategoryMappingWhenMovePsku() {
        setCategoryMappingWhenMovePskuCheck(false);
        storageKeyValueService.putValue(LegacyOfferMappingActionService.ENABLE_CHANGE_PSKU_CATEGORY_KEY, true);
        setCategoryMappingWhenMovePskuCheck(true);
    }

    @Test
    public void setCategoryMappingWhenSupplierCategoryIsTheSame() {
        Offer offer = OfferTestUtils.simpleOffer()
            .setCategoryIdForTests(CATEGORY_1, Offer.BindingKind.SUGGESTED)
            .setDataCampOffer(true);
        Supplier supplier = OfferTestUtils.simpleSupplier();
        Category category = new Category().setCategoryId(CATEGORY_1)
            .setAcceptContentFromWhiteShops(true);
        var context = new CategoryMappingContext()
            .setSupplier(supplier)
            .setCategory(category);
        offerMappingActionService.PARTNER.setCategoryMapping(offer, context);
        OfferAssertions.assertThat(offer)
            .hasCategoryId(CATEGORY_1)
            .hasSupplierCategoryId(CATEGORY_1)
            .hasSupplierCategoryMappingStatus(MappingStatus.ACCEPTED)
            .hasMappedCategoryId(CATEGORY_1);
    }

    @Test
    public void removeCategoryRestrictionErrorAfterAcceptSupplierMapping() {
        Offer offer = OfferTestUtils.simpleOffer()
            .setCategoryIdForTests(CATEGORY_1, Offer.BindingKind.SUGGESTED)
            .setDataCampOffer(true);
        offer.setContentStatusActiveError(MbocErrors.get().categoryRestrictedToSingle("1", 1));
        Supplier supplier = OfferTestUtils.simpleSupplier();
        Category category = new Category().setCategoryId(CATEGORY_1)
            .setAcceptContentFromWhiteShops(true);
        var context = new CategoryMappingContext()
            .setSupplier(supplier)
            .setCategory(category);
        offerMappingActionService.PARTNER.setCategoryMapping(offer, context);
        OfferAssertions.assertThat(offer)
            .hasCategoryId(CATEGORY_1)
            .hasSupplierCategoryMappingStatus(MappingStatus.ACCEPTED)
            .doesNotHaveContentStatusActiveError();
    }

    @Test
    public void skipNonCategoryRestrictionErrorAfterAcceptSupplierMapping() {
        Offer offer = OfferTestUtils.simpleOffer()
            .setCategoryIdForTests(CATEGORY_1, Offer.BindingKind.SUGGESTED)
            .setDataCampOffer(true);
        offer.setContentStatusActiveError(MbocErrors.get().contentProcessingFailed("1"));
        Supplier supplier = OfferTestUtils.simpleSupplier();
        Category category = new Category().setCategoryId(CATEGORY_1)
            .setAcceptContentFromWhiteShops(true);
        var context = new CategoryMappingContext()
            .setSupplier(supplier)
            .setCategory(category);
        offerMappingActionService.PARTNER.setCategoryMapping(offer, context);
        OfferAssertions.assertThat(offer)
            .hasCategoryId(CATEGORY_1)
            .hasSupplierCategoryMappingStatus(MappingStatus.ACCEPTED)
            .hasContentStatusActiveError();
    }

    @Test
    public void doNotChangeCategoryIfApprovedAndIsTheSame() {
        Offer offer = OfferTestUtils.simpleOffer()
            .setCategoryIdForTests(CATEGORY_1, Offer.BindingKind.APPROVED)
            .setDataCampOffer(true);
        Supplier supplier = OfferTestUtils.simpleSupplier();
        Category category = new Category().setCategoryId(CATEGORY_1)
            .setAcceptContentFromWhiteShops(true);
        var context = new CategoryMappingContext()
            .setSupplier(supplier)
            .setCategory(category);
        offerMappingActionService.PARTNER.setCategoryMapping(offer, context);
        OfferAssertions.assertThat(offer)
            .hasCategoryId(CATEGORY_1)
            .hasBindingKind(Offer.BindingKind.APPROVED);
    }


    @Test
    public void doChangeCategoryIfApprovedAndIsNotTheSame() {
        Offer offer = OfferTestUtils.simpleOffer()
            .setCategoryIdForTests(CATEGORY_1, Offer.BindingKind.APPROVED)
            .setDataCampOffer(true);
        Supplier supplier = OfferTestUtils.simpleSupplier();
        Category category = new Category().setCategoryId(CATEGORY_2)
            .setAcceptContentFromWhiteShops(true);
        var context = new CategoryMappingContext()
            .setSupplier(supplier)
            .setCategory(category);
        offerMappingActionService.PARTNER.setCategoryMapping(offer, context);
        OfferAssertions.assertThat(offer)
            .hasCategoryId(CATEGORY_2)
            .hasBindingKind(Offer.BindingKind.SUPPLIER);
    }

    @Test
    public void setCategoryMappingCantCalculateRestriction() {
        // когда не можем рассчитать, принимаем категорию, с расчетом на то, что потом он поедет на ручную
        // классификацию и там мы примем окончательное решение
        Mockito.when(categoryRestrictionCalculator.calculateRestriction(any())).thenReturn(Optional.empty());
        Offer offer = OfferTestUtils.simpleOffer().setCategoryIdForTests(CATEGORY_1, Offer.BindingKind.SUGGESTED);
        Supplier supplier = OfferTestUtils.simpleSupplier();
        Category category = new Category().setCategoryId(CATEGORY_2);
        var context = new CategoryMappingContext()
            .setSupplier(supplier)
            .setCategory(category);
        offerMappingActionServiceEnabledHm.PARTNER.setCategoryMapping(offer, context);
        OfferAssertions.assertThat(offer)
            .hasCategoryId(CATEGORY_2)
            .hasSupplierCategoryId(CATEGORY_2)
            .hasSupplierCategoryMappingStatus(MappingStatus.ACCEPTED)
            .hasMappedCategoryId(CATEGORY_2)
            .doesNotHaveContentStatusActiveError();
    }

    @Test
    public void setCategoryMappingSingleRestriction() {
        Offer offer = OfferTestUtils.simpleOffer().setCategoryIdForTests(CATEGORY_1, Offer.BindingKind.SUGGESTED);
        Offer offerToOk = offer.copy();
        Supplier supplier = OfferTestUtils.simpleSupplier();
        Category category = new Category().setCategoryId(CATEGORY_2);

        // case 1: when restriction disagrees
        Mockito.when(categoryRestrictionCalculator.calculateRestriction(any()))
            .thenReturn(Optional.of(new SingleCategoryRestriction(CATEGORY_1)));
        var context = new CategoryMappingContext()
            .setSupplier(supplier)
            .setCategory(category);
        offerMappingActionServiceEnabledHm.PARTNER.setCategoryMapping(offer, context);
        OfferAssertions.assertThat(offer)
            .hasCategoryId(CATEGORY_1)
            .hasSupplierCategoryId(CATEGORY_2)
            .hasSupplierCategoryMappingStatus(MappingStatus.REJECTED)
            .doesNotHaveMappedCategoryId();
        assertThat(offer.getContentStatusActiveError().getErrorCode())
            .isEqualTo("mboc.error.category-mapping.category-restricted-to-single");
        String expectedErrorStr = String.format("Для оффера '%s' разрешена только категория '%d'.",
            offer.getShopSku(), 1);
        assertThat(offer.getContentStatusActiveError().render())
            .isEqualTo(expectedErrorStr);

        // case 2: when restriction agrees
        Mockito.when(categoryRestrictionCalculator.calculateRestriction(any()))
            .thenReturn(Optional.of(new SingleCategoryRestriction(CATEGORY_2)));
        context = new CategoryMappingContext()
            .setSupplier(supplier)
            .setCategory(category);
        offerMappingActionServiceEnabledHm.PARTNER.setCategoryMapping(offerToOk, context);
        OfferAssertions.assertThat(offerToOk)
            .hasCategoryId(CATEGORY_2)
            .hasSupplierCategoryId(CATEGORY_2)
            .hasSupplierCategoryMappingStatus(MappingStatus.ACCEPTED)
            .hasMappedCategoryId(CATEGORY_2);
    }

    @Test
    public void setCategoryMappingGroupRestriction() {
        long groupId = 33;
        var legacyOfferMappingActionServiceLocal = new LegacyOfferMappingActionService(needContentStatusService,
            categoryRestrictionCalculator, offerDestinationCalculator, new StorageKeyValueServiceMock());
        var offerMappingActionServiceLocal = new OfferMappingActionService(legacyOfferMappingActionServiceLocal);
        Offer offer = OfferTestUtils.simpleOffer().setCategoryIdForTests(CATEGORY_1, Offer.BindingKind.SUGGESTED);
        Offer offerToOk = offer.copy();
        Supplier supplier = OfferTestUtils.simpleSupplier();
        Category category = new Category().setCategoryId(CATEGORY_2);

        // case 1: when restriction disagrees
        Mockito.when(categoryRestrictionCalculator.calculateRestriction(any()))
            .thenReturn(Optional.of(new GroupCategoryRestriction(groupId, Set.of(CATEGORY_1))));
        var context = new CategoryMappingContext()
            .setSupplier(supplier)
            .setCategory(category);
        offerMappingActionServiceLocal.PARTNER.setCategoryMapping(offer, context);
        OfferAssertions.assertThat(offer)
            .hasCategoryId(CATEGORY_1)
            .hasSupplierCategoryId(CATEGORY_2)
            .hasSupplierCategoryMappingStatus(MappingStatus.REJECTED)
            .doesNotHaveMappedCategoryId();
        assertThat(offer.getContentStatusActiveError().getErrorCode())
            .isEqualTo("mboc.error.category-mapping.category-restricted-to-group");
        String expectedErrorStr = String.format(
            "Для оффера '%s' разрешены только категории из группы '%d'.",
            offer.getShopSku(), groupId);
        assertThat(offer.getContentStatusActiveError().render())
            .isEqualTo(expectedErrorStr);

        // case 2: when restriction agrees
        Mockito.when(categoryRestrictionCalculator.calculateRestriction(any()))
            .thenReturn(Optional.of(new GroupCategoryRestriction(groupId, Set.of(CATEGORY_2))));
        context = new CategoryMappingContext()
            .setSupplier(supplier)
            .setCategory(category);
        offerMappingActionServiceLocal.PARTNER.setCategoryMapping(offerToOk, context);
        OfferAssertions.assertThat(offerToOk)
            .hasCategoryId(CATEGORY_2)
            .hasSupplierCategoryId(CATEGORY_2)
            .hasSupplierCategoryMappingStatus(MappingStatus.ACCEPTED)
            .hasMappedCategoryId(CATEGORY_2);
    }

    @Test
    public void setCategoryMappingAnyAllowedRestriction() {
        var legacyOfferMappingActionServiceLocal = new LegacyOfferMappingActionService(needContentStatusService,
            categoryRestrictionCalculator, offerDestinationCalculator, new StorageKeyValueServiceMock());
        var offerMappingActionServiceLocal = new OfferMappingActionService(legacyOfferMappingActionServiceLocal);
        Mockito.when(categoryRestrictionCalculator.calculateRestriction(any()))
            .thenReturn(Optional.of(new EmptyCategoryRestriction()));
        Offer offer = OfferTestUtils.simpleOffer().setCategoryIdForTests(CATEGORY_1, Offer.BindingKind.SUGGESTED);
        Supplier supplier = OfferTestUtils.simpleSupplier();
        Category category = new Category().setCategoryId(CATEGORY_2);
        var context = new CategoryMappingContext()
            .setSupplier(supplier)
            .setCategory(category);
        offerMappingActionServiceLocal.PARTNER.setCategoryMapping(offer, context);
        OfferAssertions.assertThat(offer)
            .hasCategoryId(CATEGORY_2)
            .hasSupplierCategoryId(CATEGORY_2)
            .hasSupplierCategoryMappingStatus(MappingStatus.ACCEPTED)
            .hasMappedCategoryId(CATEGORY_2);
    }

    @Test
    public void dontSetReSortForFastSkuMapping() {
        var offerFsku = OfferTestUtils.nextOffer(OfferTestUtils.businessSupplier())
            .setApprovedSkuMappingInternal(new Offer.Mapping(1, LocalDateTime.now(), Offer.SkuType.FAST_SKU))
            .approve(Offer.MappingType.APPROVED, Offer.MappingConfidence.PARTNER_FAST);

        var mappingContext = new SupplierSkuMappingContext()
            .setSupplier(OfferTestUtils.blueSupplierUnderBiz1())
            .setSkuMappingContext(new SkuMappingContext()
                .setSkuMapping(new Offer.Mapping(3, LocalDateTime.now(), Offer.SkuType.PARTNER20))
                .setCategoryId(CATEGORY_2));
        offerMappingActionService.PARTNER.setSkuMapping(offerFsku, mappingContext);

        assertThat(offerFsku.getSupplierSkuMappingStatus()).isEqualTo(MappingStatus.NEW);
    }

    @Test
    public void setReSortForPSkuMapping() {
        var offerPsku = OfferTestUtils.nextOffer(OfferTestUtils.businessSupplier())
            .setApprovedSkuMappingInternal(new Offer.Mapping(2, LocalDateTime.now(), Offer.SkuType.PARTNER20))
            .approve(Offer.MappingType.APPROVED, Offer.MappingConfidence.CONTENT);

        var mappingContext = new SupplierSkuMappingContext()
            .setSupplier(OfferTestUtils.blueSupplierUnderBiz1())
            .setSkuMappingContext(new SkuMappingContext()
                .setSkuMapping(new Offer.Mapping(4, LocalDateTime.now(), Offer.SkuType.PARTNER20))
                .setCategoryId(CATEGORY_2));

        offerMappingActionService.PARTNER.setSkuMapping(offerPsku, mappingContext);

        assertThat(offerPsku.getSupplierSkuMappingStatus()).isEqualTo(MappingStatus.RE_SORT);
    }

    @Test
    public void approvedSkuMappingCannotOverrideCategoryWithHigherConfidence() {
        var offer = OfferTestUtils.simpleOffer()
            .setCategoryIdForTests(CATEGORY_1, Offer.BindingKind.APPROVED)
            .setMappedCategoryId(CATEGORY_1)
            .setMappedCategoryConfidence(Offer.MappingConfidence.CONTENT);

        var mappingContext = new SkuMappingContext()
            .setSkuMapping(new Offer.Mapping(1, LocalDateTime.now(), Offer.SkuType.PARTNER20))
            .setCategoryId(CATEGORY_2);
        offerMappingActionService.GUTGIN.setSkuMapping(offer, mappingContext);

        MbocAssertions.assertThat(offer)
            .hasApprovedMapping(1)
            .hasApprovedSkuMappingConfidence(Offer.MappingConfidence.PARTNER_SELF)
            .hasCategoryId(CATEGORY_1)
            .hasMappedCategoryId(CATEGORY_1)
            .hasMappedCategoryConfidence(Offer.MappingConfidence.CONTENT);
    }

    @Test
    public void approvedSkuMappingOverridesCategoryWithLowerConfidence() {
        var offer = OfferTestUtils.simpleOffer()
            .setCategoryIdForTests(CATEGORY_1, Offer.BindingKind.APPROVED)
            .setMappedCategoryId(CATEGORY_1)
            .setMappedCategoryConfidence(Offer.MappingConfidence.PARTNER);

        var mappingContext = new SkuMappingContext()
            .setSkuMapping(new Offer.Mapping(1, LocalDateTime.now(), Offer.SkuType.MARKET))
            .setCategoryId(CATEGORY_2);
        offerMappingActionService.CONTENT.setSkuMapping(offer, mappingContext);

        MbocAssertions.assertThat(offer)
            .hasApprovedMapping(1)
            .hasApprovedSkuMappingConfidence(Offer.MappingConfidence.CONTENT)
            .hasCategoryId(CATEGORY_2)
            .hasMappedCategoryId(CATEGORY_1)
            .hasMappedCategoryConfidence(Offer.MappingConfidence.PARTNER);
    }

    @Test
    public void approvedSkuMappingOverridesCategoryWithSameConfidence() {
        var offer = OfferTestUtils.simpleOffer()
            .setCategoryIdForTests(CATEGORY_1, Offer.BindingKind.APPROVED)
            .setMappedCategoryId(CATEGORY_1)
            .setMappedCategoryConfidence(Offer.MappingConfidence.PARTNER_SELF);

        var mappingContext = new SkuMappingContext()
            .setSkuMapping(new Offer.Mapping(1, LocalDateTime.now(), Offer.SkuType.PARTNER20))
            .setCategoryId(CATEGORY_2);
        offerMappingActionService.GUTGIN.setSkuMapping(offer, mappingContext);

        MbocAssertions.assertThat(offer)
            .hasApprovedMapping(1)
            .hasApprovedSkuMappingConfidence(Offer.MappingConfidence.PARTNER_SELF)
            .hasCategoryId(CATEGORY_2)
            .hasMappedCategoryId(CATEGORY_1)
            .hasMappedCategoryConfidence(Offer.MappingConfidence.PARTNER_SELF);
    }

    @Test
    public void approvedSkuMappingCannotOverrideModelWithHigherConfidence() {
        var offer = OfferTestUtils.simpleOffer()
            .setModelId(1L)
            .setMappedModelId(1L)
            .setMappedModelConfidence(Offer.MappingConfidence.CONTENT);

        var mappingContext = new SkuMappingContext()
            .setSkuMapping(new Offer.Mapping(1, LocalDateTime.now(), Offer.SkuType.PARTNER20))
            .setModelId(2L);
        offerMappingActionService.GUTGIN.setSkuMapping(offer, mappingContext);

        MbocAssertions.assertThat(offer)
            .hasApprovedMapping(1)
            .hasApprovedSkuMappingConfidence(Offer.MappingConfidence.PARTNER_SELF)
            .hasModelId(1)
            .hasMappedModelId(1)
            .hasMappedModelConfidence(Offer.MappingConfidence.CONTENT);
    }

    @Test
    public void approvedSkuMappingOverridesModelWithLowerConfidence() {
        var offer = OfferTestUtils.simpleOffer()
            .setModelId(1L)
            .setMappedModelId(1L)
            .setMappedModelConfidence(Offer.MappingConfidence.PARTNER);

        var mappingContext = new SkuMappingContext()
            .setSkuMapping(new Offer.Mapping(1, LocalDateTime.now(), Offer.SkuType.MARKET))
            .setModelId(2L);
        offerMappingActionService.CONTENT.setSkuMapping(offer, mappingContext);

        MbocAssertions.assertThat(offer)
            .hasApprovedMapping(1)
            .hasApprovedSkuMappingConfidence(Offer.MappingConfidence.CONTENT)
            .hasModelId(2L)
            .hasMappedModelId(1L)
            .hasMappedModelConfidence(Offer.MappingConfidence.PARTNER);
    }

    @Test
    public void approvedSkuMappingOverridesModelWithSameConfidence() {
        var offer = OfferTestUtils.simpleOffer()
            .setModelId(1L)
            .setMappedModelId(1L)
            .setMappedModelConfidence(Offer.MappingConfidence.PARTNER_SELF);

        var mappingContext = new SkuMappingContext()
            .setSkuMapping(new Offer.Mapping(1, LocalDateTime.now(), Offer.SkuType.PARTNER20))
            .setModelId(2L);
        offerMappingActionService.GUTGIN.setSkuMapping(offer, mappingContext);

        MbocAssertions.assertThat(offer)
            .hasApprovedMapping(1)
            .hasApprovedSkuMappingConfidence(Offer.MappingConfidence.PARTNER_SELF)
            .hasModelId(2L)
            .hasMappedModelId(1L)
            .hasMappedModelConfidence(Offer.MappingConfidence.PARTNER_SELF);
    }

    @Test
    public void contentStatusActiveErrorRemovedWhenMappingIsSet() {
        var offer = OfferTestUtils.simpleOffer()
            .setContentStatusActiveError(MbocErrors.get().contentProcessingFailed(OfferTestUtils.DEFAULT_SHOP_SKU))
            .updateApprovedSkuMapping(null, null);

        var mappingContext = new SkuMappingContext();
        mappingContext.setSkuMapping(OfferTestUtils.mapping(1));
        offerMappingActionService.CONTENT.setSkuMapping(offer, mappingContext);

        MbocAssertions.assertThat(offer)
            .hasApprovedMapping(1)
            .hasApprovedSkuMappingConfidence(Offer.MappingConfidence.CONTENT)
            .doesNotHaveContentStatusActiveError();
    }

    @Test
    public void contentStatusActiveErrorRemovedWhenMappingUpdated() {
        var origMapping = OfferTestUtils.mapping(1);
        var newMapping = OfferTestUtils.mapping(2);

        var offer = OfferTestUtils.simpleOffer()
            .setContentStatusActiveError(MbocErrors.get().contentProcessingFailed(OfferTestUtils.DEFAULT_SHOP_SKU))
            .updateApprovedSkuMapping(origMapping, Offer.MappingConfidence.CONTENT);

        var mappingContext = new SkuMappingContext();
        mappingContext.setSkuMapping(newMapping);
        offerMappingActionService.CONTENT.setSkuMapping(offer, mappingContext);

        MbocAssertions.assertThat(offer)
            .hasApprovedMapping(newMapping.getMappingId())
            .hasApprovedSkuMappingConfidence(Offer.MappingConfidence.CONTENT)
            .doesNotHaveContentStatusActiveError();
    }

    @Test
    public void contentStatusActiveErrorIsNotRemovedWhenMappingIsNotChanged() {
        var mapping = OfferTestUtils.mapping(1, Offer.SkuType.FAST_SKU);
        var sameMapping = OfferTestUtils.mapping(1, Offer.SkuType.PARTNER20);

        var contentProcessingError = MbocErrors.get().contentProcessingFailed(OfferTestUtils.DEFAULT_SHOP_SKU);

        var offer = OfferTestUtils.simpleOffer()
            .updateApprovedSkuMapping(mapping, Offer.MappingConfidence.PARTNER_FAST)
            .setContentStatusActiveError(contentProcessingError);

        offerMappingActionService.GUTGIN.setSkuMapping(offer,
            new SkuMappingContext().setSkuMapping(sameMapping)
        );

        MbocAssertions.assertThat(offer)
            .hasApprovedMapping(sameMapping.getMappingId())
            .hasApprovedSkuMappingConfidence(Offer.MappingConfidence.PARTNER_SELF)
            .hasContentStatusActiveError(contentProcessingError);
    }

    @Test
    public void testUpdateApprovedConfidenceAfterConvertFastSkuToPsku() {
        var mapping = OfferTestUtils.mapping(1, Offer.SkuType.FAST_SKU);

        Offer offer = OfferTestUtils.simpleOffer()
            .updateApprovedSkuMapping(mapping, Offer.MappingConfidence.PARTNER_FAST)
            .setDataCampOffer(true);

        var sku = new Model().setSkuModel(true).setId(1)
            .setModelQuality(SimpleModel.ModelQuality.PARTNER).setModelType(SimpleModel.ModelType.SKU);
        offerMappingActionService.APPROVED.onMboSkuUpdate(offer, sku);

        MbocAssertions.assertThat(offer)
            .hasApprovedSkuMappingConfidence(Offer.MappingConfidence.PARTNER_SELF)
            .hasApprovedMapping(OfferTestUtils.mapping(mapping.getMappingId(), Offer.SkuType.PARTNER20));
    }

    @Test
    public void testResetApproveSkuMapping() {
        Offer offer = OfferTestUtils.simpleOffer()
            .setCategoryIdForTests(CATEGORY_1, Offer.BindingKind.APPROVED)
            .updateApprovedSkuMapping(OfferTestUtils.mapping(1), Offer.MappingConfidence.CONTENT)
            .updateProcessingStatusIfValid(Offer.ProcessingStatus.IN_RECHECK_MODERATION);

        offerMappingActionService.APPROVED.resetSkuMapping(offer);

        MbocAssertions.assertThat(offer)
            .hasApprovedMapping(0L)
            .doesNotHaveApprovedMapping()
            .hasApprovedSkuMappingConfidence(Offer.MappingConfidence.RESET);
    }

    @Test
    public void setCategoryOnRecheckClassification() {
        Supplier supplier = OfferTestUtils.simpleSupplier().setNewContentPipeline(true);

        Offer offerForContentAction = OfferTestUtils.simpleOffer(supplier)
                .updateAcceptanceStatusForTests(Offer.AcceptanceStatus.OK)
                .setCategoryIdForTests(CATEGORY_1, Offer.BindingKind.APPROVED)
                .setAutomaticClassification(true)
                .setRecheckCategoryId(CATEGORY_1)
                .setProcessingStatusInternal(Offer.ProcessingStatus.IN_RECHECK_CLASSIFICATION)
                .setRecheckClassificationSource(Offer.RecheckClassificationSource.PARTNER)
                .setRecheckClassificationStatus(Offer.RecheckClassificationStatus.ON_RECHECK);
        Offer offerForPartnerAction = offerForContentAction.copy();

        Category categoryChanged = new Category().setCategoryId(CATEGORY_2)
                .setAcceptContentFromWhiteShops(true);
        Category categorySame = new Category().setCategoryId(CATEGORY_1)
                .setAcceptContentFromWhiteShops(true);

        var contextWithChangedCategory = new CategoryMappingContext()
                .setSupplier(supplier)
                .setCategory(categoryChanged);
        var contextWithSameCategory = new CategoryMappingContext()
                .setSupplier(supplier)
                .setCategory(categorySame);

        // same category
        offerMappingActionService.CONTENT.setCategoryMapping(offerForContentAction, contextWithSameCategory);
        offerMappingActionService.PARTNER.setCategoryMapping(offerForPartnerAction, contextWithSameCategory);

        // do nothing here. RecheckClassificationStatus.CONFIRMED will be calc in ClassificationOffersProcessingService
        // if it was initiated there
        assertThat(offerForContentAction.getRecheckClassificationStatus())
                .isEqualTo(Offer.RecheckClassificationStatus.ON_RECHECK);
        assertThat(offerForPartnerAction.getRecheckClassificationStatus())
                .isEqualTo(Offer.RecheckClassificationStatus.ON_RECHECK);

        // category is changing
        offerMappingActionService.CONTENT.setCategoryMapping(offerForContentAction, contextWithChangedCategory);
        offerMappingActionService.PARTNER.setCategoryMapping(offerForPartnerAction, contextWithChangedCategory);

        // do nothing here. RecheckClassificationStatus.CHANGED will be calc in ClassificationOffersProcessingService
        // if it was initiated there.
        assertThat(offerForContentAction.getRecheckClassificationStatus())
                .isEqualTo(Offer.RecheckClassificationStatus.ON_RECHECK);
        // reject recheck because of someone changed moderated category
        assertThat(offerForPartnerAction.getRecheckClassificationStatus())
                .isEqualTo(Offer.RecheckClassificationStatus.REJECTED);
    }
}

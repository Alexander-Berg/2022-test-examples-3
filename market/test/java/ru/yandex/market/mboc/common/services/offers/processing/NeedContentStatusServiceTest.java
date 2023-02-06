package ru.yandex.market.mboc.common.services.offers.processing;

import java.util.Collections;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.mboc.common.dict.Supplier;
import ru.yandex.market.mboc.common.dict.SupplierRepository;
import ru.yandex.market.mboc.common.dict.SupplierService;
import ru.yandex.market.mboc.common.offers.model.Offer;
import ru.yandex.market.mboc.common.services.books.BooksService;
import ru.yandex.market.mboc.common.services.category.CategoryCachingServiceMock;
import ru.yandex.market.mboc.common.services.category.models.Category;
import ru.yandex.market.mboc.common.utils.BaseDbTestClass;
import ru.yandex.market.mboc.common.utils.OfferTestUtils;

import static ru.yandex.market.mboc.common.services.offers.processing.NeedContentStatusService.isGoodContentOffer;

/**
 * @author s-ermakov
 */
@SuppressWarnings("checkstyle:magicNumber")
public class NeedContentStatusServiceTest extends BaseDbTestClass {

    private static final long CATEGORY_ID = 777;
    private static final int OLD_PIPELINE_SUPPLIER = 321;
    private static final int WHITE_SUPPLIER = 456;
    private static final int DSBS_SUPPLIER = 567;

    @Autowired
    private SupplierRepository supplierRepository;
    private SupplierService supplierService;
    private CategoryCachingServiceMock categoryCachingServiceMock;
    private NeedContentStatusService needContentStatusService;
    private Supplier supplier;
    private Supplier oldPipelineSupplier;
    private Supplier whiteSupplier;
    private Supplier dsbsSupplier;

    @Before
    public void setUp() throws Exception {
        categoryCachingServiceMock = new CategoryCachingServiceMock();
        supplierService = new SupplierService(supplierRepository);
        needContentStatusService = new NeedContentStatusService(categoryCachingServiceMock, supplierService,
            new BooksService(categoryCachingServiceMock, Collections.emptySet()));

        supplier = OfferTestUtils.simpleSupplier()
            .setNewContentPipeline(true);
        oldPipelineSupplier = OfferTestUtils.simpleSupplier()
            .setId(OLD_PIPELINE_SUPPLIER)
            .setNewContentPipeline(false);
        whiteSupplier = OfferTestUtils.whiteSupplier()
            .setId(WHITE_SUPPLIER)
            .setNewContentPipeline(false);
        dsbsSupplier = OfferTestUtils.dsbsSupplierUnderBiz()
            .setId(DSBS_SUPPLIER)
            .setNewContentPipeline(false);

        supplierRepository.insertBatch(
            supplier,
            oldPipelineSupplier,
            whiteSupplier
        );
    }

    @Test
    public void testIsContentNeeded() {
        Offer blueWithApprovedSku = OfferTestUtils.simpleOffer()
            .updateAcceptanceStatusForTests(Offer.AcceptanceStatus.OK)
            .updateApprovedSkuMapping(OfferTestUtils.mapping(1L), Offer.MappingConfidence.CONTENT);
        Offer blueWithSuggestSku = OfferTestUtils.simpleOffer()
            .updateAcceptanceStatusForTests(Offer.AcceptanceStatus.OK)
            .setSuggestSkuMapping(OfferTestUtils.mapping(1L));
        Offer blueWithModelId = OfferTestUtils.simpleOffer()
            .updateAcceptanceStatusForTests(Offer.AcceptanceStatus.OK)
            .setModelId(1000L);
        Offer blueWithCategory = OfferTestUtils.simpleOffer()
            .updateAcceptanceStatusForTests(Offer.AcceptanceStatus.OK)
            .setCategoryIdForTests(CATEGORY_ID, Offer.BindingKind.APPROVED);

        Offer whiteWithApprovedSku = OfferTestUtils.simpleOffer(whiteSupplier)
            .updateApprovedSkuMapping(OfferTestUtils.mapping(1L), Offer.MappingConfidence.CONTENT);
        Offer whiteWithSuggestSku = OfferTestUtils.simpleOffer(whiteSupplier)
            .setSuggestSkuMapping(OfferTestUtils.mapping(1L));
        Offer whiteWithModelId = OfferTestUtils.simpleOffer(whiteSupplier)
            .setModelId(1000L);
        Offer whiteWithCategory = OfferTestUtils.simpleOffer(whiteSupplier)
            .setCategoryIdForTests(CATEGORY_ID, Offer.BindingKind.APPROVED);
        Offer whiteWithSuggestSkuWithAntiMapping = OfferTestUtils.simpleOffer(whiteSupplier)
            .setSuggestSkuMapping(OfferTestUtils.mapping(1L));

        Offer dsbsWithApprovedSku = OfferTestUtils.simpleOffer(dsbsSupplier)
            .updateApprovedSkuMapping(OfferTestUtils.mapping(1L), Offer.MappingConfidence.CONTENT);
        Offer dsbsWithSuggestSku = OfferTestUtils.simpleOffer(dsbsSupplier)
            .setSuggestSkuMapping(OfferTestUtils.mapping(1L));
        Offer dsbsWithModelId = OfferTestUtils.simpleOffer(dsbsSupplier)
            .setModelId(1000L);
        Offer dsbsWithCategory = OfferTestUtils.simpleOffer(dsbsSupplier)
            .setCategoryIdForTests(CATEGORY_ID, Offer.BindingKind.APPROVED);
        Offer dsbsWithSuggestSkuWithAntiMapping = OfferTestUtils.simpleOffer(dsbsSupplier)
            .setSuggestSkuMapping(OfferTestUtils.mapping(1L));

        Assertions.assertThat(needContentStatusService.isContentNeeded(wrapOffer(blueWithApprovedSku))).isFalse();
        Assertions.assertThat(needContentStatusService.isContentNeeded(wrapOffer(blueWithSuggestSku))).isTrue();
        Assertions.assertThat(needContentStatusService.isContentNeeded(wrapOffer(blueWithModelId))).isTrue();
        Assertions.assertThat(needContentStatusService.isContentNeeded(wrapOffer(blueWithCategory))).isTrue();

        Assertions.assertThat(needContentStatusService.isContentNeeded(wrapOffer(dsbsWithApprovedSku))).isFalse();
        Assertions.assertThat(needContentStatusService.isContentNeeded(wrapOffer(dsbsWithSuggestSku))).isFalse();
        Assertions.assertThat(needContentStatusService.isContentNeeded(wrapOffer(dsbsWithModelId))).isTrue();
        Assertions.assertThat(needContentStatusService.isContentNeeded(wrapOffer(dsbsWithCategory))).isTrue();

        Assertions.assertThat(needContentStatusService.isContentNeeded(wrapOffer(whiteWithApprovedSku))).isFalse();
        Assertions.assertThat(needContentStatusService.isContentNeeded(wrapOffer(whiteWithSuggestSku))).isFalse();
        Assertions.assertThat(needContentStatusService.isContentNeeded(wrapOffer(whiteWithModelId))).isFalse();
        Assertions.assertThat(needContentStatusService.isContentNeeded(wrapOffer(whiteWithCategory))).isFalse();
        Assertions.assertThat(needContentStatusService.isContentNeeded(
            new OffersProcessingStatusService.OfferToProcess(whiteWithSuggestSkuWithAntiMapping, false, true, false))).isFalse();
    }

    @Test
    public void testIsGCNonDatacampPipeline() {
        categoryCachingServiceMock.addCategory(1L)
            .setCategoryAcceptGoodContent(1L, true);
        Category category = categoryCachingServiceMock.getCategory(1L).get();

        Offer offerSuiteGC = OfferTestUtils.simpleOffer();

        Assertions.assertThat(isGoodContentOffer(offerSuiteGC, supplier, category)).isTrue();
    }

    @Test
    public void testIsGCIsDatacampPipeline() {
        categoryCachingServiceMock.addCategory(1L)
            .setAcceptContentFromWhiteShops(1L, true);
        Category category = categoryCachingServiceMock.getCategory(1L).get();

        Offer offerSuiteGC = OfferTestUtils.simpleOffer().setDataCampOffer(true);

        Assertions.assertThat(isGoodContentOffer(offerSuiteGC, supplier, category)).isTrue();
    }

    @Test
    public void testNotGCOldSupplier() {
        categoryCachingServiceMock.addCategory(1L)
            .setAcceptContentFromWhiteShops(1L, true);
        Category category = categoryCachingServiceMock.getCategory(1L).get();

        Offer offerSuiteGC = OfferTestUtils.simpleOffer().setDataCampOffer(true);

        Assertions.assertThat(isGoodContentOffer(offerSuiteGC, oldPipelineSupplier, category)).isFalse();
    }

    @Test
    public void testForceGC() {
        categoryCachingServiceMock.addCategory(1L)
            .setAcceptContentFromWhiteShops(1L, false);
        categoryCachingServiceMock.setCategoryAcceptGoodContent(1L, false);
        categoryCachingServiceMock.setVendorExcluded(1L, 1);
        Category category = categoryCachingServiceMock.getCategory(1L).get();

        Offer forceGood = OfferTestUtils.simpleOffer()
            .setVendorId(1)
            .setForceGoodContentStatus(Offer.ForceGoodContentStatus.FORCE_GOOD_CONTENT);
        Offer forceNotGood = OfferTestUtils.simpleOffer()
            .setVendorId(1)
            .setForceGoodContentStatus(Offer.ForceGoodContentStatus.FORCE_NOT_GOOD_CONTENT);

        Assertions.assertThat(isGoodContentOffer(forceGood, oldPipelineSupplier, category)).isTrue();
        Assertions.assertThat(isGoodContentOffer(forceNotGood, oldPipelineSupplier, category)).isFalse();
    }

    @Test
    public void testExcludeVendorFrom() {
        categoryCachingServiceMock.addCategory(1L)
            .setAcceptContentFromWhiteShops(1L, true);
        categoryCachingServiceMock.setVendorExcluded(1L, 1);
        Category category = categoryCachingServiceMock.getCategory(1L).get();

        Offer offerSuiteGC = OfferTestUtils.simpleOffer().setDataCampOffer(true).setVendorId(1);

        Assertions.assertThat(isGoodContentOffer(offerSuiteGC, supplier, category)).isFalse();
    }

    private OffersProcessingStatusService.OfferToProcess wrapOffer(Offer offer) {
        return new OffersProcessingStatusService.OfferToProcess(
            offer, false, false, false);
    }

}

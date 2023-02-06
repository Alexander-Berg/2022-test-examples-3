package ru.yandex.market.mboc.tms.executors;

import java.util.Collections;

import com.google.common.collect.ImmutableMap;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.mboc.common.assertions.MbocAssertions;
import ru.yandex.market.mboc.common.availability.msku.MskuRepository;
import ru.yandex.market.mboc.common.db.jooq.generated.mbo_category.tables.pojos.Msku;
import ru.yandex.market.mboc.common.dict.SupplierRepository;
import ru.yandex.market.mboc.common.dict.SupplierService;
import ru.yandex.market.mboc.common.honestmark.OfferCategoryRestrictionCalculator;
import ru.yandex.market.mboc.common.infrastructure.sql.TransactionHelper;
import ru.yandex.market.mboc.common.msku.TestUtils;
import ru.yandex.market.mboc.common.offers.model.Offer;
import ru.yandex.market.mboc.common.offers.repository.OfferRepository;
import ru.yandex.market.mboc.common.services.books.BooksService;
import ru.yandex.market.mboc.common.services.category.CategoryCachingServiceMock;
import ru.yandex.market.mboc.common.services.modelstorage.ModelStorageCachingService;
import ru.yandex.market.mboc.common.services.modelstorage.models.Model;
import ru.yandex.market.mboc.common.services.offers.mapping.LegacyOfferMappingActionService;
import ru.yandex.market.mboc.common.services.offers.mapping.OfferMappingActionService;
import ru.yandex.market.mboc.common.services.offers.processing.NeedContentStatusService;
import ru.yandex.market.mboc.common.utils.BaseDbTestClass;
import ru.yandex.market.mboc.common.utils.DateTimeUtils;
import ru.yandex.market.mboc.common.utils.OfferTestUtils;

import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class UpdateOfferFromApprovedMappingExecutorTest extends BaseDbTestClass {

    private static final String SKU_NAME1 = OfferTestUtils.MAPPING_NAME_PREFIX + "1";
    private static final String SKU_NAME2 = OfferTestUtils.MAPPING_NAME_PREFIX + "2";
    private static final Model SKU_1 = new Model()
        .setId(1L).setTitle(SKU_NAME1)
        .setVendorId(1).setCategoryId(1)
        .setModelType(Model.ModelType.SKU);
    private static final Model SKU_2_PARTNER = new Model()
        .setId(2L).setTitle(SKU_NAME2)
        .setVendorId(1).setCategoryId(1)
        .setModelType(Model.ModelType.PARTNER_SKU);
    private ModelStorageCachingService modelStorageCachingService;
    @Autowired
    private OfferRepository autowiredOfferRepository;
    private OfferRepository offerRepository;

    private UpdateOfferFromApprovedMappingExecutor updateOfferFromApprovedMappingExecutor;

    @Autowired
    private SupplierRepository supplierRepository;
    @Autowired
    private MskuRepository mskuRepository;

    private SupplierService supplierService;

    private CategoryCachingServiceMock categoryCachingServiceMock;
    private NeedContentStatusService needContentStatusService;
    private OfferMappingActionService offerMappingActionService;

    @Before
    public void setup() {
        modelStorageCachingService = mock(ModelStorageCachingService.class);
        offerRepository = spy(autowiredOfferRepository);

        supplierRepository.insert(OfferTestUtils.simpleSupplier());

        categoryCachingServiceMock = new CategoryCachingServiceMock();
        supplierService = new SupplierService(supplierRepository);
        needContentStatusService = new NeedContentStatusService(categoryCachingServiceMock, supplierService,
            new BooksService(categoryCachingServiceMock, Collections.emptySet()));
        var legacyOfferMappingActionService = spy(new LegacyOfferMappingActionService(needContentStatusService,
            mock(OfferCategoryRestrictionCalculator.class), offerDestinationCalculator, storageKeyValueService));
        offerMappingActionService = new OfferMappingActionService(legacyOfferMappingActionService);

        updateOfferFromApprovedMappingExecutor = new UpdateOfferFromApprovedMappingExecutor(
            offerRepository, TransactionHelper.MOCK, storageKeyValueService, modelStorageCachingService,
            offerMappingActionService, mskuRepository
        );

        when(modelStorageCachingService.getModelsFromPgOnly(anyCollection()))
            .thenReturn(ImmutableMap.of(
                1L, SKU_1,
                2L, SKU_2_PARTNER
            ));

        storageKeyValueService.invalidateCache(); // Not an issue at runtime
    }

    @Test
    public void shouldNotUpdateOffersIfNotChanged() {
        offerRepository.insertOffer(OfferTestUtils.simpleOffer()
            .setVendorId(1)
            .setCategoryIdForTests(1L, Offer.BindingKind.SUGGESTED)
            .updateApprovedSkuMapping(OfferTestUtils.mapping(1L).copyWithSkuType(Offer.SkuType.MARKET),
                Offer.MappingConfidence.CONTENT));

        Msku sku = TestUtils.newMsku(1L).setTitle(SKU_NAME1);
        mskuRepository.save(sku);

        updateOfferFromApprovedMappingExecutor.execute();

        verify(offerRepository, times(0))
            .updateOffers(anyCollection());
    }

    @Test
    public void shouldUpdateOffersIfChanged() {
        offerRepository.insertOffer(OfferTestUtils.nextOffer()
            .setVendorId(2)
            .setCategoryIdForTests(1L, Offer.BindingKind.SUGGESTED)
            .updateApprovedSkuMapping(OfferTestUtils.mapping(1L), Offer.MappingConfidence.CONTENT));
        offerRepository.insertOffer(OfferTestUtils.nextOffer()
            .setVendorId(1)
            .setCategoryIdForTests(2L, Offer.BindingKind.SUGGESTED)
            .updateApprovedSkuMapping(OfferTestUtils.mapping(1L), Offer.MappingConfidence.CONTENT));
        offerRepository.insertOffer(OfferTestUtils.nextOffer()
            .setVendorId(2)
            .setCategoryIdForTests(2L, Offer.BindingKind.SUGGESTED)
            .updateApprovedSkuMapping(new Offer.Mapping(1L, DateTimeUtils.dateTimeNow()),
                Offer.MappingConfidence.CONTENT));

        updateOfferFromApprovedMappingExecutor.execute();

        offerRepository.findAll().forEach(offer ->
            MbocAssertions.assertThat(offer)
                .hasVendorId(1)
                .hasCategoryId(1)
                .hasApprovedMapping(Offer.Mapping.fromSku(SKU_1))
                .hasApprovedSkuMappingConfidence(Offer.MappingConfidence.CONTENT));
    }

    @Test
    public void shouldUpdateSkuTypeFromModel() {
        offerRepository.insertOffer(OfferTestUtils.simpleOffer()
            .setVendorId(1)
            .setCategoryIdForTests(1L, Offer.BindingKind.SUGGESTED)
            .updateApprovedSkuMapping(OfferTestUtils.mapping(2L, "sku name"),
                Offer.MappingConfidence.CONTENT));

        updateOfferFromApprovedMappingExecutor.execute();

        offerRepository.findAll().forEach(offer ->
            MbocAssertions.assertThat(offer).hasApprovedMapping(Offer.Mapping.fromSku(SKU_2_PARTNER))
                .hasApprovedSkuMappingConfidence(Offer.MappingConfidence.CONTENT));
    }

    @Test
    public void shouldIgnoreSameUpdateUpdateSkuTypeFromModel() {
        offerRepository.insertOffer(OfferTestUtils.simpleOffer()
            .setVendorId(1)
            .setCategoryIdForTests(1L, Offer.BindingKind.SUGGESTED)
            .updateApprovedSkuMapping(OfferTestUtils.mapping(2L, "sku name"),
                Offer.MappingConfidence.CONTENT));

        Msku sku = TestUtils.newMsku(2L).setTitle(SKU_NAME2);
        mskuRepository.save(sku);


        updateOfferFromApprovedMappingExecutor.execute();

        offerRepository.findAll().get(0).updateApprovedSkuMapping(OfferTestUtils.mapping(2L, "sku name"),
            Offer.MappingConfidence.CONTENT);

        updateOfferFromApprovedMappingExecutor.execute();

        offerRepository.findAll().forEach(offer ->
            MbocAssertions.assertThat(offer).hasApprovedMapping(Offer.Mapping.fromSku(SKU_2_PARTNER))
                .hasApprovedSkuMappingConfidence(Offer.MappingConfidence.CONTENT));
    }
}

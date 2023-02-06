package ru.yandex.market.mboc.mappings.logbroker;

import java.util.Collections;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import ru.yandex.market.mbo.http.ModelStorage;
import ru.yandex.market.mbo.http.ModelStorage.ModelTransition.ModelType;
import ru.yandex.market.mboc.common.dict.MbocSupplierType;
import ru.yandex.market.mboc.common.dict.Supplier;
import ru.yandex.market.mboc.common.dict.SupplierRepositoryMock;
import ru.yandex.market.mboc.common.dict.SupplierService;
import ru.yandex.market.mboc.common.honestmark.OfferCategoryRestrictionCalculator;
import ru.yandex.market.mboc.common.offers.DefaultOfferDestinationCalculator;
import ru.yandex.market.mboc.common.offers.model.BusinessSkuKey;
import ru.yandex.market.mboc.common.offers.model.Offer;
import ru.yandex.market.mboc.common.offers.repository.OfferBatchProcessor;
import ru.yandex.market.mboc.common.offers.repository.OfferRepositoryMock;
import ru.yandex.market.mboc.common.offers.repository.search.OffersFilter;
import ru.yandex.market.mboc.common.offers.settings.ApplySettingsService;
import ru.yandex.market.mboc.common.services.books.BooksService;
import ru.yandex.market.mboc.common.services.category.CategoryCachingServiceMock;
import ru.yandex.market.mboc.common.services.offers.mapping.LegacyOfferMappingActionService;
import ru.yandex.market.mboc.common.services.offers.mapping.OfferMappingActionService;
import ru.yandex.market.mboc.common.services.offers.processing.NeedContentStatusService;
import ru.yandex.market.mboc.common.services.storage.StorageKeyValueServiceMock;
import ru.yandex.market.mboc.common.utils.DateTimeUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;

public class ModelTransitionsLogbrokerMessageHandlerTest {
    private static final int SHOP_ID = 123;
    private static final String SHOP_SKU = "SKU123";

    private static final Long OLD_MODEL = 42L;
    private static final Long OLD_SKU = 22L;

    private static final Long NEW_MODEL = 43L;
    private static final Long NEW_SKU = 23L;

    private OfferRepositoryMock offerRepository;
    private ModelTransitionsLogbrokerMessageHandler modelTranstionsHandler;
    private ModelTransitionsLogbrokerMessageHandler skuTranstionsHandler;
    private SupplierRepositoryMock supplierRepository;
    private CategoryCachingServiceMock categoryCachingServiceMock;
    private NeedContentStatusService needContentStatusService;
    private OfferMappingActionService offerMappingActionService;
    private SupplierService supplierService;


    @Before
    public void setup() {
        offerRepository = new OfferRepositoryMock();
        supplierRepository = new SupplierRepositoryMock();
        supplierService = new SupplierService(supplierRepository);
        supplierRepository.insert(new Supplier(SHOP_ID, "Shop 1").setType(MbocSupplierType.MARKET_SHOP));
        categoryCachingServiceMock = new CategoryCachingServiceMock();
        needContentStatusService = new NeedContentStatusService(categoryCachingServiceMock, supplierService,
            new BooksService(categoryCachingServiceMock, Collections.emptySet()));
        var legacyOfferMappingActionService = new LegacyOfferMappingActionService(needContentStatusService,
            Mockito.mock(OfferCategoryRestrictionCalculator.class), new DefaultOfferDestinationCalculator(),
            new StorageKeyValueServiceMock());
        offerMappingActionService = new OfferMappingActionService(legacyOfferMappingActionService);
        var applySettingsService = Mockito.mock(ApplySettingsService.class);


        var offerBatchProcessor = Mockito.mock(OfferBatchProcessor.class);
        Mockito.doAnswer(invocation -> {
            OffersFilter offersFilter = invocation.getArgument(0);
            OfferBatchProcessor.OffersProcessor offersProcessor = invocation.getArgument(2);
            var offers = offerRepository.findOffers(offersFilter);
            offersProcessor.processOffers(offers, offerRepository);
            return null;
        })
            .when(offerBatchProcessor)
            .processBatchesOnMaster(any(OffersFilter.class), anyInt(), any(OfferBatchProcessor.OffersProcessor.class));

        modelTranstionsHandler = new ModelTransitionsLogbrokerMessageHandler(
            offerBatchProcessor, ModelType.MODEL, offerMappingActionService, applySettingsService);
        skuTranstionsHandler = new ModelTransitionsLogbrokerMessageHandler(
            offerBatchProcessor, ModelType.SKU, offerMappingActionService, applySettingsService);
    }

    @Test
    public void testRegularUpdateOffer() {
        offerRepository.setOffers(Offer.builder()
            .businessId(SHOP_ID)
            .shopSku(SHOP_SKU)
            .mappingDestination(Offer.MappingDestination.WHITE)
            .approvedSkuMappingConfidence(Offer.MappingConfidence.CONTENT)
            .approvedSkuMapping(new Offer.Mapping(OLD_SKU, DateTimeUtils.dateTimeNow(), Offer.SkuType.MARKET))
            .modelId(OLD_MODEL)
            .build());

        modelTranstionsHandler.process(List.of(
            ModelStorage.ModelTransition.newBuilder()
                .setModelType(ModelType.MODEL)
                .setOldEntityId(OLD_MODEL)
                .setNewEntityId(NEW_MODEL)
                .build()
        ));

        skuTranstionsHandler.process(List.of(
            ModelStorage.ModelTransition.newBuilder()
                .setModelType(ModelType.SKU)
                .setOldEntityId(OLD_SKU)
                .setNewEntityId(NEW_SKU)
                .build()
        ));

        Offer offer = offerRepository.findOfferByBusinessSkuKey(new BusinessSkuKey(SHOP_ID, SHOP_SKU));
        assertThat(offer.getApprovedSkuMapping().getMappingId()).isEqualTo(NEW_SKU);
        assertThat(offer.getApprovedSkuMappingConfidence()).isEqualTo(Offer.MappingConfidence.CONTENT);
        assertThat(offer.getModelId()).isEqualTo(NEW_MODEL);
    }

    @Test
    public void testRegularUpdateOfferWithPskuMapping() {
        offerRepository.setOffers(Offer.builder()
            .businessId(SHOP_ID)
            .shopSku(SHOP_SKU)
            .mappingDestination(Offer.MappingDestination.BLUE)
            .approvedSkuMappingConfidence(Offer.MappingConfidence.PARTNER_SELF)
            .approvedSkuMapping(new Offer.Mapping(OLD_SKU, DateTimeUtils.dateTimeNow(), Offer.SkuType.PARTNER20))
            .modelId(OLD_MODEL)
            .build());

        modelTranstionsHandler.process(List.of(
            ModelStorage.ModelTransition.newBuilder()
                .setModelType(ModelType.MODEL)
                .setOldEntityId(OLD_MODEL)
                .setNewEntityId(NEW_MODEL)
                .build()
        ));

        skuTranstionsHandler.process(List.of(
            ModelStorage.ModelTransition.newBuilder()
                .setModelType(ModelType.SKU)
                .setOldEntityId(OLD_SKU)
                .setNewEntityId(NEW_SKU)
                .build()
        ));

        Offer offer = offerRepository.findOfferByBusinessSkuKey(new BusinessSkuKey(SHOP_ID, SHOP_SKU));
        assertThat(offer.getApprovedSkuMapping().getMappingId()).isEqualTo(NEW_SKU);
        // PARTNER_SELF is updated to CONTENT
        assertThat(offer.getApprovedSkuMappingConfidence()).isEqualTo(Offer.MappingConfidence.CONTENT);
        assertThat(offer.getModelId()).isEqualTo(NEW_MODEL);
    }

    @Test
    public void testIgnoreRevertTransitionType() {
        offerRepository.setOffers(Offer.builder()
            .businessId(SHOP_ID)
            .shopSku(SHOP_SKU)
            .mappingDestination(Offer.MappingDestination.WHITE)
            .approvedSkuMapping(new Offer.Mapping(OLD_SKU, DateTimeUtils.dateTimeNow(), null))
            .modelId(OLD_MODEL)
            .build());


        modelTranstionsHandler.process(List.of(
            ModelStorage.ModelTransition.newBuilder()
                .setType(ModelStorage.ModelTransition.TransitionType.REVERT)
                .setModelType(ModelType.MODEL)
                .setOldEntityId(OLD_MODEL)
                .build()
        ));

        skuTranstionsHandler.process(List.of(
            ModelStorage.ModelTransition.newBuilder()
                .setType(ModelStorage.ModelTransition.TransitionType.REVERT)
                .setModelType(ModelType.SKU)
                .setOldEntityId(OLD_SKU)
                .build()
        ));

        Offer offer = offerRepository.findOfferByBusinessSkuKey(new BusinessSkuKey(SHOP_ID, SHOP_SKU));
        assertThat(offer.hasApprovedSkuMapping()).isTrue();
    }

    @Test
    public void testDeleteMappingsFromUpdateOffer() {
        offerRepository.setOffers(Offer.builder()
            .businessId(SHOP_ID)
            .shopSku(SHOP_SKU)
            .mappingDestination(Offer.MappingDestination.WHITE)
            .approvedSkuMapping(new Offer.Mapping(OLD_SKU, DateTimeUtils.dateTimeNow(), null))
            .modelId(OLD_MODEL)
            .build());

        modelTranstionsHandler.process(List.of(
            ModelStorage.ModelTransition.newBuilder()
                .setModelType(ModelType.MODEL)
                .setOldEntityId(OLD_MODEL)
                .build()
        ));

        skuTranstionsHandler.process(List.of(
            ModelStorage.ModelTransition.newBuilder()
                .setModelType(ModelType.SKU)
                .setOldEntityId(OLD_SKU)
                .build()
        ));

        Offer offer = offerRepository.findOfferByBusinessSkuKey(new BusinessSkuKey(SHOP_ID, SHOP_SKU));
        assertThat(offer.getApprovedSkuMapping().getMappingId()).isEqualTo(0L);
        assertThat(offer.getModelId()).isEqualTo(0L);
    }
}

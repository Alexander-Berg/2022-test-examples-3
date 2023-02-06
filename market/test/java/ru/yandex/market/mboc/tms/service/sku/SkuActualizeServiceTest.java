package ru.yandex.market.mboc.tms.service.sku;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.ir.http.PskuPostProcessor;
import ru.yandex.market.ir.http.PskuPostProcessorService;
import ru.yandex.market.mbo.storage.StorageKeyValueService;
import ru.yandex.market.mboc.common.dict.Supplier;
import ru.yandex.market.mboc.common.dict.SupplierRepository;
import ru.yandex.market.mboc.common.offers.model.Offer;
import ru.yandex.market.mboc.common.offers.model.OfferContent;
import ru.yandex.market.mboc.common.offers.model.OfferMappingHistory;
import ru.yandex.market.mboc.common.offers.repository.OfferMappingHistoryRepository;
import ru.yandex.market.mboc.common.offers.repository.OfferRepository;
import ru.yandex.market.mboc.common.utils.BaseDbTestClass;

import static ru.yandex.market.mboc.tms.service.sku.SkuActualizeService.PREVIOUS_UPPER_BOUND_PROPERTY_NAME;
import static ru.yandex.market.mboc.tms.service.sku.SkuActualizeService.SKUS_PER_LAUNCH_PROPERTY_NAME;

public class SkuActualizeServiceTest extends BaseDbTestClass {

    @Autowired
    private OfferMappingHistoryRepository offerMappingHistoryRepository;
    @Autowired
    private StorageKeyValueService skv;
    @Autowired
    private OfferRepository offerRepository;
    @Autowired
    private SupplierRepository supplierRepository;

    private PskuPostProcessorService pskuPostProcessorService;
    private SkuActualizeService skuActualizeService;
    private ArgumentCaptor<PskuPostProcessor.CleanupDeletedOffersDataRequest> requestArgumentCaptor;

    @Before
    public void setUp() {
        pskuPostProcessorService = Mockito.mock(PskuPostProcessorService.class);
        requestArgumentCaptor = ArgumentCaptor.forClass(PskuPostProcessor.CleanupDeletedOffersDataRequest.class);
        generateOkPPPService();
        skuActualizeService = new SkuActualizeService(offerMappingHistoryRepository, pskuPostProcessorService, skv);
        offerRepository.deleteAllInTest();
    }

    @Test
    public void shouldFailWhenPPPResponseFalls() {
        generateFailedPPPService();
        generateMappingsChangeHistory(1, 123, 345L);

        Throwable expectedThrowable = Assertions.catchThrowable(skuActualizeService::actualize);

        Assertions.assertThat(expectedThrowable.getMessage())
            .isEqualTo("CleanupDeletedOffersDataRequest failed with message: test error");
        Assertions.assertThat(expectedThrowable).hasSameClassAs(new IllegalStateException());
    }

    @Test
    public void shouldSendHistoryOrderedByRowId() {
        OfferMappingHistory generated = generateMappingsChangeHistory(1, 555, 345L);
        generateMappingsChangeHistory(2, 124, 346L);
        generateMappingsChangeHistory(3, 125, 347L);
        skv.putValue(SKUS_PER_LAUNCH_PROPERTY_NAME, 1);
        skv.invalidateCache();

        skuActualizeService.actualize();

        Assertions.assertThat(requestArgumentCaptor.getValue().getModelIdList()).containsExactly(555L);
        Assertions.assertThat(skv.getLong(PREVIOUS_UPPER_BOUND_PROPERTY_NAME, 0L))
            .isEqualTo(generated.getId());
    }

    @Test
    public void shouldSendEachSkuIdOnce() {
        generateMappingsChangeHistory(1, 123, 345L);
        generateMappingsChangeHistory(2, 123, 346L);
        var lastGenerated = generateMappingsChangeHistory(3, 123, 347L);

        skuActualizeService.actualize();

        Assertions.assertThat(requestArgumentCaptor.getValue().getModelIdList()).containsExactly(123L);
        Assertions.assertThat(skv.getLong(PREVIOUS_UPPER_BOUND_PROPERTY_NAME, 0L))
            .isEqualTo(lastGenerated.getId());
    }

    @Test
    public void shouldSendOnlyPartner20DeletedMappings() {
        generateMappingsChangeHistory(1, 123, 345L, Offer.SkuType.MARKET);
        generateMappingsChangeHistory(2, 234, 345L, Offer.SkuType.PARTNER20);

        skuActualizeService.actualize();

        Assertions.assertThat(requestArgumentCaptor.getValue().getModelIdList()).containsExactly(234L);
    }

    @Test
    public void shouldUpdatePreviousUpperBound() {
        OfferMappingHistory generated = generateMappingsChangeHistory(1, 123, 345L);

        skuActualizeService.actualize();

        Assertions.assertThat(skv.getLong(PREVIOUS_UPPER_BOUND_PROPERTY_NAME, 0L))
            .isEqualTo(generated.getId());
    }

    @Test
    public void shouldSendDeletedMappings() {
        generateMappingsChangeHistory(1, 123, null);

        skuActualizeService.actualize();

        Assertions.assertThat(requestArgumentCaptor.getValue().getModelIdList()).containsExactly(123L);
    }

    @Test
    public void shouldSkipSendDeletedMappingsWithDeduplication() {
        generateMappingsChangeHistory(1, 123, null);
        generateMappingsChangeHistory(2, 1234, null, null, Offer.MappingConfidence.DEDUPLICATION);

        skuActualizeService.actualize();

        Assertions.assertThat(requestArgumentCaptor.getValue().getModelIdList())
            .hasSize(1);
        Assertions.assertThat(requestArgumentCaptor.getValue().getModelIdList())
            .containsExactly(123L);
    }

    private void generateOkPPPService() {
        Mockito.when(pskuPostProcessorService.cleanupDeletedOffersData(requestArgumentCaptor.capture())).thenReturn(
            PskuPostProcessor.PskuPostProcessResponse.newBuilder()
                .setStatus(PskuPostProcessor.PskuPostProcessResponse.Status.OK)
                .build()
        );
    }

    private void generateFailedPPPService() {
        Mockito.when(pskuPostProcessorService.cleanupDeletedOffersData(requestArgumentCaptor.capture())).thenReturn(
            PskuPostProcessor.PskuPostProcessResponse.newBuilder()
                .setStatus(PskuPostProcessor.PskuPostProcessResponse.Status.FAILED)
                .setMessage("test error")
                .build()
        );
    }

    private OfferMappingHistory generateMappingsChangeHistory(long offerId,
                                                              long skuIdBefore,
                                                              Long skuIdAfter,
                                                              Offer.SkuType skuTypeBefore,
                                                              Offer.MappingConfidence confidenceAfter) {
        var supplier = new Supplier()
            .setId(123)
            .setName("testSup");
        supplierRepository.insertOrUpdate(supplier);
        var offer = Offer.builder()
            .businessId(123)
            .shopSku("testSku123" + offerId)
            .title("testTitle")
            .shopCategoryName("abc")
            .marketModelName("cda")
            .vendor("qwerty")
            .vendorCode("123")
            .barCode("23")
            .offerContent(OfferContent.initEmptyContent())
            .build();
        offer = offerRepository.insertAndGetOffer(offer);
        var result = OfferMappingHistory.builder()
            .offerId(offer.getId())
            .skuIdBefore(skuIdBefore)
            .skuIdAfter(skuIdAfter)
            .skuTypeBefore(skuTypeBefore)
            .skuTypeAfter(Offer.SkuType.FAST_SKU)
            .confidenceAfter(confidenceAfter)
            .build();
        offerMappingHistoryRepository.insertOrUpdate(result);
        return result;
    }

    private OfferMappingHistory generateMappingsChangeHistory(long offerId,
                                                              long skuIdBefore,
                                                              Long skuIdAfter,
                                                              Offer.SkuType skuTypeBefore) {
        return generateMappingsChangeHistory(offerId, skuIdBefore, skuIdAfter, skuTypeBefore,
            Offer.MappingConfidence.CONTENT);
    }

    private OfferMappingHistory generateMappingsChangeHistory(long offerId, long skuIdBefore, Long skuIdAfter) {
        return generateMappingsChangeHistory(offerId, skuIdBefore, skuIdAfter, Offer.SkuType.PARTNER20);
    }

}

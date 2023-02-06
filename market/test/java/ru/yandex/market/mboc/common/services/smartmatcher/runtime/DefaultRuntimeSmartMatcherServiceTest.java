package ru.yandex.market.mboc.common.services.smartmatcher.runtime;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.annotation.Resource;

import io.micrometer.core.instrument.Metrics;
import org.junit.Before;
import org.junit.Test;

import ru.yandex.market.ir.smartmatcher.api.ScoreApi;
import ru.yandex.market.ir.smartmatcher.model.ScoreRequest;
import ru.yandex.market.ir.smartmatcher.model.ScoreResponse;
import ru.yandex.market.ir.smartmatcher.model.ScoreStatus;
import ru.yandex.market.mboc.common.dict.MbocSupplierType;
import ru.yandex.market.mboc.common.dict.Supplier;
import ru.yandex.market.mboc.common.dict.SupplierRepository;
import ru.yandex.market.mboc.common.offers.model.Offer;
import ru.yandex.market.mboc.common.offers.model.OfferContent;
import ru.yandex.market.mboc.common.offers.repository.OfferRepository;
import ru.yandex.market.mboc.common.services.idxapi.pics.PicrobotApiServiceMock;
import ru.yandex.market.mboc.common.services.modelstorage.ModelStorageCachingServiceMock;
import ru.yandex.market.mboc.common.services.modelstorage.models.Model;
import ru.yandex.market.mboc.common.services.offers.mapping.LegacyOfferMappingActionService;
import ru.yandex.market.mboc.common.services.offers.mapping.OfferMappingActionService;
import ru.yandex.market.mboc.common.services.storage.StorageKeyValueServiceMock;
import ru.yandex.market.mboc.common.utils.BaseDbTestClass;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


public class DefaultRuntimeSmartMatcherServiceTest extends BaseDbTestClass {
    private static final long MSKU_ID = 100500L;
    public static final long CATEGORY_ID = 99L;

    private DefaultRuntimeSmartMatcherService runtimeSmartMatcherService;

    @Resource
    private SupplierRepository supplierRepository;
    @Resource
    private OfferRepository offerRepository;

    @Before
    public void setUp() {
        ScoreApi scoreApi = mock(ScoreApi.class);
        when(scoreApi.scoreBatch(anyList())).then(invocation -> {
            List<ScoreRequest> scoreRequests = invocation.getArgument(0);
            return scoreRequests.stream()
                .map(scoreRequest -> {
                    ScoreResponse scoreResponse = new ScoreResponse();
                    scoreResponse.setClassifierMagicId(scoreRequest.getClassifierMagicId());
                    scoreResponse.setStatus(ScoreStatus.OK);
                    scoreResponse.setModelId(scoreRequest.getModelId());
                    scoreResponse.setConfidence(Long.parseLong(scoreRequest.getClassifierMagicId()) % 2 == 0 ? 100. :
                        -100.);
                    return scoreResponse;
                })
                .collect(Collectors.toList());
        });
        ModelStorageCachingServiceMock modelStorageCachingService = new ModelStorageCachingServiceMock();
        modelStorageCachingService.addModel(new Model()
            .setId(MSKU_ID).setTitle("MSKU1")
            .setCategoryId(CATEGORY_ID)
            .setModelType(Model.ModelType.SKU)
        );
        var legacyOfferMappingActionService = new LegacyOfferMappingActionService(null, null, offerDestinationCalculator,
            storageKeyValueService);
        var offerMappingActionService = new OfferMappingActionService(legacyOfferMappingActionService);

        runtimeSmartMatcherService = new DefaultRuntimeSmartMatcherService(
            Metrics.globalRegistry,
            scoreApi,
            new PicrobotApiServiceMock(),
            new StorageKeyValueServiceMock(),
            offerMappingActionService,
            modelStorageCachingService,
            1
        );
    }

    @Test
    public void matchBatch() {
        Supplier supplier = new Supplier(1, "Test supplier", null, null);
        supplierRepository.insert(supplier);
        Offer offerWithNegativeConfidence = createOffer(1, supplier.getId(), "ssku1", MSKU_ID);
        Offer goodOffer = createOffer(2, supplier.getId(), "ssku2", MSKU_ID);
        Offer offerFromSm = createOffer(3, supplier.getId(), "ssku3", MSKU_ID);
        offerFromSm.setSuggestMappingSource(Offer.SuggestMappingSource.SMART_MATCHER);
        Offer offerWithoutMapping = createOffer(4, supplier.getId(), "ssku4", null);
        Offer offerWithMappingOnPsku = createOffer(5, supplier.getId(), "ssku5", MSKU_ID);
        offerWithMappingOnPsku.setSuggestSkuMapping(
            new Offer.Mapping(MSKU_ID, LocalDateTime.now(), Offer.SkuType.PARTNER20)
        );

        List<Offer> offers = List.of(
            offerWithNegativeConfidence, goodOffer, offerFromSm, offerWithoutMapping, offerWithMappingOnPsku
        );
        offerRepository.insertOffers(offers);
        Map<Long, Offer> offerMap = runtimeSmartMatcherService.matchBatch(offers);

        assertThat(offerMap).hasSize(1).containsOnlyKeys(2L);
        Offer offer = offerMap.get(2L);
        assertThat(offer.isAutoApprovedMapping()).isTrue();
        assertThat(offer.getAutoApprovedMappingSource()).isEqualTo(Offer.AutoApprovedMappingSource.SMART_MATCHER);
        assertThat(offer.getApprovedSkuId()).isEqualTo(MSKU_ID);
        assertThat(offer.getApprovedSkuMappingConfidence()).isEqualTo(Offer.MappingConfidence.AUTO_APPROVE);
        assertThat(offer.getTransientModifiedBy()).isEqualTo(DefaultRuntimeSmartMatcherService.class.getSimpleName());
    }

    private static Offer createOffer(int id, int supplierId, String ssku, Long mskuId) {
        Offer offer = new Offer();
        offer.setId(id);
        offer.setBusinessId(supplierId);
        offer.setShopSku(ssku);
        offer.setTitle("title " + ssku);
        offer.setCategoryIdForTests(CATEGORY_ID, Offer.BindingKind.APPROVED);
        if (mskuId != null) {
            offer.setSuggestSkuMapping(new Offer.Mapping(mskuId, LocalDateTime.now(), Offer.SkuType.MARKET));
        }
        offer.setSuggestMappingSource(Offer.SuggestMappingSource.ULTRA_CONTROLLER);
        offer.setShopCategoryName("shop category");
        offer.setIsOfferContentPresent(true);
        offer.storeOfferContent(OfferContent.builder().build());
        offer.setServiceOffers(List.of(new Offer.ServiceOffer(supplierId).setSupplierType(MbocSupplierType.THIRD_PARTY)));
        return offer;
    }

}

package ru.yandex.market.mboc.app.pipeline;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import Market.DataCamp.DataCampOffer;
import Market.DataCamp.DataCampOfferMapping;
import Market.DataCamp.DataCampUnitedOffer;
import org.assertj.core.api.Assertions;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import ru.yandex.market.ir.http.OfferContentProcessing;
import ru.yandex.market.mboc.app.pipeline.datacamp.BaseDatacampPipelineTest;
import ru.yandex.market.mboc.common.contentprocessing.to.model.ContentProcessingOffer;
import ru.yandex.market.mboc.common.datacamp.OfferBuilder;
import ru.yandex.market.mboc.common.datacamp.model.DataCampUnitedOffersEvent;
import ru.yandex.market.mboc.common.dict.MbocSupplierType;
import ru.yandex.market.mboc.common.dict.Supplier;
import ru.yandex.market.mboc.common.offers.model.BusinessSkuKey;
import ru.yandex.market.mboc.common.offers.model.Offer;

import static ru.yandex.market.mboc.common.services.datacamp.LogbrokerDatacampOfferMessageHandlerDbTest.offerToProcess;
import static ru.yandex.market.mboc.common.utils.OfferTestUtils.TEST_CATEGORY_INFO_ID;

public class CskuIntegrationTest extends BaseDatacampPipelineTest {
    private static final int BIZ_ID = CSKU_WHITE_LIST_BIZ;
    private static final int OTHER_BIZ_ID = 12;
    private static final int SUP_ID = 321654;
    private static final int OTHER_SUP_ID = 321655;
    private static final long CATEGORY_ID = TEST_CATEGORY_INFO_ID;
    private static final String CATEGORY_NAME = "category";

    @Before
    public void setUpBeforeEach() {
        supplierRepository.insertBatch(
            new Supplier(BIZ_ID, "voda")
                .setDatacamp(true)
                .setType(MbocSupplierType.BUSINESS)
                .setNewContentPipeline(true)
                .setEats(false),
            new Supplier(SUP_ID, "los")
                .setDatacamp(true)
                .setType(MbocSupplierType.THIRD_PARTY)
                .setNewContentPipeline(true)
                .setBusinessId(BIZ_ID)
                .setEats(false),
            new Supplier(OTHER_BIZ_ID, "other voda")
                .setDatacamp(true)
                .setType(MbocSupplierType.BUSINESS)
                .setNewContentPipeline(true)
                .setEats(false),
            new Supplier(OTHER_SUP_ID, "other los")
                .setDatacamp(true)
                .setType(MbocSupplierType.THIRD_PARTY)
                .setNewContentPipeline(true)
                .setBusinessId(OTHER_BIZ_ID)
                .setEats(false)
        );
    }

    @Test
    public void testAllowCreateUpdateWhiteList() {
        var event = testAllowCreateUpdate(BIZ_ID, SUP_ID);
        Assert.assertTrue(event.getContent().getStatus().getContentSystemStatus().getAllowModelCreateUpdate());
    }

    private DataCampOffer.Offer testAllowCreateUpdate(int biz, int sup) {
        String shopSku1 = "asdsdf";
        receiveOffer(shopSku1, biz, sup, true);
        var foundOffer = offerRepository.findOfferByBusinessSkuKeyWithContent(
            new BusinessSkuKey(biz, shopSku1));

        foundOffer.updateApprovedSkuMapping(new Offer.Mapping(12324L, LocalDateTime.now(),
                Offer.SkuType.PARTNER20))
            .setApprovedSkuMappingConfidence(Offer.MappingConfidence.CONTENT);

        offerRepository.updateOffer(foundOffer);

        tmsSendDataCampOfferStates().accept(null);
        List<DataCampUnitedOffersEvent> events = logbrokerEventPublisherMock.getSendEvents();
        Assertions.assertThat(events).hasSize(1);

        List<DataCampOffer.Offer> dataCampOffers = events.stream()
            .flatMap(e -> e.getPayload().getUnitedOffersList().stream())
            .flatMap(offersBatch -> offersBatch.getOfferList().stream())
            .map(DataCampUnitedOffer.UnitedOffer::getBasic)
            .collect(Collectors.toList());

        Assertions.assertThat(dataCampOffers).hasSize(1);

        Map<Integer, DataCampOffer.Offer> offersByBusiness =
            dataCampOffers.stream().collect(Collectors.toMap(o -> o.getIdentifiers().getBusinessId(),
                Function.identity()));
        var event = offersByBusiness.get(biz);
        Assert.assertNotNull(event);
        return event;
    }

    @Test
    public void testDoSendProcessedOffersIfNotWhiteList() {
        doSendProcessedOffers(BIZ_ID, SUP_ID, true);
    }

    public void doSendProcessedOffers(int biz, int sup, boolean shouldBeSent) {
        String shopSku1 = "asdsdf";

        var dcOffer = receiveOffer(shopSku1, biz, sup, true);
        var foundOffer = offerRepository.findOfferByBusinessSkuKeyWithContent(
            new BusinessSkuKey(biz, shopSku1));

        foundOffer.updateApprovedSkuMapping(new Offer.Mapping(12324L, LocalDateTime.now(),
                Offer.SkuType.PARTNER20))
            .setApprovedSkuMappingConfidence(Offer.MappingConfidence.CONTENT);

        offersProcessingStatusService.processOffers(List.of(foundOffer));
        offerRepository.updateOffer(foundOffer);

        if (shouldBeSent) {
            Collection<ContentProcessingOffer> queue = contentProcessingQueueRepository
                .findAllByBusinessSkuKeys(new BusinessSkuKey(biz, shopSku1));
            Assertions.assertThat(queue).hasSize(1);
            makeContentProcessingOfferChangedDayAgo(biz, shopSku1);
            putOfferToDatacampService(dcOffer);
            ungroupedContentProcessingSenderService.sendFromQueue();
            ArgumentCaptor<OfferContentProcessing.OfferContentProcessingRequest> requestCaptor =
                ArgumentCaptor.forClass(OfferContentProcessing.OfferContentProcessingRequest.class);
            Mockito.verify(mockServer, Mockito.times(1)
                    .description("qwe"))
                .startContentProcessing(requestCaptor.capture(), Mockito.any());
            Mockito.clearInvocations(mockServer);

            OfferContentProcessing.OfferContentProcessingRequest sentRequest = requestCaptor.getValue();
            Assertions.assertThat(sentRequest.getOffersWithFlagsList()).hasSize(1);
            DataCampOffer.Offer sentOffer = sentRequest.getOffersWithFlags(0).getOffer();
            Assert.assertEquals(shopSku1, sentOffer.getIdentifiers().getOfferId());
        } else {
            Collection<ContentProcessingOffer> queue = contentProcessingQueueRepository
                .findAllByBusinessSkuKeys(new BusinessSkuKey(biz, shopSku1));
            Assertions.assertThat(queue).isEmpty();
        }
    }

    private DataCampOffer.Offer receiveOffer(String shopSku, int businessId, int supplierId, boolean addPartner) {
        var dcOfferReceived = OfferBuilder.create(initialOffer())
            .withIdentifiers(businessId, shopSku)
            .get()
            .setContent(offerToProcess().getContentBuilder()
                .setBinding(DataCampOfferMapping.ContentBinding.newBuilder()
                    .setPartner(addPartner ? DataCampOfferMapping.Mapping.newBuilder()
                        .setMarketSkuId(MARKET_SKU_ID_1)
                        .setMarketModelId(MODEL_PARENT_ID)
                        .setMarketCategoryId(Math.toIntExact(TEST_CATEGORY_INFO_ID))
                        .build() : DataCampOfferMapping.Mapping.newBuilder().build())
                    .setUcMapping(OfferBuilder.categoryMapping(CATEGORY_ID, CATEGORY_NAME))
                    .build()))
            .build();

        logbrokerDatacampOfferMessageHandler.process(Collections.singletonList(toMessage(supplierId, dcOfferReceived)));
        return dcOfferReceived;
    }
}

package ru.yandex.market.mboc.app.migration;

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

import ru.yandex.market.mboc.app.pipeline.datacamp.BaseDatacampPipelineTest;
import ru.yandex.market.mboc.common.datacamp.OfferBuilder;
import ru.yandex.market.mboc.common.datacamp.model.DataCampUnitedOffersEvent;
import ru.yandex.market.mboc.common.dict.MbocSupplierType;
import ru.yandex.market.mboc.common.dict.Supplier;
import ru.yandex.market.mboc.common.offers.model.BusinessSkuKey;
import ru.yandex.market.mboc.common.offers.model.Offer;

import static ru.yandex.market.mboc.common.services.datacamp.LogbrokerDatacampOfferMessageHandlerDbTest.offerToProcess;
import static ru.yandex.market.mboc.common.utils.OfferTestUtils.TEST_CATEGORY_INFO_ID;

public class EatsIntegrationTest extends BaseDatacampPipelineTest {
    private static final int EATS_BIZ_ID = 123456;
    private static final int EATS_SUP_ID = 321654;
    private static final int SRC_BIZ = 2;
    private static final long CATEGORY_ID = TEST_CATEGORY_INFO_ID;
    private static final long CATEGORY_ID_NO_KNOWLEDGE = TEST_CATEGORY_INFO_ID + 1;
    private static final String CATEGORY_NAME = "category";

    @Before
    public void setUpBeforeEach() {
        supplierRepository.insertBatch(
            new Supplier(EATS_BIZ_ID, "voda")
                .setDatacamp(false)
                .setType(MbocSupplierType.BUSINESS)
                .setNewContentPipeline(true)
                .setEats(true)
        );
    }

    @Test
    public void testEatsOfferStoredWithFakeServicePartAndSentBackToDataCamp() {
        var shopSku1 = "eatsoffer1";
        receiveOffer(shopSku1, EATS_BIZ_ID, EATS_SUP_ID, true);
        var foundOffer = offerRepository.findOfferByBusinessSkuKeyWithContent(new BusinessSkuKey(EATS_BIZ_ID, shopSku1));

        Assert.assertEquals(Offer.ProcessingStatus.IN_MODERATION, foundOffer.getProcessingStatus());
        Assert.assertEquals(Offer.MappingDestination.BLUE, foundOffer.getOfferDestination());

        var serviceOffers = foundOffer.getServiceOffers();
        Assertions.assertThat(serviceOffers).hasSize(1);
        var so = serviceOffers.get(0);
        Assert.assertEquals(MbocSupplierType.THIRD_PARTY, so.getSupplierType());
        Assert.assertEquals(EATS_BIZ_ID, so.getSupplierId());

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
        var event = offersByBusiness.get(EATS_BIZ_ID);
        Assert.assertNotNull(event);
    }

    private void receiveOffer(String shopSku, int businessId, int supplierId, boolean addPartner) {
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
    }
}

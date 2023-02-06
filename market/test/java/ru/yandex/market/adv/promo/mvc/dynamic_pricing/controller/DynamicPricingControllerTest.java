package ru.yandex.market.adv.promo.mvc.dynamic_pricing.controller;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;

import Market.DataCamp.API.DatacampMessageOuterClass;
import Market.DataCamp.DataCampOffer;
import Market.DataCamp.DataCampOfferPrice;
import Market.DataCamp.DataCampUnitedOffer;
import Market.DataCamp.SyncAPI.OffersBatch;
import org.apache.commons.collections4.CollectionUtils;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import ru.yandex.market.adv.promo.FunctionalTest;
import ru.yandex.market.adv.promo.logbroker.model.DatacampMessageLogbrokerEvent;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.common.test.spring.FunctionalTestHelper;
import ru.yandex.market.logbroker.LogbrokerEventPublisher;
import ru.yandex.market.mbi.datacamp.saas.SaasService;
import ru.yandex.market.mbi.datacamp.saas.impl.attributes.ServiceLocalSearchAttribute;
import ru.yandex.market.mbi.datacamp.saas.impl.model.SaasOfferInfo;
import ru.yandex.market.mbi.datacamp.saas.impl.model.SaasSearchResult;
import ru.yandex.market.mbi.datacamp.stroller.DataCampClient;

import static Market.DataCamp.DataCampOfferPrice.DynamicPricing.Type.MINIMAL_PRICE_ON_MARKET;
import static Market.DataCamp.DataCampOfferPrice.DynamicPricing.Type.RECOMMENDED_PRICE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static ru.yandex.market.adv.promo.utils.DataCampOfferUtils.createBasicOffer;
import static ru.yandex.market.mbi.datacamp.saas.impl.attributes.DataCampSearchAttribute.DYNAMIC_PRICING_TYPE;
import static ru.yandex.market.mbi.datacamp.stroller.model.graphql.OfferField.OFFER_IDENTIFIERS;

class DynamicPricingControllerTest extends FunctionalTest {
    @Autowired
    private SaasService saasService;
    @Autowired
    private DataCampClient dataCampClient;
    @Autowired
    private LogbrokerEventPublisher<DatacampMessageLogbrokerEvent> logbrokerPublisher;

    @Test
    @DbUnitDataSet
    public void testDisableAll() {
        mockSaasAndDatacampResponses();
        testDisableAllInternal();
    }

    @Test
    @DbUnitDataSet(before = "before.csv")
    public void testDisableAllGraphqlQuery() {
        mockSaasAndDatacampResponsesGraphqlQuery();
        testDisableAllInternal();
    }

    private void testDisableAllInternal() {

        long partnerId = 1;
        long businessId = 1001;

        String url = "/partner/dynamic-pricing/by-ssku/disable-all?partnerId={partnerId}&businessId={businessId}";
        ResponseEntity<String> response = FunctionalTestHelper.put(baseUrl() + url, null, partnerId, businessId);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        ArgumentCaptor<DatacampMessageLogbrokerEvent> eventCaptor =
                ArgumentCaptor.forClass(DatacampMessageLogbrokerEvent.class);
        verify(logbrokerPublisher, times(2)).publishEvent(eventCaptor.capture());

        List<List<DataCampOffer.Offer>> sortedList = new ArrayList<>();
        DatacampMessageLogbrokerEvent event1 = eventCaptor.getAllValues().get(0);
        DatacampMessageOuterClass.DatacampMessage msg1 = event1.getPayload();
        List<DataCampOffer.Offer> offers1 = msg1.getOffersList().get(0).getOfferList();
        sortedList.add(offers1);
        DatacampMessageLogbrokerEvent event2 = eventCaptor.getAllValues().get(1);
        DatacampMessageOuterClass.DatacampMessage msg2 = event2.getPayload();
        List<DataCampOffer.Offer> offers2 = msg2.getOffersList().get(0).getOfferList();
        sortedList.add(offers2);
        sortedList.sort(Comparator.comparing(List::size));
        offers1 = sortedList.get(1);
        assertThat(offers1).hasSize(2);
        assertThat(offers1).map(offer -> offer.getIdentifiers().getOfferId()).containsExactly("offer01", "offer02");
        assertThat(offers1).map(offer -> offer.getPrice().getDynamicPricing().hasType()).containsExactly(false, false);

        offers2 = sortedList.get(0);
        assertThat(offers2).hasSize(1);
        assertThat(offers2).map(offer -> offer.getIdentifiers().getOfferId()).containsExactly("offer11");
        assertThat(offers2).map(offer -> offer.getPrice().getDynamicPricing().hasType()).containsExactly(false);
    }

    private void mockSaasAndDatacampResponses() {
        mockSaasResponse();
        mockDatacampResponse();
    }

    private void mockSaasAndDatacampResponsesGraphqlQuery() {
        mockSaasResponse();
        mockDatacampResponseGraphqlQuery();
    }

    private void mockSaasResponse() {
        SaasOfferInfo saasOfferInfo1 = SaasOfferInfo.newBuilder()
                .addShopId(1L)
                .addOfferId("offer01")
                .build();
        SaasOfferInfo saasOfferInfo2 = SaasOfferInfo.newBuilder()
                .addShopId(1L)
                .addOfferId("offer02")
                .build();
        SaasSearchResult saasSearchResult1 = SaasSearchResult.builder()
                .setOffers(List.of(saasOfferInfo1, saasOfferInfo2))
                .setTotalCount(1)
                .build();
        doReturn(saasSearchResult1).when(saasService).searchBusinessOffers(argThat(filter -> filter.getFiltersMap().get(new ServiceLocalSearchAttribute(DYNAMIC_PRICING_TYPE, 1)).contains("1")));

        SaasOfferInfo saasOfferInfo3 = SaasOfferInfo.newBuilder()
                .addShopId(1L)
                .addOfferId("offer11")
                .build();
        SaasSearchResult saasSearchResult2 = SaasSearchResult.builder()
                .setOffers(List.of(saasOfferInfo3))
                .setTotalCount(1)
                .build();
        doReturn(saasSearchResult2).when(saasService).searchBusinessOffers(argThat(filter -> filter.getFiltersMap().get(new ServiceLocalSearchAttribute(DYNAMIC_PRICING_TYPE, 1)).contains("2")));
    }

    private void mockDatacampResponse() {
        doReturn(createUnitedResponse1())
                .when(dataCampClient)
                .getBusinessUnitedOffers(
                        eq(1001L),
                        argThat(collection -> collection.containsAll(List.of("offer01", "offer02"))),
                        eq(1L)
                );
        doReturn(createUnitedResponse2())
                .when(dataCampClient)
                .getBusinessUnitedOffers(
                        eq(1001L),
                        argThat(collection -> collection.contains("offer11")),
                        eq(1L)
                );
    }

    private void mockDatacampResponseGraphqlQuery() {
        doReturn(createUnitedResponse1())
                .when(dataCampClient)
                .getBusinessUnitedOffers(
                        eq(1001L),
                        argThat(collection -> collection.containsAll(List.of("offer01", "offer02"))),
                        eq(1L),
                        argThat(offerQuery -> CollectionUtils.isEqualCollection(
                                offerQuery.getFields(), EnumSet.of(OFFER_IDENTIFIERS)))
                );
        doReturn(createUnitedResponse2())
                .when(dataCampClient)
                .getBusinessUnitedOffers(
                        eq(1001L),
                        argThat(collection -> collection.contains("offer11")),
                        eq(1L),
                        argThat(offerQuery -> CollectionUtils.isEqualCollection(
                                offerQuery.getFields(), EnumSet.of(OFFER_IDENTIFIERS)))
                );
    }

    private OffersBatch.UnitedOffersBatchResponse createUnitedResponse1() {
        DataCampOffer.Offer basicOffer1 = createBasicOffer("offer01", 1, 1001);
        DataCampOffer.Offer serviceOffer1 = createServiceOffer(basicOffer1, 1);
        DataCampOffer.Offer basicOffer2 = createBasicOffer("offer02", 1, 1001);
        DataCampOffer.Offer serviceOffer2 = createServiceOffer(basicOffer2, 1);

        return OffersBatch.UnitedOffersBatchResponse.newBuilder()
                .addEntries(OffersBatch.UnitedOffersBatchResponse.Entry.newBuilder()
                        .setUnitedOffer(DataCampUnitedOffer.UnitedOffer.newBuilder()
                                .setBasic(basicOffer1)
                                .putActual(1, DataCampUnitedOffer.ActualOffers.newBuilder()
                                        .putWarehouse(1, basicOffer1)
                                        .build())
                                .putService(1, serviceOffer1)
                                .build())
                        .build())
                .addEntries(OffersBatch.UnitedOffersBatchResponse.Entry.newBuilder()
                        .setUnitedOffer(DataCampUnitedOffer.UnitedOffer.newBuilder()
                                .setBasic(basicOffer2)
                                .putActual(1, DataCampUnitedOffer.ActualOffers.newBuilder()
                                        .putWarehouse(1, basicOffer2)
                                        .build())
                                .putService(1, serviceOffer2)
                                .build())
                        .build())
                .build();
    }

    private OffersBatch.UnitedOffersBatchResponse createUnitedResponse2() {
        DataCampOffer.Offer basicOffer3 = createBasicOffer("offer11", 1, 1001);
        DataCampOffer.Offer serviceOffer3 = createServiceOffer(basicOffer3, 2);

        return OffersBatch.UnitedOffersBatchResponse.newBuilder()
                .addEntries(OffersBatch.UnitedOffersBatchResponse.Entry.newBuilder()
                        .setUnitedOffer(DataCampUnitedOffer.UnitedOffer.newBuilder()
                                .setBasic(basicOffer3)
                                .putActual(1, DataCampUnitedOffer.ActualOffers.newBuilder()
                                        .putWarehouse(1, basicOffer3)
                                        .build())
                                .putService(1, serviceOffer3)
                                .build())
                        .build())
                .build();
    }

    private DataCampOffer.Offer createServiceOffer(
            DataCampOffer.Offer basicOffer,
            int strategyType
    ) {
        return DataCampOffer.Offer.newBuilder()
                .setIdentifiers(basicOffer.getIdentifiers())
                .setPrice(DataCampOfferPrice.OfferPrice.newBuilder()
                        .setDynamicPricing(DataCampOfferPrice.DynamicPricing.newBuilder()
                                .setType(strategyType == 1 ? RECOMMENDED_PRICE : MINIMAL_PRICE_ON_MARKET)
                        )
                )
                .build();

    }
}

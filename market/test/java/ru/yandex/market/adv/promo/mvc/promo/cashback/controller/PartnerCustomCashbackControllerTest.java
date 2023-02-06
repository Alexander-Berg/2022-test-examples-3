package ru.yandex.market.adv.promo.mvc.promo.cashback.controller;

import java.util.Collections;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.stream.Collectors;

import Market.DataCamp.API.DatacampMessageOuterClass;
import Market.DataCamp.DataCampOffer;
import Market.DataCamp.DataCampOfferMeta;
import Market.DataCamp.DataCampOfferPromos;
import Market.DataCamp.DataCampPromo;
import Market.DataCamp.DataCampPromo.PromoMechanics.PartnerCustomCashback.CreationTab;
import Market.DataCamp.DataCampUnitedOffer;
import Market.DataCamp.SyncAPI.OffersBatch;
import Market.DataCamp.SyncAPI.SyncGetPromo;
import NMarket.Common.Promo.Promo;
import com.google.protobuf.Timestamp;
import org.apache.commons.collections.CollectionUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.skyscreamer.jsonassert.JSONAssert;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import ru.yandex.market.adv.promo.FunctionalTest;
import ru.yandex.market.adv.promo.logbroker.model.DatacampMessageLogbrokerEvent;
import ru.yandex.market.adv.promo.utils.CommonTestUtils;
import ru.yandex.market.adv.promo.utils.model.BasicAndServiceOffersPair;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.common.test.spring.FunctionalTestHelper;
import ru.yandex.market.core.promo.CommitPromoOffersResponse;
import ru.yandex.market.core.promo.CommitPromoOffersResult;
import ru.yandex.market.logbroker.LogbrokerEventPublisher;
import ru.yandex.market.mbi.api.client.MbiApiClient;
import ru.yandex.market.mbi.datacamp.saas.SaasService;
import ru.yandex.market.mbi.datacamp.saas.impl.model.SaasOfferInfo;
import ru.yandex.market.mbi.datacamp.saas.impl.model.SaasSearchResult;
import ru.yandex.market.mbi.datacamp.stroller.DataCampClient;
import ru.yandex.market.mbi.datacamp.stroller.model.GetPromoBatchRequestWithFilters;
import ru.yandex.market.mbi.datacamp.stroller.model.PromoDatacampRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static ru.yandex.market.adv.promo.datacamp.utils.DateTimeUtils.getDateTimeInSeconds;
import static ru.yandex.market.adv.promo.datacamp.utils.PromoStorageUtils.TERMLESS_PROMO_DATE_TIME;
import static ru.yandex.market.adv.promo.utils.CommonTestUtils.getResource;
import static ru.yandex.market.adv.promo.utils.CustomCashbackMechanicTestUtils.createCustomCashbackGetResponse;
import static ru.yandex.market.adv.promo.utils.CustomCashbackMechanicTestUtils.createDeletePromoBatchResponse;
import static ru.yandex.market.adv.promo.utils.CustomCashbackMechanicTestUtils.createGetResponseForDeleteRequest;
import static ru.yandex.market.adv.promo.utils.CustomCashbackMechanicTestUtils.createGetResponseForUpdatePrioritiesRequest;
import static ru.yandex.market.adv.promo.utils.CustomCashbackMechanicTestUtils.createGetResponseForUpdateRequest;
import static ru.yandex.market.adv.promo.utils.CustomCashbackMechanicTestUtils.createResponseForPriorityRequest;
import static ru.yandex.market.adv.promo.utils.DataCampOfferUtils.createBasicOffer;
import static ru.yandex.market.adv.promo.utils.DataCampOfferUtils.createPromo;
import static ru.yandex.market.adv.promo.utils.DataCampOfferUtils.createServiceOfferWithPromos;
import static ru.yandex.market.adv.promo.utils.DataCampOfferUtils.createUnitedOffersBatchResponse;
import static ru.yandex.market.mbi.datacamp.stroller.model.graphql.OfferField.ALL_PARTNER_CASHBACK_PROMOS;
import static ru.yandex.market.mbi.datacamp.stroller.model.graphql.OfferField.ALL_PARTNER_CASHBACK_PROMOS_META;
import static ru.yandex.market.mbi.datacamp.stroller.model.graphql.OfferField.OFFER_CATEGORY_ID;
import static ru.yandex.market.mbi.datacamp.stroller.model.graphql.OfferField.OFFER_IDENTIFIERS;
import static ru.yandex.market.mbi.datacamp.stroller.model.graphql.OfferField.OFFER_VENDOR_ID;

public class PartnerCustomCashbackControllerTest  extends FunctionalTest {

    private final static String RESULT_JSON = "customCashback_response.json";

    @Autowired
    private DataCampClient dataCampClient;

    @Autowired
    private MbiApiClient mbiApiClient;

    @Autowired
    private SaasService saasDataCampShopService;

    @Autowired
    private LogbrokerEventPublisher<DatacampMessageLogbrokerEvent> logbrokerService;

    @Test
    @DisplayName("Проверка корректности получения кастомных кешбечных акций")
    void getCustomCashbackTest() {
        long partnerId = 12345;
        long businessId = 789;

        doReturn(createCustomCashbackGetResponse())
                .when(dataCampClient).getPromos(any(PromoDatacampRequest.class));

        ResponseEntity<String> response = getCustomCashback(partnerId, businessId);

        String expected = getResource(this.getClass(), RESULT_JSON);
        JSONAssert.assertEquals(expected, response.getBody(), true);
    }

    @Test
    @DbUnitDataSet
    @DisplayName("Получение кастомных кешбечных акций, в которых участвует оффер")
    void getCustomCashbackForOfferTest() {
        int partnerId = 12345;
        int businessId = 789;
        String ssku = "ssku";
        Integer categoryId = 1;
        Integer brandId = 7;

        DataCampOffer.Offer basicOffer = createBasicOffer(ssku, partnerId, businessId, categoryId, brandId);
        DataCampOffer.Offer serviceOffer = createServiceOfferWithPromos(
                basicOffer.getIdentifiers(),
                null,
                null,
                null,
                List.of(createPromo("12345_qwe"))
        );
        BasicAndServiceOffersPair offersPair = new BasicAndServiceOffersPair(basicOffer, serviceOffer);

        doReturn(createCustomCashbackGetResponse())
                .when(dataCampClient).getPromos(any(PromoDatacampRequest.class));

        doReturn(createUnitedOffersBatchResponse(partnerId, List.of(offersPair)))
                .when(dataCampClient)
                .getBusinessUnitedOffers(
                        eq(Long.valueOf(businessId)),
                        argThat(collection -> collection.contains(ssku)),
                        eq(Long.valueOf(partnerId))
                );

        ResponseEntity<String> response = getCustomCashback(partnerId, businessId, ssku);

        String expected = getResource(this.getClass(), RESULT_JSON);
        JSONAssert.assertEquals(expected, response.getBody(), true);
    }

    @Test
    @DbUnitDataSet(before = "before.csv")
    void getCustomCashbackForOfferGraphqlQueryTest() {
        int partnerId = 12345;
        int businessId = 789;
        String ssku = "ssku";
        Integer categoryId = 1;
        Integer brandId = 7;

        DataCampOffer.Offer basicOffer = createBasicOffer(ssku, partnerId, businessId, categoryId, brandId);
        DataCampOffer.Offer serviceOffer = createServiceOfferWithPromos(
                basicOffer.getIdentifiers(),
                null,
                null,
                null,
                List.of(createPromo("12345_qwe"))
        );
        BasicAndServiceOffersPair offersPair = new BasicAndServiceOffersPair(basicOffer, serviceOffer);

        doReturn(createCustomCashbackGetResponse())
                .when(dataCampClient).getPromos(any(PromoDatacampRequest.class));

        doReturn(createUnitedOffersBatchResponse(partnerId, List.of(offersPair)))
                .when(dataCampClient)
                .getBusinessUnitedOffers(
                        eq(Long.valueOf(businessId)),
                        argThat(collection -> collection.contains(ssku)),
                        eq(Long.valueOf(partnerId)),
                        argThat(offerQuery -> CollectionUtils.isEqualCollection(
                                offerQuery.getFields(), EnumSet.of(
                                        OFFER_CATEGORY_ID,
                                        OFFER_VENDOR_ID,
                                        ALL_PARTNER_CASHBACK_PROMOS
                                )))
                );

        ResponseEntity<String> response = getCustomCashback(partnerId, businessId, ssku);

        String expected = getResource(this.getClass(), RESULT_JSON);
        JSONAssert.assertEquals(expected, response.getBody(), true);
    }

    @Test
    @DisplayName("Проверка корректности создания кастомного кешбека")
    void createCustomCashbackTest() {
        long partnerId = 12345;
        long businessId = 789;
        int priority = 1;

        doReturn(createResponseForPriorityRequest(priority))
                .when(dataCampClient).getPromos(any(PromoDatacampRequest.class));

        String requestBody = CommonTestUtils.getResource(this.getClass(), "createCustomCashback.json");
        ResponseEntity<String> response = sendCreateCustomCashbackRequest(partnerId, businessId, requestBody);

        assertEquals(HttpStatus.OK, response.getStatusCode());

        ArgumentCaptor<DataCampPromo.PromoDescription> captor =
                ArgumentCaptor.forClass(DataCampPromo.PromoDescription.class);
        verify(dataCampClient, times(1)).addPromo(captor.capture());
        DataCampPromo.PromoDescription promo = captor.getValue();

        assertNotNull(promo);
        assertEquals("12345_promo", promo.getPrimaryKey().getPromoId());
        assertEquals("Test promo", promo.getAdditionalInfo().getName());
        assertEquals(
                DataCampPromo.PromoType.PARTNER_CUSTOM_CASHBACK,
                promo.getPromoGeneralInfo().getPromoType()
        );
        assertTrue(promo.getMechanicsData().hasPartnerCustomCashback());
        DataCampPromo.PromoMechanics.PartnerCustomCashback cashback =
                promo.getMechanicsData().getPartnerCustomCashback();
        assertTrue(cashback.hasMarketTariffsVersionId());
        assertEquals(0, cashback.getMarketTariffsVersionId());
        assertEquals(priority + 1, cashback.getPriority());
        assertEquals(CreationTab.DYNAMIC_GROUPS, cashback.getSource());

        DataCampPromo.PromoConstraints constraints = promo.getConstraints();
        assertEquals(1_600_500_000, constraints.getStartDate());
        assertEquals(1_600_700_000, constraints.getEndDate());

        DataCampPromo.PromoConstraints.OffersMatchingRule offersMatchingRule =
                promo.getConstraints().getOffersMatchingRules(0);
        assertThat(offersMatchingRule.getOrigionalCategoryRestriction().getIncludeCategegoryRestrictionList())
                .containsExactlyInAnyOrderElementsOf(
                        List.of(
                                DataCampPromo.PromoConstraints.OffersMatchingRule.PromoCategory.newBuilder()
                                        .setId(11111)
                                        .build(),
                                DataCampPromo.PromoConstraints.OffersMatchingRule.PromoCategory.newBuilder()
                                        .setId(22222)
                                        .build())
                );
        assertThat(offersMatchingRule.getOriginalBrandRestriction().getIncludeBrands().getBrandsList())
                .containsExactlyInAnyOrderElementsOf(
                        List.of(
                                DataCampPromo.PromoBrand.newBuilder()
                                        .setId(33333)
                                        .build(),
                                DataCampPromo.PromoBrand.newBuilder()
                                        .setId(55555)
                                        .build(),
                                DataCampPromo.PromoBrand.newBuilder()
                                        .setId(77777)
                                        .build()
                        )
                );
    }

    @Test
    @DisplayName("Создание кастомного кешбека: бессрочная акция")
    void createCustomCashbackTest_termless() {
        long partnerId = 12345;
        long businessId = 789;
        int priority = 1;

        doReturn(new CommitPromoOffersResponse((CommitPromoOffersResult.COMMITTED)))
                .when(mbiApiClient).commitPromoOffers(any(String.class));

        doReturn(createResponseForPriorityRequest(priority))
                .when(dataCampClient).getPromos(any(PromoDatacampRequest.class));

        String requestBody = CommonTestUtils.getResource(this.getClass(), "createCustomCashback_termless.json");
        ResponseEntity<String> response = sendCreateCustomCashbackRequest(partnerId, businessId, requestBody);

        assertEquals(HttpStatus.OK, response.getStatusCode());

        ArgumentCaptor<DataCampPromo.PromoDescription> captor =
                ArgumentCaptor.forClass(DataCampPromo.PromoDescription.class);
        verify(dataCampClient, times(1)).addPromo(captor.capture());
        DataCampPromo.PromoDescription promo = captor.getValue();

        assertEquals(CreationTab.FILE, promo.getMechanicsData().getPartnerCustomCashback().getSource());
        assertEquals(getDateTimeInSeconds(TERMLESS_PROMO_DATE_TIME), promo.getConstraints().getEndDate());
    }

    @Test
    @DbUnitDataSet
    @DisplayName("Проверка корректности обновления кастомного кешбека")
    void updateCustomCashbackTest() {
        String cashbackId = "12345_promo";
        String cashbackWithoutClearingId = "12345_cashback";
        mockOffersForClearing(cashbackId, cashbackWithoutClearingId);

        updateCustomCashbackTestInternal(cashbackId, cashbackWithoutClearingId);
    }

    @Test
    @DbUnitDataSet(before = "before.csv")
    void updateCustomCashbackGraphqlQueryTest() {
        String cashbackId = "12345_promo";
        String cashbackWithoutClearingId = "12345_cashback";
        mockOffersForClearingGraphqlQuery(cashbackId, cashbackWithoutClearingId);

        updateCustomCashbackTestInternal(cashbackId, cashbackWithoutClearingId);
    }

    void updateCustomCashbackTestInternal(
            String cashbackId,
            String cashbackWithoutClearingId
    ) {
        long partnerId = 12345;
        long businessId = 789;
        String cashbackName = "Test promo";

        doReturn(new CommitPromoOffersResponse((CommitPromoOffersResult.COMMITTED)))
                .when(mbiApiClient).commitPromoOffers(any(String.class));

        doReturn(createGetResponseForUpdateRequest())
                .when(dataCampClient).getPromos(any(GetPromoBatchRequestWithFilters.class));

        mockSaasResponse();

        String requestBody = CommonTestUtils.getResource(this.getClass(), "updateCustomCashback.json");
        sendUpdateCustomCashbackRequest(partnerId, businessId, requestBody);

        ArgumentCaptor<DataCampPromo.PromoDescription> captor =
                ArgumentCaptor.forClass(DataCampPromo.PromoDescription.class);
        verify(dataCampClient, times(1)).addPromo(captor.capture());
        DataCampPromo.PromoDescription promo = captor.getValue();

        assertNotNull(promo);
        assertEquals(cashbackId, promo.getPrimaryKey().getPromoId());
        assertEquals(cashbackName, promo.getAdditionalInfo().getName());

        assertTrue(promo.getMechanicsData().hasPartnerCustomCashback());
        DataCampPromo.PromoMechanics.PartnerCustomCashback cashback =
                promo.getMechanicsData().getPartnerCustomCashback();
        assertEquals(CreationTab.DYNAMIC_GROUPS, cashback.getSource());
        assertEquals(10, cashback.getCashbackValue());

        DataCampPromo.PromoConstraints constraints = promo.getConstraints();
        assertEquals(1_600_500_000, constraints.getStartDate());
        assertEquals(1_600_700_000, constraints.getEndDate());

        DataCampPromo.PromoConstraints.OffersMatchingRule offersMatchingRule =
                promo.getConstraints().getOffersMatchingRules(0);
        assertThat(offersMatchingRule.getOrigionalCategoryRestriction().getIncludeCategegoryRestrictionList())
                .containsExactlyInAnyOrderElementsOf(
                        List.of(
                                DataCampPromo.PromoConstraints.OffersMatchingRule.PromoCategory.newBuilder()
                                        .setId(11111)
                                        .build(),
                                DataCampPromo.PromoConstraints.OffersMatchingRule.PromoCategory.newBuilder()
                                        .setId(22222)
                                        .build())
                );
        assertThat(offersMatchingRule.getOriginalBrandRestriction().getIncludeBrands().getBrandsList())
                .containsExactlyInAnyOrderElementsOf(
                        List.of(
                                DataCampPromo.PromoBrand.newBuilder()
                                        .setId(33333)
                                        .build(),
                                DataCampPromo.PromoBrand.newBuilder()
                                        .setId(55555)
                                        .build(),
                                DataCampPromo.PromoBrand.newBuilder()
                                        .setId(77777)
                                        .build()
                        )
                );

        //Проверка корректности чистки офферов
        ArgumentCaptor<DatacampMessageLogbrokerEvent> eventCaptor =
                ArgumentCaptor.forClass(DatacampMessageLogbrokerEvent.class);
        verify(logbrokerService, times(1)).publishEvent(eventCaptor.capture());
        DatacampMessageLogbrokerEvent event = eventCaptor.getValue();
        DatacampMessageOuterClass.DatacampMessage msg = event.getPayload();
        List<DataCampOffer.Offer> offers = msg.getOffersList().get(0).getOfferList();
        assertEquals(3, offers.size());
        Assertions.assertEquals("offer1", offers.get(0).getIdentifiers().getOfferId());
        checkOfferHasCashbackPromo(cashbackWithoutClearingId, offers.get(0));
        Assertions.assertEquals("offer2", offers.get(1).getIdentifiers().getOfferId());
        checkOfferHasCashbackPromo(cashbackWithoutClearingId, offers.get(1));
    }

    private void mockOffersForClearing(
            String cashbackForClearingId,
            String cashbackWithoutClearingId
    ) {

        doReturn(createUnitedResponse(cashbackForClearingId, cashbackWithoutClearingId))
                .when(dataCampClient).getBusinessUnitedOffers(anyLong(), anyCollection(), any());
    }

    private void mockOffersForClearingGraphqlQuery(
            String cashbackForClearingId,
            String cashbackWithoutClearingId
    ) {
        doReturn(createUnitedResponse(cashbackForClearingId, cashbackWithoutClearingId))
                .when(dataCampClient).getBusinessUnitedOffers(
                        anyLong(),
                        anyCollection(),
                        any(),
                        argThat(offerQuery -> CollectionUtils.isEqualCollection(
                                offerQuery.getFields(), EnumSet.of(
                                        OFFER_IDENTIFIERS,
                                        ALL_PARTNER_CASHBACK_PROMOS,
                                        ALL_PARTNER_CASHBACK_PROMOS_META
                                )))
                        );
    }

    private OffersBatch.UnitedOffersBatchResponse createUnitedResponse(
            String cashbackForClearingId,
            String cashbackWithoutClearingId
    ) {
        List<String> promoIds = Collections.emptyList();
        List<String> cashbackPromoIds = List.of(cashbackForClearingId, cashbackWithoutClearingId);

        DataCampOffer.Offer basicOffer1 = createBasicOffer("offer1", 11111, 100);
        DataCampOffer.Offer serviceOffer1 =
                createServiceOffer(basicOffer1, 100, 1, promoIds, cashbackPromoIds);
        DataCampOffer.Offer basicOffer2 = createBasicOffer("offer2", 11111, 100);
        DataCampOffer.Offer serviceOffer2 =
                createServiceOffer(basicOffer2, 100, 1, promoIds, cashbackPromoIds);
        DataCampOffer.Offer basicOffer3 = createBasicOffer("offer3", 11111, 100);
        DataCampOffer.Offer serviceOffer3 =
                createServiceOffer(basicOffer3, 100, 1, promoIds, cashbackPromoIds);

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
            long seconds,
            int nanos,
            List<String> promoIds,
            List<String> cashbackPromoIds
    ) {
        return DataCampOffer.Offer.newBuilder()
                .setIdentifiers(basicOffer.getIdentifiers())
                .setPromos(DataCampOfferPromos.OfferPromos.newBuilder()
                        .setPartnerPromos(DataCampOfferPromos.Promos.newBuilder()
                                .setMeta(DataCampOfferMeta.UpdateMeta.newBuilder()
                                        .setTimestamp(Timestamp.newBuilder()
                                                .setSeconds(seconds)
                                                .setNanos(nanos)
                                                .build())
                                        .build())
                                .addAllPromos(promoIds.stream().map(promoId ->
                                        DataCampOfferPromos.Promo.newBuilder()
                                                .setId(promoId)
                                                .build()).collect(Collectors.toList())
                                )
                                .build())
                        .setPartnerCashbackPromos(
                                DataCampOfferPromos.Promos.newBuilder()
                                        .addAllPromos(cashbackPromoIds.stream().map(promoId ->
                                                DataCampOfferPromos.Promo.newBuilder()
                                                        .setId(promoId)
                                                        .build()).collect(Collectors.toList())
                                        )

                        )
                        .setAnaplanPromos(
                                DataCampOfferPromos.MarketPromos.newBuilder()
                                        .setAllPromos(DataCampOfferPromos.Promos.newBuilder()
                                                .setMeta(DataCampOfferMeta.UpdateMeta.newBuilder()
                                                        .setTimestamp(Timestamp.newBuilder()
                                                                .setSeconds(seconds)
                                                                .setNanos(nanos)
                                                                .build())
                                                        .build())
                                                .addAllPromos(List.of(
                                                        DataCampOfferPromos.Promo.newBuilder()
                                                                .setId("another_promo")
                                                                .build())
                                                )
                                                .build())
                                        .build()
                        )
                        .build())
                .build();

    }

    private void checkOfferHasCashbackPromo(String promoId, DataCampOffer.Offer offer) {
        assertTrue(offer.getPromos().hasPartnerCashbackPromos());
        assertFalse(offer.getPromos().hasAnaplanPromos());
        assertEquals(1, offer.getPromos().getPartnerCashbackPromos().getPromosCount());
        DataCampOfferPromos.Promo promo1 = offer.getPromos().getPartnerCashbackPromos().getPromos(0);
        Assertions.assertEquals(promoId, promo1.getId());
    }

    private void mockSaasResponse() {
        SaasOfferInfo saasOfferInfo = SaasOfferInfo.newBuilder()
                .addShopId(12345L)
                .addOfferId("offer-1")
                .build();
        SaasSearchResult saasSearchResult = SaasSearchResult.builder()
                .setOffers(List.of(saasOfferInfo))
                .setTotalCount(1)
                .build();
        doReturn(saasSearchResult)
                .doReturn(
                        SaasSearchResult.builder()
                                .setOffers(Collections.emptyList())
                                .setTotalCount(0)
                                .build()
                )
                .when(saasDataCampShopService).searchBusinessOffers(any());
    }

    @Test
    @DbUnitDataSet
    @DisplayName("Проверка корректности удаления кастомного кешбека")
    void deleteCustomCashbackTest() {
        long partnerId = 12345;
        long businessId = 789;
        String promoId = "12345_promo";

        doReturn(createGetResponseForDeleteRequest())
                .when(dataCampClient).getPromos(any(GetPromoBatchRequestWithFilters.class));

        doReturn(createDeletePromoBatchResponse())
                .when(dataCampClient).deletePromo(any(SyncGetPromo.DeletePromoBatchRequest.class));

        sendDeleteCustomCashbackRequest(partnerId, businessId, promoId);

        ArgumentCaptor<SyncGetPromo.DeletePromoBatchRequest> captor =
                ArgumentCaptor.forClass(SyncGetPromo.DeletePromoBatchRequest.class);
        verify(dataCampClient, times(1)).deletePromo(captor.capture());
        SyncGetPromo.DeletePromoBatchRequest deleteRequest = captor.getValue();

        SyncGetPromo.PromoIdentifiers identifier = deleteRequest.getIdentifiers(0);

        assertEquals("12345_promo", identifier.getPrimaryKey().getPromoId());
        assertEquals(Promo.ESourceType.PARTNER_SOURCE, identifier.getPrimaryKey().getSource());
        assertEquals((int)businessId, identifier.getPrimaryKey().getBusinessId());
        assertEquals((int)partnerId, identifier.getPartnerId());
    }

    @Test
    @DisplayName("Проверка корректности обновления приоритетов кастомного кешбека")
    void updateCustomCashbackPrioritiesTest() {
        long partnerId = 12345;
        long businessId = 789;

        doReturn(createGetResponseForUpdatePrioritiesRequest())
                .when(dataCampClient).getPromos(any(GetPromoBatchRequestWithFilters.class));

        String requestBody = CommonTestUtils.getResource(this.getClass(), "updateCustomCashbackPriorities.json");
        sendUpdateCustomCashbackPrioritiesRequest(partnerId, businessId, requestBody);

        ArgumentCaptor<SyncGetPromo.UpdatePromoBatchRequest> captor =
                ArgumentCaptor.forClass(SyncGetPromo.UpdatePromoBatchRequest.class);
        verify(dataCampClient, times(1)).addPromo(captor.capture(), eq(businessId));
        SyncGetPromo.UpdatePromoBatchRequest updateRequest = captor.getValue();

        List<DataCampPromo.PromoDescription> promos = updateRequest.getPromos().getPromoList()
                .stream()
                .sorted(Comparator.comparingInt(
                        promo -> promo.getMechanicsData().getPartnerCustomCashback().getPriority())
                )
                .collect(Collectors.toList());
        assertEquals(2, promos.size());

        DataCampPromo.PromoDescription promo1 = promos.get(0);
        DataCampPromo.PromoDescription promo2 = promos.get(1);
        assertEquals("12345_promo", promo1.getPrimaryKey().getPromoId());
        assertEquals(1, promo1.getMechanicsData().getPartnerCustomCashback().getPriority());
        assertEquals("12345_qwe", promo2.getPrimaryKey().getPromoId());
        assertEquals(2, promo2.getMechanicsData().getPartnerCustomCashback().getPriority());
    }


    private void sendUpdateCustomCashbackPrioritiesRequest(long partnerId, long businessId, String body) {
        FunctionalTestHelper.put(
                baseUrl() + "/partner/promo/cashback/custom/priorities/change?partnerId="
                        + partnerId + "&businessId=" + businessId,
                new HttpEntity<>(body, getDefaultHeaders())
        );
    }

    private void sendDeleteCustomCashbackRequest(long partnerId, long businessId, String promoId) {
        FunctionalTestHelper.delete(
                baseUrl() + "/partner/promo/cashback/custom?" +
                        "partnerId=" + partnerId +
                        "&businessId=" + businessId +
                        "&promoId=" + promoId
        );
    }

    private void sendUpdateCustomCashbackRequest(long partnerId, long businessId, String body) {
        FunctionalTestHelper.put(
                baseUrl() + "/partner/promo/cashback/custom?partnerId=" + partnerId + "&businessId=" + businessId,
                new HttpEntity<>(body, getDefaultHeaders())
        );
    }

    private ResponseEntity<String> sendCreateCustomCashbackRequest(long partnerId, long businessId, String body) {
        return FunctionalTestHelper.post(
                baseUrl() + "/partner/promo/cashback/custom?partnerId=" + partnerId + "&businessId=" + businessId,
                new HttpEntity<>(body, getDefaultHeaders())
        );
    }

    ResponseEntity<String> getCustomCashback(long partnerId, long businessId) {
        return FunctionalTestHelper.get(baseUrl() + "/partner/promo/cashback/custom?" +
                "partnerId=" + partnerId + "&businessId=" + businessId);
    }

    ResponseEntity<String> getCustomCashback(long partnerId, long businessId, String ssku) {
        return FunctionalTestHelper.get(baseUrl() + "/partner/promo/cashback/custom?" +
                "partnerId=" + partnerId + "&businessId=" + businessId + "&ssku=" + ssku);
    }
}

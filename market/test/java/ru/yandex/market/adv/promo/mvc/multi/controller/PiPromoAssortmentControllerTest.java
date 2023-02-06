package ru.yandex.market.adv.promo.mvc.multi.controller;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import Market.DataCamp.API.DatacampMessageOuterClass;
import Market.DataCamp.DataCampOffer;
import Market.DataCamp.DataCampOfferPromos;
import Market.DataCamp.DataCampPromo;
import Market.DataCamp.SyncAPI.SyncGetPromo;
import org.apache.commons.lang3.tuple.Pair;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.skyscreamer.jsonassert.JSONAssert;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;

import ru.yandex.market.adv.promo.FunctionalTest;
import ru.yandex.market.adv.promo.logbroker.model.DatacampMessageLogbrokerEvent;
import ru.yandex.market.adv.promo.utils.CommonTestUtils;
import ru.yandex.market.adv.promo.utils.model.BasicAndServiceOffersPair;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.common.test.spring.FunctionalTestHelper;
import ru.yandex.market.logbroker.LogbrokerEventPublisher;
import ru.yandex.market.mbi.datacamp.stroller.DataCampClient;
import ru.yandex.market.mbi.datacamp.stroller.model.GetPromoBatchRequestWithFilters;
import ru.yandex.market.mbi.datacamp.stroller.model.PromoDatacampRequest;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static ru.yandex.market.adv.promo.datacamp.utils.DatacampOffersUtils.powToIdx;
import static ru.yandex.market.adv.promo.utils.CheapestAsGiftMechanicTestUtils.createAnaplanCheapestAsGiftDescription;
import static ru.yandex.market.adv.promo.utils.CommonTestUtils.getResource;
import static ru.yandex.market.adv.promo.utils.DataCampOfferUtils.createBasicOffer;
import static ru.yandex.market.adv.promo.utils.DataCampOfferUtils.createDirectDiscountPromo;
import static ru.yandex.market.adv.promo.utils.DataCampOfferUtils.createPromo;
import static ru.yandex.market.adv.promo.utils.DataCampOfferUtils.createServiceOfferWithPromos;
import static ru.yandex.market.adv.promo.utils.DataCampOfferUtils.createUnitedOffersBatchResponse;
import static ru.yandex.market.adv.promo.utils.DirectDiscountMechanicTestUtils.createAnaplanDirectDiscountDescription;

class PiPromoAssortmentControllerTest extends FunctionalTest {

    @Autowired
    private DataCampClient dataCampClient;

    @Autowired
    private LogbrokerEventPublisher<DatacampMessageLogbrokerEvent> logbrokerService;

    @BeforeEach
    public void beforeEach() {
        doReturn(
                SyncGetPromo.GetPromoBatchResponse.getDefaultInstance()
        ).when(dataCampClient).getPromos(any(PromoDatacampRequest.class));
        doReturn(
                SyncGetPromo.GetPromoBatchResponse.getDefaultInstance()
        ).when(dataCampClient).getPromos(any(GetPromoBatchRequestWithFilters.class));
    }

    @Test
    @DbUnitDataSet(
            before = "PiPromoAssortmentControllerTest/createOfferDiscountPromoTest.before.csv",
            after = "PiPromoAssortmentControllerTest/createOfferDiscountPromoTest.after.csv"
    )
    @DisplayName("Успешное добавление ассортимента в скидочную акцию")
    void createOfferDiscountPromoTest() {
        int businessId = 8967;
        int partnerId = 9090;
        String promoId = "#2020";
        int categoryId = 1456;
        String id1 = "id1";
        String id2 = "id2";

        Map<String, List<DataCampOfferPromos.Promo>> allAnaplanPromosByOfferId = Map.of(
                id1, List.of(createDirectDiscountPromo(promoId, null, 1000L)),
                id2, List.of(createDirectDiscountPromo(promoId, null, 1500L), createPromo("#1111"))
        );
        mockOffersRequest(
                businessId, partnerId, Set.of(id1, id2), allAnaplanPromosByOfferId, Map.of(), categoryId
        );
        discountPromo_mockGetPromosRequest(promoId, categoryId);

        String requestBody = CommonTestUtils.getResource(this.getClass(), "createOfferDiscountPromoTest_request.json");
        ResponseEntity<String> response = sendUpdateOfferPromoRequest(partnerId, businessId, requestBody);
        assertEquals(HttpStatus.OK, response.getStatusCode());

        ArgumentCaptor<DatacampMessageLogbrokerEvent> eventCaptor =
                ArgumentCaptor.forClass(DatacampMessageLogbrokerEvent.class);
        verify(logbrokerService, times(1)).publishEvent(eventCaptor.capture());
        DatacampMessageLogbrokerEvent event = eventCaptor.getValue();
        DatacampMessageOuterClass.DatacampMessage msg = event.getPayload();
        List<DataCampOffer.Offer> offers = msg.getOffersList().get(0).getOfferList();
        assertEquals(2, offers.size());

        Map<String, DataCampOffer.Offer> offerById = offers.stream()
                .collect(
                        Collectors.toMap(
                                offer -> offer.getIdentifiers().getOfferId(),
                                Function.identity()
                        )
                );
        DataCampOffer.Offer offer1 = offerById.get(id1);
        DataCampOffer.Offer offer2 = offerById.get(id2);
        assertTrue(offer1.getPromos().getAnaplanPromos().hasActivePromos());
        assertTrue(offer2.getPromos().getAnaplanPromos().hasActivePromos());
        assertEquals(1, offer1.getPromos().getAnaplanPromos().getActivePromos().getPromosCount());
        assertEquals(1, offer2.getPromos().getAnaplanPromos().getActivePromos().getPromosCount());
        assertEquals(promoId, offer1.getPromos().getAnaplanPromos().getActivePromos().getPromos(0).getId());
        assertEquals(promoId, offer2.getPromos().getAnaplanPromos().getActivePromos().getPromos(0).getId());
        assertTrue(offer1.getPromos().getAnaplanPromos().getActivePromos().getPromos(0).hasDirectDiscount());
        assertTrue(offer2.getPromos().getAnaplanPromos().getActivePromos().getPromos(0).hasDirectDiscount());

        DataCampOfferPromos.Promo ddPromo1 =
                offer1.getPromos().getAnaplanPromos().getActivePromos().getPromos(0);
        assertEquals(powToIdx(BigDecimal.valueOf(300)), ddPromo1.getDirectDiscount().getPrice().getPrice());
        assertEquals(powToIdx(BigDecimal.valueOf(800)), ddPromo1.getDirectDiscount().getBasePrice().getPrice());
        DataCampOfferPromos.Promo ddPromo2 =
                offer2.getPromos().getAnaplanPromos().getActivePromos().getPromos(0);
        assertEquals(powToIdx(BigDecimal.valueOf(1000)), ddPromo2.getDirectDiscount().getPrice().getPrice());
        assertEquals(powToIdx(BigDecimal.valueOf(2000)), ddPromo2.getDirectDiscount().getBasePrice().getPrice());
    }

    @Test
    @DbUnitDataSet(
            before = "PiPromoAssortmentControllerTest/createOfferDiscountPromoWithValidationErrorTest.before.csv",
            after = "PiPromoAssortmentControllerTest/createOfferDiscountPromoWithValidationErrorTest.after.csv"
    )
    @DisplayName("Добавление ассортимента в скидочную акцию с ошибками валидации")
    void createOfferDiscountPromoWithValidationErrorTest() {
        int businessId = 8967;
        int partnerId = 9090;
        String promoId = "#2020";
        int categoryId = 1456;
        String id1 = "id1";
        String id2 = "id2";
        String id4 = "id4";

        Map<String, List<DataCampOfferPromos.Promo>> allAnaplanPromosByOfferId = Map.of(
                id1, List.of(createDirectDiscountPromo(promoId, null, 800L)),
                id2, List.of(createDirectDiscountPromo(promoId, null, 1000L))
        );
        mockOffersRequest(
                businessId, partnerId, Set.of(id1, id2, id4), allAnaplanPromosByOfferId, Map.of(), categoryId
        );
        discountPromo_mockGetPromosRequest(promoId, categoryId);

        String requestBody = CommonTestUtils.getResource(
                this.getClass(),
                "createOfferDiscountPromoWithValidationErrorTest_request.json"
        );
        ResponseEntity<String> response = sendUpdateOfferPromoRequest(partnerId, businessId, requestBody);
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    private void discountPromo_mockGetPromosRequest(String targetPromoId, long categoryId) {
        mockGetPromosRequest(
                targetPromoId,
                createAnaplanDirectDiscountDescription(
                        targetPromoId,
                        Set.of(Pair.of(categoryId, 35))
                )
        );
    }

    @Test
    @DbUnitDataSet(
            before = "PiPromoAssortmentControllerTest/updateOfferNonDiscountPromoTest.before.csv",
            after = "PiPromoAssortmentControllerTest/updateOfferNonDiscountPromoTest.after.csv"
    )
    @DisplayName("Успешное обновление ассортимента в нескидочной акции")
    void updateOfferNonDiscountPromoTest() {
        int businessId = 7890;
        int partnerId = 5050;
        String promoId = "#3030";
        String id1 = "id1";
        String id2 = "id2";
        String id3 = "id3";

        Map<String, List<DataCampOfferPromos.Promo>> allAnaplanPromosByOfferId = Map.of(
                id1, List.of(createPromo(promoId)),
                id2, List.of(createPromo(promoId)),
                id3, List.of(createPromo(promoId))
        );
        Map<String, List<DataCampOfferPromos.Promo>> activeAnaplanPromosByOfferId = Map.of(
                id3, List.of(createPromo(promoId))
        );
        mockOffersRequest(
                businessId, partnerId, Set.of(id1, id2, id3),
                allAnaplanPromosByOfferId, activeAnaplanPromosByOfferId, null
        );
        nonDiscountPromo_mockGetPromosRequest(promoId);

        String requestBody =
                CommonTestUtils.getResource(this.getClass(), "updateOfferNonDiscountPromoTest_request.json");
        ResponseEntity<String> response = sendUpdateOfferPromoRequest(partnerId, businessId, requestBody);
        assertEquals(HttpStatus.OK, response.getStatusCode());

        ArgumentCaptor<DatacampMessageLogbrokerEvent> eventCaptor =
                ArgumentCaptor.forClass(DatacampMessageLogbrokerEvent.class);
        verify(logbrokerService, times(1)).publishEvent(eventCaptor.capture());
        DatacampMessageLogbrokerEvent event = eventCaptor.getValue();
        DatacampMessageOuterClass.DatacampMessage msg = event.getPayload();
        List<DataCampOffer.Offer> offers = msg.getOffersList().get(0).getOfferList();
        assertEquals(3, offers.size());

        Map<String, DataCampOffer.Offer> offerById = offers.stream()
                .collect(
                        Collectors.toMap(
                                offer -> offer.getIdentifiers().getOfferId(),
                                Function.identity()
                        )
                );
        DataCampOffer.Offer offer1 = offerById.get(id1);
        DataCampOffer.Offer offer2 = offerById.get(id2);
        DataCampOffer.Offer offer3 = offerById.get(id3);
        assertTrue(offer1.getPromos().getAnaplanPromos().hasActivePromos());
        assertTrue(offer2.getPromos().getAnaplanPromos().hasActivePromos());
        assertTrue(offer3.getPromos().getAnaplanPromos().hasActivePromos());
        assertEquals(1, offer1.getPromos().getAnaplanPromos().getActivePromos().getPromosCount());
        assertEquals(0, offer2.getPromos().getAnaplanPromos().getActivePromos().getPromosCount());
        assertEquals(1, offer3.getPromos().getAnaplanPromos().getActivePromos().getPromosCount());
        assertEquals(promoId, offer1.getPromos().getAnaplanPromos().getActivePromos().getPromos(0).getId());
        assertEquals(promoId, offer3.getPromos().getAnaplanPromos().getActivePromos().getPromos(0).getId());
    }

    private void nonDiscountPromo_mockGetPromosRequest(String targetPromoId) {
        mockGetPromosRequest(targetPromoId, createAnaplanCheapestAsGiftDescription(targetPromoId));
    }

    private void mockGetPromosRequest(String targetPromoId, DataCampPromo.PromoDescription promoDescription) {
        doReturn(
                SyncGetPromo.GetPromoBatchResponse.newBuilder()
                        .setPromos(
                                DataCampPromo.PromoDescriptionBatch.newBuilder()
                                        .addPromo(promoDescription)
                                        .build()
                        )
                        .build()
        ).when(dataCampClient).getPromos((GetPromoBatchRequestWithFilters)
                argThat(arg -> {
                    GetPromoBatchRequestWithFilters requestWithFilters = ((GetPromoBatchRequestWithFilters) arg);
                    return requestWithFilters.getRequest().getEntriesCount() == 1 &&
                            targetPromoId.equals(requestWithFilters.getRequest().getEntries(0).getPromoId());
                })
        );
    }

    private void mockOffersRequest(
            int businessId,
            int partnerId,
            Set<String> offerIds,
            Map<String, List<DataCampOfferPromos.Promo>> allAnaplanPromosByOfferId,
            Map<String, List<DataCampOfferPromos.Promo>> activeAnaplanPromosByOfferId,
            Integer categoryId
    ) {
        List<BasicAndServiceOffersPair> basicAndServiceOfferPairs = offerIds.stream().map(offerId -> {
            DataCampOffer.Offer basicOffer = createBasicOffer(offerId, partnerId, businessId, categoryId);
            DataCampOffer.Offer serviceOffer = createServiceOfferWithPromos(
                    basicOffer.getIdentifiers(),
                    allAnaplanPromosByOfferId.get(offerId),
                    activeAnaplanPromosByOfferId.get(offerId),
                    null, null
            );
            return new BasicAndServiceOffersPair(basicOffer, serviceOffer);
        }).collect(Collectors.toList());
        doReturn(createUnitedOffersBatchResponse(partnerId, basicAndServiceOfferPairs))
                .when(dataCampClient)
                .getBusinessUnitedOffers(
                        eq(Long.valueOf(businessId)),
                        anyCollection(),
                        eq(Long.valueOf(partnerId))
                );
    }

    @Test
    @DbUnitDataSet(before = "PiPromoAssortmentControllerTest/getSuccessfulProcessingStatus.before.csv")
    @DisplayName("Получение статуса для успешного выполненного запроса")
    void getSuccessfulProcessingStatus() {
        ResponseEntity<String> response = getProcessingStatusRequest(1234, 2345, "processingId2");
        assertEquals(HttpStatus.OK, response.getStatusCode());
        JSONAssert.assertEquals("{\"status\": \"COMMITTED\"}", response.getBody(), true);
    }

    @Test
    @DbUnitDataSet(before = "PiPromoAssortmentControllerTest/getErrorProcessingStatus.before.csv")
    @DisplayName("Получение статуса для запроса, завершившегося с ошибкой")
    void getErrorProcessingStatus() {
        ResponseEntity<String> response = getProcessingStatusRequest(1111, 2222, "processingId");
        assertEquals(HttpStatus.OK, response.getStatusCode());

        String expected = getResource(this.getClass(), "getErrorProcessingStatus_response.json");
        JSONAssert.assertEquals(expected, response.getBody(), false);
    }

    @Test
    @DbUnitDataSet(before = "PiPromoAssortmentControllerTest/getNonexistentProcessingStatus.before.csv")
    @DisplayName("Получение статуса для несуществующего запроса")
    void getNonexistentProcessingStatus() {
        Assertions.assertThrows(
                HttpClientErrorException.class,
                () -> getProcessingStatusRequest(1111, 2222, "nonexistentProcessingId")
        );
    }

    @Test
    @DbUnitDataSet(
            before = "PiPromoAssortmentControllerTest/getOffersWithMultiConflicts_wrongProcessingStatusTest.before.csv"
    )
    @DisplayName("Некоректный статус процессинга при получении офферов с конфликтами мульти")
    void getOffersWithMultiConflicts_wrongProcessingStatusTest() {
        var err = Assertions.assertThrows(
                HttpClientErrorException.class,
                () -> getOffersWithMultiConflictsRequest(1111, 2222, "processingId", 0, 20)
        );
        assertTrue(
                err.getResponseBodyAsString().contains(
                        "Wrong processing status, should be MULTI_CONFLICTS " +
                                "[processingId=processingId, status=ERROR_VALIDATION_FAILED]"
                )
        );
    }

    @Test
    @DbUnitDataSet(
            before = "PiPromoAssortmentControllerTest/getOffersWithMultiConflictsTest.before.csv"
    )
    @DisplayName("Получение списка офферов с мульти с пагинацией")
    void getOffersWithMultiConflictsTest() {
        long partnerId = 1234;
        long businessId = 2345;
        String processingId = "processingId";
        int pageNum = 0;
        int count = 2;
        ResponseEntity<String> response =
                getOffersWithMultiConflictsRequest(partnerId, businessId, processingId, pageNum, count);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        String expected = getResource(this.getClass(), "getOffersWithMultiConflicts_1_response.json");
        JSONAssert.assertEquals(expected, response.getBody(), false);

        pageNum++;
        response =
                getOffersWithMultiConflictsRequest(partnerId, businessId, processingId, pageNum, count);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        expected = getResource(this.getClass(), "getOffersWithMultiConflicts_2_response.json");
        JSONAssert.assertEquals(expected, response.getBody(), false);

        pageNum++;
        response =
                getOffersWithMultiConflictsRequest(partnerId, businessId, processingId, pageNum, count);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        expected = getResource(this.getClass(), "getOffersWithMultiConflicts_3_response.json");
        JSONAssert.assertEquals(expected, response.getBody(), false);
    }

    @Test
    @DbUnitDataSet(
            before = "PiPromoAssortmentControllerTest/solveConflictsTest.before.csv",
            after = "PiPromoAssortmentControllerTest/solveConflictsTest.after.csv"
    )
    @DisplayName("Проверка ручки разрешения конфликтов")
    void solveConflictsTest() {
        long partnerId = 1010;
        long businessId = 1111;
        String promoId = "#1234";
        String id1 = "id1";
        String id2 = "id2";
        String id3 = "id3";

        Map<String, List<DataCampOfferPromos.Promo>> activeAnaplanPromosByOfferId = Map.of(
                id1, List.of(createPromo(promoId), createPromo("#1223"), createPromo("#9999")),
                id2, List.of(createPromo(promoId))
        );
        mockOffersRequest(
                (int) businessId, (int) partnerId, Set.of(id1, id2, id3),
                Collections.emptyMap(), activeAnaplanPromosByOfferId, null
        );

        String requestBody =
                CommonTestUtils.getResource(this.getClass(), "solveConflictsTest_request.json");
        ResponseEntity<String> response = solveConflictsAndCommitOffersRequest(partnerId, businessId, requestBody);
        assertEquals(HttpStatus.OK, response.getStatusCode());

        ArgumentCaptor<DatacampMessageLogbrokerEvent> eventCaptor =
                ArgumentCaptor.forClass(DatacampMessageLogbrokerEvent.class);
        verify(logbrokerService, times(1)).publishEvent(eventCaptor.capture());
        DatacampMessageLogbrokerEvent event = eventCaptor.getValue();
        DatacampMessageOuterClass.DatacampMessage msg = event.getPayload();
        List<DataCampOffer.Offer> offers = msg.getOffersList().get(0).getOfferList();
        assertEquals(3, offers.size());

        Map<String, DataCampOffer.Offer> offerById = offers.stream()
                .collect(
                        Collectors.toMap(
                                offer -> offer.getIdentifiers().getOfferId(),
                                Function.identity()
                        )
                );

        DataCampOffer.Offer offer1 = offerById.get(id1);
        assertTrue(offer1.getPromos().getAnaplanPromos().getActivePromos().hasMeta());
        MatcherAssert.assertThat(
                offer1.getPromos().getAnaplanPromos().getActivePromos().getPromosList(),
                containsInAnyOrder(createPromo(promoId), createPromo("#1223"))
        );

        DataCampOffer.Offer offer2 = offerById.get(id2);
        assertTrue(offer2.getPromos().getAnaplanPromos().getActivePromos().hasMeta());
        MatcherAssert.assertThat(
                offer2.getPromos().getAnaplanPromos().getActivePromos().getPromosList(),
                empty()
        );

        DataCampOffer.Offer offer3 = offerById.get(id3);
        assertTrue(offer3.getPromos().getAnaplanPromos().getActivePromos().hasMeta());
        MatcherAssert.assertThat(
                offer3.getPromos().getAnaplanPromos().getActivePromos().getPromosList(),
                containsInAnyOrder(createPromo(promoId))
        );
    }

    private ResponseEntity<String> sendUpdateOfferPromoRequest(long partnerId, long businessId, String body) {
        return FunctionalTestHelper.post(
                baseUrl() + "/partner/promo/multi/offers?partnerId=" + partnerId + "&businessId=" + businessId,
                new HttpEntity<>(body, getDefaultHeaders())
        );
    }

    private ResponseEntity<String> getProcessingStatusRequest(long partnerId, long businessId, String processingId) {
        return FunctionalTestHelper.get(
                baseUrl() + "/partner/promo/multi/offers/status?" +
                        "partnerId=" + partnerId + "&businessId=" + businessId + "&processingId=" + processingId
        );
    }

    private ResponseEntity<String> getOffersWithMultiConflictsRequest(
            long partnerId,
            long businessId,
            String processingId,
            int pageNum,
            int count
    ) {
        return FunctionalTestHelper.get(
                baseUrl() + "/partner/promo/multi/offers/with-conflicts?" +
                        "partnerId=" + partnerId + "&businessId=" + businessId + "&processingId=" + processingId +
                        "&pageNum=" + pageNum + "&count=" + count
        );
    }

    private ResponseEntity<String> solveConflictsAndCommitOffersRequest(
            long partnerId,
            long businessId,
            String body
    ) {
        return FunctionalTestHelper.post(
                baseUrl() + "/partner/promo/multi/offers/solve-conflicts?" +
                        "partnerId=" + partnerId + "&businessId=" + businessId,
                new HttpEntity<>(body, getDefaultHeaders())
        );
    }
}

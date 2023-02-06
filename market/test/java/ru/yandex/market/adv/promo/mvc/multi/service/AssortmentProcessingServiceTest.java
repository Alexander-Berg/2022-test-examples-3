package ru.yandex.market.adv.promo.mvc.multi.service;

import java.time.LocalDate;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import Market.DataCamp.API.DatacampMessageOuterClass;
import Market.DataCamp.DataCampOffer;
import Market.DataCamp.DataCampOfferPromos;
import Market.DataCamp.DataCampPromo;
import Market.DataCamp.SyncAPI.SyncGetPromo;
import org.apache.commons.collections.CollectionUtils;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.adv.promo.FunctionalTest;
import ru.yandex.market.adv.promo.logbroker.model.DatacampMessageLogbrokerEvent;
import ru.yandex.market.adv.promo.mvc.multi.model.assortment.AssortmentProcessingStatus;
import ru.yandex.market.adv.promo.mvc.multi.model.assortment.OfferPromo;
import ru.yandex.market.adv.promo.mvc.multi.model.assortment.ProcessingBaseInfo;
import ru.yandex.market.adv.promo.mvc.multi.model.assortment.ProcessingStatusFullInfo;
import ru.yandex.market.adv.promo.service.environment.EnvironmentService;
import ru.yandex.market.adv.promo.utils.model.BasicAndServiceOffersPair;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.logbroker.LogbrokerEventPublisher;
import ru.yandex.market.mbi.datacamp.stroller.DataCampClient;
import ru.yandex.market.mbi.datacamp.stroller.model.GetPromoBatchRequestWithFilters;
import ru.yandex.market.mbi.datacamp.stroller.model.PromoDatacampRequest;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static ru.yandex.market.adv.promo.service.environment.constant.EnvironmentSettingConstants.DATACAMP_GRAPHQL_QUERY;
import static ru.yandex.market.adv.promo.utils.CheapestAsGiftMechanicTestUtils.createAnaplanCheapestAsGiftDescription;
import static ru.yandex.market.adv.promo.utils.CustomCashbackMechanicTestUtils.createCustomCashbackPromo;
import static ru.yandex.market.adv.promo.utils.DataCampOfferUtils.createBasicOffer;
import static ru.yandex.market.adv.promo.utils.DataCampOfferUtils.createDirectDiscountPromo;
import static ru.yandex.market.adv.promo.utils.DataCampOfferUtils.createPromo;
import static ru.yandex.market.adv.promo.utils.DataCampOfferUtils.createServiceOfferWithPromos;
import static ru.yandex.market.adv.promo.utils.DataCampOfferUtils.createUnitedOffersBatchResponse;
import static ru.yandex.market.adv.promo.utils.DirectDiscountMechanicTestUtils.createAnaplanDirectDiscountDescription;
import static ru.yandex.market.adv.promo.utils.PromocodeMechanicTestUtils.createAnaplanPromocodeDescription;
import static ru.yandex.market.adv.promo.utils.PromocodeMechanicTestUtils.createPartnerPromocodeDescription;
import static ru.yandex.market.mbi.datacamp.stroller.model.graphql.OfferField.ACTIVE_ANAPLAN_PROMOS;
import static ru.yandex.market.mbi.datacamp.stroller.model.graphql.OfferField.ALL_ANAPLAN_PROMOS;
import static ru.yandex.market.mbi.datacamp.stroller.model.graphql.OfferField.ALL_PARTNER_CASHBACK_PROMOS;
import static ru.yandex.market.mbi.datacamp.stroller.model.graphql.OfferField.ALL_PARTNER_PROMOS;
import static ru.yandex.market.mbi.datacamp.stroller.model.graphql.OfferField.OFFER_CATEGORY_ID;
import static ru.yandex.market.mbi.datacamp.stroller.model.graphql.OfferField.OFFER_IDENTIFIERS;
import static ru.yandex.market.mbi.datacamp.stroller.model.graphql.OfferField.OFFER_VENDOR_ID;

class AssortmentProcessingServiceTest extends FunctionalTest {
    @Autowired
    private AssortmentProcessingService assortmentProcessingService;

    @Autowired
    private DataCampClient dataCampClient;

    @Autowired
    private LogbrokerEventPublisher<DatacampMessageLogbrokerEvent> logbrokerService;

    @Autowired
    private EnvironmentService environmentService;

    @BeforeEach
    public void beforeEach() {
        doReturn(
                SyncGetPromo.GetPromoBatchResponse.getDefaultInstance()
        ).when(dataCampClient).getPromos(any(PromoDatacampRequest.class));
        doReturn(
                SyncGetPromo.GetPromoBatchResponse.getDefaultInstance()
        ).when(dataCampClient).getPromos(any(GetPromoBatchRequestWithFilters.class));
    }

    /**
     * У оффера id1 конфликты по типам промо.
     * У оффера id2 нет конфликтов.
     * У оффера id3 нет конфликтов (макс число промок на оффере). upd: раньше тут был тест на конфликты по числу промок,
     *      но после того, как максимальное число промок на оффере подняли до четырех, у нас больше нет пяти механик,
     *      которые могли бы одновременно применяться не конфликтуя
     */
    @Test
    @DbUnitDataSet(
            before = "AssortmentProcessingServiceTest/sendValidationTask_multiConflictsTest.before.csv",
            after = "AssortmentProcessingServiceTest/sendValidationTask_multiConflictsTest.after.csv"
    )
    void sendValidationTask_multiConflictsTest() {
        multiConflictsTest(false);
    }

    @Test
    @DbUnitDataSet(
            before = "AssortmentProcessingServiceTest/sendValidationTask_multiConflictsTest_graphqlQuery.before.csv",
            after = "AssortmentProcessingServiceTest/sendValidationTask_multiConflictsTest.after.csv"
    )
    void sendValidationTaskGraphqlQuery_multiConflictsTest() {
        multiConflictsTest(false);
    }

    @Test
    @DbUnitDataSet(
            before = "AssortmentProcessingServiceTest/sendValidationTask_multiConflictsTest.before.csv",
            after = "AssortmentProcessingServiceTest/sendValidationTask_multiConflictsTest.after.csv"
    )
    void restartValidation_multiConflictsTest() {
        multiConflictsTest(true);
    }

    @Test
    @DbUnitDataSet(
            before = "AssortmentProcessingServiceTest/" +
                    "sendValidationTask_multiConflictsTest_graphqlQuery.before.csv",
            after = "AssortmentProcessingServiceTest/sendValidationTask_multiConflictsTest.after.csv"
    )
    void restartValidationGraphqlQuery_multiConflictsTest() {
        multiConflictsTest(true);
    }

    private void restartValidation_multiConflicts() {
        multiConflictsTest(true);
    }

    private void multiConflictsTest(boolean validationRestart) {
        int businessId = 1111;
        int partnerId = 1010;
        String promoId = "#1234";
        String id1 = "id1";
        String id2 = "id2";
        String id3 = "id3";
        String partnerPromocodeId = "1010_MPC_12345";
        String anaplanCAG = "#1111";
        String anaplanDD = "#2222";
        String partnerCCP = "1010_PCC_12345";

        Map<String, List<DataCampOfferPromos.Promo>> allAnaplanPromosByOfferId = Map.of(
                id1,
                List.of(
                        createPromo(promoId)
                ),
                id2,
                List.of(
                        createPromo(promoId),
                        createPromo(anaplanCAG)
                ),
                id3,
                List.of(
                        createPromo(promoId),
                        createPromo(anaplanCAG),
                        createDirectDiscountPromo(anaplanDD, null, 1500L)
                )
        );
        Map<String, List<DataCampOfferPromos.Promo>> activeAnaplanPromosByOfferId = Map.of(
                id2,
                List.of(
                        createPromo(anaplanCAG)
                ),
                id3,
                List.of(
                        createPromo(promoId),
                        createPromo(anaplanCAG),
                        createDirectDiscountPromo(anaplanDD, 800L, 2500L)
                )
        );
        Map<String, List<DataCampOfferPromos.Promo>> partnerPromosByOfferId = Map.of(
                id1,
                List.of(
                        createPromo(partnerPromocodeId)
                )
        );
        Map<String, List<DataCampOfferPromos.Promo>> cashbackPromosByOfferId = Map.of(
                id3,
                List.of(
                        createPromo(partnerCCP)
                )
        );

        if (environmentService.getSettingsBooleanValue(DATACAMP_GRAPHQL_QUERY).orElse(false)) {
            mockOffersRequestGraphqlQuery(
                    businessId,
                    partnerId,
                    Set.of(id1, id2, id3),
                    allAnaplanPromosByOfferId,
                    activeAnaplanPromosByOfferId,
                    partnerPromosByOfferId,
                    cashbackPromosByOfferId
            );
        } else {
            mockOffersRequest(
                    businessId,
                    partnerId,
                    Set.of(id1, id2, id3),
                    allAnaplanPromosByOfferId,
                    activeAnaplanPromosByOfferId,
                    partnerPromosByOfferId,
                    cashbackPromosByOfferId
            );
        }
        targetPromo_mokGetPromosRequest(promoId);
        otherActivePromos_mokGetPromosRequest(partnerPromocodeId, anaplanCAG, anaplanDD, partnerCCP);

        if (!validationRestart) {
            assortmentProcessingService.sendValidationTask(
                    new ProcessingBaseInfo("processingIdTest", businessId, partnerId, promoId),
                    List.of(
                            new OfferPromo(id1, true, null, null),
                            new OfferPromo(id2, true, null, null),
                            new OfferPromo(id3, true, null, null)
                    )
            );
        } else {
            assortmentProcessingService.restartValidation();
        }
    }

    private void multiConflictsGraphqlQueryTest(boolean validationRestart) {
        int businessId = 1111;
        int partnerId = 1010;
        String promoId = "#1234";
        String id1 = "id1";
        String id2 = "id2";
        String id3 = "id3";
        String partnerPromocodeId = "1010_MPC_12345";
        String anaplanCAG = "#1111";
        String anaplanDD = "#2222";
        String partnerCCP = "1010_PCC_12345";

        mockOffersRequestGraphqlQuery(
                businessId,
                partnerId,
                Set.of(id1, id2, id3),
                Map.of(
                        id1,
                        List.of(
                                createPromo(promoId)
                        ),
                        id2,
                        List.of(
                                createPromo(promoId),
                                createPromo(anaplanCAG)
                        ),
                        id3,
                        List.of(
                                createPromo(promoId),
                                createPromo(anaplanCAG),
                                createDirectDiscountPromo(anaplanDD, null, 1500L)
                        )
                ),
                Map.of(
                        id2,
                        List.of(
                                createPromo(anaplanCAG)
                        ),
                        id3,
                        List.of(
                                createPromo(promoId),
                                createPromo(anaplanCAG),
                                createDirectDiscountPromo(anaplanDD, 800L, 2500L)
                        )
                ),
                Map.of(
                        id1,
                        List.of(
                                createPromo(partnerPromocodeId)
                        )
                ),
                Map.of(
                        id3,
                        List.of(
                                createPromo(partnerCCP)
                        )
                )
        );
        targetPromo_mokGetPromosRequest(promoId);
        otherActivePromos_mokGetPromosRequest(partnerPromocodeId, anaplanCAG, anaplanDD, partnerCCP);

        if (!validationRestart) {
            assortmentProcessingService.sendValidationTask(
                    new ProcessingBaseInfo("processingIdTest", businessId, partnerId, promoId),
                    List.of(
                            new OfferPromo(id1, true, null, null),
                            new OfferPromo(id2, true, null, null),
                            new OfferPromo(id3, true, null, null)
                    )
            );
        } else {
            assortmentProcessingService.restartValidation();
        }
    }

    private void targetPromo_mokGetPromosRequest(String targetPromoId) {
        doReturn(
                SyncGetPromo.GetPromoBatchResponse.newBuilder()
                        .setPromos(
                                DataCampPromo.PromoDescriptionBatch.newBuilder()
                                        .addPromo(createAnaplanPromocodeDescription(
                                                targetPromoId, LocalDate.of(2022, 1, 1), LocalDate.of(2022, 12, 31)
                                        ))
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

    private void otherActivePromos_mokGetPromosRequest(
            String partnerPromocodeId,
            String anaplanCAG,
            String anaplanDD,
            String partnerCCP
    ) {
        doReturn(
                SyncGetPromo.GetPromoBatchResponse.newBuilder()
                        .setPromos(
                                DataCampPromo.PromoDescriptionBatch.newBuilder()
                                        .addPromo(createPartnerPromocodeDescription(
                                                partnerPromocodeId, LocalDate.of(2021, 12, 1), LocalDate.of(2022, 2, 1)
                                        ))
                                        .addPromo(createAnaplanCheapestAsGiftDescription(
                                                anaplanCAG, LocalDate.of(2022, 1, 1), LocalDate.of(2023, 1, 1)
                                        ))
                                        .addPromo(createAnaplanDirectDiscountDescription(
                                                anaplanDD, LocalDate.of(2022, 1, 1), LocalDate.of(2023, 1, 1)
                                        ))
                                        .addPromo(createCustomCashbackPromo(
                                                partnerCCP, LocalDate.of(2022, 5, 1), LocalDate.of(2022, 6, 1)
                                        ))
                                        .build()
                        )
                        .build()
        ).when(dataCampClient).getPromos((GetPromoBatchRequestWithFilters)
                argThat(arg -> {
                    GetPromoBatchRequestWithFilters requestWithFilters = ((GetPromoBatchRequestWithFilters) arg);
                    if (requestWithFilters.getRequest().getEntriesCount() != 4) {
                        return false;
                    }
                    Set<String> requestedIds = requestWithFilters.getRequest().getEntriesList().stream()
                            .map(DataCampPromo.PromoDescriptionIdentifier::getPromoId)
                            .collect(Collectors.toSet());
                    return requestedIds.containsAll(Set.of(partnerPromocodeId, anaplanCAG, anaplanDD, partnerCCP));
                })
        );
    }

    @Test
    @DbUnitDataSet(
            before = "AssortmentProcessingServiceTest/solveMultiConflicts_internalErrorTest.before.csv",
            after = "AssortmentProcessingServiceTest/solveMultiConflicts_internalErrorTest.after.csv"
    )
    void solveMultiConflicts_internalErrorTest() {
        solveMultiConflicts_internalError();
    }

    @Test
    @DbUnitDataSet(
            before = "AssortmentProcessingServiceTest/solveMultiConflicts_internalErrorTest_graphqlQuery.before.csv",
            after = "AssortmentProcessingServiceTest/solveMultiConflicts_internalErrorTest.after.csv"
    )
    void solveMultiConflictsGraphqlQuery_internalErrorTest() {
        solveMultiConflicts_internalError();
    }

    private void solveMultiConflicts_internalError() {
        long businessId = 1111;
        long partnerId = 1010;
        String processingId = "processingIdTest";

        String errTest = "Test internalError exception";
        if (environmentService.getSettingsBooleanValue(DATACAMP_GRAPHQL_QUERY).orElse(false)) {
            when(dataCampClient.getBusinessUnitedOffers(
                    eq(businessId),
                    anyCollection(),
                    eq(partnerId),
                    argThat(offerQuery -> CollectionUtils.isEqualCollection(
                            offerQuery.getFields(), EnumSet.of(
                                    OFFER_IDENTIFIERS,
                                    ACTIVE_ANAPLAN_PROMOS,
                                    ALL_PARTNER_PROMOS
                            )))
            ))
                    .thenThrow(new RuntimeException(errTest));
        } else {
            when(dataCampClient.getBusinessUnitedOffers(eq(businessId), anyCollection(), eq(partnerId)))
                    .thenThrow(new RuntimeException(errTest));
        }
        ProcessingStatusFullInfo status = assortmentProcessingService.solveMultiConflicts(
                new ProcessingBaseInfo(processingId, businessId, partnerId, "#1234"),
                DataCampPromo.PromoType.MARKET_PROMOCODE,
                Collections.emptyMap()
        );
        assertEquals(AssortmentProcessingStatus.ERROR_INTERNAL, status.getStatus());
        assertTrue(status.getErrorDetails().contains(errTest));

        Optional<ProcessingStatusFullInfo> statusInfo =
                assortmentProcessingService.getProcessingStatusInfo(processingId, partnerId, businessId);
        assertTrue(statusInfo.isPresent());
        assertEquals(AssortmentProcessingStatus.ERROR_INTERNAL, statusInfo.get().getStatus());
        assertTrue(statusInfo.get().getErrorDetails().contains(errTest));
    }

    /**
     * id1 - добавляется в акцию   - есть конфликты мульти - есть удаляющиеся промки (анаплановские и партнерские)
     * id2 - добавляется в акцию   - есть конфликты мульти - нет удаляющихся промок
     * id3 - добавляется в акцию   - нет конфликтов
     * id4 - удаляется из акции
     * id5 - редактируется в акции - есть конфликты мульти - есть удаляющиеся промки (только партнерские)
     * id6 - редактируется в акции - есть конфликты мульти - нет удаляющихся промок
     * id7 - редактируется в акции - нет конфликтов
     * id8 - добавляется в акцию   - есть конфликты мульти - есть удаляющиеся промки (только анаплановские)
     */
    @Test
    @DbUnitDataSet(
            before = "AssortmentProcessingServiceTest/solveMultiConflicts_okTest.before.csv",
            after = "AssortmentProcessingServiceTest/solveMultiConflicts_okTest.after.csv"
    )
    void solveMultiConflicts_okTest() {
        solveMultiConflicts_ok();
    }

    @Test
    @DbUnitDataSet(
            before = "AssortmentProcessingServiceTest/solveMultiConflicts_okTest_graphqlQuery.before.csv",
            after = "AssortmentProcessingServiceTest/solveMultiConflicts_okTest.after.csv"
    )
    void solveMultiConflictsGraphqlQuery_okTest() {
        solveMultiConflicts_ok();
    }

    @SuppressWarnings("checkstyle:MethodLength")
    private void solveMultiConflicts_ok() {
        long businessId = 1111;
        long partnerId = 1010;
        String processingId = "processingIdTest";
        String targetPromoId = "#1234";
        String id1 = "id1";
        String id2 = "id2";
        String id3 = "id3";
        String id4 = "id4";
        String id5 = "id5";
        String id6 = "id6";
        String id7 = "id7";
        String id8 = "id8";

        Map<String, List<DataCampOfferPromos.Promo>> activeAnaplanPromosByOfferId = Map.of(
                id1, List.of(
                        createDirectDiscountPromo("#1111", 600L, 1200L),
                        createPromo("#2222"),
                        createPromo("#3333")
                ),
                id3, List.of(
                        createPromo("#2222"),
                        createPromo("#3333")
                ),
                id4, List.of(
                        createDirectDiscountPromo("#1111", 900L, 5000L),
                        createPromo("#2222"),
                        createDirectDiscountPromo(targetPromoId, 800L, 3000L)
                ),
                id5, List.of(
                        createPromo("#3333"),
                        createPromo("#4444"),
                        createDirectDiscountPromo(targetPromoId, 400L, 450L)
                ),
                id6, List.of(
                        createDirectDiscountPromo("#1111", 550L, 800L),
                        createPromo("#3333"),
                        createPromo("#4444"),
                        createPromo("#5555"),
                        createDirectDiscountPromo(targetPromoId, 30L, 450L)
                ),
                id7, List.of(
                        createDirectDiscountPromo(targetPromoId, 30L, 450L)
                ),
                id8, List.of(
                        createDirectDiscountPromo("#1111", 650L, 900L),
                        createPromo("#2222"),
                        createPromo("#4444"),
                        createPromo("#5555")
                )
        );
        Map<String, List<DataCampOfferPromos.Promo>> partnerPromosByOfferId = Map.of(
                id1, List.of(
                        createPromo("1010_MPC_1234"),
                        createPromo("1010_MPC_2345"),
                        createPromo("1010_CAG_3456"),
                        createPromo("1010_CAG_4567")
                ),
                id2, List.of(
                        createPromo("1010_MPC_1234"),
                        createPromo("1010_MPC_2345"),
                        createPromo("1010_CAG_3456"),
                        createPromo("1010_CAG_4567")
                ),
                id4, List.of(
                        createPromo("1010_MPC_1234")
                ),
                id5, List.of(
                        createPromo("1010_MPC_2345"),
                        createPromo("1010_CAG_3456"),
                        createPromo("1010_CAG_4567")
                )
        );
        if (environmentService.getSettingsBooleanValue(DATACAMP_GRAPHQL_QUERY).orElse(false)) {
            mockOffersRequestMultiConflictsGraphqlQuery(
                    (int) businessId,
                    (int) partnerId,
                    Set.of(id1, id2, id3, id4, id5, id6, id7, id8),
                    Map.of(),
                    activeAnaplanPromosByOfferId,
                    partnerPromosByOfferId,
                    Map.of()
            );
        } else {
            mockOffersRequest(
                    (int) businessId,
                    (int) partnerId,
                    Set.of(id1, id2, id3, id4, id5, id6, id7, id8),
                    Map.of(),
                    activeAnaplanPromosByOfferId,
                    partnerPromosByOfferId,
                    Map.of()
            );
        }

        ProcessingStatusFullInfo status = assortmentProcessingService.solveMultiConflicts(
                new ProcessingBaseInfo(processingId, businessId, partnerId, "#1234"),
                DataCampPromo.PromoType.DIRECT_DISCOUNT,
                Map.of(
                        id1, Set.of("#1111", "#3333", "1010_MPC_2345", "1010_MPC_1234"),
                        id5, Set.of("1010_MPC_2345", "1010_CAG_4567", "1010_CAG_3456"),
                        id8, Set.of("#1111", "#2222"),
                        id4, Set.of()
                )
        );
        assertEquals(AssortmentProcessingStatus.COMMITTED, status.getStatus());

        ArgumentCaptor<DatacampMessageLogbrokerEvent> eventCaptor =
                ArgumentCaptor.forClass(DatacampMessageLogbrokerEvent.class);
        verify(logbrokerService, times(1)).publishEvent(eventCaptor.capture());
        DatacampMessageLogbrokerEvent event = eventCaptor.getValue();
        DatacampMessageOuterClass.DatacampMessage msg = event.getPayload();
        List<DataCampOffer.Offer> offers = msg.getOffersList().get(0).getOfferList();
        assertEquals(8, offers.size());

        Map<String, DataCampOffer.Offer> offerById = offers.stream()
                .collect(
                        Collectors.toMap(
                                offer -> offer.getIdentifiers().getOfferId(),
                                Function.identity()
                        )
                );
        DataCampOffer.Offer offer1 = offerById.get(id1);
        checkAnaplanPromos(offer1,
                createPromo("#2222"),
                createDirectDiscountPromo(targetPromoId, 500L, 1000L)
        );
        assertTrue(offer1.getPromos().hasPartnerPromos());
        assertTrue(offer1.getPromos().getPartnerPromos().hasMeta());
        MatcherAssert.assertThat(
                offer1.getPromos().getPartnerPromos().getPromosList(),
                containsInAnyOrder(
                        createPromo("1010_CAG_3456"),
                        createPromo("1010_CAG_4567")
                )
        );

        DataCampOffer.Offer offer2 = offerById.get(id2);
        checkAnaplanPromos(offer2, createDirectDiscountPromo(targetPromoId, 100L, 200L));
        assertFalse(offer2.getPromos().hasPartnerPromos());

        DataCampOffer.Offer offer3 = offerById.get(id3);
        checkAnaplanPromos(offer3,
                createPromo("#2222"),
                createPromo("#3333"),
                createDirectDiscountPromo(targetPromoId, 200L, 300L)
        );
        assertFalse(offer3.getPromos().hasPartnerPromos());

        DataCampOffer.Offer offer4 = offerById.get(id4);
        checkAnaplanPromos(offer4,
                createDirectDiscountPromo("#1111", 900L, 5000L),
                createPromo("#2222")
        );
        assertFalse(offer4.getPromos().hasPartnerPromos());

        DataCampOffer.Offer offer5 = offerById.get(id5);
        checkAnaplanPromos(offer5,
                createPromo("#3333"),
                createPromo("#4444"),
                createDirectDiscountPromo(targetPromoId, 300L, 400L)
        );
        assertTrue(offer5.getPromos().hasPartnerPromos());
        assertTrue(offer5.getPromos().getPartnerPromos().hasMeta());
        MatcherAssert.assertThat(offer5.getPromos().getPartnerPromos().getPromosList(), empty());

        DataCampOffer.Offer offer6 = offerById.get(id6);
        checkAnaplanPromos(offer6,
                createDirectDiscountPromo("#1111", 550L, 800L),
                createPromo("#3333"),
                createPromo("#4444"),
                createPromo("#5555"),
                createDirectDiscountPromo(targetPromoId, 400L, 500L)
        );
        assertFalse(offer6.getPromos().hasPartnerPromos());

        DataCampOffer.Offer offer7 = offerById.get(id7);
        checkAnaplanPromos(offer7, createDirectDiscountPromo(targetPromoId, 500L, 600L));
        assertFalse(offer7.getPromos().hasPartnerPromos());

        DataCampOffer.Offer offer8 = offerById.get(id8);
        checkAnaplanPromos(offer8,
                createPromo("#4444"),
                createPromo("#5555"),
                createDirectDiscountPromo(targetPromoId, 500L, 600L)
        );
        assertFalse(offer8.getPromos().hasPartnerPromos());
    }

    private void checkAnaplanPromos(DataCampOffer.Offer offer, DataCampOfferPromos.Promo... promos) {
        assertTrue(offer.getPromos().getAnaplanPromos().getActivePromos().hasMeta());
        MatcherAssert.assertThat(
                offer.getPromos().getAnaplanPromos().getActivePromos().getPromosList(),
                containsInAnyOrder(promos)
        );
    }

    private void mockOffersRequest(
            int businessId,
            int partnerId,
            Set<String> offerIds,
            Map<String, List<DataCampOfferPromos.Promo>> allAnaplanPromosByOfferId,
            Map<String, List<DataCampOfferPromos.Promo>> activeAnaplanPromosByOfferId,
            Map<String, List<DataCampOfferPromos.Promo>> partnerPromosByOfferId,
            Map<String, List<DataCampOfferPromos.Promo>> cashbackPromosByOfferId
    ) {
        List<BasicAndServiceOffersPair> basicAndServiceOfferPairs = offerIds.stream().map(offerId -> {
            DataCampOffer.Offer basicOffer = createBasicOffer(offerId, partnerId, businessId);
            DataCampOffer.Offer serviceOffer = createServiceOfferWithPromos(
                    basicOffer.getIdentifiers(),
                    allAnaplanPromosByOfferId.get(offerId),
                    activeAnaplanPromosByOfferId.get(offerId),
                    partnerPromosByOfferId.get(offerId),
                    cashbackPromosByOfferId.get(offerId)
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

    private void mockOffersRequestGraphqlQuery(
            int businessId,
            int partnerId,
            Set<String> offerIds,
            Map<String, List<DataCampOfferPromos.Promo>> allAnaplanPromosByOfferId,
            Map<String, List<DataCampOfferPromos.Promo>> activeAnaplanPromosByOfferId,
            Map<String, List<DataCampOfferPromos.Promo>> partnerPromosByOfferId,
            Map<String, List<DataCampOfferPromos.Promo>> cashbackPromosByOfferId
    ) {
        List<BasicAndServiceOffersPair> basicAndServiceOfferPairs = offerIds.stream().map(offerId -> {
            DataCampOffer.Offer basicOffer = createBasicOffer(offerId, partnerId, businessId);
            DataCampOffer.Offer serviceOffer = createServiceOfferWithPromos(
                    basicOffer.getIdentifiers(),
                    allAnaplanPromosByOfferId.get(offerId),
                    activeAnaplanPromosByOfferId.get(offerId),
                    partnerPromosByOfferId.get(offerId),
                    cashbackPromosByOfferId.get(offerId)
            );
            return new BasicAndServiceOffersPair(basicOffer, serviceOffer);
        }).collect(Collectors.toList());
        doReturn(createUnitedOffersBatchResponse(partnerId, basicAndServiceOfferPairs))
                .when(dataCampClient)
                .getBusinessUnitedOffers(
                        eq(Long.valueOf(businessId)),
                        anyCollection(),
                        eq(Long.valueOf(partnerId)),
                        argThat(offerQuery -> CollectionUtils.isEqualCollection(
                                offerQuery.getFields(), EnumSet.of(
                                        OFFER_IDENTIFIERS,
                                        ACTIVE_ANAPLAN_PROMOS,
                                        ALL_PARTNER_PROMOS,
                                        ALL_PARTNER_CASHBACK_PROMOS,
                                        ALL_ANAPLAN_PROMOS,
                                        OFFER_CATEGORY_ID,
                                        OFFER_VENDOR_ID
                                )))
                );
    }

    private void mockOffersRequestMultiConflictsGraphqlQuery(
            int businessId,
            int partnerId,
            Set<String> offerIds,
            Map<String, List<DataCampOfferPromos.Promo>> allAnaplanPromosByOfferId,
            Map<String, List<DataCampOfferPromos.Promo>> activeAnaplanPromosByOfferId,
            Map<String, List<DataCampOfferPromos.Promo>> partnerPromosByOfferId,
            Map<String, List<DataCampOfferPromos.Promo>> cashbackPromosByOfferId
    ) {
        List<BasicAndServiceOffersPair> basicAndServiceOfferPairs = offerIds.stream().map(offerId -> {
            DataCampOffer.Offer basicOffer = createBasicOffer(offerId, partnerId, businessId);
            DataCampOffer.Offer serviceOffer = createServiceOfferWithPromos(
                    basicOffer.getIdentifiers(),
                    allAnaplanPromosByOfferId.get(offerId),
                    activeAnaplanPromosByOfferId.get(offerId),
                    partnerPromosByOfferId.get(offerId),
                    cashbackPromosByOfferId.get(offerId)
            );
            return new BasicAndServiceOffersPair(basicOffer, serviceOffer);
        }).collect(Collectors.toList());
        doReturn(createUnitedOffersBatchResponse(partnerId, basicAndServiceOfferPairs))
                .when(dataCampClient)
                .getBusinessUnitedOffers(
                        eq(Long.valueOf(businessId)),
                        anyCollection(),
                        eq(Long.valueOf(partnerId)),
                        argThat(offerQuery -> CollectionUtils.isEqualCollection(
                                offerQuery.getFields(), EnumSet.of(
                                        OFFER_IDENTIFIERS,
                                        ACTIVE_ANAPLAN_PROMOS,
                                        ALL_PARTNER_PROMOS
                                )))
                );
    }
}

package ru.yandex.market.adv.promo.mvc.multi.service;

import java.time.LocalDate;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import Market.DataCamp.DataCampOffer;
import Market.DataCamp.DataCampOfferPromos;
import Market.DataCamp.DataCampPromo;
import Market.DataCamp.SyncAPI.OffersBatch;
import Market.DataCamp.SyncAPI.SyncGetPromo;
import org.apache.commons.collections4.CollectionUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatcher;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.adv.promo.FunctionalTest;
import ru.yandex.market.adv.promo.mvc.multi.model.assortment.OfferPromo;
import ru.yandex.market.adv.promo.mvc.multi.model.intersection.PromoConstructorInfo;
import ru.yandex.market.adv.promo.mvc.promo.promo_id.dto.PiPromoMechanicDto;
import ru.yandex.market.adv.promo.service.environment.EnvironmentService;
import ru.yandex.market.adv.promo.service.loyalty.client.LoyaltyClient;
import ru.yandex.market.adv.promo.service.loyalty.dto.MarketTariffResponse;
import ru.yandex.market.adv.promo.service.loyalty.dto.PartnerCashbackMarketTariffsResponse;
import ru.yandex.market.adv.promo.utils.CashbackMechanicTestUtils;
import ru.yandex.market.adv.promo.utils.CustomCashbackMechanicTestUtils;
import ru.yandex.market.adv.promo.utils.model.BasicAndServiceOffersPair;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.mbi.datacamp.stroller.DataCampClient;
import ru.yandex.market.mbi.datacamp.stroller.model.GetPromoBatchRequestWithFilters;
import ru.yandex.market.mbi.datacamp.stroller.model.PromoDatacampRequest;

import static org.apache.commons.collections4.CollectionUtils.isEmpty;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static ru.yandex.market.adv.promo.service.environment.constant.EnvironmentSettingConstants.DATACAMP_GRAPHQL_QUERY;
import static ru.yandex.market.adv.promo.utils.BlueFlashMechanicTestUtils.createAnaplanBlueFlashDescription;
import static ru.yandex.market.adv.promo.utils.CheapestAsGiftMechanicTestUtils.createAnaplanCheapestAsGiftDescription;
import static ru.yandex.market.adv.promo.utils.CheapestAsGiftMechanicTestUtils.createPartnerCheapestAsGiftDescription;
import static ru.yandex.market.adv.promo.utils.DataCampOfferUtils.createBasicOffer;
import static ru.yandex.market.adv.promo.utils.DataCampOfferUtils.createDirectDiscountPromo;
import static ru.yandex.market.adv.promo.utils.DataCampOfferUtils.createFlashPromo;
import static ru.yandex.market.adv.promo.utils.DataCampOfferUtils.createPromo;
import static ru.yandex.market.adv.promo.utils.DataCampOfferUtils.createServiceOfferWithPromos;
import static ru.yandex.market.adv.promo.utils.DataCampOfferUtils.createUnitedOffersBatchResponse;
import static ru.yandex.market.adv.promo.utils.DataCampOfferUtils.createUnitedOffersBatchResponseWithBasicInfo;
import static ru.yandex.market.adv.promo.utils.DirectDiscountMechanicTestUtils.createAnaplanDirectDiscountDescription;
import static ru.yandex.market.adv.promo.utils.PromocodeMechanicTestUtils.createAnaplanPromocodeDescription;
import static ru.yandex.market.mbi.datacamp.stroller.model.graphql.OfferField.ACTIVE_ANAPLAN_PROMOS;
import static ru.yandex.market.mbi.datacamp.stroller.model.graphql.OfferField.ALL_ANAPLAN_PROMOS;
import static ru.yandex.market.mbi.datacamp.stroller.model.graphql.OfferField.ALL_PARTNER_CASHBACK_PROMOS;
import static ru.yandex.market.mbi.datacamp.stroller.model.graphql.OfferField.ALL_PARTNER_PROMOS;
import static ru.yandex.market.mbi.datacamp.stroller.model.graphql.OfferField.OFFER_CATEGORY_ID;
import static ru.yandex.market.mbi.datacamp.stroller.model.graphql.OfferField.OFFER_IDENTIFIERS;
import static ru.yandex.market.mbi.datacamp.stroller.model.graphql.OfferField.OFFER_VENDOR_ID;

class IntersectedMultiPromosServiceTest extends FunctionalTest {
    @Autowired
    private IntersectedMultiPromosService intersectedMultiPromosService;

    @Autowired
    private DataCampClient dataCampClient;

    @Autowired
    private LoyaltyClient loyaltyClient;

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
        doReturn(
                OffersBatch.UnitedOffersBatchResponse.getDefaultInstance()
        ).when(dataCampClient).getBusinessUnitedOffers(anyLong(), anyCollection(), anyLong());
    }

    /**
     * На офферах нет никаких акций, кастомного кешбека на группы и стандартного кешбека тоже нет.
     */
    @Test
    @DbUnitDataSet
    @DisplayName(
            "Пустой список конструкторов для новодобавляемой промокодовой акции (страница добавления ассортимента)"
    )
    void noPromos_noIncludeTest() {
        noPromos_noInclude();
    }

    @Test
    @DbUnitDataSet(before = "IntersectedMultiPromosServiceTest/before.csv")
    void noPromosGraphqlQuery_noIncludeTest() {
        noPromos_noInclude();
    }

    private void noPromos_noInclude() {
        int businessId = 1111;
        int partnerId = 1111;
        String targetPromoId = "#12345";
        Set<String> offerIds = Set.of("offer01", "offer02");

        if (environmentService.getSettingsBooleanValue(DATACAMP_GRAPHQL_QUERY).orElse(false)) {
            noPromos_mockGetOffersRequestGraphqlQuery(businessId, partnerId, offerIds);
        } else {
            noPromos_mockGetOffersRequest(businessId, partnerId, offerIds);
        }
        mockGetPromosRequest(targetPromoId);

        Map<String, List<PromoConstructorInfo>> constructorsByOfferIds =
                intersectedMultiPromosService.calculateConstructorInfoForOffers(
                        partnerId, businessId, offerIds, targetPromoId, Collections.emptySet()
                );

        assertThat(constructorsByOfferIds.keySet(), containsInAnyOrder(offerIds.toArray()));
        offerIds.forEach(offerId -> assertEquals(0, constructorsByOfferIds.get(offerId).size()));
    }

    /**
     * На офферах нет никаких акций, кастомного кешбека на группы и стандартного кешбека тоже нет.
     */
    @Test
    @DbUnitDataSet
    @DisplayName("В конструкторах только новодобавляемая промокодовая акция (страница мульти)")
    void noPromos_withIncludeTest() {
        noPromos_withInclude();
    }

    @Test
    @DbUnitDataSet(before = "IntersectedMultiPromosServiceTest/before.csv")
    void noPromosGraphqlQuery_withIncludeTest() {
        noPromos_withInclude();
    }

    private void noPromos_withInclude() {
        int businessId = 1111;
        int partnerId = 1111;
        String targetPromoId = "#12345";
        String offerId1 = "offer01";
        String offerId2 = "offer02";
        Set<String> offerIds = Set.of(offerId1, offerId2);

        if (environmentService.getSettingsBooleanValue(DATACAMP_GRAPHQL_QUERY).orElse(false)) {
            noPromos_mockGetOffersRequestGraphqlQuery(businessId, partnerId, offerIds);
        } else {
            noPromos_mockGetOffersRequest(businessId, partnerId, offerIds);
        }
        mockGetPromosRequest(targetPromoId);

        Map<String, List<PromoConstructorInfo>> constructorsByOfferIds =
                intersectedMultiPromosService.calculateConstructorInfoForOffers(
                        partnerId, businessId, offerIds, targetPromoId,
                        Set.of(
                                new OfferPromo(offerId1, true, null, null),
                                new OfferPromo(offerId2, true, null, null)
                        )
                );

        assertThat(constructorsByOfferIds.keySet(), containsInAnyOrder(offerIds.toArray()));
        offerIds.forEach(offerId -> assertEquals(1, constructorsByOfferIds.get(offerId).size()));
        offerIds.forEach(offerId ->
                assertEquals(
                        targetPromoId,
                        constructorsByOfferIds.get(offerId).get(0).getPromoId()
                )
        );
        offerIds.forEach(offerId ->
                assertTrue(isEmpty(constructorsByOfferIds.get(offerId).get(0).getConflictPromoIds()))
        );
        offerIds.forEach(offerId ->
                assertFalse(constructorsByOfferIds.get(offerId).get(0).isHidden())
        );
    }

    private void noPromos_mockGetOffersRequest(int businessId, int partnerId, Set<String> offerIds) {
        List<DataCampOffer.Offer> basicOffers = offerIds.stream()
                .map(offerId -> createBasicOffer(offerId, partnerId, businessId))
                .collect(Collectors.toList());

        doReturn(createUnitedOffersBatchResponseWithBasicInfo(partnerId, basicOffers))
                .when(dataCampClient)
                .getBusinessUnitedOffers(
                        eq(Long.valueOf(businessId)),
                        argThat(collection -> collection.containsAll(offerIds)),
                        eq(Long.valueOf(partnerId))
                );
    }

    private void noPromos_mockGetOffersRequestGraphqlQuery(int businessId, int partnerId, Set<String> offerIds) {
        List<DataCampOffer.Offer> basicOffers = offerIds.stream()
                .map(offerId -> createBasicOffer(offerId, partnerId, businessId))
                .collect(Collectors.toList());

        doReturn(createUnitedOffersBatchResponseWithBasicInfo(partnerId, basicOffers))
                .when(dataCampClient)
                .getBusinessUnitedOffers(
                        eq(Long.valueOf(businessId)),
                        argThat(collection -> collection.containsAll(offerIds)),
                        eq(Long.valueOf(partnerId)),
                        argThat(offerQuery -> org.apache.commons.collections.CollectionUtils.isEqualCollection(
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

    /**
     * каст_кешбек(1-4)
     *         партнерская_cag(3-6,145)
     *                               анаплан_флеш(7-10,145)
     *                                 анаплановская_dd(9-14)
     *           целевая ---> анаплановский_промокод(5-12) <--- целевая
     *                                  анаплановская_cag(9-14,145)
     */
    @Test
    @DisplayName("Кострукторы акций с оффера (целевая акция на оффере не включена; страница добавления ассортимаета)")
    void promosOnOffersTest() {
        promosOnOffers();
    }

    @Test
    @DbUnitDataSet(before = "IntersectedMultiPromosServiceTest/before.csv")
    void promosOnOffersGraphqlQueryTest() {
        promosOnOffers();
    }

    private void promosOnOffers() {
        int businessId = 1111;
        int partnerId = 1111;
        String targetPromoId = "#12345";
        String offerId = "offer01";
        Set<String> offerIds = Set.of(offerId);
        String ddId = "#1111";
        String flashId = "#2222";
        String cagId = "#4444";
        String partnerCagId = partnerId + "_CAG_12345";
        String partnerCbId = partnerId + "_PCC_54321";

        Map<String, List<DataCampOfferPromos.Promo>> allAnaplanPromosByOfferId = Map.of(
                offerId,
                List.of(
                        createDirectDiscountPromo("#564453", null, 5000L),
                        createDirectDiscountPromo(ddId, null, 5000L),
                        createFlashPromo(flashId, null, 10000L),
                        createPromo(targetPromoId),
                        createPromo(cagId)
                )
        );
        Map<String, List<DataCampOfferPromos.Promo>> activeAnaplanPromosByOfferId = Map.of(
                offerId,
                List.of(
                        createDirectDiscountPromo(ddId, 2550L, 7500L),
                        createFlashPromo(flashId, 5000L, null),
                        createPromo(cagId)
                )
        );

        if (environmentService.getSettingsBooleanValue(DATACAMP_GRAPHQL_QUERY).orElse(false)) {
            mockGetOffersRequestGraphqlQuery(
                    businessId, partnerId, offerIds,
                    allAnaplanPromosByOfferId,
                    activeAnaplanPromosByOfferId,
                    Map.of(offerId, List.of(createPromo(partnerCagId))),
                    Map.of(offerId, List.of(createPromo(partnerCbId)))
            );
        } else {
            mockGetOffersRequest(
                    businessId, partnerId, offerIds,
                    allAnaplanPromosByOfferId,
                    activeAnaplanPromosByOfferId,
                    Map.of(offerId, List.of(createPromo(partnerCagId))),
                    Map.of(offerId, List.of(createPromo(partnerCbId)))
            );
        }
        promosOnOffers_mockGetPromosRequest(
                targetPromoId,
                ddId,
                flashId,
                cagId,
                partnerCagId,
                partnerCbId
        );

        Map<String, List<PromoConstructorInfo>> constructorsByOfferIds =
                intersectedMultiPromosService.calculateConstructorInfoForOffers(
                        partnerId, businessId, offerIds, targetPromoId, Collections.emptySet()
                );

        assertThat(constructorsByOfferIds.keySet(), containsInAnyOrder(offerIds.toArray()));
        assertEquals(5, constructorsByOfferIds.get(offerId).size());

        // Сортируем по убыванию значения приоритета.
        // Т е первой будет самая приоритетная акция, последней - наименее приоритетная.
        List<PromoConstructorInfo> sortedConstructors = constructorsByOfferIds.get(offerId).stream()
                .sorted(
                        Comparator.comparingLong(PromoConstructorInfo::getPriority).reversed()
                )
                .collect(Collectors.toList());

        // Самая приоритетная промка
        assertEquals(partnerCbId, sortedConstructors.get(0).getPromoId());
        assertEquals(
                PiPromoMechanicDto.PARTNER_CUSTOM_CASHBACK,
                sortedConstructors.get(0).getMechanic()
        );
        assertTrue(sortedConstructors.get(0).isPartnerPromo());
        assertEquals(18, sortedConstructors.get(0).getDiscountPercentage());
        assertTrue(CollectionUtils.isEmpty(sortedConstructors.get(0).getConflictPromoIds()));
        assertTrue(sortedConstructors.get(0).isHidden());

        // Вторая по приоритетности
        assertEquals(partnerCagId, sortedConstructors.get(1).getPromoId());
        assertEquals(
                PiPromoMechanicDto.CHEAPEST_AS_GIFT,
                sortedConstructors.get(1).getMechanic()
        );
        assertTrue(sortedConstructors.get(1).isPartnerPromo());
        assertTrue(CollectionUtils.isEmpty(sortedConstructors.get(1).getConflictPromoIds()));
        assertFalse(sortedConstructors.get(1).isHidden());

        // Третья по приоритетности
        assertEquals(flashId, sortedConstructors.get(2).getPromoId());
        assertEquals(
                PiPromoMechanicDto.BLUE_FLASH,
                sortedConstructors.get(2).getMechanic()
        );
        assertFalse(sortedConstructors.get(2).isPartnerPromo());
        assertEquals(50, sortedConstructors.get(2).getDiscountPercentage());
        assertThat(sortedConstructors.get(2).getConflictPromoIds(), containsInAnyOrder(ddId, cagId));
        assertFalse(sortedConstructors.get(2).isHidden());

        // Четвертая по приоритетности
        assertEquals(ddId, sortedConstructors.get(3).getPromoId());
        assertEquals(
                PiPromoMechanicDto.DIRECT_DISCOUNT,
                sortedConstructors.get(3).getMechanic()
        );
        assertFalse(sortedConstructors.get(2).isPartnerPromo());
        assertEquals(66, sortedConstructors.get(3).getDiscountPercentage());
        assertThat(sortedConstructors.get(3).getConflictPromoIds(), containsInAnyOrder(flashId));
        assertFalse(sortedConstructors.get(3).isHidden());

        // Наименее приоритетная промка
        assertEquals(cagId, sortedConstructors.get(4).getPromoId());
        assertEquals(
                PiPromoMechanicDto.CHEAPEST_AS_GIFT,
                sortedConstructors.get(4).getMechanic()
        );
        assertFalse(sortedConstructors.get(2).isPartnerPromo());
        assertThat(sortedConstructors.get(4).getConflictPromoIds(), containsInAnyOrder(flashId));
        assertFalse(sortedConstructors.get(4).isHidden());
    }

    @Test
    @DbUnitDataSet
    @DisplayName("Целевая акция выключается на оффере (страница мульти)")
    void switchOffPromoOnOfferTest() {
        switchOffPromoOnOffer();
    }

    @Test
    @DbUnitDataSet(before = "IntersectedMultiPromosServiceTest/before.csv")
    void switchOffPromoOnOfferGraphqlQueryTest() {
        switchOffPromoOnOffer();
    }

    private void switchOffPromoOnOffer() {
        int businessId = 1111;
        int partnerId = 1111;
        String targetPromoId = "#12345";
        String offerId = "offer01";
        Set<String> offerIds = Set.of(offerId);

        if (environmentService.getSettingsBooleanValue(DATACAMP_GRAPHQL_QUERY).orElse(false)) {
            mockGetOffersRequestGraphqlQuery(
                    businessId, partnerId, offerIds,
                    Map.of(
                            offerId,
                            List.of(createPromo(targetPromoId))
                    ),
                    Map.of(
                            offerId,
                            List.of(createPromo(targetPromoId))
                    ),
                    Map.of(),
                    Map.of()
            );

        } else {
            mockGetOffersRequest(
                    businessId, partnerId, offerIds,
                    Map.of(
                            offerId,
                            List.of(createPromo(targetPromoId))
                    ),
                    Map.of(
                            offerId,
                            List.of(createPromo(targetPromoId))
                    ),
                    Map.of(),
                    Map.of()
            );
        }
        mockGetPromosRequest(targetPromoId);

        Map<String, List<PromoConstructorInfo>> constructorsByOfferIds =
                intersectedMultiPromosService.calculateConstructorInfoForOffers(
                        partnerId, businessId, offerIds, targetPromoId,
                        Set.of(new OfferPromo(offerId, false, null, null))
                );

        assertThat(constructorsByOfferIds.keySet(), containsInAnyOrder(offerIds.toArray()));
        assertEquals(0, constructorsByOfferIds.get(offerId).size());
    }

    @Test
    @DbUnitDataSet
    @DisplayName("На оффер добавляется новая скидочная акция (страница мульти)")
    void addNewDiscountPromoOnOfferTest() {
        addNewDiscountPromoOnOffer();
    }

    @Test
    @DbUnitDataSet(before = "IntersectedMultiPromosServiceTest/before.csv")
    void addNewDiscountPromoOnOfferGraphqlQueryTest() {
        addNewDiscountPromoOnOffer();
    }

    private void addNewDiscountPromoOnOffer() {
        int businessId = 1111;
        int partnerId = 1111;
        String targetPromoId = "#12345";
        String offerId = "offer01";
        Set<String> offerIds = Set.of(offerId);

        if (environmentService.getSettingsBooleanValue(DATACAMP_GRAPHQL_QUERY).orElse(false)) {
            mockGetOffersRequestGraphqlQuery(
                    businessId, partnerId, offerIds,
                    Map.of(
                            offerId,
                            List.of(createDirectDiscountPromo(targetPromoId, null, 1500L))
                    ),
                    Map.of(),
                    Map.of(),
                    Map.of()
            );
        } else {
            mockGetOffersRequest(
                    businessId, partnerId, offerIds,
                    Map.of(
                            offerId,
                            List.of(createDirectDiscountPromo(targetPromoId, null, 1500L))
                    ),
                    Map.of(),
                    Map.of(),
                    Map.of()
            );
        }
        mockGetPromosRequest(targetPromoId, createAnaplanDirectDiscountDescription(targetPromoId));

        Map<String, List<PromoConstructorInfo>> constructorsByOfferIds =
                intersectedMultiPromosService.calculateConstructorInfoForOffers(
                        partnerId, businessId, offerIds, targetPromoId,
                        Set.of(new OfferPromo(offerId, true, 2500L, 5000L))
                );

        assertThat(constructorsByOfferIds.keySet(), containsInAnyOrder(offerIds.toArray()));
        assertEquals(1, constructorsByOfferIds.get(offerId).size());

        PromoConstructorInfo constructor = constructorsByOfferIds.get(offerId).get(0);
        assertEquals(targetPromoId, constructor.getPromoId());
        assertEquals(50, constructor.getDiscountPercentage());
        assertFalse(constructor.isHidden());
        assertTrue(isEmpty(constructor.getConflictPromoIds()));
    }

    @Test
    @DbUnitDataSet
    @DisplayName("У оффера в скидочной акции меняются цены и процент скидки (страница мульти)")
    void updateDiscountPromoOnOfferTest() {
        updateDiscountPromoOnOffer();
    }

    @Test
    @DbUnitDataSet(before = "IntersectedMultiPromosServiceTest/before.csv")
    void updateDiscountPromoOnOfferGraphqlQueryTest() {
        updateDiscountPromoOnOffer();
    }

    private void updateDiscountPromoOnOffer() {
        int businessId = 1111;
        int partnerId = 1111;
        String targetPromoId = "#12345";
        String offerId = "offer01";
        Set<String> offerIds = Set.of(offerId);

        if (environmentService.getSettingsBooleanValue(DATACAMP_GRAPHQL_QUERY).orElse(false)) {
            mockGetOffersRequestGraphqlQuery(
                    businessId, partnerId, offerIds,
                    Map.of(
                            offerId,
                            List.of(createDirectDiscountPromo(targetPromoId, null, 1500L))
                    ),
                    Map.of(
                            offerId,
                            List.of(createDirectDiscountPromo(targetPromoId, 800L, 2000L))
                    ),
                    Map.of(),
                    Map.of()
            );
        } else {
            mockGetOffersRequest(
                    businessId, partnerId, offerIds,
                    Map.of(
                            offerId,
                            List.of(createDirectDiscountPromo(targetPromoId, null, 1500L))
                    ),
                    Map.of(
                            offerId,
                            List.of(createDirectDiscountPromo(targetPromoId, 800L, 2000L))
                    ),
                    Map.of(),
                    Map.of()
            );
        }
        mockGetPromosRequest(targetPromoId, createAnaplanDirectDiscountDescription(targetPromoId));

        Map<String, List<PromoConstructorInfo>> constructorsByOfferIds =
                intersectedMultiPromosService.calculateConstructorInfoForOffers(
                        partnerId, businessId, offerIds, targetPromoId,
                        Set.of(new OfferPromo(offerId, true, 1000L, 5000L))
                );

        assertThat(constructorsByOfferIds.keySet(), containsInAnyOrder(offerIds.toArray()));
        assertEquals(1, constructorsByOfferIds.get(offerId).size());

        PromoConstructorInfo constructor = constructorsByOfferIds.get(offerId).get(0);
        assertEquals(targetPromoId, constructor.getPromoId());
        assertEquals(80, constructor.getDiscountPercentage());
        assertFalse(constructor.isHidden());
        assertTrue(isEmpty(constructor.getConflictPromoIds()));
    }

    @Test
    @DbUnitDataSet
    @DisplayName("Проверяется отображение стандартного кешбека на офферах партнера")
    void standardCashbackTest() {
        standardCashback();
    }

    @Test
    @DbUnitDataSet(before = "IntersectedMultiPromosServiceTest/before.csv")
    void standardCashbackGraphqlQueryTest() {
        standardCashback();
    }

    private void standardCashback() {
        int businessId = 1111;
        int partnerId = 1111;
        String targetPromoId = "#12345";
        String offerId1 = "offer01";
        Integer offerId1CategoryId = 123; // cehac
        String offerId2 = "offer02";
        Integer offerId2CategoryId = 234; // diy
        String offerId3 = "offer03";
        Integer offerId3CategoryId = 345; // default
        String offerId4 = "offer04";
        Set<String> offerIds = Set.of(offerId1, offerId2, offerId3, offerId4);
        int cehacValue = 2;
        int diyValue = 5;
        int defaultValue = 18;

        if (environmentService.getSettingsBooleanValue(DATACAMP_GRAPHQL_QUERY).orElse(false)) {
            mockGetOffersRequestGraphqlQuery(
                    businessId,
                    partnerId,
                    offerIds,
                    Map.of(
                            offerId1, offerId1CategoryId,
                            offerId2, offerId2CategoryId,
                            offerId3, offerId3CategoryId
                    )
            );
        } else {
            mockGetOffersRequest(
                    businessId,
                    partnerId,
                    offerIds,
                    Map.of(
                            offerId1, offerId1CategoryId,
                            offerId2, offerId2CategoryId,
                            offerId3, offerId3CategoryId
                    )
            );
        }
        mockGetPromosRequest(targetPromoId);
        mockStandardCashbackRequest(
                partnerId + "_PSC_12345", businessId, cehacValue, diyValue, defaultValue
        );
        mockLoyaltyTariffsRequest(
                Map.of(
                        offerId1CategoryId, "cehac",
                        offerId2CategoryId, "diy",
                        offerId3CategoryId, "default"
                )
        );

        Map<String, List<PromoConstructorInfo>> constructorsByOfferIds =
                intersectedMultiPromosService.calculateConstructorInfoForOffers(
                        partnerId, businessId, offerIds, targetPromoId, Collections.emptySet()
                );

        checkStandardCashbackOnOffer(constructorsByOfferIds.get(offerId1), cehacValue);
        checkStandardCashbackOnOffer(constructorsByOfferIds.get(offerId2), diyValue);
        checkStandardCashbackOnOffer(constructorsByOfferIds.get(offerId3), defaultValue);
        checkStandardCashbackOnOffer(constructorsByOfferIds.get(offerId4), defaultValue);
    }

    private void checkStandardCashbackOnOffer(List<PromoConstructorInfo> constructorsForOffer, int value) {
        assertEquals(1, constructorsForOffer.size());
        assertEquals(PiPromoMechanicDto.PARTNER_STANDART_CASHBACK, constructorsForOffer.get(0).getMechanic());
        assertEquals(value, constructorsForOffer.get(0).getDiscountPercentage());
    }

    private void promosOnOffers_mockGetPromosRequest(
            String targetPromoId,
            String ddId,
            String flashId,
            String cagId,
            String partnerCagId,
            String partnerCbId
    ) {
        Set<String> ids = Set.of(targetPromoId, ddId, flashId, cagId, partnerCagId, partnerCbId);
        doReturn(
                SyncGetPromo.GetPromoBatchResponse.newBuilder()
                        .setPromos(
                                DataCampPromo.PromoDescriptionBatch.newBuilder()
                                        .addPromo(CustomCashbackMechanicTestUtils.createCustomCashbackPromo(
                                                partnerCbId,
                                                LocalDate.of(22, 4, 1),
                                                LocalDate.of(22, 4, 4),
                                                18
                                        ))
                                        .addPromo(createPartnerCheapestAsGiftDescription(
                                                partnerCagId,
                                                LocalDate.of(22, 4, 3),
                                                LocalDate.of(22, 4, 6)
                                        ))
                                        .addPromo(createAnaplanPromocodeDescription(
                                                targetPromoId,
                                                LocalDate.of(22, 4, 5),
                                                LocalDate.of(22, 4, 12)
                                        ))
                                        .addPromo(createAnaplanBlueFlashDescription(
                                                flashId,
                                                LocalDate.of(22, 4, 7),
                                                LocalDate.of(22, 4, 10)
                                        ))
                                        .addPromo(createAnaplanCheapestAsGiftDescription(
                                                cagId,
                                                LocalDate.of(22, 4, 9),
                                                LocalDate.of(22, 4, 14)
                                        ))
                                        .addPromo(createAnaplanDirectDiscountDescription(
                                                ddId,
                                                LocalDate.of(22, 4, 9),
                                                LocalDate.of(22, 4, 14)
                                        ))
                                        .build()
                        )
                        .build()
        ).when(dataCampClient).getPromos((GetPromoBatchRequestWithFilters)
                argThat(arg -> {
                    GetPromoBatchRequestWithFilters requestWithFilters = ((GetPromoBatchRequestWithFilters) arg);
                    Set<String> requestedIds = requestWithFilters.getRequest().getEntriesList().stream()
                            .map(DataCampPromo.PromoDescriptionIdentifier::getPromoId)
                            .collect(Collectors.toSet());
                    return requestedIds.containsAll(ids) &&
                            requestWithFilters.getOnlyUnfinished() &&
                            requestWithFilters.getEnabled();
                })
        );
    }

    private void mockGetOffersRequest(
            int businessId,
            int partnerId,
            Set<String> offerIds,
            Map<String, Integer> categoryIdByOfferId
    ) {
        List<DataCampOffer.Offer> basicOffers = offerIds.stream()
                .map(offerId ->
                        createBasicOffer(offerId, partnerId, businessId, categoryIdByOfferId.get(offerId))
                )
                .collect(Collectors.toList());

        doReturn(createUnitedOffersBatchResponseWithBasicInfo(partnerId, basicOffers))
                .when(dataCampClient)
                .getBusinessUnitedOffers(
                        eq(Long.valueOf(businessId)),
                        argThat(collection -> collection.containsAll(offerIds)),
                        eq(Long.valueOf(partnerId))
                );
    }

    private void mockGetOffersRequestGraphqlQuery(
            int businessId,
            int partnerId,
            Set<String> offerIds,
            Map<String, Integer> categoryIdByOfferId
    ) {
        List<DataCampOffer.Offer> basicOffers = offerIds.stream()
                .map(offerId ->
                        createBasicOffer(offerId, partnerId, businessId, categoryIdByOfferId.get(offerId))
                )
                .collect(Collectors.toList());

        doReturn(createUnitedOffersBatchResponseWithBasicInfo(partnerId, basicOffers))
                .when(dataCampClient)
                .getBusinessUnitedOffers(
                        eq(Long.valueOf(businessId)),
                        argThat(collection -> collection.containsAll(offerIds)),
                        eq(Long.valueOf(partnerId)),
                        argThat(offerQuery -> org.apache.commons.collections.CollectionUtils.isEqualCollection(
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

    private void mockStandardCashbackRequest(
            String promoId,
            int businessId,
            int cehacValue,
            int diyValue,
            int otherValue
    ) {
        DataCampPromo.PromoDescription standardCashback = CashbackMechanicTestUtils.createStandardCashback(
                promoId, businessId, 1,
                cehacValue, diyValue, otherValue,
                true
        );
        ArgumentMatcher<PromoDatacampRequest> standardCashbackRequest = request ->
                CollectionUtils.isNotEmpty(request.getPromoType()) &&
                        request.getPromoType().size() == 1 &&
                        request.getPromoType().contains(DataCampPromo.PromoType.PARTNER_STANDART_CASHBACK);
        doReturn(
                SyncGetPromo.GetPromoBatchResponse.newBuilder()
                        .setPromos(
                                DataCampPromo.PromoDescriptionBatch.newBuilder()
                                        .addPromo(standardCashback)
                                        .build()
                        )
                        .build()
        ).when(dataCampClient).getPromos(argThat(standardCashbackRequest));
    }

    private void mockLoyaltyTariffsRequest(Map<Integer, String> codeNameByCategoryId) {
        List<MarketTariffResponse> tariffs = codeNameByCategoryId.entrySet().stream()
                .map(codeNameByCategoryIdEntry ->
                        new MarketTariffResponse(
                                codeNameByCategoryIdEntry.getKey(),
                                codeNameByCategoryIdEntry.getValue()
                        )
                ).collect(Collectors.toList());
        PartnerCashbackMarketTariffsResponse response = new PartnerCashbackMarketTariffsResponse(1, tariffs);

        doReturn(response)
                .when(loyaltyClient)
                .getMarketTariffs(
                        any(),
                        argThat(collection -> collection.containsAll(codeNameByCategoryId.keySet()))
                );
    }

    private void mockGetOffersRequest(
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
                        argThat(collection -> collection.containsAll(offerIds)),
                        eq(Long.valueOf(partnerId))
                );
    }

    private void mockGetOffersRequestGraphqlQuery(
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
                        argThat(collection -> collection.containsAll(offerIds)),
                        eq(Long.valueOf(partnerId)),
                        argThat(offerQuery -> org.apache.commons.collections.CollectionUtils.isEqualCollection(
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

    private void mockGetPromosRequest(String targetPromoId) {
        mockGetPromosRequest(targetPromoId, createAnaplanPromocodeDescription(targetPromoId));
    }

    private void mockGetPromosRequest(String targetPromoId, DataCampPromo.PromoDescription description) {
        doReturn(
                SyncGetPromo.GetPromoBatchResponse.newBuilder()
                        .setPromos(
                                DataCampPromo.PromoDescriptionBatch.newBuilder()
                                        .addPromo(description)
                                        .build()
                        )
                        .build()
        ).when(dataCampClient).getPromos((GetPromoBatchRequestWithFilters)
                argThat(arg -> {
                    GetPromoBatchRequestWithFilters requestWithFilters = ((GetPromoBatchRequestWithFilters) arg);
                    return requestWithFilters.getRequest().getEntriesCount() == 1 &&
                            targetPromoId.equals(requestWithFilters.getRequest().getEntries(0).getPromoId()) &&
                            requestWithFilters.getOnlyUnfinished() &&
                            requestWithFilters.getEnabled();
                })
        );
    }
}

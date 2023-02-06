package ru.yandex.market.partner.mvc.controller.supplier.promo;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import Market.DataCamp.API.DatacampMessageOuterClass;
import Market.DataCamp.DataCampOffer;
import Market.DataCamp.DataCampOfferContent;
import Market.DataCamp.DataCampOfferIdentifiers;
import Market.DataCamp.DataCampOfferMapping;
import Market.DataCamp.DataCampOfferPrice;
import Market.DataCamp.DataCampOfferPromos;
import Market.DataCamp.DataCampPromo;
import Market.DataCamp.SyncAPI.SyncChangeOffer;
import Market.DataCamp.SyncAPI.SyncGetPromo;
import NMarket.Common.Promo.Promo;
import NMarketIndexer.Common.Common;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.common.test.util.StringTestUtil;
import ru.yandex.market.core.datacamp.DataCampUtil;
import ru.yandex.market.core.logbroker.event.datacamp.DatacampMessageLogbrokerEvent;
import ru.yandex.market.logbroker.LogbrokerService;
import ru.yandex.market.mbi.datacamp.saas.SaasService;
import ru.yandex.market.mbi.datacamp.saas.impl.model.SaasOfferInfo;
import ru.yandex.market.mbi.datacamp.saas.impl.model.SaasSearchResult;
import ru.yandex.market.mbi.datacamp.stroller.DataCampClient;
import ru.yandex.market.mbi.datacamp.stroller.model.GetPromoBatchRequestWithFilters;
import ru.yandex.market.partner.test.context.FunctionalTest;
import ru.yandex.market.partner.util.FunctionalTestHelper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static ru.yandex.market.common.test.util.JsonTestUtil.assertEquals;
import static ru.yandex.market.partner.util.DataCampMessagesCreator.createBlueFlashPromo;
import static ru.yandex.market.partner.util.DataCampMessagesCreator.createSimplePromoWithIdOnly;
import static ru.yandex.market.partner.util.DataCampMessagesCreator.createDiscountPromo;
import static ru.yandex.market.partner.util.DataCampMessagesCreator.createMarketPromos;

@DbUnitDataSet(before = "pi/PromoPIControllerFunctionalTest.before.csv")
class PromoPIControllerFunctionalTest extends FunctionalTest {

    @Autowired
    @Qualifier("promoOfferLogbrokerService")
    private LogbrokerService promoOfferLogbrokerService;

    @Autowired
    @Qualifier("dataCampShopClient")
    private DataCampClient dataCampShopClient;

    @Autowired
    private SaasService saasDataCampShopService;

    @Test
    void unknownPromoTest() {
        String requestBody = StringTestUtil.getString(this.getClass(), "pi/offer-without-promo.json");
        ResponseEntity<String> response = sendUpdatePromoOffersRequest(1001, requestBody);
        assertEquals(response, "{\"status\":\"INCORRECT_REQUEST__UNKNOWN_PROMO\"}");
    }

    @Test
    void noOffersTest() {
        String requestBody = StringTestUtil.getString(this.getClass(), "pi/no-offers-in-request.json");
        ResponseEntity<String> response = sendUpdatePromoOffersRequest(1001, requestBody);
        assertEquals(response, "{\"status\":\"INCORRECT_REQUEST__NO_OFFERS\"}");
    }

    @Test
    void incorrectOffersParamsTest() {
        String requestBody = StringTestUtil.getString(this.getClass(), "pi/incorrest_offers_params.json");
        ResponseEntity<String> response = sendUpdatePromoOffersRequest(1001, requestBody);
        assertEquals(response, "{\"status\":\"INCORRECT_REQUEST__INVALID_OFFER_PARAMS\"}");
    }

    @Test
    void failedValidationTest() {
        mockPromoDescriptions();
        mockDataCampOffers();
        String requestBody = StringTestUtil.getString(this.getClass(), "pi/failed-validation-request.json");
        ResponseEntity<String> response = sendUpdatePromoOffersRequest(1001, requestBody);
        assertEquals(response, "{\"status\":\"INCORRECT_REQUEST__FAIL_VALIDATION\"}");
    }

    @Test
    void correctValidationTest() {
        mockPromoDescriptions();
        mockDataCampOffers();
        String requestBody = StringTestUtil.getString(this.getClass(), "pi/correct-request.json");

        ResponseEntity<String> response = sendUpdatePromoOffersRequest(1001, requestBody);

        ArgumentCaptor<DatacampMessageLogbrokerEvent> eventCaptor =
                ArgumentCaptor.forClass(DatacampMessageLogbrokerEvent.class);
        verify(promoOfferLogbrokerService, times(1)).publishEvent(eventCaptor.capture());
        DatacampMessageLogbrokerEvent event = eventCaptor.getValue();
        DatacampMessageOuterClass.DatacampMessage msg = event.getPayload();
        List<DataCampOffer.Offer> offers = msg.getOffersList().get(0).getOfferList();
        assertEquals(3, offers.size());
        assertEquals("hid_1", offers.get(0).getIdentifiers().getOfferId());
        checkOfferHasPromo("#1111", DataCampPromo.PromoType.DIRECT_DISCOUNT, 3893, 1, offers.get(0), 5900L);
        assertEquals("hid_2", offers.get(1).getIdentifiers().getOfferId());
        checkOfferHasNoPromo(offers.get(1));
        assertEquals("hid_3", offers.get(2).getIdentifiers().getOfferId());
        checkOfferHasPromo("#1111", DataCampPromo.PromoType.DIRECT_DISCOUNT, 56620, 1, offers.get(2), 70500L);

        assertEquals(response, "{\"status\":\"SUCCESS\"}");

        ArgumentCaptor<DataCampPromo.PromoDescription> updatePromoDescriptionCaptor =
                ArgumentCaptor.forClass(DataCampPromo.PromoDescription.class);
        verify(dataCampShopClient, times(1)).addPromo(updatePromoDescriptionCaptor.capture());
        DataCampPromo.PromoDescription updatedDescription = updatePromoDescriptionCaptor.getValue();
        assertEquals("#1111", updatedDescription.getPrimaryKey().getPromoId());
        assertEquals(1111, updatedDescription.getUpdateInfo().getCreatedAt());
        assertTrue(2222 < updatedDescription.getUpdateInfo().getUpdatedAt());
    }

    @Test
    void correctValidationForFlashTest() {
        mockPromoDescriptions();
        mockDataCampOffers();
        String requestBody = StringTestUtil.getString(this.getClass(), "pi/flash-request.json");

        ResponseEntity<String> response = sendUpdatePromoOffersRequest(1001, requestBody);

        ArgumentCaptor<DatacampMessageLogbrokerEvent> eventCaptor =
                ArgumentCaptor.forClass(DatacampMessageLogbrokerEvent.class);
        verify(promoOfferLogbrokerService, times(1)).publishEvent(eventCaptor.capture());
        DatacampMessageLogbrokerEvent event = eventCaptor.getValue();
        DatacampMessageOuterClass.DatacampMessage msg = event.getPayload();
        List<DataCampOffer.Offer> offers = msg.getOffersList().get(0).getOfferList();
        assertEquals(2, offers.size());
        assertEquals("hid_1", offers.get(0).getIdentifiers().getOfferId());
        checkOfferHasPromo("#3333", DataCampPromo.PromoType.BLUE_FLASH, 1300, 1, offers.get(0), 3400L);
        assertEquals("check_intersection_flash", offers.get(1).getIdentifiers().getOfferId());
        checkOfferHasPromo("#3333", DataCampPromo.PromoType.BLUE_FLASH, 3500, 1, offers.get(1), 6000L);

        assertEquals(response, "{\"status\":\"SUCCESS\"}");
    }

    @Test
    void correctValidationForPromocodeTest() {
        mockPromoDescriptions();
        mockDataCampOffers();
        String requestBody = StringTestUtil.getString(this.getClass(), "pi/market-promocode-request.json");

        ResponseEntity<String> response = sendUpdatePromoOffersRequest(1001, requestBody);

        ArgumentCaptor<DatacampMessageLogbrokerEvent> eventCaptor =
                ArgumentCaptor.forClass(DatacampMessageLogbrokerEvent.class);
        verify(promoOfferLogbrokerService, times(1)).publishEvent(eventCaptor.capture());
        DatacampMessageLogbrokerEvent event = eventCaptor.getValue();
        DatacampMessageOuterClass.DatacampMessage msg = event.getPayload();
        List<DataCampOffer.Offer> offers = msg.getOffersList().get(0).getOfferList();
        assertEquals(2, offers.size());
        assertEquals("hid_1", offers.get(0).getIdentifiers().getOfferId());
        checkOfferHasPromo("#1111", DataCampPromo.PromoType.DIRECT_DISCOUNT, 3900, 2, offers.get(0), null);
        checkOfferHasPromo("#5555", DataCampPromo.PromoType.MARKET_PROMOCODE, 0, 2, offers.get(0), null);
        assertEquals("hid_2", offers.get(1).getIdentifiers().getOfferId());
        checkOfferHasPromo("#1111", DataCampPromo.PromoType.DIRECT_DISCOUNT, 6785, 2, offers.get(1), null);
        checkOfferHasPromo("#5555", DataCampPromo.PromoType.MARKET_PROMOCODE, 0, 2, offers.get(1), null);

        assertEquals(response, "{\"status\":\"SUCCESS\"}");
    }

    private void checkOfferHasPromo(
            String promoId,
            DataCampPromo.PromoType promoType,
            long price,
            int activePromosCount,
            DataCampOffer.Offer offer,
            Long oldPrice
    ) {
        assertTrue(offer.getPromos().getAnaplanPromos().hasActivePromos());
        assertFalse(offer.getPromos().getAnaplanPromos().hasAllPromos());
        assertEquals(activePromosCount, offer.getPromos().getAnaplanPromos().getActivePromos().getPromosCount());
        Map<String, DataCampOfferPromos.Promo> promoById =
                offer.getPromos().getAnaplanPromos().getActivePromos().getPromosList().stream()
                        .collect(
                                Collectors.toMap(DataCampOfferPromos.Promo::getId, Function.identity())
                        );
        DataCampOfferPromos.Promo promo1 = promoById.get(promoId);
        assertEquals(promoId, promo1.getId());
        if (promoType == DataCampPromo.PromoType.DIRECT_DISCOUNT) {
            assertTrue(promo1.hasDirectDiscount());
            assertEquals(DataCampUtil.powToIdx(BigDecimal.valueOf(price)),
                    promo1.getDirectDiscount().getPrice().getPrice());
            if (oldPrice == null) {
                assertFalse(promo1.getDirectDiscount().hasBasePrice());
            } else {
                assertTrue(promo1.getDirectDiscount().hasBasePrice());
                assertEquals(DataCampUtil.powToIdx(BigDecimal.valueOf(oldPrice)),
                        promo1.getDirectDiscount().getBasePrice().getPrice());
            }
        } else if (promoType == DataCampPromo.PromoType.BLUE_FLASH) {
            assertTrue(promo1.hasFlash());
            assertEquals(DataCampUtil.powToIdx(BigDecimal.valueOf(price)),
                    promo1.getFlash().getPrice().getPrice());
            if (oldPrice == null) {
                assertFalse(promo1.getFlash().hasBasePrice());
            }
        }
    }

    private void checkOfferHasNoPromo(DataCampOffer.Offer offer) {
        assertEquals(0, offer.getPromos().getAnaplanPromos().getActivePromos().getPromosCount());
        assertFalse(offer.getPromos().getAnaplanPromos().hasAllPromos());
    }

    private ResponseEntity<String> sendUpdatePromoOffersRequest(
            long campaignId,
            Object body
    ) {
        return FunctionalTestHelper.post(baseUrl + "/supplier/promo/offers?campaign_id=" + campaignId, body);
    }

    private void mockPromoDescriptions() {
        doNothing().when(dataCampShopClient).addPromo(any(DataCampPromo.PromoDescription.class));

        long year_2100 = 4102492536L;
        DataCampPromo.PromoDescription promo1111 = DataCampPromo.PromoDescription.newBuilder()
                .setPrimaryKey(DataCampPromo.PromoDescriptionIdentifier.newBuilder()
                        .setPromoId("#1111")
                        .setSource(Promo.ESourceType.ANAPLAN)
                        .build())
                .setPromoGeneralInfo(DataCampPromo.PromoGeneralInfo.newBuilder()
                        .setPromoType(DataCampPromo.PromoType.DIRECT_DISCOUNT)
                        .build())
                .setAdditionalInfo(DataCampPromo.PromoAdditionalInfo.newBuilder()
                        .setName("Promo #1111")
                        .build())
                .setConstraints(DataCampPromo.PromoConstraints.newBuilder()
                        .addOffersMatchingRules(DataCampPromo.PromoConstraints.OffersMatchingRule.newBuilder()
                                .setCategoryRestriction(
                                        DataCampPromo.PromoConstraints.OffersMatchingRule.CategoryRestriction.newBuilder()
                                                .addAllPromoCategory(
                                                        List.of(
                                                                DataCampPromo.PromoConstraints.OffersMatchingRule
                                                                        .PromoCategory.newBuilder()
                                                                        .setId(1101)
                                                                        .setName("Category 1101")
                                                                        .setMinDiscount(10)
                                                                        .build(),
                                                                DataCampPromo.PromoConstraints.OffersMatchingRule
                                                                        .PromoCategory.newBuilder()
                                                                        .setId(1102)
                                                                        .setName("Category 1102")
                                                                        .setMinDiscount(15)
                                                                        .build()
                                                        )
                                                )
                                                .build()
                                )
                                .build())
                        .setStartDate(year_2100 + 1)
                        .setEndDate(year_2100 + 15)
                        .build())
                .setUpdateInfo(
                        DataCampPromo.UpdateInfo.newBuilder()
                                .setCreatedAt(1111)
                                .setUpdatedAt(2222)
                                .build()
                )
                .build();

        DataCampPromo.PromoDescription promo2222 = DataCampPromo.PromoDescription.newBuilder()
                .setPrimaryKey(DataCampPromo.PromoDescriptionIdentifier.newBuilder()
                        .setPromoId("#2222")
                        .setSource(Promo.ESourceType.ANAPLAN)
                        .build())
                .setPromoGeneralInfo(DataCampPromo.PromoGeneralInfo.newBuilder()
                        .setPromoType(DataCampPromo.PromoType.CHEAPEST_AS_GIFT)
                        .build())
                .setAdditionalInfo(DataCampPromo.PromoAdditionalInfo.newBuilder()
                        .setName("Promo #2222")
                        .build())
                .setConstraints(DataCampPromo.PromoConstraints.newBuilder()
                        .addOffersMatchingRules(
                                DataCampPromo.PromoConstraints.OffersMatchingRule.newBuilder()
                                        .setWarehouseRestriction(
                                                DataCampPromo.PromoConstraints.OffersMatchingRule.WarehouseRestriction.newBuilder()
                                                        .setWarehouse(
                                                                DataCampPromo.PromoConstraints.OffersMatchingRule.IntList.newBuilder()
                                                                        .addId(145)
                                                                        .build()
                                                        )
                                                        .build()
                                        )
                                        .build()
                        )
                        .setStartDate(year_2100 + 10)
                        .setEndDate(year_2100 + 25)
                        .build())
                .setUpdateInfo(
                        DataCampPromo.UpdateInfo.newBuilder()
                                .setCreatedAt(1111)
                                .setUpdatedAt(2222)
                                .build()
                )
                .build();

        DataCampPromo.PromoDescription promo3333 = DataCampPromo.PromoDescription.newBuilder()
                .setPrimaryKey(DataCampPromo.PromoDescriptionIdentifier.newBuilder()
                        .setPromoId("#3333")
                        .setSource(Promo.ESourceType.ANAPLAN)
                        .build())
                .setPromoGeneralInfo(DataCampPromo.PromoGeneralInfo.newBuilder()
                        .setPromoType(DataCampPromo.PromoType.BLUE_FLASH)
                        .build())
                .setAdditionalInfo(DataCampPromo.PromoAdditionalInfo.newBuilder()
                        .setName("Promo #3333")
                        .build())
                .setConstraints(DataCampPromo.PromoConstraints.newBuilder()
                        .addOffersMatchingRules(DataCampPromo.PromoConstraints.OffersMatchingRule.newBuilder()
                                .setCategoryRestriction(
                                        DataCampPromo.PromoConstraints.OffersMatchingRule.CategoryRestriction.newBuilder()
                                                .addAllPromoCategory(
                                                        List.of(
                                                                DataCampPromo.PromoConstraints.OffersMatchingRule
                                                                        .PromoCategory.newBuilder()
                                                                        .setId(1101)
                                                                        .setName("Category 1101")
                                                                        .setMinDiscount(10)
                                                                        .build(),
                                                                DataCampPromo.PromoConstraints.OffersMatchingRule
                                                                        .PromoCategory.newBuilder()
                                                                        .setId(1102)
                                                                        .setName("Category 1102")
                                                                        .setMinDiscount(15)
                                                                        .build()
                                                        )
                                                )
                                                .build()
                                )
                                .build())
                        .setStartDate(year_2100 + 14)
                        .setEndDate(year_2100 + 24)
                        .build())
                .setUpdateInfo(
                        DataCampPromo.UpdateInfo.newBuilder()
                                .setCreatedAt(1111)
                                .setUpdatedAt(2222)
                                .build()
                )
                .build();

        DataCampPromo.PromoDescription promo4444 = DataCampPromo.PromoDescription.newBuilder()
                .setPrimaryKey(DataCampPromo.PromoDescriptionIdentifier.newBuilder()
                        .setPromoId("#4444")
                        .setSource(Promo.ESourceType.ANAPLAN)
                        .build())
                .setPromoGeneralInfo(DataCampPromo.PromoGeneralInfo.newBuilder()
                        .setPromoType(DataCampPromo.PromoType.BLUE_FLASH)
                        .build())
                .setAdditionalInfo(DataCampPromo.PromoAdditionalInfo.newBuilder()
                        .setName("Promo #4444")
                        .build())
                .setConstraints(DataCampPromo.PromoConstraints.newBuilder()
                        .addOffersMatchingRules(DataCampPromo.PromoConstraints.OffersMatchingRule.newBuilder()
                                .setCategoryRestriction(
                                        DataCampPromo.PromoConstraints.OffersMatchingRule.CategoryRestriction.newBuilder()
                                                .addAllPromoCategory(
                                                        List.of(
                                                                DataCampPromo.PromoConstraints.OffersMatchingRule
                                                                        .PromoCategory.newBuilder()
                                                                        .setId(9009)
                                                                        .setName("Category 9009")
                                                                        .setMinDiscount(30)
                                                                        .build(),
                                                                DataCampPromo.PromoConstraints.OffersMatchingRule
                                                                        .PromoCategory.newBuilder()
                                                                        .setId(9019)
                                                                        .setName("Category 9019")
                                                                        .setMinDiscount(45)
                                                                        .build()
                                                        )
                                                )
                                                .build()
                                )
                                .setWarehouseRestriction(
                                        DataCampPromo.PromoConstraints.OffersMatchingRule.WarehouseRestriction.newBuilder()
                                                .setWarehouse(
                                                        DataCampPromo.PromoConstraints.OffersMatchingRule.IntList.newBuilder()
                                                                .addId(172)
                                                                .build()
                                                )
                                                .build()
                                )
                                .build())
                        .setStartDate(year_2100 + 24)
                        .setEndDate(year_2100 + 25)
                        .build())
                .setUpdateInfo(
                        DataCampPromo.UpdateInfo.newBuilder()
                                .setCreatedAt(1111)
                                .setUpdatedAt(2222)
                                .build()
                )
                .build();

        DataCampPromo.PromoDescription promo5555 = DataCampPromo.PromoDescription.newBuilder()
                .setPrimaryKey(DataCampPromo.PromoDescriptionIdentifier.newBuilder()
                        .setPromoId("#5555")
                        .setSource(Promo.ESourceType.ANAPLAN)
                        .build())
                .setPromoGeneralInfo(DataCampPromo.PromoGeneralInfo.newBuilder()
                        .setPromoType(DataCampPromo.PromoType.MARKET_PROMOCODE)
                        .build())
                .setAdditionalInfo(DataCampPromo.PromoAdditionalInfo.newBuilder()
                        .setName("Promo #5555")
                        .build())
                .setConstraints(DataCampPromo.PromoConstraints.newBuilder()
                        .setStartDate(year_2100 + 1)
                        .setEndDate(year_2100 + 25)
                        .build())
                .setMechanicsData(DataCampPromo.PromoMechanics.newBuilder()
                        .setMarketPromocode(DataCampPromo.PromoMechanics.MarketPromocode.newBuilder()
                                .setDiscountType(DataCampPromo.PromoMechanics.MarketPromocode.DiscountType.PERCENTAGE)
                                .setValue(50)
                        )
                )
                .setUpdateInfo(
                        DataCampPromo.UpdateInfo.newBuilder()
                                .setCreatedAt(1111)
                                .setUpdatedAt(2222)
                                .build()
                )
                .build();

        doReturn(SyncGetPromo.GetPromoBatchResponse.newBuilder()
                .setPromos(DataCampPromo.PromoDescriptionBatch.newBuilder()
                        .addPromo(promo1111)
                        .addPromo(promo2222)
                        .addPromo(promo3333)
                        .addPromo(promo4444)
                        .addPromo(promo5555)
                        .build())
                .build())
                .when(dataCampShopClient).getPromos(any(GetPromoBatchRequestWithFilters.class));
    }

    private void mockDataCampOffers() {
        int shopId_1 = 1;
        int warehouseId_1 = 1;
        int businessId_1 = 1;
        int msku_1 = 1;
        String productName_1 = "productName_1";
        DataCampOffer.Offer dataCampOffer_1 = DataCampOffer.Offer
                .newBuilder()
                .setIdentifiers(
                        DataCampOfferIdentifiers.OfferIdentifiers.newBuilder()
                                .setBusinessId(businessId_1)
                                .setOfferId("hid_1")
                                .setShopId(shopId_1)
                                .setWarehouseId(warehouseId_1)
                                .build()
                )
                .setContent(
                        DataCampOfferContent.OfferContent.newBuilder()
                                .setMarket(
                                        DataCampOfferContent.MarketContent.newBuilder()
                                                .setCategoryId(1101)
                                                .setProductName(productName_1)
                                )
                                .setBinding(DataCampOfferMapping.ContentBinding.newBuilder()
                                        .setApproved(DataCampOfferMapping.Mapping.newBuilder()
                                                .setMarketSkuId(msku_1)
                                        )
                                )
                )
                .setPromos(
                        DataCampOfferPromos.OfferPromos.newBuilder()
                                .setAnaplanPromos(
                                        createMarketPromos(
                                                Collections.singletonList(
                                                        createDiscountPromo("#1111", 3900L, null)
                                                ),
                                                Arrays.asList(
                                                        createDiscountPromo("#1111", null, 5900L),
                                                        createBlueFlashPromo("#3333", null, 3400L),
                                                        createSimplePromoWithIdOnly("#5555")
                                                )
                                        )
                                )
                )
                .setPrice(DataCampOfferPrice.OfferPrice.newBuilder()
                        .setBasic(DataCampOfferPrice.PriceBundle.newBuilder()
                                .setBinaryPrice(Common.PriceExpression.newBuilder()
                                        .setPrice(1000000)
                                        .build())
                                .build())
                        .build())
                .build();
        DataCampOffer.Offer dataCampOffer_1_2 = DataCampOffer.Offer
                .newBuilder()
                .setIdentifiers(
                        DataCampOfferIdentifiers.OfferIdentifiers.newBuilder()
                                .setBusinessId(businessId_1)
                                .setOfferId("check_intersection_flash")
                                .setShopId(shopId_1)
                                .setWarehouseId(warehouseId_1)
                )
                .setContent(
                        DataCampOfferContent.OfferContent.newBuilder()
                                .setMarket(
                                        DataCampOfferContent.MarketContent.newBuilder()
                                                .setCategoryId(1101)
                                                .setProductName(productName_1)
                                )
                                .setBinding(
                                        DataCampOfferMapping.ContentBinding.newBuilder()
                                            .setApproved(DataCampOfferMapping.Mapping.newBuilder()
                                                .setMarketSkuId(msku_1)
                                            )
                                )
                )
                .setPromos(
                        DataCampOfferPromos.OfferPromos.newBuilder()
                                .setAnaplanPromos(
                                        createMarketPromos(
                                                Arrays.asList(
                                                        createSimplePromoWithIdOnly("#2222"),
                                                        createBlueFlashPromo("#4444", 340L, null)
                                                ),
                                                Arrays.asList(
                                                        createSimplePromoWithIdOnly("#2222"),
                                                        createBlueFlashPromo("#4444", null, 500L),
                                                        createBlueFlashPromo("#3333", null, 6000L)
                                                )
                                        )
                                )
                )
                .setPrice(DataCampOfferPrice.OfferPrice.newBuilder()
                        .setBasic(DataCampOfferPrice.PriceBundle.newBuilder()
                                .setBinaryPrice(Common.PriceExpression.newBuilder()
                                        .setPrice(1000000)
                                        .build())
                                .build())
                        .build())
                .build();

        int shopId_2 = 2;
        int warehouseId_2 = 2;
        int businessId_2 = 2;
        long msku_2 = 2;
        String productName_2 = "productName_2";
        DataCampOffer.Offer dataCampOffer_2 = DataCampOffer.Offer
                .newBuilder()
                .setIdentifiers(
                        DataCampOfferIdentifiers.OfferIdentifiers.newBuilder()
                                .setBusinessId(businessId_2)
                                .setOfferId("hid_2")
                                .setShopId(shopId_2)
                                .setWarehouseId(warehouseId_2)
                                .build()
                )
                .setContent(
                        DataCampOfferContent.OfferContent.newBuilder()
                                .setMarket(
                                        DataCampOfferContent.MarketContent.newBuilder()
                                                .setCategoryId(1101)
                                                .setProductName(productName_2)
                                )
                                .setBinding(
                                        DataCampOfferMapping.ContentBinding.newBuilder()
                                                .setApproved(DataCampOfferMapping.Mapping.newBuilder()
                                                        .setMarketSkuId(msku_2)
                                                )
                                )
                )
                .setPromos(
                        DataCampOfferPromos.OfferPromos.newBuilder()
                                .setAnaplanPromos(
                                        createMarketPromos(
                                                Collections.singletonList(
                                                        createDiscountPromo("#1111", 6785L, null)
                                                ),
                                                List.of(
                                                        createDiscountPromo("#1111", null, 8893L),
                                                        createSimplePromoWithIdOnly("#5555")
                                                )
                                        )
                                )
                )
                .setPrice(DataCampOfferPrice.OfferPrice.newBuilder()
                        .setBasic(DataCampOfferPrice.PriceBundle.newBuilder()
                                .setBinaryPrice(Common.PriceExpression.newBuilder()
                                        .setPrice(1000000)
                                        .build())
                                .build())
                        .build())
                .build();

        int shopId_3 = 3;
        int warehouseId_3 = 3;
        int businessId_3 = 3;
        long msku_3 = 3;
        String productName_3 = "product_name_3";
        DataCampOffer.Offer dataCampOffer_3 = DataCampOffer.Offer
                .newBuilder()
                .setIdentifiers(
                        DataCampOfferIdentifiers.OfferIdentifiers.newBuilder()
                                .setBusinessId(businessId_3)
                                .setOfferId("hid_3")
                                .setShopId(shopId_3)
                                .setWarehouseId(warehouseId_3)
                                .build()
                )
                .setContent(
                        DataCampOfferContent.OfferContent.newBuilder()
                                .setMarket(
                                        DataCampOfferContent.MarketContent.newBuilder()
                                                .setCategoryId(1102)
                                                .setProductName(productName_3)
                                )
                                .setBinding(
                                        DataCampOfferMapping.ContentBinding.newBuilder()
                                                .setApproved(DataCampOfferMapping.Mapping.newBuilder()
                                                        .setMarketSkuId(msku_3)
                                                )
                                )
                )
                .setPromos(
                        DataCampOfferPromos.OfferPromos.newBuilder()
                                .setAnaplanPromos(
                                        createMarketPromos(
                                                Collections.emptyList(),
                                                Arrays.asList(
                                                        createDiscountPromo("#1111", null, 70500L),
                                                        createSimplePromoWithIdOnly("#2222")
                                                )
                                        )
                                )
                )
                .setPrice(DataCampOfferPrice.OfferPrice.newBuilder()
                        .setBasic(DataCampOfferPrice.PriceBundle.newBuilder()
                                .setBinaryPrice(Common.PriceExpression.newBuilder()
                                        .setPrice(1000000)
                                        .build())
                                .build())
                        .build())
                .build();

        doReturn(
                SyncChangeOffer.FullOfferResponse.newBuilder()
                        .addAllOffer(List.of(dataCampOffer_1, dataCampOffer_1_2, dataCampOffer_2, dataCampOffer_3))
                        .build()
        ).when(dataCampShopClient).getOffers(anyLong(), anyLong(), any());

        SaasOfferInfo saasOfferInfo = SaasOfferInfo.newBuilder()
                .addShopId(774L)
                .addOfferId("0516465165")
                .build();
        SaasSearchResult saasSearchResult = SaasSearchResult.builder()
                .setOffers(List.of(saasOfferInfo))
                .setTotalCount(1)
                .build();
        doReturn(saasSearchResult)
                .when(saasDataCampShopService).searchBusinessOffers(any());
    }
}

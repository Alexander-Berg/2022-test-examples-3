package ru.yandex.market.marketpromo.web.controller;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;

import ru.yandex.market.marketpromo.core.application.security.MBOCAuthenticationRequest;
import ru.yandex.market.marketpromo.core.dao.AssortmentPromoParticipationDao;
import ru.yandex.market.marketpromo.core.dao.DatacampOfferDao;
import ru.yandex.market.marketpromo.core.dao.DirectDiscountAssortmentDao;
import ru.yandex.market.marketpromo.core.dao.PromoDao;
import ru.yandex.market.marketpromo.core.service.impl.CachedAssortmentService;
import ru.yandex.market.marketpromo.core.test.generator.DatacampOfferPromoMechanics;
import ru.yandex.market.marketpromo.core.test.generator.Offers;
import ru.yandex.market.marketpromo.core.test.generator.Promos;
import ru.yandex.market.marketpromo.filter.AssortmentFilter;
import ru.yandex.market.marketpromo.filter.AssortmentRequest;
import ru.yandex.market.marketpromo.model.DirectDiscountOfferParticipation;
import ru.yandex.market.marketpromo.model.MechanicsType;
import ru.yandex.market.marketpromo.model.OfferDisabledSource;
import ru.yandex.market.marketpromo.model.OfferId;
import ru.yandex.market.marketpromo.model.OfferPromoParticipation;
import ru.yandex.market.marketpromo.model.Promo;
import ru.yandex.market.marketpromo.model.PublishActionState;
import ru.yandex.market.marketpromo.model.SupplierType;
import ru.yandex.market.marketpromo.model.processing.PublishingStatus;
import ru.yandex.market.marketpromo.security.MBOCAuthenticationFilter;
import ru.yandex.market.marketpromo.security.SecurityRoles;
import ru.yandex.market.marketpromo.test.MockedWebTestBase;
import ru.yandex.market.marketpromo.test.client.AssortmentRequests;
import ru.yandex.market.marketpromo.utils.IdentityUtils;
import ru.yandex.market.marketpromo.web.model.DirectDiscountMarkToParticipateItem;
import ru.yandex.market.marketpromo.web.model.DirectDiscountOfferProperties;
import ru.yandex.market.marketpromo.web.model.MarkToParticipateItemBase;
import ru.yandex.market.marketpromo.web.model.request.MarkToParticipateRequest;
import ru.yandex.market.marketpromo.web.model.response.ImportResponse;
import ru.yandex.market.marketpromo.web.model.response.OfferItemsCountResponse;
import ru.yandex.market.marketpromo.web.model.response.OfferItemsPagingResponse;
import ru.yandex.market.marketpromo.web.model.response.OfferItemsResponse;
import ru.yandex.market.marketpromo.web.model.response.PublishingResponse;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.aMapWithSize;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.comparesEqualTo;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.emptyString;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.marketpromo.core.test.generator.Offers.basePrice;
import static ru.yandex.market.marketpromo.core.test.generator.Offers.categoryId;
import static ru.yandex.market.marketpromo.core.test.generator.Offers.datacampOffer;
import static ru.yandex.market.marketpromo.core.test.generator.Offers.disabled;
import static ru.yandex.market.marketpromo.core.test.generator.Offers.disabledSource;
import static ru.yandex.market.marketpromo.core.test.generator.Offers.name;
import static ru.yandex.market.marketpromo.core.test.generator.Offers.potentialPromo;
import static ru.yandex.market.marketpromo.core.test.generator.Offers.price;
import static ru.yandex.market.marketpromo.core.test.generator.Offers.shop;
import static ru.yandex.market.marketpromo.core.test.generator.Offers.shopSku;
import static ru.yandex.market.marketpromo.core.test.generator.Offers.stocks;
import static ru.yandex.market.marketpromo.core.test.generator.Offers.supplierType;
import static ru.yandex.market.marketpromo.core.test.generator.Offers.wareMd5;
import static ru.yandex.market.marketpromo.core.test.generator.Offers.warehouse;
import static ru.yandex.market.marketpromo.core.test.generator.PromoMechanics.minimalDiscountPercentSize;
import static ru.yandex.market.marketpromo.core.test.generator.Promos.assortmentAutopublication;
import static ru.yandex.market.marketpromo.core.test.generator.Promos.cheapestAsGift;
import static ru.yandex.market.marketpromo.core.test.generator.Promos.directDiscount;
import static ru.yandex.market.marketpromo.core.test.generator.Promos.id;
import static ru.yandex.market.marketpromo.core.test.generator.Promos.promo;
import static ru.yandex.market.marketpromo.core.test.generator.Promos.promoId;
import static ru.yandex.market.marketpromo.core.test.generator.Promos.promoName;
import static ru.yandex.market.marketpromo.core.test.generator.Promos.system;
import static ru.yandex.market.marketpromo.test.client.AssortmentRequests.markAssortmentAction;
import static ru.yandex.market.marketpromo.test.client.AssortmentRequests.publishAssortmentAction;

public class AssortmentApiControllerTest extends MockedWebTestBase {

    private static final long WAREHOUSE_ID = 123L;
    private static final long SHOP_ID = 12L;
    private static final String SSKU_1 = "ssku-1";
    private static final String SSKU_2 = "ssku-2";
    private static final String SSKU_3 = "ssku-3";
    private static final String SSKU_4 = "ssku-4";
    private static final String WARE_1 = "ware-1";
    private static final String WARE_2 = "ware-2";
    private static final String DUMMY_PROMO_ID = "direct-discount$83972ad2-da80-11ea-87d0-000000000000";
    private static final String DD_PROMO_ID = "#21098";
    private static final String SYSTEM_DD_PROMO_ID = "#21999";
    private static final String AUTOPUBLISHED_DD_PROMO_ID = "#22222";
    private static final String DD_PROMO_NAME = "DD Promo";
    private static final String CAG_PROMO_ID = "#21099";
    private static final String CAG_PROMO_ID_2 = "#21100";
    private static final String CAG_PROMO_NAME = "CAG Promo";

    @Autowired
    private AssortmentPromoParticipationDao assortmentPromoParticipationDao;
    @Autowired
    private DirectDiscountAssortmentDao directDiscountAssortmentDao;
    @Autowired
    private PromoDao promoDao;
    @Autowired
    private DatacampOfferDao datacampOfferDao;
    @Value("classpath:excel/assortment.xlsx")
    private Resource assortmentImportResource;
    @Autowired
    private CachedAssortmentService cachedAssortmentService;

    private Promo directDiscount;
    private Promo cheapestAsGift;
    private Promo systemDirectDiscount;
    private Promo autopublishedDirectDiscount;

    @BeforeEach
    void configure() {
        directDiscount = promoDao.replace(promo(
                id(IdentityUtils.hashId(DD_PROMO_ID)),
                promoId(DD_PROMO_ID),
                promoName(DD_PROMO_NAME),
                directDiscount(
                        minimalDiscountPercentSize(10)
                )
        ));
        cheapestAsGift = promoDao.replace(promo(
                id(IdentityUtils.hashId(CAG_PROMO_ID)),
                promoName(CAG_PROMO_NAME),
                Promos.warehouse(WAREHOUSE_ID),
                cheapestAsGift()
        ));

        systemDirectDiscount = promoDao.replaceAllFields(promo(
                id(IdentityUtils.hashId(SYSTEM_DD_PROMO_ID)),
                promoId(SYSTEM_DD_PROMO_ID),
                promoName(DD_PROMO_NAME),
                directDiscount(
                        minimalDiscountPercentSize(10)
                ),
                system(true)
        ));

        autopublishedDirectDiscount = promoDao.replace(promo(
                id(IdentityUtils.hashId(AUTOPUBLISHED_DD_PROMO_ID)),
                promoId(AUTOPUBLISHED_DD_PROMO_ID),
                promoName(DD_PROMO_NAME),
                directDiscount(
                        minimalDiscountPercentSize(10)
                ),
                assortmentAutopublication()
        ));

        datacampOfferDao.replace(List.of(
                datacampOffer(
                        name(SSKU_1),
                        shopSku(SSKU_1),
                        shop(SHOP_ID),
                        wareMd5(WARE_1),
                        warehouse(WAREHOUSE_ID),
                        supplierType(SupplierType._1P),
                        price(1000),
                        basePrice(1500),
                        disabledSource(OfferDisabledSource.MARKET_ABO),
                        disabledSource(OfferDisabledSource.MARKET_IDX),
                        disabled(true),
                        stocks(15L),
                        categoryId(123L),
                        potentialPromo(IdentityUtils.hashId(DD_PROMO_ID), BigDecimal.valueOf(150))
                ),
                datacampOffer(
                        name(SSKU_2),
                        shopSku(SSKU_2),
                        shop(SHOP_ID),
                        wareMd5(WARE_2),
                        warehouse(WAREHOUSE_ID),
                        supplierType(SupplierType._1P),
                        price(1000),
                        basePrice(1500),
                        categoryId(123L),
                        stocks(15L),
                        potentialPromo(IdentityUtils.hashId(CAG_PROMO_ID)),
                        potentialPromo(IdentityUtils.hashId(CAG_PROMO_ID_2)),
                        Offers.activePromos(
                                DatacampOfferPromoMechanics.cheapestAsGift(IdentityUtils.hashId(CAG_PROMO_ID))
                        )
                ),
                datacampOffer(
                        name(SSKU_3),
                        shopSku(SSKU_3),
                        shop(SHOP_ID),
                        warehouse(WAREHOUSE_ID),
                        supplierType(SupplierType._1P),
                        price(1000),
                        basePrice(1500),
                        stocks(15L),
                        categoryId(123L),
                        disabledSource(OfferDisabledSource.MARKET_ABO),
                        disabledSource(OfferDisabledSource.UNKNOWN_SOURCE),
                        potentialPromo(IdentityUtils.hashId(DD_PROMO_ID), BigDecimal.valueOf(150)),
                        potentialPromo(IdentityUtils.hashId(CAG_PROMO_ID)),
                        Offers.activePromos(
                                DatacampOfferPromoMechanics.cheapestAsGift(IdentityUtils.hashId(CAG_PROMO_ID)),
                                DatacampOfferPromoMechanics.cheapestAsGift(IdentityUtils.hashId(CAG_PROMO_ID_2))
                        )
                ),
                datacampOffer(
                        name(SSKU_4),
                        shopSku(SSKU_4),
                        shop(SHOP_ID),
                        warehouse(WAREHOUSE_ID),
                        supplierType(SupplierType._1P),
                        price(1000),
                        basePrice(1500),
                        stocks(15L),
                        categoryId(123L),
                        disabledSource(OfferDisabledSource.MARKET_ABO),
                        disabledSource(OfferDisabledSource.UNKNOWN_SOURCE),
                        potentialPromo(IdentityUtils.hashId(DD_PROMO_ID), BigDecimal.valueOf(150)),
                        potentialPromo(IdentityUtils.hashId(CAG_PROMO_ID))
                )
        ));

        cachedAssortmentService.refreshAssortmentCache();
    }

    @Test
    void shouldRespondOnDirectDiscountAssortmentGet() throws Exception {
        OfferItemsPagingResponse pagingResponse = AssortmentRequests.getAssortment(mockMvc,
                AssortmentRequest.builder(directDiscount.toPromoKey())
                        .build());

        OfferItemsCountResponse countResponse = AssortmentRequests.getAssortmentCount(mockMvc,
                AssortmentRequest.builder(directDiscount.toPromoKey())
                        .build());

        assertThat(pagingResponse, notNullValue());
        assertThat(pagingResponse.getItems(), notNullValue());
        assertThat(pagingResponse.getItems(), hasSize(3));
        assertThat(pagingResponse.getItems(), everyItem(allOf(
                hasProperty("id"),
                hasProperty("name"),
                hasProperty("basePrice", comparesEqualTo((BigDecimal.valueOf(1500)))),
                hasProperty("price", comparesEqualTo((BigDecimal.valueOf(1000)))),
                hasProperty("categoryId", comparesEqualTo(123L)),
                hasProperty("availableCount", comparesEqualTo(15L)),
                hasProperty("mechanicsType", is(MechanicsType.DIRECT_DISCOUNT)),
                hasProperty("mechanicsProperties", allOf(
//                        hasProperty("fixedPrice", nullValue()),
//                        hasProperty("fixedBasePrice", comparesEqualTo(BigDecimal.valueOf(150))),
                        hasProperty("minimalDiscountPercentSize", comparesEqualTo(BigDecimal.TEN))
                ))
        )));
        assertThat(countResponse.getTotalItems(), comparesEqualTo(3L));
        assertThat(pagingResponse.getMeta(), notNullValue());
        assertThat(pagingResponse.getMeta().getPublishStatus(), is(PublishActionState.DISABLED_NO_ACCESS));
    }

    @Test
    void shouldRespondOnCheapestAsGiftAssortmentGet() throws Exception {
        OfferItemsPagingResponse pagingResponse = AssortmentRequests.getAssortment(mockMvc,
                AssortmentRequest.builder(cheapestAsGift.toPromoKey())
                        .filter(AssortmentFilter.SSKU, SSKU_2)
                        .build());

        assertThat(pagingResponse, notNullValue());
        assertThat(pagingResponse.getItems(), notNullValue());
        assertThat(pagingResponse.getItems(), hasItem(allOf(
                hasProperty("id"),
                hasProperty("name"),
                hasProperty("ssku"),
                hasProperty("mechanicsType", is(MechanicsType.CHEAPEST_AS_GIFT)),
                hasProperty("basePrice", comparesEqualTo((BigDecimal.valueOf(1500)))),
                hasProperty("price", comparesEqualTo((BigDecimal.valueOf(1000)))),
                hasProperty("categoryId", comparesEqualTo(123L)),
                hasProperty("availableCount", comparesEqualTo(15L)),
                hasProperty("actualPromos", not(hasItem(
                        hasProperty("promoId", is(IdentityUtils.hashId(CAG_PROMO_ID))))))
        )));
        assertThat(pagingResponse.getMeta().getPublishStatus(), is(PublishActionState.DISABLED_NO_ACCESS));
    }

    @Test
    void shouldRespondOnDirectDiscountAssortmentCountGet() throws Exception {
        OfferItemsCountResponse countResponse = AssortmentRequests.getAssortmentCount(mockMvc,
                AssortmentRequest.builder(cheapestAsGift.toPromoKey())
                        .filter(AssortmentFilter.SSKU, SSKU_2)
                        .build());

        assertThat(countResponse, notNullValue());
        assertThat(countResponse.getAppliedFilters(), notNullValue());
        assertThat(countResponse.getTotalItems(), comparesEqualTo(1L));
    }

    @Test
    void shouldRespondOnCheapestAsGiftAssortmentCountGet() throws Exception {
        OfferItemsCountResponse countResponse = AssortmentRequests.getAssortmentCount(mockMvc,
                AssortmentRequest.builder(directDiscount.toPromoKey())
                        .build());

        assertThat(countResponse, notNullValue());
        assertThat(countResponse.getAppliedFilters(), notNullValue());
        assertThat(countResponse.getTotalItems(), comparesEqualTo(3L));
    }

    @Test
    void shouldRespondOnCheapestAsGiftAssortmentGetHideOtherActivePromos() throws Exception {
        OfferItemsPagingResponse pagingResponse = AssortmentRequests.getAssortment(mockMvc,
                AssortmentRequest.builder(cheapestAsGift.toPromoKey())
                        .filter(AssortmentFilter.HIDE_OTHER_ACTIVE_PROMOS, true)
                        .build());

        assertThat(pagingResponse, notNullValue());
        assertThat(pagingResponse.getItems(), notNullValue());
        assertThat(pagingResponse.getItems(), hasItem(allOf(
                hasProperty("id"),
                hasProperty("name"),
                hasProperty("ssku"),
                hasProperty("mechanicsType", is(MechanicsType.CHEAPEST_AS_GIFT)),
                hasProperty("basePrice", comparesEqualTo((BigDecimal.valueOf(1500)))),
                hasProperty("price", comparesEqualTo((BigDecimal.valueOf(1000)))),
                hasProperty("categoryId", comparesEqualTo(123L)),
                hasProperty("availableCount", comparesEqualTo(15L))
        )));
        assertThat(pagingResponse.getMeta().getPublishStatus(), is(PublishActionState.DISABLED_NO_ACCESS));
    }

    @Test
    void shouldRespondOnAssortmentMarkingToDirectDiscountPromo() throws Exception {
        OfferItemsResponse response = AssortmentRequests.markAssortment(mockMvc, directDiscount,
                MarkToParticipateRequest.builder()
                        .item(DirectDiscountMarkToParticipateItem.builder()
                                .id(OfferId.of(IdentityUtils.hashId(SSKU_1), SHOP_ID))
                                .mechanicsType(MechanicsType.DIRECT_DISCOUNT)
                                .participates(true)
                                .mechanicsProperties(DirectDiscountOfferProperties.builder()
                                        .fixedBasePrice(BigDecimal.TEN)
                                        .fixedPrice(BigDecimal.ONE)
                                        .minimalDiscountPercentSize(BigDecimal.TEN)
                                        .build())
                                .build())
                        .build());

        assertThat(response, notNullValue());
        assertThat(response.getItems(), notNullValue());
        assertThat(response.getItems(), hasItem(allOf(
                hasProperty("id", is(OfferId.of(IdentityUtils.hashId(SSKU_1), SHOP_ID).toString())),
                hasProperty("ssku"),
                hasProperty("mechanicsType", is(MechanicsType.DIRECT_DISCOUNT)),
                hasProperty("basePrice", comparesEqualTo((BigDecimal.valueOf(1500)))),
                hasProperty("price", comparesEqualTo((BigDecimal.valueOf(1000)))),
                hasProperty("disabledSources", hasSize(2)),
                hasProperty("disabled", is(true)),
                hasProperty("participates", is(true)),
                hasProperty("actualPromos", not(hasItem(
                        hasProperty("promoId", is(directDiscount.getId()))
                ))),
                hasProperty("mechanicsProperties", allOf(
//                        hasProperty("fixedBasePrice", comparesEqualTo(BigDecimal.TEN)),
//                        hasProperty("fixedPrice", comparesEqualTo(BigDecimal.ONE)),
                        hasProperty("minimalDiscountPercentSize", comparesEqualTo(BigDecimal.TEN))
                ))
        )));
        assertThat(response.getMeta().getPublishStatus(), is(PublishActionState.ENABLED));
    }

    @Test
    void shouldBeWarningAfterAssortmentMarking() throws Exception {
        datacampOfferDao.update(datacampOffer(
                name(SSKU_1),
                shopSku(SSKU_1),
                shop(SHOP_ID),
                wareMd5(WARE_1),
                warehouse(WAREHOUSE_ID),
                supplierType(SupplierType._1P),
                price(1000),
                disabledSource(OfferDisabledSource.MARKET_ABO),
                disabledSource(OfferDisabledSource.MARKET_IDX),
                disabled(true),
                stocks(15L),
                categoryId(123L),
                potentialPromo(IdentityUtils.hashId(DD_PROMO_ID))
        ));

        OfferItemsResponse response = AssortmentRequests.markAssortment(mockMvc, directDiscount,
                MarkToParticipateRequest.builder()
                        .item(DirectDiscountMarkToParticipateItem.builder()
                                .id(OfferId.of(IdentityUtils.hashId(SSKU_1), SHOP_ID))
                                .mechanicsType(MechanicsType.DIRECT_DISCOUNT)
                                .participates(true)
                                .mechanicsProperties(DirectDiscountOfferProperties.builder()
                                        .fixedPrice(BigDecimal.ONE)
                                        .minimalDiscountPercentSize(BigDecimal.TEN)
                                        .build())
                                .build())
                        .build());

        assertThat(response, notNullValue());
        assertThat(response.getItems(), notNullValue());
        assertThat(response.getMeta().getPublishStatus(), is(PublishActionState.ENABLED));

        OfferItemsPagingResponse pagingResponse = AssortmentRequests.getAssortment(mockMvc,
                AssortmentRequest.builder(directDiscount.toPromoKey())
                        .build());

        assertThat(pagingResponse, notNullValue());
        assertThat(pagingResponse.getItems(), notNullValue());
        assertThat(pagingResponse.getItems(), hasSize(3));
    }

    @Test
    void shouldRespondOnAssortmentDisableMarkToDirectDiscountPromo() throws Exception {
        OfferItemsResponse response = AssortmentRequests.markAssortment(mockMvc, directDiscount,
                MarkToParticipateRequest.builder()
                        .item(DirectDiscountMarkToParticipateItem.builder()
                                .id(OfferId.of(IdentityUtils.hashId(SSKU_1), SHOP_ID))
                                .mechanicsType(MechanicsType.DIRECT_DISCOUNT)
                                .participates(false)
                                .mechanicsProperties(DirectDiscountOfferProperties.builder()
                                        .fixedBasePrice(BigDecimal.TEN)
                                        .fixedPrice(BigDecimal.ONE)
                                        .minimalDiscountPercentSize(BigDecimal.TEN)
                                        .build())
                                .build())
                        .build());

        assertThat(response, notNullValue());
        assertThat(response.getItems(), notNullValue());
        assertThat(response.getItems(), hasItem(allOf(
                hasProperty("id", is(OfferId.of(IdentityUtils.hashId(SSKU_1), SHOP_ID).toString())),
                hasProperty("ssku"),
                hasProperty("mechanicsType", is(MechanicsType.DIRECT_DISCOUNT)),
                hasProperty("basePrice", comparesEqualTo((BigDecimal.valueOf(1500)))),
                hasProperty("price", comparesEqualTo((BigDecimal.valueOf(1000)))),
                hasProperty("participates", is(false)),
                hasProperty("mechanicsProperties", allOf(
//                        hasProperty("fixedBasePrice", comparesEqualTo(BigDecimal.TEN)),
//                        hasProperty("fixedPrice", comparesEqualTo(BigDecimal.ONE)),
                        hasProperty("minimalDiscountPercentSize", comparesEqualTo(BigDecimal.TEN))
                ))
        )));
        assertThat(response.getMeta().getPublishStatus(), is(PublishActionState.ENABLED));
    }

    @Test
    void shouldRespondOnAssortmentMarkingToCheapestAsGiftPromo() throws Exception {
        OfferItemsResponse response = AssortmentRequests.markAssortment(mockMvc, cheapestAsGift,
                MarkToParticipateRequest.builder()
                        .item(MarkToParticipateItemBase.baseBuilder()
                                .id(OfferId.of(IdentityUtils.hashId(SSKU_2), SHOP_ID))
                                .mechanicsType(MechanicsType.CHEAPEST_AS_GIFT)
                                .participates(true)
                                .build())
                        .build());

        assertThat(response, notNullValue());
        assertThat(response.getItems(), notNullValue());
        assertThat(response.getItems(), hasItem(allOf(
                hasProperty("id", is(OfferId.of(IdentityUtils.hashId(SSKU_2), SHOP_ID).toString())),
                hasProperty("ssku"),
                hasProperty("stocksByWarehouse", hasEntry(WAREHOUSE_ID, 15L)),
                hasProperty("mechanicsType", is(MechanicsType.CHEAPEST_AS_GIFT)),
                hasProperty("basePrice", comparesEqualTo((BigDecimal.valueOf(1500)))),
                hasProperty("participates", is(true)),
                hasProperty("price", comparesEqualTo((BigDecimal.valueOf(1000))))
        )));
    }

    @Test
    void shouldRespondOnAssortmentDisableMarkToCheapestAsGiftPromo() throws Exception {
        OfferItemsResponse response = AssortmentRequests.markAssortment(mockMvc, cheapestAsGift,
                MarkToParticipateRequest.builder()
                        .item(MarkToParticipateItemBase.baseBuilder()
                                .id(OfferId.of(IdentityUtils.hashId(SSKU_3), SHOP_ID))
                                .mechanicsType(MechanicsType.CHEAPEST_AS_GIFT)
                                .participates(false)
                                .build())
                        .build());

        assertThat(response, notNullValue());
        assertThat(response.getItems(), notNullValue());
        assertThat(response.getItems(), hasItem(allOf(
                hasProperty("id", is(OfferId.of(IdentityUtils.hashId(SSKU_3), SHOP_ID).toString())),
                hasProperty("ssku"),
                hasProperty("mechanicsType", is(MechanicsType.CHEAPEST_AS_GIFT)),
                hasProperty("basePrice", comparesEqualTo((BigDecimal.valueOf(1500)))),
                hasProperty("participates", is(false)),
                hasProperty("price", comparesEqualTo((BigDecimal.valueOf(1000))))
        )));
    }

    @Test
    void shouldSaveDirectDiscountParticipation() throws Exception {
        OfferItemsResponse response = AssortmentRequests.markAssortment(mockMvc, directDiscount,
                MarkToParticipateRequest.builder()
                        .item(DirectDiscountMarkToParticipateItem.builder()
                                .id(OfferId.of(IdentityUtils.hashId(SSKU_1), SHOP_ID))
                                .mechanicsType(MechanicsType.DIRECT_DISCOUNT)
                                .participates(true)
                                .mechanicsProperties(DirectDiscountOfferProperties.builder()
                                        .fixedBasePrice(BigDecimal.TEN)
                                        .fixedPrice(BigDecimal.valueOf(9.4))
                                        .minimalDiscountPercentSize(BigDecimal.TEN)
                                        .build())
                                .build())
                        .build());

        //todo: call service method instead of direct db access in web test
        Map<OfferId, OfferPromoParticipation> participation = assortmentPromoParticipationDao
                .getOfferParticipationInPromo(directDiscount.toPromoKey(), List.of(
                        OfferId.of(IdentityUtils.hashId(SSKU_1), SHOP_ID)
                ));
        Map<OfferId, DirectDiscountOfferParticipation> directDiscountParticipation =
                directDiscountAssortmentDao.getParticipationPromoDetails(
                        directDiscount.toPromoKey(),
                        participation
                );
        assertThat(participation, aMapWithSize(1));
        assertThat(participation, hasEntry(
                is(OfferId.of(IdentityUtils.hashId(SSKU_1), SHOP_ID)),
                hasProperty("participate", is(true))
        ));
//        assertThat(participation, hasEntry(
//                is(OfferId.of(IdentityUtils.hashId(SSKU_1), SHOP_ID)),
//                hasProperty("warnings", hasSize(1))
//        ));
        assertThat(directDiscountParticipation, aMapWithSize(1));
        assertThat(directDiscountParticipation, hasEntry(
                is(OfferId.of(IdentityUtils.hashId(SSKU_1), SHOP_ID)),
                allOf(
//                        hasProperty("fixedBasePrice", comparesEqualTo(BigDecimal.TEN)),
//                        hasProperty("fixedPrice", comparesEqualTo(BigDecimal.valueOf(9.4)))
                )
        ));
    }

    @Test
    void shouldSaveCheapestAsGiftParticipation() throws Exception {
        OfferItemsResponse response = AssortmentRequests.markAssortment(mockMvc, cheapestAsGift,
                MarkToParticipateRequest.builder()
                        .item(MarkToParticipateItemBase.baseBuilder()
                                .id(OfferId.of(IdentityUtils.hashId(SSKU_2), SHOP_ID))
                                .mechanicsType(MechanicsType.CHEAPEST_AS_GIFT)
                                .participates(true)
                                .build())
                        .build());

        //todo: call service method instead of direct db access in web test
        Map<OfferId, OfferPromoParticipation> participation = assortmentPromoParticipationDao
                .getOfferParticipationInPromo(cheapestAsGift.toPromoKey(), List.of(
                        OfferId.of(IdentityUtils.hashId(SSKU_2), SHOP_ID)
                ));
        assertThat(participation, aMapWithSize(1));
        assertThat(participation, hasEntry(
                is(OfferId.of(IdentityUtils.hashId(SSKU_2), SHOP_ID)),
                hasProperty("participate", is(true))
        ));
    }

    @Test
    void shouldPublishDirectDiscountParticipation() throws Exception {
        OfferItemsResponse response = AssortmentRequests.markAssortment(mockMvc, directDiscount,
                MarkToParticipateRequest.builder()
                        .item(DirectDiscountMarkToParticipateItem.builder()
                                .id(OfferId.of(IdentityUtils.hashId(SSKU_1), SHOP_ID))
                                .mechanicsType(MechanicsType.DIRECT_DISCOUNT)
                                .participates(true)
                                .mechanicsProperties(DirectDiscountOfferProperties.builder()
                                        .fixedBasePrice(BigDecimal.TEN)
                                        .fixedPrice(BigDecimal.ONE)
                                        .minimalDiscountPercentSize(BigDecimal.TEN)
                                        .build())
                                .build())
                        .build());

        PublishingResponse publishingResponse = AssortmentRequests.publishAssortment(mockMvc, directDiscount);

        assertThat(publishingResponse, notNullValue());
        assertThat(publishingResponse.getPublishingStatus(), is(PublishingStatus.PUBLISHED));
        assertThat(publishingResponse.getToken(), not(emptyString()));
    }

    @Test
    void shouldNotFoundWhenTryToMarkSystemDirectDiscount() throws Exception {
        final MBOCAuthenticationRequest auth = MBOCAuthenticationRequest.builder()
                .roles(Set.of(SecurityRoles.MANAGE_PROMO_ASSORTMENT))
                .build();

        final String content = markAssortmentAction(mockMvc, systemDirectDiscount,
                MarkToParticipateRequest.builder().build(), auth)
                .andExpect(status().is(404))
                .andReturn().getResponse().getContentAsString();

        final String id = IdentityUtils.encodePromoId(systemDirectDiscount);
        assertThat(content, containsString("System promo '" + id + "' can't be changed."));
    }

    @Test
    void shouldNotFoundWhenTryToPublishSystemDirectDiscountParticipation() throws Exception {
        final String content = publishAssortmentAction(mockMvc, systemDirectDiscount)
                .andExpect(status().is(404))
                .andReturn().getResponse().getContentAsString();

        final String id = IdentityUtils.encodePromoId(systemDirectDiscount);
        assertThat(content, containsString("System promo '" + id + "' can't be changed."));
    }

    @Test
    void shouldNotFoundWhenTryToPublishAutopublishedDirectDiscountParticipation() throws Exception {
        final String content = publishAssortmentAction(mockMvc, autopublishedDirectDiscount)
                .andExpect(status().is(404))
                .andReturn().getResponse().getContentAsString();

        final String id = IdentityUtils.encodePromoId(autopublishedDirectDiscount);
        assertThat(content, containsString("Autopublished promo '" + id + "' can't be changed."));
    }

    @Test
    void shouldGetPublishToken() throws Exception {
        AssortmentRequests.markAssortment(mockMvc, directDiscount,
                MarkToParticipateRequest.builder()
                        .item(DirectDiscountMarkToParticipateItem.builder()
                                .id(OfferId.of(IdentityUtils.hashId(SSKU_1), SHOP_ID))
                                .mechanicsType(MechanicsType.DIRECT_DISCOUNT)
                                .participates(true)
                                .mechanicsProperties(DirectDiscountOfferProperties.builder()
                                        .fixedBasePrice(BigDecimal.TEN)
                                        .fixedPrice(BigDecimal.ONE)
                                        .minimalDiscountPercentSize(BigDecimal.TEN)
                                        .build())
                                .build())
                        .build());

        PublishingResponse publishingResponse = AssortmentRequests.publishAssortment(mockMvc, directDiscount);

        assertThat(publishingResponse, notNullValue());

        publishingResponse = AssortmentRequests.assortmentPublishingState(mockMvc, directDiscount,
                publishingResponse.getToken());


        assertThat(publishingResponse.getPublishingStatus(), is(PublishingStatus.PUBLISHED));
        assertThat(publishingResponse.getToken(), not(emptyString()));
    }

    @Test
    void shouldGetTotalAndPublishedCount() throws Exception {
        AssortmentRequests.markAssortment(mockMvc, directDiscount,
                MarkToParticipateRequest.builder()
                        .item(DirectDiscountMarkToParticipateItem.builder()
                                .id(OfferId.of(IdentityUtils.hashId(SSKU_1), SHOP_ID))
                                .mechanicsType(MechanicsType.DIRECT_DISCOUNT)
                                .participates(true)
                                .mechanicsProperties(DirectDiscountOfferProperties.builder()
                                        .fixedBasePrice(BigDecimal.TEN)
                                        .fixedPrice(BigDecimal.ONE)
                                        .minimalDiscountPercentSize(BigDecimal.TEN)
                                        .build())
                                .build())
                        .build());

        PublishingResponse publishingResponse = AssortmentRequests.publishAssortment(mockMvc, directDiscount);

        assertThat(publishingResponse, notNullValue());

        publishingResponse = AssortmentRequests.assortmentPublishingState(mockMvc, directDiscount,
                publishingResponse.getToken());

        assertThat(publishingResponse.getPublishingStatus(), is(PublishingStatus.PUBLISHED));
        assertThat(publishingResponse.getTotal(), comparesEqualTo(1L));
        assertThat(publishingResponse.getPublished(), comparesEqualTo(0L));
    }

    @Test
    void shouldSaveAndPublishCheapestAsGiftParticipation() throws Exception {
        AssortmentRequests.markAssortment(mockMvc, cheapestAsGift,
                MarkToParticipateRequest.builder()
                        .item(MarkToParticipateItemBase.baseBuilder()
                                .id(OfferId.of(IdentityUtils.hashId(SSKU_2), SHOP_ID))
                                .mechanicsType(MechanicsType.CHEAPEST_AS_GIFT)
                                .participates(true)
                                .build())
                        .build());

        PublishingResponse publishingResponse = AssortmentRequests.publishAssortment(mockMvc, cheapestAsGift);

        assertThat(publishingResponse, notNullValue());
        assertThat(publishingResponse.getPublishingStatus(), is(PublishingStatus.PUBLISHED));
        assertThat(publishingResponse.getToken(), not(emptyString()));
    }

    @Test
    @Disabled
    void shouldImportAssortment() throws Exception {
        final byte[] bytes = Files.readAllBytes(assortmentImportResource.getFile().toPath());

        datacampOfferDao.replace(List.of(
                datacampOffer(
                        shopSku("qxr.215928"),
                        shop(SHOP_ID),
                        supplierType(SupplierType._1P),
                        potentialPromo(directDiscount.getId())
                ),
                datacampOffer(
                        shopSku("etu.200327"),
                        shop(SHOP_ID),
                        supplierType(SupplierType._1P),
                        potentialPromo(directDiscount.getId())
                )
        ));

        MockMultipartFile importFile = new MockMultipartFile(
                "files",
                "assortment.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                bytes);

        ImportResponse importResponse = AssortmentRequests.importXlsx(mockMvc, directDiscount, importFile);

        assertThat(importResponse.getErrors(), nullValue());

        Map<OfferId, OfferPromoParticipation> participation = assortmentPromoParticipationDao
                .getOfferParticipationInPromo(directDiscount.toPromoKey());

        assertThat(participation, aMapWithSize(2));
        assertThat(participation, allOf(
                hasEntry(
                        is(OfferId.of(IdentityUtils.hashId("qxr.215928"), SHOP_ID)),
                        hasProperty("participate", is(true))
                ),
                hasEntry(
                        is(OfferId.of(IdentityUtils.hashId("etu.200327"), SHOP_ID)),
                        hasProperty("participate", is(false))
                )
        ));
    }

    @Test
    void shouldResetAssortmentForCheapestAsDiscountPromo() throws Exception {
        OfferPromoParticipation itemParticipation = OfferPromoParticipation.builder()
                .promoId(cheapestAsGift.getId())
                .offerId(OfferId.of(IdentityUtils.hashId(SSKU_2), SHOP_ID))
                .participate(true)
                .build();

        assortmentPromoParticipationDao.saveParticipateToPromo(cheapestAsGift.toPromoKey(),
                Collections.singletonList(itemParticipation));

        AssortmentRequests.resetAssortment(mockMvc, cheapestAsGift);

        Map<OfferId, OfferPromoParticipation> participation = assortmentPromoParticipationDao
                .getOfferParticipationInPromo(cheapestAsGift.toPromoKey());

        assertThat(participation, aMapWithSize(0));
    }

    @Test
    void shouldNotFindPromo() throws Exception {
        mockMvc.perform(
                delete("/v1/promos/{promoId}/assortment/reset", DUMMY_PROMO_ID)
                        .contentType(MediaType.APPLICATION_JSON_UTF8_VALUE)
                        .header(MBOCAuthenticationFilter.USER_ROLES_HEADER, SecurityRoles.MANAGE_PROMO_ASSORTMENT)
        ).andExpect(status().is4xxClientError());
    }

    @Test
    void shouldGetWareMd5Property() throws Exception {
        OfferItemsPagingResponse pagingResponse = AssortmentRequests.getAssortment(mockMvc,
                AssortmentRequest.builder(directDiscount.toPromoKey())
                        .filter(AssortmentFilter.SSKU, SSKU_1)
                        .build());

        assertThat(pagingResponse.getItems(), hasSize(1));
        assertThat(pagingResponse.getItems(), hasItem(allOf(
                hasProperty("wareMd5", is(WARE_1))
        )));

        pagingResponse = AssortmentRequests.getAssortment(mockMvc,
                AssortmentRequest.builder(cheapestAsGift.toPromoKey())
                        .filter(AssortmentFilter.SSKU, SSKU_2)
                        .build());

        assertThat(pagingResponse.getItems(), hasSize(1));
        assertThat(pagingResponse.getItems(), hasItem(allOf(
                hasProperty("wareMd5", is(WARE_2))
        )));
    }
}

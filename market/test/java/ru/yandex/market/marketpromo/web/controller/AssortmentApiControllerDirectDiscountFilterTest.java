package ru.yandex.market.marketpromo.web.controller;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.marketpromo.core.dao.DatacampOfferDao;
import ru.yandex.market.marketpromo.core.dao.PromoDao;
import ru.yandex.market.marketpromo.core.service.impl.CachedAssortmentService;
import ru.yandex.market.marketpromo.core.test.generator.DatacampOfferPromoMechanics;
import ru.yandex.market.marketpromo.filter.AssortmentFilter;
import ru.yandex.market.marketpromo.filter.AssortmentRequest;
import ru.yandex.market.marketpromo.model.DirectDiscountOfferParticipation;
import ru.yandex.market.marketpromo.model.OfferId;
import ru.yandex.market.marketpromo.model.OfferPromoParticipation;
import ru.yandex.market.marketpromo.model.Promo;
import ru.yandex.market.marketpromo.model.SupplierType;
import ru.yandex.market.marketpromo.service.AssortmentService;
import ru.yandex.market.marketpromo.test.MockedWebTestBase;
import ru.yandex.market.marketpromo.test.client.AssortmentRequests;
import ru.yandex.market.marketpromo.utils.IdentityUtils;
import ru.yandex.market.marketpromo.web.model.response.OfferItemsCountResponse;
import ru.yandex.market.marketpromo.web.model.response.OfferItemsPagingResponse;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.comparesEqualTo;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static ru.yandex.market.marketpromo.core.test.generator.Offers.activePromos;
import static ru.yandex.market.marketpromo.core.test.generator.Offers.basePrice;
import static ru.yandex.market.marketpromo.core.test.generator.Offers.categoryId;
import static ru.yandex.market.marketpromo.core.test.generator.Offers.datacampOffer;
import static ru.yandex.market.marketpromo.core.test.generator.Offers.disabled;
import static ru.yandex.market.marketpromo.core.test.generator.Offers.emptyStocks;
import static ru.yandex.market.marketpromo.core.test.generator.Offers.marketSku;
import static ru.yandex.market.marketpromo.core.test.generator.Offers.name;
import static ru.yandex.market.marketpromo.core.test.generator.Offers.potentialPromo;
import static ru.yandex.market.marketpromo.core.test.generator.Offers.price;
import static ru.yandex.market.marketpromo.core.test.generator.Offers.shop;
import static ru.yandex.market.marketpromo.core.test.generator.Offers.shopSku;
import static ru.yandex.market.marketpromo.core.test.generator.Offers.stocks;
import static ru.yandex.market.marketpromo.core.test.generator.Offers.supplierType;
import static ru.yandex.market.marketpromo.core.test.generator.Offers.warehouse;
import static ru.yandex.market.marketpromo.core.test.generator.PromoMechanics.minimalDiscountPercentSize;
import static ru.yandex.market.marketpromo.core.test.generator.Promos.directDiscount;
import static ru.yandex.market.marketpromo.core.test.generator.Promos.id;
import static ru.yandex.market.marketpromo.core.test.generator.Promos.promo;
import static ru.yandex.market.marketpromo.core.test.generator.Promos.promoName;

public class AssortmentApiControllerDirectDiscountFilterTest extends MockedWebTestBase {

    private static final long WAREHOUSE_ID = 123L;
    private static final long SHOP_ID = 12L;
    private static final String SSKU_1 = "ssku-1";
    private static final String SSKU_2 = "ssku-2";
    private static final String SSKU_3 = "ssku-3";
    private static final String DD_PROMO_ID = "#21098";
    private static final String DD_ANOTHER_PROMO_ID = "some another promo";
    private static final String DD_PROMO_NAME = "DD Promo";

    @Autowired
    private PromoDao promoDao;
    @Autowired
    private DatacampOfferDao datacampOfferDao;
    @Autowired
    private AssortmentService assortmentService;

    @Autowired
    private CachedAssortmentService cachedAssortmentService;

    private Promo ddPromo;
    private Promo anotherDdPromo;

    @BeforeEach
    void configure() {
        ddPromo = promoDao.replace(promo(
                id(IdentityUtils.hashId(DD_PROMO_ID)),
                promoName(DD_PROMO_NAME),
                directDiscount(
                        minimalDiscountPercentSize(10)
                )
        ));
        anotherDdPromo = promoDao.replace(promo(
                id(IdentityUtils.hashId(DD_ANOTHER_PROMO_ID)),
                promoName(DD_PROMO_NAME),
                directDiscount(
                        minimalDiscountPercentSize(10)
                )
        ));
    }

    @Test
    void shouldFilterByName() throws Exception {
        datacampOfferDao.replace(List.of(
                datacampOffer(
                        name("some name for searching"),
                        shopSku(SSKU_1),
                        warehouse(WAREHOUSE_ID),
                        shop(SHOP_ID),
                        price(1000),
                        basePrice(1500),
                        stocks(15L),
                        categoryId(123L),
                        potentialPromo(IdentityUtils.hashId(DD_PROMO_ID), BigDecimal.valueOf(2000))
                ),
                datacampOffer(
                        name("another name for searching"),
                        shopSku(SSKU_2),
                        warehouse(WAREHOUSE_ID),
                        shop(SHOP_ID),
                        price(1000),
                        basePrice(1500),
                        stocks(15L),
                        categoryId(123L),
                        potentialPromo(IdentityUtils.hashId(DD_PROMO_ID), BigDecimal.valueOf(2000))
                )
        ));

        cachedAssortmentService.refreshAssortmentCache();

        OfferItemsPagingResponse pagingResponse = AssortmentRequests.getAssortment(mockMvc,
                AssortmentRequest.builder(ddPromo.toPromoKey())
                        .filter(AssortmentFilter.NAME, "some")
                        .build());

        assertThat(pagingResponse, notNullValue());
        assertThat(pagingResponse.getItems(), hasSize(1));
        assertThat(pagingResponse.getItems(), hasItem(allOf(
                hasProperty("id"),
                hasProperty("name", is("some name for searching")),
                hasProperty("ssku", is(SSKU_1))
        )));

        OfferItemsCountResponse countResponse = AssortmentRequests.getAssortmentCount(mockMvc,
                AssortmentRequest.builder(ddPromo.toPromoKey())
                        .filter(AssortmentFilter.NAME, "some")
                        .build());

        assertThat(countResponse, notNullValue());
        assertThat(countResponse.getAppliedFilters(), notNullValue());
        assertThat(countResponse.getTotalItems(), comparesEqualTo(1L));
    }

    @Test
    void shouldFilterByMsku() throws Exception {
        datacampOfferDao.replace(List.of(
                datacampOffer(
                        name(SSKU_1),
                        shopSku(SSKU_1),
                        marketSku(123L),
                        warehouse(WAREHOUSE_ID),
                        shop(SHOP_ID),
                        price(1000),
                        basePrice(1500),
                        stocks(15L),
                        categoryId(123L),
                        potentialPromo(IdentityUtils.hashId(DD_PROMO_ID), BigDecimal.valueOf(2000))
                ),
                datacampOffer(
                        name(SSKU_2),
                        shopSku(SSKU_2),
                        marketSku(124L),
                        warehouse(WAREHOUSE_ID),
                        shop(SHOP_ID),
                        price(1000),
                        basePrice(1500),
                        stocks(15L),
                        categoryId(123L),
                        potentialPromo(IdentityUtils.hashId(DD_PROMO_ID), BigDecimal.valueOf(2000))
                ),
                datacampOffer(
                        name(SSKU_3),
                        shopSku(SSKU_3),
                        marketSku(125L),
                        warehouse(WAREHOUSE_ID),
                        shop(SHOP_ID),
                        price(1000),
                        basePrice(1500),
                        stocks(15L),
                        categoryId(123L),
                        potentialPromo(IdentityUtils.hashId(DD_PROMO_ID), BigDecimal.valueOf(2000))
                )
        ));

        cachedAssortmentService.refreshAssortmentCache();

        OfferItemsPagingResponse pagingResponse = AssortmentRequests.getAssortment(mockMvc,
                AssortmentRequest.builder(ddPromo.toPromoKey())
                        .filterList(AssortmentFilter.MSKU, List.of(123, 124))
                        .build());

        assertThat(pagingResponse, notNullValue());
        assertThat(pagingResponse.getItems(), hasSize(2));
        assertThat(pagingResponse.getItems(), hasItems(allOf(
                hasProperty("id"),
                hasProperty("name", is(SSKU_1)),
                hasProperty("ssku", is(SSKU_1))
        ), allOf(
                hasProperty("id"),
                hasProperty("name", is(SSKU_2)),
                hasProperty("ssku", is(SSKU_2))
        )));

        OfferItemsCountResponse countResponse = AssortmentRequests.getAssortmentCount(mockMvc,
                AssortmentRequest.builder(ddPromo.toPromoKey())
                        .filterList(AssortmentFilter.MSKU, List.of(123, 124))
                        .build());

        assertThat(countResponse, notNullValue());
        assertThat(countResponse.getAppliedFilters(), notNullValue());
        assertThat(countResponse.getTotalItems(), comparesEqualTo(2L));
    }

    @Test
    void shouldFilterByCategory() throws Exception {
        datacampOfferDao.replace(List.of(
                datacampOffer(
                        name(SSKU_1),
                        shopSku(SSKU_1),
                        marketSku(123L),
                        warehouse(WAREHOUSE_ID),
                        shop(SHOP_ID),
                        price(1000),
                        basePrice(1500),
                        stocks(15L),
                        categoryId(123L),
                        potentialPromo(IdentityUtils.hashId(DD_PROMO_ID), BigDecimal.valueOf(2000))
                ),
                datacampOffer(
                        name(SSKU_2),
                        shopSku(SSKU_2),
                        marketSku(124L),
                        warehouse(WAREHOUSE_ID),
                        shop(SHOP_ID),
                        price(1000),
                        basePrice(1500),
                        stocks(15L),
                        categoryId(123L),
                        potentialPromo(IdentityUtils.hashId(DD_PROMO_ID), BigDecimal.valueOf(2000))
                ),
                datacampOffer(
                        name(SSKU_3),
                        shopSku(SSKU_3),
                        marketSku(125L),
                        warehouse(WAREHOUSE_ID),
                        shop(SHOP_ID),
                        price(1000),
                        basePrice(1500),
                        stocks(15L),
                        categoryId(124L),
                        potentialPromo(IdentityUtils.hashId(DD_PROMO_ID), BigDecimal.valueOf(2000))
                )
        ));

        cachedAssortmentService.refreshAssortmentCache();

        OfferItemsPagingResponse pagingResponse = AssortmentRequests.getAssortment(mockMvc,
                AssortmentRequest.builder(ddPromo.toPromoKey())
                        .filter(AssortmentFilter.CATEGORY_ID, 123)
                        .build());

        assertThat(pagingResponse, notNullValue());
        assertThat(pagingResponse.getItems(), hasSize(2));
        assertThat(pagingResponse.getItems(), hasItems(allOf(
                hasProperty("id"),
                hasProperty("name", is(SSKU_1)),
                hasProperty("ssku", is(SSKU_1))
        ), allOf(
                hasProperty("id"),
                hasProperty("name", is(SSKU_2)),
                hasProperty("ssku", is(SSKU_2))
        )));

        OfferItemsCountResponse countResponse = AssortmentRequests.getAssortmentCount(mockMvc,
                AssortmentRequest.builder(ddPromo.toPromoKey())
                        .filter(AssortmentFilter.CATEGORY_ID, 123)
                        .build());

        assertThat(countResponse, notNullValue());
        assertThat(countResponse.getAppliedFilters(), notNullValue());
        assertThat(countResponse.getTotalItems(), comparesEqualTo(2L));
    }

    @Test
    void shouldFilterByShop() throws Exception {
        datacampOfferDao.replace(List.of(
                datacampOffer(
                        name(SSKU_1),
                        shopSku(SSKU_1),
                        marketSku(123L),
                        warehouse(WAREHOUSE_ID),
                        shop(SHOP_ID),
                        price(1000),
                        basePrice(1500),
                        stocks(15L),
                        categoryId(123L),
                        potentialPromo(IdentityUtils.hashId(DD_PROMO_ID), BigDecimal.valueOf(2000))
                ),
                datacampOffer(
                        name(SSKU_2),
                        shopSku(SSKU_2),
                        marketSku(124L),
                        warehouse(WAREHOUSE_ID),
                        shop(SHOP_ID),
                        price(1000),
                        basePrice(1500),
                        stocks(15L),
                        categoryId(123L),
                        potentialPromo(IdentityUtils.hashId(DD_PROMO_ID), BigDecimal.valueOf(2000))
                ),
                datacampOffer(
                        name(SSKU_3),
                        shopSku(SSKU_3),
                        marketSku(125L),
                        warehouse(WAREHOUSE_ID),
                        shop(13),
                        price(1000),
                        basePrice(1500),
                        stocks(15L),
                        categoryId(123L),
                        potentialPromo(IdentityUtils.hashId(DD_PROMO_ID), BigDecimal.valueOf(2000))
                )
        ));

        cachedAssortmentService.refreshAssortmentCache();

        OfferItemsPagingResponse pagingResponse = AssortmentRequests.getAssortment(mockMvc,
                AssortmentRequest.builder(ddPromo.toPromoKey())
                        .filter(AssortmentFilter.SUPPLIER_ID, 12)
                        .build());

        assertThat(pagingResponse, notNullValue());
        assertThat(pagingResponse.getItems(), hasSize(2));
        assertThat(pagingResponse.getItems(), hasItems(allOf(
                hasProperty("id"),
                hasProperty("name", is(SSKU_1)),
                hasProperty("ssku", is(SSKU_1))
        ), allOf(
                hasProperty("id"),
                hasProperty("name", is(SSKU_2)),
                hasProperty("ssku", is(SSKU_2))
        )));

        OfferItemsCountResponse countResponse = AssortmentRequests.getAssortmentCount(mockMvc,
                AssortmentRequest.builder(ddPromo.toPromoKey())
                        .filter(AssortmentFilter.SUPPLIER_ID, 12)
                        .build());

        assertThat(countResponse, notNullValue());
        assertThat(countResponse.getAppliedFilters(), notNullValue());
        assertThat(countResponse.getTotalItems(), comparesEqualTo(2L));
    }

    @Test
    void shouldFilterByParticipation() throws Exception {
        datacampOfferDao.replace(List.of(
                datacampOffer(
                        name(SSKU_1),
                        shopSku(SSKU_1),
                        marketSku(123L),
                        warehouse(WAREHOUSE_ID),
                        shop(SHOP_ID),
                        price(1000),
                        basePrice(1500),
                        stocks(15L),
                        categoryId(123L),
                        potentialPromo(IdentityUtils.hashId(DD_PROMO_ID), BigDecimal.valueOf(2000)),
                        activePromos(
                                DatacampOfferPromoMechanics.directDiscount(IdentityUtils.hashId(DD_PROMO_ID),
                                        DatacampOfferPromoMechanics.price(BigDecimal.valueOf(500))
                                )
                        )
                ),
                datacampOffer(
                        name(SSKU_2),
                        shopSku(SSKU_2),
                        marketSku(124L),
                        warehouse(WAREHOUSE_ID),
                        shop(SHOP_ID),
                        price(1000),
                        basePrice(1500),
                        stocks(15L),
                        categoryId(123L),
                        potentialPromo(IdentityUtils.hashId(DD_PROMO_ID), BigDecimal.valueOf(2000))
                )
        ));

        cachedAssortmentService.refreshAssortmentCache();

        OfferItemsPagingResponse pagingResponse = AssortmentRequests.getAssortment(mockMvc,
                AssortmentRequest.builder(ddPromo.toPromoKey())
                        .filter(AssortmentFilter.PARTICIPATE, true)
                        .build());

        assertThat(pagingResponse, notNullValue());
        assertThat(pagingResponse.getItems(), hasSize(1));
        assertThat(pagingResponse.getItems(), hasItem(allOf(
                hasProperty("id"),
                hasProperty("name", is(SSKU_1)),
                hasProperty("ssku", is(SSKU_1)),
                hasProperty("actualPromos", not(hasItems(
                        hasProperty("promoId", is(DD_PROMO_ID)))))
        )));

        OfferItemsCountResponse countResponse = AssortmentRequests.getAssortmentCount(mockMvc,
                AssortmentRequest.builder(ddPromo.toPromoKey())
                        .filter(AssortmentFilter.PARTICIPATE, true)
                        .build());

        assertThat(countResponse, notNullValue());
        assertThat(countResponse.getAppliedFilters(), notNullValue());
        assertThat(countResponse.getTotalItems(), comparesEqualTo(1L));
    }

    @Test
    void shouldNotShowDeletedActualParticipation() throws Exception {
        datacampOfferDao.replace(List.of(
                datacampOffer(
                        name(SSKU_1),
                        shopSku(SSKU_1),
                        marketSku(123L),
                        warehouse(WAREHOUSE_ID),
                        shop(SHOP_ID),
                        price(1000),
                        basePrice(1500),
                        stocks(15L),
                        categoryId(123L),
                        potentialPromo(IdentityUtils.hashId(DD_PROMO_ID), BigDecimal.valueOf(2000)),
                        activePromos(
                                DatacampOfferPromoMechanics.directDiscount(IdentityUtils.hashId(DD_PROMO_ID),
                                        DatacampOfferPromoMechanics.price(BigDecimal.valueOf(500))
                                )
                        )
                )
        ));

        datacampOfferDao.update(datacampOffer(
                name(SSKU_1),
                shopSku(SSKU_1),
                marketSku(123L),
                warehouse(WAREHOUSE_ID),
                shop(SHOP_ID),
                price(1000),
                basePrice(1500),
                stocks(15L),
                categoryId(123L),
                potentialPromo(IdentityUtils.hashId(DD_PROMO_ID), BigDecimal.valueOf(2000))
        ));

        cachedAssortmentService.refreshAssortmentCache();

        assortmentService.markToParticipate(ddPromo, List.of(
                OfferPromoParticipation.builder()
                        .promoId(ddPromo.getId())
                        .offerId(OfferId.of(IdentityUtils.hashId(SSKU_1), SHOP_ID))
                        .participate(false)
                        .build()
        ));

        OfferItemsPagingResponse pagingResponse = AssortmentRequests.getAssortment(mockMvc,
                AssortmentRequest.builder(ddPromo.toPromoKey())
                        .filter(AssortmentFilter.PARTICIPATE, true)
                        .build());

        assertThat(pagingResponse, notNullValue());
        assertThat(pagingResponse.getItems(), empty());

        OfferItemsCountResponse countResponse = AssortmentRequests.getAssortmentCount(mockMvc,
                AssortmentRequest.builder(ddPromo.toPromoKey())
                        .filter(AssortmentFilter.PARTICIPATE, true)
                        .build());

        assertThat(countResponse, notNullValue());
        assertThat(countResponse.getAppliedFilters(), notNullValue());
        assertThat(countResponse.getTotalItems(), comparesEqualTo(0L));
    }

    @Test
    void shouldNotHidePassiveParticipatedPromo() throws Exception {
        datacampOfferDao.replace(List.of(
                datacampOffer(
                        name(SSKU_1),
                        shopSku(SSKU_1),
                        marketSku(123L),
                        warehouse(WAREHOUSE_ID),
                        shop(SHOP_ID),
                        price(1000),
                        basePrice(1500),
                        stocks(15L),
                        categoryId(123L),
                        potentialPromo(IdentityUtils.hashId(DD_PROMO_ID), BigDecimal.valueOf(2000)),
                        potentialPromo(IdentityUtils.hashId(DD_ANOTHER_PROMO_ID), BigDecimal.valueOf(2000))
                ),
                datacampOffer(
                        name(SSKU_2),
                        shopSku(SSKU_2),
                        marketSku(124L),
                        warehouse(WAREHOUSE_ID),
                        shop(SHOP_ID),
                        price(1000),
                        basePrice(1500),
                        stocks(15L),
                        categoryId(123L),
                        potentialPromo(IdentityUtils.hashId(DD_PROMO_ID), BigDecimal.valueOf(2000)),
                        potentialPromo(IdentityUtils.hashId(DD_ANOTHER_PROMO_ID), BigDecimal.valueOf(2000))
                )
        ));

        cachedAssortmentService.refreshAssortmentCache();

        OfferItemsPagingResponse pagingResponse = AssortmentRequests.getAssortment(mockMvc,
                AssortmentRequest.builder(ddPromo.toPromoKey())
                        .filter(AssortmentFilter.HIDE_OTHER_ACTIVE_PROMOS, true)
                        .build());

        assertThat(pagingResponse, notNullValue());
        assertThat(pagingResponse.getItems(), hasSize(2));
        assertThat(pagingResponse.getItems(), hasItems(allOf(
                hasProperty("ssku", is(SSKU_1)),
                hasProperty("actualPromos", empty())
        ), allOf(
                hasProperty("ssku", is(SSKU_2)),
                hasProperty("actualPromos", empty())
        )));
    }

    @Test
    void shouldHideOtherActivePromos() throws Exception {
        datacampOfferDao.replace(List.of(
                datacampOffer(
                        name(SSKU_1),
                        shopSku(SSKU_1),
                        marketSku(123L),
                        warehouse(WAREHOUSE_ID),
                        shop(SHOP_ID),
                        price(1000),
                        basePrice(1500),
                        stocks(15L),
                        categoryId(123L),
                        potentialPromo(IdentityUtils.hashId(DD_PROMO_ID), BigDecimal.valueOf(2000)),
                        potentialPromo(IdentityUtils.hashId(DD_ANOTHER_PROMO_ID), BigDecimal.valueOf(2000)),
                        activePromos(
                                DatacampOfferPromoMechanics.directDiscount(IdentityUtils.hashId(DD_PROMO_ID),
                                        DatacampOfferPromoMechanics.price(BigDecimal.valueOf(500))
                                ),
                                DatacampOfferPromoMechanics.directDiscount(IdentityUtils.hashId(DD_ANOTHER_PROMO_ID),
                                        DatacampOfferPromoMechanics.price(BigDecimal.valueOf(500))
                                )
                        )
                ),
                datacampOffer(
                        name(SSKU_2),
                        shopSku(SSKU_2),
                        marketSku(124L),
                        warehouse(WAREHOUSE_ID),
                        shop(SHOP_ID),
                        price(1000),
                        basePrice(1500),
                        stocks(15L),
                        categoryId(123L),
                        potentialPromo(IdentityUtils.hashId(DD_PROMO_ID), BigDecimal.valueOf(2000))
                )
        ));

        cachedAssortmentService.refreshAssortmentCache();

        OfferItemsPagingResponse pagingResponse = AssortmentRequests.getAssortment(mockMvc,
                AssortmentRequest.builder(ddPromo.toPromoKey())
                        .filter(AssortmentFilter.HIDE_OTHER_ACTIVE_PROMOS, true)
                        .build());

        assertThat(pagingResponse, notNullValue());
        assertThat(pagingResponse.getItems(), hasSize(1));
        assertThat(pagingResponse.getItems(), hasItem(allOf(
                hasProperty("id"),
                hasProperty("name", is(SSKU_2)),
                hasProperty("ssku", is(SSKU_2))
        )));

        OfferItemsCountResponse countResponse = AssortmentRequests.getAssortmentCount(mockMvc,
                AssortmentRequest.builder(ddPromo.toPromoKey())
                        .filter(AssortmentFilter.HIDE_OTHER_ACTIVE_PROMOS, true)
                        .build());

        assertThat(countResponse, notNullValue());
        assertThat(countResponse.getAppliedFilters(), notNullValue());
        assertThat(countResponse.getTotalItems(), comparesEqualTo(1L));
    }

    @Test
    void shouldNotHideLocalActivePromos() throws Exception {
        datacampOfferDao.replace(List.of(
                datacampOffer(
                        name(SSKU_1),
                        shopSku(SSKU_1),
                        marketSku(123L),
                        warehouse(WAREHOUSE_ID),
                        shop(SHOP_ID),
                        supplierType(SupplierType._1P),
                        price(1000),
                        basePrice(1500),
                        stocks(15L),
                        categoryId(123L),
                        potentialPromo(IdentityUtils.hashId(DD_PROMO_ID), BigDecimal.valueOf(2000)),
                        potentialPromo(IdentityUtils.hashId(DD_ANOTHER_PROMO_ID), BigDecimal.valueOf(2000)),
                        activePromos(
                                DatacampOfferPromoMechanics.directDiscount(IdentityUtils.hashId(DD_PROMO_ID),
                                        DatacampOfferPromoMechanics.price(BigDecimal.valueOf(500))
                                )
                        )
                ),
                datacampOffer(
                        name(SSKU_2),
                        shopSku(SSKU_2),
                        marketSku(124L),
                        warehouse(WAREHOUSE_ID),
                        shop(SHOP_ID),
                        supplierType(SupplierType._1P),
                        price(1000),
                        basePrice(1500),
                        stocks(15L),
                        categoryId(123L),
                        potentialPromo(IdentityUtils.hashId(DD_PROMO_ID), BigDecimal.valueOf(2000)),
                        potentialPromo(IdentityUtils.hashId(DD_ANOTHER_PROMO_ID), BigDecimal.valueOf(2000))
                )
        ));

        cachedAssortmentService.refreshAssortmentCache();

        assortmentService.markToParticipate(anotherDdPromo, List.of(
                OfferPromoParticipation.builder()
                        .promoId(ddPromo.getId())
                        .offerId(OfferId.of(IdentityUtils.hashId(SSKU_1), SHOP_ID))
                        .participate(false)
                        .build()
        ));

        OfferItemsPagingResponse pagingResponse = AssortmentRequests.getAssortment(mockMvc,
                AssortmentRequest.builder(ddPromo.toPromoKey())
                        .filter(AssortmentFilter.HIDE_OTHER_ACTIVE_PROMOS, true)
                        .build());

        assertThat(pagingResponse, notNullValue());
        assertThat(pagingResponse.getItems(), hasSize(2));
        assertThat(pagingResponse.getItems(), hasItems(allOf(
                hasProperty("id"),
                hasProperty("ssku", is(SSKU_1)),
                hasProperty("actualPromos", empty())
        ), allOf(
                hasProperty("id"),
                hasProperty("ssku", is(SSKU_2)),
                hasProperty("actualPromos", empty())
        )));
    }

    @Test
    void shouldNotUseLocalInactivePromos() throws Exception {
        datacampOfferDao.replace(List.of(
                datacampOffer(
                        name(SSKU_1),
                        shopSku(SSKU_1),
                        marketSku(123L),
                        warehouse(WAREHOUSE_ID),
                        shop(SHOP_ID),
                        supplierType(SupplierType._1P),
                        price(1000),
                        basePrice(1500),
                        stocks(15L),
                        categoryId(123L),
                        potentialPromo(IdentityUtils.hashId(DD_PROMO_ID), BigDecimal.valueOf(2000)),
                        potentialPromo(IdentityUtils.hashId(DD_ANOTHER_PROMO_ID), BigDecimal.valueOf(2000))
                ),
                datacampOffer(
                        name(SSKU_2),
                        shopSku(SSKU_2),
                        marketSku(124L),
                        warehouse(WAREHOUSE_ID),
                        shop(SHOP_ID),
                        supplierType(SupplierType._1P),
                        price(1000),
                        basePrice(1500),
                        stocks(15L),
                        categoryId(123L),
                        potentialPromo(IdentityUtils.hashId(DD_PROMO_ID), BigDecimal.valueOf(2000)),
                        potentialPromo(IdentityUtils.hashId(DD_ANOTHER_PROMO_ID), BigDecimal.valueOf(2000))
                )
        ));

        cachedAssortmentService.refreshAssortmentCache();

        assortmentService.markDirectDiscountToParticipate(anotherDdPromo.toPromoKey(), List.of(
                DirectDiscountOfferParticipation.builder()
                        .offerPromoParticipation(OfferPromoParticipation.builder()
                                .promoId(anotherDdPromo.getId())
                                .offerId(OfferId.of(IdentityUtils.hashId(SSKU_2), SHOP_ID))
                                .participate(true)
                                .build())
                        .fixedBasePrice(BigDecimal.valueOf(1000))
                        .fixedPrice(BigDecimal.valueOf(100))
                        .build()
        ));

        OfferItemsPagingResponse pagingResponse = AssortmentRequests.getAssortment(mockMvc,
                AssortmentRequest.builder(ddPromo.toPromoKey())
                        .filter(AssortmentFilter.HIDE_OTHER_ACTIVE_PROMOS, true)
                        .build());

        assertThat(pagingResponse, notNullValue());
        assertThat(pagingResponse.getItems(), hasSize(2));
        assertThat(pagingResponse.getItems(), hasItems(allOf(
                hasProperty("id"),
                hasProperty("ssku", is(SSKU_1)),
                hasProperty("actualPromos", empty())
        ), allOf(
                hasProperty("id"),
                hasProperty("ssku", is(SSKU_2)),
                hasProperty("actualPromos", empty())
        )));
    }

    @Test
    @Disabled
    void shouldFilterOffersWithErrors() throws Exception {
        datacampOfferDao.replace(List.of(
                datacampOffer(
                        name(SSKU_1),
                        shopSku(SSKU_1),
                        marketSku(123L),
                        warehouse(WAREHOUSE_ID),
                        shop(SHOP_ID),
                        price(1000),
                        stocks(15L),
                        categoryId(123L),
                        potentialPromo(IdentityUtils.hashId(DD_PROMO_ID), BigDecimal.valueOf(2000)),
                        activePromos(
                                DatacampOfferPromoMechanics.directDiscount(IdentityUtils.hashId(DD_PROMO_ID),
                                        DatacampOfferPromoMechanics.price(BigDecimal.valueOf(500))
                                )
                        )
                ),
                datacampOffer(
                        name(SSKU_2),
                        shopSku(SSKU_2),
                        marketSku(124L),
                        warehouse(WAREHOUSE_ID),
                        shop(SHOP_ID),
                        price(1000),
                        stocks(15L),
                        categoryId(123L),
                        potentialPromo(IdentityUtils.hashId(DD_PROMO_ID), BigDecimal.valueOf(2000))
                )
        ));

        cachedAssortmentService.refreshAssortmentCache();

        assortmentService.markDirectDiscountToParticipate(ddPromo.toPromoKey(), List.of(
                DirectDiscountOfferParticipation.builder()
                        .offerPromoParticipation(OfferPromoParticipation.builder()
                                .offerId(OfferId.of(IdentityUtils.hashId(SSKU_1), SHOP_ID))
                                .promoId(ddPromo.getId())
                                .participate(true)
                                .build())
                        .fixedPrice(BigDecimal.valueOf(800))
                        .fixedBasePrice(BigDecimal.valueOf(810))
                        .build()
        ));
        assortmentService.markDirectDiscountToParticipate(ddPromo.toPromoKey(), List.of(
                DirectDiscountOfferParticipation.builder()
                        .offerPromoParticipation(OfferPromoParticipation.builder()
                                .offerId(OfferId.of(IdentityUtils.hashId(SSKU_2), SHOP_ID))
                                .promoId(ddPromo.getId())
                                .participate(true)
                                .build())
                        .fixedPrice(BigDecimal.valueOf(800))
                        .fixedBasePrice(BigDecimal.valueOf(3000))
                        .build()
        ));

        OfferItemsPagingResponse pagingResponse = AssortmentRequests.getAssortment(mockMvc,
                AssortmentRequest.builder(ddPromo.toPromoKey())
                        .filter(AssortmentFilter.HAS_ERRORS, true)
                        .build());

        assertThat(pagingResponse, notNullValue());
        assertThat(pagingResponse.getItems(), hasSize(1));
        assertThat(pagingResponse.getItems(), hasItem(allOf(
                hasProperty("id"),
                hasProperty("name", is(SSKU_1)),
                hasProperty("ssku", is(SSKU_1)),
                hasProperty("warnings", not(empty()))
        )));

        OfferItemsCountResponse countResponse = AssortmentRequests.getAssortmentCount(mockMvc,
                AssortmentRequest.builder(ddPromo.toPromoKey())
                        .filter(AssortmentFilter.HAS_ERRORS, true)
                        .build());

        assertThat(countResponse, notNullValue());
        assertThat(countResponse.getAppliedFilters(), notNullValue());
        assertThat(countResponse.getTotalItems(), comparesEqualTo(1L));
    }

    @Test
    void shouldHideDisabled() throws Exception {
        datacampOfferDao.replace(List.of(
                datacampOffer(
                        name(SSKU_1),
                        shopSku(SSKU_1),
                        marketSku(123L),
                        warehouse(WAREHOUSE_ID),
                        shop(SHOP_ID),
                        price(1000),
                        basePrice(1500),
                        stocks(15L),
                        categoryId(123L),
                        potentialPromo(IdentityUtils.hashId(DD_PROMO_ID), BigDecimal.valueOf(2000)),
                        activePromos(
                                DatacampOfferPromoMechanics.directDiscount(IdentityUtils.hashId(DD_PROMO_ID),
                                        DatacampOfferPromoMechanics.price(BigDecimal.valueOf(500))
                                )
                        ),
                        disabled(true)
                ),
                datacampOffer(
                        name(SSKU_2),
                        shopSku(SSKU_2),
                        marketSku(124L),
                        warehouse(WAREHOUSE_ID),
                        shop(SHOP_ID),
                        price(1000),
                        basePrice(1500),
                        stocks(15L),
                        categoryId(123L),
                        potentialPromo(IdentityUtils.hashId(DD_PROMO_ID), BigDecimal.valueOf(2000))
                )
        ));

        cachedAssortmentService.refreshAssortmentCache();

        OfferItemsPagingResponse pagingResponse = AssortmentRequests.getAssortment(mockMvc,
                AssortmentRequest.builder(ddPromo.toPromoKey())
                        .filter(AssortmentFilter.HIDE_DISABLED, true)
                        .build());

        assertThat(pagingResponse, notNullValue());
        assertThat(pagingResponse.getItems(), hasSize(1));
        assertThat(pagingResponse.getItems(), hasItem(allOf(
                hasProperty("id"),
                hasProperty("name", is(SSKU_2)),
                hasProperty("ssku", is(SSKU_2))
        )));

        OfferItemsCountResponse countResponse = AssortmentRequests.getAssortmentCount(mockMvc,
                AssortmentRequest.builder(ddPromo.toPromoKey())
                        .filter(AssortmentFilter.HIDE_DISABLED, true)
                        .build());

        assertThat(countResponse, notNullValue());
        assertThat(countResponse.getAppliedFilters(), notNullValue());
        assertThat(countResponse.getTotalItems(), comparesEqualTo(1L));
    }

    @Test
    void shouldHideAssortmentWithEmptyStocks() throws Exception {
        datacampOfferDao.replace(List.of(
                datacampOffer(
                        name(SSKU_1),
                        shopSku(SSKU_1),
                        marketSku(123L),
                        warehouse(WAREHOUSE_ID),
                        shop(SHOP_ID),
                        price(1000),
                        basePrice(1500),
                        stocks(15L),
                        categoryId(123L),
                        potentialPromo(IdentityUtils.hashId(DD_PROMO_ID), BigDecimal.valueOf(2000)),
                        activePromos(
                                DatacampOfferPromoMechanics.directDiscount(IdentityUtils.hashId(DD_PROMO_ID),
                                        DatacampOfferPromoMechanics.price(BigDecimal.valueOf(500))
                                )
                        )
                ),
                datacampOffer(
                        name(SSKU_2),
                        shopSku(SSKU_2),
                        marketSku(124L),
                        warehouse(WAREHOUSE_ID),
                        shop(SHOP_ID),
                        price(1000),
                        basePrice(1500),
                        emptyStocks(),
                        categoryId(123L),
                        potentialPromo(IdentityUtils.hashId(DD_PROMO_ID), BigDecimal.valueOf(2000))
                )
        ));

        cachedAssortmentService.refreshAssortmentCache();

        OfferItemsPagingResponse pagingResponse = AssortmentRequests.getAssortment(mockMvc,
                AssortmentRequest.builder(ddPromo.toPromoKey())
                        .filter(AssortmentFilter.HIDE_EMPTY_STOCKS, true)
                        .build());

        assertThat(pagingResponse, notNullValue());
        assertThat(pagingResponse.getItems(), hasSize(1));
        assertThat(pagingResponse.getItems(), hasItem(allOf(
                hasProperty("ssku", is(SSKU_1))
        )));

        OfferItemsCountResponse countResponse = AssortmentRequests.getAssortmentCount(mockMvc,
                AssortmentRequest.builder(ddPromo.toPromoKey())
                        .filter(AssortmentFilter.HIDE_EMPTY_STOCKS, true)
                        .build());

        assertThat(countResponse, notNullValue());
        assertThat(countResponse.getAppliedFilters(), notNullValue());
        assertThat(countResponse.getTotalItems(), comparesEqualTo(1L));
    }
}

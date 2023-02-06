package ru.yandex.market.marketpromo.web.controller;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.marketpromo.core.dao.DatacampOfferDao;
import ru.yandex.market.marketpromo.core.dao.PromoDao;
import ru.yandex.market.marketpromo.core.service.impl.CachedAssortmentService;
import ru.yandex.market.marketpromo.core.test.generator.DatacampOfferPromoMechanics;
import ru.yandex.market.marketpromo.filter.AssortmentRequest;
import ru.yandex.market.marketpromo.model.DirectDiscountOfferParticipation;
import ru.yandex.market.marketpromo.model.OfferId;
import ru.yandex.market.marketpromo.model.OfferPromoParticipation;
import ru.yandex.market.marketpromo.model.Promo;
import ru.yandex.market.marketpromo.service.AssortmentService;
import ru.yandex.market.marketpromo.test.MockedWebTestBase;
import ru.yandex.market.marketpromo.test.client.AssortmentRequests;
import ru.yandex.market.marketpromo.utils.IdentityUtils;
import ru.yandex.market.marketpromo.web.model.response.OfferItemsPagingResponse;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static ru.yandex.market.marketpromo.core.test.generator.Offers.activePromos;
import static ru.yandex.market.marketpromo.core.test.generator.Offers.basePrice;
import static ru.yandex.market.marketpromo.core.test.generator.Offers.categoryId;
import static ru.yandex.market.marketpromo.core.test.generator.Offers.datacampOffer;
import static ru.yandex.market.marketpromo.core.test.generator.Offers.disabled;
import static ru.yandex.market.marketpromo.core.test.generator.Offers.name;
import static ru.yandex.market.marketpromo.core.test.generator.Offers.potentialPromo;
import static ru.yandex.market.marketpromo.core.test.generator.Offers.price;
import static ru.yandex.market.marketpromo.core.test.generator.Offers.shop;
import static ru.yandex.market.marketpromo.core.test.generator.Offers.shopSku;
import static ru.yandex.market.marketpromo.core.test.generator.Offers.stocks;
import static ru.yandex.market.marketpromo.core.test.generator.Offers.warehouse;
import static ru.yandex.market.marketpromo.core.test.generator.PromoMechanics.minimalDiscountPercentSize;
import static ru.yandex.market.marketpromo.core.test.generator.Promos.directDiscount;
import static ru.yandex.market.marketpromo.core.test.generator.Promos.id;
import static ru.yandex.market.marketpromo.core.test.generator.Promos.promo;
import static ru.yandex.market.marketpromo.core.test.generator.Promos.promoName;

public class AssortmentApiControllerPromosTest extends MockedWebTestBase {

    private static final long WAREHOUSE_ID = 123L;
    private static final long SHOP_ID = 12L;
    private static final String SSKU_1 = "ssku-1";
    private static final String DD_PROMO_ID = "#21098";
    private static final String DD_PROMO_NAME = "DD Promo";

    @Autowired
    private AssortmentService assortmentService;
    @Autowired
    private PromoDao promoDao;
    @Autowired
    private DatacampOfferDao datacampOfferDao;
    @Autowired
    private CachedAssortmentService cachedAssortmentService;

    private Promo directDiscount;

    @BeforeEach
    void configure() {
        directDiscount = promoDao.replace(promo(
                id(IdentityUtils.hashId(DD_PROMO_ID)),
                promoName(DD_PROMO_NAME),
                directDiscount(
                        minimalDiscountPercentSize(10)
                )
        ));
    }

    @Test
    void shouldNptReturnCurrentPromoAsActivePromo() throws Exception {
        datacampOfferDao.replace(List.of(
                datacampOffer(
                        name(SSKU_1),
                        shopSku(SSKU_1),
                        warehouse(WAREHOUSE_ID),
                        shop(SHOP_ID),
                        price(1000),
                        basePrice(1500),
                        disabled(true),
                        stocks(15L),
                        categoryId(123L),
                        potentialPromo(IdentityUtils.hashId(DD_PROMO_ID), BigDecimal.valueOf(2000)),
                        activePromos(
                                DatacampOfferPromoMechanics.directDiscount(IdentityUtils.hashId(DD_PROMO_ID),
                                        DatacampOfferPromoMechanics.basePrice(BigDecimal.valueOf(2500)),
                                        DatacampOfferPromoMechanics.price(BigDecimal.valueOf(500))
                                )
                        )
                )
        ));

        cachedAssortmentService.refreshAssortmentCache();

        OfferItemsPagingResponse pagingResponse = AssortmentRequests.getAssortment(mockMvc,
                AssortmentRequest.builder(directDiscount.toPromoKey())
                        .build());

        assertThat(pagingResponse, notNullValue());
        assertThat(pagingResponse.getItems(), notNullValue());
        assertThat(pagingResponse.getItems(), hasItem(allOf(
                hasProperty("id"),
                hasProperty("name"),
                hasProperty("ssku", is(SSKU_1)),
                hasProperty("actualPromos", empty()),
                hasProperty("actualPromos", not(hasItem(
                        hasProperty("promoId", is(DD_PROMO_ID))
                ))))));
    }

    @Test
    void shouldNotReturnPotentialPromosAsActivePromos() throws Exception {
        datacampOfferDao.replace(List.of(
                datacampOffer(
                        name(SSKU_1),
                        shopSku(SSKU_1),
                        warehouse(WAREHOUSE_ID),
                        shop(SHOP_ID),
                        price(1000),
                        basePrice(1500),
                        disabled(true),
                        stocks(15L),
                        categoryId(123L),
                        potentialPromo(IdentityUtils.hashId(DD_PROMO_ID), BigDecimal.valueOf(2000))
                )
        ));

        cachedAssortmentService.refreshAssortmentCache();

        OfferItemsPagingResponse pagingResponse = AssortmentRequests.getAssortment(mockMvc,
                AssortmentRequest.builder(directDiscount.toPromoKey())
                        .build());

        assertThat(pagingResponse, notNullValue());
        assertThat(pagingResponse.getItems(), notNullValue());
        assertThat(pagingResponse.getItems(), hasItem(allOf(
                hasProperty("id"),
                hasProperty("name"),
                hasProperty("ssku", is(SSKU_1)),
                hasProperty("actualPromos", empty())
        )));
    }

    @Test
    void shouldNotReturnLocalPromosAsActivePromos() throws Exception {
        datacampOfferDao.replace(List.of(
                datacampOffer(
                        name(SSKU_1),
                        shopSku(SSKU_1),
                        warehouse(WAREHOUSE_ID),
                        shop(SHOP_ID),
                        price(1000),
                        basePrice(1500),
                        disabled(true),
                        stocks(15L),
                        categoryId(123L),
                        potentialPromo(IdentityUtils.hashId(DD_PROMO_ID), BigDecimal.valueOf(2000))
                )
        ));

        cachedAssortmentService.refreshAssortmentCache();

        assortmentService.markDirectDiscountToParticipate(directDiscount.toPromoKey(), List.of(
                DirectDiscountOfferParticipation.builder()
                        .offerPromoParticipation(OfferPromoParticipation.builder()
                                .offerId(OfferId.of(IdentityUtils.hashId(SSKU_1), SHOP_ID))
                                .promoId(directDiscount.getId())
                                .participate(true)
                                .build())
                        .fixedPrice(BigDecimal.valueOf(800))
                        .fixedBasePrice(BigDecimal.valueOf(3000))
                        .build()
        ));

        OfferItemsPagingResponse pagingResponse = AssortmentRequests.getAssortment(mockMvc,
                AssortmentRequest.builder(directDiscount.toPromoKey())
                        .build());

        assertThat(pagingResponse, notNullValue());
        assertThat(pagingResponse.getItems(), notNullValue());
        assertThat(pagingResponse.getItems(), hasItem(allOf(
                hasProperty("id"),
                hasProperty("name"),
                hasProperty("ssku", is(SSKU_1)),
                hasProperty("actualPromos", not(hasItem(
                        hasProperty("promoId", is(directDiscount.getId()))
                )))
        )));
    }
}

package ru.yandex.market.marketpromo.web.controller;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.marketpromo.core.dao.DatacampOfferDao;
import ru.yandex.market.marketpromo.core.dao.PromoDao;
import ru.yandex.market.marketpromo.core.service.impl.CachedAssortmentService;
import ru.yandex.market.marketpromo.filter.AssortmentRequest;
import ru.yandex.market.marketpromo.model.DirectDiscountOfferParticipation;
import ru.yandex.market.marketpromo.model.MechanicsType;
import ru.yandex.market.marketpromo.model.OfferId;
import ru.yandex.market.marketpromo.model.OfferPromoParticipation;
import ru.yandex.market.marketpromo.model.Promo;
import ru.yandex.market.marketpromo.model.SupplierType;
import ru.yandex.market.marketpromo.service.AssortmentService;
import ru.yandex.market.marketpromo.test.MockedWebTestBase;
import ru.yandex.market.marketpromo.test.client.AssortmentRequests;
import ru.yandex.market.marketpromo.utils.IdentityUtils;
import ru.yandex.market.marketpromo.web.model.response.OfferItemsPagingResponse;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.comparesEqualTo;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static ru.yandex.market.marketpromo.core.test.generator.Offers.DEFAULT_SHOP_ID;
import static ru.yandex.market.marketpromo.core.test.generator.Offers.basePrice;
import static ru.yandex.market.marketpromo.core.test.generator.Offers.categoryId;
import static ru.yandex.market.marketpromo.core.test.generator.Offers.datacampOffer;
import static ru.yandex.market.marketpromo.core.test.generator.Offers.name;
import static ru.yandex.market.marketpromo.core.test.generator.Offers.potentialPromo;
import static ru.yandex.market.marketpromo.core.test.generator.Offers.price;
import static ru.yandex.market.marketpromo.core.test.generator.Offers.shopSku;
import static ru.yandex.market.marketpromo.core.test.generator.Offers.stocks;
import static ru.yandex.market.marketpromo.core.test.generator.Offers.supplierType;
import static ru.yandex.market.marketpromo.core.test.generator.Offers.warehouse;
import static ru.yandex.market.marketpromo.core.test.generator.PromoMechanics.minimalDiscountPercentSize;
import static ru.yandex.market.marketpromo.core.test.generator.Promos.categoryMinimalDiscountPercentSize;
import static ru.yandex.market.marketpromo.core.test.generator.Promos.directDiscount;
import static ru.yandex.market.marketpromo.core.test.generator.Promos.id;
import static ru.yandex.market.marketpromo.core.test.generator.Promos.promo;

public class DirectDiscountAssortmentApiControllerTest extends MockedWebTestBase {

    private static final long WAREHOUSE_ID = 123L;
    private static final long CATEGORY_ID = 123L;
    private static final String SSKU_1 = "ssku-1";
    private static final String DD_PROMO_ID = "some direct discount promo";

    @Autowired
    private PromoDao promoDao;
    @Autowired
    private DatacampOfferDao datacampOfferDao;
    @Autowired
    private AssortmentService assortmentService;
    @Autowired
    private CachedAssortmentService cachedAssortmentService;

    @Test
    void shouldReturnZeroMinimalDiscountPercentIfNoSet() throws Exception {
        Promo directDiscount = promoDao.replace(promo(
                id(IdentityUtils.hashId(DD_PROMO_ID)),
                directDiscount()
        ));

        datacampOfferDao.replace(List.of(
                datacampOffer(
                        name(SSKU_1),
                        shopSku(SSKU_1),
                        warehouse(WAREHOUSE_ID),
                        price(1000),
                        basePrice(1500),
                        stocks(15L),
                        categoryId(CATEGORY_ID),
                        potentialPromo(IdentityUtils.hashId(DD_PROMO_ID), BigDecimal.valueOf(150))
                )
        ));

        cachedAssortmentService.refreshAssortmentCache();

        OfferItemsPagingResponse pagingResponse = AssortmentRequests.getAssortment(mockMvc,
                AssortmentRequest.builder(directDiscount.toPromoKey())
                        .build());

        assertThat(pagingResponse, notNullValue());
        assertThat(pagingResponse.getItems(), notNullValue());
        assertThat(pagingResponse.getItems(), hasItem(allOf(
                hasProperty("ssku", is(SSKU_1)),
                hasProperty("stocksByWarehouse", hasEntry(123L, 15L)),
                hasProperty("mechanicsType", is(MechanicsType.DIRECT_DISCOUNT)),
                hasProperty("mechanicsProperties", allOf(
                        hasProperty("minimalDiscountPercentSize", comparesEqualTo(BigDecimal.ZERO))
                ))
        )));
    }

    @Test
    void shouldReturnDefaultMinimalDiscountPercentIfNoCategorySet() throws Exception {
        Promo directDiscount = promoDao.replace(promo(
                id(IdentityUtils.hashId(DD_PROMO_ID)),
                directDiscount(
                        minimalDiscountPercentSize(BigDecimal.TEN)
                )
        ));

        datacampOfferDao.replace(List.of(
                datacampOffer(
                        name(SSKU_1),
                        shopSku(SSKU_1),
                        warehouse(WAREHOUSE_ID),
                        price(1000),
                        basePrice(1500),
                        stocks(15L),
                        categoryId(CATEGORY_ID),
                        potentialPromo(IdentityUtils.hashId(DD_PROMO_ID), BigDecimal.valueOf(150))
                )
        ));

        cachedAssortmentService.refreshAssortmentCache();


        OfferItemsPagingResponse pagingResponse = AssortmentRequests.getAssortment(mockMvc,
                AssortmentRequest.builder(directDiscount.toPromoKey())
                        .build());

        assertThat(pagingResponse, notNullValue());
        assertThat(pagingResponse.getItems(), notNullValue());
        assertThat(pagingResponse.getItems(), hasItem(allOf(
                hasProperty("ssku", is(SSKU_1)),
                hasProperty("stocksByWarehouse", hasEntry(123L, 15L)),
                hasProperty("mechanicsType", is(MechanicsType.DIRECT_DISCOUNT)),
                hasProperty("mechanicsProperties", allOf(
                        hasProperty("minimalDiscountPercentSize", comparesEqualTo(BigDecimal.TEN))
                ))
        )));
    }

    @Test
    void shouldReturnCategoryMinimalDiscountPercentIfSet() throws Exception {
        Promo directDiscount = promoDao.replace(promo(
                id(IdentityUtils.hashId(DD_PROMO_ID)),
                directDiscount(
                        minimalDiscountPercentSize(BigDecimal.TEN)
                ),
                categoryMinimalDiscountPercentSize(CATEGORY_ID, BigDecimal.valueOf(20))
        ));

        datacampOfferDao.replace(List.of(
                datacampOffer(
                        name(SSKU_1),
                        shopSku(SSKU_1),
                        warehouse(WAREHOUSE_ID),
                        price(1000),
                        basePrice(1500),
                        stocks(15L),
                        categoryId(CATEGORY_ID),
                        potentialPromo(IdentityUtils.hashId(DD_PROMO_ID), BigDecimal.valueOf(150))
                )
        ));

        cachedAssortmentService.refreshAssortmentCache();

        OfferItemsPagingResponse pagingResponse = AssortmentRequests.getAssortment(mockMvc,
                AssortmentRequest.builder(directDiscount.toPromoKey())
                        .build());

        assertThat(pagingResponse, notNullValue());
        assertThat(pagingResponse.getItems(), notNullValue());
        assertThat(pagingResponse.getItems(), hasItem(allOf(
                hasProperty("ssku", is(SSKU_1)),
                hasProperty("stocksByWarehouse", hasEntry(123L, 15L)),
                hasProperty("mechanicsType", is(MechanicsType.DIRECT_DISCOUNT)),
                hasProperty("mechanicsProperties", allOf(
                        hasProperty("minimalDiscountPercentSize", comparesEqualTo(BigDecimal.valueOf(20)))
                ))
        )));
    }

    @Test
    @Disabled
    void shouldResetWarningsOnResetParticipation() throws Exception {
        Promo directDiscount = promoDao.replace(promo(
                id(IdentityUtils.hashId(DD_PROMO_ID)),
                directDiscount(
                        minimalDiscountPercentSize(BigDecimal.TEN)
                ),
                categoryMinimalDiscountPercentSize(CATEGORY_ID, BigDecimal.valueOf(20))
        ));

        datacampOfferDao.replace(List.of(
                datacampOffer(
                        name(SSKU_1),
                        shopSku(SSKU_1),
                        warehouse(WAREHOUSE_ID),
                        price(1000),
                        stocks(15L),
                        categoryId(CATEGORY_ID),
                        supplierType(SupplierType._1P),
                        potentialPromo(IdentityUtils.hashId(DD_PROMO_ID), BigDecimal.valueOf(150))
                )
        ));

        cachedAssortmentService.refreshAssortmentCache();

        assortmentService.markDirectDiscountToParticipate(directDiscount.toPromoKey(), List.of(
                DirectDiscountOfferParticipation.builder()
                        .offerPromoParticipation(OfferPromoParticipation.builder()
                                .offerId(OfferId.of(IdentityUtils.hashId(SSKU_1), DEFAULT_SHOP_ID))
                                .promoId(directDiscount.getId())
                                .participate(true)
                                .build())
                        .fixedBasePrice(BigDecimal.valueOf(900))
                        .fixedPrice(BigDecimal.valueOf(800))
                        .build()
        ));

        OfferItemsPagingResponse pagingResponse = AssortmentRequests.getAssortment(mockMvc,
                AssortmentRequest.builder(directDiscount.toPromoKey())
                        .build());

        assertThat(pagingResponse, notNullValue());
        assertThat(pagingResponse.getItems(), notNullValue());
        assertThat(pagingResponse.getItems(), hasItem(allOf(
                hasProperty("ssku", is(SSKU_1)),
                hasProperty("warnings", not(empty()))
        )));

        assortmentService.markDirectDiscountToParticipate(directDiscount.toPromoKey(), List.of(
                DirectDiscountOfferParticipation.builder()
                        .offerPromoParticipation(OfferPromoParticipation.builder()
                                .offerId(OfferId.of(IdentityUtils.hashId(SSKU_1), DEFAULT_SHOP_ID))
                                .participate(false)
                                .promoId(directDiscount.getId())
                                .build())
                        .fixedPrice(BigDecimal.valueOf(800))
                        .build()
        ));

        pagingResponse = AssortmentRequests.getAssortment(mockMvc,
                AssortmentRequest.builder(directDiscount.toPromoKey())
                        .build());

        assertThat(pagingResponse, notNullValue());
        assertThat(pagingResponse.getItems(), notNullValue());
        assertThat(pagingResponse.getItems(), hasItem(allOf(
                hasProperty("ssku", is(SSKU_1)),
                hasProperty("warnings", empty())
        )));
    }
}

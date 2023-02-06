package ru.yandex.market.marketpromo.web.controller;

import java.math.BigDecimal;
import java.util.List;
import java.util.Queue;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import ru.yandex.market.marketpromo.core.dao.DatacampOfferDao;
import ru.yandex.market.marketpromo.core.dao.PromoDao;
import ru.yandex.market.marketpromo.core.data.source.logbroker.OfferLogbrokerEvent;
import ru.yandex.market.marketpromo.model.DirectDiscountOfferParticipation;
import ru.yandex.market.marketpromo.model.OfferId;
import ru.yandex.market.marketpromo.model.OfferPromoParticipation;
import ru.yandex.market.marketpromo.model.Promo;
import ru.yandex.market.marketpromo.model.PromoWarningCode;
import ru.yandex.market.marketpromo.service.AssortmentService;
import ru.yandex.market.marketpromo.test.MockedWebTestBase;
import ru.yandex.market.marketpromo.utils.IdentityUtils;
import ru.yandex.market.marketpromo.web.model.response.PromosPagingResponse;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.marketpromo.core.test.generator.Offers.categoryId;
import static ru.yandex.market.marketpromo.core.test.generator.Offers.datacampOffer;
import static ru.yandex.market.marketpromo.core.test.generator.Offers.name;
import static ru.yandex.market.marketpromo.core.test.generator.Offers.potentialPromo;
import static ru.yandex.market.marketpromo.core.test.generator.Offers.price;
import static ru.yandex.market.marketpromo.core.test.generator.Offers.shop;
import static ru.yandex.market.marketpromo.core.test.generator.Offers.shopSku;
import static ru.yandex.market.marketpromo.core.test.generator.Offers.warehouse;
import static ru.yandex.market.marketpromo.core.test.generator.PromoMechanics.minimalDiscountPercentSize;
import static ru.yandex.market.marketpromo.core.test.generator.Promos.directDiscount;
import static ru.yandex.market.marketpromo.core.test.generator.Promos.promo;

public class PromosApiControllerFiltersTest extends MockedWebTestBase {

    private static final int WAREHOUSE_ID = 123;
    private static final long SHOP_ID = 12L;
    private static final String SSKU_1 = "ssku-1";

    @Autowired
    private AssortmentService assortmentService;
    @Autowired
    private Queue<OfferLogbrokerEvent> mockedLogbrokerQueue;
    @Autowired
    private PromoDao promoDao;
    @Autowired
    private DatacampOfferDao datacampOfferDao;

    private Promo directDiscount;

    @BeforeEach
    void setUp() {
        mockedLogbrokerQueue.clear();

        directDiscount = promoDao.replace(promo(
                directDiscount(
                        minimalDiscountPercentSize(10)
                )
        ));

        datacampOfferDao.replace(List.of(
                datacampOffer(
                        name(SSKU_1),
                        shopSku(SSKU_1),
                        warehouse(WAREHOUSE_ID),
                        shop(SHOP_ID),
                        price(1000),
                        categoryId(123L),
                        potentialPromo(directDiscount.getId(), BigDecimal.valueOf(150))
                )
        ));
    }

    @AfterEach
    void clean() {
        mockedLogbrokerQueue.clear();
    }

    @Test
    @Disabled
    void shouldFilterPromosByHavingErrors() throws Exception {
        assortmentService.markDirectDiscountToParticipate(directDiscount.toPromoKey(), List.of(
                DirectDiscountOfferParticipation.builder()
                        .offerPromoParticipation(OfferPromoParticipation.builder()
                                .offerId(OfferId.of(IdentityUtils.hashId(SSKU_1), SHOP_ID))
                                .promoId(directDiscount.getId())
                                .participate(true)
                                .build())
                        .fixedPrice(BigDecimal.valueOf(800))
                        .fixedBasePrice(BigDecimal.valueOf(840))
                        .build()
        ));

        PromosPagingResponse promoResponse = objectMapper.readValue(
                mockMvc.perform(get("/v1/promos")
                        .param("hasErrors", "1")
                        .contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
                        .andExpect(status().is2xxSuccessful())
                        .andReturn().getResponse().getContentAsString(), PromosPagingResponse.class);

        assertThat(promoResponse, notNullValue());
        assertThat(promoResponse.getPromos(), notNullValue());
        assertThat(promoResponse.getPromos(), hasSize(1));
        assertThat(promoResponse.getPromos(), hasItem(allOf(
                hasProperty("promoId", is(directDiscount.getPromoId())),
                hasProperty("warnings", hasItem(PromoWarningCode.PARTICIPATION_WARNINGS))
        )));
    }

    @Test
    void shouldFilterPromosByHavingUnpublishedChanges() throws Exception {
        assortmentService.markDirectDiscountToParticipate(directDiscount.toPromoKey(), List.of(
                DirectDiscountOfferParticipation.builder()
                        .offerPromoParticipation(OfferPromoParticipation.builder()
                                .offerId(OfferId.of(IdentityUtils.hashId(SSKU_1), SHOP_ID))
                                .promoId(directDiscount.getId())
                                .participate(true)
                                .build())
                        .fixedPrice(BigDecimal.valueOf(800))
                        .fixedBasePrice(BigDecimal.valueOf(1800))
                        .build()
        ));

        PromosPagingResponse promoResponse = objectMapper.readValue(
                mockMvc.perform(get("/v1/promos")
                        .param("hasErrors", "1")
                        .contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
                        .andExpect(status().is2xxSuccessful())
                        .andReturn().getResponse().getContentAsString(), PromosPagingResponse.class);

        assertThat(promoResponse, notNullValue());
        assertThat(promoResponse.getPromos(), notNullValue());
        assertThat(promoResponse.getPromos(), hasSize(1));
        assertThat(promoResponse.getPromos(), hasItem(allOf(
                hasProperty("promoId", is(directDiscount.getPromoId())),
                hasProperty("warnings", hasItem(PromoWarningCode.CHANGES_NOT_PUBLISHED))
        )));
    }
}

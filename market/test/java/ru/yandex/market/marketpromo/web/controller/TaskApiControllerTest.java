package ru.yandex.market.marketpromo.web.controller;

import java.math.BigDecimal;
import java.util.List;

import javax.servlet.http.Cookie;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import ru.yandex.market.marketpromo.core.dao.DatacampOfferDao;
import ru.yandex.market.marketpromo.core.dao.LocalPromoOfferDao;
import ru.yandex.market.marketpromo.core.dao.PromoDao;
import ru.yandex.market.marketpromo.core.dao.internal.SchedulingLogDao;
import ru.yandex.market.marketpromo.core.service.task.CategoryTreeRefreshTask;
import ru.yandex.market.marketpromo.filter.AssortmentRequest;
import ru.yandex.market.marketpromo.model.LocalOffer;
import ru.yandex.market.marketpromo.model.PagerList;
import ru.yandex.market.marketpromo.model.Promo;
import ru.yandex.market.marketpromo.test.MockedWebTestBase;
import ru.yandex.market.marketpromo.utils.IdentityUtils;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.marketpromo.core.test.generator.Offers.basePrice;
import static ru.yandex.market.marketpromo.core.test.generator.Offers.categoryId;
import static ru.yandex.market.marketpromo.core.test.generator.Offers.datacampOffer;
import static ru.yandex.market.marketpromo.core.test.generator.Offers.name;
import static ru.yandex.market.marketpromo.core.test.generator.Offers.potentialPromo;
import static ru.yandex.market.marketpromo.core.test.generator.Offers.price;
import static ru.yandex.market.marketpromo.core.test.generator.Offers.shop;
import static ru.yandex.market.marketpromo.core.test.generator.Offers.shopSku;
import static ru.yandex.market.marketpromo.core.test.generator.Offers.warehouse;
import static ru.yandex.market.marketpromo.core.test.generator.PromoMechanics.minimalDiscountPercentSize;
import static ru.yandex.market.marketpromo.core.test.generator.Promos.DD_PROMO_KEY;
import static ru.yandex.market.marketpromo.core.test.generator.Promos.directDiscount;
import static ru.yandex.market.marketpromo.core.test.generator.Promos.id;
import static ru.yandex.market.marketpromo.core.test.generator.Promos.promo;

public class TaskApiControllerTest extends MockedWebTestBase {

    private static final int WAREHOUSE_ID = 123;
    private static final long SHOP_ID = 12L;
    private static final String SSKU_1 = "ssku-1";

    @Autowired
    private PromoDao promoDao;
    @Autowired
    private DatacampOfferDao offerDao;
    @Autowired
    private LocalPromoOfferDao localPromoOfferDao;
    @Autowired
    private SchedulingLogDao schedulingLogDao;

    private Promo directDiscount;

    @Test
    void shouldRunTask() throws Exception {
        mockMvc.perform(post("/v1/service/task/" + CategoryTreeRefreshTask.class.getSimpleName())
                .cookie(new Cookie("stub", "1"))
                .contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
                .andExpect(status().is2xxSuccessful());

        Thread.sleep(5000);

        assertThat(schedulingLogDao.lastLogs("categoryTreeRefreshTask", 1), hasSize(1));
    }

    @Test
    void shouldRefreshCache() throws Exception {

        directDiscount = promoDao.replace(promo(
                id(DD_PROMO_KEY.getId()),
                directDiscount(
                        minimalDiscountPercentSize(10)
                )
        ));

        offerDao.replace(List.of(
                datacampOffer(
                        name(SSKU_1),
                        shopSku(SSKU_1),
                        warehouse(WAREHOUSE_ID),
                        shop(SHOP_ID),
                        price(1000),
                        basePrice(1500),
                        categoryId(123L),
                        potentialPromo(directDiscount.getId(), BigDecimal.valueOf(150))
                )
        ));

        mockMvc.perform(post("/v1/service/refresh/" +
                IdentityUtils.encodePromoId(DD_PROMO_KEY.getMechanicsType(), DD_PROMO_KEY.getId()))
                .cookie(new Cookie("stub", "1"))
                .contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
                .andExpect(status().is2xxSuccessful());

        PagerList<LocalOffer> pagerList = localPromoOfferDao.getOffersByRequest(
                AssortmentRequest.builder(DD_PROMO_KEY)
                        .limit(100)
                        .build());

        assertThat(pagerList.getList(), hasSize(1));
    }
}

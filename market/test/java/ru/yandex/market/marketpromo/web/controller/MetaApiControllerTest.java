package ru.yandex.market.marketpromo.web.controller;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import ru.yandex.market.marketpromo.core.dao.PromoDao;
import ru.yandex.market.marketpromo.test.MockedWebTestBase;

import ru.yandex.market.marketpromo.web.model.response.MetaResponse;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.marketpromo.web.controller.MetaApiController.ALLOWED_ASSORTMENT_SORT_FIELDS;
import static ru.yandex.market.marketpromo.web.controller.MetaApiController.ALLOWED_OFFER_FILTERS;
import static ru.yandex.market.marketpromo.web.controller.MetaApiController.ALLOWED_PROMO_FILTERS;
import static ru.yandex.market.marketpromo.web.controller.MetaApiController.ALLOWED_PROMO_SORT_FIELDS;

public class MetaApiControllerTest extends MockedWebTestBase {

    @Autowired
    PromoDao promoDao;

    @Test
    void shouldRespondOnGetMeta() throws Exception {
        final MetaResponse metaResponse = objectMapper.readValue(mockMvc.perform(
                get("/v1/promos/meta")
                        .contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
                .andExpect(status().is2xxSuccessful())
                .andReturn().getResponse().getContentAsString(), MetaResponse.class);

        assertThat(metaResponse.getAllowedOfferFilters(), Matchers.is(
                Matchers.containsInAnyOrder(ALLOWED_OFFER_FILTERS.toArray())));
        assertThat(metaResponse.getAllowedAssortmentSortFields(), Matchers.is(
                Matchers.containsInAnyOrder(ALLOWED_ASSORTMENT_SORT_FIELDS.toArray())));
        assertThat(metaResponse.getAllowedPromoFilters(), Matchers.is(
                Matchers.containsInAnyOrder(ALLOWED_PROMO_FILTERS.toArray())));
        assertThat(metaResponse.getAllowedPromoSortFields(), Matchers.is(
                Matchers.containsInAnyOrder(ALLOWED_PROMO_SORT_FIELDS.toArray())));
    }
}

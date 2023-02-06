package ru.yandex.market.adv.promo.mvc.common.mapper;

import org.junit.jupiter.api.Test;

import ru.yandex.market.adv.promo.mvc.common.model.MethodAndPattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static ru.yandex.market.adv.promo.mvc.common.mapper.PageMatcherMapper.formatUrl;

class PageMatcherMapperTest {

    @Test
    void formatUrlTest() {
        String pageType = "test";
        assertEquals(
                "POST_partner_offer_price-info_by-ssku\tPOST:partner/offer/price-info/by-ssku\t" + pageType,
                formatUrl(new MethodAndPattern("POST", "partner/offer/price-info/by-ssku"), pageType)
        );
        assertEquals(
                "GET_monitoring_jobStatus_jobName\tGET:monitoring/jobStatus/<jobName>\t" + pageType,
                formatUrl(new MethodAndPattern("GET", "/monitoring/jobStatus/{jobName}"), pageType)
        );
    }
}

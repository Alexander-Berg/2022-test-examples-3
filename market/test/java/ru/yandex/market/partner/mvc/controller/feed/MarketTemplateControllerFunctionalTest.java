package ru.yandex.market.partner.mvc.controller.feed;

import java.util.List;

import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;

import ru.yandex.market.mbi.environment.EnvironmentService;
import ru.yandex.market.mbi.util.MbiMatchers;
import ru.yandex.market.mbi.util.MoreMbiMatchers;
import ru.yandex.market.partner.test.context.FunctionalTest;
import ru.yandex.market.partner.util.FunctionalTestHelper;

class MarketTemplateControllerFunctionalTest extends FunctionalTest {

    @Autowired
    private EnvironmentService environmentService;

    @Test
    void testGetMarketTemplates() {
        ResponseEntity<String> response =
                FunctionalTestHelper.get(baseUrl + "/campaigns/123/assortment/validations/market-templates");
        MatcherAssert.assertThat(response, MoreMbiMatchers
                .responseBodyMatches(MbiMatchers.jsonPropertyEquals("result", "" +
                        "{\"marketTemplates\":[\"OZON_ASSORTMENT\",\"WLB_ASSORTMENT\",\"SBER_MM_ASSORTMENT\"]}}")));
    }

    @Test
    void testGetMarketTemplatesWIthBlackList() {
        environmentService.setValues("united.catalog.upload.feed.template.blacklist",
                List.of("WLB_ASSORTMENT", "OZON_ASSORTMENT"));
        ResponseEntity<String> response =
                FunctionalTestHelper.get(baseUrl + "/campaigns/123/assortment/validations/market-templates");
        MatcherAssert.assertThat(response, MoreMbiMatchers
                .responseBodyMatches(MbiMatchers.jsonPropertyEquals("result", "" +
                        "{\"marketTemplates\":[\"SBER_MM_ASSORTMENT\"]}}")));
    }

    @Test
    void testGetMarketTemplatesWithoutCampaign() {
        ResponseEntity<String> response =
                FunctionalTestHelper.get(baseUrl + "validations/market-templates");
        MatcherAssert.assertThat(response, MoreMbiMatchers
                .responseBodyMatches(MbiMatchers.jsonPropertyEquals("result", "" +
                        "{\"marketTemplates\":[\"OZON_ASSORTMENT\",\"WLB_ASSORTMENT\",\"SBER_MM_ASSORTMENT\"]}}")));
    }
}

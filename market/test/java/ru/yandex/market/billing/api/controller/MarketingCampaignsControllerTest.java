package ru.yandex.market.billing.api.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultMatcher;

import ru.yandex.market.billing.api.config.MarketingCampaignsConfig;
import ru.yandex.market.billing.api.config.MvcConfig;
import ru.yandex.market.billing.api.controller.type.MarketingCampaignBillingType;
import ru.yandex.market.billing.api.controller.type.MarketingCampaignType;
import ru.yandex.market.billing.security.config.PassportConfig;

import static org.hamcrest.CoreMatchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = MarketingCampaignsController.class)
@AutoConfigureMockMvc(secure = false)
@ActiveProfiles("functionalTest")
@Import(MarketingCampaignsConfig.class)
class MarketingCampaignsControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockBean
    PassportConfig passportConfig;
    @MockBean
    MvcConfig mvcConfig;


    @Test
    @DisplayName("Проверка get запроса")
    void getMarketingCampaignCountTest() throws Exception {
        System.out.println(mockMvc.perform(get("/marketing/campaign-params")));
        mockMvc.perform(get("/marketing/campaign-params"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8))
                .andExpect(checkMarketingCampaignType(MarketingCampaignType.PROMO_BANNERS))
                .andExpect(checkMarketingCampaignType(MarketingCampaignType.PROMO_LANDING))
                .andExpect(checkMarketingCampaignType(MarketingCampaignType.PROMO_LIVE))
                .andExpect(checkMarketingCampaignType(MarketingCampaignType.PROMO_LANDING_BRAND_SHOP))
                .andExpect(checkMarketingCampaignType(MarketingCampaignType.PROMO_CONTENT))
                .andExpect(checkMarketingCampaignType(MarketingCampaignType.PROMO_OTHER))
                .andExpect(checkMarketingCampaignType(MarketingCampaignType.NOTIFICATIONS_EMAIL_INDIVIDUAL))
                .andExpect(checkMarketingCampaignType(MarketingCampaignType.NOTIFICATIONS_PUSH))
                .andExpect(checkMarketingCampaignType(MarketingCampaignType.NOTIFICATIONS_EMAIL_BULK))
                .andExpect(checkMarketingCampaignType(MarketingCampaignType.FEDERAL_PROMO))
                .andExpect(checkMarketingCampaignType(MarketingCampaignType.GENERIC_BUNDLE))
                .andExpect(checkMarketingCampaignType(MarketingCampaignType.BLUE_SET))
                .andExpect(checkMarketingCampaignType(MarketingCampaignType.CHEAPEST_AS_GIFT))
                .andExpect(checkMarketingCampaignType(MarketingCampaignType.MARKET_COUPON))
                .andExpect(checkMarketingCampaignType(MarketingCampaignType.MARKET_COIN))
                .andExpect(checkMarketingCampaignType(MarketingCampaignType.CASHBACK))
                .andExpect(checkMarketingCampaignType(MarketingCampaignType.UNIVERSAL_PROMO))
                .andExpect(checkMarketingCampaignType(MarketingCampaignType.PROMO_TV))
                .andExpect(checkMarketingCampaignType(MarketingCampaignType.EXTERNAL_PROMO))
                .andExpect(checkMarketingCampaignType(MarketingCampaignType.EXTERNAL_PROMO_CONTEXT))
                .andExpect(checkMarketingCampaignType(MarketingCampaignType.EXTERNAL_PROMO_OLV));

    }

    private ResultMatcher checkMarketingCampaignType(MarketingCampaignType marketingCampaignType) {
        return jsonPath("$.campaignTypes." + marketingCampaignType.getId() + ".isAnaplanIdRequired",
                is(isRequiredForCompensation(marketingCampaignType)));
    }

    private boolean isRequiredForCompensation(MarketingCampaignType marketingCampaignType) {
        return marketingCampaignType.getBillingType() == MarketingCampaignBillingType.COMPENSATIONAL;
    }
}

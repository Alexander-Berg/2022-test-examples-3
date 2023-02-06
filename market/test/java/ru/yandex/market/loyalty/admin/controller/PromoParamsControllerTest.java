package ru.yandex.market.loyalty.admin.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

import ru.yandex.market.loyalty.admin.config.MarketLoyaltyAdmin;
import ru.yandex.market.loyalty.admin.test.MarketLoyaltyAdminMockedDbTest;
import ru.yandex.market.loyalty.core.service.ConfigurationService;
import ru.yandex.market.loyalty.test.TestFor;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.log;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * @author <a href="mailto:khamitov-rail@yandex-team.ru">Rail Khamitov</a>
 * @date 04.02.2021
 */
@TestFor(PromoParamsController.class)
public class PromoParamsControllerTest extends MarketLoyaltyAdminMockedDbTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    @MarketLoyaltyAdmin
    private ObjectMapper objectMapper;


    @Test
    public void shouldGetCorrectUiPromoFlags() throws Exception {
        configurationService.set(ConfigurationService.UI_PROMO_FLAGS_AVAILABLE_LIST, "flag_1,flag_2,flag_3");
        List<String> uiPromoFlags = callRest();
        assertEquals(3, uiPromoFlags.size());
        assertTrue(uiPromoFlags.contains("flag_2"));
    }

    @Test
    public void shouldGetEmptyUiPromoFlags() throws Exception {
        configurationService.set(ConfigurationService.UI_PROMO_FLAGS_AVAILABLE_LIST, "");
        List<String> uiPromoFlags = callRest();
        assertEquals(0, uiPromoFlags.size());
    }

    private List<String> callRest() throws Exception {
        String jsonResponse = mockMvc
                .perform(get("/api/promo/params/getAvailableUiPromoFlags").with(csrf()))
                .andDo(log())
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readValue(jsonResponse, List.class);
    }

}

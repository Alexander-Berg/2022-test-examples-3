package ru.yandex.market.loyalty.back.controller;

import java.util.Map;
import java.util.Optional;

import com.fasterxml.jackson.core.type.TypeReference;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.loyalty.back.test.MarketLoyaltyBackMockedDbTestBase;
import ru.yandex.market.loyalty.core.dao.ConfigDao;
import ru.yandex.market.loyalty.test.TestFor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.log;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * @author <a href="mailto:khamitov-rail@yandex-team.ru">Rail Khamitov</a>
 * @date 13.04.2021
 */
@TestFor(LoyaltyConfigController.class)
public class LoyaltyConfigControllerTest extends MarketLoyaltyBackMockedDbTestBase {

    @Autowired
    private ConfigDao configDao;

    @Test
    public void shouldResetCache() throws Exception {
        configDao.put("test.key.check", "true");
        mockMvc
                .perform(get("/loyalty-config/resetLocalCache"))
                .andDo(log())
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        Optional<String> nullableVal = configurationService.getNullable("market.loyalty.config.test.key.check");
        assertTrue(nullableVal.isPresent() && Boolean.parseBoolean(nullableVal.get()));
    }

    @Test
    public void shouldGetLocalCache() throws Exception {
        configDao.put("test.key.check", "true");
        configurationService.getNullable("market.loyalty.config.test.check.null");
        configurationService.reloadCache();
        TypeReference<Map<String, String>> mapType =
                new TypeReference<>() {
                };
        Map<String, String> localCache = objectMapper.readValue(mockMvc
                .perform(get("/loyalty-config/getLocalCache"))
                .andDo(log())
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(), mapType);
        assertEquals(3, localCache.size());
    }

}

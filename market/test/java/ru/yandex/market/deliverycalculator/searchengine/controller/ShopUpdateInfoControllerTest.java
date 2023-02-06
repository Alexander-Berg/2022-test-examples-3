package ru.yandex.market.deliverycalculator.searchengine.controller;

import java.util.stream.Stream;

import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.deliverycalculator.searchengine.FunctionalTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Проверяем получение данных в {@link ShopUpdateInfoController}.
 */
@DbUnitDataSet(before = "db/ShopUpdateInfo.before.csv")
class ShopUpdateInfoControllerTest extends FunctionalTest {

    @ParameterizedTest
    @MethodSource("args")
    void getShopUpdateTimeInfo(long shopId, boolean expectedUseYml) throws Exception {
        String url = String.format("/shopUpdateTimeInfo?shopId=%d", shopId);
        String json = mockMvc.perform(get(url))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertEquals(shopId, new JSONObject(json).getLong("shopId"));
        assertEquals(expectedUseYml, new JSONObject(json).getBoolean("useYmlDelivery"));
    }

    private static Stream<Arguments> args() {
        return Stream.of(
                Arguments.of(774L, true),
                Arguments.of(775L, false)
        );
    }

    @Test
    void getShopUpdateTimeInfoUnknownShop() throws Exception {
        String url = String.format("/shopUpdateTimeInfo?shopId=%d", 776L);
        mockMvc.perform(get(url))
                .andExpect(status().is4xxClientError())
                .andExpect(content().json("{\"message\":\"Not found info for shop 776\"}"));
    }
}

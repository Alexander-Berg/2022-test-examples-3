package ru.yandex.market.loyalty.back.controller;

import org.junit.Test;
import org.springframework.web.util.UriComponentsBuilder;

import ru.yandex.market.loyalty.api.model.MarketPlatform;
import ru.yandex.market.loyalty.back.test.MarketLoyaltyBackMockedDbTestBase;
import ru.yandex.market.loyalty.test.TestFor;

import javax.annotation.Nullable;

import static org.junit.Assert.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.log;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@TestFor(TestController.class)
public class EnumParamOfControllerTest extends MarketLoyaltyBackMockedDbTestBase {

    @Test
    public void shouldHandleUnknownCodeInQueryParam() throws Exception {
        String colorParam = "red234234";
        checkParamInQuery(colorParam, MarketPlatform.UNKNOWN);
    }

    @Test
    public void shouldHandleLowerCaseInQueryParam() throws Exception {
        String colorParam = "red";
        checkParamInQuery(colorParam, MarketPlatform.RED);
    }

    @Test
    public void shouldHandleUpperCaseInQueryParam() throws Exception {
        String colorParam = "RED";
        checkParamInQuery(colorParam, MarketPlatform.RED);
    }

    @Test
    public void shouldHandleUnknownCodeInPath() throws Exception {
        String colorParam = "red234234";
        checkParamInPath(colorParam, MarketPlatform.UNKNOWN);
    }

    @Test
    public void shouldHandleLowerCaseInPath() throws Exception {
        String colorParam = "red";
        checkParamInPath(colorParam, MarketPlatform.RED);
    }

    @Test
    public void shouldHandleUpperCaseInPath() throws Exception {
        String colorParam = "RED";
        checkParamInPath(colorParam, MarketPlatform.RED);
    }

    @Test
    public void shouldHandleAbsentInPath() throws Exception {
        checkParamInPath(null, MarketPlatform.BLUE);
    }

    private void checkParamInQuery(String colorParam, MarketPlatform platform) throws Exception {
        UriComponentsBuilder ucb = UriComponentsBuilder
                .fromUriString("/for/test/withEnumParamInQuery")
                .queryParam("color", colorParam);

        String response = mockMvc
                .perform(get(ucb.build().encode().toUri()))
                .andDo(log())
                .andExpect(status().isOk())
                .andReturn()
                .getResponse().getContentAsString();
        assertEquals(platform.getCode(), response);
    }

    private void checkParamInPath(@Nullable String colorParam, MarketPlatform platform) throws Exception {
        String uri = "/for/test/withEnumParamInPath/";
        if (colorParam != null) {
            uri += colorParam;
        }
        UriComponentsBuilder ucb = UriComponentsBuilder
                .fromUriString(uri);

        String response = mockMvc
                .perform(get(ucb.build().encode().toUri()))
                .andDo(log())
                .andExpect(status().isOk())
                .andReturn()
                .getResponse().getContentAsString();
        assertEquals(platform.getCode(), response);
    }
}

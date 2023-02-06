package ru.yandex.market.checkout.checkouterpumpkin.controllers;

import java.time.Instant;
import java.time.ZoneId;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.springframework.http.MediaType;

import ru.yandex.market.checkout.checkouterpumpkin.BasePumpkinTest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class PumpkinCartControllerTest extends BasePumpkinTest {

    @BeforeEach
    public void setup() {
        testableClock.setFixed(Instant.parse("2021-09-08T12:00:00Z"), ZoneId.of("Europe/Moscow"));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "1_initial"
    })
    public void pumpkinCartTest(String name) throws Exception {
        var cartRq = loadResourceAsString(String.format("cart/%s_req.json", name));
        var expectedCartRs = loadResourceAsString(String.format("cart/%s_resp.json", name));

        var cartRs = mockMvc.perform(post("/cart")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cartRq))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        JSONAssert.assertEquals(expectedCartRs, cartRs, JSONCompareMode.NON_EXTENSIBLE);
    }

}

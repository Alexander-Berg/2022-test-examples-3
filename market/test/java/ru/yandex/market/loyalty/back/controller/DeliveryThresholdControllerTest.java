package ru.yandex.market.loyalty.back.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import org.junit.Test;
import org.springframework.test.web.servlet.MvcResult;

import ru.yandex.market.loyalty.api.model.delivery.DeliveryThresholdResponse;
import ru.yandex.market.loyalty.back.test.MarketLoyaltyBackRegionSettingsTest;
import ru.yandex.market.loyalty.test.TestFor;

import java.math.BigDecimal;
import java.util.List;

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.comparesEqualTo;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.log;

@TestFor(DeliveryThresholdController.class)
public class DeliveryThresholdControllerTest extends MarketLoyaltyBackRegionSettingsTest {

    private static final TypeReference<List<DeliveryThresholdResponse>> DELIVERY_THRESHOLD_RESPONSE_LIST_TYPE =
            new TypeReference<List<DeliveryThresholdResponse>>() {
            };

    @Test
    public void shouldReturnDeliveryThresholdCorrectly() throws Exception {
        MvcResult result = mockMvc.perform(get("/delivery/thresholds").param("regionId", String.valueOf(73)))
                .andDo(log())
                .andReturn();

        List<DeliveryThresholdResponse> response = objectMapper.readValue(
                result.getResponse().getContentAsByteArray(),
                DELIVERY_THRESHOLD_RESPONSE_LIST_TYPE
        );

        assertThat(response, contains(allOf(
                hasProperty("regionId", is(equalTo(73))),
                hasProperty("threshold", comparesEqualTo(BigDecimal.valueOf(7000)))
        )));
    }

    @Test
    public void shouldProcessMultipleRegions() throws Exception {
        MvcResult result = mockMvc.perform(get("/delivery/thresholds")
                .param("regionId", String.valueOf(73))
                .param("regionId", String.valueOf(76))
        )
                .andDo(log())
                .andReturn();

        List<DeliveryThresholdResponse> response = objectMapper.readValue(
                result.getResponse().getContentAsByteArray(),
                DELIVERY_THRESHOLD_RESPONSE_LIST_TYPE
        );

        assertThat(response, contains(
                allOf(
                        hasProperty("regionId", is(equalTo(73))),
                        hasProperty("threshold", comparesEqualTo(BigDecimal.valueOf(7000)))
                ),
                allOf(
                        hasProperty("regionId", is(equalTo(76))),
                        hasProperty("threshold", nullValue())
                )
        ));
    }
}

package ru.yandex.market.checkout.checkouter.controller;

import java.math.BigDecimal;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import ru.yandex.market.checkout.application.AbstractWebTestBase;

import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.checkout.checkouter.client.CheckoutCommonParams.X_EXPERIMENTS;
import static ru.yandex.market.checkout.checkouter.feature.type.common.MapFeatureType.MULTI_CART_MIN_COSTS_BY_REGION;
import static ru.yandex.market.checkout.checkouter.feature.type.common.MapFeatureType.MULTI_CART_MIN_COSTS_BY_REGION_EXPERIMENT;

public class CheckMultiCartCostTest extends AbstractWebTestBase {

    private static final String MULTICART_MINIMAL_COST_URI = "/check-multicart-min-cost";
    private static final String REGION_ID_PARAM = "regionId";
    private static final String MULTI_CART_COST_PARAM = "multiCartCost";

    @Test
    public void shouldThrow4xxErrorOnRegionIdMissing() throws Exception {
        mockMvc.perform(
                requestBuilder(null, "1000"))
                .andExpect(status().is4xxClientError());
    }

    @Test
    public void shouldThrow4xxErrorOnNonNumericRegionId() throws Exception {
        mockMvc.perform(
                requestBuilder(null, "1000"))
                .andExpect(status().is4xxClientError());
    }

    @Test
    public void shouldPassOnValidRegionId() throws Exception {
        mockMvc.perform(
                requestBuilder("1", null))
                .andExpect(status().isOk());
    }

    @Test
    public void shouldThrow4xxErrorOnInvalidMinimalCostValue() throws Exception {
        mockMvc.perform(
                requestBuilder("1", "0"))
                .andExpect(status().is4xxClientError());
    }

    @Test
    public void shouldThrow4xxErrorOnNonNumericMinimalCostValue() throws Exception {
        mockMvc.perform(
                requestBuilder("1", "QWERTY"))
                .andExpect(status().is4xxClientError());
    }

    @Test
    public void shouldPassOnValidMinimalCostValue() throws Exception {
        mockMvc.perform(
                requestBuilder("1", "1000"))
                .andExpect(status().isOk());
    }

    @Test
    public void shouldCheckPropertiesOnMultiCartCostMissing() throws Exception {
        setupMinimalMultiСartCost(Map.of(
                1L, new BigDecimal("999.99"),
                10L, new BigDecimal("5000")));

        mockMvc.perform(
                requestBuilder("1", null))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.minCost").value(999.99))
                .andExpect(jsonPath("$.remainingBeforeCheckout").value(0))
                .andExpect(jsonPath("$.errors", empty()));

        mockMvc.perform(
                requestBuilder("2", null))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.minCost").value(0))
                .andExpect(jsonPath("$.remainingBeforeCheckout").value(0))
                .andExpect(jsonPath("$.errors", empty()));

        mockMvc.perform(
                requestBuilder("10", null))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.minCost").value(5000))
                .andExpect(jsonPath("$.remainingBeforeCheckout").value(0))
                .andExpect(jsonPath("$.errors", empty()));
    }

    @Test
    public void shouldCheckMinimalCostForRemainingCostCalculation() throws Exception {
        setupMinimalMultiСartCost(Map.of(
                1L, new BigDecimal("999.99"),
                10L, new BigDecimal("5000")));
        setupMinimalMultiСartCostTest(Map.of(10L, new BigDecimal("3000")));

        mockMvc.perform(
                requestBuilder("1", "555.55"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.minCost").value(999.99))
                .andExpect(jsonPath("$.remainingBeforeCheckout").value(444.44))
                .andExpect(jsonPath("$.errors", hasSize(1)))
                .andExpect(jsonPath("$.errors[0]").value("TOO_CHEAP_MULTI_CART"));

        mockMvc.perform(
                requestBuilder("1", "999.99"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.minCost").value(999.99))
                .andExpect(jsonPath("$.remainingBeforeCheckout").value(0))
                .andExpect(jsonPath("$.errors", hasSize(0)));

        mockMvc.perform(
                requestBuilder("1", "1000"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.minCost").value(999.99))
                .andExpect(jsonPath("$.remainingBeforeCheckout").value(0))
                .andExpect(jsonPath("$.errors", hasSize(0)));

        mockMvc.perform(
                requestBuilder("10", "4999.99", "market_checkouter_cart_threshold=0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.minCost").value(5_000))
                .andExpect(jsonPath("$.remainingBeforeCheckout").value(0.01))
                .andExpect(jsonPath("$.errors", hasSize(1)))
                .andExpect(jsonPath("$.errors[0]").value("TOO_CHEAP_MULTI_CART"));
        mockMvc.perform(
                requestBuilder("10", "5000", "market_checkouter_cart_threshold=0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.minCost").value(5_000))
                .andExpect(jsonPath("$.remainingBeforeCheckout").value(0))
                .andExpect(jsonPath("$.errors", hasSize(0)));

        mockMvc.perform(
                requestBuilder("10", "2999.99", "market_checkouter_cart_threshold=1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.minCost").value(3_000))
                .andExpect(jsonPath("$.remainingBeforeCheckout").value(0.01))
                .andExpect(jsonPath("$.errors", hasSize(1)))
                .andExpect(jsonPath("$.errors[0]").value("TOO_CHEAP_MULTI_CART"));
        mockMvc.perform(
                requestBuilder("10", "3000", "market_checkouter_cart_threshold=1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.minCost").value(3_000))
                .andExpect(jsonPath("$.remainingBeforeCheckout").value(0))
                .andExpect(jsonPath("$.errors", hasSize(0)));

        mockMvc.perform(
                requestBuilder("2", "1000"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.minCost").value(0))
                .andExpect(jsonPath("$.remainingBeforeCheckout").value(0))
                .andExpect(jsonPath("$.errors", hasSize(0)));
    }

    private void setupMinimalMultiСartCostTest(Map<Long, BigDecimal> minCostsTest) {
        checkouterFeatureWriter.writeValue(MULTI_CART_MIN_COSTS_BY_REGION_EXPERIMENT, minCostsTest);
    }

    private void setupMinimalMultiСartCost(Map<Long, BigDecimal> minCosts) {
        checkouterFeatureWriter.writeValue(MULTI_CART_MIN_COSTS_BY_REGION, minCosts);
    }

    private MockHttpServletRequestBuilder requestBuilder(String regionId, String multiCartCost, String header) {
        MockHttpServletRequestBuilder builder = requestBuilder(regionId, multiCartCost);
        builder.header(X_EXPERIMENTS, header);

        return builder;
    }

    private MockHttpServletRequestBuilder requestBuilder(String regionId, String multiCartCost) {
        MockHttpServletRequestBuilder builder = MockMvcRequestBuilders
                .get(MULTICART_MINIMAL_COST_URI)
                .contentType(MediaType.APPLICATION_JSON);
        if (regionId != null) {
            builder.param(REGION_ID_PARAM, regionId);
        }
        if (multiCartCost != null) {
            builder.param(MULTI_CART_COST_PARAM, multiCartCost);
        }
        return builder;
    }
}

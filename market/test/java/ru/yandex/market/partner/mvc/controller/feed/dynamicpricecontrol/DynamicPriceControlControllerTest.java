package ru.yandex.market.partner.mvc.controller.feed.dynamicpricecontrol;

import java.util.Collections;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.HttpClientErrorException;

import ru.yandex.market.partner.test.context.FunctionalTest;
import ru.yandex.market.partner.util.FunctionalTestHelper;

import static org.hamcrest.MatcherAssert.assertThat;
import static ru.yandex.market.mbi.util.MbiMatchers.jsonPath;

/**
 * Тесты для {@link PartnerDynamicPriceControlController}
 */
@ParametersAreNonnullByDefault
class DynamicPriceControlControllerTest extends FunctionalTest {
    private String url;

    @Nonnull
    static HttpEntity<?> jsonRequest(String requestText) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        return new HttpEntity<>(requestText, headers);
    }

    @BeforeEach
    void initUrl() {
        url = String.format("%s/campaigns/%d/feed/dynamic-price-control", baseUrl, 10774L);
    }

    @Test
    void testDynamicPriceControlConfigUpdate() {
        //language=json
        String config = ""
                + "{"
                + "    \"maxAllowedDiscountPercent\": 55"
                + "}";

        HttpClientErrorException exception = Assertions.assertThrows(HttpClientErrorException.class,
                this::requestCurrentDynamicPriceControlConfig);
        Assertions.assertEquals(404, exception.getStatusCode().value());

        FunctionalTestHelper.put(url, jsonRequest(config));
        String current = requestCurrentDynamicPriceControlConfig();
        assertThat(current, jsonPath("$.maxAllowedDiscountPercent", "55"));
        assertThat(current, jsonPath("$.strategy", "buybox"));

        FunctionalTestHelper.delete(url);
        exception = Assertions.assertThrows(HttpClientErrorException.class,
                this::requestCurrentDynamicPriceControlConfig);
        Assertions.assertEquals(404, exception.getStatusCode().value());
    }

    @Test
    void testFractionalDynamicPriceControlConfigUpdate() {
        //language=json
        String config = ""
                + "{"
                + "    \"maxAllowedDiscountPercent\": 7.5"
                + "}";

        FunctionalTestHelper.put(url, jsonRequest(config));
        String current = requestCurrentDynamicPriceControlConfig();
        assertThat(current, jsonPath("$.maxAllowedDiscountPercent", "7.5"));
        assertThat(current, jsonPath("$.strategy", "buybox"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"buybox", "reference"})
    void testDynamicPriceControlStrategyUpdate(String strategy) {
        //language=json
        String config = ""
                + "{"
                + "    \"maxAllowedDiscountPercent\": 7.5,"
                + "    \"strategy\": \"" + strategy + "\""
                + "}";

        FunctionalTestHelper.put(url, jsonRequest(config));
        String current = requestCurrentDynamicPriceControlConfig();
        assertThat(current, jsonPath("$.maxAllowedDiscountPercent", "7.5"));
        assertThat(current, jsonPath("$.strategy", strategy));
    }

    @Test
    void testDynamicPriceControlWrongStrategy() {
        //language=json
        String config = ""
                + "{"
                + "    \"maxAllowedDiscountPercent\": 7.5,"
                + "    \"strategy\": \"wrong\""
                + "}";

        HttpClientErrorException exception = Assertions.assertThrows(HttpClientErrorException.class,
                () -> FunctionalTestHelper.put(url, jsonRequest(config)));
        Assertions.assertEquals(400, exception.getStatusCode().value());
        String body = exception.getResponseBodyAsString();
        assertThat(body, jsonPath("$.errors[0].code", "BAD_PARAM"));
        assertThat(body, jsonPath("$.errors[0].details.field", "strategy"));
        assertThat(body, jsonPath("$.errors[0].details.subcode", "INVALID"));
        assertThat(body, jsonPath("$.errors[0].details.value", "wrong"));

    }

    @ParameterizedTest
    @ValueSource(strings = {"0.01", "0.1", "1", "7.5", "99", "99.9", "100"})
    void testIsValidDynamicPriceControlConfigUpdate(String valueString) {
        //language=json
        String config = ""
                + "{"
                + "    \"maxAllowedDiscountPercent\": " + valueString
                + "}";
        FunctionalTestHelper.put(url, jsonRequest(config));
    }

    @ParameterizedTest
    @ValueSource(strings = {"-100", "-1", "-0.01", "-0.1", "0", "100.1", "100.01", "100.001", "101", "105", "200", "1000", "10000"})
    void testIsInvalidDynamicPriceControlConfigUpdate(String valueString) {
        //language=json
        String config = ""
                + "{"
                + "    \"maxAllowedDiscountPercent\": " + valueString
                + "}";

        HttpClientErrorException exception =
                Assertions.assertThrows(
                        HttpClientErrorException.class,
                        () -> FunctionalTestHelper.put(url, jsonRequest(config))
                );
        Assertions.assertEquals(400, exception.getStatusCode().value());
    }

    private String requestCurrentDynamicPriceControlConfig() {
        return FunctionalTestHelper.get(url).getBody();
    }
}

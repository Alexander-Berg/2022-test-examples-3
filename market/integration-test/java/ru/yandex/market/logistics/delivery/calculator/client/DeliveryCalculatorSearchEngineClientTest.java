package ru.yandex.market.logistics.delivery.calculator.client;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.Set;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.client.ResponseCreator;

import ru.yandex.market.logistics.delivery.calculator.client.model.DeliveryOption;
import ru.yandex.market.logistics.delivery.calculator.client.model.DeliveryOptionService;
import ru.yandex.market.logistics.delivery.calculator.client.model.DeliverySearchRequest;
import ru.yandex.market.logistics.delivery.calculator.client.model.DeliverySearchResponse;
import ru.yandex.market.logistics.delivery.calculator.client.model.TariffType;
import ru.yandex.market.logistics.delivery.calculator.client.model.enums.DeliveryOptionServicePriceRule;
import ru.yandex.market.logistics.delivery.calculator.client.model.enums.DeliveryServiceCode;

import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;

class DeliveryCalculatorSearchEngineClientTest extends AbstractClientTest {

    private static final DeliverySearchRequest DELIVERY_SEARCH_REQUEST =
        DeliverySearchRequest.builder()
            .locationFrom(1)
            .locationsTo(Set.of(2))
            .weight(new BigDecimal("20.3"))
            .length(10)
            .width(20)
            .height(30)
            .deliveryServiceIds(ImmutableSet.of(100L, 200L))
            .tariffType(TariffType.POST)
            .tariffId(22L)
            .pickupPoints(ImmutableSet.of(100500L, 200600L))
            .build();

    private static final DeliveryOption DELIVERY_OPTION = DeliveryOption.builder()
        .tariffId(22)
        .deliveryServiceId(100)
        .minDays(1)
        .maxDays(5)
        .cost(10020)
        .pickupPoints(Arrays.asList(100500L, 200600L))
        .tariffType(TariffType.POST)
        .services(ImmutableList.of(
            DeliveryOptionService.builder()
                .code(DeliveryServiceCode.CASH_SERVICE)
                .priceCalculationRule(DeliveryOptionServicePriceRule.PERCENT_CASH)
                .priceCalculationParameter(0.006)
                .minPrice(3000)
                .maxPrice(300000)
                .enabledByDefault(true)
                .paidByCustomer(false)
                .build()
        ))
        .build();

    @Autowired
    private DeliveryCalculatorSearchEngineClient searchEngineClient;

    /**
     * Проверяет сериализацию/десериализацию моделей при обращении к методу /deliverySearch через клиент.
     */
    @Test
    void deliverySearch() {
        prepareMockRequest("/daas/deliverySearch", "request/delivery_search.json", "response/delivery_search.json");
        final DeliverySearchResponse response = searchEngineClient.deliverySearch(DELIVERY_SEARCH_REQUEST);
        softly.assertThat(response).isNotNull();
        softly.assertThat(response).isEqualToComparingFieldByFieldRecursively(
            DeliverySearchResponse.builder().deliveryOptions(Collections.singletonList(DELIVERY_OPTION))
        );
    }

    private void prepareMockRequest(String path, String requestFile, String responseFile) {
        ResponseCreator taskResponseCreator = withStatus(OK)
            .contentType(APPLICATION_JSON)
            .body(getFileContent(responseFile));

        mock.expect(requestTo(uri + path))
            .andExpect(content().contentType(APPLICATION_JSON))
            .andExpect(content().json(getFileContent(requestFile), true))
            .andRespond(taskResponseCreator);
    }

}

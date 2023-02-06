package ru.yandex.market.fulfillment.wrap.marschroute.functional.scenario.concrete;

import org.springframework.http.HttpMethod;
import ru.yandex.market.fulfillment.wrap.core.scenario.builder.FulfillmentInteraction;
import ru.yandex.market.fulfillment.wrap.core.scenario.builder.FulfillmentUrl;
import ru.yandex.market.fulfillment.wrap.core.scenario.builder.FunctionalTestScenarioBuilder;
import ru.yandex.market.logistic.api.model.common.response.AbstractResponse;

import javax.annotation.Nonnull;
import java.util.Collections;

public class FunctionalTestScenarios {


    public static <T extends AbstractResponse> FunctionalTestScenarioBuilder<T> marschrouteOrderCreationWithCityCheck(
        @Nonnull Class<T> responseClass,
        @Nonnull String wrapRequestPath,
        @Nonnull String expectedMarschrouteRequestPath,
        @Nonnull String marschrouteResponsePath,
        @Nonnull String expectedWrapResponsePath,
        @Nonnull String kladr,
        @Nonnull String marschrouteDeliveryCityResponsePath
    ) {
        FulfillmentInteraction marschrouteInteraction = FulfillmentInteraction.createMarschrouteInteraction()
            .setFulfillmentUrl(FulfillmentUrl.fulfillmentUrl(Collections.singletonList("order"), HttpMethod.PUT))
            .setExpectedRequestPath(expectedMarschrouteRequestPath)
            .setResponsePath(marschrouteResponsePath);

        FulfillmentInteraction marschrouteDeliveryCity = FulfillmentInteraction.createMarschrouteInteraction()
            .setFulfillmentUrl(FulfillmentUrl.fulfillmentUrl(
                Collections.singletonList("delivery_city"),
                HttpMethod.GET,
                Collections.singletonMap("kladr", Collections.singletonList(kladr)))
            )
            .setResponsePath(marschrouteDeliveryCityResponsePath);

        return FunctionalTestScenarioBuilder.start(responseClass)
            .sendRequestToWrapQueryGateway(wrapRequestPath)
            .thenMockFulfillmentRequest(marschrouteDeliveryCity)
            .thenMockFulfillmentRequest(marschrouteInteraction)
            .andExpectWrapAnswerToBeEqualTo(expectedWrapResponsePath);

    }

    public static <T extends AbstractResponse> FunctionalTestScenarioBuilder<T> marschrouteMarketOrderCreation(
        @Nonnull Class<T> responseClass,
        @Nonnull String wrapRequestPath,
        @Nonnull String expectedMarschrouteRequestPath,
        @Nonnull String marschrouteResponsePath,
        @Nonnull String expectedWrapResponsePath
    ) {
        FulfillmentInteraction marschrouteInteraction = FulfillmentInteraction.createMarschrouteInteraction()
            .setFulfillmentUrl(FulfillmentUrl.fulfillmentUrl(Collections.singletonList("order"), HttpMethod.PUT))
            .setExpectedRequestPath(expectedMarschrouteRequestPath)
            .setResponsePath(marschrouteResponsePath);

        return FunctionalTestScenarioBuilder.start(responseClass)
            .sendRequestToWrapQueryGateway(wrapRequestPath)
            .thenMockFulfillmentRequest(marschrouteInteraction)
            .andExpectWrapAnswerToBeEqualTo(expectedWrapResponsePath);

    }


    public static <T extends AbstractResponse> FunctionalTestScenarioBuilder<T> marschrouteOrderCreationEmptyCityId(
        @Nonnull Class<T> responseClass,
        @Nonnull String wrapRequestPath,
        @Nonnull String expectedMarschrouteRequestPath,
        @Nonnull String marschrouteResponsePath,
        @Nonnull String expectedWrapResponsePath
    ) {
        FulfillmentInteraction marschrouteInteraction = FulfillmentInteraction.createMarschrouteInteraction()
            .setFulfillmentUrl(FulfillmentUrl.fulfillmentUrl(Collections.singletonList("order"), HttpMethod.PUT))
            .setExpectedRequestPath(expectedMarschrouteRequestPath)
            .setResponsePath(marschrouteResponsePath);
        return FunctionalTestScenarioBuilder.start(responseClass)
            .sendRequestToWrapQueryGateway(wrapRequestPath)
            .thenMockFulfillmentRequest(marschrouteInteraction)
            .andExpectWrapAnswerToBeEqualTo(expectedWrapResponsePath);

    }
}

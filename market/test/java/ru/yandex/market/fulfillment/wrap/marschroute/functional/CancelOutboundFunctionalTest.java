package ru.yandex.market.fulfillment.wrap.marschroute.functional;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import javax.annotation.Nonnull;

import com.google.common.collect.ImmutableMap;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;

import ru.yandex.market.fulfillment.wrap.core.scenario.builder.FulfillmentInteraction;
import ru.yandex.market.fulfillment.wrap.core.scenario.builder.FulfillmentUrl;
import ru.yandex.market.fulfillment.wrap.core.scenario.builder.FunctionalTestScenarioBuilder;
import ru.yandex.market.fulfillment.wrap.marschroute.entity.OutboundInfo;
import ru.yandex.market.fulfillment.wrap.marschroute.functional.configuration.IntegrationTest;
import ru.yandex.market.fulfillment.wrap.marschroute.model.base.MarschrouteStockType;
import ru.yandex.market.logistic.api.model.common.ErrorCode;
import ru.yandex.market.logistic.api.model.common.ErrorPair;
import ru.yandex.market.logistic.api.model.common.response.AbstractResponse;

import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

class CancelOutboundFunctionalTest extends IntegrationTest {

    private static final String YANDEX_ID = "outbound-1000";
    private static final String PARTNER_ID = "666555444";

    /**
     * Проверяет сценарий, в котором происходит отмена изъятия со стока А.
     * <p>
     * Прослойка должна обратиться в ручку отмены заказа с соответствующим partnerId из запроса.
     */
    @Test
    void cancelOutboundForFitStock() throws Exception {
        executePositiveScenario(
            new OutboundInfo(YANDEX_ID, MarschrouteStockType.A, LocalDateTime.now()),
            FulfillmentUrl.fulfillmentUrl(Arrays.asList("order", PARTNER_ID), HttpMethod.DELETE),
            "functional/cancel_outbound/fit/marschroute_response.json"
        );
    }

    /**
     * Проверяет сценарий, в котором происходит отмена изъятия со стока D.
     * <p>
     * Прослойка должна обратиться в ручку отмены накладной с соответствующим partnerId из запроса.
     */
    @Test
    void cancelOutboundForNotFitStock() throws Exception {
        executePositiveScenario(
            new OutboundInfo(YANDEX_ID, MarschrouteStockType.D, LocalDateTime.now()),
            FulfillmentUrl.fulfillmentUrl(Arrays.asList("waybill", PARTNER_ID), HttpMethod.DELETE),
            "functional/cancel_outbound/not_fit/marschroute_response.json"
        );
    }


    /**
     * Проверяет сценарий, в котором приходит запрос на отмену изъятия,
     * информация о котором не сохранена в БД прослойки.
     * <p>
     * Прослойка должна выбросить соответствующую ошибку.
     */
    @Test
    void cancelUnknownOutbound() throws Exception {
        given(outboundInfoRepository.findByYandexId(YANDEX_ID)).willReturn(Optional.empty());

        FunctionalTestScenarioBuilder.start(AbstractResponse.class)
            .sendRequestToWrapQueryGateway("functional/cancel_outbound/wrap_request.xml")
            .andExpectWrapAnswerToMeetRequirements((response, assertions) -> {
                List<ErrorPair> errorCodes = response.getRequestState().getErrorCodes();

                assertions.assertThat(errorCodes)
                    .as("Assert that error codes contain only 1 value")
                    .hasSize(1);

                ErrorPair errorCode = errorCodes.get(0);

                assertions.assertThat(errorCode.getCode())
                    .as("Assert that error code belongs to unknown error")
                    .isEqualTo(ErrorCode.UNKNOWN_ERROR);

                assertions.assertThat(errorCode.getMessage())
                    .as("Assert message value ")
                    .contains("Failed to find information about outbound with id [" + YANDEX_ID + "]");
            })
            .build(mockMvc, restTemplate, fulfillmentMapper, apiUrl, apiKey)
            .start();

        then(outboundInfoRepository).should().findByYandexId(YANDEX_ID);
    }


    /**
     * Проверяет, что в случае отсутствия yandex_id при отмене изъятия -
     * запрос не пройдет валидацию.
     */
    @Test
    void outboundIdValidationOnMissingYandexId() throws Exception {
        testOutboundIdValidation("functional/cancel_outbound/negative/missing_yandex_id.xml");
    }

    /**
     * Проверяет, что в случае отсутствия partner_id при отмене изъятия -
     * запрос не пройдет валидацию.
     */
    @Test
    void outboundIdValidationOnMissingPartnerId() throws Exception {
        testOutboundIdValidation("functional/cancel_outbound/negative/missing_partner_id.xml");
    }

    /**
     * Проверяет, что в случае отсутствия yandex_id и partner_id при отмене изъятия -
     * запрос не пройдет валидацию.
     */
    @Test
    void outboundIdValidationOnBothIdsMissing() throws Exception {
        testOutboundIdValidation("functional/cancel_outbound/negative/missing_both.xml");
    }

    private void executePositiveScenario(@Nonnull OutboundInfo outboundInfo,
                                         @Nonnull FulfillmentUrl fulfillmentUrl,
                                         @Nonnull String marschrouteResponsePath) throws Exception {
        given(outboundInfoRepository.findByYandexId(YANDEX_ID)).willReturn(
            Optional.of(outboundInfo)
        );

        FulfillmentInteraction interaction = FulfillmentInteraction.createMarschrouteInteraction()
            .setFulfillmentUrl(fulfillmentUrl)
            .setResponsePath(marschrouteResponsePath);

        FunctionalTestScenarioBuilder.start(AbstractResponse.class)
            .sendRequestToWrapQueryGateway("functional/cancel_outbound/wrap_request.xml")
            .thenMockFulfillmentRequest(interaction)
            .andExpectWrapAnswerToBeEqualTo("functional/cancel_outbound/wrap_response.xml")
            .build(mockMvc, restTemplate, fulfillmentMapper, apiUrl, apiKey)
            .start();

        then(outboundInfoRepository).should().findByYandexId(YANDEX_ID);
    }

    private void testOutboundIdValidation(String requestPath) throws Exception {
        FunctionalTestScenarioBuilder.start(AbstractResponse.class)
            .sendRequestToWrapQueryGateway(requestPath)
            .andExpectWrapAnswerToContainErrors(ImmutableMap.of(ErrorCode.BAD_REQUEST, 1))
            .build(mockMvc, restTemplate, fulfillmentMapper, apiUrl, apiKey)
            .start();
    }
}

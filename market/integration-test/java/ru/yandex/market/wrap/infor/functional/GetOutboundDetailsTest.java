package ru.yandex.market.wrap.infor.functional;

import java.util.Arrays;

import com.github.springtestdbunit.annotation.DatabaseOperation;
import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;

import ru.yandex.market.fulfillment.wrap.core.scenario.builder.FulfillmentInteraction;
import ru.yandex.market.fulfillment.wrap.core.scenario.builder.FunctionalTestScenarioBuilder;
import ru.yandex.market.logistic.api.model.fulfillment.response.GetOutboundDetailsResponse;

import static ru.yandex.market.fulfillment.wrap.core.scenario.builder.FulfillmentUrl.fulfillmentUrl;

/**
 * Тесты проверки получения деталей изъятия.
 *
 * @author avetokhin 14.11.18.
 */
@DatabaseSetup(
    connection = "wrapConnection",
    value = "classpath:fixtures/functional/get_outbound_details/state.xml",
    type = DatabaseOperation.CLEAN_INSERT
)
class GetOutboundDetailsTest extends AbstractFunctionalTest {

    private static final String OUTBOUND_ID = "0000000001";

    /**
     * Сценарий #1:
     * <p>
     * Запрашиваются детали существующего подготовленного изъятия, которое еще не забрали.
     */
    @Test
    void getOutboundDetailsPositive() throws Exception {
        executeScenario(
            "fixtures/functional/get_outbound_details/1/wrap_request.xml",
            "fixtures/functional/get_outbound_details/1/wrap_response.xml",
            inforInteraction(fulfillmentUrl(
                Arrays.asList(clientProperties.getWarehouseKey(), "shipments", OUTBOUND_ID), HttpMethod.GET)
            )
                .setResponsePath("fixtures/functional/get_outbound_details/1/get_shipments_response.json")

        );
    }

    /**
     * Сценарий #2:
     * <p>
     * Запрашиваются детали существующего подготовленного изъятия с кривым маппингом.
     */
    @Test
    void getOutboundDetailsNoMapping() throws Exception {
        executeScenario(
            "fixtures/functional/get_outbound_details/2/wrap_request.xml",
            "fixtures/functional/get_outbound_details/2/wrap_response.xml",
            inforInteraction(fulfillmentUrl(
                Arrays.asList(clientProperties.getWarehouseKey(), "shipments", OUTBOUND_ID), HttpMethod.GET)
            )
                .setResponsePath("fixtures/functional/get_outbound_details/2/get_shipments_response.json")

        );
    }

    /**
     * Сценарий #3:
     * <p>
     * Запрашиваются детали не существующего изъятия.
     */
    @Test
    void getOutboundDetailsNotFound() throws Exception {
        executeScenario(
            "fixtures/functional/get_outbound_details/3/wrap_request.xml",
            "fixtures/functional/get_outbound_details/3/wrap_response.xml",
            inforInteraction(fulfillmentUrl(
                Arrays.asList(clientProperties.getWarehouseKey(), "shipments", OUTBOUND_ID), HttpMethod.GET)
            ).setResponsePath("fixtures/functional/common/failed_client_response.json")
                .setResponseStatus(HttpStatus.NOT_FOUND)

        );
    }

    /**
     * Сценарий #4:
     * <p>
     * Запрашиваются детали с кривыми входными данными.
     */
    @Test
    void getOutboundDetailsInvalidInputData() throws Exception {
        executeScenario(
            "fixtures/functional/get_outbound_details/4/wrap_request.xml",
            "fixtures/functional/get_outbound_details/4/wrap_response.xml"
        );
    }

    /**
     * Сценарий #5:
     * <p>
     * Запрашиваются детали существующего не подготовленного изъятия.
     */
    @Test
    void getInboundDetailsNotReady() throws Exception {
        executeScenario(
            "fixtures/functional/get_outbound_details/5/wrap_request.xml",
            "fixtures/functional/get_outbound_details/5/wrap_response.xml",
            inforInteraction(fulfillmentUrl(
                Arrays.asList(clientProperties.getWarehouseKey(), "shipments", OUTBOUND_ID), HttpMethod.GET)
            )
                .setResponsePath("fixtures/functional/get_outbound_details/5/get_shipments_response.json")

        );
    }

    /**
     * Сценарий #6:
     * <p>
     * Запрашиваются детали существующего подготовленного изъятия, которое уже забрали.
     */
    @Test
    void getOutboundDetailsPositiveWhenAlreadyTransferred() throws Exception {
        executeScenario(
            "fixtures/functional/get_outbound_details/6/wrap_request.xml",
            "fixtures/functional/get_outbound_details/6/wrap_response.xml",
            inforInteraction(fulfillmentUrl(
                Arrays.asList(clientProperties.getWarehouseKey(), "shipments", OUTBOUND_ID), HttpMethod.GET)
            )
                .setResponsePath("fixtures/functional/get_outbound_details/6/get_shipments_response.json")
        );
    }

    /**
     * Сценарий #7:
     * <p>
     * Запрашиваются детали существующего изъятия, у которого детали находятся в разных статусах.
     * Изъятие имеет {@link StatusCode#ASSEMBLED}, детали в разных статусах, соответствующих разным полям для
     * определения в ответе Инфора действительного собранного количества.
     */
    @Test
    void getOutboundDetailsWithDifferentDetailsStatuses() throws Exception {
        executeScenario(
            "fixtures/functional/get_outbound_details/7/wrap_request.xml",
            "fixtures/functional/get_outbound_details/7/wrap_response.xml",
            inforInteraction(fulfillmentUrl(
                Arrays.asList(clientProperties.getWarehouseKey(), "shipments", OUTBOUND_ID), HttpMethod.GET)
            )
                .setResponsePath("fixtures/functional/get_outbound_details/7/get_shipments_response.json")
        );
    }


    /**
     * Сценарий #8:
     * <p>
     * Запрашиваются детали существующего изъятия, у которого детали находятся в разных статусах.
     * Изъятие имеет {@link StatusCode#ASSEMBLED}, детали в разных статусах, одна деталь
     * имеет статус ниже {@link StatusCode#ASSEMBLED}. Возвращается ошибка.
     */
    @Test
    void getOutboundDetailsWithDetailStatusAssembled() throws Exception {
        executeScenario(
            "fixtures/functional/get_outbound_details/8/wrap_request.xml",
            "fixtures/functional/get_outbound_details/8/wrap_response.xml",
            inforInteraction(fulfillmentUrl(
                Arrays.asList(clientProperties.getWarehouseKey(), "shipments", OUTBOUND_ID), HttpMethod.GET)
            )
                .setResponsePath("fixtures/functional/get_outbound_details/8/get_shipments_response.json")
        );
    }

    /**
     * Сценарий #9:
     * <p>
     * Запрашиваются детали существующего подготовленного изъятия, которое уже забрали.
     * Изъятие имеет товар, который был удален.
     */
    @Test
    void getOutboundDetailsPositiveWithRemovedDetails() throws Exception {
        executeScenario(
                "fixtures/functional/get_outbound_details/9/wrap_request.xml",
                "fixtures/functional/get_outbound_details/9/wrap_response.xml",
                inforInteraction(fulfillmentUrl(
                        Arrays.asList(clientProperties.getWarehouseKey(), "shipments", OUTBOUND_ID), HttpMethod.GET)
                ).setResponsePath("fixtures/functional/get_outbound_details/9/get_shipments_response.json")
        );
    }

    private void executeScenario(String wrapRequest,
                                 String wrapResponse,
                                 FulfillmentInteraction... interactions) throws Exception {
        FunctionalTestScenarioBuilder.start(GetOutboundDetailsResponse.class)
            .sendRequestToWrapQueryGateway(wrapRequest)
            .thenMockFulfillmentRequests(interactions)
            .andExpectWrapAnswerToBeEqualTo(wrapResponse)
            .build(mockMvc, restTemplate, fulfillmentMapper, clientProperties.getUrl())
            .start();
    }
}

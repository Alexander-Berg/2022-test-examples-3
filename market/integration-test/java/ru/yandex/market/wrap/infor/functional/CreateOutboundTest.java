package ru.yandex.market.wrap.infor.functional;

import java.util.Arrays;

import com.github.springtestdbunit.annotation.DatabaseOperation;
import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;

import ru.yandex.market.fulfillment.wrap.core.scenario.builder.FulfillmentInteraction;
import ru.yandex.market.fulfillment.wrap.core.scenario.builder.FunctionalTestScenarioBuilder;
import ru.yandex.market.logistic.api.model.fulfillment.response.CreateOutboundResponse;

import static ru.yandex.market.fulfillment.wrap.core.scenario.builder.FulfillmentUrl.fulfillmentUrl;

/**
 * Тесты проверки логики создания изъятий.
 *
 * @author avetokhin 12.11.18.
 */
@DatabaseSetup(
    connection = "wrapConnection",
    value = "classpath:fixtures/functional/create_outbound/state.xml",
    type = DatabaseOperation.CLEAN_INSERT
)
class CreateOutboundTest extends AbstractFunctionalTest {

    /**
     * Сценарий #1:
     * <p>
     * Успешное создание изъятия с годного стока.
     */
    @Test
    void createSuccessfullyFit() throws Exception {
        executeScenario(
            "fixtures/functional/create_outbound/1/wrap_request.xml",
            "fixtures/functional/create_outbound/1/wrap_response.xml",
            inforInteraction(fulfillmentUrl(
                Arrays.asList(clientProperties.getWarehouseKey(), "shipments"), HttpMethod.POST)
            )
                .setExpectedRequestPath("fixtures/functional/create_outbound/1/create_shipments_request.json")
                .setResponsePath("fixtures/functional/create_outbound/1/create_shipments_response.json")
        );
    }

    /**
     * Сценарий #2:
     * <p>
     * Успешное создание изъятия брака.
     */
    @Test
    void createSuccessfullyDefect() throws Exception {
        executeScenario(
            "fixtures/functional/create_outbound/2/wrap_request.xml",
            "fixtures/functional/create_outbound/2/wrap_response.xml",
            inforInteraction(fulfillmentUrl(
                Arrays.asList(clientProperties.getWarehouseKey(), "shipments"), HttpMethod.POST)
            )
                .setExpectedRequestPath("fixtures/functional/create_outbound/2/create_shipments_request.json")
                .setResponsePath("fixtures/functional/create_outbound/2/create_shipments_response.json")
        );

    }

    /**
     * Сценарий #3:
     * <p>
     * Успешное создание изъятия просрочки.
     */
    @Test
    void createSuccessfullyExpired() throws Exception {
        executeScenario(
            "fixtures/functional/create_outbound/3/wrap_request.xml",
            "fixtures/functional/create_outbound/3/wrap_response.xml",
            inforInteraction(fulfillmentUrl(
                Arrays.asList(clientProperties.getWarehouseKey(), "shipments"), HttpMethod.POST)
            )
                .setExpectedRequestPath("fixtures/functional/create_outbound/3/create_shipments_request.json")
                .setResponsePath("fixtures/functional/create_outbound/3/create_shipments_response.json")
        );
    }

    /**
     * Сценарий #4:
     * <p>
     * Попытка изъятия с неподдерживаемого стока.
     * Ничего не создает, возвращает ошибку. 
     */
    @Test
    void createUnsupportedStock() throws Exception {
        executeScenario(
            "fixtures/functional/create_outbound/4/wrap_request.xml",
            "fixtures/functional/create_outbound/4/wrap_response.xml"
        );
    }

    /**
     * Сценарий #5:
     * <p>
     * Попытка изъятия, когда изъятие с таким ID уже существует.
     * Ничего не создает, просто возвращает идентификатор существующего изъятия.
     */
    @Test
    @DatabaseSetup(
        value = "classpath:fixtures/functional/create_outbound/5/state.xml",
        connection = "wmsConnection"
    )
    void createWhenAlreadyExists() throws Exception {
        executeScenario(
            "fixtures/functional/create_outbound/5/wrap_request.xml",
            "fixtures/functional/create_outbound/5/wrap_response.xml"
        );
    }

    /**
     * Сценарий #6:
     * <p>
     * Попытка изъятия, когда нет необходимого маппинга товарных предложений.
     * Ничего не создает, возвращает ошибку.
     */
    @Test
    void createWhenNoMapping() throws Exception {
        executeScenario(
            "fixtures/functional/create_outbound/6/wrap_request.xml",
            "fixtures/functional/create_outbound/6/wrap_response.xml"
        );
    }

    /**
     * Сценарий #7:
     * <p>
     * Попытка изъятия, когда не указан идентификатор изъятия в запросе.
     * Ничего не создает, возвращает ошибку.
     */
    @Test
    void createYandexIdNotSet() throws Exception {
        executeScenario(
            "fixtures/functional/create_outbound/7/wrap_request.xml",
            "fixtures/functional/create_outbound/7/wrap_response.xml"
        );
    }

    /**
     * Сценарий #8:
     * <p>
     * Успешное создание изъятия излишков.
     */
    @Test
    void createSuccessfullySurplus() throws Exception {
        executeScenario(
            "fixtures/functional/create_outbound/8/wrap_request.xml",
            "fixtures/functional/create_outbound/8/wrap_response.xml",
            inforInteraction(fulfillmentUrl(
                Arrays.asList(clientProperties.getWarehouseKey(), "shipments"), HttpMethod.POST)
            )
                .setExpectedRequestPath("fixtures/functional/create_outbound/8/create_shipments_request.json")
                .setResponsePath("fixtures/functional/create_outbound/8/create_shipments_response.json")
        );
    }

    private void executeScenario(String wrapRequest,
                                 String wrapResponse,
                                 FulfillmentInteraction... interactions) throws Exception {
        FunctionalTestScenarioBuilder.start(CreateOutboundResponse.class)
            .sendRequestToWrapQueryGateway(wrapRequest)
            .thenMockFulfillmentRequests(interactions)
            .andExpectWrapAnswerToBeEqualTo(wrapResponse)
            .build(mockMvc, restTemplate, fulfillmentMapper, clientProperties.getUrl())
            .start();

    }

}

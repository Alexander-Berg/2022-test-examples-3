package ru.yandex.market.wrap.infor.functional;

import java.util.Arrays;

import com.github.springtestdbunit.annotation.DatabaseOperation;
import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;

import ru.yandex.market.fulfillment.wrap.core.scenario.builder.FulfillmentInteraction;
import ru.yandex.market.fulfillment.wrap.core.scenario.builder.FunctionalTestScenarioBuilder;
import ru.yandex.market.logistic.api.model.fulfillment.response.CreateOrderResponse;

import static ru.yandex.market.fulfillment.wrap.core.scenario.builder.FulfillmentUrl.fulfillmentUrl;

/**
 * Тесты проверки создания заказа.
 *
 * @author avetokhin 17.10.18.
 */
@DatabaseSetup(
    connection = "wrapConnection",
    value = "classpath:fixtures/functional/create_order/state.xml",
    type = DatabaseOperation.CLEAN_INSERT
)
@DatabaseSetup(
    connection = "wmsConnection",
    value = "classpath:fixtures/functional/create_order/wms_state.xml",
    type = DatabaseOperation.CLEAN_INSERT
)
class CreateOrderTest extends AbstractFunctionalTestWithIrisCommunication {

    /**
     * Сценарий #1:
     * <p>
     * Попытка создания заказа без указания order.orderId.yandexId. Ошибка валидации.
     */
    @Test
    void createOrderWithoutYandexId() throws Exception {
        executeScenario(
            "fixtures/functional/create_order/1/wrap_request.xml",
            "fixtures/functional/create_order/1/wrap_response.xml"
        );
    }

    /**
     * Сценарий #2:
     * <p>
     * Успешное создание нового заказа.
     */
    @Test
    void createOrderSuccessfully() throws Exception {
        executeScenario(
            "fixtures/functional/create_order/2/wrap_request.xml",
            "fixtures/functional/create_order/2/wrap_response.xml",

            putStorerInteraction(),

            inforInteraction(fulfillmentUrl(
                Arrays.asList(clientProperties.getWarehouseKey(), "shipments"), HttpMethod.POST)
            )
                .setExpectedRequestPath("fixtures/functional/create_order/2/create_shipments_request.json")
                .setResponsePath("fixtures/functional/create_order/2/create_shipments_response.json")
        );
    }

    /**
     * Сценарий #3:
     * <p>
     * Попытка повторного создания заказа. Должен вернуться уже существующий идентификатор.
     */
    @Test
    @DatabaseSetup(
        value = "classpath:fixtures/functional/create_order/3/state.xml",
        connection = "wmsConnection"
    )
    void createOrderSecondary() throws Exception {
        executeScenario(
            "fixtures/functional/create_order/3/wrap_request.xml",
            "fixtures/functional/create_order/3/wrap_response.xml"
        );
    }

    /**
     * Сценарий #4:
     * <p>
     * Ошибка клиента при создании нового заказа.
     */
    @Test
    void createOrderFailure() throws Exception {
        executeScenario(
            "fixtures/functional/create_order/4/wrap_request.xml",
            "fixtures/functional/create_order/4/wrap_response.xml",

            putStorerInteraction(),

            inforInteraction(fulfillmentUrl(
                Arrays.asList(clientProperties.getWarehouseKey(), "shipments"), HttpMethod.POST)
            ).setResponsePath("fixtures/functional/common/failed_client_response.json")
                .setResponseStatus(HttpStatus.NOT_FOUND)
        );
    }

    /**
     * Сценарий #5:
     * <p>
     * Успешное создание нового заказа в случае нагрузочного тестирования.
     * Приходит запрос от checkouter-shooting@yandex-team.ru -> создаем заказ со специальным типом: 47.
     */
    @Test
    void createOrderInCaseOfLoadTesting() throws Exception {
        executeScenario(
            "fixtures/functional/create_order/5/wrap_request.xml",
            "fixtures/functional/create_order/5/wrap_response.xml",

            putStorerInteraction(),

            inforInteraction(fulfillmentUrl(
                Arrays.asList(clientProperties.getWarehouseKey(), "shipments"), HttpMethod.POST)
            )
                .setExpectedRequestPath("fixtures/functional/create_order/5/create_shipments_request.json")
                .setResponsePath("fixtures/functional/create_order/5/create_shipments_response.json")

        );
    }

    /**
     * Сценарий #6:
     * <p>
     * Успешное создание нового заказа в случае, когда email заказчика не задан.
     */
    @Test
    void createOrderWithoutUserEmail() throws Exception {
        executeScenario(
            "fixtures/functional/create_order/6/wrap_request.xml",
            "fixtures/functional/create_order/6/wrap_response.xml",

            putStorerInteraction(),

            inforInteraction(fulfillmentUrl(
                Arrays.asList(clientProperties.getWarehouseKey(), "shipments"), HttpMethod.POST)
            )
                .setExpectedRequestPath("fixtures/functional/create_order/6/create_shipments_request.json")
                .setResponsePath("fixtures/functional/create_order/6/create_shipments_response.json")

        );
    }

    /**
     * Сценарий #7:
     * <p>
     * Успешное создание нового заказа, в ФИО которого есть запрещенные символы.
     */
    @Test
    void createOrderSuccessfullyIllegalSymbols() throws Exception {
        executeScenario(
            "fixtures/functional/create_order/7/wrap_request.xml",
            "fixtures/functional/create_order/7/wrap_response.xml",

            putStorerInteraction(),

            inforInteraction(fulfillmentUrl(
                Arrays.asList(clientProperties.getWarehouseKey(), "shipments"), HttpMethod.POST)
            )
                .setExpectedRequestPath("fixtures/functional/create_order/7/create_shipments_request.json")
                .setResponsePath("fixtures/functional/create_order/7/create_shipments_response.json")
        );
    }

    /**
     * Сценарий #8:
     * <p>
     * Успешное создание нового заказа для двух товаров, один из которых присутсвует в БД, а второй - нет.
     * В данном случае произойдет поход в IRIS для получения данной информации.
     */
    @Test
    void createOrderWithNotExistingItemSuccessfully() throws Exception {
        mockIrisCommunication("fixtures/functional/create_order/8/iris_request.json",
            "fixtures/functional/create_order/8/iris_response.json");

        executeScenario(
            "fixtures/functional/create_order/8/wrap_request.xml",
            "fixtures/functional/create_order/8/wrap_response.xml",

            putStorerInteraction(),

            inforInteraction(fulfillmentUrl(Arrays.asList(
                clientProperties.getEnterpriseKey(), "items", "itembatch"), HttpMethod.POST)
            ).setExpectedRequestPath("fixtures/functional/create_order/8/put_item_request.json")
                .setResponsePath("fixtures/functional/create_order/8/put_item_response.json"),

            inforInteraction(fulfillmentUrl(
                Arrays.asList(clientProperties.getWarehouseKey(), "shipments"), HttpMethod.POST)
            )
                .setExpectedRequestPath("fixtures/functional/create_order/8/create_shipments_request.json")
                .setResponsePath("fixtures/functional/create_order/8/create_shipments_response.json")
        );
    }

    /**
     * Сценарий #9:
     * <p>
     * Попытка создания заказа с maxAbsentItemsPricePercent вне диапазона (0, 100).
     */
    @Test
    void createOrderWithInocorrectMaxAbsentItemsPricePercent() throws Exception {
        // TODO тест, вероятно, не работает
        /*executeScenario(
                "fixtures/functional/create_order/9/wrap_request.xml",
                "fixtures/functional/create_order/9/wrap_response.xml"
        );*/
    }

    /**
     * Сценарий #10:
     * <p>
     * Создание заказа с кроссдок позицией.
     */
    @Test
    void createOrderWithCrossdockItem() throws Exception {
        executeScenario(
                "fixtures/functional/create_order/10/wrap_request.xml",
                "fixtures/functional/create_order/10/wrap_response.xml",

                putStorerInteraction(),

                inforInteraction(fulfillmentUrl(
                        Arrays.asList(clientProperties.getWarehouseKey(), "shipments"), HttpMethod.POST)
                )
                        .setExpectedRequestPath("fixtures/functional/create_order/10/create_shipments_request.json")
                        .setResponsePath("fixtures/functional/create_order/10/create_shipments_response.json")
        );
    }

    private void executeScenario(String wrapRequest,
                                 String wrapResponse,
                                 FulfillmentInteraction... interactions) throws Exception {
        FunctionalTestScenarioBuilder.start(CreateOrderResponse.class)
            .sendRequestToWrapQueryGateway(wrapRequest)
            .thenMockFulfillmentRequests(interactions)
            .andExpectWrapAnswerToBeEqualTo(wrapResponse)
            .build(mockMvc, restTemplate, fulfillmentMapper, clientProperties.getUrl())
            .start();

    }

    private FulfillmentInteraction putStorerInteraction() {
        return inforInteraction(fulfillmentUrl(Arrays.asList(
            clientProperties.getWarehouseKey(), "storers", "storerbatch"), HttpMethod.POST)
        ).setExpectedRequestPath("fixtures/functional/create_order/common/put_storer_request.json")
            .setResponsePath("fixtures/functional/create_order/common/put_storer_response.json");
    }

}

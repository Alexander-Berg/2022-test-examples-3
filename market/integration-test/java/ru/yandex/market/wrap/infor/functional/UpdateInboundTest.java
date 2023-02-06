package ru.yandex.market.wrap.infor.functional;

import java.util.Arrays;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.google.common.collect.ImmutableMap;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import ru.yandex.market.fulfillment.wrap.core.scenario.builder.FulfillmentInteraction;
import ru.yandex.market.fulfillment.wrap.core.scenario.builder.FunctionalTestScenarioBuilder;
import ru.yandex.market.logistic.api.model.common.ErrorCode;
import ru.yandex.market.logistic.api.model.fulfillment.response.UpdateInboundResponse;

import static ru.yandex.market.fulfillment.wrap.core.scenario.builder.FulfillmentUrl.fulfillmentUrl;

class UpdateInboundTest extends AbstractFunctionalTestWithIrisCommunication {

    private static final String RECEIPT_KEY_FOR_UPDATE = "0000000013";

    /**
     * Сценарий #1:
     * <p>
     * Обновляем поставку без товаров - в ответ ожидаем получить ошибку сериализации.
     * Взаимодействия с Infor SCE произойти не должно.
     */
    @Test
    void updateEmptyInbound() throws Exception {
        FunctionalTestScenarioBuilder.start(UpdateInboundResponse.class)
            .sendRequestToWrapQueryGateway("fixtures/functional/update_inbound/1/wrap_request.xml")
            .andExpectWrapAnswerToContainErrors(ImmutableMap.of(ErrorCode.BAD_REQUEST, 1))
            .build(mockMvc, restTemplate, fulfillmentMapper, clientProperties.getUrl())
            .start();
    }

    /**
     * Сценарий #2:
     * <p>
     * Попытка обновить поставку некорректным запросом (отсутствует partnerId).
     * <p>Проверяем, что будет возвращена ошибка BAD_REQUEST.</p>
     * Взаимодействия с Infor SCE произойти не должно.
     */
    @Test
    void updateInboundWithoutPartnerId() throws Exception {
        assertScenarioWithoutInteraction(
            "fixtures/functional/update_inbound/2/wrap_request.xml",
            "fixtures/functional/update_inbound/2/wrap_response.xml"
        );
    }

    /**
     * Сценарий #3:
     * <p>
     * Попытка обновить поставку partnerId, которой отсутствует в базе.
     * <p>Проверяем, что будет возвращена ошибка BAD_REQUEST.</p>
     * <p>Взаимодействия с Infor SCE произойти не должно.<p>
     */
    @Test
    void updateInboundWithIncorrectPartnerId() throws Exception {
        assertScenarioWithoutInteraction(
            "fixtures/functional/update_inbound/3/wrap_request.xml",
            "fixtures/functional/update_inbound/3/wrap_response.xml"
        );
    }

    /**
     * Сценарий #4:
     * Обновление поставки со статусом, при котором операцию нельзя произвести
     * <p>Проверяем, что будет возвращена ошибка BAD_REQUEST.</p>
     * <p>Взаимодействия с Infor SCE произойти не должно.</p>
     */
    @DatabaseSetup(
        value = "/fixtures/functional/update_inbound/common/state.xml",
        connection = "wmsConnection"
    )
    @Test
    void updateInboundWithNonUpdatableStatus() throws Exception {
        assertScenarioWithoutInteraction(
            "fixtures/functional/update_inbound/4/wrap_request.xml",
            "fixtures/functional/update_inbound/4/wrap_response.xml"
        );
    }

    /**
     * Сценарий #5.1:
     * Положительный сценарий с успешным обновлением поставки.
     * <p>Проверяем, что будет выполнен запрос к инфору с обновлением.</p>
     */
    @DatabaseSetup(
        value = "/fixtures/functional/update_inbound/common/state.xml",
        connection = "wmsConnection"
    )
    @Test
    void updateInboundPositive() throws Exception {
        mockIrisCommunication(
            "fixtures/functional/update_inbound/5/1/iris_request.json",
            "fixtures/functional/update_inbound/5/1/iris_response.json"
        );

        executeSingleItemScenario();
    }


    /**
     * Сценарий #5.2:
     * <p>
     * Обновляем поставку с одним товаром - в ответ ожидаем получить честный идентификатор поставки.
     * В БД у данного товара кастомная упаковка (НЕ STD)
     * <p>
     * Ожидаемое взаимодействие с Infor SCE:
     * 1 запрос на получение Receipt
     * 1 запрос на создание Storer'а
     * 1 запрос в IRIS (с корректными данными)
     * 1 запрос на создание SKU со всеми ALTSKU + ВГХ
     * 1 запрос на обновление Receipt
     */
    @Test
    @DatabaseSetup(
        connection = "wrapConnection",
        value = "classpath:fixtures/functional/update_inbound/common/db_state.xml"
    )
    @DatabaseSetup(
        connection = "wmsConnection",
        value = "classpath:fixtures/functional/update_inbound/5/2/wms_db_state.xml"
    )
    void updateWithSingleItemWithoutNeedForPackUpdate() throws Exception {
        executeSingleItemScenario();
    }

    /**
     * Сценарий #5.3:
     * <p>
     * Обновляем корректную поставку с одним товаром - в ответ ожидаем получить честный идентификатор поставки.
     * В БД у данного товара стандартная упаковка кастомная упаковка (STD)
     * <p>
     * Ожидаемое взаимодействие с Infor SCE:
     * 1 запрос на получение Receipt
     * 1 запрос на создание Storer'а
     * 1 запрос на создание SKU со всеми ALTSKU (но без вгх)
     * 1 запрос на обновление Receipt
     */
    @Test
    @DatabaseSetup(
        connection = "wrapConnection",
        value = "classpath:fixtures/functional/update_inbound/common/db_state.xml"
    )
    @DatabaseSetup(
        connection = "wmsConnection",
        value = "classpath:fixtures/functional/update_inbound/5/3/wms_db_state.xml"
    )
    void updateWithSingleItemWithNeedForPackUpdateAndWithIrisData() throws Exception {
        mockIrisCommunication(
            "fixtures/functional/update_inbound/5/3/iris_request.json",
            "fixtures/functional/update_inbound/5/3/iris_response.json"
        );

        executeSingleItemScenario(
            "fixtures/functional/update_inbound/5/3/put_item_request.json",
            "fixtures/functional/update_inbound/common/put_item_response.json"
        );
    }


    /**
     * Сценарий #5.4:
     * <p>
     * Обновляем корректную поставку с одним товаром - в ответ ожидаем получить честный идентификатор поставки.
     * В БД у данного товара стандартная упаковка кастомная упаковка (STD)
     * <p>
     * Ожидаемое взаимодействие с Infor SCE:
     * 1 запрос на получение Receipt
     * 1 запрос на создание Storer'а
     * 1 запрос на создание SKU со всеми ALTSKU (но без вгх)
     * 1 запрос на обновление Receipt
     */
    @Test
    @DatabaseSetup(
        connection = "wrapConnection",
        value = "classpath:fixtures/functional/update_inbound/common/db_state.xml"
    )
    @DatabaseSetup(
        connection = "wmsConnection",
        value = "classpath:fixtures/functional/update_inbound/5/4/wms_db_state.xml"
    )
    void shouldGetPackFromDbForMultiPackedItem() throws Exception {
        mockIrisCommunication(
            "fixtures/functional/update_inbound/5/4/iris_request.json",
            "fixtures/functional/update_inbound/5/4/iris_response.json"
        );

        executeScenario(
            "fixtures/functional/update_inbound/5/4/wrap_request.xml",
            "fixtures/functional/update_inbound/common/wrap_response.xml",

            inforInteraction(fulfillmentUrl(Arrays.asList(
                clientProperties.getWarehouseKey(), "receipts", RECEIPT_KEY_FOR_UPDATE), HttpMethod.GET)
            ).setResponsePath("fixtures/functional/update_inbound/common/get_receipt_response.json"),

            inforInteraction(fulfillmentUrl(Arrays.asList(
                clientProperties.getWarehouseKey(), "storers", "storerbatch"), HttpMethod.POST)
            ).setExpectedRequestPath("fixtures/functional/update_inbound/common/put_storer_request.json")
                .setResponsePath("fixtures/functional/update_inbound/common/put_storer_response.json"),

            inforInteraction(fulfillmentUrl(Arrays.asList(
                clientProperties.getEnterpriseKey(), "items", "itembatch"), HttpMethod.POST)
            ).setExpectedRequestPath("fixtures/functional/update_inbound/5/4/put_item_request.json")
                .setResponsePath("fixtures/functional/update_inbound/common/put_item_response.json"),

            inforInteraction(fulfillmentUrl(Arrays.asList(
                clientProperties.getWarehouseKey(), "receipts", RECEIPT_KEY_FOR_UPDATE), HttpMethod.PUT)
            ).setExpectedRequestPath("fixtures/functional/update_inbound/5/4/put_receipt_request.json")
                .setResponsePath("fixtures/functional/update_inbound/5/4/put_receipt_response.json")
        );
    }

    /**
     * Сценарий #5.5:
     * <p>
     * Обновляем корректную поставку с одним возвратным товаром - в ответ ожидаем получить честный идентификатор поставки.
     * В БД у данного товара стандартная упаковка (STD)
     * От IRIS отсутствует информация по товару.
     * <p>
     * Ожидаемое взаимодействие с Infor SCE:
     * 1 запрос на получение Receipt
     * 1 запрос на создание Storer'а
     * 1 запрос на получение информации из IRIS (но без данных)
     * 1 запрос на создание SKU со всеми ALTSKU (но без вгх)
     * 1 запрос на обновление Receipt
     */
    @Test
    @DatabaseSetup(
        connection = "wrapConnection",
        value = "classpath:fixtures/functional/update_inbound/common/db_state.xml"
    )
    @DatabaseSetup(
        connection = "wmsConnection",
        value = "classpath:fixtures/functional/update_inbound/5/5/wms_db_state.xml"
    )
    void updateWithSingleReturnItem() throws Exception {
        mockIrisCommunication(
            "fixtures/functional/update_inbound/5/5/iris_request.json",
            "fixtures/functional/update_inbound/5/5/iris_response.json"
        );

        executeSingleItemScenario(
            "fixtures/functional/update_inbound/common/put_item_request.json",
            "fixtures/functional/update_inbound/common/put_item_response.json",
            "fixtures/functional/update_inbound/common/get_receipt_response.json",
            "fixtures/functional/update_inbound/5/5/put_receipt_request.json",
            "fixtures/functional/update_inbound/5/5/put_receipt_response.json",
            "fixtures/functional/update_inbound/5/5/wrap_request.xml",
            "fixtures/functional/update_inbound/common/wrap_response.xml"
        );
    }

    /**
     * Сценарий #5.6:
     * <p>
     * Обновляем корректную кроссдок поставку - в ответ ожидаем получить честный идентификатор поставки.
     * В БД у данного товара стандартная упаковка (STD)
     * От IRIS отсутствует информация по товару.
     * <p>
     * Ожидаемое взаимодействие с Infor SCE:
     * 1 запрос на получение Receipt
     * 1 запрос на создание Storer'а
     * 1 запрос на получение информации из IRIS (но без данных)
     * 1 запрос на создание SKU со всеми ALTSKU (но без вгх)
     * 1 запрос на обноление Receipt
     */
    @Test
    @DatabaseSetup(
        connection = "wrapConnection",
        value = "classpath:fixtures/functional/update_inbound/common/db_state.xml"
    )
    @DatabaseSetup(
        connection = "wmsConnection",
        value = "classpath:fixtures/functional/update_inbound/5/6/wms_db_state.xml"
    )
    void updateWithSingleCrossdockItem() throws Exception {
        mockIrisCommunication(
            "fixtures/functional/update_inbound/5/6/iris_request.json",
            "fixtures/functional/update_inbound/5/6/iris_response.json"
        );

        executeSingleItemScenario(
            "fixtures/functional/update_inbound/common/put_item_request.json",
            "fixtures/functional/update_inbound/common/put_item_response.json",
            "fixtures/functional/update_inbound/5/6/get_receipt_response.json",
            "fixtures/functional/update_inbound/5/6/put_receipt_request.json",
            "fixtures/functional/update_inbound/5/6/put_receipt_response.json",
            "fixtures/functional/update_inbound/5/6/wrap_request.xml",
            "fixtures/functional/update_inbound/common/wrap_response.xml"
        );
    }

    /**
     * Сценарий #5.7:
     * <p>
     * Обновляем корректную поставку с двумя возвратными товарами от двух разных contractor'ов
     * - в ответ ожидаем получить честный идентификатор поставки.
     * В БД у данных товаров стандартная упаковка (STD)
     * От IRIS отсутствует информация по товарам.
     * <p>
     * Ожидаемое взаимодействие с Infor SCE:
     * 1 запрос на получение Receipt
     * 1 запрос на создание двух Storer'ов
     * 1 запрос на создание двух Storer-contractor'ов
     * 1 запрос на получение информации из IRIS (но без данных)
     * 1 запрос на создание двух SKU со всеми ALTSKU (но без вгх) и информацией о contractor'ах
     * 1 запрос на обновление Receipt c информацией о contractor'ах в receiptDetail'ах
     */
    @Test
    @DatabaseSetup(
        connection = "wrapConnection",
        value = "classpath:fixtures/functional/update_inbound/5/7/db_state.xml"
    )
    @DatabaseSetup(
        connection = "wmsConnection",
        value = "classpath:fixtures/functional/update_inbound/5/7/wms_db_state.xml"
    )
    void updateWithTwoReturnItemsAndContractorInfo() throws Exception {
        mockIrisCommunication(
            "fixtures/functional/update_inbound/5/7/iris_request.json",
            "fixtures/functional/update_inbound/5/7/iris_response.json"
        );

        executeScenario(
            "fixtures/functional/update_inbound/5/7/wrap_request.xml",
            "fixtures/functional/update_inbound/common/wrap_response.xml",

            inforInteraction(fulfillmentUrl(Arrays.asList(
                clientProperties.getWarehouseKey(), "receipts", RECEIPT_KEY_FOR_UPDATE), HttpMethod.GET)
            ).setResponsePath("fixtures/functional/update_inbound/5/7/get_receipt_response.json"),

            inforInteraction(fulfillmentUrl(Arrays.asList(
                clientProperties.getWarehouseKey(), "storers", "storerbatch"), HttpMethod.POST)
            ).setExpectedRequestPath("fixtures/functional/update_inbound/5/7/put_storer_request.json")
                .setResponsePath("fixtures/functional/update_inbound/5/7/put_storer_response.json"),

            inforInteraction(fulfillmentUrl(Arrays.asList(
                clientProperties.getWarehouseKey(), "storers", "storerbatch"), HttpMethod.POST)
            ).setExpectedRequestPath("fixtures/functional/update_inbound/5/7/put_contractor_storer_request.json")
                .setResponsePath("fixtures/functional/update_inbound/5/7/put_contractor_storer_response.json"),

            inforInteraction(fulfillmentUrl(Arrays.asList(
                clientProperties.getEnterpriseKey(), "items", "itembatch"), HttpMethod.POST)
            ).setExpectedRequestPath("fixtures/functional/update_inbound/5/7/put_item_request.json")
                .setResponsePath("fixtures/functional/update_inbound/5/7/put_item_response.json"),

            inforInteraction(fulfillmentUrl(Arrays.asList(
                clientProperties.getWarehouseKey(), "receipts", RECEIPT_KEY_FOR_UPDATE), HttpMethod.PUT)
            ).setExpectedRequestPath("fixtures/functional/update_inbound/5/7/put_receipt_request.json")
                .setResponsePath("fixtures/functional/update_inbound/5/7/put_receipt_response.json")
        );
    }

    /**
     * Сценарий #5.8:
     * <p>
     * Обновляем корректную поставку с двумя возвратными товарами: один с contractor'ом, другой без
     * - в ответ ожидаем получить честный идентификатор поставки.
     * В БД у данных товаров стандартная упаковка (STD)
     * От IRIS отсутствует информация по товарам.
     * <p>
     * Ожидаемое взаимодействие с Infor SCE:
     * 1 запрос на получение Receipt
     * 1 запрос на создание двух Storer'ов
     * 1 запрос на создание одного Storer-contractor'а
     * 1 запрос на получение информации из IRIS (но без данных)
     * 1 запрос на создание двух SKU со всеми ALTSKU (но без вгх) и информацией о contractor'е для одного из них
     * 1 запрос на обновление Receipt c информацией о contractor'e в одном из receiptDetail'ов
     */
    @Test
    @DatabaseSetup(
        connection = "wrapConnection",
        value = "classpath:fixtures/functional/update_inbound/5/8/db_state.xml"
    )
    @DatabaseSetup(
        connection = "wmsConnection",
        value = "classpath:fixtures/functional/update_inbound/5/8/wms_db_state.xml"
    )
    void updateWithTwoReturnItemsAndPartialContractorInfo() throws Exception {
        mockIrisCommunication(
            "fixtures/functional/update_inbound/5/8/iris_request.json",
            "fixtures/functional/update_inbound/5/8/iris_response.json"
        );

        executeScenario(
            "fixtures/functional/update_inbound/5/8/wrap_request.xml",
            "fixtures/functional/update_inbound/common/wrap_response.xml",

            inforInteraction(fulfillmentUrl(Arrays.asList(
                clientProperties.getWarehouseKey(), "receipts", RECEIPT_KEY_FOR_UPDATE), HttpMethod.GET)
            ).setResponsePath("fixtures/functional/update_inbound/5/8/get_receipt_response.json"),

            inforInteraction(fulfillmentUrl(Arrays.asList(
                clientProperties.getWarehouseKey(), "storers", "storerbatch"), HttpMethod.POST)
            ).setExpectedRequestPath("fixtures/functional/update_inbound/5/8/put_storer_request.json")
                .setResponsePath("fixtures/functional/update_inbound/5/8/put_storer_response.json"),

            inforInteraction(fulfillmentUrl(Arrays.asList(
                clientProperties.getWarehouseKey(), "storers", "storerbatch"), HttpMethod.POST)
            ).setExpectedRequestPath("fixtures/functional/update_inbound/5/8/put_contractor_storer_request.json")
                .setResponsePath("fixtures/functional/update_inbound/5/8/put_contractor_storer_response.json"),

            inforInteraction(fulfillmentUrl(Arrays.asList(
                clientProperties.getEnterpriseKey(), "items", "itembatch"), HttpMethod.POST)
            ).setExpectedRequestPath("fixtures/functional/update_inbound/5/8/put_item_request.json")
                .setResponsePath("fixtures/functional/update_inbound/5/8/put_item_response.json"),

            inforInteraction(fulfillmentUrl(Arrays.asList(
                clientProperties.getWarehouseKey(), "receipts", RECEIPT_KEY_FOR_UPDATE), HttpMethod.PUT)
            ).setExpectedRequestPath("fixtures/functional/update_inbound/5/8/put_receipt_request.json")
                .setResponsePath("fixtures/functional/update_inbound/5/8/put_receipt_response.json")
        );
    }

    /**
     * Сценарий #6:
     * <p>
     * Обновляем корректную кроссдок поставку
     * Request к нам приходит с дефолтным типом, в то время как в Infor поставка - это КроссДок(18).
     * Тип поставки не меняется.
     *
     * В ответ ожидаем получить честный идентификатор поставки.
     * <p>
     * update_sku_on_inbound_creation=false + все sku уже созданы.
     * <p>
     * Ожидаемое взаимодействие с Infor SCE:
     * 1 запрос на получение Receipt
     * 1 запрос на создание Storer'а
     * НИ ОДНОГО запроса на создание SKU со всеми ALTSKU
     * 1 запрос на обноление Receipt
     */
    @Test
    @DatabaseSetup(
        connection = "wrapConnection",
        value = "classpath:fixtures/functional/update_inbound/6/db_state.xml"
    )
    @DatabaseSetup(
        connection = "wmsConnection",
        value = "classpath:fixtures/functional/update_inbound/6/wms_db_state.xml"
    )
    void updateInboundWithoutChangeType() throws Exception {
        executeScenario(
            "fixtures/functional/update_inbound/6/wrap_request.xml",
            "fixtures/functional/update_inbound/common/wrap_response.xml",

            inforInteraction(fulfillmentUrl(Arrays.asList(
                clientProperties.getWarehouseKey(), "receipts", RECEIPT_KEY_FOR_UPDATE), HttpMethod.GET)
            ).setResponsePath("fixtures/functional/update_inbound/6/get_receipt_response.json"),

            inforInteraction(fulfillmentUrl(Arrays.asList(
                clientProperties.getWarehouseKey(), "storers", "storerbatch"), HttpMethod.POST)
            ).setExpectedRequestPath("fixtures/functional/update_inbound/common/put_storer_request.json")
                .setResponsePath("fixtures/functional/update_inbound/common/put_storer_response.json"),

            inforInteraction(fulfillmentUrl(Arrays.asList(
                clientProperties.getWarehouseKey(), "receipts", RECEIPT_KEY_FOR_UPDATE), HttpMethod.PUT)
            ).setExpectedRequestPath("fixtures/functional/update_inbound/6/put_receipt_request.json")
                .setResponsePath("fixtures/functional/update_inbound/6/put_receipt_response.json")
        );
    }


    /**
     * Сценарий #7
     * Пытаемся обновить поставку, которой нет
     * Падаем при попытке ее найти
     * Взаимодействия с Infor SCE произойти не должно.
     * </p>
     */
    @Test
    void updateNonExistingInbound() throws Exception {
        FunctionalTestScenarioBuilder.start(UpdateInboundResponse.class)
            .sendRequestToWrapQueryGateway("fixtures/functional/update_inbound/common/wrap_request.xml")
            .andExpectWrapAnswerToContainErrors(ImmutableMap.of(ErrorCode.BAD_REQUEST, 1))
            .build(mockMvc, restTemplate, fulfillmentMapper, clientProperties.getUrl())
            .start();
    }

    private void assertScenarioWithoutInteraction(String wrapRequest, String wrapResponse) throws Exception {
        FunctionalTestScenarioBuilder.start(UpdateInboundResponse.class)
            .sendRequestToWrapQueryGateway(wrapRequest)
            .andExpectWrapAnswerToBeEqualTo(wrapResponse)
            .build(mockMvc, restTemplate, fulfillmentMapper, clientProperties.getUrl())
            .start();
    }

    private void executeSingleItemScenario() throws Exception {
        executeSingleItemScenario(
            "fixtures/functional/update_inbound/common/put_item_request.json",
            "fixtures/functional/update_inbound/common/put_item_response.json"
        );
    }

    private void executeSingleItemScenario(String putRequestPath,
                                           String putResponsePath) throws Exception {
        executeSingleItemScenario(putRequestPath, putResponsePath,
            "fixtures/functional/update_inbound/common/get_receipt_response.json",
            "fixtures/functional/update_inbound/common/put_receipt_request.json",
            "fixtures/functional/update_inbound/common/put_receipt_response.json",
            "fixtures/functional/update_inbound/common/wrap_request.xml",
            "fixtures/functional/update_inbound/common/wrap_response.xml");
    }

    private void executeSingleItemScenario(String putRequestPath,
                                           String putResponsePath,
                                           String getReceiptResponsePath,
                                           String putReceiptRequestPath,
                                           String putReceiptResponsePath,
                                           String wrapRequestPath,
                                           String wrapResponsePath) throws Exception {

        executeScenario(wrapRequestPath, wrapResponsePath,

            inforInteraction(fulfillmentUrl(Arrays.asList(
                clientProperties.getWarehouseKey(), "receipts", RECEIPT_KEY_FOR_UPDATE), HttpMethod.GET)
            ).setResponsePath(getReceiptResponsePath),

            inforInteraction(fulfillmentUrl(Arrays.asList(
                clientProperties.getWarehouseKey(), "storers", "storerbatch"), HttpMethod.POST)
            ).setExpectedRequestPath("fixtures/functional/update_inbound/common/put_storer_request.json")
                .setResponsePath("fixtures/functional/update_inbound/common/put_storer_response.json"),

            inforInteraction(fulfillmentUrl(Arrays.asList(
                clientProperties.getEnterpriseKey(), "items", "itembatch"), HttpMethod.POST)
            ).setExpectedRequestPath(putRequestPath)
                .setResponsePath(putResponsePath),

            inforInteraction(fulfillmentUrl(Arrays.asList(
                clientProperties.getWarehouseKey(), "receipts", RECEIPT_KEY_FOR_UPDATE), HttpMethod.PUT)
            ).setExpectedRequestPath(putReceiptRequestPath)
                .setResponsePath(putReceiptResponsePath)
        );
    }

    private void executeScenario(String wrapRequest,
                                 String wrapResponse,
                                 FulfillmentInteraction... interactions) throws Exception {
        FunctionalTestScenarioBuilder.start(UpdateInboundResponse.class)
            .sendRequestToWrapQueryGateway(wrapRequest)
            .thenMockFulfillmentRequests(interactions)
            .andExpectWrapAnswerToBeEqualTo(wrapResponse)
            .build(mockMvc, restTemplate, fulfillmentMapper, clientProperties.getUrl())
            .start();

    }
}


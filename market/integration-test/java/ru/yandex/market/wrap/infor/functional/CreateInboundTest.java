package ru.yandex.market.wrap.infor.functional;

import java.util.Arrays;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.google.common.collect.ImmutableMap;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;

import ru.yandex.market.fulfillment.wrap.core.scenario.builder.FulfillmentInteraction;
import ru.yandex.market.fulfillment.wrap.core.scenario.builder.FunctionalTestScenarioBuilder;
import ru.yandex.market.logistic.api.model.common.ErrorCode;
import ru.yandex.market.logistic.api.model.fulfillment.response.CreateInboundResponse;

import static ru.yandex.market.fulfillment.wrap.core.scenario.builder.FulfillmentUrl.fulfillmentUrl;

class CreateInboundTest extends AbstractFunctionalTestWithIrisCommunication {

    /**
     * Сценарий #1:
     * <p>
     * Создаем поставку без товаров - в ответ ожидаем получить ошибку сериализации.
     * Взаимодействия с Infor SCE произойти не должно.
     */
    @Test
    void createEmptyInbound() throws Exception {
        FunctionalTestScenarioBuilder.start(CreateInboundResponse.class)
            .sendRequestToWrapQueryGateway("fixtures/functional/create_inbound/1/wrap_request.xml")
            .andExpectWrapAnswerToContainErrors(ImmutableMap.of(ErrorCode.BAD_REQUEST, 1))
            .build(mockMvc, restTemplate, fulfillmentMapper, clientProperties.getUrl())
            .start();
    }

    /**
     * Сценарий #2.1:
     * <p>
     * Создаем корректную поставку с одним товаром - в ответ ожидаем получить честный идентификатор поставки.
     * В БД у данного товара стандартная упаковка (STD)
     * От IRIS отсутствует информация по товару.
     * <p>
     * Ожидаемое взаимодействие с Infor SCE:
     * 1 запрос на создание Storer'а
     * 1 запрос на получение информации из IRIS (но без данных)
     * 1 запрос на создание SKU со всеми ALTSKU (но без вгх)
     * 1 запрос на создание Receipt
     */
    @Test
    @DatabaseSetup(
        connection = "wrapConnection",
        value = "classpath:fixtures/functional/create_inbound/2/db_state.xml"
    )
    @DatabaseSetup(
        connection = "wmsConnection",
        value = "classpath:fixtures/functional/create_inbound/2/1/sku.xml"
    )
    void createWithSingleItemWithNeedForPackUpdateButWithoutIrisData() throws Exception {
        mockIrisCommunication(
            "fixtures/functional/create_inbound/2/1/iris_request.json",
            "fixtures/functional/create_inbound/2/1/iris_response.json"
        );

        executeSingleItemScenario();
    }

    /**
     * Сценарий #2.2:
     * <p>
     * Создаем корректную поставку с одним товаром - в ответ ожидаем получить честный идентификатор поставки.
     * В БД у данного товара кастомная упаковка (НЕ STD)
     * <p>
     * Ожидаемое взаимодействие с Infor SCE:
     * 1 запрос на создание Storer'а
     * 1 запрос в IRIS (с корректными данными)
     * 1 запрос на создание SKU со всеми ALTSKU + ВГХ
     * 1 запрос на создание Receipt
     */
    @Test
    @DatabaseSetup(
        connection = "wrapConnection",
        value = "classpath:fixtures/functional/create_inbound/2/db_state.xml"
    )
    @DatabaseSetup(
        connection = "wmsConnection",
        value = "classpath:fixtures/functional/create_inbound/2/2/sku.xml"
    )
    void createWithSingleItemWithoutNeedForPackUpdate() throws Exception {
        executeSingleItemScenario();
    }

    /**
     * Сценарий #2.3:
     * <p>
     * Создаем корректную поставку с одним товаром - в ответ ожидаем получить честный идентификатор поставки.
     * В БД у данного товара стандартная упаковка кастомная упаковка (STD)
     * <p>
     * Ожидаемое взаимодействие с Infor SCE:
     * 1 запрос на создание Storer'а
     * 1 запрос на создание SKU со всеми ALTSKU (но без вгх)
     * 1 запрос на создание Receipt
     */
    @Test
    @DatabaseSetup(
        connection = "wrapConnection",
        value = "classpath:fixtures/functional/create_inbound/2/db_state.xml"
    )
    @DatabaseSetup(
        connection = "wmsConnection",
        value = "classpath:fixtures/functional/create_inbound/2/3/sku.xml"
    )
    void createWithSingleItemWithNeedForPackUpdateAndWithIrisData() throws Exception {
        mockIrisCommunication(
            "fixtures/functional/create_inbound/2/3/iris_request.json",
            "fixtures/functional/create_inbound/2/3/iris_response.json"
        );

        executeSingleItemScenario(
            "fixtures/functional/create_inbound/2/3/put_item_request.json",
            "fixtures/functional/create_inbound/2/put_item_response.json"
        );
    }


    /**
     * Сценарий #2.4:
     * <p>
     * Создаем корректную поставку с одним товаром - в ответ ожидаем получить честный идентификатор поставки.
     * В БД у данного товара стандартная упаковка кастомная упаковка (STD)
     * <p>
     * Ожидаемое взаимодействие с Infor SCE:
     * 1 запрос на создание Storer'а
     * 1 запрос на создание SKU со всеми ALTSKU (но без вгх)
     * 1 запрос на создание Receipt
     */
    @Test
    @DatabaseSetup(
        connection = "wrapConnection",
        value = "classpath:fixtures/functional/create_inbound/2/db_state.xml"
    )
    @DatabaseSetup(
        connection = "wmsConnection",
        value = "classpath:fixtures/functional/create_inbound/2/4/sku.xml"
    )
    void shouldGetPackFromDbForMultiPackedItem() throws Exception {
        mockIrisCommunication(
            "fixtures/functional/create_inbound/2/4/iris_request.json",
            "fixtures/functional/create_inbound/2/4/iris_response.json"
        );

        executeScenario(
            "fixtures/functional/create_inbound/2/4/wrap_request.xml",
            "fixtures/functional/create_inbound/2/wrap_response.xml",

            inforInteraction(fulfillmentUrl(Arrays.asList(
                clientProperties.getWarehouseKey(), "storers", "storerbatch"), HttpMethod.POST)
            ).setExpectedRequestPath("fixtures/functional/create_inbound/2/put_storer_request.json")
                .setResponsePath("fixtures/functional/create_inbound/2/put_storer_response.json"),

            inforInteraction(fulfillmentUrl(Arrays.asList(
                clientProperties.getEnterpriseKey(), "items", "itembatch"), HttpMethod.POST)
            ).setExpectedRequestPath("fixtures/functional/create_inbound/2/4/put_item_request.json")
                .setResponsePath("fixtures/functional/create_inbound/2/put_item_response.json"),

            inforInteraction(fulfillmentUrl(Arrays.asList(
                clientProperties.getWarehouseKey(), "receipts"), HttpMethod.POST)
            ).setExpectedRequestPath("fixtures/functional/create_inbound/2/post_receipt_request.json")
                .setResponsePath("fixtures/functional/create_inbound/2/post_receipt_response.json")
        );
    }

    /**
     * Сценарий #2.5:
     * <p>
     * Создаем корректную поставку с одним возвратным товаром - в ответ ожидаем получить честный идентификатор поставки.
     * В БД у данного товара стандартная упаковка (STD)
     * От IRIS отсутствует информация по товару.
     * <p>
     * Ожидаемое взаимодействие с Infor SCE:
     * 1 запрос на создание Storer'а
     * 1 запрос на получение информации из IRIS (но без данных)
     * 1 запрос на создание SKU со всеми ALTSKU (но без вгх)
     * 1 запрос на создание Receipt
     */
    @Test
    @DatabaseSetup(
        connection = "wrapConnection",
        value = "classpath:fixtures/functional/create_inbound/2/db_state.xml"
    )
    @DatabaseSetup(
        connection = "wmsConnection",
        value = "classpath:fixtures/functional/create_inbound/2/5/sku.xml"
    )
    void createWithSingleReturnItem() throws Exception {
        mockIrisCommunication(
            "fixtures/functional/create_inbound/2/5/iris_request.json",
            "fixtures/functional/create_inbound/2/5/iris_response.json"
        );

        executeSingleItemScenario(
            "fixtures/functional/create_inbound/2/put_item_request.json",
            "fixtures/functional/create_inbound/2/put_item_response.json",
            "fixtures/functional/create_inbound/2/5/post_receipt_request.json",
            "fixtures/functional/create_inbound/2/5/post_receipt_response.json",
            "fixtures/functional/create_inbound/2/5/wrap_request.xml",
            "fixtures/functional/create_inbound/2/wrap_response.xml"
        );
    }

    /**
     * Сценарий #2.6:
     * <p>
     * Создаем корректную кроссдок поставку - в ответ ожидаем получить честный идентификатор поставки.
     * В БД у данного товара стандартная упаковка (STD)
     * От IRIS отсутствует информация по товару.
     * <p>
     * Ожидаемое взаимодействие с Infor SCE:
     * 1 запрос на создание Storer'а
     * 1 запрос на получение информации из IRIS (но без данных)
     * 1 запрос на создание SKU со всеми ALTSKU (но без вгх)
     * 1 запрос на создание Receipt
     */
    @Test
    @DatabaseSetup(
        connection = "wrapConnection",
        value = "classpath:fixtures/functional/create_inbound/2/db_state.xml"
    )
    @DatabaseSetup(
        connection = "wmsConnection",
        value = "classpath:fixtures/functional/create_inbound/2/6/sku.xml"
    )
    void createWithSingleCrossdockItem() throws Exception {
        mockIrisCommunication(
            "fixtures/functional/create_inbound/2/6/iris_request.json",
            "fixtures/functional/create_inbound/2/6/iris_response.json"
        );

        executeSingleItemScenario(
            "fixtures/functional/create_inbound/2/put_item_request.json",
            "fixtures/functional/create_inbound/2/put_item_response.json",
            "fixtures/functional/create_inbound/2/6/post_receipt_request.json",
            "fixtures/functional/create_inbound/2/6/post_receipt_response.json",
            "fixtures/functional/create_inbound/2/6/wrap_request.xml",
            "fixtures/functional/create_inbound/2/wrap_response.xml"
        );
    }

    /**
     * Сценарий #2.7:
     * <p>
     * Создаем корректную поставку с двумя возвратными товарами от двух разных contractor'ов
     * - в ответ ожидаем получить честный идентификатор поставки.
     * В БД у данных товаров стандартная упаковка (STD)
     * От IRIS отсутствует информация по товарам.
     * <p>
     * Ожидаемое взаимодействие с Infor SCE:
     * 1 запрос на создание двух Storer'ов
     * 1 запрос на создание двух Storer-contractor'ов
     * 1 запрос на получение информации из IRIS (но без данных)
     * 1 запрос на создание двух SKU со всеми ALTSKU (но без вгх) и информацией о contractor'ах
     * 1 запрос на создание Receipt c информацией о contractor'ах в receiptDetail'ах
     */
    @Test
    @DatabaseSetup(
        connection = "wrapConnection",
        value = "classpath:fixtures/functional/create_inbound/2/7/db_state.xml"
    )
    @DatabaseSetup(
        connection = "wmsConnection",
        value = "classpath:fixtures/functional/create_inbound/2/7/sku.xml"
    )
    void createWithTwoReturnItemsAndContractorInfo() throws Exception {
        mockIrisCommunication(
            "fixtures/functional/create_inbound/2/7/iris_request.json",
            "fixtures/functional/create_inbound/2/7/iris_response.json"
        );

        executeScenario(
            "fixtures/functional/create_inbound/2/7/wrap_request.xml",
            "fixtures/functional/create_inbound/2/wrap_response.xml",

            inforInteraction(fulfillmentUrl(Arrays.asList(
                clientProperties.getWarehouseKey(), "storers", "storerbatch"), HttpMethod.POST)
            ).setExpectedRequestPath("fixtures/functional/create_inbound/2/7/put_storer_request.json")
                .setResponsePath("fixtures/functional/create_inbound/2/7/put_storer_response.json"),

            inforInteraction(fulfillmentUrl(Arrays.asList(
                clientProperties.getWarehouseKey(), "storers", "storerbatch"), HttpMethod.POST)
            ).setExpectedRequestPath("fixtures/functional/create_inbound/2/7/put_contractor_storer_request.json")
                .setResponsePath("fixtures/functional/create_inbound/2/7/put_contractor_storer_response.json"),

            inforInteraction(fulfillmentUrl(Arrays.asList(
                clientProperties.getEnterpriseKey(), "items", "itembatch"), HttpMethod.POST)
            ).setExpectedRequestPath("fixtures/functional/create_inbound/2/7/put_item_request.json")
                .setResponsePath("fixtures/functional/create_inbound/2/7/put_item_response.json"),

            inforInteraction(fulfillmentUrl(Arrays.asList(
                clientProperties.getWarehouseKey(), "receipts"), HttpMethod.POST)
            ).setExpectedRequestPath("fixtures/functional/create_inbound/2/7/post_receipt_request.json")
                .setResponsePath("fixtures/functional/create_inbound/2/7/post_receipt_response.json")
        );
    }

    /**
     * Сценарий #2.8:
     * <p>
     * Создаем корректную поставку с двумя возвратными товарами: один с contractor'ом, другой без
     * - в ответ ожидаем получить честный идентификатор поставки.
     * В БД у данных товаров стандартная упаковка (STD)
     * От IRIS отсутствует информация по товарам.
     * <p>
     * Ожидаемое взаимодействие с Infor SCE:
     * 1 запрос на создание двух Storer'ов
     * 1 запрос на создание одного Storer-contractor'а
     * 1 запрос на получение информации из IRIS (но без данных)
     * 1 запрос на создание двух SKU со всеми ALTSKU (но без вгх) и информацией о contractor'е для одного из них
     * 1 запрос на создание Receipt c информацией о contractor'e в одном из receiptDetail'ов
     */
    @Test
    @DatabaseSetup(
        connection = "wrapConnection",
        value = "classpath:fixtures/functional/create_inbound/2/8/db_state.xml"
    )
    @DatabaseSetup(
        connection = "wmsConnection",
        value = "classpath:fixtures/functional/create_inbound/2/8/sku.xml"
    )
    void createWithTwoReturnItemsAndPartialContractorInfo() throws Exception {
        mockIrisCommunication(
            "fixtures/functional/create_inbound/2/8/iris_request.json",
            "fixtures/functional/create_inbound/2/8/iris_response.json"
        );

        executeScenario(
            "fixtures/functional/create_inbound/2/8/wrap_request.xml",
            "fixtures/functional/create_inbound/2/wrap_response.xml",

            inforInteraction(fulfillmentUrl(Arrays.asList(
                clientProperties.getWarehouseKey(), "storers", "storerbatch"), HttpMethod.POST)
            ).setExpectedRequestPath("fixtures/functional/create_inbound/2/8/put_storer_request.json")
                .setResponsePath("fixtures/functional/create_inbound/2/8/put_storer_response.json"),

            inforInteraction(fulfillmentUrl(Arrays.asList(
                clientProperties.getWarehouseKey(), "storers", "storerbatch"), HttpMethod.POST)
            ).setExpectedRequestPath("fixtures/functional/create_inbound/2/8/put_contractor_storer_request.json")
                .setResponsePath("fixtures/functional/create_inbound/2/8/put_contractor_storer_response.json"),

            inforInteraction(fulfillmentUrl(Arrays.asList(
                clientProperties.getEnterpriseKey(), "items", "itembatch"), HttpMethod.POST)
            ).setExpectedRequestPath("fixtures/functional/create_inbound/2/8/put_item_request.json")
                .setResponsePath("fixtures/functional/create_inbound/2/8/put_item_response.json"),

            inforInteraction(fulfillmentUrl(Arrays.asList(
                clientProperties.getWarehouseKey(), "receipts"), HttpMethod.POST)
            ).setExpectedRequestPath("fixtures/functional/create_inbound/2/8/post_receipt_request.json")
                .setResponsePath("fixtures/functional/create_inbound/2/8/post_receipt_response.json")
        );
    }


    /**
     * Сценарий #3:
     * <p>
     * Создаем поставку с несколькими товарами от разных поставщиков.
     * Ожидаемое взаимодействие с Infor SCE:
     * 1 запроса на создание 2-х Storer'ов
     * 2 запроса на создание SKU со всеми ALTSKU
     * 1 запрос на создание Receipt
     */
    @Test
    @DatabaseSetup(
        connection = "wrapConnection",
        value = "classpath:fixtures/functional/create_inbound/3/db_state.xml"
    )
    @DatabaseSetup(
        connection = "wmsConnection",
        value = "classpath:fixtures/functional/create_inbound/3/wms_db_state.xml"
    )
    void createWithMultipleMerchants() throws Exception {

        executeScenario(
            "fixtures/functional/create_inbound/3/wrap_request.xml",
            "fixtures/functional/create_inbound/3/wrap_response.xml",

            inforInteraction(fulfillmentUrl(Arrays.asList(
                clientProperties.getWarehouseKey(), "storers", "storerbatch"), HttpMethod.POST)
            ).setExpectedRequestPath("fixtures/functional/create_inbound/3/put_storer_request.json")
                .setResponsePath("fixtures/functional/create_inbound/3/put_storer_response.json"),

            inforInteraction(fulfillmentUrl(Arrays.asList(
                clientProperties.getEnterpriseKey(), "items", "itembatch"), HttpMethod.POST)
            ).setExpectedRequestPath("fixtures/functional/create_inbound/3/put_two_items_request.json")
                .setResponsePath("fixtures/functional/create_inbound/3/put_two_items_response.json"),

            inforInteraction(fulfillmentUrl(Arrays.asList(
                clientProperties.getWarehouseKey(), "receipts"), HttpMethod.POST)
            ).setExpectedRequestPath("fixtures/functional/create_inbound/3/post_receipt_request.json")
                .setResponsePath("fixtures/functional/create_inbound/3/post_receipt_response.json")
        );
    }

    /**
     * Сценарий #4:
     * <p>
     * Создаем корректную поставку с несколькими товарами от одного поставщика
     * - в ответ ожидаем получить честный идентификатор поставки.
     * <p>
     * Ожидаемое взаимодействие с Infor SCE:
     * 1 запрос на создание Storer'а
     * 2 запроса на создание SKU со всеми ALTSKU
     * 1 запрос на создание Receipt
     */
    @Test
    @DatabaseSetup(
        connection = "wrapConnection",
        value = "classpath:fixtures/functional/create_inbound/4/db_state.xml"
    )
    @DatabaseSetup(
        connection = "wmsConnection",
        value = "classpath:fixtures/functional/create_inbound/4/wms_db_state.xml"
    )
    void createWithMultipleItemsFromSingleMerchant() throws Exception {

        executeScenario(
            "fixtures/functional/create_inbound/common/wrap_request.xml",
            "fixtures/functional/create_inbound/common/wrap_response.xml",

            inforInteraction(fulfillmentUrl(Arrays.asList(
                clientProperties.getWarehouseKey(), "storers", "storerbatch"), HttpMethod.POST)
            ).setExpectedRequestPath("fixtures/functional/create_inbound/common/put_storer_request.json")
                .setResponsePath("fixtures/functional/create_inbound/common/put_storer_response.json"),

            inforInteraction(fulfillmentUrl(Arrays.asList(
                clientProperties.getEnterpriseKey(), "items", "itembatch"), HttpMethod.POST)
            ).setExpectedRequestPath("fixtures/functional/create_inbound/common/put_two_items_request.json")
                .setResponsePath("fixtures/functional/create_inbound/common/put_two_items_response.json"),

            inforInteraction(fulfillmentUrl(Arrays.asList(
                clientProperties.getWarehouseKey(), "receipts"), HttpMethod.POST)
            ).setExpectedRequestPath("fixtures/functional/create_inbound/common/post_receipt_request.json")
                .setResponsePath("fixtures/functional/create_inbound/common/post_receipt_response.json")
        );
    }

    /**
     * Сценарий #5:
     * <p>
     * Поставка с таким идентификатором уже существует в БД.
     * <p>
     * В результате исполнения запроса клиенту вернется записанная
     * в БД связка yandexId/partnerId без попытки создать новую поставку.
     */
    @Test
    @DatabaseSetup(
        connection = "wmsConnection",
        value = "classpath:fixtures/functional/create_inbound/5/db_state.xml"
    )
    void returnExistingInbound() throws Exception {
        executeScenario(
            "fixtures/functional/create_inbound/5/wrap_request.xml",
            "fixtures/functional/create_inbound/5/wrap_response.xml"
        );
    }

    /**
     * Сценарий #6:
     * <p>
     * Создаем корректную поставку с несколькими товарами от одного поставщика
     * - в ответ ожидаем получить честный идентификатор поставки.
     * <p>
     * update_sku_on_inbound_creation=false + все sku уже созданы.
     * <p>
     * Ожидаемое взаимодействие с Infor SCE:
     * 1 запрос на создание Storer'а
     * НИ ОДНОГО запроса на создание SKU со всеми ALTSKU
     * 1 запрос на создание Receipt
     */
    @Test
    @DatabaseSetup(
        connection = "wrapConnection",
        value = "classpath:fixtures/functional/create_inbound/common/identifier_mapping_db_state.xml"
    )
    @DatabaseSetup(
        connection = "wmsConnection",
        value = "classpath:fixtures/functional/create_inbound/6/sku_db_state.xml"
    )
    @DatabaseSetup(
        connection = "wrapConnection",
        value = "classpath:fixtures/functional/create_inbound/6/system_properties_db_state.xml"
    )
    void createWithUpdateSkuPropertyAsTrueAndFullyCreatedSkus() throws Exception {

        executeScenario(
            "fixtures/functional/create_inbound/common/wrap_request.xml",
            "fixtures/functional/create_inbound/common/wrap_response.xml",

            inforInteraction(fulfillmentUrl(Arrays.asList(
                clientProperties.getWarehouseKey(), "storers", "storerbatch"), HttpMethod.POST)
            ).setExpectedRequestPath("fixtures/functional/create_inbound/common/put_storer_request.json")
                .setResponsePath("fixtures/functional/create_inbound/common/put_storer_response.json"),

            inforInteraction(fulfillmentUrl(Arrays.asList(
                clientProperties.getWarehouseKey(), "receipts"), HttpMethod.POST)
            ).setExpectedRequestPath("fixtures/functional/create_inbound/common/post_receipt_request.json")
                .setResponsePath("fixtures/functional/create_inbound/common/post_receipt_response.json")
        );
    }

    /**
     * Сценарий #7:
     * <p>
     * Создаем корректную поставку с несколькими товарами от одного поставщика
     * - в ответ ожидаем получить честный идентификатор поставки.
     * <p>
     * update_sku_on_inbound_creation=false + только одна sku из 2-х уже создана.
     * <p>
     * Ожидаемое взаимодействие с Infor SCE:
     * 1 запрос на создание Storer'а
     * 1 запрос на создание SKU со всеми ALTSKU
     * 1 запрос на создание Receipt
     */
    @Test
    @DatabaseSetup(
        connection = "wrapConnection",
        value = "classpath:fixtures/functional/create_inbound/common/identifier_mapping_db_state.xml"
    )
    @DatabaseSetup(
        connection = "wmsConnection",
        value = "classpath:fixtures/functional/create_inbound/7/sku_db_state.xml"
    )
    @DatabaseSetup(
        connection = "wrapConnection",
        value = "classpath:fixtures/functional/create_inbound/7/system_properties_db_state.xml"
    )
    void createWithUpdateSkuPropertyAsTrueAndPartiallyCreatedSkus() throws Exception {

        mockIrisCommunication(
            "fixtures/functional/create_inbound/common/get_wgh_item1_request.json",
            "fixtures/functional/create_inbound/common/get_wgh_item1_response.json"
        );

        executeScenario(
            "fixtures/functional/create_inbound/common/wrap_request.xml",
            "fixtures/functional/create_inbound/common/wrap_response.xml",

            inforInteraction(fulfillmentUrl(Arrays.asList(
                clientProperties.getWarehouseKey(), "storers", "storerbatch"), HttpMethod.POST)
            ).setExpectedRequestPath("fixtures/functional/create_inbound/common/put_storer_request.json")
                .setResponsePath("fixtures/functional/create_inbound/common/put_storer_response.json"),

            inforInteraction(fulfillmentUrl(Arrays.asList(
                clientProperties.getEnterpriseKey(), "items", "itembatch"), HttpMethod.POST)
            ).setExpectedRequestPath("fixtures/functional/create_inbound/common/put_item1_request.json")
                .setResponsePath("fixtures/functional/create_inbound/common/put_item1_response.json"),

            inforInteraction(fulfillmentUrl(Arrays.asList(
                clientProperties.getWarehouseKey(), "receipts"), HttpMethod.POST)
            ).setExpectedRequestPath("fixtures/functional/create_inbound/common/post_receipt_request.json")
                .setResponsePath("fixtures/functional/create_inbound/common/post_receipt_response.json")
        );
    }

    /**
     * Сценарий #8:
     * <p>
     * Создаем корректную поставку с несколькими товарами от одного поставщика
     * - в ответ ожидаем получить честный идентификатор поставки.
     * <p>
     * update_sku_on_inbound_creation=true и обе sku уже созданы.
     * <p>
     * Ожидаемое взаимодействие с Infor SCE:
     * 1 запрос на создание Storer'а
     * 2 запроса на создание SKU со всеми ALTSKU
     * 1 запрос на создание Receipt
     */
    @Test
    @DatabaseSetup(
        connection = "wrapConnection",
        value = "classpath:fixtures/functional/create_inbound/common/identifier_mapping_db_state.xml"
    )
    @DatabaseSetup(
        connection = "wmsConnection",
        value = "classpath:fixtures/functional/create_inbound/8/sku_db_state.xml"
    )
    @DatabaseSetup(
        connection = "wrapConnection",
        value = "classpath:fixtures/functional/create_inbound/8/system_properties_db_state.xml"
    )
    void createWithUpdateSkuPropertyAsTrueAndNonCreatedSkus() throws Exception {

        executeScenario(
            "fixtures/functional/create_inbound/common/wrap_request.xml",
            "fixtures/functional/create_inbound/common/wrap_response.xml",

            inforInteraction(fulfillmentUrl(Arrays.asList(
                clientProperties.getWarehouseKey(), "storers", "storerbatch"), HttpMethod.POST)
            ).setExpectedRequestPath("fixtures/functional/create_inbound/common/put_storer_request.json")
                .setResponsePath("fixtures/functional/create_inbound/common/put_storer_response.json"),

            inforInteraction(fulfillmentUrl(Arrays.asList(
                clientProperties.getEnterpriseKey(), "items", "itembatch"), HttpMethod.POST)
            ).setExpectedRequestPath("fixtures/functional/create_inbound/common/put_two_items_request.json")
                .setResponsePath("fixtures/functional/create_inbound/common/put_two_items_response.json"),

            inforInteraction(fulfillmentUrl(Arrays.asList(
                clientProperties.getWarehouseKey(), "receipts"), HttpMethod.POST)
            ).setExpectedRequestPath("fixtures/functional/create_inbound/common/post_receipt_request.json")
                .setResponsePath("fixtures/functional/create_inbound/common/post_receipt_response.json")
        );
    }

    /**
     * Проверка корректного пробрасывания исключения от InforClient.
     * </p>
     */
    @Test
    void clientRespondsWithException() throws Exception {

        executeScenario(
            "fixtures/functional/create_inbound/6/wrap_request.xml",
            "fixtures/functional/create_inbound/6/wrap_response.xml",
            inforInteraction(fulfillmentUrl(Arrays.asList(
                clientProperties.getWarehouseKey(), "storers", "storerbatch"), HttpMethod.POST))
                .setResponsePath("fixtures/functional/common/failed_client_response.json")
                .setResponseStatus(HttpStatus.NOT_FOUND)
        );
    }

    @Test
    @DatabaseSetup(
            connection = "wrapConnection",
            value = "classpath:fixtures/functional/create_inbound/4/db_state.xml"
    )
    @DatabaseSetup(
            connection = "wmsConnection",
            value = "classpath:fixtures/functional/create_inbound/4/wms_db_state.xml"
    )
    void createWithImeiAndSn() throws Exception {
        executeScenario(
                "fixtures/functional/create_inbound/9/wrap_request.xml",
                "fixtures/functional/create_inbound/common/wrap_response.xml",

                inforInteraction(fulfillmentUrl(Arrays.asList(
                        clientProperties.getWarehouseKey(), "storers", "storerbatch"), HttpMethod.POST)
                ).setExpectedRequestPath("fixtures/functional/create_inbound/common/put_storer_request.json")
                        .setResponsePath("fixtures/functional/create_inbound/common/put_storer_response.json"),

                inforInteraction(fulfillmentUrl(Arrays.asList(
                        clientProperties.getEnterpriseKey(), "items", "itembatch"), HttpMethod.POST)
                ).setExpectedRequestPath("fixtures/functional/create_inbound/common/put_two_items_request.json")
                        .setResponsePath("fixtures/functional/create_inbound/common/put_two_items_response.json"),

                inforInteraction(fulfillmentUrl(Arrays.asList(
                        clientProperties.getEnterpriseKey(), "items", "identity"), HttpMethod.PUT)
                ).setExpectedRequestPath("fixtures/functional/create_inbound/9/put_items_identity_request.json")
                        .setResponsePath("fixtures/functional/create_inbound/common/items_identity_response.json"),

                inforInteraction(fulfillmentUrl(Arrays.asList(
                        clientProperties.getWarehouseKey(), "receipts"), HttpMethod.POST)
                ).setExpectedRequestPath("fixtures/functional/create_inbound/common/post_receipt_request.json")
                        .setResponsePath("fixtures/functional/create_inbound/common/post_receipt_response.json")
        );
    }

    @Test
    @DatabaseSetup(
            connection = "wrapConnection",
            value = "classpath:fixtures/functional/create_inbound/4/db_state.xml"
    )
    @DatabaseSetup(
            connection = "wmsConnection",
            value = "classpath:fixtures/functional/create_inbound/4/wms_db_state.xml"
    )
    void createWithImeiAndNoSn() throws Exception {
        executeScenario(
                "fixtures/functional/create_inbound/10/wrap_request.xml",
                "fixtures/functional/create_inbound/common/wrap_response.xml",

                inforInteraction(fulfillmentUrl(Arrays.asList(
                        clientProperties.getWarehouseKey(), "storers", "storerbatch"), HttpMethod.POST)
                ).setExpectedRequestPath("fixtures/functional/create_inbound/common/put_storer_request.json")
                        .setResponsePath("fixtures/functional/create_inbound/common/put_storer_response.json"),

                inforInteraction(fulfillmentUrl(Arrays.asList(
                        clientProperties.getEnterpriseKey(), "items", "itembatch"), HttpMethod.POST)
                ).setExpectedRequestPath("fixtures/functional/create_inbound/common/put_two_items_request.json")
                        .setResponsePath("fixtures/functional/create_inbound/common/put_two_items_response.json"),

                inforInteraction(fulfillmentUrl(Arrays.asList(
                        clientProperties.getEnterpriseKey(), "items", "identity"), HttpMethod.PUT)
                ).setExpectedRequestPath("fixtures/functional/create_inbound/10/put_items_identity_request.json")
                        .setResponsePath("fixtures/functional/create_inbound/common/items_identity_response.json"),

                inforInteraction(fulfillmentUrl(Arrays.asList(
                        clientProperties.getWarehouseKey(), "receipts"), HttpMethod.POST)
                ).setExpectedRequestPath("fixtures/functional/create_inbound/common/post_receipt_request.json")
                        .setResponsePath("fixtures/functional/create_inbound/common/post_receipt_response.json")
        );
    }

    @Test
    @DatabaseSetup(
            connection = "wrapConnection",
            value = "classpath:fixtures/functional/create_inbound/4/db_state.xml"
    )
    @DatabaseSetup(
            connection = "wmsConnection",
            value = "classpath:fixtures/functional/create_inbound/4/wms_db_state.xml"
    )
    void createWithRequiredAndNotRequiredIdentities() throws Exception {
        executeScenario(
                "fixtures/functional/create_inbound/11/wrap_request.xml",
                "fixtures/functional/create_inbound/common/wrap_response.xml",

                inforInteraction(fulfillmentUrl(Arrays.asList(
                        clientProperties.getWarehouseKey(), "storers", "storerbatch"), HttpMethod.POST)
                ).setExpectedRequestPath("fixtures/functional/create_inbound/common/put_storer_request.json")
                        .setResponsePath("fixtures/functional/create_inbound/common/put_storer_response.json"),

                inforInteraction(fulfillmentUrl(Arrays.asList(
                        clientProperties.getEnterpriseKey(), "items", "itembatch"), HttpMethod.POST)
                ).setExpectedRequestPath("fixtures/functional/create_inbound/common/put_two_items_request.json")
                        .setResponsePath("fixtures/functional/create_inbound/common/put_two_items_response.json"),

                inforInteraction(fulfillmentUrl(Arrays.asList(
                        clientProperties.getEnterpriseKey(), "items", "identity"), HttpMethod.DELETE)
                ).setExpectedRequestPath("fixtures/functional/create_inbound/11/delete_items_identity_request.json")
                        .setResponsePath("fixtures/functional/create_inbound/common/items_identity_response.json"),

                inforInteraction(fulfillmentUrl(Arrays.asList(
                        clientProperties.getEnterpriseKey(), "items", "identity"), HttpMethod.PUT)
                ).setExpectedRequestPath("fixtures/functional/create_inbound/11/put_items_identity_request.json")
                        .setResponsePath("fixtures/functional/create_inbound/common/items_identity_response.json"),

                inforInteraction(fulfillmentUrl(Arrays.asList(
                        clientProperties.getWarehouseKey(), "receipts"), HttpMethod.POST)
                ).setExpectedRequestPath("fixtures/functional/create_inbound/common/post_receipt_request.json")
                        .setResponsePath("fixtures/functional/create_inbound/common/post_receipt_response.json")
        );
    }

    private void executeSingleItemScenario() throws Exception {
        executeSingleItemScenario(
            "fixtures/functional/create_inbound/2/put_item_request.json",
            "fixtures/functional/create_inbound/2/put_item_response.json"
        );
    }

    private void executeSingleItemScenario(String putRequestPath,
                                           String putResponsePath) throws Exception {
        executeSingleItemScenario(putRequestPath, putResponsePath,
            "fixtures/functional/create_inbound/2/post_receipt_request.json",
            "fixtures/functional/create_inbound/2/post_receipt_response.json",
            "fixtures/functional/create_inbound/2/wrap_request.xml",
            "fixtures/functional/create_inbound/2/wrap_response.xml");
    }

    private void executeSingleItemScenario(String putRequestPath,
                                           String putResponsePath,
                                           String postReceiptRequestPath,
                                           String postReceiptResponsePath,
                                           String wrapRequestPath,
                                           String wrapResponsePath) throws Exception {

        executeScenario(wrapRequestPath, wrapResponsePath,

            inforInteraction(fulfillmentUrl(Arrays.asList(
                clientProperties.getWarehouseKey(), "storers", "storerbatch"), HttpMethod.POST)
            ).setExpectedRequestPath("fixtures/functional/create_inbound/2/put_storer_request.json")
                .setResponsePath("fixtures/functional/create_inbound/2/put_storer_response.json"),

            inforInteraction(fulfillmentUrl(Arrays.asList(
                clientProperties.getEnterpriseKey(), "items", "itembatch"), HttpMethod.POST)
            ).setExpectedRequestPath(putRequestPath)
                .setResponsePath(putResponsePath),

            inforInteraction(fulfillmentUrl(Arrays.asList(
                clientProperties.getWarehouseKey(), "receipts"), HttpMethod.POST)
            ).setExpectedRequestPath(postReceiptRequestPath)
                .setResponsePath(postReceiptResponsePath)
        );
    }

    private void executeScenario(String wrapRequest,
                                 String wrapResponse,
                                 FulfillmentInteraction... interactions) throws Exception {
        FunctionalTestScenarioBuilder.start(CreateInboundResponse.class)
            .sendRequestToWrapQueryGateway(wrapRequest)
            .thenMockFulfillmentRequests(interactions)
            .andExpectWrapAnswerToBeEqualTo(wrapResponse)
            .build(mockMvc, restTemplate, fulfillmentMapper, clientProperties.getUrl())
            .start();

    }
}


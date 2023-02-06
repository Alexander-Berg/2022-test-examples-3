package ru.yandex.market.wrap.infor.functional;

import java.util.Arrays;
import java.util.List;

import com.google.common.collect.ImmutableMap;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.http.HttpMethod;

import ru.yandex.market.fulfillment.wrap.core.scenario.builder.FulfillmentInteraction;
import ru.yandex.market.fulfillment.wrap.core.scenario.builder.FunctionalTestScenarioBuilder;
import ru.yandex.market.logistic.api.model.fulfillment.ErrorCode;
import ru.yandex.market.logistic.api.model.fulfillment.ErrorItem;
import ru.yandex.market.logistic.api.model.fulfillment.ErrorPair;
import ru.yandex.market.logistic.api.model.fulfillment.Item;
import ru.yandex.market.logistic.api.model.fulfillment.UnitId;
import ru.yandex.market.logistic.api.model.fulfillment.exception.FulfillmentApiException;
import ru.yandex.market.logistic.api.model.fulfillment.response.PutReferenceItemsResponse;
import ru.yandex.market.wrap.infor.service.common.PutReferenceItemsService;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static ru.yandex.market.fulfillment.wrap.core.scenario.builder.FulfillmentUrl.fulfillmentUrl;

class PutReferenceItemsTest extends AbstractFunctionalTestWithIrisCommunication {

    private static final UnitId FIRST_UNIT_ID = new UnitId("1", 1L, "1");
    private static final UnitId SECOND_UNIT_ID = new UnitId("2", 2L, "2");
    private static final Item FIRST_ITEM = createItem(FIRST_UNIT_ID);
    private static final Item SECOND_ITEM = createItem(SECOND_UNIT_ID);
    private static final String EXCEPTION_MESSAGE = "Something comes wrong";
    private static final ErrorItem ERROR_WITH_SECOND_ITEM = createErrorItem(SECOND_ITEM);

    @SpyBean
    private PutReferenceItemsService putReferenceItemsService;

    /**
     * Сценарий #1:
     * <p>
     * Выполняем putReferenceItems без товаров - в ответ ожидаем получить ошибку сериализации.
     * Взаимодействия с Infor SCE произойти не должно.
     */
    @Test
    void putEmptyItemsList() throws Exception {
        FunctionalTestScenarioBuilder.start(PutReferenceItemsResponse.class)
            .sendRequestToWrapQueryGateway("fixtures/functional/put_reference_items/1/wrap_request.xml")
            .andExpectWrapAnswerToContainErrors(
                ImmutableMap.of(ru.yandex.market.logistic.api.model.common.ErrorCode.BAD_REQUEST, 1))
            .build(mockMvc, restTemplate, fulfillmentMapper, clientProperties.getUrl())
            .start();
    }

    /**
     * Сценарий #2:
     * <p>
     * Выполняем putReferenceItems с одиним товаром - в ответ ожидаем получить успешный результат.
     * Ожидаемое взаимодействие с Infor SCE:
     * 1 запрос на создание Storer'а
     * 1 запрос на получение информации из IRIS (но без данных)
     * 1 запрос на создание SKU со всеми ALTSKU (но без вгх)
     */
    @Test
    void successfulPutItemReference() throws Exception {
        mockIrisCommunication(
            "fixtures/functional/put_reference_items/2/iris_request.json",
            "fixtures/functional/put_reference_items/2/iris_response.json"
        );

        executeSingleItemScenario("fixtures/functional/put_reference_items/2/put_item_request.json");
    }

    /**
     * Сценарий #2.1:
     * <p>
     * Выполняем putReferenceItems с одиним товаром - в ответ ожидаем получить успешный результат.
     * Ожидаемое взаимодействие с Infor SCE:
     * 1 запрос на создание Storer'а
     * 1 запрос на получение информации из IRIS (с корректными данными)
     * 1 запрос на создание SKU со всеми ALTSKU + ВГХ
     */
    @Test
    void successfulPutItemReferenceWithIrisData() throws Exception {
        mockIrisCommunication(
            "fixtures/functional/put_reference_items/2/iris_request.json",
            "fixtures/functional/put_reference_items/2/1/iris_response.json"
        );

        executeSingleItemScenario("fixtures/functional/put_reference_items/2/1/put_item_request.json");
    }

    /**
     * Сценарий #2.2:
     * <p>
     * Выполняем putReferenceItems с одиним товаром и с наличием contractor - в ответ ожидаем получить успешный результат.
     * Ожидаемое взаимодействие с Infor SCE:
     * 1 запрос на создание Storer'а
     * 1 запрос на создание contractor
     * 1 запрос на получение информации из IRIS (но без данных)
     * 1 запрос на создание SKU со всеми ALTSKU (но без вгх)
     */
    @Test
    void successfulPutItemReferenceWithContractor() throws Exception {
        mockIrisCommunication(
            "fixtures/functional/put_reference_items/2/iris_request.json",
            "fixtures/functional/put_reference_items/2/iris_response.json"
        );

        executeScenario("fixtures/functional/put_reference_items/2/2/wrap_request.xml",
            "fixtures/functional/put_reference_items/2/wrap_response.xml",

            inforInteraction(fulfillmentUrl(Arrays.asList(
                clientProperties.getWarehouseKey(), "storers", "storerbatch"), HttpMethod.POST)
            ).setExpectedRequestPath("fixtures/functional/put_reference_items/2/put_storer_request.json")
                .setResponsePath("fixtures/functional/put_reference_items/2/put_storer_response.json"),

            inforInteraction(fulfillmentUrl(Arrays.asList(
                clientProperties.getWarehouseKey(), "storers", "storerbatch"), HttpMethod.POST)
            ).setExpectedRequestPath("fixtures/functional/put_reference_items/2/2/put_contractor_storer_request.json")
                .setResponsePath("fixtures/functional/put_reference_items/2/2/put_contractor_storer_response.json"),

            inforInteraction(fulfillmentUrl(Arrays.asList(
                clientProperties.getEnterpriseKey(), "items", "itembatch"), HttpMethod.POST)
            ).setExpectedRequestPath("fixtures/functional/put_reference_items/2/2/put_item_request.json")
                .setResponsePath("fixtures/functional/put_reference_items/2/2/put_item_response.json"));
    }

    /**
     * Сценарий #3:
     * <p>
     * Выполняем putReferenceItems с одиним товаром без vendorId - в ответ ожидаем получить ошибку.
     * Взаимодействия с Infor SCE произойти не должно.
     */
    @Test
    void failedToPutItemReferenceWithoutVendorId() throws Exception {
        FunctionalTestScenarioBuilder.start(PutReferenceItemsResponse.class)
            .sendRequestToWrapQueryGateway("fixtures/functional/put_reference_items/3/wrap_request.xml")
            .andExpectWrapAnswerToContainErrors(
                ImmutableMap.of(ru.yandex.market.logistic.api.model.common.ErrorCode.BAD_REQUEST, 1))
            .andExpectWrapAnswerToBeEqualTo("fixtures/functional/put_reference_items/3/wrap_response.xml")
            .build(mockMvc, restTemplate, fulfillmentMapper, clientProperties.getUrl())
            .start();
    }

    @Test
    void testPutSingleItemWorks() {
        doReturn(null)
            .when(putReferenceItemsService).putReferenceItems(any(), anyBoolean());

        List<ErrorItem> errorItems =
            putReferenceItemsService.putReferenceItemsUsingBatches(singletonList(FIRST_ITEM), true);

        assertTrue(errorItems.isEmpty(), "There shouldn't be any errors");
        verify(putReferenceItemsService).putReferenceItems(any(), eq(true));
    }

    @Test
    void testPutTwoItemsInTwoBatchesWithDifferentResultWorks() {

        doReturn(null)
            .doThrow(new RuntimeException(EXCEPTION_MESSAGE))
            .when(putReferenceItemsService).putReferenceItems(any(), anyBoolean());

        List<ErrorItem> errorItems =
            putReferenceItemsService.putReferenceItemsUsingBatches(asList(FIRST_ITEM, SECOND_ITEM), true);

        assertEquals(1, errorItems.size(), "There should be one error");
        assertEquals(ERROR_WITH_SECOND_ITEM, errorItems.get(0), "Error should be with second item");
        verify(putReferenceItemsService, times(2)).putReferenceItems(any(), eq(true));
    }

    @Test
    void testPutTwoItemsInTwoBatchesWithExceptionsInBothWorks() {

        doThrow(new RuntimeException(EXCEPTION_MESSAGE))
            .doThrow(new RuntimeException(EXCEPTION_MESSAGE))
            .when(putReferenceItemsService).putReferenceItems(any(), anyBoolean());

        Assertions.assertThatThrownBy(() -> putReferenceItemsService.putReferenceItemsUsingBatches(
            asList(FIRST_ITEM, SECOND_ITEM), true
        ))
            .isInstanceOf(FulfillmentApiException.class);
    }

    private void executeSingleItemScenario(String putItemRequestPath) throws Exception {

        executeScenario("fixtures/functional/put_reference_items/2/wrap_request.xml",
            "fixtures/functional/put_reference_items/2/wrap_response.xml",

            inforInteraction(fulfillmentUrl(Arrays.asList(
                clientProperties.getWarehouseKey(), "storers", "storerbatch"), HttpMethod.POST)
            ).setExpectedRequestPath("fixtures/functional/put_reference_items/2/put_storer_request.json")
                .setResponsePath("fixtures/functional/put_reference_items/2/put_storer_response.json"),

            inforInteraction(fulfillmentUrl(Arrays.asList(
                clientProperties.getEnterpriseKey(), "items", "itembatch"), HttpMethod.POST)
            ).setExpectedRequestPath(putItemRequestPath)
                .setResponsePath("fixtures/functional/put_reference_items/2/put_item_response.json"));
    }

    private static Item createItem(UnitId unitId) {
        return new Item.ItemBuilder(null, null, null).setUnitId(unitId).build();
    }

    private static ErrorItem createErrorItem(Item item) {
        return new ErrorItem.ErrorItemBuilder(item.getUnitId())
            .setErrorCode(new ErrorPair.ErrorPairBuilder(ErrorCode.UNKNOWN_ERROR, EXCEPTION_MESSAGE).build())
            .build();
    }

    private void executeScenario(String wrapRequest,
                                 String wrapResponse,
                                 FulfillmentInteraction... interactions) throws Exception {
        FunctionalTestScenarioBuilder.start(PutReferenceItemsResponse.class)
            .sendRequestToWrapQueryGateway(wrapRequest)
            .thenMockFulfillmentRequests(interactions)
            .andExpectWrapAnswerToBeEqualTo(wrapResponse)
            .build(mockMvc, restTemplate, fulfillmentMapper, clientProperties.getUrl())
            .start();

    }
}

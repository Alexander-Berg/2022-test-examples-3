package ru.yandex.market.api.partner.controllers.order.v2.edit;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.stream.Stream;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.HttpClientErrorException;

import ru.yandex.market.api.partner.apisupport.ErrorRestModelCode;
import ru.yandex.market.api.partner.context.Format;
import ru.yandex.market.api.partner.context.FunctionalTestHelper;
import ru.yandex.market.api.partner.controllers.order.model.OrderItemsModificationRequestReason;
import ru.yandex.market.api.partner.controllers.order.v2.OrderControllerV2TestTemplate;
import ru.yandex.market.checkout.checkouter.event.HistoryEventReason;
import ru.yandex.market.checkout.checkouter.order.OrderEditRequest;
import ru.yandex.market.checkout.checkouter.order.OrderItemCisesValidationException;
import ru.yandex.market.checkout.checkouter.order.OrderItemInstance;
import ru.yandex.market.checkout.checkouter.order.OrderStatusNotAllowedException;
import ru.yandex.market.checkout.checkouter.order.changerequest.itemsremoval.CannotRemoveItemException;
import ru.yandex.market.checkout.checkouter.order.changerequest.itemsremoval.ItemInfo;
import ru.yandex.market.checkout.checkouter.order.changerequest.itemsremoval.MissingItemsNotification;
import ru.yandex.market.checkout.checkouter.request.RequestClientInfo;
import ru.yandex.market.checkout.common.rest.ErrorCodeException;
import ru.yandex.market.common.retrofit.CommonRetrofitHttpExecutionException;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.common.test.util.JsonTestUtil;
import ru.yandex.market.mbi.util.MbiAsserts;
import ru.yandex.market.orderservice.client.model.ActorType;
import ru.yandex.market.orderservice.client.model.ApiError;
import ru.yandex.market.orderservice.client.model.ChangeOrderLinesRequest;
import ru.yandex.market.orderservice.client.model.ChangeOrderLinesResponse;
import ru.yandex.market.orderservice.client.model.ChangeOrderLinesResponseDto;
import ru.yandex.market.orderservice.client.model.ChangeRequestStatusType;
import ru.yandex.market.orderservice.client.model.ChangedOrderItemDto;
import ru.yandex.market.orderservice.client.model.CommonApiResponse;
import ru.yandex.market.orderservice.client.model.OrderLineChange;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static ru.yandex.market.api.partner.controllers.order.config.OrderControllerV2Config.ENV_ROUTE_REQS_TO_ORDER_SERVICE;
import static ru.yandex.market.checkout.checkouter.order.OrderItemsException.CANNOT_REMOVE_LAST_ITEM_CODE;
import static ru.yandex.market.checkout.checkouter.order.OrderItemsException.INVALID_ITEM_CODE;
import static ru.yandex.market.checkout.checkouter.order.OrderItemsException.ITEMS_ADDITION_NOT_SUPPORTED_CODE;
import static ru.yandex.market.checkout.checkouter.order.OrderItemsException.ITEM_DUPLICATE_CODE;
import static ru.yandex.market.checkout.checkouter.order.OrderItemsException.ITEM_NOT_FOUND_CODE;
import static ru.yandex.market.checkout.checkouter.order.OrderItemsException.ITEM_SHIPPED_CODE;
import static ru.yandex.market.core.matchers.HttpClientErrorMatcher.hasErrorCode;

/**
 * Тесты ручки изменения состава заказа (orders/{orderId}/items)
 */
@DbUnitDataSet(before = "../OrderControllerTest.before.csv")
public class OrderEditTest extends OrderControllerV2TestTemplate {

    @ParameterizedTest
    @CsvSource({
            "true,USER_REQUESTED_REMOVE,USER_REQUESTED_REMOVE",
            "true,PARTNER_REQUESTED_REMOVE,ITEMS_NOT_SUPPLIED",
            "true,,ITEMS_NOT_SUPPLIED",
            "false,USER_REQUESTED_REMOVE,USER_REQUESTED_REMOVE",
            "false,PARTNER_REQUESTED_REMOVE,ITEMS_NOT_SUPPLIED",
            "false,,ITEMS_NOT_SUPPLIED",
    })
    @DisplayName("Изменение состава заказа (XML)")
    void testUpdateOrderItemsXml(Boolean isDbs, OrderItemsModificationRequestReason reason,
                                 HistoryEventReason expectedCheckouterReason) {
        long orderId = 2827258L;
        long partnerId = isDbs ? 2001L : 666L;

        prepareFf4ShopsGetRemovalPermissionsMock("mocks/ff4shops/order_removal_permissions.json", orderId, partnerId);

        prepareUpdateOrderItemsCheckouterMock("mocks/checkouter/get_order_update_items.json", orderId, partnerId);

        //language=xml
        String requestBody = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
                "<orderItemsModificationRequest>" +
                "    <items>" +
                "        <item id='4055992' count='5'>" +
                "            <instances>" +
                "                <instance cis='CIS-4055992-1'/>" +
                "                <instance cis='CIS-4055992-3'/>" +
                "                <instance cis='CIS-4055992-5'/>" +
                "            </instances>" +
                "        </item>" +
                "    </items>" +
                (reason == null ? "" : ("    <reason>" + reason.name() + "</reason>")) +
                "</orderItemsModificationRequest>";

        FunctionalTestHelper.makeRequest(
                urlBasePrefix + "/campaigns/" + (isDbs ? DROPSHIP_BY_SELLER_CAMPAIGN_ID : DROPSHIP_CAMPAIGN_ID) +
                        "/orders/" + orderId + "/items.xml",
                HttpMethod.PUT,
                Format.XML,
                requestBody);

        ArgumentCaptor<OrderEditRequest> captor = forClass(OrderEditRequest.class);
        ArgumentCaptor<RequestClientInfo> requestClientInfoCaptor = forClass(RequestClientInfo.class);
        verify(checkouterAPI, times(1)).editOrder(eq(orderId), requestClientInfoCaptor.capture(), anyList(),
                captor.capture());

        OrderEditRequest orderEditRequest = captor.getValue();
        assertNotNull(orderEditRequest);

        MissingItemsNotification missingItemsNotification = orderEditRequest.getMissingItemsNotification();
        assertNotNull(missingItemsNotification);

        assertEquals(partnerId, requestClientInfoCaptor.getValue().getClientId());

        assertEquals(expectedCheckouterReason, missingItemsNotification.getReason());
        assertTrue(missingItemsNotification.isAlreadyRemovedByWarehouse());
        Collection<ItemInfo> remainedItems = missingItemsNotification.getRemainedItems();
        assertNotNull(remainedItems);
        assertThat(remainedItems).hasSize(2);
        assertThat(remainedItems).containsExactlyInAnyOrder(
                new ItemInfo(4055993L, null, null, 0, Collections.emptySet()),
                new ItemInfo(4055992L, null, null, 5,
                        Set.of(
                                new OrderItemInstance("CIS-4055992-1"),
                                new OrderItemInstance("CIS-4055992-3"),
                                new OrderItemInstance("CIS-4055992-5")
                        )
                )
        );
    }


    @ParameterizedTest
    @CsvSource({
            "true,USER_REQUESTED_REMOVE,USER_REQUESTED_REMOVE",
            "true,PARTNER_REQUESTED_REMOVE,ITEMS_NOT_SUPPLIED",
            "true,,ITEMS_NOT_SUPPLIED",
            "false,USER_REQUESTED_REMOVE,USER_REQUESTED_REMOVE",
            "false,PARTNER_REQUESTED_REMOVE,ITEMS_NOT_SUPPLIED",
            "false,,ITEMS_NOT_SUPPLIED",
    })
    @DisplayName("Изменение состава заказа (JSON)")
    void testUpdateOrderItemsJson(Boolean isDbs, OrderItemsModificationRequestReason reason,
                                  HistoryEventReason expectedCheckouterReason) {
        long orderId = 2827258L;
        long partnerId = isDbs ? 2001L : 666L;

        prepareFf4ShopsGetRemovalPermissionsMock("mocks/ff4shops/order_removal_permissions.json", orderId, partnerId);

        prepareUpdateOrderItemsCheckouterMock("mocks/checkouter/get_order_update_items.json", orderId, partnerId);

        //language=json
        String requestBody = "" +
                "{ " +
                "  \"items\":[" +
                "    {" +
                "      \"id\":4055992, " +
                "      \"count\":5, " +
                "      \"instances\": [" +
                "        {" +
                "          \"cis\": \"CIS-4055992-1\"" +
                "        }," +
                "        {" +
                "          \"cis\": \"CIS-4055992-3\"" +
                "        }," +
                "        {" +
                "          \"cis\": \"CIS-4055992-5\"" +
                "        }" +
                "      ]" +
                "    }" +
                "  ]" + (reason == null ? "" : ", \"reason\":\"" + reason.name() + "\"") +
                "}";

        FunctionalTestHelper.makeRequest(
                urlBasePrefix + "/campaigns/" + (isDbs ? DROPSHIP_BY_SELLER_CAMPAIGN_ID : DROPSHIP_CAMPAIGN_ID) +
                        "/orders/" + orderId + "/items.json",
                HttpMethod.PUT,
                Format.JSON,
                requestBody);

        ArgumentCaptor<OrderEditRequest> captor = forClass(OrderEditRequest.class);
        ArgumentCaptor<RequestClientInfo> requestClientInfoCaptor = forClass(RequestClientInfo.class);
        verify(checkouterAPI, times(1)).editOrder(eq(orderId), requestClientInfoCaptor.capture(), anyList(),
                captor.capture());

        OrderEditRequest orderEditRequest = captor.getValue();
        assertNotNull(orderEditRequest);

        MissingItemsNotification missingItemsNotification = orderEditRequest.getMissingItemsNotification();
        assertNotNull(missingItemsNotification);

        assertEquals(partnerId, requestClientInfoCaptor.getValue().getClientId());

        assertEquals(expectedCheckouterReason, missingItemsNotification.getReason());
        assertTrue(missingItemsNotification.isAlreadyRemovedByWarehouse());
        Collection<ItemInfo> remainedItems = missingItemsNotification.getRemainedItems();
        assertNotNull(remainedItems);
        assertThat(remainedItems).hasSize(2);
        assertThat(remainedItems).containsExactlyInAnyOrder(
                new ItemInfo(4055993L, null, null, 0, Collections.emptySet()),
                new ItemInfo(4055992L, null, null, 5,
                        Set.of(
                                new OrderItemInstance("CIS-4055992-1"),
                                new OrderItemInstance("CIS-4055992-3"),
                                new OrderItemInstance("CIS-4055992-5")
                        )
                )
        );
    }

    @ParameterizedTest
    @CsvSource({
            "true,USER_REQUESTED_REMOVE,USER_REQUESTED_REMOVE",
            "true,PARTNER_REQUESTED_REMOVE,ITEMS_NOT_SUPPLIED",
            "true,,ITEMS_NOT_SUPPLIED",
            "false,USER_REQUESTED_REMOVE,USER_REQUESTED_REMOVE",
            "false,PARTNER_REQUESTED_REMOVE,ITEMS_NOT_SUPPLIED",
            "false,,ITEMS_NOT_SUPPLIED",
    })
    @DisplayName("Изменение состава заказа (JSON) через order-service")
    void testUpdateOrderItemsJsonViaOS(Boolean isDbs, OrderItemsModificationRequestReason reason,
                                       HistoryEventReason expectedCheckouterReason) {
        environmentService.setValue(ENV_ROUTE_REQS_TO_ORDER_SERVICE, "true");

        long orderId = 2827258L;
        long partnerId = isDbs ? 2001L : 666L;

        prepareFf4ShopsGetRemovalPermissionsMock("mocks/ff4shops/order_removal_permissions.json", orderId, partnerId);
        prepareUpdateOrderItemsCheckouterMock("mocks/checkouter/get_order_update_items.json", orderId, partnerId);
        prepareUpdateOrderLinesOrderServiceMock(partnerId, orderId);

        //language=json
        String requestBody = "" +
                "{ " +
                "  \"items\":[" +
                "    {" +
                "      \"id\":4055992, " +
                "      \"count\":5, " +
                "      \"instances\": [" +
                "        {" +
                "          \"cis\": \"CIS-4055992-1\"" +
                "        }," +
                "        {" +
                "          \"cis\": \"CIS-4055992-3\"" +
                "        }," +
                "        {" +
                "          \"cis\": \"CIS-4055992-5\"" +
                "        }" +
                "      ]" +
                "    }" +
                "  ]" + (reason == null ? "" : ", \"reason\":\"" + reason.name() + "\"") +
                "}";

        FunctionalTestHelper.makeRequest(
                urlBasePrefix + "/campaigns/" + (isDbs ? DROPSHIP_BY_SELLER_CAMPAIGN_ID : DROPSHIP_CAMPAIGN_ID) +
                        "/orders/" + orderId + "/items.json",
                HttpMethod.PUT,
                Format.XML,
                requestBody);

        verify(checkouterAPI, never()).editOrder(anyLong(), any(), anyList(), any());

        ArgumentCaptor<ChangeOrderLinesRequest> captor = forClass(ChangeOrderLinesRequest.class);
        verify(papiOrderServiceClient, times(1)).postEditOrder(
                eq(partnerId),
                eq(orderId),
                captor.capture(),
                eq(ActorType.API)
        );

        ChangeOrderLinesRequest capturedRequest = captor.getValue();
        assertNotNull(capturedRequest);
        assertEquals(expectedCheckouterReason.name(), capturedRequest.getReason().name());

        var lines = capturedRequest.getLines();
        assertEquals(2, lines.size());
        var line1 = new OrderLineChange();
        line1.setLineId(4055992L);
        line1.setCount(5);
        line1.setCis(List.of("CIS-4055992-1", "CIS-4055992-3", "CIS-4055992-5"));
        var line2 = new OrderLineChange();
        line2.setLineId(4055993L);
        line2.setCount(0);
        line2.setCis(emptyList());
        assertThat(lines).containsExactlyInAnyOrder(line1, line2);

        environmentService.setValue(ENV_ROUTE_REQS_TO_ORDER_SERVICE, "false");
    }

    @Test
    @DisplayName("Валидация разметки при изменении состава заказа. Заказ нельзя менять из-за способов оплаты. (XML)")
    void testUpdateOrderItemsValidatePermissionsOrderNotAllowedXml() {
        long orderId = 2827258L;
        long partnerId = 666L;

        prepareFf4ShopsGetRemovalPermissionsMock("mocks/ff4shops/order_removal_permissions_order_not_allowed.json",
                orderId
                , partnerId);

        prepareUpdateOrderItemsCheckouterMock("mocks/checkouter/get_order_update_items.json", orderId, partnerId);

        //language=xml
        String requestBody = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
                "<orderItemsModificationRequest>" +
                "    <items>" +
                "        <item id='4055992' count='5'/>" +
                "    </items>" +
                "    <reason>USER_REQUESTED_REMOVE</reason>" +
                "</orderItemsModificationRequest>";

        HttpClientErrorException exception = Assertions.assertThrows(HttpClientErrorException.class,
                () -> FunctionalTestHelper.makeRequest(
                        urlBasePrefix + "/campaigns/" + DROPSHIP_CAMPAIGN_ID + "/orders/" + orderId + "/items.xml",
                        HttpMethod.PUT,
                        Format.XML,
                        requestBody)
        );

        assertEquals(HttpStatus.BAD_REQUEST.value(), exception.getRawStatusCode());

        //language=xml
        String expected = "<response>" +
                "  <status>ERROR</status>" +
                "  <errors>" +
                "    <error code=\"PAYMENT_PROHIBITS_DELETE\" " +
                "           message=\"Cannot remove items for order 2827258\"/>" +
                "  </errors>" +
                "</response>";
        MbiAsserts.assertXmlEquals(expected,
                exception.getResponseBodyAsString(),
                MbiAsserts.IGNORE_ORDER
        );
    }

    @Test
    @DisplayName("Валидация разметки при изменении состава заказа. Заказ нельзя менять из-за способов оплаты. (JSON)")
    void testUpdateOrderItemsValidatePermissionsOrderNotAllowedJson() {
        long orderId = 2827258L;
        long partnerId = 666L;

        prepareFf4ShopsGetRemovalPermissionsMock("mocks/ff4shops/order_removal_permissions_order_not_allowed.json",
                orderId
                , partnerId);

        prepareUpdateOrderItemsCheckouterMock("mocks/checkouter/get_order_update_items.json", orderId, partnerId);

        //language=json
        String requestBody = "{ \"items\":[{\"id\":4055992, \"count\":5}], \"reason\":\"PARTNER_REQUESTED_REMOVE\" }";

        HttpClientErrorException exception = Assertions.assertThrows(HttpClientErrorException.class,
                () -> FunctionalTestHelper.makeRequest(
                        urlBasePrefix + "/campaigns/" + DROPSHIP_CAMPAIGN_ID + "/orders/" + orderId + "/items.json",
                        HttpMethod.PUT,
                        Format.JSON,
                        requestBody)
        );

        assertEquals(HttpStatus.BAD_REQUEST.value(), exception.getRawStatusCode());

        //language=json
        String expected = "" +
                "{" +
                "  \"status\":\"ERROR\", " +
                "  \"errors\":[{" +
                "    \"code\":\"PAYMENT_PROHIBITS_DELETE\", " +
                "    \"message\":\"Cannot remove items for order 2827258\"" +
                "  }]" +
                "}";
        JsonTestUtil.assertEquals(
                expected,
                exception.getResponseBodyAsString()
        );
    }

    @Test
    @DisplayName("Валидация разметки при изменении состава заказа через OS. Заказ нельзя менять из-за способов оплаты. (JSON)")
    void testUpdateOrderItemsValidatePermissionsOrderNotAllowedJsonViaOS() {
        environmentService.setValue(ENV_ROUTE_REQS_TO_ORDER_SERVICE, "true");

        long orderId = 2827258L;
        long partnerId = 666L;

        prepareFf4ShopsGetRemovalPermissionsMock("mocks/ff4shops/order_removal_permissions_order_not_allowed.json",
                orderId
                , partnerId);

        prepareUpdateOrderItemsCheckouterMock("mocks/checkouter/get_order_update_items.json", orderId, partnerId);
        prepareUpdateOrderLinesOrderServiceMock(partnerId, orderId);

        //language=json
        String requestBody = "{ \"items\":[{\"id\":4055992, \"count\":5}], \"reason\":\"PARTNER_REQUESTED_REMOVE\" }";

        HttpClientErrorException exception = Assertions.assertThrows(HttpClientErrorException.class,
                () -> FunctionalTestHelper.makeRequest(
                        urlBasePrefix + "/campaigns/" + DROPSHIP_CAMPAIGN_ID + "/orders/" + orderId + "/items.json",
                        HttpMethod.PUT,
                        Format.JSON,
                        requestBody)
        );

        verifyNoInteractions(papiOrderServiceClient);

        assertEquals(HttpStatus.BAD_REQUEST.value(), exception.getRawStatusCode());

        //language=json
        String expected = "" +
                "{" +
                "  \"status\":\"ERROR\", " +
                "  \"errors\":[{" +
                "    \"code\":\"PAYMENT_PROHIBITS_DELETE\", " +
                "    \"message\":\"Cannot remove items for order 2827258\"" +
                "  }]" +
                "}";
        JsonTestUtil.assertEquals(
                expected,
                exception.getResponseBodyAsString()
        );

        environmentService.setValue(ENV_ROUTE_REQS_TO_ORDER_SERVICE, "false");
    }

    @Test
    @DisplayName("Валидация разметки при изменении состава заказа. Заказ нельзя менять из-за СД. (XML)")
    void testUpdateOrderItemsValidatePermissionsOrderNotAllowedByDeliveryXml() {
        long orderId = 2827258L;
        long partnerId = 666L;

        prepareFf4ShopsGetRemovalPermissionsMock(
                "mocks/ff4shops/order_removal_permissions_order_not_allowed_by_delivery" +
                        ".json", orderId, partnerId);

        prepareUpdateOrderItemsCheckouterMock("mocks/checkouter/get_order_update_items.json", orderId, partnerId);

        //language=xml
        String requestBody = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
                "<orderItemsModificationRequest>" +
                "    <items>" +
                "        <item id='4055992' count='5'/>" +
                "    </items>" +
                "    <reason>USER_REQUESTED_REMOVE</reason>" +
                "</orderItemsModificationRequest>";

        HttpClientErrorException exception = Assertions.assertThrows(HttpClientErrorException.class,
                () -> FunctionalTestHelper.makeRequest(
                        urlBasePrefix + "/campaigns/" + DROPSHIP_CAMPAIGN_ID + "/orders/" + orderId + "/items.xml",
                        HttpMethod.PUT,
                        Format.XML,
                        requestBody)
        );

        assertEquals(HttpStatus.BAD_REQUEST.value(), exception.getRawStatusCode());

        //language=xml
        String expected = "<response>" +
                "  <status>ERROR</status>" +
                "  <errors>" +
                "    <error code=\"NOT_SUPPORTED_BY_DELIVERY_SERVICE\" " +
                "           message=\"Cannot remove items for order 2827258\"/>" +
                "  </errors>" +
                "</response>";
        MbiAsserts.assertXmlEquals(expected,
                exception.getResponseBodyAsString(),
                MbiAsserts.IGNORE_ORDER
        );
    }

    @Test
    @DisplayName("Валидация разметки при изменении состава заказа. Заказ нельзя менять из-за СД. (JSON)")
    void testUpdateOrderItemsValidatePermissionsOrderNotAllowedDeliveryJson() {
        long orderId = 2827258L;
        long partnerId = 666L;

        prepareFf4ShopsGetRemovalPermissionsMock(
                "mocks/ff4shops/order_removal_permissions_order_not_allowed_by_delivery" +
                        ".json", orderId, partnerId);

        prepareUpdateOrderItemsCheckouterMock("mocks/checkouter/get_order_update_items.json", orderId, partnerId);

        //language=json
        String requestBody = "{ \"items\":[{\"id\":4055992, \"count\":5}], \"reason\":\"PARTNER_REQUESTED_REMOVE\" }";

        HttpClientErrorException exception = Assertions.assertThrows(HttpClientErrorException.class,
                () -> FunctionalTestHelper.makeRequest(
                        urlBasePrefix + "/campaigns/" + DROPSHIP_CAMPAIGN_ID + "/orders/" + orderId + "/items.json",
                        HttpMethod.PUT,
                        Format.JSON,
                        requestBody)
        );

        assertEquals(HttpStatus.BAD_REQUEST.value(), exception.getRawStatusCode());

        //language=json
        String expected = "" +
                "{" +
                "  \"status\":\"ERROR\", " +
                "  \"errors\":[{" +
                "    \"code\":\"NOT_SUPPORTED_BY_DELIVERY_SERVICE\", " +
                "    \"message\":\"Cannot remove items for order 2827258\"" +
                "  }]" +
                "}";
        JsonTestUtil.assertEquals(
                expected,
                exception.getResponseBodyAsString()
        );
    }

    @ParameterizedTest
    @DisplayName("Трансляция ошибок от чекаутера при изменении состава заказа (XML)")
    @MethodSource("checkouterErrorArgs")
    void testCheckouterErrorOnUpdateOrderItemsXml(ErrorCodeException checkouterError, ErrorRestModelCode expectedCode) {
        long orderId = 1L;
        long partnerId = 2001L;

        prepareUpdateOrderItemsCheckouterWithErrorMock("mocks/checkouter/get_order_update_items.json", orderId,
                partnerId,
                checkouterError);

        //language=xml
        String requestBody = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
                "<orderItemsModificationRequest>" +
                "    <items>" +
                "        <item id='4055992' count='5'/>" +
                "    </items>" +
                "    <reason>PARTNER_REQUESTED_REMOVE</reason>" +
                "</orderItemsModificationRequest>";

        HttpClientErrorException exception = Assertions.assertThrows(
                HttpClientErrorException.class,
                () -> FunctionalTestHelper.makeRequest(
                        urlBasePrefix + "/campaigns/20001/orders/1/items.xml",
                        HttpMethod.PUT,
                        Format.XML,
                        requestBody)
        );

        //language=xml
        String expected = "<response>" +
                "  <status>ERROR</status>" +
                "  <errors>" +
                "    <error code=\"" + expectedCode + "\" " +
                "           message=\"" + checkouterError.getMessage() + "\"/>" +
                "  </errors>" +
                "</response>";
        MbiAsserts.assertXmlEquals(expected,
                exception.getResponseBodyAsString(),
                MbiAsserts.IGNORE_ORDER
        );
    }

    @ParameterizedTest
    @DisplayName("Трансляция ошибок от чекаутера при изменении состава заказа (JSON)")
    @MethodSource("checkouterErrorArgs")
    void testCheckouterErrorOnUpdateOrderItemsJson(ErrorCodeException checkouterError,
                                                   ErrorRestModelCode expectedCode) {
        long orderId = 1L;
        long partnerId = 2001L;

        prepareUpdateOrderItemsCheckouterWithErrorMock("mocks/checkouter/get_order_update_items.json", orderId,
                partnerId,
                checkouterError);

        //language=json
        String requestBody = "{ \"items\":[{\"id\":4055992, \"count\":5}], \"reason\":\"PARTNER_REQUESTED_REMOVE\" }";

        HttpClientErrorException exception = Assertions.assertThrows(
                HttpClientErrorException.class,
                () -> FunctionalTestHelper.makeRequest(
                        urlBasePrefix + "/campaigns/20001/orders/1/items.json",
                        HttpMethod.PUT,
                        Format.JSON,
                        requestBody)
        );

        //language=json
        String expected = "" +
                "{" +
                "  \"status\":\"ERROR\", " +
                "  \"errors\":[{" +
                "    \"code\":\"" + expectedCode + "\", " +
                "    \"message\":\"" + checkouterError.getMessage() + "\"" +
                "  }]" +
                "}";
        JsonTestUtil.assertEquals(
                expected,
                exception.getResponseBodyAsString()
        );
    }

    @ParameterizedTest
    @DisplayName("Трансляция ошибок от чекаутера при изменении состава заказа через OS (JSON)")
    @MethodSource("checkouterErrorArgs")
    void testCheckouterErrorOnUpdateOrderItemsJsonOS(ErrorCodeException checkouterError,
                                                     ErrorRestModelCode expectedCode) throws JsonProcessingException {
        environmentService.setValue(ENV_ROUTE_REQS_TO_ORDER_SERVICE, "true");

        long orderId = 1L;
        long partnerId = 2001L;

        prepareUpdateOrderItemsCheckouterWithErrorMock("mocks/checkouter/get_order_update_items.json", orderId,
                partnerId,
                checkouterError);
        prepareUpdateOrderLinesWithCheckouterErrorMock(partnerId, orderId, checkouterError);

        //language=json
        String requestBody = "{ \"items\":[{\"id\":4055992, \"count\":5}], \"reason\":\"PARTNER_REQUESTED_REMOVE\" }";

        HttpClientErrorException exception = Assertions.assertThrows(
                HttpClientErrorException.class,
                () -> FunctionalTestHelper.makeRequest(
                        urlBasePrefix + "/campaigns/20001/orders/1/items.json",
                        HttpMethod.PUT,
                        Format.JSON,
                        requestBody)
        );

        //language=json
        String expected = "" +
                "{" +
                "  \"status\":\"ERROR\", " +
                "  \"errors\":[{" +
                "    \"code\":\"" + expectedCode + "\", " +
                "    \"message\":\"" + checkouterError.getMessage() + "\"" +
                "  }]" +
                "}";
        JsonTestUtil.assertEquals(
                expected,
                exception.getResponseBodyAsString()
        );

        environmentService.setValue(ENV_ROUTE_REQS_TO_ORDER_SERVICE, "false");
    }

    @Test
    @DisplayName("Ошибка OrderNotFound при изменении состава заказа (XML)")
    void testOrderNotFoundErrorOnUpdateOrderItemsXml() {
        long orderId = 1L;
        long partnerId = 2001L;
        prepareNotFoundCheckouterMock(orderId, partnerId);

        //language=xml
        String requestBody = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
                "<orderItemsModificationRequest>" +
                "    <items>" +
                "        <item id='4055992' count='5'/>" +
                "    </items>" +
                "    <reason>PARTNER_REQUESTED_REMOVE</reason>" +
                "</orderItemsModificationRequest>";

        HttpClientErrorException exception = Assertions.assertThrows(
                HttpClientErrorException.class,
                () -> FunctionalTestHelper.makeRequest(
                        urlBasePrefix + "/campaigns/20001/orders/1/items.xml",
                        HttpMethod.PUT,
                        Format.XML,
                        requestBody)
        );

        //language=xml
        String expected = "" +
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
                "<response>" +
                "  <status>ERROR</status>" +
                "  <errors>" +
                "    <error code=\"NOT_FOUND\" message=\"Order not found: 1\"/>" +
                "  </errors>" +
                "</response>";
        MatcherAssert.assertThat(
                exception,
                hasErrorCode(HttpStatus.NOT_FOUND)
        );
        MbiAsserts.assertXmlEquals(
                expected,
                exception.getResponseBodyAsString()
        );
    }

    @Test
    @DisplayName("Ошибка OrderNotFound при изменении состава заказа (JSON)")
    void testOrderNotFoundErrorOnUpdateOrderItemsJson() {
        long orderId = 1L;
        long partnerId = 2001L;
        prepareNotFoundCheckouterMock(orderId, partnerId);

        //language=json
        String requestBody = "{ \"items\":[{\"id\":4055992, \"count\":5}], \"reason\":\"PARTNER_REQUESTED_REMOVE\" }";

        HttpClientErrorException exception = Assertions.assertThrows(
                HttpClientErrorException.class,
                () -> FunctionalTestHelper.makeRequest(
                        urlBasePrefix + "/campaigns/20001/orders/1/items.json",
                        HttpMethod.PUT,
                        Format.JSON,
                        requestBody)
        );

        //language=json
        String expected = "" +
                "{" +
                "  \"status\":\"ERROR\", " +
                "  \"errors\":[{" +
                "    \"code\":\"NOT_FOUND\", " +
                "    \"message\":\"Order not found: 1\"" +
                "  }]" +
                "}";
        MatcherAssert.assertThat(
                exception,
                hasErrorCode(HttpStatus.NOT_FOUND)
        );
        JsonTestUtil.assertEquals(
                expected,
                exception.getResponseBodyAsString()
        );
    }

    @Test
    @DisplayName("Ошибка ITEM_DUPLICATE при изменении состава заказа (XML)")
    void testItemDuplicateErrorOnUpdateOrderItemsXml() {
        long orderId = 1L;
        long partnerId = 2001L;
        prepareUpdateOrderItemsCheckouterMock("mocks/checkouter/get_order_update_items.json", orderId, partnerId);

        //language=xml
        String requestBody = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
                "<orderItemsModificationRequest>" +
                "    <items>" +
                "        <item id='4055992' count='5'/>" +
                "        <item id='4055992' count='50'/>" +
                "    </items>" +
                "    <reason>PARTNER_REQUESTED_REMOVE</reason>" +
                "</orderItemsModificationRequest>";

        HttpClientErrorException exception = Assertions.assertThrows(
                HttpClientErrorException.class,
                () -> FunctionalTestHelper.makeRequest(
                        urlBasePrefix + "/campaigns/20001/orders/1/items.xml",
                        HttpMethod.PUT,
                        Format.XML,
                        requestBody)
        );

        //language=xml
        String expected = "" +
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
                "<response>" +
                "  <status>ERROR</status>" +
                "  <errors>" +
                "    <error code=\"ITEM_DUPLICATE\" message=\"Item should be specified only once: 4055992\"/>" +
                "  </errors>" +
                "</response>";
        MatcherAssert.assertThat(
                exception,
                hasErrorCode(HttpStatus.BAD_REQUEST)
        );
        MbiAsserts.assertXmlEquals(
                expected,
                exception.getResponseBodyAsString()
        );
    }

    @Test
    @DisplayName("Ошибка ITEM_DUPLICATE при изменении состава заказа (JSON)")
    void testItemDuplicateErrorOnUpdateOrderItemsJson() {
        long orderId = 1L;
        long partnerId = 2001L;
        prepareUpdateOrderItemsCheckouterMock("mocks/checkouter/get_order_update_items.json", orderId, partnerId);

        //language=json
        String requestBody = "{ \"items\":[{\"id\":4055992, \"count\":5}, {\"id\":4055992, \"count\":50}], " +
                "\"reason\":\"PARTNER_REQUESTED_REMOVE\" }";

        HttpClientErrorException exception = Assertions.assertThrows(
                HttpClientErrorException.class,
                () -> FunctionalTestHelper.makeRequest(
                        urlBasePrefix + "/campaigns/20001/orders/1/items.json",
                        HttpMethod.PUT,
                        Format.JSON,
                        requestBody)
        );

        //language=json
        String expected = "" +
                "{" +
                "  \"status\":\"ERROR\", " +
                "  \"errors\":[{" +
                "    \"code\":\"ITEM_DUPLICATE\", " +
                "    \"message\":\"Item should be specified only once: 4055992\"" +
                "  }]" +
                "}";
        MatcherAssert.assertThat(
                exception,
                hasErrorCode(HttpStatus.BAD_REQUEST)
        );
        JsonTestUtil.assertEquals(
                expected,
                exception.getResponseBodyAsString()
        );
    }

    @Test
    @DisplayName("Ошибка BAD_REQUEST при изменении состава заказа, если не заполнено поле item.id (XML)")
    void testBadRequestOnNullItemIdErrorOnUpdateOrderItemsXml() {
        long orderId = 1L;
        long partnerId = 2001L;
        prepareUpdateOrderItemsCheckouterMock("mocks/checkouter/get_order_update_items.json", orderId, partnerId);

        //language=xml
        String requestBody = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
                "<orderItemsModificationRequest>" +
                "    <items>" +
                "        <item count='5'/>" +
                "    </items>" +
                "    <reason>PARTNER_REQUESTED_REMOVE</reason>" +
                "</orderItemsModificationRequest>";

        HttpClientErrorException exception = Assertions.assertThrows(
                HttpClientErrorException.class,
                () -> FunctionalTestHelper.makeRequest(
                        urlBasePrefix + "/campaigns/20001/orders/1/items.xml",
                        HttpMethod.PUT,
                        Format.XML,
                        requestBody)
        );

        //language=xml
        String expected = "" +
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
                "<response>" +
                "  <status>ERROR</status>" +
                "  <errors>" +
                "    <error code=\"BAD_REQUEST\" message=\"items[0].id must not be null (rejected value: null)\"/>" +
                "  </errors>" +
                "</response>";
        MatcherAssert.assertThat(
                exception,
                hasErrorCode(HttpStatus.BAD_REQUEST)
        );
        MbiAsserts.assertXmlEquals(
                expected,
                exception.getResponseBodyAsString()
        );
    }

    @Test
    @DisplayName("Ошибка BAD_REQUEST при изменении состава заказа, если не заполнено поле item.id (JSON)")
    void testBadRequestOnNullItemIdErrorOnUpdateOrderItemsJson() {
        long orderId = 1L;
        long partnerId = 2001L;
        prepareUpdateOrderItemsCheckouterMock("mocks/checkouter/get_order_update_items.json", orderId, partnerId);

        //language=json
        String requestBody = "{ \"items\":[{\"count\":5}], \"reason\":\"PARTNER_REQUESTED_REMOVE\" }";

        HttpClientErrorException exception = Assertions.assertThrows(
                HttpClientErrorException.class,
                () -> FunctionalTestHelper.makeRequest(
                        urlBasePrefix + "/campaigns/20001/orders/1/items.json",
                        HttpMethod.PUT,
                        Format.JSON,
                        requestBody)
        );

        //language=json
        String expected = "" +
                "{" +
                "  \"status\":\"ERROR\", " +
                "  \"errors\":[{" +
                "    \"code\":\"BAD_REQUEST\", " +
                "    \"message\":\"items[0].id must not be null (rejected value: null)\"" +
                "  }]" +
                "}";
        MatcherAssert.assertThat(
                exception,
                hasErrorCode(HttpStatus.BAD_REQUEST)
        );
        JsonTestUtil.assertEquals(
                expected,
                exception.getResponseBodyAsString()
        );
    }

    @Test
    @DisplayName("Ошибка BAD_REQUEST при изменении состава заказа, если не заполнено поле count (XML)")
    void testBadRequestOnNullCountErrorOnUpdateOrderItemsXml() {
        long orderId = 1L;
        long partnerId = 2001L;
        prepareUpdateOrderItemsCheckouterMock("mocks/checkouter/get_order_update_items.json", orderId, partnerId);

        //language=xml
        String requestBody = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
                "<orderItemsModificationRequest>" +
                "    <items>" +
                "        <item id='4055992'/>" +
                "    </items>" +
                "    <reason>PARTNER_REQUESTED_REMOVE</reason>" +
                "</orderItemsModificationRequest>";

        HttpClientErrorException exception = Assertions.assertThrows(
                HttpClientErrorException.class,
                () -> FunctionalTestHelper.makeRequest(
                        urlBasePrefix + "/campaigns/20001/orders/1/items.xml",
                        HttpMethod.PUT,
                        Format.XML,
                        requestBody)
        );

        //language=xml
        String expected = "" +
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
                "<response>" +
                "  <status>ERROR</status>" +
                "  <errors>" +
                "    <error code=\"BAD_REQUEST\" message=\"items[0].count must not be null (rejected value: null)\"/>" +
                "  </errors>" +
                "</response>";
        MatcherAssert.assertThat(
                exception,
                hasErrorCode(HttpStatus.BAD_REQUEST)
        );
        MbiAsserts.assertXmlEquals(
                expected,
                exception.getResponseBodyAsString()
        );
    }

    @Test
    @DisplayName("Ошибка BAD_REQUEST при изменении состава заказа, если не заполнено поле count (JSON)")
    void testBadRequestOnNullCountErrorOnUpdateOrderItemsJson() {
        long orderId = 1L;
        long partnerId = 2001L;
        prepareUpdateOrderItemsCheckouterMock("mocks/checkouter/get_order_update_items.json", orderId, partnerId);

        //language=json
        String requestBody = "{ \"items\":[{\"id\":4055992}], \"reason\":\"PARTNER_REQUESTED_REMOVE\" }";

        HttpClientErrorException exception = Assertions.assertThrows(
                HttpClientErrorException.class,
                () -> FunctionalTestHelper.makeRequest(
                        urlBasePrefix + "/campaigns/20001/orders/1/items.json",
                        HttpMethod.PUT,
                        Format.JSON,
                        requestBody)
        );

        //language=json
        String expected = "" +
                "{" +
                "  \"status\":\"ERROR\", " +
                "  \"errors\":[{" +
                "    \"code\":\"BAD_REQUEST\", " +
                "    \"message\":\"items[0].count must not be null (rejected value: null)\"" +
                "  }]" +
                "}";
        MatcherAssert.assertThat(
                exception,
                hasErrorCode(HttpStatus.BAD_REQUEST)
        );
        JsonTestUtil.assertEquals(
                expected,
                exception.getResponseBodyAsString()
        );
    }

    @Test
    @DisplayName("Ошибка BAD_REQUEST при изменении состава заказа, если поле count меньше нуля (XML)")
    void testBadRequestOnNegativeCountErrorOnUpdateOrderItemsXml() {
        long orderId = 1L;
        long partnerId = 2001L;
        prepareUpdateOrderItemsCheckouterMock("mocks/checkouter/get_order_update_items.json", orderId, partnerId);

        //language=xml
        String requestBody = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
                "<orderItemsModificationRequest>" +
                "    <items>" +
                "        <item id='4055992' count='-10'/>" +
                "    </items>" +
                "    <reason>PARTNER_REQUESTED_REMOVE</reason>" +
                "</orderItemsModificationRequest>";

        HttpClientErrorException exception = Assertions.assertThrows(
                HttpClientErrorException.class,
                () -> FunctionalTestHelper.makeRequest(
                        urlBasePrefix + "/campaigns/20001/orders/1/items.xml",
                        HttpMethod.PUT,
                        Format.XML,
                        requestBody)
        );

        //language=xml
        String expected = "" +
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
                "<response>" +
                "  <status>ERROR</status>" +
                "  <errors>" +
                "    <error code=\"BAD_REQUEST\" message=\"items[0].count must be positive (rejected value: -10)\"/>" +
                "  </errors>" +
                "</response>";
        MatcherAssert.assertThat(
                exception,
                hasErrorCode(HttpStatus.BAD_REQUEST)
        );
        MbiAsserts.assertXmlEquals(
                expected,
                exception.getResponseBodyAsString()
        );
    }

    @Test
    @DisplayName("Ошибка BAD_REQUEST при изменении состава заказа, если поле count меньше нуля (JSON)")
    void testBadRequestOnNegativeCountErrorOnUpdateOrderItemsJson() {
        long orderId = 1L;
        long partnerId = 2001L;
        prepareUpdateOrderItemsCheckouterMock("mocks/checkouter/get_order_update_items.json", orderId, partnerId);

        //language=json
        String requestBody = "{ \"items\":[{\"id\":4055992, \"count\":-10}], \"reason\":\"PARTNER_REQUESTED_REMOVE\" }";

        HttpClientErrorException exception = Assertions.assertThrows(
                HttpClientErrorException.class,
                () -> FunctionalTestHelper.makeRequest(
                        urlBasePrefix + "/campaigns/20001/orders/1/items.json",
                        HttpMethod.PUT,
                        Format.JSON,
                        requestBody)
        );

        //language=json
        String expected = "" +
                "{" +
                "  \"status\":\"ERROR\", " +
                "  \"errors\":[{" +
                "    \"code\":\"BAD_REQUEST\", " +
                "    \"message\":\"items[0].count must be positive (rejected value: -10)\" " +
                "  }]" +
                "}";
        MatcherAssert.assertThat(
                exception,
                hasErrorCode(HttpStatus.BAD_REQUEST)
        );
        JsonTestUtil.assertEquals(
                expected,
                exception.getResponseBodyAsString()
        );
    }

    static Stream<Arguments> checkouterErrorArgs() {
        return Stream.of(
                Arguments.of(new ErrorCodeException(ITEM_NOT_FOUND_CODE, "msg1", 400),
                        ErrorRestModelCode.ITEM_NOT_FOUND),
                Arguments.of(new ErrorCodeException(INVALID_ITEM_CODE, "msg", 400), ErrorRestModelCode.INVALID_ITEM),
                Arguments.of(new ErrorCodeException(ITEM_DUPLICATE_CODE, "msg", 400),
                        ErrorRestModelCode.ITEM_DUPLICATE),
                Arguments.of(new ErrorCodeException(ITEM_SHIPPED_CODE, "msg", 400), ErrorRestModelCode.ITEM_SHIPPED),
                Arguments.of(new ErrorCodeException(ITEMS_ADDITION_NOT_SUPPORTED_CODE, "msg", 400),
                        ErrorRestModelCode.ITEMS_ADDITION_NOT_SUPPORTED),
                Arguments.of(new ErrorCodeException(CANNOT_REMOVE_LAST_ITEM_CODE, "msg", 400),
                        ErrorRestModelCode.OTHER_REMOVE_ITEM_ERROR),
                Arguments.of(new ErrorCodeException(OrderStatusNotAllowedException.NOT_ALLOWED_CODE, "msg", 400),
                        ErrorRestModelCode.STATUS_NOT_ALLOWED),
                Arguments.of(new ErrorCodeException(OrderItemCisesValidationException.TOO_MANY_CISES_FOR_ITEM_CODE,
                        "msg", 400), ErrorRestModelCode.TOO_MANY_CISES_FOR_ITEM),
                Arguments.of(new ErrorCodeException(OrderItemCisesValidationException.TOO_FEW_CISES_FOR_ITEM_CODE,
                        "msg", 400), ErrorRestModelCode.TOO_FEW_CISES_FOR_ITEM),
                Arguments.of(new ErrorCodeException(OrderItemCisesValidationException.INVALID_CIS_CODE, "msg", 400),
                        ErrorRestModelCode.INVALID_CIS),
                Arguments.of(new ErrorCodeException(CannotRemoveItemException.DELETION_AMOUNT_EXCEEDS_THRESHOLD_CODE,
                        "msg", 400), ErrorRestModelCode.DELETED_ITEMS_EXCEEDS_THRESHOLD),
                Arguments.of(new ErrorCodeException(CannotRemoveItemException.NOT_ALLOWED_PROMO_CODE, "msg", 400),
                        ErrorRestModelCode.PROMO_PROHIBITS_DELETE),
                Arguments.of(new ErrorCodeException(CannotRemoveItemException.NOT_ALLOWED_PAYMENT_TYPE_CODE, "msg",
                        400), ErrorRestModelCode.PAYMENT_PROHIBITS_DELETE),
                Arguments.of(new ErrorCodeException(CannotRemoveItemException.CANCELLATION_REQUESTED_CODE, "msg",
                        400), ErrorRestModelCode.CANCELLATION_REQUESTED)
        );
    }

    private void prepareUpdateOrderItemsCheckouterMock(String getOrderBodyPath, long orderId, long clientId) {
        MockRestServiceServer server = checkouterMockHelper.getServer();

        checkouterMockHelper.mockGetOrder(server, orderId, clientId)
                .andRespond(withSuccess()
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(resourceAsString(getOrderBodyPath)));

        checkouterMockHelper.mockEditOrder(server, orderId, clientId, "BLUE%2CWHITE")
                .andRespond(withSuccess()
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("[]"));
    }

    private void prepareUpdateOrderLinesOrderServiceMock(long partnerId, long orderId) {
        var answer = new ChangeOrderLinesResponse();
        var result = new ChangeOrderLinesResponseDto();
        result.setPartnerId(partnerId);
        result.setOrderId(orderId);
        result.setChangeRequestStatus(ChangeRequestStatusType.APPLIED);
        result.setItems(List.of(new ChangedOrderItemDto()));
        answer.setResult(result);
        when(papiOrderServiceClient.postEditOrder(eq(partnerId), eq(orderId), any(), eq(ActorType.API)))
                .thenReturn(CompletableFuture.completedFuture(answer));
    }

    private void prepareFf4ShopsGetRemovalPermissionsMock(String getPermissionsBodyPath, long orderId, long partnerId) {
        MockRestServiceServer server = MockRestServiceServer.createServer(ff4ShopsRestTemplate);
        server.expect(method(HttpMethod.GET))
                .andExpect(requestTo(
                        String.format("%s/orders/%d/removalPermissions?clientId=%d&shopId=%d&clientRole=SHOP",
                                ff4shopsUrl, orderId, partnerId, partnerId
                        )))
                .andRespond(withSuccess()
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(resourceAsString(getPermissionsBodyPath)));
    }

    private void prepareUpdateOrderItemsCheckouterWithErrorMock(String getOrderBodyPath, long orderId, long clientId,
                                                                ErrorCodeException ex) {
        MockRestServiceServer server = checkouterMockHelper.getServer();

        checkouterMockHelper.mockGetOrder(server, orderId, clientId)
                .andRespond(withSuccess()
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(resourceAsString(getOrderBodyPath)));


        checkouterMockHelper.mockEditOrder(server, orderId, clientId, "BLUE%2CWHITE")
                .andRespond(withStatus(HttpStatus.valueOf(ex.getStatusCode()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(
                                //language=json
                                "{\n" +
                                        "  \"message\": \"" + ex.getMessage() + "\",\n" +
                                        "  \"code\": \"" + ex.getCode() + "\",\n" +
                                        "  \"status\": " + ex.getStatusCode() + "\n" +
                                        "}")
                );
    }

    private void prepareUpdateOrderLinesWithCheckouterErrorMock(long partnerId, long orderId, ErrorCodeException ex)
            throws JsonProcessingException {
        var result = new CommonApiResponse();
        var error = new ApiError();
        error.setCode(ApiError.CodeEnum.ORDER_EDIT_ERROR);
        error.setDetails(Map.of("checkouterErrorCode", ex.getCode()));
        error.setMessage(ex.getMessage());
        result.setErrors(List.of(error));

        when(papiOrderServiceClient.postEditOrder(eq(partnerId), eq(orderId), any(), any()))
                .thenReturn(CompletableFuture.failedFuture(
                                new CompletionException(
                                        new CommonRetrofitHttpExecutionException(
                                                "some message",
                                                400,
                                                null,
                                                new ObjectMapper().writeValueAsString(result)
                                        )
                                )
                        )
                );
    }
}

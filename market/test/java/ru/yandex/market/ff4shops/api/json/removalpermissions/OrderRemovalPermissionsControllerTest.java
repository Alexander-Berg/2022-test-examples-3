package ru.yandex.market.ff4shops.api.json.removalpermissions;

import java.math.BigDecimal;

import javax.annotation.Nonnull;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import ru.yandex.market.checkout.checkouter.client.CheckouterAPI;
import ru.yandex.market.checkout.checkouter.order.OrderNotFoundException;
import ru.yandex.market.checkout.checkouter.order.itemsremoval.OrderItemRemovalPermission;
import ru.yandex.market.checkout.checkouter.order.itemsremoval.OrderItemsRemovalPermissionResponse;
import ru.yandex.market.checkout.checkouter.order.itemsremoval.ReasonForNotAbleRemoveFromOrder;
import ru.yandex.market.checkout.checkouter.order.itemsremoval.ReasonForNotAbleRemoveItem;
import ru.yandex.market.checkout.checkouter.request.OrderRequest;
import ru.yandex.market.checkout.checkouter.request.RequestClientInfo;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.ff4shops.api.json.AbstractJsonControllerFunctionalTest;
import ru.yandex.market.ff4shops.api.model.auth.ClientRole;
import ru.yandex.market.ff4shops.factory.CheckouterFactory;
import ru.yandex.market.ff4shops.util.FF4ShopsUrlBuilder;
import ru.yandex.market.ff4shops.util.FunctionalTestHelper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.refEq;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@DisplayName("Тест контроллера OrderRemovalPermissionsController")
public class OrderRemovalPermissionsControllerTest extends AbstractJsonControllerFunctionalTest {

    private static final long DELIVERY_SERVICE_ID = 107;
    private static final long ORDER_ALLOWED_ID = 771;
    private static final long ORDER_DISABLE_ID = 937;
    private static final long ORDER_NOT_FOUND_ID = 666;
    private static final long CLIENT_ID = 31;
    private static final long SHOP_ID = 73;
    private static final ClientRole CLIENT_ROLE = ClientRole.SHOP_USER;

    @Autowired
    private CheckouterAPI checkouterAPI;

    @AfterEach
    void checkMocks() {
        verifyNoMoreInteractions(checkouterAPI);
    }

    @Test
    @DisplayName("Получить разметку, где разрешено удаление товаров из заказа")
    @DbUnitDataSet(before = "OrderRemovalPermissionsControllerTest.removalAllowed.before.csv")
    void getAllowedRemovalPermissions() {
        mockCheckouterGetOrder(ORDER_ALLOWED_ID);
        when(checkouterAPI.getOrderItemsRemovalPermissions(eq(ORDER_ALLOWED_ID)))
            .thenReturn(
                OrderItemsRemovalPermissionResponse.Builder.initAllowed(ORDER_ALLOWED_ID)
                    .setMaxTotalPercentRemovable(BigDecimal.valueOf(99))
                    .addItemPermission(OrderItemRemovalPermission.initAllowed(1L))
                    .build()
            );

        ResponseEntity<String> response = getRemovalPermissions(ORDER_ALLOWED_ID);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertResponseBody(
            response.getBody(),
            "ru/yandex/market/ff4shops/api/json/removalpermissions/allowed.success.json"
        );

        verifyCheckouterGetOrder(ORDER_ALLOWED_ID);
        verify(checkouterAPI).getOrderItemsRemovalPermissions(eq(ORDER_ALLOWED_ID));
    }

    @Test
    @DisplayName("Получить разметку, где запрещено удаление товаров из заказа")
    @DbUnitDataSet(before = "OrderRemovalPermissionsControllerTest.removalAllowed.before.csv")
    void getDisabledRemovalPermissions() {
        mockCheckouterGetOrder(ORDER_DISABLE_ID);
        when(checkouterAPI.getOrderItemsRemovalPermissions(eq(ORDER_DISABLE_ID)))
            .thenReturn(
                OrderItemsRemovalPermissionResponse.Builder
                    .initDisable(ORDER_DISABLE_ID, ReasonForNotAbleRemoveFromOrder.NOT_ALLOWED_PAYMENT_TYPE)
                    .setMaxTotalPercentRemovable(BigDecimal.valueOf(99))
                    .addItemPermission(
                        OrderItemRemovalPermission.initDisabled(2L, ReasonForNotAbleRemoveItem.NOT_ALLOWED_BY_ORDER)
                    )
                    .build()
            );

        ResponseEntity<String> response = getRemovalPermissions(ORDER_DISABLE_ID);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertResponseBody(
            response.getBody(),
            "ru/yandex/market/ff4shops/api/json/removalpermissions/disabled.success.json"
        );

        verifyCheckouterGetOrder(ORDER_DISABLE_ID);
        verify(checkouterAPI).getOrderItemsRemovalPermissions(eq(ORDER_DISABLE_ID));
    }

    @Test
    @DisplayName("Получение разметки для ненайденного заказа")
    void getRemovalPermissionsForNotFoundOrder() {
        when(checkouterAPI.getOrder(any(RequestClientInfo.class), any(OrderRequest.class)))
            .thenThrow(new OrderNotFoundException(ORDER_NOT_FOUND_ID));

        ResponseEntity<String> response = getRemovalPermissions(ORDER_NOT_FOUND_ID);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertResponseBody(
            response.getBody(),
            "ru/yandex/market/ff4shops/api/json/removalpermissions/notFoundOrder.success.json"
        );

        verify(checkouterAPI).getOrder(any(RequestClientInfo.class), any(OrderRequest.class));
    }

    @Test
    @DisplayName("Изменение товаров заказа выключено у последней мили")
    @DbUnitDataSet(before = "OrderRemovalPermissionsControllerTest.removalDisabled.before.csv")
    void partnerDisableChangeOrderItems() {
        mockCheckouterGetOrder(ORDER_ALLOWED_ID);
        when(checkouterAPI.getOrderItemsRemovalPermissions(eq(ORDER_ALLOWED_ID)))
            .thenReturn(
                OrderItemsRemovalPermissionResponse.Builder.initAllowed(ORDER_ALLOWED_ID)
                    .setMaxTotalPercentRemovable(BigDecimal.valueOf(99))
                    .addItemPermission(OrderItemRemovalPermission.initAllowed(1L))
                    .build()
            );

        ResponseEntity<String> response = getRemovalPermissions(ORDER_ALLOWED_ID);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertResponseBody(
            response.getBody(),
            "ru/yandex/market/ff4shops/api/json/removalpermissions/partnerCannotChangeOrderItems.success.json"
        );

        verifyCheckouterGetOrder(ORDER_ALLOWED_ID);
        verify(checkouterAPI).getOrderItemsRemovalPermissions(eq(ORDER_ALLOWED_ID));
    }

    @Nonnull
    private ResponseEntity<String> getRemovalPermissions(long orderId) {
        String referenceUrl = FF4ShopsUrlBuilder.getRemovalPermissions(
            randomServerPort,
            orderId,
            CLIENT_ID,
            SHOP_ID,
            CLIENT_ROLE
        );
        return FunctionalTestHelper.getForEntity(
            referenceUrl,
            FunctionalTestHelper.jsonHeaders()
        );
    }

    @Nonnull
    private OrderRequest orderRequest(long orderId) {
        return OrderRequest.builder(orderId).build();
    }

    @Nonnull
    private RequestClientInfo requestClientInfo() {
        return new RequestClientInfo(
            ru.yandex.market.checkout.checkouter.client.ClientRole.SHOP_USER,
            CLIENT_ID,
            SHOP_ID
        );
    }

    private void mockCheckouterGetOrder(long orderId) {
        when(checkouterAPI.getOrder(refEq(requestClientInfo()), refEq(orderRequest(orderId))))
            .thenReturn(CheckouterFactory.createOrder(orderId, DELIVERY_SERVICE_ID));
    }

    private void verifyCheckouterGetOrder(long orderId) {
        verify(checkouterAPI).getOrder(refEq(requestClientInfo()), refEq(orderRequest(orderId)));
    }
}

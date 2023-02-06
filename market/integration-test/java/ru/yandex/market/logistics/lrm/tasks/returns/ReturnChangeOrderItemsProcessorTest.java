package ru.yandex.market.logistics.lrm.tasks.returns;

import java.io.IOException;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Set;

import javax.annotation.Nonnull;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.checkouter.client.CheckouterAPI;
import ru.yandex.market.checkout.checkouter.client.ClientRole;
import ru.yandex.market.checkout.checkouter.event.HistoryEventReason;
import ru.yandex.market.checkout.checkouter.order.Color;
import ru.yandex.market.checkout.checkouter.order.OrderEditRequest;
import ru.yandex.market.checkout.checkouter.order.OrderItem;
import ru.yandex.market.checkout.checkouter.order.OrderItemInstance;
import ru.yandex.market.checkout.checkouter.order.OrderItems;
import ru.yandex.market.checkout.checkouter.order.changerequest.itemsremoval.ItemInfo;
import ru.yandex.market.checkout.checkouter.order.changerequest.itemsremoval.MissingItemsNotification;
import ru.yandex.market.checkout.checkouter.request.BasicOrderRequest;
import ru.yandex.market.checkout.checkouter.request.RequestClientInfo;
import ru.yandex.market.logistics.lrm.AbstractIntegrationTest;
import ru.yandex.market.logistics.lrm.queue.payload.ReturnIdPayload;
import ru.yandex.market.logistics.lrm.queue.processor.ReturnChangeOrderItemsProcessor;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.safeRefEq;

@DisplayName("Отправка изменения клиентского заказа по возврату")
class ReturnChangeOrderItemsProcessorTest extends AbstractIntegrationTest {

    private static final long RETURN_ID = 1;
    private static final long CHECKOUTER_ORDER_ID = 654987;

    private final ArgumentCaptor<OrderEditRequest> editOrderCaptor = ArgumentCaptor.forClass(OrderEditRequest.class);

    @Autowired
    private ReturnChangeOrderItemsProcessor processor;

    @Autowired
    private CheckouterAPI checkouterAPI;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setup() {
        clock.setFixed(Instant.parse("2021-09-06T11:12:13.00Z"), ZoneId.systemDefault());
    }

    @AfterEach
    void verifyMocks() {
        verifyNoMoreInteractions(checkouterAPI);
    }

    @Test
    @DisplayName("Возврат из ПВЗ")
    @DatabaseSetup("/database/tasks/returns/change-items/before/pickup_point.xml")
    @ExpectedDatabase(
        value = "/database/tasks/returns/change-items/after/pickup_point.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void pickupPoint() throws Exception {
        mockGetItems(defaultItems());

        execute();

        verify(checkouterAPI).editOrder(
            eq(CHECKOUTER_ORDER_ID),
            eq(ClientRole.PICKUP_SERVICE),
            eq(1234L),
            eq(List.of(Color.BLUE)),
            editOrderCaptor.capture()
        );

        verifyGetItems();
        verifyEditOrderRequest(List.of(
            new ItemInfo(100L, 0, Set.of()),
            new ItemInfo(
                101L,
                1,
                Set.of(itemInstance("third-cis", null))
            )
        ));
    }

    @Test
    @DisplayName("Возврат от курьера")
    @DatabaseSetup("/database/tasks/returns/change-items/before/courier.xml")
    @ExpectedDatabase(
        value = "/database/tasks/returns/change-items/after/courier.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void courier() throws Exception {
        mockGetItems(defaultItems());

        execute();

        verify(checkouterAPI).editOrder(
            eq(CHECKOUTER_ORDER_ID),
            eq(ClientRole.DELIVERY_SERVICE),
            isNull(),
            eq(List.of(Color.BLUE)),
            editOrderCaptor.capture()
        );

        verifyGetItems();
        verifyEditOrderRequest(List.of(
            new ItemInfo(100L, 1, Set.of(itemInstance("first-cis", "first-uit"))),
            new ItemInfo(101L, 1, Set.of(itemInstance("third-cis", null)))
        ));
    }

    @Test
    @DisplayName("Товары без маркировок")
    @DatabaseSetup("/database/tasks/returns/change-items/before/no_instances.xml")
    @ExpectedDatabase(
        value = "/database/tasks/returns/change-items/after/no_instances.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void noInstances() {
        mockGetItems(List.of(
            orderItem(100, 1, 5000, "sku-1"),
            orderItem(101, 2, 5001, "sku-2")
        ));

        execute();

        verify(checkouterAPI).editOrder(
            eq(CHECKOUTER_ORDER_ID),
            eq(ClientRole.PICKUP_SERVICE),
            eq(1234L),
            eq(List.of(Color.BLUE)),
            editOrderCaptor.capture()
        );

        verifyGetItems();
        verifyEditOrderRequest(List.of(
            new ItemInfo(100L, 0, Set.of()),
            new ItemInfo(101L, 1, Set.of(new OrderItemInstance()))
        ));
    }

    @Test
    @DisplayName("Товары не совпадают")
    @DatabaseSetup("/database/tasks/returns/change-items/before/no_instances.xml")
    @ExpectedDatabase(
        value = "/database/tasks/returns/change-items/after/no_events.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void incorrectItems() {
        mockGetItems(List.of(
            orderItem(100, 1, 5000, "sku-1"),
            orderItem(101, 2, 5002, "sku-2")
        ));

        softly.assertThatThrownBy(this::execute)
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("Unexpected items after change for return 1: 2 instead of 1");

        verifyGetItems();
    }

    private void mockGetItems(List<OrderItem> items) {
        when(checkouterAPI.getOrderItems(
            safeRefEq(new RequestClientInfo(ClientRole.SYSTEM, null)),
            safeRefEq(BasicOrderRequest.builder(CHECKOUTER_ORDER_ID).build())
        )).thenReturn(new OrderItems(items));
    }

    @Nonnull
    private List<OrderItem> defaultItems() throws IOException {
        return List.of(
            orderItem(100, 1, "[{\"cis\" : \"first-cis\", \"UIT\" : \"first-uit\"}]"),
            orderItem(101, 2, "[{\"cis\" : \"third-cis\"}, {\"cis\" : \"second-cis\"}]")
        );
    }

    private void verifyGetItems() {
        verify(checkouterAPI).getOrderItems(
            safeRefEq(new RequestClientInfo(ClientRole.SYSTEM, null)),
            safeRefEq(BasicOrderRequest.builder(CHECKOUTER_ORDER_ID).build())
        );
    }

    private void verifyEditOrderRequest(List<ItemInfo> itemsAfter) {
        OrderEditRequest request = new OrderEditRequest();
        request.setMissingItemsNotification(new MissingItemsNotification(
            true,
            itemsAfter,
            HistoryEventReason.USER_REQUESTED_REMOVE,
            true
        ));
        softly.assertThat(editOrderCaptor.getValue())
            .usingRecursiveComparison()
            .isEqualTo(request);
    }

    @Nonnull
    private OrderItem orderItem(long itemId, int count, String instancesJson) throws IOException {
        OrderItem result = new OrderItem();
        result.setId(itemId);
        result.setCount(count);
        result.setInstances((ArrayNode) objectMapper.readTree(instancesJson));
        return result;
    }

    @Nonnull
    private OrderItem orderItem(long itemId, int count, long supplierId, String vendorCode) {
        OrderItem result = new OrderItem();
        result.setId(itemId);
        result.setCount(count);
        result.setSupplierId(supplierId);
        result.setShopSku(vendorCode);
        return result;
    }

    @Nonnull
    private OrderItemInstance itemInstance(String cis, String uit) {
        OrderItemInstance result = new OrderItemInstance();
        result.setCis(cis);
        result.setUit(uit);
        return result;
    }

    private void execute() {
        processor.execute(ReturnIdPayload.builder().returnId(RETURN_ID).build());
    }

}

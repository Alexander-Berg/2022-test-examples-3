package ru.yandex.market.checkout.checkouter.returns;

import java.math.BigDecimal;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

import com.google.common.collect.Iterables;
import io.qameta.allure.Epic;
import io.qameta.allure.Story;
import org.hamcrest.CoreMatchers;
import org.hamcrest.core.IsNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.allure.Stories;
import ru.yandex.market.checkout.checkouter.ReturnableCategoryRule;
import ru.yandex.market.checkout.checkouter.client.ClientInfo;
import ru.yandex.market.checkout.checkouter.client.ClientRole;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryType;
import ru.yandex.market.checkout.checkouter.order.Color;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderItem;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.checkouter.order.SupplierType;
import ru.yandex.market.checkout.checkouter.pay.PaymentMethod;
import ru.yandex.market.checkout.checkouter.request.BasicOrderRequest;
import ru.yandex.market.checkout.checkouter.request.RequestClientInfo;
import ru.yandex.market.checkout.checkouter.tasks.queuedcalls.CheckouterQCType;
import ru.yandex.market.checkout.checkouter.viewmodel.NonReturnableItemViewModel;
import ru.yandex.market.checkout.checkouter.viewmodel.NonReturnableReasonType;
import ru.yandex.market.checkout.helpers.RefundHelper;
import ru.yandex.market.checkout.helpers.ReturnHelper;
import ru.yandex.market.checkout.helpers.YandexMarketDeliveryHelper;
import ru.yandex.market.checkout.helpers.utils.Parameters;
import ru.yandex.market.checkout.util.ClientHelper;
import ru.yandex.market.common.report.model.specs.Specs;
import ru.yandex.market.queuedcalls.QueuedCallService;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.core.IsNot.not;
import static ru.yandex.market.checkout.checkouter.viewmodel.NonReturnableReasonType.NOT_RETURNABLE_CATEGORY;
import static ru.yandex.market.checkout.providers.BlueParametersProvider.defaultBlueOrderParameters;
import static ru.yandex.market.checkout.providers.BlueParametersProvider.postpaidBlueOrderParameters;
import static ru.yandex.market.checkout.test.providers.DeliveryProvider.MOCK_DELIVERY_SERVICE_ID;

public class NonReturnableItemReasonsTest extends AbstractReturnTestBase {

    @Autowired
    private ReturnHelper returnHelper;

    @Autowired
    private QueuedCallService queuedCallService;

    @Autowired
    private RefundHelper refundHelper;
    @Autowired
    private YandexMarketDeliveryHelper yandexMarketDeliveryHelper;

    @BeforeEach
    public void setUp() {
        returnHelper.mockShopInfo();
    }

    @Test
    @Epic(ru.yandex.market.checkout.allure.Epics.RETURN)
    @Story(Stories.GET_RETURNS_ITEMS)
    @DisplayName("Ручка /returns/items фильтрует товары, которые нельзя вернуть потому что они уже возвращены")
    public void checkReturnCreateForOneItem() {
        trustMockConfigurer.mockWholeTrust();
        Parameters params = postpaidBlueOrderParameters(123L);
        params.addOtherItem();
        params.addAnotherItem();
        params.setSupplierTypeForAllItems(SupplierType.FIRST_PARTY);
        Order order = orderCreateHelper.createOrder(params);
        returnHelper.mockSupplierInfo();
        orderStatusHelper.proceedOrderToStatus(order, OrderStatus.DELIVERED);
        queuedCallService.executeQueuedCallBatch(CheckouterQCType.ORDER_CREATE_CASH_PAYMENT);

        trustMockConfigurer.resetAll();
        trustMockConfigurer.mockWholeTrust();
        returnHelper.mockShopInfo();

        ReturnOptionsResponse returnOptions = getReturnOptions(order);
        returnOptions.setItems(
                Collections.singletonList(returnOptions.getItems().stream().findFirst().get())
        );
        ReturnDelivery delivery = convertOptionToDelivery(returnOptions.getDeliveryOptions().get(0));
        addCompensation(returnOptions);
        returnOptions.setDelivery(delivery);
        returnOptions.setComment("Comment");
        addBankDetails(returnOptions);

        Return orderReturn = client.returns().initReturn(order.getId(), ClientRole.SYSTEM, 3331L, returnOptions);
        assertThat(orderReturn.getItems(), hasSize(1));
        assertThat(
                order.getItems().stream().map(OrderItem::getId).collect(Collectors.toList()),
                hasItem(Iterables.getOnlyElement(orderReturn.getItems()).getItemId())
        );
        orderReturn = client.returns().resumeReturn(order.getId(), orderReturn.getId(), ClientRole.REFEREE,
                ClientHelper.REFEREE_UID, orderReturn);

        // Create refunds
        returnService.processReturnPayments(order.getId(), orderReturn.getId(), ClientInfo.SYSTEM);
        refundHelper.proceedAsyncRefunds(order.getId());
        notifyRefundReceipts(orderReturn);
        // Notify refunds
        returnService.processReturnPayments(order.getId(), orderReturn.getId(), ClientInfo.SYSTEM);
        orderReturn = getReturnById(orderReturn.getId());

        assertThat(orderReturn, not(IsNull.nullValue()));
        assertThat(orderReturn.getStatus(), CoreMatchers.equalTo(ReturnStatus.REFUNDED));

        ReturnableItemsResponse returnableItems = client.returns()
                .getReturnableItems(order.getId(), ClientRole.SYSTEM, 1L);

        assertThat(returnableItems.getReturnableItems(), hasSize(2));
        assertThat(returnableItems.getNonReturnableItems(), hasSize(1));
        Set<NonReturnableReasonType> reasonTypes =
                returnableItems.getNonReturnableItems()
                        .stream()
                        .map(NonReturnableItemViewModel::getNonReturnableReason)
                        .collect(Collectors.toSet());
        assertThat(reasonTypes, containsInAnyOrder(NonReturnableReasonType.ALREADY_REFUNDED));
        assertThat(returnableItems.getNonReturnableReasonSet(),
                containsInAnyOrder(NonReturnableReasonType.ALREADY_REFUNDED));
    }

    @Test
    @Epic(ru.yandex.market.checkout.allure.Epics.RETURN)
    @Story(Stories.GET_RETURNS_ITEMS)
    @DisplayName("Ручка /returns/items фильтрует товары, которые нельзя вернуть потому что они уже возвращены")
    public void checkReturnCreateForOneItemWithLessCount() {
        trustMockConfigurer.mockWholeTrust();
        Parameters params = postpaidBlueOrderParameters(123L);
        params.addOtherItem();
        params.addAnotherItem();
        params.setSupplierTypeForAllItems(SupplierType.FIRST_PARTY);
        params.getOrder().getItems().forEach(orderItem -> {
            orderItem.setCount(10);
            orderItem.setQuantity(BigDecimal.TEN);
        });
        Order order = orderCreateHelper.createOrder(params);
        returnHelper.mockSupplierInfo();
        orderStatusHelper.proceedOrderToStatus(order, OrderStatus.DELIVERED);
        queuedCallService.executeQueuedCallBatch(CheckouterQCType.ORDER_CREATE_CASH_PAYMENT);

        trustMockConfigurer.resetAll();
        trustMockConfigurer.mockWholeTrust();
        returnHelper.mockShopInfo();

        ReturnOptionsResponse returnOptions = getReturnOptions(order);
        ReturnItem anyItemToReturn = returnOptions.getItems().stream().findFirst().get();
        anyItemToReturn.setCount(5);
        anyItemToReturn.setQuantity(BigDecimal.valueOf(5));
        returnOptions.setItems(Collections.singletonList(anyItemToReturn));
        ReturnDelivery delivery = convertOptionToDelivery(returnOptions.getDeliveryOptions().get(0));
        addCompensation(returnOptions);
        returnOptions.setDelivery(delivery);
        returnOptions.setComment("Comment");
        addBankDetails(returnOptions);

        Return orderReturn = client.returns().initReturn(order.getId(), ClientRole.SYSTEM, 3331L, returnOptions);
        assertThat(orderReturn.getItems(), hasSize(1));
        assertThat(
                order.getItems().stream().map(OrderItem::getId).collect(Collectors.toList()),
                hasItem(Iterables.getOnlyElement(orderReturn.getItems()).getItemId())
        );
        orderReturn = client.returns().resumeReturn(order.getId(),
                orderReturn.getId(), ClientRole.REFEREE, ClientHelper.REFEREE_UID, orderReturn);

        // Create refunds
        returnService.processReturnPayments(order.getId(), orderReturn.getId(), ClientInfo.SYSTEM);
        refundHelper.proceedAsyncRefunds(order.getId());
        notifyRefundReceipts(orderReturn);
        // Notify refunds
        returnService.processReturnPayments(order.getId(), orderReturn.getId(), ClientInfo.SYSTEM);
        orderReturn = getReturnById(orderReturn.getId());

        assertThat(orderReturn, not(IsNull.nullValue()));
        assertThat(orderReturn.getStatus(), CoreMatchers.equalTo(ReturnStatus.REFUNDED));

        ReturnableItemsResponse returnableItems = client.returns()
                .getReturnableItems(order.getId(), ClientRole.SYSTEM, 1L);

        assertThat(returnableItems.getReturnableItems(), hasSize(3));
        assertThat(returnableItems.getNonReturnableItems(), hasSize(0));
    }

    @Test
    @Epic(ru.yandex.market.checkout.allure.Epics.RETURN)
    @Story(Stories.GET_RETURNS_ITEMS)
    @DisplayName("Ручка /returns/items фильтрует товары, которые нельзя вернуть после заданного в настройках " +
            "количества дней")
    public void testReturnableItemsHandleItemsThatCannotBeReturnedAfterSpecifiedPeriod() throws Exception {
        Parameters parameters = defaultBlueOrderParameters();
        parameters.addOtherItem();
        parameters.addAnotherItem();

        returnHelper.setNonReturnableItemsViaHttp(
                Arrays.asList(
                        new ReturnableCategoryRule(999, 0),
                        new ReturnableCategoryRule(123, 7)
                )
        );

        parameters.getOrder().getItems().stream()
                .findAny()
                .orElseThrow(() -> new RuntimeException("Nema"))
                .setCategoryId(999);
        parameters.getOrder().getItems().stream()
                .filter(item -> item.getCategoryId() != 999)
                .findAny()
                .orElseThrow(() -> new RuntimeException("Nema"))
                .setCategoryId(456);
        Order order = orderCreateHelper.createOrder(parameters);
        orderStatusHelper.proceedOrderToStatus(order, OrderStatus.DELIVERED);

        setFixedTime(getClock().instant().plus(8, ChronoUnit.DAYS));
        ReturnableItemsResponse returnableItems = client.returns()
                .getReturnableItems(order.getId(), ClientRole.USER, order.getBuyer().getUid());
        assertThat(returnableItems.getReturnableItems(), hasSize(1));
        assertThat(returnableItems.getNonReturnableItems(), hasSize(2));
        assertThat(
                returnableItems.getNonReturnableItems(),
                containsInAnyOrder(
                        hasProperty("item", hasProperty("itemId", notNullValue())),
                        hasProperty("item", hasProperty("itemId", notNullValue()))
                )
        );

        Set<NonReturnableReasonType> reasonTypes =
                returnableItems.getNonReturnableItems()
                        .stream()
                        .map(NonReturnableItemViewModel::getNonReturnableReason)
                        .collect(Collectors.toSet());
        assertThat(reasonTypes,
                containsInAnyOrder(NOT_RETURNABLE_CATEGORY,
                        NonReturnableReasonType.RETURNABLE_CATEGORY_WITH_TIME_LIMIT));
        assertThat(returnableItems.getNonReturnableReasonSet(),
                containsInAnyOrder(NOT_RETURNABLE_CATEGORY,
                        NonReturnableReasonType.RETURNABLE_CATEGORY_WITH_TIME_LIMIT));

        returnHelper.setNonReturnableItemsViaHttp(Collections.emptyList());
    }

    @Test
    @Epic(ru.yandex.market.checkout.allure.Epics.RETURN)
    @Story(Stories.GET_RETURNS_ITEMS)
    @DisplayName("Ручка /returns/items фильтрует товары, которые нельзя вернуть после 15 дней")
    public void testReturnableItemsHandleItemsThatCannotBeReturnedAfterMaxAvailablePeriod() throws Exception {
        Parameters parameters = defaultBlueOrderParameters();
        parameters.addOtherItem();
        parameters.addAnotherItem();

        returnHelper.setNonReturnableItemsViaHttp(
                Arrays.asList(
                        new ReturnableCategoryRule(999, 0),
                        new ReturnableCategoryRule(123, 20)
                )
        );

        parameters.getOrder().getItems().stream()
                .findAny()
                .orElseThrow(() -> new RuntimeException("Nema"))
                .setCategoryId(999);
        parameters.getOrder().getItems().stream()
                .filter(item -> item.getCategoryId() != 999)
                .findAny()
                .orElseThrow(() -> new RuntimeException("Nema"))
                .setCategoryId(456);
        Order order = orderCreateHelper.createOrder(parameters);
        orderStatusHelper.proceedOrderToStatus(order, OrderStatus.DELIVERED);

        setFixedTime(getClock().instant().plus(16, ChronoUnit.DAYS));
        ReturnableItemsResponse returnableItems = client.returns()
                .getReturnableItems(order.getId(), ClientRole.USER, order.getBuyer().getUid());
        assertThat(returnableItems.getReturnableItems(), hasSize(0));
        assertThat(returnableItems.getNonReturnableItems(), hasSize(3));
        assertThat(
                returnableItems.getNonReturnableItems(),
                containsInAnyOrder(
                        hasProperty("item", hasProperty("itemId", notNullValue())),
                        hasProperty("item", hasProperty("itemId", notNullValue())),
                        hasProperty("item", hasProperty("itemId", notNullValue()))
                )
        );

        Set<NonReturnableReasonType> reasonTypes =
                returnableItems.getNonReturnableItems()
                        .stream()
                        .map(NonReturnableItemViewModel::getNonReturnableReason)
                        .collect(Collectors.toSet());
        assertThat(reasonTypes,
                containsInAnyOrder(NOT_RETURNABLE_CATEGORY,
                        NonReturnableReasonType.MAX_RETURN_TIME_LIMIT_REACHED));
        assertThat(returnableItems.getNonReturnableReasonSet(),
                containsInAnyOrder(NOT_RETURNABLE_CATEGORY,
                        NonReturnableReasonType.MAX_RETURN_TIME_LIMIT_REACHED));

        returnHelper.setNonReturnableItemsViaHttp(Collections.emptyList());
    }

    @Test
    @DisplayName("Лекарства нельзя вернуть")
    void medicineItemShouldBeForbiddenForReturn() {
        Parameters parameters = yandexMarketDeliveryHelper.newMarDoOrderBuilder()
                .withDeliveryServiceId(MOCK_DELIVERY_SERVICE_ID)
                .withDeliveryType(DeliveryType.PICKUP)
                .withColor(Color.BLUE)
                .buildParameters();
        parameters.setPaymentMethod(PaymentMethod.CASH_ON_DELIVERY);
        OrderItem item = parameters.getItems().stream()
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Должен быть хотя бы один OrderItem"));
        item.setMedicalSpecsInternal(Specs.fromSpecValues(Set.of("medicine")));
        Order order = orderCreateHelper.createOrder(parameters);
        order = orderStatusHelper.proceedOrderToStatus(order, OrderStatus.DELIVERED);
        RequestClientInfo clientInfo = new RequestClientInfo(ClientRole.USER, order.getBuyer().getUid());
        BasicOrderRequest request = BasicOrderRequest.builder(order.getId()).build();

        ReturnableItemsResponse returnableItems = client.returns().getReturnableItems(clientInfo, request);

        assertThat(returnableItems.getNonReturnableReasonSet(), containsInAnyOrder(NOT_RETURNABLE_CATEGORY));
        assertThat(returnableItems.getNonReturnableItems(), hasSize(1));
    }
}

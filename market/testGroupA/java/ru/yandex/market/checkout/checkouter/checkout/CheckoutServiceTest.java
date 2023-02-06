package ru.yandex.market.checkout.checkouter.checkout;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;

import com.google.common.collect.Iterables;
import io.qameta.allure.Epic;
import io.qameta.allure.Story;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.allure.Epics;
import ru.yandex.market.checkout.allure.Stories;
import ru.yandex.market.checkout.application.AbstractArchiveWebTestBase;
import ru.yandex.market.checkout.checkouter.client.ClientInfo;
import ru.yandex.market.checkout.checkouter.event.HistoryEventType;
import ru.yandex.market.checkout.checkouter.event.OrderHistoryEvent;
import ru.yandex.market.checkout.checkouter.event.PagedEvents;
import ru.yandex.market.checkout.checkouter.feature.type.common.BooleanFeatureType;
import ru.yandex.market.checkout.checkouter.order.MoveOrderParams;
import ru.yandex.market.checkout.checkouter.order.MoveOrderResponse;
import ru.yandex.market.checkout.checkouter.order.MoveOrderStatus;
import ru.yandex.market.checkout.checkouter.order.MoveOrdersService;
import ru.yandex.market.checkout.checkouter.order.MultiOrder;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderUidNotMatchedException;
import ru.yandex.market.checkout.checkouter.storage.StorageType;
import ru.yandex.market.checkout.checkouter.tasks.queuedcalls.CheckouterQCType;
import ru.yandex.market.checkout.checkouter.validation.ValidationResult;
import ru.yandex.market.checkout.common.rest.Pager;
import ru.yandex.market.checkout.helpers.OrderCreateHelper;
import ru.yandex.market.checkout.helpers.utils.Parameters;
import ru.yandex.market.checkout.providers.B2bCustomersTestProvider;
import ru.yandex.market.checkout.providers.BlueParametersProvider;
import ru.yandex.market.checkout.test.providers.BuyerProvider;
import ru.yandex.market.checkout.test.providers.OrderProvider;
import ru.yandex.market.checkout.util.b2b.B2bCustomersMockConfigurer;
import ru.yandex.market.queuedcalls.QueuedCallService;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertNull;

public class CheckoutServiceTest extends AbstractArchiveWebTestBase {

    private static final long MUID = 1L << 60 | 1;
    private static final long UID = 123L;
    @Autowired
    protected QueuedCallService qcService;
    @Autowired
    protected OrderCreateHelper orderCreateHelper;
    @Autowired
    private MoveOrdersService moveOrdersService;
    @Autowired
    private B2bCustomersMockConfigurer b2bCustomersMockConfigurer;

    @BeforeEach
    void initProperties() {
        checkouterProperties.setEnableArchivingBulkInsert(false);
        b2bCustomersMockConfigurer.mockIsClientCanOrder(BuyerProvider.UID,
                B2bCustomersTestProvider.BUSINESS_BALANCE_ID, true);
    }

    @AfterEach
    void resetMocks() {
        b2bCustomersMockConfigurer.resetAll();
    }

    @Epic(Epics.CHANGE_ORDER)
    @Story(Stories.MOVE_ORDERS)
    @DisplayName("ORDER.NO_AUTH должен изменяться при прикреплении заказа к пользователю")
    @Test
    public void shouldChangeNoAuthOnMoveOrder() throws Exception {
        Order order = OrderProvider.getBlueOrder();
        order.setNoAuth(true);
        order.getBuyer().setUid(MUID);
        order.getBuyer().setMuid(MUID);

        long orderId = orderCreateService.createOrder(order, ClientInfo.SYSTEM);
        // иначе не будет ивента
        orderUpdateService.reserveOrder(orderId, String.valueOf(orderId), order.getDelivery());

        MoveOrderParams params = new MoveOrderParams(MUID, UID, ClientInfo.SYSTEM);
        List<MoveOrderResponse> result = moveOrdersService.moveOrders(Collections.singletonList(orderId), params);

        Assertions.assertEquals(MoveOrderStatus.SUCCESS, Iterables.getOnlyElement(result).getStatus());

        Order movedOrder = orderService.getOrder(orderId);
        Assertions.assertFalse(movedOrder.isNoAuth());

        PagedEvents events = eventService.getPagedOrderHistoryEvents(orderId, Pager.atPage(1, 100), null, null,
                Collections.emptySet(), false, ClientInfo.SYSTEM, null);
        Optional<OrderHistoryEvent> orderUidUpdated = events.getItems().stream()
                .filter(it -> it.getType() == HistoryEventType.ORDER_UID_UPDATED)
                .findAny();

        Assertions.assertTrue(orderUidUpdated.isPresent());

        Assertions.assertFalse(orderUidUpdated.get().getOrderAfter().isNoAuth(), "NoAuth after should be false");
        Assertions.assertTrue(orderUidUpdated.get().getOrderBefore().isNoAuth(), "NoAuth before should be true");
    }

    @Test
    @DisplayName("Перенос заказа из архивной базы на другой Uid")
    void shouldMoveArchivedOrderTest() {
        // создаем заказ и перемещаем его в архивную БД
        Order order = createArchivedOrder();
        Set<Long> ordersIds = new HashSet<>(Collections.singletonList(order.getId()));
        assertSuccessfulTaskRun(ordersIds);
        moveArchivedOrders();

        MoveOrderParams params = new MoveOrderParams(order.getUid(), UID, ClientInfo.SYSTEM);
        List<MoveOrderResponse> result = moveOrdersService.moveOrders(ordersIds, params);

        // заказ должен поставиться в очередь на разархивацию и перенос
        Assertions.assertEquals(MoveOrderStatus.PROCESSING, Iterables.getOnlyElement(result).getStatus());

        qcService.executeQueuedCallBatch(CheckouterQCType.UNARCHIVE_AND_MOVE_ORDER_TO_UID);

        Order movedOrder = orderService.getOrder(order.getId());
        assertThat(false, equalTo(movedOrder.isArchived()));
        assertThat(UID, equalTo(movedOrder.getUid()));
    }

    @Test
    @DisplayName("Перенос заказа из архивной базы на другой Uid")
    void wrongFromMuid() {
        checkouterFeatureWriter.writeValue(BooleanFeatureType.THROW_ERROR_ON_WRONG_USER_MUID, true);

        // создаем заказ и перемещаем его в архивную БД
        Order order = createArchivedOrder();
        Set<Long> ordersIds = new HashSet<>(Collections.singletonList(order.getId()));
        assertSuccessfulTaskRun(ordersIds);
        moveArchivedOrders();

        MoveOrderParams params = new MoveOrderParams(order.getUid() + 1, UID, ClientInfo.SYSTEM);
        Assertions.assertThrows(OrderUidNotMatchedException.class, () -> moveOrdersService.moveOrders(ordersIds,
                params));

        checkouterFeatureWriter.writeValue(BooleanFeatureType.THROW_ERROR_ON_WRONG_USER_MUID, false);
    }

    @Test
    @DisplayName("Бизнес клиенты должны делать запросы в Лоялти")
    public void shouldSendRequestsToLoyaltyForBusinessClients() {
        Parameters parameters = BlueParametersProvider.defaultBlueOrderParameters();
        parameters.getOrder().getBuyer().setBusinessBalanceId(123L);

        MultiOrder multiOrder = orderCreateHelper.createMultiOrder(parameters);

        assertThat(multiOrder, allOf(
                hasProperty("cashbackOptionsProfiles", nullValue()),
                hasProperty("cashbackBalance", notNullValue()),
                hasProperty("validationErrors", nullValue()),
                hasProperty("cashback", notNullValue())));
    }

    @Test
    @DisplayName("Перенос заказа из архивной базы на другой Uid")
    void fakeSuccessFromMuid() {
        checkouterFeatureWriter.writeValue(BooleanFeatureType.THROW_ERROR_ON_WRONG_USER_MUID, false);

        // создаем заказ и перемещаем его в архивную БД
        Order order = createArchivedOrder();
        Set<Long> ordersIds = new HashSet<>(Collections.singletonList(order.getId()));
        assertSuccessfulTaskRun(ordersIds);
        moveArchivedOrders();

        MoveOrderParams params = new MoveOrderParams(order.getUid() + 1, UID, ClientInfo.SYSTEM);
        Assertions.assertDoesNotThrow(() -> moveOrdersService.moveOrders(ordersIds, params));

        // этим вызовом сбрасываем из очереди
        qcService.executeQueuedCallBatch(CheckouterQCType.UNARCHIVE_AND_MOVE_ORDER_TO_UID);

        assertThat(qcService.findQueuedCalls(CheckouterQCType.UNARCHIVE_AND_MOVE_ORDER_TO_UID, order.getId()),
                hasSize(0));
    }

    @Test
    @DisplayName("Бабуся не разрешила бизнес клиенту оформить заказ")
    public void shouldContainsValidationErrorIfBusinessUserCannotOrder() {
        // Если Бабуся сказала, что юзер не может оформлять заказ, мультикорзина бизнес клиента будет
        // содержать ошибку валидации BUSINESS_CLIENT_VERIFICATION_FAILED
        b2bCustomersMockConfigurer.mockIsClientCanOrder(BuyerProvider.UID,
                B2bCustomersTestProvider.BUSINESS_BALANCE_ID, false);

        Parameters parameters = BlueParametersProvider.defaultBlueOrderParameters();
        parameters.getOrder().getBuyer().setBusinessBalanceId(B2bCustomersTestProvider.BUSINESS_BALANCE_ID);
        parameters.configuration().checkout().response().setCheckOrderCreateErrors(false);

        MultiOrder multiOrder = orderCreateHelper.createMultiOrder(parameters);

        var validationCodes = multiOrder.getValidationErrors()
                .stream()
                .map(ValidationResult::getCode)
                .collect(Collectors.toList());

        assertThat(validationCodes, contains("BUSINESS_CLIENT_VERIFICATION_FAILED"));
    }

    @Test
    @DisplayName("Небизнесовые клиенты в Бабусю не ходят")
    public void nonBusinessClientsShouldNotUseBabusa() {
        // для небизнесовых клиентов вызов в Бабусю даже не пойдет,
        // поэтому мокать ее нам не надо
        Parameters parameters = BlueParametersProvider.defaultBlueOrderParameters();
        MultiOrder multiOrder = orderCreateHelper.createMultiOrder(parameters);
        assertNull(multiOrder.getValidationErrors());
    }

    private void assertSuccessfulTaskRun(@Nonnull Collection<Long> ids) {
        moveArchivedOrders();
        ids.forEach(id -> checkOrderRecordsExistence(StorageType.BASIC, id, false));
    }
}

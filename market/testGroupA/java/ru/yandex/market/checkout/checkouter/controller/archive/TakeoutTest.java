package ru.yandex.market.checkout.checkouter.controller.archive;

import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import javax.servlet.http.HttpServletResponse;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.application.AbstractArchiveWebTestBase;
import ru.yandex.market.checkout.checkouter.order.Color;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.checkouter.order.archive.ArchiveStorageFutureFactory;
import ru.yandex.market.checkout.checkouter.order.archive.ThrowingCallable;
import ru.yandex.market.checkout.checkouter.order.archive.requests.OrderMovingDirection;
import ru.yandex.market.checkout.checkouter.pay.PaymentService;
import ru.yandex.market.checkout.common.rest.ErrorCodeException;
import ru.yandex.market.checkout.helpers.utils.Parameters;
import ru.yandex.market.checkout.providers.BlueParametersProvider;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

public class TakeoutTest extends AbstractArchiveWebTestBase {

    private static final String CARD_NUM_IN_MAIN_STORAGE = "first";
    private static final String ARCHIVE_ORDER_CARD_NUM_IN_MAIN_STORAGE = "second";
    private static final String CARD_NUM_IN_ARCHIVE_STORAGE = "third";

    @Autowired
    private PaymentService paymentService;
    @Autowired
    private ArchiveStorageFutureFactory archiveStorageFutureFactory;

    @AfterEach
    void cleanContext() {
        paymentService.setArchiveStorageFutureFactory(archiveStorageFutureFactory);
    }


    @DisplayName("Два заказа в основной базе(архивный и не архивный) и заказ в архивной базе " +
            "оплачены разными банковскими картами")
    @Test
    public void testGetAllUserCardsFromAllStorages() {
        Order orderInMainStorage = createThreeOrders();

        Set<String> blueCards = client.getAllCardsByUid(orderInMainStorage.getBuyer().getUid(), Color.BLUE);
        assertThat(blueCards, hasSize(3));
    }

    @DisplayName("Одна из баз не доступна")
    @Test
    public void testDBUnavailable() throws ExecutionException, InterruptedException {
        ArchiveStorageFutureFactory archiveStorageFutureFactoryMock = Mockito.spy(archiveStorageFutureFactory);
        paymentService.setArchiveStorageFutureFactory(archiveStorageFutureFactoryMock);

        Order orderInMainStorage = createThreeOrders();

        CompletableFuture<Object> future = Mockito.mock(CompletableFuture.class);
        CompletableFuture<Object> throwingFuture = Mockito.mock(CompletableFuture.class);
        List<CompletableFuture<Object>> futures = List.of(future, throwingFuture);

        when(future.get()).thenReturn(Set.of());
        when(throwingFuture.get()).thenThrow(ExecutionException.class);

        when(archiveStorageFutureFactoryMock.buildFutures((ThrowingCallable<Object>) Mockito.any()))
                .thenReturn(futures);

        ErrorCodeException exception = assertThrows(ErrorCodeException.class, () -> client.getAllCardsByUid(
                orderInMainStorage.getBuyer().getUid(), Color.BLUE));
        assertEquals(HttpServletResponse.SC_SERVICE_UNAVAILABLE, exception.getStatusCode());
    }

    private Order createOrderAndSetCardNum(String cardNum) {
        Parameters orderParameters = BlueParametersProvider.defaultBlueOrderParameters();
        Order order = orderCreateHelper.createOrder(orderParameters);
        order = orderStatusHelper.proceedOrderToStatus(order, OrderStatus.DELIVERED);
        long orderId = order.getId();
        transactionTemplate.execute(ts -> masterJdbcTemplate.update(
                "update payment set card_number = ? where order_id = ?", cardNum, orderId));
        return order;
    }

    private Order createThreeOrders() {
        Order orderInArchiveStorage = createOrderAndSetCardNum(CARD_NUM_IN_ARCHIVE_STORAGE);
        archiveOrder(orderInArchiveStorage);
        moveArchivingData(OrderMovingDirection.BASIC_TO_ARCHIVE);

        Order orderInMainStorage = createOrderAndSetCardNum(CARD_NUM_IN_MAIN_STORAGE);

        Order archiveOrderInMainStorage = createOrderAndSetCardNum(ARCHIVE_ORDER_CARD_NUM_IN_MAIN_STORAGE);
        archiveOrder(archiveOrderInMainStorage);

        return orderInMainStorage;
    }
}

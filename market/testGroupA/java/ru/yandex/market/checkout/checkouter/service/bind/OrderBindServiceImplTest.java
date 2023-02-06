package ru.yandex.market.checkout.checkouter.service.bind;

import java.time.Clock;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import ru.yandex.common.util.collections.Either;
import ru.yandex.common.util.date.DateUtil;
import ru.yandex.market.checkout.checkouter.auth.AuthService;
import ru.yandex.market.checkout.checkouter.client.ClientInfo;
import ru.yandex.market.checkout.checkouter.order.Buyer;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderSearchRequest;
import ru.yandex.market.checkout.checkouter.order.OrderService;
import ru.yandex.market.checkout.checkouter.order.PagedOrders;
import ru.yandex.market.checkout.common.rest.Pager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
public class OrderBindServiceImplTest {

    private static final String BIND_KEY = "bindKey";
    private static final long UID = 1L;
    private static final long ANOTHER_UID = 2L;
    private static final long MUID = 111111111111111111L;
    private static final String ORDER_PHONE = "+74952234562";
    private static final Collection<String> USER_PHONES = Collections.singleton(ORDER_PHONE);

    @Mock
    private OrderService orderService;
    @Mock
    private AuthService authService;
    @InjectMocks
    private OrderBindServiceImpl orderBindService;

    @BeforeEach
    public void setUp() {
        orderBindService.setClock(Clock.systemDefaultZone());
    }

    @Test
    public void shouldDenyOnNotFound() {
        Mockito.when(orderService.getOrders(Mockito.any(OrderSearchRequest.class), Mockito.any(ClientInfo.class)))
                .thenReturn(new PagedOrders(Collections.emptyList(), Pager.fromTo(0, 0)));

        Either<Order, BindFailReason> orderByBindKey = orderBindService.findOrderByBindKey(BIND_KEY, MUID,
                USER_PHONES, UID, null);
        assertFalse(orderByBindKey.isLeftNotRight());
        assertEquals(BindFailReason.ORDER_NOT_FOUND, orderByBindKey.asRight());
    }

    @Test
    public void shouldDenyOnBindExpired() {
        Order o = new Order();
        o.setCreationDate(DateUtil.addDay(new Date(), -91));

        Mockito.when(orderService.getOrders(Mockito.any(OrderSearchRequest.class), Mockito.any(ClientInfo.class)))
                .thenReturn(new PagedOrders(Collections.singletonList(o), Pager.fromTo(0, 0)));

        Either<Order, BindFailReason> orderByBindKey = orderBindService.findOrderByBindKey(BIND_KEY, MUID,
                USER_PHONES, UID, null);
        assertFalse(orderByBindKey.isLeftNotRight());
        assertEquals(BindFailReason.ORDER_BIND_EXPIRED, orderByBindKey.asRight());
    }

    @Test
    public void shouldDenyOnAlreadyBoundWithFalseNoAuth() {
        Order o = new Order();
        o.setUid(UID);
        o.setCreationDate(new Date());

        Mockito.when(orderService.getOrders(Mockito.any(OrderSearchRequest.class), Mockito.any(ClientInfo.class)))
                .thenReturn(new PagedOrders(Collections.singletonList(o), Pager.fromTo(0, 0)));

        Either<Order, BindFailReason> orderByBindKey = orderBindService.findOrderByBindKey(BIND_KEY, MUID,
                USER_PHONES, UID, false);
        assertFalse(orderByBindKey.isLeftNotRight());
        assertEquals(BindFailReason.ORDER_ALREADY_BOUND, orderByBindKey.asRight());
    }

    @Test
    public void shouldDenyOnAlreadyBound() {
        Order o = new Order();
        o.setUid(UID);
        o.setCreationDate(new Date());

        Mockito.when(authService.isNoAuth(1L))
                .thenReturn(false);

        Mockito.when(orderService.getOrders(Mockito.any(OrderSearchRequest.class), Mockito.any(ClientInfo.class)))
                .thenReturn(new PagedOrders(Collections.singletonList(o), Pager.fromTo(0, 0)));

        Either<Order, BindFailReason> orderByBindKey = orderBindService.findOrderByBindKey(BIND_KEY, MUID,
                USER_PHONES, UID, null);
        assertFalse(orderByBindKey.isLeftNotRight());
        assertEquals(BindFailReason.ORDER_ALREADY_BOUND, orderByBindKey.asRight());
    }

    @Test
    public void shouldReplyNotFoundOnAlreadyBoundToAnotherUser() {
        Order o = new Order();
        o.setUid(ANOTHER_UID);
        o.setCreationDate(new Date());

        Mockito.when(orderService.getOrders(Mockito.any(OrderSearchRequest.class), Mockito.any(ClientInfo.class)))
                .thenReturn(new PagedOrders(Collections.singletonList(o), Pager.fromTo(0, 0)));

        Either<Order, BindFailReason> orderByBindKey = orderBindService.findOrderByBindKey(BIND_KEY, MUID,
                USER_PHONES, UID, null);
        assertFalse(orderByBindKey.isLeftNotRight());
        assertEquals(BindFailReason.ORDER_NOT_FOUND, orderByBindKey.asRight());
    }

    @Test
    public void shouldReplyNotFoundOnAlreadyBoundToAnotherUserIfFalseNoAuth() {
        Order o = new Order();
        o.setUid(ANOTHER_UID);
        o.setCreationDate(new Date());

        Mockito.when(orderService.getOrders(Mockito.any(OrderSearchRequest.class), Mockito.any(ClientInfo.class)))
                .thenReturn(new PagedOrders(Collections.singletonList(o), Pager.fromTo(0, 0)));

        Either<Order, BindFailReason> orderByBindKey = orderBindService.findOrderByBindKey(BIND_KEY, MUID,
                USER_PHONES, UID, false);
        assertFalse(orderByBindKey.isLeftNotRight());
        assertEquals(BindFailReason.ORDER_NOT_FOUND, orderByBindKey.asRight());
    }

    @Test
    public void shouldReturnBadCredentialsIfNeitherMuidNorPhoneIsOk() {
        Buyer buyer = new Buyer();
        buyer.setPhone("+78432234562");

        Order o = new Order();
        o.setUid(UID);
        o.setCreationDate(new Date());
        o.setBuyer(buyer);

        Mockito.when(authService.isNoAuth(1L))
                .thenReturn(true);

        Mockito.when(orderService.getOrders(Mockito.any(OrderSearchRequest.class), Mockito.any()))
                .thenReturn(new PagedOrders(Collections.singletonList(o), Pager.fromTo(0, 1)));

        Either<Order, BindFailReason> orderByBindKey = orderBindService.findOrderByBindKey(BIND_KEY, MUID,
                USER_PHONES, UID, null);
        assertFalse(orderByBindKey.isLeftNotRight());
        assertEquals(BindFailReason.NOT_ENOUGH_CREDENTIALS, orderByBindKey.asRight());
    }

    @Test
    public void shouldReturnIfOkRequestedDayBeforeExpire() {
        Buyer buyer = new Buyer();
        buyer.setPhone(ORDER_PHONE);

        Order o = new Order();
        o.setUid(MUID);
        o.setCreationDate(DateUtil.addDay(new Date(), -89));
        o.setBuyer(buyer);

        Mockito.when(authService.isNoAuth(MUID))
                .thenReturn(true);

        Mockito.when(orderService.getOrders(Mockito.any(OrderSearchRequest.class), Mockito.any()))
                .thenReturn(new PagedOrders(Collections.singletonList(o), Pager.fromTo(0, 1)));

        Either<Order, BindFailReason> orderByBindKey = orderBindService.findOrderByBindKey(BIND_KEY, MUID,
                USER_PHONES, UID, null);
        assertTrue(orderByBindKey.isLeftNotRight());
        assertEquals(o, orderByBindKey.asLeft());
    }

    @Test
    public void shouldReturnIfOk() {
        Buyer buyer = new Buyer();
        buyer.setPhone(ORDER_PHONE);

        Order o = new Order();
        o.setUid(MUID);
        o.setCreationDate(new Date());
        o.setBuyer(buyer);

        Mockito.when(authService.isNoAuth(MUID))
                .thenReturn(true);

        Mockito.when(orderService.getOrders(Mockito.any(OrderSearchRequest.class), Mockito.any()))
                .thenReturn(new PagedOrders(Collections.singletonList(o), Pager.fromTo(0, 1)));

        Either<Order, BindFailReason> orderByBindKey = orderBindService.findOrderByBindKey(BIND_KEY, MUID,
                USER_PHONES, UID, null);
        assertTrue(orderByBindKey.isLeftNotRight());
        assertEquals(o, orderByBindKey.asLeft());
    }

    @Test
    public void shouldReturnIfOkIfUidIsNull() {
        Buyer buyer = new Buyer();
        buyer.setPhone(ORDER_PHONE);

        Order o = new Order();
        o.setUid(MUID);
        o.setCreationDate(new Date());
        o.setBuyer(buyer);

        Mockito.when(orderService.getOrders(Mockito.any(OrderSearchRequest.class), Mockito.any()))
                .thenReturn(new PagedOrders(Collections.singletonList(o), Pager.fromTo(0, 1)));

        Either<Order, BindFailReason> orderByBindKey = orderBindService.findOrderByBindKey(BIND_KEY, MUID,
                USER_PHONES, null, true);
        assertTrue(orderByBindKey.isLeftNotRight());
        assertEquals(o, orderByBindKey.asLeft());
    }

    @Test
    public void shouldReturnOkIfPhoneIsOkEvenIfMuidIsNullAndUidIsNull() {
        Buyer buyer = new Buyer();
        buyer.setPhone(ORDER_PHONE);

        Order o = new Order();
        o.setUid(MUID);
        o.setCreationDate(new Date());
        o.setBuyer(buyer);

        Mockito.when(orderService.getOrders(Mockito.any(OrderSearchRequest.class), Mockito.any()))
                .thenReturn(new PagedOrders(Collections.singletonList(o), Pager.fromTo(0, 1)));

        Either<Order, BindFailReason> orderByBindKey = orderBindService.findOrderByBindKey(BIND_KEY, null,
                USER_PHONES, null, true);
        assertTrue(orderByBindKey.isLeftNotRight());
        assertEquals(o, orderByBindKey.asLeft());
    }

    @Test
    public void shouldReturnOkIfPhoneIsOkEvenIfMuidIsNull() {
        Buyer buyer = new Buyer();
        buyer.setPhone(ORDER_PHONE);

        Order o = new Order();
        o.setUid(MUID);
        o.setCreationDate(new Date());
        o.setBuyer(buyer);

        Mockito.when(authService.isNoAuth(MUID))
                .thenReturn(true);

        Mockito.when(orderService.getOrders(Mockito.any(OrderSearchRequest.class), Mockito.any()))
                .thenReturn(new PagedOrders(Collections.singletonList(o), Pager.fromTo(0, 1)));

        Either<Order, BindFailReason> orderByBindKey = orderBindService.findOrderByBindKey(BIND_KEY, null,
                USER_PHONES, UID, null);
        assertTrue(orderByBindKey.isLeftNotRight());
        assertEquals(o, orderByBindKey.asLeft());
    }
}

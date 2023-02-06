package ru.yandex.market.crm.operatorwindow.domain;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;

import ru.yandex.market.checkout.checkouter.order.Buyer;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.crm.operatorwindow.domain.order.OrderEventsIgnoreRules;
import ru.yandex.market.jmf.configuration.ConfigurationService;
import ru.yandex.market.sdk.userinfo.service.NoSideEffectUserService;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

public class IgnoreOrderEventTest {

    private static final String DO_NOT_PARTICIPATE_IN_SHOOTING = "doNotParticipateInShooting";
    private static final ConfigurationService configurationService = mock(ConfigurationService.class);
    private static final NoSideEffectUserService noSideEffectUserService = mock(NoSideEffectUserService.class);

    private final OrderEventsIgnoreRules rules = createOrderEventIgnoredRules();

    @BeforeEach
    void setUp() {
        reset(noSideEffectUserService);
        when(configurationService.getValue(DO_NOT_PARTICIPATE_IN_SHOOTING)).thenReturn(true);
    }

    @NotNull
    private OrderEventsIgnoreRules createOrderEventIgnoredRules() {
        return new OrderEventsIgnoreRules(
                configurationService,
                noSideEffectUserService);
    }

    @Test
    public void orderEventShouldBeProcessed() {
        Order order = createOrderThatShouldBeProcessed();

        assertOrderEventShouldBeProcessed(order);
    }

    @Test
    public void orderHasPlacingStatus__waitOrderEventShouldBeIgnored() {
        Order order = createOrderThatShouldBeProcessed();
        order.setStatus(OrderStatus.PLACING);

        assertOrderEventShouldBeIgnored(order);
    }

    @Test
    public void orderHasReservedStatus__waitOrderEventShouldBeIgnored() {
        Order order = createOrderThatShouldBeProcessed();
        order.setStatus(OrderStatus.RESERVED);

        assertOrderEventShouldBeIgnored(order);
    }

    @Test
    public void orderBuyerHasIgnoredUid__waitOrderEventShouldBeIgnored() {
        Order order = createOrderThatShouldBeProcessed();
        when(noSideEffectUserService.isNoSideEffectUid(ArgumentMatchers.anyLong())).thenReturn(true);
        assertOrderEventShouldBeIgnored(order);
    }

    @Test
    public void orderBuyerHasIgnoredEmail__waitOrderEventShouldBeIgnored() {
        Order order = createOrderThatShouldBeProcessed();
        when(noSideEffectUserService.isNoSideEffectEmail(ArgumentMatchers.anyString())).thenReturn(true);
        assertOrderEventShouldBeIgnored(order);
    }

    @Test
    public void orderHasFakeFlak__waitOrderEventShouldBeIgnored() {
        Order order = createOrderThatShouldBeProcessed();
        order.setFake(true);

        assertOrderEventShouldBeIgnored(order);
    }

    @Test
    public void doNotIgnoreFireProdOrdersAndOrderHasFireProdUid__waitOrderEventShouldNotBeIgnored() {
        when(configurationService.getValue(DO_NOT_PARTICIPATE_IN_SHOOTING)).thenReturn(false);
        when(noSideEffectUserService.isNoSideEffectUid(ArgumentMatchers.anyLong())).thenReturn(true);
        Order order = createOrderThatShouldBeProcessed();

        Assertions.assertFalse(rules.shouldOrderEventBeIgnored(order));
    }

    private Order createOrderThatShouldBeProcessed() {
        Order order = new Order();
        order.setStatus(OrderStatus.PROCESSING);
        order.setFake(false);
        final Buyer buyer = new Buyer();
        buyer.setUid(123L);
        buyer.setEmail("buyer@example.com");
        order.setBuyer(buyer);
        return order;
    }

    private void assertOrderEventShouldBeIgnored(Order order) {
        Assertions.assertTrue(rules.shouldOrderEventBeIgnored(order));
    }

    private void assertOrderEventShouldBeProcessed(Order order) {
        Assertions.assertFalse(rules.shouldOrderEventBeIgnored(order));
    }
}

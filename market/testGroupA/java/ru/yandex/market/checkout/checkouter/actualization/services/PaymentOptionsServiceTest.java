package ru.yandex.market.checkout.checkouter.actualization.services;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import ru.yandex.market.checkout.checkouter.actualization.model.PushApiCartResponse;
import ru.yandex.market.checkout.checkouter.actualization.utils.DeliveryProvider;
import ru.yandex.market.checkout.checkouter.delivery.Delivery;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryPartnerType;
import ru.yandex.market.checkout.checkouter.order.Buyer;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.pay.PaymentMethod;
import ru.yandex.market.checkout.checkouter.pay.PaymentType;
import ru.yandex.market.checkout.checkouter.util.CheckouterProperties;
import ru.yandex.market.sdk.userinfo.service.NoSideEffectUserService;

import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

@SuppressWarnings("unchecked")
@ExtendWith(MockitoExtension.class)
public class PaymentOptionsServiceTest {

    private static final Set<PaymentMethod> FORCED_PAYMENT_OPTIONS = Set.of(PaymentMethod.CASH_ON_DELIVERY);

    @Mock
    private DeliveryProvider deliveryProvider;
    @Mock
    private CheckouterProperties checkouterProperties;
    @Mock
    private NoSideEffectUserService noSideEffectUserService;

    @InjectMocks
    private PaymentOptionsService service;

    @Test
    public void syncPaymentOptions_ifB2bClientThenForcedPaymentOptionsAreNotForced() {
        // Given
        Buyer buyer = b2bBuyer();
        buyer.setUid(3333L);
        Order userOrder = Mockito.mock(Order.class);
        PushApiCartResponse shopOrder = Mockito.mock(PushApiCartResponse.class);

        setUpToForceAvailablePaymentOptions(buyer, userOrder, shopOrder);

        // When
        service.syncPaymentOptions(userOrder, shopOrder, false, Collections.emptyList(), false);

        // Then
        ArgumentCaptor<Set> availablePaymentMethods = ArgumentCaptor.forClass(Set.class);
        Mockito.verify(userOrder).setPaymentOptions(availablePaymentMethods.capture());
        assertFalse(availablePaymentMethods.getValue().containsAll(FORCED_PAYMENT_OPTIONS));
    }

    private void setUpToForceAvailablePaymentOptions(Buyer buyer, Order userOrder,
                                                     PushApiCartResponse shopOrder) {
        Delivery delivery = Mockito.mock(Delivery.class);
        doReturn(delivery).when(userOrder).getDelivery();
        doReturn(DeliveryPartnerType.SHOP).when(delivery).getDeliveryPartnerType();

        doReturn(false).when(checkouterProperties).getEnableSbpPayment();
        doReturn(CheckouterProperties.ForcePostpaid.FORCE).when(checkouterProperties).getForcePostpaid();

        doReturn(buyer).when(userOrder).getBuyer();

        doReturn(List.of(PaymentMethod.YANDEX)).when(shopOrder).getPaymentMethods();
    }

    @NotNull
    private Buyer b2bBuyer() {
        Buyer buyer = new Buyer();
        buyer.setBusinessBalanceId(1L);
        return buyer;
    }

    @Test
    public void syncPaymentOptions_ifNotB2bClientThenPaymentOptionsAreForced() {
        // Given
        Buyer buyer = new Buyer();
        buyer.setUid(555L);
        Order userOrder = Mockito.mock(Order.class);
        PushApiCartResponse shopOrder = Mockito.mock(PushApiCartResponse.class);

        setUpToForceAvailablePaymentOptions(buyer, userOrder, shopOrder);

        // When
        service.syncPaymentOptions(userOrder, shopOrder, false, Collections.emptyList(), false);

        // Then
        ArgumentCaptor<Set> availablePaymentMethods = ArgumentCaptor.forClass(Set.class);
        Mockito.verify(userOrder).setPaymentOptions(availablePaymentMethods.capture());
        assertTrue(availablePaymentMethods.getValue().containsAll(FORCED_PAYMENT_OPTIONS));
    }

    @Test
    public void enablePostpaidMethods() {
        // Given
        Order order = new Order();
        order.setPaymentOptions(Set.of(PaymentMethod.YANDEX));

        // When
        service.enablePostpaidMethods(order);

        // Then
        Set<PaymentType> paymentTypes =
                order.getPaymentOptions().stream().map(PaymentMethod::getPaymentType).collect(Collectors.toSet());
        assertThat(paymentTypes, hasItem(PaymentType.POSTPAID));
    }

    @Test
    public void syncPaymentOptions_addPostpaidPaymentMethodsForShootOrders() {
        // Given
        Order userOrder = Mockito.mock(Order.class);
        Buyer buyer = b2bBuyer();
        buyer.setUid(3333L);
        doReturn(buyer).when(userOrder).getBuyer();
        PushApiCartResponse shopOrder = Mockito.mock(PushApiCartResponse.class);
        when(shopOrder.getPaymentMethods()).thenReturn(List.of(PaymentMethod.YANDEX));
        when(noSideEffectUserService.isNoSideEffectUid(anyLong())).thenReturn(true);

        // When
        service.syncPaymentOptions(userOrder, shopOrder, false, Collections.emptyList(), false);

        // Then
        ArgumentCaptor<Set<PaymentMethod>> availablePaymentMethods = ArgumentCaptor.forClass(Set.class);
        Mockito.verify(userOrder).setPaymentOptions(availablePaymentMethods.capture());
        assertTrue(availablePaymentMethods.getValue().containsAll(Set.of(PaymentMethod.CASH_ON_DELIVERY,
                PaymentMethod.CARD_ON_DELIVERY, PaymentMethod.YANDEX)));
    }
}

package ru.yandex.market.checkout.checkouter.order;

import java.util.List;

import org.apache.commons.collections.CollectionUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import ru.yandex.common.util.currency.Currency;
import ru.yandex.market.checkout.checkouter.actualization.model.ImmutableMultiCartParameters;
import ru.yandex.market.checkout.checkouter.cart.MultiCart;
import ru.yandex.market.checkout.checkouter.delivery.Delivery;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryValidator;
import ru.yandex.market.checkout.checkouter.feature.CheckouterFeatureReader;
import ru.yandex.market.checkout.checkouter.order.validation.IMultiOrderConstraint;
import ru.yandex.market.checkout.checkouter.order.validation.OrderConstraint;
import ru.yandex.market.checkout.checkouter.pay.PaymentMethod;
import ru.yandex.market.checkout.checkouter.validation.EmailValidator;
import ru.yandex.market.checkout.common.pay.FinancialValidator;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static ru.yandex.market.checkout.checkouter.feature.type.common.BooleanFeatureType.USE_PERSONAL_EMAIL_ID;
import static ru.yandex.market.checkout.checkouter.feature.type.common.BooleanFeatureType.USE_PERSONAL_FULL_NAME_ID;
import static ru.yandex.market.checkout.checkouter.feature.type.common.BooleanFeatureType.USE_PERSONAL_PHONE_ID;

@ExtendWith(MockitoExtension.class)
public class OrderValidatorTest {

    @Mock
    IMultiOrderConstraint multiOrderConstraintMock;
    private OrderValidator orderValidator;
    @Mock
    private PhoneValidator phoneValidatorMock;
    @Mock
    private EmailValidator emailValidatorMock;
    @Mock
    private FinancialValidator financialValidatorMock;
    @Mock
    private OrderItemValidator orderItemsValidatorMock;
    @Mock
    private DeliveryValidator deliveryValidatorMock;
    @Mock
    private OrderConstraint orderConstraintMock;
    @Mock
    CheckouterFeatureReader checkouterFeatureReader;

    @BeforeEach
    public void setup() {
        orderValidator = new OrderValidator(
                phoneValidatorMock,
                emailValidatorMock,
                financialValidatorMock,
                orderItemsValidatorMock,
                deliveryValidatorMock,
                List.of(orderConstraintMock),
                List.of(multiOrderConstraintMock),
                checkouterFeatureReader);
    }

    @Test
    public void preprocessAndValidateMultiCartShouldValidateCurrencyAndCartItemsAndCartDelivery() {
        // given:
        final Color color = Color.RED;
        final Delivery delivery = new Delivery();

        final OrderItem orderItem = new OrderItem();
        final List<OrderItem> orderItems = List.of(orderItem);

        final Order order = new Order();
        order.setShopId(2L);
        order.setDelivery(delivery);
        order.setItems(orderItems);
        order.setRgb(color);

        final Currency currency = Currency.CAD;
        final List<Order> orders = List.of(order);

        final MultiCart multiCart = new MultiCart();
        multiCart.setCarts(orders);
        multiCart.setBuyerCurrency(currency);
        multiCart.setBuyerRegionId(1L);

        ImmutableMultiCartParameters parameters = ImmutableMultiCartParameters.builder().build();

        // when:
        orderValidator.validateMultiCart(multiCart, parameters);

        // then:
        verify(orderItemsValidatorMock).validateCartItems(argThat(arg ->
                CollectionUtils.isEqualCollection(arg, orderItems)));
        verify(financialValidatorMock).validateBuyerCurrency(currency);
        verify(deliveryValidatorMock).validateCartDelivery(delivery, Platform.UNKNOWN);
        verifyNoMoreInteractions();
    }

    @Test
    public void validateConstraintsShouldValidateOrderConstraintsAndMultiOrderConstraints() {
        // given:
        final Order order = new Order();
        final MultiCart multiCart = new MultiCart();
        multiCart.setCarts(List.of(order));
        multiCart.setBuyerRegionId(1L);

        // when:
        orderValidator.validateConstraints(multiCart);

        // then:
        verify(orderConstraintMock).validate(eq(order), any());
        verify(multiOrderConstraintMock).validate(eq(multiCart), any());
        verifyNoMoreInteractions();
    }

    /**
     * Тест обратной совместимости при использовании открытого номера телефона.
     * Удалить в MARKETCHECKOUT-27094
     */
    @Test
    public void preprocessAndValidateMultiOrderWithPhoneShouldInvokePreprocessorsAndValidators() {
        Mockito.when(checkouterFeatureReader.getBoolean(USE_PERSONAL_PHONE_ID)).thenReturn(false);
        Mockito.when(checkouterFeatureReader.getBoolean(USE_PERSONAL_FULL_NAME_ID)).thenReturn(false);
        Mockito.when(checkouterFeatureReader.getBoolean(USE_PERSONAL_EMAIL_ID)).thenReturn(false);
        // given:
        final Color color = Color.RED;
        final Delivery delivery = new Delivery();

        final OrderItem orderItem = new OrderItem();
        final List<OrderItem> orderItems = List.of(orderItem);

        final Order order = new Order();
        order.setShopId(2L);
        order.setDelivery(delivery);
        order.setItems(orderItems);
        order.setRgb(color);

        final Currency currency = Currency.CAD;
        final List<Order> orders = List.of(order);
        final String phone = "+7 123 456 7890";
        final String email = "hello@localhost.ru";

        final Buyer buyer = new Buyer();
        buyer.setId("1234");
        buyer.setUid(123123123L);
        buyer.setPhone(phone);
        buyer.setEmail(email);
        buyer.setLastName("Иванов");
        buyer.setFirstName("Иван");
        buyer.setMiddleName("Иваныч");

        final MultiOrder multiOrder = new MultiOrder();
        multiOrder.setBuyer(buyer);
        multiOrder.setOrders(orders);
        multiOrder.setBuyerCurrency(currency);
        multiOrder.setBuyerRegionId(1L);
        multiOrder.setPaymentMethod(PaymentMethod.YANDEX);
        final boolean isReserveOnly = false;

        ImmutableMultiCartParameters parameters = ImmutableMultiCartParameters.builder().build();

        // when:
        orderValidator.preprocessAndValidateMultiOrder(multiOrder, isReserveOnly, parameters);

        // then:
        verify(financialValidatorMock).validateBuyerCurrency(currency);
        verify(phoneValidatorMock).validate(phone);
        verify(emailValidatorMock).validate(email, true);
        verify(orderItemsValidatorMock).validateOrderItems(argThat(arg ->
                CollectionUtils.isEqualCollection(arg, orderItems)));
        verify(deliveryValidatorMock).validateDeliveryForCheckout(delivery, Platform.UNKNOWN);
        verifyNoMoreInteractions();
    }

    @Test
    public void preprocessAndValidateMultiOrderShouldInvokePreprocessorsAndValidators() {
        Mockito.when(checkouterFeatureReader.getBoolean(USE_PERSONAL_PHONE_ID)).thenReturn(false);
        Mockito.when(checkouterFeatureReader.getBoolean(USE_PERSONAL_FULL_NAME_ID)).thenReturn(false);
        Mockito.when(checkouterFeatureReader.getBoolean(USE_PERSONAL_EMAIL_ID)).thenReturn(false);
        // given:
        final Color color = Color.RED;
        final Delivery delivery = new Delivery();

        final OrderItem orderItem = new OrderItem();
        final List<OrderItem> orderItems = List.of(orderItem);

        final Order order = new Order();
        order.setShopId(2L);
        order.setDelivery(delivery);
        order.setItems(orderItems);
        order.setRgb(color);

        final Currency currency = Currency.CAD;
        final List<Order> orders = List.of(order);
        final String personalPhoneId = "0123456789abcdef0123456789abcdef";
        final String phone = "+7 123 456 7890";
        final String email = "hello@localhost.ru";

        final Buyer buyer = new Buyer();
        buyer.setId("1234");
        buyer.setUid(123123123L);
        buyer.setPersonalPhoneId(personalPhoneId);
        buyer.setPhone(phone);
        buyer.setEmail(email);
        buyer.setLastName("Иванов");
        buyer.setFirstName("Иван");
        buyer.setMiddleName("Иваныч");

        final MultiOrder multiOrder = new MultiOrder();
        multiOrder.setBuyer(buyer);
        multiOrder.setOrders(orders);
        multiOrder.setBuyerCurrency(currency);
        multiOrder.setBuyerRegionId(1L);
        multiOrder.setPaymentMethod(PaymentMethod.YANDEX);
        final boolean isReserveOnly = false;

        ImmutableMultiCartParameters parameters = ImmutableMultiCartParameters.builder().build();

        // when:
        orderValidator.preprocessAndValidateMultiOrder(multiOrder, isReserveOnly, parameters);

        // then:
        verify(financialValidatorMock).validateBuyerCurrency(currency);
        verify(phoneValidatorMock).validate(phone);
        verify(emailValidatorMock).validate(email, true);
        verify(orderItemsValidatorMock).validateOrderItems(argThat(arg ->
                CollectionUtils.isEqualCollection(arg, orderItems)));
        verify(deliveryValidatorMock).validateDeliveryForCheckout(delivery, Platform.UNKNOWN);
        verifyNoMoreInteractions();
    }

    private void verifyNoMoreInteractions() {
        Mockito.verifyNoMoreInteractions(phoneValidatorMock,
                emailValidatorMock,
                financialValidatorMock,
                orderItemsValidatorMock,
                deliveryValidatorMock,
                orderConstraintMock,
                multiOrderConstraintMock);
    }
}


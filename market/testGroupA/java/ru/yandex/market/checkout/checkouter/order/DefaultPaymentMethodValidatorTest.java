package ru.yandex.market.checkout.checkouter.order;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import ru.yandex.market.checkout.checkouter.actualization.services.PaymentMethodValidator;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryType;
import ru.yandex.market.checkout.checkouter.pay.PaymentMethod;
import ru.yandex.market.checkout.checkouter.pay.PaymentOption;
import ru.yandex.market.checkout.checkouter.pay.PaymentOptionHiddenReason;
import ru.yandex.market.checkout.checkouter.shop.PaymentClass;
import ru.yandex.market.checkout.checkouter.util.CheckouterPropertiesImpl;
import ru.yandex.market.checkout.pushapi.client.entity.DeliveryResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
public class DefaultPaymentMethodValidatorTest {
    private static final Long DEFAULT_SHOP_ID = 1L;
    private static final Long SHOP_ID_IGNORE_FORCE_PAYMENT_METHODS = 2L;

    private PaymentMethodValidator validator;

    @BeforeEach
    public void setUp() throws Exception {
        validator = new PaymentMethodValidator(new CheckouterPropertiesImpl()
                .withIgnoreForcePostpaidShopIds(Set.of(SHOP_ID_IGNORE_FORCE_PAYMENT_METHODS)));
    }

    @Test
    public void shouldAddYandexPaymentWhenPaymentClassIsYandex() {
        List<PaymentMethod> paymentMethods = Collections.emptyList();
        List<PaymentOption> methods = validator.getAndMarkForShop(
                paymentMethods,
                Collections.singleton(PaymentClass.YANDEX),
                DEFAULT_SHOP_ID
        );
        assertEquals(1, methods.size());
        assertEquals(new PaymentOption(PaymentMethod.YANDEX), methods.get(0));
    }

    @Test
    public void shouldFilterOutYandexForPaymentClassShop() {
        List<PaymentMethod> paymentMethods = Arrays.asList(
                PaymentMethod.SHOP_PREPAID,
                PaymentMethod.YANDEX);
        List<PaymentOption> methods = validator.getAndMarkForShop(
                paymentMethods,
                Collections.singleton(PaymentClass.SHOP),
                DEFAULT_SHOP_ID
        );
        assertEquals(1, methods.size());
        assertEquals(new PaymentOption(PaymentMethod.SHOP_PREPAID), methods.get(0));
    }

    @Test
    public void shouldFilterOutPrepaidForPaymentClassOff() {
        List<PaymentMethod> paymentMethods = Arrays.asList(
                PaymentMethod.SHOP_PREPAID,
                PaymentMethod.YANDEX);
        List<PaymentOption> methods = validator.getAndMarkForShop(
                paymentMethods,
                Collections.singleton(PaymentClass.OFF),
                DEFAULT_SHOP_ID
        );
        assertTrue(methods.isEmpty());
    }

    @Test
    public void shouldHideYandexForPostDelivery() {
        DeliveryResponse delivery = new DeliveryResponse();
        delivery.setPaymentAllow(false);
        delivery.setType(DeliveryType.POST);
        List<PaymentOption> methods = validator.getAndMarkForDelivery(
                delivery,
                Collections.singleton(PaymentClass.YANDEX)
        );
        assertEquals(1, methods.size());
        assertEquals(new PaymentOption(PaymentMethod.YANDEX, PaymentOptionHiddenReason.POST), methods.get(0));
    }

    @Test
    public void shouldHideYandexForPostDeliveryOnly() {
        for (Boolean paymentAllowed : Arrays.asList(Boolean.TRUE, Boolean.FALSE)) {
            for (DeliveryType deliveryType : DeliveryType.values()) {
                if (deliveryType == DeliveryType.POST && !paymentAllowed) {
                    continue;
                }
                DeliveryResponse delivery = new DeliveryResponse();
                delivery.setPaymentAllow(paymentAllowed);
                delivery.setType(deliveryType);
                List<PaymentOption> methods = validator.getAndMarkForDelivery(
                        delivery,
                        Collections.singleton(PaymentClass.YANDEX)
                );
                assertFalse(methods.isEmpty());
            }
        }
    }

    @Test
    public void shouldReturnPrepaymentsForShopPaymentClass() {
        List<PaymentMethod> paymentMethods = Arrays.asList(
                PaymentMethod.SHOP_PREPAID, PaymentMethod.YANDEX,
                PaymentMethod.CARD_ON_DELIVERY, PaymentMethod.CASH_ON_DELIVERY);
        List<PaymentOption> methods = validator.getAndMarkForShop(
                paymentMethods,
                Collections.singleton(PaymentClass.SHOP),
                DEFAULT_SHOP_ID
        );
        assertEquals(3, methods.size());
        assertEquals(
                new HashSet<>(Arrays.asList(PaymentMethod.CARD_ON_DELIVERY, PaymentMethod.CASH_ON_DELIVERY,
                        PaymentMethod.SHOP_PREPAID)),
                new HashSet<>(methods.stream().map(PaymentOption::getPaymentMethod).collect(Collectors.toSet())));
    }

    @Test
    public void shouldReturnPostpaymentsForYandexPaymentClassWithIgnoreShopId() {
        List<PaymentMethod> paymentMethods = Arrays.asList(
                PaymentMethod.CARD_ON_DELIVERY, PaymentMethod.CASH_ON_DELIVERY
        );
        List<PaymentOption> methods = validator.getAndMarkForShop(
                paymentMethods,
                Collections.singleton(PaymentClass.YANDEX),
                SHOP_ID_IGNORE_FORCE_PAYMENT_METHODS
        );
        assertEquals(3, methods.size());
        assertEquals(
                new HashSet<>(Arrays.asList(PaymentMethod.CARD_ON_DELIVERY, PaymentMethod.CASH_ON_DELIVERY)),
                new HashSet<>(methods.stream()
                        .filter(method -> !method.isHidden())
                        .map(PaymentOption::getPaymentMethod).collect(Collectors.toSet()))
        );
        assertEquals(
                new HashSet<>(Arrays.asList(PaymentMethod.YANDEX)),
                new HashSet<>(methods.stream()
                        .filter(PaymentOption::isHidden)
                        .map(PaymentOption::getPaymentMethod).collect(Collectors.toSet()))
        );
        assertEquals(
                new HashSet<>(Arrays.asList(PaymentOptionHiddenReason.IGNORED_BY_PARTNER)),
                new HashSet<>(methods.stream()
                        .filter(PaymentOption::isHidden)
                        .map(PaymentOption::getHiddenReason).collect(Collectors.toSet()))
        );

    }

    @Test
    public void shouldReturnPostpaymentsForYandexPaymentClassWithoutIgnoreShopId() {
        List<PaymentMethod> paymentMethods = Arrays.asList(
                PaymentMethod.CARD_ON_DELIVERY, PaymentMethod.CASH_ON_DELIVERY
        );
        List<PaymentOption> methods = validator.getAndMarkForShop(
                paymentMethods,
                Collections.singleton(PaymentClass.YANDEX),
                DEFAULT_SHOP_ID
        );
        assertEquals(3, methods.size());
        assertEquals(
                new HashSet<>(Arrays.asList(PaymentMethod.CARD_ON_DELIVERY, PaymentMethod.CASH_ON_DELIVERY,
                        PaymentMethod.YANDEX)),
                new HashSet<>(methods.stream()
                        .filter(method -> !method.isHidden())
                        .map(PaymentOption::getPaymentMethod).collect(Collectors.toSet()))
        );
    }

    @Test
    public void shouldReturnTheOnlyOneYandexPaymentForKnownUser() {
        List<PaymentMethod> paymentMethods = Collections.singletonList(PaymentMethod.YANDEX);
        List<PaymentOption> methods = validator.getAndMarkForShop(
                paymentMethods,
                Collections.singleton(PaymentClass.YANDEX),
                DEFAULT_SHOP_ID
        );
        assertEquals(1, methods.size());
        assertEquals(new PaymentOption(PaymentMethod.YANDEX), methods.get(0));
    }

    @Test
    public void shouldAllowOnlyYandexMoneyForGlobal() {
        List<PaymentMethod> paymentMethods = Arrays.asList(
                PaymentMethod.CARD_ON_DELIVERY, PaymentMethod.CASH_ON_DELIVERY, PaymentMethod.BANK_CARD,
                PaymentMethod.YANDEX
        );
        List<PaymentOption> methods = validator.getAndMarkForShop(
                paymentMethods,
                Collections.singleton(PaymentClass.GLOBAL),
                DEFAULT_SHOP_ID
        );
        assertEquals(1, methods.size());
        assertEquals(PaymentMethod.YANDEX, methods.get(0).getPaymentMethod());
    }
}

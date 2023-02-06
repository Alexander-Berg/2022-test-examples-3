package ru.yandex.market.checkout.checkouter.order;

import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import ru.yandex.market.checkout.checkouter.jackson.CheckouterDateFormats;
import ru.yandex.market.checkout.checkouter.pay.PaymentMethod;
import ru.yandex.market.checkout.common.time.TestableClock;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class OrderPropertyTypeTest {

    @Test
    public void testGetPropertyByName() {
        OrderPropertyType<?> type = OrderPropertyType.getPropertyByName("platform");
        Assertions.assertEquals(OrderPropertyType.PLATFORM, type);
    }

    @Test
    public void testPartnerPaymentMethods() {
        OrderPropertyType<List<PaymentMethod>> partnerPaymentMethods = OrderPropertyType.PARTNER_PAYMENT_METHODS;

        assertNull(assertDoesNotThrow(() -> partnerPaymentMethods.serialize(null)));
        assertEquals("", assertDoesNotThrow(() -> partnerPaymentMethods.serialize(new ArrayList<>())));
        assertEquals(PaymentMethod.YANDEX.toString(),
                partnerPaymentMethods.serialize(Collections.singletonList(PaymentMethod.YANDEX)));
        assertEquals(String.format("%s,%s", PaymentMethod.APPLE_PAY, PaymentMethod.YANDEX),
                partnerPaymentMethods.serialize(Arrays.asList(PaymentMethod.APPLE_PAY, PaymentMethod.YANDEX)));

        assertIterableEquals(Collections.emptyList(), partnerPaymentMethods.deserialize(null));
        assertIterableEquals(Collections.emptyList(), partnerPaymentMethods.deserialize(""));
        assertIterableEquals(Collections.singletonList(PaymentMethod.YANDEX),
                partnerPaymentMethods.deserialize(PaymentMethod.YANDEX.toString()));
        assertIterableEquals(Arrays.asList(PaymentMethod.APPLE_PAY, PaymentMethod.YANDEX),
                partnerPaymentMethods.deserialize(
                        String.format("%s,%s", PaymentMethod.APPLE_PAY, PaymentMethod.YANDEX)));
    }

    @Test
    public void testOrderPaymentMethods() {
        OrderPropertyType<Set<PaymentMethod>> orderPaymentMethods = OrderPropertyType.ORDER_PAYMENT_OPTIONS;
        EnumSet<PaymentMethod> emptyEnumSet = EnumSet.noneOf(PaymentMethod.class);

        assertNull(assertDoesNotThrow(() -> orderPaymentMethods.serialize(null)));
        assertEquals("", assertDoesNotThrow(() -> orderPaymentMethods.serialize(emptyEnumSet)));
        assertEquals(
                PaymentMethod.YANDEX.toString(), orderPaymentMethods.serialize(EnumSet.of(PaymentMethod.YANDEX))
        );
        assertEquals(String.format("%s,%s", PaymentMethod.YANDEX, PaymentMethod.APPLE_PAY),
                orderPaymentMethods.serialize(EnumSet.of(PaymentMethod.APPLE_PAY, PaymentMethod.YANDEX)));
        assertEquals(emptyEnumSet, orderPaymentMethods.deserialize(null));
        assertEquals(emptyEnumSet, orderPaymentMethods.deserialize(""));
        assertEquals(
                EnumSet.of(PaymentMethod.YANDEX), orderPaymentMethods.deserialize(PaymentMethod.YANDEX.toString())
        );
        assertEquals(
                EnumSet.of(PaymentMethod.APPLE_PAY, PaymentMethod.YANDEX),
                orderPaymentMethods.deserialize(String.format("%s,%s", PaymentMethod.APPLE_PAY, PaymentMethod.YANDEX))
        );
    }

    @Test
    public void testRealDeliveryDateProperty() throws Exception {
        OrderPropertyType<Date> realDeliveryDateProp = OrderPropertyType.REAL_DELIVERY_DATE;
        assertNull(assertDoesNotThrow(() -> realDeliveryDateProp.serialize(null)));

        SimpleDateFormat dateFormat = new SimpleDateFormat(CheckouterDateFormats.DATE_FORMAT);
        assertEquals("12-12-2021", realDeliveryDateProp.serialize(dateFormat.parse("12-12-2021")));

        final TestableClock clock = new TestableClock();
        clock.setFixed(Instant.parse("2021-12-03T12:00:00Z"), ZoneId.of("Europe/Moscow"));
        assertEquals("03-12-2021", realDeliveryDateProp.serialize(Date.from(clock.instant())));

        assertNull(assertDoesNotThrow(() -> realDeliveryDateProp.deserialize(null)));
        assertEquals(dateFormat.parse("12-12-2021"), realDeliveryDateProp.deserialize("12-12-2021"));
    }
}

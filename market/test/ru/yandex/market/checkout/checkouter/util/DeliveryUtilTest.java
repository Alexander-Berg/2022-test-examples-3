package ru.yandex.market.checkout.checkouter.util;

import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.HashSet;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import ru.yandex.market.checkout.checkouter.delivery.Delivery;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryDates;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryPartnerType;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryType;
import ru.yandex.market.checkout.checkouter.delivery.RawDeliveryInterval;
import ru.yandex.market.checkout.checkouter.delivery.RawDeliveryIntervalsCollection;
import ru.yandex.market.checkout.checkouter.pay.PaymentMethod;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author mmetlov
 */
public class DeliveryUtilTest {

    private static final ThreadLocal<SimpleDateFormat> SIMPLE_DATE_FORMAT = ThreadLocal.withInitial(
            () -> new SimpleDateFormat("dd-MM-yyyy")
    );

    private static Stream<Arguments> timeIntervalsSource() throws ParseException {
        var resultWithoutInterval = "774_DELIVERY_2345_20-05-2013_noreserve_23-05-2013_1_24_99_SHOP";
        var date = SIMPLE_DATE_FORMAT.get().parse("20-05-2013");

        var deliveryIntervals = new RawDeliveryIntervalsCollection();
        deliveryIntervals.add(new RawDeliveryInterval(date,
                LocalTime.of(9, 30), LocalTime.of(18, 55)));
        deliveryIntervals.add(new RawDeliveryInterval(date,
                LocalTime.of(10, 0), LocalTime.of(20, 30)));

        var singleDeliveryInterval = new RawDeliveryIntervalsCollection();
        singleDeliveryInterval.add(new RawDeliveryInterval(date,
                LocalTime.of(10, 0), LocalTime.of(20, 30)));

        return Stream.of(
                Arguments.of(deliveryIntervals, resultWithoutInterval + "_0930-1855_TRYING_OFF"),
                Arguments.of(singleDeliveryInterval, resultWithoutInterval + "_1000-2030_TRYING_OFF"),
                Arguments.of(new RawDeliveryIntervalsCollection(), resultWithoutInterval + "_null_TRYING_OFF"),
                Arguments.of(null, resultWithoutInterval + "_null_TRYING_OFF")
        );
    }

    @Test
    public void testBuildDeliveryOptionId() throws Exception {
        Delivery delivery = new Delivery() {{
            setShopDeliveryId("1");
            setType(DeliveryType.DELIVERY);
            setServiceName("2345");
            setPrice(new BigDecimal(10));
            setBuyerPrice(new BigDecimal(10));
            setDeliveryDates(new DeliveryDates(
                    SIMPLE_DATE_FORMAT.get().parse("20-05-2013"), SIMPLE_DATE_FORMAT.get().parse("23-05-2013")
            ));
            setPaymentOptions(new HashSet<>(Arrays.asList(PaymentMethod.CARD_ON_DELIVERY,
                    PaymentMethod.CASH_ON_DELIVERY)));
            setDeliveryServiceId(99L);
            setDeliveryPartnerType(DeliveryPartnerType.SHOP);
            setTryingAvailable(true);
        }};
        String actualId = DeliveryUtil.buildDeliveryOptionId(774, delivery);
        assertEquals("774_DELIVERY_2345_20-05-2013_noreserve_23-05-2013_1_24_99_SHOP_null_TRYING_ON", actualId);
    }

    @ParameterizedTest
    @MethodSource("timeIntervalsSource")
    public void testBuildDeliveryOptionI_withTimeIntervals(RawDeliveryIntervalsCollection deliveryIntervals,
                                                           String expectedOptionId) throws Exception {
        // Assign
        Delivery delivery = new Delivery() {{
            setShopDeliveryId("1");
            setType(DeliveryType.DELIVERY);
            setServiceName("2345");
            setPrice(new BigDecimal(10));
            setBuyerPrice(new BigDecimal(10));
            setDeliveryDates(new DeliveryDates(
                    SIMPLE_DATE_FORMAT.get().parse("20-05-2013"), SIMPLE_DATE_FORMAT.get().parse("23-05-2013")
            ));
            setPaymentOptions(new HashSet<>(Arrays.asList(PaymentMethod.CARD_ON_DELIVERY,
                    PaymentMethod.CASH_ON_DELIVERY)));
            setDeliveryServiceId(99L);
            setDeliveryPartnerType(DeliveryPartnerType.SHOP);
            setRawDeliveryIntervals(deliveryIntervals);
            setTryingAvailable(false);
        }};
        // Act
        String actualId = DeliveryUtil.buildDeliveryOptionId(774, delivery);
        // Assert
        assertEquals(expectedOptionId, actualId);
    }

    @Test
    public void testBuildPaymentOptionsId() throws Exception {
        String actualId = DeliveryUtil.buildPaymentOptionsId(Arrays.asList(
                PaymentMethod.SHOP_PREPAID,
                PaymentMethod.BANK_CARD,
                PaymentMethod.YANDEX_MONEY,
                PaymentMethod.CARD_ON_DELIVERY,
                PaymentMethod.CASH_ON_DELIVERY,
                PaymentMethod.YANDEX
        ));
        assertEquals("63", actualId);
    }
}

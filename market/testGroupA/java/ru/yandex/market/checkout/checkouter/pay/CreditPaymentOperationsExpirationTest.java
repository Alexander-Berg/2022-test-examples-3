package ru.yandex.market.checkout.checkouter.pay;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Collections;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import ru.yandex.market.checkout.checkouter.client.ClientInfo;
import ru.yandex.market.checkout.checkouter.sberbank.model.SberOrderStatusResponse;
import ru.yandex.market.checkout.helpers.OrderPayHelper;
import ru.yandex.market.checkout.util.sberbank.SberMockConfigurer;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class CreditPaymentOperationsExpirationTest extends AbstractPaymentTestBase {

    private static final Instant CREATE_DATE = newInstant(2019, 05, 17, 18, 10);

    @Autowired
    private CreditPaymentOperations creditPaymentOperations;
    @Autowired
    private SberMockConfigurer sberMockConfigurer;
    @Autowired
    private OrderPayHelper paymentHelper;

    @Value("${checkouter.credit.payment.paymentExpireTimeoutInMinutes}")
    private Integer creditPaymentExpirationMinutes;

    private static Instant newInstant(int year, int month, int day, int hour, int minute) {
        return LocalDateTime.of(year, month, day, hour, minute)
                .atZone(ZoneId.systemDefault())
                .toInstant();
    }

    @BeforeEach
    public void initiliaze() {
        setFixedTime(CREATE_DATE);
        sberMockConfigurer.mockWholeSber();
        orderServiceTestHelper.createUnpaidBlueOrder(order -> order.setPaymentMethod(PaymentMethod.CREDIT));
    }

    @Test
    public void testThresholdOne() {
        Payment payment = paymentHelper.pay(order().getId());

        Instant instant = incrementAndSetDate(CREATE_DATE, 20);
        updatePaymentExpiration(payment);

        // < часа --> каждую минуту
        validatePaymentExpirationDate(instant.plus(1, ChronoUnit.MINUTES));
    }

    @Test
    public void testThresholdTwo() {
        Payment payment = paymentHelper.pay(order().getId());

        Instant instant = incrementAndSetDate(CREATE_DATE, 67);
        updatePaymentExpiration(payment);

        // < 3х часов   --> каждые 5 минут
        validatePaymentExpirationDate(instant.plus(5, ChronoUnit.MINUTES));
    }

    @Test
    public void testBeyondAllNormalThresholds() {
        Payment payment = paymentHelper.pay(order().getId());

        Instant instant = incrementAndSetDate(CREATE_DATE, 200);
        updatePaymentExpiration(payment);

        //больше 3х часов -- каждые 15 минут
        validatePaymentExpirationDate(instant.plus(15, ChronoUnit.MINUTES));
    }

    @Test
    public void testNearToDieThreshhold() {
        Payment payment = paymentHelper.pay(order().getId());

        Instant instant = incrementAndSetDate(CREATE_DATE, creditPaymentExpirationMinutes - 4);
        updatePaymentExpiration(payment);

        //Почти уже заекспайрился платеж
        validatePaymentExpirationDate(instant.plus(5, ChronoUnit.MINUTES));
    }

    @Test
    public void testAfterExpirationTimeUpdate() {
        Payment payment = paymentHelper.pay(order().getId());

        incrementAndSetDate(CREATE_DATE, creditPaymentExpirationMinutes + 4);
        updatePaymentExpiration(payment);

        payment = orderService.getOrder(order().getId()).getPayment();
        assertEquals(PaymentStatus.CANCELLED, payment.getStatus());
    }

    private Instant incrementAndSetDate(Instant originalDate, int minutesToAdd) {
        Instant newInstant = originalDate.plus(minutesToAdd, ChronoUnit.MINUTES);
        setFixedTime(newInstant);
        return newInstant;
    }

    private void validatePaymentExpirationDate(Instant expectedExpiration) {
        Payment payment = orderService.getOrder(order().getId()).getPayment();
        assertEquals(expectedExpiration, payment.getStatusExpiryDate().toInstant());
    }

    private void updatePaymentExpiration(Payment payment) {
        transactionTemplate.execute(t -> {
            SberOrderStatusResponse response = new SberOrderStatusResponse();
            response.setOrderStatus(0);
            creditPaymentOperations.updateWaitingPayment(Collections.singletonList(order()), payment,
                    ClientInfo.SYSTEM, response);
            return null;
        });
    }
}

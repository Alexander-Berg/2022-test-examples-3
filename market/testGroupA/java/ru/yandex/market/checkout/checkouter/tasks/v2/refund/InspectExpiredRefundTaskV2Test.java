package ru.yandex.market.checkout.checkouter.tasks.v2.refund;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Collections;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.checkouter.pay.PaymentGoal;
import ru.yandex.market.checkout.checkouter.refund.AbstractRefundTestBase;
import ru.yandex.market.checkout.checkouter.tasks.v2.TaskRunType;
import ru.yandex.market.checkout.checkouter.tasks.v2.TaskStageType;
import ru.yandex.market.checkout.util.b2b.B2bCustomersMockConfigurer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static ru.yandex.market.checkout.checkouter.tasks.v2.refund.InspectExpiredRefundTaskV2.EXHAUST_AFTER_DAYS;

public class InspectExpiredRefundTaskV2Test extends AbstractRefundTestBase {
    @Autowired
    private InspectExpiredRefundTaskV2 inspectExpiredRefundTaskV2;
    @Autowired
    private B2bCustomersMockConfigurer b2bCustomersMockConfigurer;

    @Test
    public void shouldWorkWithExpiredRefunds() throws Exception {
        checkouterProperties.setAsyncRefundStrategies(Collections.emptySet());
        var refund = prepareRefund();
        setFixedTime(getClock().instant().plus(1, ChronoUnit.HOURS));

        var expiredRefunds = refundService.getRefundsWithExpiredStatus(LocalDateTime.now(getClock()));
        assertEquals(1, expiredRefunds.size());
        var refundWithExpiredStatus = expiredRefunds.iterator().next();
        Assertions.assertEquals(refund.getId(), refundWithExpiredStatus.getId());

        var result = inspectExpiredRefundTaskV2.run(TaskRunType.ONCE);
        Assertions.assertEquals(TaskStageType.SUCCESS, result.getStage(), result.toString());

        assertEquals(0, refundService.getRefundsWithExpiredStatus(LocalDateTime.now(getClock())).size());
    }

    @Test
    public void shouldFilterOutOldRefunds() throws Exception {
        var refund = prepareRefund();
        setFixedTime(getClock().instant().plus(1, ChronoUnit.HOURS));

        var expiredRefunds = refundService.getRefundsWithExpiredStatus(LocalDateTime.now(getClock()));
        assertEquals(1, expiredRefunds.size());
        var refundWithExpiredStatus = expiredRefunds.iterator().next();
        Assertions.assertEquals(refund.getId(), refundWithExpiredStatus.getId());

        setFixedTime(getClock().instant().plus(EXHAUST_AFTER_DAYS + 1, ChronoUnit.DAYS));
        var result = inspectExpiredRefundTaskV2.run(TaskRunType.ONCE);
        Assertions.assertEquals(TaskStageType.SUCCESS, result.getStage(), result.toString());

        //все еще остался один рефанд, который таска пропустила
        assertEquals(1, refundService.getRefundsWithExpiredStatus(LocalDateTime.now(getClock())).size());
    }

    @Test
    public void shouldSkipB2bRefund() throws Exception {
        b2bCustomersMockConfigurer.mockReservationDate(LocalDate.now().plusDays(5));
        checkouterProperties.setAsyncRefundStrategies(Collections.emptySet());
        var refund = prepareB2bRefund();
        setFixedTime(getClock().instant().plus(1, ChronoUnit.HOURS));

        var expiredRefunds = refundService.getRefundsWithExpiredStatus(LocalDateTime.now(getClock()));
        assertEquals(0, expiredRefunds.size());
        Assertions.assertNotNull(order.getPayment());
        Assertions.assertEquals(PaymentGoal.ORDER_ACCOUNT_PAYMENT, order.getPayment().getType());

        var result = inspectExpiredRefundTaskV2.run(TaskRunType.ONCE);
        Assertions.assertEquals(TaskStageType.SUCCESS, result.getStage(), result.toString());
        Assertions.assertEquals(0, inspectExpiredRefundTaskV2.countItemsToProcess());
        Assertions.assertEquals(0, inspectExpiredRefundTaskV2.prepareBatch().size());

        assertEquals(0, refundService.getRefundsWithExpiredStatus(LocalDateTime.now(getClock())).size());
    }
}

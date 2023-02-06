package ru.yandex.market.shopadminstub.sax.serialize;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Sets;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import ru.yandex.market.checkout.checkouter.pay.PaymentMethod;
import ru.yandex.market.shopadminstub.model.Item;
import ru.yandex.market.shopadminstub.model.ReportOutlet;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Set;

public class ReportOutletBuilderTest {

    private static final Set<PaymentMethod> PAYMENT_METHODS = Sets.newHashSet(PaymentMethod.CASH_ON_DELIVERY);

    @Test
    public void shouldCalculateInterception() {
        Item first = new Item();
        first.addReportOutlet(new ReportOutlet(1, 1, 2, PAYMENT_METHODS, new BigDecimal(1)));
        first.addReportOutlet(new ReportOutlet(2, 2, 3, PAYMENT_METHODS, new BigDecimal(2)));
        first.addReportOutlet(new ReportOutlet(3, 3, 4, PAYMENT_METHODS, new BigDecimal(3)));
        //
        Item second = new Item();
        second.addReportOutlet(new ReportOutlet(1, 2, 3, PAYMENT_METHODS, new BigDecimal(1)));
        second.addReportOutlet(new ReportOutlet(2, 2, 3, PAYMENT_METHODS, new BigDecimal(2)));
        //
        Item third = new Item();
        third.addReportOutlet(new ReportOutlet(2, 2, 3, PAYMENT_METHODS, new BigDecimal(2)));
        third.addReportOutlet(new ReportOutlet(3, 2, 5, PAYMENT_METHODS, new BigDecimal(3)));
        //
        Map<Long, ReportOutlet> result = ReportOutletBuilder.calculateCommonOutlets(ImmutableList.of(first, second,
                third));
        Assertions.assertEquals(1, result.size());
        Assertions.assertTrue(result.containsKey(2L));
    }

    @Test
    public void shouldMergeDeliveryDates() {
        Item first = new Item();
        first.addReportOutlet(new ReportOutlet(1, 2, 4, PAYMENT_METHODS, BigDecimal.TEN));
        Item second = new Item();
        second.addReportOutlet(new ReportOutlet(1, 1, 5, PAYMENT_METHODS, BigDecimal.TEN));
        Map<Long, ReportOutlet> result = ReportOutletBuilder.calculateCommonOutlets(ImmutableList.of(first, second));
        Assertions.assertEquals(1, result.size());
        ReportOutlet resultOutlet = result.get(1L);
        Assertions.assertNotNull(resultOutlet);
        Assertions.assertEquals((Integer) 2, resultOutlet.getMinDeliveryDays());
        Assertions.assertEquals((Integer) 5, resultOutlet.getMaxDeliveryDays());
    }

    @Test
    public void shouldNotFailOnNullMaxDeliveryDays() {
        Item first = new Item();
        first.addReportOutlet(new ReportOutlet(1, 2, 4, PAYMENT_METHODS, BigDecimal.ZERO));
        //
        Item second = new Item();
        second.addReportOutlet(new ReportOutlet(1, 30, null, PAYMENT_METHODS, BigDecimal.ZERO));
        //
        Map<Long, ReportOutlet> result = ReportOutletBuilder.calculateCommonOutlets(ImmutableList.of(first, second));
        Assertions.assertEquals(1, result.size());
        ReportOutlet resultOutlet = result.get(1L);
        Assertions.assertNotNull(resultOutlet);
        Assertions.assertEquals((Integer) 30, resultOutlet.getMinDeliveryDays());
        Assertions.assertEquals(null, resultOutlet.getMaxDeliveryDays());
    }

    @Test
    public void shouldMergeCost() {
        BigDecimal expectedCost = new BigDecimal(20);
        Item first = new Item();
        first.addReportOutlet(new ReportOutlet(1, 1, 4, PAYMENT_METHODS, BigDecimal.TEN));
        Item second = new Item();
        second.addReportOutlet(new ReportOutlet(1, 1, 4, PAYMENT_METHODS, expectedCost));
        Map<Long, ReportOutlet> result = ReportOutletBuilder.calculateCommonOutlets(ImmutableList.of(first, second));
        Assertions.assertEquals(1, result.size());
        ReportOutlet resultOutlet = result.get(1L);
        Assertions.assertNotNull(resultOutlet);
        Assertions.assertEquals(expectedCost, resultOutlet.getCost());
    }

    @Test
    public void shouldUseProvidedCostIfSomeAreNull() {
        Item first = new Item();
        first.addReportOutlet(new ReportOutlet(1, 2, 4, PAYMENT_METHODS, null));
        //
        Item second = new Item();
        second.addReportOutlet(new ReportOutlet(1, 2, 4, PAYMENT_METHODS, BigDecimal.TEN));
        //
        Map<Long, ReportOutlet> result = ReportOutletBuilder.calculateCommonOutlets(ImmutableList.of(first, second));
        Assertions.assertEquals(1, result.size());
        ReportOutlet resultOutlet = result.get(1L);
        Assertions.assertNotNull(resultOutlet);
        Assertions.assertEquals(BigDecimal.TEN, resultOutlet.getCost());
    }

    @Test
    public void shouldSetCostZeroIfAllCostsNull() {
        Item first = new Item();
        first.addReportOutlet(new ReportOutlet(1, 2, 4, PAYMENT_METHODS, null));
        //
        Item second = new Item();
        second.addReportOutlet(new ReportOutlet(1, 2, 4, PAYMENT_METHODS, null));
        //
        Map<Long, ReportOutlet> result = ReportOutletBuilder.calculateCommonOutlets(ImmutableList.of(first, second));
        Assertions.assertEquals(1, result.size());
        ReportOutlet resultOutlet = result.get(1L);
        Assertions.assertNotNull(resultOutlet);
        Assertions.assertEquals(BigDecimal.ZERO, resultOutlet.getCost());
    }
}

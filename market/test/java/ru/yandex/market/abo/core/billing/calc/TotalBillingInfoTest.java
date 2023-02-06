package ru.yandex.market.abo.core.billing.calc;

import java.math.BigDecimal;
import java.util.Arrays;

import org.junit.jupiter.api.Test;

import ru.yandex.market.abo.core.billing.calc.row.BillingResultInfo;
import ru.yandex.market.abo.core.billing.calc.row.BillingSummaryRow;
import ru.yandex.market.abo.core.billing.rate.BillingColor;
import ru.yandex.market.abo.core.billing.rate.BillingItem;
import ru.yandex.market.abo.core.billing.report.BillingReport;
import ru.yandex.market.abo.core.billing.report.BillingResult;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author antipov93.
 * @date 06.07.18.
 */
public class TotalBillingInfoTest {

    private static final double EPSILON = 0.01;

    @Test
    public void testCalculateTotalPayments() {
        BillingSummaryRow firstRow = new BillingSummaryRow(1L, "1", null, Arrays.asList(
                createBillingResultInfo(1, BillingColor.GREEN, 1, 10), // 10
                createBillingResultInfo(2, BillingColor.RED, 2, 3), // 6
                createBillingResultInfo(3, BillingColor.GREEN, 4, 1) // 4
        ));
        BillingSummaryRow secondRow = new BillingSummaryRow(1L, "1", null, Arrays.asList(
                createBillingResultInfo(1, BillingColor.GREEN, 1, 6), // 6
                createBillingResultInfo(2, BillingColor.BLUE, 5, 2), // 10
                createBillingResultInfo(4, BillingColor.BONUS, 2, 1) // 2
        ));

        TotalBillingInfo totalBillingInfo = TotalBillingInfo.calculateTotalPayments(Arrays.asList(firstRow, secondRow));
        assertEquals(38.0, totalBillingInfo.totalPayments, EPSILON);
        assertEquals(2.0, totalBillingInfo.paymentsByColor.get(BillingColor.BONUS).doubleValue(), EPSILON);
        assertEquals(20.0, totalBillingInfo.paymentsByColor.get(BillingColor.GREEN).doubleValue(), EPSILON);
        assertEquals(10.0, totalBillingInfo.paymentsByColor.get(BillingColor.BLUE).doubleValue(), EPSILON);
        assertEquals(6.0, totalBillingInfo.paymentsByColor.get(BillingColor.RED).doubleValue(), EPSILON);
    }


    private static BillingResultInfo createBillingResultInfo(int resultId, BillingColor color, int rate, int count) {
        BillingReport report = new BillingReport();
        report.setId(1);
        BillingResult result = new BillingResult();
        result.setId(resultId);
        result.setReport(report);
        BillingItem item = new BillingItem();
        item.setColor(color);
        result.setItem(item);
        return new BillingResultInfo(result, BigDecimal.valueOf(count), BigDecimal.valueOf(rate));
    }
}

package ru.yandex.market.abo.core.billing.calc.calculator.type;

import java.time.LocalDate;
import java.util.TreeSet;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.EmptyTest;
import ru.yandex.market.abo.core.billing.report.BillingReport;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author antipov93.
 * @date 26.07.18.
 */
public class BillingStCalculatorTest extends EmptyTest {

    @Autowired
    private BillingStCalculator billingStCalculator;

    @Test
    public void testEmptyReport() {
        BillingReport report = new BillingReport();
        report.setResults(new TreeSet<>());
        LocalDate fromDate = LocalDate.now().minusDays(10);
        LocalDate toDate = LocalDate.now();
        assertEquals(0, billingStCalculator.calculate(report, fromDate, toDate).size());
    }
}

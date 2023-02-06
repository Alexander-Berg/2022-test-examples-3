package ru.yandex.market.abo.core.billing.calc.calculator.type;

import java.math.BigDecimal;
import java.math.MathContext;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.EmptyTest;
import ru.yandex.market.abo.core.CoreConfig;
import ru.yandex.market.abo.core.billing.calc.calculator.BillingReportCalculator;
import ru.yandex.market.abo.core.billing.calc.row.BillingReportRow;
import ru.yandex.market.abo.core.billing.calc.row.BillingResultInfo;
import ru.yandex.market.abo.core.billing.rate.BillingRateService;
import ru.yandex.market.abo.core.billing.report.BillingReport;
import ru.yandex.market.abo.core.billing.report.BillingReportService;
import ru.yandex.market.abo.core.billing.report.BillingResult;
import ru.yandex.market.util.db.ConfigurationService;

import static java.util.stream.Collectors.toMap;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

/**
 * @author antipov93.
 * @date 08.08.18.
 */
public class BillingTargetsCalculatorTest extends EmptyTest {

    private static final int TARGETS_REPORT_ID = 13;
    private static final LocalDate FROM_DATE = LocalDate.now().minusDays(10);
    private static final LocalDate TO_DATE = LocalDate.now();
    private static final BigDecimal BONUS_VALUE = BigDecimal.valueOf(1000);
    private static final double EPSILON = 0.001;

    @Autowired
    @InjectMocks
    private BillingTargetsCalculator billingTargetsCalculator;
    @Mock
    private BillingReportCalculator billingReportCalculator;

    @Autowired
    private ConfigurationService aboConfigurationService;
    @Autowired
    private BillingReportService billingReportService;
    @Autowired
    private BillingRateService billingRateService;


    @BeforeEach
    public void setUp() throws Exception {
        MockitoAnnotations.openMocks(this);
        long stResultId = aboConfigurationService.getValueAsLong(
                CoreConfig.ST_DELIVERY_BILLING_RESULT_ID.getIdAsString());

        when(billingReportCalculator.calculate(stResultId, FROM_DATE, TO_DATE)).thenReturn(
                new HashMap<Long, BillingResultInfo>() {{
                    put(1L, createBillingResultInfo(15));
                    put(2L, createBillingResultInfo(10));
                    put(3L, createBillingResultInfo(25));
                }}
        );
        when(billingReportCalculator.calculate(BillingTargetsCalculator.CORE_TICKET_DELIVERY_BILLING_RESULT_ID,
                FROM_DATE, TO_DATE)).thenReturn(
                new HashMap<Long, BillingResultInfo>() {{
                    put(1L, createBillingResultInfo(15));
                    put(2L, createBillingResultInfo(10));
                    put(4L, createBillingResultInfo(25));
                }}
        );
    }

    @Test
    public void calculateTargets() {
        BillingReport report = billingReportService.load(TARGETS_REPORT_ID);
        BillingResult bonusResult = report.getResult(BillingTargetsCalculator.BONUS_INDEX);
        billingRateService.addRate(BONUS_VALUE, FROM_DATE, bonusResult.getId());
        List<BillingReportRow> rows = billingTargetsCalculator.calculate(report, FROM_DATE, TO_DATE);
        assertEquals(4, rows.size());
        Map<Long, BillingReportRow> resultMap = rows.stream().collect(
                toMap(BillingReportRow::getUserId, Function.identity())
        );
        double gross = BONUS_VALUE.divide(BigDecimal.valueOf(billingTargetsCalculator.getTax()), MathContext.DECIMAL32)
                .doubleValue();
        assertEquals(gross, resultMap.get(1L).getSum().doubleValue(), EPSILON);
        assertEquals(0, resultMap.get(2L).getSum().doubleValue(), EPSILON);
        assertEquals(gross, resultMap.get(3L).getSum().doubleValue(), EPSILON);
        assertEquals(gross, resultMap.get(4L).getSum().doubleValue(), EPSILON);
    }

    private static BillingResultInfo createBillingResultInfo(int count) {
        return new BillingResultInfo(null, BigDecimal.valueOf(count), null);
    }
}

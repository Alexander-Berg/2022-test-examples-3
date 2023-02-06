package ru.yandex.market.abo.core.billing.calc.calculator.type;

import java.math.BigDecimal;
import java.math.MathContext;
import java.time.LocalDate;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.LongStream;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import ru.yandex.EmptyTest;
import ru.yandex.market.abo.core.CoreConfig;
import ru.yandex.market.abo.core.assessor.AssessorService;
import ru.yandex.market.abo.core.assessor.model.Assessor;
import ru.yandex.market.abo.core.billing.calc.calculator.BillingReportCalculator;
import ru.yandex.market.abo.core.billing.calc.row.BillingReportRow;
import ru.yandex.market.abo.core.billing.rate.BillingRateService;
import ru.yandex.market.abo.core.billing.report.BillingReport;
import ru.yandex.market.abo.core.billing.report.BillingReportService;
import ru.yandex.market.abo.core.billing.report.BillingResult;
import ru.yandex.market.util.db.ConfigurationService;

import static java.util.stream.Collectors.toMap;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author antipov93.
 * @date 30.06.18.
 */
public abstract class BillingReportCalculatorTest extends EmptyTest {

    private static final double EPSILON = 0.1;
    protected static final Random RND = new Random();

    @PersistenceContext
    private EntityManager entityManager;
    @Autowired
    private BillingReportCalculator billingReportCalculator;
    @Autowired
    private BillingReportService billingReportService;
    @Autowired
    private BillingRateService billingRateService;
    @Autowired
    protected AssessorService assessorService;
    @Autowired
    private ConfigurationService aboConfigurationService;
    @Autowired
    protected JdbcTemplate pgJdbcTemplate;


    private LocalDate startDate = LocalDate.now().minusDays(10);
    private LocalDate finishDate = LocalDate.now().plusDays(1);

    protected long reportId;
    protected Collection<Long> assessorIds;
    protected BillingReport billingReport;

    private Map<Integer, BigDecimal> columnIndexToRate = new HashMap<>();
    private Map<Long, Map<Integer, Integer>> userIdToCountsMap = new HashMap<>();
    private double incomeTax;

    private AtomicLong nextId = new AtomicLong(1000);

    public BillingReportCalculatorTest() {
    }

    public BillingReportCalculatorTest(long reportId) {
        this.reportId = reportId;
    }

    @BeforeEach
    public void setUp() {
        createAssessors();
        populateData();
        createBillingRates();

        flushAndClear();
        billingReport = billingReportService.load(reportId); // attach added rates
    }

    private void createBillingRates() {
        incomeTax = aboConfigurationService.getValue(CoreConfig.PERSONAL_INCOME_TAX.getIdAsString(), Double.class);

        billingReport = billingReportService.load(reportId);
        billingReport.getResults().stream().filter(BillingResult::isBilled).forEach(this::addRate);
    }

    private void addRate(BillingResult column) {
        BigDecimal rateValue = BigDecimal.valueOf(RND.nextInt(1000000) / 1000.0); // maximum 3 points after digit
        billingRateService.addRate(rateValue, startDate, column.getId());
        columnIndexToRate.put(column.getResultIndex(), rateValue);
    }

    private void createAssessors() {
        assessorIds = nextIds(10);
        assessorIds.stream().map(id -> new Assessor(id, "Assessor #" + id)).forEach(assessorService::saveAssessor);
    }

    protected abstract void populateData();

    protected void addItemsToUser(long userId, int index, int count) {
        Map<Integer, Integer> counts = userIdToCountsMap.getOrDefault(userId, new HashMap<>());
        counts.put(index, count);
        userIdToCountsMap.put(userId, counts);
    }

    @Test
    public void testCalculate() {
        Map<Long, BillingReportRow> rows = calculate();
        assertEquals(assessorIds, rows.values().stream().map(BillingReportRow::getUserId).collect(Collectors.toSet()));
        rows.forEach((userId, row) -> checkRow(row));
    }

    private Map<Long, BillingReportRow> calculate() {
        List<BillingReportRow> calculate = billingReportCalculator.calculate(billingReport, startDate, finishDate);
        return calculate.stream()
                .collect(toMap(BillingReportRow::getUserId, Function.identity()));
    }


    private BigDecimal getCountByIndex(BillingReportRow row, int index) {
        return row.getResultByIndex(index).getCount();
    }

    private void checkRow(BillingReportRow row) {
        Map<Integer, Integer> columnIndexToExpectedCount = userIdToCountsMap.get(row.getUserId());
        columnIndexToExpectedCount.forEach((index, count) ->
                assertEquals(count.intValue(), getCountByIndex(row, index).intValue(), "column " + index + " differs for row " + row)
        );

        BigDecimal expectedSum = columnIndexToExpectedCount.keySet().stream()
                .map(index -> {
                    BigDecimal rate = columnIndexToRate.get(index);
                    rate = rate.divide(BigDecimal.valueOf(incomeTax), MathContext.DECIMAL32);
                    int expectedCount = columnIndexToExpectedCount.get(index);
                    return rate.multiply(BigDecimal.valueOf(expectedCount));
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        assertEquals(expectedSum.doubleValue(), row.getSum().doubleValue(), EPSILON);
    }

    protected long nextId() {
        return nextId.getAndIncrement();
    }

    protected Collection<Long> nextIds() {
        return LongStream.range(0, 5 + RND.nextInt(6))
                .map(i -> nextId()).boxed()
                .collect(Collectors.toSet());
    }

    private Collection<Long> nextIds(int size) {
        return LongStream.range(0, size)
                .map(i -> 1120000000000000L + nextId()).boxed()
                .collect(Collectors.toSet());
    }
}

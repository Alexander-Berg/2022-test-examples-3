package ru.yandex.market.abo.core.billing;

import java.util.List;
import java.util.Set;
import java.util.SortedSet;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import ru.yandex.EmptyTest;
import ru.yandex.market.abo.core.billing.rate.BillingItem;
import ru.yandex.market.abo.core.billing.rate.BillingRate;
import ru.yandex.market.abo.core.billing.report.BillingReport;
import ru.yandex.market.abo.core.billing.report.BillingReportService;
import ru.yandex.market.abo.core.billing.report.BillingResult;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author antipov93.
 * @date 13.06.18.
 */
public class BillingReportServiceTest extends EmptyTest {

    @Autowired
    private BillingReportService billingReportService;

    @Autowired
    private JdbcTemplate pgJdbcTemplate;

    @BeforeEach
    public void setUp() {
        pgJdbcTemplate.update("INSERT INTO billing_report (id, active, report_type) VALUES (-1, TRUE, 'SQL_QUERY')");
        pgJdbcTemplate.update("INSERT INTO billing_report (id, active, report_type) VALUES (-2, TRUE, 'SQL_QUERY')");

        pgJdbcTemplate.update("INSERT INTO billing_result (id, report_id, result_index, hidden) " +
                "VALUES (-1, -1, 1, FALSE)");
        pgJdbcTemplate.update("INSERT INTO billing_result (id, report_id, result_index, hidden) " +
                "VALUES (-2, -1, 2, FALSE)");
        pgJdbcTemplate.update("INSERT INTO billing_result (id, report_id, result_index, hidden) " +
                "VALUES (-3, -1, 3, FALSE)");

        pgJdbcTemplate.update("INSERT INTO billing_result (id, report_id, result_index, hidden) " +
                "VALUES (-4, -2, 1, FALSE)");
        pgJdbcTemplate.update("INSERT INTO billing_result (id, report_id, result_index, hidden) " +
                "VALUES (-5, -2, 2, FALSE)");


        pgJdbcTemplate.update("INSERT INTO billing_rate (id, creation_time, billing_query_result_id) " +
                "VALUES (-1, now(), -1)");
        pgJdbcTemplate.update("INSERT INTO billing_rate (id, creation_time, billing_query_result_id) " +
                "VALUES (-2, now(), -1)");
        pgJdbcTemplate.update("INSERT INTO billing_rate (id, creation_time, billing_query_result_id) " +
                "VALUES (-3, now(), -2)");


        pgJdbcTemplate.update("INSERT INTO billing_rate (id, creation_time, billing_query_result_id) " +
                "VALUES (-4, now(), -5)");
        pgJdbcTemplate.update("INSERT INTO billing_rate (id, creation_time, billing_query_result_id) " +
                "VALUES (-5, now(), -5)");
        pgJdbcTemplate.update("INSERT INTO billing_rate (id, creation_time, billing_query_result_id) " +
                "VALUES (-6, now(), -5)");

        pgJdbcTemplate.update("INSERT INTO billing_item (result_id, name, unit, color)" +
                "VALUES (-1, 'name', 'unit', 0)");
        pgJdbcTemplate.update("INSERT INTO billing_item (result_id, name, unit, color)" +
                "VALUES (-5, 'name', 'unit', 1)");
    }

    /**
     * Check that property "results" of {@link ru.yandex.market.abo.core.billing.report.BillingReport} was not loaded.
     */
    @Test
    public void testResultsNotLoaded() {
        List<BillingReport> reports = billingReportService.loadActive();
        assertTrue(reports.stream().noneMatch(report -> isInitialized(report.getResults())));
    }

    @Test
    public void testFindOne() {
        BillingReport coreTicketBilling = billingReportService.load(-1L);
        SortedSet<BillingResult> results = coreTicketBilling.getResults();
        List<BillingRate> rates = results.stream().flatMap(result -> result.getRates().stream())
                .collect(toList());
        assertNotNull(rates);
        BillingItem billingItem = results.stream().filter(result -> result.getId() == -1)
                .map(BillingResult::getItem).findFirst().orElse(null);
        assertNotNull(billingItem);
    }

    @Test
    public void testFindAllWithResults() {
        Set<BillingReport> distinctByActiveTrue = billingReportService.loadActiveWithResults().stream()
                .filter(billingReport -> billingReport.getId() < 0).collect(toSet());
        assertEquals(2, distinctByActiveTrue.size());
        List<BillingRate> rates = distinctByActiveTrue.stream()
                .flatMap(query -> query.getResults().stream())
                .flatMap(result -> result.getRates().stream())
                .collect(toList());
        assertNotNull(rates);
        assertEquals(6, rates.size());
    }
}

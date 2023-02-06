package ru.yandex.market.mbo.billing.report;

import java.util.Arrays;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import ru.yandex.market.mbo.billing.report.personal.PersonalBilling;
import ru.yandex.market.mbo.billing.report.personal.PersonalBillingFilter;
import ru.yandex.market.mbo.billing.report.personal.PersonalBillingReportServiceImpl;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static ru.yandex.market.mbo.billing.report.personal.PersonalBillingFilter.OrderedColumn.PRICE;

@SuppressWarnings("checkstyle:magicNumber")
public class PersonalBillingReportServiceImplTest {

    @InjectMocks
    private PersonalBillingReportServiceImpl service;
    @Mock
    private NamedParameterJdbcTemplate jdbcTemplate;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        when(jdbcTemplate.query(anyString(), anyMap(), (RowMapper) any())).thenReturn(Arrays.asList(
            new PersonalBilling().setUserId(12345L).setOperationId(1L),
            new PersonalBilling().setUserId(12345L).setOperationId(2L),
            new PersonalBilling().setUserId(12345L).setOperationId(3L)
        ));
        when(jdbcTemplate.queryForObject(anyString(), anyMap(), eq(Integer.class))).thenReturn(3);
        when(jdbcTemplate.queryForObject(anyString(), anyMap(), eq(Double.class))).thenReturn(300.0);
    }

    @Test
    public void findByFilters() {
        List<PersonalBilling> results = service.findByFilters(new PersonalBillingFilter()
            .setOperatorId(12354L)
            .setOrderedColumn(PRICE));
        assertEquals(3, results.size());

        ArgumentCaptor<String> query = ArgumentCaptor.forClass(String.class);
        verify(jdbcTemplate, times(1)).query(query.capture(), anyMap(), (RowMapper) any());
        String expectedQuery = "SELECT /*+ no_merge */ * " +
            "FROM v_ng_billing_report " +
            "WHERE USER_ID = :userId " +
            "and FINISHED_TIME >= :startTime " +
            "and FINISHED_TIME <= :finishTime " +
            "ORDER BY PRICE ASC " +
            "OFFSET 0 ROWS FETCH NEXT 1000 ROWS ONLY";
        assertEquals(expectedQuery, query.getValue().replace("\n", ""));
    }

    @Test
    public void countByFilter() {
        int count = service.countByFilter(new PersonalBillingFilter().setOperatorId(12354L));
        assertEquals(3, count);

        ArgumentCaptor<String> query = ArgumentCaptor.forClass(String.class);
        verify(jdbcTemplate, times(1)).queryForObject(query.capture(), anyMap(), eq(Integer.class));
        String expectedQuery = "SELECT COUNT(*) " +
            "FROM v_ng_billing_report " +
            "WHERE USER_ID = :userId " +
            "and FINISHED_TIME >= :startTime " +
            "and FINISHED_TIME <= :finishTime ";
        assertEquals(expectedQuery, query.getValue().replace("\n", ""));
    }

    @Test
    public void calcPaymentByFilter() {
        double payment = service.calcPaymentByFilter(new PersonalBillingFilter().setOperatorId(12354L));
        assertEquals(300.0, payment, 0.01);

        ArgumentCaptor<String> query = ArgumentCaptor.forClass(String.class);
        verify(jdbcTemplate, times(1)).queryForObject(query.capture(), anyMap(), eq(Double.class));
        String expectedQuery = "SELECT SUM(price) " +
            "FROM v_ng_billing_report " +
            "WHERE USER_ID = :userId " +
            "and FINISHED_TIME >= :startTime " +
            "and FINISHED_TIME <= :finishTime ";
        assertEquals(expectedQuery, query.getValue().replace("\n", ""));
    }

    @Test
    public void statsForPayments() {
        service.statsForPayments(12345L);
        ArgumentCaptor<String> query = ArgumentCaptor.forClass(String.class);
        verify(jdbcTemplate, times(1)).queryForObject(query.capture(), anyMap(), (RowMapper) any());
        String expectedQuery = "select y.sum as yesterday, p.sum as period, m.sum as month " +
            "from" +
            "  (select SUM(price) as sum" +
            "     from v_ng_billing_report" +
            "     where USER_ID = :userId" +
            "       and FINISHED_TIME = :yesterday) y," +
            "  (select SUM(price) as sum" +
            "     from v_ng_billing_report" +
            "     where USER_ID = :userId" +
            "       and FINISHED_TIME >= :startPeriod" +
            "       and FINISHED_TIME <= :today) p," +
            "  (select SUM(price) as sum" +
            "     from v_ng_billing_report" +
            "     where USER_ID = :userId" +
            "       and FINISHED_TIME >= :startMonth" +
            "       and FINISHED_TIME <= :today) m";
        assertEquals(expectedQuery, query.getValue());
    }
}

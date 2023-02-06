package ru.yandex.market.loyalty.core.config.trace;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.sql.DataSource;

import net.ttddyy.dsproxy.support.ProxyDataSource;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.loyalty.core.dao.coin.CoinDao;
import ru.yandex.market.loyalty.core.test.MarketLoyaltyCoreMockedDbTestBase;
import ru.yandex.market.request.datasource.trace.DataSourceTraceUtil;
import ru.yandex.market.request.trace.Module;
import ru.yandex.market.request.trace.RequestLogRecordBuilder;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.stringContainsInOrder;
import static ru.yandex.market.loyalty.core.config.trace.LoyaltyTraceQueryExecutionListener.MARK_KEY;
import static ru.yandex.market.loyalty.core.config.trace.LoyaltyTraceQueryExecutionListener.UNRECOGNIZED_SOURCE;

public class DatasourceTraceUtilsTest extends MarketLoyaltyCoreMockedDbTestBase {
    private static final String TEST_SQL = "select deleted from reference "
            + "where name in (?, ?, ?) and id = ? "
            + "and key in (values(?),(?),(?),(?),(?),(?),(?),(?),(?),(?),(?))";

    @Autowired
    private DataSource dataSource;
    @Autowired
    protected TransactionTemplate transactionTemplate;

    private ProxyDataSource wrappedDataSource;
    private List<RequestLogRecordBuilder> records;

    @Before
    public void initEach() {
        records = new ArrayList<>();

        var config = new DataSourceTraceUtil.Config();
        config.setLoggerConsumer(records::add);
        config.setLogEnabledSupplier(() -> true);
        wrappedDataSource = DatasourceTraceUtils.wrap(dataSource, Module.PGAAS, () -> true, config);
    }

    @Test
    public void shouldAddUnrecognizedSourceMarkToRecord() throws Exception {
        var testSql = "select * from loyalty_config";
        try(var statement = wrappedDataSource.getConnection().createStatement()) {
            statement.executeQuery(testSql);
        }
        var cache = LoyaltyTraceQueryExecutionListener.getSqlToMethodCache();
        assertThat(cache.get(testSql), equalTo(UNRECOGNIZED_SOURCE));

        assertThat(records.size(), equalTo(1));
        assertThat(records.get(0).build(), stringContainsInOrder(MARK_KEY, UNRECOGNIZED_SOURCE));
    }

    @Test
    public void shouldAddDaoSourceMarkToRecord() {
        var mark = "CoinDao.getInsertResultsBySourceKeys";
        var coinDao = new CoinDao(
                new JdbcTemplate(wrappedDataSource),
                new NamedParameterJdbcTemplate(wrappedDataSource),
                clock,
                configurationService,
                transactionTemplate
        );

        coinDao.getInsertResultsBySourceKeys(Set.of("1"));

        var sql = CoinDao.SELECT_INSERT_RESULT.replaceAll("\\(.*\\)", "");
        var cache = LoyaltyTraceQueryExecutionListener.getSqlToMethodCache();
        assertThat(cache.get(sql), equalTo(mark));

        assertThat(records.size(), equalTo(1));
        assertThat(records.get(0).build(), stringContainsInOrder(MARK_KEY, mark));
    }

    @Test
    public void shouldTrimSqlCorrect() {
        var expected = "select deleted from reference where name in and id = and key in ()";
        assertThat(LoyaltyTraceQueryExecutionListener.trimSql(TEST_SQL), equalTo(expected));
    }

    @Test
    public void shouldTrimLongSqlWithoutStackOverFlowError() {
        var expected = "SELECT DH.ID FROM DISCOUNT_HISTORY DH WHERE DH.DISCOUNT_ID IN () ORDER BY DH.ID DESC";

        int limit = 10_000;
        String longSql =
                "SELECT DH.ID FROM DISCOUNT_HISTORY DH WHERE DH.DISCOUNT_ID IN (VALUES" + Stream.generate(() -> "(?)")
                .limit(limit)
                .collect(Collectors.joining(",")) + ") ORDER BY DH.ID DESC";
        assertThat(LoyaltyTraceQueryExecutionListener.trimSql(longSql), equalTo(expected));
    }
}

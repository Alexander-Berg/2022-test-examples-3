package ru.yandex.market.billing.tasks;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.google.common.collect.ImmutableList;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.bolts.collection.impl.DefaultMapF;
import ru.yandex.inside.yt.kosher.Yt;
import ru.yandex.inside.yt.kosher.impl.ytree.YTreeStringNodeImpl;
import ru.yandex.inside.yt.kosher.ytree.YTreeNode;
import ru.yandex.inside.yt.kosher.ytree.YTreeStringNode;
import ru.yandex.market.billing.FunctionalTest;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.mbi.environment.EnvironmentService;
import ru.yandex.market.mbi.tms.jobs.YqlLoaderColumn;

class GetShowsExecutorTest extends FunctionalTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Autowired
    private EnvironmentService environmentService;

    private GetShowsExecutor getShowsExecutor;

    /**
     * Проверяет, что показы правильно агрегируются и импортируются.
     */
    @Test
    @DbUnitDataSet(
            before = "getShowsExecutorTest.before.csv",
            after = "getShowsExecutorTest.after.csv")
    void testGetShows() {
        getShowsExecutor = getShowsExecutor(jdbcTemplate, transactionTemplate);
        getShowsExecutor.doJob(null);
    }

    private GetShowsExecutor getShowsExecutor(JdbcTemplate jdbcTemplate,
                                              TransactionTemplate transactionTemplate) {
        GetShowsExecutor executor = new MockGetShowsExecutor();
        executor.setBatchSize(2);
        executor.setDaysBefore(10000);
        executor.setEnvironmentService(environmentService);
        executor.setJdbcTemplate(jdbcTemplate);
        executor.setYqlJdbcTemplate(jdbcTemplate);
        executor.setTransactionTemplate(transactionTemplate);
        executor.setYt(yt());
        return executor;
    }

    private Yt yt() {
        Yt yt = Mockito.mock(Yt.class, Mockito.RETURNS_DEEP_STUBS);
        List<YTreeStringNode> list = new ArrayList<>();
        list.add(new YTreeStringNodeImpl("2017-01-01", DefaultMapF.wrap(new HashMap<String, YTreeNode>() {{
            put("modification_time", new YTreeStringNodeImpl("2017-01-02T00:01:00.12345Z", null));
        }})));
        list.add(new YTreeStringNodeImpl("2017-01-02", DefaultMapF.wrap(new HashMap<String, YTreeNode>() {{
            put("modification_time", new YTreeStringNodeImpl("2017-01-04T00:01:00.12345Z", null));
        }})));
        list.add(new YTreeStringNodeImpl("2017-01-03", DefaultMapF.wrap(new HashMap<String, YTreeNode>() {{
            put("modification_time", new YTreeStringNodeImpl("2017-01-04T00:01:00.12345Z", null));
        }})));
        Mockito.when(yt.cypress().list(Mockito.any(), Mockito.anyCollection())).thenReturn(list);
        return yt;
    }

    private class MockGetShowsExecutor extends GetShowsExecutor {
        @Override
        protected String getYqlTodayFromClause() {
            return "(select * from market_billing.yql_shows where event_time = date'2017-01-04') someAlias";
        }

        @Override
        protected String getYqlHistoryFromClause(LocalDate date) {
            return "(select * from market_billing.yql_shows where event_time = date'" + DATE_FORMATTER.format(date) + "') someAlias";
        }

        @Override
        protected List<YqlLoaderColumn> getColumns() {
            return ImmutableList.<YqlLoaderColumn>builder()
                    .add(new YqlLoaderColumn("1 as showtime_date", "showtime"))
                    .add(new YqlLoaderColumn("campaign_id", "campaign_id", "campaign_id"))
                    .add(new YqlLoaderColumn("pp", "pp", "pp"))
                    .add(new YqlLoaderColumn("count(*) as count", "count"))
                    .build();
        }

        @Override
        protected LocalDate today() {
            return LocalDate.parse("2017-01-04", DATE_FORMATTER);
        }
    }
}

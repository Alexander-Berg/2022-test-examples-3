package ru.yandex.market.antifraud.yql.validate.filter;

import com.google.common.collect.ImmutableSet;
import lombok.extern.slf4j.Slf4j;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import ru.yandex.market.antifraud.db.LoggingJdbcTemplate;
import ru.yandex.market.antifraud.filter.ClicksAndShows;
import ru.yandex.market.antifraud.filter.RndUtil;
import ru.yandex.market.antifraud.filter.TestClick;
import ru.yandex.market.antifraud.filter.TestShow;
import ru.yandex.market.antifraud.filter.fields.FilterConstants;
import ru.yandex.market.antifraud.filter.generators.ForShowsFilters;
import ru.yandex.market.antifraud.yql.config.IntegrationTestAntifraudYqlConfig;
import ru.yandex.market.antifraud.yql.model.UnvalidatedDay;
import ru.yandex.market.antifraud.yql.model.YqlSession;
import ru.yandex.market.antifraud.yql.model.YtConfig;
import ru.yandex.market.antifraud.yql.validate.YtTestDataGenerator;
import ru.yandex.market.antifraud.yql.yt.YtTablesHelper;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import static com.google.common.base.Preconditions.checkArgument;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {IntegrationTestAntifraudYqlConfig.class})
@ActiveProfiles("integration-tests")
@Slf4j
public class ShowFiltersITest {
    @Autowired
    private LoggingJdbcTemplate yqlJdbcTemplate;

    @Autowired
    private YtTablesHelper ytTablesHelper;

    @Autowired
    private YtTestDataGenerator testDataGenerator;

    @Autowired
    private YtConfig ytConfig;

    @Autowired
    private YqlFilterSet showsFilterSet;

    @Before
    public void initTestData() {
        testDataGenerator.initOnce();
    }

    private final ForShowsFilters forShowsFilters = new ForShowsFilters();

    @Test
    public void f02_04() {
        List<TestShow> shows = new ArrayList<>();
        shows.addAll(forShowsFilters.generateForNoneFilter());
        shows.addAll(forShowsFilters.generateFor02Filter());
        shows.addAll(forShowsFilters.generateFor04Filter());
        filterToRowid(shows);
        String partition = saveShows(shows);
        YqlSession session = createSessionWithPartition(partition);
        getFilterById(2).apply(session);
        getFilterById(4).apply(session);
        Map<String, Set<Long>> filtersResult = getFiltersResult(session);
        checkFiltersResult(filtersResult, partition, session);
    }

    @Test
    public void f08() {
        List<TestShow> shows = new ArrayList<>();
        shows.addAll(forShowsFilters.generateForNoneFilter());
        shows.addAll(forShowsFilters.generateFor08Filter());
        shows.addAll(forShowsFilters.generateFor08FilterEmptyCookie());
        filterToRowid(shows);
        String partition = saveShows(shows);
        YqlSession session = createSessionWithPartition(partition);
        getFilterById(8).apply(session);
        Map<String, Set<Long>> filtersResult = getFiltersResult(session);
        checkFiltersResult(filtersResult, partition, session);
    }

    @Test
    public void f09() {
        List<TestShow> shows = new ArrayList<>();
        shows.addAll(forShowsFilters.generateForNoneFilter());
        shows.addAll(forShowsFilters.generateFor09Filter());
        filterToRowid(shows);
        String partition = saveShows(shows);
        YqlSession session = createSessionWithPartition(partition);
        getFilterById(9).apply(session);
        Map<String, Set<Long>> filtersResult = getFiltersResult(session);
        checkFiltersResult(filtersResult, partition, session);
    }

    @Test
    public void f10() {
        List<TestShow> shows = new ArrayList<>();
        shows.addAll(forShowsFilters.generateForNoneFilter());
        shows.addAll(forShowsFilters.generateFor10Filter());
        filterToRowid(shows);
        String partition = saveShows(shows);
        YqlSession session = createSessionWithPartition(partition);
        getFilterById(10).apply(session);
        Map<String, Set<Long>> filtersResult = getFiltersResult(session);
        checkFiltersResult(filtersResult, partition, session);
    }

    @Test
    public void f11() {
        List<TestShow> shows = new ArrayList<>();
        shows.addAll(forShowsFilters.generateForNoneFilter());
        shows.addAll(forShowsFilters.generateFor11Filter());
        filterToRowid(shows);
        String partition = saveShows(shows);
        YqlSession session = createSessionWithPartition(partition);
        getFilterById(11).apply(session);
        Map<String, Set<Long>> filtersResult = getFiltersResult(session);
        checkFiltersResult(filtersResult, partition, session);
    }

    @Test
    public void f12() {
        List<TestShow> shows = new ArrayList<>();
        shows.addAll(forShowsFilters.generateForNoneFilter());
        ClicksAndShows f12data = forShowsFilters.generateFor12Filter();
        shows.addAll(f12data.getShows());
        filterToRowid(shows);
        String clicksPartition = saveClicks(f12data.getClicks());
        String showsPartition = saveShows(shows);
        YqlSession session = createSessionWithPartition(
            showsPartition, clicksPartition);
        getFilterById(12).apply(session);
        Map<String, Set<Long>> filtersResult = getFiltersResult(session);
        checkFiltersResult(filtersResult, showsPartition, session);
    }

    @Test
    public void f13() {
        List<TestShow> shows = new ArrayList<>();
        shows.addAll(forShowsFilters.generateForNoneFilter());
        shows.addAll(forShowsFilters.generateFor13Filter());
        shows.addAll(forShowsFilters.generateFor13FilterInstead9EmptyCookie());
        shows.addAll(forShowsFilters.generateFor13FilterInsteadOf10());
        shows.addAll(forShowsFilters.generateFor13FilterInsteadOf11());
        filterToRowid(shows);
        String partition = saveShows(shows);
        YqlSession session = createSessionWithPartition(partition);
        getFilterById(13).apply(session);
        Map<String, Set<Long>> filtersResult = getFiltersResult(session);
        checkFiltersResult(filtersResult, partition, session);
    }

    @Test
    @Ignore("too long")
    public void f_all() {
        List<TestShow> shows = forShowsFilters.generateForAllFilters();
        ClicksAndShows f12data = forShowsFilters.generateFor12Filter();
        shows.addAll(f12data.getShows());
        filterToRowid(shows);
        String showsPartition = saveShows(shows);
        // FIXME: use clicks partition for 12 filter
        String clicksPartition = saveClicks(f12data.getClicks());

        YqlSession session = createSessionWithPartition(showsPartition, clicksPartition);
        Arrays.asList(2, 4, 8, 9, 10, 11, 12, 13).stream()
            .map((filterId) -> getFilterById(filterId))
            .forEach((filter) -> filter.apply(session));

        Map<String, Set<Long>> filtersResult = getFiltersResult(session);
        checkFiltersResult(filtersResult, showsPartition, session);
    }

    private void checkFiltersResult(Map<String, Set<Long>> result, String partitions, YqlSession session) {
        StringBuilder errors = new StringBuilder(4096);
        // checks both: filtered and untouched
        for(Map.Entry<String, Set<Long>> e: result.entrySet()) {
            String filterId = filterFromRowid(e.getKey());
            if(!e.getValue().contains(filterId)) {
                errors.append(e.getKey())
                    .append(',')
                    .append(e.getValue())
                    .append('\n');
            }
        }

        assertTrue("Generated shows " + partitions + "\n" +
            "Generated rollbacks " + session.getTmpRollbacksFile() + "\n" +
            errors.toString(), errors.length() == 0);
    }

    private Map<String, Set<Long>> getFiltersResult(YqlSession session) {
        Map<String, Set<Long>> rowidToFilters = new HashMap<>();
        yqlJdbcTemplate.query("" +
                "select rowid, filter " +
                "from CONCAT('" + session.getTmpRollbacksFile() + "')",
            rs -> {
                String rowid = rs.getString("rowid");
                long filter = rs.getLong("filter");
                rowidToFilters.computeIfAbsent(
                    rowid,
                    (r) -> new HashSet<>()).add(filter);
            });
        return rowidToFilters;
    }

    private YqlFilter getFilterById(long filterId) {
        for(YqlFilter filter: showsFilterSet.getFilters()) {
            if(filter instanceof YqlSqlFilter && filterId == filter.getId()) {
                return filter;
            }

            if(filter instanceof YqlCompositeRowFilter && ((YqlCompositeRowFilter) filter).getIds().contains(filterId)) {
                return filter;
            }
        }

        throw new RuntimeException("Filter '" + filterId + "' not found");
    }

    private YqlSession createSessionWithPartition(String showsPartition) {
        return createSessionWithPartition(showsPartition, null);
    }

    private YqlSession createSessionWithPartition(String showsPartition, String clicksPartition) {
        String tmpRollbacks = createTmpRollbacksTableName();
        YqlSession yqlSession = mock(YqlSession.class);
        UnvalidatedDay unvalidatedDay = mock(UnvalidatedDay.class);
        when(yqlSession.getDay()).thenReturn(unvalidatedDay);
        when(yqlSession.getTmpRollbacksFile()).thenReturn(tmpRollbacks);
        when(yqlSession.getPartitionsForQuery()).thenReturn(" CONCAT('" + showsPartition + "') ");
        when(yqlSession.getPrevDayPartitions()).thenReturn(Arrays.asList(showsPartition));
        when(yqlSession.getDependencyPartitions()).thenReturn(Arrays.asList(
            clicksPartition
        ));
        when(yqlSession.getPartitions()).thenReturn(Arrays.asList(showsPartition));
        when(yqlSession.getPoolPragma()).thenReturn("");
        when(unvalidatedDay.getDay()).thenReturn(-1);
        when(unvalidatedDay.getScale()).thenReturn(UnvalidatedDay.Scale.ARCHIVE);

        return yqlSession;
    }

    private String saveShows(List<TestShow> shows) {
        String tblName = createShowsTableName();
        checkArgument(!ytTablesHelper.exists(tblName));
        yqlJdbcTemplate.batchInsert(
            shows.size(),
            new TreeSet<>(TestShow.fieldToCol.values()),
            "insert into `" + tblName + "` %columns% values %value_placeholders%",
            (row, column) -> TestShow.getFieldValueByCol(shows.get(row), column));
        return tblName;
    }

    private String saveClicks(List<TestClick> clicks) {
        String tblName = createClicksTableName();
        checkArgument(!ytTablesHelper.exists(tblName));
        SortedSet<String> columns = new TreeSet<>(ImmutableSet.of(
            "rowid", "show_block_id", "pp", "yandex_uid", "filter"
        ));
        yqlJdbcTemplate.batchInsert(
            clicks.size(),
            columns,
            "insert into `" + tblName + "` %columns% values %value_placeholders%",
            (row, column) -> clicks.get(row).get(column));
        return tblName;
    }

    private String createShowsTableName() {
        // no need to cleanup this shit, AfRootDir is temporary
        return ytConfig.getAfRootDir() + "/shows_" + RndUtil.randomAlphabetic(12);
    }

    private String createClicksTableName() {
        // no need to cleanup this shit, AfRootDir is temporary
        return ytConfig.getAfRootDir() + "/clicks_" + RndUtil.randomAlphabetic(12);
    }

    private String createTmpRollbacksTableName() {
        // no need to cleanup this shit, AfRootDir is temporary
        return ytConfig.getAfRootDir() + "/shows_rollbacks_" + RndUtil.randomAlphabetic(12);
    }


    private void filterToRowid(List<TestShow> shows) {
        for(TestShow show: shows) {
            show.setRowid(show.getRowid() + "_f" + show.getFilter().id());
            show.setFilter(FilterConstants.FILTER_0);
        }
    }

    private String filterFromRowid(String rowid) {
        String parts[] = rowid.split("_");
        if(parts.length <= 1) {
            throw new RuntimeException("No filter id in rowid (no _) " + rowid);
        }
        String filterIdPart = parts[parts.length - 1];
        if(filterIdPart.charAt(0) != 'f') {
            throw new RuntimeException("No filter id in rowid (no f) " + rowid);
        }

        return filterIdPart.substring(1);
    }
}

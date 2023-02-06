package ru.yandex.market.antifraud.yql.validate;

import com.google.common.base.Joiner;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import ru.yandex.market.antifraud.db.LoggingJdbcTemplate;
import ru.yandex.market.antifraud.filter.RndUtil;
import ru.yandex.market.antifraud.model.SessionStatusEnum;
import ru.yandex.market.antifraud.yql.config.IntegrationTestAntifraudYqlConfig;
import ru.yandex.market.antifraud.yql.model.UnvalidatedDay;
import ru.yandex.market.antifraud.yql.model.YqlSession;
import ru.yandex.market.antifraud.yql.model.YtConfig;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.google.common.base.Preconditions.checkArgument;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {IntegrationTestAntifraudYqlConfig.class})
@ActiveProfiles("integration-tests")
public class YqlJoinWithDataITest {
    @Autowired
    private LoggingJdbcTemplate yqlJdbcTemplate;

    @Autowired
    private YqlJoinWithData joinWithData;

    @Autowired
    private YtTestDataGenerator testDataGenerator;

    @Autowired
    private LoggingJdbcTemplate jdbcTemplate;

    @Autowired
    private YtConfig ytConfig;

    @Autowired
    private YqlValidatorHelper yqlValidatorHelper;

    @Before
    public void prepareTestData() {
        testDataGenerator.initOnce();
    }

    @Test
    public void mustFailSessionOnRecentPartitionLoss() throws InterruptedException {
        YqlSession session = session(new UnvalidatedDay(
                testDataGenerator.getRecentDay(), 0, UnvalidatedDay.Scale.RECENT));
        session = Mockito.spy(session);
        Mockito.when(session.getPartitions()).thenReturn(Arrays.asList("//nonexistant_yt_dir"));
        joinWithData.joinWithData(session);
        assertThat(jdbcTemplate.query("select status from sessions where session_id = :session_id",
                "session_id", session.getId(),
                String.class),
                is(SessionStatusEnum.FAILED.toString()));
    }

    @Test
    public void a_testOfGenerateAndSaveDaytable() {
        String dayRollbacksPath = ytConfig.getRollbacksDir(testDataGenerator.log()) + "/2017-10-30_test_gsd";

                YqlSession session = Mockito.mock(YqlSession.class);
        Mockito.when(session.getPartitionsForQuery()).thenReturn("[//home/market/testing/mstat/yqlaf/test_data/market-new-shows-log/1d/2017-10-30]");

        Mockito.when(session.getRollbacksDayTable()).thenReturn(dayRollbacksPath);

        Set<Rollback> r = generateRollbacks(session, 200, 60);
        generateAndSaveDaytable(session, r, 50, 50);

        assertThat(yqlJdbcTemplate.query(
                "select count(*) from CONCAT('" + dayRollbacksPath + "')", Long.class),
                is(50L));
    }

    // each of the folowing test runs ~7m =(, most of this time takes joinWithData itself
    @Test
    public void testRecentWithUnmergedAndWithDaytable() throws InterruptedException {
        YqlSession session = session(new UnvalidatedDay(
                testDataGenerator.getRecentDay(), 0, UnvalidatedDay.Scale.RECENT));
        Set<Rollback> rollbacks = generateRollbacks(session, 200, 60);
        saveRollbacks(session, rollbacks);
        Set<Rollback> unmergedRollbacks = generateAndSaveUnmerged(session, rollbacks, 50);
        Set<Rollback> daytableRollbacks = generateAndSaveDaytable(session, rollbacks, 50, 50);
        joinWithData.joinWithData(session);
        rollbacks.removeAll(unmergedRollbacks);
        rollbacks.removeAll(daytableRollbacks);
        assertRollbacksJoined(session, rollbacks);
    }

    @Test
    public void testRecentWithoutUnmergedAndWithoutDaytable() throws InterruptedException {
        YqlSession session = session(new UnvalidatedDay(
                testDataGenerator.getRecentDay(), 0, UnvalidatedDay.Scale.RECENT));
        Set<Rollback> rollbacks = generateRollbacks(session, 100, 30);
        saveRollbacks(session, rollbacks);
        joinWithData.joinWithData(session);
        assertRollbacksJoined(session, rollbacks);
    }

    @Test
    public void testArchiveWithoutUnmergedAndWithoutDaytable() throws InterruptedException {
        YqlSession session = session(new UnvalidatedDay(
                testDataGenerator.getArchiveDay(), 0, UnvalidatedDay.Scale.ARCHIVE));
        Set<Rollback> rollbacks = generateRollbacks(session, 100, 30);
        saveRollbacks(session, rollbacks);
        joinWithData.joinWithData(session);
        assertRollbacksJoined(session, rollbacks);
    }

    @Test
    public void testArchiveWithUnmergedAndWithoutDaytable() throws InterruptedException {
        YqlSession session = session(new UnvalidatedDay(
                testDataGenerator.getArchiveDay(), 0, UnvalidatedDay.Scale.ARCHIVE));
        Set<Rollback> rollbacks = generateRollbacks(session, 100, 30);
        saveRollbacks(session, rollbacks);
        Set<Rollback> unmergedRollbacks = generateAndSaveUnmerged(session, rollbacks, 50);
        joinWithData.joinWithData(session);
        rollbacks.removeAll(unmergedRollbacks);
        assertRollbacksJoined(session, rollbacks);
    }

    private YqlSession session(UnvalidatedDay d) {
        YqlSession session = yqlValidatorHelper.createSession(d);
        yqlValidatorHelper.sessionFiltersExecuted(session);
        return session;
    }

    @EqualsAndHashCode(of = "rowid")
    @ToString(includeFieldNames = false)
    private static class Rollback {
        String rowid;
        String filter;

        Rollback(String rowid, int filter) {
            this.rowid = rowid;
            this.filter = Integer.toString(filter);
        }

        Rollback(String rowid, String filter) {
            this.rowid = rowid;
            this.filter = filter;
        }

        int intFilter() {
            return Integer.parseInt(filter);
        }
    }

    private Set<Rollback> generateRollbacks(YqlSession session, int qty, int dupes) {
        checkArgument(dupes <= qty);

        List<String> realRowids = new ArrayList<>(qty);
        // acts like stupid filter selecting first %{qty} rows
        yqlJdbcTemplate.query("select rowid from " + session.getPartitionsForQuery() + " limit " + qty,
                (rs) -> {
                    realRowids.add(rs.getString("rowid"));
                });

        checkArgument(realRowids.size() == qty);

        List<Rollback> rollbacks = new ArrayList<>(qty + dupes);
        for(int i = 0; i < qty; i++) {
            rollbacks.add(new Rollback(realRowids.get(i), RndUtil.nextInt(3) + 1));
        }

        for(int i = 0; i < dupes; i++) {
            rollbacks.add(new Rollback(rollbacks.get(i).rowid, rollbacks.get(i).intFilter() + 1));
        }

        return new HashSet<>(rollbacks);
    }

    private void saveRollbacks(YqlSession session, Set<Rollback> rollbacks) {
        List<String> values = new ArrayList<>();
        for(Rollback rollback: rollbacks) {
            values.add("('" + rollback.rowid + "', " + rollback.filter + ")");
        }
        yqlJdbcTemplate.exec("insert into `" + session.getFinalRollbacksFile() + "` " +
                "(rowid, filter) values " + Joiner.on(",\n").join(values));
    }

    private void assertRollbacksJoined(YqlSession session, Set<Rollback> rollbacks) {
        assertThat(jdbcTemplate.query("select status from sessions where session_id = :session_id",
                "session_id", session.getId(),
                String.class), is(SessionStatusEnum.DATA_READY.toString()));
        Set<Rollback> dataJoinedRollbacks = new HashSet<>();
        yqlJdbcTemplate.query("select rowid, String::JoinFromList(ListFlatMap(filter_list, 'ToString'), ',') as fl " +
                        "from CONCAT('" + session.getDataReadyFile() + "')",
                (rs) -> {
                    String filters[] = rs.getString("fl").split(",");
                    for(String filter: filters) {
                        dataJoinedRollbacks.add(new Rollback(
                                rs.getString("rowid"),
                                filter
                        ));
                    }
                });
        assertThat(dataJoinedRollbacks, is(rollbacks));
    }

    private Set<Rollback> generateAndSaveUnmerged(YqlSession session, Set<Rollback> rollbacks, int qty) {
        checkArgument(qty <= rollbacks.size());

        List<String> values = new ArrayList<>();
        Set<Rollback> unmerged = new HashSet<>();
        for(Rollback rollback: rollbacks) {
            if(qty-- <= 0) {
                break;
            }
            unmerged.add(rollback);
            values.add("('" + rollback.rowid + "', " + rollback.filter + ")");
        }
        yqlJdbcTemplate.exec("insert into `" + session.getDataReadyFile() + "_unmerged_chunk` " +
                "(rowid, filter) values " + Joiner.on(",\n").join(values));
        return unmerged;
    }

    private Set<Rollback> generateAndSaveDaytable(YqlSession session, Set<Rollback> rollbacks, int qty, int offset) {
        int limit = qty + offset;
        checkArgument(limit <= rollbacks.size());
        List<String> values = new ArrayList<>();
        Set<Rollback> merged = new HashSet<>();
        int i = 0;
        for(Rollback rollback: rollbacks) {
            i++;
            if(i <= offset) {
                continue;
            }
            if(i > limit) {
                break;
            }
            merged.add(rollback);
            values.add("('" + rollback.rowid + "', " + rollback.filter + ")");
        }
        yqlJdbcTemplate.exec("insert into `" + session.getRollbacksDayTable() + "` " +
                "(rowid, filter) values " + Joiner.on(",\n").join(values));
        return merged;
    }
}

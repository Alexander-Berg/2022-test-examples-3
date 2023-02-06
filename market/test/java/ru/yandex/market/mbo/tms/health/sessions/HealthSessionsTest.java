package ru.yandex.market.mbo.tms.health.sessions;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Range;
import org.hamcrest.MatcherAssert;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.util.ReflectionTestUtils;
import ru.yandex.inside.yt.kosher.Yt;
import ru.yandex.inside.yt.kosher.cypress.CypressNodeType;
import ru.yandex.inside.yt.kosher.cypress.YPath;
import ru.yandex.inside.yt.kosher.ytree.YTreeStringNode;
import ru.yandex.market.mbo.gwt.models.visual.TovarCategory;
import ru.yandex.market.mbo.synchronizer.export.modelstorage.BaseCategoryModelsExtractorTestClass;
import ru.yandex.market.mbo.synchronizer.export.modelstorage.mapreduce.YtExportMapReduceService;
import ru.yandex.market.mbo.synchronizer.export.modelstorage.mapreduce.YtExportModelsTableService;
import ru.yandex.market.mbo.synchronizer.export.modelstorage.mapreduce.util.ExportMapReduceUtil;
import ru.yandex.market.mbo.tms.health.YtHealthMapReduceService;
import ru.yandex.market.mbo.tms.health.published.guru.GuruCounterSupply;
import ru.yandex.market.mbo.user.AutoUser;
import ru.yandex.market.mbo.yt.TestCypress;
import ru.yandex.market.mbo.yt.TestYt;

import javax.annotation.Nullable;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static org.hamcrest.Matchers.containsInAnyOrder;

/**
 * @author york
 * @since 24.04.2018
 */
@SuppressWarnings("checkstyle:MagicNumber")
public class HealthSessionsTest extends BaseCategoryModelsExtractorTestClass {
    private static final YPath JOB_ROOT_PATH = YPath.simple("//home/york");
    private static final YPath TMP_PATH = YPath.simple("//tmp");
    private static final String MAP_REDUCE_POOL = "mapReducePool";

    private TestCypress cypressStub;
    private YtHealthMapReduceServiceExt ytHealthMapReduceService;

    @Before
    public void init() {
        TestYt yt = new TestYt();
        cypressStub = yt.cypress();
        cypressStub.create(TMP_PATH, CypressNodeType.MAP);
        cypressStub.create(YPath.simple("//home/some/path"), CypressNodeType.MAP);
        cypressStub.create(YPath.simple("//home/some/path2"), CypressNodeType.MAP);
        cypressStub.create(YPath.simple("//home/some/path/tmbewr"), CypressNodeType.TABLE);

        JdbcTemplate yqlJdbcTemplate = Mockito.mock(JdbcTemplate.class);
        ytHealthMapReduceService = new YtHealthMapReduceServiceExt(yt, ytExportModelsTableService, ytExportMRService,
            yqlJdbcTemplate, autoUser, JOB_ROOT_PATH, TMP_PATH, MAP_REDUCE_POOL);
        ReflectionTestUtils.setField(ytHealthMapReduceService, "fulfillmentDataPath", "//home/some/path");
        ReflectionTestUtils.setField(ytHealthMapReduceService, "skuMappingTablePath", "//home/some/path2");
    }

    @Test
    public void testMatchers() {
        String sessionId = "20180423_1132";

        Range<LocalDate> daily = HealthSessionsHelper.getCurrentInterval(
            HealthSessionsHelper.getSessionDate(sessionId),
            Scale.DAY);

        Assert.assertTrue(HealthSessionsHelper.matches("20180423_2359", daily));
        Assert.assertTrue(HealthSessionsHelper.matches("20180423_0000", daily));
        Assert.assertFalse(HealthSessionsHelper.matches("20180422_2300", daily));

        Range<LocalDate> week = HealthSessionsHelper.getPreviousInterval(
            HealthSessionsHelper.getSessionDate(sessionId),
            Scale.WEEK);

        Assert.assertFalse(HealthSessionsHelper.matches("20180423_0049", week));
        Assert.assertTrue(HealthSessionsHelper.matches("20180422_2359", week));
        Assert.assertTrue(HealthSessionsHelper.matches("20180416_0000", week));
        Assert.assertFalse(HealthSessionsHelper.matches("20180415_1210", week));
        Assert.assertFalse(HealthSessionsHelper.matches("20180401_2300", week));

        Range<LocalDate> month = HealthSessionsHelper.getPreviousInterval(
            HealthSessionsHelper.getSessionDate(sessionId),
            Scale.MONTH);

        Assert.assertFalse(HealthSessionsHelper.matches("20180423_0049", month));
        Assert.assertFalse(HealthSessionsHelper.matches("20180401_2300", month));
        Assert.assertTrue(HealthSessionsHelper.matches("20180301_2300", month));
        Assert.assertTrue(HealthSessionsHelper.matches("20180316_0000", month));
        Assert.assertFalse(HealthSessionsHelper.matches("20180215_1210", month));
        Assert.assertFalse(HealthSessionsHelper.matches("20180131_2300", month));
    }

    @Test
    public void testRotation() {
        LocalDate baseDate = LocalDate.of(2018, 5, 16);
        Multimap<Scale, String> allSessions = HashMultimap.create();
        allSessions.putAll(Scale.DAY, Arrays.asList("20180516_1132", "20180516_1632", "20180516_1732",
                                      "20180515_1431", "20180515_1737",
                                      "20180514_1031",
                                      "20180505_1843",
                                      "20180504_1515",
                                      "20180428_1919"
        ));
        allSessions.putAll(Scale.WEEK, Arrays.asList("20180513_1312", "20180513_1832",
            "20180512_1639",
            "20180428_1719"
        ));
        allSessions.putAll(Scale.MONTH, Arrays.asList("20180429_0012", "20180429_0132",
            "20180428_2339"
        ));
        Collection<String> noScale = new ArrayList<>(Arrays.asList("20180516_1021", "20180515_0223", "20180515_0523",
            "20180514_0123", "20180514_0423", "20180514_0612", "20180505_0612", "20180505_0642"));


        allSessions.entries().forEach(e -> createSessionInCypress(e.getValue(), e.getKey()));
        noScale.forEach(s -> createSessionInCypress(s));

        Set<String> deleted = new HashSet<>();
        //
        HealthSessionsHelper.rotateSessions(baseDate, allSessions.asMap(), noScale, deleted::add);

        allSessions.entries().removeIf(e -> deleted.contains(e.getValue()));
        noScale.removeAll(deleted);

        MatcherAssert.assertThat(allSessions.get(Scale.DAY),
            containsInAnyOrder("20180516_1132", "20180516_1632", "20180516_1732", "20180515_1737", "20180514_1031",
                "20180505_1843"));

        MatcherAssert.assertThat(allSessions.get(Scale.WEEK),
            containsInAnyOrder("20180513_1832"));

        MatcherAssert.assertThat(allSessions.get(Scale.MONTH),
            containsInAnyOrder("20180429_0132"));
    }

    @Test
    public void testRepair() throws Exception {
        final String oldWeekSessionId = "20180501_1141";
        createSessionWithRepair(oldWeekSessionId, Scale.WEEK, true);
        String daySessionId0 = "20180508_1951";
        createSessionWithRepair(daySessionId0, Scale.DAY, true);
        String daySessionId1 = "20180509_1741";
        createSessionWithRepair(daySessionId1, Scale.DAY, true);
        String yesterdaySession = "20180514_1041";
        createSessionWithRepair(yesterdaySession, Scale.DAY, true);

        checkSessions(
            Arrays.asList(daySessionId1, yesterdaySession),
            Arrays.asList(oldWeekSessionId),
            Collections.emptyList());

        final String todaySessionId = "20180515_1641";
        ytHealthMapReduceService.updateNow(todaySessionId);
        List<YtHealthMapReduceService.StatsInfo> infos = ytHealthMapReduceService.calculateStatsAndRepair(
            Collections.emptyList(),
            emptySupply(),
            Scale.WEEK);
        Assert.assertEquals(2, infos.size());
        YtHealthMapReduceService.StatsInfo prevWeekStat = infos.get(0);
        YtHealthMapReduceService.StatsInfo todayWeekStat;
        if (prevWeekStat.getSessionId().equals(todaySessionId)) {
            todayWeekStat = prevWeekStat;
            prevWeekStat = infos.get(1);
        } else {
            todayWeekStat = infos.get(1);
        }
        Assert.assertEquals(todaySessionId, todayWeekStat.getSessionId());
        Assert.assertEquals(daySessionId1, ExportMapReduceUtil.getSessionStr(todayWeekStat.getPrevTime()));
        Assert.assertEquals(daySessionId1, prevWeekStat.getSessionId());
        Assert.assertEquals(oldWeekSessionId, ExportMapReduceUtil.getSessionStr(prevWeekStat.getPrevTime()));
        infos.forEach(info -> info.getCallback().accept(true));

        final String now = "20180515_2001";
        //now only current session in result - previous was repaired
        YtHealthMapReduceService.StatsInfo info = createSessionWithRepair(now, Scale.WEEK, true);
        Assert.assertEquals(daySessionId1, ExportMapReduceUtil.getSessionStr(info.getPrevTime()));

        MatcherAssert.assertThat(ytHealthMapReduceService.listSessions(Scale.WEEK),
            containsInAnyOrder(oldWeekSessionId, daySessionId1, todaySessionId, now));

        MatcherAssert.assertThat(ytHealthMapReduceService.listSessions(Scale.DAY),
            containsInAnyOrder(daySessionId1, yesterdaySession));
    }

    @Test(expected = RuntimeException.class)
    public void testFailNoPrev() throws Exception {
        ytHealthMapReduceService.updateNow("20180425_1132");
        ytHealthMapReduceService.calculateStats(Collections.emptyList(), emptySupply(),
            Scale.DAY, true);
    }

    @Test
    public void testDaily() throws Exception {
        final String session1 = "20180425_1132";
        ytHealthMapReduceService.updateNow(session1);
        YtHealthMapReduceService.StatsInfo info = ytHealthMapReduceService.calculateStats(
            Collections.emptyList(), emptySupply(),
            Scale.DAY, false);
        Assert.assertNotNull(info);
        Assert.assertNotNull(cypressStub.get(JOB_ROOT_PATH));
        List<YTreeStringNode> sessionsInCypress = cypressStub.list(JOB_ROOT_PATH);
        Assert.assertEquals(1, sessionsInCypress.size());
        Assert.assertTrue(ytHealthMapReduceService.listSessions().isEmpty());
        Assert.assertNull(info.getPrevTime());
        info.getCallback().accept(true);

        checkSessions(Arrays.asList(session1), Collections.emptyList(), Collections.emptyList());

        //one more session - not approving it
        final String session2 = "20180425_1431";
        createSession(session2, Scale.DAY, false);
        Assert.assertNull(info.getPrevTime());
        checkSessions(Arrays.asList(session1), Collections.emptyList(), Collections.emptyList());

        //one more normal
        final String session3 = "20180425_1631";
        info = createSession(session3, Scale.DAY, false);
        Assert.assertNull(info.getPrevTime());
        info.getCallback().accept(true);
        checkSessions(Arrays.asList(session1, session3), Collections.emptyList(), Collections.emptyList());

        MatcherAssert.assertThat(getSessionsInCypress(),
            containsInAnyOrder(session1, session2, session3));

        //not approving it
        final String session4 = "20180425_1900";
        createSession(session4, Scale.DAY, false);

        //one more normal
        final String session5 = "20180426_1212";
        info = createSession(session5, Scale.DAY, false);
        Assert.assertNotNull(info.getPrevTime());
        Assert.assertEquals((Long) ExportMapReduceUtil.getSessionTime(session3), info.getPrevTime());
        info.getCallback().accept(true);

        checkSessions(Arrays.asList(session3, session5), Collections.emptyList(), Collections.emptyList());
        MatcherAssert.assertThat(getSessionsInCypress(),
            containsInAnyOrder(session3, session5, session4, session2));
    }

    @Test
    public void testChoosingPrevForWeek() throws Exception {
        createNormSession("20180421_1641", Scale.WEEK);
        checkSessions(Collections.emptyList(), Arrays.asList("20180421_1641"), Collections.emptyList());
        createNormSession("20180422_1741", Scale.DAY);
        checkSessions(Arrays.asList("20180422_1741"), Arrays.asList("20180421_1641"), Collections.emptyList());
        createNormSession("20180422_1841", Scale.DAY);
        checkSessions(Arrays.asList("20180422_1741", "20180422_1841"),
            Arrays.asList("20180421_1641"), Collections.emptyList());

        YtHealthMapReduceService.StatsInfo info = createSession("20180429_1951", Scale.WEEK, false);
        Assert.assertNotNull(info.getPrevTime());
        Assert.assertEquals("20180421_1641", ExportMapReduceUtil.getSessionStr(info.getPrevTime()));
    }

    @Test
    public void testChoosingPrevForMonth() throws Exception {
        createNormSession("20180329_1641", Scale.MONTH);
        checkSessions(Collections.emptyList(), Collections.emptyList(), Arrays.asList("20180329_1641"));
        createNormSession("20180329_1751", Scale.MONTH);
        createSession("20180329_2241", Scale.MONTH, false);

        checkSessions(Collections.emptyList(), Collections.emptyList(), Arrays.asList("20180329_1641",
            "20180329_1751"));
        createNormSession("20180330_2041", Scale.DAY);
        checkSessions(Arrays.asList("20180330_2041"), Collections.emptyList(), Arrays.asList("20180329_1751"));

        YtHealthMapReduceService.StatsInfo info = createSession("20180429_1154", Scale.MONTH, false);
        Assert.assertNotNull(info.getPrevTime());
        Assert.assertEquals("20180329_1751", ExportMapReduceUtil.getSessionStr(info.getPrevTime()));
    }

    @Test
    public void testFromOtherScale() throws Exception {
        createNormSession("20180422_1741", Scale.DAY);
        createNormSession("20180422_1841", Scale.DAY);
        checkSessions(Arrays.asList("20180422_1741", "20180422_1841"), Collections.emptyList(),
            Collections.emptyList());

        createNormSession("20180429_1851", Scale.DAY);
        //20180422_1841 should be left because no week session for that week
        checkSessions(Arrays.asList("20180429_1851", "20180422_1841"), Collections.emptyList(),
            Collections.emptyList());

        YtHealthMapReduceService.StatsInfo info = createSession("20180429_1951", Scale.WEEK, false);
        Assert.assertNotNull(info.getPrevTime());
        Assert.assertEquals("20180422_1841", ExportMapReduceUtil.getSessionStr(info.getPrevTime()));
    }

    @Test
    public void testGettingCurrentDaySessionForWeek() throws Exception {
        createNormSession("20180421_1741", Scale.DAY);
        createNormSession("20180421_1841", Scale.DAY);
        checkSessions(Arrays.asList("20180421_1741", "20180421_1841"), Collections.emptyList(),
            Collections.emptyList());
        Assert.assertEquals(2, ytHealthMapReduceService.dumpedDates.size());

        YtHealthMapReduceService.StatsInfo info = createSession("20180421_1941", Scale.WEEK, true);
        Assert.assertEquals("20180421_1841", ExportMapReduceUtil.getSessionStr(info.getTime()));
        Assert.assertNull(info.getPrevTime());
        //session not dumped
        Assert.assertEquals(2, ytHealthMapReduceService.dumpedDates.size());

        info = createSession("20180422_1515", Scale.WEEK, true);
        Assert.assertEquals("20180422_1515", ExportMapReduceUtil.getSessionStr(info.getTime()));
        Assert.assertNull(info.getPrevTime());
        Assert.assertEquals(3, ytHealthMapReduceService.dumpedDates.size());
    }

    @Test(expected = RuntimeException.class)
    public void testSimultaneousSessions() throws Exception {
        createNormSession("20180421_1741", Scale.WEEK);
        createNormSession("20180421_1741", Scale.DAY);
    }

    private YtHealthMapReduceService.StatsInfo createSessionWithRepair(String sessionId,
                                                             Scale scale, boolean approve) throws Exception {
        ytHealthMapReduceService.updateNow(sessionId);
        Collection<YtHealthMapReduceService.StatsInfo> infos = ytHealthMapReduceService.calculateStatsAndRepair(
            Collections.emptyList(), emptySupply(),
            scale);
        Assert.assertEquals(1, infos.size());
        YtHealthMapReduceService.StatsInfo info = infos.iterator().next();
        info.getCallback().accept(approve);
        return info;
    }

    private YtHealthMapReduceService.StatsInfo createSession(String sessionId,
                                                             Scale scale, boolean approve) throws Exception {
        return createSessionWithRepair(sessionId, scale, approve);
    }

    private YtHealthMapReduceService.StatsInfo createNormSession(String sessionId, Scale scale) throws Exception {
        return createSession(sessionId, scale, true);
    }

    private YPath createSessionInCypress(String sessionId) {
        YPath path = JOB_ROOT_PATH.child(sessionId);
        cypressStub.create(path, CypressNodeType.MAP);
        return path;
    }

    private void setSessionScaleInCypress(String sessionId, Scale scale) {
        YPath path = JOB_ROOT_PATH.child(sessionId);
        cypressStub.set(path.attribute(YtHealthMapReduceServiceExt.getSessionScaleKey()), scale.name());
    }

    private void createSessionInCypress(String sessionId, @Nullable Scale scale) {
        createSessionInCypress(sessionId);
        if (scale != null) {
            setSessionScaleInCypress(sessionId, scale);
        }
    }

    private List<String> getSessionsInCypress() {
        return cypressStub.list(JOB_ROOT_PATH).stream()
            .map(s -> s.getValue())
            .collect(Collectors.toList());
    }

    private void checkSessions(List<String> day, List<String> week, List<String> month) {
        MatcherAssert.assertThat(ytHealthMapReduceService.listSessions(Scale.DAY),
            containsInAnyOrder(day.toArray()));
        MatcherAssert.assertThat(ytHealthMapReduceService.listSessions(Scale.WEEK),
            containsInAnyOrder(week.toArray()));
        MatcherAssert.assertThat(ytHealthMapReduceService.listSessions(Scale.MONTH),
            containsInAnyOrder(month.toArray()));
        Assert.assertEquals(day.size() + week.size() + month.size(),
            ytHealthMapReduceService.listSessions().size());
    }

    private GuruCounterSupply emptySupply() {
        return new GuruCounterSupply(Collections.emptySet(), Collections.emptySet(),
            Collections.emptyMap(), Collections.emptyMap(), Collections.emptyMap());
    }

    private static class YtHealthMapReduceServiceExt extends YtHealthMapReduceService {
        Date now;

        List<Date> dumpedDates = new ArrayList<>();

        @SuppressWarnings("checkstyle:ParameterNumber")
        YtHealthMapReduceServiceExt(Yt yt,
                                    YtExportModelsTableService ytExportModelsTableService,
                                    YtExportMapReduceService ytExportMapReduceService,
                                    JdbcTemplate yqlJdbcTemplate,
                                    AutoUser autoUser,
                                    YPath jobRootPath,
                                    YPath tmpPath,
                                    String mapReducePool) {
            super(yt, ytExportModelsTableService, ytExportMapReduceService, yqlJdbcTemplate, autoUser, jobRootPath,
                tmpPath, mapReducePool);
        }

        public static String getSessionScaleKey() {
            return SESSION_SCALES;
        }

        @Override
        protected List<String> listSessions() {
            return super.listSessions();
        }

        @Override
        protected List<String> listSessions(Scale scale) {
            return super.listSessions(scale);
        }

        @Override
        public Date getCurrentTime() {
            return now;
        }

        @Override
        protected int getCreateSessionRetryCount() {
            return 1;
        }

        @Override
        protected YPaths dumpSessionToYt(List<TovarCategory> tovarCategories, GuruCounterSupply supply,
                                         Consumer<Date> startConsumer) {
            Consumer<Date> consumer = dt -> {
                dumpedDates.add(dt);
                startConsumer.accept(dt);
            };
            YPaths output = prepareOutput(consumer);
            return output;
        }

        public void updateNow(String firstSession) {
            now = new Date(ExportMapReduceUtil.getSessionTime(firstSession));
        }
    }
}

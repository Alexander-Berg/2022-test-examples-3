package ru.yandex.market.antifraud.yql.validate;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.ImmutableSetMultimap;
import lombok.SneakyThrows;
import org.apache.commons.lang3.time.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import ru.yandex.hadoop.woodsman.loaders.logbroker.parser.service.pp.PpStateService;
import ru.yandex.hadoop.woodsman.loaders.logbroker.parser.service.pp.reader.PpReader;
import ru.yandex.market.antifraud.db.LoggingJdbcTemplate;
import ru.yandex.market.antifraud.util.IntDateUtil;
import ru.yandex.market.antifraud.yql.model.UnvalidatedDay;
import ru.yandex.market.antifraud.yql.model.YtConfig;
import ru.yandex.market.antifraud.yql.model.YtLogConfig;
import ru.yandex.market.antifraud.yql.yt.YtTablesHelper;

import java.lang.reflect.Field;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalUnit;
import java.util.Date;
import java.util.NavigableSet;
import java.util.concurrent.atomic.AtomicReference;

/*
This runs, when not enough data in YT //home/market/testing/mstat/yqlaf/test_data.
Load takes ~1 hour =(
 */
@Component
@Profile("integration-tests")
public class YtTestDataGenerator {
    public static final String ITEST_DIR = "//home/market/testing/mstat/yqlaf_itests";
    private static final YtLogConfig LOG_CONF = new YtLogConfig("market-new-shows-log");
    private static final YtLogConfig CLICK_ROLLBACKS_LOG_CONF = new YtLogConfig("market-clicks-rollbacks-log");
    private static volatile boolean initialized = false;

    @Autowired
    private YtTablesHelper ytTablesHelper;

    @Autowired
    private LoggingJdbcTemplate yqlJdbcTemplate;

    @Autowired
    private YtCreateAfDirs createAfDirs;

    @Autowired
    private YtConfig ytConfig;

    @Autowired
    private PpStateService ppStateService;

    // these days would be reloaded if missing in
    // //home/market/testing/mstat/yqlaf/test_data
    // recent day would be yesterday, if 20171105 is missing from test_data
    private int archiveDay = 20171030;
    private int recentDay = 20171105;

    // this data is readonly for af

    public synchronized void initOnce() {
        if(!initialized) {
            deleteOldTestDirs();
            checkTestData();
            initPps();
            initialized = true;
        }
        cleanupCurrentITestDir();
    }

    @SneakyThrows
    private void initPps() {
//        Field updatedAtField = ppStateService.getClass().getSuperclass().getDeclaredField("updatedAt");
//        updatedAtField.setAccessible(true);
//        updatedAtField.set(ppStateService, Instant.MAX);
//
//        Field ppSetupField = ppStateService.getClass().getDeclaredField("ppSetup");
//        ppSetupField.setAccessible(true);
//        AtomicReference<PpReader.PpSetup> ppSetup = new AtomicReference<PpReader.PpSetup>(new PpReader.PpSetup(ImmutableMap.of(
//            PpReader.PpKind.VALID, ImmutableSetMultimap.of("market", 7))));
//        ppSetupField.set(ppStateService, ppSetup);
    }

    private void checkTestData() {
        String archiveDayStr = IntDateUtil.hyphenated(archiveDay);
        loadArchive(LOG_CONF, archiveDayStr);
        loadArchive(CLICK_ROLLBACKS_LOG_CONF, archiveDayStr);

        String prevArchiveDayStr = IntDateUtil.hyphenated(IntDateUtil.toInt(
                DateUtils.addDays(IntDateUtil.fromInt(archiveDay), -1)));
        loadArchive(LOG_CONF, prevArchiveDayStr);
        loadArchive(CLICK_ROLLBACKS_LOG_CONF, prevArchiveDayStr);

        String recent = ytConfig.getLogDir(LOG_CONF, UnvalidatedDay.Scale.RECENT);
        mkdirIfNoExists(recent);

        String recentClicksRb = ytConfig.getLogDir(CLICK_ROLLBACKS_LOG_CONF, UnvalidatedDay.Scale.RECENT);
        mkdirIfNoExists(recentClicksRb);

        NavigableSet<String> recentTables = ytTablesHelper.list(recent);

        if (recentTables.size() < 48 * 2) {
            ytTablesHelper.remove(recent);
            ytTablesHelper.mkDir(recent);
            ytTablesHelper.remove(recentClicksRb);
            ytTablesHelper.mkDir(recentClicksRb);

            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
            String yesterday = sdf.format(DateUtils.addDays(new Date(), -1));
            loadRecentDay(recent, yesterday, LOG_CONF);
            loadRecentDay(recentClicksRb, yesterday, CLICK_ROLLBACKS_LOG_CONF);
            String dayBeforeYesterday = sdf.format(DateUtils.addDays(new Date(), -2));
            loadRecentDay(recent, dayBeforeYesterday, LOG_CONF);
            loadRecentDay(recentClicksRb, dayBeforeYesterday, CLICK_ROLLBACKS_LOG_CONF);

            recentDay = IntDateUtil.hyphenated(yesterday);
        } else {
            recentDay = IntDateUtil.hyphenated(recentTables.pollLast().split("T")[0]);
        }
    }

    private void mkdirIfNoExists(String dir) {
        if (!ytTablesHelper.exists(dir)) {
            ytTablesHelper.mkDir(dir);
        }
    }

    private void loadArchive(YtLogConfig conf, String day) {
        String dir = ytConfig.getLogDir(conf, UnvalidatedDay.Scale.ARCHIVE);
        if (!ytTablesHelper.exists(dir + "/" + day)) {
            ytTablesHelper.mkDir(dir);
            yqlJdbcTemplate.exec("insert into `" + dir + "/" + day + "` " +
                "select * from `//logs/" + conf.getLogName().getLogName() + "/1d/" + day + "` limit 10000");
        }
    }

    private void loadRecentDay(String recent, String day, YtLogConfig logConfig) {
        StringBuilder query = new StringBuilder();
        for (int i = 0; i < 48; i++) {
            String p = day + "T" + String.format("%02d", i / 2) + ":" + (i % 2 == 0 ? "00" : "30") + ":00";
            query.append("insert into `" + recent + "/" + p + "`" +
                "select * from `//logs/" + logConfig.getLogName().getLogName() + "/30min/" + p + "` limit 2000;\n");
        }
        yqlJdbcTemplate.exec(query.toString());
    }

    private void cleanupCurrentITestDir() {
        ytTablesHelper.remove(ytConfig.getAfRootDir());
        ytTablesHelper.mkDir(ytConfig.getAfRootDir());
        createAfDirs.createYtDirs();
    }

    private void deleteOldTestDirs() {
        if(!ytTablesHelper.exists(ITEST_DIR)) {
            return;
        }

        NavigableSet<String> tests = ytTablesHelper.list(ITEST_DIR);
        if(tests.size() > 20) {
            int delete = tests.size() - 20;
            for(String test: tests) {
                if(delete-- > 0) {
                    ytTablesHelper.remove(ITEST_DIR + "/" + test);
                }
            }
        }
    }

    public int getArchiveDay() {
        return archiveDay;
    }

    public int getRecentDay() {
        return recentDay;
    }

    public YtLogConfig log() {
        return LOG_CONF;
    }

    public String getCluster() {
        return ytConfig.getCluster();
    }
}

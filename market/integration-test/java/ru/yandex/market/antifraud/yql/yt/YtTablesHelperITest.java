package ru.yandex.market.antifraud.yql.yt;

import com.google.common.collect.ImmutableMap;
import lombok.extern.slf4j.Slf4j;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import ru.yandex.inside.yt.kosher.Yt;
import ru.yandex.inside.yt.kosher.common.GUID;
import ru.yandex.market.antifraud.db.LoggingJdbcTemplate;
import ru.yandex.market.antifraud.util.SleepUtil;
import ru.yandex.market.antifraud.yql.config.IntegrationTestAntifraudYqlConfig;
import ru.yandex.market.antifraud.yql.model.UnvalidatedDay;
import ru.yandex.market.antifraud.yql.model.YtConfig;
import ru.yandex.market.antifraud.yql.validate.YtTestDataGenerator;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.SortedSet;
import java.util.concurrent.ThreadLocalRandom;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {IntegrationTestAntifraudYqlConfig.class})
@ActiveProfiles("integration-tests")
@Slf4j
public class YtTablesHelperITest {

    @Autowired
    private YtTablesHelper ytTablesHelper;

    @Autowired
    private YtTestDataGenerator testDataGenerator;

    @Autowired
    private YtConfig ytConfig;

    @Before
    public void init() {
        testDataGenerator.initOnce();
    }

    @Test
    public void mustLoadTablesFromYT() {
        String recentDir = ytConfig.getLogPath(testDataGenerator.log(), UnvalidatedDay.Scale.RECENT, "_partition_placeholder_")
                .replace("/_partition_placeholder_", "");

        SortedSet<String> tables = ytTablesHelper.list(recentDir);

        assertThat(tables.size(), is(96));
        for (String table : tables) {
            assertFalse(table.contains("/"));
        }
    }

    @Test
    public void testCreateIfNotExists() {
        String exists = ytConfig.getAfRootDir() + "/tblexists";
        ytTablesHelper.create(exists);
        assertTrue(ytTablesHelper.exists(exists));
        ytTablesHelper.createIfNotExists(exists, ImmutableMap.of("col1", YtType.STRING));
        assertTrue(ytTablesHelper.exists(exists));
    }
}

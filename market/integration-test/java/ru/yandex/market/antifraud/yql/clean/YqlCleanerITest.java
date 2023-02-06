package ru.yandex.market.antifraud.yql.clean;

import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import ru.yandex.market.antifraud.util.IntDateUtil;
import ru.yandex.market.antifraud.yql.config.IntegrationTestAntifraudYqlConfig;
import ru.yandex.market.antifraud.yql.validate.YtTestDataGenerator;
import ru.yandex.market.antifraud.yql.yt.YtTablesHelper;

import java.util.Arrays;
import java.util.TreeSet;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {IntegrationTestAntifraudYqlConfig.class})
@ActiveProfiles("integration-tests")
@Slf4j
public class YqlCleanerITest {
    @Autowired
    private YtTablesHelper ytTablesHelper;

    @Autowired
    private YqlCleaner yqlCleaner;

    @Test
    public void mustDelete() {
        int today = 20180527;
        String testDir = YtTestDataGenerator.ITEST_DIR + "/archive";
        if(!ytTablesHelper.exists(testDir)) {
            ytTablesHelper.mkDir(testDir);
        }

        tryCreate(testDir + "/2018-05-27");
        tryCreate(testDir + "/2018-05-26");
        tryCreate(testDir + "/2018-05-25");
        tryCreate(testDir + "/2018-05-24");

        // keepdays = 2 in itest
        yqlCleaner.rmOlderThan(testDir, today, filename -> IntDateUtil.hyphenated(filename));
        assertThat(ytTablesHelper.list(testDir),
            is(new TreeSet<String>(Arrays.asList("2018-05-27", "2018-05-26"))));
    }

    private void tryCreate(String path) {
        try {
            ytTablesHelper.create(path);
        } catch (Exception e) {
            // ignore
        }
    }
}

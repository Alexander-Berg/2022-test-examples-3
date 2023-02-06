package ru.yandex.market.antifraud.yql.yt;

import lombok.extern.slf4j.Slf4j;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import ru.yandex.inside.yt.kosher.common.GUID;
import ru.yandex.market.antifraud.db.LoggingJdbcTemplate;
import ru.yandex.market.antifraud.util.SleepUtil;
import ru.yandex.market.antifraud.yql.config.IntegrationTestAntifraudYqlConfig;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {IntegrationTestAntifraudYqlConfig.class})
@ActiveProfiles("integration-tests")
@Slf4j
public class YtTxTest {
    @Autowired
    private YtTablesHelper ytTablesHelper;

    @Autowired
    private LoggingJdbcTemplate yqlJdbcTemplate;

    @Test
    @Ignore
    public void runInTx() {
        GUID guid = ytTablesHelper.startTx();
        yqlJdbcTemplate.exec("" +
                "use hahn;" +
                "pragma yt.ExternalTx = \"" + guid.toString() + "\";\n" +
                "select rowid from `//logs/market-new-shows-log/1d/2017-11-23` limit 100;"
        );

        // tx-pinger thread is running in YtTablesHelper
        SleepUtil.sleep(60_000);
        ytTablesHelper.commitTx(guid);
    }
}

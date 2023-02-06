package ru.yandex.market.antifraud.yql.model;

import org.junit.Test;

import java.util.Random;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;


public class YtConfigImplTest {
    @Test
    public void testConfig() {
        YtConfigImpl config = new YtConfigImpl("token1", "cluster1", "env1", "pool1", "prioritypool1");
        YtLogConfig logConfig = new YtLogConfig("log1");
        String rootDir = config.getAfRootDir();
        assertThat(config.getTmpRollbacksDir(logConfig),
                is(rootDir + "/tmp_session/log1"));
        assertThat(config.getLogPath(logConfig, UnvalidatedDay.Scale.ARCHIVE, "_partition_placeholder_"),
                is("//logs/log1/1d/_partition_placeholder_"));
    }
}

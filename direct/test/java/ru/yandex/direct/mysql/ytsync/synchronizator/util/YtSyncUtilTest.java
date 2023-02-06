package ru.yandex.direct.mysql.ytsync.synchronizator.util;

import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class YtSyncUtilTest {
    @Test
    public void formatMillis() {
        assertThat(YtSyncUtil.formatMillis(0L), is("0ms"));
        assertThat(YtSyncUtil.formatMillis(1L), is("1ms"));
        assertThat(YtSyncUtil.formatMillis(-1L), is("-1ms"));
        assertThat(YtSyncUtil.formatMillis(1000L), is("1s000ms"));
        assertThat(YtSyncUtil.formatMillis(1002L), is("1s002ms"));
        assertThat(YtSyncUtil.formatMillis(60000L), is("1m00s000ms"));
        assertThat(YtSyncUtil.formatMillis(62003L), is("1m02s003ms"));
        assertThat(YtSyncUtil.formatMillis(3600000L), is("1h00m00s000ms"));
        assertThat(YtSyncUtil.formatMillis(3723004L), is("1h02m03s004ms"));
        assertThat(YtSyncUtil.formatMillis(86400000L), is("1d00h00m00s000ms"));
        assertThat(YtSyncUtil.formatMillis(93784005L), is("1d02h03m04s005ms"));
    }
}

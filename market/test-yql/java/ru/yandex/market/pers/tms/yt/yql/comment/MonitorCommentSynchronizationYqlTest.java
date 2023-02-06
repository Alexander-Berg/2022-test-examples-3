package ru.yandex.market.pers.tms.yt.yql.comment;

import org.junit.jupiter.api.Test;

import ru.yandex.market.pers.tms.yt.yql.AbstractPersYqlTest;

public class MonitorCommentSynchronizationYqlTest extends AbstractPersYqlTest {

    @Test
    public void test() {
        runTest(loadScript("/yql/comment/grade_comment_monitor.sql"),
                "/comment/grade_comment_monitor_expected.json",
                "/comment/grade_comment_monitor.mock");
    }
}

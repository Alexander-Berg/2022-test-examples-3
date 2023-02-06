package ru.yandex.direct.logviewercore.domain;

import org.junit.Test;

import ru.yandex.direct.logviewercore.domain.ppclog.LogApiRecord;
import ru.yandex.direct.logviewercore.domain.ppclog.LogCmdRecord;
import ru.yandex.direct.logviewercore.domain.ppclog.LogMessages;
import ru.yandex.direct.logviewercore.domain.ppclog.LogRecord;

import static org.hamcrest.Matchers.emptyIterable;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertThat;

public class LogRecordInfoTest {
    private LogRecordInfo<? extends LogRecord> logApiInfo = new LogRecordInfo<>(LogApiRecord.class);
    private LogRecordInfo<? extends LogRecord> logCmdInfo = new LogRecordInfo<>(LogCmdRecord.class);
    private LogRecordInfo<? extends LogRecord> logMessagesInfo = new LogRecordInfo<>(LogMessages.class);

    @Test
    public void getColumnNames_Simple() {
        assertThat(logCmdInfo.getColumnNames(),
                hasItems("log_time", "reqid", "pid"));
    }

    @Test
    public void getColumnNames_WithUnderscore() {
        assertThat(logApiInfo.getColumnNames(),
                hasItems("log_time", "reqid", "interface"));
    }

    @Test
    public void getColumnField_Simple() {
        assertThat(logApiInfo.getColumnField("reqid").getName(), is("reqid"));
    }

    @Test
    public void getColumnField_WithUnderscore() {
        assertThat(logApiInfo.getColumnField("interface").getName(), is("_interface"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void getColumnField_Null() {
        logApiInfo.getColumnField("xxx");
    }

    @Test(expected = IllegalArgumentException.class)
    public void getColumnField_NullWithUnderscore() {
        logApiInfo.getColumnField("_interface");
    }

    @Test
    public void isHeavyField_True() {
        assertThat(logApiInfo.isHeavyField("param"), is(true));
    }

    @Test
    public void isHeavyField_False() {
        assertThat(logApiInfo.isHeavyField("http_status"), is(false));
    }

    @Test
    public void hasSelectiveFields_True() {
        assertThat(logApiInfo.hasSelectiveFields(), is(true));
    }

    @Test
    public void hasSelectiveFields_False() {
        assertThat(logMessagesInfo.hasSelectiveFields(), is(false));
    }

    @Test
    public void getSelectiveFields_Presents() {
        assertThat(logApiInfo.getSelectiveFields(), not(emptyIterable()));
    }

    @Test
    public void getSelectiveFields_NotPresents() {
        assertThat(logMessagesInfo.getSelectiveFields(), emptyIterable());
    }
}

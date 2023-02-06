package ru.yandex.direct.logviewercore.domain;

import org.junit.Test;

import ru.yandex.direct.logviewercore.domain.ppclog.LogApiRecord;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.sameInstance;

public class LogTablesInfoManagerTest {

    @Test
    public void allTableClasses_Size() {
        assertThat(LogTablesInfoManager.allTableClasses().keySet().size(), greaterThanOrEqualTo(2));
    }

    @Test
    public void allTableClasses_ExistentTable_ReturnsTableClass() {
        assertThat(LogTablesInfoManager.allTableClasses().get("ppclog_api"), sameInstance(LogApiRecord.class));
    }

    @Test
    public void allTableClasses_NotExistentTable_ReturnsNull() {
        assertThat(LogTablesInfoManager.allTableClasses().get("ppclog_api_xxx"), nullValue());
    }

    @Test(expected = UnsupportedOperationException.class)
    public void allTableClasses_ReturnsUnmodifyableMap() {
        LogTablesInfoManager.allTableClasses().remove("ppclog_api");
    }

    @Test
    public void getLogRecordInfo_ExistentLog_ReturnsNotNull() {
        assertThat(LogTablesInfoManager.getLogRecordInfo("ppclog_api"), notNullValue());
    }

    @Test(expected = IllegalArgumentException.class)
    public void getLogRecordInfo_NotExistentLog_ThrowsException() {
        LogTablesInfoManager.getLogRecordInfo("ppclog_api_xxx");
    }
}

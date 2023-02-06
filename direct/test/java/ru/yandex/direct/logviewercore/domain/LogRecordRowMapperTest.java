package ru.yandex.direct.logviewercore.domain;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Arrays;

import org.junit.Before;
import org.junit.Test;

import ru.yandex.direct.logviewercore.domain.ppclog.ExampleLogRecord;
import ru.yandex.direct.logviewercore.domain.ppclog.LogRecord;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;

public class LogRecordRowMapperTest {
    private LogRecordRowMapper rowMapper;
    private ResultSet resultSet;
    private ExampleLogRecord expected;

    @Before
    public void setUp() throws Exception {
        LogRecordInfo<LogRecord> exampleLogInfo = LogTablesInfoManager.getLogRecordInfo("example_log");
        rowMapper = new LogRecordRowMapper<>(Arrays.asList("reqid", "log_time"), exampleLogInfo);

        expected = new ExampleLogRecord();
        expected.reqid = 123;
        expected.log_time = new Timestamp(1000);

        resultSet = mock(ResultSet.class);
        when(resultSet.getLong(1)).thenReturn(expected.reqid);
        when(resultSet.getTimestamp(2)).thenReturn(expected.log_time);
    }

    @Test
    public void mapReturnsLongAndTimestamp() throws SQLException {
        assertThat(rowMapper.mapRow(resultSet, 0), beanDiffer(expected));
    }

    @Test
    public void mapRowToObjectsListReturnsLongAndTimestamp() throws SQLException {
        assertEquals(Arrays.asList(expected.reqid, expected.log_time), rowMapper.mapRowToObjectsList(resultSet, 0));
    }
}

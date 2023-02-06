package ru.yandex.direct.logviewercore.service;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hamcrest.Matcher;
import org.junit.Before;
import org.junit.Test;
import org.springframework.jdbc.core.RowMapper;

import ru.yandex.clickhouse.response.ClickHouseResponse;
import ru.yandex.clickhouse.response.ClickHouseResultSet;
import ru.yandex.direct.clickhouse.SqlBuilder;
import ru.yandex.direct.core.entity.feature.service.FeatureManagingService;
import ru.yandex.direct.core.entity.feature.service.FeatureService;
import ru.yandex.direct.dbutil.sharding.ShardHelper;
import ru.yandex.direct.dbutil.wrapper.DatabaseWrapper;
import ru.yandex.direct.dbutil.wrapper.DatabaseWrapperProvider;
import ru.yandex.direct.dbutil.wrapper.SimpleDb;
import ru.yandex.direct.logviewercore.domain.LogRecordInfo;
import ru.yandex.direct.logviewercore.domain.ppclog.ExampleDbShardsLogRecord;
import ru.yandex.direct.logviewercore.domain.ppclog.ExampleLogRecord;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.hamcrest.MockitoHamcrest.argThat;
import static ru.yandex.direct.logviewercore.FieldsMatcher.fieldsEquals;

@SuppressWarnings("unchecked")
public class LogViewerServiceGetLogRowsTest {

    private LogViewerService testingService;

    private DatabaseWrapper clickHouseJdbcTemplate;

    private SimpleDb db;
    private LogRecordInfo<ExampleLogRecord> logRecordInfo;
    private List<String> allFields;
    private LocalDateTime dateFrom;
    private LocalDateTime dateTo;
    private Map<String, List<String>> conditions;
    private int limit;
    private int offset;
    private boolean reverseOrder;

    @Before
    public void prepare() {
        ClickHouseResponse resp = mock(ClickHouseResponse.class);
        when(resp.getData()).thenReturn(singletonList(asList("2017-08-09 00:00:09", "12")));
        when(resp.getTotals()).thenReturn(asList("0000-00-00 00:00:00", "12"));

        clickHouseJdbcTemplate = mock(DatabaseWrapper.class);
        when(clickHouseJdbcTemplate.clickhouseQuery(any(), any()))
                .thenReturn(resp);

        testingService = new LogViewerService(
                mock(DatabaseWrapperProvider.class),
                mock(ShardHelper.class),
                emptyList(),
                mock(FeatureService.class),
                mock(FeatureManagingService.class)
        ){
            @Override
            protected String generateFullSql(SimpleDb db, SqlBuilder query) {
                return "";
            }
        };

        when(testingService.dbProvider.get(SimpleDb.CLICKHOUSE_CLOUD)).thenReturn(clickHouseJdbcTemplate);

        // method parameters
        db = SimpleDb.CLICKHOUSE_CLOUD;
        logRecordInfo = new LogRecordInfo<>(ExampleLogRecord.class);
        allFields = logRecordInfo.getColumnNames();
        dateFrom = LocalDateTime.of(2015, 12, 3, 15, 0);
        dateTo = LocalDateTime.of(2016, 5, 31, 18, 0);
        conditions = new HashMap<>();
        conditions.put("host", singletonList("some_string"));      // String with wildcard
        conditions.put("token", singletonList("another-string"));  // String without wildcard
        conditions.put("http_status", singletonList("200"));       // int
        conditions.put("reqid", singletonList("4444"));            // long
        conditions.put("cid", singletonList("5555"));              // long[]
        conditions.put("runtime", singletonList("1234.5"));        // float
        limit = 10;
        offset = 3;
        reverseOrder = false;
    }

    @Test
    public void getLogRows_SqlContainsDateAndTimeLimitations() {
        Matcher<String> sqlMatcher = allOf(
                containsString("`log_date` BETWEEN"),
                containsString("`log_time` BETWEEN")
        );

        testingService.getLogRows(db, logRecordInfo, allFields, dateFrom, dateTo, conditions, limit, offset, reverseOrder);
        verify(clickHouseJdbcTemplate).query(
                argThat(sqlMatcher),
                (Object[]) any(),
                (RowMapper) any());
    }

    @Test
    public void getLogRows_SqlContainsStringLikeCondition() {
        testingService.getLogRows(db, logRecordInfo, allFields, dateFrom, dateTo, conditions, limit, offset, reverseOrder);
        verify(clickHouseJdbcTemplate).query(
                argThat(containsString("like(`host`, ?)")),
                (Object[]) any(),
                (RowMapper) any());
    }

    @Test
    public void getLogRows_SqlContainsStringEqualsCondition() {
        testingService.getLogRows(db, logRecordInfo, allFields, dateFrom, dateTo, conditions, limit, offset, reverseOrder);
        verify(clickHouseJdbcTemplate).query(
                argThat(containsString("`token` = ?")),
                (Object[]) any(),
                (RowMapper) any());
    }

    @Test
    public void getLogRows_SqlContainsIntegerCondition() {
        testingService.getLogRows(db, logRecordInfo, allFields, dateFrom, dateTo, conditions, limit, offset, reverseOrder);
        verify(clickHouseJdbcTemplate).query(
                argThat(containsString("`http_status` = ?")),
                (Object[]) any(),
                (RowMapper) any());
    }

    @Test
    public void getLogRows_SqlContainsLongCondition() {
        testingService.getLogRows(db, logRecordInfo, allFields, dateFrom, dateTo, conditions, limit, offset, reverseOrder);
        verify(clickHouseJdbcTemplate).query(
                argThat(containsString("`reqid` = ?")),
                (Object[]) any(),
                (RowMapper) any());
    }

    @Test
    public void getLogRows_SqlContainsLongArrayCondition() {
        testingService.getLogRows(db, logRecordInfo, allFields, dateFrom, dateTo, conditions, limit, offset, reverseOrder);
        verify(clickHouseJdbcTemplate).query(
                argThat(containsString("has(`cid`, ?)")),
                (Object[]) any(),
                (RowMapper) any());
    }

    @Test
    public void getLogRows_SqlContainsFloatCondition() {
        testingService.getLogRows(db, logRecordInfo, allFields, dateFrom, dateTo, conditions, limit, offset, reverseOrder);
        verify(clickHouseJdbcTemplate).query(
                argThat(containsString("`runtime` = ?")),
                (Object[]) any(),
                (RowMapper) any());
    }

    @Test
    public void getLogRows_SqlContainsOffsetAndLimit() {
        testingService.getLogRows(db, logRecordInfo, allFields, dateFrom, dateTo, conditions, limit, offset, reverseOrder);
        verify(clickHouseJdbcTemplate).query(
                argThat(containsString("LIMIT ?, ?")),
                (Object[]) any(),
                (RowMapper) any());
    }

    @Test
    public void getLogRows_SqlContainsOrderBy() {
        testingService.getLogRows(db, logRecordInfo, allFields, dateFrom, dateTo, conditions, limit, offset, reverseOrder);
        verify(clickHouseJdbcTemplate).query(
                argThat(containsString("ORDER BY `log_time` ASC")),
                (Object[]) any(),
                (RowMapper) any());
    }

    @Test
    public void getLogRows_AllRows_RowMapperWorksCorrect() throws SQLException {
        LogRecordInfo<ExampleDbShardsLogRecord> info = new LogRecordInfo<>(ExampleDbShardsLogRecord.class);

        // expected object
        ExampleDbShardsLogRecord expectedRecord = new ExampleDbShardsLogRecord();
        expectedRecord.source = "some source";
        expectedRecord.host = "some host";
        expectedRecord.reqid = 457990L;
        expectedRecord.key = "some key";
        expectedRecord.ids = new long[]{1, 3, 7, 11};
        expectedRecord.insert_data = "some data";

        // fields of expected object
        List<String> expectedFields = asList("source", "host", "reqid", "key", "ids", "insert_data");

        // mock of ResultSet returns set of fields in specified order
        final ClickHouseResultSet resultSet = mock(ClickHouseResultSet.class);
        when(resultSet.getString(1)).thenReturn(expectedRecord.source);
        when(resultSet.getString(2)).thenReturn(expectedRecord.host);
        when(resultSet.getLong(3)).thenReturn(expectedRecord.reqid);
        when(resultSet.getString(4)).thenReturn(expectedRecord.key);
        when(resultSet.getLongArray(5)).thenReturn(expectedRecord.ids);
        when(resultSet.getString(6)).thenReturn(expectedRecord.insert_data);

        // mock of clickHouseJdbcTemplate.query() method invocation inside getLogRows method
        doAnswer(invocation -> {
            RowMapper<ExampleDbShardsLogRecord> rowMapper = invocation.getArgument(2);
            return singletonList(rowMapper.mapRow(resultSet, 0));
        }).when(clickHouseJdbcTemplate).query(any(), (Object[]) any(), (RowMapper) any());

        // invocation to check anonymous RowMapper that method passes to clickHouseJdbcTemplate.query() method
        List<? extends ExampleDbShardsLogRecord> records = testingService.getLogRows(
                db, info, expectedFields,
                dateFrom, dateTo, new HashMap<>(), limit, offset, reverseOrder).rows();

        assertThat("returned record differs with expected: anonymous RowMapper works incorrect",
                records.get(0), fieldsEquals(expectedRecord));
    }

    @Test
    public void getLogRows_PartialRows_RowMapperWorksCorrect() throws SQLException {
        LogRecordInfo<ExampleDbShardsLogRecord> info = new LogRecordInfo<>(ExampleDbShardsLogRecord.class);

        // expected object
        ExampleDbShardsLogRecord expectedRecord = new ExampleDbShardsLogRecord();
        expectedRecord.source = "some source";
        expectedRecord.host = null;
        expectedRecord.reqid = 457990L;
        expectedRecord.key = null;
        expectedRecord.ids = new long[]{1, 3, 7, 11};
        expectedRecord.insert_data = "some data";

        // fields of expected object
        List<String> expectedFields = asList("source", "reqid", "ids", "insert_data");

        // mock of ResultSet returns set of fields in specified order
        final ClickHouseResultSet resultSet = mock(ClickHouseResultSet.class);
        when(resultSet.getString(1)).thenReturn(expectedRecord.source);
        when(resultSet.getLong(2)).thenReturn(expectedRecord.reqid);
        when(resultSet.getLongArray(3)).thenReturn(expectedRecord.ids);
        when(resultSet.getString(4)).thenReturn(expectedRecord.insert_data);

        // mock of clickHouseJdbcTemplate.query() method invocation inside getLogRows method
        doAnswer(invocation -> {
            RowMapper<ExampleDbShardsLogRecord> rowMapper = invocation.getArgument(2);
            return singletonList(rowMapper.mapRow(resultSet, 0));
        }).when(clickHouseJdbcTemplate).query(any(), (Object[]) any(), (RowMapper) any());

        // invocation to check anonymous RowMapper that method passes to clickHouseJdbcTemplate.query() method
        List<? extends ExampleDbShardsLogRecord> records = testingService.getLogRows(
                db, info, expectedFields, dateFrom, dateTo, new HashMap<>(), limit, offset, reverseOrder).rows();

        assertThat("returned record differs with expected: anonymous RowMapper works incorrect",
                records.get(0), fieldsEquals(expectedRecord));
    }
}

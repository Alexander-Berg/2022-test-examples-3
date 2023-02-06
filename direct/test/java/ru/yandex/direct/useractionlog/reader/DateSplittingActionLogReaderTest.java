package ru.yandex.direct.useractionlog.reader;

import java.sql.Date;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.apache.commons.lang3.tuple.Pair;
import org.assertj.core.api.Assertions;
import org.assertj.core.api.JUnitSoftAssertions;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;

import ru.yandex.direct.binlogclickhouse.schema.FieldValueList;
import ru.yandex.direct.clickhouse.SqlBuilder;
import ru.yandex.direct.tracing.data.DirectTraceInfo;
import ru.yandex.direct.useractionlog.ClientId;
import ru.yandex.direct.useractionlog.db.ReadActionLogTable;
import ru.yandex.direct.useractionlog.schema.ActionLogRecord;
import ru.yandex.direct.useractionlog.schema.ActionLogSchema;
import ru.yandex.direct.useractionlog.schema.ObjectPath;
import ru.yandex.direct.useractionlog.schema.Operation;
import ru.yandex.direct.useractionlog.schema.RecordSource;

@ParametersAreNonnullByDefault
@RunWith(JUnitParamsRunner.class)
public class DateSplittingActionLogReaderTest {
    private static RecordSource recordSource;
    @Rule
    public JUnitSoftAssertions softly = new JUnitSoftAssertions();
    private Map<SqlBuilder, SqlBuilderMockData> getRecordsMockMap;
    private Map<SqlBuilder, SqlBuilderMockData> getDateCountMockMap;
    private Collection<ActionLogRecord> tableContent = Collections.emptyList();

    private static ActionLogRecord sampleRecord(LocalDateTime dateTime, int eventId) {
        return ActionLogRecord.builder()
                .withDateTime(dateTime)
                .withPath(new ObjectPath.ClientPath(new ClientId(1)))
                .withGtid("serveruuid:" + eventId)
                .withQuerySerial(0)
                .withRowSerial(0)
                .withDirectTraceInfo(DirectTraceInfo.empty())
                .withDb("test")
                .withType("test")
                .withOperation(Operation.INSERT)
                .withOldFields(FieldValueList.empty())
                .withNewFields(FieldValueList.empty())
                .withRecordSource(recordSource)
                .build();
    }

    @Before
    public void setUp() {
        recordSource = RecordSource.makeDaemonRecordSource();
        getRecordsMockMap = new IdentityHashMap<>();
        getDateCountMockMap = new IdentityHashMap<>();
    }

    private SqlBuilder sqlBuilderMock(Map<SqlBuilder, SqlBuilderMockData> map,
                                      @Nullable ReadActionLogTable.Offset offset,
                                      @Nullable ReadActionLogTable.Order order) {
        SqlBuilder sqlBuilder = Mockito.mock(SqlBuilder.class);
        Mockito.when(sqlBuilder.whereIn(Mockito.any(SqlBuilder.Column.class), Mockito.<java.sql.Date>anyCollection()))
                .then(invocation -> {
                    if (!invocation.getArgument(0).toString().equals(ActionLogSchema.DATE.getExpr())) {
                        throw new UnsupportedOperationException(invocation.toString());
                    }
                    SqlBuilder thisObj = (SqlBuilder) invocation.getMock();
                    SqlBuilderMockData data = Objects.requireNonNull(map.get(thisObj));
                    data.filterDates.addAll(invocation.getArgument(1));
                    return thisObj;
                });
        map.put(sqlBuilder, new SqlBuilderMockData(offset, order));
        return sqlBuilder;
    }

    private ReadActionLogTable readActionLogTableMock() {
        ReadActionLogTable readActionLogTable = Mockito.mock(ReadActionLogTable.class);
        Mockito.when(readActionLogTable.dateAndCountSqlBuilder(Mockito.any(), Mockito.any()))
                .then(invocation ->
                        sqlBuilderMock(getDateCountMockMap, invocation.getArgument(0), invocation.getArgument(1)));
        Mockito.when(readActionLogTable.getCountByDate(Mockito.any(SqlBuilder.class)))
                .then(invocation ->
                        Objects.requireNonNull(getDateCountMockMap.get((SqlBuilder) invocation.getArgument(0)))
                                .getCountByDate());
        Mockito.when(readActionLogTable.sqlBuilderWithSort(Mockito.any(), Mockito.any()))
                .then(invocation ->
                        sqlBuilderMock(getRecordsMockMap, invocation.getArgument(0), invocation.getArgument(1)));
        Mockito.when(readActionLogTable.select(Mockito.any(SqlBuilder.class)))
                .then(invocation ->
                        Objects.requireNonNull(getRecordsMockMap.get((SqlBuilder) invocation.getArgument(0)))
                                .getRecords());
        return readActionLogTable;
    }

    @Parameters({"ASC", "DESC"})
    @SuppressWarnings("unchecked")
    @Test
    public void empty(ReadActionLogTable.Order order) {
        tableContent = Collections.emptyList();
        ReadRequestStats readRequestStats = new ReadRequestStats();
        List<ActionLogRecord> result = ImmutableList.copyOf(
                new DateSplittingActionLogReader(readActionLogTableMock(),
                        this::tableReader,
                        this::testingSqlBuilderFilter,
                        1,
                        null,
                        order,
                        readRequestStats));
        softly.assertThat(result)
                .extracting(ActionLogRecord::getDateTime)
                .isEmpty();
        softly.assertThat(readRequestStats.dateCountQueriesDone)
                .isEqualTo(1);
    }

    @Parameters({"ASC", "DESC"})
    @SuppressWarnings("unchecked")
    @Test
    public void singleDate(ReadActionLogTable.Order order) {
        tableContent = ImmutableList.of(
                sampleRecord(LocalDateTime.of(2000, 1, 1, 23, 59), 3),
                sampleRecord(LocalDateTime.of(2000, 1, 1, 0, 0), 1),
                sampleRecord(LocalDateTime.of(2000, 1, 1, 12, 0), 2));
        ReadRequestStats readRequestStats = new ReadRequestStats();
        List<ActionLogRecord> result = ImmutableList.copyOf(
                new DateSplittingActionLogReader(readActionLogTableMock(),
                        this::tableReader,
                        this::testingSqlBuilderFilter,
                        1,
                        null,
                        order,
                        readRequestStats));
        softly.assertThat(result)
                .extracting(ActionLogRecord::getDateTime)
                .isEqualTo(tableContent.stream()
                        .map(ActionLogRecord::getDateTime)
                        .sorted(order == ReadActionLogTable.Order.ASC
                                ? Comparator.naturalOrder()
                                : Comparator.reverseOrder())
                        .collect(Collectors.toList()));
        softly.assertThat(readRequestStats.dateCountQueriesDone)
                .isEqualTo(1);
        softly.assertThat(getRecordsMockMap)
                .describedAs("All created SqlBuilders was used")
                .matches(m -> m.values().stream().allMatch(d -> d.used));
        softly.assertThat(getRecordsMockMap.values())
                .describedAs("Should be done one query for all records because they contains the same date")
                .extracting(d -> d.filterDates)
                .containsExactlyInAnyOrder(ImmutableSet.of(java.sql.Date.valueOf(LocalDate.of(2000, 1, 1))));
    }

    @Parameters({"ASC", "DESC"})
    @SuppressWarnings("unchecked")
    @Test
    public void threeSeparateDates(ReadActionLogTable.Order order) {
        tableContent = ImmutableList.of(
                sampleRecord(LocalDateTime.of(2000, 1, 2, 0, 0), 2),
                sampleRecord(LocalDateTime.of(2000, 1, 1, 0, 0), 1),
                sampleRecord(LocalDateTime.of(2000, 1, 3, 0, 0), 3));
        ReadRequestStats readRequestStats = new ReadRequestStats();
        ReadActionLogTable readActionLogTable = readActionLogTableMock();
        List<ActionLogRecord> result = ImmutableList.copyOf(
                new DateSplittingActionLogReader(readActionLogTable,
                        this::tableReader,
                        this::testingSqlBuilderFilter,
                        1,
                        null,
                        order,
                        readRequestStats));
        softly.assertThat(result)
                .extracting(ActionLogRecord::getDateTime)
                .isEqualTo(tableContent.stream()
                        .map(ActionLogRecord::getDateTime)
                        .sorted(order == ReadActionLogTable.Order.ASC
                                ? Comparator.naturalOrder()
                                : Comparator.reverseOrder())
                        .collect(Collectors.toList()));
        softly.assertThat(readRequestStats.dateCountQueriesDone)
                .isEqualTo(1);

        softly.assertThat(getRecordsMockMap)
                .describedAs("All created SqlBuilders was used")
                .matches(m -> m.values().stream().allMatch(d -> d.used));
        softly.assertThat(getRecordsMockMap.values())
                .describedAs("Should be done separate queries for all records because they has different dates")
                .extracting(d -> d.filterDates)
                .containsExactlyInAnyOrder(ImmutableSet.of(java.sql.Date.valueOf(LocalDate.of(2000, 1, 1))),
                        ImmutableSet.of(java.sql.Date.valueOf(LocalDate.of(2000, 1, 2))),
                        ImmutableSet.of(java.sql.Date.valueOf(LocalDate.of(2000, 1, 3))));

    }

    @Parameters({"ASC", "DESC"})
    @SuppressWarnings("unchecked")
    @Test
    public void threeSeparateDatesJoining2(ReadActionLogTable.Order order) {
        tableContent = ImmutableList.of(
                sampleRecord(LocalDateTime.of(2000, 1, 2, 0, 0), 2),
                sampleRecord(LocalDateTime.of(2000, 1, 1, 0, 0), 1),
                sampleRecord(LocalDateTime.of(2000, 1, 3, 0, 0), 3));
        ReadRequestStats readRequestStats = new ReadRequestStats();
        ReadActionLogTable readActionLogTable = readActionLogTableMock();
        List<ActionLogRecord> result = ImmutableList.copyOf(
                new DateSplittingActionLogReader(readActionLogTable,
                        this::tableReader,
                        this::testingSqlBuilderFilter,
                        2,
                        null,
                        order,
                        readRequestStats));
        softly.assertThat(result)
                .extracting(ActionLogRecord::getDateTime)
                .isEqualTo(tableContent.stream()
                        .map(ActionLogRecord::getDateTime)
                        .sorted(order == ReadActionLogTable.Order.ASC
                                ? Comparator.naturalOrder()
                                : Comparator.reverseOrder())
                        .collect(Collectors.toList()));
        softly.assertThat(readRequestStats.dateCountQueriesDone)
                .isEqualTo(1);

        softly.assertThat(getRecordsMockMap)
                .describedAs("All created SqlBuilders was used")
                .matches(m -> m.values().stream().allMatch(d -> d.used));
        ImmutableSet[] expectedChunks;
        if (order == ReadActionLogTable.Order.ASC) {
            expectedChunks = new ImmutableSet[]{
                    ImmutableSet.of(
                            java.sql.Date.valueOf(LocalDate.of(2000, 1, 1)),
                            java.sql.Date.valueOf(LocalDate.of(2000, 1, 2))),
                    ImmutableSet.of(
                            java.sql.Date.valueOf(LocalDate.of(2000, 1, 3)))
            };
        } else {
            expectedChunks = new ImmutableSet[]{
                    ImmutableSet.of(
                            java.sql.Date.valueOf(LocalDate.of(2000, 1, 1))),
                    ImmutableSet.of(
                            java.sql.Date.valueOf(LocalDate.of(2000, 1, 2)),
                            java.sql.Date.valueOf(LocalDate.of(2000, 1, 3)))
            };
        }
        softly.assertThat(getRecordsMockMap.values())
                .describedAs("Should be done two queries for all records because they has different dates"
                        + " and with tryFetchAtLeast=2 matches first two dates")
                .extracting(d -> d.filterDates)
                .containsExactlyInAnyOrder(expectedChunks);

    }

    @Parameters({"ASC", "DESC"})
    @SuppressWarnings("unchecked")
    @Test
    public void offset(ReadActionLogTable.Order order) {
        tableContent = ImmutableList.of(
                sampleRecord(LocalDateTime.of(2000, 1, 1, 0, 0), 10),
                sampleRecord(LocalDateTime.of(2000, 1, 2, 0, 0), 20),
                sampleRecord(LocalDateTime.of(2000, 1, 3, 0, 0), 30),
                sampleRecord(LocalDateTime.of(2000, 1, 3, 0, 0), 35),
                sampleRecord(LocalDateTime.of(2000, 1, 4, 0, 0), 40));
        ReadRequestStats readRequestStats = new ReadRequestStats();
        ReadActionLogTable readActionLogTable = readActionLogTableMock();
        ReadActionLogTable.Offset offset = new ReadActionLogTable.Offset() {
            @Override
            public LocalDateTime getDateTime() {
                return LocalDateTime.of(2000, 1, 3, 0, 0);
            }

            @Override
            public String getGtid() {
                return "serveruuid:30";
            }

            @Override
            public int getQuerySerial() {
                return 0;
            }

            @Override
            public int getRowSerial() {
                return 0;
            }
        };
        List<ActionLogRecord> result = ImmutableList.copyOf(
                new DateSplittingActionLogReader(readActionLogTable,
                        this::tableReader,
                        this::testingSqlBuilderFilter,
                        2,
                        offset,
                        order,
                        readRequestStats));
        List<Pair<LocalDateTime, String>> expectedResult;
        if (order == ReadActionLogTable.Order.ASC) {
            expectedResult = Arrays.asList(
                    Pair.of(LocalDateTime.of(2000, 1, 3, 0, 0), "serveruuid:30"),
                    Pair.of(LocalDateTime.of(2000, 1, 3, 0, 0), "serveruuid:35"),
                    Pair.of(LocalDateTime.of(2000, 1, 4, 0, 0), "serveruuid:40"));
        } else {
            expectedResult = Arrays.asList(
                    Pair.of(LocalDateTime.of(2000, 1, 3, 0, 0), "serveruuid:30"),
                    Pair.of(LocalDateTime.of(2000, 1, 2, 0, 0), "serveruuid:20"),
                    Pair.of(LocalDateTime.of(2000, 1, 1, 0, 0), "serveruuid:10"));
        }
        softly.assertThat(result)
                .extracting(r -> Pair.of(r.getDateTime(), r.getGtid()))
                .isEqualTo(expectedResult);
        softly.assertThat(readRequestStats.dateCountQueriesDone)
                .isEqualTo(1);

        softly.assertThat(getRecordsMockMap)
                .describedAs("All created SqlBuilders was used")
                .matches(m -> m.values().stream().allMatch(d -> d.used));
        ImmutableSet[] expectedChunks;
        if (order == ReadActionLogTable.Order.ASC) {
            expectedChunks = new ImmutableSet[]{
                    ImmutableSet.of(
                            Date.valueOf(LocalDate.of(2000, 1, 3))),
                    ImmutableSet.of(
                            Date.valueOf(LocalDate.of(2000, 1, 4)))
            };
        } else {
            expectedChunks = new ImmutableSet[]{
                    ImmutableSet.of(
                            Date.valueOf(LocalDate.of(2000, 1, 1)),
                            Date.valueOf(LocalDate.of(2000, 1, 2))),
                    ImmutableSet.of(
                            Date.valueOf(LocalDate.of(2000, 1, 3)))
            };
        }
        softly.assertThat(getRecordsMockMap.values())
                .describedAs("Record with the last date that does not fit offset should not be requested."
                        + " Also because of offset the last date will be queried separately.")
                .extracting(d -> d.filterDates)
                .containsExactlyInAnyOrder(expectedChunks);
    }

    private void testingSqlBuilderFilter(SqlBuilder sqlBuilder) {
        SqlBuilderMockData data = getRecordsMockMap.get(sqlBuilder);
        if (data == null) {
            data = getDateCountMockMap.get(sqlBuilder);
        }
        Objects.requireNonNull(data).filterWasApplied = true;
    }

    private Iterator<ActionLogRecord> tableReader(Consumer<SqlBuilder> filterFn,
                                                  @Nullable ReadActionLogTable.Offset offset, ReadActionLogTable.Order order) {
        ReadActionLogTable readActionLogTable = readActionLogTableMock();
        SqlBuilder sqlBuilder = readActionLogTable.sqlBuilderWithSort(offset, order);
        filterFn.accept(sqlBuilder);
        return readActionLogTable.select(sqlBuilder).iterator();
    }

    private class SqlBuilderMockData {
        ReadActionLogTable.Offset offset;
        ReadActionLogTable.Order order;
        Collection<ActionLogRecord> records;
        Collection<java.sql.Date> filterDates;
        boolean filterWasApplied;
        boolean used;

        private SqlBuilderMockData(@Nullable ReadActionLogTable.Offset offset,
                                   @Nullable ReadActionLogTable.Order order) {
            this.offset = offset;
            this.order = order;
            records = ImmutableList.copyOf(tableContent);
            filterDates = new HashSet<>();
            filterWasApplied = false;
            used = false;
        }

        private void checks() {
            softly.assertThat(filterWasApplied)
                    .describedAs("Filter function should be applied")
                    .isTrue();
            Assertions.assertThat(used)
                    .describedAs("SqlBuilder object should be disposable")
                    .isFalse();
            used = true;
        }

        private boolean filterRecord(ActionLogRecord record) {
            if (!filterDates.isEmpty()
                    && !filterDates.contains(java.sql.Date.valueOf(record.getDateTime().toLocalDate()))) {
                return false;
            }
            if (offset != null) {
                // ActionLogRecord не наследует интерфейс Offset, поэтому сравнить с помощью Comparator не получится.
                // Так легче сравнивать кортежи, чем городить лесенку из if-else
                Pair<Pair<LocalDateTime, String>, Pair<Integer, Integer>> fromRecord =
                        Pair.of(Pair.of(record.getDateTime(), record.getGtid()),
                                Pair.of(record.getQuerySerial(), record.getRowSerial()));
                Pair<Pair<LocalDateTime, String>, Pair<Integer, Integer>> fromOffset =
                        Pair.of(Pair.of(offset.getDateTime(), offset.getGtid()),
                                Pair.of(offset.getQuerySerial(), offset.getRowSerial()));
                if (order == ReadActionLogTable.Order.ASC) {
                    return fromOffset.compareTo(fromRecord) <= 0;
                } else {
                    return fromOffset.compareTo(fromRecord) >= 0;
                }
            }
            return true;
        }

        List<ReadActionLogTable.DateAndCount> getCountByDate() {
            checks();
            return records.stream()
                    .filter(this::filterRecord)
                    .collect(Collectors.groupingBy((ActionLogRecord r) -> r.getDateTime().toLocalDate()))
                    .entrySet()
                    .stream()
                    .map(e -> new ReadActionLogTable.DateAndCount(e.getKey(), e.getValue().size()))
                    .collect(Collectors.toList());
        }

        List<ActionLogRecord> getRecords() {
            checks();
            Comparator<ActionLogRecord> actionLogRecordComparator = Comparator
                    .comparing(ActionLogRecord::getDateTime)
                    .thenComparing(ActionLogRecord::getGtid)
                    .thenComparingInt(ActionLogRecord::getQuerySerial)
                    .thenComparingInt(ActionLogRecord::getRowSerial);
            if (order == null) {
                throw new IllegalArgumentException("order is not specified");
            } else if (order == ReadActionLogTable.Order.DESC) {
                actionLogRecordComparator = actionLogRecordComparator.reversed();
            }
            return records.stream()
                    .filter(this::filterRecord)
                    .sorted(actionLogRecordComparator)
                    .collect(Collectors.toList());
        }
    }
}

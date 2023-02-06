package ru.yandex.direct.common.log;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import one.util.streamex.IntStreamEx;
import org.assertj.core.api.AutoCloseableSoftAssertions;
import org.junit.Before;
import org.junit.Test;

import ru.yandex.direct.common.log.container.LogEntry;
import ru.yandex.direct.common.log.container.LogType;

import static com.google.common.base.Preconditions.checkState;
import static org.assertj.core.api.Assertions.assertThat;

public class LogHelperTest {

    private static final int PARTITION_SIZE = 1000;
    private static final LogType LOG_TYPE = new LogType("testType");

    private LogHelper logHelper;

    @Before
    public void setUp() throws Exception {
        logHelper = new LogHelper(LOG_TYPE);
    }

    @Test
    public void getLogEntry_dataAreAddedToLogEntry() throws Exception {
        String payload = "testStringData";
        LogEntry<String> actual = logHelper.getLogEntry(payload);
        assertThat(actual).isNotNull();
        assertThat(actual.getData()).isEqualTo(payload);
    }

    @Test
    public void getLogEntry_logEntryFilledWithParameters() throws Exception {
        String payload = "testStringData";
        LogEntry<String> actual = logHelper.getLogEntry(payload);
        checkState(actual != null, "Returned from LogHelper LogEntry must not be null");
        try (AutoCloseableSoftAssertions softly = new AutoCloseableSoftAssertions()) {
            softly.assertThat(actual.getUid()).isNull();
            softly.assertThat(actual.getMethod()).isEqualTo("unknown");
            softly.assertThat(actual.getService()).isEqualTo("unknown");
            softly.assertThat(actual.getIp()).isNull();
            softly.assertThat(actual.getReqId()).isEqualTo(0);
            softly.assertThat(actual.getLogHostname()).isNotNull();
            softly.assertThat(actual.getLogTime()).isNotEmpty();
            softly.assertThat(actual.getLogType()).isEqualTo(LOG_TYPE);
            softly.assertThat(actual.getData()).isEqualTo(payload);
        }
    }

    @Test
    public void getPartitionedEntriesStream_returnOneItem_whenLessThan1000Elements() throws Exception {
        List<Integer> payload = IntStreamEx.range(PARTITION_SIZE).boxed().toList();
        Stream<LogEntry<List<Integer>>> actualStream = logHelper.getPartitionedEntriesStream(payload, 0L);
        List<LogEntry<List<Integer>>> actualLogEntries = actualStream.collect(Collectors.toList());

        assertThat(actualLogEntries).hasSize(1);
        assertThat(actualLogEntries.get(0).getData()).hasSize(PARTITION_SIZE);
    }

    @Test
    public void getPartitionedEntriesStream_returnTwoItems_whenMoreThan1000Elements() throws Exception {
        List<Integer> payload = IntStreamEx.range(PARTITION_SIZE + 1).boxed().toList();
        Stream<LogEntry<List<Integer>>> actualStream = logHelper.getPartitionedEntriesStream(payload, 0L);
        List<LogEntry<List<Integer>>> actualLogEntries = actualStream.collect(Collectors.toList());

        assertThat(actualLogEntries).hasSize(2);
        assertThat(actualLogEntries.get(0).getData()).hasSize(PARTITION_SIZE);
        assertThat(actualLogEntries.get(1).getData()).hasSize(1);
    }

}

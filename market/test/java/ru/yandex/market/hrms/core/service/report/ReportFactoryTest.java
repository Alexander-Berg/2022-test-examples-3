package ru.yandex.market.hrms.core.service.report;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Stream;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;
import org.junit.jupiter.params.provider.ArgumentsSource;
import org.mockserver.serialization.ObjectMapperFactory;
import org.skyscreamer.jsonassert.JSONAssert;

import ru.yandex.market.hrms.core.AbstractCoreTest;
import ru.yandex.market.hrms.core.domain.report.events.LoggedOperationEvent;
import ru.yandex.market.hrms.core.domain.report.events.LoggedTimexEvent;
import ru.yandex.market.hrms.core.domain.report.events.ScheduledWorkEvent;
import ru.yandex.market.hrms.core.service.analyzer.ActivityLogSource;


class ReportFactoryTest extends AbstractCoreTest {

    private final ObjectMapper objectMapper = ObjectMapperFactory.createObjectMapper()
            .findAndRegisterModules();

    @Test
    public void empty() throws Exception {

        var date = LocalDate.parse("2022-05-06");
        var timeStart = LocalTime.of(9, 0);
        var timeEnd = LocalTime.of(21, 0);

        var startedAt = toInstantUTC(date, timeStart);
        var endedAt = toInstantUTC(date, timeEnd);

        var workShiftInterval = new ScheduledWorkEvent(1L, 1L, date, startedAt, endedAt, 1L);

        var reportData = ReportFactory.toReportData(workShiftInterval, List.of(), true, false);
        var BASE_JSON = """
                {
                    "hasTimexLogs": false,
                    "hasTimexLogsInOperationArea": false,
                    "npoDuration": 0.0,
                    "npoDurationOutOfOperationArea": 0.0,
                    "operationAreaLogsRequired": true,
                    "operationsCount": 0,
                    "operationsCountOutOfOperationArea": 0,
                    "scheduledEnd": "2022-05-06T21:00:00Z",
                    "scheduledStart": "2022-05-06T09:00:00Z",
                    "scheduledWorkingTime": 39600.0
                }
                """;
        JSONAssert.assertEquals(BASE_JSON, objectMapper.writeValueAsString(reportData), true);
    }

    @Test
    public void emptyWhenNotConfiguredOperatingAreas() throws Exception {

        var date = LocalDate.parse("2022-05-06");
        var timeStart = LocalTime.of(9, 0);
        var timeEnd = LocalTime.of(21, 0);

        var startedAt = toInstantUTC(date, timeStart);
        var endedAt = toInstantUTC(date, timeEnd);

        var workShiftInterval = new ScheduledWorkEvent(1L, 1L, date, startedAt, endedAt, 1L);

        var reportData = ReportFactory.toReportData(workShiftInterval, List.of(), false, false);
        var BASE_JSON = """
                {
                    "hasTimexLogs": false,
                    "npoDuration": 0.0,
                    "npoDurationOutOfOperationArea": 0.0,
                    "operationsCount": 0,
                    "operationsCountOutOfOperationArea": 0,
                    "scheduledEnd": "2022-05-06T21:00:00Z",
                    "scheduledStart": "2022-05-06T09:00:00Z",
                    "scheduledWorkingTime": 43200.0
                }
                """;
        JSONAssert.assertEquals(BASE_JSON, objectMapper.writeValueAsString(reportData), true);
    }

    @ParameterizedTest
    @ArgumentsSource(TimexWithoutOperatingAreasArgumentsProviderImpl.class)
    public void timexLogsOutOfOperatingAreas(boolean isConfiguredOperationAreas, String expectedJson) throws Exception {
        var date = LocalDate.parse("2022-05-06");
        var timeStart = LocalTime.of(9, 0);
        var timeEnd = LocalTime.of(21, 0);

        var startedAt = toInstantUTC(date, timeStart);
        var endedAt = toInstantUTC(date, timeEnd);

        var workShiftInterval = new ScheduledWorkEvent(1L, 1L, date, startedAt, endedAt, 1L);
        var logs = List.of(
                new LoggedTimexEvent(1L,
                        startedAt.plus(15, ChronoUnit.MINUTES),
                        endedAt.minus(20, ChronoUnit.MINUTES),
                        false)
        );

        var reportData = ReportFactory.toReportData(workShiftInterval, logs, isConfiguredOperationAreas, false);
        JSONAssert.assertEquals(expectedJson, objectMapper.writeValueAsString(reportData), true);
    }

    static class TimexWithoutOperatingAreasArgumentsProviderImpl implements ArgumentsProvider {

        @Override
        public Stream<? extends Arguments> provideArguments(ExtensionContext context) {
            return Stream.of(
                    Arguments.of(true, """
                            {
                                "hasTimexLogs": false,
                                "hasTimexLogsInOperationArea": false,
                                "npoDuration": 0.0,
                                "npoDurationOutOfOperationArea": 0.0,
                                "operationAreaLogsRequired": true,
                                "operationsCount": 0,
                                "operationsCountOutOfOperationArea": 0,
                                "scheduledEnd": "2022-05-06T21:00:00Z",
                                "scheduledStart": "2022-05-06T09:00:00Z",
                                "scheduledWorkingTime": 39600.0
                            }
                            """),
                    Arguments.of(false, """
                            {
                                "hasTimexLogs": true,
                                "npoDuration": 0.0,
                                "npoDurationOutOfOperationArea": 0.0,
                                "operationsCount": 0,
                                "operationsCountOutOfOperationArea": 0,
                                "scheduledEnd": "2022-05-06T21:00:00Z",
                                "scheduledStart": "2022-05-06T09:00:00Z",
                                "scheduledWorkingTime": 43200.0,
                                "timexFirstLog": "2022-05-06T09:15:00Z",
                                "timexLastLog": "2022-05-06T20:40:00Z",
                                "timexWorkingTime": 41100.0
                            }
                            """)
            );
        }
    }


    @Test
    public void timexLogsInOperatingAreas() throws Exception {
        var date = LocalDate.parse("2022-05-06");
        var timeStart = LocalTime.of(9, 0);
        var timeEnd = LocalTime.of(21, 0);

        var startedAt = toInstantUTC(date, timeStart);
        var endedAt = toInstantUTC(date, timeEnd);

        var workShiftInterval = new ScheduledWorkEvent(1L, 1L, date, startedAt, endedAt, 1L);
        var logs = List.of(
                new LoggedTimexEvent(1L,
                        startedAt,
                        startedAt.plus(15, ChronoUnit.MINUTES),
                        false),
                new LoggedTimexEvent(1L,
                        startedAt.plus(15, ChronoUnit.MINUTES),
                        endedAt.minus(20, ChronoUnit.MINUTES),
                        true),
                new LoggedTimexEvent(1L,
                        startedAt.minus(20, ChronoUnit.MINUTES),
                        null,
                        false)
        );

        var reportData = ReportFactory.toReportData(workShiftInterval, logs, true, false);
        JSONAssert.assertEquals("""
                {
                    "hasTimexLogs": true,
                    "hasTimexLogsInOperationArea": true,
                    "npoDuration": 0.0,
                    "npoDurationOutOfOperationArea": 0.0,
                    "operationAreaLogsRequired": true,
                    "operationsCount": 0,
                    "operationsCountOutOfOperationArea": 0,
                    "scheduledEnd": "2022-05-06T21:00:00Z",
                    "scheduledStart": "2022-05-06T09:00:00Z",
                    "scheduledWorkingTime": 39600.0,
                    "timexFirstLog": "2022-05-06T09:15:00Z",
                    "timexLastLog": "2022-05-06T20:40:00Z",
                    "timexWorkingTime": 41100.0,
                    "inOperationAreaWorkingTime": 41100.0
                }
                """, objectMapper.writeValueAsString(reportData), true);
    }

    @Test
    public void wmsLogs() throws Exception {
        var date = LocalDate.parse("2022-05-06");
        var timeStart = LocalTime.of(9, 0);
        var timeEnd = LocalTime.of(21, 0);

        var startedAt = toInstantUTC(date, timeStart);
        var endedAt = toInstantUTC(date, timeEnd);

        var workShiftInterval = new ScheduledWorkEvent(1L, 1L, date, startedAt, endedAt, 1L);
        var logs = List.of(
                new LoggedTimexEvent(1L,
                        startedAt.plus(15, ChronoUnit.MINUTES),
                        endedAt.minus(20, ChronoUnit.MINUTES),
                        true),
                new LoggedOperationEvent(ActivityLogSource.WMS, 1L,
                        startedAt.plus(5, ChronoUnit.MINUTES),
                        startedAt.plus(100, ChronoUnit.MINUTES)),
                new LoggedOperationEvent(ActivityLogSource.WMS, 1L,
                        startedAt.plus(100, ChronoUnit.MINUTES),
                        endedAt.minus(100, ChronoUnit.MINUTES)),
                new LoggedOperationEvent(ActivityLogSource.WMS, 1L,
                        endedAt.minus(100, ChronoUnit.MINUTES),
                        endedAt.minus(5, ChronoUnit.MINUTES))
        );

        var reportData = ReportFactory.toReportData(workShiftInterval, logs, true, false);
        JSONAssert.assertEquals("""
                {
                    "hasTimexLogs": true,
                    "hasTimexLogsInOperationArea": true,
                    "npoDuration": 0.0,
                    "npoDurationOutOfOperationArea": 0.0,
                    "operationAreaLogsRequired": true,
                    "operationsCount": 3,
                    "operationsCountOutOfOperationArea": 2,
                    "scheduledEnd": "2022-05-06T21:00:00Z",
                    "scheduledStart": "2022-05-06T09:00:00Z",
                    "scheduledWorkingTime": 39600.0,
                    "timexFirstLog": "2022-05-06T09:15:00Z",
                    "timexLastLog": "2022-05-06T20:40:00Z",
                    "timexWorkingTime": 41100.0,
                    "inOperationAreaWorkingTime": 41100.0
                }
                """, objectMapper.writeValueAsString(reportData), true);
    }

    @Test
    public void wmsLogsWhenNotConfiguredOperatingAreas() throws Exception {
        var date = LocalDate.parse("2022-05-06");
        var timeStart = LocalTime.of(9, 0);
        var timeEnd = LocalTime.of(21, 0);

        var startedAt = toInstantUTC(date, timeStart);
        var endedAt = toInstantUTC(date, timeEnd);

        var workShiftInterval = new ScheduledWorkEvent(1L, 1L, date, startedAt, endedAt, 1L);
        var logs = List.of(
                new LoggedTimexEvent(1L,
                        startedAt.plus(15, ChronoUnit.MINUTES),
                        endedAt.minus(20, ChronoUnit.MINUTES),
                        false),
                new LoggedOperationEvent(ActivityLogSource.WMS, 1L,
                        startedAt.plus(5, ChronoUnit.MINUTES),
                        startedAt.plus(100, ChronoUnit.MINUTES)),
                new LoggedOperationEvent(ActivityLogSource.WMS, 1L,
                        startedAt.plus(100, ChronoUnit.MINUTES),
                        endedAt.minus(100, ChronoUnit.MINUTES)),
                new LoggedOperationEvent(ActivityLogSource.WMS, 1L,
                        endedAt.minus(100, ChronoUnit.MINUTES),
                        endedAt.minus(5, ChronoUnit.MINUTES))
        );

        var reportData = ReportFactory.toReportData(workShiftInterval, logs, false, false);
        JSONAssert.assertEquals("""
                {
                    "hasTimexLogs": true,
                    "npoDuration": 0.0,
                    "npoDurationOutOfOperationArea": 0.0,
                    "operationsCount": 3,
                    "operationsCountOutOfOperationArea": 2,
                    "scheduledEnd": "2022-05-06T21:00:00Z",
                    "scheduledStart": "2022-05-06T09:00:00Z",
                    "scheduledWorkingTime": 43200.0,
                    "timexFirstLog": "2022-05-06T09:15:00Z",
                    "timexLastLog": "2022-05-06T20:40:00Z",
                    "timexWorkingTime": 41100.0
                }
                """, objectMapper.writeValueAsString(reportData), true);
    }

    @Test
    public void npoLogs() throws Exception {
        var date = LocalDate.parse("2022-05-06");
        var timeStart = LocalTime.of(9, 0);
        var timeEnd = LocalTime.of(21, 0);

        var startedAt = toInstantUTC(date, timeStart);
        var endedAt = toInstantUTC(date, timeEnd);

        var workShiftInterval = new ScheduledWorkEvent(1L, 1L, date, startedAt, endedAt, 1L);
        var logs = List.of(
                new LoggedTimexEvent(1L,
                        startedAt.plus(15, ChronoUnit.MINUTES),
                        endedAt.minus(20, ChronoUnit.MINUTES),
                        true),
                new LoggedOperationEvent(ActivityLogSource.HRMS_NPO, 1L, startedAt, endedAt)
        );

        var reportData = ReportFactory.toReportData(workShiftInterval, logs, true, false);
        JSONAssert.assertEquals("""
                {
                    "hasTimexLogs": true,
                    "hasTimexLogsInOperationArea": true,
                    "npoDuration": 43200.0,
                    "npoDurationOutOfOperationArea": 2100.0,
                    "operationAreaLogsRequired": true,
                    "operationsCount": 0,
                    "operationsCountOutOfOperationArea": 0,
                    "scheduledEnd": "2022-05-06T21:00:00Z",
                    "scheduledStart": "2022-05-06T09:00:00Z",
                    "scheduledWorkingTime": 39600.0,
                    "timexFirstLog": "2022-05-06T09:15:00Z",
                    "timexLastLog": "2022-05-06T20:40:00Z",
                    "timexWorkingTime": 43200.0,
                    "inOperationAreaWorkingTime": 41100.0
                }
                """, objectMapper.writeValueAsString(reportData), true);
    }


    @Test
    public void npoLogsWhenNotConfiguredOperatingAreas() throws Exception {
        var date = LocalDate.parse("2022-05-06");
        var timeStart = LocalTime.of(9, 0);
        var timeEnd = LocalTime.of(21, 0);

        var startedAt = toInstantUTC(date, timeStart);
        var endedAt = toInstantUTC(date, timeEnd);

        var workShiftInterval = new ScheduledWorkEvent(1L, 1L, date, startedAt, endedAt, 1L);
        var logs = List.of(
                new LoggedTimexEvent(1L,
                        startedAt.plus(15, ChronoUnit.MINUTES),
                        endedAt.minus(20, ChronoUnit.MINUTES),
                        false),
                new LoggedOperationEvent(ActivityLogSource.HRMS_NPO, 1L, startedAt, endedAt)
        );

        var reportData = ReportFactory.toReportData(workShiftInterval, logs, false, false);
        JSONAssert.assertEquals("""
                {
                    "hasTimexLogs": true,
                    "npoDuration": 43200.0,
                    "npoDurationOutOfOperationArea": 2100.0,
                    "operationsCount": 0,
                    "operationsCountOutOfOperationArea": 0,
                    "scheduledEnd": "2022-05-06T21:00:00Z",
                    "scheduledStart": "2022-05-06T09:00:00Z",
                    "scheduledWorkingTime": 43200.0,
                    "timexFirstLog": "2022-05-06T09:15:00Z",
                    "timexLastLog": "2022-05-06T20:40:00Z",
                    "timexWorkingTime": 41100.0
                }
                """, objectMapper.writeValueAsString(reportData), true);
    }

    private Instant toInstantUTC(LocalDate date, LocalTime time) {
        return OffsetDateTime.of(date, time, ZoneOffset.UTC).toInstant();
    }
}
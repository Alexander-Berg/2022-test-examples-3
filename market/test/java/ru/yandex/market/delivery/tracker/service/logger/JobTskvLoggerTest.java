package ru.yandex.market.delivery.tracker.service.logger;

import java.text.SimpleDateFormat;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.Map;
import java.util.TimeZone;

import com.google.common.collect.ImmutableMap;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobKey;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

class JobTskvLoggerTest {
    @Mock
    private TskvLogger tskvLogger;

    @InjectMocks
    private JobTskvLogger jobTskvLogger;

    private final Clock clock = Clock.fixed(Instant.parse("2020-01-01T00:00:00Z"), ZoneId.of("UTC"));

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);
        jobTskvLogger = new JobTskvLogger(tskvLogger, clock);
    }

    @Test
    void logExecutionTime() {
        String jobName = RandomStringUtils.randomAlphabetic(10);
        String instanceId = RandomStringUtils.randomAlphabetic(10);
        Date jobStartDate = Date.from(clock.instant().plus(1, ChronoUnit.MINUTES));
        Date scheduledFireTime = Date.from(clock.instant());

        SimpleDateFormat format = new SimpleDateFormat(TskvLogger.DEFAULT_DATE_TIME_PATTERN);
        format.setTimeZone(TimeZone.getTimeZone("UTC"));
        when(tskvLogger.formatDate(ArgumentMatchers.any(Date.class)))
            .thenAnswer(args -> format.format(args.getArgument(0)));

        JobExecutionContext context = mock(JobExecutionContext.class);
        JobDetail jobDetail = mock(JobDetail.class);
        JobKey jobKey = new JobKey(jobName);

        when(context.getJobDetail()).thenReturn(jobDetail);
        when(context.getFireInstanceId()).thenReturn(instanceId);
        when(context.getScheduledFireTime()).thenReturn(scheduledFireTime);
        when(jobDetail.getKey()).thenReturn(jobKey);

        jobTskvLogger.logExecutionTime(context, jobStartDate);

        Map<String, String> expectedTskvMap = ImmutableMap.<String, String>builder()
            .put("jobName", jobName)
            .put("instanceId", instanceId)
            .put("scheduledFireTs", "2020-01-01 00:00:00.000+0000")
            .put("jobStartTs", "2020-01-01 00:01:00.000+0000")
            .put("jobProcessingTimeMs", "-60000") // expected because of clock skew
            .build();

        verify(context, times(1)).getJobDetail();
        verify(context, times(1)).getFireInstanceId();
        verify(context, times(1)).getScheduledFireTime();
        verify(jobDetail, times(1)).getKey();
        verifyZeroInteractions(context, jobDetail);

        verify(tskvLogger).log(expectedTskvMap);
    }
}

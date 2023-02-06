package ru.yandex.market.tms.quartz2.service;

import java.sql.Timestamp;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import ru.yandex.market.tms.quartz2.model.JobMonitoringResult;
import ru.yandex.market.tms.quartz2.model.MonitoringConfigInfo;
import ru.yandex.market.tms.quartz2.model.MonitoringStatus;
import ru.yandex.market.tms.quartz2.model.TmsMonitoringResult;
import ru.yandex.market.tms.quartz2.spring.config.JdbcTestConfig;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author otedikova
 */
@SpringJUnitConfig(classes = JdbcTestConfig.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class TmsMonitoringServiceTest {
    private static final String TEST_QRTZ_LOG_TABLE_NAME = "TEST_QRTZ_LOG";
    private static final String INSERT_LOG_ENTRY_QUERY = "insert into " + TEST_QRTZ_LOG_TABLE_NAME
            + " (JOB_NAME, JOB_GROUP, TRIGGER_FIRE_TIME, JOB_FINISHED_TIME, JOB_STATUS, HOST_NAME) "
            + "values (?, 'DEFAULT', ?, ?, ?, 'host')";
    private static final ZoneId MOSCOW_TIME_ZONE = ZoneId.of("Europe/Moscow");
    @Autowired
    private TmsMonitoringService tmsMonitoringService;
    @Autowired
    private JdbcTemplate jdbcTemplate;
    private MonitoringConfigInfo defaultMonitoringConfig;

    @BeforeEach
    public void setUp() {
        defaultMonitoringConfig = tmsMonitoringService.getDefaultMonitoringConfig();
    }

    /**
     * Тест на CRIT мониторинг для джоб, которые ни разу не завершились.
     */
    @Test
    void testNeverFinishedJobsResult() {
        tmsMonitoringService.addMonitoringConfig(monitoringConfigInfo("job1", defaultMonitoringConfig));
        tmsMonitoringService.addMonitoringConfig(monitoringConfigInfo("job2", defaultMonitoringConfig));
        //job1 ни разу не запускалась
        //job2 запустилась, но не закончилась
        createCurrentlyExecutingJobEntry("job2", OffsetDateTime.now(MOSCOW_TIME_ZONE).minusMinutes(5));

        TmsMonitoringResult tmsMonitoringResult = tmsMonitoringService.getTmsMonitoringResult(null);

        assertEquals(MonitoringStatus.CRIT, tmsMonitoringResult.getMonitoringStatus());
        Map<String, JobMonitoringResult> jobResults = tmsMonitoringResult.getJobResults().stream().collect(
                Collectors.toMap(JobMonitoringResult::getJobName, Function.identity()));
        assertEquals(jobResults.size(), 2);

        JobMonitoringResult job1MonitoringResult = jobResults.get("job1");
        assertEquals(MonitoringStatus.CRIT, job1MonitoringResult.getMonitoringStatus());
        assertEquals("Job job1 has never been started yet.", getErrorMessage(job1MonitoringResult));

        JobMonitoringResult job2MonitoringResult = jobResults.get("job2");
        assertEquals(MonitoringStatus.CRIT, job2MonitoringResult.getMonitoringStatus());
        assertEquals("Job job2 has never been finished yet.", getErrorMessage(job2MonitoringResult));
    }

    /**
     * Тест на WARN мониторинг для джоб, которые ни разу не завершились.
     */
    @Test
    void testNeverFinishedJobsResultWithCustom() {
        MonitoringConfigInfo job1Config = monitoringConfigInfo("job1", defaultMonitoringConfig);
        job1Config.setOnNotStartedYetStatus(MonitoringStatus.WARN);
        MonitoringConfigInfo job2Config = monitoringConfigInfo("job2", defaultMonitoringConfig);
        job2Config.setOnNeverBeenFinishedYetStatus(MonitoringStatus.WARN);
        tmsMonitoringService.addMonitoringConfig(job1Config);
        tmsMonitoringService.addMonitoringConfig(job2Config);
        //job1 ни разу не запускалась
        //job2 запустилась, но не закончилась
        createCurrentlyExecutingJobEntry("job2", OffsetDateTime.now(MOSCOW_TIME_ZONE).minusMinutes(5));

        TmsMonitoringResult tmsMonitoringResult = tmsMonitoringService.getTmsMonitoringResult(null);

        assertEquals(MonitoringStatus.WARN, tmsMonitoringResult.getMonitoringStatus());
        Map<String, JobMonitoringResult> jobResults = tmsMonitoringResult.getJobResults().stream().collect(
                Collectors.toMap(JobMonitoringResult::getJobName, Function.identity()));
        assertEquals(jobResults.size(), 2);

        JobMonitoringResult job1MonitoringResult = jobResults.get("job1");
        assertEquals(MonitoringStatus.WARN, job1MonitoringResult.getMonitoringStatus());
        assertEquals("Job job1 has never been started yet.", getErrorMessage(job1MonitoringResult));

        JobMonitoringResult job2MonitoringResult = jobResults.get("job2");
        assertEquals(MonitoringStatus.WARN, job2MonitoringResult.getMonitoringStatus());
        assertEquals("Job job2 has never been finished yet.", getErrorMessage(job2MonitoringResult));
    }

    /**
     * Тест на CRIT мониторинг для джоб, которые ни разу не завершились несколько раз подряд.
     */
    @Test
    void testTwiceNeverFinishedJobsResultWithWarn() {
        tmsMonitoringService.addMonitoringConfig(monitoringConfigInfo("job1", defaultMonitoringConfig));
        tmsMonitoringService.addMonitoringConfig(monitoringConfigInfo("job2", defaultMonitoringConfig));

        // История запусков в обратной хронологии
        // 00:00 - 00:01 job1 OK
        // 00:05 -       job1 запустилась, но не закончилась
        // 00:10 -       job1 запустилась, но не закончилась
        OffsetDateTime startTime = OffsetDateTime.now(MOSCOW_TIME_ZONE).minusMinutes(60);
        createQrtzLogEntry("job1", startTime, startTime.plusMinutes(1), "OK");
        createCurrentlyExecutingJobEntry("job1", startTime.plusMinutes(5));
        createCurrentlyExecutingJobEntry("job1", startTime.plusMinutes(10));

        // История запусков в обратной хронологии
        // 00:05 -       job2 запустилась, но не закончилась
        // 00:10 -       job2 запустилась, но не закончилась
        createCurrentlyExecutingJobEntry("job2", startTime.plusMinutes(5));
        createCurrentlyExecutingJobEntry("job2", startTime.plusMinutes(10));

        TmsMonitoringResult tmsMonitoringResult = tmsMonitoringService.getTmsMonitoringResult(null);

        assertEquals(MonitoringStatus.CRIT, tmsMonitoringResult.getMonitoringStatus());
        Map<String, JobMonitoringResult> jobResults = tmsMonitoringResult.getJobResults().stream().collect(
                Collectors.toMap(JobMonitoringResult::getJobName, Function.identity()));
        assertEquals(jobResults.size(), 2);

        JobMonitoringResult job1MonitoringResult = jobResults.get("job1");
        assertEquals(MonitoringStatus.CRIT, job1MonitoringResult.getMonitoringStatus());
        assertEquals("Job job1 has no error message." +
                        " Possible due to hard restart during job execution",
                getErrorMessage(job1MonitoringResult));

        JobMonitoringResult job2MonitoringResult = jobResults.get("job2");
        assertEquals(MonitoringStatus.CRIT, job2MonitoringResult.getMonitoringStatus());
        assertEquals("Job job2 has no error message. Possible due to hard restart during job execution",
                getErrorMessage(job2MonitoringResult));
    }

    /**
     * Тест на WARN мониторинг для джоб, которые ни разу не завершились несколько раз подряд.
     */
    @Test
    void testTwiceNeverFinishedJobsResult() {
        tmsMonitoringService.addMonitoringConfig(monitoringConfigInfo("job1", 3, 1, "", -1, -1));
        tmsMonitoringService.addMonitoringConfig(monitoringConfigInfo("job2", 3, 1, "", -1, -1));

        // История запусков в обратной хронологии
        // 00:00 - 00:01 job1 OK
        // 00:05 -       job1 запустилась, но не закончилась
        // 00:10 -       job1 запустилась, но не закончилась
        OffsetDateTime startTime = OffsetDateTime.now(MOSCOW_TIME_ZONE).minusMinutes(60);
        createQrtzLogEntry("job1", startTime, startTime.plusMinutes(1), "OK");
        createCurrentlyExecutingJobEntry("job1", startTime.plusMinutes(5));
        createCurrentlyExecutingJobEntry("job1", startTime.plusMinutes(10));

        // История запусков в обратной хронологии
        // 00:05 -       job2 запустилась, но не закончилась
        // 00:10 -       job2 запустилась, но не закончилась
        createCurrentlyExecutingJobEntry("job2", startTime.plusMinutes(5));
        createCurrentlyExecutingJobEntry("job2", startTime.plusMinutes(10));

        TmsMonitoringResult tmsMonitoringResult = tmsMonitoringService.getTmsMonitoringResult(null);

        assertEquals(MonitoringStatus.WARN, tmsMonitoringResult.getMonitoringStatus());
        Map<String, JobMonitoringResult> jobResults = tmsMonitoringResult.getJobResults().stream().collect(
                Collectors.toMap(JobMonitoringResult::getJobName, Function.identity()));
        assertEquals(jobResults.size(), 2);

        JobMonitoringResult job1MonitoringResult = jobResults.get("job1");
        assertEquals(MonitoringStatus.WARN, job1MonitoringResult.getMonitoringStatus());
        assertEquals("Job job1 has no error message. Possible due to hard restart during job execution",
                getErrorMessage(job1MonitoringResult));

        JobMonitoringResult job2MonitoringResult = jobResults.get("job2");
        assertEquals(MonitoringStatus.WARN, job2MonitoringResult.getMonitoringStatus());
        assertEquals("Job job2 has no error message. Possible due to hard restart during job execution",
                getErrorMessage(job2MonitoringResult));
    }

    /**
     * Тест на CRIT мониторинг для джоб,
     * - у которых предыдущий запуск был итогового состояния (крайнемаловероятная ситуация),
     * - а текущий не завершился.
     */
    @Test
    void testLastNeverFinishedAndPreviosWasFailedButWithoutException() {
        tmsMonitoringService.addMonitoringConfig(monitoringConfigInfo("job1", defaultMonitoringConfig));

        // История запусков в обратной хронологии
        // 00:05 - 00:06 job1 Failed
        // 00:10 -       job1 запустилась, но не закончилась
        OffsetDateTime startTime = OffsetDateTime.now(MOSCOW_TIME_ZONE).minusMinutes(60);
        createQrtzLogEntry("job1", startTime.plusMinutes(5), startTime.plusMinutes(6), null);
        createCurrentlyExecutingJobEntry("job1", startTime.plusMinutes(10));

        TmsMonitoringResult tmsMonitoringResult = tmsMonitoringService.getTmsMonitoringResult(null);

        assertEquals(MonitoringStatus.CRIT, tmsMonitoringResult.getMonitoringStatus());
        Map<String, JobMonitoringResult> jobResults = tmsMonitoringResult.getJobResults().stream().collect(
                Collectors.toMap(JobMonitoringResult::getJobName, Function.identity()));
        assertEquals(jobResults.size(), 1);

        JobMonitoringResult job1MonitoringResult = jobResults.get("job1");
        assertEquals(MonitoringStatus.CRIT, job1MonitoringResult.getMonitoringStatus());
        assertEquals("Job job1 has no error message.", getErrorMessage(job1MonitoringResult));
    }

    /**
     * Тест на WARN мониторинг для джоб,
     * - у которых предыдущий запуск был итогового состояния (крайнемаловероятная ситуация),
     * - а текущий не завершился.
     */
    @Test
    void testLastNeverFinishedAndPreviosWasFailedButWithoutExceptionWarn() {
        tmsMonitoringService.addMonitoringConfig(monitoringConfigInfo("job1", 2, 1, "", -1, -1));

        // История запусков в обратной хронологии
        // 00:05 - 00:06 job1 Failed
        // 00:10 -       job1 запустилась, но не закончилась
        OffsetDateTime startTime = OffsetDateTime.now(MOSCOW_TIME_ZONE).minusMinutes(60);
        createQrtzLogEntry("job1", startTime.plusMinutes(5), startTime.plusMinutes(6), null);
        createCurrentlyExecutingJobEntry("job1", startTime.plusMinutes(10));

        TmsMonitoringResult tmsMonitoringResult = tmsMonitoringService.getTmsMonitoringResult(null);

        assertEquals(MonitoringStatus.WARN, tmsMonitoringResult.getMonitoringStatus());
        Map<String, JobMonitoringResult> jobResults = tmsMonitoringResult.getJobResults().stream().collect(
                Collectors.toMap(JobMonitoringResult::getJobName, Function.identity()));
        assertEquals(jobResults.size(), 1);

        JobMonitoringResult job1MonitoringResult = jobResults.get("job1");
        assertEquals(MonitoringStatus.WARN, job1MonitoringResult.getMonitoringStatus());
        assertEquals("Job job1 has no error message.", getErrorMessage(job1MonitoringResult));
    }

    /**
     * Тест на CRIT мониторинг для джоб завершившихся ошибкой. Дефолтные настройки.
     */
    @Test
    void testJobErrorCritResultDefaultConfig() {
        List<String> jobNames = Lists.newArrayList("job1", "job2", "job3");
        tmsMonitoringService.addMonitoringConfig(monitoringConfigInfo("job1", defaultMonitoringConfig));
        tmsMonitoringService.addMonitoringConfig(monitoringConfigInfo("job2", defaultMonitoringConfig));
        tmsMonitoringService.addMonitoringConfig(monitoringConfigInfo("job3", defaultMonitoringConfig));
        //job1 запустилась 1 раз и закончилась ошибкой
        createQrtzLogEntry("job1", OffsetDateTime.now(MOSCOW_TIME_ZONE).minusSeconds(10),
                OffsetDateTime.now(MOSCOW_TIME_ZONE).minusSeconds(8), "job1 Internal failure");
        //job2 запустилась 2 раза. Первый запуск закончился ошибкой, 2й еще идет
        createQrtzLogEntry("job2", OffsetDateTime.now(MOSCOW_TIME_ZONE).minusSeconds(20),
                OffsetDateTime.now(MOSCOW_TIME_ZONE).minusSeconds(15), "job2 Internal failure");
        createQrtzLogEntry("job2", OffsetDateTime.now(MOSCOW_TIME_ZONE).minusSeconds(10),
                null, null);
        //job2 запустилась 2 раза. Первый запуск закончился успешно, 2й - ошибкой.
        createQrtzLogEntry("job3", OffsetDateTime.now(MOSCOW_TIME_ZONE).minusSeconds(20),
                OffsetDateTime.now(MOSCOW_TIME_ZONE).minusSeconds(15), "OK");
        createQrtzLogEntry("job3", OffsetDateTime.now(MOSCOW_TIME_ZONE).minusSeconds(10),
                OffsetDateTime.now(MOSCOW_TIME_ZONE).minusSeconds(5), "job3 Internal failure");

        TmsMonitoringResult tmsMonitoringResult = tmsMonitoringService.getTmsMonitoringResult(null);

        assertEquals(MonitoringStatus.CRIT, tmsMonitoringResult.getMonitoringStatus());
        Map<String, JobMonitoringResult> jobResults = tmsMonitoringResult.getJobResults().stream().collect(
                Collectors.toMap(JobMonitoringResult::getJobName, Function.identity()));
        for (String jobName : jobNames) {
            JobMonitoringResult jobMonitoringResult = jobResults.get(jobName);
            assertEquals(MonitoringStatus.CRIT, jobMonitoringResult.getMonitoringStatus());
            assertEquals(String.format("%s Internal failure", jobName), getErrorMessage(jobMonitoringResult));
        }
    }

    /**
     * Тест на CRIT мониторинг для джоб завершившихся ошибкой. Недефолтные настройки CRIT.
     */
    @Test
    void testJobErrorCritResult() {
        String jobName = "job1";

        tmsMonitoringService.addMonitoringConfig(monitoringConfigInfo(jobName, 3, 1, "", -1, -1));
        //job1 запустилась 4 раза, 1й успешно, 3 других - с ошибкой.

        createQrtzLogEntry(jobName, OffsetDateTime.now(MOSCOW_TIME_ZONE).minusSeconds(40),
                OffsetDateTime.now(MOSCOW_TIME_ZONE).minusSeconds(38), "OK");
        createQrtzLogEntry(jobName, OffsetDateTime.now(MOSCOW_TIME_ZONE).minusSeconds(30),
                OffsetDateTime.now(MOSCOW_TIME_ZONE).minusSeconds(28), "job1 Internal failure 1");
        createQrtzLogEntry(jobName, OffsetDateTime.now(MOSCOW_TIME_ZONE).minusSeconds(20),
                OffsetDateTime.now(MOSCOW_TIME_ZONE).minusSeconds(18), "job1 Internal failure 2");
        createQrtzLogEntry(jobName, OffsetDateTime.now(MOSCOW_TIME_ZONE).minusSeconds(20),
                OffsetDateTime.now(MOSCOW_TIME_ZONE).minusSeconds(18), "job1 Internal failure 3");

        TmsMonitoringResult tmsMonitoringResult = tmsMonitoringService.getTmsMonitoringResult(null);

        assertEquals(MonitoringStatus.CRIT, tmsMonitoringResult.getMonitoringStatus());
        Map<String, JobMonitoringResult> jobResults = tmsMonitoringResult.getJobResults().stream().collect(
                Collectors.toMap(JobMonitoringResult::getJobName, Function.identity()));
        JobMonitoringResult jobMonitoringResult = jobResults.get(jobName);
        assertEquals(MonitoringStatus.CRIT, jobMonitoringResult.getMonitoringStatus());
        assertEquals(String.format("%s Internal failure 3", jobName), getErrorMessage(jobMonitoringResult));
    }

    /**
     * Тест на WARNING мониторинг для джоб завершившихся ошибкой.
     */
    @Test
    void testJobErrorWARNResult() {
        String jobName1 = "job1";
        String jobName2 = "job2";
        String jobName3 = "job3";
        tmsMonitoringService.addMonitoringConfig(monitoringConfigInfo(jobName1, 3, 1, "", -1, -1));
        tmsMonitoringService.addMonitoringConfig(monitoringConfigInfo(jobName2, 4, 2, "", -1, -1));
        tmsMonitoringService.addMonitoringConfig(monitoringConfigInfo(jobName3, 2, 1, "", -1, -1));
        //job1 запустилась 2 раза: 1й успешно, 2й - с ошибкой.
        createQrtzLogEntry(jobName1, OffsetDateTime.now(MOSCOW_TIME_ZONE).minusSeconds(40),
                OffsetDateTime.now(MOSCOW_TIME_ZONE).minusSeconds(38), "OK");
        createQrtzLogEntry(jobName1, OffsetDateTime.now(MOSCOW_TIME_ZONE).minusSeconds(30),
                OffsetDateTime.now(MOSCOW_TIME_ZONE).minusSeconds(28), "job1 Internal failure.");
        //job2 запустилась 4 раза: 1й успешно, 2й, 3й и 4й - с ошибками .
        createQrtzLogEntry(jobName2, OffsetDateTime.now(MOSCOW_TIME_ZONE).minusSeconds(40),
                OffsetDateTime.now(MOSCOW_TIME_ZONE).minusSeconds(38), "OK");
        createQrtzLogEntry(jobName2, OffsetDateTime.now(MOSCOW_TIME_ZONE).minusSeconds(30),
                OffsetDateTime.now(MOSCOW_TIME_ZONE).minusSeconds(28), "job2 Internal failure 1.");
        createQrtzLogEntry(jobName2, OffsetDateTime.now(MOSCOW_TIME_ZONE).minusSeconds(20),
                OffsetDateTime.now(MOSCOW_TIME_ZONE).minusSeconds(18), "job2 Internal failure 2.");
        createQrtzLogEntry(jobName2, OffsetDateTime.now(MOSCOW_TIME_ZONE).minusSeconds(10),
                OffsetDateTime.now(MOSCOW_TIME_ZONE).minusSeconds(8), "job2 Internal failure 3.");
        //job3 запустилась 1 раз с ошибкой
        createQrtzLogEntry(jobName3, OffsetDateTime.now(MOSCOW_TIME_ZONE).minusSeconds(10),
                OffsetDateTime.now(MOSCOW_TIME_ZONE).minusSeconds(8), "job3 Internal failure.");

        TmsMonitoringResult tmsMonitoringResult = tmsMonitoringService.getTmsMonitoringResult(null);

        assertEquals(MonitoringStatus.WARN, tmsMonitoringResult.getMonitoringStatus());
        Map<String, JobMonitoringResult> jobResults = tmsMonitoringResult.getJobResults().stream().collect(
                Collectors.toMap(JobMonitoringResult::getJobName, Function.identity()));

        JobMonitoringResult jobMonitoringResult1 = jobResults.get(jobName1);
        assertEquals(MonitoringStatus.WARN, jobMonitoringResult1.getMonitoringStatus());
        assertEquals(String.format("%s Internal failure.", jobName1), getErrorMessage(jobMonitoringResult1));

        JobMonitoringResult jobMonitoringResult2 = jobResults.get(jobName2);
        assertEquals(MonitoringStatus.WARN, jobMonitoringResult2.getMonitoringStatus());
        assertEquals(String.format("%s Internal failure 3.", jobName2), getErrorMessage(jobMonitoringResult2));

        JobMonitoringResult jobMonitoringResult3 = jobResults.get(jobName3);
        assertEquals(MonitoringStatus.WARN, jobMonitoringResult3.getMonitoringStatus());
        assertEquals(String.format("%s Internal failure.", jobName3), getErrorMessage(jobMonitoringResult3));

    }

    /**
     * Тест на ОК мониторинг.
     */
    @Test
    void testJobOKResult() {
        List<String> jobNames = Lists.newArrayList("job1", "job2", "job3", "job4", "job5");
        tmsMonitoringService.addMonitoringConfig(monitoringConfigInfo("job1", 4, 2, "", -1, -1));
        tmsMonitoringService.addMonitoringConfig(monitoringConfigInfo("job2", 4, 2, "", -1, -1));
        tmsMonitoringService.addMonitoringConfig(monitoringConfigInfo("job3", defaultMonitoringConfig));
        tmsMonitoringService.addMonitoringConfig(monitoringConfigInfo("job4", 2, 1, "", -1, 120000));
        tmsMonitoringService.addMonitoringConfig(monitoringConfigInfo("job5", 1, 1, "", 120000, -1));
        //job1 запустилась 1 раз с ошибкой. Для нее сконфигурировано 4 падения для CRIT и 2 для WARN.
        createQrtzLogEntry("job1", OffsetDateTime.now(MOSCOW_TIME_ZONE).minusSeconds(10),
                OffsetDateTime.now(MOSCOW_TIME_ZONE).minusSeconds(8), "job1 Internal failure.");
        //job2 запустилась 3 раза. 1й и 3й с ошибкой, 2й успешно. Для нее сконфигурировано 4 падения для CRIT и 2 для
        // WARN.
        createQrtzLogEntry("job2", OffsetDateTime.now(MOSCOW_TIME_ZONE).minusSeconds(30),
                OffsetDateTime.now(MOSCOW_TIME_ZONE).minusSeconds(28), "job2 Internal failure 1.");
        createQrtzLogEntry("job2", OffsetDateTime.now(MOSCOW_TIME_ZONE).minusSeconds(20),
                OffsetDateTime.now(MOSCOW_TIME_ZONE).minusSeconds(18), "OK");
        createQrtzLogEntry("job2", OffsetDateTime.now(MOSCOW_TIME_ZONE).minusSeconds(10),
                OffsetDateTime.now(MOSCOW_TIME_ZONE).minusSeconds(8), "job2 Internal failure 2.");
        //job3 запустилась 2 раза. 1й с ошибкой, 2й - успешно.
        createQrtzLogEntry("job3", OffsetDateTime.now(MOSCOW_TIME_ZONE).minusSeconds(20),
                OffsetDateTime.now(MOSCOW_TIME_ZONE).minusSeconds(18), "job3 Internal failure.");
        createQrtzLogEntry("job3", OffsetDateTime.now(MOSCOW_TIME_ZONE).minusSeconds(10),
                OffsetDateTime.now(MOSCOW_TIME_ZONE).minusSeconds(8), "OK");
        //job4 запустилась 2 раза, 1й успешно, 2й еще идет. У джобы сконфигурировано максимальное время выполнения 2
        // мин.
        //джоба выполняется минуту
        createQrtzLogEntry("job4", OffsetDateTime.now(MOSCOW_TIME_ZONE).minusMinutes(10),
                OffsetDateTime.now(MOSCOW_TIME_ZONE).minusMinutes(9), "OK");
        createCurrentlyExecutingJobEntry("job4", OffsetDateTime.now(MOSCOW_TIME_ZONE).minusMinutes(1));
        //job5 запустилась успешно 1 раз. У джобы сконфигурировано максимальное время задержки выполнения 2 мин.
        //джоба выполнилась минуту назад
        createQrtzLogEntry("job5", OffsetDateTime.now(MOSCOW_TIME_ZONE).minusMinutes(2),
                OffsetDateTime.now(MOSCOW_TIME_ZONE).minusMinutes(1), "OK");


        TmsMonitoringResult tmsMonitoringResult = tmsMonitoringService.getTmsMonitoringResult(null);

        assertEquals(MonitoringStatus.OK, tmsMonitoringResult.getMonitoringStatus());
        Map<String, JobMonitoringResult> jobResults = tmsMonitoringResult.getJobResults().stream().collect(
                Collectors.toMap(JobMonitoringResult::getJobName, Function.identity()));
        for (String jobName : jobNames) {
            JobMonitoringResult jobMonitoringResult = jobResults.get(jobName);
            assertEquals(MonitoringStatus.OK, jobMonitoringResult.getMonitoringStatus());
            assertEquals(0, jobMonitoringResult.getErrorMessages().size());
        }
    }

    /**
     * Тест на CRIT мониторинг для затянувшихся джоб
     */
    @Test
    void testExecutionTimeExceedJobsResults() {
        tmsMonitoringService.addMonitoringConfig(monitoringConfigInfo("job1",
                1, 1, null, -1, 120000));
        tmsMonitoringService.addMonitoringConfig(monitoringConfigInfo("job2",
                2, 2, null, -1, 120000));
        //job1 запустилась 1 раз, но не закончилась. Для нее сконфигурировано максимальное время выполнения 2 мин,
        // джоба работает 2 мин и секунду.
        createCurrentlyExecutingJobEntry("job1", OffsetDateTime.now(MOSCOW_TIME_ZONE).minusMinutes(2).minusSeconds(1));
        //job2 запустилась 2 раза, 1й запуск успешен, 2й не закончился. Для нее сконфигурировано максимальное время
        // выполнения 2 мин, джоба работает 2 мин и секунду.
        createQrtzLogEntry("job2", OffsetDateTime.now(MOSCOW_TIME_ZONE).minusMinutes(10),
                OffsetDateTime.now(MOSCOW_TIME_ZONE).minusMinutes(8), "OK");
        createCurrentlyExecutingJobEntry("job2", OffsetDateTime.now(MOSCOW_TIME_ZONE).minusMinutes(2).minusSeconds(1));
        TmsMonitoringResult tmsMonitoringResult = tmsMonitoringService.getTmsMonitoringResult(null);

        assertEquals(MonitoringStatus.CRIT, tmsMonitoringResult.getMonitoringStatus());
        Map<String, JobMonitoringResult> jobResults = tmsMonitoringResult.getJobResults().stream().collect(
                Collectors.toMap(JobMonitoringResult::getJobName, Function.identity()));
        assertEquals(jobResults.size(), 2);

        JobMonitoringResult job1MonitoringResult = jobResults.get("job1");
        assertEquals(MonitoringStatus.CRIT, job1MonitoringResult.getMonitoringStatus());
        assertEquals("Job job1 has never been finished yet. " +
                        "Job job1 execution time exceeded. Max: 120 seconds, actual: 121 seconds.",
                getErrorMessage(job1MonitoringResult));

        JobMonitoringResult job2MonitoringResult = jobResults.get("job2");
        assertEquals(MonitoringStatus.CRIT, job2MonitoringResult.getMonitoringStatus());
        assertEquals("Job job2 execution time exceeded. Max: 120 seconds, actual: 121 seconds.",
                getErrorMessage(job2MonitoringResult));
    }

    /**
     * Тест на CRIT мониторинг для джобы с задержкой запуска
     */
    @Test
    void testDelayTimeExceedJobsResults() {
        String jobName1 = "job1";
        String jobName2 = "job2";
        tmsMonitoringService.addMonitoringConfig(monitoringConfigInfo("job1",
                1, 1, null, 600000, -1));
        tmsMonitoringService.addMonitoringConfig(monitoringConfigInfo("job2",
                1, 1, null, 600000, -1));
        //job1 запустилась успешно 1 раз, но время 2го запуска задерживается на секунду.
        createQrtzLogEntry("job1", OffsetDateTime.now(MOSCOW_TIME_ZONE).minusMinutes(11),
                OffsetDateTime.now(MOSCOW_TIME_ZONE).minusMinutes(10).minusSeconds(1), "OK");
        //job2 запустилась 1 раз и упала, время 2го запуска задерживается на секунду.
        createQrtzLogEntry("job2", OffsetDateTime.now(MOSCOW_TIME_ZONE).minusMinutes(11),
                OffsetDateTime.now(MOSCOW_TIME_ZONE).minusMinutes(10).minusSeconds(1), "job2 failed.");

        TmsMonitoringResult tmsMonitoringResult = tmsMonitoringService.getTmsMonitoringResult(null);

        assertEquals(MonitoringStatus.CRIT, tmsMonitoringResult.getMonitoringStatus());
        Map<String, JobMonitoringResult> jobResults = tmsMonitoringResult.getJobResults().stream().collect(
                Collectors.toMap(JobMonitoringResult::getJobName, Function.identity()));

        JobMonitoringResult jobMonitoringResult1 = jobResults.get(jobName1);
        assertEquals(MonitoringStatus.CRIT, jobMonitoringResult1.getMonitoringStatus());
        assertTrue(getErrorMessage(jobMonitoringResult1)
                .matches("Job " + jobName1 + " delay time exceeded. Max: 600 seconds, actual: 601 seconds."));

        JobMonitoringResult jobMonitoringResult2 = jobResults.get(jobName2);
        assertEquals(MonitoringStatus.CRIT, jobMonitoringResult2.getMonitoringStatus());
        assertEquals("job2 failed. Job " + jobName2 + " delay time exceeded." +
                " Max: 600 seconds, actual: 601 seconds.", getErrorMessage(jobMonitoringResult2));
    }

    /**
     * Тест на фильтрацию джоб по ответственной команде
     */
    @Test
    void testResponsibleTeamFilter() {
        tmsMonitoringService.addMonitoringConfig(monitoringConfigInfo("job1", defaultMonitoringConfig));
        tmsMonitoringService.addMonitoringConfig(monitoringConfigInfo("job2", 1, 1, "team1", -1, -1));
        tmsMonitoringService.addMonitoringConfig(monitoringConfigInfo("job3", 1, 1, "team2", -1, -1));

        //если не указана команда, возвращаем все джобы
        TmsMonitoringResult tmsMonitoringResult = tmsMonitoringService.getTmsMonitoringResult(null);

        Set<String> resultJobNames = tmsMonitoringResult.getJobResults().stream()
                .map(JobMonitoringResult::getJobName).collect(Collectors.toSet());
        assertTrue(resultJobNames.containsAll(Sets.newHashSet("job1", "job2", "job3")));

        //если указана, возвращаем с такой же командой + джобы, у которых команда не сконфигурирована
        tmsMonitoringResult = tmsMonitoringService.getTmsMonitoringResult("team2");
        resultJobNames = tmsMonitoringResult.getJobResults().stream()
                .map(JobMonitoringResult::getJobName).collect(Collectors.toSet());
        assertTrue(resultJobNames.containsAll(Sets.newHashSet("job1", "job3")));
    }

    /**
     * Тест на общий статус мониторинга по результатам всех джоб - CRIT.
     */
    @Test
    void testMergedMonitoringStatusCRIT() {
        tmsMonitoringService.addMonitoringConfig(monitoringConfigInfo("job1", 2, 1, "", -1, -1));
        tmsMonitoringService.addMonitoringConfig(monitoringConfigInfo("job2", defaultMonitoringConfig));
        tmsMonitoringService.addMonitoringConfig(monitoringConfigInfo("job3", defaultMonitoringConfig));
        //job1 WARN, job2 OK, job3 CRIT - получим CRIT
        createQrtzLogEntry("job1", OffsetDateTime.now(MOSCOW_TIME_ZONE).minusMinutes(1),
                OffsetDateTime.now(MOSCOW_TIME_ZONE), "Failed");
        createQrtzLogEntry("job2", OffsetDateTime.now(MOSCOW_TIME_ZONE).minusMinutes(1),
                OffsetDateTime.now(MOSCOW_TIME_ZONE), "OK");
        createQrtzLogEntry("job3", OffsetDateTime.now(MOSCOW_TIME_ZONE).minusMinutes(1),
                OffsetDateTime.now(MOSCOW_TIME_ZONE), "Failed");

        TmsMonitoringResult tmsMonitoringResult = tmsMonitoringService.getTmsMonitoringResult(null);

        assertEquals(MonitoringStatus.CRIT, tmsMonitoringResult.getMonitoringStatus());
    }

    /**
     * Тест на общий статус мониторинга по результатам всех джоб - WARN.
     */
    @Test
    void testMergedMonitoringStatusWARN() {
        tmsMonitoringService.addMonitoringConfig(monitoringConfigInfo("job1", 2, 1, "", -1, -1));
        tmsMonitoringService.addMonitoringConfig(monitoringConfigInfo("job2", defaultMonitoringConfig));
        //job1 WARN, job2 OK - получим WARN. job3 не учитывается
        createQrtzLogEntry("job1", OffsetDateTime.now(MOSCOW_TIME_ZONE).minusMinutes(1),
                OffsetDateTime.now(MOSCOW_TIME_ZONE), "Failed");
        createQrtzLogEntry("job2", OffsetDateTime.now(MOSCOW_TIME_ZONE).minusMinutes(1),
                OffsetDateTime.now(MOSCOW_TIME_ZONE), "OK");
        createQrtzLogEntry("job3", OffsetDateTime.now(MOSCOW_TIME_ZONE).minusMinutes(1),
                OffsetDateTime.now(MOSCOW_TIME_ZONE), "Failed");

        TmsMonitoringResult tmsMonitoringResult = tmsMonitoringService.getTmsMonitoringResult(null);

        assertEquals(MonitoringStatus.WARN, tmsMonitoringResult.getMonitoringStatus());
    }

    /**
     * CRIT, если в компоненте не сконфигурировано ни одной джобы.
     */
    @Test
    void testNoJobsConfiguredResult() {
        TmsMonitoringResult tmsMonitoringResult = tmsMonitoringService.getTmsMonitoringResult(null);
        assertEquals(MonitoringStatus.CRIT, tmsMonitoringResult.getMonitoringStatus());
        assertEquals("There are no jobs configured for this component.", tmsMonitoringResult.getCommonMessage());
    }

    @Test
    void testJobSkipStatusesFiltered() {
        final String statusToSkip = "must be ignored";
        final String statusToPass = "OK";
        tmsMonitoringService.addMonitoringConfig(new MonitoringConfigInfo("job").setJobStatusesToSkip(statusToSkip));

        createQrtzLogEntry("job", OffsetDateTime.now(MOSCOW_TIME_ZONE).minusMinutes(2),
                OffsetDateTime.now(MOSCOW_TIME_ZONE), statusToPass);
        createQrtzLogEntry("job", OffsetDateTime.now(MOSCOW_TIME_ZONE).minusMinutes(1),
                OffsetDateTime.now(MOSCOW_TIME_ZONE), statusToSkip);

        assertEquals(MonitoringStatus.OK, tmsMonitoringService.getTmsMonitoringResult(null).getMonitoringStatus());
    }

    @Test
    void testJobSkipWithEmptyStatus() {
        final String statusToSkip1 = null;
        final String statusToSkip2 = "";
        final String statusToPass = "OK";
        tmsMonitoringService.addMonitoringConfig(new MonitoringConfigInfo("job").setSkipEmptyJobStatuses(true));

        createQrtzLogEntry("job", OffsetDateTime.now(MOSCOW_TIME_ZONE).minusMinutes(3),
                OffsetDateTime.now(MOSCOW_TIME_ZONE), statusToPass);
        createQrtzLogEntry("job", OffsetDateTime.now(MOSCOW_TIME_ZONE).minusMinutes(2),
                OffsetDateTime.now(MOSCOW_TIME_ZONE), statusToSkip1);
        createQrtzLogEntry("job", OffsetDateTime.now(MOSCOW_TIME_ZONE).minusMinutes(1),
                OffsetDateTime.now(MOSCOW_TIME_ZONE), statusToSkip2);

        assertEquals(MonitoringStatus.OK, tmsMonitoringService.getTmsMonitoringResult(null).getMonitoringStatus());
    }

    @Test
    void testGetTmsMonitoringResultForTeamOnly() {
        tmsMonitoringService.addMonitoringConfig(monitoringConfigInfo("job1", defaultMonitoringConfig));
        tmsMonitoringService.addMonitoringConfig(monitoringConfigInfo("job2", 1, 1, "team1", -1, -1));
        tmsMonitoringService.addMonitoringConfig(monitoringConfigInfo("job3", 1, 1, "team2", -1, -1));
        tmsMonitoringService.addMonitoringConfig(monitoringConfigInfo("job4", 1, 1, "team1", -1, -1));

        //если не указана команда, возвращаем все джобы
        TmsMonitoringResult tmsMonitoringResult = tmsMonitoringService.getTmsMonitoringResultForTeamOnly("team1");

        Set<String> resultJobNames = tmsMonitoringResult.getJobResults().stream()
                .map(JobMonitoringResult::getJobName).collect(Collectors.toSet());
        assertTrue(resultJobNames.contains("job2"));
        assertTrue(resultJobNames.contains("job4"));
        assertEquals(2, resultJobNames.size());
    }

    @Test
    void testGetTmsMonitoringResultWithoutTeam() {
        tmsMonitoringService.addMonitoringConfig(monitoringConfigInfo("job1", defaultMonitoringConfig));
        tmsMonitoringService.addMonitoringConfig(monitoringConfigInfo("job2", 1, 1, "team1", -1, -1));
        tmsMonitoringService.addMonitoringConfig(monitoringConfigInfo("job3", 1, 1, "team2", -1, -1));
        tmsMonitoringService.addMonitoringConfig(monitoringConfigInfo("job4", 1, 1, "", -1, -1));

        TmsMonitoringResult tmsMonitoringResult = tmsMonitoringService.getTmsMonitoringResultWithoutTeam();

        Set<String> resultJobNames = tmsMonitoringResult.getJobResults().stream()
                .map(JobMonitoringResult::getJobName).collect(Collectors.toSet());
        assertTrue(resultJobNames.contains("job1"));
        assertTrue(resultJobNames.contains("job4"));
        assertEquals(2, resultJobNames.size());
    }

    @Test
    void testGetJobMonitoringResult() {
        tmsMonitoringService.addMonitoringConfig(monitoringConfigInfo("job1", 2, 1, "", -1, -1));
        tmsMonitoringService.addMonitoringConfig(monitoringConfigInfo("job2", defaultMonitoringConfig));
        tmsMonitoringService.addMonitoringConfig(monitoringConfigInfo("job3", defaultMonitoringConfig));
        tmsMonitoringService.addMonitoringConfig(monitoringConfigInfo("job4", defaultMonitoringConfig, true, "Reason"));
        //job1 WARN, job2 OK, job3 CRIT, job4 CRIT
        createQrtzLogEntry("job1", OffsetDateTime.now(MOSCOW_TIME_ZONE).minusMinutes(1),
                OffsetDateTime.now(MOSCOW_TIME_ZONE), "Failed");
        createQrtzLogEntry("job2", OffsetDateTime.now(MOSCOW_TIME_ZONE).minusMinutes(1),
                OffsetDateTime.now(MOSCOW_TIME_ZONE), "OK");
        createQrtzLogEntry("job3", OffsetDateTime.now(MOSCOW_TIME_ZONE).minusMinutes(1),
                OffsetDateTime.now(MOSCOW_TIME_ZONE), "Failed");
        createQrtzLogEntry("job4", OffsetDateTime.now(MOSCOW_TIME_ZONE).minusMinutes(1),
                OffsetDateTime.now(MOSCOW_TIME_ZONE), "Failed");

        TmsMonitoringResult tmsMonitoringResult = tmsMonitoringService.getJobMonitoringResult("unexpected_job");
        assertEquals(MonitoringStatus.CRIT, tmsMonitoringResult.getMonitoringStatus());
        assertEquals("Failed to find 'unexpected_job' job", tmsMonitoringResult.getTotalMessage());

        TmsMonitoringResult tmsMonitoringResult1 = tmsMonitoringService.getJobMonitoringResult("job1");
        assertEquals(MonitoringStatus.WARN, tmsMonitoringResult1.getMonitoringStatus());
        assertEquals("<WARN> job1 : Failed", tmsMonitoringResult1.getTotalMessage());

        TmsMonitoringResult tmsMonitoringResult2 = tmsMonitoringService.getJobMonitoringResult("job2");
        assertEquals(MonitoringStatus.OK, tmsMonitoringResult2.getMonitoringStatus());
        assertEquals("OK", tmsMonitoringResult2.getTotalMessage());

        TmsMonitoringResult tmsMonitoringResult3 = tmsMonitoringService.getJobMonitoringResult("job3");
        assertEquals(MonitoringStatus.CRIT, tmsMonitoringResult3.getMonitoringStatus());
        assertEquals("<CRIT> job3 : Failed", tmsMonitoringResult3.getTotalMessage());

        TmsMonitoringResult tmsMonitoringResult4 = tmsMonitoringService.getJobMonitoringResult("job4");
        assertEquals(MonitoringStatus.CRIT, tmsMonitoringResult4.getMonitoringStatus());
        assertEquals("<CRIT> job4 : Failed", tmsMonitoringResult4.getTotalMessage());
    }

    private void createCurrentlyExecutingJobEntry(String jobName, OffsetDateTime triggerFireTime) {
        createQrtzLogEntry(jobName, triggerFireTime, null, null);
    }

    private void createQrtzLogEntry(String jobName, OffsetDateTime triggerFireTime, OffsetDateTime jobFinishTime,
                                    String jobStatus) {
        Timestamp jobFinishTimestamp = jobFinishTime != null ? Timestamp.from(jobFinishTime.toInstant()) : null;
        jdbcTemplate.update(INSERT_LOG_ENTRY_QUERY, jobName, Timestamp.from(triggerFireTime.toInstant()),
                jobFinishTimestamp, jobStatus);
    }

    private String getErrorMessage(JobMonitoringResult job1MonitoringResult) {
        return String.join(" ", job1MonitoringResult.getErrorMessages());
    }

    private MonitoringConfigInfo monitoringConfigInfo(String jobName,
                                                      int failsToCrit,
                                                      int failsToWarn,
                                                      String responsibleTeam,
                                                      long maxDelayTimeMillis,
                                                      long maxExecutionTimeMillis) {
        return new MonitoringConfigInfo(jobName)
                .setFailsToCrit(failsToCrit)
                .setFailsToWarn(failsToWarn)
                .setResponsibleTeam(responsibleTeam)
                .setMaxDelayTimeMillis(maxDelayTimeMillis)
                .setMaxExecutionTimeMillis(maxExecutionTimeMillis);
    }

    private MonitoringConfigInfo monitoringConfigInfo(String jobName, MonitoringConfigInfo defaultMonitoringConfig) {
        return new MonitoringConfigInfo(jobName, defaultMonitoringConfig);
    }

    private MonitoringConfigInfo monitoringConfigInfo(String job, MonitoringConfigInfo monitoringConfig,
                                                      boolean skipHealth, String reason) {
        return new MonitoringConfigInfo(job,
                defaultMonitoringConfig.getFailsToCrit(),
                defaultMonitoringConfig.getFailsToWarn(),
                defaultMonitoringConfig.getResponsibleTeam(),
                defaultMonitoringConfig.getMaxDelayTimeMillis(),
                defaultMonitoringConfig.getMaxExecutionTimeMillis(),
                defaultMonitoringConfig.getOnNotStartedYetStatus(),
                defaultMonitoringConfig.getOnNeverBeenFinishedYetStatus(),
                defaultMonitoringConfig.getJobStatusesToSkip(),
                defaultMonitoringConfig.isSkipEmptyJobStatuses(),
                skipHealth,
                reason);
    }
}

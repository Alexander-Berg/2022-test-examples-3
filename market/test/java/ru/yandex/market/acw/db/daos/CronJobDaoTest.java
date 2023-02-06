package ru.yandex.market.acw.db.daos;

import java.time.LocalDateTime;
import java.util.Map;

import org.junit.jupiter.api.Test;

import ru.yandex.market.acw.config.Base;
import ru.yandex.market.acw.jooq.enums.JobStatus;
import ru.yandex.market.acw.jooq.tables.pojos.CronJob;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.data.MapEntry.entry;
import static ru.yandex.market.acw.jooq.tables.CronJob.CRON_JOB;

class CronJobDaoTest extends Base {

    @Test
    void collectLastJobStatus() {
        String task1 = "task1";
        String task2 = "task2";
        LocalDateTime time = LocalDateTime.of(2010, 1, 1, 12, 0);
        LocalDateTime timeMinusMinute = time.minusMinutes(1);
        LocalDateTime timeMinus2Minutes = time.minusMinutes(2);

        //task1
        CronJob runningJob1 = new CronJob();
        runningJob1.setTask(task1);
        runningJob1.setCreateTime(time);
        runningJob1.setStatus(JobStatus.running);
        CronJob completedJob1 = new CronJob();
        completedJob1.setTask(task1);
        completedJob1.setCreateTime(timeMinusMinute);
        completedJob1.setStatus(JobStatus.completed);
        CronJob failedJob1 = new CronJob();
        failedJob1.setTask(task1);
        failedJob1.setCreateTime(timeMinus2Minutes);
        failedJob1.setStatus(JobStatus.failed);

        //task2
        CronJob failedJob2 = new CronJob();
        failedJob2.setTask(task2);
        failedJob2.setCreateTime(time);
        failedJob2.setStatus(JobStatus.failed);
        CronJob completedJob2 = new CronJob();
        completedJob2.setTask(task2);
        completedJob2.setCreateTime(timeMinus2Minutes);
        completedJob2.setStatus(JobStatus.completed);

        configuration.dsl().newRecord(CRON_JOB, runningJob1).store();
        configuration.dsl().newRecord(CRON_JOB, completedJob1).store();
        configuration.dsl().newRecord(CRON_JOB, failedJob1).store();

        configuration.dsl().newRecord(CRON_JOB, failedJob2).store();
        configuration.dsl().newRecord(CRON_JOB, completedJob2).store();

        CronJobDao cronJobDao = new CronJobDao(configuration);

        Map<String, JobStatus> statuses = cronJobDao.collectLastJobStatus();

        assertThat(statuses).contains(
                entry("task1", JobStatus.completed),
                entry("task2", JobStatus.failed)
        );
    }
}

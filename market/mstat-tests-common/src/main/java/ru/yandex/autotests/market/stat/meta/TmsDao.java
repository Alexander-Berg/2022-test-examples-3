package ru.yandex.autotests.market.stat.meta;

import java.time.LocalDateTime;
import ru.yandex.autotests.market.stat.beans.Job;
import ru.yandex.autotests.market.stat.beans.meta.TmsRunState;

import java.util.List;

/**
 * Created by entarrion on 01.04.15.
 */
public interface TmsDao {

    TmsRunState getLastJobRun(Job job);

    TmsRunState getLastJobRun(String jobName);

    TmsRunState getLastFinishedJobRun(Job job);

    TmsRunState getNextJobAfter(TmsRunState previousJob);

    TmsRunState getJobById(TmsRunState job);

    List<TmsRunState> getJobsByIds(List<TmsRunState> job);

    List<TmsRunState> getJobRunStatesAfter(Job job, LocalDateTime after);

    Boolean checkFiredTriggersForJobs(List<TmsRunState> jobs);

    TmsRunState getLastSuccessfulJobRun(Job job);

}

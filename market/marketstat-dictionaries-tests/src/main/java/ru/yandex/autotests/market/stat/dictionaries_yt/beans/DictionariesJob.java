package ru.yandex.autotests.market.stat.dictionaries_yt.beans;

import ru.yandex.autotests.market.stat.beans.Job;

import java.time.Duration;

/**
 * Created by entarrion on 20.09.16.
 */
public class DictionariesJob implements Job {

    private String jobName;

    @Override
    public Duration getMaxDuration() {
        return Duration.ofMinutes(6);
    }

    DictionariesJob(String jobName) {
        this.jobName = jobName;
    }

    DictionariesJob(DictType dictionary) {
        this.jobName = dictionary.getTableName() + "_loader";
    }

    @Override
    public String getName() {
        return jobName;
    }

    @Override
    public String toString() {
        return jobName;
    }
}

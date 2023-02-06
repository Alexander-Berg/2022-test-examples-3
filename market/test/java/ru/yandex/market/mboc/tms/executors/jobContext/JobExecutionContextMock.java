package ru.yandex.market.mboc.tms.executors.jobContext;

import java.util.Date;

import org.quartz.Calendar;
import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.Trigger;
import org.quartz.TriggerKey;
import org.quartz.impl.JobDetailImpl;

/**
 * @author s-ermakov
 */
public class JobExecutionContextMock implements JobExecutionContext {
    public static final JobExecutionContextMock INSTANCE = new JobExecutionContextMock();

    private final String jobName;

    public JobExecutionContextMock() {
        this("test-executor");
    }

    public JobExecutionContextMock(String jobName) {
        this.jobName = jobName;
    }

    @Override
    public Scheduler getScheduler() {
        return null;
    }

    @Override
    public Trigger getTrigger() {
        return null;
    }

    @Override
    public Calendar getCalendar() {
        return null;
    }

    @Override
    public boolean isRecovering() {
        return false;
    }

    @Override
    public TriggerKey getRecoveringTriggerKey() throws IllegalStateException {
        return null;
    }

    @Override
    public int getRefireCount() {
        return 0;
    }

    @Override
    public JobDataMap getMergedJobDataMap() {
        return null;
    }

    @Override
    public JobDetail getJobDetail() {
        JobDetailImpl jobDetail = new JobDetailImpl();
        jobDetail.setKey(JobKey.jobKey(jobName));
        return jobDetail;
    }

    @Override
    public Job getJobInstance() {
        return null;
    }

    @Override
    public Date getFireTime() {
        return null;
    }

    @Override
    public Date getScheduledFireTime() {
        return null;
    }

    @Override
    public Date getPreviousFireTime() {
        return null;
    }

    @Override
    public Date getNextFireTime() {
        return null;
    }

    @Override
    public String getFireInstanceId() {
        return null;
    }

    @Override
    public Object getResult() {
        return null;
    }

    @Override
    public void setResult(Object result) {

    }

    @Override
    public long getJobRunTime() {
        return 0;
    }

    @Override
    public void put(Object key, Object value) {

    }

    @Override
    public Object get(Object key) {
        return null;
    }
}

package ru.yandex.market.crm.campaign.test.tms;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.quartz.Calendar;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobKey;
import org.quartz.ListenerManager;
import org.quartz.Scheduler;
import org.quartz.SchedulerContext;
import org.quartz.SchedulerException;
import org.quartz.SchedulerMetaData;
import org.quartz.Trigger;
import org.quartz.TriggerKey;
import org.quartz.impl.matchers.GroupMatcher;
import org.quartz.spi.JobFactory;

import ru.yandex.market.mcrm.utils.test.StatefulHelper;

import static org.mockito.Mockito.mock;

/**
 * @author apershukov
 */
public class TestScheduler implements Scheduler, StatefulHelper {

    private final List<JobDetail> jobs = Collections.synchronizedList(new ArrayList<>());
    private final List<Trigger> triggers = Collections.synchronizedList(new ArrayList<>());

    public List<JobDetail> getJobs() {
        return jobs;
    }

    public List<Trigger> getTriggers() {
        return triggers;
    }

    @Override
    public void resetTriggerFromErrorState(TriggerKey triggerKey) throws SchedulerException {
    }

    @Override
    public String getSchedulerName() {
        return "test-scheduler";
    }

    @Override
    public String getSchedulerInstanceId() {
        return "SchedulerInstanceId";
    }

    @Override
    public SchedulerContext getContext() {
        return mock(SchedulerContext.class);
    }

    @Override
    public void start() {
    }

    @Override
    public void startDelayed(int seconds) {
    }

    @Override
    public boolean isStarted() {
        return true;
    }

    @Override
    public void standby() {
    }

    @Override
    public boolean isInStandbyMode() {
        return false;
    }

    @Override
    public void shutdown() {
    }

    @Override
    public void shutdown(boolean waitForJobsToComplete) {
    }

    @Override
    public boolean isShutdown() {
        return false;
    }

    @Override
    public SchedulerMetaData getMetaData() {
        return mock(SchedulerMetaData.class);
    }

    @Override
    public List<JobExecutionContext> getCurrentlyExecutingJobs() {
        return List.of();
    }

    @Override
    public void setJobFactory(JobFactory factory) {
    }

    @Override
    public ListenerManager getListenerManager() {
        return mock(ListenerManager.class);
    }

    @Override
    public Date scheduleJob(JobDetail jobDetail, Trigger trigger) {
        jobs.add(jobDetail);
        triggers.add(trigger);
        return new Date();
    }

    @Override
    public Date scheduleJob(Trigger trigger) {
        return new Date();
    }

    @Override
    public void scheduleJobs(Map<JobDetail, Set<? extends Trigger>> triggersAndJobs, boolean replace) {
    }

    @Override
    public void scheduleJob(JobDetail jobDetail, Set<? extends Trigger> triggersForJob, boolean replace) {
    }

    @Override
    public boolean unscheduleJob(TriggerKey triggerKey) {
        return true;
    }

    @Override
    public boolean unscheduleJobs(List<TriggerKey> triggerKeys) {
        return true;
    }

    @Override
    public Date rescheduleJob(TriggerKey triggerKey, Trigger newTrigger) {
        return new Date();
    }

    @Override
    public void addJob(JobDetail jobDetail, boolean replace) {
    }

    @Override
    public void addJob(JobDetail jobDetail, boolean replace, boolean storeNonDurableWhileAwaitingScheduling) {
    }

    @Override
    public boolean deleteJob(JobKey jobKey) {
        jobs.removeIf(job -> job.getKey().equals(jobKey));
        triggers.removeIf(trigger -> trigger.getJobKey().equals(jobKey));
        return true;
    }

    @Override
    public boolean deleteJobs(List<JobKey> jobKeys) {
        return true;
    }

    @Override
    public void triggerJob(JobKey jobKey) {
    }

    @Override
    public void triggerJob(JobKey jobKey, JobDataMap data) {
    }

    @Override
    public void pauseJob(JobKey jobKey) {
    }

    @Override
    public void pauseJobs(GroupMatcher<JobKey> matcher) {
    }

    @Override
    public void pauseTrigger(TriggerKey triggerKey) {
    }

    @Override
    public void pauseTriggers(GroupMatcher<TriggerKey> matcher) {
    }

    @Override
    public void resumeJob(JobKey jobKey) {
    }

    @Override
    public void resumeJobs(GroupMatcher<JobKey> matcher) {
    }

    @Override
    public void resumeTrigger(TriggerKey triggerKey) {
    }

    @Override
    public void resumeTriggers(GroupMatcher<TriggerKey> matcher) {
    }

    @Override
    public void pauseAll() {
    }

    @Override
    public void resumeAll() {
    }

    @Override
    public List<String> getJobGroupNames() {
        return List.of();
    }

    @Override
    public Set<JobKey> getJobKeys(GroupMatcher<JobKey> matcher) {
        return Set.of();
    }

    @Override
    public List<? extends Trigger> getTriggersOfJob(JobKey jobKey) {
        return List.of();
    }

    @Override
    public List<String> getTriggerGroupNames() {
        return List.of();
    }

    @Override
    public Set<TriggerKey> getTriggerKeys(GroupMatcher<TriggerKey> matcher) {
        return Set.of();
    }

    @Override
    public Set<String> getPausedTriggerGroups() {
        return Set.of();
    }

    @Override
    public JobDetail getJobDetail(JobKey jobKey) {
        return null;
    }

    @Override
    public Trigger getTrigger(TriggerKey triggerKey) {
        return null;
    }

    @Override
    public Trigger.TriggerState getTriggerState(TriggerKey triggerKey) {
        return null;
    }

    @Override
    public void addCalendar(String calName, Calendar calendar, boolean replace, boolean updateTriggers) {

    }

    @Override
    public boolean deleteCalendar(String calName) {
        return false;
    }

    @Override
    public Calendar getCalendar(String calName) {
        return null;
    }

    @Override
    public List<String> getCalendarNames() {
        return null;
    }

    @Override
    public boolean interrupt(JobKey jobKey) {
        return false;
    }

    @Override
    public boolean interrupt(String fireInstanceId) {
        return false;
    }

    @Override
    public boolean checkExists(JobKey jobKey) {
        return false;
    }

    @Override
    public boolean checkExists(TriggerKey triggerKey) {
        return false;
    }

    @Override
    public void clear() {
    }

    @Override
    public void setUp() {
    }

    @Override
    public void tearDown() {
        jobs.clear();
        triggers.clear();
    }
}

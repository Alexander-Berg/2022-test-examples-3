package ru.yandex.market.tms.quartz2.controller;


import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;

import com.google.common.collect.Lists;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.quartz.JobKey;
import org.quartz.TriggerKey;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import ru.yandex.common.util.collections.immutable.SingletonMap;
import ru.yandex.market.tms.quartz2.logging.JobHistoryService;
import ru.yandex.market.tms.quartz2.model.JobInfo;
import ru.yandex.market.tms.quartz2.model.JobLastRunInfo;
import ru.yandex.market.tms.quartz2.model.JobLogEntry;
import ru.yandex.market.tms.quartz2.service.JobService;
import ru.yandex.market.tms.quartz2.spring.AnnotatedTriggersFactory;

import static java.lang.ClassLoader.getSystemResourceAsStream;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class TmsControllerTest {

    private static final String TEST_EXPR = AnnotatedTriggersFactory.NEVER_RUN_CRON_EXPR;

    @Mock
    private JobService jobService;

    @Mock
    private JobHistoryService jobHistoryService;

    private MockMvc mockMvc;

    @Captor
    private ArgumentCaptor<String> captor;

    private static String pathToJson(String relativePath) {
        try {
            return IOUtils.toString(
                    getSystemResourceAsStream(relativePath),
                    StandardCharsets.UTF_8
            );
        } catch (IOException e) {
            throw new RuntimeException("Error during reading from file " + relativePath, e);
        }
    }

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new TmsController(jobService, jobHistoryService)).build();
    }

    @Test
    void getAllJobsLittleInfo() throws Exception {
        when(jobService.getAllJobs()).thenReturn(buildJobsLittleInfo());
        mockMvc.perform(MockMvcRequestBuilders.get("/tms/jobs"))
                .andExpect(status().isOk())
                .andExpect(content().json(pathToJson("json/all_jobs_request.json"), true));
        Mockito.verify(jobService, times(1)).getAllJobs();
    }

    @Test
    void getAllJobsFullInfo() throws Exception {
        when(jobService.getAllJobsFullInfo()).thenReturn(buildJobsFullInfo());
        mockMvc.perform(MockMvcRequestBuilders.get("/tms/jobs/full-info"))
                .andExpect(status().isOk())
                .andExpect(content().json(pathToJson("json/all_jobs_full_info.json"), true));
        Mockito.verify(jobService, times(1)).getAllJobsFullInfo();
    }

    @Test
    void lastRunFullInfo() throws Exception {
        when(jobService.getAllJobsFullInfo()).thenReturn(buildJobsFullInfo());
        when(jobHistoryService.getLastRunJobsLogEntries(Mockito.any())).thenReturn(buildJobsLogEntry());

        mockMvc.perform(MockMvcRequestBuilders.get("/tms/jobs/last-run-info"))
                .andExpect(status().isOk())
                .andExpect(content().json(pathToJson("json/all_jobs_last-run-info.json"), true));

        Mockito.verify(jobService, times(1)).getAllJobsFullInfo();
        Mockito.verify(jobHistoryService, times(1)).getLastRunJobsLogEntries(Mockito.any());
    }

    @Test
    void getRunningJobs() throws Exception {
        when(jobService.getRunningJobs()).thenReturn(buildJobsLittleInfo());
        mockMvc.perform(MockMvcRequestBuilders.get("/tms/jobs/running"))
                .andExpect(status().isOk())
                .andExpect(content().json(pathToJson("json/all_jobs_request.json"), true));
        Mockito.verify(jobService, times(1)).getRunningJobs();
    }

    @Test
    void runNowAllFilled() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.post("/tms/jobs/run")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(pathToJson("json/job_request.json")))
                .andExpect(status().isOk());
        verify(jobService).runNow(captor.capture(), captor.capture());
        captor.getAllValues().containsAll(Arrays.asList("simpleTrigger", "DEFAULT"));
    }

    @Test
    void runNowWithoutJobNameReturnsBadRequest() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.post("/tms/jobs/run")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
        verify(jobService, never()).runNow(anyString(), anyString());
    }

    @Test
    void runJob() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/tms/jobs/run?jobName=simpleTrigger")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
        verify(jobService).runNow(captor.capture(), captor.capture());
        captor.getAllValues().containsAll(Arrays.asList("simpleTrigger", "DEFAULT"));
    }

    @Test
    void runJobWithoutJobNameReturnsBadRequest() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/tms/jobs/run")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
        verify(jobService, never()).runNow(anyString(), anyString());
    }

    @Test
    void runJobWithJobGroup() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/tms/jobs/run?jobName=simpleTrigger&jobGroup=TEST")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
        verify(jobService).runNow(captor.capture(), captor.capture());
        captor.getAllValues().containsAll(Arrays.asList("simpleTrigger", "TEST"));
    }

    @Test
    void deleteJobFilled() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.delete("/tms/jobs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(pathToJson("json/job_request.json")))
                .andExpect(status().isNoContent());
        verify(jobService).removeJob(captor.capture(), captor.capture());
        captor.getAllValues().containsAll(Arrays.asList("simpleTrigger", "DEFAULT"));
    }

    @Test
    void deleteJobWithoutJobNameReturnsBadRequest() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.delete("/tms/jobs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
        verify(jobService, never()).removeJob(anyString(), anyString());
    }

    @Test
    void rescheduleJobFilled() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.put("/tms/jobs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(pathToJson("json/job_request_cron.json")))
                .andExpect(status().isOk());
        verify(jobService).rescheduleJob(captor.capture(), captor.capture(), captor.capture());
        captor.getAllValues().containsAll(Arrays.asList("simpleTrigger", "DEFAULT", "0/1 * * * * ?"));
    }

    @Test
    void rescheduleJobWithoutCronReturnsBadRequest() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.put("/tms/jobs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(pathToJson("json/job_request.json")))
                .andExpect(status().isBadRequest());
        verify(jobService, never()).rescheduleJob(anyString(), anyString(), anyString());
    }

    Collection<JobInfo> buildJobsLittleInfo() {
        return Arrays.asList(
                new JobInfo("testExecutor", "DEFAULT", "0/1 * * * * ?"),
                new JobInfo("testExecutor2", "DEFAULT", "0/1 * * * * ?"),
                new JobInfo("testExecutor3", "DEFAULT", "0/1 * * * * ?"),
                new JobInfo("testExecutor4", "DEFAULT", "0 0 5 * * ?")
        );
    }

    @Test
    void jobFilteredHistory() throws Exception {
        when(jobService.getAllJobsFullInfo()).thenReturn(buildJobsFullInfo());
        when(jobHistoryService
                .getJobLogEntriesWithOffset("testExecutor", 5L, 20L, "OK", 0, 2))
                .thenReturn(buildJobsLogEntryForJobFilteredHistory());
        mockMvc.perform(MockMvcRequestBuilders
                        .get("/tms/jobs/filter-history?jobName=testExecutor" +
                                "&minDuration=5" +
                                "&maxDuration=20" +
                                "&jobStatus=OK&" +
                                "limit=2&" +
                                "since=0")
                )
                .andExpect(status().isOk())
                .andExpect(content().json(pathToJson("json/job_filtered_history_response.json"), true));

        Mockito.verify(jobService, times(1)).getAllJobsFullInfo();
        Mockito.verify(jobHistoryService, times(1))
                .getJobLogEntriesWithOffset("testExecutor", 5L, 20L, "OK", 0, 2);
    }

    Collection<JobInfo> buildJobsFullInfo() {
        return Arrays.asList(
                new JobInfo(
                        new JobKey("testExecutor", "DEFAULT"),
                        null,
                        new HashMap<>(),
                        new SingletonMap<>(
                                new TriggerKey("testExecutor", "DEFAULT"),
                                TEST_EXPR),
                        null
                ),
                new JobInfo(
                        new JobKey("testExecutor2", "DEFAULT"),
                        null,
                        new HashMap<>(),
                        new SingletonMap<>(
                                new TriggerKey("testExecutor2", "DEFAULT"),
                                TEST_EXPR),
                        null
                ),
                new JobInfo(
                        new JobKey("testClassExecutor", "DEFAULT"),
                        null,
                        new HashMap<>(),
                        new SingletonMap<>(
                                new TriggerKey("testClassExecutor", "DEFAULT"),
                                TEST_EXPR),
                        null
                )
        );
    }

    Collection<JobLastRunInfo> buildJobsLastRunEntry() {
        JobLastRunInfo entry1 = new JobLastRunInfo();
        entry1.setJobName("testExecutor");
        entry1.setJobGroup("DEFAULT");
        entry1.setDescription(null);
        entry1.setTriggers(new SingletonMap<>(
                new TriggerKey("testExecutor", "DEFAULT"),
                TEST_EXPR));
        entry1.setJobDataMap(new HashMap<>());
        entry1.setTriggerFireTime(Instant.ofEpochSecond(10));
        entry1.setJobFinishedTime(Instant.ofEpochSecond(20));
        entry1.setJobStatus("OK");
        entry1.setHost("hostname-1");

        JobLastRunInfo entry2 = new JobLastRunInfo();
        entry2.setJobName("testExecutor2");
        entry2.setJobGroup("DEFAULT");
        entry2.setDescription(null);
        entry2.setTriggers(new SingletonMap<>(
                new TriggerKey("testExecutor2", "DEFAULT"),
                TEST_EXPR));
        entry2.setJobDataMap(new HashMap<>());
        entry2.setTriggerFireTime(Instant.ofEpochSecond(10));
        entry2.setJobFinishedTime(null);
        entry2.setJobStatus(null);
        entry2.setHost("hostname-2");

        JobLastRunInfo entry3 = new JobLastRunInfo();
        entry3.setJobName("testClassExecutor");
        entry3.setJobGroup("DEFAULT");
        entry3.setDescription(null);
        entry3.setTriggers(new SingletonMap<>(
                new TriggerKey("testClassExecutor", "DEFAULT"),
                TEST_EXPR));
        entry3.setJobDataMap(new HashMap<>());
        entry3.setTriggerFireTime(Instant.ofEpochSecond(10));
        entry3.setJobFinishedTime(Instant.ofEpochSecond(12));
        entry3.setJobStatus("exception");
        entry3.setHost("hostname-1");


        return Arrays.asList(
                entry1,
                entry2,
                entry3
        );
    }

    LinkedList<JobLogEntry> buildJobsLogEntry() {
        JobLogEntry entry1 = new JobLogEntry();
        entry1.setJobName("testExecutor");
        entry1.setJobStatus("OK");
        entry1.setHost("hostname-1");
        entry1.setTriggerFireTime(Instant.ofEpochSecond(10));
        entry1.setJobFinishedTime(Instant.ofEpochSecond(20));

        JobLogEntry entry2 = new JobLogEntry();
        entry2.setJobName("testExecutor2");
        entry2.setJobStatus(null);
        entry2.setHost("hostname-2");
        entry2.setTriggerFireTime(Instant.ofEpochSecond(10));
        entry2.setJobFinishedTime(null);

        JobLogEntry entry3 = new JobLogEntry();
        entry3.setJobName("testClassExecutor");
        entry3.setJobStatus("exception");
        entry3.setHost("hostname-1");
        entry3.setTriggerFireTime(Instant.ofEpochSecond(10));
        entry3.setJobFinishedTime(Instant.ofEpochSecond(12));

        return Lists.newLinkedList(Lists.newArrayList(entry1, entry2, entry3));
    }

    LinkedList<JobLogEntry> buildJobsLogEntryForJobFilteredHistory() {
        JobLogEntry entry1 = new JobLogEntry();
        entry1.setId(1L);
        entry1.setJobName("testExecutor");
        entry1.setJobStatus("OK");
        entry1.setHost("hostname-1");
        entry1.setTriggerFireTime(Instant.ofEpochSecond(10));
        entry1.setJobFinishedTime(Instant.ofEpochSecond(20));

        JobLogEntry entry2 = new JobLogEntry();
        entry2.setId(2L);
        entry2.setJobName("testExecutor");
        entry2.setJobStatus("OK");
        entry2.setHost("hostname-1");
        entry2.setTriggerFireTime(Instant.ofEpochSecond(110));
        entry2.setJobFinishedTime(Instant.ofEpochSecond(130));

        return Lists.newLinkedList(Lists.newArrayList(entry1, entry2));
    }
}

package ru.yandex.market.load.admin.dao;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.load.admin.AbstractFunctionalTest;
import ru.yandex.market.load.admin.entity.Job;
import ru.yandex.market.load.admin.entity.JobStatus;
import ru.yandex.market.load.admin.entity.JobType;
import ru.yandex.market.load.admin.entity.TaskType;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Created by aproskriakov on 7/6/22
 */
public class JobDaoTest extends AbstractFunctionalTest {

    @Autowired
    JobDao jobDao;

    @Autowired
    JobTypeDao jobTypeDao;

    @Test
    void testSaveAndGet() {
        JobType jobType = JobType.builder()
                .name("Test")
                .task(TaskType.INIT)
                .projectId(1L)
                .build();
        jobType = jobTypeDao.save(jobType);
        Job job = Job.builder()
                .status(JobStatus.FAILED)
                .type(jobType)
                .build();
        jobDao.insert(job);

        List<Job> jobs = jobDao.findAllJobs();
        Job checkJob = jobs.get(0);

        assertEquals(JobStatus.FAILED, checkJob.getStatus());
    }
}

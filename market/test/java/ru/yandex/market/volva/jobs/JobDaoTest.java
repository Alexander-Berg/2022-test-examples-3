package ru.yandex.market.volva.jobs;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcOperations;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;


import ru.yandex.market.volva.annotations.DaoLayerTest;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.market.volva.utils.DaoUtils.params;

/**
 * @author dzvyagin
 */
@DaoLayerTest
@RunWith(SpringJUnit4ClassRunner.class)
public class JobDaoTest {

    @Autowired
    private NamedParameterJdbcOperations jdbcTemplate;

    private JobDao jobDao;

    @Before
    public void init() {
        jdbcTemplate.update("create table if not exists jobs\n" +
                "(\n" +
                "    id            BIGSERIAL    NOT NULL PRIMARY KEY,\n" +
                "    job_name      varchar(255) NOT NULL,\n" +
                "    instance_name varchar(255) NOT NULL,\n" +
                "    started_at    timestamptz  NOT NULL,\n" +
                "    updated_at    timestamptz,\n" +
                "    singleton     boolean      NOT NULL,\n" +
                "    status        varchar(255) NOT NULL,\n" +
                "    error         text\n" +
                ");\n" +
                "create index if not exists jobs_name_status\n" +
                "    on jobs (job_name, status);", params());
        jobDao = new JobDao(jdbcTemplate);
    }

    @Test
    public void findJobs() {
        JobEntity je1 = jobDao.save(job("job-1", JobStatus.RUNNING));
        JobEntity je2 = jobDao.save(job("job-1", JobStatus.FINISHED));
        JobEntity je3 = jobDao.save(job("job-2", JobStatus.RUNNING));

        List<JobEntity> jobs = jobDao.findJobs("job-1", JobStatus.RUNNING);

        assertThat(jobs).contains(je1);
        assertThat(jobs).doesNotContain(je2, je3);
    }

    @Test
    public void changeStatus() {
        JobEntity je1 = jobDao.save(job("job-cs-1", JobStatus.RUNNING));

        assertThat(jobDao.findJobs("job-cs-1", JobStatus.RUNNING)).isNotEmpty();
        assertThat(jobDao.findJobs("job-cs-1", JobStatus.FINISHED)).isEmpty();

        jobDao.changeStatus(je1.getId(), JobStatus.FINISHED, Instant.now());

        assertThat(jobDao.findJobs("job-cs-1", JobStatus.RUNNING)).isEmpty();
        assertThat(jobDao.findJobs("job-cs-1", JobStatus.FINISHED)).isNotEmpty();
    }


    @Test
    public void changeStatusWithError() {
        JobEntity je1 = jobDao.save(job("job-cse-2", JobStatus.RUNNING));

        assertThat(jobDao.findJobs("job-cse-2", JobStatus.RUNNING)).isNotEmpty();
        assertThat(jobDao.findJobs("job-cse-2", JobStatus.FAILED)).isEmpty();

        jobDao.changeStatus(je1.getId(), JobStatus.FAILED, "test_err", Instant.now());

        assertThat(jobDao.findJobs("job-cse-2", JobStatus.RUNNING)).isEmpty();
        List<JobEntity> failedJobs = jobDao.findJobs("job-cse-2", JobStatus.FAILED);
        assertThat(failedJobs).isNotEmpty();
        JobEntity failed = failedJobs.get(0);
        assertThat(failed.getError()).isEqualTo("test_err");
    }

    @Test
    public void getLastJobsStat() {
        JobEntity je3 = jobDao.save(job("job-ljs-1", JobStatus.FINISHED));
        JobEntity je8 = jobDao.save(job("job-ljs-1", JobStatus.FINISHED));
        JobEntity je9 = jobDao.save(job("job-ljs-1", JobStatus.FINISHED));
        JobEntity je10 = jobDao.save(job("job-ljs-1", JobStatus.FINISHED));
        JobEntity je2 = jobDao.save(job("job-ljs-1", JobStatus.FAILED));
        JobEntity je1 = jobDao.save(job("job-ljs-1", JobStatus.RUNNING));
        JobEntity je11 = jobDao.save(job("job-ljs-1", JobStatus.FINISHED));
        JobEntity je4 = jobDao.save(job("job-ljs-2", JobStatus.RUNNING));
        JobEntity je5 = jobDao.save(job("job-ljs-2", JobStatus.RUNNING));
        JobEntity je6 = jobDao.save(job("job-ljs-2", JobStatus.FAILED));
        JobEntity je7 = jobDao.save(job("job-ljs-3", JobStatus.FINISHED));

        Map<String, List<JobEntity>> jobs = jobDao.getLastJobsStat(3);
        assertThat(jobs.get("job-ljs-1")).isNotNull();
        assertThat(jobs.get("job-ljs-1")).containsExactlyInAnyOrder(je2, je11, je10);
        assertThat(jobs.get("job-ljs-2")).isNotNull();
        assertThat(jobs.get("job-ljs-2")).containsExactlyInAnyOrder(je6);
        assertThat(jobs.get("job-ljs-3")).isNotNull();
        assertThat(jobs.get("job-ljs-3")).containsExactlyInAnyOrder(je7);

    }

    private JobEntity job(String name, JobStatus status) {
        return JobEntity.builder()
                .instanceName("i1")
                .jobName(name)
                .status(status)
                .singleton(false)
                .startedAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }

}

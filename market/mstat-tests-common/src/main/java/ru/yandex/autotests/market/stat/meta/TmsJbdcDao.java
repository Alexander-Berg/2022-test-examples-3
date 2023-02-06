package ru.yandex.autotests.market.stat.meta;

import java.time.LocalDateTime;
import org.springframework.jdbc.core.JdbcTemplate;
import ru.yandex.autotests.market.stat.beans.Job;
import ru.yandex.autotests.market.stat.beans.meta.TmsRunState;
import ru.yandex.autotests.market.stat.beans.meta.TmsStatus;
import ru.yandex.autotests.market.stat.meta.query.*;

import javax.sql.DataSource;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by entarrion on 02.04.15.
 */
public class TmsJbdcDao implements TmsDao {

    private JdbcTemplate jdbcTemplate;

    public TmsJbdcDao(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private DataSource dataSource() {
        return jdbcTemplate.getDataSource();
    }

    @Override
    public TmsRunState getLastJobRun(Job job) {
        LastJobRunSqlQuery query = new LastJobRunSqlQuery(dataSource(), job);
        return query.findObject();
    }

    @Override
    public TmsRunState getLastJobRun(String jobName) {
        LastJobRunSqlQuery query = new LastJobRunSqlQuery(dataSource(), jobName);
        return query.findObject();
    }

    @Override
    public TmsRunState getLastFinishedJobRun(Job job) {
        LastFinishedJobRunSqlQuery query = new LastFinishedJobRunSqlQuery(dataSource(), job);
        return query.findObject();
    }

    @Override
    public TmsRunState getNextJobAfter(TmsRunState previousJob) {
        TmsNextJobAfterSqlQuery query = new TmsNextJobAfterSqlQuery(dataSource(), previousJob);
        return query.findObject();
    }

    @Override
    public TmsRunState getJobById(TmsRunState job) {
        TmsJobByIdSqlQuery query = new TmsJobByIdSqlQuery(dataSource(), job);
        return query.findObject();
    }

    @Override
    public List<TmsRunState> getJobsByIds(List<TmsRunState> jobs) {
        TmsJobsByIdsSqlQuery query = new TmsJobsByIdsSqlQuery(dataSource(),
                jobs.stream().map(TmsRunState::getId).collect(Collectors.toList()));
        return query.execute();
    }

    @Override
    public List<TmsRunState> getJobRunStatesAfter(Job job, LocalDateTime after) {
        TmsJobStatesAfterSqlQuery query = new TmsJobStatesAfterSqlQuery(dataSource(), job, after);
        return query.execute();
    }

    @Override
    public Boolean checkFiredTriggersForJobs(List<TmsRunState> jobs) {
        FiredTriggersQuery query = new FiredTriggersQuery(dataSource(), jobs);
        return query.findObject();
    }

    @Override
    public TmsRunState getLastSuccessfulJobRun(Job job) {
        LastJobRunWithStatusSqlQuery query = new LastJobRunWithStatusSqlQuery(dataSource(), job, TmsStatus.OK);
        return query.findObject();
    }
}

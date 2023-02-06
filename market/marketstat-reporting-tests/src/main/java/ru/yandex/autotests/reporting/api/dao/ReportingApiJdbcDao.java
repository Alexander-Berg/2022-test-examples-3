package ru.yandex.autotests.reporting.api.dao;

import org.springframework.jdbc.core.JdbcTemplate;
import ru.yandex.autotests.market.stat.meta.MetaJdbcTemplateFactory;
import ru.yandex.autotests.reporting.api.beans.BuildReportTmsJob;
import ru.yandex.autotests.reporting.api.dao.query.GetJobsDetailsQuery;

import javax.sql.DataSource;

/**
 * Created by kateleb on 17.11.16.
 */
public class ReportingApiJdbcDao implements ReportingApiDao {
    private JdbcTemplate reportingJdbcTemplate;

    // инициализация и синглетоновость
    private ReportingApiJdbcDao(JdbcTemplate jdbcTemplate) {
        this.reportingJdbcTemplate = jdbcTemplate;
    }

    public static ReportingApiJdbcDao getInstance() {
        return JdbcDaoSingletonHolder.INSTANCE;
    }

    private DataSource dataSource() {
        return reportingJdbcTemplate.getDataSource();
    }

    @Override
    public BuildReportTmsJob getJobDetails(String name) {
        GetJobsDetailsQuery query = GetJobsDetailsQuery.getInstance(dataSource(), name);
        return query.findObject();
    }

    private static class JdbcDaoSingletonHolder {
        private static final ReportingApiJdbcDao INSTANCE =
                new ReportingApiJdbcDao(MetaJdbcTemplateFactory.getInstanceReporting());
    }
}

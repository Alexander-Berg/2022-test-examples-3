package ru.yandex.autotests.market.stat.meta.query;

import org.apache.commons.lang3.StringUtils;
import org.springframework.jdbc.object.MappingSqlQuery;
import ru.yandex.autotests.market.stat.beans.Job;
import ru.yandex.autotests.market.stat.beans.meta.TmsRunState;
import ru.yandex.autotests.market.stat.mappers.TmsStateRowMapper;
import ru.yandex.autotests.market.common.attacher.Attacher;

import javax.sql.DataSource;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Created by jkt on 14.05.14.
 */
public class LastJobRunSqlQuery extends MappingSqlQuery<TmsRunState> {

    public LastJobRunSqlQuery(DataSource ds, Job job) {
        super(ds, sqlFor(job));
    }

    public LastJobRunSqlQuery(DataSource ds, String jobName) {
        super(ds, sqlFor(jobName));
    }

    protected static String sqlFor(Job job) {
        if (job == null) {
            throw new IllegalArgumentException("Can not get status for null job");
        }
        return forJobName(job.getName());
    }

    private static String sqlFor(String job) {
        if (StringUtils.isEmpty(job)) {
            throw new IllegalArgumentException("Can not get status for null job");
        }
        return forJobName(job);
    }

    private static String forJobName(String jobName) {
        String sql = "SELECT * FROM QRTZ_LOG\n" +
                "WHERE ID = (\n" +
                "\tSELECT MAX(ID) FROM QRTZ_LOG\n" +
                "\tWHERE JOB_NAME ='" + jobName + "'\n" +
                ")";
        Attacher.attachSql(sql);
        return sql;
    }

    @Override
    protected TmsRunState mapRow(ResultSet rs, int rowNum) throws SQLException {
        return new TmsStateRowMapper().mapRow(rs, rowNum);
    }
}
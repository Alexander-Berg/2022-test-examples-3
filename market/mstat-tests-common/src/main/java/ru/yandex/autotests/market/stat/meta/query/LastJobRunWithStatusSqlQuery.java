package ru.yandex.autotests.market.stat.meta.query;

import org.springframework.jdbc.object.MappingSqlQuery;
import ru.yandex.autotests.market.stat.beans.Job;
import ru.yandex.autotests.market.stat.beans.meta.TmsRunState;
import ru.yandex.autotests.market.stat.beans.meta.TmsStatus;
import ru.yandex.autotests.market.stat.mappers.TmsStateRowMapper;
import ru.yandex.autotests.market.common.attacher.Attacher;

import javax.sql.DataSource;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Created by jkt on 14.05.14.
 */
public class LastJobRunWithStatusSqlQuery extends MappingSqlQuery<TmsRunState> {

    public LastJobRunWithStatusSqlQuery(DataSource ds, Job job, TmsStatus status) {
        super(ds, sqlFor(job, status));
    }

    private static String sqlFor(Job job, TmsStatus status) {
        if (job == null) {
            throw new IllegalArgumentException("Can not get status for null job");
        }
        String jobName = job.getName();
        String statusCondition = asSqlCondition(status);
        String sql = "SELECT * FROM QRTZ_LOG\n" +
                "WHERE ID = (\n" +
                "\tSELECT max(ID) FROM QRTZ_LOG\n" +
                "\tWHERE JOB_NAME in('" + jobName + "')\n" +
                "\tAND JOB_STATUS " + statusCondition + "\n" +
                ")";
        Attacher.attachSql(sql);
        return sql;
    }

    private static String asSqlCondition(TmsStatus status) {
        if (TmsStatus.RUNNING == status) {
            return " is null";
        }
        return "= '" + status.mask() + "'";
    }

    @Override
    protected TmsRunState mapRow(ResultSet rs, int rowNum) throws SQLException {
        return new TmsStateRowMapper().mapRow(rs, rowNum);
    }
}
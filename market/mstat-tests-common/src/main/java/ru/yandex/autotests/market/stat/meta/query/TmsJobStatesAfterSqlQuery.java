package ru.yandex.autotests.market.stat.meta.query;

import java.time.LocalDateTime;
import org.springframework.jdbc.object.MappingSqlQuery;
import ru.yandex.autotests.market.stat.beans.Job;
import ru.yandex.autotests.market.stat.beans.meta.TmsRunState;
import ru.yandex.autotests.market.stat.mappers.TmsStateRowMapper;
import ru.yandex.autotests.market.common.attacher.Attacher;
import static ru.yandex.autotests.market.stat.util.DateUtils.getMillis;

import javax.sql.DataSource;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Created by jkt on 14.05.14.
 */
public class TmsJobStatesAfterSqlQuery extends MappingSqlQuery<TmsRunState> {

    public TmsJobStatesAfterSqlQuery(DataSource ds, Job job, LocalDateTime after) {
        super(ds, sqlFor(job, after));
    }

    private static String sqlFor(Job job, LocalDateTime after) {
        if (job == null) {
            throw new IllegalArgumentException("Can not get status for null job");
        }
        if (after == null) {
            throw new IllegalArgumentException("Can not get job status after null date");
        }
        long minFireTime = getMillis(after);
        String sql = "SELECT * FROM QRTZ_LOG\n" +
                "WHERE TRIGGER_FIRE_TIME > " + minFireTime + "\n" +
                "AND JOB_NAME= '" + job.getName() + "'\n";
        Attacher.attachSql(sql);
        return sql;
    }

    @Override
    protected TmsRunState mapRow(ResultSet rs, int rowNum) throws SQLException {
        return new TmsStateRowMapper().mapRow(rs, rowNum);
    }
}

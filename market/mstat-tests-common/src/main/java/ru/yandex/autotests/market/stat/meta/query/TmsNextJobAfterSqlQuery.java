package ru.yandex.autotests.market.stat.meta.query;


import org.springframework.jdbc.object.MappingSqlQuery;
import ru.yandex.autotests.market.stat.beans.meta.TmsRunState;
import ru.yandex.autotests.market.stat.mappers.TmsStateRowMapper;
import ru.yandex.autotests.market.common.attacher.Attacher;

import javax.sql.DataSource;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Created by kateleb on 26.12.14.
 */
public class TmsNextJobAfterSqlQuery extends MappingSqlQuery<TmsRunState> {

    public TmsNextJobAfterSqlQuery(DataSource dataSource, TmsRunState previousJob) {
        super(dataSource, sqlFor(previousJob));
    }

    private static String forJobName(String jobName, String id) {
        String sql = "SELECT * FROM QRTZ_LOG\n" +
                "WHERE JOB_NAME ='" + jobName + "'\n" +
                "AND ID >" + id + "\n" +
                "ORDER BY ID LIMIT 1";
        Attacher.attachSql(sql);
        return sql;
    }

    protected static String sqlFor(TmsRunState previousJob) {
        return forJobName(previousJob.getJobName(), previousJob.getId());
    }

    @Override
    protected TmsRunState mapRow(ResultSet rs, int rowNum) throws SQLException {
        return new TmsStateRowMapper().mapRow(rs, rowNum);
    }

}
package ru.yandex.autotests.market.stat.meta.query;

import org.springframework.jdbc.object.MappingSqlQuery;
import ru.yandex.autotests.market.stat.beans.meta.TmsRunState;
import ru.yandex.autotests.market.stat.mappers.TmsStateRowMapper;
import ru.yandex.autotests.market.common.attacher.Attacher;

import javax.sql.DataSource;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Created by entarrion on 02.04.15.
 */
public class TmsJobByIdSqlQuery extends MappingSqlQuery<TmsRunState> {
    public TmsJobByIdSqlQuery(DataSource ds, String runId) {
        super(ds, sqlFor(runId));
    }

    public TmsJobByIdSqlQuery(DataSource ds, TmsRunState job) {
        this(ds, job.getId());
    }

    private static String sqlFor(String id) {
        String sql = "SELECT * from QRTZ_LOG\n" +
                "WHERE id = " + id;
        Attacher.attachSql(sql);
        return sql;
    }

    @Override
    protected TmsRunState mapRow(ResultSet rs, int rowNum) throws SQLException {
        return new TmsStateRowMapper().mapRow(rs, rowNum);
    }
}

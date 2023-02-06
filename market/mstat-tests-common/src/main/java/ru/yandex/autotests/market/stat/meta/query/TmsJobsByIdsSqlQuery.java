package ru.yandex.autotests.market.stat.meta.query;

import org.apache.commons.lang3.StringUtils;
import org.springframework.jdbc.object.MappingSqlQuery;
import ru.yandex.autotests.market.stat.beans.meta.TmsRunState;
import ru.yandex.autotests.market.stat.mappers.TmsStateRowMapper;
import ru.yandex.autotests.market.common.attacher.Attacher;

import javax.sql.DataSource;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

/**
 * Created by entarrion on 15.04.15.
 */
public class TmsJobsByIdsSqlQuery extends MappingSqlQuery<TmsRunState> {
    public TmsJobsByIdsSqlQuery(DataSource ds, List<String> runIds) {
        super(ds, sqlFor(runIds));
    }

    private static String sqlFor(List<String> ids) {
        String sql = "SELECT * from QRTZ_LOG\n" +
                "WHERE id in (" + StringUtils.join(ids, ", ") + ")";
        Attacher.attachSql(sql);
        return sql;
    }

    @Override
    protected TmsRunState mapRow(ResultSet rs, int rowNum) throws SQLException {
        return new TmsStateRowMapper().mapRow(rs, rowNum);
    }
}

package ru.yandex.autotests.reporting.api.dao.query;

import org.springframework.jdbc.object.MappingSqlQuery;
import ru.yandex.autotests.market.stat.mappers.RowMapperUtils;
import ru.yandex.autotests.market.common.attacher.Attacher;
import ru.yandex.autotests.reporting.api.beans.BuildReportTmsJob;

import javax.sql.DataSource;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Created by kateleb on 18.11.16.
 */
public class GetJobsDetailsQuery extends MappingSqlQuery<BuildReportTmsJob>{
    public static GetJobsDetailsQuery getInstance(DataSource dataSource, String name) {
        return new GetJobsDetailsQuery(dataSource, name);
    }

    public GetJobsDetailsQuery(DataSource ds, String name) {
        super(ds, sqlFor(name));
    }

    private static String sqlFor(String name) {
        String sql = "SELECT * FROM reporting_yt_schema_testing.jobs \n" +
                "WHERE name ='" + name + "'\n" +
                "limit 1";
        Attacher.attachSql(sql);
        return sql;
    }

    @Override
    protected BuildReportTmsJob mapRow(ResultSet resultSet, int i) throws SQLException {
        return RowMapperUtils.mapRow(BuildReportTmsJob.class, resultSet, i);
    }
}

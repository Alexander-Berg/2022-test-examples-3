package ru.yandex.autotests.market.stat.meta.query;

import com.google.common.base.Joiner;
import java.time.LocalDateTime;

import static ru.yandex.autotests.market.stat.util.DateUtils.getMillis;
import org.springframework.jdbc.object.MappingSqlQuery;
import ru.yandex.autotests.market.stat.beans.meta.TmsRunState;

import javax.sql.DataSource;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by kateleb on 18.12.14.
 */
public class FiredTriggersQuery extends MappingSqlQuery<Boolean> {
    public static final String CNT = "cnt";

    public FiredTriggersQuery(DataSource ds, List<TmsRunState> jobs) {
        super(ds, sqlFor(jobs));
    }

    private static String sqlFor(List<TmsRunState> jobs) {
        List<String> names = jobs.stream().map(TmsRunState::getJobName).collect(Collectors.toList());
        List<LocalDateTime> times = jobs.stream().map(TmsRunState::getFireTime).collect(Collectors.toList());

        LocalDateTime latestStart = Collections.max(times);
        String jobnames = Joiner.on("', '").join(names);

        return "SELECT count(*) " + CNT + "\n" +
                "FROM QRTZ_FIRED_TRIGGERS\n" +
                "WHERE JOB_NAME in ('" + jobnames + "')\n" +
                "AND FIRED_TIME < " + getMillis(latestStart.plusMinutes(2));
    }

    @Override
    protected Boolean mapRow(ResultSet rs, int rowNum) throws SQLException {
        int count = rs.getInt(CNT);
        return count > 0;
    }
}
package ru.yandex.market.replenishment.autoorder.integration.test.imports;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.market.application.properties.AppPropertyContextInitializer;
import ru.yandex.misc.lang.StringUtils;
import ru.yandex.replenishment.autoorder.integration.test.config.PostgresDataSourceConfig;

@RunWith(SpringRunner.class)
@ContextConfiguration(
        classes = {PostgresDataSourceConfig.class},
        initializers = AppPropertyContextInitializer.class)
public class ImportTest {

    private static final long IMPORT_TIME = 90L;
    private static final String LAST_IMPORT_FINISHED =
            "select events_group, time_start, time_end from import_log where events_group = 'replenishment' " +
                    "order by time_start desc limit 1;";

    @Qualifier("jdbcTemplate")
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    public void checkImportTime() {
        final ImportLog lastImportLog =
                jdbcTemplate.queryForObject(LAST_IMPORT_FINISHED, (rs, rowNum) ->
                        new ImportLog(
                                rs.getString("events_group"),
                                getLocalDateTime(rs, "time_start"),
                                getLocalDateTime(rs, "time_end")
                        ));
        Assert.assertNotNull(lastImportLog);
        Assert.assertTrue(ChronoUnit.MINUTES
                .between(lastImportLog.getTimeStart(), lastImportLog.getTimeEnd()) <= IMPORT_TIME);
    }

    private static LocalDateTime getLocalDateTime(ResultSet rs, String name) throws SQLException {
        final String string = rs.getString(name);
        return StringUtils.isEmpty(string) || rs.wasNull() ? null
                : Timestamp.valueOf(string).toLocalDateTime();
    }

    private static class ImportLog {
        private final String eventsGroup;
        private final LocalDateTime timeStart;
        private final LocalDateTime timeEnd;

        private ImportLog(String eventsGroup, LocalDateTime timeStart, LocalDateTime timeEnd) {
            this.eventsGroup = eventsGroup;
            this.timeStart = timeStart;
            this.timeEnd = timeEnd;
        }

        public String getEventsGroup() {
            return eventsGroup;
        }

        public LocalDateTime getTimeStart() {
            return timeStart;
        }

        public LocalDateTime getTimeEnd() {
            return timeEnd;
        }
    }
}

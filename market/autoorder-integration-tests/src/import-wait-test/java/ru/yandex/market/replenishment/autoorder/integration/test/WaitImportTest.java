package ru.yandex.market.replenishment.autoorder.integration.test;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.LocalDate;

import org.awaitility.Awaitility;
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
public class WaitImportTest {
    private static final String LAST_IMPORT_FINISHED =
            "select events_group, time_start, time_end from import_log where events_group = 'replenishment' " +
                    "order by time_start desc limit 1;";

    @Qualifier("jdbcTemplate")
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    public void waitImport() {
        Awaitility.await().atMost(Duration.ofMinutes(90))
                .until(() ->
                        jdbcTemplate.queryForObject(LAST_IMPORT_FINISHED, (rs, rowNum) ->
                                new ImportLog(
                                        rs.getString("events_group"),
                                        getLocalDate(rs, "time_start"),
                                        getLocalDate(rs, "time_end")
                                )).getTimeEnd() != null
                );
    }

    private static LocalDate getLocalDate(ResultSet rs, String name) throws SQLException {
        final String string = rs.getString(name);
        return StringUtils.isEmpty(string) || rs.wasNull() ? null
                : Timestamp.valueOf(string).toLocalDateTime().toLocalDate();
    }

    private static class ImportLog {
        private final String eventsGroup;
        private final LocalDate timeStart;
        private final LocalDate timeEnd;

        private ImportLog(String eventsGroup, LocalDate timeStart, LocalDate timeEnd) {
            this.eventsGroup = eventsGroup;
            this.timeStart = timeStart;
            this.timeEnd = timeEnd;
        }

        public String getEventsGroup() {
            return eventsGroup;
        }

        public LocalDate getTimeStart() {
            return timeStart;
        }

        public LocalDate getTimeEnd() {
            return timeEnd;
        }
    }
}

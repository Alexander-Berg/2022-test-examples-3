package ru.yandex.market.replenishment.autoorder.integration.test.imports;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.LocalDate;
import java.util.Collections;
import java.util.Objects;

import org.awaitility.Awaitility;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.client.RestTemplate;

import ru.yandex.market.application.properties.AppPropertyContextInitializer;
import ru.yandex.misc.lang.StringUtils;
import ru.yandex.replenishment.autoorder.integration.test.config.PostgresDataSourceConfig;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

@RunWith(SpringRunner.class)
@ContextConfiguration(
        classes = {PostgresDataSourceConfig.class},
        initializers = AppPropertyContextInitializer.class)
public class LaunchImportTest {

    private static final String LAST_IMPORT_FINISHED =
            "select events_group, time_start, time_end from import_log where events_group = 'replenishment' " +
                    "order by time_start desc limit 1;";

    @Qualifier("jdbcTemplate")
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Value("${autoorder.robot.login}")
    private String robotLogin;

    @Value("${autoorder.robot.password}")
    private String robotPassword;

    @Value("${autoorder.integration-test.root-uri}")
    private String rootUri;

    private String cookie;

    @Before
    public void setUp() {
        final HttpHeaders headers = new HttpHeaders();
        headers.setAccept(Collections.singletonList(MediaType.ALL));
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        assertNotNull(robotPassword);
        assertFalse(robotPassword.isBlank());
        final HttpEntity<String> entity = new HttpEntity<>("login=" + robotLogin + "&passwd=" + robotPassword, headers);
        final ResponseEntity<String> response = new RestTemplate()
                .exchange("https://passport.yandex-team.ru/passport?mode=auth",
                        HttpMethod.POST, entity, String.class);
        cookie = String.join(";", Objects.requireNonNull(response.getHeaders().get("Set-Cookie")));
    }

    @Test
    public void launchImport() throws JSONException {
        final HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON_UTF8);
        headers.set("Cookie", cookie);
        final HttpEntity<String> entity = new HttpEntity<>(makeRequest().toString(), headers);
        final ResponseEntity<String> result = new RestTemplate()
                .exchange(rootUri + "/autoorder/api/v2/admin/recommendations-reimport",
                        HttpMethod.POST, entity, String.class);
        Assert.assertNotNull(result);
        Awaitility.await().pollDelay(Duration.ofMinutes(10)).atMost(Duration.ofMinutes(30))
                .until(() -> jdbcTemplate.queryForObject(LAST_IMPORT_FINISHED, (rs, rowNum) ->
                        new ImportLog(
                                rs.getString("events_group"),
                                getLocalDate(rs, "time_start"),
                                getLocalDate(rs, "time_end")
                        )).getTimeEnd() == null);
    }

    private JSONObject makeRequest() throws JSONException {
        final JSONObject request = new JSONObject();
        request.put("recommendationsImportEvent", "TYPE_1P");
        request.put("tablePath", "");
        return request;
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

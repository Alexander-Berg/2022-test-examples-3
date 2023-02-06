package ru.yandex.market.antifraud.yql.step;

import lombok.extern.slf4j.Slf4j;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import ru.yandex.market.antifraud.db.LoggingJdbcTemplate;
import ru.yandex.market.antifraud.yql.config.IntegrationTestAntifraudYqlConfig;
import ru.yandex.market.antifraud.yql.validate.YtTestDataGenerator;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

import static org.junit.Assert.fail;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {IntegrationTestAntifraudYqlConfig.class})
@ActiveProfiles("integration-tests")
@Slf4j
public class DaycloseStepEventSenderITest {
    @Autowired
    private DaycloseStepEventSender daycloseStepEventSender;

    @Autowired
    private LoggingJdbcTemplate jdbcTemplate;

    @Autowired
    private YtTestDataGenerator testDataGenerator;

    @Value("${antifraud.yql.step.url}")
    private String stepUrl;

    @Value("${antifraud.yql.rollback.eventname}")
    private String eventName;

    @Before
    public void checkTestData() {
        testDataGenerator.initOnce();
    }

    @Test
    public void mustSendAndGetEvent() {
        jdbcTemplate.exec("truncate closed_days");
        jdbcTemplate.exec("insert into closed_days (log, day, reason) values " +
                "('" + testDataGenerator.log().getLogName().getLogName() + "', 20171020, 'itest')");
        daycloseStepEventSender.sendDay();
        String eventId = jdbcTemplate.query("select step_event_id from closed_days where day = 20171020 limit 1", String.class);
        log.info("eventId: " + eventId);
        checkEventIdExistsInStep(stepUrl, eventId);
    }

    public static void checkEventIdExistsInStep(String stepUrl, String eventId) {
        int tries = 5;
        while(tries --> 0) {
            try {
                int code = getStatusCode(new URL(stepUrl + "/" + eventId));
                if(code == 200) {
                    return;
                }
                else if(code == 404) {
                    fail("404 no such event");
                }
                else {
                    throw new RuntimeException("Unexpected status " + code);
                }
            } catch (IOException e) {
                log.warn("Retrying " + tries);
            }
        }
    }

    private static int getStatusCode(URL url) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        try {
            connection.setRequestMethod("GET");
            connection.connect();
            return connection.getResponseCode();
        }
        finally {
            connection.disconnect();
        }
    }
}

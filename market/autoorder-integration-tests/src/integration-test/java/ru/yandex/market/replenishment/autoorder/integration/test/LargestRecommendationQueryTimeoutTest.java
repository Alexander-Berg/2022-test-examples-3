package ru.yandex.market.replenishment.autoorder.integration.test;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.web.client.RestTemplate;

public class LargestRecommendationQueryTimeoutTest extends AbstractIntegrationTest {

    public static final String TIMEOUT_QUERY = "select value from environment where name = 'TEST_TIMEOUT_SEC_VALUE'";
    public static final String LARGEST_1P_DEMAND_QUERY = "select id from demand_1p order by mskus desc limit 1";
    public static final String LARGEST_TENDER_DEMAND_QUERY = "select id from demand_tender order by mskus desc limit 1";
    public static final String LARGEST_3P_DEMAND_QUERY = "select id from demand_3p where parent_demand_id IN " +
            "(select parent_demand_id from demand_3p group by parent_demand_id order by sum(mskus) desc limit 1)";

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

    private final RowMapper<Long> rm = (rs, rowNum) -> rs.getLong(0);
    private final RowMapper<Long> envLongString = (rs, rowNum) -> Long.parseLong(rs.getString(0));

    @Before
    public void setUp() {
        final HttpHeaders headers = new HttpHeaders();
        headers.setAccept(Collections.singletonList(MediaType.ALL));
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        Assertions.assertNotNull(robotPassword);
        Assertions.assertFalse(robotPassword.isBlank());
        final HttpEntity<String> entity = new HttpEntity<>("login=" + robotLogin + "&passwd=" + robotPassword, headers);
        final ResponseEntity<String> response = new RestTemplate()
                .exchange("https://passport.yandex-team.ru/passport?mode=auth",
                        HttpMethod.POST, entity, String.class);
        cookie = String.join(";", Objects.requireNonNull(response.getHeaders().get("Set-Cookie")));
    }

    @Test
    public void testQuery1p() throws JSONException {
        testQuery(LARGEST_1P_DEMAND_QUERY, "/autoorder/api/v2/recommendations/with-count?demandType=TYPE_1P");
    }

    @Test
    public void testQueryTender() throws JSONException {
        testQuery(LARGEST_TENDER_DEMAND_QUERY, "/autoorder/api/v2/recommendations/tender/with-count");
    }

    @Test
    public void testQuery3p() throws JSONException {
        testQuery(LARGEST_3P_DEMAND_QUERY, "/autoorder/api/v2/recommendations/with-count?demandType=TYPE_3P");
    }

    private void testQuery(String queryForIds, String path) throws JSONException {
        var demandIds = jdbcTemplate.query(queryForIds, rm);
        queryRecommendation(demandIds, path);
    }

    private Long getTimeoutValue() {
        return jdbcTemplate.query(TIMEOUT_QUERY, envLongString).stream().findFirst().orElse(30L);
    }

    private void queryRecommendation(List<Long> ids, String path) throws JSONException {
        final HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON_UTF8);
        headers.set("Cookie", cookie);
        final HttpEntity<String> entity = new HttpEntity<>(makeRequestFilter(ids).toString(), headers);

        Assertions.assertTimeout(Duration.ofSeconds(getTimeoutValue()), () -> {
            var result = new RestTemplate()
                    .exchange(rootUri + path,
                            HttpMethod.POST, entity, String.class);
            Assertions.assertEquals(result.getStatusCode(), HttpStatus.OK);
        });
    }

    private JSONObject makeRequestFilter(List<Long> ids) throws JSONException {
        final JSONObject request = new JSONObject();
        final JSONObject filter = new JSONObject();
        final JSONArray demands = new JSONArray();
        demands.put(ids);

        request.put("filter", filter);
        filter.put("demandIds", demands);
        return request;
    }
}

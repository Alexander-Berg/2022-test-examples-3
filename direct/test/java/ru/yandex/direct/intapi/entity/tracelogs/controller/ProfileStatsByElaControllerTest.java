package ru.yandex.direct.intapi.entity.tracelogs.controller;

import java.sql.Date;
import java.time.LocalDate;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.util.NestedServletException;

import ru.yandex.clickhouse.response.ClickHouseResultSet;
import ru.yandex.direct.dbutil.wrapper.DatabaseWrapper;
import ru.yandex.direct.dbutil.wrapper.DatabaseWrapperProvider;
import ru.yandex.direct.dbutil.wrapper.SimpleDb;
import ru.yandex.direct.intapi.configuration.IntApiTest;
import ru.yandex.direct.intapi.entity.tracelogs.model.profilestatsbyela.StatsElement;
import ru.yandex.direct.intapi.entity.tracelogs.service.profilestats.ProfileStatsService;
import ru.yandex.direct.intapi.entity.tracelogs.service.profilestatsbyela.ProfileStatsByElaService;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static net.javacrumbs.jsonunit.spring.JsonUnitResultMatchers.json;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringJUnit4ClassRunner.class)
@IntApiTest
public class ProfileStatsByElaControllerTest {
    private static final String URL = "/trace_logs/profile_stats_by_ela";
    private static final Date DATE_VALUE = Date.valueOf(LocalDate.of(2018, 11, 22));

    private static final String LOG_DATE = "log_date";
    private static final String CMD_TYPE = "cmd_type";
    private static final String CMD = "cmd";
    private static final String CNT = "cnt";
    private static final String ELA_SUM = "ela_sum";
    private static final String CPU_USER = "cpu_user";
    private static final String CPU_SYSTEM = "cpu_system";
    private static final String MEM = "mem";
    private static final String ROUNDED_ELA = "rounded_ela";
    private static final String FUNC_PARAM = "func_param";
    private static final String ELA_PROC = "ela_proc";
    private static final String CMD_MATCH = "cmdmatch0";

    private static final String WRONG_TIMES_BODY = "{"
            + "\"date_from\":\"2018-11-29\",\n"
            + "\"date_to\":\"2018-11-22\",\n"
            + "\"compare_dates\":false,\n"
            + "\"regexps\":[\"direct.web/showClients\"],"
            + "\"boundaries\": [\"0.1\", \"0.2\", \"1\"]"
            + "}";

    private DatabaseWrapper clickHouseJdbcTemplate;
    private MockMvc mockMvc;

    @Before
    public void before() {
        clickHouseJdbcTemplate = mock(DatabaseWrapper.class);
        DatabaseWrapperProvider dbProvider = mock(DatabaseWrapperProvider.class);
        ProfileStatsService profileStatsService = mock(ProfileStatsService.class);
        ProfileStatsByElaService profileStatsByElaService = new ProfileStatsByElaService(dbProvider);
        TraceLogsController controller = new TraceLogsController(profileStatsService, profileStatsByElaService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
        when(dbProvider.get(SimpleDb.CLICKHOUSE_CLOUD)).thenReturn(clickHouseJdbcTemplate);
    }

    @Test(expected = NestedServletException.class)
    public void wrongTimesExceptionTest() throws Exception {
        mockMvc
                .perform(
                        post(URL)
                                .content(WRONG_TIMES_BODY)
                                .contentType(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().is5xxServerError());
    }

    @Test
    public void simpleQueryTest() throws Exception {
        ClickHouseResultSet resultSetFunc = mock(ClickHouseResultSet.class);
        mockResultSet(resultSetFunc);

        String body = "{"
                + "\"date_from\":\"2018-11-22\","
                + "\"date_to\":\"2018-11-22\","
                + "\"compare_dates\":false,"
                + "\"regexps\":[\"direct.web/showClients\"],"
                + "\"boundaries\": [\"0.1\", \"0.2\", \"1\"]"
                + "}";

        sendRequest(body, getResponseBody(singletonList("direct.web/showClients"), singletonList(0.1f)));
    }

    @Test
    public void compareDatesTrueQueryTest() throws Exception {
        ClickHouseResultSet resultSetFunc = mock(ClickHouseResultSet.class);
        mockResultSet(resultSetFunc);

        String body = "{"
                + "\"date_from\":\"2018-11-22\","
                + "\"date_to\":\"2018-11-22\","
                + "\"compare_dates\":true,"
                + "\"regexps\":[\"direct.web/showClients\"],"
                + "\"boundaries\": [\"0.1\", \"0.2\", \"1\"]"
                + "}";

        sendRequest(body, getResponseBody(singletonList("direct.web/showClients"), singletonList(0.1f)));
    }

    @Test
    public void emptyRegexpsQueryTest() throws Exception {
        ClickHouseResultSet resultSetFunc = mock(ClickHouseResultSet.class);
        mockResultSet(resultSetFunc);

        String body = "{"
                + "\"date_from\":\"2018-11-22\","
                + "\"date_to\":\"2018-11-22\","
                + "\"compare_dates\":false,"
                + "\"regexps\":[],"
                + "\"boundaries\": [\"0.1\"]"
                + "}";

        sendRequest(body, getResponseBody(emptyList(), emptyList()));
    }

    private void sendRequest(String requestBody, String responseBody) throws Exception {
        mockMvc
                .perform(
                        post(URL)
                                .content(requestBody)
                                .contentType(MediaType.APPLICATION_JSON)
                ).andExpect(json().isEqualTo(responseBody))
                .andReturn()
                .getResponse()
                .getContentAsString();
    }

    private void mockRowMapper(ClickHouseResultSet resultSet) {
        doAnswer(invocation -> {
            RowMapper<StatsElement> rowMapper = invocation.getArgument(2);
            return singletonList(rowMapper.mapRow(resultSet, 0));
        }).when(clickHouseJdbcTemplate).query(any(), (Object[]) any(), (RowMapper) any());
    }

    private void mockResultSet(ClickHouseResultSet resultSet) throws Exception {
        when(resultSet.getDate(LOG_DATE)).thenReturn(DATE_VALUE);
        when(resultSet.getInt(CMD_MATCH)).thenReturn(1);
        when(resultSet.getString(FUNC_PARAM)).thenReturn("db:read");
        when(resultSet.getFloat(ROUNDED_ELA)).thenReturn(0.1f);
        when(resultSet.getFloat(ELA_PROC)).thenReturn(0.01f);
        when(resultSet.getString(CMD_TYPE)).thenReturn("direct.web");
        when(resultSet.getString(CMD)).thenReturn("getSmth");
        when(resultSet.getInt(CNT)).thenReturn(1);
        when(resultSet.getDouble(CPU_USER)).thenReturn(15.0);
        when(resultSet.getDouble(CPU_SYSTEM)).thenReturn(15.0);
        when(resultSet.getFloat(MEM)).thenReturn(20.0f);
        when(resultSet.getDouble(ELA_SUM)).thenReturn(0.05);

        mockRowMapper(resultSet);
    }

    private String getResponseBody(List<String> regexps, List<Float> boundaries) {
        StringBuilder body = new StringBuilder("{\"error\": null, \"result\": {\"stats\": [");

        for (int i = 0; i < regexps.size(); i++) {
            body.append("{\"regexp\": \"").append(regexps.get(i)).append("\", \"date\": \"${json-unit.ignore}\",");
            body.append("\"cmds\": "
                    + "[{" + "\"cmd_type\": \"${json-unit.ignore}\","
                    + "\"cmd\": \"${json-unit.ignore}\","
                    + "\"cnt\": \"${json-unit.ignore}\","
                    + "\"ela\": \"${json-unit.ignore}\","
                    + "\"cpu_user\": \"${json-unit.ignore}\","
                    + "\"cpu_system\": \"${json-unit.ignore}\","
                    + "\"mem\": \"${json-unit.ignore}\"" + "}],");
            body.append("\"summary\": [");
            for (int j = 0; j < boundaries.size(); j++) {
                body.append("{\"rounded_ela\":").append(boundaries.get(j))
                        .append(", \"cnt\": \"${json-unit.ignore}\"}");

                if (j != boundaries.size() - 1) {
                    body.append(",");
                }
            }
            body.append("]," + "\"funcs_ela\": {\"").append("db:read").append("\": {");
            for (int j = 0; j < boundaries.size(); j++) {
                body.append("\"").append(boundaries.get(j)).append("\":\"${json-unit.ignore}\"");

                if (j != boundaries.size() - 1) {
                    body.append(",");
                }
            }
            body.append("}}}");

            if (i != regexps.size() - 1) {
                body.append(",");
            }
        }

        body.append("]}}");
        return body.toString();
    }
}

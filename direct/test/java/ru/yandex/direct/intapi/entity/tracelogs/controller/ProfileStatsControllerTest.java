package ru.yandex.direct.intapi.entity.tracelogs.controller;

import java.sql.ResultSetMetaData;
import java.util.Arrays;
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
import ru.yandex.direct.intapi.entity.tracelogs.model.profilestats.ProfileData;
import ru.yandex.direct.intapi.entity.tracelogs.service.profilestats.ProfileStatsService;
import ru.yandex.direct.intapi.entity.tracelogs.service.profilestatsbyela.ProfileStatsByElaService;

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
public class ProfileStatsControllerTest {
    private static final String URL = "/trace_logs/profile_stats";

    private static final String FUNC_ELA = "func_ela";
    private static final String FUNC_CNT = "func_cnt";
    private static final String FUNC_OBJ_NUM = "func_obj_num";
    private static final String FUNC_OBJ_NUM_1 = "func_obj_num_1";
    private static final String FUNC_ELA_AVG = "func_ela_avg";
    private static final String FUNC_OBJ_ELA_AVG = "func_obj_ela_avg";
    private static final String ELA = "ela";
    private static final String CNT = "cnt";
    private static final String ELA_AVG = "ela_avg";
    private static final String CPU_USER = "cpu_user";
    private static final String CPU_SYSTEM = "cpu_system";
    private static final String MEM = "mem";

    private static final String FUNC = "func";
    private static final String CMD_TYPE = "cmd_type";

    private static final String WRONG_BODY_TIME = "{\"group_by\":[\"func\"]}";
    private static final String WRONG_BODY_DATES = "{"
            + "\"group_by\": [\"func\"], "
            + "\"time_from\": \"2018-11-20 16:17:10\", "
            + "\"time_to\": \"2018-11-20 23:59:59\""
            + "}";
    private static final String WRONG_BODY_TIMES = "{"
            + "\"group_by\":[\"cmd_type\"], "
            + "\"time_from\": \"2018-11-20 18:17:10\", "
            + "\"time_to\": \"2018-11-20 16:59:59\""
            + "}";
    private static final String WRONG_BODY_GROUP_BY_1 = "{}";
    private static final String WRONG_BODY_GROUP_BY_2 = "{\"group_by\":[]}";

    private DatabaseWrapper clickHouseJdbcTemplate;

    private MockMvc mockMvc;

    @Before
    public void before() {
        clickHouseJdbcTemplate = mock(DatabaseWrapper.class);
        DatabaseWrapperProvider dbProvider = mock(DatabaseWrapperProvider.class);
        ProfileStatsService profileStatsService = new ProfileStatsService(dbProvider);
        ProfileStatsByElaService profileStatsByElaService = mock(ProfileStatsByElaService.class);
        TraceLogsController controller = new TraceLogsController(profileStatsService, profileStatsByElaService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
        when(dbProvider.get(SimpleDb.CLICKHOUSE_CLOUD)).thenReturn(clickHouseJdbcTemplate);
    }

    @Test(expected = NestedServletException.class)
    public void timePeriodAndTimeFromNullExceptionTest() throws Exception {
        mockMvc
                .perform(
                        post(URL)
                                .content(WRONG_BODY_TIME)
                                .contentType(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().is5xxServerError());
    }

    @Test(expected = NestedServletException.class)
    public void datesEmptyExceptionTest() throws Exception {
        mockMvc
                .perform(
                        post(URL)
                                .content(WRONG_BODY_DATES)
                                .contentType(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().is5xxServerError());
    }

    @Test(expected = NestedServletException.class)
    public void BodyEmptyExceptionTest() throws Exception {
        mockMvc
                .perform(
                        post(URL)
                                .content(WRONG_BODY_GROUP_BY_1)
                                .contentType(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().is5xxServerError());
    }

    @Test(expected = NestedServletException.class)
    public void groupByEmptyExceptionTest() throws Exception {
        mockMvc
                .perform(
                        post(URL)
                                .content(WRONG_BODY_GROUP_BY_2)
                                .contentType(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().is5xxServerError());
    }

    @Test(expected = NestedServletException.class)
    public void wrongTimesExceptionTest() throws Exception {
        mockMvc
                .perform(
                        post(URL)
                                .content(WRONG_BODY_TIMES)
                                .contentType(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().is5xxServerError());
    }

    @Test
    public void simpleFuncQueryTest() throws Exception {
        ClickHouseResultSet resultSet = mock(ClickHouseResultSet.class);
        ResultSetMetaData metaData = mock(ResultSetMetaData.class);
        when(resultSet.getMetaData()).thenReturn(metaData);
        when(metaData.getColumnCount()).thenReturn(5);

        when(metaData.getColumnName(1)).thenReturn(FUNC);
        when(metaData.getColumnName(2)).thenReturn(FUNC_ELA);
        when(metaData.getColumnName(3)).thenReturn(FUNC_CNT);
        when(metaData.getColumnName(4)).thenReturn(FUNC_OBJ_NUM);
        when(metaData.getColumnName(5)).thenReturn(FUNC_OBJ_NUM_1);

        mockFuncResultSet(resultSet);

        when(resultSet.getString(FUNC)).thenReturn("db:read");

        String requestBody = "{"
                + "\"time_period\": 10,"
                + "\"group_by\":[\"func\"],"
                + "\"filters\":{\"cmd_type\":[\"Cmd\",\"PublicCmd\",\"direct.web\"]},"
                + "\"time_agg\":10"
                + "}";

        List<String> fields = Arrays.asList(FUNC_ELA, FUNC_CNT, FUNC_OBJ_NUM, FUNC_ELA_AVG, FUNC_OBJ_ELA_AVG);
        List<String> textSortFields = singletonList(FUNC);
        String responseBody = getResponseBody(textSortFields, fields);

        sendRequest(requestBody, responseBody);
    }

    @Test
    public void emptyFiltersTest() throws Exception {
        ClickHouseResultSet resultSet = mock(ClickHouseResultSet.class);
        ResultSetMetaData metaData = mock(ResultSetMetaData.class);
        when(resultSet.getMetaData()).thenReturn(metaData);
        when(metaData.getColumnCount()).thenReturn(5);

        when(metaData.getColumnName(1)).thenReturn(FUNC);
        when(metaData.getColumnName(2)).thenReturn(FUNC_ELA);
        when(metaData.getColumnName(3)).thenReturn(FUNC_CNT);
        when(metaData.getColumnName(4)).thenReturn(FUNC_OBJ_NUM);
        when(metaData.getColumnName(5)).thenReturn(FUNC_OBJ_NUM_1);

        when(resultSet.getString(FUNC)).thenReturn("db:read");

        mockFuncResultSet(resultSet);

        String requestBody = "{"
                + "\"time_period\": 10,"
                + "\"group_by\":[\"func\"],"
                + "\"filters\":{},"
                + "\"time_agg\":10"
                + "}";

        List<String> fields = Arrays.asList(FUNC_ELA, FUNC_CNT, FUNC_OBJ_NUM, FUNC_ELA_AVG, FUNC_OBJ_ELA_AVG);
        List<String> textSortFields = singletonList(FUNC);
        String responseBody = getResponseBody(textSortFields, fields);

        sendRequest(requestBody, responseBody);
    }

    @Test
    public void simpleStatsQueryTest() throws Exception {
        ClickHouseResultSet resultSet = mock(ClickHouseResultSet.class);
        ResultSetMetaData metaData = mock(ResultSetMetaData.class);
        when(resultSet.getMetaData()).thenReturn(metaData);
        when(metaData.getColumnCount()).thenReturn(7);

        when(metaData.getColumnName(1)).thenReturn(CMD_TYPE);
        when(metaData.getColumnName(2)).thenReturn(ELA);
        when(metaData.getColumnName(3)).thenReturn(CPU_USER);
        when(metaData.getColumnName(4)).thenReturn(CPU_SYSTEM);
        when(metaData.getColumnName(5)).thenReturn(MEM);
        when(metaData.getColumnName(6)).thenReturn(CNT);
        when(metaData.getColumnName(7)).thenReturn(ELA_AVG);

        when(resultSet.getString(CMD_TYPE)).thenReturn("direct.script");

        mockStatsResultSet(resultSet);

        String requestBody = "{"
                + "\"group_by\":[\"cmd_type\"],"
                + "\"filters\":{},"
                + "\"time_agg\":10,"
                + "\"time_from\": \"2018-11-20 12:12:12\","
                + "\"time_to\": \"2018-11-20 13:13:13\""
                + "}";

        List<String> fields = Arrays.asList(ELA, CNT, ELA_AVG, CPU_USER, CPU_SYSTEM, MEM);
        List<String> textSortFields = singletonList(CMD_TYPE);
        String responseBody = getResponseBody(textSortFields, fields);

        sendRequest(requestBody, responseBody);
    }

    @Test
    public void profileFilterQueryTest() throws Exception {
        ClickHouseResultSet resultSet = mock(ClickHouseResultSet.class);
        ResultSetMetaData metaData = mock(ResultSetMetaData.class);
        when(resultSet.getMetaData()).thenReturn(metaData);
        when(metaData.getColumnCount()).thenReturn(5);

        when(metaData.getColumnName(1)).thenReturn(FUNC);
        when(metaData.getColumnName(2)).thenReturn(FUNC_ELA);
        when(metaData.getColumnName(3)).thenReturn(FUNC_CNT);
        when(metaData.getColumnName(4)).thenReturn(FUNC_OBJ_NUM);
        when(metaData.getColumnName(5)).thenReturn(FUNC_OBJ_NUM_1);

        when(resultSet.getString(FUNC)).thenReturn("db:read");

        mockStatsResultSet(resultSet);

        String requestBody = "{"
                + "\"time_from\":\"2018-11-27 12:00:00\","
                + "\"time_to\" : \"2018-11-29 12:00:00\","
                + "\"group_by\":[\"func\"],"
                + "\"filters\":{\"func\":[\"db:%\"]}"
                + "}";

        List<String> fields = Arrays.asList(FUNC_ELA, FUNC_CNT, FUNC_OBJ_NUM, FUNC_ELA_AVG, FUNC_OBJ_ELA_AVG);
        List<String> textSortFields = singletonList(FUNC);
        String responseBody = getResponseBody(textSortFields, fields);

        sendRequest(requestBody, responseBody);
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

    private String getResponseBody(List<String> textSortFields, List<String> fields) {
        StringBuilder body = new StringBuilder("{"
                + "\"error\":null,"
                + "\"result\": {"
                + "\"fields\": [");

        for (String textSortField : textSortFields) {
            body.append("{\"title\":\"").append(textSortField).append("\",\"id\":\"").append(textSortField)
                    .append("\",\"text_sort\": true},");
        }

        for (int i = 0; i < fields.size(); i++) {
            body.append("{\"title\":\"").append(fields.get(i)).append("\",\"id\":\"").append(fields.get(i))
                    .append("\"}");
            if (i != fields.size() - 1) {
                body.append(",");
            }
        }

        body.append("], \"data\":[{");
        for (String field : textSortFields) {
            body.append("\"").append(field).append("\":").append("\"${json-unit.ignore}\",");
        }

        for (int i = 0; i < fields.size(); i++) {
            body.append("\"").append(fields.get(i)).append("\":").append("\"${json-unit.ignore}\"");
            if (i != fields.size() - 1) {
                body.append(",");
            }
        }
        body.append("}]}}");
        return body.toString();
    }

    private void mockStatsResultSet(ClickHouseResultSet resultSet) throws Exception {
        when(resultSet.getLong(CNT)).thenReturn(10L);
        when(resultSet.getDouble(ELA)).thenReturn(0.8);
        when(resultSet.getLong(CPU_USER)).thenReturn(50L);
        when(resultSet.getLong(CPU_SYSTEM)).thenReturn(50L);
        when(resultSet.getLong(MEM)).thenReturn(50L);

        mockMapRower(resultSet);
    }

    private void mockFuncResultSet(ClickHouseResultSet resultSet) throws Exception {
        when(resultSet.getLong(FUNC_CNT)).thenReturn(134L);
        when(resultSet.getDouble(FUNC_ELA)).thenReturn(0.8);
        when(resultSet.getLong(FUNC_OBJ_NUM_1)).thenReturn(10L);
        when(resultSet.getLong(FUNC_OBJ_NUM)).thenReturn(10L);

        mockMapRower(resultSet);
    }

    private void mockMapRower(ClickHouseResultSet resultSet) {
        doAnswer(invocation -> {
            RowMapper<ProfileData> rowMapper = invocation.getArgument(2);
            return singletonList(rowMapper.mapRow(resultSet, 0));
        }).when(clickHouseJdbcTemplate).query(any(), (Object[]) any(), (RowMapper) any());
    }
}

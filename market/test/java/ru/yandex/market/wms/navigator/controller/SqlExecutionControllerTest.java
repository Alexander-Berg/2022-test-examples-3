package ru.yandex.market.wms.navigator.controller;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

import org.assertj.core.api.Assertions;
import org.assertj.core.api.recursive.comparison.RecursiveComparisonConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import ru.yandex.market.wms.navigator.BaseWmsNavigatorTest;
import ru.yandex.market.wms.navigator.dto.enums.Source;
import ru.yandex.market.wms.navigator.dto.sql.SqlCreateRequest;
import ru.yandex.market.wms.navigator.dto.sql.SqlDescriptionResponse;
import ru.yandex.market.wms.navigator.dto.sql.SqlExecutionRequest;
import ru.yandex.market.wms.navigator.dto.sql.SqlExecutionRun;
import ru.yandex.market.wms.navigator.dto.sql.SqlValidationRequest;
import ru.yandex.market.wms.navigator.entity.sql.SqlRun;
import ru.yandex.market.wms.navigator.service.TableService;
import ru.yandex.market.wms.navigator.service.pojo.SqlRow;
import ru.yandex.market.wms.shared.libs.utils.time.WarehouseTimeZoneConverter;

/**
 * Tests of {@link SqlExecutionController}.
 */
class SqlExecutionControllerTest extends BaseWmsNavigatorTest {

    private static final RecursiveComparisonConfiguration IGNORE_FIELDS =
            RecursiveComparisonConfiguration.builder().withIgnoredFields(
                    "descriptionId",
                    "createdAt",
                    "createdLogin",
                    "lastValidationRun.descriptionId",
                    "lastValidationRun.runId",
                    "lastValidationRun.message",
                    "lastValidationRun.runAt",
                    "lastValidationRun.runLogin",
                    "lastValidationRun.source",
                    "lastValidationRun.warehouse",
                    "lastValidationRun.type",
                    "lastValidationRun.sql",
                    "lastExecutionRun.descriptionId",
                    "lastExecutionRun.runId",
                    "lastExecutionRun.message",
                    "lastExecutionRun.runAt",
                    "lastExecutionRun.runLogin",
                    "lastExecutionRun.source",
                    "lastExecutionRun.warehouse",
                    "lastExecutionRun.type",
                    "lastExecutionRun.sql"
            ).build();

    @Autowired
    private TableService tableService;
    @Autowired
    private WarehouseTimeZoneConverter warehouseTimeZoneConverter;
    private NamedParameterJdbcTemplate warehouse1JdbcTemplate;
    private NamedParameterJdbcTemplate warehouse2JdbcTemplate;

    @BeforeEach
    void setUp() throws Exception {
        // assert warehouses is not empty
        mockMvc.perform(MockMvcRequestBuilders.get("/warehouses"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.content().json("""
                        {"unit_test_warehouse":"Unit-test-warehouse","unit_test_warehouse2":"Unit test warehouse #2"}
                        """));
        mockMvc.perform(MockMvcRequestBuilders.get("/unit_test_warehouse/sources"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.content().json("""
                        {"MSSQL":"Оперативная база"}
                        """));
        mockMvc.perform(MockMvcRequestBuilders.get("/unit_test_warehouse2/sources"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.content().json("""
                        {"MSSQL":"Оперативная база"}
                        """));

        // clear table
        warehouse1JdbcTemplate = tableService.getNamedJdbcTemplate(Source.MSSQL, "unit_test_warehouse");
        warehouse1JdbcTemplate.update("truncate table my_schema.my_table", new MapSqlParameterSource());
        warehouse2JdbcTemplate = tableService.getNamedJdbcTemplate(Source.MSSQL, "unit_test_warehouse");
        warehouse2JdbcTemplate.update("truncate table my_schema.my_table", new MapSqlParameterSource());
    }

    @Test
    void testCreate() throws Exception {
        var mvcResult = postJson("/sql/create/unit_test_warehouse/MSSQL", new SqlCreateRequest("select 1"));

        var sqlResponse = readJson(mvcResult, SqlDescriptionResponse.class);
        Assertions.assertThat(sqlResponse)
                .usingRecursiveComparison(IGNORE_FIELDS)
                .isEqualTo(SqlDescriptionResponse.builder()
                        .sql("select 1")
                        .warehouse("unit_test_warehouse")
                        .source(Source.MSSQL)
                );
    }

    @Test
    void testValidate() throws Exception {
        var createResult = postJson("/sql/create/unit_test_warehouse/MSSQL", new SqlCreateRequest("select 1 as one"));
        var sqlResponse = readJson(createResult, SqlDescriptionResponse.class);

        var validationResult = postJson("/sql/validate/" + sqlResponse.getDescriptionId(),
                new SqlValidationRequest(sqlResponse.getDescriptionId()));
        sqlResponse = readJson(validationResult, SqlDescriptionResponse.class);
        Assertions.assertThat(sqlResponse)
                .usingRecursiveComparison(IGNORE_FIELDS)
                .isEqualTo(SqlDescriptionResponse.builder()
                        .sql("select 1 as one")
                        .warehouse("unit_test_warehouse")
                        .source(Source.MSSQL)
                        .lastValidationRun(SqlExecutionRun.builder()
                                .state(SqlRun.State.SUCCESS)
                                .message("Ok")
                                .count(1)
                                .sampleResults(List.of(new SqlRow().addValue("ONE", "1")))
                                .build())
                );
    }

    @Test
    void testValidationFailed() throws Exception {
        var createResult = postJson("/sql/create/unit_test_warehouse/MSSQL", new SqlCreateRequest("bla-bla-bla"));
        var sqlResponse = readJson(createResult, SqlDescriptionResponse.class);

        var validationResult = postJson("/sql/validate/" + sqlResponse.getDescriptionId(),
                new SqlValidationRequest(sqlResponse.getDescriptionId()));
        sqlResponse = readJson(validationResult, SqlDescriptionResponse.class);
        Assertions.assertThat(sqlResponse)
                .usingRecursiveComparison(IGNORE_FIELDS)
                .isEqualTo(SqlDescriptionResponse.builder()
                        .sql("bla-bla-bla")
                        .warehouse("unit_test_warehouse")
                        .source(Source.MSSQL)
                        .lastValidationRun(SqlExecutionRun.builder()
                                .state(SqlRun.State.FAILED)
                                .message("Ok")
                                .build())
                );
        Assertions.assertThat(sqlResponse.getLastValidationRun().getMessage())
                .contains("Syntax error in SQL statement \"[*]bla-bla-bla\"");
    }

    @Test
    void testExecute() throws Exception {
        var createResult = postJson("/sql/create/unit_test_warehouse/MSSQL", new SqlCreateRequest("select 1 as one"));
        var sqlResponse = readJson(createResult, SqlDescriptionResponse.class);

        var validationResult = postJson("/sql/validate/" + sqlResponse.getDescriptionId(),
                new SqlValidationRequest(sqlResponse.getDescriptionId()));
        sqlResponse = readJson(validationResult, SqlDescriptionResponse.class);
        Assertions.assertThat(sqlResponse.getLastValidationRun()).isNotNull();
        Assertions.assertThat(sqlResponse.getLastValidationRun().getMessage()).isEqualTo("Ok");
        Assertions.assertThat(sqlResponse.getLastValidationRun().getState()).isEqualTo(SqlRun.State.SUCCESS);

        var executionResult = postJson("/sql/execute/" + sqlResponse.getDescriptionId(),
                new SqlExecutionRequest(sqlResponse.getDescriptionId()));
        sqlResponse = readJson(executionResult, SqlDescriptionResponse.class);

        Assertions.assertThat(sqlResponse)
                .usingRecursiveComparison(IGNORE_FIELDS)
                .isEqualTo(SqlDescriptionResponse.builder()
                        .sql("select 1 as one")
                        .warehouse("unit_test_warehouse")
                        .source(Source.MSSQL)
                        .lastValidationRun(SqlExecutionRun.builder()
                                .state(SqlRun.State.SUCCESS)
                                .message("Ok")
                                .count(1)
                                .sampleResults(List.of(new SqlRow().addValue("ONE", "1")))
                                .build())
                        .lastExecutionRun(SqlExecutionRun.builder()
                                .state(SqlRun.State.SUCCESS)
                                .message("Ok")
                                .count(1)
                                .sampleResults(List.of(new SqlRow().addValue("ONE", "1")))
                                .build())
                );
    }

    @Test
    void testSelect() throws Exception {
        warehouse1JdbcTemplate.update("insert into my_schema.my_table (name) values ('cat'), ('dog'), ('mouse')",
                Map.of());

        var sql = "select name from my_schema.my_table where name in ('cat', 'dog') order by name";
        var createResult = postJson("/sql/create/unit_test_warehouse/MSSQL", new SqlCreateRequest(sql));
        var sqlResponse = readJson(createResult, SqlDescriptionResponse.class);

        var validationResult = postJson("/sql/validate/" + sqlResponse.getDescriptionId(),
                new SqlValidationRequest(sqlResponse.getDescriptionId()));
        sqlResponse = readJson(validationResult, SqlDescriptionResponse.class);
        Assertions.assertThat(sqlResponse.getLastValidationRun()).isNotNull();
        Assertions.assertThat(sqlResponse.getLastValidationRun().getMessage()).isEqualTo("Ok");
        Assertions.assertThat(sqlResponse.getLastValidationRun().getState()).isEqualTo(SqlRun.State.SUCCESS);

        var executionResult = postJson("/sql/execute/" + sqlResponse.getDescriptionId(),
                new SqlExecutionRequest(sqlResponse.getDescriptionId()));
        sqlResponse = readJson(executionResult, SqlDescriptionResponse.class);

        Assertions.assertThat(sqlResponse)
                .usingRecursiveComparison(IGNORE_FIELDS)
                .isEqualTo(SqlDescriptionResponse.builder()
                        .sql(sql)
                        .warehouse("unit_test_warehouse")
                        .source(Source.MSSQL)
                        .lastValidationRun(SqlExecutionRun.builder()
                                .state(SqlRun.State.SUCCESS)
                                .message("Ok")
                                .count(2)
                                .sampleResults(List.of(
                                        new SqlRow().addValue("NAME", "cat"),
                                        new SqlRow().addValue("NAME", "dog")
                                ))
                                .build())
                        .lastExecutionRun(SqlExecutionRun.builder()
                                .state(SqlRun.State.SUCCESS)
                                .message("Ok")
                                .count(2)
                                .sampleResults(List.of(
                                        new SqlRow().addValue("NAME", "cat"),
                                        new SqlRow().addValue("NAME", "dog")
                                ))
                                .build())
                );
    }

    @Test
    void testInsert() throws Exception {
        var sql = "insert into my_schema.my_table (name) values ('cat'), ('dog'), ('mouse')";
        var createResult = postJson("/sql/create/unit_test_warehouse/MSSQL", new SqlCreateRequest(sql));
        var sqlResponse = readJson(createResult, SqlDescriptionResponse.class);

        var validationResult = postJson("/sql/validate/" + sqlResponse.getDescriptionId(),
                new SqlValidationRequest(sqlResponse.getDescriptionId()));
        sqlResponse = readJson(validationResult, SqlDescriptionResponse.class);
        Assertions.assertThat(sqlResponse.getLastValidationRun()).isNotNull();
        Assertions.assertThat(sqlResponse.getLastValidationRun().getMessage()).isEqualTo("Ok");
        Assertions.assertThat(sqlResponse.getLastValidationRun().getState()).isEqualTo(SqlRun.State.SUCCESS);
        Assertions.assertThat(sqlResponse.getLastValidationRun().getCount()).isEqualTo(3);

        var names = warehouse1JdbcTemplate.queryForList("select name from my_schema.my_table", Map.of(), String.class);
        Assertions.assertThat(names).isEmpty();

        var executionResult = postJson("/sql/execute/" + sqlResponse.getDescriptionId(),
                new SqlExecutionRequest(sqlResponse.getDescriptionId()));
        sqlResponse = readJson(executionResult, SqlDescriptionResponse.class);

        Assertions.assertThat(sqlResponse)
                .usingRecursiveComparison(IGNORE_FIELDS)
                .isEqualTo(SqlDescriptionResponse.builder()
                        .sql(sql)
                        .warehouse("unit_test_warehouse")
                        .source(Source.MSSQL)
                        .lastValidationRun(SqlExecutionRun.builder()
                                .state(SqlRun.State.SUCCESS)
                                .message("Ok")
                                .count(3)
                                .build())
                        .lastExecutionRun(SqlExecutionRun.builder()
                                .state(SqlRun.State.SUCCESS)
                                .message("Ok")
                                .count(3)
                                .build())
                );

        names = warehouse1JdbcTemplate.queryForList("select name from my_schema.my_table", Map.of(), String.class);
        Assertions.assertThat(names).containsExactlyInAnyOrder("cat", "dog", "mouse");
    }

    @Test
    void testUpdate() throws Exception {
        var sql = "update my_schema.my_table set NAME = 'ivan'";
        var createResult = postJson("/sql/create/unit_test_warehouse/MSSQL", new SqlCreateRequest(sql));
        var sqlResponse = readJson(createResult, SqlDescriptionResponse.class);

        var validationResult = postJson("/sql/validate/" + sqlResponse.getDescriptionId(),
                new SqlValidationRequest(sqlResponse.getDescriptionId()));
        sqlResponse = readJson(validationResult, SqlDescriptionResponse.class);
        Assertions.assertThat(sqlResponse.getLastValidationRun()).isNotNull();
        Assertions.assertThat(sqlResponse.getLastValidationRun().getMessage()).isEqualTo("Ok");
        Assertions.assertThat(sqlResponse.getLastValidationRun().getState()).isEqualTo(SqlRun.State.SUCCESS);

        var executionResult = postJson("/sql/execute/" + sqlResponse.getDescriptionId(),
                new SqlExecutionRequest(sqlResponse.getDescriptionId()));
        sqlResponse = readJson(executionResult, SqlDescriptionResponse.class);

        Assertions.assertThat(sqlResponse)
                .usingRecursiveComparison(IGNORE_FIELDS)
                .isEqualTo(SqlDescriptionResponse.builder()
                        .sql(sql)
                        .warehouse("unit_test_warehouse")
                        .source(Source.MSSQL)
                        .lastValidationRun(SqlExecutionRun.builder()
                                .state(SqlRun.State.SUCCESS)
                                .message("Ok")
                                .count(0)
                                .build())
                        .lastExecutionRun(SqlExecutionRun.builder()
                                .state(SqlRun.State.SUCCESS)
                                .message("Ok")
                                .count(0)
                                .build())
                );
    }

    @Test
    void testUpdate2() throws Exception {
        var sql = "update my_schema.my_table as mt set NAME = " +
                "(select name from my_schema.my_table as mt2 WHERE mt.id=mt.id)";
        var createResult = postJson("/sql/create/unit_test_warehouse/MSSQL", new SqlCreateRequest(sql));
        var sqlResponse = readJson(createResult, SqlDescriptionResponse.class);

        var validationResult = postJson("/sql/validate/" + sqlResponse.getDescriptionId(),
                new SqlValidationRequest(sqlResponse.getDescriptionId()));
        sqlResponse = readJson(validationResult, SqlDescriptionResponse.class);
        Assertions.assertThat(sqlResponse.getLastValidationRun()).isNotNull();
        Assertions.assertThat(sqlResponse.getLastValidationRun().getMessage()).isEqualTo("Ok");
        Assertions.assertThat(sqlResponse.getLastValidationRun().getState()).isEqualTo(SqlRun.State.SUCCESS);

        var executionResult = postJson("/sql/execute/" + sqlResponse.getDescriptionId(),
                new SqlExecutionRequest(sqlResponse.getDescriptionId()));
        sqlResponse = readJson(executionResult, SqlDescriptionResponse.class);

        Assertions.assertThat(sqlResponse)
                .usingRecursiveComparison(IGNORE_FIELDS)
                .isEqualTo(SqlDescriptionResponse.builder()
                        .sql(sql)
                        .warehouse("unit_test_warehouse")
                        .source(Source.MSSQL)
                        .lastValidationRun(SqlExecutionRun.builder()
                                .state(SqlRun.State.SUCCESS)
                                .message("Ok")
                                .count(0)
                                .build())
                        .lastExecutionRun(SqlExecutionRun.builder()
                                .state(SqlRun.State.SUCCESS)
                                .message("Ok")
                                .count(0)
                                .build())
                );
    }

    @Test
    void testDelete() throws Exception {
        warehouse1JdbcTemplate.update("insert into my_schema.my_table (name) values ('cat'), ('dog'), ('mouse')",
                Map.of());

        var sql = "delete from my_schema.my_table where name in ('cat', 'dog')";
        var createResult = postJson("/sql/create/unit_test_warehouse/MSSQL", new SqlCreateRequest(sql));
        var sqlResponse = readJson(createResult, SqlDescriptionResponse.class);

        var validationResult = postJson("/sql/validate/" + sqlResponse.getDescriptionId(),
                new SqlValidationRequest(sqlResponse.getDescriptionId()));
        sqlResponse = readJson(validationResult, SqlDescriptionResponse.class);
        Assertions.assertThat(sqlResponse.getLastValidationRun()).isNotNull();
        Assertions.assertThat(sqlResponse.getLastValidationRun().getMessage()).isEqualTo("Ok");
        Assertions.assertThat(sqlResponse.getLastValidationRun().getState()).isEqualTo(SqlRun.State.SUCCESS);
        Assertions.assertThat(sqlResponse.getLastValidationRun().getCount()).isEqualTo(2);

        var names = warehouse1JdbcTemplate.queryForList("select name from my_schema.my_table", Map.of(), String.class);
        Assertions.assertThat(names).containsExactlyInAnyOrder("cat", "dog", "mouse");

        var executionResult = postJson("/sql/execute/" + sqlResponse.getDescriptionId(),
                new SqlExecutionRequest(sqlResponse.getDescriptionId()));
        sqlResponse = readJson(executionResult, SqlDescriptionResponse.class);

        Assertions.assertThat(sqlResponse)
                .usingRecursiveComparison(IGNORE_FIELDS)
                .isEqualTo(SqlDescriptionResponse.builder()
                        .sql(sql)
                        .warehouse("unit_test_warehouse")
                        .source(Source.MSSQL)
                        .lastValidationRun(SqlExecutionRun.builder()
                                .state(SqlRun.State.SUCCESS)
                                .message("Ok")
                                .count(2)
                                .build())
                        .lastExecutionRun(SqlExecutionRun.builder()
                                .state(SqlRun.State.SUCCESS)
                                .message("Ok")
                                .count(2)
                                .build())
                );

        names = warehouse1JdbcTemplate.queryForList("select name from my_schema.my_table", Map.of(), String.class);
        Assertions.assertThat(names).containsExactlyInAnyOrder("mouse");
    }

    @Test
    void testNotAllowedExecuteBeforeValidate() throws Exception {
        var sql = "insert tra-ta-ta-ta!!";
        var createResult = postJson("/sql/create/unit_test_warehouse/MSSQL", new SqlCreateRequest(sql));
        var sqlResponse = readJson(createResult, SqlDescriptionResponse.class);

        var validationResult = postJson("/sql/validate/" + sqlResponse.getDescriptionId(),
                new SqlValidationRequest(sqlResponse.getDescriptionId()));
        sqlResponse = readJson(validationResult, SqlDescriptionResponse.class);
        Assertions.assertThat(sqlResponse.getLastValidationRun()).isNotNull();
        Assertions.assertThat(sqlResponse.getLastValidationRun().getState()).isEqualTo(SqlRun.State.FAILED);

        var executionResult = post400Json("/sql/execute/" + sqlResponse.getDescriptionId(),
                new SqlExecutionRequest(sqlResponse.getDescriptionId()));
        Assertions.assertThat(executionResult.getResponse().getContentAsString())
                .contains("Sql (descriptionId: " + sqlResponse.getDescriptionId() + ") is not validated");
    }

    @Test
    void testNotAllowedExecuteIfValidationFailed() throws Exception {
        var sql = "insert into my_schema.my_table (name) values ('cat'), ('dog'), ('mouse')";
        var createResult = postJson("/sql/create/unit_test_warehouse/MSSQL", new SqlCreateRequest(sql));
        var sqlResponse = readJson(createResult, SqlDescriptionResponse.class);

        var executionResult = post400Json("/sql/execute/" + sqlResponse.getDescriptionId(),
                new SqlExecutionRequest(sqlResponse.getDescriptionId()));
        Assertions.assertThat(executionResult.getResponse().getContentAsString())
                .contains("Sql (descriptionId: " + sqlResponse.getDescriptionId() + ") is not validated");
    }

    @Test
    void testList() throws Exception {
        var sql = "insert into my_schema.my_table (name) values ('cat'), ('dog'), ('mouse')";

        var create11 = postJson("/sql/create/unit_test_warehouse/MSSQL",
                new SqlCreateRequest(sql), SqlDescriptionResponse.class);
        var create21 = postJson("/sql/create/unit_test_warehouse2/MSSQL",
                new SqlCreateRequest(sql), SqlDescriptionResponse.class);

        postJson("/sql/validate/" + create11.getDescriptionId(), new SqlExecutionRequest(create11.getDescriptionId()));
        postJson("/sql/execute/" + create11.getDescriptionId(), new SqlExecutionRequest(create11.getDescriptionId()));

        postJson("/sql/validate/" + create21.getDescriptionId(), new SqlExecutionRequest(create21.getDescriptionId()));
        postJson("/sql/execute/" + create21.getDescriptionId(), new SqlExecutionRequest(create21.getDescriptionId()));

        var response = getJson("/sql");
        JSONAssert.assertEquals("""
                {
                    "limit": 20,
                    "offset": 0,
                    "total": 4,
                    "content": [
                        {
                            "warehouse": "unit_test_warehouse2",
                            "sql": "insert into my_schema.my_table (name) values ('cat'), ('dog'), ('mouse')",
                            "type": "EXECUTION",
                            "state": "SUCCESS"
                        },
                        {
                            "warehouse": "unit_test_warehouse2",
                            "sql": "insert into my_schema.my_table (name) values ('cat'), ('dog'), ('mouse')",
                            "type": "VALIDATION",
                            "state": "SUCCESS"
                        },
                        {
                            "warehouse": "unit_test_warehouse",
                            "sql": "insert into my_schema.my_table (name) values ('cat'), ('dog'), ('mouse')",
                            "type": "EXECUTION",
                            "state": "SUCCESS"
                        },
                        {
                            "warehouse": "unit_test_warehouse",
                            "sql": "insert into my_schema.my_table (name) values ('cat'), ('dog'), ('mouse')",
                            "type": "VALIDATION",
                            "state": "SUCCESS"
                        }
                    ]
                }
                """, response.getResponse().getContentAsString(), JSONCompareMode.STRICT_ORDER);

        var response2 = getJson("/sql?filter=type==EXECUTION");
        JSONAssert.assertEquals("""
                {
                    "limit": 20,
                    "offset": 0,
                    "total": 2,
                    "content": [
                        {
                            "warehouse": "unit_test_warehouse2",
                            "sql": "insert into my_schema.my_table (name) values ('cat'), ('dog'), ('mouse')",
                            "type": "EXECUTION",
                            "state": "SUCCESS"
                        },
                        {
                            "warehouse": "unit_test_warehouse",
                            "sql": "insert into my_schema.my_table (name) values ('cat'), ('dog'), ('mouse')",
                            "type": "EXECUTION",
                            "state": "SUCCESS"
                        }
                    ]
                }
                """, response2.getResponse().getContentAsString(), JSONCompareMode.STRICT_ORDER);

        var response3 = getJson("/sql?filter=descriptionId==" + create11.getDescriptionId() + "&sort=runAt&order=ASC");
        JSONAssert.assertEquals("""
                {
                    "limit": 20,
                    "offset": 0,
                    "total": 2,
                    "content": [
                        {
                            "warehouse": "unit_test_warehouse",
                            "sql": "insert into my_schema.my_table (name) values ('cat'), ('dog'), ('mouse')",
                            "type": "VALIDATION",
                            "state": "SUCCESS"
                        },
                        {
                            "warehouse": "unit_test_warehouse",
                            "sql": "insert into my_schema.my_table (name) values ('cat'), ('dog'), ('mouse')",
                            "type": "EXECUTION",
                            "state": "SUCCESS"
                        }
                    ]
                }
                """, response3.getResponse().getContentAsString(), JSONCompareMode.STRICT_ORDER);

        var nowTimeStr = warehouseTimeZoneConverter.convertFromInstant(Instant.now().plus(10, ChronoUnit.SECONDS))
                .format(warehouseTimeZoneConverter.getDefaultFormatter());

        var response4 = getJson("/sql?filter=descriptionId==%d;runAt<'%s'"
                .formatted(create11.getDescriptionId(), nowTimeStr));
        JSONAssert.assertEquals("""
                {
                    "limit": 20,
                    "offset": 0,
                    "total": 2,
                    "content": [
                        {
                            "warehouse": "unit_test_warehouse",
                            "sql": "insert into my_schema.my_table (name) values ('cat'), ('dog'), ('mouse')",
                            "type": "EXECUTION",
                            "state": "SUCCESS"
                        },
                        {
                            "warehouse": "unit_test_warehouse",
                            "sql": "insert into my_schema.my_table (name) values ('cat'), ('dog'), ('mouse')",
                            "type": "VALIDATION",
                            "state": "SUCCESS"
                        }
                    ]
                }
                """, response4.getResponse().getContentAsString(), JSONCompareMode.STRICT_ORDER);
    }
}

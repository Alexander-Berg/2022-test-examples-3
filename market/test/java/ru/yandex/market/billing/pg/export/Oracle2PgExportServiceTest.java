package ru.yandex.market.billing.pg.export;

import java.util.Map;

import name.falgout.jeffrey.testing.junit.mockito.MockitoExtension;
import org.assertj.core.api.Assertions;
import org.codehaus.groovy.syntax.Types;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.billing.FunctionalTest;
import ru.yandex.market.core.util.filters.Filters;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;

@ExtendWith(MockitoExtension.class)
class Oracle2PgExportServiceTest extends FunctionalTest {

    private static final String ORA_TABLE = "market_billing.table_ora";
    private static final String PG_TABLE = "market_billing.table_pg";
    public static final SqlTableMeta TEST_TABLE_META = new SqlTableMeta(ORA_TABLE, PG_TABLE, Map.of("a", Types.NUMBER
            , "b", Types.NUMBER));

    @Autowired
    private TransactionTemplate transactionTemplate;
    @Mock
    private NamedParameterJdbcTemplate oraJdbcTemplateMock;
    @Mock
    private NamedParameterJdbcTemplate pgJdbcTemplateMock;

    private Oracle2PgExportService service;

    @BeforeEach
    void setup() {
        Mockito.when(pgJdbcTemplateMock.batchUpdate(anyString(), any(SqlParameterSource[].class)))
                .thenReturn(new int[0]);
        service = new Oracle2PgExportService(transactionTemplate, pgJdbcTemplateMock, oraJdbcTemplateMock);
    }

    @Test
    void testExport() {
        service.exportToPg(TEST_TABLE_META);

        Mockito.verify(pgJdbcTemplateMock)
                .update(Mockito.argThat(sql -> {
                            Assertions.assertThat(sql)
                                    .startsWith("delete")
                                    .contains(PG_TABLE)
                                    .doesNotContain(ORA_TABLE);
                            return true;
                        }),
                        anyMap()
                );

        Mockito.verify(oraJdbcTemplateMock)
                .query(Mockito.argThat(sql -> {
                            Assertions.assertThat(sql)
                                    .startsWith("select")
                                    .contains(ORA_TABLE)
                                    .doesNotContain(PG_TABLE);
                            return true;
                        }),
                        any(RowCallbackHandler.class)
                );

        Mockito.verifyNoMoreInteractions(oraJdbcTemplateMock);

        Mockito.verify(pgJdbcTemplateMock)
                .batchUpdate(
                        Mockito.argThat(sql -> {
                            Assertions.assertThat(sql)
                                    .startsWith("insert")
                                    .contains(PG_TABLE)
                                    .doesNotContain(ORA_TABLE);
                            return true;
                        }),
                        Mockito.any(SqlParameterSource[].class)
                );

        Mockito.verifyNoMoreInteractions(pgJdbcTemplateMock);
    }

    @Test
    void testExportWithCondition() {
        service.exportToPg(
                TEST_TABLE_META,
                Filters.or(
                        Filters.ge("a", "a", 10),
                        Filters.lt("b", "b", 20)
                )
        );

        Mockito.verify(oraJdbcTemplateMock)
                .query(Mockito.argThat(sql -> {
                            Assertions.assertThat(sql)
                                    .startsWith("select")
                                    .endsWith("where (a >= :a OR b < :b)")
                                    .contains(ORA_TABLE)
                                    .doesNotContain(PG_TABLE);
                            return true;
                        }),
                        argThat((Map<String, Object> map) ->
                                map.containsKey("a") && map.get("a").equals(10)
                                        && map.containsKey("b") && map.get("b").equals(20)
                        ),
                        any(RowCallbackHandler.class)
                );

        Mockito.verifyNoMoreInteractions(oraJdbcTemplateMock);

        Mockito.verify(pgJdbcTemplateMock)
                .batchUpdate(
                        Mockito.argThat(sql -> {
                            Assertions.assertThat(sql)
                                    .startsWith("insert")
                                    .endsWith("on conflict do nothing")
                                    .contains(PG_TABLE)
                                    .doesNotContain(ORA_TABLE);
                            return true;
                        }),
                        Mockito.any(SqlParameterSource[].class)
                );

        Mockito.verifyNoMoreInteractions(pgJdbcTemplateMock);
    }
}

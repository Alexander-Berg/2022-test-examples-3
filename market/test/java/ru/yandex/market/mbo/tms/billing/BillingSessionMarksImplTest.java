package ru.yandex.market.mbo.tms.billing;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.util.Collections;
import java.util.Optional;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/*
 * @author Nur-Magomed Dzhamiev <a href="mailto:n-mago@yandex-team.ru"></a>
 * @date 19.2.2020
 */
@RunWith(MockitoJUnitRunner.class)
public class BillingSessionMarksImplTest {

    private static final String EMPTY_HOST = "";
    private static final String SOME_HOST = "2guns.yandex.ru";

    @Mock
    NamedParameterJdbcTemplate siteCatalogPgNamedJdbcTemplate;

    @Mock
    ResultSet resultSet;

    BillingSessionMarksImpl billingSessionMarks;

    @Before
    public void setUp() {
        billingSessionMarks = new BillingSessionMarksImpl(siteCatalogPgNamedJdbcTemplate);
    }

    @Test
    public void loadLastIncompleteSessionHostnameOk() throws Exception {
        prepareQueryResultMocks(SOME_HOST, false);
        Optional<String> gotHostname = billingSessionMarks.loadLastIncompleteSessionHostname();
        Assertions.assertThat(gotHostname.get()).isEqualTo(SOME_HOST);
    }

    @Test
    public void loadLastIncompleteSessionHostnameNull() throws Exception {
        prepareQueryResultMocks(null, false);
        Optional<String> gotHostname = billingSessionMarks.loadLastIncompleteSessionHostname();
        Assertions.assertThat(gotHostname.isPresent()).isEqualTo(false);
    }

    @Test
    public void loadLastIncompleteSessionHostnameEmpty() throws Exception {
        prepareQueryResultMocks(EMPTY_HOST, false);
        Optional<String> gotHostname = billingSessionMarks.loadLastIncompleteSessionHostname();
        Assertions.assertThat(gotHostname.get()).isEqualTo(EMPTY_HOST);
    }

    @Test
    public void loadLastIncompleteSessionHostnameEmptyResultList() throws Exception {
        prepareQueryResultMocks(EMPTY_HOST, true);
        Optional<String> gotHostname = billingSessionMarks.loadLastIncompleteSessionHostname();
        Assertions.assertThat(gotHostname.isPresent()).isEqualTo(false);
    }

    private void prepareQueryResultMocks(String hostname, boolean emptyResult) throws Exception {
        ResultSetMetaData resultSetMetaData = mock(ResultSetMetaData.class);
        when(resultSetMetaData.getColumnCount()).thenReturn(1);
        when(resultSet.getMetaData()).thenReturn(resultSetMetaData);
        when(resultSet.getObject(eq(1))).thenReturn(hostname);
        doAnswer(invocation -> {
            final int mapperArg = 2;
            RowMapper<String> rmCallback = invocation.getArgument(mapperArg);
            return emptyResult ? Collections.emptyList() :
                Collections.singletonList(rmCallback.mapRow(resultSet, 1));
        }).when(siteCatalogPgNamedJdbcTemplate).query(anyString(), anyMap(), ArgumentMatchers.<RowMapper<String>>any());
    }
}

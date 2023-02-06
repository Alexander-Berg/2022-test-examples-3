package ru.yandex.market.core.database;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Duration;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import javax.sql.DataSource;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.BadSqlGrammarException;
import org.springframework.jdbc.CannotGetJdbcConnectionException;
import org.springframework.jdbc.support.JdbcUtils;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;

import ru.yandex.common.cache.memcached.MemCachedAgent;
import ru.yandex.common.cache.memcached.MemCachedService;
import ru.yandex.common.cache.memcached.MemCachedServiceConfig;
import ru.yandex.common.cache.memcached.MemCachingService;
import ru.yandex.common.cache.memcached.cacheable.ServiceMethodBulkMemCacheable;
import ru.yandex.common.cache.memcached.impl.DefaultMemCachingService;
import ru.yandex.common.transaction.CallbackBasedTransactionManagerMixin;
import ru.yandex.common.util.db.LongRowMapper;
import ru.yandex.market.core.database.OracleToPostgresJdbcTemplate.EventsCollector;
import ru.yandex.market.mbi.FunctionalTest;
import ru.yandex.market.mbi.FunctionalTestConfig;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static ru.yandex.market.core.database.OracleToPostgresJdbcTemplate.ExpQueryRouting;

class DelegatingPlatformTransactionManager implements CallbackBasedTransactionManagerMixin {
    private PlatformTransactionManager delegate;

    DelegatingPlatformTransactionManager(PlatformTransactionManager delegate) {
        setDelegate(delegate);
    }

    void setDelegate(PlatformTransactionManager delegate) {
        this.delegate = Objects.requireNonNull(delegate);
    }

    @Override
    public TransactionStatus getTransaction(TransactionDefinition definition) {
        return delegate.getTransaction(definition);
    }

    @Override
    public void commit(TransactionStatus status) {
        delegate.commit(status);
    }

    @Override
    public void rollback(TransactionStatus status) {
        delegate.commit(status);
    }

    @Override
    public <T> T execute(TransactionDefinition definition, TransactionCallback<T> callback) {
        return execute(this, definition, callback);
    }

    @Override
    public <T> T execute(
            PlatformTransactionManager self,
            TransactionDefinition definition,
            TransactionCallback<T> callback
    ) {
        return (delegate instanceof CallbackBasedTransactionManagerMixin)
                ? ((CallbackBasedTransactionManagerMixin) delegate).execute(self, definition, callback)
                : CallbackBasedTransactionManagerMixin.doExecute(self, definition, callback);
    }
}

class Config extends FunctionalTestConfig {
    @Bean
    DataSource regularDataSource(DataSource dataSource) {
        return asSpy(dataSource);
    }

    @Bean
    DataSource experimentDataSource(DataSource dataSource) {
        return asSpy(dataSource);
    }

    @Override
    @Bean
    public DelegatingPlatformTransactionManager txManager(
            @Qualifier("regularDataSource") DataSource regularDataSource
    ) {
        return new DelegatingPlatformTransactionManager(super.txManager(regularDataSource));
    }

    private static DataSource asSpy(DataSource dataSource) {
        var md = Mockito.mockingDetails(dataSource);
        assertThat(md.isMock()).isFalse();
        assertThat(md.isSpy()).isFalse();
        return spy(dataSource);
    }
}

@SpringJUnitConfig(classes = Config.class)
abstract class OracleToPostgresJdbcTemplateTest extends FunctionalTest {
    static final String SQL_SELECT = "select 1";
    static final String SQL_UPDATE = "create local temporary table if not exists xxx(id int)";
    Supplier<ExpQueryRouting> experiment = mock(Supplier.class);
    EventsCollector collector = mock(EventsCollector.class);

    @Autowired
    DataSource regularDataSource;

    @Autowired
    DataSource experimentDataSource;

    @Autowired
    DelegatingPlatformTransactionManager txManager;

    @BeforeEach
    void setUp() {
        when(experiment.get()).thenReturn(ExpQueryRouting.WITH_FALLBACK);
        setUpWithRegularDataSource(regularDataSource);
    }

    private void setUpWithRegularDataSource(DataSource regularDataSource) {
        var readOnly = areDataSourcesReadOnly();
        jdbcTemplate = new OracleToPostgresJdbcTemplate(
                regularDataSource,
                experimentDataSource,
                readOnly,
                experiment,
                collector
        );
        txManager.setDelegate(new OracleToPostgresDataSourceTransactionManager(
                regularDataSource,
                experimentDataSource,
                readOnly,
                experiment
        ));
    }

    protected abstract boolean areDataSourcesReadOnly();

    @Test
    void shouldIgnoreDataSourceSetterInFavorOfCtor() {
        // given
        var other = mock(DataSource.class);
        when(experiment.get()).thenReturn(null); // null works just as disabled

        // when
        var dsBefore = jdbcTemplate.getDataSource();
        jdbcTemplate.setDataSource(other);
        var dsAfter = jdbcTemplate.getDataSource();

        // then
        assertThat(dsAfter)
                .as("setter should ignore passed DataSource to avoid undesired byName autowirings")
                .isSameAs(dsBefore)
                .isNotSameAs(other);
    }

    @Test
    void shouldBeCalledWithEnabledExperiment() {
        // given
        when(experiment.get()).thenReturn(ExpQueryRouting.ENABLED);

        // when
        testEverything();

        // then
        verifyNoInteractions(regularDataSource);
    }

    @Test
    void shouldNotBeCalledWithDisabledExperiment() {
        // given
        when(experiment.get()).thenReturn(null); // null works just as disabled

        // when
        testEverything();

        // then
        verifyNoInteractions(experimentDataSource);
    }

    @Test
    void shouldNotBeCalledWithFailingExperimentSupplier() {
        // given
        when(experiment.get()).thenThrow(new RuntimeException("boom"));

        // when
        testEverything();

        // then
        verifyNoInteractions(experimentDataSource);
    }

    @Test
    void shouldBeCopyCalledWithinTransaction() throws SQLException {
        // when
        transactionTemplate.execute(status -> {
            testSqlSelect();
            return null;
        });

        // then
        verifyCalled(experimentDataSource);
        verifyCalled(regularDataSource);
    }

    @Test
    void shouldPreferFlagOverTransactionOnConflictingRouting() throws SQLException {
        // given
        when(experiment.get())
                .thenReturn(
                        ExpQueryRouting.ENABLED,
                        ExpQueryRouting.ENABLED,
                        ExpQueryRouting.DISABLED // until the end of tx
                );

        // when
        transactionTemplate.execute(status -> {
            testSqlSelect();
            return null;
        });

        // then
        verifyCalled(experimentDataSource);
        verifyCalled(regularDataSource);
    }

    @Test
    void shouldNotRollbackTransactionOnError() throws SQLException {
        // given
        JdbcUtils.closeConnection(doThrow(new SQLException("boom")).when(experimentDataSource).getConnection());

        // when
        transactionTemplate.execute(status -> {
            testSqlSelect();
            return null;
        });

        // then
        verifyCalled(experimentDataSource);
        verifyCalled(regularDataSource);
    }

    @Test
    void shouldBeCopyCalledWithinMemcacheQuerying() throws SQLException {
        // given
        var memCachedAgent = mock(MemCachedAgent.class);
        var memCachingService = new DefaultMemCachingService();
        memCachingService.setMemCachedAgent(memCachedAgent);
        var memCachedService = new MemCachedService() {
            @Override
            public MemCachedServiceConfig getConfig() {
                return MemCachedServiceConfig.of("serviceName", Duration.ofDays(1L));
            }

            @Override
            public MemCachingService getMemCachingService() {
                return memCachingService;
            }
        };
        var memCacheable = new ServiceMethodBulkMemCacheable<Integer, Integer>(memCachedService, "methodName") {
            @Override
            public Map<Integer, Integer> queryNonCachedBulk(Collection<Integer> queries) {
                testSqlSelect();
                return queries.stream().collect(Collectors.toMap(Function.identity(), Function.identity()));
            }

            @Override
            public Integer queryNonCached(Integer query) {
                testSqlSelect();
                return query;
            }
        };

        // when
        var kv = 100500;
        var resultSingle = memCachingService.query(memCacheable, kv);
        var resultBulk = memCachingService.queryBulk(memCacheable, Set.of(kv));

        // then
        assertThat(resultSingle).isEqualTo(kv);
        assertThat(resultBulk).isEqualTo(Map.of(kv, kv));
        verify(memCachedAgent).putInCache(eq("serviceName_methodName_100500"), eq(kv), any());
        verify(memCachedAgent).putInCache(eq(Map.of("serviceName_methodName_100500", kv)), any());
        verifyCalled(experimentDataSource);
        verifyCalled(regularDataSource);
    }

    @Test
    void shouldNotBeCalledWithCallbackQueries() {
        // when
        testCallbackSelect();
        testCallbackUpdate();

        // then
        verifyNoInteractions(experimentDataSource);
    }

    @Test
    void shouldNotBeCalledWithUpdateQueries() {
        // when
        testSqlUpdate();

        // then
        verifyNoInteractions(experimentDataSource);
    }

    @Test
    void shouldNotFailWithFailingCollector() {
        // given
        doThrow(new RuntimeException("boom")).when(collector).onExecute(anyBoolean(), any());

        // when
        testSqlSelect();
    }

    @Test
    void shouldFallbackToRegularDataSourceOnConnectionRetrievalError() throws SQLException {
        // given
        JdbcUtils.closeConnection(doThrow(new SQLException("boom")).when(experimentDataSource).getConnection());

        // when
        testSqlSelect();

        // then
        verifyCalled(regularDataSource);
        verifyCalled(experimentDataSource);
    }

    @Test
    void shouldFallbackToRegularDataSourceOnSqlError() throws SQLException {
        // when
        assertThatExceptionOfType(BadSqlGrammarException.class)
                .isThrownBy(() -> jdbcTemplate.queryForObject("select 1 from missing_table", Long.class));

        // then
        verifyCalled(regularDataSource);
        verifyCalled(experimentDataSource);
    }

    @Test
    void shouldFallbackToRegularDataSourceOnCallbackError() throws SQLException {
        // when
        assertThatExceptionOfType(DataIntegrityViolationException.class)
                .isThrownBy(() -> jdbcTemplate.queryForObject("select 'whatever'", Long.class));

        // then
        verifyCalled(regularDataSource);
        verifyCalled(experimentDataSource);
    }

    /**
     * @see OracleToPostgresJdbcTemplate#getExceptionTranslator()
     */
    @Test
    void shouldTranslateExperimentalDataSourceErrors() throws SQLException {
        // given
        // важно чтобы основной датасорс имел тип, отличный от экспериментального,
        // иначе ошибки второго будут корректно обрабатываться транслятором для первого и ошибку в тесте не заметим
        var mockDataSource = mock(DataSource.class);
        when(mockDataSource.getConnection()).thenThrow(new CannotGetJdbcConnectionException("should not get here"));
        when(experiment.get()).thenReturn(ExpQueryRouting.ENABLED);
        setUpWithRegularDataSource(mockDataSource);
        jdbcTemplate.afterPropertiesSet(); // явно вызываем чтобы гарантированно инициировался транслятор

        // when-then
        var id = ThreadLocalRandom.current().nextInt();
        jdbcTemplate.execute("create temporary table if not exists zzz(id int primary key)");
        jdbcTemplate.update("insert into zzz values (?)", id);
        assertThatExceptionOfType(DuplicateKeyException.class)
                .as("should not throw generic DataIntegrityViolationException here")
                .isThrownBy(() -> jdbcTemplate.update("insert into zzz values (?)", id));
    }

    static void verifyCalled(DataSource dataSource) throws SQLException {
        JdbcUtils.closeConnection(verify(dataSource, atLeastOnce()).getConnection());
    }

    void testEverything() {
        testSqlSelect();
        testSqlUpdate();
        testCallbackSelect();
        testCallbackUpdate();
        transactionTemplate.execute(status -> {
            testSqlSelect();
            testSqlUpdate();

            // nested query should not alter outermost flag for consequent requests
            jdbcTemplate.query(SQL_SELECT, (rs, rowNum) -> jdbcTemplate.update(SQL_UPDATE));

            // nested tx should work too
            return transactionTemplate.execute(statusNested -> {
                testCallbackSelect();
                testCallbackUpdate();
                return null;
            });
        });
    }

    void testSqlSelect() {
        jdbcTemplate.execute(SQL_SELECT);
        assertThat(jdbcTemplate.query(SQL_SELECT, new LongRowMapper())).containsExactly(1L);
        assertThat(jdbcTemplate.queryForList(SQL_SELECT, Long.class)).containsExactly(1L);
        assertThat(jdbcTemplate.queryForMap(SQL_SELECT)).containsEntry("1", 1);
        assertThat(jdbcTemplate.queryForObject(SQL_SELECT, Long.class)).isEqualTo(1L);
        assertThat(jdbcTemplate.queryForRowSet(SQL_SELECT)).isNotNull();
    }

    void testSqlUpdate() {
        assertThat(jdbcTemplate.update(SQL_UPDATE)).isZero();
        assertThat(jdbcTemplate.batchUpdate(SQL_UPDATE)).isNotEmpty();
    }

    void testCallbackUpdate() {
        assertThat(jdbcTemplate.execute((Statement stmt) -> stmt.execute(SQL_UPDATE))).isFalse();
        assertThat(jdbcTemplate.update(con -> con.prepareStatement(SQL_UPDATE))).isZero();
    }

    void testCallbackSelect() {
        assertThat(jdbcTemplate.execute((Connection con) -> {
            try (var st = con.createStatement()) {
                return st.execute(SQL_SELECT);
            }
        })).isTrue();
        assertThat(jdbcTemplate.query(
                con -> con.prepareStatement(SQL_SELECT),
                new LongRowMapper()
        )).containsExactly(1L);
    }
}

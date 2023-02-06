package ru.yandex.direct.jobs.directdb;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import javax.annotation.ParametersAreNonnullByDefault;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;

import ru.yandex.direct.common.db.PpcPropertiesSupport;
import ru.yandex.direct.common.db.PpcProperty;
import ru.yandex.direct.common.db.PpcPropertyData;
import ru.yandex.direct.dbutil.sharding.ShardHelper;
import ru.yandex.direct.env.EnvironmentType;
import ru.yandex.direct.jobs.configuration.DirectExportYtClustersParametersSource;
import ru.yandex.direct.jobs.directdb.helper.TestDoneResultSetFuture;
import ru.yandex.direct.jobs.directdb.helper.TestErrorResultSetFuture;
import ru.yandex.direct.jobs.directdb.helper.TestResultSetFuture;
import ru.yandex.direct.jobs.directdb.helper.TestResultSetFuture2;
import ru.yandex.direct.jobs.directdb.metrics.HomeDirectDbMetricsReporter;
import ru.yandex.direct.jobs.directdb.metrics.HomeDirectDbOperationsMetricProvider;
import ru.yandex.direct.jobs.directdb.model.Operation;
import ru.yandex.direct.jobs.directdb.model.SnapshotAttributes;
import ru.yandex.direct.jobs.directdb.repository.OperationRepository;
import ru.yandex.direct.jobs.directdb.service.HomeDirectDbFullWorkPropObtainerService;
import ru.yandex.direct.jobs.directdb.service.SnapshotUserAttributeService;
import ru.yandex.direct.jobs.directdb.service.YqlClasspathObtainerService;
import ru.yandex.direct.ytwrapper.YtPathUtil;
import ru.yandex.direct.ytwrapper.client.YtClusterConfig;
import ru.yandex.direct.ytwrapper.client.YtProvider;
import ru.yandex.direct.ytwrapper.model.YtCluster;
import ru.yandex.direct.ytwrapper.model.YtOperator;
import ru.yandex.inside.yt.kosher.Yt;
import ru.yandex.inside.yt.kosher.cypress.Cypress;
import ru.yandex.inside.yt.kosher.cypress.YPath;
import ru.yandex.yql.YqlConnection;
import ru.yandex.yql.YqlDataSource;
import ru.yandex.yql.YqlPreparedStatement;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.MockitoAnnotations.initMocks;
import static ru.yandex.direct.common.db.PpcPropertyNames.HOME_DIRECT_DB_DATE;
import static ru.yandex.direct.common.db.PpcPropertyNames.HOME_DIRECT_DB_EXCLUDE;
import static ru.yandex.direct.common.db.PpcPropertyNames.HOME_DIRECT_DB_FULL_WORK;
import static ru.yandex.direct.common.db.PpcPropertyNames.HOME_DIRECT_DB_INCLUDE;
import static ru.yandex.direct.jobs.util.yt.YtEnvPath.relativePart;

@ParametersAreNonnullByDefault
class HomeDirectDbOperationsJobTest {

    private static final String YT_HOME = "//home/direct";
    private static final String HOME_DB_PATH = "db-archive";

    @Mock
    private YtProvider ytProvider;

    @Mock
    private YtOperator ytOperator;

    @Mock
    private DirectExportYtClustersParametersSource parametersSource;

    @Mock
    private SnapshotUserAttributeService snapshotUserAttributeService;

    @Mock
    private YqlClasspathObtainerService yqlClasspathObtainerService;

    @Mock
    private YqlDataSource yqlDataSource;

    @Mock
    private YqlConnection connection;

    @Mock
    private YqlPreparedStatement stmt;

    @Mock
    private YtClusterConfig clusterConfig;

    @Mock
    private Yt yt;

    @Mock
    private Cypress cypress;

    @Mock
    private ShardHelper shardHelper;

    @Mock
    private OperationRepository operationRepository;

    @Mock
    private PpcPropertiesSupport ppcPropertiesSupport;

    @Mock
    private PpcProperty<LocalDate> datePpcProperty;

    @Mock
    private PpcProperty<Set<String>> includePpcProperty;

    @Mock
    private PpcProperty<Set<String>> excludePpcProperty;

    @Mock
    private PpcProperty<Boolean> fullWorkPpcProperty;

    @Mock
    private HomeDirectDbOperationsMetricProvider homeDirectDbOperationsMetricProvider;

    @Mock
    private HomeDirectDbMetricsReporter homeDirectDbMetricsReporter;

    @Captor
    private ArgumentCaptor<Collection<Operation>> collectionArgumentCaptor;

    private HomeDirectDbOperationsJob job;
    private HomeDirectDbFullWorkPropObtainerService fullWorkPropObtainerService;


    @BeforeEach
    void setUp() throws SQLException {

        initMocks(this);

        given(ytProvider.getOperator(any(), any())).willReturn(ytOperator);
        given(ytProvider.getYql(any(), any())).willReturn(yqlDataSource);
        given(ytProvider.getClusterConfig(any())).willReturn(clusterConfig);
        given(ytProvider.get(any())).willReturn(yt);
        given(yt.cypress()).willReturn(cypress);
        given(clusterConfig.getHome()).willReturn(YT_HOME);
        given(yqlDataSource.getConnection()).willReturn(connection);
        given(connection.prepareStatement(any())).willReturn(stmt);
        given(parametersSource.convertStringToParam(any())).willReturn(YtCluster.HAHN);
        given(shardHelper.dbShards()).willReturn(IntStream.range(1, 22).boxed().collect(Collectors.toList()));

        given(ppcPropertiesSupport.get(eq(HOME_DIRECT_DB_DATE))).willReturn(datePpcProperty);
        given(ppcPropertiesSupport.get(eq(HOME_DIRECT_DB_INCLUDE))).willReturn(includePpcProperty);
        given(ppcPropertiesSupport.get(eq(HOME_DIRECT_DB_EXCLUDE))).willReturn(excludePpcProperty);
        given(ppcPropertiesSupport.get(eq(HOME_DIRECT_DB_FULL_WORK), any())).willReturn(fullWorkPpcProperty);
        given(ppcPropertiesSupport.getFullByNames(eq(Set.of(HOME_DIRECT_DB_FULL_WORK.getName()))))
                .willReturn(Map.of(HOME_DIRECT_DB_FULL_WORK.getName(),
                        new PpcPropertyData<>("true", LocalDateTime.now())));

        given(fullWorkPpcProperty.getOrDefault(false)).willReturn(true);

        fullWorkPropObtainerService = new HomeDirectDbFullWorkPropObtainerService(
                ppcPropertiesSupport, EnvironmentType.PRODUCTION);

        job = new HomeDirectDbOperationsJob(
                parametersSource,
                snapshotUserAttributeService,
                yqlClasspathObtainerService,
                ytProvider,
                shardHelper,
                operationRepository,
                ppcPropertiesSupport,
                homeDirectDbOperationsMetricProvider,
                homeDirectDbMetricsReporter,
                fullWorkPropObtainerService
        ) {
            @Override
            public String getParam() {
                return YtCluster.HAHN.name();
            }
        };
    }

    @Test
    @DisplayName("Снепшот не готов, ничего делать не надо")
    void shouldDoNothingIfSnapshotIsNotYetReady() throws SQLException {
        var path = YtPathUtil.generatePath(YT_HOME, relativePart(), "mysql-sync/snapshot--v.13--2020-01-23");
        var attributes = new SnapshotAttributes(path, false);
        given(snapshotUserAttributeService.getConvertedUserAttributes(any()))
                .willReturn(Collections.singletonList(attributes));
        given(operationRepository.getForDate(any(), any())).willReturn(Collections.emptyList());

        job.execute();

        verify(ytOperator, never()).yqlQueryBegin(any());
    }

    @Test
    @DisplayName("Снепшот готов, операций еще не было запущено")
    void shouldRunOperationsIfThereAreNoAndSnapshotIsFinished() throws SQLException {
        var path = YtPathUtil.generatePath(YT_HOME, relativePart(), "mysql-sync/snapshot--v.13--2020-01-23");
        var attributes = new SnapshotAttributes(path, true);
        given(snapshotUserAttributeService.getConvertedUserAttributes(any()))
                .willReturn(Collections.singletonList(attributes));
        given(ytOperator.yqlQueryBegin(any())).willReturn(new TestResultSetFuture());
        given(yqlClasspathObtainerService.obtainYqlQueriesFromClassPath())
                .willReturn(Collections.singletonList(Pair.of("test.yql", "some query")));
        given(datePpcProperty.get()).willReturn(LocalDate.of(2020, 1, 23));
        given(operationRepository.getForDate(any(), any())).willReturn(Collections.emptyList());

        job.execute();

        verify(operationRepository, atLeastOnce()).upsert(any(), collectionArgumentCaptor.capture());
        var operations = collectionArgumentCaptor.getAllValues().get(0);
        assertThat(operations).hasSize(1);
        assertThat(operations).extracting("operationId").contains("operation-id");
        assertThat(operations).extracting("name").contains("test.yql");
        assertThat(operations).extracting("status").contains(Operation.OperationStatus.IN_PROGRESS);
        assertThat(operations).extracting("date").contains(LocalDate.of(2020, 1, 23));
    }

    private enum TestFullWorkLastChangeType {
        LAST_CHANGE_NEW("Новое изменение"),
        LAST_CHANGE_OLD("Старое изменение");

        TestFullWorkLastChangeType(String description) {
        }
    }

    private enum TestFullWorkResultType {
        QUERY_SKIPPED("Запрос пропущен"),
        QUERY_STARTED("Запрос запущен");

        TestFullWorkResultType(String description) {
        }
    }

    static Object[] fullWorkTestData() {
        return new Object[][]{
                {
                    EnvironmentType.PRODUCTION, true,
                    TestFullWorkLastChangeType.LAST_CHANGE_NEW, TestFullWorkResultType.QUERY_STARTED
                },
                {
                    EnvironmentType.PRODUCTION, false,
                    TestFullWorkLastChangeType.LAST_CHANGE_NEW, TestFullWorkResultType.QUERY_STARTED
                },
                {
                    EnvironmentType.PRODUCTION, true,
                    TestFullWorkLastChangeType.LAST_CHANGE_OLD, TestFullWorkResultType.QUERY_STARTED
                },
                {
                    EnvironmentType.PRODUCTION, false,
                    TestFullWorkLastChangeType.LAST_CHANGE_OLD, TestFullWorkResultType.QUERY_STARTED
                },
                {
                    EnvironmentType.TESTING, true,
                    TestFullWorkLastChangeType.LAST_CHANGE_NEW, TestFullWorkResultType.QUERY_STARTED
                },
                {
                    EnvironmentType.TESTING, false,
                    TestFullWorkLastChangeType.LAST_CHANGE_NEW, TestFullWorkResultType.QUERY_SKIPPED
                },
                {
                    EnvironmentType.TESTING, true,
                    TestFullWorkLastChangeType.LAST_CHANGE_OLD, TestFullWorkResultType.QUERY_SKIPPED
                },
                {
                    EnvironmentType.TESTING, false,
                    TestFullWorkLastChangeType.LAST_CHANGE_OLD, TestFullWorkResultType.QUERY_SKIPPED
                },
                {
                    EnvironmentType.DEVELOPMENT, true,
                    TestFullWorkLastChangeType.LAST_CHANGE_NEW, TestFullWorkResultType.QUERY_STARTED
                },
                {
                    EnvironmentType.DEVELOPMENT, false,
                    TestFullWorkLastChangeType.LAST_CHANGE_NEW, TestFullWorkResultType.QUERY_STARTED
                },
                {
                    EnvironmentType.DEVELOPMENT, true,
                    TestFullWorkLastChangeType.LAST_CHANGE_OLD, TestFullWorkResultType.QUERY_STARTED
                },
                {
                    EnvironmentType.DEVELOPMENT, false,
                    TestFullWorkLastChangeType.LAST_CHANGE_OLD, TestFullWorkResultType.QUERY_STARTED
                }
        };
    }

    @ParameterizedTest(name = "Проверка флага full work (снепшот готов, операций еще не было запущено): " +
            "{0}, full_work={1}, {2}, {3}")
    @MethodSource("fullWorkTestData")
    @DisplayName("Проверка флага full work (снепшот готов, операций еще не было запущено)")
    void testFullWork(EnvironmentType environmentType, Boolean fullWorkValueBoolean,
                      TestFullWorkLastChangeType fullWorkLastChangeType, TestFullWorkResultType resultType
    ) {
        var path = YtPathUtil.generatePath(YT_HOME, relativePart(), "mysql-sync/snapshot--v.13--2020-01-23");
        var attributes = new SnapshotAttributes(path, true);
        given(snapshotUserAttributeService.getConvertedUserAttributes(any()))
                .willReturn(Collections.singletonList(attributes));
        given(ytOperator.yqlQueryBegin(any())).willReturn(new TestResultSetFuture());
        given(yqlClasspathObtainerService.obtainYqlQueriesFromClassPath())
                .willReturn(Collections.singletonList(Pair.of("test.yql", "some query")));
        given(datePpcProperty.get()).willReturn(LocalDate.of(2020, 1, 23));
        given(operationRepository.getForDate(any(), any())).willReturn(Collections.emptyList());

        final LocalDateTime fullWorkLastChange;
        if (fullWorkLastChangeType == TestFullWorkLastChangeType.LAST_CHANGE_NEW) {
            fullWorkLastChange = LocalDateTime.now();
        } else {
            fullWorkLastChange = LocalDateTime.now().minusHours(6);
        }

        given(ppcPropertiesSupport.getFullByNames(eq(Set.of(HOME_DIRECT_DB_FULL_WORK.getName()))))
                .willReturn(Map.of(HOME_DIRECT_DB_FULL_WORK.getName(),
                        new PpcPropertyData<>(String.valueOf(fullWorkValueBoolean), fullWorkLastChange)));

        given(fullWorkPpcProperty.getOrDefault(false)).willReturn(fullWorkValueBoolean);

        fullWorkPropObtainerService = new HomeDirectDbFullWorkPropObtainerService(
                ppcPropertiesSupport, environmentType);

        job = new HomeDirectDbOperationsJob(
                parametersSource,
                snapshotUserAttributeService,
                yqlClasspathObtainerService,
                ytProvider,
                shardHelper,
                operationRepository,
                ppcPropertiesSupport,
                homeDirectDbOperationsMetricProvider,
                homeDirectDbMetricsReporter,
                fullWorkPropObtainerService
        ) {
            @Override
            public String getParam() {
                return YtCluster.HAHN.name();
            }
        };

        job.execute();

        if (resultType == TestFullWorkResultType.QUERY_STARTED) {
            verify(operationRepository, atLeastOnce()).upsert(any(), collectionArgumentCaptor.capture());
            var operations = collectionArgumentCaptor.getAllValues().get(0);
            assertThat(operations).hasSize(1);
            assertThat(operations).extracting("operationId").contains("operation-id");
            assertThat(operations).extracting("name").contains("test.yql");
            assertThat(operations).extracting("status").contains(Operation.OperationStatus.IN_PROGRESS);
            assertThat(operations).extracting("date").contains(LocalDate.of(2020, 1, 23));
        } else {
            verify(ytOperator, never()).yqlQueryBegin(any());
        }
    }

    @Test
    @DisplayName("Снепшот готов, есть запущенная операция, проверяем статус, статус не меняется")
    void shouldCheckStatusWhenThereIsRunningOperations() throws SQLException {
        var path = YtPathUtil.generatePath(YT_HOME, relativePart(), "mysql-sync/snapshot--v.13--2020-01-23");
        var operation = new Operation(
                "test.yql",
                LocalDateTime.now(),
                LocalDateTime.now(),
                LocalDate.of(2020, 1, 23),
                "operation-id",
                Operation.OperationStatus.IN_PROGRESS,
                1,
                null
        );
        var attributes = new SnapshotAttributes(path, true);
        given(snapshotUserAttributeService.getConvertedUserAttributes(any()))
                .willReturn(Collections.singletonList(attributes));
        given(connection.restoreResultSetFuture(eq("operation-id"), any())).willReturn(new TestResultSetFuture());
        given(yqlClasspathObtainerService.obtainYqlQueriesFromClassPath())
                .willReturn(Collections.singletonList(Pair.of("test.yql", "some query")));
        given(datePpcProperty.get()).willReturn(LocalDate.of(2020, 1, 24));
        given(operationRepository.getForDate(any(), any())).willReturn(Collections.singletonList(operation));
        given(operationRepository.getNotFinished(any(), any())).willReturn(Collections.singletonList(operation));

        job.execute();

        verify(operationRepository, atLeastOnce()).upsert(any(), collectionArgumentCaptor.capture());
        var operations = collectionArgumentCaptor.getValue();
        assertThat(operations).hasSize(1);
        assertThat(operations).extracting("operationId").contains("operation-id");
        assertThat(operations).extracting("name").contains("test.yql");
        assertThat(operations).extracting("status").contains(Operation.OperationStatus.IN_PROGRESS);
        assertThat(operations).extracting("date").contains(LocalDate.of(2020, 1, 23));
    }

    @Test
    @DisplayName("Снепшот готов, есть запущенная операция, проверяем статус, меняется без ошибки")
    void shouldWriteNewStatusIfChanged() throws SQLException {
        var path = YtPathUtil.generatePath(YT_HOME, relativePart(), "mysql-sync/snapshot--v.13--2020-01-23");
        var operation = new Operation(
                "test.yql",
                LocalDateTime.now(),
                LocalDateTime.now(),
                LocalDate.of(2020, 1, 23),
                "operation-id",
                Operation.OperationStatus.IN_PROGRESS,
                1,
                null
        );
        var attributes = new SnapshotAttributes(path, true);
        given(snapshotUserAttributeService.getConvertedUserAttributes(any()))
                .willReturn(Collections.singletonList(attributes));
        given(connection.restoreResultSetFuture(eq("operation-id"), any())).willReturn(new TestDoneResultSetFuture());
        given(datePpcProperty.get()).willReturn(LocalDate.of(2020, 1, 24));
        given(operationRepository.getForDate(any(), any())).willReturn(Collections.singletonList(operation));
        given(operationRepository.getNotFinished(any(), any())).willReturn(Collections.singletonList(operation));

        job.execute();

        verify(operationRepository, atLeastOnce()).upsert(any(), collectionArgumentCaptor.capture());
        var operations = collectionArgumentCaptor.getValue();
        assertThat(operations).hasSize(1);
        assertThat(operations).extracting("operationId").contains("operation-id");
        assertThat(operations).extracting("name").contains("test.yql");
        assertThat(operations).extracting("status").contains(Operation.OperationStatus.DONE);
        assertThat(operations).extracting("date").contains(LocalDate.of(2020, 1, 23));
    }

    @Test
    @DisplayName("Снепшот готов, есть запущенная операция, проверяем статус, случилась ошибка")
    void shouldWriteErrorMessageIfErrorOccurred() throws SQLException {
        var path = YtPathUtil.generatePath(YT_HOME, relativePart(), "mysql-sync/snapshot--v.13--2020-01-23");
        var operation = new Operation(
                "test.yql",
                LocalDateTime.now(),
                LocalDateTime.now(),
                LocalDate.of(2020, 1, 23),
                "operation-id",
                Operation.OperationStatus.IN_PROGRESS,
                1,
                null
        );
        var attributes = new SnapshotAttributes(path, true);
        given(snapshotUserAttributeService.getConvertedUserAttributes(any()))
                .willReturn(Collections.singletonList(attributes));
        given(ytOperator.yqlQueryBegin(any())).willReturn(new TestResultSetFuture());
        given(connection.restoreResultSetFuture(eq("operation-id"), any())).willReturn(new TestErrorResultSetFuture());
        given(datePpcProperty.get()).willReturn(LocalDate.of(2020, 1, 24));
        given(operationRepository.getForDate(any(), any())).willReturn(Collections.singletonList(operation));
        given(operationRepository.getNotFinished(any(), any())).willReturn(Collections.singletonList(operation));

        var invocations = new AtomicInteger();

        doAnswer(invocation -> {
            if (invocations.get() == 0) {
                List<Operation> operations = invocation.getArgument(1);
                assertThat(operations).hasSize(1);
                assertThat(operations).extracting("operationId").contains("operation-id");
                assertThat(operations).extracting("name").contains("test.yql");
                assertThat(operations).extracting("status").contains(Operation.OperationStatus.ERROR);
                assertThat(operations).extracting("date").contains(LocalDate.of(2020, 1, 23));
                assertThat(operations)
                        .extracting("errorMessage")
                        .contains(new ExecutionException("Error", new RuntimeException()).toString());

            }
            invocations.getAndIncrement();
            return null;
        }).when(operationRepository).upsert(any(), any());

        job.execute();

    }

    @Test
    @DisplayName("Снепшот готов, есть ошибочная операция, надо перезапустить")
    void shouldRerunOperationIfError() throws SQLException {
        var path = YtPathUtil.generatePath(YT_HOME, relativePart(), "mysql-sync/snapshot--v.13--2020-01-23");
        var operation = new Operation(
                "test.yql",
                LocalDateTime.now(),
                LocalDateTime.now(),
                LocalDate.of(2020, 1, 23),
                "operation-id",
                Operation.OperationStatus.ERROR,
                1,
                null
        );
        var attributes = new SnapshotAttributes(path, true);
        given(snapshotUserAttributeService.getConvertedUserAttributes(any()))
                .willReturn(Collections.singletonList(attributes));
        given(connection.restoreResultSetFuture(eq("operation-id"), any()))
                .willReturn(new TestDoneResultSetFuture());
        given(ytOperator.yqlQueryBegin(any())).willReturn(new TestResultSetFuture2());
        given(datePpcProperty.get()).willReturn(LocalDate.of(2020, 1, 24));
        given(operationRepository.getForDate(any(), any())).willReturn(Collections.singletonList(operation));
        given(operationRepository.getNotFinished(any(), any())).willReturn(Collections.singletonList(operation));

        var invocations = new AtomicInteger();

        doAnswer(invocation -> {
            if (invocations.get() == 1) {
                // The first invocation will be on checking statuses (error should not be checked, however, anyway write
                // will be executed)
                List<Operation> operations = invocation.getArgument(1);
                assertThat(operations).hasSize(1);
                assertThat(operations).extracting("operationId").contains("operation-id2");
                assertThat(operations).extracting("name").contains("test.yql");
                assertThat(operations).extracting("status").contains(Operation.OperationStatus.IN_PROGRESS);
                assertThat(operations).extracting("attempts").contains(2);
                assertThat(operations).extracting("date").contains(LocalDate.of(2020, 1, 23));
            }

            invocations.incrementAndGet();
            return null;
        }).when(operationRepository).upsert(any(), any());

        job.execute();
    }

    @Test
    @DisplayName("Снепшот готов, есть ошибочная операция, но attempts = maxAttempts, ничего делать не надо")
    void shouldDoNothingIfAttemptsEqualsMaxAttempts() throws SQLException {
        var path = YtPathUtil.generatePath(YT_HOME, relativePart(), "mysql-sync/snapshot--v.13--2020-01-23");
        var operation = new Operation(
                "test.yql",
                LocalDateTime.now(),
                LocalDateTime.now(),
                LocalDate.of(2020, 1, 23),
                "operation-id",
                Operation.OperationStatus.ERROR,
                5,
                null
        );
        var attributes = new SnapshotAttributes(path, true);
        given(snapshotUserAttributeService.getConvertedUserAttributes(any()))
                .willReturn(Collections.singletonList(attributes));
        given(connection.restoreResultSetFuture(eq("operation-id"), any())).willReturn(new TestErrorResultSetFuture());
        given(datePpcProperty.get()).willReturn(LocalDate.of(2020, 1, 24));
        given(operationRepository.getForDate(any(), any())).willReturn(Collections.singletonList(operation));
        given(operationRepository.getNotFinished(any(), any())).willReturn(Collections.singletonList(operation));

        doAnswer(invocation -> {
            // Checking statuses and rerunning anyway always write data to snapshot cypress node meta
            List<Operation> operations = invocation.getArgument(1);
            assertThat(operations).isEqualTo(Collections.singletonList(operation));
            return null;
        }).when(operationRepository).upsert(any(), any());

        job.execute();
    }

    @Test
    @DisplayName("Несколько операций")
    void shouldCorrectlyWorkWithMultipleItems() throws SQLException {
        var path = YtPathUtil.generatePath(YT_HOME, relativePart(), "mysql-sync/snapshot--v.13--2020-01-23");

        var operation1 = new Operation(
                "test1.yql",
                LocalDateTime.now(),
                LocalDateTime.now(),
                LocalDate.of(2020, 1, 23),
                "operation-id",
                Operation.OperationStatus.ERROR,
                3,
                null
        );
        var operation2 = new Operation(
                "test2.yql",
                LocalDateTime.now(),
                LocalDateTime.now(),
                LocalDate.of(2020, 1, 23),
                "operation-id2",
                Operation.OperationStatus.IN_PROGRESS,
                1,
                null
        );

        var attributes = new SnapshotAttributes(path, true);

        given(snapshotUserAttributeService.getConvertedUserAttributes(any()))
                .willReturn(List.of(attributes));
        given(yqlClasspathObtainerService.obtainYqlQueriesFromClassPath())
                .willReturn(
                        List.of(
                                Pair.of("test1.yql", "some query"),
                                Pair.of("test2.yql", "some query")
                        )
                );

        given(connection.restoreResultSetFuture(eq("operation-id"), any())).willReturn(new TestResultSetFuture2());
        given(connection.restoreResultSetFuture(eq("operation-id2"), any())).willReturn(new TestDoneResultSetFuture());

        given(ytOperator.yqlQueryBegin(any())).willReturn(new TestResultSetFuture()).willReturn(new TestResultSetFuture2());
        given(datePpcProperty.get()).willReturn(LocalDate.of(2020, 1, 23));
        given(operationRepository.getForDate(any(), any())).willReturn(List.of(operation1, operation2));
        given(operationRepository.getNotFinished(any(), any())).willReturn(List.of(operation1, operation2));

        var invocations = new AtomicInteger();

        doAnswer(invocation -> {
            List<Operation> operations = invocation.getArgument(1);
            switch (invocations.get()) { // check the first and second invocations
                case 0:
                    // Run operations recently finished snapshot
                    assertThat(operations).hasSize(2);
                    assertThat(operations).extracting("operationId").contains("operation-id").contains("operation-id2");
                    assertThat(operations).extracting("name").contains("test1.yql").contains("test2.yql");
                    assertThat(operations).extracting("status").contains(Operation.OperationStatus.DONE);
                    assertThat(operations).extracting("attempts").contains(1);
                    assertThat(operations).extracting("date").contains(LocalDate.of(2020, 1, 23));
                    break;
                case 1:
                    // Re-run errored operations
                    assertThat(operations).hasSize(2);
                    assertThat(operations).extracting("operationId").contains("operation-id").contains("operation-id2");
                    assertThat(operations).extracting("name").contains("test1.yql").contains("test2.yql");
                    assertThat(operations).extracting("status").contains(Operation.OperationStatus.IN_PROGRESS);
                    assertThat(operations).extracting("attempts").contains(4);
                    assertThat(operations).extracting("date").contains(LocalDate.of(2020, 1, 23));
            }

            invocations.incrementAndGet();
            return null;
        }).when(operationRepository).upsert(any(), any());

        job.execute();
    }

    @Test
    @DisplayName("Нужно создать симлинк на последний успешно обработанный снепшот")
    void shouldCreateSymLinkWhenAllOperationsIsDone() {
        var path = YtPathUtil.generatePath(YT_HOME, relativePart(), "mysql-sync/snapshot--v.13--2020-01-23");
        var attributes = new SnapshotAttributes(path, false);

        var rawSymlinkPath = YtPathUtil.generatePath(YT_HOME, relativePart(), HOME_DB_PATH, "current");
        var rawHomeDbArchivePath = YtPathUtil.generatePath(YT_HOME, relativePart(), HOME_DB_PATH, "2020-01-03");
        var expectedSymlinkPath = YPath.simple(rawSymlinkPath);
        var expectedHomeDbArchivePath = YPath.simple(rawHomeDbArchivePath);

        given(snapshotUserAttributeService.getConvertedUserAttributes(any()))
                .willReturn(Collections.singletonList(attributes));
        given(operationRepository.getForDate(any(), any())).willReturn(Collections.emptyList());
        given(operationRepository.getLatestReadySnapshotDate(any())).willReturn(Optional.of(LocalDate.of(2020, 1, 3)));
        given(cypress.exists(eq(expectedHomeDbArchivePath))).willReturn(true);

        job.execute();

        verify(cypress, atLeastOnce()).link(eq(expectedHomeDbArchivePath), eq(expectedSymlinkPath), eq(true));
    }

    @Test
    @DisplayName("Нужно создать симлинк на последний успешно обработанный снепшот")
    void shouldNotCreateSymLinkWhenSnapshotDoesNotExists() {
        var path = YtPathUtil.generatePath(YT_HOME, relativePart(), "mysql-sync/snapshot--v.13--2020-01-23");
        var attributes = new SnapshotAttributes(path, false);

        var rawSymlinkPath = YtPathUtil.generatePath(YT_HOME, relativePart(), HOME_DB_PATH, "current");
        var rawHomeDbArchivePath = YtPathUtil.generatePath(YT_HOME, relativePart(), HOME_DB_PATH, "2020-01-03");
        var expectedSymlinkPath = YPath.simple(rawSymlinkPath);
        var expectedHomeDbArchivePath = YPath.simple(rawHomeDbArchivePath);

        given(snapshotUserAttributeService.getConvertedUserAttributes(any()))
                .willReturn(Collections.singletonList(attributes));
        given(operationRepository.getForDate(any(), any())).willReturn(Collections.emptyList());
        given(operationRepository.getLatestReadySnapshotDate(any())).willReturn(Optional.of(LocalDate.of(2020, 1, 3)));
        given(cypress.exists(eq(expectedHomeDbArchivePath))).willReturn(false);

        job.execute();

        verify(cypress, never()).link(eq(expectedHomeDbArchivePath), eq(expectedSymlinkPath), eq(true));
    }

    @Test
    @DisplayName("Не создаем симлинк, если нету ни одного обработанного снепшота")
    void shouldNotCreateSymlinkIfThereAreNoHandledSnapshot() {
        var path = YtPathUtil.generatePath(YT_HOME, relativePart(), "mysql-sync/snapshot--v.13--2020-01-23");
        var attributes = new SnapshotAttributes(path, false);
        given(snapshotUserAttributeService.getConvertedUserAttributes(any()))
                .willReturn(Collections.singletonList(attributes));
        given(operationRepository.getForDate(any(), any())).willReturn(Collections.emptyList());
        given(operationRepository.getLatestReadySnapshotDate(any())).willReturn(Optional.empty());

        job.execute();

        verify(cypress, never()).link(any(), any(), eq(true));
    }

    @Test
    @DisplayName("Если параметр date был установлен, надо сбросить по окончании работы джоба")
    void shouldRemoveDatePpcPropertyIfExists() {
        var path = YtPathUtil.generatePath(YT_HOME, relativePart(), "mysql-sync/snapshot--v.13--2020-01-23");
        var attributes = new SnapshotAttributes(path, false);
        given(snapshotUserAttributeService.getConvertedUserAttributes(any()))
                .willReturn(Collections.singletonList(attributes));
        given(operationRepository.getForDate(any(), any())).willReturn(Collections.emptyList());
        given(operationRepository.getLatestReadySnapshotDate(any())).willReturn(Optional.empty());
        given(datePpcProperty.get()).willReturn(LocalDate.of(2020, 1, 24));

        job.execute();

        verify(ppcPropertiesSupport).remove(eq(HOME_DIRECT_DB_DATE.getName()));
    }

    @Test
    @DisplayName("Если параметр date не задан, то не надо ничего делать")
    void shouldDoNothingIfDatePpcPropertyDoesNotExist() {
        var path = YtPathUtil.generatePath(YT_HOME, relativePart(), "mysql-sync/snapshot--v.13--2020-01-23");
        var attributes = new SnapshotAttributes(path, false);
        given(snapshotUserAttributeService.getConvertedUserAttributes(any()))
                .willReturn(Collections.singletonList(attributes));
        given(operationRepository.getForDate(any(), any())).willReturn(Collections.emptyList());
        given(operationRepository.getLatestReadySnapshotDate(any())).willReturn(Optional.empty());

        job.execute();

        verify(ppcPropertiesSupport, never()).remove(anyString());
    }

    @Test
    @DisplayName("Include выставлен, нужно выполнить только этот запрос")
    void shouldExecuteOnlyIncludedQueries() throws SQLException {
        var path = YtPathUtil.generatePath(YT_HOME, relativePart(), "mysql-sync/snapshot--v.13--2020-01-23");
        var attributes = new SnapshotAttributes(path, true);
        given(snapshotUserAttributeService.getConvertedUserAttributes(any()))
                .willReturn(Collections.singletonList(attributes));
        given(ytOperator.yqlQueryBegin(any())).willReturn(new TestResultSetFuture());
        given(yqlClasspathObtainerService.obtainYqlQueriesFromClassPath())
                .willReturn(
                        List.of(
                                Pair.of("test.yql", "some query"),
                                Pair.of("test1.yql", "some query")
                        )
                );
        given(datePpcProperty.get()).willReturn(LocalDate.of(2020, 1, 23));
        given(operationRepository.getForDate(any(), any())).willReturn(Collections.emptyList());
        given(includePpcProperty.get()).willReturn(Collections.singleton("test"));

        job.execute();

        verify(operationRepository, atLeastOnce()).upsert(any(), collectionArgumentCaptor.capture());
        var operations = collectionArgumentCaptor.getAllValues().get(0);
        assertThat(operations).hasSize(1);
        assertThat(operations).extracting("operationId").contains("operation-id");
        assertThat(operations).extracting("name").contains("test.yql");
        assertThat(operations).extracting("status").contains(Operation.OperationStatus.IN_PROGRESS);
        assertThat(operations).extracting("date").contains(LocalDate.of(2020, 1, 23));
    }

    @Test
    @DisplayName("Exclude выставлен, нужно выполнить только этот запрос")
    void shouldNotExecuteOnlyExcludedQueries() throws SQLException {
        var path = YtPathUtil.generatePath(YT_HOME, relativePart(), "mysql-sync/snapshot--v.13--2020-01-23");
        var attributes = new SnapshotAttributes(path, true);
        given(snapshotUserAttributeService.getConvertedUserAttributes(any()))
                .willReturn(Collections.singletonList(attributes));
        given(ytOperator.yqlQueryBegin(any())).willReturn(new TestResultSetFuture());
        given(yqlClasspathObtainerService.obtainYqlQueriesFromClassPath())
                .willReturn(
                        List.of(
                                Pair.of("test.yql", "some query"),
                                Pair.of("test1.yql", "some query")
                        )
                );
        given(datePpcProperty.get()).willReturn(LocalDate.of(2020, 1, 23));
        given(operationRepository.getForDate(any(), any())).willReturn(Collections.emptyList());
        given(excludePpcProperty.get()).willReturn(Collections.singleton("test"));

        job.execute();

        verify(operationRepository, atLeastOnce()).upsert(any(), collectionArgumentCaptor.capture());
        var operations = collectionArgumentCaptor.getAllValues().get(0);
        assertThat(operations).hasSize(1);
        assertThat(operations).extracting("operationId").contains("operation-id");
        assertThat(operations).extracting("name").contains("test1.yql");
        assertThat(operations).extracting("status").contains(Operation.OperationStatus.IN_PROGRESS);
        assertThat(operations).extracting("date").contains(LocalDate.of(2020, 1, 23));
    }

    @Test
    @DisplayName("Include и Exclude выставлены одновременно, так нельзя, ничего не делаем")
    void shouldDoNothingIfIncludeAndExcludeSetSimultaneously() throws SQLException {
        var path = YtPathUtil.generatePath(YT_HOME, relativePart(), "mysql-sync/snapshot--v.13--2020-01-23");
        var attributes = new SnapshotAttributes(path, true);
        given(snapshotUserAttributeService.getConvertedUserAttributes(any()))
                .willReturn(Collections.singletonList(attributes));
        given(ytOperator.yqlQueryBegin(any())).willReturn(new TestResultSetFuture());
        given(yqlClasspathObtainerService.obtainYqlQueriesFromClassPath())
                .willReturn(
                        List.of(
                                Pair.of("test.yql", "some query"),
                                Pair.of("test1.yql", "some query")
                        )
                );
        given(datePpcProperty.get()).willReturn(LocalDate.of(2020, 1, 23));
        given(operationRepository.getForDate(any(), any())).willReturn(Collections.emptyList());
        given(includePpcProperty.get()).willReturn(Collections.singleton("test"));
        given(excludePpcProperty.get()).willReturn(Collections.singleton("test"));

        assertThrows(IllegalArgumentException.class, () -> job.execute());
    }

}

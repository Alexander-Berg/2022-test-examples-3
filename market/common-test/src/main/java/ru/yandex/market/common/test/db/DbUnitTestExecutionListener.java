package ru.yandex.market.common.test.db;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.sql.DataSource;

import com.google.common.annotations.VisibleForTesting;
import org.apache.commons.lang.StringUtils;
import org.dbunit.dataset.IDataSet;
import org.springframework.jdbc.datasource.TransactionAwareDataSourceProxy;
import org.springframework.test.context.TestContext;
import org.springframework.test.context.support.AbstractTestExecutionListener;
import org.springframework.test.context.transaction.TransactionalTestExecutionListener;
import org.springframework.util.ReflectionUtils;

import ru.yandex.common.util.collections.Pair;
import ru.yandex.market.common.test.annotation.AnnotationUtils;

/**
 * @author Georgiy Klimov gaklimov@yandex-team.ru
 */
public class DbUnitTestExecutionListener extends AbstractTestExecutionListener {

    /**
     * Должны вызывать после коммита транзакции.
     * {@link TransactionalTestExecutionListener}.
     */
    public static final int LISTENER_ORDER = 3999;

    @Override
    public int getOrder() {
        return LISTENER_ORDER;
    }

    @Override
    public void beforeTestMethod(TestContext testContext) throws Exception {
        DbUnitDataBaseConfig dbUnitConfig = testContext.getTestClass().getAnnotation(DbUnitDataBaseConfig.class);
        Map<String, List<DbUnitDataSet>> groupedDataSets = collectAnnotations(testContext,
                DbUnitDataSet.class);
        Map<String, List<DbUnitTruncatePolicy>> groupedTruncatePolicies = collectAnnotations(testContext,
                DbUnitTruncatePolicy.class);
        Map<String, List<DbUnitRefreshMatViews>> refreshMatViewsMap = collectAnnotations(testContext,
                DbUnitRefreshMatViews.class);
        Map<DataSourceIdentity, Set<String>> dataSources = collectDataSources(
                testContext,
                Stream.of(
                        groupedDataSets.keySet(),
                        groupedTruncatePolicies.keySet(),
                        refreshMatViewsMap.keySet()
                ).flatMap(Collection::stream)
        );
        for (Map.Entry<DataSourceIdentity, Set<String>> dataSourceWithBeanNames : dataSources.entrySet()) {
            DataSource dataSource = dataSourceWithBeanNames.getKey().getDataSource();
            Set<String> dataSourceBeanNames = dataSourceWithBeanNames.getValue();
            List<DbUnitDataSet> dataSets = getAnnotations(groupedDataSets, dataSourceBeanNames);
            List<DbUnitTruncatePolicy> truncatePolicies = getAnnotations(groupedTruncatePolicies, dataSourceBeanNames);
            List<DbUnitRefreshMatViews> refreshMatViews = getAnnotations(refreshMatViewsMap, dataSourceBeanNames);
            String schema = getSchema(dataSets);
            DataSetProcessor dataSetProcessor = new DataSetProcessor(dataSource, schema, dbUnitConfig);

            if (shouldTruncateAllTables(dataSets, truncatePolicies)) {
                Set<String> ignoredTables = dataSets.stream()
                        .map(DbUnitDataSet::nonTruncatedTables)
                        .flatMap(Stream::of)
                        .map(StringUtils::trimToNull)
                        .filter(Objects::nonNull)
                        .collect(Collectors.toSet());

                Set<String> ignoredSequences = dataSets.stream()
                        .map(DbUnitDataSet::nonRestartedSequences)
                        .flatMap(Stream::of)
                        .map(StringUtils::trimToNull)
                        .filter(Objects::nonNull)
                        .collect(Collectors.toSet());

                dataSetProcessor.truncateAllTables(ignoredTables);
                dataSetProcessor.restartAllSequences(ignoredSequences);
            }

            DataSetLoader dataSetLoader = new DataSetLoader(testContext.getTestClass());
            IDataSet dataSet = dataSetLoader.getBeforeDataSets(dataSets);
            dataSetProcessor.applyDataSetOnSetUp(dataSet);

            if (!refreshMatViews.isEmpty()) {
                Set<String> ignoredMatViews = refreshMatViews.stream()
                        .map(DbUnitRefreshMatViews::ignore)
                        .flatMap(Stream::of)
                        .map(StringUtils::trimToNull)
                        .filter(Objects::nonNull)
                        .collect(Collectors.toSet());
                dataSetProcessor.refreshAllMatViews(ignoredMatViews);
            }

            // TODO сделать DatabaseTester тоже с множественными датасорцами
            initDbUnitTestersInTestClass(testContext, dataSource, schema);
        }
    }

    /**
     * Инициализирует поле типа {@link DbUnitTester} с аннотацией {@link InitByDbUnitListener}.
     */
    private static void initDbUnitTestersInTestClass(TestContext testContext, DataSource dataSource, String schema) {
        ReflectionUtils.doWithFields(testContext.getTestClass(),
                field -> {
                    try {
                        field.setAccessible(true);
                        field.set(testContext.getTestInstance(),
                                new DbUnitTester(testContext.getTestClass(), dataSource, schema));
                    } catch (IllegalAccessException e) {
                        throw new RuntimeException(String.format("Error initiating %s %s", DbUnitTester.class, field));
                    }
                },
                field -> field.getType() == DbUnitTester.class
                        && field.getAnnotation(InitByDbUnitListener.class) != null);
    }

    @Override
    public void afterTestMethod(TestContext testContext) throws Exception {
        //noinspection ThrowableResultOfMethodCallIgnored
        if (testContext.getTestException() != null) {
            // если тест выбросил исключение, проверять ничего не нужно
            return;
        }

        DbUnitDataBaseConfig dbUnitConfig = testContext.getTestClass().getAnnotation(DbUnitDataBaseConfig.class);
        Map<String, List<DbUnitDataSet>> groupedDataSets = collectAnnotations(testContext, DbUnitDataSet.class);
        Map<DataSourceIdentity, Set<String>> dataSources = collectDataSources(
                testContext,
                groupedDataSets.keySet().stream()
        );
        for (Map.Entry<DataSourceIdentity, Set<String>> dataSourceWithBeanNames : dataSources.entrySet()) {
            DataSource dataSource = dataSourceWithBeanNames.getKey().getDataSource();
            Set<String> dataSourceBeanNames = dataSourceWithBeanNames.getValue();
            List<DbUnitDataSet> dataSets = getAnnotations(groupedDataSets, dataSourceBeanNames);
            DataSetLoader dataSetLoader = new DataSetLoader(testContext.getTestClass());
            IDataSet expectedDataSet = dataSetLoader.getAfterDataSets(dataSets);
            if (expectedDataSet == null) {
                continue;
            }
            String schema = getSchema(dataSets);
            DataSetProcessor dataSetProcessor = new DataSetProcessor(dataSource, schema, dbUnitConfig);
            dataSetProcessor.assertDataSet(expectedDataSet);
        }
    }

    private static <T extends Annotation> List<T> getAnnotations(
            Map<String, ? extends Collection<T>> groupedDataSets,
            Collection<String> dataSourceBeanNames
    ) {
        return dataSourceBeanNames.stream()
                .map(groupedDataSets::get)
                .filter(Objects::nonNull)
                .flatMap(Collection::stream)
                .collect(Collectors.toList());
    }

    /**
     * на датасорсы могут ссылаться по разным алиасам в аннотациях
     *
     * @return ds > bean names & aliases
     */
    private static Map<DataSourceIdentity, Set<String>> collectDataSources(
            TestContext testContext,
            Stream<String> dataSourceBeanNames
    ) {
        return dataSourceBeanNames
                .distinct()
                .map(bn -> Pair.of(testContext.getApplicationContext().getBean(bn, DataSource.class), bn))
                .collect(Collectors.groupingBy(
                        p -> new DataSourceIdentity(p.getKey()),
                        LinkedHashMap::new,
                        Collectors.mapping(
                                Pair::getValue,
                                Collectors.toCollection(LinkedHashSet::new)
                        )
                ));
    }

    @VisibleForTesting
    static <T extends Annotation> Map<String, List<T>> collectAnnotations(
            TestContext testContext,
            Class<T> annotation
    ) {
        Set<T> classDataSets = AnnotationUtils.findAllAnnotations(testContext.getTestClass(), annotation);
        Set<T> methodDataSets = AnnotationUtils.findMethodAnnotations(testContext.getTestMethod(), annotation);
        return Stream.of(classDataSets, methodDataSets)
                .flatMap(Collection::stream)
                .collect(Collectors.groupingBy(v -> {
                            try {
                                Method method = v.getClass().getMethod("dataSource");
                                return (String) method.invoke(v);
                            } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
                                throw new RuntimeException(e);
                            }
                        },
                        LinkedHashMap::new,
                        Collectors.toList()
                ));
    }

    /**
     * @return непустое имя схемы либо {@code null}
     */
    @VisibleForTesting
    static String getSchema(List<? extends Annotation> dataSets) {
        return dataSets.stream()
                .map((Function<Annotation, String>) v -> {
                    try {
                        Method method = v.getClass().getMethod("schema");
                        return (String) method.invoke(v);
                    } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
                        throw new RuntimeException(e);
                    }
                })
                .map(StringUtils::trimToNull)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);
    }

    @VisibleForTesting
    static boolean shouldTruncateAllTables(List<DbUnitDataSet> dataSets, List<DbUnitTruncatePolicy> truncatePolicies) {
        Map<TruncateType, List<DbUnitTruncatePolicy>> truncateType = truncatePolicies.stream()
                .collect(Collectors.groupingBy(DbUnitTruncatePolicy::truncateType));
        if (truncateType.containsKey(TruncateType.TRUNCATE) && truncateType.containsKey(TruncateType.NOT_TRUNCATE)) {
            // Если на методах и классах по всей иерархии встретилось и TRUNCATE и NOT_TRUNCATE, то произошел конфликт
            throw new IllegalStateException("Test methods contains both TRUNCATE and NOT_TRUNCATE, choose only one");
        }

        // Если хотя бы где-то есть аннотация NOT_TRUNCATE, то сразу возвращаем ответ
        if (truncateType.containsKey(TruncateType.NOT_TRUNCATE)) {
            return false;
        }

        // помним что порядок от базового класса к тестовому методу
        // boolean truncate = (boolean) DbUnitDataSet.class.getMethod("truncateAllTables").getDefaultValue()
        boolean truncate = true; // дефолт как в аннотации
        for (DbUnitDataSet dataSet : dataSets) {
            truncate = truncate
                    ? dataSet.truncateAllTables()
                    : (dataSet.before().length > 0
                            || dataSet.nonTruncatedTables().length > 0
                            || dataSet.nonRestartedSequences().length > 0
                    );
        }
        return truncate;
    }

    /**
     * оборачиваем в TransactionAwareDataSourceProxy, чтобы соединения работали изнутри транзакции
     */
    private static DataSource wrapWithTx(DataSource dataSource) {
        return dataSource instanceof TransactionAwareDataSourceProxy
                ? dataSource
                : new TransactionAwareDataSourceProxy(dataSource);
    }

    /**
     * суррогатный ключ для использования в {@link LinkedHashMap},
     * тк в {@link java.util.IdentityHashMap} порядок не сохраняется
     */
    private static final class DataSourceIdentity {
        private final DataSource dataSource;

        private DataSourceIdentity(DataSource dataSource) {
            this.dataSource = dataSource;
        }

        DataSource getDataSource() {
            return wrapWithTx(dataSource);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof DataSourceIdentity)) {
                return false;
            }
            DataSourceIdentity that = (DataSourceIdentity) o;
            return dataSource == that.dataSource; // identity, not equals
        }

        @Override
        public int hashCode() {
            return dataSource.hashCode();
        }
    }
}

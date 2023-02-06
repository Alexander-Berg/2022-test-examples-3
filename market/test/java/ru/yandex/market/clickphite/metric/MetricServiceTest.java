package ru.yandex.market.clickphite.metric;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;

import ru.yandex.market.clickhouse.ClickhouseException;
import ru.yandex.market.clickhouse.ClickhouseTemplate;
import ru.yandex.market.clickhouse.HttpRowCallbackHandler;
import ru.yandex.market.clickhouse.ddl.ColumnType;
import ru.yandex.market.clickphite.config.WorkerPoolConfigurationService;
import ru.yandex.market.clickphite.whitelist.SplitTypeCache;
import ru.yandex.market.clickphite.whitelist.SplitWhitelistCache;
import ru.yandex.market.clickphite.whitelist.UndefinedSplitType;
import ru.yandex.market.health.configs.clickphite.ClickHouseTable;
import ru.yandex.market.health.configs.clickphite.MetricPeriod;
import ru.yandex.market.health.configs.clickphite.QueryBuilder;
import ru.yandex.market.health.configs.clickphite.TimeRange;
import ru.yandex.market.health.configs.clickphite.config.metric.MetricField;
import ru.yandex.market.health.configs.clickphite.metric.MetricContextGroup;
import ru.yandex.market.health.configs.clickphite.metric.MetricFieldImpl;
import ru.yandex.market.health.configs.clickphite.metric.MetricQueries;
import ru.yandex.market.health.configs.clickphite.mongo.SplitWhitelistAutoUpdateEntity;
import ru.yandex.market.health.configs.clickphite.mongo.SplitWhitelistEntity;
import ru.yandex.market.health.configs.clickphite.mongo.SplitWhitelistSettingsEntity;
import ru.yandex.market.health.configs.common.TableEntity;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class MetricServiceTest {

    private static final String CH_DATABASE = "some_database";
    private static final String CH_TABLE = "some_table";
    private static final String FULL_TABLE_NAME = CH_DATABASE + "." + CH_TABLE;
    private static final String SPLIT_NAME = "some_split_name";
    private static final String SPLIT_EXPRESSION = "some_split_expression";
    private static final String MANUAL_VALUE = "some_manual_value";
    private static final String CACHED_VALUE = "some_cached_value";
    private static final SplitWhitelistAutoUpdateEntity SPLIT_WHITELIST_AUTO_UPDATE_ENTITY =
        new SplitWhitelistAutoUpdateEntity(1, 2);
    private static final SplitWhitelistEntity.Id SPLIT_WHITELIST_ID = new SplitWhitelistEntity.Id(
        new TableEntity(
            CH_DATABASE,
            CH_TABLE
        ),
        SPLIT_EXPRESSION,
        SPLIT_WHITELIST_AUTO_UPDATE_ENTITY
    );
    private static final SplitTypeCache.Id SPLIT_ID = new SplitTypeCache.Id(
        FULL_TABLE_NAME,
        SPLIT_EXPRESSION
    );

    private MetricContextGroup metricContextGroup;
    private ClickhouseTemplate clickhouseTemplate;
    private WorkerPoolConfigurationService workerPoolConfigurationService;
    private SplitWhitelistCache splitWhitelistCache;
    private SplitTypeCache splitTypeCache;

    private MetricService metricService;

    //тесты белых списков

    @Before
    public void setup() {
        metricContextGroup = mock(MetricContextGroup.class);
        initMetricContextGroup(createMetricFieldList(Collections.singletonList(MANUAL_VALUE)));
        clickhouseTemplate = mock(ClickhouseTemplate.class);
        initClickhouseTemplate();
        workerPoolConfigurationService = mock(WorkerPoolConfigurationService.class);
        when(workerPoolConfigurationService.getPermits(Mockito.anyString())).thenReturn(100);
        splitWhitelistCache = mock(SplitWhitelistCache.class);
        initSplitWhitelistCache(Collections.singletonList(CACHED_VALUE));
        splitTypeCache = mock(SplitTypeCache.class);
        initSplitTypeCache(ColumnType.String);
        initMetricService();
    }

    @Test
    public void generalCase() throws Exception {
        updateMetricGroup();

        checkQuery("(some_split_name IN ('some_cached_value', 'some_manual_value'))");
        checkInvalidateWasNotCalled();
    }

    //тип сплита без скобок
    @Test
    public void noWrappingBrackets() throws Exception {
        initSplitTypeCache(ColumnType.Int64);

        updateMetricGroup();

        checkQuery("(some_split_name IN (some_cached_value, some_manual_value))");
        checkInvalidateWasNotCalled();
    }

    //закэшировано несколько значений, частично совпадают с введенными вручную
    @Test
    public void cachedValuesOverlapWithManual() throws Exception {
        initSplitWhitelistCache(Arrays.asList(CACHED_VALUE, MANUAL_VALUE));

        updateMetricGroup();

        checkQuery("(some_split_name IN ('some_cached_value', 'some_manual_value'))");
        checkInvalidateWasNotCalled();
    }

    //нет закэшированного значения
    @Test
    public void noCachedValue() throws Exception {
        initSplitWhitelistCache((SplitWhitelistEntity) null);

        updateMetricGroup();

        checkQuery("(some_split_name IN ('some_manual_value'))");
        checkInvalidateWasNotCalled();
    }

    //нет ручного значения, только закэшированное
    @Test
    public void moManualValue() throws Exception {
        initMetricContextGroup(createMetricFieldList(Collections.emptyList()));

        updateMetricGroup();

        checkQuery("(some_split_name IN ('some_cached_value'))");
        checkInvalidateWasNotCalled();
    }

    //нет ни ручного ни закэшированного значения
    @Test
    public void noValuesAtAll() throws Exception {
        initSplitWhitelistCache((SplitWhitelistEntity) null);
        initMetricContextGroup(createMetricFieldList(Collections.emptyList()));

        updateMetricGroup();

        checkQuery("1");
        checkInvalidateWasNotCalled();
    }

    //нет сплитов
    @Test
    public void noSplits() throws Exception {
        initMetricContextGroup(null);

        updateMetricGroup();

        checkQuery("1");
        checkInvalidateWasNotCalled();
    }

    @Test
    public void noSplitWhitelistParams() throws Exception {
        initMetricContextGroup(Collections.singletonList(new MetricFieldImpl(
            SPLIT_NAME,
            SPLIT_EXPRESSION,
            null
        )));

        updateMetricGroup();

        checkQuery("1");
        checkInvalidateWasNotCalled();
    }

    //не определяется тип сплита
    @Test
    public void splitTypeUndefined() throws Exception {
        initSplitTypeCache(new UndefinedSplitType());

        updateMetricGroup();

        checkQuery("1");
        checkInvalidateWasNotCalled();
    }

    //ошибка при выполнении запроса к КХ, говорящая об изменении типа сплита
    @Test
    public void badSplitType() {
        final ClickhouseException clickhouseException = createBadSplitTypeException();
        makeClickhouseTemplateQueryThrowsException(clickhouseException);

        assertThatThrownBy(this::updateMetricGroup).isEqualTo(clickhouseException);

        checkQuery("(some_split_name IN ('some_cached_value', 'some_manual_value'))");
        checkInvalidateCalled();
    }

    //несколько сплитов
    @Test
    public void complexCase() throws Exception {
        final String splitWithoutBracketsExpr = "split_without_brackets_expr";
        final String splitWithUndefinedTypeExpr = "split_with_undefined_type_expr";
        initMetricContextGroup(Arrays.asList(
            new MetricFieldImpl(
                SPLIT_NAME,
                SPLIT_EXPRESSION,
                new SplitWhitelistSettingsEntity(
                    Collections.singletonList(MANUAL_VALUE),
                    SPLIT_WHITELIST_AUTO_UPDATE_ENTITY,
                    true
                )
            ),
            new MetricFieldImpl(
                "split_without_brackets_name",
                splitWithoutBracketsExpr,
                new SplitWhitelistSettingsEntity(
                    Collections.singletonList("1"),
                    null,
                    true
                )
            ),
            new MetricFieldImpl(
                "split_without_values_at_all_name",
                "split_without_values_at_all_expr",
                new SplitWhitelistSettingsEntity(
                    Collections.emptyList(),
                    SPLIT_WHITELIST_AUTO_UPDATE_ENTITY,
                    true
                )
            ),
            new MetricFieldImpl(
                "split_with_undefined_type_name",
                splitWithUndefinedTypeExpr,
                new SplitWhitelistSettingsEntity(
                    Collections.singletonList("99"),
                    SPLIT_WHITELIST_AUTO_UPDATE_ENTITY,
                    true
                )
            ),
            new MetricFieldImpl(
                "split_without_settings_name",
                "split_without_settings_expr",
                null
            )
        ));
        when(splitTypeCache.get(new SplitTypeCache.Id(FULL_TABLE_NAME, splitWithoutBracketsExpr)))
            .thenReturn(ColumnType.Int32);
        when(splitTypeCache.get(new SplitTypeCache.Id(FULL_TABLE_NAME, splitWithUndefinedTypeExpr)))
            .thenThrow(new UndefinedSplitType());

        updateMetricGroup();

        checkQuery("(some_split_name IN ('some_cached_value', 'some_manual_value') " +
            "AND split_without_brackets_name IN (1))");
        checkInvalidateWasNotCalled();
    }

    private void initMetricContextGroup(List<MetricField> metricFieldList) {
        MetricQueries metricQueries = mock(MetricQueries.class);
        when(metricQueries.getMainQuery()).thenReturn(QueryBuilder.SPLIT_WHITELIST_VARIABLE);
        when(metricContextGroup.getQueries()).thenReturn(metricQueries);
        when(metricContextGroup.getTable()).thenReturn(new ClickHouseTable(CH_DATABASE, CH_TABLE));
        when(metricContextGroup.getPeriod()).thenReturn(MetricPeriod.ONE_MIN);
        when(metricContextGroup.getSplits()).then((Answer<List<MetricField>>) invocation -> metricFieldList);
    }

    private void initClickhouseTemplate() {
        when(clickhouseTemplate.queryForInt(MetricService.CURRENT_TIMESTAMP_QUERY)).then(
            (Answer<Integer>) invocation -> Math.toIntExact(new Date().getTime() / 1000));
    }

    private void checkQuery(String expectedQuery) {
        ArgumentCaptor<String> queryCaptor = ArgumentCaptor.forClass(String.class);
        verify(clickhouseTemplate).query(queryCaptor.capture(), any(HttpRowCallbackHandler.class), anyString());
        assertEquals(expectedQuery, queryCaptor.getValue());
    }

    private void makeClickhouseTemplateQueryThrowsException(ClickhouseException clickhouseException) {
        doThrow(clickhouseException).when(clickhouseTemplate).query(anyString(), any(HttpRowCallbackHandler.class),
            anyString());
    }

    private void initSplitWhitelistCache(List<String> values) {
        initSplitWhitelistCache(new SplitWhitelistEntity(
            null,
            null,
            values.stream().map(v -> new SplitWhitelistEntity.Element(v, null, 0)).collect(Collectors.toList())
        ));
    }

    private void initSplitWhitelistCache(SplitWhitelistEntity splitWhitelistEntity) {
        when(splitWhitelistCache.get(SPLIT_WHITELIST_ID)).thenReturn(splitWhitelistEntity);
    }

    private void initSplitTypeCache(ColumnType columnType) {
        when(splitTypeCache.get(SPLIT_ID)).thenReturn(columnType);
    }

    private void initSplitTypeCache(Throwable throwable) {
        when(splitTypeCache.get(SPLIT_ID)).thenThrow(throwable);
    }

    private ClickhouseException createBadSplitTypeException() {
        final String host = "localhost";
        final int port = 8123;
        final ClickhouseException exception = new ClickhouseException("Response code 400 response: Code: 53, e" +
            ".displayText() = DB::Exception: " +
            "Type mismatch in IN or VALUES section. Expected: String. Got: UInt64 " +
            "(version 19.17.4.11 (official build))", host, port);
        return new ClickhouseException("Exception executing sql:", exception, host, port);
    }

    private void initMetricService() {
        metricService = new MetricService();
        metricService.setWorkerPoolConfigurationService(workerPoolConfigurationService);
        metricService.setLightQueriesClickhouseTemplate(clickhouseTemplate);
        metricService.setSplitWhitelistCache(splitWhitelistCache);
        metricService.setSplitTypeCache(splitTypeCache);
        try {
            metricService.afterPropertiesSet();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void updateMetricGroup() throws Exception {
        metricService.updateMetricGroup(metricContextGroup, new TimeRange(0, 0), QueryWeight.LIGHT, "TEST_POOL");
    }

    private void checkInvalidateCalled() {
        verify(splitTypeCache).invalidate(SPLIT_ID);
    }

    private void checkInvalidateWasNotCalled() {
        verify(splitTypeCache, times(0)).invalidate(any());
    }

    private List<MetricField> createMetricFieldList(List<String> manualValues) {
        return Collections.singletonList(new MetricFieldImpl(
            SPLIT_NAME,
            SPLIT_EXPRESSION,
            new SplitWhitelistSettingsEntity(
                manualValues,
                SPLIT_WHITELIST_AUTO_UPDATE_ENTITY,
                true
            )
        ));
    }

}

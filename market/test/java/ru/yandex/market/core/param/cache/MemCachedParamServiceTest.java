package ru.yandex.market.core.param.cache;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.google.common.collect.ImmutableMap;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.mockito.Mockito;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.core.PreparedStatementSetter;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.support.SimpleTransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.common.cache.memcached.MemCachingService;
import ru.yandex.common.util.collections.MultiMap;
import ru.yandex.market.core.history.HistoryService;
import ru.yandex.market.core.param.db.ParamValueDao;
import ru.yandex.market.core.param.listener.ParamValueListener;
import ru.yandex.market.core.param.model.BooleanParamValue;
import ru.yandex.market.core.param.model.NumberParamValue;
import ru.yandex.market.core.param.model.ParamType;
import ru.yandex.market.core.param.model.ParamValue;
import ru.yandex.market.core.param.model.StringParamValue;
import ru.yandex.market.core.param.validator.ParamValueValidatorsRegistry;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.same;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

/**
 * Тесты для {@link MemCachedParamService}.
 */
class MemCachedParamServiceTest {

    private MemCachedParamService service;

    @BeforeEach
    void setUp() {
        service = new MemCachedParamService();
    }

    @Test
    void testGetParams() {
        long entityId = 1L;
        Set<ParamType> paramTypes = new HashSet<>();
        paramTypes.add(ParamType.RESERVED_1);

        BooleanParamValue value = new BooleanParamValue(ParamType.RESERVED_1, entityId, true);

        MemCachingService memCachingService = mock(MemCachingService.class);
        when(memCachingService.query(any(), eq(entityId))).thenReturn(Collections.singletonList(value));

        service.setMemCachingService(memCachingService);

        MultiMap<ParamType, ParamValue> params = service.getParams(entityId, paramTypes);
        assertEquals(params.entrySet().size(), 1);
        assertSame(params.get(ParamType.RESERVED_1).get(0), value);

        verify(memCachingService).query(any(), eq(entityId));
        verifyNoMoreInteractions(memCachingService);
    }

    /**
     * Тест для ситуации, когда из кеша возвращается номер типа параметра, которого нет в ParamType.
     */
    @Test
    void testGetParamsIncorrectNumber() {
        long entityId = 1L;
        Set<ParamType> paramTypes = new HashSet<>();
        paramTypes.add(ParamType.RESERVED_1);

        BooleanParamValue value = new BooleanParamValue(ParamType.RESERVED_1, entityId, true);

        MemCachingService memCachingService = mock(MemCachingService.class);
        when(memCachingService.query(any(), eq(entityId))).thenReturn(Collections.singletonList(value));

        service.setMemCachingService(memCachingService);

        MultiMap<ParamType, ParamValue> params = service.getParams(entityId, paramTypes);
        assertEquals(params.entrySet().size(), 1);
        assertSame(params.get(ParamType.RESERVED_1).get(0), value);

        verify(memCachingService).query(any(), eq(entityId));
        verifyNoMoreInteractions(memCachingService);
    }

    /**
     * Тест для ситуации, когда в список кешируемых параметров добавили новый, а в кеше его нет.
     */
    @Test
    void testGetParamsQuery() {
        long entityId = 1L;
        // Кешируем и запрашиваем два параметра
        Set<ParamType> paramTypes = new HashSet<>();
        paramTypes.add(ParamType.RESERVED_1);
        paramTypes.add(ParamType.PHONE_NUMBER);

        // Из кеша возвращается один параметр
        BooleanParamValue value = new BooleanParamValue(ParamType.RESERVED_1, entityId, true);
        List<BooleanParamValue> parameters = Collections.singletonList(value);

        MemCachingService memCachingService = mock(MemCachingService.class);
        when(memCachingService.query(any(), eq(entityId))).thenReturn(parameters);

        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        when(jdbcTemplate.query(any(PreparedStatementCreator.class), any(PreparedStatementSetter.class),
                any(ResultSetExtractor.class))).thenReturn(parameters);

        service.setMemCachingService(memCachingService);
        service.setParamValueDao(new ParamValueDao(jdbcTemplate));

        MultiMap<ParamType, ParamValue> params = service.getParams(entityId, paramTypes);
        assertEquals(params.entrySet().size(), 1);
        assertSame(params.get(ParamType.RESERVED_1).get(0), value);

        verify(memCachingService).query(any(), eq(entityId));
        verifyNoMoreInteractions(memCachingService);
    }

    @Test
    void testGetParamsForCollection() {
        long entityId = 1L;
        Set<Long> entityIds = new HashSet<>();
        entityIds.add(entityId);

        Set<ParamType> paramTypes = new HashSet<>();
        paramTypes.add(ParamType.RESERVED_1);

        BooleanParamValue value = new BooleanParamValue(ParamType.RESERVED_1, entityId, true);

        MemCachingService memCachingService = mock(MemCachingService.class);
        when(memCachingService.<List<ParamValue>, Long>queryBulk(any(), same(entityIds)))
                .thenReturn(ImmutableMap.of(1L, Collections.singletonList(value)));

        service.setMemCachingService(memCachingService);

        Map<Long, MultiMap<ParamType, ParamValue>> params = service.getParams(entityIds, paramTypes);
        assertEquals(params.entrySet().size(), 1);
        MultiMap<ParamType, ParamValue> paramValues = params.get(entityId);
        assertEquals(paramValues.entrySet().size(), 1);
        assertSame(paramValues.get(ParamType.RESERVED_1).get(0), value);

        verify(memCachingService).queryBulk(any(), same(entityIds));
        verifyNoMoreInteractions(memCachingService);
    }


    @Test
    void testGetParamsByEntityNameQuery() {
        long entityId = 1L;
        // Кешируем и запрашиваем два параметра
        Set<ParamType> paramTypes = new HashSet<>();
        paramTypes.add(ParamType.SHOP_NAME);
        paramTypes.add(ParamType.PHONE_NUMBER);
        paramTypes.add(ParamType.RESERVED_1);
        paramTypes.add(ParamType.TARIFF_MAX_DURATION);

        List<? extends ParamValue<? extends Serializable>> parameters = Arrays.asList(
                new StringParamValue(ParamType.SHOP_NAME, entityId, "domain"),
                new StringParamValue(ParamType.PHONE_NUMBER, entityId, "123"),
                new BooleanParamValue(ParamType.RESERVED_1, entityId, true),
                new NumberParamValue(ParamType.TARIFF_MAX_DURATION, entityId, 1L)
        );

        // Из кеша возвращается один параметр
        MemCachingService memCachingService = mock(MemCachingService.class);
        when(memCachingService.query(any(), eq(entityId))).thenReturn(parameters);

        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        when(jdbcTemplate.query(
                any(PreparedStatementCreator.class),
                any(PreparedStatementSetter.class),
                any(ResultSetExtractor.class)
        )).thenReturn(parameters);

        service.setMemCachingService(memCachingService);
        service.setParamValueDao(new ParamValueDao(jdbcTemplate));

        MultiMap<ParamType, ParamValue> params = service.getParams(entityId, paramTypes);
        assertThat(params.entrySet().stream().map(Map.Entry::getKey).collect(Collectors.toList()),
                Matchers.containsInAnyOrder(
                        ParamType.SHOP_NAME,
                        ParamType.PHONE_NUMBER,
                        ParamType.RESERVED_1,
                        ParamType.TARIFF_MAX_DURATION));

        verify(memCachingService).query(any(), eq(entityId));
        verifyNoMoreInteractions(memCachingService);
    }

    @Test
    void testCacheCleanBeforeListenerOnParamUpdate() {
        long entityId = 1L;
        long actionId = 5L;

        MemCachingService memCachingService = mock(MemCachingService.class);
        ParamValueDao paramValueDao = mock(ParamValueDao.class);
        ParamValueListener paramValueListener = mock(ParamValueListener.class);
        ParamValueValidatorsRegistry paramValueValidatorsRegistry = mock(ParamValueValidatorsRegistry.class);
        HistoryService historyService = mock(HistoryService.class);

        when(historyService.buildUpdateRecord(any())).thenReturn(mock(HistoryService.Record.Builder.class));
        when(paramValueDao.getParam(any(), anyLong())).thenReturn(new BooleanParamValue(ParamType.RESERVED_1,
                entityId, false));

        service.setMemCachingService(memCachingService);
        service.setHistoryService(historyService);
        service.setTransactionTemplate(new TransactionTemplate() {
            @Override
            public <T> T execute(TransactionCallback<T> action) throws TransactionException {
                return action.doInTransaction(new SimpleTransactionStatus());
            }
        });
        service.setParamValueValidatorsRegistry(paramValueValidatorsRegistry);
        service.setParamValueDao(paramValueDao);
        service.addListener(paramValueListener);

        service.setParam(new BooleanParamValue(ParamType.RESERVED_1, entityId, true), actionId);

        InOrder inOrder = Mockito.inOrder(memCachingService, paramValueListener);
        inOrder.verify(memCachingService).clean(any(), anyLong());
        inOrder.verify(paramValueListener).onValueChanged(any(), any(), anyLong());
    }

    @Test
    void testCacheCleanBeforeListenerOnParamCreate() {
        long entityId = 1L;
        long actionId = 5L;

        MemCachingService memCachingService = mock(MemCachingService.class);
        ParamValueDao paramValueDao = mock(ParamValueDao.class);
        ParamValueListener paramValueListener = mock(ParamValueListener.class);
        ParamValueValidatorsRegistry paramValueValidatorsRegistry = mock(ParamValueValidatorsRegistry.class);
        HistoryService historyService = mock(HistoryService.class);

        when(historyService.buildCreateRecord(any())).thenReturn(mock(HistoryService.Record.Builder.class));
        when(paramValueDao.getParam(any(), anyLong())).thenReturn(null);

        service.setMemCachingService(memCachingService);
        service.setHistoryService(historyService);
        service.setTransactionTemplate(new TransactionTemplate() {
            @Override
            public <T> T execute(TransactionCallback<T> action) throws TransactionException {
                return action.doInTransaction(new SimpleTransactionStatus());
            }
        });
        service.setParamValueValidatorsRegistry(paramValueValidatorsRegistry);
        service.setParamValueDao(paramValueDao);
        service.addListener(paramValueListener);

        service.setParam(new BooleanParamValue(ParamType.RESERVED_1, entityId, true), actionId);

        InOrder inOrder = Mockito.inOrder(memCachingService, paramValueListener);
        inOrder.verify(memCachingService).clean(any(), anyLong());
        inOrder.verify(paramValueListener).onValueChanged(any(), any(), anyLong());
    }
}

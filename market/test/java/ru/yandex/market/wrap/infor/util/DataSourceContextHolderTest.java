package ru.yandex.market.wrap.infor.util;

import java.util.Map;

import javax.sql.DataSource;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.testcontainers.shaded.com.google.common.collect.ImmutableMap;

import ru.yandex.market.fulfillment.wrap.core.processing.validation.TokenContextHolder;
import ru.yandex.market.logistic.api.model.fulfillment.exception.FulfillmentApiException;
import ru.yandex.market.wrap.infor.configuration.DataSourceContextHolder;
import ru.yandex.market.wrap.infor.configuration.WmsDataSourceTypeContextHolder;
import ru.yandex.market.wrap.infor.configuration.enums.DataSourceType;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DataSourceContextHolderTest {

    private static final String TOKEN = "TOKEN";

    private TokenContextHolder tokenContextHolder;
    private WmsDataSourceTypeContextHolder wmsDataSourceTypeContextHolder;
    private DataSource readWriteDataSource;
    private DataSource readOnlyDataSource;
    private DataSourceContextHolder<DataSource> contextHolder;

    @BeforeEach
    public void init() {
        readWriteDataSource = Mockito.mock(DataSource.class);
        readOnlyDataSource = Mockito.mock(DataSource.class);
        tokenContextHolder = new TokenContextHolder();
        wmsDataSourceTypeContextHolder = new WmsDataSourceTypeContextHolder();

        Map<DataSourceType, DataSource> mapping = ImmutableMap.of(DataSourceType.READ_WRITE, readWriteDataSource,
            DataSourceType.READ_ONLY, readOnlyDataSource);
        Map<String, Map<DataSourceType, DataSource>> tokenMapping = ImmutableMap.of(TOKEN, mapping);
        contextHolder = new DataSourceContextHolder<>(tokenMapping, tokenContextHolder, wmsDataSourceTypeContextHolder);

        tokenContextHolder.setToken(TOKEN);
        wmsDataSourceTypeContextHolder.setDataSourceType(DataSourceType.READ_WRITE);
    }

    @Test
    public void exceptionForIncorrectToken() {
        tokenContextHolder.setToken("INCORRECT_TOKEN");
        assertThrows(FulfillmentApiException.class, () -> contextHolder.getContext());
    }

    @Test
    public void returnReadWriteIfRequired() {
        DataSource result = contextHolder.getContext();
        assertEquals(readWriteDataSource, result);
    }

    @Test
    public void returnReadOnlyIfRequired() {
        wmsDataSourceTypeContextHolder.setDataSourceType(DataSourceType.READ_ONLY);
        DataSource result = contextHolder.getContext();
        assertEquals(readOnlyDataSource, result);
    }

    @Test
    public void returnReadWriteIfThereAreNotRequiredReadOnly() {
        Map<DataSourceType, DataSource> mapping = ImmutableMap.of(DataSourceType.READ_WRITE, readWriteDataSource);
        Map<String, Map<DataSourceType, DataSource>> tokenMapping = ImmutableMap.of(TOKEN, mapping);
        contextHolder = new DataSourceContextHolder<>(tokenMapping, tokenContextHolder, wmsDataSourceTypeContextHolder);

        wmsDataSourceTypeContextHolder.setDataSourceType(DataSourceType.READ_ONLY);
        DataSource result = contextHolder.getContext();
        assertEquals(readWriteDataSource, result);
    }
}

package ru.yandex.market.tsup.core.cache.sly_cacher.service;

import lombok.Data;
import org.springframework.stereotype.Component;

import ru.yandex.market.tpl.common.data_provider.filter.ProviderFilter;
import ru.yandex.market.tpl.common.data_provider.provider.DataProvider;
import ru.yandex.market.tsup.core.cache.sly_cacher.SlyCached;
import ru.yandex.market.tsup.core.cache.sly_cacher.invalidation.InvalidationStrategy;

@Component
@Data
public class TestInvalidationStrategy implements InvalidationStrategy {
    private boolean supportsExpiration = true;

    private boolean executed = false;
    private DataProvider<?, ?> dataProvider;
    private Class<? extends ProviderFilter> filterClass;
    private SlyCached annotation;

    public void reset() {
        executed = false;
        dataProvider = null;
        filterClass = null;
        annotation = null;
    }

    @Override
    public boolean supportsExpiration() {
        return supportsExpiration;
    }

    @Override
    public <T extends ProviderFilter> void processLogRecords(
        DataProvider<?, T> dataProvider,
        Class<T> filterClass,
        SlyCached annotation
    ) {
        executed = true;
        this.dataProvider = dataProvider;
        this.filterClass = filterClass;
        this.annotation = annotation;
    }
}

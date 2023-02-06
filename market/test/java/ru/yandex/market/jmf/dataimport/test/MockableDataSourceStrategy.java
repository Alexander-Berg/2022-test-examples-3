package ru.yandex.market.jmf.dataimport.test;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;

import ru.yandex.market.jmf.dataimport.conf.datasource.DataSourceConf;
import ru.yandex.market.jmf.dataimport.datasource.AbstractDataSourceStrategy;

/**
 * Позволяет добавлять моки на data source
 *
 * @param <T>
 */
@Order(Ordered.HIGHEST_PRECEDENCE)
public abstract class MockableDataSourceStrategy<T extends DataSourceConf>
        extends AbstractDataSourceStrategy<T> {
    MockableDataSourceStrategy(Class<T> type) {
        super(type);
    }
}

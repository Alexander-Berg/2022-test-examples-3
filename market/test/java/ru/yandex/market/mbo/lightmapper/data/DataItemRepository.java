package ru.yandex.market.mbo.lightmapper.data;

import java.util.Optional;

import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.mbo.lightmapper.CompositeGenericMapper;
import ru.yandex.market.mbo.lightmapper.CompositeMapper;
import ru.yandex.market.mbo.lightmapper.GenericMapperRepositoryImpl;
import ru.yandex.market.mbo.lightmapper.InstantMapper;
import ru.yandex.market.mbo.lightmapper.metrics.RepositoryMetricsCollector;

public class DataItemRepository extends GenericMapperRepositoryImpl<DataItem, Integer> {
    public static final String TABLE_NAME = "test.data_item";

    private static String metricsContext;
    private static RepositoryMetricsCollector collector = new RepositoryMetricsCollector(TABLE_NAME,
            () -> Optional.ofNullable(metricsContext));

    public DataItemRepository(NamedParameterJdbcTemplate jdbcTemplate, TransactionTemplate transactionTemplate) {
        super(new CompositeGenericMapper<>(CompositeMapper.builder(DataItem::new)
                        .map("id", DataItem::getId, DataItem::setId).mark(PRIMARY_KEY, GENERATED)
                        .map("name", DataItem::getName, DataItem::setName)
                        .map("version",
                                DataItem::getVersion, DataItem::setVersion, new InstantMapper()).mark(VERSION_INSTANT)
                        .build()),
                jdbcTemplate, transactionTemplate, TABLE_NAME, collector);
    }

    public static String getMetricsContext() {
        return metricsContext;
    }

    public static void setMetricsContext(String metricsContext) {
        DataItemRepository.metricsContext = metricsContext;
    }
}

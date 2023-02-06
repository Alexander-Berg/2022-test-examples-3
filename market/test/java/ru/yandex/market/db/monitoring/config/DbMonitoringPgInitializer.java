package ru.yandex.market.db.monitoring.config;

import java.util.Map;

import javax.annotation.Nonnull;

import ru.yandex.market.mboc.common.utils.PGaaSZonkyInitializer;

public class DbMonitoringPgInitializer extends PGaaSZonkyInitializer {
    @Override
    public void exportProperties(@Nonnull ConnectionParameters parameters, @Nonnull Map<String, Object> properties) {
        super.exportProperties(parameters, properties);
        properties.put("sql.liquibase.tables.schema", "public");
        properties.put("sql.port", String.valueOf(parameters.getPort()));
    }
}

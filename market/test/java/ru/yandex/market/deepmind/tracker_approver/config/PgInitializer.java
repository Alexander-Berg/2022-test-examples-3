package ru.yandex.market.deepmind.tracker_approver.config;

import java.util.Map;

import ru.yandex.market.mboc.common.utils.PGaaSZonkyInitializer;

public class PgInitializer extends PGaaSZonkyInitializer {
    @Override
    public void exportProperties(ConnectionParameters parameters, Map<String, Object> properties) {
        super.exportProperties(parameters, properties);
        properties.put("sql.port", String.valueOf(parameters.getPort()));
    }
}

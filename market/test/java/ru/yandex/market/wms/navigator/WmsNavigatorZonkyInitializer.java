package ru.yandex.market.wms.navigator;

import java.util.Map;

import ru.yandex.market.mboc.common.utils.PGaaSZonkyInitializer;

public class WmsNavigatorZonkyInitializer extends PGaaSZonkyInitializer {

    @Override
    protected boolean reusePg() {
        return false;
    }

    @Override
    public void exportProperties(ConnectionParameters parameters, Map<String, Object> properties) {
        super.exportProperties(parameters, properties);
        properties.put("postgres.navigator.datasource.url", parameters.getUrl());
        properties.put("postgres.navigator.datasource.username", parameters.getUserName());
        properties.put("postgres.navigator.datasource.password", parameters.getPassword());

        properties.put("spring.pg-sorter-datasource.connection.url", parameters.getUrl());
        properties.put("spring.pg-sorter-datasource.connection.username", parameters.getUserName());
        properties.put("spring.pg-sorter-datasource.connection.password", parameters.getPassword());
    }
}

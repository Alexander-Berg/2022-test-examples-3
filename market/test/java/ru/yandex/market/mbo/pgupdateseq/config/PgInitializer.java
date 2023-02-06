package ru.yandex.market.mbo.pgupdateseq.config;

import java.util.Map;

import javax.annotation.Nonnull;

import ru.yandex.market.mboc.common.utils.PGaaSZonkyInitializer;


/**
 * @author yuramalinov
 * @created 02.08.2019
 */
public class PgInitializer extends PGaaSZonkyInitializer {
    @Override
    public void exportProperties(@Nonnull ConnectionParameters parameters, @Nonnull Map<String, Object> properties) {
        super.exportProperties(parameters, properties);
        properties.put("sql.port", String.valueOf(parameters.getPort()));
    }
}

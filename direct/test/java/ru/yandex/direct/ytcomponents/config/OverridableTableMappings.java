package ru.yandex.direct.ytcomponents.config;

import java.util.HashMap;
import java.util.Map;

import javax.annotation.ParametersAreNonnullByDefault;

import org.jooq.Table;

import ru.yandex.direct.ytwrapper.dynamic.TableMappings;

/**
 * TableMappings на базе маппинга, генерируемого из конфигурации Директа,
 * С возможностью переопределить расположение конкретных таблиц.
 * Не immutable.
 */
@ParametersAreNonnullByDefault
public class OverridableTableMappings implements TableMappings {
    private final DirectDynTablesMapping directDynTablesMapping;
    private final Map<Table<?>, String> overrides;

    public OverridableTableMappings(DirectDynTablesMapping directDynTablesMapping) {
        this.directDynTablesMapping = directDynTablesMapping;
        overrides = new HashMap<>();
    }

    @Override
    public Map<Table<?>, String> getTableMappings() {
        Map<Table<?>, String> result = new HashMap<>(directDynTablesMapping.getTableMappings());
        result.putAll(overrides);
        return result;
    }

    /**
     * Переопределить путь к таблице на произвольный
     */
    public void addOverride(Table<?> table, String path) {
        overrides.put(table, path);
    }
}

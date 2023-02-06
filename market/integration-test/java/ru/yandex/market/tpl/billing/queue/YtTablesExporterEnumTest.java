package ru.yandex.market.tpl.billing.queue;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.tpl.billing.AbstractFunctionalTest;
import ru.yandex.market.tpl.billing.model.yt.YtTablesExporterEnum;
import ru.yandex.market.tpl.billing.service.yt.exports.YtBillingExportService;

import static org.hamcrest.Matchers.hasSize;

public class YtTablesExporterEnumTest extends AbstractFunctionalTest {

    @Autowired
    Map<String, YtBillingExportService> exportersByTables;

    @Test
    @DisplayName("Enum содержит сервисы для всех выгрузок")
    public void enumContainsAllExporters() {
        Set<String> exportersInEnum = Arrays.stream(YtTablesExporterEnum.values())
                .map(YtTablesExporterEnum::getExportServiceName).collect(Collectors.toSet());

        List<String> notConfiguredExporters = exportersByTables.keySet().stream()
                .filter(tableName -> !exportersInEnum.contains(tableName))
                .collect(Collectors.toList());

        MatcherAssert.assertThat(
                "Exporters for tables " + notConfiguredExporters + " don't specified at YtTableExporterEnum " +
                        "or IntegrationTestConfig",
                notConfiguredExporters,
                hasSize(0)
        );
    }

    @Test
    @DisplayName("Enum не содержит сервисов для выгрузок, которых нет")
    public void enumContainsOnlyConfiguredExporters() {
        Set<String> configuredExporters = exportersByTables.keySet();

        List<YtTablesExporterEnum> surplusExporters = Arrays.stream(YtTablesExporterEnum.values())
                .filter(exporter -> !configuredExporters.contains(exporter.getExportServiceName()))
                .collect(Collectors.toList());

        MatcherAssert.assertThat(
                "Enam contains surplus exporters: " + surplusExporters,
                surplusExporters,
                hasSize(0)
        );
    }

}

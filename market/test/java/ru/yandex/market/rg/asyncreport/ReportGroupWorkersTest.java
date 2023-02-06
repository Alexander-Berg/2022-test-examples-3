package ru.yandex.market.rg.asyncreport;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.core.asyncreport.model.ReportsType;
import ru.yandex.market.core.asyncreport.model.ReportsTypeGroup;
import ru.yandex.market.core.asyncreport.worker.ReportsExecutorService;
import ru.yandex.market.core.asyncreport.worker.ReportsExecutorSettings;
import ru.yandex.market.rg.config.FunctionalTest;
import ru.yandex.market.rg.config.ReportsConfig;

/**
 * Тесты для {@link ReportsConfig}.
 *
 * @author Kirill Batalin (batalin@yandex-team.ru)
 */
class ReportGroupWorkersTest extends FunctionalTest {

    @Autowired
    private List<ReportsExecutorService<ReportsType, ReportsTypeGroup>> executorServices;

    @Test
    @DisplayName("Для каждой группы отчета настроен пулл воркеров")
    void testReportGroupWorkers() {
        Set<ReportsTypeGroup> configuredReportTypes = executorServices.stream()
                .map(ReportsExecutorService::getSettings)
                .map(ReportsExecutorSettings::getReportsGroup)
                .collect(Collectors.toSet());

        Set<ReportsTypeGroup> allGroups = Arrays.stream(ReportsType.values())
                .map(ReportsType::getGroup)
                .collect(Collectors.toSet());

        Assertions.assertThat(configuredReportTypes)
                .containsExactlyInAnyOrderElementsOf(allGroups);
    }
}

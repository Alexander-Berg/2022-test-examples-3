package ru.yandex.market.billing.tasks.delivery.direction.file;

import java.text.MessageFormat;
import java.util.List;
import java.util.Map;

import com.google.common.collect.ImmutableMap;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.billing.FunctionalTest;
import ru.yandex.market.billing.tasks.delivery.direction.DeliveryDirectionSchedule;
import ru.yandex.market.common.test.db.DbUnitDataSet;

import static org.hamcrest.CoreMatchers.describedAs;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.not;

/**
 * Тест для {@link DeliveryDirectionSchedulesProviderFromFile}.
 *
 * @author ivmelnik
 * @since 26.09.18
 */
@DbUnitDataSet(before = "all-regions.csv")
class DeliveryDirectionSchedulesProviderFromFileTest extends FunctionalTest {

    private static final long PEK_ID = 9;
    private static final long MAXIPOST_ID = 50;
    private static final long CDEK_ID = 51;
    private static final long STRIZH_ID = 48;
    private static final long DPD_ID = 1003937;

    private static final int EXPECTED_TOTAL_SCHEDULES_COUNT = 1026;
    private static final Map<Long, Long> EXPECTED_DS_SCHEDULES_COUNT = ImmutableMap.of(
            PEK_ID, 2L,
            MAXIPOST_ID, 62L,
            STRIZH_ID, 62L,
            CDEK_ID, 450L,
            DPD_ID, 450L
    );

    @Autowired
    private DeliveryDirectionSchedulesProviderFromFile providerFromFile;

    /**
     * Тест предназначен только для ручного запуска.
     * Перед запуском необходимо сдампить дерево регионов в файл all-regions.csv
     * Дерево можно взять как view из первых 4х столбцов таблицы shops_web.regions
     */
    @Disabled
    @Test
    void provideSchedules() {
        List<DeliveryDirectionSchedule> schedules = providerFromFile.provideSchedules();
        assertThat(schedules, not(empty()));
        assertThat(schedules.size(), describedAs(MessageFormat.format(
                "Количество ожидаемых записей зафиксировано для текущего варианта файла и равно {0}.\n" +
                        "При изменениях:\n" +
                        "Проверить, что ошибок нет в логах.\n" +
                        "Проверить, что в бд-файле с регионами все обновлено.", EXPECTED_TOTAL_SCHEDULES_COUNT),
                Matchers.equalTo(EXPECTED_TOTAL_SCHEDULES_COUNT)));
        checkCountByDeliveryServices(schedules);
        // TODO check no errors from ErrorCollector MBI-31776
    }

    private void checkCountByDeliveryServices(List<DeliveryDirectionSchedule> schedules) {
        EXPECTED_DS_SCHEDULES_COUNT.forEach((ds_id, count) -> checkCountByDeliveryService(ds_id, count, schedules));
    }

    private void checkCountByDeliveryService(Long deliveryServiceId, Long expectedCount, List<DeliveryDirectionSchedule> schedules) {
        long actualCount = schedules.stream()
                .filter(s -> s.getDeliveryServiceId() == deliveryServiceId)
                .count();
        assertThat(actualCount, describedAs(MessageFormat.format(
                "Количество ожидаемых записей для службы {0} зафиксировано для текущего варианта файла и равно {1}."
                , deliveryServiceId, expectedCount),
                Matchers.equalTo(expectedCount)));
    }

}

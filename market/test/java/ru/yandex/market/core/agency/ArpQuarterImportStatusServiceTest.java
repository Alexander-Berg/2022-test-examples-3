package ru.yandex.market.core.agency;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.FunctionalTest;
import ru.yandex.market.core.agency.program.ProgramRewardType;
import ru.yandex.market.core.agency.program.quarter.ArpCalculatingLevel;
import ru.yandex.market.core.agency.program.quarter.ArpQuarterImportStatusService;
import ru.yandex.market.core.agency.program.quarter.Quarter;

/**
 * Тесты для {@link ArpQuarterImportStatusService}.
 *
 * @author Kirill Batalin (batalin@yandex-team.ru)
 */
class ArpQuarterImportStatusServiceTest extends FunctionalTest {

    @Autowired
    private ArpQuarterImportStatusService arpQuarterImportStatusService;

    @Test
    @DisplayName("В статусе не учитываются кварталы раньше начала премии")
    @DbUnitDataSet(before = "csv/ArpQuarterImportStatusService.testGetAllStatusQuarters.before.csv")
    void testGetAllStatusQuarters() {
        final List<Quarter> expected = List.of(
                Quarter.of(2020, 1),
                Quarter.of(2020, 2),
                Quarter.of(2020, 3)
        );
        final List<Quarter> actual = arpQuarterImportStatusService.getAllStatusQuarters();
        Assertions.assertEquals(expected, actual);
    }

    @Test
    @DisplayName("Удаление статуса импорта")
    @DbUnitDataSet(
            before = "csv/ArpQuarterImportStatusService.testDeleteARP.before.csv",
            after = "csv/ArpQuarterImportStatusService.testDeleteARP.after.csv"
    )
    void testDeleteARP() {
        arpQuarterImportStatusService.deleteStatus(ProgramRewardType.ACTIVITY_QUALITY, ArpCalculatingLevel.DATASOURCE, Quarter.of(2019, 1));

    }

    @Test
    @DisplayName("Обновление даты начала квартала")
    @DbUnitDataSet(
            before = "csv/ArpQuarterImportStatusService.testUpdateARPStartDate.before.csv",
            after = "csv/ArpQuarterImportStatusService.testUpdateARPStartDate.after.csv"
    )
    void testUpdateARPStartDate() {
        arpQuarterImportStatusService.updateARPStartDate("auction", Quarter.of(2021, 2));
    }

    @Test
    @DisplayName("Получить закрытые кварталы")
    @DbUnitDataSet(before = "csv/ArpQuarterImportStatusService.testGetClosedQuarters.before.csv")
    void testGetClosedQuarters() {
        final Map<Quarter, Boolean> expected = Map.of(
                Quarter.of(2017, 1), true,
                Quarter.of(2017, 2), false,
                Quarter.of(2017, 3), false,
                Quarter.of(2017, 4), true,
                Quarter.of(2018, 1), true,
                Quarter.of(2018, 2), false,
                Quarter.of(2018, 3), false
        );
        final Map<Quarter, Boolean> actual = arpQuarterImportStatusService.getClosedQuarters();

        Assertions.assertEquals(expected, actual);
    }
}

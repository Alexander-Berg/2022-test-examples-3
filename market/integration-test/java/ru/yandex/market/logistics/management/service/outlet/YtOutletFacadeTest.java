package ru.yandex.market.logistics.management.service.outlet;

import java.time.Instant;
import java.time.ZoneId;
import java.util.Set;
import java.util.stream.LongStream;

import com.github.springtestdbunit.annotation.DatabaseOperation;
import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.logistics.management.AbstractContextualTest;
import ru.yandex.market.logistics.management.domain.dto.filter.InternalLogisticsPointFilter;
import ru.yandex.market.logistics.management.facade.outlet.YtOutletFacade;
import ru.yandex.market.logistics.management.service.client.LogisticsPointService;
import ru.yandex.market.logistics.management.util.TestableClock;

import static org.mockito.Mockito.verify;

class YtOutletFacadeTest extends AbstractContextualTest {
    private static final Instant FIXED_TIME = Instant.parse("2020-03-13T10:15:30Z");

    @Autowired
    private YtOutletFacade ytOutletFacade;

    @Autowired
    private TestableClock clock;

    @Autowired
    private LogisticsPointService logisticsPointService;

    @BeforeEach
    void setup() {
        clock.setFixed(FIXED_TIME, ZoneId.systemDefault());
    }

    @AfterEach
    void teardown() {
        clock.clearFixed();
    }

    @Test
    @DisplayName("Обновление расписаний выходных всех аутлетов")
    @DatabaseSetup("/data/service/outlet/before/prepare_multiple_logistics_points.xml")
    @ExpectedDatabase(
        value = "/data/service/outlet/after/calendar_holidays_updated.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void updateAllYtOutlets() {
        ytOutletFacade.updateAllYtOutletCalendarHolidays();
        LongStream.range(1, 4).forEach(this::verifyLogisticPointSearch);
    }

    @Test
    @DisplayName("Обновление расписаний выходных аутлета — расписание пусто")
    @DatabaseSetup("/data/service/outlet/before/prepare_single_logistics_point.xml")
    @ExpectedDatabase(
        value = "/data/service/outlet/after/calendar_is_empty.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void updateYtOutletCalendarIsEmpty() {
        ytOutletFacade.updateAllYtOutletCalendarHolidays();
    }

    @Test
    @DisplayName("Обновление расписаний выходных аутлета — игнорирование выходных, которые не входят в диапазон")
    @DatabaseSetup({
        "/data/service/outlet/before/prepare_single_logistics_point.xml",
        "/data/service/outlet/before/calendar_day_modifiers/calendar_holidays_out_of_start_date_end_date_range.xml"
    })
    @ExpectedDatabase(
        value = "/data/service/outlet/after/holidays_out_of_range.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void updateYtOutletIgnoreHolidaysOutOfRange() {
        ytOutletFacade.updateAllYtOutletCalendarHolidays();
    }

    @Test
    @DisplayName("Обновление расписаний выходных аутлета — учёт выходных родительского календаря")
    @DatabaseSetup("/data/service/outlet/before/prepare_single_logistics_point.xml")
    @DatabaseSetup(
        value = "/data/service/outlet/before/calendar_day_modifiers/consider_parent_calendar.xml",
        type = DatabaseOperation.REFRESH
    )
    @ExpectedDatabase(
        value = "/data/service/outlet/after/consider_parent_calendar.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void updateYtOutletConsiderParentCalendar() {
        ytOutletFacade.updateAllYtOutletCalendarHolidays();
    }

    @Test
    @DisplayName("Обновление расписаний выходных аутлета — не учитываем рабочие дни")
    @DatabaseSetup({
        "/data/service/outlet/before/prepare_single_logistics_point.xml",
        "/data/service/outlet/before/calendar_day_modifiers/update_holidays_only.xml"
    })
    @ExpectedDatabase(
        value = "/data/service/outlet/after/update_holidays_only.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void updateYtOutletHolidaysOnly() {
        ytOutletFacade.updateAllYtOutletCalendarHolidays();
    }

    @Test
    @DisplayName("Обновление названий ПВЗ")
    @DatabaseSetup("/data/service/outlet/before/prepare_points_of_different_ds.xml")
    @ExpectedDatabase(
        value = "/data/service/outlet/after/outlet_names_updated.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void updateOutletName() {
        ytOutletFacade.updateOutletNames(107L, "new name");
    }

    private void verifyLogisticPointSearch(Long logisticPointId) {
        verify(logisticsPointService).search(
            InternalLogisticsPointFilter.newBuilder()
                .ids(Set.of(logisticPointId))
                .build()
        );
    }
}

package ru.yandex.market.hrms.core.domain.outstaff;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

import com.google.common.collect.BoundType;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.hrms.core.AbstractCoreTest;
import ru.yandex.market.hrms.core.service.outstaff.OutstaffCalendarService;
import ru.yandex.market.hrms.core.service.outstaff.shift.OutStaffShift;
import ru.yandex.market.hrms.model.view.outstaff.OutstaffActivityFilter;
import ru.yandex.market.tpl.common.util.datetime.LocalDateInterval;

@DbUnitDataSet(before = "OutstaffCalendarPageLoaderServiceTest.before.csv")
class OutstaffCalendarPageLoaderServiceTest extends AbstractCoreTest {
    @Autowired
    private OutstaffCalendarService service;

    @Test
    @DbUnitDataSet(before = "OutstaffCalendarPageLoaderServiceTest.simpleDay.csv")
    void testSimple() {
        var outstaffShiftData = service.getOutstaffShiftData(OutstaffCalendarRequest.builder()
                .domainId(1)
                .interval(LocalDateInterval.valueOf("2021-06-01/2021-06-30", BoundType.CLOSED))
                .activityFilter(OutstaffActivityFilter.SHOW_ALL)
                .build(), Pageable.unpaged());

        List<Long> expectedIds = List.of(100L, 101L, 102L, 103L, 104L, 105L, 107L);
        List<Long> actualIds = outstaffShiftData.outstaffPage()
                .getContent()
                .stream()
                .map(OutstaffEntity::getId)
                .sorted()
                .collect(Collectors.toList());

        Assertions.assertThat(outstaffShiftData.outstaffPage()).hasSize(7);
        Assertions.assertThat(actualIds).isEqualTo(expectedIds);
    }

    @Test
    @DbUnitDataSet(before = "OutstaffCalendarPageLoaderServiceTest.simpleDay.csv")
    void testFilterByName() {
        var outstaffShiftData = service.getOutstaffShiftData(OutstaffCalendarRequest.builder()
                .domainId(1)
                .interval(LocalDateInterval.valueOf("2021-06-01/2021-06-30", BoundType.CLOSED))
                .outstaffName("Петр")
                .activityFilter(OutstaffActivityFilter.SHOW_ALL)
                .build(), Pageable.unpaged());
        Assertions.assertThat(outstaffShiftData.outstaffPage()).extracting(OutstaffEntity::getId).containsExactly(101L);
    }

    @Test
    @DbUnitDataSet(before = "OutstaffCalendarPageLoaderServiceTest.simpleDay.csv")
    void testFilterByNameWhenWmsLoginIsNull() {
        var outstaffShiftData = service.getOutstaffShiftData(OutstaffCalendarRequest.builder()
                .domainId(1)
                .interval(LocalDateInterval.valueOf("2021-06-01/2021-06-30", BoundType.CLOSED))
                .outstaffName("Виктор")
                .activityFilter(OutstaffActivityFilter.SHOW_ALL)
                .build(), Pageable.unpaged());
        Assertions.assertThat(outstaffShiftData.outstaffPage()).extracting(OutstaffEntity::getId).containsExactly(107L);
    }

    @Test
    @DbUnitDataSet(before = "OutstaffCalendarPageLoaderServiceTest.simpleDay.csv")
    void testFilterByWmsLogin() {
        var outstaffShiftData = service.getOutstaffShiftData(OutstaffCalendarRequest.builder()
                .domainId(1)
                .interval(LocalDateInterval.valueOf("2021-06-01/2021-06-30", BoundType.CLOSED))
                .outstaffName("user-101")
                .activityFilter(OutstaffActivityFilter.SHOW_ALL)
                .build(), Pageable.unpaged());
        Assertions.assertThat(outstaffShiftData.outstaffPage()).extracting(OutstaffEntity::getId).containsExactly(101L);
    }

    @Test
    @DbUnitDataSet(before = "OutstaffCalendarPageLoaderServiceTest.simpleDay.csv")
    void testPagination() {
        var outstaffShiftData = service.getOutstaffShiftData(OutstaffCalendarRequest.builder()
                .domainId(1)
                .interval(LocalDateInterval.valueOf("2021-06-01/2021-06-30", BoundType.CLOSED))
                .activityFilter(OutstaffActivityFilter.SHOW_ALL)
                .build(), PageRequest.of(0, 2));
        Assertions.assertThat(outstaffShiftData.outstaffPage()).hasSize(2);

        outstaffShiftData = service.getOutstaffShiftData(OutstaffCalendarRequest.builder()
                .domainId(1)
                .interval(LocalDateInterval.valueOf("2021-06-01/2021-06-30", BoundType.CLOSED))
                .outstaffName("Петр")
                .activityFilter(OutstaffActivityFilter.SHOW_ALL)
                .build(), PageRequest.of(0, 2));
        Assertions.assertThat(outstaffShiftData.outstaffPage()).hasSize(1);

        outstaffShiftData = service.getOutstaffShiftData(OutstaffCalendarRequest.builder()
                .domainId(1)
                .interval(LocalDateInterval.valueOf("2021-06-01/2021-06-30", BoundType.CLOSED))
                .outstaffName("Петр")
                .activityFilter(OutstaffActivityFilter.SHOW_ALL)
                .build(), PageRequest.of(1, 2));
        Assertions.assertThat(outstaffShiftData.outstaffPage()).isEmpty();
    }

    @Test
    @DbUnitDataSet(before = "OutstaffCalendarPageLoaderServiceTest.simpleDay.csv")
    void testFilterByWorkingDate() {
        var outstaffShiftData = service.getOutstaffShiftData(OutstaffCalendarRequest.builder()
                .domainId(1)
                .workingDate(LocalDate.parse("2021-06-01"))
                .interval(LocalDateInterval.valueOf("2021-06-01/2021-06-30", BoundType.CLOSED))
                .activityFilter(OutstaffActivityFilter.SHOW_ALL)
                .build(), Pageable.unpaged());
        Assertions.assertThat(outstaffShiftData.outstaffPage()).extracting(OutstaffEntity::getId)
                .containsExactly(100L);

        outstaffShiftData = service.getOutstaffShiftData(OutstaffCalendarRequest.builder()
                .domainId(1)
                .workingDate(LocalDate.parse("2021-06-02"))
                .interval(LocalDateInterval.valueOf("2021-06-01/2021-06-30", BoundType.CLOSED))
                .activityFilter(OutstaffActivityFilter.SHOW_ALL)
                .build(), Pageable.unpaged());

        Assertions.assertThat(outstaffShiftData.outstaffPage()).isEmpty();

        outstaffShiftData = service.getOutstaffShiftData(OutstaffCalendarRequest.builder()
                .domainId(1)
                .workingDate(LocalDate.parse("2021-06-01"))
                .shiftIds(List.of(1L))
                .interval(LocalDateInterval.valueOf("2021-06-01/2021-06-30", BoundType.CLOSED))
                .activityFilter(OutstaffActivityFilter.SHOW_ALL)
                .build(), Pageable.unpaged());

        Assertions.assertThat(outstaffShiftData.outstaffPage()).isEmpty();

        outstaffShiftData = service.getOutstaffShiftData(OutstaffCalendarRequest.builder()
                .domainId(1)
                .workingDate(LocalDate.parse("2021-06-01"))
                .shiftIds(List.of(2L))
                .interval(LocalDateInterval.valueOf("2021-06-01/2021-06-30", BoundType.CLOSED))
                .activityFilter(OutstaffActivityFilter.SHOW_ALL)
                .build(), Pageable.unpaged());

        Assertions.assertThat(outstaffShiftData.outstaffPage()).extracting(OutstaffEntity::getId)
                .containsExactly(100L);
    }

    @Test
    @DbUnitDataSet(before = "OutstaffCalendarPageLoaderServiceTest.paginationWithWorkingDateFiltering.csv")
    void paginationWithWorkingDateFiltering() {
        var outstaffShiftData1 = service.getOutstaffShiftData(OutstaffCalendarRequest.builder()
                .domainId(1)
                .workingDate(LocalDate.parse("2021-06-01"))
                .activityFilter(OutstaffActivityFilter.SHOW_ALL)
                .interval(LocalDateInterval.valueOf("2021-06-01/2021-06-30", BoundType.CLOSED))
                .build(), PageRequest.of(0, 2));
        Assertions.assertThat(outstaffShiftData1.outstaffPage()).extracting(OutstaffEntity::getId)
                .containsExactlyInAnyOrder(100L, 101L);
        Assertions.assertThat(outstaffShiftData1.outstaffShiftResult().getShifts())
                .extracting(OutStaffShift::getOutstaffId)
                .containsExactlyInAnyOrder(100L, 101L);

        var outstaffShiftData2 = service.getOutstaffShiftData(OutstaffCalendarRequest.builder()
                .domainId(1)
                .workingDate(LocalDate.parse("2021-06-01"))
                .activityFilter(OutstaffActivityFilter.SHOW_ALL)
                .interval(LocalDateInterval.valueOf("2021-06-01/2021-06-30", BoundType.CLOSED))
                .build(), PageRequest.of(1, 2));
        Assertions.assertThat(outstaffShiftData2.outstaffPage()).extracting(OutstaffEntity::getId)
                .containsExactly(102L);
        Assertions.assertThat(outstaffShiftData2.outstaffShiftResult().getShifts())
                .extracting(OutStaffShift::getOutstaffId)
                .containsExactly(102L);
    }
}

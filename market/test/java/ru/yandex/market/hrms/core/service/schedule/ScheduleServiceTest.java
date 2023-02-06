package ru.yandex.market.hrms.core.service.schedule;

import java.time.ZoneId;
import java.util.List;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.hrms.core.AbstractCoreTest;
import ru.yandex.market.hrms.core.domain.domain.repo.Domain;
import ru.yandex.market.hrms.core.domain.employee.repo.HROperationType;
import ru.yandex.market.hrms.core.domain.schedule.ScheduleService;
import ru.yandex.market.hrms.core.domain.schedule.repo.ScheduleEntity;
import ru.yandex.market.hrms.core.service.oebs.OebsMapper;
import ru.yandex.market.hrms.core.service.oebs.client.model.ScheduleDetailDto;

@RequiredArgsConstructor
@DbUnitDataSet(before = "ScheduleServiceTest.beforeEach.csv")
public class ScheduleServiceTest extends AbstractCoreTest {

    private static final long DOMAIN_ID = 1L;

    private static final HROperationType TEST_HR_OPERATION_TYPE = new HROperationType(1L, "", "", "", "", "",
            false, false, "", false, null, null);

    @Autowired
    private ApplicationContext context;

    @Autowired
    private ScheduleService scheduleService;

    @Test
    @DbUnitDataSet(after = "ScheduleServiceTest.resultSuccess.csv")
    public void shouldSuccessfullySaveNonOverlappingScheduleDetails() {
        List<ScheduleEntity> schedules = scheduleService.loadSchedules(DOMAIN_ID);
        mapSchedulesDateTimes(schedules);
        scheduleService.saveSchedules(schedules);
    }

    @Test
    @DbUnitDataSet(before = "ScheduleServiceTest.addOverlappingSchedule.csv")
    public void shouldNotSaveOverlappingSchedules() {
        List<ScheduleEntity> schedules = scheduleService.loadSchedules(DOMAIN_ID);
        mapSchedulesDateTimes(schedules);
        var saved = scheduleService.saveSchedules(schedules);

        Assertions.assertEquals(0, saved.size());
    }

    private void mapSchedulesDateTimes(List<ScheduleEntity> schedules) {
        var mapper = context.getBean(OebsMapper.class);

        for (var schedule : schedules) {
            var detailsSet = schedule.getScheduleDetails();
            for (var detail : detailsSet) {
                var day = mapper.scheduleDetailToScheduleDay(new ScheduleDetailDto("Y", detail.getDate(),
                        detail.getStart().toString(), detail.getEnd().toString(), detail.getOffset()));
                var mappedDetail = mapper.mapToEntity(
                        day, TEST_HR_OPERATION_TYPE, Domain.builder().timezone(ZoneId.systemDefault()).build()
                );
                detail.setStartDateTime(mappedDetail.getStartDateTime());
                detail.setEndDateTime(mappedDetail.getEndDateTime());
            }
        }
    }
}

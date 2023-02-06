package ru.yandex.market.hrms.core.service.schedule;

import java.util.Optional;
import java.util.Set;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import one.util.streamex.StreamEx;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.hrms.core.AbstractCoreTest;
import ru.yandex.market.hrms.core.domain.new_schedule.NewScheduleMapper;
import ru.yandex.market.hrms.core.domain.new_schedule.repo.NewScheduleEntity;
import ru.yandex.market.hrms.core.service.employee.HROperationTypeService;
import ru.yandex.market.hrms.core.service.oebs.OebsMapper;
import ru.yandex.market.hrms.core.service.oebs.client.model.GetDepartmentScheduleResponse;
import ru.yandex.market.hrms.core.service.oebs.client.model.ScheduleDto;
import ru.yandex.market.hrms.core.service.oebs.model.Schedule;

public abstract class AbstractNewScheduleTest extends AbstractCoreTest {

    @Autowired
    NewScheduleMapper newScheduleMapper;

    @Autowired
    HROperationTypeService hrOperationTypeService;
    @Autowired
    OebsMapper oebsMapper;
    @Autowired
    ObjectMapper objectMapper;

    Set<NewScheduleEntity> mapFromFile(String file) {
        String json = loadFromFile(file);

        GetDepartmentScheduleResponse response = fromJson(json);
        return StreamEx.of(response.getSchedules())
                .filter(ScheduleDto::isOk)
                .map(oebsMapper::scheduleDtoToSchedule)
                .map(s -> newScheduleMapper.mapToNewScheduleEntity(s, hrOperationTypeService.loadHrOperationTypeMap()))
                .toSet();
    }

    Optional<Schedule> mapOebsScheduleFromFile(String file) {
        String json = loadFromFile(file);

        GetDepartmentScheduleResponse response = fromJson(json);
        return StreamEx.of(response.getSchedules())
                .filter(ScheduleDto::isOk)
                .map(oebsMapper::scheduleDtoToSchedule)
                .findFirst();
    }

    private GetDepartmentScheduleResponse fromJson(String json) {
        try {
            return objectMapper.readValue(json, GetDepartmentScheduleResponse.class);
        }
        catch (JsonProcessingException e) {
            throw new RuntimeException("failed to read json: " + json, e);
        }
    }
}

package ru.yandex.market.tpl.carrier.core.domain.duty_schedule;

import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

import ru.yandex.market.tpl.carrier.core.domain.schedule.command.ScheduleCommand;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface DutyScheduleGeneratorMapper {

    ScheduleCommand.Create map(DutyScheduleGenerator.ScheduleGenerateParams params);

    DutyScheduleCommand.Create map(DutyScheduleGenerator.DutyScheduleGenerateParams params, long scheduleId);
}

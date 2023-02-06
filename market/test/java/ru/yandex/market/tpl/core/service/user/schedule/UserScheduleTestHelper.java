package ru.yandex.market.tpl.core.service.user.schedule;

import java.time.LocalDate;

import lombok.experimental.UtilityClass;

import ru.yandex.market.tpl.api.model.schedule.UserScheduleRuleDto;
import ru.yandex.market.tpl.api.model.schedule.UserScheduleType;
import ru.yandex.market.tpl.api.model.user.CourierVehicleType;
import ru.yandex.market.tpl.common.util.datetime.LocalTimeInterval;
import ru.yandex.market.tpl.common.util.datetime.RelativeTimeInterval;
import ru.yandex.market.tpl.core.domain.partner.SortingCenter;

@UtilityClass
public class UserScheduleTestHelper {

    public static UserScheduleRuleDto ruleDto(UserScheduleType type, LocalDate activeFrom, LocalDate activeTo) {
        return ruleDtoApplyFrom(type, activeFrom, activeTo, activeFrom);
    }

    public static UserScheduleRuleDto ruleDtoApplyFrom(UserScheduleType type,
                                                       LocalDate activeFrom,
                                                       LocalDate activeTo,
                                                       LocalDate applyFrom,
                                                       boolean[] maskWorkDays,
                                                       long sortingCenterId,
                                                       UserScheduleData scheduleData
                                                       ) {
        if (type == UserScheduleType.OVERRIDE_SKIP) {
            return UserScheduleRuleDto.createOverrideSkipDto(activeFrom, activeTo, applyFrom);
        }
        return new UserScheduleRuleDto(null,
                type,
                activeFrom, activeTo, applyFrom,
                scheduleData.getShiftStart(), scheduleData.getShiftEnd(),
                scheduleData.getVehicleType(),
                sortingCenterId,
                maskWorkDays,
                null
        );
    }

    public static UserScheduleRuleDto ruleDtoApplyFrom(UserScheduleType type,
                                                       LocalDate activeFrom,
                                                       LocalDate activeTo,
                                                       LocalDate applyFrom) {
        return ruleDtoApplyFrom(
                type,
                activeFrom,
                activeTo,
                applyFrom,
                type.getMaskWorkDays(),
                SortingCenter.DEFAULT_SC_ID,
                createScheduleData(CourierVehicleType.CAR)
        );
    }

    public static UserScheduleRuleDto ruleDtoApplyFrom(UserScheduleType type,
                                                       LocalDate activeFrom, LocalDate activeTo,
                                                       LocalDate applyFrom,
                                                       LocalTimeInterval shiftInterval,
                                                       long sortingCenterId) {
        if (type == UserScheduleType.OVERRIDE_SKIP) {
            return UserScheduleRuleDto.createOverrideSkipDto(activeFrom, activeTo, applyFrom);
        }
        UserScheduleData scheduleData = createScheduleData(CourierVehicleType.CAR);
        return new UserScheduleRuleDto(null,
                type,
                activeFrom, activeTo, applyFrom,
                shiftInterval.getStart(), shiftInterval.getEnd(),
                scheduleData.getVehicleType(),
                sortingCenterId,
                null,
                null
        );
    }

    public static UserScheduleData createScheduleData(CourierVehicleType vehicleType) {
        return new UserScheduleData(vehicleType, RelativeTimeInterval.valueOf("10:00-18:00"));
    }

}

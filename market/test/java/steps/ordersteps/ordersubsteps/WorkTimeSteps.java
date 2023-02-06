package steps.ordersteps.ordersubsteps;

import java.time.DayOfWeek;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import ru.yandex.market.delivery.entities.common.TimeInterval;
import ru.yandex.market.delivery.entities.common.WorkTime;

public class WorkTimeSteps {

    private WorkTimeSteps() {
        throw new UnsupportedOperationException();
    }

    public static WorkTime getWorkTime() {
        WorkTime workTime = new WorkTime();

        workTime.setDay(DayOfWeek.MONDAY.getValue());
        workTime.setPeriods(Collections.singletonList(new TimeInterval("09:00/18:00")));

        return workTime;
    }

    public static List<WorkTime> getWorkTimeArray() {
        return Arrays.asList(getWorkTime(), getWorkTime());
    }
}

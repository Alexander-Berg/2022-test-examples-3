package ru.yandex.market.tsum.tms.tasks.duty.switchduty.tasks;

import org.springframework.stereotype.Component;
import ru.yandex.market.tsum.tms.tasks.duty.switchduty.SwitchDutyMarketInfra;

/**
 * @author Mishunin Andrei <a href="mailto:mishunin@yandex-team.ru"></a>
 * @date 22.11.2019
 */
@Component
public class SwitchDutyMarketInfraTest extends SwitchDutyMarketInfra {
    public static final String TRACKER_QUEUE = "MARKETINFRATEST";

    @Override
    protected String getTrackerQueue() {
        return TRACKER_QUEUE;
    }

}

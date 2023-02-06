package ru.yandex.market.tpl.core.domain.routing.schedule;

import java.time.LocalTime;

import lombok.experimental.UtilityClass;

import ru.yandex.market.tpl.core.domain.partner.SortingCenter;

@UtilityClass
public class RoutingScheduleRuleUtil {
    public RoutingScheduleRule routingScheduleRule() {
        return routingScheduleRule(SortingCenter.DEFAULT_SC_ID);
    }

    private RoutingScheduleRule routingScheduleRule(long sortingCenterId) {
        RoutingScheduleRule routingScheduleRule = new RoutingScheduleRule();
        routingScheduleRule.setSortingCenterId(sortingCenterId);
        routingScheduleRule.setSameDay(false);
        routingScheduleRule.setPreRoutingStartTime(LocalTime.of(21, 0));
        routingScheduleRule.setMainRoutingStartTime(LocalTime.of(22, 0));
        return routingScheduleRule;
    }

    public RoutingScheduleRule routingScheduleRule(long sortingCenterId,
                                                    boolean sameDay,
                                                    LocalTime preRoutingStartTime,
                                                    LocalTime mainRoutingStartTime) {
        RoutingScheduleRule routingScheduleRule = new RoutingScheduleRule();
        routingScheduleRule.setSortingCenterId(sortingCenterId);
        routingScheduleRule.setSameDay(sameDay);
        routingScheduleRule.setPreRoutingStartTime(preRoutingStartTime);
        routingScheduleRule.setMainRoutingStartTime(mainRoutingStartTime);
        return routingScheduleRule;
    }
}

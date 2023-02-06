package ru.yandex.market.clickphite.metric.mocks;

import ru.yandex.market.monitoring.ComplicatedMonitoring;
import ru.yandex.market.monitoring.MonitoringUnit;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Anton Sukhonosenko <a href="mailto:algebraic@yandex-team.ru"></a>
 * @date 01.12.16
 */
public class ComplicatedMonitoringMock extends ComplicatedMonitoring {
    private List<MonitoringUnit> units = new ArrayList<>();

    @Override
    public void addUnit(MonitoringUnit unit) {
        units.add(unit);
    }

    public List<MonitoringUnit> getUnits() {
        return units;
    }

    public MonitoringUnit getUnit() {
        return units.get(0);
    }
}

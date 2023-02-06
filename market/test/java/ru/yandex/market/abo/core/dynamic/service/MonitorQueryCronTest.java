package ru.yandex.market.abo.core.dynamic.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.abo.core.monitor.MonitorQueryRepository;
import ru.yandex.market.abo.core.monitor.model.MonitorQuery;

/**
 * @author artemmz
 * @date 04/03/19.
 */
public class MonitorQueryCronTest extends CronExpressionsTest {
    @Autowired
    private MonitorQueryRepository monitorQueryRepository;

    @Override
    public List<String> getCronExpressions() {
        return monitorQueryRepository.findAll().stream()
                .map(MonitorQuery::getCronExpression).collect(Collectors.toList());
    }
}

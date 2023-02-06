package ru.yandex.market.pers.address.util;

import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import ru.yandex.market.monitoring.ComplicatedMonitoring;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;

import static org.springframework.util.ReflectionUtils.findField;
import static org.springframework.util.ReflectionUtils.getField;
import static org.springframework.util.ReflectionUtils.makeAccessible;

public class ComplicatedMonitorCleaner implements BeforeEachCallback {
    @Override
    public void beforeEach(ExtensionContext context) throws Exception {
        ApplicationContext applicationContext = SpringExtension.getApplicationContext(context);
        Collection<ComplicatedMonitoring> complicatedMonitors = applicationContext.getBeansOfType(ComplicatedMonitoring.class).values();

        complicatedMonitors.stream()
            .map(complicatedMonitoring -> Optional.ofNullable(findField(ComplicatedMonitoring.class, "units"))
                .map(field -> {
                    makeAccessible(field);
                    return (Map<?, ?>) getField(field, complicatedMonitoring);
                })
                .orElseThrow(() -> new AssertionError("Can not get field 'units' in " + complicatedMonitoring)))
            .forEach(Map::clear);
    }
}

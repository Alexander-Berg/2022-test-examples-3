package ru.yandex.chemodan.app.psbilling.web.model;

import org.junit.Test;

import ru.yandex.chemodan.app.psbilling.core.synchronization.engine.Target;

import static org.junit.Assert.assertNotNull;

public class ServiceStatusTest {

    @Test
    public void testToCore() {
        for (ServiceStatus type : ServiceStatus.values()) {
            assertNotNull(type.toCoreEnum());
        }
    }

    @Test
    public void testFromCore() {
        for (Target v : Target.values()) {
            assertNotNull(ServiceStatus.fromCoreEnum(v));
        }
    }
}

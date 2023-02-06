package ru.yandex.chemodan.app.psbilling.web.model;

import org.junit.Test;

import ru.yandex.chemodan.app.psbilling.core.synchronization.engine.SynchronizationStatus;

import static org.junit.Assert.assertNotNull;

public class SynchronizationStatusApiTest {

    @Test
    public void testFromCoreEnum() {
        for (SynchronizationStatus v : SynchronizationStatus.values()) {
            assertNotNull(SynchronizationStatusApi.fromCoreEnum(v));
        }
    }
}

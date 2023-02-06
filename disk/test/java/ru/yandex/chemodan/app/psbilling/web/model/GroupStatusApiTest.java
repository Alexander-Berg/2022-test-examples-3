package ru.yandex.chemodan.app.psbilling.web.model;

import org.junit.Test;

import ru.yandex.chemodan.app.psbilling.core.entities.groups.GroupStatus;

import static org.junit.Assert.assertNotNull;

public class GroupStatusApiTest {

    @Test
    public void testToCore() {
        for (GroupStatusApi type : GroupStatusApi.values()) {
            assertNotNull(type.toCoreEnum());
        }
    }

    @Test
    public void testFromCore() {
        for (GroupStatus v : GroupStatus.values()) {
            assertNotNull(GroupStatusApi.fromCoreEnum(v));
        }
    }
}

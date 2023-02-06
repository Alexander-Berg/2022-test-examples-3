package ru.yandex.chemodan.app.psbilling.web.model;

import org.junit.Test;

import ru.yandex.chemodan.app.psbilling.core.entities.groups.GroupType;

import static org.junit.Assert.assertNotNull;

public class GroupTypeApiTest {

    @Test
    public void testToCore() {
        for (GroupTypeApi type : GroupTypeApi.values()) {
            assertNotNull(type.toCoreEnum());
        }
    }

    @Test
    public void testFromCore() {
        for (GroupType v : GroupType.values()) {
            assertNotNull(GroupTypeApi.fromCoreEnum(v));
        }
    }
}

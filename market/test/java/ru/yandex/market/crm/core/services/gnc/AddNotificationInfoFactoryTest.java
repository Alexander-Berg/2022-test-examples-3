package ru.yandex.market.crm.core.services.gnc;

import java.util.Collections;
import java.util.UUID;

import com.google.common.collect.ImmutableMap;
import org.junit.Test;

import ru.yandex.market.crm.core.services.external.gnc.domain.AddNotificationInfo;
import ru.yandex.market.crm.core.services.external.gnc.domain.ModelMeta;
import ru.yandex.market.crm.core.services.external.gnc.domain.NotificationMeta;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class AddNotificationInfoFactoryTest {

    private static void assertInfoEquals(AddNotificationInfo expected, AddNotificationInfo actual) {
        assertEquals(expected.getType(), actual.getType());
        assertEquals(expected.getPuid(), actual.getPuid());
        assertEquals(expected.getService(), actual.getService());
        assertEquals(expected.getActor(), actual.getActor());
        assertEquals(expected.getGroupKey(), actual.getGroupKey());
        assertEquals(expected.getSubscriptionKey(), actual.getSubscriptionKey());

        assertMetaEquals(expected.getMeta(), actual.getMeta());
    }

    private static void assertMetaEquals(NotificationMeta expected, NotificationMeta actual) {
        if (expected == null) {
            assertNull(actual);
            return;
        }

        assertNotNull(actual);
        assertActionEquals(expected.getAction(), actual.getAction());
        assertEntityEquals(expected.getEntity(), actual.getEntity());
        assertMetaSubType(expected, actual);
    }

    private static void assertActionEquals(NotificationMeta.Action expected, NotificationMeta.Action actual) {
        if (expected == null) {
            assertNull(actual);
        } else {
            assertNotNull(actual);
            assertEquals(expected.getType(), actual.getType());
            assertEquals(expected.getLink(), actual.getLink());
        }
    }

    private static void assertEntityEquals(NotificationMeta.Entity expected, NotificationMeta.Entity actual) {
        if (expected == null) {
            assertNull(actual);
        } else {
            assertNotNull(actual);
            assertEquals(expected.getType(), expected.getType());
            assertEquals(expected.getPreview(), expected.getPreview());
        }
    }

    private static void assertMetaSubType(NotificationMeta expected, NotificationMeta actual) {
        assertEquals(expected.getClass(), actual.getClass());
        if (expected instanceof ModelMeta) {
            assertEquals(((ModelMeta) expected).getModel().getType(), ((ModelMeta) actual).getModel().getType());
            assertEquals(((ModelMeta) expected).getModel().getName(), ((ModelMeta) actual).getModel().getName());
        } else {
            throw new IllegalArgumentException("Unknown meta type: " + expected.getClass());
        }
    }

    @Test
    public void makeNewAnswerAddNotificationInfoTest() {
        AddNotificationInfo actual = AddNotificationInfoFactory.make(
                "new-answer",
                111,
                123123L,
                "actionLink",
                "resourceLink",
                ImmutableMap.of("model", "iphone")
        );

        AddNotificationInfo expected = new AddNotificationInfo("111", "123123", "new-answer", "market");
        NotificationMeta meta = new ModelMeta("iphone").setAction("actionLink").setEntity("resourceLink");
        expected.setMeta(meta).setGroupKey(UUID.nameUUIDFromBytes(("111iphone").getBytes()).toString());

        assertInfoEquals(expected, actual);
    }

    @Test
    public void testMakesNotificationInfoWithoutLinks() {
        AddNotificationInfo actual = AddNotificationInfoFactory.make(
                "new-answer",
                111,
                123123L,
                null,
                null,
                ImmutableMap.of("model", "айфон")
        );

        AddNotificationInfo expected = new AddNotificationInfo("111", "123123", "new-answer", "market");
        ModelMeta iphone = new ModelMeta("айфон");
        expected.setMeta(iphone).setGroupKey(UUID.nameUUIDFromBytes(("111айфон").getBytes()).toString());

        assertInfoEquals(expected, actual);
    }

    @Test
    public void testMakeStaticNotificationInfo() {
        AddNotificationInfo actual = AddNotificationInfoFactory.make(
                "somePromoGnc", 555L, null, null, null, Collections.emptyMap());

        AddNotificationInfo expected = new AddNotificationInfo("555", "market", "somePromoGnc", "market");
        assertInfoEquals(expected, actual);
    }
}
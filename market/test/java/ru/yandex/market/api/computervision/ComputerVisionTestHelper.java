package ru.yandex.market.api.computervision;

import com.google.common.base.Strings;

import ru.yandex.market.api.integration.UnitTestBase;
import ru.yandex.market.api.internal.computervision.EntityId;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author dimkarp93
 */
public class ComputerVisionTestHelper extends UnitTestBase {
    public static void assertModel(long modelId, EntityId entityId) {
        assertTrue(entityId.hasModel());
        assertEquals(modelId, entityId.getModelId());
    }

    public static void assertOffer(String wareMd5, EntityId entityId) {
        assertTrue(entityId.hasOffer());
        assertEquals(wareMd5, entityId.getWareMd5());
    }

    public static void assertEmpty(EntityId entityId) {
        assertFalse(entityId.hasModel());
        assertFalse(entityId.hasOffer());
    }

    public static EntityId offer(String wareMd5) {
        assertTrue(!Strings.isNullOrEmpty(wareMd5));

        EntityId entityId = new EntityId();
        entityId.setWareMd5(wareMd5);

        assertTrue(entityId.hasOffer());

        return entityId;
    }

    public static EntityId model(long modelId) {
        assertTrue(modelId > 0);

        EntityId entityId = new EntityId();
        entityId.setModelId(modelId);

        assertTrue(entityId.hasModel());

        return entityId;
    }
}

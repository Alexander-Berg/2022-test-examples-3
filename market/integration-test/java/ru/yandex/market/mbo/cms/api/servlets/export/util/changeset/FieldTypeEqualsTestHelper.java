package ru.yandex.market.mbo.cms.api.servlets.export.util.changeset;

import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.Assert;

import ru.yandex.market.mbo.cms.core.models.FieldType;

public class FieldTypeEqualsTestHelper {

    private FieldTypeEqualsTestHelper() {
    }

    private static void assertEquals(FieldType f1, FieldType f2) {
        Assert.assertEquals(f1.getProperties(), f2.getProperties());
    }

    public static void assertEquals(LinkedHashMap<String, FieldType> f1, LinkedHashMap<String, FieldType> f2) {
        if (f1 == null && f2 == null) {
            return;
        }
        Assert.assertTrue(f1 != null && f2 != null);
        Assert.assertEquals(f1.keySet(), f2.keySet());
        for (Map.Entry<String, FieldType> entry : f1.entrySet()) {
            assertEquals(entry.getValue(), f2.get(entry.getKey()));
        }
    }

}

package ru.yandex.market.mbo.cms.api.servlets.export.util.changeset;

import java.util.Iterator;
import java.util.List;

import org.junit.Assert;

import ru.yandex.market.mbo.cms.core.models.KeyTemplate;

public class KeyTemplateEqualsTestHelper {

    private KeyTemplateEqualsTestHelper() {
    }

    private static void assertEquals(KeyTemplate t1, KeyTemplate t2) {

        Assert.assertEquals(t1.getTemplate(), t2.getTemplate());
        Assert.assertEquals(t1.getUniq(), t2.getUniq());
        Assert.assertEquals(t1.getRequired(), t2.getRequired());
        Assert.assertEquals(t1.getRequiredGroup(), t2.getRequiredGroup());
    }

    public static void assertEquals(List<KeyTemplate> t1, List<KeyTemplate> t2) {
        if (t1 == null && t2 == null) {
            return;
        }
        Assert.assertTrue(t1 != null && t2 != null);
        Assert.assertEquals(t1.size(), t2.size());

        Iterator<KeyTemplate> t2Iterator = t2.iterator();
        for (KeyTemplate t : t1) {
            assertEquals(t, t2Iterator.next());
        }
    }

}

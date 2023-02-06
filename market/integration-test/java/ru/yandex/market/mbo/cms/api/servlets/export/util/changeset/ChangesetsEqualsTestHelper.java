package ru.yandex.market.mbo.cms.api.servlets.export.util.changeset;

import org.junit.Assert;

import ru.yandex.market.mbo.cms.core.models.Changeset;

public class ChangesetsEqualsTestHelper {

    private ChangesetsEqualsTestHelper() {
    }

    public static void assertEquals(Changeset c1, Changeset c2) {
        Assert.assertEquals(c1.getId(), c2.getId());
        Assert.assertEquals(c1.getSource(), c2.getSource());
        Assert.assertEquals(c1.getActionType(), c2.getActionType());
        NodeTypesEqualsTestHelper.assertEquals(c1.getNodeTypes(), c2.getNodeTypes());
        DocumentTypeEqualsTestHelper.assertEquals(c1.getDocuments(), c2.getDocuments());
    }
}

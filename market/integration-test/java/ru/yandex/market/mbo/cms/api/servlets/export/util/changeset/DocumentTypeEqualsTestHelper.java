package ru.yandex.market.mbo.cms.api.servlets.export.util.changeset;

import java.util.Map;

import org.junit.Assert;

import ru.yandex.market.mbo.cms.core.models.DocumentDescription;

public class DocumentTypeEqualsTestHelper {

    private DocumentTypeEqualsTestHelper() {
    }

    public static void assertEquals(DocumentDescription d1, DocumentDescription d2) {
        Assert.assertEquals(d1.getRootTemplate(), d2.getRootTemplate());
        Assert.assertEquals(d1.getSimilarDomain(), d2.getSimilarDomain());
        Assert.assertEquals(d1.getType(), d2.getType());
        Assert.assertEquals(d1.getLabel(), d2.getLabel());
        Assert.assertEquals(d1.getNodeVersions(), d2.getNodeVersions());
        Assert.assertTrue(
                (d1.getNodeVersions() == null && d2.getNodeVersions() == null)
                        || d1.getNodeVersions().equals(d2.getNodeVersions())
        );
        DocumentExportEqualsTestHelper.assertEquals(d1.getExports(), d2.getExports());
    }

    public static void assertEquals(Map<String, DocumentDescription> d1, Map<String, DocumentDescription> d2) {
        if (d1 == null && d2 == null) {
            return;
        }
        Assert.assertTrue(d1 != null && d2 != null);

        Assert.assertEquals(d1.keySet(), d2.keySet());
        for (Map.Entry<String, DocumentDescription> entry : d1.entrySet()) {
            assertEquals(entry.getValue(), d2.get(entry.getKey()));
        }
    }
}

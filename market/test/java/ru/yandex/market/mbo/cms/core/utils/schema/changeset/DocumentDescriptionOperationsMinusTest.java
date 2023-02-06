package ru.yandex.market.mbo.cms.core.utils.schema.changeset;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import ru.yandex.market.mbo.cms.core.models.Constants;
import ru.yandex.market.mbo.cms.core.models.DocumentDescription;
import ru.yandex.market.mbo.cms.core.models.DocumentExport;

public class DocumentDescriptionOperationsMinusTest {

    @Test(expected = IllegalArgumentException.class)
    public void testNullMinusNull() {
        Assert.assertNull(DocumentDescriptionOperations.documentMinus(null, null));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testDocMinusNull() {
        Assert.assertNull(
                DocumentDescriptionOperations.documentMinus(new DocumentDescription(), null)
        );
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNullMinusDoc() {
        Assert.assertNull(
                DocumentDescriptionOperations.documentMinus(null, new DocumentDescription())
        );
    }

    @Test
    public void testEmptyDocMinusEmptyDoc() {
        DocumentDescription doc = DocumentDescriptionOperations.documentMinus(
                new DocumentDescription(), new DocumentDescription()
        );
        Assert.assertNull(doc.getExports());
        Assert.assertNull(doc.getLabel());
        Assert.assertNull(doc.getNamespace());
        Assert.assertNull(doc.getNodeVersions());
        Assert.assertNull(doc.getRootTemplate());
        Assert.assertNull(doc.getSimilarDomain());
        Assert.assertNull(doc.getType());
    }

    @Test
    public void testDocsMinusDocs() {
        Assert.assertNull(DocumentDescriptionOperations.documentsMinus(null, null));
        Assert.assertNotNull(DocumentDescriptionOperations.documentsMinus(new HashMap<>(), null));
        Assert.assertNull(DocumentDescriptionOperations.documentsMinus(null, new HashMap<>()));
    }

    @Test
    public void testExportsMinusExports() {
        DocumentDescription doc1 = new DocumentDescription();
        DocumentDescription doc2 = new DocumentDescription();

        doc1.setExports(null);
        doc2.setExports(Collections.emptyList());
        Assert.assertNull(DocumentDescriptionOperations.documentMinus(doc1, doc2).getExports());

        doc1.setExports(Collections.emptyList());
        doc2.setExports(null);
        Assert.assertEquals(0, DocumentDescriptionOperations.documentMinus(doc1, doc2).getExports().size());

        doc1.setExports(createDocumentEmptyExportList(Constants.Device.DESKTOP, Constants.Format.JSON, "view"));
        doc2.setExports(null);
        Assert.assertEquals(1, DocumentDescriptionOperations.documentMinus(doc1, doc2).getExports().size());

        doc1.setExports(createDocumentEmptyExportList(Constants.Device.DESKTOP, Constants.Format.JSON, "view"));
        doc2.setExports(createDocumentEmptyExportList(Constants.Device.DESKTOP, Constants.Format.JSON, "view"));
        Assert.assertEquals(0, DocumentDescriptionOperations.documentMinus(doc1, doc2).getExports().size());

        doc1.setExports(createDocumentEmptyExportList(Constants.Device.DESKTOP, Constants.Format.JSON, "view"));
        doc2.setExports(createDocumentEmptyExportList(Constants.Device.PHONE, Constants.Format.JSON, "view"));
        Assert.assertEquals(1, DocumentDescriptionOperations.documentMinus(doc1, doc2).getExports().size());

        doc1.setExports(createDocumentEmptyExportList(Constants.Device.DESKTOP, Constants.Format.JSON, "view"));
        doc2.setExports(createDocumentEmptyExportList(Constants.Device.DESKTOP, Constants.Format.XML, "view"));
        Assert.assertEquals(1, DocumentDescriptionOperations.documentMinus(doc1, doc2).getExports().size());

        doc1.setExports(createDocumentEmptyExportList(Constants.Device.DESKTOP, Constants.Format.JSON, "view"));
        doc2.setExports(createDocumentEmptyExportList(Constants.Device.DESKTOP, Constants.Format.JSON, "view2"));
        Assert.assertEquals(1, DocumentDescriptionOperations.documentMinus(doc1, doc2).getExports().size());
    }

    @Test
    public void testDocMinusEmptyDoc() {
        DocumentDescription doc1 = new DocumentDescription();
        DocumentDescription doc2 = new DocumentDescription();

        doc1.setType("type");
        doc1.setNamespace("namespace");
        doc1.setLabel("type");
        doc1.setSimilarDomain("type");
        doc1.setRootTemplate("type");
        doc1.setNodeVersions(Collections.singleton(1));

        DocumentDescription result = DocumentDescriptionOperations.documentMinus(doc1, doc2);

        Assert.assertEquals(doc1.getType(), result.getType());
        Assert.assertEquals(doc1.getNamespace(), result.getNamespace());
        Assert.assertEquals(doc1.getLabel(), result.getLabel());
        Assert.assertEquals(doc1.getSimilarDomain(), result.getSimilarDomain());
        Assert.assertEquals(doc1.getRootTemplate(), result.getRootTemplate());
        Assert.assertEquals(doc1.getNodeVersions(), result.getNodeVersions());
    }

    @Test
    public void testEmptyDocMinusDoc() {
        DocumentDescription doc1 = new DocumentDescription();
        DocumentDescription doc2 = new DocumentDescription();

        doc1.setType("type1");

        doc2.setType("type2");
        doc2.setNamespace("namespace");
        doc2.setLabel("type");
        doc2.setSimilarDomain("type");
        doc2.setRootTemplate("type");
        doc2.setNodeVersions(Collections.singleton(1));

        DocumentDescription result = DocumentDescriptionOperations.documentMinus(doc1, doc2);

        Assert.assertEquals(doc1.getType(), result.getType());
        Assert.assertNull(result.getNamespace());
        Assert.assertNull(result.getLabel());
        Assert.assertNull(result.getSimilarDomain());
        Assert.assertNull(result.getRootTemplate());
        Assert.assertNull(result.getNodeVersions());
    }

    @Test
    public void testIsDocumentEmpty() {
        DocumentDescription doc = new DocumentDescription();
        Assert.assertTrue(DocumentDescriptionOperations.isDocumentEmpty(doc));

        doc.setExports(Collections.emptyList());
        Assert.assertFalse(DocumentDescriptionOperations.isDocumentEmpty(doc));
    }

    private List<DocumentExport> createDocumentEmptyExportList(
            Constants.Device device, Constants.Format format, String view
    ) {
        DocumentExport result = new DocumentExport();
        result.setDevice(device);
        result.setFormat(format);
        result.setView(view);
        return Collections.singletonList(result);
    }
}

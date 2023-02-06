package ru.yandex.market.mbo.cms.core.utils.schema.changeset;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import ru.yandex.market.mbo.cms.core.models.Constants;
import ru.yandex.market.mbo.cms.core.models.DocumentDescription;
import ru.yandex.market.mbo.cms.core.models.DocumentExport;

public class DocumentDescriptionOperationsPlusTest {

    @Test(expected = IllegalArgumentException.class)
    public void testNullPlusNull() {
        Assert.assertNull(DocumentDescriptionOperations.documentPlus(null, null));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testDocPlusNull() {
        Assert.assertNull(
                DocumentDescriptionOperations.documentPlus(new DocumentDescription(), null)
        );
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNullPlusDoc() {
        Assert.assertNull(
                DocumentDescriptionOperations.documentPlus(null, new DocumentDescription())
        );
    }

    @Test
    public void testEmptyDocPlusEmptyDoc() {
        DocumentDescription doc = DocumentDescriptionOperations.documentPlus(
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
    public void testDocsPlusDocs() {
        Assert.assertNull(DocumentDescriptionOperations.documentsPlus(null, null));
        Assert.assertNotNull(DocumentDescriptionOperations.documentsPlus(new HashMap<>(), null));
        Assert.assertNotNull(DocumentDescriptionOperations.documentsPlus(null, new HashMap<>()));
    }

    @Test
    public void testExportsPlusExports() {
        DocumentDescription doc1 = new DocumentDescription();
        DocumentDescription doc2 = new DocumentDescription();

        doc1.setExports(null);
        doc2.setExports(Collections.emptyList());
        Assert.assertEquals(0, DocumentDescriptionOperations.documentPlus(doc1, doc2).getExports().size());

        doc1.setExports(Collections.emptyList());
        doc2.setExports(null);
        Assert.assertEquals(0, DocumentDescriptionOperations.documentPlus(doc1, doc2).getExports().size());

        doc1.setExports(createDocumentExportList(Constants.Device.DESKTOP, Constants.Format.JSON, "view"));
        doc2.setExports(null);
        Assert.assertEquals(1, DocumentDescriptionOperations.documentPlus(doc1, doc2).getExports().size());

        doc1.setExports(createDocumentExportList(Constants.Device.DESKTOP, Constants.Format.JSON, "view"));
        doc2.setExports(Collections.emptyList());
        Assert.assertEquals(1, DocumentDescriptionOperations.documentPlus(doc1, doc2).getExports().size());

        doc1.setExports(createDocumentExportList(Constants.Device.DESKTOP, Constants.Format.JSON, "view"));
        doc2.setExports(createDocumentExportList(Constants.Device.DESKTOP, Constants.Format.JSON, "view"));
        Assert.assertEquals(1, DocumentDescriptionOperations.documentPlus(doc1, doc2).getExports().size());

        doc1.setExports(createDocumentExportList(Constants.Device.DESKTOP, Constants.Format.JSON, "view"));
        doc2.setExports(createDocumentExportList(Constants.Device.PHONE, Constants.Format.JSON, "view"));
        Assert.assertEquals(2, DocumentDescriptionOperations.documentPlus(doc1, doc2).getExports().size());

        doc1.setExports(createDocumentExportList(Constants.Device.DESKTOP, Constants.Format.JSON, "view"));
        doc2.setExports(createDocumentExportList(Constants.Device.DESKTOP, Constants.Format.XML, "view"));
        Assert.assertEquals(2, DocumentDescriptionOperations.documentPlus(doc1, doc2).getExports().size());

        doc1.setExports(createDocumentExportList(Constants.Device.DESKTOP, Constants.Format.JSON, "view"));
        doc2.setExports(createDocumentExportList(Constants.Device.DESKTOP, Constants.Format.JSON, "view2"));
        Assert.assertEquals(2, DocumentDescriptionOperations.documentPlus(doc1, doc2).getExports().size());
    }

    @Test
    public void testDocPlusEmptyDoc() {
        DocumentDescription doc1 = new DocumentDescription();
        DocumentDescription doc2 = new DocumentDescription();

        doc1.setType("type");
        doc1.setNamespace("namespace");
        doc1.setLabel("type");
        doc1.setSimilarDomain("type");
        doc1.setRootTemplate("type");
        doc1.setNodeVersions(Collections.singleton(1));

        DocumentDescription result = DocumentDescriptionOperations.documentPlus(doc1, doc2);

        Assert.assertEquals(doc1.getType(), result.getType());
        Assert.assertEquals(doc1.getNamespace(), result.getNamespace());
        Assert.assertEquals(doc1.getLabel(), result.getLabel());
        Assert.assertEquals(doc1.getSimilarDomain(), result.getSimilarDomain());
        Assert.assertEquals(doc1.getRootTemplate(), result.getRootTemplate());
        Assert.assertEquals(doc1.getNodeVersions(), result.getNodeVersions());
    }

    @Test
    public void testEmptyDocPlusDoc() {
        DocumentDescription doc1 = new DocumentDescription();
        DocumentDescription doc2 = new DocumentDescription();

        doc1.setType("type1");

        doc2.setType("type2");
        doc2.setNamespace("namespace");
        doc2.setLabel("type");
        doc2.setSimilarDomain("type");
        doc2.setRootTemplate("type");
        doc2.setNodeVersions(Collections.singleton(1));

        DocumentDescription result = DocumentDescriptionOperations.documentPlus(doc1, doc2);

        Assert.assertEquals(doc1.getType(), result.getType());
        Assert.assertEquals(doc2.getNamespace(), result.getNamespace());
        Assert.assertEquals(doc2.getLabel(), result.getLabel());
        Assert.assertEquals(doc2.getSimilarDomain(), result.getSimilarDomain());
        Assert.assertEquals(doc2.getRootTemplate(), result.getRootTemplate());
        Assert.assertEquals(doc2.getNodeVersions(), result.getNodeVersions());
    }

    private List<DocumentExport> createDocumentExportList(
            Constants.Device device, Constants.Format format, String view
    ) {
        DocumentExport result = new DocumentExport();
        result.setDevice(device);
        result.setFormat(format);
        result.setView(view);
        return Collections.singletonList(result);
    }
}

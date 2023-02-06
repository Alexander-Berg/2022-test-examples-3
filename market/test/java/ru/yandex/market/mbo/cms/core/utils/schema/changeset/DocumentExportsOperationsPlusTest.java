package ru.yandex.market.mbo.cms.core.utils.schema.changeset;

import java.util.Collections;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import ru.yandex.market.mbo.cms.core.models.Constants;
import ru.yandex.market.mbo.cms.core.models.DocumentExport;
import ru.yandex.market.mbo.cms.core.models.KeyTemplate;

public class DocumentExportsOperationsPlusTest {

    @Test(expected = IllegalArgumentException.class)
    public void testNullPlusNull() {
        DocumentExportsOperations.exportPlus(null, null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNullPlusExport() {
        DocumentExportsOperations.exportPlus(null, new DocumentExport());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testExportPlusNull() {
        DocumentExportsOperations.exportPlus(new DocumentExport(), null);
    }

    @Test
    public void testExportsPlus() {
        Assert.assertNull(DocumentExportsOperations.exportsPlus(null, null));

        Assert.assertEquals(
                1,
                DocumentExportsOperations.exportsPlus(
                        createDocumentExportList(Constants.Device.DESKTOP, Constants.Format.JSON, "view"),
                        null
                ).size()
        );

        Assert.assertEquals(
                1,
                DocumentExportsOperations.exportsPlus(
                        null,
                        createDocumentExportList(Constants.Device.DESKTOP, Constants.Format.JSON, "view")
                ).size()
        );

        Assert.assertEquals(
                1,
                DocumentExportsOperations.exportsPlus(
                        Collections.emptyList(),
                        createDocumentExportList(Constants.Device.DESKTOP, Constants.Format.JSON, "view")
                ).size()
        );

        Assert.assertEquals(
                1,
                DocumentExportsOperations.exportsPlus(
                        createDocumentExportList(Constants.Device.DESKTOP, Constants.Format.JSON, "view"),
                        Collections.emptyList()
                ).size()
        );

        Assert.assertEquals(
                1,
                DocumentExportsOperations.exportsPlus(
                        createDocumentExportList(Constants.Device.DESKTOP, Constants.Format.JSON, "view"),
                        createDocumentExportList(Constants.Device.DESKTOP, Constants.Format.JSON, "view")
                ).size()
        );

        Assert.assertEquals(
                2,
                DocumentExportsOperations.exportsPlus(
                        createDocumentExportList(Constants.Device.DESKTOP, Constants.Format.JSON, "view"),
                        createDocumentExportList(Constants.Device.PHONE, Constants.Format.JSON, "view")
                ).size()
        );

        Assert.assertEquals(
                2,
                DocumentExportsOperations.exportsPlus(
                        createDocumentExportList(Constants.Device.DESKTOP, Constants.Format.JSON, "view"),
                        createDocumentExportList(Constants.Device.DESKTOP, Constants.Format.XML, "view")
                ).size()
        );

        Assert.assertEquals(
                2,
                DocumentExportsOperations.exportsPlus(
                        createDocumentExportList(Constants.Device.DESKTOP, Constants.Format.JSON, "view"),
                        createDocumentExportList(Constants.Device.DESKTOP, Constants.Format.JSON, "view2")
                ).size()
        );
    }

    @Test
    public void testEmptyExportPlusEmptyExport() {
        DocumentExport e1 = createDocumentExport(Constants.Device.DESKTOP, Constants.Format.JSON, "view");
        DocumentExport e2 = createDocumentExport(Constants.Device.DESKTOP, Constants.Format.JSON, "view");

        DocumentExport result = DocumentExportsOperations.exportPlus(e1, e2);
        Assert.assertNotNull(result.getDevice());
        Assert.assertEquals(e1.getDevice(), result.getDevice());
        Assert.assertNotNull(result.getFormat());
        Assert.assertEquals(e1.getFormat(), result.getFormat());
        Assert.assertNotNull(result.getView());
        Assert.assertEquals(e1.getView(), result.getView());

        Assert.assertNull(result.getKeyTemplates());
        Assert.assertNull(result.getSimilarKeyTemplates());
        Assert.assertNull(result.getIdentityFields());
        Assert.assertNull(result.getUrlPrefix());
        Assert.assertNull(result.getClient());
        Assert.assertNull(result.getResponsibles());

    }

    @Test
    public void testExportPlusEmptyExport() {
        DocumentExport e1 = createDocumentExport(Constants.Device.DESKTOP, Constants.Format.JSON, "view");
        DocumentExport e2 = createDocumentExport(Constants.Device.DESKTOP, Constants.Format.JSON, "view");

        e1.setKeyTemplates(Collections.singletonList(new KeyTemplate()));
        e1.setSimilarKeyTemplates(Collections.singletonList(new KeyTemplate()));
        e1.setIdentityFields(Collections.singletonList("idfield"));
        e1.setUrlPrefix("urlPrefix");
        e1.setClient("client");
        e1.setResponsibles(Collections.singletonList("resp"));

        DocumentExport result = DocumentExportsOperations.exportPlus(e1, e2);
        Assert.assertNotNull(result.getDevice());
        Assert.assertEquals(e1.getDevice(), result.getDevice());
        Assert.assertNotNull(result.getFormat());
        Assert.assertEquals(e1.getFormat(), result.getFormat());
        Assert.assertNotNull(result.getView());
        Assert.assertEquals(e1.getView(), result.getView());

        Assert.assertNotNull(result.getKeyTemplates());
        Assert.assertEquals(e1.getKeyTemplates(), result.getKeyTemplates());
        Assert.assertNotNull(result.getSimilarKeyTemplates());
        Assert.assertEquals(e1.getSimilarKeyTemplates(), result.getSimilarKeyTemplates());
        Assert.assertNotNull(result.getIdentityFields());
        Assert.assertEquals(e1.getIdentityFields(), result.getIdentityFields());
        Assert.assertNotNull(result.getUrlPrefix());
        Assert.assertEquals(e1.getUrlPrefix(), result.getUrlPrefix());
        Assert.assertNotNull(result.getClient());
        Assert.assertEquals(e1.getClient(), result.getClient());
        Assert.assertNotNull(result.getResponsibles());
        Assert.assertEquals(e1.getResponsibles(), result.getResponsibles());
    }


    @Test
    public void testEmptyExportPlusExport() {
        DocumentExport e1 = createDocumentExport(Constants.Device.DESKTOP, Constants.Format.JSON, "view");
        DocumentExport e2 = createDocumentExport(Constants.Device.DESKTOP, Constants.Format.JSON, "view");

        e2.setKeyTemplates(Collections.singletonList(new KeyTemplate()));
        e2.setSimilarKeyTemplates(Collections.singletonList(new KeyTemplate()));
        e2.setIdentityFields(Collections.singletonList("idfield"));
        e2.setUrlPrefix("urlPrefix");
        e2.setClient("client");
        e2.setResponsibles(Collections.singletonList("resp"));

        DocumentExport result = DocumentExportsOperations.exportPlus(e1, e2);
        Assert.assertNotNull(result.getDevice());
        Assert.assertEquals(e1.getDevice(), result.getDevice());
        Assert.assertNotNull(result.getFormat());
        Assert.assertEquals(e1.getFormat(), result.getFormat());
        Assert.assertNotNull(result.getView());
        Assert.assertEquals(e1.getView(), result.getView());

        Assert.assertNotNull(result.getKeyTemplates());
        Assert.assertEquals(e2.getKeyTemplates(), result.getKeyTemplates());
        Assert.assertNotNull(result.getSimilarKeyTemplates());
        Assert.assertEquals(e2.getSimilarKeyTemplates(), result.getSimilarKeyTemplates());
        Assert.assertNotNull(result.getIdentityFields());
        Assert.assertEquals(e2.getIdentityFields(), result.getIdentityFields());
        Assert.assertNotNull(result.getUrlPrefix());
        Assert.assertEquals(e2.getUrlPrefix(), result.getUrlPrefix());
        Assert.assertNotNull(result.getClient());
        Assert.assertEquals(e2.getClient(), result.getClient());
        Assert.assertNotNull(result.getResponsibles());
        Assert.assertEquals(e2.getResponsibles(), result.getResponsibles());
    }

    @Test
    public void testExportPlusExport() {
        DocumentExport e1 = createDocumentExport(Constants.Device.DESKTOP, Constants.Format.JSON, "view");
        DocumentExport e2 = createDocumentExport(Constants.Device.DESKTOP, Constants.Format.JSON, "view");

        e1.setKeyTemplates(Collections.singletonList(new KeyTemplate()));
        e1.setSimilarKeyTemplates(Collections.singletonList(new KeyTemplate()));
        e1.setIdentityFields(Collections.singletonList("idfield"));
        e1.setUrlPrefix("urlPrefix");
        e1.setClient("client");
        e1.setResponsibles(Collections.singletonList("resp"));

        e2.setKeyTemplates(Collections.emptyList());
        e2.setSimilarKeyTemplates(Collections.emptyList());
        e2.setIdentityFields(Collections.singletonList("idfield2"));
        e2.setUrlPrefix("urlPrefix2");
        e2.setClient("client2");
        e2.setResponsibles(Collections.emptyList());

        DocumentExport result = DocumentExportsOperations.exportPlus(e1, e2);
        Assert.assertNotNull(result.getDevice());
        Assert.assertEquals(e1.getDevice(), result.getDevice());
        Assert.assertNotNull(result.getFormat());
        Assert.assertEquals(e1.getFormat(), result.getFormat());
        Assert.assertNotNull(result.getView());
        Assert.assertEquals(e1.getView(), result.getView());
        Assert.assertNotNull(result.getKeyTemplates());
        Assert.assertEquals(e1.getKeyTemplates(), result.getKeyTemplates());
        Assert.assertNotNull(result.getSimilarKeyTemplates());
        Assert.assertEquals(e1.getSimilarKeyTemplates(), result.getSimilarKeyTemplates());
        Assert.assertNotNull(result.getIdentityFields());
        Assert.assertEquals(e2.getIdentityFields(), result.getIdentityFields());
        Assert.assertNotNull(result.getUrlPrefix());
        Assert.assertEquals(e2.getUrlPrefix(), result.getUrlPrefix());
        Assert.assertNotNull(result.getClient());
        Assert.assertEquals(e2.getClient(), result.getClient());
        Assert.assertNotNull(result.getResponsibles());
        Assert.assertEquals(e2.getResponsibles(), result.getResponsibles());
    }

    private List<DocumentExport> createDocumentExportList(
            Constants.Device device, Constants.Format format, String view
    ) {
        return Collections.singletonList(createDocumentExport(device, format, view));
    }

    private DocumentExport createDocumentExport(
            Constants.Device device, Constants.Format format, String view
    ) {
        DocumentExport result = new DocumentExport();
        result.setDevice(device);
        result.setFormat(format);
        result.setView(view);
        return result;
    }
}

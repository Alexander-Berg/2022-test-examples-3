package ru.yandex.market.mbo.cms.core.utils.schema.changeset;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import ru.yandex.market.mbo.cms.core.models.Constants;
import ru.yandex.market.mbo.cms.core.models.DocumentExport;
import ru.yandex.market.mbo.cms.core.models.KeyTemplate;

public class DocumentExportsOperationsMinusTest {

    @Test(expected = IllegalArgumentException.class)
    public void testNullMinusExports() {
        DocumentExportsOperations.exportMinus(
                null,
                new DocumentExport()
        );
    }

    @Test(expected = IllegalArgumentException.class)
    public void testExportsMinusNull() {
        DocumentExportsOperations.exportMinus(
                new DocumentExport(),
                null
        );
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNullMinusNull() {
        DocumentExportsOperations.exportMinus(null, null);
    }

    @Test
    public void testExportsMinus() {
        Assert.assertNull(DocumentExportsOperations.exportsMinus(null, null));

        Assert.assertEquals(
                1,
                DocumentExportsOperations.exportsMinus(
                        createDocumentExportList(Constants.Device.DESKTOP, Constants.Format.JSON, "view"),
                        null
                ).size()
        );

        Assert.assertNull(
                DocumentExportsOperations.exportsMinus(
                        null,
                        createDocumentExportList(Constants.Device.DESKTOP, Constants.Format.JSON, "view")
                )
        );

        Assert.assertEquals(
                0,
                DocumentExportsOperations.exportsMinus(
                        Collections.emptyList(),
                        createDocumentExportList(Constants.Device.DESKTOP, Constants.Format.JSON, "view")
                ).size()
        );

        Assert.assertEquals(
                1,
                DocumentExportsOperations.exportsMinus(
                        createDocumentExportList(Constants.Device.DESKTOP, Constants.Format.JSON, "view"),
                        Collections.emptyList()
                ).size()
        );

        Assert.assertEquals(
                0,
                DocumentExportsOperations.exportsMinus(
                        createDocumentExportList(Constants.Device.DESKTOP, Constants.Format.JSON, "view"),
                        createDocumentExportList(Constants.Device.DESKTOP, Constants.Format.JSON, "view")
                ).size()
        );

        Assert.assertEquals(
                1,
                DocumentExportsOperations.exportsMinus(
                        createDocumentExportList(Constants.Device.DESKTOP, Constants.Format.JSON, "view"),
                        createDocumentExportList(Constants.Device.PHONE, Constants.Format.JSON, "view")
                ).size()
        );

        Assert.assertEquals(
                1,
                DocumentExportsOperations.exportsMinus(
                        createDocumentExportList(Constants.Device.DESKTOP, Constants.Format.JSON, "view"),
                        createDocumentExportList(Constants.Device.DESKTOP, Constants.Format.XML, "view")
                ).size()
        );

        Assert.assertEquals(
                1,
                DocumentExportsOperations.exportsMinus(
                        createDocumentExportList(Constants.Device.DESKTOP, Constants.Format.JSON, "view"),
                        createDocumentExportList(Constants.Device.DESKTOP, Constants.Format.JSON, "view2")
                ).size()
        );
    }

    @Test
    public void testExportsKeyTemplatesMinus() {
        List<String> key1 = new ArrayList<>();
        key1.add("f1");
        key1.add("f2");
        key1.add("f3");

        List<String> key2 = new ArrayList<>();
        key2.add("f2");
        key2.add("f3");
        key2.add("f4");

        KeyTemplate kt1 = new KeyTemplate(key1, false, false, "g1");
        KeyTemplate kt2 = new KeyTemplate(key2, true, true, "g2");

        Assert.assertEquals(
                0,
                DocumentExportsOperations.exportsMinus(
                        createDocumentExportList(
                                Constants.Device.DESKTOP,
                                Constants.Format.JSON,
                                "view",
                                Collections.singletonList(kt1),
                                Collections.singletonList(kt2)
                        ),
                        createDocumentExportList(Constants.Device.DESKTOP, Constants.Format.JSON, "view")
                ).size()
        );

        Assert.assertEquals(
                0,
                DocumentExportsOperations.exportsMinus(
                        createDocumentExportList(
                                Constants.Device.DESKTOP,
                                Constants.Format.JSON,
                                "view",
                                Collections.singletonList(kt1),
                                null
                        ),
                        createDocumentExportList(Constants.Device.DESKTOP, Constants.Format.JSON, "view")
                ).size()
        );

        Assert.assertEquals(
                0,
                DocumentExportsOperations.exportsMinus(
                        createDocumentExportList(
                                Constants.Device.DESKTOP,
                                Constants.Format.JSON,
                                "view",
                                null,
                                Collections.singletonList(kt1)
                        ),
                        createDocumentExportList(Constants.Device.DESKTOP, Constants.Format.JSON, "view")
                ).size()
        );

        Assert.assertEquals(
                1,
                DocumentExportsOperations.exportsMinus(
                        createDocumentExportList(
                                Constants.Device.DESKTOP,
                                Constants.Format.JSON,
                                "view",
                                Collections.singletonList(kt1),
                                Collections.singletonList(kt2)
                        ),
                        createDocumentExportList(
                                Constants.Device.DESKTOP,
                                Constants.Format.JSON,
                                "view",
                                Collections.singletonList(kt1),
                                Collections.singletonList(kt2)
                        )
                ).size()
        );

        Assert.assertEquals(
                1,
                DocumentExportsOperations.exportsMinus(
                        createDocumentExportList(
                                Constants.Device.DESKTOP,
                                Constants.Format.JSON,
                                "view",
                                Collections.singletonList(kt1),
                                Collections.singletonList(kt2)
                        ),
                        createDocumentExportList(
                                Constants.Device.DESKTOP,
                                Constants.Format.JSON,
                                "view",
                                Collections.singletonList(kt2),
                                Collections.singletonList(kt1)
                        )
                ).size()
        );

        Assert.assertEquals(
                1,
                DocumentExportsOperations.exportsMinus(
                        createDocumentExportList(
                                Constants.Device.DESKTOP,
                                Constants.Format.JSON,
                                "view",
                                Collections.singletonList(kt1),
                                Collections.singletonList(kt2)
                        ),
                        createDocumentExportList(
                                Constants.Device.DESKTOP,
                                Constants.Format.JSON,
                                "view",
                                Collections.singletonList(kt2),
                                null
                        )
                ).size()
        );

        Assert.assertEquals(
                1,
                DocumentExportsOperations.exportsMinus(
                        createDocumentExportList(
                                Constants.Device.DESKTOP,
                                Constants.Format.JSON,
                                "view",
                                Collections.singletonList(kt1),
                                Collections.singletonList(kt2)
                        ),
                        createDocumentExportList(
                                Constants.Device.DESKTOP,
                                Constants.Format.JSON,
                                "view",
                                null,
                                Collections.singletonList(kt1)
                        )
                ).size()
        );
    }

    private List<DocumentExport> createDocumentExportList(
            Constants.Device device, Constants.Format format, String view
    ) {
        return createDocumentExportList(device, format, view, null, null);
    }

    private List<DocumentExport> createDocumentExportList(
            Constants.Device device,
            Constants.Format format,
            String view,
            List<KeyTemplate> keyTemplates,
            List<KeyTemplate> similarKeyTemplates
    ) {
        return Collections.singletonList(createDocumentExport(device, format, view, keyTemplates, similarKeyTemplates));
    }

    private DocumentExport createDocumentExport(
            Constants.Device device, Constants.Format format, String view
    ) {
        return createDocumentExport(device, format, view, null, null);
    }

    private DocumentExport createDocumentExport(
            Constants.Device device,
            Constants.Format format,
            String view,
            List<KeyTemplate> keyTemplates,
            List<KeyTemplate> similarKeyTemplates
    ) {
        DocumentExport result = new DocumentExport();
        result.setDevice(device);
        result.setFormat(format);
        result.setView(view);
        result.setKeyTemplates(keyTemplates);
        result.setSimilarKeyTemplates(similarKeyTemplates);
        return result;
    }

}

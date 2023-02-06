package ru.yandex.market.mbo.cms.api.servlets.export.util.changeset;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.junit.Assert;

import ru.yandex.market.mbo.cms.core.models.Constants;
import ru.yandex.market.mbo.cms.core.models.DocumentExport;

public class DocumentExportEqualsTestHelper {

    private DocumentExportEqualsTestHelper() {
    }

    private static void assertEquals(DocumentExport e1, DocumentExport e2) {
        Assert.assertEquals(e1.getIdentityFields(), e2.getIdentityFields());
        Assert.assertEquals(e1.getUrlPrefix(), e2.getUrlPrefix());
        Assert.assertEquals(e1.getClient(), e2.getClient());
        Assert.assertEquals(e1.getResponsibles(), e2.getResponsibles());
        KeyTemplateEqualsTestHelper.assertEquals(e1.getKeyTemplates(), e2.getKeyTemplates());
        KeyTemplateEqualsTestHelper.assertEquals(e1.getSimilarKeyTemplates(), e2.getSimilarKeyTemplates());
    }

    public static void assertEquals(
            Map<DocumentExportKey, DocumentExport> e1, Map<DocumentExportKey, DocumentExport> e2
    ) {
        Assert.assertEquals(e1.keySet(), e2.keySet());
        for (Map.Entry<DocumentExportKey, DocumentExport> entry : e1.entrySet()) {
            assertEquals(entry.getValue(), e2.get(entry.getKey()));
        }
    }

    public static void assertEquals(List<DocumentExport> e1, List<DocumentExport> e2) {
        if (e1 == null && e2 == null) {
            return;
        }
        Assert.assertTrue(e1 != null && e2 != null);
        Assert.assertEquals(e1.size(), e2.size());
        assertEquals(exportsToMap(e1), exportsToMap(e2));
    }

    private static Map<DocumentExportKey, DocumentExport> exportsToMap(List<DocumentExport> documentExports) {
        if (documentExports == null || documentExports.isEmpty()) {
            return new HashMap<>();
        }

        Map<DocumentExportKey, DocumentExport> result = new HashMap<>();
        documentExports.forEach(doc -> {
            DocumentExportKey key = new DocumentExportKey(doc.getDevice(), doc.getFormat(), doc.getView());
            result.put(key, doc);
        });
        return result;
    }

    private static class DocumentExportKey {
        private Constants.Device device;
        private Constants.Format format;
        private String view;

        private DocumentExportKey() {
        }

        DocumentExportKey(Constants.Device device, Constants.Format format, String view) {
            this.device = device;
            this.format = format;
            this.view = view;
        }

        public Constants.Device getDevice() {
            return device;
        }

        public Constants.Format getFormat() {
            return format;
        }

        public String getView() {
            return view;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            DocumentExportKey that = (DocumentExportKey) o;
            return device == that.device &&
                    format == that.format &&
                    Objects.equals(view, that.view);
        }

        @Override
        public int hashCode() {
            return Objects.hash(device, format, view);
        }
    }
}

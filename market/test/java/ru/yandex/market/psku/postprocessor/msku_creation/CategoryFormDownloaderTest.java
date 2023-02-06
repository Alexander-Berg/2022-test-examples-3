package ru.yandex.market.psku.postprocessor.msku_creation;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@Ignore // manual test to check form with no knowledge
public class CategoryFormDownloaderTest {
    private static final long EMPTY_FORM = 18118780L;
    private static final long GOOD_FORM = 7811911L;

    private CategoryFormDownloader categoryFormDownloader;

    @Before
    public void init() {
        categoryFormDownloader = new CategoryFormDownloader("http://mbo-http-exporter.tst.vs.market.yandex.net:8084");
    }

    @Test
    public void shouldDownloadForm() {
        byte[] form = categoryFormDownloader.downloadForm(GOOD_FORM);
        assertTrue(form.length > 0);
    }

    @Test
    public void shouldDownloadEmptyForm() {
        byte[] form = categoryFormDownloader.downloadForm(EMPTY_FORM);
        assertEquals(0, form.length);
    }
}

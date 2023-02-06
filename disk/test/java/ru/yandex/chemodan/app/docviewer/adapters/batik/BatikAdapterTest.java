package ru.yandex.chemodan.app.docviewer.adapters.batik;

import org.apache.batik.ext.awt.image.spi.ImageWriter;
import org.apache.batik.ext.awt.image.spi.ImageWriterRegistry;
import org.junit.Test;

import ru.yandex.misc.test.Assert;

public class BatikAdapterTest {

    @Test
    public void testPngWriter() {
        ImageWriter imageWriter = ImageWriterRegistry.getInstance().getWriterFor("image/png");
        Assert.notNull(imageWriter);
    }
}

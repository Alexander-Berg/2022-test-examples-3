package ru.yandex.chemodan.app.docviewer.adapters.javapsdlibrary;

import java.awt.image.BufferedImage;

import javax.imageio.ImageIO;

import org.junit.Test;

import ru.yandex.chemodan.app.docviewer.TestResources;
import ru.yandex.commune.image.psd.PsdAdapter;
import ru.yandex.misc.io.ClassPathResourceInputStreamSource;
import ru.yandex.misc.io.file.File2;
import ru.yandex.misc.test.Assert;

/**
 * @author ssytnik
 */
public class PsdAdapterTest {

    @Test
    public void vsnNew01() throws Exception {
        BufferedImage image = PsdAdapter.convertToImage(
                new ClassPathResourceInputStreamSource(TestResources.class, "test/psd/vsn_new_01.psd"));
        Assert.equals(1000, image.getWidth());
        Assert.equals(747, image.getHeight());

        File2 resultFile = new File2("result.png");
        try {
            ImageIO.write(image, "png", resultFile.getFile());
            Assert.gt(resultFile.length(), 0L);
        } finally {
            resultFile.deleteIfExists();
        }
    }

}

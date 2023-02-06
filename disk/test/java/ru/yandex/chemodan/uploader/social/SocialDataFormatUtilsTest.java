package ru.yandex.chemodan.uploader.social;

import org.junit.Test;

import ru.yandex.commune.image.ImageFormat;
import ru.yandex.misc.io.InputStreamSource;
import ru.yandex.misc.io.cl.ClassLoaderUtils;
import ru.yandex.misc.test.Assert;

/**
 * @author nshmakov
 */
public class SocialDataFormatUtilsTest {

    @Test
    public void jpeg() throws Exception {
        InputStreamSource jpeg = ClassLoaderUtils.streamSourceForResource(
                "ru/yandex/chemodan/uploader/preview/cat.jpg");
        ImageFormat imageFormat = SocialDataFormatUtils.detectAndValidateDataFormat(jpeg);
        Assert.equals(ImageFormat.JPEG, imageFormat);
    }

    @Test
    public void png() {
        InputStreamSource png = ClassLoaderUtils.streamSourceForResource(
                "ru/yandex/chemodan/uploader/preview/cubs.png");
        ImageFormat imageFormat = SocialDataFormatUtils.detectAndValidateDataFormat(png);
        Assert.equals(ImageFormat.PNG, imageFormat);
    }

    @Test(expected = IllegalArgumentException.class)
    public void svg() {
        InputStreamSource svg = ClassLoaderUtils.streamSourceForResource(
                "ru/yandex/chemodan/uploader/preview/SVG-logo.svg");
        SocialDataFormatUtils.detectAndValidateDataFormat(svg);
    }
}

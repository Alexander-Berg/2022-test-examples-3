package ru.yandex.direct.core.entity.image;

import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.Collection;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.direct.core.entity.image.service.ImageUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.core.entity.image.service.ImageConstants.GIF_MIME_TYPE;
import static ru.yandex.direct.core.entity.image.service.ImageConstants.JPG_MIME_TYPE;
import static ru.yandex.direct.core.entity.image.service.ImageConstants.PNG_MIME_TYPE;
import static ru.yandex.direct.core.entity.image.service.ImageConstants.SVG_MIME_TYPE;
import static ru.yandex.direct.core.testing.data.TestImages.generateBlankGifImageData;
import static ru.yandex.direct.core.testing.data.TestImages.generateBlankImageData;
import static ru.yandex.direct.core.testing.data.TestImages.generateBlankSvgImageDate;
import static ru.yandex.direct.core.testing.data.TestImages.generateBlankSvgImageDateWithoutLeadingXmlTag;

@RunWith(Parameterized.class)
public class ImageUtilsMimeTypeTest {
    @Parameterized.Parameter()
    public String description;

    @Parameterized.Parameter(1)
    public String expectedMimeType;

    @Parameterized.Parameter(2)
    public byte[] imageBytes;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> getParameters() {
        return Arrays.asList(new Object[][]{
                {"gif", GIF_MIME_TYPE, generateBlankGifImageData(100, 200)},
                {"jpeg", JPG_MIME_TYPE, generateBlankImageData(100, 200, "jpg", BufferedImage.TYPE_INT_RGB)},
                {"png", PNG_MIME_TYPE, generateBlankImageData(100, 200, "png", BufferedImage.TYPE_INT_RGB)},
                {"svg", SVG_MIME_TYPE, generateBlankSvgImageDate(100, 200)},
                {"svg without xml", SVG_MIME_TYPE, generateBlankSvgImageDateWithoutLeadingXmlTag(100, 200)},
                {"svg without anything", SVG_MIME_TYPE, "<svg/>".getBytes()},

                // следующие проверки нужны здесь только для фиксации поведения функции
                // указанные MIME типы не используется в коде, поэтому могут быть изменены, если нужно
                {"bmp", "image/bmp", generateBlankImageData(100, 200, "bmp", BufferedImage.TYPE_INT_RGB)},
                {"text", "text/plain", "some useless text string".getBytes()},
                {"empty", "application/octet-stream", new byte[0]},
                {"unknown", "application/octet-stream", new byte[]{0x11, 0x22}}
        });
    }

    @Test
    public void checkMimeTypeDetection() {
        String mimeType = ImageUtils.getMimeType(imageBytes);
        assertThat(mimeType).as(description).isEqualTo(expectedMimeType);
    }
}

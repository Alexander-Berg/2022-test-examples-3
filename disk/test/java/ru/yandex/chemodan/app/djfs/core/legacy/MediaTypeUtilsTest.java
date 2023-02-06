package ru.yandex.chemodan.app.djfs.core.legacy;

import org.junit.Test;

import ru.yandex.bolts.collection.Option;
import ru.yandex.chemodan.app.djfs.core.filesystem.model.MediaType;
import ru.yandex.chemodan.app.djfs.core.legacy.formatting.mediatype.MediaTypeUtils;
import ru.yandex.misc.test.Assert;

/**
 * @author yashunsky
 */
public class MediaTypeUtilsTest {
    @Test
    public void byExtension() {
        Assert.some(MediaType.IMAGE, MediaTypeUtils.getMediaTypeByExtension("jpg"));
    }

    @Test
    public void byMimeType() {
        Assert.some(MediaType.DOCUMENT, MediaTypeUtils.getMediaTypeByMimeType(
                "application/vnd.openxmlformats-officedocument.presentationml.presentation"));
    }

    @Test
    public void testOrder() {
        Assert.some(MediaType.DATA, MediaTypeUtils.getMediaTypeByMimeType("application/octet-stream"));
    }

    @Test
    public void testXmlMimeType() {
        Assert.some(MediaType.DATA,
                MediaTypeUtils.getMediaTypeByExtensionOrMimeType("aae", Option.of("text/xml")));
    }
}

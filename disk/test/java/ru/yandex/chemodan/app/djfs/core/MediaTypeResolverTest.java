package ru.yandex.chemodan.app.djfs.core;

import org.junit.Test;

import ru.yandex.bolts.collection.Option;
import ru.yandex.chemodan.app.djfs.core.filesystem.model.DjfsResourcePath;
import ru.yandex.chemodan.app.djfs.core.filesystem.model.MediaType;
import ru.yandex.misc.test.Assert;

public class MediaTypeResolverTest {

    @Test
    public void getMediaTypeExtensionPresentedButUnknownMimetype() {
        DjfsResourcePath path = DjfsResourcePath.cons("1234", "/disk/text.txt");
        Option<MediaType> mediaTypeO = MediaTypeResolver.resolve(path, Option.of("any"));
        Assert.some(MediaType.DOCUMENT, mediaTypeO);
    }

    @Test
    public void getMediaTypeExtensionMissingButKnownMimetype() {
        DjfsResourcePath path = DjfsResourcePath.cons("1234", "/disk/text");
        Option<MediaType> mediaTypeO = MediaTypeResolver.resolve(path, Option.of("application/vnd.oasis.opendocument.text"));
        Assert.some(MediaType.DOCUMENT, mediaTypeO);
    }

    @Test
    public void getMediaTypeExtensionPresentedAndKnownMimetype() {
        DjfsResourcePath path = DjfsResourcePath.cons("1234", "/disk/text.txt");
        Option<MediaType> mediaTypeO = MediaTypeResolver.resolve(path, Option.of("application/vnd.oasis.opendocument.text"));
        Assert.some(MediaType.DOCUMENT, mediaTypeO);
    }

    @Test
    public void emptyMediaTypeForExtensionMissingUnknownMimetype() {
        DjfsResourcePath path = DjfsResourcePath.cons("1234", "/disk/text");
        Option<MediaType> mediaTypeO = MediaTypeResolver.resolve(path, Option.of("any"));
        Assert.isEmpty(mediaTypeO);
    }

    @Test
    public void getMediaTypeExtensionPresentedButEmptyMimetype() {
        DjfsResourcePath path = DjfsResourcePath.cons("1234", "/disk/text.txt");
        Option<MediaType> mediaTypeO = MediaTypeResolver.resolve(path, Option.empty());
        Assert.some(MediaType.DOCUMENT, mediaTypeO);
    }

    @Test
    public void emptyMediaTypeForExtensionMissingEmptyMimetype() {
        DjfsResourcePath path = DjfsResourcePath.cons("1234", "/disk/text");
        Option<MediaType> mediaTypeO = MediaTypeResolver.resolve(path, Option.empty());
        Assert.isEmpty(mediaTypeO);
    }
}

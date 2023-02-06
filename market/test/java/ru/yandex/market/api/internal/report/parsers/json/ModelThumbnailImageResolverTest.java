package ru.yandex.market.api.internal.report.parsers.json;

import org.junit.Assert;
import org.junit.Test;
import ru.yandex.market.api.domain.v2.Image;
import ru.yandex.market.api.integration.BaseTest;

import java.util.Optional;

public class ModelThumbnailImageResolverTest extends BaseTest  {

    @Test
    public void imageFromAvatars_noThumbInfo_waitNotResolved() {
        assertNotResolved("//avatars.mdst.yandex.net/mpic/603/b0815221113_img_id3409227122925646746/orig");
    }

    @Test
    public void imageFromAvatars_thumbSizeEqualToOne_waitUseOriginalImage() {
        assertResolved("//avatars.mdst.yandex.net/mpic/603/b0815221113_img_id3409227122925646746/1",
            "https://avatars.mdst.yandex.net/mpic/603/b0815221113_img_id3409227122925646746/orig");
    }

    @Test
    public void imageFromAvatars_urlEndsWithSizeNotEqualToOne_waitNotResolved() {
        for (int i = 2; i <= 9; ++i) {
            assertNotResolved(String.format("//avatars.mds.yandex.net/mpic/603/b0815221113_img_id3409227122925646746/%d", i));
        }
    }

    @Test
    public void imageFromMdata_noSizeParameter_waitNotResolved() {
        assertNotResolved(String.format("//mdata.yandex.net/i?path=b1209182145_img_id3039823263039513208.jpeg"));
    }

    @Test
    public void imageFromMdata_unknownHost_urlEndsWithSizeEqualToOne_waitNotResolved() {
        assertNotResolved(String.format("//unknown.yandex.net/i?path=b1209182145_img_id3039823263039513208.jpeg&size=1"));
    }

    @Test
    public void imageFromMdata_urlEndsWithSizeEqualToOne_WaitUseOriginalImage_sizeParamIsRemoved() {
        assertResolved("//mdata.yandex.net/i?path=b1209182145_img_id3039823263039513208.jpeg&size=1",
            "https://mdata.yandex.net/i?path=b1209182145_img_id3039823263039513208.jpeg");
    }

    @Test
    public void imageFromMdata_urlEndsWithSizeNotEqualToOne_waitNotResolved() {
        for (int i = 2; i <= 9; ++i) {
            assertNotResolved(String.format("//mdata.yandex.net/i?path=b1209182145_img_id3039823263039513208.jpeg&size=%d", i));
        }
    }

    private void assertNotResolved(String url) {
        Assert.assertFalse(ModelThumbnailImageResolver.tryResolve(url).isPresent());
    }

    private void assertResolved(String url, String expectedUrl) {
        Optional<Image> image = ModelThumbnailImageResolver.tryResolve(url);
        Assert.assertTrue(image.isPresent());
        Assert.assertEquals(expectedUrl, image.get().getUrl());
    }
}

package ru.yandex.market.cleanweb.client;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;


public class ImageVerdictTest {

    @Test
    public void fromRawVerdict() {
        CWRawVerdict cwRawVerdict = new CWRawVerdict("some-key",
            "HAS_TEXT",
            "true",
            "some-source",
            "some-subsource",
            "some-entity");
        ImageVerdict imageVerdict = ImageVerdict.fromRawVerdict(cwRawVerdict);

        assertThat(imageVerdict).isEqualTo(ImageVerdict.HAS_TEXT);
    }

    @Test
    public void fromRawCleanVerdict() {
        CWRawVerdict cwRawVerdict = new CWRawVerdict(
                "some-key",
                "watermark_clean",
                "true",
                "some-source",
                "watermark_image_toloka",
                "some-entity"
        );
        ImageVerdict imageVerdict = ImageVerdict.fromRawVerdict(cwRawVerdict);

        assertThat(imageVerdict).isEqualTo(ImageVerdict.WATERMARK_CLEAN);
    }

    @Test
    public void fromRawHasWatermarkVerdict() {
        CWRawVerdict cwRawVerdict = new CWRawVerdict(
                "some-key",
                "has_watermark",
                "true",
                "some-source",
                "watermark_image_toloka",
                "some-entity"
        );
        ImageVerdict imageVerdict = ImageVerdict.fromRawVerdict(cwRawVerdict);

        assertThat(imageVerdict).isEqualTo(ImageVerdict.HAS_WATERMARK);
    }

    @Test
    public void fashionTest() {
        CWRawVerdict cwRawVerdict = new CWRawVerdict(
                "1106767_22477824_0",
                "fashion_background_good",
                "true",
                "clean-web",
                "fashion_background_toloka",
                "image"
        );
        ImageVerdict imageVerdict = ImageVerdict.fromRawVerdict(cwRawVerdict);

        assertThat(imageVerdict).isEqualTo(ImageVerdict.FASHION_BACKGROUND_GOOD);
    }

    @Test
    public void shoesTest() {
        CWRawVerdict cwRawVerdict = new CWRawVerdict(
            "1106767_22477824_0",
            "shoes_orientation_right",
            "true",
            "clean-web",
            "shoes_background_toloka",
            "image"
        );
        ImageVerdict imageVerdict = ImageVerdict.fromRawVerdict(cwRawVerdict);

        assertThat(imageVerdict).isEqualTo(ImageVerdict.SHOES_ORIENTATION_RIGHT);
    }
}
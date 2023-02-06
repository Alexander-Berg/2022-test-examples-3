package ru.yandex.direct.core.entity.image.converter;

import java.util.Collections;
import java.util.Map;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import org.apache.commons.lang3.RandomStringUtils;
import org.assertj.core.api.SoftAssertions;
import org.junit.Test;

import ru.yandex.direct.avatars.client.model.AvatarInfo;
import ru.yandex.direct.avatars.client.model.answer.ImageSize;
import ru.yandex.direct.core.entity.image.container.BannerImageType;
import ru.yandex.direct.core.entity.image.model.BannerImageFormat;
import ru.yandex.direct.core.entity.image.model.ImageMdsMeta;
import ru.yandex.direct.core.entity.image.model.ImageSizeMeta;
import ru.yandex.direct.core.entity.image.model.ImageSmartCenter;
import ru.yandex.direct.dbschema.ppc.enums.BannerImagesFormatsAvatarsHost;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.core.entity.image.service.ImageConstants.SUPPORTED_IMAGE_AD_FORMATS;
import static ru.yandex.direct.core.entity.image.service.ImageConstants.SUPPORTED_REGULAR_FORMATS;
import static ru.yandex.direct.core.entity.image.service.ImageConstants.SUPPORTED_WIDE_FORMATS;

public class BannerImageConverterTest {

    private static final String HOST = BannerImagesFormatsAvatarsHost.avatars_mdst_yandex_net.getLiteral();
    private static final String HASH = RandomStringUtils.random(22);
    public static final String PATH = RandomStringUtils.randomAlphabetic(10);

    @Test
    public void toBannerImageFormat_TextBannerUnsupportedFormat_Filtered() {
        String unsupportedFormat = "wx1081";
        BannerImageFormat imageFormat = toBannerImageFormatWithSpecialFormatName(unsupportedFormat, BannerImageType.BANNER_TEXT);
        assertThat(imageFormat.getFormats()).isEmpty();
    }

    @Test
    public void toBannerImageFormat_TextBannerFormatOfOtherImageType_Filtered() {
        String formatOfOtherImageType = Iterables.getLast(SUPPORTED_WIDE_FORMATS);
        BannerImageFormat imageFormat =
                toBannerImageFormatWithSpecialFormatName(formatOfOtherImageType, BannerImageType.BANNER_TEXT);
        assertThat(imageFormat.getFormats()).isEmpty();
    }

    @Test
    public void toBannerImageFormat_TextBannerSupportedFormat_NotFiltered() {
        String supportedFormat = Iterables.getLast(SUPPORTED_REGULAR_FORMATS);
        BannerImageFormat imageFormat = toBannerImageFormatWithSpecialFormatName(supportedFormat, BannerImageType.BANNER_TEXT);
        assertThat(imageFormat.getFormats()).containsKey(supportedFormat);
        assertThat(imageFormat.getFormats().get(supportedFormat).getPath()).isNull();
    }

    @Test
    public void toBannerImageFormat_ImageBannerFormatOfOtherImageTyp_Filtered() {
        String formatOfOtherImageType = Iterables.getLast(SUPPORTED_REGULAR_FORMATS);
        BannerImageFormat imageFormat =
                toBannerImageFormatWithSpecialFormatName(formatOfOtherImageType, BannerImageType.BANNER_IMAGE_AD);
        assertThat(imageFormat.getFormats()).isEmpty();
    }

    @Test
    public void mergeImageMdsMeta_success() {
        String sizeToChange = "x150";
        String immutableSize = "x300";

        ImageSmartCenter smartCenter = new ImageSmartCenter()
                .withHeight(1)
                .withWidth(1)
                .withX(0)
                .withY(0);
        ImageMdsMeta imageMdsMeta = new ImageMdsMeta()
                .withSizes(ImmutableMap.of(
                        sizeToChange, createImageSizeMeta(smartCenter),
                        immutableSize, createImageSizeMeta(smartCenter)));

        ImageSmartCenter overriddenSmartCenter = new ImageSmartCenter()
                .withHeight(1)
                .withWidth(1)
                .withX(100)
                .withY(100);
        ImageMdsMeta overriddenMdsMeta = new ImageMdsMeta()
                .withSizes(ImmutableMap.of(
                        sizeToChange, createImageSizeMeta(overriddenSmartCenter)));

        ImageMdsMeta newMdsMeta = BannerImageConverter.mergeImageMdsMeta(imageMdsMeta, overriddenMdsMeta);

        SoftAssertions.assertSoftly(sa -> {
            Map<String, ImageSizeMeta> sizes = newMdsMeta.getSizes();
            sa.assertThat(sizes.get(sizeToChange)).isNotNull();
            sa.assertThat(sizes.get(immutableSize)).isNotNull();

            sa.assertThat(sizes.get(sizeToChange))
                    .describedAs("Смарт-центр для размера %s должен быть изменен", sizeToChange)
                    .isEqualTo(overriddenMdsMeta.getSizes().get(sizeToChange));

            sa.assertThat(sizes.get(immutableSize))
                    .describedAs("Смарт-центр для размера %s должен остаться без изменений", immutableSize)
                    .isEqualTo(imageMdsMeta.getSizes().get(immutableSize));
        });
    }

    @Test
    public void mergeImageMdsMeta_nullImageMdsMeta() {
        ImageMdsMeta overriddenMdsMeta = new ImageMdsMeta()
                .withSizes(ImmutableMap.of(
                        "x150", createImageSizeMeta(new ImageSmartCenter())));
        ImageMdsMeta newMdsMeta = BannerImageConverter.mergeImageMdsMeta(null, overriddenMdsMeta);
        assertThat(newMdsMeta).isNull();
    }

    @Test
    public void mergeImageMdsMeta_nullOverriddenMdsMeta() {
        ImageMdsMeta imageMdsMeta = new ImageMdsMeta()
                .withSizes(ImmutableMap.of(
                        "x150", createImageSizeMeta(new ImageSmartCenter())));
        ImageMdsMeta newMdsMeta = BannerImageConverter.mergeImageMdsMeta(imageMdsMeta, null);
        assertThat(newMdsMeta)
                .describedAs("Ожидается исходное значение imageMdsMeta")
                .isEqualTo(imageMdsMeta);
    }

    private ImageSizeMeta createImageSizeMeta(ImageSmartCenter smartCenter) {
        return new ImageSizeMeta()
                .withHeight(10)
                .withWidth(10)
                .withPath("path")
                .withSmartCenters(ImmutableMap.of("1:1", smartCenter));
    }

    @Test
    public void toBannerImageFormat_ImageBannerSupportedFormat_NotFiltered() {
        String supportedFormat = Iterables.getLast(SUPPORTED_IMAGE_AD_FORMATS);
        BannerImageFormat imageFormat = toBannerImageFormatWithSpecialFormatName(supportedFormat, BannerImageType.BANNER_IMAGE_AD);
        assertThat(imageFormat.getFormats()).containsKey(supportedFormat);
        assertThat(imageFormat.getFormats().get(supportedFormat).getPath()).isNotNull();
    }

    private static BannerImageFormat toBannerImageFormatWithSpecialFormatName(String format, BannerImageType bannerType) {
        Map<String, ImageSize> sizes = Collections.singletonMap(format, new ImageSize().withPath(PATH));
        AvatarInfo avatarInfo = new AvatarInfo("direct", 1, "key", "meta", sizes);
        ru.yandex.direct.core.entity.banner.model.ImageSize imageSizeForBanner =
                new ru.yandex.direct.core.entity.banner.model.ImageSize()
                        .withWidth(500)
                        .withHeight(500);
        return BannerImageConverter.toBannerImageFormat(imageSizeForBanner,
                bannerType, HOST, HASH, avatarInfo);
    }
}

package ru.yandex.direct.grid.processing.service.client.converter;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.annotation.ParametersAreNonnullByDefault;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import org.junit.Test;

import ru.yandex.direct.core.entity.banner.model.ImageType;
import ru.yandex.direct.core.entity.image.model.ImageMdsMeta;
import ru.yandex.direct.core.entity.image.model.ImageSizeMeta;
import ru.yandex.direct.grid.processing.model.cliententity.image.GdImageFormat;
import ru.yandex.direct.grid.processing.util.ImageTestDataUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.direct.core.entity.image.service.ImageConstants.SUPPORTED_REGULAR_FORMATS;
import static ru.yandex.direct.core.entity.image.service.ImageConstants.SUPPORTED_WIDE_FORMATS;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;

@ParametersAreNonnullByDefault
public class ToSupportedGdImageFormatsTest {

    @Test
    public void toSupportedGdImageFormatsTest_WhenImageMdsMetaIsNull() {
        //noinspection ConstantConditions
        List<GdImageFormat> gdImageFormats =
                ClientEntityConverter.toSupportedGdImageFormats(ImageType.REGULAR, null);

        assertThat(gdImageFormats)
                .isNull();
    }

    @Test
    public void toSupportedGdImageFormatsTest_WhenImageMdsMetaSizesIsNull() {
        List<GdImageFormat> gdImageFormats =
                ClientEntityConverter.toSupportedGdImageFormats(ImageType.REGULAR, new ImageMdsMeta().withSizes(null));

        assertThat(gdImageFormats)
                .isNull();
    }

    @Test
    public void toSupportedGdImageFormatsTest_filterSupportedFormats() {
        ImageSizeMeta sizeMeta = ImageTestDataUtils.generateImageSizeMeta();
        String supportedFormat = Iterables.getLast(SUPPORTED_REGULAR_FORMATS);
        String formatOfOtherImageType = Iterables.getLast(SUPPORTED_WIDE_FORMATS);

        Map<String, ImageSizeMeta> sizes = ImmutableMap.<String, ImageSizeMeta>builder()
                .put(supportedFormat, sizeMeta)
                .put(formatOfOtherImageType, ImageTestDataUtils.generateImageSizeMeta())
                .build();
        ImageMdsMeta imageMdsMeta = new ImageMdsMeta()
                .withSizes(sizes);
        List<GdImageFormat> gdImageFormats =
                ClientEntityConverter.toSupportedGdImageFormats(ImageType.REGULAR, imageMdsMeta);

        GdImageFormat expectedGdImageFormat = ClientEntityConverter.toGdImageFormat(sizeMeta);
        assertThat(gdImageFormats)
                .is(matchedBy(beanDiffer(Collections.singletonList(expectedGdImageFormat))));
    }

}

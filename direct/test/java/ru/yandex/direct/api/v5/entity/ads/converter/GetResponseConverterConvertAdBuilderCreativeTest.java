package ru.yandex.direct.api.v5.entity.ads.converter;

import javax.annotation.ParametersAreNonnullByDefault;

import org.junit.Test;

import ru.yandex.direct.core.entity.creative.model.Creative;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.api.v5.entity.ads.converter.GetResponseConverter.convertAdBuilderCreative;

@ParametersAreNonnullByDefault
public class GetResponseConverterConvertAdBuilderCreativeTest {

    @Test
    public void creativeIsConverted() {
        Long creativeId = 1L;
        String thumbnailUrl = "thumbnailUrl";
        String previewUrl = "previewUrl";

        Creative creative =
                new Creative().withId(creativeId).withPreviewUrl(thumbnailUrl).withLivePreviewUrl(previewUrl);

        assertThat(convertAdBuilderCreative(creative))
                .hasFieldOrPropertyWithValue("CreativeId", creativeId)
                .hasFieldOrPropertyWithValue("ThumbnailUrl", thumbnailUrl)
                .hasFieldOrPropertyWithValue("PreviewUrl", previewUrl);
    }
}

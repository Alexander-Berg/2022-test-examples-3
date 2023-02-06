package ru.yandex.direct.grid.processing.service.client.converter;

import java.util.List;

import junitparams.JUnitParamsRunner;
import org.assertj.core.api.recursive.comparison.RecursiveComparisonConfiguration;
import org.junit.Test;
import org.junit.runner.RunWith;

import ru.yandex.direct.core.entity.creative.model.AdditionalData;
import ru.yandex.direct.core.entity.creative.model.Creative;
import ru.yandex.direct.core.entity.creative.model.CreativeBusinessType;
import ru.yandex.direct.core.entity.creative.model.ModerationInfo;
import ru.yandex.direct.core.entity.creative.model.ModerationInfoText;
import ru.yandex.direct.core.testing.data.TestCreatives;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.grid.processing.model.cliententity.GdCanvasCreative;
import ru.yandex.direct.grid.processing.model.cliententity.GdCreativeBusinessType;
import ru.yandex.direct.grid.processing.model.cliententity.GdHtml5Creative;
import ru.yandex.direct.grid.processing.model.cliententity.GdModerationInfo;
import ru.yandex.direct.grid.processing.model.cliententity.GdModerationInfoText;
import ru.yandex.direct.grid.processing.model.cliententity.GdSmartCreative;
import ru.yandex.direct.grid.processing.model.cliententity.GdTypedCreative;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(JUnitParamsRunner.class)
public class ClientEntityConverterTest {

    @Test
    public void toGdCreativeImplementation_success_forSmartCreative() {
        long creativeId = 10L;
        long themeId = 40L;
        long layoutId = 2L;
        CreativeBusinessType businessType = CreativeBusinessType.REALTY;
        GdCreativeBusinessType expectedBusinessType = GdCreativeBusinessType.REALTY;

        Creative input = TestCreatives.defaultPerformanceCreative(ClientId.fromLong(1L), creativeId)
                .withBusinessType(businessType)
                .withThemeId(themeId)
                .withLayoutId(layoutId);

        GdTypedCreative typedCreative = ClientEntityConverter.toGdCreativeImplementation(input);

        assertThat(typedCreative).isInstanceOf(GdSmartCreative.class);
        GdSmartCreative expected = new GdSmartCreative()
                .withCreativeId(creativeId)
                .withBusinessType(expectedBusinessType)
                .withLayoutId(layoutId)
                .withThemeId(themeId);
        assertThat(typedCreative)
                .isEqualToComparingOnlyGivenFields(expected, "creativeId", "businessType", "layoutId", "themeId");
    }

    @Test
    public void toGdCreativeImplementation_success_forAdaptiveWithTexts() {
        long creativeId = 42L;
        String[] data = {"type", "some text", "#FFFFFF"};
        Creative input = TestCreatives.defaultAdaptive(ClientId.fromLong(1L), creativeId)
                .withModerationInfo(new ModerationInfo().withTexts(List.of(
                        new ModerationInfoText().withType(data[0]).withText(data[1]).withColor(data[2]),
                        new ModerationInfoText()
                )));

        GdTypedCreative typedCreative = ClientEntityConverter.toGdCreativeImplementation(input);

        GdCanvasCreative expected = new GdCanvasCreative().withCreativeId(creativeId)
                .withIsAdaptive(true)
                .withModerationInfo(new GdModerationInfo().withTexts(List.of(
                        new GdModerationInfoText().withType(data[0]).withText(data[1]),
                        new GdModerationInfoText()
                )));
        assertThat(typedCreative).hasSameClassAs(typedCreative);
        assertThat(typedCreative)
                .isEqualToComparingOnlyGivenFields(expected, "creativeId", "isAdaptive", "moderationInfo");
    }

    @Test
    public void toGdCreativeImplementation_success_forCanvasCreativeFillsBatchId() {
        long creativeId = 42L;
        String batchId = "5e218a851290ca709b5e3280";
        String livePreviewUrl =
                "https://canvas.yandex.ru/creatives/5e218a851290ca709b5e3280/1110588073/preview?isCompactPreview=true";

        Creative input = TestCreatives.defaultAdaptive(ClientId.fromLong(1L), creativeId)
                .withLivePreviewUrl(livePreviewUrl);

        GdTypedCreative typedCreative = ClientEntityConverter.toGdCreativeImplementation(input);

        GdCanvasCreative expected = new GdCanvasCreative().withCreativeId(creativeId).withBatchId(batchId);
        assertThat(typedCreative).isEqualToComparingOnlyGivenFields(expected, "creativeId", "batchId");
    }

    @Test
    public void toGdCreativeImplementation_success_forHtml5Creative() {
        long creativeId = 42L;
        var originalHeight = 602;
        var originalWidth = 500;

        Creative input = TestCreatives.defaultHtml5(ClientId.fromLong(1L), creativeId)
                .withAdditionalData(
                        new AdditionalData()
                                .withOriginalHeight(originalHeight)
                                .withOriginalWidth(originalWidth)
                );

        GdTypedCreative typedCreative = ClientEntityConverter.toGdCreativeImplementation(input);

        var expected = new GdHtml5Creative()
                .withOriginalHeight(originalHeight)
                .withOriginalWidth(originalWidth);

        assertThat(typedCreative)
                .usingRecursiveComparison(
                        RecursiveComparisonConfiguration.builder()
                                .withComparedFields("originalHeight", "originalWidth")
                                .build()
                ).isEqualTo(expected);
    }
}

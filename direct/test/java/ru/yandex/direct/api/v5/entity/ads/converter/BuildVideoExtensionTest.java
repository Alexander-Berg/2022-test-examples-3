package ru.yandex.direct.api.v5.entity.ads.converter;

import java.util.function.Consumer;

import javax.xml.bind.JAXBElement;

import com.yandex.direct.api.v5.ads.VideoExtensionGetItem;
import com.yandex.direct.api.v5.general.StatusEnum;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.direct.core.entity.banner.model.BannerCreativeStatusModerate;
import ru.yandex.direct.core.entity.banner.model.TextBanner;
import ru.yandex.direct.core.entity.creative.model.Creative;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.api.v5.entity.ads.converter.ModerationStatusBuilder.convertVideoExtension;

@RunWith(Parameterized.class)
public class BuildVideoExtensionTest {

    private static final Long creativeId = 1L;
    private static final String thumbnailUrl = "thumbnail_url";
    private static final String previewUrl = "preview_url";

    @Parameterized.Parameter
    public String desc;

    @Parameterized.Parameter(1)
    public TextBanner bannerCreative;

    @Parameterized.Parameter(2)
    public Creative creative;

    @Parameterized.Parameter(3)
    public Consumer<JAXBElement<VideoExtensionGetItem>> checkExpectations;

    @Parameterized.Parameters(name = "{0}}")
    public static Object[][] getParameters() {
        return new Object[][]{
                {"no banner creative, no creative", null, null, isNil()},
                {"no banner creative", null, new Creative(), isNil()},
                {"no creative", new TextBanner(), null, isNil()},
                {"new => draft", new TextBanner().withCreativeStatusModerate(BannerCreativeStatusModerate.NEW),
                        new Creative().withId(creativeId).withPreviewUrl(thumbnailUrl).withLivePreviewUrl(previewUrl),
                        isEqual(StatusEnum.DRAFT)},
                {"sending => moderation", new TextBanner().withCreativeStatusModerate(BannerCreativeStatusModerate.SENDING),
                        new Creative().withId(creativeId).withPreviewUrl(thumbnailUrl).withLivePreviewUrl(previewUrl),
                        isEqual(StatusEnum.MODERATION)},
                {"sent => moderation", new TextBanner().withCreativeStatusModerate(BannerCreativeStatusModerate.SENT),
                        new Creative().withId(creativeId).withPreviewUrl(thumbnailUrl).withLivePreviewUrl(previewUrl),
                        isEqual(StatusEnum.MODERATION)},
                {"ready => moderation", new TextBanner().withCreativeStatusModerate(BannerCreativeStatusModerate.READY),
                        new Creative().withId(creativeId).withPreviewUrl(thumbnailUrl).withLivePreviewUrl(previewUrl),
                        isEqual(StatusEnum.MODERATION)},
                {"yes => accepted", new TextBanner().withCreativeStatusModerate(BannerCreativeStatusModerate.YES),
                        new Creative().withId(creativeId).withPreviewUrl(thumbnailUrl).withLivePreviewUrl(previewUrl),
                        isEqual(StatusEnum.ACCEPTED)},
                {"no => rejected", new TextBanner().withCreativeStatusModerate(BannerCreativeStatusModerate.NO),
                        new Creative().withId(creativeId).withPreviewUrl(thumbnailUrl).withLivePreviewUrl(previewUrl),
                        isEqual(StatusEnum.REJECTED)},
        };
    }

    private static Consumer<JAXBElement<VideoExtensionGetItem>> isNil() {
        return elem -> assertThat(elem.isNil()).isTrue();
    }

    private static Consumer<JAXBElement<VideoExtensionGetItem>> isEqual(StatusEnum expectedStatus) {
        return elem -> assertThat(elem.getValue().getStatus()).isEqualTo(expectedStatus);
    }

    @Test
    public void test() {
        JAXBElement<VideoExtensionGetItem> videoExtension = convertVideoExtension(bannerCreative, creative);
        checkExpectations.accept(videoExtension);
    }
}

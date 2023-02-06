package ru.yandex.direct.api.v5.entity.ads.converter;

import java.util.function.Consumer;

import com.yandex.direct.api.v5.general.ExtensionModeration;
import com.yandex.direct.api.v5.general.StatusEnum;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestContextManager;

import ru.yandex.direct.api.v5.testing.configuration.Api5Test;
import ru.yandex.direct.common.TranslationService;
import ru.yandex.direct.core.entity.banner.model.BannerWithBannerImage;
import ru.yandex.direct.core.entity.banner.model.StatusBannerImageModerate;
import ru.yandex.direct.core.entity.banner.model.TextBanner;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.api.v5.entity.ads.converter.ModerationStatusBuilder.buildAdImageModeration;

@Api5Test
@RunWith(Parameterized.class)
public class BuildAdImageModerationTest {

    @Autowired
    public TranslationService translationService;

    @Parameterized.Parameter
    public String desc;

    @Parameterized.Parameter(1)
    public BannerWithBannerImage ad;

    @Parameterized.Parameter(2)
    public Consumer<ExtensionModeration> checkExpectations;

    @Parameterized.Parameters(name = "{0}}")
    public static Object[][] getParameters() {
        return new Object[][]{
                {"no banner image", new TextBanner(), isNull()},
                {"new => draft", new TextBanner()
                        .withImageHash("2")
                        .withImageStatusModerate(StatusBannerImageModerate.NEW),
                        isEqual(StatusEnum.DRAFT)},
                {"sending => moderation",
                        new TextBanner()
                                .withImageHash("2")
                                .withImageStatusModerate(StatusBannerImageModerate.SENDING),
                        isEqual(StatusEnum.MODERATION)},
                {"sent => moderation", new TextBanner()
                        .withImageHash("2")
                        .withImageStatusModerate(StatusBannerImageModerate.SENT),
                        isEqual(StatusEnum.MODERATION)},
                {"ready => moderation", new TextBanner()
                        .withImageHash("2")
                        .withImageStatusModerate(StatusBannerImageModerate.READY),
                        isEqual(StatusEnum.MODERATION)},
                {"yes => accepted", new TextBanner()
                        .withImageHash("2")
                        .withImageStatusModerate(StatusBannerImageModerate.YES),
                        isEqual(StatusEnum.ACCEPTED)},
                {"no => rejected", new TextBanner()
                        .withImageHash("2")
                        .withImageStatusModerate(StatusBannerImageModerate.NO),
                        isEqual(StatusEnum.REJECTED)},
        };
    }

    private static Consumer<ExtensionModeration> isNull() {
        return elem -> assertThat(elem).isNull();
    }

    private static Consumer<ExtensionModeration> isEqual(StatusEnum expectedStatus) {
        return elem -> assertThat(elem).extracting("Status").isEqualTo(expectedStatus);
    }

    @Before
    public void before() throws Exception {
        new TestContextManager(getClass()).prepareTestInstance(this);
    }

    @Test
    public void test() {
        ExtensionModeration buildAdImageModeration = buildAdImageModeration(ad, emptyList(), translationService);

        checkExpectations.accept(buildAdImageModeration);
    }

}

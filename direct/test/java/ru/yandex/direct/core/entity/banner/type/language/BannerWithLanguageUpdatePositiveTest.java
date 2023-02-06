package ru.yandex.direct.core.entity.banner.type.language;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.direct.core.entity.banner.model.BannerStatusModerate;
import ru.yandex.direct.core.entity.banner.model.BannerStatusPostModerate;
import ru.yandex.direct.core.entity.banner.model.ContentPromotionBanner;
import ru.yandex.direct.core.entity.banner.model.CpmOutdoorBanner;
import ru.yandex.direct.core.entity.banner.model.Language;
import ru.yandex.direct.core.entity.banner.type.BannerNewBannerInfoUpdateOperationTestBase;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.data.banner.TestContentPromotionBanners;
import ru.yandex.direct.core.testing.info.NewContentPromoBannerInfo;
import ru.yandex.direct.core.testing.info.NewCpmOutdoorBannerInfo;
import ru.yandex.direct.core.testing.steps.CpmOutdoorBannerSteps;
import ru.yandex.direct.core.testing.steps.banner.ContentPromotionBannerSteps;
import ru.yandex.direct.model.ModelChanges;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static ru.yandex.direct.core.testing.data.TestNewCpmOutdoorBanners.fullCpmOutdoorBanner;

@CoreTest
@RunWith(SpringRunner.class)
public class BannerWithLanguageUpdatePositiveTest extends BannerNewBannerInfoUpdateOperationTestBase {

    @Autowired
    private ContentPromotionBannerSteps contentPromotionBannerSteps;

    @Autowired
    private CpmOutdoorBannerSteps cpmOutdoorBannerSteps;
    @Autowired
    private TestContentPromotionBanners testNewContentPromotionBanners;

    @Test
    public void validLanguageForContentPromotionBanner() {
        ContentPromotionBanner banner = testNewContentPromotionBanners.fullContentPromoBanner(null, null)
                .withTitle("English title");

        bannerInfo = steps.oldContentPromotionBannerSteps().createContentPromotionBanner(
                new NewContentPromoBannerInfo().withBanner(banner));

        ModelChanges<ContentPromotionBanner> modelChanges =
                new ModelChanges<>(bannerInfo.getBannerId(), ContentPromotionBanner.class)
                        .process("русский заголовок", ContentPromotionBanner.TITLE);
        Long id = prepareAndApplyValid(modelChanges);

        ContentPromotionBanner actualBanner = getBanner(id, ContentPromotionBanner.class);
        assertThat(actualBanner.getLanguage(), equalTo(Language.RU_));
    }

    @Test
    public void validLanguageForOutdoorBanner() {
        CpmOutdoorBanner banner = fullCpmOutdoorBanner(null)
                .withStatusModerate(BannerStatusModerate.NEW)
                .withStatusPostModerate(BannerStatusPostModerate.NO);
        bannerInfo = cpmOutdoorBannerSteps.createCpmOutdoorBanner(
                new NewCpmOutdoorBannerInfo().withBanner(banner));

        ModelChanges<CpmOutdoorBanner> modelChanges =
                new ModelChanges<>(bannerInfo.getBannerId(), CpmOutdoorBanner.class)
                        .process("https://www.yandex.ru", CpmOutdoorBanner.HREF);
        Long id = prepareAndApplyValid(modelChanges);

        CpmOutdoorBanner actualBanner = getBanner(id, CpmOutdoorBanner.class);
        assertThat(actualBanner.getLanguage(), equalTo(Language.UNKNOWN));
    }
}

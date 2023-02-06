package ru.yandex.direct.core.entity.banner.type.language;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.direct.core.entity.adgroup.model.ContentPromotionAdgroupType;
import ru.yandex.direct.core.entity.banner.model.ContentPromotionBanner;
import ru.yandex.direct.core.entity.banner.model.CpmOutdoorBanner;
import ru.yandex.direct.core.entity.banner.model.Language;
import ru.yandex.direct.core.entity.banner.type.BannerAdGroupInfoAddOperationTestBase;
import ru.yandex.direct.core.entity.contentpromotion.model.ContentPromotionContent;
import ru.yandex.direct.core.entity.contentpromotion.model.ContentPromotionContentType;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.CreativeInfo;
import ru.yandex.direct.core.testing.steps.CreativeSteps;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static ru.yandex.direct.core.testing.data.TestNewCpmOutdoorBanners.clientCpmOutdoorBanner;
import static ru.yandex.direct.core.testing.data.banner.TestContentPromotionBanners.clientContentPromoBanner;

@CoreTest
@RunWith(SpringRunner.class)
public class BannerWithLanguageAddPositiveTest extends BannerAdGroupInfoAddOperationTestBase {

    private static final String TITLE = "some test title";
    private static final String BODY = "some test body";

    @Autowired
    private CreativeSteps creativeSteps;

    @Test
    public void languageIsComputedAndSavedInContentPromotionBanner() {
        adGroupInfo = steps.adGroupSteps().createDefaultContentPromotionAdGroup(ContentPromotionAdgroupType.VIDEO);
        ContentPromotionContent content = steps.contentPromotionSteps()
                .createContentPromotionContent(adGroupInfo.getClientId(), ContentPromotionContentType.VIDEO);
        ContentPromotionBanner banner = clientContentPromoBanner(content.getId())
                .withAdGroupId(adGroupInfo.getAdGroupId())
                .withTitle(TITLE)
                .withBody(BODY);

        Long id = prepareAndApplyValid(banner);

        ContentPromotionBanner actualBanner = getBanner(id, ContentPromotionBanner.class);
        assertThat(actualBanner.getLanguage(), equalTo(Language.EN));
    }

    @Test
    public void languageIsComputedAndSavedInCpmOutdoorBanner() {
        adGroupInfo = steps.adGroupSteps().createDefaultCpmOutdoorAdGroup();
        CreativeInfo creativeInfo = creativeSteps
                .addDefaultCpmOutdoorVideoCreative(adGroupInfo.getClientInfo());
        CpmOutdoorBanner banner = clientCpmOutdoorBanner(creativeInfo.getCreativeId())
                .withAdGroupId(adGroupInfo.getAdGroupId());

        Long id = prepareAndApplyValid(banner);

        CpmOutdoorBanner actualBanner = getBanner(id, CpmOutdoorBanner.class);
        assertThat(actualBanner.getLanguage(), equalTo(Language.UNKNOWN));
    }
}

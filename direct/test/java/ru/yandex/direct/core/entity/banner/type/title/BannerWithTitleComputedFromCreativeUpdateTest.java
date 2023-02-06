package ru.yandex.direct.core.entity.banner.type.title;

import java.util.Arrays;
import java.util.Collection;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestContextManager;

import ru.yandex.direct.core.entity.banner.model.CpcVideoBanner;
import ru.yandex.direct.core.entity.banner.type.BannerNewBannerInfoUpdateOperationTestBase;
import ru.yandex.direct.core.entity.creative.model.Creative;
import ru.yandex.direct.core.entity.creative.model.ModerationInfo;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.data.TestNewCpcVideoBanners;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.info.NewCpcVideoBannerInfo;
import ru.yandex.direct.model.ModelChanges;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.core.entity.banner.model.BannerWithCreative.CREATIVE_ID;
import static ru.yandex.direct.core.entity.banner.type.BannerWithBodyOrTitleComputedFromCreativeTestUtils.BODY_TEXT;
import static ru.yandex.direct.core.entity.banner.type.BannerWithBodyOrTitleComputedFromCreativeTestUtils.TITLE;
import static ru.yandex.direct.core.entity.banner.type.BannerWithBodyOrTitleComputedFromCreativeTestUtils.TITLE_TEXT;
import static ru.yandex.direct.core.testing.data.TestCreatives.defaultCpcVideoForCpcVideoBanner;

@CoreTest
@RunWith(Parameterized.class)
public class BannerWithTitleComputedFromCreativeUpdateTest extends BannerNewBannerInfoUpdateOperationTestBase {

    @Autowired
    private TestNewCpcVideoBanners testNewCpcVideoBanners;

    private ClientInfo defaultClient;

    @Parameterized.Parameter
    public ModerationInfo creativeModerationInfo;

    @Parameterized.Parameter(1)
    public String expectedTitle;

    @Parameterized.Parameters
    public static Collection<Object[]> parameters() {
        return Arrays.asList(new Object[][]{
                {
                        new ModerationInfo().withTexts(asList(TITLE_TEXT, BODY_TEXT)),
                        TITLE
                },
                {
                        new ModerationInfo().withTexts(singletonList(BODY_TEXT)),
                        null
                },
                {
                        new ModerationInfo().withTexts(emptyList()),
                        null
                },
                {
                        new ModerationInfo().withTexts(null),
                        null
                },
                {
                        null,
                        null
                }
        });
    }

    @Before
    public void before() throws Exception {
        new TestContextManager(getClass()).prepareTestInstance(this);
        defaultClient = steps.clientSteps().createDefaultClient();
    }

    @Test
    public void setsTitleFromCreativeWhenUpdatingBannerWithNotNullTitle() {
        // создаем баннер, который будем обновлять, у него title != null
        bannerInfo = createBannerForUpdate("old title");

        prepareAndApplyValid(createModelChanges());

        CpcVideoBanner banner = getBanner(bannerInfo.getBannerId(), CpcVideoBanner.class);
        assertThat(banner.getTitle()).isEqualTo(expectedTitle);
    }

    @Test
    public void setsTitleFromCreativeWhenUpdatingBannerWithNullTitle() {
        // создаем баннер, который будем обновлять, у него body == null
        bannerInfo = createBannerForUpdate(null);

        prepareAndApplyValid(createModelChanges());

        CpcVideoBanner banner = getBanner(bannerInfo.getBannerId(), CpcVideoBanner.class);
        assertThat(banner.getTitle()).isEqualTo(expectedTitle);
    }

    private NewCpcVideoBannerInfo createBannerForUpdate(String title) {
        Creative oldCreative = defaultCpcVideoForCpcVideoBanner(null, null);
        steps.creativeSteps().createCreative(oldCreative, defaultClient);
        return steps.cpcVideoBannerSteps().createBanner(
                new NewCpcVideoBannerInfo()
                        .withBanner(testNewCpcVideoBanners.fullCpcVideoBanner(oldCreative.getId()).withTitle(title))
                        .withClientInfo(defaultClient));
    }

    private ModelChanges<CpcVideoBanner> createModelChanges() {
        Creative newCreative = defaultCpcVideoForCpcVideoBanner(null, null)
                .withModerationInfo(creativeModerationInfo);
        steps.creativeSteps().createCreative(newCreative, defaultClient);

        return ModelChanges.build(bannerInfo.getBannerId(), CpcVideoBanner.class,
                CREATIVE_ID, newCreative.getId());
    }
}

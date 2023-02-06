package ru.yandex.direct.core.entity.banner.type.title;

import java.util.Arrays;
import java.util.Collection;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.springframework.test.context.TestContextManager;

import ru.yandex.direct.core.entity.banner.model.CpcVideoBanner;
import ru.yandex.direct.core.entity.banner.type.BannerAdGroupInfoAddOperationTestBase;
import ru.yandex.direct.core.entity.creative.model.Creative;
import ru.yandex.direct.core.entity.creative.model.ModerationInfo;
import ru.yandex.direct.core.testing.configuration.CoreTest;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.core.entity.banner.type.BannerWithBodyOrTitleComputedFromCreativeTestUtils.BODY_TEXT;
import static ru.yandex.direct.core.entity.banner.type.BannerWithBodyOrTitleComputedFromCreativeTestUtils.TITLE;
import static ru.yandex.direct.core.entity.banner.type.BannerWithBodyOrTitleComputedFromCreativeTestUtils.TITLE_TEXT;
import static ru.yandex.direct.core.testing.data.TestCreatives.defaultCpcVideoForCpcVideoBanner;
import static ru.yandex.direct.core.testing.data.TestNewCpcVideoBanners.clientCpcVideoBanner;

@CoreTest
@RunWith(Parameterized.class)
public class BannerWithTitleComputedFromCreativeAddTest extends BannerAdGroupInfoAddOperationTestBase {

    @Parameterized.Parameter(0)
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
        adGroupInfo = steps.adGroupSteps().createActiveTextAdGroup();
    }

    @Test
    public void setsTitleFromCreative() {
        Creative creative = defaultCpcVideoForCpcVideoBanner(null, null)
                .withModerationInfo(creativeModerationInfo);
        steps.creativeSteps().createCreative(creative, adGroupInfo.getClientInfo());

        Long bannerId = prepareAndApplyValid(clientCpcVideoBanner(creative.getId())
                .withAdGroupId(adGroupInfo.getAdGroupId()));

        CpcVideoBanner banner = getBanner(bannerId, CpcVideoBanner.class);
        assertThat(banner.getTitle()).isEqualTo(expectedTitle);
    }
}

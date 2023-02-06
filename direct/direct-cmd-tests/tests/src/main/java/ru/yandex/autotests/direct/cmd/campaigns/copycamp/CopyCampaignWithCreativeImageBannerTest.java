package ru.yandex.autotests.direct.cmd.campaigns.copycamp;

import java.util.Arrays;
import java.util.Collection;

import org.junit.After;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.data.commons.banner.Banner;
import ru.yandex.autotests.direct.cmd.rules.CreativeBannerRule;
import ru.yandex.autotests.direct.cmd.rules.DirectCmdRule;
import ru.yandex.autotests.direct.cmd.tags.CampTypeTag;
import ru.yandex.autotests.direct.cmd.tags.CmdTag;
import ru.yandex.autotests.direct.cmd.tags.ObjectTag;
import ru.yandex.autotests.direct.httpclient.TestFeatures;
import ru.yandex.autotests.direct.utils.campaigns.CampaignTypeEnum;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

import static ru.yandex.autotests.direct.cmd.util.CampaignHelper.deleteAdGroupMobileContent;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies.onlyExpectedFields;

@Aqua.Test
@Description("Копирование ТГО/РМП кампании с ГО с креативом")
@Stories(TestFeatures.Campaigns.COPY_CAMP)
@Features(TestFeatures.CAMPAIGNS)
@Tag(CmdTag.COPY_CAMP)
@Tag(ObjectTag.CAMPAIGN)
@Tag(CampTypeTag.TEXT)
@Tag(CampTypeTag.MOBILE)
@RunWith(Parameterized.class)
public class CopyCampaignWithCreativeImageBannerTest {

    private static final String CLIENT = "at-direct-creative-construct";

    @ClassRule
    public static DirectCmdRule stepsClassRule = DirectCmdRule.defaultClassRule();
    @Rule
    public DirectCmdRule cmdRule;
    private CreativeBannerRule bannersRule;
    private Long newCid;

    public CopyCampaignWithCreativeImageBannerTest(CampaignTypeEnum campaignType) {
        bannersRule = new CreativeBannerRule(campaignType).withUlogin(CLIENT);
        cmdRule = DirectCmdRule.defaultRule().withRules(bannersRule);
    }

    @Parameterized.Parameters(name = "Копирование графического объявления. Тип кампании {0}")
    public static Collection<Object[]> testData() {
        return Arrays.asList(new Object[][]{
                {CampaignTypeEnum.TEXT},
                {CampaignTypeEnum.MOBILE}
        });
    }

    @After
    public void shutDown() {
        if (newCid != null) {
            deleteAdGroupMobileContent(newCid, CLIENT);
            cmdRule.apiAggregationSteps().deleteActiveCampaignQuietly(CLIENT, newCid);
        }
    }

    @Test
    @ru.yandex.qatools.allure.annotations.TestCaseId("9401")
    public void imageShouldBeCopied() {
        newCid = cmdRule.cmdSteps().copyCampSteps().copyCampWithinClient(CLIENT, bannersRule.getCampaignId());
        Banner copiedBanner = cmdRule.cmdSteps().campaignSteps().getShowCamp(CLIENT, newCid.toString())
                .getGroups().get(0);

        assertThat("Баннер скопировался", copiedBanner,
                beanDiffer(getExpectedBanner()).useCompareStrategy(onlyExpectedFields()));
    }

    private Banner getExpectedBanner() {
        return new Banner()
                .withCreativeBanner(bannersRule.getBanner().getCreativeBanner())
                .withHref(bannersRule.getBanner().getUrlProtocol() + bannersRule.getBanner().getHref())
                .withBannerType(bannersRule.getBanner().getBannerType())
                .withAdType(bannersRule.getBanner().getAdType())
                .withImageAd(bannersRule.getBanner().getImageAd());
    }
}

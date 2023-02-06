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
import ru.yandex.autotests.direct.cmd.data.images.UploadImageResponse;
import ru.yandex.autotests.direct.cmd.rules.DirectCmdRule;
import ru.yandex.autotests.direct.cmd.rules.ImageBannerRule;
import ru.yandex.autotests.direct.cmd.steps.images.NewImagesUploadHelper;
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
import static ru.yandex.autotests.directapi.matchers.beandiffer2.Api5CompareStrategies.allFieldsExcept;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.autotests.irt.testutils.beandiffer2.beanfield.BeanFieldPath.newPath;

@Aqua.Test
@Description("Копирование ТГО/РМП кампании с графическим баннером")
@Stories(TestFeatures.Campaigns.COPY_CAMP)
@Features(TestFeatures.CAMPAIGNS)
@Tag(CmdTag.COPY_CAMP)
@Tag(ObjectTag.CAMPAIGN)
@Tag(CampTypeTag.TEXT)
@Tag(CampTypeTag.MOBILE)
@RunWith(Parameterized.class)
public class CopyCampaignWithImageBannerTest {

    private static final String CLIENT = "at-direct-image-banner71";

    @ClassRule
    public static DirectCmdRule stepsClassRule = DirectCmdRule.defaultClassRule();
    @Rule
    public DirectCmdRule cmdRule;
    private ImageBannerRule bannersRule;
    private Long newCid;

    public CopyCampaignWithImageBannerTest(CampaignTypeEnum campaignType) {
        bannersRule = new ImageBannerRule(campaignType)
                .withImageUploader(new NewImagesUploadHelper())
                .withUlogin(CLIENT);
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
    @ru.yandex.qatools.allure.annotations.TestCaseId("9402")
    public void imageShouldBeCopied() {
        newCid = cmdRule.cmdSteps().copyCampSteps().copyCampWithinClient(CLIENT, bannersRule.getCampaignId());
        Banner copiedBanner = cmdRule.cmdSteps().campaignSteps().getShowCamp(CLIENT, newCid.toString())
                .getGroups().get(0);
        UploadImageResponse actualImageAd = (UploadImageResponse) new UploadImageResponse()
                .withGroupId(String.valueOf(copiedBanner.getImfMdsGroupId()))
                .withScale(copiedBanner.getScale())
                .withName(copiedBanner.getName())
                .withHeight(copiedBanner.getHeight())
                .withWidth(copiedBanner.getWidth())
                .withHash(copiedBanner.getImageAd().getHash());

        UploadImageResponse expectedImageAd = bannersRule.getImageUploadHelper().getUploadResponse();

        assertThat("Картинка скопировалась", actualImageAd,
                beanDiffer(expectedImageAd).useCompareStrategy(allFieldsExcept(
                        newPath("result")
                )));
    }
}

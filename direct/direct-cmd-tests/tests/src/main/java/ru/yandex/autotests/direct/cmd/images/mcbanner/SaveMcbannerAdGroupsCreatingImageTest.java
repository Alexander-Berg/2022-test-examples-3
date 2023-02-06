package ru.yandex.autotests.direct.cmd.images.mcbanner;

import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.data.Logins;
import ru.yandex.autotests.direct.cmd.data.campaigns.ShowCampMultiEditRequest;
import ru.yandex.autotests.direct.cmd.data.campaigns.ShowCampMultiEditResponse;
import ru.yandex.autotests.direct.cmd.data.commons.banner.Banner;
import ru.yandex.autotests.direct.cmd.data.commons.banner.ImageAd;
import ru.yandex.autotests.direct.cmd.data.commons.group.Group;
import ru.yandex.autotests.direct.cmd.rules.DirectCmdRule;
import ru.yandex.autotests.direct.cmd.rules.MCBannerRule;
import ru.yandex.autotests.direct.cmd.tags.CampTypeTag;
import ru.yandex.autotests.direct.cmd.tags.CmdTag;
import ru.yandex.autotests.direct.cmd.tags.ObjectTag;
import ru.yandex.autotests.direct.cmd.tags.SmokeTag;
import ru.yandex.autotests.direct.httpclient.TestFeatures;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

import static ru.yandex.autotests.direct.cmd.util.ImageUtils.ImageFormat;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies.onlyExpectedFields;

@Aqua.Test
@Description("Создание группы для ГО на поиске")
@Stories(TestFeatures.BannerImages.SAVE_BANNER_WITH_IMAGE)
@Features(TestFeatures.BANNER_IMAGES)
@Tag(CmdTag.SAVE_TEXT_ADGROUPS)
@Tag(ObjectTag.BANNER)
@Tag(ObjectTag.IMAGE)
@Tag(CampTypeTag.MCBANNER)
@Tag(SmokeTag.YES)
public class SaveMcbannerAdGroupsCreatingImageTest {

    private static final String CLIENT = Logins.DEFAULT_CLIENT;
    private static final ImageFormat FORMAT = ImageFormat.JPG;
    @ClassRule
    public static DirectCmdRule defaultClassRule = DirectCmdRule.defaultClassRule();
    private MCBannerRule bannersRule = new MCBannerRule().withUlogin(CLIENT);

    @Rule
    public DirectCmdRule cmdRule = DirectCmdRule.defaultRule().as(CLIENT).withRules(bannersRule);

    @Test
    @Description("Создание текстовой группы с картинкой (bannersMultiSave)")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9886")
    public void testCreatingImageAtBannersMultiSave() {
        Group createdGroup = getGroup();
        Banner createdBanner = createdGroup.getBanners().get(0);

        Banner expectedBanner = new Banner();
          expectedBanner.withImageAd(new ImageAd()
                  .withName(bannersRule.getLastUsedImageAd().getName())
                  .withHeight(bannersRule.getLastUsedImageAd().getHeight())
                  .withWidth(bannersRule.getLastUsedImageAd().getWidth())
          );
        expectedBanner.setAdType("mcbanner");
        expectedBanner.setImageAdStatusModerate("New");

        assertThat("в сохраненном баннере присутствуют все необходимые поля",
                createdBanner,
                beanDiffer(expectedBanner).useCompareStrategy(onlyExpectedFields()));
    }

    private Group getGroup() {
        ShowCampMultiEditRequest request = ShowCampMultiEditRequest.forSingleBanner(
                CLIENT, bannersRule.getCampaignId(), bannersRule.getGroupId(), bannersRule.getBannerId());
        ShowCampMultiEditResponse response = cmdRule.cmdSteps().campaignSteps().getShowCampMultiEdit(request);
        return response.getCampaign().getGroups().get(0);
    }
}

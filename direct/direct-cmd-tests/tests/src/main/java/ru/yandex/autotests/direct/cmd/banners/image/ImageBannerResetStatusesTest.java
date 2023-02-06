package ru.yandex.autotests.direct.cmd.banners.image;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.bssynced.BsSyncedHelper;
import ru.yandex.autotests.direct.cmd.common.AppMetricaHrefs;
import ru.yandex.autotests.direct.cmd.data.banners.BannersFactory;
import ru.yandex.autotests.direct.cmd.data.banners.GroupsParameters;
import ru.yandex.autotests.direct.cmd.data.commons.StatusModerate;
import ru.yandex.autotests.direct.cmd.data.commons.banner.Banner;
import ru.yandex.autotests.direct.cmd.data.commons.group.Group;
import ru.yandex.autotests.direct.cmd.rules.BannersRule;
import ru.yandex.autotests.direct.cmd.rules.DirectCmdRule;
import ru.yandex.autotests.direct.cmd.rules.ImageBannerRule;
import ru.yandex.autotests.direct.cmd.steps.images.NewImagesUploadHelper;
import ru.yandex.autotests.direct.cmd.tags.CampTypeTag;
import ru.yandex.autotests.direct.cmd.tags.CmdTag;
import ru.yandex.autotests.direct.cmd.tags.ObjectTag;
import ru.yandex.autotests.direct.db.models.jooq.ppc.enums.ImagesStatusmoderate;
import ru.yandex.autotests.direct.httpclient.TestEnvironment;
import ru.yandex.autotests.direct.httpclient.TestFeatures;
import ru.yandex.autotests.direct.utils.campaigns.CampaignTypeEnum;
import ru.yandex.autotests.directapi.enums.StatusBsSynced;
import ru.yandex.autotests.httpclient.lite.core.exceptions.BackEndClientException;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

import static java.util.Collections.emptyMap;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies.onlyExpectedFields;

@Aqua.Test
@Description("Сброс статуса модерации и bsSynced графического баннера в ТГО/РМП кампаниях")
@Stories(TestFeatures.Banners.IMAGE_BANNERS_PARAMETERS)
@Features(TestFeatures.BANNERS)
@Tag(CmdTag.SAVE_TEXT_ADGROUPS)
@Tag(CmdTag.SAVE_MOBILE_ADGROUPS)
@Tag(ObjectTag.BANNER)
@Tag(CampTypeTag.TEXT)
@Tag(CampTypeTag.MOBILE)
@RunWith(Parameterized.class)
public class ImageBannerResetStatusesTest {

    private static final String CLIENT = "at-direct-image-banner71";
    private static final String ANOTHER_HREF = "https://video.yandex.ru";
    private static final String ANOTHER_MOBILE_HREF = AppMetricaHrefs.HREF_THREE;

    @ClassRule
    public static DirectCmdRule defaultClassRule = DirectCmdRule.defaultClassRule();
    @Rule
    public DirectCmdRule cmdRule;
    private BannersRule bannerRule;
    private Group expectedGroup;
    private NewImagesUploadHelper imagesUploadHelper;
    private Long campaignId;

    private CampaignTypeEnum campaignType;

    public ImageBannerResetStatusesTest(CampaignTypeEnum campaignType) {
        this.campaignType = campaignType;
        bannerRule = new ImageBannerRule(campaignType)
                .withImageUploader(new NewImagesUploadHelper())
                .withUlogin(CLIENT);
        cmdRule = DirectCmdRule.defaultRule().withRules(bannerRule);
        imagesUploadHelper = (NewImagesUploadHelper) new NewImagesUploadHelper()
                .withClient(CLIENT)
                .withBannerImageSteps(
                        cmdRule.cmdSteps().bannerImagesSteps()
                );
    }

    @Parameterized.Parameters(name = "Сброс статуса модерации и bsSynced графического баннера. Тип кампании: {0}")
    public static Collection<Object[]> testData() {
        return Arrays.asList(new Object[][]{
                {CampaignTypeEnum.TEXT},
                {CampaignTypeEnum.MOBILE}
        });
    }

    @Before
    public void before() {
        campaignId = bannerRule.getCampaignId();
        cmdRule.apiSteps().groupFakeSteps().makeGroupFullyModerated(bannerRule.getGroupId());
        cmdRule.apiSteps().bannersFakeSteps().makeBannerFullyModerated(bannerRule.getBannerId());
        cmdRule.apiSteps().campaignFakeSteps().setStatusModerate(bannerRule.getCampaignId(), "Yes");
        BsSyncedHelper.setBannerBsSynced(cmdRule, bannerRule.getBannerId(), StatusBsSynced.YES);
        TestEnvironment.newDbSteps().useShardForLogin(CLIENT).imagesSteps().setImageStatusModerate(
                bannerRule.getBannerId(),
                bannerRule.getBanner().getImageAd().getHash(),
                ImagesStatusmoderate.Yes);

        expectedGroup = cmdRule.cmdSteps().groupsSteps()
                .getGroup(CLIENT, bannerRule.getCampaignId(), bannerRule.getGroupId())
                .withTags(emptyMap());
    }

    @Test
    @Description("Сброс статусов при изменении изображения в графическом баннере")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9228")
    public void imageHashChangeStatusesTest() {
        imagesUploadHelper.upload();
        expectedGroup.getBanners().get(0).getImageAd()
                .withHash(imagesUploadHelper.getUploadResponse().getHash());

        bannerRule.saveGroup(GroupsParameters.forExistingCamp(CLIENT, campaignId, getGroupToSave()));

        assertThat("статус модерации сбросился", bannerRule.getCurrentGroup(),
                beanDiffer(getExpectedGroup()).useCompareStrategy(onlyExpectedFields()));
    }

    @Test
    @Description("Сброс статусов при изменении ссылки в графическом баннере")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9229")
    public void imageBannerHrefChangeStatusesTest() {
        expectedGroup = getGroupToSave();
        expectedGroup.getBanners().get(0)
                .withHref(campaignType == CampaignTypeEnum.MOBILE ? ANOTHER_MOBILE_HREF : ANOTHER_HREF);
        bannerRule.saveGroup(GroupsParameters.forExistingCamp(CLIENT, bannerRule.getCampaignId(), expectedGroup));

        assertThat("статус модерации сбросился", bannerRule.getCurrentGroup(),
                beanDiffer(getExpectedGroup()).useCompareStrategy(onlyExpectedFields()));
    }

    private Group getGroupToSave() {
        switch (campaignType) {
            case TEXT:
                return expectedGroup;
            case MOBILE:
                BannersFactory.addNeededAttribute(expectedGroup.getBanners().get(0));
                return expectedGroup;
            default:
                throw new BackEndClientException("Не указан тип кампании");
        }
    }

    private Group getExpectedGroup() {
        return new Group().withBanners(Collections.singletonList(new Banner()
                .withStatusBsSynced(StatusBsSynced.NO.toString())
                .withStatusModerate(StatusModerate.READY.toString())));
    }
}

package ru.yandex.autotests.direct.cmd.banners.image;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.DirectCmdSteps;
import ru.yandex.autotests.direct.cmd.data.banners.BannersFactory;
import ru.yandex.autotests.direct.cmd.data.banners.GroupsParameters;
import ru.yandex.autotests.direct.cmd.data.commons.banner.Banner;
import ru.yandex.autotests.direct.cmd.data.commons.banner.ImageAd;
import ru.yandex.autotests.direct.cmd.data.commons.group.Group;
import ru.yandex.autotests.direct.cmd.rules.BannersRule;
import ru.yandex.autotests.direct.cmd.rules.BannersRuleFactory;
import ru.yandex.autotests.direct.cmd.rules.DirectCmdRule;
import ru.yandex.autotests.direct.cmd.steps.base.DirectCmdStepsException;
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

import java.util.Arrays;
import java.util.Collection;

import static java.util.Collections.emptyMap;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assumeThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies.onlyExpectedFields;

@Aqua.Test
@Description("Сохранение графического баннера в ТГО/РМП кампаниях")
@Stories(TestFeatures.Banners.IMAGE_BANNERS_PARAMETERS)
@Features(TestFeatures.BANNERS)
@Tag(CmdTag.SAVE_TEXT_ADGROUPS)
@Tag(CmdTag.SAVE_MOBILE_ADGROUPS)
@Tag(ObjectTag.BANNER)
@Tag(CampTypeTag.TEXT)
@Tag(CampTypeTag.MOBILE)
@RunWith(Parameterized.class)
public class SaveImageBannerCampaignTest {

    private static final String CLIENT = "at-direct-image-banner71";

    @ClassRule
    public static DirectCmdRule defaultClassRule = DirectCmdRule.defaultClassRule();
    @Rule
    public DirectCmdRule cmdRule;
    public CampaignTypeEnum campaignType;
    private BannersRule bannersRule;
    private DirectCmdSteps directCmdSteps;
    private NewImagesUploadHelper imagesUploadHelper;
    private Long campaignId;
    private Banner saveBanner;

    public SaveImageBannerCampaignTest(CampaignTypeEnum campaignType) {
        this.campaignType = campaignType;
        bannersRule = BannersRuleFactory.getBannersRuleBuilderByCampType(campaignType).withUlogin(CLIENT);
        cmdRule = DirectCmdRule.defaultRule().withRules(bannersRule);
        imagesUploadHelper = (NewImagesUploadHelper) new NewImagesUploadHelper()
                .withBannerImageSteps(
                        cmdRule.cmdSteps().bannerImagesSteps()
                ).withClient(CLIENT);
    }

    @Parameterized.Parameters(name = "Сохранение графических объявлений. Тип кампании: {0}")
    public static Collection<Object[]> testData() {
        return Arrays.asList(new Object[][]{
                {CampaignTypeEnum.TEXT},
                {CampaignTypeEnum.MOBILE}
        });
    }

    @Before
    public void before() {
        directCmdSteps = cmdRule.cmdSteps();
        campaignId = bannersRule.getCampaignId();
        imagesUploadHelper.upload();
        saveBanner = BannersFactory.getDefaultImageBanner(campaignType)
                .withImageAd(new ImageAd().withHash(imagesUploadHelper.getUploadResponse().getHash()));
    }

    @Test
    @Description("Добавление новой группы с графическим баннером")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9244")
    public void addGroupWithImageBanner() {
        Group expectedGroup = bannersRule.getGroup();
        expectedGroup.getBanners().clear();
        expectedGroup.getBanners().add(saveBanner);
        saveGroup(expectedGroup);

        Long newGroupId = getNewGroupId();

        Group actualGroup = directCmdSteps.groupsSteps().getGroup(CLIENT, campaignId, newGroupId)
                .withTags(emptyMap());
        assumeThat("группа сохранилась", actualGroup, notNullValue());
        assumeThat("баннер сохранился", actualGroup.getBanners(), hasSize(1));
        assertThat("параметры графического баннера соответствуют ожиданию", actualGroup.getBanners().get(0),
                beanDiffer(getExpectedBanner()).useCompareStrategy(onlyExpectedFields()));
    }

    @Test
    @Description("Добавление новой группы с текстовым и графическим баннером")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9245")
    public void addGroupWithTextAndImageBanner() {
        Group expectedGroup = bannersRule.getGroup();
        expectedGroup.getBanners().add(saveBanner);
        saveGroup(expectedGroup);

        Long newGroupId = getNewGroupId();

        Group actualGroup = directCmdSteps.groupsSteps().getGroup(CLIENT, campaignId, newGroupId)
                .withTags(emptyMap());
        assumeThat("группа сохранилась", actualGroup, notNullValue());
        assumeThat("баннер сохранился", actualGroup.getBanners(), hasSize(2));
        assertThat("параметры графического баннера соответствуют ожиданию", actualGroup.getBanners().get(1),
                beanDiffer(getExpectedBanner()).useCompareStrategy(onlyExpectedFields()));
    }

    @Test
    @Description("Добавление графического баннера в существующую группу")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9246")
    public void addImageBannerToGroup() {
        Group expectedGroup = directCmdSteps.groupsSteps().getGroup(CLIENT, campaignId, bannersRule.getGroupId())
                .withTags(emptyMap());
        expectedGroup.getBanners().add(saveBanner);
        saveGroup(expectedGroup);

        Group actualGroup = directCmdSteps.groupsSteps().getGroup(CLIENT, campaignId, bannersRule.getGroupId());
        assumeThat("баннер сохранился", actualGroup.getBanners(), hasSize(2));
        assertThat("параметры графического баннера соответствуют ожиданию", actualGroup.getBanners().get(1),
                beanDiffer(getExpectedBanner()).useCompareStrategy(onlyExpectedFields()));
    }

    private Long getNewGroupId() {
        return Long.valueOf(directCmdSteps.groupsSteps().getGroups(CLIENT, campaignId).stream()
                .filter(t -> !t.getAdGroupID().equals(String.valueOf(bannersRule.getGroupId())))
                .findFirst().orElseThrow(() -> new DirectCmdStepsException("новая группа не сохранилась"))
                .getAdGroupID());
    }

    private void saveGroup(Group expectedGroup) {
        bannersRule.saveGroup(GroupsParameters.forExistingCamp(CLIENT, campaignId, expectedGroup));
    }

    private Banner getExpectedBanner() {
        return new Banner()
                .withHref(saveBanner.getUrlProtocol() + saveBanner.getHref())
                .withBannerType(saveBanner.getBannerType())
                .withAdType(saveBanner.getAdType())
                .withImageAd(saveBanner.getImageAd());
    }
}

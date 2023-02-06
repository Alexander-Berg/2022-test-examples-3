package ru.yandex.autotests.direct.cmd.banners.imagecreative;

import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.data.banners.BannersFactory;
import ru.yandex.autotests.direct.cmd.data.banners.GroupsParameters;
import ru.yandex.autotests.direct.cmd.data.commons.banner.Banner;
import ru.yandex.autotests.direct.cmd.data.commons.group.Group;
import ru.yandex.autotests.direct.cmd.data.performanceGroups.common.CreativeBanner;
import ru.yandex.autotests.direct.cmd.rules.BannersRule;
import ru.yandex.autotests.direct.cmd.rules.BannersRuleFactory;
import ru.yandex.autotests.direct.cmd.rules.DirectCmdRule;
import ru.yandex.autotests.direct.cmd.steps.base.DirectCmdStepsException;
import ru.yandex.autotests.direct.cmd.tags.CampTypeTag;
import ru.yandex.autotests.direct.cmd.tags.CmdTag;
import ru.yandex.autotests.direct.cmd.tags.ObjectTag;
import ru.yandex.autotests.direct.cmd.util.PerformanceCampaignHelper;
import ru.yandex.autotests.direct.httpclient.TestEnvironment;
import ru.yandex.autotests.direct.httpclient.TestFeatures;
import ru.yandex.autotests.direct.utils.campaigns.CampaignTypeEnum;
import ru.yandex.autotests.directapi.model.User;
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
@Description("Сохранение ГО с креативом в ТГО/РМП кампаниях")
@Stories(TestFeatures.Banners.CANVAS_BANNERS_PARAMETERS)
@Features(TestFeatures.BANNERS)
@Tag(CmdTag.SAVE_TEXT_ADGROUPS)
@Tag(CmdTag.SAVE_MOBILE_ADGROUPS)
@Tag(ObjectTag.BANNER)
@Tag(CampTypeTag.TEXT)
@Tag(CampTypeTag.MOBILE)
@RunWith(Parameterized.class)
public class SaveCreativeImageBannerCampaignTest {

    private static final String CLIENT = "at-direct-creative-construct1";

    @ClassRule
    public static DirectCmdRule defaultClassRule = DirectCmdRule.defaultClassRule();
    @Rule
    public DirectCmdRule cmdRule;
    public CampaignTypeEnum campaignType;
    private BannersRule bannersRule;
    private Long campaignId;
    private Banner saveBanner;
    private Long creativeId;

    public SaveCreativeImageBannerCampaignTest(CampaignTypeEnum campaignType) {
        this.campaignType = campaignType;
        bannersRule = BannersRuleFactory.getBannersRuleBuilderByCampType(campaignType).withUlogin(CLIENT);
        cmdRule = DirectCmdRule.defaultRule().withRules(bannersRule);
    }

    @Parameterized.Parameters(name = "Сохранение объявлений с креативом. Тип кампании: {0}")
    public static Collection<Object[]> testData() {
        return Arrays.asList(new Object[][]{
                {CampaignTypeEnum.TEXT},
                {CampaignTypeEnum.MOBILE}
        });
    }

    @Before
    public void before() {
        campaignId = bannersRule.getCampaignId();
        creativeId = TestEnvironment.newDbSteps().useShardForLogin(CLIENT).perfCreativesSteps()
                .saveDefaultCanvasCreativesForClient(Long.valueOf(User.get(CLIENT).getClientID()));
        saveBanner = BannersFactory.getDefaultImageBanner(campaignType)
                .withCreativeBanner(new CreativeBanner().withCreativeId(creativeId));
    }

    @After
    public void after() {
        if (creativeId != null) {
            PerformanceCampaignHelper.deleteCreativeQuietly(CLIENT, creativeId);
        }
    }

    @Test
    @Description("Добавление новой группы с объявлением с креативом")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9270")
    public void addGroupWithImageCreativeBanner() {
        Group expectedGroup = bannersRule.getGroup();
        expectedGroup.getBanners().clear();
        expectedGroup.getBanners().add(saveBanner);
        saveGroup(expectedGroup);

        Long newGroupId = getNewGroupId();
        Group actualGroup = cmdRule.cmdSteps().groupsSteps().getGroup(CLIENT, campaignId, newGroupId)
                .withTags(emptyMap());

        assumeThat("группа сохранилась", actualGroup, notNullValue());
        assumeThat("баннер сохранился", actualGroup.getBanners(), hasSize(1));
        assertThat("параметры баннера с креативом соответствуют ожиданию", actualGroup.getBanners().get(0),
                beanDiffer(getExpectedBanner()).useCompareStrategy(onlyExpectedFields()));
    }

    @Test
    @Description("Добавление новой группы с текстовым баннером и баннером с креативом")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9272")
    public void addGroupWithTextAndImageCreativeBanner() {
        Group expectedGroup = bannersRule.getGroup();
        expectedGroup.getBanners().add(saveBanner);
        saveGroup(expectedGroup);

        Long newGroupId = getNewGroupId();

        Group actualGroup = cmdRule.cmdSteps().groupsSteps().getGroup(CLIENT, campaignId, newGroupId)
                .withTags(emptyMap());
        assumeThat("группа сохранилась", actualGroup, notNullValue());
        assumeThat("баннер сохранился", actualGroup.getBanners(), hasSize(2));
        assertThat("параметры баннера с креативом соответствуют ожиданию", actualGroup.getBanners().get(1),
                beanDiffer(getExpectedBanner()).useCompareStrategy(onlyExpectedFields()));
    }

    @Test
    @Description("Добавление графического баннера в существующую группу")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9271")
    public void addImageCreativeBannerToGroup() {
        Group expectedGroup = cmdRule.cmdSteps().groupsSteps().getGroup(CLIENT, campaignId, bannersRule.getGroupId())
                .withTags(emptyMap());
        expectedGroup.getBanners().add(saveBanner);
        saveGroup(expectedGroup);

        Group actualGroup = cmdRule.cmdSteps().groupsSteps().getGroup(CLIENT, campaignId, bannersRule.getGroupId());
        assumeThat("баннер сохранился", actualGroup.getBanners(), hasSize(2));
        assertThat("параметры баннера с креативом соответствуют ожиданию", actualGroup.getBanners().get(1),
                beanDiffer(getExpectedBanner()).useCompareStrategy(onlyExpectedFields()));
    }

    private Long getNewGroupId() {
        return Long.valueOf(cmdRule.cmdSteps().groupsSteps().getGroups(CLIENT, campaignId).stream()
                .filter(t -> !t.getAdGroupID().equals(String.valueOf(bannersRule.getGroupId())))
                .findFirst().orElseThrow(() -> new DirectCmdStepsException("новая группа не сохранилась"))
                .getAdGroupID());
    }

    private void saveGroup(Group expectedGroup) {
        bannersRule.saveGroup(GroupsParameters.forExistingCamp(CLIENT, campaignId, expectedGroup));
    }

    private Banner getExpectedBanner() {
        return new Banner()
                .withCreativeBanner(saveBanner.getCreativeBanner())
                .withHref(saveBanner.getUrlProtocol() + saveBanner.getHref())
                .withBannerType(saveBanner.getBannerType())
                .withAdType(saveBanner.getAdType())
                .withImageAd(saveBanner.getImageAd());
    }

}

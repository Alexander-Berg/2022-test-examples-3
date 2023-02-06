package ru.yandex.autotests.direct.cmd.banners.imagecreative;

import java.util.Arrays;
import java.util.Collection;

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
import ru.yandex.autotests.direct.cmd.rules.CreativeBannerRule;
import ru.yandex.autotests.direct.cmd.rules.DirectCmdRule;
import ru.yandex.autotests.direct.cmd.tags.CampTypeTag;
import ru.yandex.autotests.direct.cmd.tags.CmdTag;
import ru.yandex.autotests.direct.cmd.tags.ObjectTag;
import ru.yandex.autotests.direct.cmd.util.PerformanceCampaignHelper;
import ru.yandex.autotests.direct.httpclient.TestEnvironment;
import ru.yandex.autotests.direct.httpclient.TestFeatures;
import ru.yandex.autotests.direct.utils.campaigns.CampaignTypeEnum;
import ru.yandex.autotests.directapi.model.User;
import ru.yandex.autotests.httpclient.lite.core.exceptions.BackEndClientException;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

import static java.util.Collections.emptyMap;
import static org.hamcrest.Matchers.hasSize;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assumeThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies.onlyExpectedFields;


@Aqua.Test
@Description("Изменение креатива ГО в ТГО/РМП кампаниях")
@Stories(TestFeatures.Banners.CANVAS_BANNERS_PARAMETERS)
@Features(TestFeatures.BANNERS)
@Tag(CmdTag.SAVE_TEXT_ADGROUPS)
@Tag(CmdTag.SAVE_MOBILE_ADGROUPS)
@Tag(ObjectTag.BANNER)
@Tag(CampTypeTag.TEXT)
@Tag(CampTypeTag.MOBILE)
@RunWith(Parameterized.class)
public class ChangeCreativeImageBannerTest {

    private static final String CLIENT = "at-direct-creative-construct";

    @ClassRule
    public static DirectCmdRule stepsClassRule = DirectCmdRule.defaultClassRule();
    @Rule
    public DirectCmdRule cmdRule;
    private CreativeBannerRule bannersRule;
    private Group savingGroup;
    private Long prevCreativeId;
    private Long newCreativeId;
    private CampaignTypeEnum campaignType;

    public ChangeCreativeImageBannerTest(CampaignTypeEnum campaignType) {
        this.campaignType = campaignType;
        bannersRule = new CreativeBannerRule(campaignType).withUlogin(CLIENT);
        cmdRule = DirectCmdRule.defaultRule().withRules(bannersRule);
    }

    @Parameterized.Parameters(name = "Изменение креатива в ГО. Тип кампании: {0}")
    public static Collection<Object[]> testData() {
        return Arrays.asList(new Object[][]{
                {CampaignTypeEnum.TEXT},
                {CampaignTypeEnum.MOBILE}
        });
    }


    @Before
    public void before() {
        prevCreativeId = bannersRule.getCreativeId();
        newCreativeId = TestEnvironment.newDbSteps().useShardForLogin(CLIENT).perfCreativesSteps()
                .saveDefaultCanvasCreativesForClient(Long.parseLong(User.get(CLIENT).getClientID()));

        savingGroup = bannersRule.getCurrentGroup().withTags(emptyMap());
        prepareSave(savingGroup);
        savingGroup.getBanners().get(0).withCreativeBanner(new CreativeBanner().withCreativeId(newCreativeId));
    }

    @After
    public void after() {
        if (prevCreativeId != null) {
            PerformanceCampaignHelper.deleteCreativeQuietly(CLIENT, prevCreativeId);
        }

        if (newCreativeId != null) {
            PerformanceCampaignHelper.deleteCreativeQuietly(CLIENT, newCreativeId);
        }
    }

    @Test
    @Description("Изменение креатива ГО")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9256")
    public void changeCreativeImageBanner() {
        GroupsParameters request = GroupsParameters.forExistingCamp(CLIENT, bannersRule.getCampaignId(), savingGroup);
        cmdRule.cmdSteps().groupsSteps().postSaveTextAdGroups(request);

        Group group = bannersRule.getCurrentGroup();
        assumeThat("В группе 1 баннер", group.getBanners(), hasSize(1));

        Banner actualBanner = group.getBanners().get(0);

        assertThat("Креатив изменился", actualBanner, beanDiffer(getExpectedBanner())
                .useCompareStrategy(onlyExpectedFields()));
    }

    private Banner getExpectedBanner() {
        return new Banner().withCreativeBanner(
                new CreativeBanner()
                        .withCreativeId(newCreativeId)
        );
    }

    private void prepareSave(Group group) {
        switch (campaignType) {
            case TEXT:
                break;
            case MOBILE:
                BannersFactory.addNeededAttribute(group.getBanners().get(0));
                break;
            default:
                throw new BackEndClientException("Не указан тип кампании");
        }
    }
}

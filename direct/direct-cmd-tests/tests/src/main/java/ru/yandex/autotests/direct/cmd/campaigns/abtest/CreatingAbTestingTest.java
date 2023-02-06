package ru.yandex.autotests.direct.cmd.campaigns.abtest;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.data.CmdBeansMaps;
import ru.yandex.autotests.direct.cmd.data.savecamp.SaveCampRequest;
import ru.yandex.autotests.direct.cmd.rules.DirectCmdRule;
import ru.yandex.autotests.direct.cmd.steps.campaings.AbTestingHelper;
import ru.yandex.autotests.direct.cmd.tags.CampTypeTag;
import ru.yandex.autotests.direct.cmd.tags.CmdTag;
import ru.yandex.autotests.direct.cmd.tags.ObjectTag;
import ru.yandex.autotests.direct.cmd.tags.TrunkTag;
import ru.yandex.autotests.direct.cmd.util.BeanLoadHelper;
import ru.yandex.autotests.direct.httpclient.TestEnvironment;
import ru.yandex.autotests.direct.httpclient.TestFeatures;
import ru.yandex.autotests.direct.utils.campaigns.CampaignTypeEnum;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

import java.util.Arrays;
import java.util.Collection;

//TESTIRT-9619
@Aqua.Test
@Description("Создание эксперимента")
@Stories(TestFeatures.Campaigns.CREATE_AB_TEST)
@Features(TestFeatures.CAMPAIGNS)
@Tag(CmdTag.CREATE_AB_TEST)
@Tag(ObjectTag.CAMPAIGN)
@Tag(CampTypeTag.TEXT)
@Tag(TrunkTag.YES)
@RunWith(Parameterized.class)
public class CreatingAbTestingTest {
    protected static final String CLIENT = "at-direct-backend-rus-os9";
    public static SaveCampRequest requestText = BeanLoadHelper.loadCmdBean(
            CmdBeansMaps.MEDIA_TYPE_TO_TEMPLATE.get(CampaignTypeEnum.TEXT),
            SaveCampRequest.class
    );
    public static SaveCampRequest requestDMO = BeanLoadHelper.loadCmdBean(
            CmdBeansMaps.MEDIA_TYPE_TO_TEMPLATE.get(CampaignTypeEnum.DTO),
            SaveCampRequest.class
    );
    @ClassRule
    public static DirectCmdRule defaultClassRule = DirectCmdRule.defaultClassRule();
    protected static Integer SHARD;
    @Parameterized.Parameter(0)
    public Integer percent;
    @Parameterized.Parameter(1)
    public SaveCampRequest saveCampRequestFirst;
    @Parameterized.Parameter(2)
    public SaveCampRequest saveCampRequestSecond;
    @Parameterized.Parameter(3)
    public String text;
    @Rule
    public DirectCmdRule cmdRule = DirectCmdRule.defaultRule();
    private AbTestingHelper abTestingHelper;

    @Parameterized.Parameters(name = "Эксперимент с кампаниями {3} типа; процент: {0}")
    public static Collection<Object[]> testData() {
        return Arrays.asList(new Object[][]{
                {70, requestText, requestText, "одного"},
                {50, requestText, requestText, "одного"},
//                {50, requestDMO, requestText, "разного"},
                {30, requestText, requestText, "одного"}
        });
    }

    @Before
    public void before() {
        SHARD = TestEnvironment.newDbSteps().shardingSteps().getShardByLogin(CLIENT);
        abTestingHelper = new AbTestingHelper().withClient(CLIENT).withCmdRule(cmdRule);
    }

    @Test
    @Description("создаем эксперимент")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9384")
    public void sameCampsWithDiffPercentage() {
        percent = 70;
        Long firstCampId = cmdRule.cmdSteps().campaignSteps().saveNewCampaign(saveCampRequestFirst.withUlogin(CLIENT));
        abTestingHelper.createGroup(firstCampId);
        Long secondCampId = cmdRule.cmdSteps().campaignSteps().saveNewCampaign(saveCampRequestSecond.withUlogin(CLIENT));
        abTestingHelper.createGroup(secondCampId);
        abTestingHelper.saveAndCheck(percent, firstCampId, secondCampId);
    }
}

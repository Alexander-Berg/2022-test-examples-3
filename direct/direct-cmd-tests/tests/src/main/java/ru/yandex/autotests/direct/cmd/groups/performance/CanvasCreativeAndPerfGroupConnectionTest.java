package ru.yandex.autotests.direct.cmd.groups.performance;

import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.data.Logins;
import ru.yandex.autotests.direct.cmd.data.banners.GroupsParameters;
import ru.yandex.autotests.direct.cmd.data.commons.group.Group;
import ru.yandex.autotests.direct.cmd.data.performanceGroups.savePerformanceAdGroups.GroupErrorsResponse;
import ru.yandex.autotests.direct.cmd.rules.DirectCmdRule;
import ru.yandex.autotests.direct.cmd.rules.PerformanceBannersRule;
import ru.yandex.autotests.direct.cmd.tags.CampTypeTag;
import ru.yandex.autotests.direct.cmd.tags.CmdTag;
import ru.yandex.autotests.direct.cmd.tags.ObjectTag;
import ru.yandex.autotests.direct.cmd.tags.TrunkTag;
import ru.yandex.autotests.direct.httpclient.TestEnvironment;
import ru.yandex.autotests.direct.httpclient.TestFeatures;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;

// таск:
@Aqua.Test
@Description("Проверка привязки canvas креатива к perfomance banner")
@Stories(TestFeatures.Groups.FEEDS)
@Features(TestFeatures.GROUPS)
@Tag(CmdTag.SAVE_PERFORMANCE_AD_GROUPS)
@Tag(ObjectTag.GROUP)
@Tag(CampTypeTag.PERFORMANCE)
@Tag(TrunkTag.YES)
public class CanvasCreativeAndPerfGroupConnectionTest {
    protected static final String CLIENT_FIRST = "at-direct-search-creatives1";
    protected static final String CLIENT_SECOND = Logins.DEFAULT_CLIENT;
    private static final String ERROR_TEXT_ILLIGAL_CREATIVE_ID = "Ошибка: указан некорректный или несуществующий номер креатива";
    @ClassRule
    public static DirectCmdRule defaultClassRule = DirectCmdRule.defaultClassRule();
    private PerformanceBannersRule bannersRule = (ru.yandex.autotests.direct.cmd.rules.PerformanceBannersRule)
            new PerformanceBannersRule().withUlogin(CLIENT_FIRST);
    @Rule
    public DirectCmdRule cmdRule = DirectCmdRule.defaultRule().withRules(bannersRule);
    private Long firstClientCreativeId;
    private Long firstClientId;

    @Before
    public void before() {
        firstClientId = Long.valueOf(cmdRule.darkSideSteps().getClientFakeSteps().getClientData(CLIENT_FIRST).getClientID());
        firstClientCreativeId = TestEnvironment.newDbSteps().useShardForLogin(CLIENT_FIRST)
                .perfCreativesSteps().saveDefaultCanvasCreativesForClient(firstClientId);

    }

    @Test
    @Description("Привязка canvas креатива к perfomance banner")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9819")
    public void connectCanvasCreativeWithPerfBanner() {
        Group group = bannersRule.getGroupRequest();
        group.getBanners().get(0)
                .getCreativeBanner().setCreativeId(firstClientCreativeId);
        GroupsParameters groupRequest = GroupsParameters
                .forExistingCamp(CLIENT_FIRST, bannersRule.getCampaignId(), group);
        GroupErrorsResponse response = cmdRule.cmdSteps().groupsSteps()
                .postSavePerformanceAdGroupsErrorResponse((groupRequest));
        assertThat("ошибка соотвествует ожиданиям", response.getErrors().getGenericErrors().get(0).getText(),
                containsString(ERROR_TEXT_ILLIGAL_CREATIVE_ID));

    }

    @After
    public void after() {
        TestEnvironment.newDbSteps().perfCreativesSteps().deletePerfCreatives(firstClientCreativeId);
    }

}

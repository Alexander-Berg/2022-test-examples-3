package ru.yandex.autotests.direct.cmd.campaigns.copycamp;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.data.commons.banner.Banner;
import ru.yandex.autotests.direct.cmd.data.commons.banner.Callout;
import ru.yandex.autotests.direct.cmd.data.commons.group.Group;
import ru.yandex.autotests.direct.cmd.rules.DirectCmdRule;
import ru.yandex.autotests.direct.cmd.rules.TextBannersRule;
import ru.yandex.autotests.direct.cmd.tags.CampTypeTag;
import ru.yandex.autotests.direct.cmd.tags.CmdTag;
import ru.yandex.autotests.direct.cmd.tags.ObjectTag;
import ru.yandex.autotests.direct.db.models.jooq.ppc.enums.AdditionsItemCalloutsStatusmoderate;
import ru.yandex.autotests.direct.httpclient.TestEnvironment;
import ru.yandex.autotests.direct.httpclient.TestFeatures;
import ru.yandex.autotests.directapi.model.User;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assumeThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies.onlyExpectedFields;

@Aqua.Test
@Description("Проверка копирования текстовой кампании с callouts (copyCamp)")
@Stories(TestFeatures.Campaigns.COPY_CAMP)
@Features(TestFeatures.CAMPAIGNS)
@Tag(CmdTag.COPY_CAMP)
@Tag(ObjectTag.CAMPAIGN)
@Tag(ObjectTag.CALLOUTS)
@Tag(CampTypeTag.TEXT)
@RunWith(Parameterized.class)
public class CopyCampWithCalloutsTest {

    @ClassRule
    public static DirectCmdRule defaultClassRuleChain = DirectCmdRule.defaultClassRule();
    public TextBannersRule bannersRule;
    @Rule
    public DirectCmdRule cmdRule;
    private List<Callout> callouts = Arrays.asList(
            new Callout().withCalloutText("BMW"),
            new Callout().withCalloutText("bmw")
    );
    private Long newCid;

    private String fromLogin;
    private String toLogin;
    private String copyModerateStatus;
    private AdditionsItemCalloutsStatusmoderate expectedStatusModerate;

    public CopyCampWithCalloutsTest(String fromLogin, String toLogin, String copyModerateStatus, AdditionsItemCalloutsStatusmoderate expectedStatusModerate) {
        this.fromLogin = fromLogin;
        this.toLogin = toLogin;
        this.copyModerateStatus = copyModerateStatus;
        this.expectedStatusModerate = expectedStatusModerate;

        deleteAllCallouts(fromLogin);
        deleteAllCallouts(toLogin);

        bannersRule = new TextBannersRule()
                .overrideBannerTemplate(new Banner()
                        .withCallouts(callouts))
                .withUlogin(fromLogin);
        cmdRule = DirectCmdRule.defaultRule().withRules(bannersRule);
    }

    @Parameterized.Parameters(name = "Копирование текстовой кампании с уточнениями." +
            " от {0} к {1} (с копированием statusModerate = {3}/{2})")
    public static Collection<Object[]> testData() {
        return Arrays.asList(new Object[][]{
                {"at-direct-banners-callouts-25", "at-direct-banners-callouts-25", "1", AdditionsItemCalloutsStatusmoderate.Yes},
                {"at-direct-banners-callouts-25", "at-direct-banners-callouts-25", "0", AdditionsItemCalloutsStatusmoderate.Yes},
                {"at-direct-banners-callouts-25", "at-direct-banners-callouts-26", "1", AdditionsItemCalloutsStatusmoderate.Ready}, // вернуть в Yes после DIRECT-58374
                {"at-direct-banners-callouts-25", "at-direct-banners-callouts-26", "0", AdditionsItemCalloutsStatusmoderate.Ready},
                {"at-direct-banners-callouts-25", "at-direct-backend-c", "1", AdditionsItemCalloutsStatusmoderate.Ready}, // клиенту в другом шарде, вернуть в Yes после DIRECT-58374
                {"at-direct-banners-callouts-25", "at-direct-backend-c", "0", AdditionsItemCalloutsStatusmoderate.Ready} // клиенту в другом шарде
        });
    }

    @Before
    public void before() {
        Group group = cmdRule.cmdSteps().groupsSteps()
                .getGroup(fromLogin, bannersRule.getCampaignId(), bannersRule.getGroupId());
        assumeThat("текстовые дополнения сохранились", group.getBanners().get(0).getCallouts(), hasSize(2));

        moderateCallouts();
        callouts.stream().forEach(t -> t.withStatusModerate(expectedStatusModerate));

    }

    @After
    public void after() {
        if (newCid != null) {
            cmdRule.apiAggregationSteps().deleteActiveCampaignQuietly(toLogin, newCid);
        }
    }

    @Test
    @ru.yandex.qatools.allure.annotations.TestCaseId("9400")
    public void checkCopyCallouts() {
        newCid = cmdRule.cmdSteps().copyCampSteps()
                .copyCamp(fromLogin, toLogin, bannersRule.getCampaignId(), copyModerateStatus);

        Banner copiedBanner = cmdRule.cmdSteps().campaignSteps().getShowCamp(toLogin, newCid.toString())
                .getGroups().get(0);
        assertThat("текстовые дополнения соответсвуют ожиданию", copiedBanner.getCallouts(),
                beanDiffer(callouts).useCompareStrategy(onlyExpectedFields()));
    }

    private void moderateCallouts() {
        List<Callout> callouts = cmdRule.cmdSteps().bannersAdditionsSteps().getCallouts(fromLogin).getCallouts();

        callouts.forEach(t -> TestEnvironment.newDbSteps().useShardForLogin(fromLogin)
                .bannerAdditionsSteps().setAdditionsItemCalloutsStatusModerated(
                        t.getAdditionsItemId(), AdditionsItemCalloutsStatusmoderate.Yes));
    }

    private void deleteAllCallouts(String client) {
        TestEnvironment.newDbSteps().useShardForLogin(client).bannerAdditionsSteps()
                .clearCalloutsForClient(Long.valueOf(User.get(client).getClientID()));
    }
}

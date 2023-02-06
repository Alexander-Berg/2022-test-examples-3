package ru.yandex.autotests.direct.httpclient.firstAid.AcceptOptimizeTest;

import java.util.Arrays;
import java.util.stream.Collectors;

import org.apache.commons.lang.StringUtils;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestWatcher;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.data.banners.BannersFactory;
import ru.yandex.autotests.direct.cmd.data.commons.group.Group;
import ru.yandex.autotests.direct.cmd.data.commons.phrase.Phrase;
import ru.yandex.autotests.direct.cmd.rules.DirectCmdRule;
import ru.yandex.autotests.direct.cmd.rules.TextBannersRule;
import ru.yandex.autotests.direct.cmd.tags.CmdTag;
import ru.yandex.autotests.direct.cmd.tags.OldTag;
import ru.yandex.autotests.direct.httpclient.CocaineSteps;
import ru.yandex.autotests.direct.httpclient.TestFeatures;
import ru.yandex.autotests.direct.httpclient.core.DirectResponse;
import ru.yandex.autotests.direct.httpclient.data.CSRFToken;
import ru.yandex.autotests.direct.httpclient.data.Logins;
import ru.yandex.autotests.direct.httpclient.data.textresources.firstAid.AcceptOptimizeErrors;
import ru.yandex.autotests.direct.utils.textresource.TextResourceFormatter;
import ru.yandex.autotests.directapi.darkside.model.Status;
import ru.yandex.autotests.directapi.model.User;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

import static org.hamcrest.Matchers.equalTo;

/**
 * @author : Alex Samokhin (alex-samo@yandex-team.ru)
 *         Date: 12.05.15
 *         https://st.yandex-team.ru/TESTIRT-4962
 */

@Aqua.Test
@Description("Проверка валидации при принятии незавершенных рекомендаций первой помощи (первый шаг) с помощью контроллера acceptOptimize")
@Stories(TestFeatures.FirstAid.ACCEPT_OPTIMIZE)
@Features(TestFeatures.FIRST_AID)
@Tag(CmdTag.ACCEPT_OPTIMIZE)
@Tag(OldTag.YES)
public class AcceptOptimizeRequestStatusValidationTest {

    public static final String CLIENT = "at-direct-b-firstaid-c5";
    private static final Float CAMPAIGN_MONEY = 1000f;
    @ClassRule
    public static DirectCmdRule defaultClassRule = DirectCmdRule.defaultClassRule();

    private TextBannersRule bannersRule = new TextBannersRule()
            .overrideGroupTemplate(new Group().withBanners(
                    Arrays.asList(BannersFactory.getDefaultTextBanner(), BannersFactory.getDefaultTextBanner())))
            .withUlogin(CLIENT);

    @Rule
    public DirectCmdRule cmdRule = DirectCmdRule.defaultRule().withRules(new TestWatcher() {
        @Override
        protected void starting(org.junit.runner.Description description) {
            cmdRule.apiAggregationSteps().deleteAllCampaigns(CLIENT);
        }
    }, bannersRule);

    private Integer campaignId;
    private String optimizeRequestID;
    private String phraseIds;

    @Before
    public void before() {
        campaignId = bannersRule.getCampaignId().intValue();
        cmdRule.apiAggregationSteps().activateCampaignWithMoney(CLIENT, campaignId.longValue(), CAMPAIGN_MONEY);
        cmdRule.darkSideSteps().getClientFakeSteps().setShowFaTeaserToUser(CLIENT, Status.YES);

        cmdRule.oldSteps().onPassport().authoriseAs(Logins.SUPER, User.get(Logins.SUPER).getPassword());
        CSRFToken csrfToken = CocaineSteps.getCsrfTokenFromCocaine(User.get(Logins.SUPER).getPassportUID());
        optimizeRequestID = cmdRule.oldSteps().firstHelpSteps().requestFirstHelp(CLIENT, campaignId, csrfToken);
        Group group = bannersRule.getCurrentGroup();
        phraseIds = StringUtils.join(group.getPhrases().stream()
                .map(Phrase::getId).collect(Collectors.toList()), ",");
    }

    @Test
    @Description("Проверяем валидацию при принятии рекомендаций до начала оптимизации баннеров")
    @ru.yandex.qatools.allure.annotations.TestCaseId("10470")
    public void acceptOptimizeBeforeOptimizeVaidationTest() {
        CSRFToken csrfToken = CocaineSteps.getCsrfTokenFromCocaine(User.get(CLIENT).getPassportUID());
        cmdRule.oldSteps().onPassport().authoriseAs(CLIENT, User.get(CLIENT).getPassword());
        DirectResponse response = cmdRule.oldSteps().firstHelpSteps()
                .acceptOptimize(campaignId, phraseIds, optimizeRequestID, csrfToken);
        cmdRule.oldSteps().commonSteps().checkDirectResponseError(response, equalTo(TextResourceFormatter.resource(
                AcceptOptimizeErrors.INCORRECT_CAMPAIGN_STATUS).toString()));
    }

    @Test
    @Description("Проверяем валидацию при принятии рекомендаций до заверешния оптимизации баннеров")
    @ru.yandex.qatools.allure.annotations.TestCaseId("10471")
    public void acceptOptimizeBeforeCompleteOptimizingTest() {
        String adGroupIds = bannersRule.getGroupId().toString();
        CSRFToken csrfToken = CocaineSteps.getCsrfTokenFromCocaine(User.get(Logins.SUPER).getPassportUID());
        cmdRule.oldSteps().firstHelpSteps().sendOptimize(CLIENT, campaignId, adGroupIds, optimizeRequestID, csrfToken);
        csrfToken = CocaineSteps.getCsrfTokenFromCocaine(User.get(CLIENT).getPassportUID());
        cmdRule.oldSteps().onPassport().authoriseAs(CLIENT, User.get(CLIENT).getPassword());
        DirectResponse response =
                cmdRule.oldSteps().firstHelpSteps().acceptOptimize(campaignId, phraseIds, optimizeRequestID, csrfToken);
        cmdRule.oldSteps().commonSteps().checkDirectResponseError(response, equalTo(TextResourceFormatter.resource(
                AcceptOptimizeErrors.INCORRECT_CAMPAIGN_STATUS).toString()));
    }
}

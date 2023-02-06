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
import ru.yandex.autotests.direct.cmd.tags.TrunkTag;
import ru.yandex.autotests.direct.httpclient.CocaineSteps;
import ru.yandex.autotests.direct.httpclient.TestFeatures;
import ru.yandex.autotests.direct.httpclient.core.DirectResponse;
import ru.yandex.autotests.direct.httpclient.data.CSRFToken;
import ru.yandex.autotests.direct.httpclient.data.Logins;
import ru.yandex.autotests.direct.httpclient.data.firsthelp.AcceptOptimizeRequestBean;
import ru.yandex.autotests.direct.httpclient.data.textresources.campaigns.CampaignValidationErrors;
import ru.yandex.autotests.direct.httpclient.util.PropertyLoader;
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
 *         Date: 07.05.15
 *         https://st.yandex-team.ru/TESTIRT-4962
 */

@Aqua.Test
@Description("Принятие рекомендаций первой помощи (первый шаг) с помощью контроллера acceptOptimize")
@Stories(TestFeatures.FirstAid.ACCEPT_OPTIMIZE)
@Features(TestFeatures.FIRST_AID)
@Tag(TrunkTag.YES)
@Tag(CmdTag.ACCEPT_OPTIMIZE)
@Tag(OldTag.YES)
public class AcceptOptimizeTest {

    public static final String CLIENT = "at-direct-b-firstaid-c4";
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
    private AcceptOptimizeRequestBean acceptOptimizeRequestBean;
    private String phraseIds;
    private CSRFToken csrfToken;

    @Before
    public void before() {
        csrfToken = CocaineSteps.getCsrfTokenFromCocaine(User.get(Logins.SUPER).getPassportUID());

        campaignId = bannersRule.getCampaignId().intValue();
        cmdRule.apiAggregationSteps().activateCampaignWithMoney(CLIENT, campaignId.longValue(), CAMPAIGN_MONEY);
        cmdRule.darkSideSteps().getClientFakeSteps().setShowFaTeaserToUser(CLIENT, Status.YES);

        cmdRule.oldSteps().onPassport().authoriseAs(Logins.SUPER, User.get(Logins.SUPER).getPassword());
        optimizeRequestID = cmdRule.oldSteps().firstHelpSteps().requestFirstHelp(CLIENT, campaignId, csrfToken);
        Group group = bannersRule.getCurrentGroup();
        phraseIds = StringUtils.join(group.getPhrases().stream()
                .map(Phrase::getId).collect(Collectors.toList()), ",");
        cmdRule.oldSteps().firstHelpSteps()
                .sendOptimize(CLIENT, campaignId, bannersRule.getGroupId().toString(), optimizeRequestID, csrfToken);
        cmdRule.oldSteps().firstHelpSteps().completeOptimizing(campaignId, optimizeRequestID, csrfToken);

        PropertyLoader<AcceptOptimizeRequestBean> loader = new PropertyLoader<>(AcceptOptimizeRequestBean.class);
        acceptOptimizeRequestBean = loader.getHttpBean("acceptOptimizeRequestBean");
        acceptOptimizeRequestBean.setCid(String.valueOf(campaignId));
        acceptOptimizeRequestBean.setOptimizeRequestId(optimizeRequestID);
        acceptOptimizeRequestBean.setPhIds(phraseIds);
    }

    @Test
    @Description("Проверяем валидацию пустого cid")
    @ru.yandex.qatools.allure.annotations.TestCaseId("10472")
    public void emptyCidValidationTest() {
        acceptOptimizeRequestBean.setCid(null);
        DirectResponse response = cmdRule.oldSteps().firstHelpSteps()
                .acceptOptimize(acceptOptimizeRequestBean, csrfToken);
        cmdRule.oldSteps().commonSteps().checkDirectResponseErrorCMDText(response,
                TextResourceFormatter.resource(CampaignValidationErrors.EMPTY_CID).toString());
    }

    @Test
    @Description("Проверяем валидацию при пустом параметре optimize_request_id")
    @ru.yandex.qatools.allure.annotations.TestCaseId("10476")
    public void emptyOptimizeRequestIdValidationTest() {
        acceptOptimizeRequestBean.setOptimizeRequestId(null);
        DirectResponse response = cmdRule.oldSteps().firstHelpSteps()
                .acceptOptimize(acceptOptimizeRequestBean, csrfToken);
        cmdRule.oldSteps().commonSteps().checkDirectResponseError(response, equalTo(TextResourceFormatter.resource(
                CampaignValidationErrors.THIS_CAMPAIGN_DOESNT_BELONG_TO_USER).toString()));
    }


}

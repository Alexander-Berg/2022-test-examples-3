package ru.yandex.autotests.direct.cmd.teaser;

//Task: TESTIRT-9250.

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.rules.DirectCmdRule;
import ru.yandex.autotests.direct.cmd.rules.TextBannersRule;
import ru.yandex.autotests.direct.cmd.tags.CampTypeTag;
import ru.yandex.autotests.direct.cmd.tags.CmdTag;
import ru.yandex.autotests.direct.cmd.tags.ObjectTag;
import ru.yandex.autotests.direct.cmd.tags.TrunkTag;
import ru.yandex.autotests.direct.db.beans.UserOptionsModel.UserOptionsModel;
import ru.yandex.autotests.direct.db.models.jooq.ppc.tables.records.UsersOptionsRecord;
import ru.yandex.autotests.direct.httpclient.TestEnvironment;
import ru.yandex.autotests.direct.httpclient.TestFeatures;
import ru.yandex.autotests.directapi.model.User;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

import static org.hamcrest.core.IsEqual.equalTo;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assumeThat;

@Aqua.Test
@Description("Проверка появления тизера")
@Stories(TestFeatures.UserOptions.AJAX_SET_RECOMEDATIONS_EMAIL)
@Features(TestFeatures.USER_OPTIONS)
@Tag(CmdTag.AJAX_SET_RECOMEDATIONS_EMAIL)
@Tag(CmdTag.AJAX_USER_OPTIONS)
@Tag(ObjectTag.CAMPAIGN)
@Tag(CampTypeTag.TEXT)
@Tag(TrunkTag.YES)
public class EmailTeaserTest {
    private static final String CLIENT = "at-direct-teaser";
    private static final String EMAIL = "at-direct-teaser@yandex.ru";
    @ClassRule
    public static DirectCmdRule defaultClassRule = DirectCmdRule.defaultClassRule();

    private TextBannersRule bannersRule = new TextBannersRule().withUlogin(CLIENT);

    @Rule
    public DirectCmdRule cmdRule = DirectCmdRule.defaultRule().withRules(bannersRule);
    private User user;

    @Before
    public void before() {
        user = User.get(CLIENT);
        cmdRule.cmdSteps().ajaxUserOptionsSteps().postAjaxUserOptions(CLIENT, "0");
        UsersOptionsRecord actualOptions = TestEnvironment.newDbSteps().useShardForLogin(CLIENT).usersOptionsSteps().getUsersOptions(Long.valueOf(user.getPassportUID()));
        assumeThat("Показ тизера соответсует ожиданиям", UserOptionsModel.fromString(actualOptions.getOptions()).getEmailTeaser(), equalTo("0"));
    }

    @Test
    @Description("выставляем непоказ тизера")
    @ru.yandex.qatools.allure.annotations.TestCaseId("10029")
    public void setHideByAjaxUserOptions() {
        String hide = "1";
        cmdRule.cmdSteps().ajaxUserOptionsSteps().postAjaxUserOptions(CLIENT, hide);
        UsersOptionsRecord actualOptions = TestEnvironment.newDbSteps().useShardForLogin(CLIENT).usersOptionsSteps().getUsersOptions(Long.valueOf(user.getPassportUID()));
        assertThat("Показ тизера соответсует ожиданиям", UserOptionsModel.fromString(actualOptions.getOptions()).getEmailTeaser(), equalTo("1"));
    }

    @Test
    @Description("выставляем email для показа тизера")
    @ru.yandex.qatools.allure.annotations.TestCaseId("10030")
    public void setHideByRecommendationsEmail() {
        cmdRule.cmdSteps().ajaxSetRecomedationsEmailSteps().postAjaxSetRecomendationsEmail(CLIENT, EMAIL);
        UsersOptionsRecord actualOptions = TestEnvironment.newDbSteps().useShardForLogin(CLIENT).usersOptionsSteps().getUsersOptions(Long.valueOf(user.getPassportUID()));
        assumeThat("Показ тизера соответсует ожиданиям", UserOptionsModel.fromString(actualOptions.getOptions()).getEmailTeaser(), equalTo("1"));
        assertThat("почта соответстует ожиданиям", actualOptions.getRecommendationsEmail(), equalTo(EMAIL));
    }

    @Test
    @Description("проверяем что после удаления кампании не слетел email")
    @ru.yandex.qatools.allure.annotations.TestCaseId("10031")
    public void setHideByRecommendationsEmailAfterDelete() {
        cmdRule.apiAggregationSteps().deleteActiveCampaignQuietly(CLIENT, bannersRule.getCampaignId());
        cmdRule.cmdSteps().ajaxSetRecomedationsEmailSteps().postAjaxSetRecomendationsEmail(CLIENT, EMAIL);
        UsersOptionsRecord actualOptions = TestEnvironment.newDbSteps().useShardForLogin(CLIENT).usersOptionsSteps().getUsersOptions(Long.valueOf(user.getPassportUID()));
        assumeThat("Показ тизера соответсует ожиданиям", UserOptionsModel.fromString(actualOptions.getOptions()).getEmailTeaser(), equalTo("1"));
        assertThat("почта соответстует ожиданиям", actualOptions.getRecommendationsEmail(), equalTo(EMAIL));
    }

    @Test
    @Description("проверяем что после удаления кампании не слетел непоказ тизера")
    @ru.yandex.qatools.allure.annotations.TestCaseId("10032")
    public void setHideByAjaxUserOptionsAfterDelete() {
        cmdRule.apiAggregationSteps().deleteActiveCampaignQuietly(CLIENT, bannersRule.getCampaignId());
        String hide = "1";
        cmdRule.cmdSteps().ajaxUserOptionsSteps().postAjaxUserOptions(CLIENT, hide);
        UsersOptionsRecord actualOptions = TestEnvironment.newDbSteps().useShardForLogin(CLIENT).usersOptionsSteps().getUsersOptions(Long.valueOf(user.getPassportUID()));
        assertThat("Показ тизера соответсует ожиданиям", UserOptionsModel.fromString(actualOptions.getOptions()).getEmailTeaser(), equalTo("1"));
    }

}

package ru.yandex.autotests.direct.cmd.feeds;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.data.Logins;
import ru.yandex.autotests.direct.cmd.data.feeds.FeedSaveRequest;
import ru.yandex.autotests.direct.cmd.data.savecamp.SaveCampRequest;
import ru.yandex.autotests.direct.cmd.rules.DirectCmdRule;
import ru.yandex.autotests.direct.cmd.rules.PerformanceBannersRule;
import ru.yandex.autotests.direct.cmd.steps.feeds.FeedHelper;
import ru.yandex.autotests.direct.cmd.tags.CmdTag;
import ru.yandex.autotests.direct.cmd.tags.ObjectTag;
import ru.yandex.autotests.direct.httpclient.TestFeatures;
import ru.yandex.autotests.directapi.model.User;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

import java.util.Arrays;
import java.util.Collection;

@Aqua.Test
@Description("Проверка возможности изменения фида клиента менеджером/агенством")
@Stories(TestFeatures.Feeds.SAVE_FEED)
@Features(TestFeatures.FEEDS)
@Tag(CmdTag.SAVE_FEED)
@Tag(ObjectTag.FEED)
@RunWith(Parameterized.class)
public class FeedsAtSaveFeedAllowedTest extends FeedsAtSaveFeedTestBase {

    public String user;
    public String client;

    public FeedsAtSaveFeedAllowedTest(String user, String client) {
        this.user = user;
        this.client = client;
        switch (client) {
            case Logins.AGENCY_CLIENT:
                cmdRule = DirectCmdRule.defaultRule().withRules(
                        new PerformanceBannersRule()
                                .overrideCampTemplate(new SaveCampRequest().withFor_agency(user))
                                .withUlogin(client));
                break;
            case "test-agency-rub-client-rub":
                cmdRule = DirectCmdRule.defaultRule().withRules(
                        new PerformanceBannersRule()
                                .overrideCampTemplate(new SaveCampRequest().withFor_agency("test-agency-rub"))
                                .withUlogin(client));
                break;
            default:
                cmdRule = DirectCmdRule.defaultRule().withRules(new PerformanceBannersRule().withUlogin(client));
                break;
        }
    }

    @Parameterized.Parameters(name = "Редактирование фидов под {0} у {1}")
    public static Collection<Object[]> testData() {
        return Arrays.asList(new Object[][]{
                {Logins.AGENCY, Logins.AGENCY_CLIENT},
                {Logins.MANAGER, "at-direct-mng-client3"},
                {Logins.MANAGER, "test-agency-rub-client-rub"}
        });
    }

    @Override
    protected String getClient() {
        return client;
    }

    @Before
    public void before() {
        super.before();
        cmdRule.cmdSteps().authSteps().authenticate(User.get(Logins.SUPER));
        FeedHelper.deleteAllFeeds(cmdRule, client);
        cmdRule.cmdSteps().authSteps().authenticate(User.get(user));

        defaultFeed = FeedSaveRequest.getDefaultFeed(User.get(getClient()).getClientID());
    }

    @Test
    @Override
    @ru.yandex.qatools.allure.annotations.TestCaseId("9738")
    public void checkSaveNewFeedAtSaveFeed() {
        super.checkSaveNewFeedAtSaveFeed();
    }

    @Test
    @Override
    @ru.yandex.qatools.allure.annotations.TestCaseId("9739")
    public void checkSaveFeedAtSaveFeed() {
        super.checkSaveFeedAtSaveFeed();
    }

}

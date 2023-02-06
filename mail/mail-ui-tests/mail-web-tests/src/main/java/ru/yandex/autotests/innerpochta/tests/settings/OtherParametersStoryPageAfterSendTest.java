package ru.yandex.autotests.innerpochta.tests.settings;

import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import io.qameta.allure.junit4.Tag;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.annotations.ConditionalIgnore;
import ru.yandex.autotests.innerpochta.conditions.TicketInProgress;
import ru.yandex.autotests.innerpochta.data.QuickFragments;
import ru.yandex.autotests.innerpochta.rules.RestAssuredAuthRule;
import ru.yandex.autotests.innerpochta.rules.SendToStatAfterClassRule;
import ru.yandex.autotests.innerpochta.rules.acclock.AccLockRule;
import ru.yandex.autotests.innerpochta.steps.AllureStepStorage;
import ru.yandex.autotests.innerpochta.tests.BaseTest;
import ru.yandex.autotests.innerpochta.util.FeaturesConst;
import ru.yandex.autotests.innerpochta.util.Utils;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Issue;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;

import static com.google.common.collect.ImmutableMap.of;
import static ru.yandex.autotests.innerpochta.data.MailEnums.PageAfterSend;
import static ru.yandex.autotests.innerpochta.data.MailEnums.PageAfterSend.DONE;
import static ru.yandex.autotests.innerpochta.data.MailEnums.PageAfterSend.SENT;
import static ru.yandex.autotests.innerpochta.rules.ClearAccRule.clearAcc;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.LAYOUT_2PANE;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.LAYOUT_3PANE_VERTICAL;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.SETTINGS_PARAM_CURRENT_LIST;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.SETTINGS_PARAM_LAYOUT;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.SETTINGS_PARAM_PAGE_AFTER_SENT;

/**
 * <p> Created by IntelliJ IDEA.
 * <p> User: lanwen
 * <p> Date: 22.05.12
 * <p> Time: 18:48
 */

@Aqua.Test
@Title("Страница после отсылки письма")
@Features(FeaturesConst.SETTINGS)
@Tag(FeaturesConst.SETTINGS)
@Stories(FeaturesConst.OTHER_SETTINGS)
@RunWith(DataProviderRunner.class)
public class OtherParametersStoryPageAfterSendTest extends BaseTest {

    private AccLockRule lock = AccLockRule.use().useTusAccount();
    private RestAssuredAuthRule auth = RestAssuredAuthRule.auth(lock);
    private AllureStepStorage user = new AllureStepStorage(webDriverRule, auth);

    @ClassRule
    public static SendToStatAfterClassRule sendToStat = new SendToStatAfterClassRule();

    @Rule
    public RuleChain rules = RuleChain.outerRule(lock).around(auth).around(clearAcc(() -> user));

    @Before
    public void logIn() {
        user.apiSettingsSteps().callWithListAndParams(
            "Выбираем переход к текущему списку после отправки письма, включаем 2пейн",
            of(
                SETTINGS_PARAM_PAGE_AFTER_SENT, SETTINGS_PARAM_CURRENT_LIST,
                SETTINGS_PARAM_LAYOUT, LAYOUT_2PANE
            )
        );
        user.loginSteps().forAcc(lock.firstAcc()).logins(QuickFragments.SETTINGS_OTHER);
    }

    @Test
    @Title("После отсылки письма остаёмся там же")
    @TestCaseId("1812")
    @DataProvider({LAYOUT_2PANE, LAYOUT_3PANE_VERTICAL})
    public void testPageCurrentAfterSend(String layout) {
        user.apiSettingsSteps().callWithListAndParams(
            "Изменяем лейаут (2pane/3pane)",
            of(SETTINGS_PARAM_LAYOUT, layout)
        );
        user.defaultSteps().opensFragment(QuickFragments.SPAM)
            .shouldSee(onMessagePage().displayedMessages())
            .clicksOn(onMessagePage().composeButton());
        user.composeSteps().inputsAddressInFieldTo(lock.firstAcc().getSelfEmail())
            .inputsSubject(Utils.getRandomString());
        user.defaultSteps().clicksOn(onComposePopup().expandedPopup().sendBtn());
        user.composeSteps().waitForMessageToBeSend();
        user.defaultSteps().shouldSee(onMessagePage().displayedMessages())
            .shouldBeOnUrlWith(QuickFragments.SPAM);
    }

    @Test
    @Title("После отсылки письма переходим в отправленные")
    @TestCaseId("1811")
    @ConditionalIgnore(condition = TicketInProgress.class)
    @Issue("DARIA-69542")
    public void testPageSentAfterSend() {
        pageAfterSend(SENT);
        user.defaultSteps().shouldBeOnUrlWith(QuickFragments.SENT);
    }

    @Test
    @Title("После отсылки письма переходим на страницу «Done»")
    @TestCaseId("1810")
    public void testPageDoneAfterSend() {
        pageAfterSend(DONE);
        user.defaultSteps().shouldSee(user.pages().ComposePopup().doneScreen());
    }

    private void pageAfterSend(PageAfterSend selectNum) {
        user.defaultSteps()
            .shouldSeeElementsCount(
                onOtherSettings().blockSetupOther().bottomPanel().pageAfterOptionsList(),
                3
            )
            .clicksOn(onOtherSettings().blockSetupOther().bottomPanel().pageAfterOptionsList().get(0))
            .clicksOn(onSettingsPage().selectConditionDropdown().conditionsList().get(selectNum.getValue()));
        user.settingsSteps().saveSettingsIfCanAndClicksOn(onMessagePage().mail360HeaderBlock().serviceIcons().get(0));
        user.composeSteps().prepareDraftFor(
            lock.firstAcc().getSelfEmail(),
            Utils.getRandomString(),
            Utils.getRandomString()
        );
        user.defaultSteps().clicksOn(onComposePopup().expandedPopup().sendBtn());
    }
}

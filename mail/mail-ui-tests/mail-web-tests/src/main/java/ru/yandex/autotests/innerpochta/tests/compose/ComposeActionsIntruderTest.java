package ru.yandex.autotests.innerpochta.tests.compose;

import io.qameta.allure.junit4.Tag;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
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
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Issue;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;

import static com.google.common.collect.ImmutableMap.of;
import static ru.yandex.autotests.innerpochta.rules.ClearAccRule.clearAcc;
import static ru.yandex.autotests.innerpochta.util.MailConst.REMIND_LABEL_5DAYS;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.EMPTY_STR;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.SETTINGS_PARAM_NO_REPLY_NOTIFY;

@Aqua.Test
@Title("Новый композ - Тест на чекбокс напомнить")
@Features(FeaturesConst.COMPOSE)
@Tag(FeaturesConst.COMPOSE)
@Stories(FeaturesConst.ADDITIONAL)
public class ComposeActionsIntruderTest extends BaseTest {

    private static final String REMIND_LABEL_TOMORROW = "завтра в 12:00";
    private static final String LABEL_TOMORROW = "Завтра";
    private static final String LABEL_REMIND = "Напомнить";

    private AccLockRule lock = AccLockRule.use().useTusAccount();
    public RestAssuredAuthRule auth = RestAssuredAuthRule.auth(lock);
    private AllureStepStorage user = new AllureStepStorage(webDriverRule, auth);

    @ClassRule
    public static SendToStatAfterClassRule sendToStat = new SendToStatAfterClassRule();

    @Rule
    public RuleChain rules = RuleChain.outerRule(lock)
        .around(auth)
        .around(clearAcc(() -> user));

    @Before
    public void loginAndDeleteAllNotAnsweredMail() {
        user.apiSettingsSteps().callWithListAndParams(
            SETTINGS_PARAM_NO_REPLY_NOTIFY,
            of(SETTINGS_PARAM_NO_REPLY_NOTIFY, EMPTY_STR)
        );
        user.loginSteps().forAcc(lock.firstAcc()).logins(QuickFragments.COMPOSE);
    }

    @Test
    @Title("Чекбокс напомнить и выпадушка")
    @TestCaseId("1193")
    public void composeSelectWaitForAnswerCheckbox() {
        user.defaultSteps().clicksOn(onComposePopup().expandedPopup().notifyBtn())
            .shouldSee(onComposePopup().expandedPopup().notifyPopup())
            .turnTrue(onComposePopup().expandedPopup().notifyPopup().options().get(0))
            .clicksOn(onComposePopup().expandedPopup().notifyPopup().waitTime().get(1))
            .clicksOnElementWithText(onComposePopup().expandedPopup().notifyPopup().waitTime(), LABEL_TOMORROW)
            .shouldSeeThatElementTextEquals(
                onComposePopup().expandedPopup().notifyPopup().header(),
                REMIND_LABEL_TOMORROW
            );
    }

    @Test
    @Title("Выпадушка без чекбокса напомнить")
    @TestCaseId("1194")
    public void composeSelectWaitForAnswerCheckboxThroughDropdown() {
        user.defaultSteps().clicksOn(onComposePopup().expandedPopup().notifyBtn())
            .turnTrue(onComposePopup().expandedPopup().notifyPopup().options().get(0))
            .clicksOnElementWithText(onComposePopup().expandedPopup().notifyPopup().waitTime(), LABEL_TOMORROW)
            .shouldSeeThatElementTextEquals(
                onComposePopup().expandedPopup().notifyPopup().header(),
                REMIND_LABEL_TOMORROW
            )
            .deselects(onComposePopup().expandedPopup().notifyPopup().options().get(0))
            .shouldNotSee(onComposePopup().expandedPopup().notifyPopup().header());
    }

    @Test
    @Title("Напоминать всегда о получении ответа")
    @TestCaseId("1195")
    @ConditionalIgnore(condition = TicketInProgress.class)
    @Issue("DARIA-68249")
    public void composeSelectAlwaysWaitForAnswer() {
        user.defaultSteps().clicksOn(onComposePopup().expandedPopup().notifyBtn())
            .turnTrue(onComposePopup().expandedPopup().notifyPopup().options().get(1))
            .clicksOn(onComposePopup().expandedPopup().notifyBtn());
        user.composeSteps().disableComposeAlert();
        user.defaultSteps().opensFragment(QuickFragments.COMPOSE)
            .shouldSeeThatElementTextEquals(onComposePage().footerSendBlock().remindBtn(), REMIND_LABEL_5DAYS)
            .clicksOn(onComposePage().footerSendBlock().remindBtn())
            .deselects(onComposePage().mailNotifyPopup().notifyAlwaysCheckbox());
        user.composeSteps().disableComposeAlert();
        user.defaultSteps().opensFragment(QuickFragments.COMPOSE).refreshPage()
            .shouldSeeThatElementTextEquals(onComposePage().footerSendBlock().remindBtn(), LABEL_REMIND);
    }
}

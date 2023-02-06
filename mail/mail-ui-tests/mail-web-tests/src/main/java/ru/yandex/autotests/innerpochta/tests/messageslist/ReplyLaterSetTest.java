package ru.yandex.autotests.innerpochta.tests.messageslist;

import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import io.qameta.allure.junit4.Tag;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.data.QuickFragments;
import ru.yandex.autotests.innerpochta.rules.RestAssuredAuthRule;
import ru.yandex.autotests.innerpochta.rules.SendToStatAfterClassRule;
import ru.yandex.autotests.innerpochta.rules.acclock.AccLockRule;
import ru.yandex.autotests.innerpochta.steps.AllureStepStorage;
import ru.yandex.autotests.innerpochta.steps.beans.message.Message;
import ru.yandex.autotests.innerpochta.tests.BaseTest;
import ru.yandex.autotests.innerpochta.util.FeaturesConst;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Step;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;

import java.io.IOException;

import static com.google.common.collect.ImmutableMap.of;
import static ru.yandex.autotests.innerpochta.rules.ClearAccRule.clearAcc;
import static ru.yandex.autotests.innerpochta.util.Utils.getRandomString;
import static ru.yandex.autotests.innerpochta.util.experiments.ExperimentsConstants.REPLY_LATER_EXP;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.REPLY_LATER;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.STATUS_TRUE;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.LAYOUT_3PANE_VERTICAL;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.SETTINGS_OPEN_MSG_LIST;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.SETTINGS_PARAM_LAYOUT;

/**
 * @author oleshko
 */
@Aqua.Test
@Title("Тесты на включение напоминания о письме")
@Features(FeaturesConst.MESSAGE_LIST)
@Stories(FeaturesConst.MESSAGE_LIST)
@Tag(FeaturesConst.REPLY_LATER)
@RunWith(DataProviderRunner.class)
public class ReplyLaterSetTest extends BaseTest {

    private static final String REPLY_LATER_FOLDER_SYMBOL = "#reply_later";
    private static final String TIME = "12:34";

    Message msg;

    private AccLockRule lock = AccLockRule.use().useTusAccount();
    private RestAssuredAuthRule auth = RestAssuredAuthRule.auth(lock);
    private AllureStepStorage user = new AllureStepStorage(webDriverRule, auth);

    @ClassRule
    public static SendToStatAfterClassRule sendToStat = new SendToStatAfterClassRule();

    @Rule
    public RuleChain rules = RuleChain.outerRule(lock)
        .around(auth)
        .around(clearAcc(() -> user));

    @Before
    public void logIn() throws IOException {
        msg = user.apiMessagesSteps().sendMailWithNoSave(lock.firstAcc(), getRandomString(), "");
        user.loginSteps().forAcc(lock.firstAcc()).logins();
        user.defaultSteps().addExperimentsWithYexp(REPLY_LATER_EXP);
    }

    @Test
    @Title("Включаем напоминание о письме из пресетов")
    @TestCaseId("6373")
    public void shouldSetReminderInMsgList() {
        user.messagesSteps().clicksOnMessageCheckBoxByNumber(0);
        setReminderFromToolbar();
        user.apiMessagesSteps().shouldGetMsgCountViaApi(REPLY_LATER, 1);
    }

    @Test
    @Title("Включаем напоминание о письме в просмотре письма на отдельной странице")
    @TestCaseId("6373")
    public void shouldSetReminderInFullMsgView() {
        user.defaultSteps().opensFragment(QuickFragments.MSG_FRAGMENT.fragment(msg.getMid()));
        setReminderFromToolbar();
        user.apiMessagesSteps().shouldGetMsgCountViaApi(REPLY_LATER, 1);
    }

    @Test
    @Title("Включаем напоминание о письме в просмотре письма в списке писем")
    @TestCaseId("6377")
    public void shouldSetReminderInMsgView() {
        user.apiSettingsSteps().callWithListAndParams(
            "Включаем просмотр письма в списке писем",
            of(
                SETTINGS_OPEN_MSG_LIST, STATUS_TRUE
            )
        );
        user.defaultSteps().refreshPage();
        user.messagesSteps().clicksOnMessageWithSubject(msg.getSubject());
        user.defaultSteps()
            .clicksOn(onMessageView().contentToolbarBlock().moreBtn())
            .shouldSee(onMessageView().miscField())
            .clicksOn(onMessageView().miscField().remindLaterBtn())
            .clicksOn(onMessageView().replyLaterDropDown().get(0))
            .shouldSee(onMessagePage().emptyFolder());
        user.apiMessagesSteps().shouldGetMsgCountViaApi(REPLY_LATER, 1);
    }

    @Test
    @Title("Включаем напоминание о письме из контекстного тулбара в 3пейн")
    @TestCaseId("6378")
    public void shouldSetReminderIn3pane() {
        user.apiSettingsSteps().callWithListAndParams(
            "Включаем 3pane",
            of(SETTINGS_PARAM_LAYOUT, LAYOUT_3PANE_VERTICAL)
        );
        user.defaultSteps().opensFragment(QuickFragments.MSG_FRAGMENT.fragment(msg.getMid()))
            .clicksOn(onMessageView().toolbar().moreBtn());
        setReminderFromToolbar();
        user.apiMessagesSteps().shouldGetMsgCountViaApi(REPLY_LATER, 1);
    }

    @Test
    @Title("Включаем напоминание на кастомное время")
    @TestCaseId("6374")
    public void shouldCustomReminder() {
        user.messagesSteps().clicksOnMessageCheckBoxByNumber(0);
        user.defaultSteps().clicksOn(onMessageView().toolbar().replyLaterBtn())
            .clicksOn(onMessageView().replyLaterDropDown().get(onMessagePage().replyLaterDropDown().size() - 1))
            .clicksOn(onMessagePage().calendar().calendarDates().get(0))
            .inputsTextInElementClearingThroughHotKeys(
                onMessagePage().calendar().timeInput(),
                TIME.replace(":", "")
            )
            .clicksOn(onMessagePage().calendar().saveBtn());
        user.apiMessagesSteps().shouldGetMsgCountViaApi(REPLY_LATER, 1);
        user.defaultSteps().opensDefaultUrlWithPostFix(REPLY_LATER_FOLDER_SYMBOL)
            .shouldSeeThatElementHasText(onMessagePage().displayedMessages().list().get(0).date(), TIME);
    }

    @Step("Устанавливаем напоминание о письме из выпадушки в тулбаре")
    private void setReminderFromToolbar() {
        user.defaultSteps().clicksOn(onMessageView().toolbar().replyLaterBtn())
            .clicksOn(onMessageView().replyLaterDropDown().get(0))
            .shouldSee(onMessagePage().emptyFolder());
    }
}

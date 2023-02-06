package ru.yandex.autotests.innerpochta.tests.messagefullview;

import io.qameta.allure.junit4.Tag;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.data.QuickFragments;
import ru.yandex.autotests.innerpochta.rules.RestAssuredAuthRule;
import ru.yandex.autotests.innerpochta.rules.SendToStatAfterClassRule;
import ru.yandex.autotests.innerpochta.rules.acclock.AccLockRule;
import ru.yandex.autotests.innerpochta.steps.AllureStepStorage;
import ru.yandex.autotests.innerpochta.steps.beans.message.Message;
import ru.yandex.autotests.innerpochta.tests.BaseTest;
import ru.yandex.autotests.innerpochta.util.FeaturesConst;
import ru.yandex.autotests.innerpochta.util.Utils;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Step;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;

import java.io.IOException;

import static com.google.common.collect.ImmutableMap.of;
import static ru.yandex.autotests.innerpochta.rules.ClearAccRule.clearAcc;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.SPAM_RU;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.LAYOUT_2PANE;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.SETTINGS_PARAM_LAYOUT;

/**
 * @author arttimofeev
 */
@Aqua.Test
@Title("Тест на кнопку «Спам»")
@Features(FeaturesConst.MESSAGE_FULL_VIEW)
@Tag(FeaturesConst.MESSAGE_FULL_VIEW)
@Stories(FeaturesConst.TOOLBAR)
public class SpamButtonTest extends BaseTest {

    @ClassRule
    public static SendToStatAfterClassRule sendToStat = new SendToStatAfterClassRule();
    private static final String MESSAGE_CONTENT = "http://yandex.ru";
    private Message msg;
    private final AccLockRule lock = AccLockRule.use().useTusAccount();
    private final RestAssuredAuthRule auth = RestAssuredAuthRule.auth(lock);
    private final AllureStepStorage user = new AllureStepStorage(webDriverRule, auth);
    @Rule
    public RuleChain rules = RuleChain.outerRule(lock)
        .around(auth)
        .around(clearAcc(() -> user));

    @Before
    public void logIn() throws IOException {
        user.apiSettingsSteps().callWithListAndParams("Включаем 2pane", of(SETTINGS_PARAM_LAYOUT, LAYOUT_2PANE));
        msg = user.apiMessagesSteps().sendMailWithNoSave(lock.firstAcc(), Utils.getRandomName(), "");
        user.loginSteps().forAcc(lock.firstAcc()).logins();
    }

    @Test
    @Title("Отправляем письмо в спам при просмотре. Смотрим счётчики")
    @TestCaseId("1659")
    public void messageViewSpamButton() {
        user.defaultSteps().opensFragment(QuickFragments.MSG_FRAGMENT.fragment(msg.getMid()))
            .clicksOn(onMessageView().toolbar().spamButton());
        user.leftColumnSteps().shouldBeInFolder(SPAM_RU);
        user.defaultSteps().opensFragment(QuickFragments.SPAM);
        user.messagesSteps().shouldSeeMessageWithSubject(msg.getSubject());
    }

    @Test
    @Title("Тест на предупреждение, что ссылки и картинки отключены в спаме")
    @TestCaseId("1660")
    public void testSpamLinksDisabled() {
        sendSpamMessage();
        user.defaultSteps().opensFragment(QuickFragments.MSG_FRAGMENT.fragment(msg.getMid()))
            .shouldSee(onMessageView().dangerNotification());
    }

    @Test
    @Title("Восстанавливаем письмо из спама")
    @TestCaseId("1027")
    public void messageViewNotSpamButton() {
        sendSpamMessage();
        user.defaultSteps().opensFragment(QuickFragments.MSG_FRAGMENT.fragment(msg.getMid()))
            .clicksOn(onMessageView().toolbar().notSpamButton())
            .clicksIfCanOn(onMessagePage().rightSubmitActionBtn())
            .opensFragment(QuickFragments.INBOX);
        user.messagesSteps().shouldSeeMessageWithSubject(msg.getSubject());
    }

    @Step("Присылаем письмо в спам")
    private void sendSpamMessage() {
        String subject = Utils.getRandomName();
        msg = user.apiMessagesSteps().sendMailWithNoSave(lock.firstAcc(), subject, MESSAGE_CONTENT);
        user.defaultSteps().refreshPage();
        user.messagesSteps().clicksOnMessageCheckBoxWithSubject(subject);
        user.defaultSteps().clicksOn(onMessagePage().toolbar().spamButton());
    }
}

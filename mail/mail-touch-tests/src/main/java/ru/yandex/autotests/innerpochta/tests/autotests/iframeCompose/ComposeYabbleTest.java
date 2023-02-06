package ru.yandex.autotests.innerpochta.tests.autotests.iframeCompose;

import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import org.openqa.selenium.Keys;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.annotations.ConditionalIgnore;
import ru.yandex.autotests.innerpochta.conditions.TicketInProgress;
import ru.yandex.autotests.innerpochta.rules.InitStepsRule;
import ru.yandex.autotests.innerpochta.rules.SendToStatAfterClassRule;
import ru.yandex.autotests.innerpochta.rules.TouchRulesManager;
import ru.yandex.autotests.innerpochta.rules.ZombieProxyEnableRule;
import ru.yandex.autotests.innerpochta.rules.acclock.AccLockRule;
import ru.yandex.autotests.innerpochta.steps.beans.message.Message;
import ru.yandex.autotests.innerpochta.util.FeaturesConst;
import ru.yandex.autotests.innerpochta.util.Utils;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Issue;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;

import static org.hamcrest.Matchers.empty;
import static org.hamcrest.core.IsNot.not;
import static ru.yandex.autotests.innerpochta.data.QuickFragments.MSG_FRAGMENT;
import static ru.yandex.autotests.innerpochta.rules.TouchRulesManager.touchRulesManager;
import static ru.yandex.autotests.innerpochta.touch.data.ToolbarBtns.REPLY;
import static ru.yandex.autotests.innerpochta.util.MailConst.DEV_NULL_EMAIL;
import static ru.yandex.autotests.innerpochta.util.MailConst.DEV_NULL_EMAIL_2;
import static ru.yandex.autotests.innerpochta.util.Utils.getRandomName;

/**
 * @author oleshko
 */

@Aqua.Test
@Title("Тесты на ябблы в композе")
@Features(FeaturesConst.COMPOSE)
@Stories(FeaturesConst.YABBLE)
@RunWith(DataProviderRunner.class)
public class ComposeYabbleTest {

    private static final String DEV_NULL_NAME = "Имя Фамилия";

    private TouchRulesManager rules = touchRulesManager().withLock(AccLockRule.use().useTusAccount());
    private AccLockRule accLock = rules.getLock();
    private InitStepsRule steps = rules.getSteps();

    private String addresses;

    @ClassRule
    public static ZombieProxyEnableRule zomb = new ZombieProxyEnableRule();

    @ClassRule
    public static SendToStatAfterClassRule sendToStat = new SendToStatAfterClassRule();

    @Rule
    public RuleChain chain = rules.createTouchRuleChain();

    @Before
    public void prep() {
        steps.user().apiAbookSteps().addNewContacts(steps.user().abookSteps().createDefaultContact());
        steps.user().loginSteps().forAcc(accLock.firstAcc()).logins();
        steps.user().touchSteps().openComposeViaUrl();
        addresses = DEV_NULL_EMAIL + " " + DEV_NULL_EMAIL_2 + " " + accLock.firstAcc().getSelfEmail() + " " +
            getRandomName() + " " + getRandomName() + " " + getRandomName() + " " + getRandomName() + " " +
            getRandomName() + " " + getRandomName() + " " + getRandomName() + " " + getRandomName();
    }

    @Test
    @Title("Удаляем яббл")
    @TestCaseId("69")
    public void shouldDeleteYabble() {
        steps.user().defaultSteps()
            .clicksOn(steps.pages().touch().composeIframe().inputTo())
            .clicksOn(steps.pages().touch().composeIframe().composeSuggestItems().get(0))
            .shouldSee(steps.pages().touch().composeIframe().yabble())
            .clicksOn(steps.pages().touch().composeIframe().deleteYabble())
            .shouldNotSee(steps.pages().touch().composeIframe().yabble());
    }

    @Test
    @Title("Удаляем яббл с помощью Backspace")
    @TestCaseId("69")
    public void shouldDeleteYabbleWithBackSpace() {
        steps.user().defaultSteps()
            .clicksOn(steps.pages().touch().composeIframe().inputTo())
            .clicksOn(steps.pages().touch().composeIframe().composeSuggestItems().waitUntil(not(empty())).get(0))
            .shouldSee(steps.pages().touch().composeIframe().yabble());
        steps.user().hotkeySteps().pressHotKeys(steps.pages().touch().composeIframe().inputTo(), Keys.BACK_SPACE)
            .pressHotKeys(steps.pages().touch().composeIframe().inputTo(), Keys.BACK_SPACE);
        steps.user().defaultSteps().shouldNotSee(steps.pages().touch().composeIframe().yabble());
    }

    @Test
    @Title("Редактируем яббл")
    @TestCaseId("70")
    public void shouldEditYabble() {
        String newEmail = Utils.getRandomString();
        steps.user().defaultSteps().shouldSee(steps.pages().touch().composeIframe().header().sendBtn())
            .clicksOn(steps.pages().touch().composeIframe().inputTo())
            .clicksAndInputsText(steps.pages().touch().composeIframe().inputTo(), DEV_NULL_EMAIL)
            .clicksOn(steps.pages().touch().composeIframe().expandComposeFields())
            .clicksOn(steps.pages().touch().composeIframe().yabble())
            .appendTextInElement(steps.pages().touch().composeIframe().editableYabble(), newEmail);
        steps.user().hotkeySteps().pressHotKeys(steps.pages().touch().composeIframe().editableYabble(), Keys.ENTER);
        steps.user().defaultSteps()
            .shouldContainText(steps.pages().touch().composeIframe().yabble(), DEV_NULL_EMAIL + newEmail);
    }

    @Test
    @Title("Должен сформироваться ябл по тапу на enter")
    @TestCaseId("201")
    public void shouldMakeYabbleByPressEnter() {
        steps.user().defaultSteps()
            .clicksAndInputsText(steps.pages().touch().composeIframe().inputTo(), DEV_NULL_EMAIL_2)
            .shouldNotSee(steps.pages().touch().composeIframe().yabble());
        steps.pages().touch().composeIframe().inputTo().sendKeys(Keys.ENTER);
        steps.user().defaultSteps().shouldSee(steps.pages().touch().composeIframe().yabble());
    }

    @Test
    @Title("Не должен сформироваться ябл по тапу на пробел")
    @TestCaseId("168")
    public void shouldNotMakeYabbleByPressSpace() {
        steps.user().defaultSteps()
            .clicksAndInputsText(steps.pages().touch().composeIframe().inputTo(), DEV_NULL_EMAIL_2)
            .shouldNotSee(steps.pages().touch().composeIframe().yabble());
        steps.pages().touch().composeIframe().inputTo().sendKeys(Keys.SPACE);
        steps.user().defaultSteps().shouldNotSee(steps.pages().touch().composeIframe().yabble())
            .shouldSeeThatElementHasText(steps.pages().touch().composeIframe().inputTo(), DEV_NULL_EMAIL_2);
    }

    @ConditionalIgnore(condition = TicketInProgress.class)
    @Issue("QUINN-5836")
    @Test
    @Title("Должен сформироваться яббл тапом в свободную область поля")
    @TestCaseId("202")
    public void shouldMakeYabbleByTap() {
        steps.user().defaultSteps()
            .clicksAndInputsText(steps.pages().touch().composeIframe().inputTo(), DEV_NULL_EMAIL_2)
            .shouldNotSee(steps.pages().touch().composeIframe().yabble())
            .offsetClick(steps.pages().touch().composeIframe().inputTo(), 250, 40)
            .shouldSee(steps.pages().touch().composeIframe().yabble());
    }

    @Test
    @Title("Должны вставить адрес из буфера обмена")
    @TestCaseId("171")
    @DataProvider({DEV_NULL_EMAIL, DEV_NULL_NAME})
    public void shouldNotMakeYabbleByInsert(String text) {
        steps.user().defaultSteps().inputsTextInElement(steps.pages().touch().composeIframe().inputTo(), text);
        steps.user().hotkeySteps().pressHotKeysWithDestination(
            steps.pages().touch().composeIframe().inputTo(),
            Keys.chord(Keys.CONTROL, "a")
        );
        steps.user().hotkeySteps().pressHotKeysWithDestination(
            steps.pages().touch().composeIframe().inputTo(),
            Keys.chord(Keys.CONTROL, "x")
        );
        steps.user().defaultSteps().shouldNotSee(steps.pages().touch().composeIframe().yabble());
        steps.user().hotkeySteps().pressHotKeysWithDestination(
            steps.pages().touch().composeIframe().inputTo(),
            Keys.chord(Keys.CONTROL, "v")
        );
        steps.user().defaultSteps().shouldNotSee(steps.pages().touch().composeIframe().yabble())
            .shouldSeeThatElementHasText(steps.pages().touch().composeIframe().inputTo(), text);
    }

    @Test
    @Title("Должен автоматически сформироваться яббл в ответе")
    @TestCaseId("173")
    public void shouldSeeYabbleInReply() {
        Message msg = steps.user().apiMessagesSteps().sendMailWithNoSave(accLock.firstAcc(), getRandomName(), "");
        steps.user().defaultSteps().opensDefaultUrlWithPostFix(MSG_FRAGMENT.makeTouchUrlPart(msg.getMid()))
            .clicksOn(steps.pages().touch().messageView().moreBtn())
            .clicksOnElementWithText(steps.pages().touch().messageView().btnsList(), REPLY.btn());
        steps.user().touchSteps().switchToComposeIframe();
        steps.user().defaultSteps().shouldSee(steps.pages().touch().composeIframe().yabble());
    }

    @Test
    @Title("Должны развернуть поле, где скрыты яблы под Еще N ")
    @TestCaseId("175")
    public void shouldSeeAllRecipient() {
        steps.user().defaultSteps().inputsTextInElement(steps.pages().touch().composeIframe().inputTo(), addresses)
            .clicksOn(steps.pages().touch().composeIframe().expandComposeFields())
            .clicksOn(steps.pages().touch().composeIframe().expandComposeFields())
            .shouldSee(steps.pages().touch().composeIframe().yabbleMore())
            .offsetClick(steps.pages().touch().composeIframe().inputTo(), 5, 5)
            .shouldNotSee(steps.pages().touch().composeIframe().yabbleMore());
    }

    @Test
    @Title("В ябле должно отображаеться имя получателя, а не адрес")
    @TestCaseId("170")
    public void shouldSeeNameInYabble() {
        steps.user().apiAbookSteps().removeAllAbookContacts().addNewContacts(
            steps.user().abookSteps().createContactWithParametrs(DEV_NULL_NAME, DEV_NULL_EMAIL));
        steps.user().defaultSteps()
            .clicksAndInputsText(steps.pages().touch().composeIframe().inputTo(), DEV_NULL_EMAIL)
            .clicksOn(steps.pages().touch().composeIframe().expandComposeFields())
            .shouldSee(steps.pages().touch().composeIframe().yabble())
            .shouldSeeThatElementTextEquals(steps.pages().touch().composeIframe().yabbleText(), DEV_NULL_NAME);
    }
}

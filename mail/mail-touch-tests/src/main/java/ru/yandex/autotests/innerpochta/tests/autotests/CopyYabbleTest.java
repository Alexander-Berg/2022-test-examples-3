package ru.yandex.autotests.innerpochta.tests.autotests;

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
import ru.yandex.autotests.innerpochta.annotations.DoTestOnlyForEnvironment;
import ru.yandex.autotests.innerpochta.rules.InitStepsRule;
import ru.yandex.autotests.innerpochta.rules.SendToStatAfterClassRule;
import ru.yandex.autotests.innerpochta.rules.TouchRulesManager;
import ru.yandex.autotests.innerpochta.rules.ZombieProxyEnableRule;
import ru.yandex.autotests.innerpochta.rules.acclock.AccLockRule;
import ru.yandex.autotests.innerpochta.util.FeaturesConst;
import ru.yandex.autotests.innerpochta.util.Utils;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Issue;
import ru.yandex.qatools.allure.annotations.Step;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;

import static ru.yandex.autotests.innerpochta.rules.TouchRulesManager.touchRulesManager;
import static ru.yandex.autotests.innerpochta.touch.pages.ComposeIframePage.IFRAME_COMPOSE;
import static ru.yandex.autotests.innerpochta.util.MailConst.DEV_NULL_EMAIL;
import static ru.yandex.autotests.innerpochta.util.MailConst.DEV_NULL_EMAIL_2;

/**
 * @author sshelgunova
 */
@Aqua.Test
@Title("Тесты на попап копирование ябблов")
@Features({FeaturesConst.MESSAGE_FULL_VIEW})
@Stories(FeaturesConst.COPY_YABBLE)
@RunWith(DataProviderRunner.class)
public class CopyYabbleTest {

    private TouchRulesManager rules = touchRulesManager().withLock(AccLockRule.use().useTusAccount());
    private AccLockRule acc = rules.getLock();
    private InitStepsRule steps = rules.getSteps();

    @ClassRule
    public static ZombieProxyEnableRule zomb = new ZombieProxyEnableRule();

    @ClassRule
    public static SendToStatAfterClassRule sendToStat = new SendToStatAfterClassRule();

    @Rule
    public RuleChain chain = rules.createTouchRuleChain();

    @Before
    public void prepare() {
        steps.user().apiMessagesSteps().sendMail(
            acc.firstAcc().getSelfEmail(), Utils.getRandomString(), Utils.getRandomString()
        );
        steps.user().loginSteps().forAcc(acc.firstAcc()).logins();
    }

    @Test
    @Title("Появление попапа копирования ябблов по клику на ябблы из полей To, From, Cc, Bcc")
    @TestCaseId("853")
    @DataProvider({"0", "1", "2"})
    @DoTestOnlyForEnvironment("Not Nexus 5")
    public void shouldSeePopupCopyYabble(int num) {
        steps.user().apiMessagesSteps().addCcEmails(DEV_NULL_EMAIL).addBccEmails(DEV_NULL_EMAIL_2)
            .sendMailWithCcAndBcc(acc.firstAcc().getSelfEmail(), Utils.getRandomName(), Utils.getRandomString());
        steps.user().defaultSteps().refreshPage();
        openPopupCopyYabble(num);
    }

    @Test
    @Title("Появление попапа копирования ябблов по клику на ябблы из поля Bcc")
    @TestCaseId("853")
    @Issue("QUINN-6520") //TODO: совместить с shouldSeePopupCopyYabble
    @DoTestOnlyForEnvironment("Apple iPad Pro")
    public void shouldSeePopupCopyYabbleBcc() {
        steps.user().apiMessagesSteps().addCcEmails(DEV_NULL_EMAIL).addBccEmails(DEV_NULL_EMAIL_2)
            .sendMailWithCcAndBcc(acc.firstAcc().getSelfEmail(), Utils.getRandomName(), Utils.getRandomString());
        steps.user().defaultSteps().refreshPage();
        openPopupCopyYabble(3);
    }

    @Test
    @Title("Должны увидеть, что попап копирования ябблов закрылся после тапа на крестик")
    @TestCaseId("854")
    public void ShouldClosePopupCopyYabble() {
        openPopupCopyYabble(0);
        steps.user().defaultSteps().clicksOn(steps.pages().touch().messageView().popup().closeBtn())
            .shouldNotSee(steps.pages().touch().messageView().yabblePopup());
    }

    @Test
    @Title("После клика по кнопке Написать в попапе копирования перешли в композ")
    @TestCaseId("856")
    public void shouldSeeComposeAfterClickOnBtnWrite() {
        openPopupCopyYabble(0);
        steps.user().defaultSteps().clicksOn(steps.pages().touch().messageView().yabblePopup().btnWriteMail())
            .shouldSee(steps.pages().touch().composeIframe().iframe())
            .switchTo(IFRAME_COMPOSE)
            .shouldSee(
                steps.pages().touch().composeIframe().header().sendBtn(),
                steps.pages().touch().composeIframe().yabble()
            );
    }

    @Test
    @Title("Попап закрывается по тапу в фон")
    @TestCaseId("855")
    public void shouldCloseYabblePopupByTapInOverlay() {
        openPopupCopyYabble(0);
        steps.user().defaultSteps().clicksOn(steps.pages().touch().messageView().overlay())
            .shouldNotSee(steps.pages().touch().messageView().yabblePopup());
    }

    @Test
    @Title("Копируем адрес через попап копирования ябблов")
    @TestCaseId("857")
    public void shouldCopyYabbleAddress() {
        openPopupCopyYabble(0);
        steps.user().defaultSteps().clicksOn(steps.pages().touch().messageView().yabblePopup().btnCopyAddress())
            .shouldSee(steps.pages().touch().messageView().statusLineInfo());
        steps.user().touchSteps().openComposeViaUrl();
        steps.user().hotkeySteps().pressHotKeysWithDestination(
            steps.pages().touch().composeIframe().inputTo(),
            Keys.chord(Keys.CONTROL, "v")
        );
        steps.user().defaultSteps().shouldSeeThatElementHasText(
            steps.pages().touch().composeIframe().inputTo(),
            acc.firstAcc().getSelfEmail()
        );
    }

    @Step("Открываем попап копирования ябблов")
    private void openPopupCopyYabble(int number) {
        steps.user().defaultSteps().clicksOn(steps.pages().touch().messageList().messageBlock().subject())
            .clicksOn(steps.pages().touch().messageView().avatarToolbar())
            .shouldSee(steps.pages().touch().messageView().msgDetails());
        steps.user().touchSteps().longTap(steps.pages().touch().messageView().yabbles().get(number));
        steps.user().defaultSteps().shouldSee(steps.pages().touch().messageView().yabblePopup());
    }
}
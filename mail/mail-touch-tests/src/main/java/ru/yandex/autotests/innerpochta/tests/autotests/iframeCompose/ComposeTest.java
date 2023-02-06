package ru.yandex.autotests.innerpochta.tests.autotests.iframeCompose;

import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.rules.InitStepsRule;
import ru.yandex.autotests.innerpochta.rules.SendToStatAfterClassRule;
import ru.yandex.autotests.innerpochta.rules.TouchRulesManager;
import ru.yandex.autotests.innerpochta.rules.ZombieProxyEnableRule;
import ru.yandex.autotests.innerpochta.rules.acclock.AccLockRule;
import ru.yandex.autotests.innerpochta.steps.beans.Sign;
import ru.yandex.autotests.innerpochta.steps.beans.contact.Contact;
import ru.yandex.autotests.innerpochta.steps.beans.contact.Email;
import ru.yandex.autotests.innerpochta.util.FeaturesConst;
import ru.yandex.autotests.innerpochta.util.Utils;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Step;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.not;
import static ru.yandex.autotests.innerpochta.data.QuickFragments.COMPOSE;
import static ru.yandex.autotests.innerpochta.data.QuickFragments.INBOX_FOLDER;
import static ru.yandex.autotests.innerpochta.data.QuickFragments.MSG_FRAGMENT;
import static ru.yandex.autotests.innerpochta.rules.TouchRulesManager.touchRulesManager;
import static ru.yandex.autotests.innerpochta.touch.data.ToolbarBtns.REPLY_ALL;
import static ru.yandex.autotests.innerpochta.touch.pages.ComposeIframePage.IFRAME_COMPOSE;
import static ru.yandex.autotests.innerpochta.util.MailConst.DEV_NULL_EMAIL;
import static ru.yandex.autotests.innerpochta.util.MailConst.DEV_NULL_EMAIL_2;
import static ru.yandex.autotests.innerpochta.util.Utils.getRandomString;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.DRAFT;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.INBOX;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.SENT;

/**
 * @author oleshko
 */

@Aqua.Test
@Title("Тесты на айфрейм композ")
@Features(FeaturesConst.COMPOSE)
@Stories(FeaturesConst.GENERAL)
@RunWith(DataProviderRunner.class)
public class ComposeTest {

    private static final String COM_DOMAIN = "@yandex.com";
    private static final int YABBLES_NUMBER = 60;
    private static final String COMPOSE_SHORTCUT =
        "?to=%s&subject=subj&body=olo&bcc=testbotauto@yandex.ru&cc=testbot1@yandex.ru";
    private static final String COMPOSE_LONGCUT = "?subject=Subject&to=a@yandex.ru,b@yandex.ru,c@yandex.ru," +
        "d@yandex.ru,e@yandex.ru,g@yandex.ru,d@yandex.ru,a@yandex.ru,v@yandex.ru,s@yandex.ru,b@yandex.ru," +
        "p@yandex.ru,i@yandex.ru,i@yandex.ru,t@yandex.ru,n@yandex.ru,n@yandex.ru,k@yandex.ru,n@yandex.ru,a@yandex.ru," +
        "o@yandex.ru,s@yandex.ru,a@yandex.ru,k@yandex.ru,e@yandex.ru,a@yandex.ru,a@yandex.ru,l@yandex.ru," +
        "d@yandex.ru,s@yandex.ru,o@yandex.ru,k@yandex.ru,p@yandex.ru,s@yandex.ru,b@yandex.ru,f@yandex.ru," +
        "r@yandex.ru,k@yandex.ru,k@yandex.ru,p@yandex.ru,r@yandex.ru,v@yandex.ru,s@yandex.ru,e@yandex.ru," +
        "s@yandex.ru,t@yandex.ru,k@yandex.ru,a@yandex.ru,k@yandex.ru,m@yandex.ru,a@yandex.ru,g@yandex.ru," +
        "v@yandex.ru,a@yandex.ru,a@yandex.ru,a@yandex.ru,e@yandex.ru,p@yandex.ru,d@yandex.ru,p@yandex.ru";

    @DataProvider
    public static Object[] StartDomain() {
        return new Object[][]{
            {"trololo@y"},
            {"trololo@m"},
            {"trololo@g"},
        };
    }

    private TouchRulesManager rules = touchRulesManager().withLock(AccLockRule.use().useTusAccount());
    private AccLockRule accLock = rules.getLock();
    private InitStepsRule steps = rules.getSteps();

    private String signature, comAlias;

    @ClassRule
    public static ZombieProxyEnableRule zomb = new ZombieProxyEnableRule();

    @ClassRule
    public static SendToStatAfterClassRule sendToStat = new SendToStatAfterClassRule();

    @Rule
    public RuleChain chain = rules.createTouchRuleChain();

    @Before
    public void prep() {
        comAlias = accLock.firstAcc().getLogin() + COM_DOMAIN;
        signature = getRandomString();
        steps.user().apiSettingsSteps().changeSignsWithTextAndAmount(
            new Sign().withText(getRandomString()).withIsDefault(true)
                .withEmails(Collections.singletonList(accLock.firstAcc().getSelfEmail())),
            new Sign().withText(signature).withIsDefault(false)
                .withEmails(Collections.singletonList(comAlias))
        );
        steps.user().apiAbookSteps().addNewContacts(steps.user().abookSteps().createDefaultContact());
        steps.user().loginSteps().forAcc(accLock.firstAcc()).logins();
        steps.user().touchSteps().openComposeViaUrl();
    }

    @Test
    @Title("Должен измениться алиас и подпись")
    @TestCaseId("71")
    public void shouldChangeAliasAndSignature() {
        steps.user().defaultSteps().shouldSee(steps.pages().touch().composeIframe().header().sendBtn())
            .clicksOn(steps.pages().touch().composeIframe().expandComposeFields())
            .shouldNotHasText(steps.pages().touch().composeIframe().signature(), signature)
            .clicksOn(steps.pages().touch().composeIframe().fieldFrom())
            .clicksOnElementWithText(steps.pages().touch().composeIframe().composeAliasItems(), comAlias)
            .shouldHasText(steps.pages().touch().composeIframe().signature(), signature);
    }

    @Test
    @Title("Отправка письма")
    @TestCaseId("72")
    public void shouldSendMessage() {
        String subject = getRandomString();
        steps.user().defaultSteps().shouldSee(steps.pages().touch().composeIframe().header().sendBtn())
            .clicksOn(steps.pages().touch().composeIframe().expandComposeFields())
            .inputsTextInElement(steps.pages().touch().composeIframe().inputTo(), DEV_NULL_EMAIL)
            .inputsTextInElement(steps.pages().touch().composeIframe().inputCc(), accLock.firstAcc().getSelfEmail())
            .inputsTextInElement(steps.pages().touch().composeIframe().inputBcc(), DEV_NULL_EMAIL_2)
            .inputsTextInElement(steps.pages().touch().composeIframe().inputSubject(), subject)
            .clicksOn(steps.pages().touch().composeIframe().fieldFrom())
            .clicksOnElementWithText(steps.pages().touch().composeIframe().composeAliasItems(), comAlias)
            .clicksOn(steps.pages().touch().composeIframe().header().sendBtn())
            .shouldNotSee(steps.pages().touch().composeIframe().header().sendBtn())
            .refreshPage()
            .shouldSee(steps.pages().touch().messageList().headerBlock())
            .shouldSeeThatElementTextEquals(steps.pages().touch().messageList().messages().get(0).subject(), subject);
    }

    @Test
    @Title("Смена подписи при изменении алиаса в черновике")
    @TestCaseId("565")
    public void replyToMessageAfterChangeInDraft() {
        steps.user().apiMessagesSteps().createDraftMessage();
        steps.user().defaultSteps().opensDefaultUrlWithPostFix(
            COMPOSE.makeTouchUrlPart(
                steps.user().apiMessagesSteps().getAllMessagesInFolder(DRAFT).get(0).getMid()
            )
        );
        steps.user().touchSteps().switchToComposeIframe();
        steps.user().defaultSteps().shouldNotHasText(steps.pages().touch().composeIframe().signature(), signature)
            .clicksOn(steps.pages().touch().composeIframe().expandComposeFields())
            .clicksOn(steps.pages().touch().composeIframe().fieldFrom())
            .clicksOnElementWithText(steps.pages().touch().composeIframe().composeAliasItems(), comAlias)
            .shouldHasText(steps.pages().touch().composeIframe().signature(), signature);
    }

    @Test
    @Title("Проверяем, что отвечаем на верный алиас")
    @TestCaseId("73")
    public void replyToMessageWithToCcBcc() {
        sendMailWithChangedFrom();
        steps.user().defaultSteps().waitInSeconds(3)
            .opensDefaultUrlWithPostFix(
                MSG_FRAGMENT.makeTouchUrlPart(
                    steps.user().apiMessagesSteps().getAllMessagesInFolder(INBOX).get(0).getMid()
                )
            )
            .clicksOn(steps.pages().touch().messageView().moreBtn())
            .clicksOnElementWithText(steps.pages().touch().messageView().btnsList(), REPLY_ALL.btn())
            .shouldSee(steps.pages().touch().composeIframe().iframe())
            .switchTo(IFRAME_COMPOSE)
            .clicksOn(steps.pages().touch().composeIframe().yabble())
            .shouldSeeThatElementHasText(steps.pages().touch().composeIframe().inputTo(), comAlias);
    }

    @Test
    @Title("Открытие композа по урлу с большим количеством адресатов")
    @TestCaseId("1005")
    public void openComposeWithManyAddressesByDirectUrl() {
        steps.user().defaultSteps().opensDefaultUrlWithPostFix(
            String.format("%s%s", COMPOSE.makeTouchUrlPart(), COMPOSE_LONGCUT)
        )
            .shouldSee(steps.pages().touch().composeIframe().iframe())
            .switchTo(IFRAME_COMPOSE)
            .shouldSeeElementsCount(
                steps.pages().touch().composeIframe().yabbles().waitUntil(not(empty())),
                YABBLES_NUMBER
            );
    }

    @Test
    @Title("Должно отправиться письмо до формировния ябла адресата")
    @TestCaseId("174")
    public void shouldSendMailBeforeMakeYabble() {
        steps.user().defaultSteps()
            .clicksAndInputsText(steps.pages().touch().composeIframe().inputTo(), DEV_NULL_EMAIL_2)
            .shouldNotSee(steps.pages().touch().composeIframe().yabble())
            .clicksOn(steps.pages().touch().composeIframe().header().sendBtn())
            .shouldBeOnUrlWith(INBOX_FOLDER);
        steps.user().apiMessagesSteps().shouldGetMsgCountViaApi(SENT, 1);
    }

    @Test
    @Title("Должен выбираться нужный email, если у контакта несколько адресов")
    @TestCaseId("1120")
    public void shouldSelectNotDefaultContactEmail() {
        List<Email> emails = Arrays.asList(
            new Email().withValue(DEV_NULL_EMAIL),
            new Email().withValue(accLock.firstAcc().getSelfEmail())
        );
        steps.user().apiAbookSteps().removeAllAbookContacts();
        Contact contact = steps.user().abookSteps()
            .createContactWithParametrs(Utils.getRandomString(), accLock.firstAcc().getSelfEmail())
            .withEmail(emails);
        steps.user().apiAbookSteps().addContactWithTwoEmails(contact.getName().getFirst(), contact);
        steps.user().touchSteps().openComposeViaUrl();
        steps.user().defaultSteps()
            .inputsTextInElement(steps.pages().touch().composeIframe().inputTo(), contact.getName().getFirst())
            .clicksOnElementWithText(
                steps.pages().touch().composeIframe().composeSuggestItems().waitUntil(not(empty())),
                DEV_NULL_EMAIL
            )
            .clicksOn(steps.pages().touch().composeIframe().yabble())
            .shouldSeeThatElementHasText(steps.pages().touch().composeIframe().inputTo(), DEV_NULL_EMAIL);
    }

    @Test
    @Title("Должны заполнить поля композа значениями из гет-параметров")
    @TestCaseId("1387")
    public void shouldOpenShortcutInCompose() {
        steps.user().defaultSteps().opensDefaultUrlWithPostFix(String.format(
            "%s%s",
            COMPOSE.makeTouchUrlPart(),
            String.format(COMPOSE_SHORTCUT, DEV_NULL_EMAIL)
        ));
        steps.user().defaultSteps().shouldSee(steps.pages().touch().composeIframe().iframe())
            .switchTo(IFRAME_COMPOSE)
            .shouldContainText(steps.pages().touch().composeIframe().inputTo(), DEV_NULL_EMAIL)
            .shouldContainText(steps.pages().touch().composeIframe().inputBcc(), "testbotauto@yandex.ru")
            .shouldContainText(steps.pages().touch().composeIframe().inputCc(), "testbot1@yandex.ru")
            .shouldHasValue(steps.pages().touch().composeIframe().inputSubject(), "subj")
            .shouldContainText(steps.pages().touch().composeIframe().inputBody(), "olo");
    }

    @Test
    @Title("Должны увидеть cаджест для нового адреса")
    @TestCaseId("463")
    @UseDataProvider("StartDomain")
    public void shouldSeeSuggestForNewContact(String domain) {
        steps.user().touchSteps().openComposeViaUrl();
        steps.user().defaultSteps().inputsTextInElement(steps.pages().touch().composeIframe().inputTo(), domain)
            .waitInSeconds(2)
            .shouldContainText(
                steps.pages().touch().composeIframe().composeSuggestItems().waitUntil(not(empty())).get(0),
                domain
            );
    }

    @Step("Прислать себе письмо с заполненными полями To, Cc, Bcc и поменянным алисасом от Кого")
    private void sendMailWithChangedFrom() {
        steps.user().defaultSteps().opensDefaultUrlWithPostFix(String.format(
            "%s%s",
            COMPOSE.makeTouchUrlPart(),
            String.format(COMPOSE_SHORTCUT, accLock.firstAcc().getSelfEmail())
        ))
            .shouldSee(steps.pages().touch().composeIframe().iframe())
            .switchTo(IFRAME_COMPOSE)
            .shouldSee(steps.pages().touch().composeIframe().inputTo())
            .clicksOn(steps.pages().touch().composeIframe().expandComposeFields())
            .clicksOn(steps.pages().touch().composeIframe().expandComposeFields())
            .clicksOn(steps.pages().touch().composeIframe().fieldFrom())
            .shouldSee(steps.pages().touch().composeIframe().suggestAliases())
            .clicksOnElementWithText(steps.pages().touch().composeIframe().composeAliasItems(), comAlias)
            .clicksOn(steps.pages().touch().composeIframe().header().sendBtn())
            .shouldNotSee(steps.pages().touch().composeIframe().yabble());
    }
}
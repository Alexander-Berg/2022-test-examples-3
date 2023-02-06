package ru.yandex.autotests.innerpochta.tests.autotests.messageView;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.annotations.ConditionalIgnore;
import ru.yandex.autotests.innerpochta.annotations.DoTestOnlyForEnvironment;
import ru.yandex.autotests.innerpochta.conditions.TicketInProgress;
import ru.yandex.autotests.innerpochta.rules.InitStepsRule;
import ru.yandex.autotests.innerpochta.rules.SendToStatAfterClassRule;
import ru.yandex.autotests.innerpochta.rules.TouchRulesManager;
import ru.yandex.autotests.innerpochta.rules.ZombieProxyEnableRule;
import ru.yandex.autotests.innerpochta.rules.acclock.AccLockRule;
import ru.yandex.autotests.innerpochta.steps.beans.message.Message;
import ru.yandex.autotests.innerpochta.util.FeaturesConst;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Issue;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;

import static org.hamcrest.Matchers.containsString;
import static ru.yandex.autotests.innerpochta.data.QuickFragments.FOLDER_ID;
import static ru.yandex.autotests.innerpochta.data.QuickFragments.MSG_FRAGMENT;
import static ru.yandex.autotests.innerpochta.rules.TouchRulesManager.touchRulesManager;
import static ru.yandex.autotests.innerpochta.touch.data.FidsAndLids.SPAM_FOLDER;
import static ru.yandex.autotests.innerpochta.util.MessageHTMLBodyBuilder.messageHTMLBodyBuilder;
import static ru.yandex.autotests.innerpochta.util.Utils.getRandomName;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.SENT;

/**
 * @author oleshko
 */
@Aqua.Test
@Title("Тесты на просмотр письма")
@Features(FeaturesConst.MESSAGE_FULL_VIEW)
@Stories(FeaturesConst.GENERAL)
public class GeneralMsgViewTest {

    private Message msg, msgWithLink;

    private static final String URL_LINK = "http://www.fontanka.ru/";
    private static final String MSG_WITH_INLINE_ATTACH = "инлайн";

    private TouchRulesManager rules = touchRulesManager().withLock(AccLockRule.use().useTusAccount());
    private AccLockRule accLock = rules.getLock();
    private InitStepsRule steps = rules.getSteps();

    @ClassRule
    public static ZombieProxyEnableRule zomb = new ZombieProxyEnableRule();

    @ClassRule
    public static SendToStatAfterClassRule sendToStat = new SendToStatAfterClassRule();

    @Rule
    public RuleChain chain = rules.createTouchRuleChain();

    @Before
    public void prep() {
        msgWithLink = steps.user().apiMessagesSteps()
            .sendMailWithNoSave(accLock.firstAcc(), getRandomName(), URL_LINK);
        steps.user().apiMessagesSteps().sendMailWithAttachmentsAndHTMLBody(
            accLock.firstAcc().getSelfEmail(),
            MSG_WITH_INLINE_ATTACH,
            messageHTMLBodyBuilder(steps.user()).makeBodyWithInlineAttachAndText()
        );
        steps.user().apiFoldersSteps().purgeFolder(steps.user().apiFoldersSteps().getFolderBySymbol(SENT));
        steps.user().loginSteps().forAcc(accLock.firstAcc()).logins();
        msg = steps.user().apiMessagesSteps().getMessageWithSubject(MSG_WITH_INLINE_ATTACH);
    }

    @Test
    @Title("Должны увидеть дополнительное море внизу письма")
    @TestCaseId("225")
    @DoTestOnlyForEnvironment("Android")
    public void shouldSeeExtraMore() {
        steps.user().defaultSteps().opensDefaultUrlWithPostFix(MSG_FRAGMENT.makeTouchUrlPart(msg.getMid()))
            .clicksOn(steps.pages().touch().messageView().moreBtnLow())
            .shouldSee(steps.pages().touch().messageList().popup());
    }

    @Test
    @Title("Переход к предыдущему письму по стрелочке в шапке")
    @TestCaseId("600")
    public void shouldOpenPrevMsg() {
        steps.user().defaultSteps().opensDefaultUrlWithPostFix(MSG_FRAGMENT.makeTouchUrlPart(msgWithLink.getMid()))
            .clicksOn(steps.pages().touch().messageView().header().prevMsg())
            .shouldBeOnUrl(containsString(msg.getMid()));
    }

    @Test
    @Title("Переход к следующему письму по стрелочке в шапке")
    @TestCaseId("60")
    public void shouldOpenNextMsg() {
        steps.user().defaultSteps().opensDefaultUrlWithPostFix(MSG_FRAGMENT.makeTouchUrlPart(msg.getMid()))
            .clicksOn(steps.pages().touch().messageView().header().nextMsg())
            .shouldBeOnUrl(containsString(msgWithLink.getMid()));
    }

    @Test
    @Title("Стрелочка перехода к предыдущему письму задизейблена, если письма нет")
    @TestCaseId("601")
    public void shouldSeeDisabledPrevArror() {
        steps.user().defaultSteps().opensDefaultUrlWithPostFix(MSG_FRAGMENT.makeTouchUrlPart(msg.getMid()))
            .shouldSee(steps.pages().touch().messageView().header().disabledPrevMsg())
            .clicksOn(steps.pages().touch().messageView().header().disabledPrevMsg())
            .shouldBeOnUrl(containsString(msg.getMid()));
    }

    @Test
    @Title("Стрелочка перехода к следующему письму задизейблена, если письма нет")
    @TestCaseId("602")
    public void shouldSeeDisabledNextArror() {
        steps.user().defaultSteps().opensDefaultUrlWithPostFix(MSG_FRAGMENT.makeTouchUrlPart(msgWithLink.getMid()))
            .shouldSee(steps.pages().touch().messageView().header().disabledNextMsg())
            .clicksOn(steps.pages().touch().messageView().header().disabledNextMsg())
            .shouldBeOnUrl(containsString(msgWithLink.getMid()));
    }

    @Test
    @Title("В папке «Спам» картинки и ссылки отключены")
    @TestCaseId("328")
    @ConditionalIgnore(condition = TicketInProgress.class)
    @Issue("QUINN-7440")
    public void shouldNotSeePicAndLinkInSpam() {
        steps.user().apiMessagesSteps().moveMessagesToSpam(msg);
        steps.user().defaultSteps().opensDefaultUrlWithPostFix(
            String.format(
                "%s/%s",
                FOLDER_ID.makeTouchUrlPart(SPAM_FOLDER),
                MSG_FRAGMENT.fragment(msg.getMid())
            )
        )
            .shouldSee(steps.pages().touch().messageView().hiddenImg())
            .shouldNotSee(steps.pages().touch().messageView().linkInMessage())
            .clicksOn(steps.pages().touch().messageView().turnOnInSpam())
            .shouldNotSee(steps.pages().touch().messageView().hiddenImg())
            .shouldSee(steps.pages().touch().messageView().linkInMessage())
            .refreshPage()
            .shouldSee(steps.pages().touch().messageView().hiddenImg())
            .shouldNotSee(steps.pages().touch().messageView().linkInMessage());
    }

    @Test
    @Title("Открываем ссылку в письме")
    @TestCaseId("375")
    public void shouldOpenLinkInMsg() {
        steps.user().defaultSteps().opensDefaultUrlWithPostFix(MSG_FRAGMENT.makeTouchUrlPart(msgWithLink.getMid()))
            .clicksOn(steps.pages().touch().messageView().linkInMessage())
            .switchOnJustOpenedWindow()
            .shouldBeOnUrl(URL_LINK);
    }

    @Test
    @Title("В деталях письма по тапу в ябл c именем, показывается адрес контакта")
    @TestCaseId("176")
    public void shouldSeeAddressAfterTapYabbleInMsgDetail() {
        steps.user().defaultSteps().opensDefaultUrlWithPostFix(MSG_FRAGMENT.makeTouchUrlPart(msgWithLink.getMid()))
            .clicksOn(steps.pages().touch().messageView().toolbar())
            .clicksOn(steps.pages().touch().messageView().yabbles().get(1))
            .shouldSeeThatElementTextEquals(
                steps.pages().touch().messageView().yabbleText(),
                accLock.firstAcc().getSelfEmail()
            );
    }
}

package ru.yandex.autotests.innerpochta.tests.screentests;

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
import ru.yandex.autotests.innerpochta.rules.InitStepsRule;
import ru.yandex.autotests.innerpochta.rules.SendToStatAfterClassRule;
import ru.yandex.autotests.innerpochta.rules.TouchScreenRulesManager;
import ru.yandex.autotests.innerpochta.rules.ZombieProxyEnableRule;
import ru.yandex.autotests.innerpochta.rules.acclock.AccLockRule;
import ru.yandex.autotests.innerpochta.util.FeaturesConst;
import ru.yandex.autotests.innerpochta.util.RunAndCompare;
import ru.yandex.autotests.innerpochta.util.Utils;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;

import java.util.function.Consumer;

import static ru.yandex.autotests.innerpochta.data.QuickFragments.MSG_FRAGMENT;
import static ru.yandex.autotests.innerpochta.rules.TouchScreenRulesManager.touchScreenRulesManager;
import static ru.yandex.autotests.innerpochta.touch.pages.ComposeIframePage.IFRAME_COMPOSE;
import static ru.yandex.autotests.innerpochta.util.MailConst.DEV_NULL_EMAIL;
import static ru.yandex.autotests.innerpochta.util.MailConst.DEV_NULL_EMAIL_2;
import static ru.yandex.autotests.innerpochta.util.RunAndCompare.runAndCompare;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.SENT;


/**
 * @author oleshko
 */
@Aqua.Test
@Title("Тесты на квикреплай")
@Features(FeaturesConst.MESSAGE_FULL_VIEW)
@Stories(FeaturesConst.QR)
@RunWith(DataProviderRunner.class)
public class QuickReplyScreenTest {

    private static final String TEXT_WITH_INDENTS = "∧＿∧ \n" +
        "( ･ω･｡)つ━・*。\n" +
        "⊂  ノ      ・゜+.\n" +
        "しーＪ     °。+ *´¨)\n" +
        ".· ´¸.·*´¨) ¸.·*¨)\n" +
        "(¸.·´ (¸.·'* вжух \n";

    private TouchScreenRulesManager rules = touchScreenRulesManager().withLock(AccLockRule.use().useTusAccount());
    private InitStepsRule stepsProd = rules.getStepsProd();
    private InitStepsRule stepsTest = rules.getStepsTest();
    private AccLockRule acc = rules.getLock();
    private RunAndCompare parallelRun = runAndCompare().withProdSteps(stepsProd).withTestSteps(stepsTest);

    @ClassRule
    public static ZombieProxyEnableRule zomb = new ZombieProxyEnableRule();

    @ClassRule
    public static SendToStatAfterClassRule sendToStat = new SendToStatAfterClassRule();

    @Rule
    public RuleChain chain = rules.createTouchRuleChain();

    @Before
    public void prep() {
        String subject = Utils.getRandomString();
        stepsTest.user().apiMessagesSteps().addBccEmails(DEV_NULL_EMAIL_2).addCcEmails(DEV_NULL_EMAIL)
            .sendMailWithCcAndBcc(acc.firstAcc().getSelfEmail(), subject, Utils.getRandomString());
        stepsTest.user().apiMessagesSteps()
            .sendMessageToThreadWithSubjectWithNoSave(subject, acc.firstAcc(), Utils.getRandomString());
        stepsTest.user().apiMessagesSteps().addBccEmails(DEV_NULL_EMAIL_2).addCcEmails(DEV_NULL_EMAIL)
            .sendMailWithCcAndBcc(acc.firstAcc().getSelfEmail(), Utils.getRandomString(), Utils.getRandomString());
        stepsTest.user().loginSteps().forAcc(acc.firstAcc()).logins();
    }

    @Test
    @Title("Переход в композ из пустого квикреплая и проверка адресатов")
    @Description("В письме 0 есть Сс и Всс, проверяем, что они подставятся в поля композа при перехоже из КР" +
        "В треде 1 в первом и последнем письмах разные адресаты, т.о. проверяем, что в КР отвечаем именно адресатам " +
        "последнего письма")
    @TestCaseId("266")
    @DataProvider({"0", "1"})
    public void shouldOpenCompose(int num) {
        Consumer<InitStepsRule> actions = st -> {
            st.user().defaultSteps()
                .shouldSee(st.pages().touch().messageList().headerBlock())
                .clicksOn(st.pages().touch().messageList().messages().get(num))
                .shouldSee(st.pages().touch().messageView().header())
                .refreshPage()
                .shouldSee(st.pages().touch().messageView().header())
                .clicksOn(st.pages().touch().messageView().quickReply().expandCompose());
            st.user().touchSteps().switchToComposeIframe();
            st.user().defaultSteps().shouldSee(st.pages().touch().composeIframe().yabble());
        };
        parallelRun.withActions(actions).withAcc(acc.firstAcc()).run();
    }

    @Test
    @Title("Переход в композ из заполненного квикреплая")
    @TestCaseId("267")
    public void shouldOpenComposeWithText() {
        Consumer<InitStepsRule> actions = st ->
            st.user().defaultSteps()
                .shouldSee(st.pages().touch().messageList().headerBlock())
                .clicksOn(st.pages().touch().messageList().messages().get(0))
                .shouldSee(st.pages().touch().messageView().header())
                .clicksAndInputsText(st.pages().touch().messageView().quickReply().input(), TEXT_WITH_INDENTS)
                .clicksOn(st.pages().touch().messageView().quickReply().expandCompose())
                .shouldSee(st.pages().touch().composeIframe().iframe())
                .switchTo(IFRAME_COMPOSE)
                .shouldSee(st.pages().touch().composeIframe().header().sendBtn())
                .shouldContainText(st.pages().touch().composeIframe().inputBody(), TEXT_WITH_INDENTS);

        parallelRun.withActions(actions).withAcc(acc.firstAcc()).run();
    }

    @Test
    @Title("Поле ввода фиксированно растягивается при вводе текста")
    @TestCaseId("270")
    public void shouldStretchInput() {
        Consumer<InitStepsRule> actions = st -> {
            st.user().defaultSteps().clicksOn(st.pages().touch().messageList().messageBlock())
                .shouldSee(st.pages().touch().messageView().quickReply());
            for (int i = 0; i < 6; i++)
                st.pages().touch().messageView().quickReply().input().sendKeys(Keys.ENTER);
        };
        parallelRun.withAcc(acc.firstAcc()).withActions(actions).run();
    }

    @Test
    @Title("Отправляем текст с форматированием из кр")
    @TestCaseId("1142")
    public void shouldSendTextWithIndents() {
        Consumer<InitStepsRule> actions = st ->
            st.user().defaultSteps().clicksOn(st.pages().touch().messageList().messageBlock())
                .clicksAndInputsText(st.pages().touch().messageView().quickReply().input(), TEXT_WITH_INDENTS)
                .clicksOn(st.pages().touch().messageView().quickReply().send())
                .shouldNotSee(st.pages().touch().messageView().quickReplyOverlay())
                .opensDefaultUrlWithPostFix(MSG_FRAGMENT.makeTouchUrlPart(
                    st.user().apiMessagesSteps().getAllMessagesInFolder(SENT).get(0).getMid()
                ));

        parallelRun.withAcc(acc.firstAcc()).withActions(actions).run();
    }
}

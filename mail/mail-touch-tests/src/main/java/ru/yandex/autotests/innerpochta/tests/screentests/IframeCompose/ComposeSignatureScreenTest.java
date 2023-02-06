package ru.yandex.autotests.innerpochta.tests.screentests.IframeCompose;

import com.google.common.collect.Sets;
import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import org.openqa.selenium.By;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.data.QuickFragments;
import ru.yandex.autotests.innerpochta.rules.InitStepsRule;
import ru.yandex.autotests.innerpochta.rules.SendToStatAfterClassRule;
import ru.yandex.autotests.innerpochta.rules.TouchScreenRulesManager;
import ru.yandex.autotests.innerpochta.rules.ZombieProxyEnableRule;
import ru.yandex.autotests.innerpochta.rules.acclock.AccLockRule;
import ru.yandex.autotests.innerpochta.util.FeaturesConst;
import ru.yandex.autotests.innerpochta.util.RunAndCompare;
import ru.yandex.autotests.innerpochta.util.Utils;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;

import java.util.Set;
import java.util.function.Consumer;

import static jersey.repackaged.com.google.common.collect.ImmutableMap.of;
import static org.openqa.selenium.By.className;
import static ru.yandex.autotests.innerpochta.data.QuickFragments.COMPOSE;
import static ru.yandex.autotests.innerpochta.data.QuickFragments.MSG_FRAGMENT;
import static ru.yandex.autotests.innerpochta.rules.TouchScreenRulesManager.touchScreenRulesManager;
import static ru.yandex.autotests.innerpochta.steps.DefaultSteps.sign;
import static ru.yandex.autotests.innerpochta.touch.data.ToolbarBtns.REPLY;
import static ru.yandex.autotests.innerpochta.util.MailConst.DEV_NULL_EMAIL;
import static ru.yandex.autotests.innerpochta.util.RunAndCompare.runAndCompare;
import static ru.yandex.autotests.innerpochta.util.Utils.getRandomString;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.SENT;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.TRUE;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.SIGNATURE_TOP;

/**
 * @author oleshko
 */
@Aqua.Test
@Title("Общие тесты на подпись в композе")
@Features({FeaturesConst.COMPOSE})
@Stories(FeaturesConst.GENERAL)
@RunWith(DataProviderRunner.class)
public class ComposeSignatureScreenTest {

    private static final String LARGE_IMG_SIGN =
        "<img src=\"https://avatars.mds.yandex.net/get-mail-signature/222735/e2322489d0bcb929c868908c3427cc05/orig\" /";
    private static final String IMG_SIGN =
        "<img src=\"https://avatars.mds.yandex.net/get-mail-signature/474754/4f9380cef1e4521bb3f30191dc5de7a2/orig\" />";
    private static final String SMILE_SIGN = "<img src=\"https://resize.yandex.net/mailservice?url=https%3A%2F%2F" +
        "img.yandex.net%2Fi%2Fsmiles%2Fsmall%2Fsmile_38.gif%3Fyandex_class%3Dyandex_smile_38&amp;proxy=yes&amp;" +
        "key=754ad5729b74da9a3a06e116c2a72ed5\" />";
    private static final String LONG_SIGN = "FUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUU" +
        "UUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUU" +
        "UUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUU";
    private static final String FORMAT_SIGN = "<b><i><u>текст</u></i></b></div><div><a href=\"https://yandex.ru/\" " +
        "rel=\"noopener noreferrer\">ссылка</a></div><div><blockquote><div style=\"text-align:center\"><span style=" +
        "\"color:#006400;font-size:16px;line-height:normal\"><span style=\"background-color:#dda0dd\"><span style=" +
        "\"font-family:'comic sans ms' , sans-serif\">цитата великих людей</span></span></span></div></blockquote>";

    private static final Set<By> IGNORE_THIS = Sets.newHashSet(className(".messageHead"));

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
    public void prepare() {
        stepsProd.user().apiSettingsSteps().changeSignsWithTextAndAmount(
            sign(LARGE_IMG_SIGN),
            sign(IMG_SIGN),
            sign(SMILE_SIGN),
            sign(LONG_SIGN),
            sign(FORMAT_SIGN)
        );
    }

    @Test
    @Title("Отображение разных подписей в композе")
    @TestCaseId("464")
    @DataProvider({"1", "2", "3", "4", "5"})
    public void shouldSeeDifferentSignatures(int num) {
        Consumer<InitStepsRule> act = st -> {
            st.user().touchSteps().switchToComposeIframe();
            st.user().defaultSteps().clicksOn(st.pages().touch().composeIframe().signBtn())
                .clicksOn(st.pages().touch().composeIframe().signPopup().signList().get(num))
                .shouldNotSee(st.pages().touch().composeIframe().signPopup());
        };
        parallelRun.withAcc(acc.firstAcc()).withActions(act).withUrlPath(COMPOSE.makeTouchUrlPart()).run();
    }

    @Test
    @Title("В отправленном письме отображается отредактированная в композе подпись")
    @TestCaseId("886")
    public void shouldSeeEditedSignature() {
        String newSignature = Utils.getRandomName();
        Consumer<InitStepsRule> act = st -> {
            st.user().touchSteps().switchToComposeIframe();
            st.user().defaultSteps().clicksAndInputsText(st.pages().touch().composeIframe().inputTo(), DEV_NULL_EMAIL)
                .clicksOn(st.pages().touch().composeIframe().signature())
                .appendTextInElement(st.pages().touch().composeIframe().signature(), newSignature)
                .clicksOn(st.pages().touch().composeIframe().header().sendBtn())
                .shouldBeOnUrlWith(QuickFragments.INBOX_FOLDER)
                .opensCurrentUrlWithPostFix(
                    MSG_FRAGMENT.makeTouchUrlPart(
                        st.user().apiMessagesSteps().getAllMessagesInFolder(SENT).get(0).getMid()
                    )
                )
                .shouldSee(st.pages().touch().messageView().header());
        };
        parallelRun.withActions(act).withAcc(acc.firstAcc()).withUrlPath(COMPOSE.makeTouchUrlPart())
            .withIgnoredElements(IGNORE_THIS).runSequentially();
    }

    @Test
    @Title("Верстка попапа с подписями")
    @TestCaseId("1412")
    public void shouldSeeSignaturePopup() {
        Consumer<InitStepsRule> act = st -> {
            st.user().touchSteps().switchToComposeIframe();
            st.user().defaultSteps().clicksOn(st.pages().touch().composeIframe().signBtn())
                .shouldSee(st.pages().touch().composeIframe().signPopup());
        };
        parallelRun.withAcc(acc.firstAcc()).withActions(act).withUrlPath(COMPOSE.makeTouchUrlPart()).run();
    }

    @Test
    @Title("Должны применить вариант «Без подписи»")
    @TestCaseId("1415")
    public void shouldSeeNoSignature() {
        Consumer<InitStepsRule> act = st -> {
            st.user().touchSteps().switchToComposeIframe();
            st.user().defaultSteps().clicksOn(st.pages().touch().composeIframe().signBtn())
                .clicksOn(st.pages().touch().composeIframe().signPopup().signList().get(0));
                //.shouldNotSee(st.pages().touch().composeIframe().signBtn()); //QUINN-7569
        };
        parallelRun.withAcc(acc.firstAcc()).withActions(act).withUrlPath(COMPOSE.makeTouchUrlPart()).run();
    }

    @Test
    @Title("Отображение разных подписей внизу всего письма")
    @TestCaseId("1419")
    @DataProvider({"1", "2", "3", "4", "5"})
    public void shouldSeeSignaturesAtTheEnd(int num) {
        Consumer<InitStepsRule> act = st -> {
            st.user().defaultSteps()
                .clicksOn(st.pages().touch().messageList().messageBlock())
                .clicksOn(st.pages().touch().messageView().moreBtn())
                .clicksOnElementWithText(st.pages().touch().messageView().btnsList(), REPLY.btn());
            st.user().touchSteps().switchToComposeIframe();
            st.user().defaultSteps()
                .clicksOn(st.pages().touch().composeIframe().signBtn())
                .clicksOn(st.pages().touch().composeIframe().signPopup().signList().get(num))
                .shouldNotSee(st.pages().touch().composeIframe().signPopup());
        };
        stepsProd.user().apiMessagesSteps()
            .sendMail(acc.firstAcc().getSelfEmail(), getRandomString(), getRandomString());
        parallelRun.withAcc(acc.firstAcc()).withActions(act).run();
    }

    @Test
    @Title("Отображение разных подписей в композе сразу после ответа")
    @TestCaseId("1413")
    @DataProvider({"1", "2", "3", "4", "5"})
    public void shouldSeeSignaturesAfterReply(int num) {
        Consumer<InitStepsRule> act = st -> {
            st.user().defaultSteps()
                .clicksOn(st.pages().touch().messageList().messageBlock())
                .clicksOn(st.pages().touch().messageView().moreBtn())
                .clicksOnElementWithText(st.pages().touch().messageView().btnsList(), REPLY.btn());
            st.user().touchSteps().switchToComposeIframe();
            st.user().defaultSteps()
                .clicksOn(st.pages().touch().composeIframe().signBtn())
                .clicksOn(st.pages().touch().composeIframe().signPopup().signList().get(num))
                .shouldNotSee(st.pages().touch().composeIframe().signPopup());
        };
        stepsProd.user().apiMessagesSteps()
            .sendMail(acc.firstAcc().getSelfEmail(), getRandomString(), getRandomString());
        stepsProd.user().apiSettingsSteps().callWithListAndParams(
            "Меняем расположение подписи при ответе на «Сразу после ответа»",
            of(SIGNATURE_TOP, TRUE)
        );
        parallelRun.withAcc(acc.firstAcc()).withActions(act).run();
    }

    @Test
    @Title("Должны проскролить список подписей")
    @TestCaseId("1521")
    public void shouldScrollSignatureList() {
        Consumer<InitStepsRule> act = st -> {
            st.user().touchSteps().switchToComposeIframe();
            st.user().defaultSteps().clicksOn(st.pages().touch().composeIframe().signBtn())
                .scrollTo(st.pages().touch().composeIframe().signPopup().signList().get(5))
                .shouldSee(st.pages().touch().composeIframe().signPopup().signList().get(5));
        };
        parallelRun.withAcc(acc.firstAcc()).withActions(act).withUrlPath(COMPOSE.makeTouchUrlPart()).run();
    }
}

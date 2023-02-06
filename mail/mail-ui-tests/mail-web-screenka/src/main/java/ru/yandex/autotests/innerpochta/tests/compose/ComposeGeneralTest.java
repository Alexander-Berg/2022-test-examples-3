package ru.yandex.autotests.innerpochta.tests.compose;

import com.google.common.collect.Sets;
import io.qameta.allure.junit4.Tag;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.openqa.selenium.By;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.annotations.ConditionalIgnore;
import ru.yandex.autotests.innerpochta.conditions.TicketInProgress;
import ru.yandex.autotests.innerpochta.rules.InitStepsRule;
import ru.yandex.autotests.innerpochta.rules.ScreenRulesManager;
import ru.yandex.autotests.innerpochta.rules.SendToStatAfterClassRule;
import ru.yandex.autotests.innerpochta.rules.ZombieProxyEnableRule;
import ru.yandex.autotests.innerpochta.rules.acclock.AccLockRule;
import ru.yandex.autotests.innerpochta.util.FeaturesConst;
import ru.yandex.autotests.innerpochta.util.RunAndCompare;
import ru.yandex.autotests.innerpochta.util.Utils;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Issue;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;

import java.util.Set;
import java.util.function.Consumer;

import static org.openqa.selenium.By.cssSelector;
import static ru.yandex.autotests.innerpochta.data.QuickFragments.COMPOSE;
import static ru.yandex.autotests.innerpochta.rules.ScreenRulesManager.screenRulesManager;
import static ru.yandex.autotests.innerpochta.util.MailConst.DEV_NULL_EMAIL;
import static ru.yandex.autotests.innerpochta.util.RunAndCompare.runAndCompare;

/**
 * @author cosmopanda
 */
@Aqua.Test
@Title("Общие тесты")
@Features(FeaturesConst.COMPOSE)
@Tag(FeaturesConst.COMPOSE)
@Stories(FeaturesConst.GENERAL)
public class ComposeGeneralTest {

    private static final String SPAM_TEXT = "XJS*C4JDBQADN1.NSBN3*2IDNEN*GTUBE-STANDARD-ANTI-UBE-TEST-EMAIL*C.34X";
    private static final Set<By> IGNORE_THIS = Sets.newHashSet(
        cssSelector(".composeHeader-SavedAt"),
        cssSelector(".ComposeReactCaptcha-Image")
    );

    private ScreenRulesManager rules = screenRulesManager();
    private InitStepsRule stepsProd = rules.getStepsProd();
    private InitStepsRule stepsTest = rules.getStepsTest();
    private AccLockRule lock = rules.getLock().useTusAccount();
    private RunAndCompare parallelRun = runAndCompare().withProdSteps(stepsProd).withTestSteps(stepsTest)
        .withAdditionalIgnoredElements(IGNORE_THIS);

    @ClassRule
    public static ZombieProxyEnableRule zomb = new ZombieProxyEnableRule();

    @ClassRule
    public static SendToStatAfterClassRule sendToStat = new SendToStatAfterClassRule();

    @Rule
    public RuleChain chain = rules.createRuleChain();

    @Before
    public void setUp() {
        stepsProd.user().apiSettingsSteps().changeSignsAmountTo(2);
    }

    @Test
    @Title("Открываем выпадушку подписей")
    @TestCaseId("3218")
    public void shouldSeeSignatureDropdown() {
        Consumer<InitStepsRule> actions = st -> {
            st.user().composeSteps().openComposePopup();
            st.user().defaultSteps().onMouseHover(st.pages().mail().composePopup().signatureBlock())
                .clicksOn(st.pages().mail().composePopup().signatureChooser())
                .shouldSee(st.pages().mail().composePopup().signaturesPopup());
        };

        parallelRun.withActions(actions).withAcc(lock.firstAcc()).run();
    }

    @Test
    @Title("Открываем попап добавления новой подписи")
    @TestCaseId("3219")
    @ConditionalIgnore(condition = TicketInProgress.class)
    @Issue("DARIA-68214")
    public void shouldSeeNewSignaturePopup() {
        Consumer<InitStepsRule> actions = st -> st.user().defaultSteps()
            .clicksOn(st.pages().mail().compose().textareaBlock().signatureFirstLine())
            .onMouseHover(st.pages().mail().compose().textareaBlock().signatureFirstLine())
            .shouldSee(st.pages().mail().compose().signatureChooser())
            .onMouseHoverAndClick(st.pages().mail().compose().signatureChooser())
            .shouldSee(st.pages().mail().compose().signaturesDropdownMenu())
            .clicksOn(st.pages().mail().compose().signaturesDropdownMenu().addSignatureButton())
            .shouldSee(st.pages().mail().compose().addSignaturePopup())
            .onMouseHover(st.pages().mail().compose().addSignaturePopup())
            .shouldNotSee(st.pages().mail().compose().signatureChooser());

        parallelRun.withActions(actions).withUrlPath(COMPOSE).withAcc(lock.firstAcc()).run();
    }

    @Test
    @Title("Должны видеть попап сохранения изменения")
    @TestCaseId("3221")
    @ConditionalIgnore(condition = TicketInProgress.class)
    @Issue("DARIA-66827")
    public void shouldSeeSaveDraftPopup() {
        Consumer<InitStepsRule> actions = st -> {
            st.user().composeSteps().openComposePopup()
                .inputsAddressInFieldTo(lock.firstAcc().getSelfEmail());
            st.user().defaultSteps().clicksOn(st.pages().mail().composePopup().expandedPopup().popupTitle())
                .refreshPage();
            st.user().defaultSteps().shouldSee(st.pages().mail().composePopup().confirmClosePopup());
        };
        parallelRun.withActions(actions).withAcc(lock.firstAcc()).run();
    }

    @Test
    @Title("Должны видеть «Done» страницу")
    @TestCaseId("3222")
    public void shouldSeeDonePage() {
        String subj = Utils.getRandomString();
        Consumer<InitStepsRule> actions = st -> {
            st.user().composeSteps().openComposePopup()
                .inputsAddressInFieldTo(DEV_NULL_EMAIL)
                .inputsSubject(subj)
                .clicksOnSendBtn();
            st.user().defaultSteps().shouldSee(st.pages().mail().composePopup().doneScreen());
        };
        parallelRun.withActions(actions).withAcc(lock.firstAcc()).run();
    }

    @Test
    @Title("Должны видеть каптчу")
    @TestCaseId("3223")
    public void shouldSeeCaptcha() {
        String subj = Utils.getRandomString();
        Consumer<InitStepsRule> actions = st -> {
            st.user().composeSteps().openComposePopup()
                .inputsAddressInFieldTo(lock.firstAcc().getSelfEmail())
                .inputsSendText(SPAM_TEXT)
                .inputsSubject(subj)
                .clicksOnSendBtn();
            st.user().defaultSteps()
                .shouldSee(
                    st.pages().mail().composePopup().confirmClosePopup().saveBtn(),
                    st.pages().mail().composePopup().confirmClosePopup().refreshCaptchaBtn()
                );
        };
        parallelRun.withActions(actions).withAcc(lock.firstAcc()).run();
    }

}

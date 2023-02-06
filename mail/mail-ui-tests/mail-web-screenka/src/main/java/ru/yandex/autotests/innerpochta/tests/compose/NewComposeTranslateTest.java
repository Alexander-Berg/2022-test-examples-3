package ru.yandex.autotests.innerpochta.tests.compose;

import com.google.common.base.Strings;
import com.google.common.collect.Sets;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import io.qameta.allure.junit4.Tag;
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
import ru.yandex.autotests.innerpochta.rules.ScreenRulesManager;
import ru.yandex.autotests.innerpochta.rules.SendToStatAfterClassRule;
import ru.yandex.autotests.innerpochta.rules.ZombieProxyEnableRule;
import ru.yandex.autotests.innerpochta.rules.acclock.AccLockRule;
import ru.yandex.autotests.innerpochta.util.FeaturesConst;
import ru.yandex.autotests.innerpochta.util.RunAndCompare;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;

import java.util.Set;
import java.util.function.Consumer;

import static com.google.common.collect.ImmutableMap.of;
import static org.openqa.selenium.By.cssSelector;
import static ru.yandex.autotests.innerpochta.rules.ScreenRulesManager.screenRulesManager;
import static ru.yandex.autotests.innerpochta.util.RunAndCompare.runAndCompare;
import static ru.yandex.autotests.innerpochta.util.TestConsts.IGNORED_ELEMENTS;
import static ru.yandex.autotests.innerpochta.util.Utils.getRandomString;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.SETTINGS_FOLDER_THREAD_VIEW;

/**
 * @author eremin-n-s
 */
@Aqua.Test
@Title("Новый композ - Перевод")
@Features(FeaturesConst.COMPOSE)
@Tag(FeaturesConst.COMPOSE)
@Stories(FeaturesConst.GENERAL)
@RunWith(DataProviderRunner.class)
public class NewComposeTranslateTest {

    private static final String URL_FOR_FORMAT_BODY = "/compose?body=<h1>Привет</h1><i>Добрый день</i>";
    private static final String TRANSLATED_TEXT = "Hello\nGood afternoon";
    private static final String TRANSLATED_TEXT_2 = "Hello";
    private static final String TEXT = "Привет";
    private static final Set<By> IGNORE_THIS = Sets.newHashSet(
        cssSelector(".js-folders"),
        cssSelector(".qa-MessageViewer-RightColumn")
    );
    private static int MULTIPLIER_TO_10K_SYMBOLS = 1700;

    private String sbj = getRandomString();

    private ScreenRulesManager rules = screenRulesManager();
    private InitStepsRule stepsProd = rules.getStepsProd();
    private InitStepsRule stepsTest = rules.getStepsTest();
    private AccLockRule lock = rules.getLock().useTusAccount();
    private RunAndCompare parallelRun = runAndCompare().withProdSteps(stepsProd).withTestSteps(stepsTest)
        .withIgnoredElements(IGNORED_ELEMENTS).withAdditionalIgnoredElements(IGNORE_THIS);

    @ClassRule
    public static ZombieProxyEnableRule zomb = new ZombieProxyEnableRule();

    @ClassRule
    public static SendToStatAfterClassRule sendToStat = new SendToStatAfterClassRule();

    @Rule
    public RuleChain chain = rules.createRuleChain();

    @Before
    public void setUp() {
        stepsProd.user().apiSettingsSteps().callWithListAndParams(
            "Выключаем треды",
            of(
                SETTINGS_FOLDER_THREAD_VIEW, false
            )
        );
    }

    @Test
    @Title("Отправляем перевод с форматированием")
    @TestCaseId("5883")
    public void shouldSendTranslateText() {
        Consumer<InitStepsRule> actions = st -> {
            st.user().defaultSteps().opensDefaultUrlWithPostFix(URL_FOR_FORMAT_BODY)
                .clicksOn(st.pages().mail().composePopup().expandedPopup().translateBtn())
                .shouldSeeThatElementTextEquals(
                    st.pages().mail().composePopup().expandedPopup().translateText(),
                    TRANSLATED_TEXT
                );
            st.user().composeSteps().inputsSubject(sbj)
                .inputsAddressInFieldTo(lock.firstAcc().getSelfEmail());
            st.user().defaultSteps().clicksOn(st.pages().mail().composePopup().expandedPopup().sendBtn())
                .opensFragment(QuickFragments.INBOX);
            st.user().messagesSteps().clicksOnMessageWithSubject(sbj);
            st.user().messageViewSteps().shouldSeeCorrectMessageText(TRANSLATED_TEXT);
        };

        parallelRun.withActions(actions).withAcc(lock.firstAcc()).runSequentially();
    }

    @Test
    @Title("Отправляем перевод с цитатой")
    @TestCaseId("5135")
    public void shouldNotTranslateQuotes() {
        stepsProd.user().apiMessagesSteps()
            .sendMail(lock.firstAcc(), sbj, Strings.repeat(TEXT, MULTIPLIER_TO_10K_SYMBOLS));
        Consumer<InitStepsRule> actions = st -> {
            st.user().messagesSteps().clicksOnMessageWithSubject(sbj);
            st.user().defaultSteps().clicksOn(st.pages().mail().msgView().toolbar().replyButton())
                .shouldSee(st.pages().mail().composePopup().expandedPopup().bodyInput())
                .appendTextInElement(st.pages().mail().composePopup().expandedPopup().bodyInput(), TEXT)
                .clicksOn(st.pages().mail().composePopup().expandedPopup().translateBtn())
                .shouldContainText(
                    st.pages().mail().composePopup().expandedPopup().translateText(), TRANSLATED_TEXT_2
                )
                .clicksOn(st.pages().mail().composePopup().expandedPopup().sendBtn())
                .opensFragment(QuickFragments.SENT);
            st.user().messagesSteps().clicksOnMessageWithSubject("Re: " + sbj);
        };

        parallelRun.withActions(actions).withAcc(lock.firstAcc()).run();
    }

}

package ru.yandex.autotests.innerpochta.tests.compose;

import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import io.qameta.allure.junit4.Tag;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.rules.InitStepsRule;
import ru.yandex.autotests.innerpochta.rules.ScreenRulesManager;
import ru.yandex.autotests.innerpochta.rules.SendToStatAfterClassRule;
import ru.yandex.autotests.innerpochta.rules.ZombieProxyEnableRule;
import ru.yandex.autotests.innerpochta.rules.acclock.AccLockRule;
import ru.yandex.autotests.innerpochta.util.FeaturesConst;
import ru.yandex.autotests.innerpochta.util.RunAndCompare;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Step;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;

import java.util.function.Consumer;

import static com.google.common.collect.ImmutableMap.of;
import static ru.yandex.autotests.innerpochta.data.GreetingMessageData.RU_MAIL_EN_LANG_SERVICES;
import static ru.yandex.autotests.innerpochta.rules.ScreenRulesManager.screenRulesManager;
import static ru.yandex.autotests.innerpochta.util.MailConst.DEV_NULL_EMAIL;
import static ru.yandex.autotests.innerpochta.util.RunAndCompare.runAndCompare;
import static ru.yandex.autotests.innerpochta.util.TestConsts.IGNORED_ELEMENTS;
import static ru.yandex.autotests.innerpochta.util.Utils.getRandomString;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.FALSE;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.STATUS_FALSE;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.COMPOSE_KUKUTZ_PROMO;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.DISABLE_PROMO;

/**
 * @author marchart
 */
@Aqua.Test
@Title("Новый композ - Kukutz")
@Features(FeaturesConst.COMPOSE)
@Tag(FeaturesConst.COMPOSE)
@Stories(FeaturesConst.COMPOSE)
@RunWith(DataProviderRunner.class)
public class NewComposeKukutzTest {
    private static final String LONG_BODY_TEXT = RU_MAIL_EN_LANG_SERVICES;
    private static final String CC_EMAIL = "testbot2@yandex.ru";
    private static final String CC_EMAIL_2 = "testbot3@yandex.ru";

    private ScreenRulesManager rules = screenRulesManager();
    private InitStepsRule stepsProd = rules.getStepsProd();
    private InitStepsRule stepsTest = rules.getStepsTest();
    private AccLockRule lock = rules.getLock().useTusAccount();
    private RunAndCompare parallelRun = runAndCompare().withProdSteps(stepsProd).withTestSteps(stepsTest)
        .withIgnoredElements(IGNORED_ELEMENTS);

    @ClassRule
    public static ZombieProxyEnableRule zomb = new ZombieProxyEnableRule();

    @ClassRule
    public static SendToStatAfterClassRule sendToStat = new SendToStatAfterClassRule();

    @Rule
    public RuleChain chain = rules.createRuleChain();

    @Before
    public void setUp() {
        stepsProd.user().apiMessagesSteps().addCcEmails(CC_EMAIL, CC_EMAIL_2)
            .sendMailWithCcAndBcc(lock.firstAcc().getSelfEmail(), getRandomString(), "");
    }

    @Test
    @Title("Кукутц скроллится с телом письма")
    @TestCaseId("5820")
    public void shouldNotSeeKukutzUnderScroll() {
        Consumer<InitStepsRule> actions = st -> {
            st.user().defaultSteps().setsWindowSize(1200, 600);
            openReply(st);
            st.user().defaultSteps()
                .appendTextInElement(st.pages().mail().composePopup().expandedPopup().popupTo(), DEV_NULL_EMAIL)
                .clicksOn(st.pages().mail().composePopup().expandedPopup().bodyInput())
                .clicksOn(st.pages().mail().composePopup().yabbleCcList().get(0).yabbleDeleteBtn())
                .shouldSee(st.pages().mail().composePopup().composeKukutz())
                .appendTextInElement(st.pages().mail().composePopup().expandedPopup().bodyInput(), LONG_BODY_TEXT);
        };
        parallelRun.withActions(actions).withAcc(lock.firstAcc()).run();
    }

    @Test
    @Title("Показываем промо кукутца")
    @TestCaseId("5825")
    public void shouldSeeKukutzPromo() {
        stepsProd.user().apiSettingsSteps().callWithListAndParams(
            "Включаем промо кукутца",
            of(
                COMPOSE_KUKUTZ_PROMO, FALSE,
                DISABLE_PROMO, STATUS_FALSE
            )
        );

        Consumer<InitStepsRule> actions = st -> {
            openReply(st);
            st.user().defaultSteps()
                .appendTextInElement(st.pages().mail().composePopup().expandedPopup().popupTo(), DEV_NULL_EMAIL)
                .clicksOn(st.pages().mail().composePopup().expandedPopup().bodyInput())
                .shouldSee(st.pages().mail().composePopup().kukutzPromo())
                .waitInSeconds(1);
        };
        parallelRun.withActions(actions).withAcc(lock.firstAcc()).run();
    }

    @Step("Применяем эксперимент нового композа и открываем ответ на письмо")
    private void openReply(InitStepsRule st) {
        st.user().messagesSteps().clicksOnMessageByNumber(0);
        st.user().defaultSteps().clicksOn(st.pages().mail().msgView().toolbar().replyToAllButton())
            .shouldSee(st.pages().mail().composePopup().expandedPopup().bodyInput());
    }

}

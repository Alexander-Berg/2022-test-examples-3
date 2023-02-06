package ru.yandex.autotests.innerpochta.tests.corp;

import static org.hamcrest.Matchers.empty;
import io.qameta.allure.junit4.Tag;
import ru.yandex.autotests.innerpochta.atlas.MailElement;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.data.QuickFragments;
import ru.yandex.autotests.innerpochta.rules.InitStepsRule;
import ru.yandex.autotests.innerpochta.rules.ScreenRulesManager;
import ru.yandex.autotests.innerpochta.rules.SendToStatAfterClassRule;
import ru.yandex.autotests.innerpochta.rules.SetCorpUrlRule;
import ru.yandex.autotests.innerpochta.rules.ZombieProxyEnableRule;
import ru.yandex.autotests.innerpochta.rules.acclock.AccLockRule;
import ru.yandex.autotests.innerpochta.rules.acclock.UseCreds;
import ru.yandex.autotests.innerpochta.util.FeaturesConst;
import ru.yandex.autotests.innerpochta.util.RunAndCompare;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Step;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;

import java.util.List;
import java.util.function.Consumer;

import static com.google.common.collect.ImmutableMap.of;
import static org.hamcrest.Matchers.not;
import static ru.yandex.autotests.innerpochta.rules.ScreenRulesManager.screenRulesManager;
import static ru.yandex.autotests.innerpochta.util.RunAndCompare.runAndCompare;
import static ru.yandex.autotests.innerpochta.util.TestConsts.IGNORED_AREAS;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.EMPTY_STR;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.STATUS_ON;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.LAYOUT_2PANE;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.SETTINGS_OPEN_MSG_LIST;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.SETTINGS_PARAM_LAYOUT;

/**
 * @author mariya-murm
 */
@Aqua.Test
@Title("Аватарки на корпе")
@Features(FeaturesConst.GENERAL)
@Tag(FeaturesConst.GENERAL)
@Stories(FeaturesConst.CORP)
@UseCreds(CorpAvatarsTest.CREDS)
public class CorpAvatarsTest {

    public static final String CREDS = "CorpAttachTest";

    private ScreenRulesManager rules = screenRulesManager().withLock(AccLockRule.use().annotation());
    private InitStepsRule stepsProd = rules.getStepsProd();
    private InitStepsRule stepsTest = rules.getStepsTest();
    private AccLockRule lock = rules.getLock();
    private RunAndCompare parallelRun = runAndCompare().withProdSteps(stepsProd).withTestSteps(stepsTest)
        .withIgnoredAreas(IGNORED_AREAS);

    private static final String SUBJ = "kukutz";
    private static final String ROBOT_EMAIL = "robot-mailcorp-5@yandex-team.ru";
    private static final String DELETE_EXP = "?experiments=12345,0,0";

    @ClassRule
    public static SetCorpUrlRule setCorpUrlRule = new SetCorpUrlRule();

    @ClassRule
    public static ZombieProxyEnableRule zomb = new ZombieProxyEnableRule();

    @ClassRule
    public static SendToStatAfterClassRule sendToStat = new SendToStatAfterClassRule();

    @Rule
    public RuleChain chain = rules.createRuleChain();

    @Before
    public void setUp() {
        stepsProd.user().apiSettingsSteps().callWithListAndParams(
            "Включаем 2пейн и  просмотр письма на отдельной странице",
            of(
                SETTINGS_OPEN_MSG_LIST, EMPTY_STR,
                SETTINGS_PARAM_LAYOUT, LAYOUT_2PANE
            )
        );
        stepsTest.user().loginSteps().forAcc(lock.firstAcc()).loginsToCorp();
    }

    @Test
    @Title("Должны видеть аватарки в списке писем")
    @TestCaseId("3587")
    public void shouldSeeAvatarsInMessageList() {
        Consumer<InitStepsRule> actions = st ->
            st.user().defaultSteps().shouldSee(st.pages().mail().home().avatarImgList());

        parallelRun.withActions(actions).withCorpAcc(lock.firstAcc()).withUrlPath(DELETE_EXP).run();
    }

    @Test
    @Title("Смотрим на аватарки в просмотре письма на отдельной странице")
    @TestCaseId("3587")
    public void shouldSeeAvatarsInMessageFullView() {
        Consumer<InitStepsRule> actions = st -> {
            st.user().messagesSteps().clicksOnMessageWithSubject(SUBJ);
            st.user().defaultSteps().shouldBeOnUrlWith(QuickFragments.MESSAGE)
                .clicksOn(st.pages().mail().msgView().messageHead().recipientsCount());
            shouldSeeAvatarsOnMessageViewLoaded(st);
        };
        parallelRun.withActions(actions).withCorpAcc(lock.firstAcc()).withUrlPath(DELETE_EXP).run();
    }

    @Test
    @Title("Смотрим на аватарки в просмотре письма в списке писем")
    @TestCaseId("3587")
    public void shouldSeeAvatarsInMessageView() {
        Consumer<InitStepsRule> actions = st -> {
            st.user().messagesSteps().clicksOnMessageWithSubject(SUBJ);
            shouldSeeAvatarsLoaded(st);
        };
        enableOpenMsgListSettings();
        parallelRun.withActions(actions).withCorpAcc(lock.firstAcc()).withUrlPath(DELETE_EXP).run();
    }

    @Test
    @Title("Смотрим на аватарки в композе")
    @TestCaseId("3587")
    public void shouldSeeAvatarsInCompose() {
        Consumer<InitStepsRule> actions = st ->
            st.user().defaultSteps().clicksOn(st.pages().mail().home().composeButton())
                .inputsTextInElement(st.pages().mail().composePopup().expandedPopup().popupTo(), ROBOT_EMAIL);

        parallelRun.withActions(actions).withCorpAcc(lock.firstAcc()).run();
    }

    @Test
    @Title("Смотрим на карточку отправителя в просмотре письма")
    @TestCaseId("3587")
    public void shouldSeeMailCardInFullView() {
        Consumer<InitStepsRule> actions = st -> {
            st.user().messagesSteps().clicksOnMessageWithSubject(SUBJ);
            st.user().defaultSteps().clicksOn(st.pages().mail().msgView().messageHead().fromName())
                .shouldSee(st.pages().mail().msgView().mailCard());
            shouldSeeAvatarsLoaded(st);
        };
        parallelRun.withActions(actions).withCorpAcc(lock.firstAcc()).withUrlPath(DELETE_EXP).run();
    }

    @Step("Должны видеть, что все аватарки загрузились")
    private void shouldSeeAvatarsLoaded(InitStepsRule steps) {
        List<MailElement> allAvatars = steps.pages().mail().msgView().allAvatars().waitUntil(not(empty()));
        steps.user().defaultSteps().shouldSee(allAvatars.toArray(new MailElement[0]));
    }

    @Step("Должны видеть, что все аватарки на странице просмотра письма загрузились")
    private void shouldSeeAvatarsOnMessageViewLoaded(InitStepsRule steps) {
        List<MailElement> allAvatars = steps.pages().mail().msgView().allAvatarsMessageView().waitUntil(not(empty()));
        steps.user().defaultSteps().shouldSee(allAvatars.toArray(new MailElement[0]));
    }

    @Step("Включаем просмотр письма в списке писем")
    private void enableOpenMsgListSettings() {
        stepsProd.user().apiSettingsSteps().callWithListAndParams(
            "Включаем просмотр письма в списке писем",
            of(SETTINGS_OPEN_MSG_LIST, STATUS_ON));
    }
}

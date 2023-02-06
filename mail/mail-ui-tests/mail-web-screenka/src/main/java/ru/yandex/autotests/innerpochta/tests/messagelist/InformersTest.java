package ru.yandex.autotests.innerpochta.tests.messagelist;

import io.qameta.allure.junit4.Tag;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
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
import static ru.yandex.autotests.innerpochta.rules.ScreenRulesManager.screenRulesManager;
import static ru.yandex.autotests.innerpochta.util.MailConst.XIVA_TIMEOUT;
import static ru.yandex.autotests.innerpochta.util.RunAndCompare.runAndCompare;
import static ru.yandex.autotests.innerpochta.util.Utils.getRandomName;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.EMPTY_STR;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.NOTIFY_MESSAGE;

/**
 * @author cosmopanda
 */
@Aqua.Test
@Title("Тесты на информер о новых письмах")
@Features(FeaturesConst.MESSAGE_LIST)
@Tag(FeaturesConst.MESSAGE_LIST)
@Stories(FeaturesConst.LEFT_PANEL)
public class InformersTest {

    private ScreenRulesManager rules = screenRulesManager();
    private InitStepsRule stepsProd = rules.getStepsProd();
    private InitStepsRule stepsTest = rules.getStepsTest();
    private AccLockRule lock = rules.getLock().useTusAccount();
    private RunAndCompare parallelRun = runAndCompare().withProdSteps(stepsProd).withTestSteps(stepsTest);

    @ClassRule
    public static ZombieProxyEnableRule zomb = new ZombieProxyEnableRule();

    @ClassRule
    public static SendToStatAfterClassRule sendToStat = new SendToStatAfterClassRule();

    @Rule
    public RuleChain chain = rules.createRuleChain();

    @Before
    public void setUp() {
        stepsProd.user().apiSettingsSteps().callWithListAndParams(
            "Выключаем показ нотификаций",
            of(NOTIFY_MESSAGE, EMPTY_STR)
        );
    }

    @Test
    @Title("Информер о новых письмах в папке")
    @TestCaseId("1937")
    public void shouldSeeNewMessagesInformerForFolders() {
        Consumer<InitStepsRule> actions = this::checkNewMessageInformer;

        parallelRun.withActions(actions).withAcc(lock.firstAcc()).runSequentially();
    }

    @Test
    @Title("Информер о новых письмах в папке пропадает после захода и выхода из папки")
    @TestCaseId("1937")
    public void shouldNotSeeNewMessagesInformerAfterOpenFolder() {
        Consumer<InitStepsRule> actions = st -> {
            checkNewMessageInformer(st);
            st.user().leftColumnSteps().opensInboxFolder();
            st.user().defaultSteps().shouldSee(st.pages().mail().home().foldersNavigation().markReadIcon());
            st.user().leftColumnSteps().opensSentFolder();
            st.user().defaultSteps().shouldSee(st.pages().mail().home().foldersNavigation().inboxUnreadCounter());
        };
        parallelRun.withActions(actions).withAcc(lock.firstAcc()).runSequentially();
    }

    @Step("Проверяем появление информера о новых письмах")
    private void checkNewMessageInformer(InitStepsRule st) {
        st.user().leftColumnSteps().opensSentFolder();
        stepsTest.user().apiMessagesSteps().sendMailWithNoSave(lock.firstAcc(), getRandomName(), "");
        st.user().defaultSteps()
            .shouldSeeWithWaiting(st.pages().mail().home().foldersNavigation().inboxUnreadCounter(), XIVA_TIMEOUT)
            .shouldSeeThatElementHasText(st.pages().mail().home().foldersNavigation().inboxUnreadCounter(), "1");
    }
}

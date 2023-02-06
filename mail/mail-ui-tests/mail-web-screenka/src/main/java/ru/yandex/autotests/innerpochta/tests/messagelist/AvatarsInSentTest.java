package ru.yandex.autotests.innerpochta.tests.messagelist;

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
import ru.yandex.autotests.innerpochta.ns.pages.messages.blocks.MessageBlock;
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

import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;

import static com.google.common.collect.ImmutableMap.of;
import static org.openqa.selenium.By.cssSelector;
import static ru.yandex.autotests.innerpochta.rules.ScreenRulesManager.screenRulesManager;
import static ru.yandex.autotests.innerpochta.util.RunAndCompare.runAndCompare;
import static ru.yandex.autotests.innerpochta.util.TestConsts.IGNORED_ELEMENTS;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.DISABLED_ADV;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.TRUE;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.SETTINGS_FOLDER_THREAD_VIEW;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.SHOW_ADVERTISEMENT;

/**
 * @author vasily-k
 */
@Aqua.Test
@Title("Отображение различных типов аватарок")
@Features(FeaturesConst.MESSAGE_LIST)
@Tag(FeaturesConst.MESSAGE_LIST)
@Stories(FeaturesConst.GENERAL)
@RunWith(DataProviderRunner.class)
public class AvatarsInSentTest {

    private static final String DEFAULT_SUBJECT = "subj";
    public static final String CREDS = "AvatarsTest";

    private ScreenRulesManager rules = screenRulesManager().withLock(AccLockRule.use().names(CREDS));
    private InitStepsRule stepsProd = rules.getStepsProd();
    private InitStepsRule stepsTest = rules.getStepsTest();
    private AccLockRule lock = rules.getLock();
    private RunAndCompare parallelRun = runAndCompare().withProdSteps(stepsProd).withTestSteps(stepsTest);

    @ClassRule
    public static ZombieProxyEnableRule zomb = new ZombieProxyEnableRule();

    @ClassRule
    public static SendToStatAfterClassRule sendToStat = new SendToStatAfterClassRule();

    @Rule
    public RuleChain chain = rules.createRuleChain();

    @Before
    public void disableIgnoringAvatars() {
        Set<By> ignoredElements = new HashSet<>(IGNORED_ELEMENTS);
        ignoredElements.remove(cssSelector(".mail-User-Avatar"));
        parallelRun.withIgnoredElements(ignoredElements);

        stepsProd.user().apiSettingsSteps().callWithListAndParams(
            "Выключаем рекламу, включаем треды",
            of(
                SHOW_ADVERTISEMENT, DISABLED_ADV,
                SETTINGS_FOLDER_THREAD_VIEW, TRUE
            )
        );
    }

    @Test
    @Title("Отображение аватарок на треде в папке «Отправленные»")
    @TestCaseId("4203")
    public void shouldSeeAvatarsInThreadSent() {
        Consumer<InitStepsRule> actions = st -> {
            st.user().leftColumnSteps().opensSentFolder();
            st.user().messagesSteps().expandsMessagesThread(DEFAULT_SUBJECT);
            shouldSeeAvatarsLoaded(st);
        };
        parallelRun.withActions(actions).withAcc(lock.firstAcc()).run();
    }

    @Step("Должны видеть, что все аватарки загрузились")
    private void shouldSeeAvatarsLoaded(InitStepsRule st) {
        for (MessageBlock messageBlock : st.pages().mail().home().displayedMessages().list()) {
            st.user().defaultSteps().shouldSee(messageBlock.avatarImg());
        }
    }
}

package ru.yandex.autotests.innerpochta.tests.differentModes;

import io.qameta.allure.junit4.Tag;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
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

import java.util.Arrays;
import java.util.Collection;
import java.util.function.Consumer;

import static ru.yandex.autotests.innerpochta.data.QuickFragments.ATTACHMENTS;
import static ru.yandex.autotests.innerpochta.data.QuickFragments.COMPOSE;
import static ru.yandex.autotests.innerpochta.data.QuickFragments.DRAFT;
import static ru.yandex.autotests.innerpochta.data.QuickFragments.LITE_CONTACTS;
import static ru.yandex.autotests.innerpochta.data.QuickFragments.SENT;
import static ru.yandex.autotests.innerpochta.data.QuickFragments.SETTINGS;
import static ru.yandex.autotests.innerpochta.data.QuickFragments.SETTINGS_ABOOK;
import static ru.yandex.autotests.innerpochta.data.QuickFragments.SETTINGS_COLLECTORS;
import static ru.yandex.autotests.innerpochta.data.QuickFragments.SETTINGS_FILTERS;
import static ru.yandex.autotests.innerpochta.data.QuickFragments.SETTINGS_FILTERS_CREATE_SIMPLE;
import static ru.yandex.autotests.innerpochta.data.QuickFragments.SETTINGS_FOLDERS;
import static ru.yandex.autotests.innerpochta.data.QuickFragments.SETTINGS_JOURNAL;
import static ru.yandex.autotests.innerpochta.data.QuickFragments.SETTINGS_OTHER;
import static ru.yandex.autotests.innerpochta.data.QuickFragments.SETTINGS_SECURITY;
import static ru.yandex.autotests.innerpochta.data.QuickFragments.SETTINGS_SENDER;
import static ru.yandex.autotests.innerpochta.data.QuickFragments.SPAM;
import static ru.yandex.autotests.innerpochta.data.QuickFragments.TRASH;
import static ru.yandex.autotests.innerpochta.data.QuickFragments.UNREAD;
import static ru.yandex.autotests.innerpochta.rules.ScreenRulesManager.screenRulesManager;
import static ru.yandex.autotests.innerpochta.util.RunAndCompare.runAndCompare;

/**
 * @author cosmopanda
 */

@Aqua.Test
@Title("Lite")
@Features(FeaturesConst.LITE)
@Tag(FeaturesConst.LITE)
@Stories(FeaturesConst.GENERAL)
@RunWith(Parameterized.class)
public class LiteTest {

    private static final String LITE_URL = "/lite/%s";

    private ScreenRulesManager rules = screenRulesManager();
    private InitStepsRule stepsProd = rules.getStepsProd();
    private InitStepsRule stepsTest = rules.getStepsTest();
    private AccLockRule lock = rules.getLock();
    private RunAndCompare parallelRun = runAndCompare().withProdSteps(stepsProd).withTestSteps(stepsTest);

    @Parameterized.Parameter()
    public QuickFragments urlPath;

    @Parameterized.Parameters(name = "url: {0}")
    public static Collection<Object[]> testData() {
        return Arrays.asList(new Object[][]{
            {UNREAD},
            {ATTACHMENTS},
            {SENT},
            {TRASH},
            {SPAM},
            {DRAFT},
            {COMPOSE},
            {SETTINGS},
            {SETTINGS_JOURNAL},
            {SETTINGS_SENDER},
            {SETTINGS_COLLECTORS},
            {SETTINGS_FOLDERS},
            {SETTINGS_FILTERS},
            {SETTINGS_SECURITY},
            {SETTINGS_ABOOK},
            {SETTINGS_OTHER},
            {LITE_CONTACTS},
            {SETTINGS_FILTERS_CREATE_SIMPLE}
        });
    }

    @ClassRule
    public static ZombieProxyEnableRule zomb = new ZombieProxyEnableRule();

    @ClassRule
    public static SendToStatAfterClassRule sendToStat = new SendToStatAfterClassRule();

    @Rule
    public RuleChain chain = rules.createRuleChain();

    @Test
    @Title("Lite: базовые урлы")
    @TestCaseId("74")
    public void shouldSeeCorrectLitePage() {
        Consumer<InitStepsRule> actions = st -> st.user().defaultSteps().waitInSeconds(2);

        parallelRun.withActions(actions).withUrlPath(String.format(LITE_URL, urlPath.fragment()))
            .withAcc(lock.firstAcc()).run();
    }

}

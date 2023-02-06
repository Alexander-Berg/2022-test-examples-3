package ru.yandex.autotests.innerpochta.tests.differentModes;

import com.google.common.collect.Sets;
import io.qameta.allure.junit4.Tag;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.openqa.selenium.By;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.rules.InitStepsRule;
import ru.yandex.autotests.innerpochta.rules.ScreenRulesManager;
import ru.yandex.autotests.innerpochta.rules.SendToStatAfterClassRule;
import ru.yandex.autotests.innerpochta.rules.ZombieProxyEnableRule;
import ru.yandex.autotests.innerpochta.rules.acclock.AccLockRule;
import ru.yandex.autotests.innerpochta.util.FeaturesConst;
import ru.yandex.autotests.innerpochta.util.RunAndCompare;
import ru.yandex.qatools.allure.annotations.Description;
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
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.STATUS_ON;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.LIZA_MINIFIED;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.LIZA_MINIFIED_HEADER;

/**
 * @author cosmopanda
 */
@Aqua.Test
@Title("Компактный режим писем и меню: выпадушки, фильтры")
@Features(FeaturesConst.COMPACT_MODE)
@Tag(FeaturesConst.COMPACT_MODE)
@Stories(FeaturesConst.GENERAL)
@Description("У юзера есть аттачи, подписки, прошлые запросы")
public class CompactModeFiltersTest {

    private static final Set<By> IGNORE_THIS = Sets.newHashSet(
        cssSelector(".mail-CollectorsList-Item"),
        cssSelector(".mail-App-Footer-Group_journal"),
        cssSelector(".ns-view-messages-pager-date")
    );

    private ScreenRulesManager rules = screenRulesManager();
    private InitStepsRule stepsProd = rules.getStepsProd();
    private InitStepsRule stepsTest = rules.getStepsTest();
    private AccLockRule lock = rules.getLock();
    private RunAndCompare parallelRun = runAndCompare().withProdSteps(stepsProd).withTestSteps(stepsTest)
        .withIgnoredElements(IGNORE_THIS);

    @ClassRule
    public static ZombieProxyEnableRule zomb = new ZombieProxyEnableRule();

    @ClassRule
    public static SendToStatAfterClassRule sendToStat = new SendToStatAfterClassRule();

    @Rule
    public RuleChain chain = rules.createRuleChain();

    @Before
    public void setUp() {
        stepsProd.user().apiSettingsSteps().callWithListAndParams(
            "Включаем компактное меню и письма",
            of(
                LIZA_MINIFIED, STATUS_ON,
                LIZA_MINIFIED_HEADER, STATUS_ON
            )
        );
    }

    @Test
    @Title("Переходим в прошлый запрос")
    @TestCaseId("3106")
    public void shouldSeeLastQueries() {
        Consumer<InitStepsRule> actions = st ->
            st.user().defaultSteps()
                .clicksOn(st.pages().mail().home().mail360HeaderBlock().searchBtnCompactMode())
                .shouldSee(st.pages().mail().search().searchSuggest())
                .clicksOn(st.pages().mail().search().lastQueriesList().get(0))
                .shouldSee(st.pages().mail().home().displayedMessages());

        parallelRun.withActions(actions).withAcc(lock.firstAcc()).run();
    }
}

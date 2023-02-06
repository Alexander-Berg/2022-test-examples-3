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
import ru.yandex.autotests.innerpochta.rules.resources.AddFolderIfNeedRule;
import ru.yandex.autotests.innerpochta.rules.resources.AddLabelIfNeedRule;
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
import static ru.yandex.autotests.innerpochta.rules.resources.AddFolderIfNeedRule.addFolderIfNeed;
import static ru.yandex.autotests.innerpochta.rules.resources.AddLabelIfNeedRule.addLabelIfNeed;
import static ru.yandex.autotests.innerpochta.util.MailConst.MAIL_COLLECTOR;
import static ru.yandex.autotests.innerpochta.util.MailConst.PASS_COLLECTOR;
import static ru.yandex.autotests.innerpochta.util.MailConst.SERVER_COLLECTOR;
import static ru.yandex.autotests.innerpochta.util.RunAndCompare.runAndCompare;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.DISABLED_ADV;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.STATUS_ON;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.LIZA_MINIFIED;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.LIZA_MINIFIED_HEADER;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.SHOW_ADVERTISEMENT;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.SIZE_LAYOUT_LEFT;

/**
 * @author cosmopanda
 */
@Aqua.Test
@Title("Компактный режим писем и меню: базовые тесты")
@Features(FeaturesConst.COMPACT_MODE)
@Tag(FeaturesConst.COMPACT_MODE)
@Stories(FeaturesConst.GENERAL)
public class CompactModeTest {

    private static final String FULL_SIZE = "220";
    private static final Set<By> IGNORE_THIS = Sets.newHashSet(
        cssSelector(".mail-CollectorsList-Item"),
        cssSelector(".mail-App-Footer-Group_journal")
    );
    public static final String CREDS = "CompactModeTest";

    private ScreenRulesManager rules = screenRulesManager().withLock(AccLockRule.use().names(CREDS));
    private AccLockRule lock = rules.getLock();
    private InitStepsRule stepsProd = rules.getStepsProd();
    private InitStepsRule stepsTest = rules.getStepsTest();

    private RunAndCompare parallelRun = runAndCompare().withProdSteps(stepsProd).withTestSteps(stepsTest)
        .withAdditionalIgnoredElements(IGNORE_THIS);
    private AddFolderIfNeedRule addFolderIfNeed = addFolderIfNeed(() -> stepsProd.user());
    private AddLabelIfNeedRule addLabelIfNeed = addLabelIfNeed(() -> stepsProd.user());

    @ClassRule
    public static ZombieProxyEnableRule zomb = new ZombieProxyEnableRule();

    @ClassRule
    public static SendToStatAfterClassRule sendToStat = new SendToStatAfterClassRule();

    @Rule
    public RuleChain chain = rules.createRuleChain()
        .around(addFolderIfNeed)
        .around(addLabelIfNeed);

    @Before
    public void setUp() {
        stepsProd.user().apiSettingsSteps().callWithListAndParams(
            "Включаем компактное меню и письма, выключаем компактную ЛК",
            of(
                LIZA_MINIFIED, STATUS_ON,
                LIZA_MINIFIED_HEADER, STATUS_ON,
                SIZE_LAYOUT_LEFT, FULL_SIZE,
                SHOW_ADVERTISEMENT, DISABLED_ADV
            )
        );
        stepsProd.user().apiCollectorSteps().createNewCollector(MAIL_COLLECTOR, PASS_COLLECTOR, SERVER_COLLECTOR);
    }

    @Test
    @Title("Должны видеть только иконку «Отписаться» в тулбаре для рассылок с компактной шапкой")
    @TestCaseId("4218")
    public void shouldSeeUnsubscribeIconOnlyOnCompactToolbar() {
        Consumer<InitStepsRule> actions = st -> {
            st.user().messagesSteps().clicksOnMessageByNumber(0);
            st.user().defaultSteps().shouldSee(st.pages().mail().home().toolbar().unsubscribeButtonIcon());
        };
        parallelRun.withActions(actions).withAcc(lock.firstAcc()).run();
    }
}

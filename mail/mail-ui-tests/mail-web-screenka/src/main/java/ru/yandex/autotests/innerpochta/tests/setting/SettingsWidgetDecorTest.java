package ru.yandex.autotests.innerpochta.tests.setting;

import com.tngtech.java.junit.dataprovider.DataProvider;
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
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;

import java.util.function.Consumer;

import static com.google.common.collect.ImmutableMap.of;
import static ru.yandex.autotests.innerpochta.rules.ScreenRulesManager.screenRulesManager;
import static ru.yandex.autotests.innerpochta.util.RunAndCompare.runAndCompare;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.FALSE;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.SETTINGS_SHOW_WIDGETS_DECOR;

/**
 * @author pavponn
 */
@Aqua.Test
@Title("Вёрстка виджетов с выключенными корешками")
@Features(FeaturesConst.SETTINGS)
@Tag(FeaturesConst.SETTINGS)
@Stories(FeaturesConst.WIDGET)
@RunWith(DataProviderRunner.class)
@Description("Юзеру каждый день приходят письма с билетами. Пак: 5d304df08a900e03944367c7")
public class SettingsWidgetDecorTest {

    private ScreenRulesManager rules = screenRulesManager();
    private InitStepsRule stepsProd = rules.getStepsProd();
    private InitStepsRule stepsTest = rules.getStepsTest();
    private AccLockRule lock = rules.getLock();
    private RunAndCompare parallelRun = runAndCompare().withProdSteps(stepsProd).withTestSteps(stepsTest);

    private final static String FLIGHT_TICKETS = "2";
    private final static String CINEMA_TICKETS = "4";
    private final static String HOTEL_BOOKINGS = "9";

    @ClassRule
    public static ZombieProxyEnableRule zomb = new ZombieProxyEnableRule();

    @ClassRule
    public static SendToStatAfterClassRule sendToStat = new SendToStatAfterClassRule();

    @Rule
    public RuleChain chain = rules.createRuleChain();

    @Before
    public void setUp() {
        stepsProd.user().apiSettingsSteps().callWithListAndParams(
            "Выключаем настройку «выделять цветом брони гостиниц, билеты и покупки»",
            of(SETTINGS_SHOW_WIDGETS_DECOR, FALSE)
        );
    }

    @Test
    @Title("Верстка писем-виджетов без корешков")
    @TestCaseId("2613")
    @DataProvider({FLIGHT_TICKETS, CINEMA_TICKETS, HOTEL_BOOKINGS})
    public void shouldNotSeeDecor(int index) {
        Consumer<InitStepsRule> actions = st -> {
            st.user().leftColumnSteps().opensCustomFolder(index);
        };
        parallelRun.withActions(actions).withAcc(lock.firstAcc()).run();
    }
}

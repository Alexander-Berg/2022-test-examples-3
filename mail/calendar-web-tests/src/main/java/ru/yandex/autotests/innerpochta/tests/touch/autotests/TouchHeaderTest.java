package ru.yandex.autotests.innerpochta.tests.touch.autotests;

import io.qameta.allure.junit4.Tag;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.rules.InitStepsRule;
import ru.yandex.autotests.innerpochta.rules.SendToStatAfterClassRule;
import ru.yandex.autotests.innerpochta.rules.TouchRulesManager;
import ru.yandex.autotests.innerpochta.rules.ZombieProxyEnableRule;
import ru.yandex.autotests.innerpochta.rules.acclock.AccLockRule;
import ru.yandex.autotests.innerpochta.util.FeaturesConst;
import ru.yandex.autotests.innerpochta.util.props.UrlProps;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;

import static ru.yandex.autotests.innerpochta.rules.ClearAccRule.clearAcc;
import static ru.yandex.autotests.innerpochta.rules.TouchRulesManager.touchRulesManager;

/**
 * @author a-zoshchuk
 */
@Aqua.Test
@Title("[Тач] Шапка Календаря")
@Features(FeaturesConst.CAL_TOUCH)
@Tag(FeaturesConst.CAL_TOUCH)
@Stories(FeaturesConst.HEAD)
public class TouchHeaderTest {

    private static final String TEXT_COLOR_ATTRIBUTE = "color";
    private static final String HOLIDAY_DATE_PARAM = "/?show_date=2019-03-09";
    private static final String CELEBRATION_DATE_PARAM = "/?show_date=2019-03-08";
    private static final String HOLIDAY_DATE_GRAY_COLOR = "rgba(153, 153, 153, 1)";
    private static final String CELEBRATION_DAY_RED_COLOR = "rgba(255, 51, 51, 1)";
    private TouchRulesManager rules = touchRulesManager();
    private InitStepsRule steps = rules.getSteps();
    private AccLockRule lock = rules.getLock().useTusAccount();

    @ClassRule
    public static ZombieProxyEnableRule zomb = new ZombieProxyEnableRule();

    @ClassRule
    public static SendToStatAfterClassRule sendToStat = new SendToStatAfterClassRule();

    @Rule
    public RuleChain chain = rules.createCalendarTouchRuleChain()
        .around(clearAcc(() -> steps.user()));

    @Before
    public void setUp() {
        steps.user().loginSteps().forAcc(lock.firstAcc()).logins();
    }

    @Test
    @Title("Проверяем цвет выходных и праздников")
    @TestCaseId("1073")
    public void shouldSeeColoredHolidays() {
        String standUrl = UrlProps.urlProps().getBaseUri();
        steps.user().defaultSteps().opensUrl(standUrl + HOLIDAY_DATE_PARAM)
            .shouldContainCSSAttributeWithValue(
                steps.pages().cal().touchHome().headerDate(),
                TEXT_COLOR_ATTRIBUTE,
                HOLIDAY_DATE_GRAY_COLOR
            )
            .opensUrl(standUrl + CELEBRATION_DATE_PARAM)
            .shouldContainCSSAttributeWithValue(
                steps.pages().cal().touchHome().headerDate(),
                TEXT_COLOR_ATTRIBUTE,
                CELEBRATION_DAY_RED_COLOR
            );
    }
}

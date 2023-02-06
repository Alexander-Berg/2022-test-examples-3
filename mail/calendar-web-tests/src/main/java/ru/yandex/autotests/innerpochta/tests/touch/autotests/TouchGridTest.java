package ru.yandex.autotests.innerpochta.tests.touch.autotests;

import io.qameta.allure.junit4.Tag;
import io.qameta.atlas.webdriver.ElementsCollection;
import ru.yandex.autotests.innerpochta.atlas.MailElement;
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
import ru.yandex.autotests.innerpochta.steps.beans.layer.Layer;
import ru.yandex.autotests.innerpochta.util.FeaturesConst;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;

import java.time.LocalTime;
import java.util.List;

import static com.jayway.jsonassert.impl.matcher.IsEmptyCollection.empty;
import static org.hamcrest.core.IsNot.not;
import static ru.yandex.autotests.innerpochta.rules.ClearAccRule.clearAcc;
import static ru.yandex.autotests.innerpochta.rules.TouchRulesManager.touchRulesManager;

/**
 * @author a-zoshchuk
 */
@Aqua.Test
@Title("[Тач] Сетка Календаря")
@Features(FeaturesConst.CAL_TOUCH)
@Tag(FeaturesConst.CAL_TOUCH)
@Stories(FeaturesConst.GRID)
public class TouchGridTest {

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
    @Title("Возврат на сегодняшнюю дату через кнопку «Сегодня»")
    @TestCaseId("1008")
    public void shouldReturnToTodayGrid() {
        String today = steps.pages().cal().touchHome().headerDate().getText();
        steps.user().defaultSteps()
            .shouldNotSee(steps.pages().cal().touchHome().todayBtn())
            .swipeLeft(getGridRows().get(LocalTime.now().getHour()))
            .clicksOn(steps.pages().cal().touchHome().todayBtn())
            .shouldHasText(steps.pages().cal().touchHome().headerDate(), today);
    }

    @Test
    @Title("Нет полоски текущего времени, если находимся не в сегодняшнем дне")
    @TestCaseId("1009")
    public void shouldNotSeeTimeLineTomorrow() {
        steps.user().defaultSteps()
            .shouldSee(steps.pages().cal().touchHome().nowLine())
            .swipeLeft(getGridRows().get(LocalTime.now().getHour()))
            .shouldNotSeeInViewport(steps.pages().cal().touchHome().nowLine());
    }

    @Test
    @Title("Доступна вся временная сетка при проскролле")
    @TestCaseId("1010")
    public void shouldScrollGrid() {
        steps.user().defaultSteps()
            .scrollTo(getGridRows().get(0))
            .shouldSeeInViewport(getGridRows().get(0))
            .scrollTo(getGridRows().get(24))
            .shouldSeeInViewport(getGridRows().get(24));
    }

    @Test
    @Title("Сетка должна листаться свайпом")
    @TestCaseId("1011")
    public void shouldSwipeGrid() {
        steps.user().defaultSteps().shouldSee(getGridRows().get(LocalTime.now().getHour()))
            .swipeRight(getGridRows().get(LocalTime.now().getHour()));
        steps.user().calTouchGridSteps().shouldBeOnPastDayGrid(1);
        steps.user().defaultSteps().shouldSee(getGridRows().get(48 + LocalTime.now().getHour()))
            .waitInSeconds(1)
            .swipeLeft(getGridRows().get(48 + LocalTime.now().getHour()))
            .shouldNotSee(steps.pages().cal().touchHome().todayBtn());
        steps.user().calTouchGridSteps().shouldBeOnTodayGrid();
        steps.user().defaultSteps().shouldSee(getGridRows().get(LocalTime.now().getHour()))
            .swipeLeft(getGridRows().get(LocalTime.now().getHour()))
            .shouldSee(steps.pages().cal().touchHome().todayBtn());
        steps.user().calTouchGridSteps().shouldBeOnFutureDayGrid(1);
    }

    @Test
    @Title("Не должны видеть события с отключенных слоёв")
    @TestCaseId("1018")
    public void shouldNotSeeEventsFromDisabledLayers() {
        steps.user().apiCalSettingsSteps().createNewLayer(steps.user().settingsCalSteps().formDefaultLayer())
            .createNewLayer(steps.user().settingsCalSteps().formDefaultLayer())
            .createNewLayer(steps.user().settingsCalSteps().formDefaultLayer());
        List<Layer> layers = steps.user().apiCalSettingsSteps().getUserLayers();
        Layer layerToNotShow1 = layers.get(0);
        Layer layerToNotShow2 = layers.get(1);
        Layer layerToShow = layers.get(2);
        steps.user().apiCalSettingsSteps()
            .createNewEvent(steps.user().settingsCalSteps().formDefaultEvent(layerToNotShow1.getId()))
            .createNewEvent(steps.user().settingsCalSteps().formDefaultEvent(layerToNotShow2.getId()))
            .createNewEvent(steps.user().settingsCalSteps().formDefaultEvent(layerToShow.getId()));

        steps.user().defaultSteps().refreshPage()
            .shouldSeeElementsCount(steps.pages().cal().touchHome().events(), 3);
        steps.user().apiCalSettingsSteps().togglerLayer(layerToNotShow1.getId(), false)
            .togglerLayer(layerToNotShow2.getId(), false);
        steps.user().defaultSteps().refreshPage()
            .shouldSeeElementsCount(steps.pages().cal().touchHome().events(), 1)
            .clicksOn(steps.pages().cal().touchHome().events().get(0))
            .shouldContainText(steps.pages().cal().touchHome().eventPage().layer(), layerToShow.getName());

    }

    private ElementsCollection<MailElement> getGridRows() {
        return steps.pages().cal().touchHome().gridRows().waitUntil(not(empty()));
    }

}

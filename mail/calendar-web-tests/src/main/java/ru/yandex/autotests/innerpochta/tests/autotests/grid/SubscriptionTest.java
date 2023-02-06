package ru.yandex.autotests.innerpochta.tests.autotests.grid;

import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import io.qameta.allure.junit4.Tag;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.rules.CalendarRulesManager;
import ru.yandex.autotests.innerpochta.rules.InitStepsRule;
import ru.yandex.autotests.innerpochta.rules.SendToStatAfterClassRule;
import ru.yandex.autotests.innerpochta.rules.ZombieProxyEnableRule;
import ru.yandex.autotests.innerpochta.rules.acclock.AccLockRule;
import ru.yandex.autotests.innerpochta.steps.beans.modelsdoupdateusersettings.Params;
import ru.yandex.autotests.innerpochta.util.FeaturesConst;
import ru.yandex.autotests.webcommon.util.Utils;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;

import static ru.yandex.autotests.innerpochta.rules.CalendarRulesManager.calendarRulesManager;

/**
 * @author crafty
 */
@Aqua.Test
@Title("Подписки")
@Description("У пользователя подключена подписка на календарь кинопоиска")
@Features(FeaturesConst.CAL)
@Tag(FeaturesConst.CAL)
@Stories(FeaturesConst.SUBSCRIPTION)
@RunWith(DataProviderRunner.class)
public class SubscriptionTest {

    private static final String SUB_EVENT_TITLE = "Фильм";
    private static final String SUB_LAYER_TITLE = "Kinopoisk";
    private static final String INVALID_URL = "https://yandex.ru";
    private static final String REQUEST_ID = "Код ошибки";
    private static final String ERROR_TEXT = "Вы уже подписаны на этот адрес через один из ваших календарей";
    private static final String IMPORT_URL = "https://www.kinopoisk.ru/view_export.php?mode=premiers";

    private CalendarRulesManager rules = calendarRulesManager();
    private InitStepsRule steps = rules.getSteps();
    private AccLockRule lock = rules.getLock();

    @ClassRule
    public static ZombieProxyEnableRule zomb = new ZombieProxyEnableRule();

    @ClassRule
    public static SendToStatAfterClassRule sendToStat = new SendToStatAfterClassRule();

    @Rule
    public RuleChain chain = rules.createCalendarRuleChain();

    @Before
    public void setUp() {
        steps.user().apiCalSettingsSteps().updateUserSettings(
            "Разворачиваем левую колонку, подписки, включаем показ выходных",
            new Params()
                .withIsAsideExpanded(true)
                .withIsSubscriptionsListExpanded(true)
                .withShowWeekends(true)
        );
        steps.user().loginSteps().forAcc(lock.firstAcc()).logins();
    }

    @Test
    @Title("Включаем/выключаем отображение событий по подписке")
    @TestCaseId("333")
    public void shouldSeeSubscription() {
        steps.user().defaultSteps()
            .deselects(steps.pages().cal().home().leftPanel().layerSubsCheckBox())
            .waitInSeconds(2) //ждем, пока подписки отключатся
            .shouldNotSeeElementInList(steps.pages().cal().home().eventsAllList(), SUB_EVENT_TITLE)
            .turnTrue(steps.pages().cal().home().leftPanel().layerSubsCheckBox())
            .waitInSeconds(2) //ждем, пока подписки подключатся
            .shouldSeeElementInList(steps.pages().cal().home().eventsAllList(), SUB_EVENT_TITLE);
    }

    @Test
    @Title("Показываем request_id при ошибках")
    @TestCaseId("1137")
    public void shouldSeeErrorMessage() {
        steps.user().defaultSteps()
            .clicksOn(steps.pages().cal().home().leftPanel().addSubscription())
            .shouldSee(steps.pages().cal().home().addFeedSideBar())
            .clicksOn(steps.pages().cal().home().addFeedSideBar().urlInput())
            .inputsTextInElement(steps.pages().cal().home().addFeedSideBar().urlInput(), INVALID_URL)
            .clicksOn(steps.pages().cal().home().addFeedSideBar().createBtn())
            .shouldSeeThatElementHasText(steps.pages().cal().home().errorNotify(), REQUEST_ID);
    }

    @Test
    @Title("Ошибка при попытке повторной подписки на уже добавленный календарь")
    @TestCaseId("449")
    public void shouldNotImportCalTwice() {
        steps.user().defaultSteps().clicksOn(
            steps.pages().cal().home().leftPanel().addSubscription(),
            steps.pages().cal().home().addFeedSideBar().urlInput()
        )
            .inputsTextInElement(steps.pages().cal().home().addFeedSideBar().urlInput(), IMPORT_URL)
            .clicksOn(steps.pages().cal().home().addFeedSideBar().nameInput())
            .inputsTextInElement(steps.pages().cal().home().addFeedSideBar().nameInput(), Utils.getRandomString())
            .clicksOn(steps.pages().cal().home().addFeedSideBar().createBtn());
        steps.user().defaultSteps()
            .shouldSee(steps.pages().cal().home().errorNotify())
            .shouldSeeThatElementHasText(steps.pages().cal().home().errorNotify(), ERROR_TEXT);
    }

    @Test
    @Title("По ховеру на слой подписки выводится полное название слоя")
    @TestCaseId("598")
    public void shouldSeeSubscriptionLayerNameTooltip() {
        steps.user().defaultSteps()
            .shouldHasTitle(steps.pages().cal().home().leftPanel().subscriptionLayersNames().get(0), SUB_LAYER_TITLE);
    }
}

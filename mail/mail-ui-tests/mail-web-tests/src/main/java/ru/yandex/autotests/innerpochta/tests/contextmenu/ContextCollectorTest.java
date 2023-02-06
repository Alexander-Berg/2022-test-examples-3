package ru.yandex.autotests.innerpochta.tests.contextmenu;

import io.qameta.allure.junit4.Tag;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.data.QuickFragments;
import ru.yandex.autotests.innerpochta.rules.RestAssuredAuthRule;
import ru.yandex.autotests.innerpochta.rules.SendToStatAfterClassRule;
import ru.yandex.autotests.innerpochta.rules.acclock.AccLockRule;
import ru.yandex.autotests.innerpochta.steps.AllureStepStorage;
import ru.yandex.autotests.innerpochta.tests.BaseTest;
import ru.yandex.autotests.innerpochta.util.FeaturesConst;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.empty;
import static ru.yandex.autotests.innerpochta.rules.ClearAccRule.clearAcc;
import static ru.yandex.autotests.innerpochta.util.MailConst.MAIL_COLLECTOR;
import static ru.yandex.autotests.innerpochta.util.MailConst.PASS_COLLECTOR;
import static ru.yandex.autotests.innerpochta.util.MailConst.SERVER_COLLECTOR;
import static ru.yandex.autotests.innerpochta.wmi.core.matchers.IsNot.not;

@Aqua.Test
@Title("Проверяем пункт “Создать новый сборщик“ и “Настройки сборщика“")
@Features(FeaturesConst.CONTEXT_MENU)
@Tag(FeaturesConst.CONTEXT_MENU)
@Stories(FeaturesConst.GENERAL)
public class ContextCollectorTest extends BaseTest {

    private static final String URL_COLLECTOR = "&email=ns-collectorforsearch@yandex.ru";
    private static final String CREATE_BTN = "Создать сборщик";
    private static final String SETUP_BTN = "Настроить сборщики";

    private AccLockRule lock = AccLockRule.use().useTusAccount();
    private RestAssuredAuthRule auth = RestAssuredAuthRule.auth(lock);
    private AllureStepStorage user = new AllureStepStorage(webDriverRule, auth);

    @ClassRule
    public static SendToStatAfterClassRule sendToStat = new SendToStatAfterClassRule();

    @Rule
    public RuleChain rules = RuleChain.outerRule(lock).around(auth)
        .around(clearAcc(() -> user));

    @Before
    public void login() {
        user.apiCollectorSteps().createNewCollector(
            MAIL_COLLECTOR,
            PASS_COLLECTOR,
            SERVER_COLLECTOR
        );
        user.loginSteps().forAcc(lock.firstAcc()).logins();
    }

    @Test
    @Title("Создание коллектора")
    @TestCaseId("1227")
    public void createCollector() {
        user.defaultSteps()
            .rightClick(onMessagePage().collectorsNavigation().collectorsList().waitUntil(not(empty())).get(0));
        user.messagesSteps().shouldSeeContextMenu();
        user.defaultSteps().clicksOnElementWithText(onMessagePage().allMenuList().get(0).itemList(), CREATE_BTN)
            .shouldBeOnUrlWith(QuickFragments.SETTINGS_COLLECTORS);
    }

    @Test
    @Title("Редактирование коллектора")
    @TestCaseId("2871")
    public void setupCollector() {
        user.defaultSteps()
            .rightClick(onMessagePage().collectorsNavigation().collectorsList().waitUntil(not(empty())).get(0));
        user.messagesSteps().shouldSeeContextMenu();
        user.defaultSteps().clicksOnElementWithText(onMessagePage().allMenuList().get(0).itemList(), SETUP_BTN)
            .shouldBeOnUrl(containsString(URL_COLLECTOR));
    }
}

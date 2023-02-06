package ru.yandex.autotests.innerpochta.tests.settings;

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
import ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;

import static com.google.common.collect.ImmutableMap.of;
import static ru.yandex.autotests.innerpochta.rules.ClearAccRule.clearAcc;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.STATUS_OFF;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.STATUS_ON;

@Aqua.Test
@Title("Показывать Дела на страницах почты")
@Features(FeaturesConst.SETTINGS)
@Tag(FeaturesConst.SETTINGS)
@Stories(FeaturesConst.TODO_BLOCK)
public class TasksStoryShowTasksBlockTest extends BaseTest {

    private AccLockRule lock = AccLockRule.use().useTusAccount();
    private RestAssuredAuthRule auth = RestAssuredAuthRule.auth(lock);
    private AllureStepStorage user = new AllureStepStorage(webDriverRule, auth);

    @ClassRule
    public static SendToStatAfterClassRule sendToStat = new SendToStatAfterClassRule();

    @Rule
    public RuleChain rules = RuleChain.outerRule(lock).around(auth).around(clearAcc(() -> user));

    @Before
    public void logIn() {
        user.loginSteps().forAcc(lock.firstAcc()).logins(QuickFragments.SETTINGS_TODO);
    }

    @Test
    @Title("Показывать Дела на страницах почты")
    @TestCaseId("1862")
    public void testTurnTodoBlockOn() {
        user.apiSettingsSteps().callWith(of(SettingsConstants.SHOW_TODO, STATUS_ON));
        user.defaultSteps().refreshPage()
            .shouldSee(user.pages().HomePage().toDoWindow());
    }

    @Test
    @Title("Не показывать Дела на страницах почты")
    @TestCaseId("1864")
    public void testTurnTodoBlockOff() {
        user.apiSettingsSteps().callWith(of(SettingsConstants.SHOW_TODO, STATUS_OFF));
        user.defaultSteps().refreshPage()
            .shouldSee(onMessagePage().mail360HeaderBlock())
            .shouldNotSee(user.pages().HomePage().toDoWindow());
    }

    @Test
    @Title("Отмена изменений на странице дел")
    @TestCaseId("1863")
    public void testCancelChangesOnTodoPage() {
        boolean checkboxIsSelected = user.settingsSteps().invertTodoCheckbox();
        user.defaultSteps().shouldSee(onSettingsPage().setupTodo().cancelLink())
            .offsetClick(onSettingsPage().setupTodo().cancelLink(), 1, 1)
            .shouldNotSee(onSettingsPage().setupTodo().cancelLink())
            .shouldSeeCheckBoxesInState(checkboxIsSelected, onSettingsPage().setupTodo().showTodoCheckbox());
    }
}

package ru.yandex.autotests.innerpochta.tests.autotests.LeftPanel;

import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.rules.InitStepsRule;
import ru.yandex.autotests.innerpochta.rules.SendToStatAfterClassRule;
import ru.yandex.autotests.innerpochta.rules.TouchRulesManager;
import ru.yandex.autotests.innerpochta.rules.ZombieProxyEnableRule;
import ru.yandex.autotests.innerpochta.rules.acclock.AccLockRule;
import ru.yandex.autotests.innerpochta.util.FeaturesConst;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Step;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;

import static ru.yandex.autotests.innerpochta.rules.TouchRulesManager.touchRulesManager;
import static ru.yandex.autotests.innerpochta.util.Utils.getRandomString;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.INBOX;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.SPAM;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.TRASH;

/**
 * @author oleshko
 */

@Aqua.Test
@Title("Тесты на попап очистки папок спам и удаленные")
@Features(FeaturesConst.LEFT_PANEL)
@Stories(FeaturesConst.GENERAL)
@RunWith(DataProviderRunner.class)
public class SpamTrashClearPopupTest {

    private TouchRulesManager rules = touchRulesManager().withLock(AccLockRule.use().useTusAccount());
    private AccLockRule accLock = rules.getLock();
    private InitStepsRule steps = rules.getSteps();

    @ClassRule
    public static ZombieProxyEnableRule zomb = new ZombieProxyEnableRule();

    @ClassRule
    public static SendToStatAfterClassRule sendToStat = new SendToStatAfterClassRule();

    @Rule
    public RuleChain chain = rules.createTouchRuleChain();

    @Before
    public void prepare() {
        steps.user().apiMessagesSteps().sendMail(accLock.firstAcc(), getRandomString(), "");
        steps.user().loginSteps().forAcc(accLock.firstAcc()).logins();
    }

    @Test
    @Title("Должны очистить папку из левой колонки")
    @TestCaseId("198")
    @DataProvider({TRASH, SPAM})
    public void shouldEmptyFolderFromLeftPanel(String folder) {
        openClearFolderPopup(folder);
        steps.user().defaultSteps().clicksOn(steps.pages().touch().sidebar().clearFolderPopup().confirm())
            .shouldNotSee(steps.pages().touch().sidebar().clearToggler());
        checkCleaningFolder(folder, 0);
    }

    @Test
    @Title("Должен закрыться попап очистки папки при нажатии отмены в списке папок")
    @TestCaseId("824")
    @DataProvider({TRASH, SPAM})
    public void shouldCloseClearFolderPopupFromLeftPanel(String folder) {
        openClearFolderPopup(folder);
        steps.user().defaultSteps().clicksOn(steps.pages().touch().sidebar().clearFolderPopup().cancelClear())
            .shouldSee(steps.pages().touch().sidebar().clearToggler());
        checkCleaningFolder(folder, 1);
    }

    @Step("Открываем попап очистки папки")
    private void openClearFolderPopup(String folder) {
        steps.user().apiMessagesSteps().moveAllMessagesFromFolderToFolder(INBOX, folder);
        steps.user().defaultSteps().refreshPage()
            .clicksOn(steps.pages().touch().messageList().headerBlock().sidebar())
            .clicksOn(steps.pages().touch().sidebar().clearToggler())
            .shouldSee(steps.pages().touch().sidebar().clearFolderPopup());
    }

    @Step("Проверяем очистилась ли папка")
    private void checkCleaningFolder(String folder, int num) {
        steps.user().defaultSteps().shouldNotSee(steps.pages().touch().sidebar().clearFolderPopup());
        steps.user().apiMessagesSteps().shouldGetMsgCountViaApi(folder, num);
    }
}

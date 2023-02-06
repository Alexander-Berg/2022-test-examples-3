package ru.yandex.autotests.innerpochta.tests.autotests.LeftPanel;

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
import ru.yandex.autotests.innerpochta.util.Utils;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;

import static ru.yandex.autotests.innerpochta.data.QuickFragments.FOLDER_ID;
import static ru.yandex.autotests.innerpochta.rules.TouchRulesManager.touchRulesManager;
import static ru.yandex.autotests.innerpochta.util.MailConst.XIVA_TIMEOUT;

/**
 * @author oleshko
 */

@Aqua.Test
@Title("Тесты на каунтеры и прыщи в списке папок")
@Features(FeaturesConst.LEFT_PANEL)
@Stories(FeaturesConst.GENERAL)
@RunWith(DataProviderRunner.class)
public class TogglesInLeftPanelTest {

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
        steps.user().loginSteps().forAcc(accLock.firstAcc()).logins();
        steps.user().defaultSteps().shouldNotSee(steps.pages().touch().messageList().bootPage());
    }

    @Test
    @Title("Желтый прыщик непрочитанности появился и погас после захода в папку")
    @TestCaseId("14")
    public void recentTogglerInLeftPanelWork() {
        steps.user().defaultSteps().clicksOn(steps.pages().touch().messageList().headerBlock().sidebar())
            .clicksOn(steps.pages().touch().sidebar().folderBlocks().get(1));
        steps.user().apiMessagesSteps().sendMail(accLock.firstAcc(), Utils.getRandomName(), "");
        steps.user().defaultSteps().clicksOn(steps.pages().touch().messageList().headerBlock().sidebar())
            .shouldSeeWithWaiting(steps.pages().touch().sidebar().recentToggler(), XIVA_TIMEOUT)
            .clicksOnElementWithText(steps.pages().touch().sidebar().folderBlocks(), "Входящие")
            .clicksOn(steps.pages().touch().messageList().headerBlock().sidebar())
            .shouldNotSee(steps.pages().touch().sidebar().recentToggler());
    }

    @Test
    @Title("По тапу на каунтер должны увидеть список непрочитанных")
    @TestCaseId("996")
    public void shouldSeeUnreadMessages() {
        steps.user().apiMessagesSteps().sendCoupleMessages(accLock.firstAcc(), 2);
        steps.user().apiMessagesSteps().markLetterRead(
            steps.user().apiMessagesSteps().sendMailWithNoSave(accLock.firstAcc(), Utils.getRandomName(), "")
        );
        steps.user().defaultSteps()
            .shouldSeeElementsCount(steps.pages().touch().messageList().messages(), 3)
            .clicksOn(steps.pages().touch().messageList().headerBlock().sidebar())
            .clicksOn(steps.pages().touch().sidebar().counter())
            .shouldSeeElementsCount(steps.pages().touch().messageList().messages(), 2);
    }

    @Test
    @Title("Желтый прыщик непрочитанности погас после захода в папку по прямому урлу")
    @TestCaseId("1141")
    public void shouldHideFreshCount() {
        steps.user().defaultSteps().clicksOn(steps.pages().touch().messageList().headerBlock().sidebar())
            .clicksOn(steps.pages().touch().sidebar().folderBlocks().get(1));
        steps.user().apiMessagesSteps().sendMail(accLock.firstAcc(), Utils.getRandomName(), "");
        steps.user().defaultSteps().clicksOn(steps.pages().touch().messageList().headerBlock().sidebar())
            .shouldSeeWithWaiting(steps.pages().touch().sidebar().recentToggler(), XIVA_TIMEOUT)
            .opensDefaultUrlWithPostFix(FOLDER_ID.makeTouchUrlPart("1"))
            .clicksOn(steps.pages().touch().messageList().headerBlock().sidebar())
            .shouldNotSee(steps.pages().touch().sidebar().recentToggler());
    }
}

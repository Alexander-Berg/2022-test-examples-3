package ru.yandex.autotests.innerpochta.tests.autotests;

import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import ru.yandex.autotests.innerpochta.atlas.MailElement;
import org.hamcrest.CoreMatchers;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.annotations.DoTestOnlyForEnvironment;
import ru.yandex.autotests.innerpochta.rules.InitStepsRule;
import ru.yandex.autotests.innerpochta.rules.SendToStatAfterClassRule;
import ru.yandex.autotests.innerpochta.rules.TouchRulesManager;
import ru.yandex.autotests.innerpochta.rules.ZombieProxyEnableRule;
import ru.yandex.autotests.innerpochta.rules.acclock.AccLockRule;
import ru.yandex.autotests.innerpochta.util.FeaturesConst;
import ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Step;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;

import static org.hamcrest.Matchers.containsString;
import static ru.yandex.autotests.innerpochta.data.QuickFragments.FOLDER_ID;
import static ru.yandex.autotests.innerpochta.data.QuickFragments.INBOX_FOLDER;
import static ru.yandex.autotests.innerpochta.data.QuickFragments.LABEL_ID;
import static ru.yandex.autotests.innerpochta.data.QuickFragments.MSG_FRAGMENT;
import static ru.yandex.autotests.innerpochta.rules.TouchRulesManager.touchRulesManager;
import static ru.yandex.autotests.innerpochta.util.MailConst.MORDA_URL;
import static ru.yandex.autotests.innerpochta.util.Utils.getRandomName;
import static ru.yandex.autotests.innerpochta.util.Utils.getRandomNumber;
import static ru.yandex.autotests.innerpochta.util.Utils.getRandomString;

/**
 * @author oleshko
 */

@Aqua.Test
@Title("Редиректы")
@Features(FeaturesConst.REDIRECTS)
@Stories(FeaturesConst.GENERAL)
@RunWith(DataProviderRunner.class)
public class RedirectTest {

    private static final String BP_URL = "/u2709";
    private static final String TOUCH = "/touch/";
    private static final String ADDRESS = "ул.Венская 4/1";
    private static final String MAP_URL = "yandex.ru/maps";
    private static final String FAKE_MID = "1";

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
        steps.user().apiMessagesSteps()
            .sendMailWithNoSave(accLock.firstAcc(), getRandomName(), ADDRESS);
        steps.user().loginSteps().forAcc(accLock.firstAcc()).logins();
    }

    @DataProvider
    public static Object[] urlParts() {
        return new Object[][]{
            {TOUCH + getRandomString()},
            {FOLDER_ID.makeTouchUrlPart(Integer.toString(getRandomNumber(10000, 10)))},
            {LABEL_ID.makeTouchUrlPart(getRandomString())},
            {LABEL_ID.makeTouchUrlPart(Integer.toString(getRandomNumber(10000, 10)))}
        };
    }

    @Test
    @Title("Переходим в БП")
    @TestCaseId("9")
    public void shouldGotoFullVersion() {
        goToFullVersion();
    }

    @Test
    @Title("Открываем 404ую страницу")
    @TestCaseId("10")
    @UseDataProvider("urlParts")
    public void shouldOpenNotFoundPage(String url) {
        steps.user().defaultSteps().opensDefaultUrlWithPostFix(url)
            .shouldSee(steps.pages().touch().messageList().notFoundPage().notFoundImg())
            .clicksOn(steps.pages().touch().messageList().notFoundPage().notFoundButton())
            .shouldBeOnUrlWith(INBOX_FOLDER);
    }

    @Test
    @Title("Редиректим в инбокс, если папка несуществует")
    @TestCaseId("10")
    public void shouldRedirectToInbox() {
        steps.user().defaultSteps().opensDefaultUrlWithPostFix(FOLDER_ID.makeTouchUrlPart(getRandomString()))
            .shouldBeOnUrlWith(INBOX_FOLDER);
    }

    @Test
    @Title("[IOS]Переходим по адресу в письме")
    @TestCaseId("376")
    @DoTestOnlyForEnvironment("iOS")
    public void shouldOpenMaps() {
        steps.user().defaultSteps().clicksOn(steps.pages().touch().messageList().messages().get(0).subject())
            .shouldSee(steps.pages().touch().messageView().ymapsLink())
            .clicksOn(steps.pages().touch().messageView().ymapsLink())
            .shouldBeOnUrl(CoreMatchers.containsString(MAP_URL));
    }

    @Test
    @Title("При обновлении должны попадать в тач")
    @TestCaseId("178")
    public void shouldOpenTouch() {
        goToFullVersion();
        steps.user().defaultSteps().opensDefaultUrl().shouldBeOnUrl(containsString(TOUCH));
    }

    @Test
    @Title("Должно открыться письмо по mid'у")
    @TestCaseId("609")
    public void shouldOpenMsgByMid() {
        String mid = steps.user().apiMessagesSteps()
            .getAllMessagesInFolder(HandlersParamNameConstants.INBOX).get(0).getMid();
        steps.user().defaultSteps()
            .opensDefaultUrlWithPostFix(MSG_FRAGMENT.makeTouchUrlPart(mid))
            .shouldSee(steps.pages().touch().messageView().header());
    }

    @Test
    @Title("Должен открыться инбокс по несуществующему mid'у")
    @TestCaseId("610")
    public void shouldOpenInboxByWrongMid() {
        steps.user().defaultSteps()
            .opensDefaultUrlWithPostFix(MSG_FRAGMENT.makeTouchUrlPart(FAKE_MID))
            .shouldSee(steps.pages().touch().messageList().messageBlock());
    }

    @Test
    @Title("Должны открыть тач с морды")
    @TestCaseId("558")
    @DoTestOnlyForEnvironment("Phone")
    public void shouldBackToMorda() {
        openTouchFromMorda(steps.pages().touch().mordaPage().mailBlockPhone());
        steps.getDriver().navigate().back();
        steps.user().defaultSteps().shouldBeOnUrl(containsString(MORDA_URL));
    }

    @Step("Переходим в полную версию почты")
    private void goToFullVersion() {
        steps.user().defaultSteps()
            .clicksOn(steps.pages().touch().messageList().headerBlock().sidebar())
            .scrollTo(steps.pages().touch().sidebar().leftPanelFooter())
            .clicksOn(steps.pages().touch().sidebar().fullVersion())
            .shouldBeOnUrl(containsString(BP_URL));
    }

    @Step("Открываем почту с морды, затем возвращаемся обратно")
    private void openTouchFromMorda(MailElement element) {
        steps.user().defaultSteps()
            .opensUrl(MORDA_URL)
            .clicksIfCanOn(steps.pages().touch().mordaPage().closeMordaPromo())
            .clicksOn(element)
            .switchOnJustOpenedWindow()
            .shouldBeOnUrl(containsString(INBOX_FOLDER.makeTouchUrlPart()));
    }
}


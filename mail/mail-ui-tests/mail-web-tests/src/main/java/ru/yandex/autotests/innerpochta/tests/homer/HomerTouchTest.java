package ru.yandex.autotests.innerpochta.tests.homer;

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
import ru.yandex.autotests.innerpochta.util.props.UrlProps;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;

import static org.hamcrest.Matchers.containsString;
import static ru.yandex.autotests.innerpochta.data.QuickFragments.FOLDER_ID;
import static ru.yandex.autotests.innerpochta.data.QuickFragments.INBOX_FOLDER;
import static ru.yandex.autotests.innerpochta.data.QuickFragments.MSG_FRAGMENT;
import static ru.yandex.autotests.innerpochta.data.QuickFragments.SEARCH_TOUCH;
import static ru.yandex.autotests.innerpochta.rules.TouchRulesManager.touchRulesManager;
import static ru.yandex.autotests.innerpochta.rules.resources.RemoveAllMessagesRule.removeAllMessages;
import static ru.yandex.autotests.innerpochta.util.Utils.getRandomString;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.INBOX;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.SENT;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.TRASH;

/**
 * @author oleshko
 */
@Aqua.Test
@Title("Тесты на шорткаты и открытие БП")
@Features(FeaturesConst.HOMER)
@Stories(FeaturesConst.GENERAL)
@RunWith(DataProviderRunner.class)
public class HomerTouchTest {

    private static final String SEARCH_SHORTCUT = "request=";
    private static final String URL_FOR_LIZA = "/u2709/?dpda=yes";
    private static final String HELP_URL = "https://yandex.ru/support/m-mail/touch.html";
    private static final String APPSTORE = "https://apps.apple.com/ru/app/yandex-mail";
    private static final String GOOGLE_PLAY = "https://play.google.com/store/apps/details?id=ru.yandex.mail";

    private String msgSubject;
    private String folderName;

    private TouchRulesManager rules = touchRulesManager();
    private AccLockRule accLock = rules.getLock();
    private InitStepsRule steps = rules.getSteps();

    @ClassRule
    public static ZombieProxyEnableRule zomb = new ZombieProxyEnableRule();

    @ClassRule
    public static SendToStatAfterClassRule sendToStat = new SendToStatAfterClassRule();

    @Rule
    public RuleChain chain = rules.createTouchRuleChain()
        .around(removeAllMessages(() -> steps.user(), INBOX, SENT, TRASH));

    @Before
    public void prepare() {
        UrlProps.urlProps().setProject("touch");
        folderName = getRandomString();
        steps.user().apiFoldersSteps().deleteAllCustomFolders()
            .createNewFolder(folderName);
        msgSubject = getRandomString();
        steps.user().apiMessagesSteps().sendMailWithNoSave(accLock.firstAcc(), msgSubject, "");
    }

    @Test
    @Title("Должны перейти в помощь")
    @TestCaseId("192")
    public void shouldOpenHelp() {
        steps.user().defaultSteps().opensDefaultUrl()
            .clicksOn(steps.pages().homer().footerBlock().helpLink())
            .shouldBeOnUrl(HELP_URL);
    }

    @Test
    @Title("Должны перейти в поиск")
    @TestCaseId("10")
    public void shouldSeeSearchResult() {
        steps.user().loginSteps().forAcc(accLock.firstAcc()).logins();
        steps.user().defaultSteps()
            .opensDefaultUrlWithPostFix(SEARCH_TOUCH.makeTouchUrlPart() + SEARCH_SHORTCUT + msgSubject)
            .shouldSee(steps.pages().touch().search().messageBlock());
    }

    @Test
    @Title("Должны увидеть открытое на просмотр письмо")
    @TestCaseId("10")
    public void shouldSeeOpenedMsg() {
        String mid = steps.user().apiMessagesSteps().getMessageWithSubject(msgSubject).getMid();
        steps.user().loginSteps().forAcc(accLock.firstAcc()).logins();
        steps.user().defaultSteps().opensDefaultUrlWithPostFix(
                String.format(
                    "%s/%s",
                    INBOX_FOLDER.makeTouchUrlPart(),
                    MSG_FRAGMENT.fragment(mid)
                )
            )
            .shouldSee(steps.pages().touch().messageView().toolbar());
    }

    @Test
    @Title("Должны открыть папку")
    @TestCaseId("10")
    public void shouldOpenMsg() {
        String fid = steps.user().apiFoldersSteps().getFolderBySymbol(folderName).getFid();
        steps.user().loginSteps().forAcc(accLock.firstAcc()).logins();
        steps.user().defaultSteps().opensDefaultUrlWithPostFix(FOLDER_ID.makeTouchUrlPart(fid))
            .shouldSeeThatElementTextEquals(steps.pages().touch().messageList().headerBlock(), folderName);
    }

    @Test
    @Title("Должны открыть БП")
    @TestCaseId("108")
    public void shouldSeeLiza() {
        steps.user().loginSteps().forAcc(accLock.firstAcc()).logins();
        steps.user().defaultSteps().opensDefaultUrlWithPostFix(URL_FOR_LIZA)
            .shouldSee(steps.pages().mail().home().footerLineBlock());
    }

    @Test
    @Title("Должны перейти в аппстор")
    @TestCaseId("192")
    public void shouldOpenAppStore() {
        steps.user().defaultSteps().opensDefaultUrl()
            .clicksOn(steps.pages().homer().appStoreButton())
            .shouldBeOnUrl(containsString(APPSTORE));
    }

    @Test
    @Title("Должны перейти в google play")
    @TestCaseId("192")
    public void shouldOpenGooglePlay() {
        steps.user().defaultSteps().opensDefaultUrl()
            .clicksOn(steps.pages().homer().googlePlayButton())
            .shouldBeOnUrl(containsString(GOOGLE_PLAY));
    }
}

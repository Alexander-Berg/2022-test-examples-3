package ru.yandex.autotests.innerpochta.tests.autotests.Advertisement;

import com.tngtech.java.junit.dataprovider.DataProviderRunner;
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
import ru.yandex.autotests.innerpochta.rules.resources.AddFolderIfNeedRule;
import ru.yandex.autotests.innerpochta.steps.beans.folder.Folder;
import ru.yandex.autotests.innerpochta.util.FeaturesConst;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Step;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;

import static com.google.common.collect.ImmutableMap.of;
import static ru.yandex.autotests.innerpochta.data.QuickFragments.FOLDER_ID;
import static ru.yandex.autotests.innerpochta.data.QuickFragments.INBOX;
import static ru.yandex.autotests.innerpochta.data.QuickFragments.SEARCH_TOUCH;
import static ru.yandex.autotests.innerpochta.data.QuickFragments.SETTINGS_TOUCH_PART;
import static ru.yandex.autotests.innerpochta.rules.TouchRulesManager.touchRulesManager;
import static ru.yandex.autotests.innerpochta.util.MailConst.OLD_USER_TAG;
import static ru.yandex.autotests.innerpochta.util.Utils.getRandomString;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.DISABLED_ADV;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.ENABLED_ADV;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.FALSE;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.TRUE;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.SHOW_ADVERTISEMENT;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.SHOW_ADVERTISEMENT_TOUCH;

/**
 * @author oleshko
 */
@Aqua.Test
@Title("Тесты на рекламу")
@Features(FeaturesConst.ADVERTISEMENT)
@Stories(FeaturesConst.GENERAL)
@RunWith(DataProviderRunner.class)
public class AdvertisementsShowTest {

    private static final String GENERAL = "general";

    private TouchRulesManager rules = touchRulesManager().withLock(AccLockRule.use().useTusAccount(OLD_USER_TAG));
    private AccLockRule accLock = rules.getLock();
    private InitStepsRule steps = rules.getSteps();
    private AddFolderIfNeedRule addFolder = AddFolderIfNeedRule.addFolderIfNeed(() -> steps.user());

    @ClassRule
    public static ZombieProxyEnableRule zomb = new ZombieProxyEnableRule();

    @ClassRule
    public static SendToStatAfterClassRule sendToStat = new SendToStatAfterClassRule();

    @Rule
    public RuleChain chain = rules.createTouchRuleChain()
        .around(addFolder);

    @Before
    public void prep() {
        steps.user().apiMessagesSteps().sendMail(accLock.firstAcc(), getRandomString(), "");
        steps.user().apiSettingsSteps().callWithListAndParams(
            "Включаем показ рекламы",
            of(SHOW_ADVERTISEMENT_TOUCH, TRUE)
        );
        steps.user().loginSteps().forAcc(accLock.firstAcc()).logins();
    }

    @Test
    @Title("Включение рекламы в настройках")
    @TestCaseId("614")
    public void shouldSeeAdvert() {
        steps.user().apiSettingsSteps().callWithListAndParams(
            "Выключаем показ рекламы",
            of(SHOW_ADVERTISEMENT_TOUCH, FALSE)
        );
        steps.user().defaultSteps().opensDefaultUrlWithPostFix(SETTINGS_TOUCH_PART.makeTouchUrlPart(GENERAL))
            .shouldNotSee(steps.pages().touch().settings().advertTogglerOn())
            .clicksOn(steps.pages().touch().settings().advertToggler())
            .shouldSee(steps.pages().touch().settings().advertTogglerOn())
            .opensDefaultUrl()
            .shouldSee(
                steps.pages().touch().messageList().messageBlock(),
                steps.pages().touch().messageList().advertisement().get(0)
            );
    }

    @Test
    @Title("Выключение рекламы в настройках")
    @TestCaseId("615")
    public void shouldNotSeeAdvert() {
        steps.user().defaultSteps().opensDefaultUrlWithPostFix(SETTINGS_TOUCH_PART.makeTouchUrlPart(GENERAL))
            .shouldSee(steps.pages().touch().settings().advertTogglerOn())
            .clicksOn(steps.pages().touch().settings().advertTogglerOn())
            .shouldNotSee(steps.pages().touch().settings().advertTogglerOn())
            .opensDefaultUrl()
            .shouldSee(steps.pages().touch().messageList().messageBlock())
            .shouldNotSee(steps.pages().touch().messageList().advertisement());
    }

    @Test
    @Title("Не должны увидеть рекламу в пустой папке")
    @TestCaseId("583")
    public void shouldNotSeeAdvertInEmptyFolder() {
        Folder clearFolder = steps.user().apiFoldersSteps().getAllUserFolders().get(0);
        steps.user().defaultSteps().opensDefaultUrlWithPostFix(FOLDER_ID.makeTouchUrlPart(clearFolder.getFid()))
            .shouldSee(steps.pages().touch().messageList().emptyFolderImg())
            .shouldNotSee(steps.pages().touch().messageList().advertisement());
    }

    @Test
    @Title("Рекламы не должно быть в поиске")
    @TestCaseId("616")
    public void shouldNotSeeAdvertInSearch() {
        steps.user().defaultSteps().opensDefaultUrlWithPostFix(SEARCH_TOUCH.makeTouchUrlPart())
            .clicksAndInputsText(steps.pages().touch().search().header().input(), accLock.firstAcc().getLogin())
            .clicksOn(steps.pages().touch().search().header().find())
            .shouldSee(steps.pages().touch().messageList().messageBlock())
            .shouldNotSee(steps.pages().touch().messageList().advertisement());
    }

    @Test
    @Title("Должны видеть рекламу в списке закреплённых")
    @TestCaseId("654")
    public void shouldSeeAdvertInPinned() {
        steps.user().apiLabelsSteps().pinLetter(steps.user().apiMessagesSteps().getAllMessages().get(0));
        steps.user().defaultSteps().refreshPage()
            .shouldSee(steps.pages().touch().messageList().advertisement())
            .clicksOn(steps.pages().touch().messageList().pinnedLettersToolbar())
            .shouldSee(
                steps.pages().touch().messageList().messageBlock(),
                steps.pages().touch().messageList().advertisement().get(0)
            );
    }

    @Test
    @Title("Должны видеть рекламу при переходе между папками")
    @TestCaseId("586")
    public void shouldSeeAdvertInOtherFolder() {
        steps.user().defaultSteps().refreshPage()
            .shouldSee(steps.pages().touch().messageList().advertisement())
            .clicksOn(steps.pages().touch().messageList().headerBlock().sidebar())
            .clicksOn(steps.pages().touch().sidebar().folderBlocks().get(1))
            .shouldSee(steps.pages().touch().messageList().emptyFolderImg())
            .shouldNotSee(steps.pages().touch().messageList().advertisement())
            .refreshPage()
            .shouldSee(steps.pages().touch().messageList().emptyFolderImg())
            .clicksOn(steps.pages().touch().messageList().headerBlock().sidebar())
            .clicksOn(steps.pages().touch().sidebar().folderBlocks().get(0))
            .shouldSee(
                steps.pages().touch().messageList().messageBlock(),
                steps.pages().touch().messageList().advertisement().get(0)
            );
    }

    @Test
    @Title("Реклама показывается при обновлении НЕ в списке писем")
    @TestCaseId("801")
    public void shouldSeeAdvertAfterRefreshOutOfMsgList() {
        steps.user().touchSteps().openComposeViaUrl();
        steps.user().defaultSteps().shouldSee(steps.pages().touch().composeIframe().header().sendBtn())
            .clicksOn(steps.pages().touch().composeIframe().header().closeBtn())
            .shouldSee(steps.pages().touch().messageList().advertisement());
    }

    @Test
    @Title("Настройки рекламы независимы в таче и БП")
    @TestCaseId("803")
    public void shouldSeeAdWhenItsOffInLiza() {
        steps.user().apiSettingsSteps().callWithListAndParams(
            "Выключаем показ рекламы в БП и включаем в таче",
            of(
                SHOW_ADVERTISEMENT, DISABLED_ADV,
                SHOW_ADVERTISEMENT_TOUCH, TRUE
            )
        );
        steps.user().defaultSteps().opensDefaultUrlWithPostFix(SETTINGS_TOUCH_PART.makeTouchUrlPart(GENERAL))
            .shouldSee(steps.pages().touch().settings().advertTogglerOn())
            .opensDefaultUrlWithPostFix(INBOX.makeTouchUrlPart())
            .shouldSee(steps.pages().touch().messageList().advertisement());
    }

    @Test
    @Title("Настройки рекламы независимы в таче и БП")
    @TestCaseId("803")
    public void shouldNotSeeAdWhenItsOnInLiza() {
        steps.user().apiSettingsSteps().callWithListAndParams(
            "Включаем показ рекламы в БП и выключаем в таче",
            of(
                SHOW_ADVERTISEMENT, ENABLED_ADV,
                SHOW_ADVERTISEMENT_TOUCH, FALSE
            )
        );
        steps.user().defaultSteps().opensDefaultUrlWithPostFix(SETTINGS_TOUCH_PART.makeTouchUrlPart(GENERAL))
            .shouldNotSee(steps.pages().touch().settings().advertTogglerOn())
            .opensDefaultUrlWithPostFix(INBOX.makeTouchUrlPart())
            .shouldNotSee(steps.pages().touch().messageList().advertisement());
    }

    @Test
    @Title("Реклама отображается после ptr")
    @TestCaseId("581")
    @DoTestOnlyForEnvironment("iOS")
    public void shouldSeeAdAfterPtr() {
        steps.user().defaultSteps().shouldSee(steps.pages().touch().messageList().advertisement());
        steps.user().touchSteps().ptr();
        steps.user().defaultSteps().shouldSee(steps.pages().touch().messageList().advertisement());
    }

    @Test
    @Title("Реклама пропадает при удалении писем")
    @TestCaseId("1119")
    public void shouldHideAdAfterDeleteMsg() {
        steps.user().apiMessagesSteps().sendCoupleMessages(accLock.firstAcc(), 7);
        steps.user().defaultSteps().refreshPage();
        CheckAdAndDeleteMsg();
        CheckAdAndDeleteMsg();
        steps.user().defaultSteps().shouldSeeElementsCount(steps.pages().touch().messageList().advertisement(), 1);
    }

    @Step("Проверячем кол-во рекламы на странице и удаляем письмо")
    private void CheckAdAndDeleteMsg() {
        steps.user().defaultSteps()
            .shouldSeeElementsCount(steps.pages().touch().messageList().advertisement(), 2)
            .clicksOn(steps.pages().touch().messageList().messages().get(0).avatar())
            .clicksOn(steps.pages().touch().messageList().groupOperationsToolbarPhone().delete());
    }
}

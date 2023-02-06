package ru.yandex.autotests.innerpochta.tests.messagefullview;

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
import ru.yandex.autotests.innerpochta.steps.beans.message.Message;
import ru.yandex.autotests.innerpochta.tests.BaseTest;
import ru.yandex.autotests.innerpochta.util.FeaturesConst;
import ru.yandex.autotests.innerpochta.util.Utils;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;

import static com.google.common.collect.ImmutableMap.of;
import static ru.yandex.autotests.innerpochta.rules.ClearAccRule.clearAcc;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.ARCHIVE_RU;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.FALSE;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.FOLDERS_OPEN;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.SETTINGS_OPEN_MSG_LIST;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.SETTINGS_USER_NAME;

@Aqua.Test
@Title("Проверяем кнопки на тулбаре")
@Features(FeaturesConst.MESSAGE_FULL_VIEW)
@Tag(FeaturesConst.MESSAGE_FULL_VIEW)
@Stories(FeaturesConst.TOOLBAR)
public class MessageviewToolbarTest extends BaseTest {

    private static final String PREFIX_FORWARD = "Fwd: ";
    private static final String PREFIX_REPLY = "Re: ";
    @ClassRule
    public static SendToStatAfterClassRule sendToStat = new SendToStatAfterClassRule();
    private Message msg;
    private final AccLockRule lock = AccLockRule.use().useTusAccount();
    private final RestAssuredAuthRule auth = RestAssuredAuthRule.auth(lock);
    private final AllureStepStorage user = new AllureStepStorage(webDriverRule, auth);
    @Rule
    public RuleChain rules = RuleChain.outerRule(lock).around(auth)
        .around(clearAcc(() -> user));

    @Before
    public void setUp() {
        user.apiSettingsSteps().callWithListAndParams(
            "Отключаем открытие письма в списке писем и раскрываем все папки",
            of(
                SETTINGS_OPEN_MSG_LIST, FALSE,
                FOLDERS_OPEN, user.apiFoldersSteps().getAllFids()
            )
        );
        msg = user.apiMessagesSteps()
            .sendMailWithNoSave(lock.firstAcc(), Utils.getRandomName(), Utils.getRandomString());
        user.loginSteps().forAcc(lock.firstAcc()).logins();
        user.defaultSteps().opensFragment(QuickFragments.MSG_FRAGMENT.fragment(msg.getMid()));
    }

    @Test
    @Title("Проверяем, что кнопка «Написать» ведёт из просмотра письма в compose")
    @TestCaseId("1624")
    public void messageViewComposeButton() {
        user.defaultSteps().clicksOn(onMessagePage().composeButton())
            .shouldSee(onComposePopup().expandedPopup());
    }

    @Test
    @Title("Проверяем, что кнопка «Проверить» ведёт из просмотра письма в inbox")
    @TestCaseId("1625")
    public void messageViewCheckMailButton() {
        user.defaultSteps().clicksOn(onMessagePage().checkMailButton())
            .shouldBeOnUrlWith(QuickFragments.INBOX);
    }

    @Test
    @Title("Тест на кнопку «Переслать»")
    @TestCaseId("1628")
    public void messageViewForwardButton() {
        user.defaultSteps().shouldSee(onMessageView().toolbar().forwardButton())
            .shouldNotSee(onMessageView().toolbar().markAsReadButton())
            .onMouseHoverAndClick(onMessageView().toolbar().forwardButton())
            .shouldHasText(onComposePopup().expandedPopup().popupToInput(), "")
            .shouldContainText(onComposePopup().expandedPopup().bodyInput(), msg.getFirstline());
        user.composeSteps().clicksOnAddEmlBtn()
            .shouldSeeSubject(PREFIX_FORWARD + msg.getSubject())
            .shouldSeeMessageAsAttachment(0, msg.getSubject());
    }

    @Test
    @Title("Тест на кнопку «Архивировать»")
    @TestCaseId("1629")
    public void messageViewArchiveButton() {
        user.defaultSteps().onMouseHoverAndClick(onMessagePage().toolbar().configureCustomButtons())
            .clicksOn(
                onCustomButtons().overview().archive(),
                onCustomButtons().overview().saveChangesButton()
            )
            .shouldSee(onMessageView().toolbar().archiveButton())
            .shouldNotSee(onMessageView().toolbar().markAsReadButton())
            .onMouseHoverAndClick(onMessageView().toolbar().archiveButton());
        user.leftColumnSteps().shouldSeeCurrentFolderIs(ARCHIVE_RU)
            .shouldSeeCustomFolderCounter(ARCHIVE_RU, 1);
    }

    @Test
    @Title("Создаем новую метку из тулбара в просмотре письма")
    @TestCaseId("3280")
    public void messageViewCreateLabel() {
        String labelName = Utils.getRandomString();
        user.defaultSteps().shouldNotSee(onMessageView().toolbar().markAsReadButton())
            .clicksOn(onMessageView().toolbar().markMessageDropDown())
            .shouldSee(onMessageView().labelsDropdownMenu())
            .clicksOn(onMessageView().labelsDropdownMenu().createNewLabel());
        user.settingsSteps().inputsLabelName(labelName);
        user.defaultSteps().clicksOn(onFoldersAndLabelsSetup().newLabelPopUp().createMarkButton())
            .shouldNotSee(onFoldersAndLabelsSetup().newLabelPopUp())
            .shouldSeeThatElementTextEquals(onMessageView().messageLabel().get(0), labelName);
    }

    @Test
    @Title("Тест на кнопку «Ответить»")
    @TestCaseId("913")
    public void messageViewReplyButton() {
        String selfName = user.apiSettingsSteps().getUserSettings(SETTINGS_USER_NAME);
        user.defaultSteps().clicksOn(onMessageView().toolbar().replyButton())
            .shouldSee(onComposePopup().expandedPopup());
        user.composeSteps().revealQuotes();
        user.defaultSteps().shouldContainText(onComposePopup().expandedPopup().bodyInput(), msg.getFirstline());
        user.composeSteps().shouldSeeSendToAreaHas(selfName)
            .shouldSeeSubject(PREFIX_REPLY + msg.getSubject());
    }
}

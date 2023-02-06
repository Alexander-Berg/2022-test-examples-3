package ru.yandex.autotests.innerpochta.tests.messageslist;


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
import ru.yandex.autotests.innerpochta.util.Utils;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;

import static com.google.common.collect.ImmutableMap.of;
import static ru.yandex.autotests.innerpochta.rules.ClearAccRule.clearAcc;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.STATUS_TRUE;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.DISABLE_PROMO;

@Aqua.Test
@Title("Тест на пользовательские кнопки для нового юзера")
@Features(FeaturesConst.MESSAGE_LIST)
@Tag(FeaturesConst.MESSAGE_LIST)
@Stories(FeaturesConst.CUSTOM_BUTTONS)
public class CustomButtonsForNewUserTest extends BaseTest {

    private String messageSubject;

    private AccLockRule lock = AccLockRule.use().createAndUseTusAccount();
    private RestAssuredAuthRule auth = RestAssuredAuthRule.auth(lock);
    private AllureStepStorage user = new AllureStepStorage(webDriverRule, auth);

    @ClassRule
    public static SendToStatAfterClassRule sendToStat = new SendToStatAfterClassRule();

    @Rule
    public RuleChain rules = RuleChain.outerRule(lock).around(auth)
        .around(clearAcc(() -> user));

    @Before
    public void logIn() {
        messageSubject = Utils.getRandomString();
        user.apiMessagesSteps().sendMailWithNoSave(lock.firstAcc().getSelfEmail(), messageSubject, "");
        user.apiSettingsSteps().callWithListAndParams(
            "Выключаем показ промо",
            of(DISABLE_PROMO, STATUS_TRUE)
        );
        user.loginSteps().forAcc(lock.firstAcc()).logins();
    }

    @Test
    @Title("Создание кнопки «Автоответ» для нового юзера")
    @TestCaseId("1497")
    public void testAutoReplyButtonForNewUser() {
        user.defaultSteps().onMouseHoverAndClick(onMessagePage().toolbar().configureCustomButtons())
            .clicksOn(onCustomButtons().overview().autoReply())
            .inputsTextInElement(
                onCustomButtons().autoReplyButtonConfigure().autoReplyTextInput(),
                Utils.getRandomString()
            )
            .clicksOn(onCustomButtons().autoReplyButtonConfigure().saveButton())
            .shouldSee(onCustomButtons().overview().activeAutoReplyBtn())
            .clicksOn(onCustomButtons().overview().saveChangesButton());
        user.messagesSteps().clicksOnMessageCheckBoxByNumber(0);
        user.defaultSteps().shouldSee(onMessagePage().toolbar().autoReplyButton());
    }

    @Test
    @Title("Создание кнопки «Архив» для нового юзера")
    @TestCaseId("1498")
    public void testArchiveButtonForNewUser() {
        user.defaultSteps().onMouseHoverAndClick(onMessagePage().toolbar().configureCustomButtons())
            .clicksOn(
                onCustomButtons().overview().archive(),
                onCustomButtons().overview().saveChangesButton()
            );
        user.defaultSteps().clicksOn(user.messagesSteps().findMessageBySubject(messageSubject).checkBox())
            .clicksOn(onMessagePage().toolbar().archiveButton());
        user.messagesSteps().shouldNotSeeMessageWithSubject(messageSubject);
        user.defaultSteps().opensFragment(QuickFragments.ARCHIVE);
        user.messagesSteps().shouldSeeMessageWithSubject(messageSubject);
    }

    @Test
    @Title("Создание кастомных кнопок для нового юзера - проверяем наличие всех кнопок в меню создания.")
    @TestCaseId("1499")
    public void testCustomButtonsForNewUser() {
        String customFolder = Utils.getRandomString();
        user.defaultSteps().opensFragment(QuickFragments.SETTINGS_FOLDERS);
        user.settingsSteps().createNewFolder(customFolder);
        user.defaultSteps().opensFragment(QuickFragments.INBOX);
        user.leftColumnSteps().openFolders()
            .opensCustomFolder(customFolder);
        user.defaultSteps().onMouseHoverAndClick(onMessagePage().toolbar().configureCustomButtons())
            .shouldSee(
                onCustomButtons().overview(),
                onCustomButtons().overview().autoReply(),
                onCustomButtons().overview().archive(),
                onCustomButtons().overview().forward(),
                onCustomButtons().overview().label(),
                onCustomButtons().overview().moveToFolder()
            )
            .clicksOn(onCustomButtons().overview().cancel())
            .shouldNotSee(onCustomButtons().overview())
            .shouldNotSee(onMessagePage().toolbar().addCustomButton())
            .onMouseHoverAndClick(onMessagePage().toolbar().configureCustomButtons())
            .shouldSee(onCustomButtons().overview());
    }
}

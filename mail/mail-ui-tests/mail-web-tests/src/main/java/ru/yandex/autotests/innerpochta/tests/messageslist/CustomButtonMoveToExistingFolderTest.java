package ru.yandex.autotests.innerpochta.tests.messageslist;


import io.qameta.allure.junit4.Tag;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import ru.yandex.aqua.annotations.project.Aqua;
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

import java.io.IOException;

import static ru.yandex.autotests.innerpochta.rules.ClearAccRule.clearAcc;
import static ru.yandex.autotests.innerpochta.util.MailConst.SPAM_RU;

/**
 * Created with IntelliJ IDEA.
 * User: arttimofeev
 * Date: 19.09.12
 * Time: 19:24
 */
@Aqua.Test
@Title("Тест на создание кнопки для перемещения письма в существующую папку")
@Features(FeaturesConst.MESSAGE_LIST)
@Tag(FeaturesConst.MESSAGE_LIST)
@Stories(FeaturesConst.CUSTOM_BUTTONS)
public class CustomButtonMoveToExistingFolderTest extends BaseTest {

    private final static String USER_FOLDER = "folder";

    private String subject;
    private AccLockRule lock = AccLockRule.use().useTusAccount();
    private RestAssuredAuthRule auth = RestAssuredAuthRule.auth(lock);
    private AllureStepStorage user = new AllureStepStorage(webDriverRule, auth);

    @ClassRule
    public static SendToStatAfterClassRule sendToStat = new SendToStatAfterClassRule();

    @Rule
    public RuleChain rules = RuleChain.outerRule(lock)
        .around(auth)
        .around(clearAcc(() -> user));

    @Before
    public void logIn() throws IOException {
        user.apiFoldersSteps().createNewFolder(USER_FOLDER);
        subject = user.apiMessagesSteps().sendMailWithNoSave(lock.firstAcc(), Utils.getRandomName(), "").getSubject();
        user.loginSteps().forAcc(lock.firstAcc()).logins();
        user.messagesSteps().removesToolbarCustomButton(onCustomButtons().overview().deleteMoveToFolderButton())
            .shouldSeeMessageWithSubject(subject);
        user.defaultSteps().onMouseHoverAndClick(onMessagePage().toolbar().configureCustomButtons());
    }

    @Test
    @Title("Создание кнопки для перемещения письма в существующую папку")
    @TestCaseId("1494")
    public void testCreateAutoMoveButtonForExistingFolder() {
        createCustomButtonForFolder(USER_FOLDER);
        user.defaultSteps().clicksOn(onMessagePage().toolbar().autoMoveButton());
        user.messagesSteps().shouldNotSeeMessageWithSubject(subject);
        user.leftColumnSteps().openFolders()
            .opensCustomFolder(0);
        user.messagesSteps().shouldSeeMessageWithSubject(subject);
    }

    @Test
    @Title("Создание кнопки для перемещения письма в папку Спам")
    @TestCaseId("1495")
    public void testCreateAutoMoveButtonForSpamFolder() {
        createCustomButtonForFolder(SPAM_RU);
        user.defaultSteps().clicksOn(onMessagePage().toolbar().autoMoveButton());
        user.messagesSteps().shouldNotSeeMessageWithSubject(subject);
        user.leftColumnSteps().opensSpamFolder();
        user.messagesSteps().shouldSeeMessageWithSubject(subject);
    }

    @Test
    @Title("Удаление кнопки для перемещения письма в папку")
    @TestCaseId("3941")
    public void shouldDeleteAutoMoveButtonForUserFolder() {
        createCustomButtonForFolder(USER_FOLDER);
        user.messagesSteps().removesToolbarCustomButton(onCustomButtons().overview().deleteMoveToFolderButton());
        user.defaultSteps().shouldNotSee(onMessagePage().toolbar().autoMoveButton());
    }

    private void createCustomButtonForFolder(String folder) {
        user.defaultSteps().clicksOn(onCustomButtons().overview().moveToFolder())
            .selectsOption(onCustomButtons().configureFoldersButton().folderSelect(), folder)
            .clicksOn(onCustomButtons().configureFoldersButton().saveButton())
            .shouldNotSee(onCustomButtons().configureFoldersButton())
            .clicksOn(onCustomButtons().overview().saveChangesButton())
            .shouldNotSee(onCustomButtons().overview().saveChangesButton());
        user.messagesSteps().clicksOnMessageCheckBoxByNumber(0);
        user.defaultSteps().shouldSeeThatElementTextEquals(onMessagePage().toolbar().autoMoveButton(), folder);
    }
}

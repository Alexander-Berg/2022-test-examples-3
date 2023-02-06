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

import java.io.IOException;

import static com.google.common.collect.ImmutableMap.of;
import static ru.yandex.autotests.innerpochta.rules.ClearAccRule.clearAcc;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.FOLDERS_OPEN;

/**
 * Created with IntelliJ IDEA.
 * User: arttimofeev
 * Date: 19.09.12
 * Time: 19:24
 */
@Aqua.Test
@Title("Тест на создание кнопки для перемещения письма в новую папку")
@Features(FeaturesConst.MESSAGE_LIST)
@Tag(FeaturesConst.MESSAGE_LIST)
@Stories(FeaturesConst.CUSTOM_BUTTONS)
public class CustomButtonMoveToNewFolderTest extends BaseTest {

    private static final String NEW_FOLDER = "Новая папка…";

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
        subject = user.apiMessagesSteps().sendMailWithNoSave(lock.firstAcc(), Utils.getRandomName(), "").getSubject();
        user.loginSteps().forAcc(lock.firstAcc()).logins(QuickFragments.INBOX);
        user.messagesSteps().shouldSeeMessageWithSubject(subject)
            .removesToolbarCustomButton(onCustomButtons().overview().deleteMoveToFolderButton());
        user.defaultSteps().shouldNotSee(onMessagePage().toolbar().autoMoveButtonIcon())
            .opensDefaultUrl();
    }

    @Test
    @Title("Создание кнопки для перемещения письма в новую папку")
    @TestCaseId("1496")
    public void testCreateAutoMoveButtonForNewFolder() {
        String folder = Utils.getRandomString();
        /* Ждем, пока заработает «Шестеренка редактирования пользовательских кнопок» */
        user.defaultSteps().waitInSeconds(2)
            .onMouseHoverAndClick(onMessagePage().toolbar().configureCustomButtons())
            .clicksOn(onCustomButtons().overview().moveToFolder())
            .selectsOption(onCustomButtons().configureFoldersButton().folderSelect(), NEW_FOLDER)
            .inputsTextInElement(onCustomButtons().configureFoldersButton().folderNameInput(), folder)
            .clicksOn(onCustomButtons().configureFoldersButton().saveButton())
            .clicksOn(onCustomButtons().overview().saveChangesButton())
            /* ждем, когда закончится анимация смены блока */
            .waitInSeconds(1)
            .shouldNotSee(onCustomButtons().overview());
        user.messagesSteps().selectMessageWithSubject(subject);
        user.defaultSteps().shouldSeeThatElementTextEquals(onMessagePage().toolbar().autoMoveButton(), folder)
            .clicksOn(onMessagePage().toolbar().autoMoveButton());
        user.messagesSteps().shouldNotSeeMessageWithSubject(subject);
        user.leftColumnSteps().openFolders()
            .opensCustomFolder(0);
        user.messagesSteps().shouldSeeMessageWithSubject(subject);
    }
}

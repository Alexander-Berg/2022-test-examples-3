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
import ru.yandex.autotests.innerpochta.util.Utils;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;

import java.io.IOException;

import static ru.yandex.autotests.innerpochta.rules.ClearAccRule.clearAcc;

@Aqua.Test
@Title("Очистка папок по теме и отправителю")
@Description("Тесты на страницу папок и меток")
@Features(FeaturesConst.SETTINGS)
@Tag(FeaturesConst.SETTINGS)
@Stories(FeaturesConst.FOLDERS_LABELS)
public class FoldersAndLabelsStoryCleanFolderBySubjectTest extends BaseTest {

    private String subject;

    private AccLockRule lock = AccLockRule.use().useTusAccount();
    private RestAssuredAuthRule auth = RestAssuredAuthRule.auth(lock);
    private AllureStepStorage user = new AllureStepStorage(webDriverRule, auth);

    @ClassRule
    public static SendToStatAfterClassRule sendToStat = new SendToStatAfterClassRule();

    @Rule
    public RuleChain rules = RuleChain.outerRule(lock).around(auth).around(clearAcc(() -> user));

    @Before
    public void logIn() throws InterruptedException, IOException {
        subject = user.apiMessagesSteps().sendMailWithNoSave(lock.firstAcc(), Utils.getRandomName(), "").getSubject();
        user.loginSteps().forAcc(lock.firstAcc()).logins(QuickFragments.SETTINGS_FOLDERS);
        user.defaultSteps().onMouseHoverAndClick(onFoldersAndLabelsSetup().setupBlock().folders().inboxFolderCounter())
            .clicksOn(onFoldersAndLabelsSetup().setupBlock().folders().clearCustomFolder())
            .clicksOn(onFoldersAndLabelsSetup().cleanFolderPopUp().advancedOptions());
    }

    @Test
    @Title("Перемещение писем в папку «Удалённые» по теме")
    @TestCaseId("1744")
    public void testMoveMessagesToTrashBySubject() {
        user.defaultSteps().clicksOn(onFoldersAndLabelsSetup().cleanFolderPopUp().cancelBtn())
            .shouldNotSee(onFoldersAndLabelsSetup().cleanFolderPopUp())
            .clicksOn(
                onFoldersAndLabelsSetup().setupBlock().folders().clearCustomFolder(),
                onFoldersAndLabelsSetup().cleanFolderPopUp().closePopUpBtn()
            )
            .shouldNotSee(onFoldersAndLabelsSetup().cleanFolderPopUp())
            .clicksOn(
                onFoldersAndLabelsSetup().setupBlock().folders().clearCustomFolder(),
                onFoldersAndLabelsSetup().cleanFolderPopUp().advancedOptions()
            )
            .inputsTextInElement(onFoldersAndLabelsSetup().cleanFolderPopUp().subject(), subject);
        cleansFolder();
    }

    @Test
    @Title("Перемещение писем в папку «Удалённые» по отправителю")
    @TestCaseId("1745")
    public void testMoveMessagesToTrashBySender() {
        user.defaultSteps().inputsTextInElement(
            onFoldersAndLabelsSetup().cleanFolderPopUp().address(),
            lock.firstAcc().getSelfEmail()
        );
        cleansFolder();
    }

    @Test
    @Title("Удаление писем по отправителю")
    @TestCaseId("1746")
    public void testDeleteMessagesBySender() {
        user.defaultSteps().inputsTextInElement(
            onFoldersAndLabelsSetup().cleanFolderPopUp().address(),
            lock.firstAcc().getSelfEmail()
        );
        cleansFolder();
    }

    @Test
    @Title("Удаление писем по теме")
    @TestCaseId("1747")
    public void testDeleteMessagesBySubject() {
        user.defaultSteps().inputsTextInElement(onFoldersAndLabelsSetup().cleanFolderPopUp().subject(), subject);
        cleansFolder();
    }

    private void cleansFolder() {
        user.defaultSteps().clicksOn(onFoldersAndLabelsSetup().cleanFolderPopUp().confirmCleaningBtn())
            .opensFragment(QuickFragments.INBOX)
            .refreshPage();
        user.messagesSteps().shouldNotSeeMessageWithSubject(subject);
        user.leftColumnSteps().opensTrashFolder();
        user.messagesSteps().shouldSeeMessageWithSubject(subject);
    }
}

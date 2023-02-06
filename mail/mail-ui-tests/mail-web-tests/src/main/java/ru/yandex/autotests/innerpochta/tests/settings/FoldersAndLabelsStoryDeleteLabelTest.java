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
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;

import static ru.yandex.autotests.innerpochta.rules.ClearAccRule.clearAcc;
import static ru.yandex.autotests.innerpochta.util.handlers.LabelsConstants.LABELS_PARAM_GREEN_COLOR;

@Aqua.Test
@Title("Удаление метки в которой есть письма")
@Features(FeaturesConst.SETTINGS)
@Tag(FeaturesConst.SETTINGS)
@Stories(FeaturesConst.FOLDERS_LABELS)
public class FoldersAndLabelsStoryDeleteLabelTest extends BaseTest {

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
    public void logIn() throws InterruptedException {
        user.loginSteps().forAcc(lock.firstAcc()).logins(QuickFragments.INBOX);
    }

    @Test
    @Title("Удаляем метку в которой есть письма")
    @TestCaseId("1759")
    public void testDeleteLabelWithMessages() {
        String labelName = Utils.getRandomString();
        user.apiLabelsSteps().addNewLabel(labelName, LABELS_PARAM_GREEN_COLOR);
        String subj = Utils.getRandomName();
        user.apiMessagesSteps().sendMailWithNoSave(lock.firstAcc(), subj, subj + subj);
        user.defaultSteps().refreshPage();
        user.messagesSteps().clicksOnMessageCheckBoxWithSubject(subj)
            .markMessageWithCustomLabel(labelName);
        user.defaultSteps().opensFragment(QuickFragments.SETTINGS_FOLDERS)
            .clicksOn(onFoldersAndLabelsSetup().setupBlock().labels().userLabelsList().get(0))
            .clicksOn(onFoldersAndLabelsSetup().setupBlock().labels().deleteLabel());
        user.settingsSteps().shouldSeeConfirmationPopUp(labelName);
        user.defaultSteps().clicksOn(onFoldersAndLabelsSetup().deleteLabelPopUpOld().cancelBtnOld())
            .shouldNotSee(onFoldersAndLabelsSetup().deleteLabelPopUpOld().cancelBtnOld())
            .clicksOn(onFoldersAndLabelsSetup().setupBlock().labels().deleteLabel())
            .clicksOn(onFoldersAndLabelsSetup().deleteLabelPopUpOld().closePopUpBtnOld())
            .shouldNotSee(onFoldersAndLabelsSetup().deleteLabelPopUpOld().cancelBtnOld())
            .clicksOn(onFoldersAndLabelsSetup().setupBlock().labels().deleteLabel())
            .clicksOn(onFoldersAndLabelsSetup().deleteLabelPopUpOld().deleteBtnOld());
        user.settingsSteps().shouldSeeLabelsCount(0);
        user.defaultSteps().opensFragment(QuickFragments.INBOX)
            .shouldNotSee(user.pages().MessagePage().labelsNavigation());
        user.messagesSteps().shouldNotSeeThatMessageIsLabeledWith(labelName, subj);
    }

    @Test
    @Title("Создаем, переименовываем, удаляем метку из настроек")
    @TestCaseId("1091")
    public void testCreateLabelInterface() {
        user.defaultSteps().opensFragment(QuickFragments.SETTINGS_FOLDERS);
        String name = Utils.getRandomString();
        user.settingsSteps().createsNewLabelIfNeed(name)
            .shouldSeeLabelCreated(name);
        String newLabelName = user.settingsSteps().renameLabel();
        user.settingsSteps().shouldSeeLabelCreated(newLabelName)
            .shouldSeeLabelsCount(1);
        user.defaultSteps().clicksOn(
            onFoldersAndLabelsSetup().setupBlock().labels().userLabelsList().get(0),
            onFoldersAndLabelsSetup().setupBlock().labels().deleteLabel()
        );
        user.settingsSteps().shouldSeeLabelsCount(0);
    }
}

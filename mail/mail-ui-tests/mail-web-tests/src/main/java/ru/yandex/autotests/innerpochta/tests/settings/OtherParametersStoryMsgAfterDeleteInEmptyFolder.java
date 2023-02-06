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

import java.io.IOException;

import static com.google.common.collect.ImmutableMap.of;
import static ru.yandex.autotests.innerpochta.rules.ClearAccRule.clearAcc;
import static ru.yandex.autotests.innerpochta.rules.acclock.AccLockRule.use;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.FOLDERS_OPEN;

@Aqua.Test
@Title("Следующее письмо после удаления текущего в папке, где больше нет писем")
@Features(FeaturesConst.SETTINGS)
@Tag(FeaturesConst.SETTINGS)
@Stories(FeaturesConst.OTHER_SETTINGS)
public class OtherParametersStoryMsgAfterDeleteInEmptyFolder extends BaseTest {

    private static final String CUSTOM_FOLDER = "folder";

    private String subject;

    private AccLockRule lock = use().useTusAccount();
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
        String foldersFids = user.apiFoldersSteps().getAllFids();
        user.apiSettingsSteps().callWithListAndParams(
            "Раскрываем все папки",
            of(FOLDERS_OPEN, foldersFids)
        );
        subject = user.apiMessagesSteps().sendMailWithNoSave(lock.firstAcc(), Utils.getRandomName(), "").getSubject();
        user.apiFoldersSteps().createNewFolder(CUSTOM_FOLDER);
        user.loginSteps().forAcc(lock.firstAcc()).logins();
        user.messagesSteps().selectMessageWithSubject(subject)
            .movesMessageToFolder(CUSTOM_FOLDER);
    }

    @Test
    @Title("Следующее письмо после удаления текущего в папке, где больше нет писем")
    @TestCaseId("1794")
    public void testNextMessagePageAfterDeleteInEmptyFolder() {
        user.leftColumnSteps().opensCustomFolder(CUSTOM_FOLDER);
        user.messagesSteps().shouldSeeMessageWithSubject(subject)
            .selectMessageWithSubject(subject);
        user.defaultSteps().clicksOn(onMessagePage().toolbar().deleteButton())
            .shouldBeOnUrlWith(QuickFragments.FOLDER);
        user.leftColumnSteps().shouldBeInFolder(CUSTOM_FOLDER);
        user.defaultSteps().shouldSee(onMessagePage().emptyFolder().inboxLink());
    }
}

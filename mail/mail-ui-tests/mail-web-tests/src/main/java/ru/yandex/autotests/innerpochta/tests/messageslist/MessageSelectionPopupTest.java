package ru.yandex.autotests.innerpochta.tests.messageslist;

import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import io.qameta.allure.junit4.Tag;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.data.QuickFragments;
import ru.yandex.autotests.innerpochta.rules.RestAssuredAuthRule;
import ru.yandex.autotests.innerpochta.rules.SendToStatAfterClassRule;
import ru.yandex.autotests.innerpochta.rules.acclock.AccLockRule;
import ru.yandex.autotests.innerpochta.steps.AllureStepStorage;
import ru.yandex.autotests.innerpochta.tests.BaseTest;
import ru.yandex.autotests.innerpochta.util.FeaturesConst;
import ru.yandex.autotests.innerpochta.util.Utils;
import ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;

import static ru.yandex.autotests.innerpochta.data.QuickFragments.ARCHIVE;
import static ru.yandex.autotests.innerpochta.data.QuickFragments.DRAFT;
import static ru.yandex.autotests.innerpochta.data.QuickFragments.INBOX;
import static ru.yandex.autotests.innerpochta.data.QuickFragments.OUTBOX;
import static ru.yandex.autotests.innerpochta.data.QuickFragments.SENT;
import static ru.yandex.autotests.innerpochta.data.QuickFragments.TEMPLATE;

/**
 * @author mariya-murm
 */
@Aqua.Test
@Title("Список писем - выбрать все письма")
@Features(FeaturesConst.MESSAGE_LIST)
@Tag(FeaturesConst.MESSAGE_LIST)
@Stories(FeaturesConst.GENERAL)
@RunWith(DataProviderRunner.class)
public class MessageSelectionPopupTest extends BaseTest {

    private AccLockRule lock = AccLockRule.use().useTusAccount();
    private RestAssuredAuthRule auth = RestAssuredAuthRule.auth(lock);
    private AllureStepStorage user = new AllureStepStorage(webDriverRule, auth);

    @ClassRule
    public static SendToStatAfterClassRule sendToStat = new SendToStatAfterClassRule();

    @Rule
    public RuleChain rules = RuleChain.outerRule(lock).around(auth);

    @Before
    public void logIn() {
        user.apiFoldersSteps().createArchiveFolder();
        for (int i = 0; i < 2; i++) {
            user.apiMessagesSteps().sendMail(lock.firstAcc(), Utils.getRandomString(), "");
            user.apiMessagesSteps().sendMail(lock.firstAcc(), Utils.getRandomString(), "");
            user.apiMessagesSteps().createDraftWithSubject(Utils.getRandomString());
            user.apiMessagesSteps().createTemplateMessage(lock.firstAcc());
            user.apiMessagesSteps().moveMessagesFromFolderToFolder(
                HandlersParamNameConstants.ARCHIVE,
                user.apiMessagesSteps().getAllMessages().get(0)
            )
                .sendMailWithSentTime(lock.firstAcc(), Utils.getRandomName(), "");
        }
        user.loginSteps().forAcc(lock.firstAcc()).logins();
    }

    @DataProvider
    public static Object[][] folders() {
        return new Object[][]{
            {ARCHIVE},
            {INBOX},
            {SENT},
            {DRAFT},
            {TEMPLATE},
            {OUTBOX}
        };
    }

    @Test
    @Title("Плашка выделения писем есть во всех папках")
    @TestCaseId("4033")
    @UseDataProvider("folders")
    public void shouldSelectAllMessages(QuickFragments fragment) {
        user.defaultSteps().opensFragment(fragment);
        user.messagesSteps().selectsAllDisplayedMessagesInFolder();
        user.defaultSteps().shouldSee(onMessagePage().selectAllMessagesPopup());
    }
}

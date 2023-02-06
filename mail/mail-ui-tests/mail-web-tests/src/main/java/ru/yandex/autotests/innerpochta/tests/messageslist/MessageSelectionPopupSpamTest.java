package ru.yandex.autotests.innerpochta.tests.messageslist;

import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import io.qameta.allure.junit4.Tag;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.rules.RestAssuredAuthRule;
import ru.yandex.autotests.innerpochta.rules.SendToStatAfterClassRule;
import ru.yandex.autotests.innerpochta.rules.acclock.AccLockRule;
import ru.yandex.autotests.innerpochta.steps.AllureStepStorage;
import ru.yandex.autotests.innerpochta.steps.beans.message.Message;
import ru.yandex.autotests.innerpochta.tests.BaseTest;
import ru.yandex.autotests.innerpochta.util.FeaturesConst;
import ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;

import static ru.yandex.autotests.innerpochta.data.QuickFragments.SPAM;
import static ru.yandex.autotests.innerpochta.data.QuickFragments.TRASH;
import static ru.yandex.autotests.innerpochta.rules.ClearAccRule.clearAcc;

/**
 * @author mariya-murm
 */
@Aqua.Test
@Title("Список писем - выбрать все письма")
@Features(FeaturesConst.MESSAGE_LIST)
@Tag(FeaturesConst.MESSAGE_LIST)
@Stories(FeaturesConst.GENERAL)
@RunWith(DataProviderRunner.class)
public class MessageSelectionPopupSpamTest extends BaseTest {

    private Message msgDelete1, msgDelete2, msgSpam1, msgSpam2;

    private AccLockRule lock = AccLockRule.use().useTusAccount();
    private RestAssuredAuthRule auth = RestAssuredAuthRule.auth(lock);
    private AllureStepStorage user = new AllureStepStorage(webDriverRule, auth);

    @ClassRule
    public static SendToStatAfterClassRule sendToStat = new SendToStatAfterClassRule();

    @Rule
    public RuleChain rules = RuleChain.outerRule(lock).around(auth)
        .around(clearAcc(() -> user));

    @Before
    public void logIn() {
        user.apiMessagesSteps().sendCoupleMessages(lock.firstAcc(), 4);
        user.loginSteps().forAcc(lock.firstAcc()).logins();
        msgSpam1 = user.apiMessagesSteps().getAllMessagesInFolder(HandlersParamNameConstants.INBOX).get(0);
        msgSpam2 = user.apiMessagesSteps().getAllMessagesInFolder(HandlersParamNameConstants.INBOX).get(1);
        msgDelete1 = user.apiMessagesSteps().getAllMessagesInFolder(HandlersParamNameConstants.INBOX).get(2);
        msgDelete2 = user.apiMessagesSteps().getAllMessagesInFolder(HandlersParamNameConstants.INBOX).get(3);
        user.apiMessagesSteps().moveMessagesToSpam(msgSpam1, msgSpam2)
            .deleteMessages(msgDelete1, msgDelete2);
    }

    @Test
    @Title("Плашка выделения писем есть в папках Удаленные и Спам")
    @TestCaseId("4033")
    public void shouldSelectAllMessagesInSpamFolder() {
        user.defaultSteps().opensFragment(TRASH);
        user.messagesSteps().selectsAllDisplayedMessagesInFolder();
        user.defaultSteps().shouldSee(onMessagePage().selectAllMessagesPopup())
            .opensFragment(SPAM);
        user.messagesSteps().selectsAllDisplayedMessagesInFolder();
        user.defaultSteps().shouldSee(onMessagePage().selectAllMessagesPopup());
    }
}

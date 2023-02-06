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
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;

import java.io.IOException;

import static com.google.common.collect.ImmutableMap.of;
import static ru.yandex.autotests.innerpochta.rules.ClearAccRule.clearAcc;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.SETTINGS_PARAM_MESSAGES_PER_PAGE;

@Aqua.Test
@Title("Тест на навигацию по письмам в треде")
@Features(FeaturesConst.MESSAGE_LIST)
@Tag(FeaturesConst.MESSAGE_LIST)
@Stories(FeaturesConst.GENERAL)
public class MessageSelectionInThreadTest extends BaseTest {

    private static final String ALL_THREAD_11_MSGS = "Вся переписка11 писем";
    private static final String THREAD = "thread";
    private static final String MORE_LETTERS = "Ещё письма";
    private static final int THREAD_SIZE = 11;

    private static final int MSG_PER_PAGE = 2;

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
        user.apiMessagesSteps().sendThread(lock.firstAcc(), THREAD, THREAD_SIZE);
        user.apiSettingsSteps().callWithListAndParams(
            SETTINGS_PARAM_MESSAGES_PER_PAGE,
            of(SETTINGS_PARAM_MESSAGES_PER_PAGE, MSG_PER_PAGE)
        );
        user.loginSteps().forAcc(lock.firstAcc()).logins();
    }

    @Test
    @Title("Показываем еще письма в треде")
    @TestCaseId("1542")
    public void shouldSeeMoreMessagesInThread() {
        user.messagesSteps().expandsMessagesThread(THREAD)
            .shouldSeeMessageCountInThread(MSG_PER_PAGE);
        user.defaultSteps().shouldContainText(onMessagePage().displayedMessages().loadMoreLink(), MORE_LETTERS)
            .clicksOn(onMessagePage().displayedMessages().loadMoreLink());
        user.messagesSteps().shouldSeeMessageCountInThread(MSG_PER_PAGE + MSG_PER_PAGE);
        user.defaultSteps().shouldSeeThatElementTextEquals(
            onMessagePage().displayedMessages().showAllText(), ALL_THREAD_11_MSGS
        );
    }

    @Test
    @Title("Показать все письма в треде")
    @TestCaseId("1542")
    public void shouldShowAllMessagesInThread() {
        user.messagesSteps().expandsMessagesThread(THREAD)
            .shouldSeeMessageCountInThread(MSG_PER_PAGE);
        user.defaultSteps().clicksOn(onMessagePage().displayedMessages().loadMoreLink())
            .clicksOn(onMessagePage().displayedMessages().showAllLink());
        user.messagesSteps().shouldSeeMessageWithSubjectCount(THREAD, THREAD_SIZE);
    }
}

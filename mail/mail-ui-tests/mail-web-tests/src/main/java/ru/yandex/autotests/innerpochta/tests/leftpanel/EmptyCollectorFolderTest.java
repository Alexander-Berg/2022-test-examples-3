package ru.yandex.autotests.innerpochta.tests.leftpanel;

import io.qameta.allure.junit4.Tag;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.ns.pages.messages.blocks.EmptyFolderBlock;
import ru.yandex.autotests.innerpochta.rules.RestAssuredAuthRule;
import ru.yandex.autotests.innerpochta.rules.SendToStatAfterClassRule;
import ru.yandex.autotests.innerpochta.rules.acclock.AccLockRule;
import ru.yandex.autotests.innerpochta.steps.AllureStepStorage;
import ru.yandex.autotests.innerpochta.tests.BaseTest;
import ru.yandex.autotests.innerpochta.util.FeaturesConst;
import ru.yandex.autotests.innerpochta.util.MailConst;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;

import static ru.yandex.autotests.innerpochta.rules.ClearAccRule.clearAcc;
import static ru.yandex.autotests.innerpochta.util.MailConst.COLLECTOR;

/**
 * @author cosmopanda
 */
@Aqua.Test
@Title("Навигация в пустой папке сборщика")
@Features(FeaturesConst.LP)
@Tag(FeaturesConst.LP)
@Stories(FeaturesConst.COLLECTORS)
public class EmptyCollectorFolderTest extends BaseTest {

    private static final String EMAIL_FOR_COLLECTOR = "ns-collectorforsearch@yandex.ru";
    private static final String COLLECTOR_FOLDER_TITLE = "Письма из ящика «ns-collectorforsearch@yandex.ru»";

    private AccLockRule lock = AccLockRule.use().useTusAccount(COLLECTOR);
    private RestAssuredAuthRule auth = RestAssuredAuthRule.auth(lock);
    private AllureStepStorage user = new AllureStepStorage(webDriverRule, auth);

    @ClassRule
    public static SendToStatAfterClassRule sendToStat = new SendToStatAfterClassRule();

    @Rule
    public RuleChain rules = RuleChain.outerRule(lock)
        .around(auth)
        .around(clearAcc(() -> user));

    @Before
    public void logIn() {
        user.loginSteps().forAcc(lock.firstAcc()).logins();
        user.defaultSteps().clicksOnElementWithText(onMessagePage()
            .collectorsNavigation().collectorsList(), EMAIL_FOR_COLLECTOR);
    }

    @Test
    @Title("Проверяем переход в пустую папку сборщика")
    @TestCaseId("1484")
    public void shouldSeeEmptyCollectorFolder() {
        user.messagesSteps().shouldSeeTitleOfCollectorFolder(COLLECTOR_FOLDER_TITLE)
            .shouldNotSeeMessagesPresent();
        user.defaultSteps().shouldSeeThatElementTextEquals(
            onMessagePage().emptyFolder().textHeader(),
            EmptyFolderBlock.TEXT_HEADER
        )
            .shouldSeeThatElementTextEquals(
                onMessagePage().emptyFolder().textList(),
                EmptyFolderBlock.TEXT
            );
    }

    @Test
    @Title("Проверяем переход в инбокс из пустой папки сборщика")
    @TestCaseId("2487")
    public void shouldNavigateToInbox() {
        user.defaultSteps().clicksOn(onMessagePage().emptyFolder().inboxLink());
        user.leftColumnSteps().shouldBeInFolder(MailConst.INBOX_RU);
    }
}

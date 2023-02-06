package ru.yandex.autotests.innerpochta.tests.settings;

import io.qameta.allure.junit4.Tag;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.data.QuickFragments;
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

import java.util.Arrays;
import java.util.Collection;

import static ru.yandex.autotests.innerpochta.rules.ClearAccRule.clearAcc;

@Aqua.Test
@Title("Создание сборщиков с помощью хэндлеров.")
@Features(FeaturesConst.SETTINGS)
@Tag(FeaturesConst.SETTINGS)
@Stories(FeaturesConst.COLLECTORS)
@RunWith(Parameterized.class)
public class CollectorSettingsStoryCreateCollectorWithHandlersTest extends BaseTest {

    private static final String PASSWORD = "testqa";
    private static final String PASSWORD_OUTLOOK = "pochta1234";

    @Parameterized.Parameter(0)
    public String login;
    @Parameterized.Parameter(1)
    public String pass;
    @Parameterized.Parameter(2)
    public String server;

    @Parameterized.Parameters(name = "Login = {0}")
    public static Collection<Object[]> testData() {
        return Arrays.asList(new Object[][]{
            {"ramblercollector@rambler.ru", PASSWORD, "mail.rambler.ru"},
            {"collectorgmaizl@gmail.com", PASSWORD + PASSWORD, "imap.gmail.com"},
            {"mailrucollector@mail.ru", PASSWORD, "imap.mail.ru"},
            {"mailforcollector@yandex.ru", PASSWORD, "imap.yandex.ru"},
            {"karteric1@outlook.com", PASSWORD_OUTLOOK, "outlook.office365.com"}
        });
    }

    private AccLockRule lock = AccLockRule.use().useTusAccount();
    private RestAssuredAuthRule auth = RestAssuredAuthRule.auth(lock);
    private AllureStepStorage user = new AllureStepStorage(webDriverRule, auth);

    @ClassRule
    public static SendToStatAfterClassRule sendToStat = new SendToStatAfterClassRule();

    @Rule
    public RuleChain rules = RuleChain.outerRule(lock)
        .around(auth)
        .around(clearAcc(() -> user));

    @Test
    @Title("Создаём сборщик и смотрим, появился ли в настройках алиас с нужным ящиком после успешного создания")
    @TestCaseId("5944")
    public void createNewCollectorFromSettingsAndCheckAlias() {
        user.loginSteps().forAcc(lock.firstAcc()).logins(QuickFragments.SETTINGS_COLLECTORS);
        user.apiCollectorSteps().removeAllUserCollectors()
            .createNewCollector(login, pass, server);
        user.defaultSteps().refreshPage();
        user.settingsSteps().shouldSeeNewCollector(login);
    }
}

package ru.yandex.autotests.innerpochta.tests.multiauth;

import io.qameta.allure.junit4.Tag;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.annotations.ConditionalIgnore;
import ru.yandex.autotests.innerpochta.conditions.TicketInProgress;
import ru.yandex.autotests.innerpochta.data.QuickFragments;
import ru.yandex.autotests.innerpochta.rules.SendToStatAfterClassRule;
import ru.yandex.autotests.innerpochta.rules.acclock.AccLockRule;
import ru.yandex.autotests.innerpochta.rules.acclock.UseCreds;
import ru.yandex.autotests.innerpochta.steps.AllureStepStorage;
import ru.yandex.autotests.innerpochta.tests.BaseTest;
import ru.yandex.autotests.innerpochta.util.FeaturesConst;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Issue;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;

import java.util.Collection;

import static java.util.Arrays.asList;

@Aqua.Test
@Title("Редиректы в МА")
@Features(FeaturesConst.MULTI_AUTH)
@Tag(FeaturesConst.MULTI_AUTH)
@Stories(FeaturesConst.GENERAL)
@RunWith(Parameterized.class)
public class MultiAuthRedirectTest extends BaseTest {

    public static final String CREDS = "MultiAuthRedirectTest";
    public static final String CREDS2 = "MultiAuthRedirectTest2";

    private AllureStepStorage user = new AllureStepStorage(webDriverRule);

    @Parameterized.Parameter
    public QuickFragments fragment;

    @Parameterized.Parameters(name = "Fragment = {0}")
    public static Collection<Object[]> getData() {
        return asList(
                new Object[] {QuickFragments.INBOX},
                new Object[] {QuickFragments.SENT},
                new Object[] {QuickFragments.TRASH},
                new Object[] {QuickFragments.SPAM},
                new Object[] {QuickFragments.DRAFT},
                new Object[] {QuickFragments.SETTINGS},
                new Object[] {QuickFragments.CONTACTS},
                new Object[] {QuickFragments.OUTBOX}
        );
    }

    @ClassRule
    public static SendToStatAfterClassRule sendToStat = new SendToStatAfterClassRule();

    @Rule
    public AccLockRule lock = AccLockRule.use().ignoreLock().annotation();

    @Before
    public void login() {
        user.loginSteps().forAcc(lock.acc(CREDS)).logins()
                .multiLoginWith(lock.acc(CREDS2));
    }

    @Test
    @UseCreds({CREDS, CREDS2})
    @TestCaseId("1670")
    @ConditionalIgnore(condition = TicketInProgress.class)
    @Issue("DARIA-68621")
    public void commonRedirectTest() {
        user.defaultSteps().opensFragment(fragment);
        user.loginSteps().changeUserFromShapka(lock.acc(CREDS).getLogin());
        user.defaultSteps().shouldBeOnUrlWith(fragment);
    }
}

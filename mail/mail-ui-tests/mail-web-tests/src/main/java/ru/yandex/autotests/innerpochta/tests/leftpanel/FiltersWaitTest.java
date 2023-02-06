package ru.yandex.autotests.innerpochta.tests.leftpanel;

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

/**
 * author @mabelpines
 */
@Aqua.Test
@Title("Фильтры в левой колонке")
@Features(FeaturesConst.LP)
@Tag(FeaturesConst.LP)
@Stories(FeaturesConst.LEFT_PANEL)
public class FiltersWaitTest extends BaseTest {

    private String subject = Utils.getRandomName();

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
    public void logIn() {
        user.loginSteps().forAcc(lock.firstAcc()).logins(QuickFragments.INBOX);
    }

    @Test
    @Title("Появление в левой колонки фильтра “Жду ответа“")
    @TestCaseId("2075")
    public void shouldSeeWaitFilter() {
        user.composeSteps().prepareDraftFor(lock.firstAcc().getSelfEmail(), subject, "");
        user.defaultSteps().clicksOn(onComposePopup().expandedPopup().notifyBtn())
            .turnTrue(onComposePopup().expandedPopup().notifyPopup().options().get(0))
            .clicksOn(onComposePopup().expandedPopup().sendBtn());
        user.defaultSteps().clicksOn(onMessagePage().checkMailButton())
            .shouldSee(onMessagePage().msgFiltersBlock().waitForAnswer())
            .clicksOn(onMessagePage().msgFiltersBlock().waitForAnswer());
        user.messagesSteps().shouldSeeMessageWithSubject(subject);
    }
}

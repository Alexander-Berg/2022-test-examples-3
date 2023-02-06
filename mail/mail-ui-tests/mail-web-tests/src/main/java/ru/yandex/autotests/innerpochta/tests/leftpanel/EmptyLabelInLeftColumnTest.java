package ru.yandex.autotests.innerpochta.tests.leftpanel;

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

/**
 * * @author yaroslavna
 */
@Aqua.Test
@Title("Тест на отображение пустой метки в левой колонке")
@Features(FeaturesConst.LP)
@Tag(FeaturesConst.LP)
@Stories(FeaturesConst.LEFT_PANEL)

public class EmptyLabelInLeftColumnTest extends BaseTest {

    private static final String LABEL = "empty label";

    private AccLockRule lock = AccLockRule.use().className();
    private RestAssuredAuthRule auth = RestAssuredAuthRule.auth(lock);
    private AllureStepStorage user = new AllureStepStorage(webDriverRule, auth);

    @ClassRule
    public static SendToStatAfterClassRule sendToStat = new SendToStatAfterClassRule();

    @Rule
    public RuleChain rules = RuleChain.outerRule(lock).around(auth);

    @Before
    public void logIn() throws IOException {
        user.loginSteps().forAcc(lock.firstAcc()).logins();
    }

    @Test
    @Title("Показ пустой метки в левой колонке")
    @TestCaseId("1933")
    public void shouldSeeEmptyLabel() {
        user.leftColumnSteps().shouldSeeCustomLabel(LABEL);
    }
}
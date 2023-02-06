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
import static ru.yandex.autotests.innerpochta.util.MailConst.MAIL_COLLECTOR;
import static ru.yandex.autotests.innerpochta.util.MailConst.PASS_COLLECTOR;
import static ru.yandex.autotests.innerpochta.util.MailConst.SERVER_COLLECTOR;
import static ru.yandex.autotests.innerpochta.util.handlers.LabelsConstants.LABELS_PARAM_GREEN_COLOR;

@Aqua.Test
@Title("Тест на добавление метки при настрйке сборщика")
@Features(FeaturesConst.SETTINGS)
@Tag(FeaturesConst.SETTINGS)
@Stories(FeaturesConst.COLLECTORS)
public class CollectorSettingsLabelsTest extends BaseTest {

    private static final int LABEL_INDEX = 1;

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
        user.apiLabelsSteps().addNewLabel(Utils.getRandomString(), LABELS_PARAM_GREEN_COLOR);
        user.apiCollectorSteps().createNewCollector(MAIL_COLLECTOR, PASS_COLLECTOR, SERVER_COLLECTOR);
        user.loginSteps().forAcc(lock.firstAcc()).logins(QuickFragments.SETTINGS_COLLECTORS);
    }

    @Test
    @Title("Проставление метки")
    @TestCaseId("1721")
    public void testLabelAndMoveMessagesFromCollector() throws Exception {
        user.settingsSteps().goesToCollectorConfiguration(0);
        user.defaultSteps().inputsTextInElement(onCollectorSettingsPage().blockSetup().password(), PASS_COLLECTOR)
            .turnTrue(onCollectorSettingsPage().blockSetup().putLabel())
            .clicksOn(onCollectorSettingsPage().blockSetup().selectLabelDropdown());
        String label = onSettingsPage().selectConditionDropdown().conditionsList().get(LABEL_INDEX).getText();
        user.defaultSteps().clicksOn(onSettingsPage().selectConditionDropdown().conditionsList().get(LABEL_INDEX));
        user.settingsSteps().clicksOnSaveChangesButton();
        user.settingsSteps().goesToCollectorConfiguration(0);
        user.defaultSteps().shouldContainText(onCollectorSettingsPage().blockSetup().selectLabelDropdown(), label);
    }
}

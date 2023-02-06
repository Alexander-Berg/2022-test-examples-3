package ru.yandex.autotests.innerpochta.tests.settings;

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
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;

import static ru.yandex.autotests.innerpochta.rules.ClearAccRule.clearAcc;
import static ru.yandex.autotests.innerpochta.util.handlers.LabelsConstants.LABELS_PARAM_GREEN_COLOR;

@Aqua.Test
@Title("Создание/изменение/удаление меток")
@Features(FeaturesConst.SETTINGS)
@Tag(FeaturesConst.SETTINGS)
@Stories(FeaturesConst.FOLDERS_LABELS)
@RunWith(DataProviderRunner.class)
public class FoldersAndLabelsStoryLabelsTest extends BaseTest {

    private static final String LABEL_ONE = "testтест12345!№";
    private static final String LABEL_TWO = ",./;'\\[]<>?:\"_+";
    private static final String LABEL_THREE = "|{}!@#$%^&*()-=";

    private AccLockRule lock = AccLockRule.use().useTusAccount();
    private RestAssuredAuthRule auth = RestAssuredAuthRule.auth(lock);
    private AllureStepStorage user = new AllureStepStorage(webDriverRule, auth);

    @ClassRule
    public static SendToStatAfterClassRule sendToStat = new SendToStatAfterClassRule();

    @Rule
    public RuleChain rules = RuleChain.outerRule(lock).around(auth).around(clearAcc(() -> user));

    @DataProvider
    public static Object[][] testData() {
        return new Object[][]{
            {LABEL_ONE},
            {LABEL_TWO},
            {LABEL_THREE}
        };
    }

    @Before
    public void logIn() {
        user.apiLabelsSteps().addNewLabel(LABEL_ONE, LABELS_PARAM_GREEN_COLOR);
        user.apiLabelsSteps().addNewLabel(LABEL_TWO, LABELS_PARAM_GREEN_COLOR);
        user.apiLabelsSteps().addNewLabel(LABEL_THREE, LABELS_PARAM_GREEN_COLOR);
        user.loginSteps().forAcc(lock.firstAcc()).logins(QuickFragments.SETTINGS_FOLDERS);
        user.settingsSteps().shouldSeeLabelCreated(LABEL_ONE)
            .shouldSeeLabelCreated(LABEL_TWO)
            .shouldSeeLabelCreated(LABEL_THREE);
    }

    @Test
    @Title("Создание новой метки")
    @TestCaseId("1766")
    @UseDataProvider("testData")
    public void testCreateLabel(String labelName) {
        user.defaultSteps().opensFragment(QuickFragments.INBOX);
        user.leftColumnSteps().shouldSeeLabelOnHomePage(labelName);
    }

    @Test
    @Title("Переименование метки")
    @TestCaseId("1767")
    @UseDataProvider("testData")
    public void testRenameLabel(String labelName) {
        user.defaultSteps().opensFragment(QuickFragments.INBOX);
        user.leftColumnSteps().shouldSeeCustomLabelOnMessagePage();
        user.defaultSteps().opensFragment(QuickFragments.SETTINGS_FOLDERS);
        String newLabelName = user.settingsSteps().renameLabelWithName(labelName);
        user.settingsSteps().shouldSeeLabelCreated(newLabelName)
            .shouldSeeLabelsCount(3);
        user.defaultSteps().opensFragment(QuickFragments.INBOX);
        user.leftColumnSteps().shouldSeeLabelOnHomePage(newLabelName)
            .shouldSeeLabelCountOnHomePage(3);
    }

    @Test
    @Title("Удаляем метку")
    @TestCaseId("1768")
    @UseDataProvider("testData")
    public void testDeleteLabel(String labelName) {
        user.defaultSteps().opensFragment(QuickFragments.INBOX);
        user.leftColumnSteps().shouldSeeCustomLabelOnMessagePage();
        user.defaultSteps().opensFragment(QuickFragments.SETTINGS_FOLDERS)
            .clicksOnElementWithText(onFoldersAndLabelsSetup().setupBlock().labels().userLabelsList(), labelName)
            .clicksOn(onFoldersAndLabelsSetup().setupBlock().labels().deleteLabel());
        user.settingsSteps().shouldSeeLabelsCount(2);
        user.defaultSteps().opensFragment(QuickFragments.INBOX);
        user.leftColumnSteps().shouldSeeLabelCountOnHomePage(2);
    }
}

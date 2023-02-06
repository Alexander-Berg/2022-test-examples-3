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
import ru.yandex.autotests.innerpochta.steps.beans.label.Label;
import ru.yandex.autotests.innerpochta.steps.beans.message.Message;
import ru.yandex.autotests.innerpochta.tests.BaseTest;
import ru.yandex.autotests.innerpochta.util.FeaturesConst;
import ru.yandex.autotests.innerpochta.util.Utils;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;

import static ru.yandex.autotests.innerpochta.rules.ClearAccRule.clearAcc;
import static ru.yandex.autotests.innerpochta.util.handlers.LabelsConstants.LABELS_PARAM_GREEN_COLOR;

/**
 * <p> Created by IntelliJ IDEA.
 * <p> User: lanwen
 * <p> Date: 22.05.12
 * <p> Time: 18:48
 */

@Aqua.Test
@Title("Сортировка меток")
@Features(FeaturesConst.SETTINGS)
@Tag(FeaturesConst.SETTINGS)
@Stories(FeaturesConst.FOLDERS_LABELS)
public class FoldersAndLabelsStorySortLabelTest extends BaseTest {

    private static final String LABEL_A = "LabelA";
    private static final String LABEL_B = "LabelB";
    private static final String LABEL_C = "LabelC";
    private static final String LABEL_D = "LabelD";

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
        user.apiLabelsSteps().addNewLabel(LABEL_C, LABELS_PARAM_GREEN_COLOR);
        Label labelA = user.apiLabelsSteps().addNewLabel(LABEL_A, LABELS_PARAM_GREEN_COLOR);
        Label labelB = user.apiLabelsSteps().addNewLabel(LABEL_B, LABELS_PARAM_GREEN_COLOR);
        Label labelD = user.apiLabelsSteps().addNewLabel(LABEL_D, LABELS_PARAM_GREEN_COLOR);
        Message msg1 = user.apiMessagesSteps().sendMailWithNoSave(lock.firstAcc(), Utils.getRandomName(), "");
        Message msg2 = user.apiMessagesSteps().sendMailWithNoSave(lock.firstAcc(), Utils.getRandomName(), "");
        Message msg3 = user.apiMessagesSteps().sendMailWithNoSave(lock.firstAcc(), Utils.getRandomName(), "");
        user.apiLabelsSteps().markWithLabel(msg1, labelD)
            .markWithLabel(msg3, labelD)
            .markWithLabel(msg2, labelD)
            .markWithLabel(msg1, labelA)
            .markWithLabel(msg2, labelA)
            .markWithLabel(msg2, labelB);
        user.loginSteps().forAcc(lock.firstAcc()).logins(QuickFragments.SETTINGS_FOLDERS);
    }

    @Test
    @Title("Сортировка меток по имени")
    @TestCaseId("1770")
    public void testSortLabelsByName() {
        user.defaultSteps().turnTrue(onFoldersAndLabelsSetup().setupBlock().labels().sortByName());
        user.settingsSteps().shouldSeeLabelsSortedByName();
    }

    @Test
    @Title("Сортировка меток по количеству сообщений")
    @TestCaseId("1771")
    public void testSortLabelsByCount() {
        user.defaultSteps().turnTrue(onFoldersAndLabelsSetup().setupBlock().labels().sortByCount());
        user.settingsSteps().shouldSeeLabelsSortedByCount();
    }
}

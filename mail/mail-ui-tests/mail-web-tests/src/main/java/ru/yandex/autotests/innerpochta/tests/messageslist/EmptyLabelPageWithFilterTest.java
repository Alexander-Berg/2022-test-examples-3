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
import ru.yandex.autotests.innerpochta.util.Utils;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;

import java.io.IOException;

import static ru.yandex.autotests.innerpochta.rules.ClearAccRule.clearAcc;
import static ru.yandex.autotests.innerpochta.util.handlers.LabelsConstants.LABELS_PARAM_GREEN_COLOR;

/**
 * Created with IntelliJ IDEA.
 * User: arttimofeev
 * Date: 07.09.12
 * Time: 15:47
 */

@Aqua.Test
@Title("Тест на окно создания фильтра из Inbox для пустой метки")
@Stories(FeaturesConst.LABELS)
@Features(FeaturesConst.MESSAGE_LIST)
@Tag(FeaturesConst.MESSAGE_LIST)
public class EmptyLabelPageWithFilterTest extends BaseTest {

    private static final String CUSTOM_LABEL = "MailWebLabel";

    private String address;
    private String subject;
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
        subject = Utils.getRandomString();
        address = Utils.getRandomString();
        user.apiLabelsSteps().addNewLabel(CUSTOM_LABEL, LABELS_PARAM_GREEN_COLOR);
        user.loginSteps().forAcc(lock.firstAcc()).logins();
    }

    @Test
    @Title("Создание фильтра для пустой метки")
    @TestCaseId("3301")
    public void testCreateFilterForEmptyLabelFromInbox() {
        user.defaultSteps().clicksOnElementWithText(onMessagePage().labelsNavigation().userLabels(), CUSTOM_LABEL)
            .shouldSee(onHomePage().putMarkAutomaticallyButton())
            .clicksOn(onHomePage().putMarkAutomaticallyButton())
            .shouldSee(onFiltersOverview().newFilterPopUp().fromInputBox())
            .inputsTextInElement(onFiltersOverview().newFilterPopUp().fromInputBox(), address)
            .shouldSee(onFiltersOverview().newFilterPopUp().subjectInputBox())
            .inputsTextInElement(onFiltersOverview().newFilterPopUp().subjectInputBox(), subject);
        user.filtersSteps().submitsSimpleFilterFromPopUp();
    }
}

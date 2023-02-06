package ru.yandex.autotests.innerpochta.tests.filters;

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
import ru.yandex.autotests.innerpochta.rules.resources.AddMessageIfNeedRule;
import ru.yandex.autotests.innerpochta.steps.AllureStepStorage;
import ru.yandex.autotests.innerpochta.steps.beans.message.Message;
import ru.yandex.autotests.innerpochta.tests.BaseTest;
import ru.yandex.autotests.innerpochta.util.FeaturesConst;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;

import static ru.yandex.autotests.innerpochta.rules.ClearAccRule.clearAcc;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.INBOX;

/**
 * @author cosmopanda
 */
@Aqua.Test
@Title("Тест на применение фильтра к существующим письмам")
@Stories({FeaturesConst.GENERAL})
@Features(FeaturesConst.FILTERS)
@Tag(FeaturesConst.FILTERS)
public class ApplyFilterToExistingMessagesTest extends BaseTest {

    private AccLockRule lock = AccLockRule.use().useTusAccount();
    private RestAssuredAuthRule auth = RestAssuredAuthRule.auth(lock);
    private AllureStepStorage user = new AllureStepStorage(webDriverRule, auth);
    private AddMessageIfNeedRule addMessageIfNeed = AddMessageIfNeedRule.addMessageIfNeed(() -> user,
        () -> lock.firstAcc());

    private Message msg;

    @ClassRule
    public static SendToStatAfterClassRule sendToStat = new SendToStatAfterClassRule();

    @Rule
    public RuleChain rules = RuleChain.outerRule(lock)
        .around(auth)
        .around(clearAcc(() -> user))
        .around(addMessageIfNeed);

    @Before
    public void setUp() {
        msg = addMessageIfNeed.getFirstMessage();
        user.loginSteps().forAcc(lock.firstAcc()).logins(QuickFragments.SETTINGS_FILTERS_CREATE);
    }

    @Test
    @Title("Проверяем применение фильтра к письмам")
    @TestCaseId("2330")
    public void shouldApplyFilterToMessage() {
        user.defaultSteps().shouldSee(onFiltersCreationPage().setupFiltersCreate()
            .blockCreateConditions().conditionsList().get(0));
        user.filtersSteps().shouldOpenFromDropdown(0);
        user.defaultSteps().clicksOn(onSettingsPage().selectConditionDropdown().conditionsList().get(4))
            .inputsTextInElement(
                onFiltersCreationPage().setupFiltersCreate().blockCreateConditions().conditionsList().get(0)
                    .inputCondition(),
                msg.getSubject()
            );
        String labelName = user.filtersSteps().chooseToPutRandomlyCreatedMark();
        user.defaultSteps().turnTrue(onFiltersCreationPage().setupFiltersCreate().applyFilterCheckBox())
            .clicksOn(onFiltersCreationPage().setupFiltersCreate().submitFilterButton())
            .shouldBeOnUrl(lock.firstAcc(), QuickFragments.SETTINGS_FILTERS)
            .opensFragment(INBOX);
        user.messagesSteps().shouldSeeLabelsOnMessage(labelName, msg.getSubject());
    }
}

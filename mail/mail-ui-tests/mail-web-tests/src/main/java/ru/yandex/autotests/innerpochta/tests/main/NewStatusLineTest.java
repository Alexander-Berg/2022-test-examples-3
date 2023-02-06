package ru.yandex.autotests.innerpochta.tests.main;

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
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;

import java.io.IOException;

import static com.google.common.collect.ImmutableMap.of;
import static org.hamcrest.Matchers.containsString;
import static ru.yandex.autotests.innerpochta.rules.ClearAccRule.clearAcc;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.EMPTY_STR;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.SHOW_FILTER_NOTIFICATION;

@Aqua.Test
@Title("Тест на новую строку статуса")
@Features(FeaturesConst.MAIN)
@Tag(FeaturesConst.MAIN)
@Stories(FeaturesConst.GENERAL)
public class NewStatusLineTest extends BaseTest {

    private static final String USER_FOLDER = "folder";

    private String subject1 = "subj1";

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
        user.apiMessagesSteps().sendMailWithNoSave(lock.firstAcc(), subject1, "");
        user.apiSettingsSteps().callWithListAndParams(
            "Сбрасываем настройку показа нотификации о создании фильтра",
            of(SHOW_FILTER_NOTIFICATION, EMPTY_STR)
        );
        user.loginSteps().forAcc(lock.firstAcc()).logins(QuickFragments.INBOX);
    }

    @Test
    @Title("Создаём фильтр через строку статус лайна")
    @TestCaseId("1548")
    public void testCreateFilterFromNewStatusLine() {
        user.messagesSteps().selectMessageWithSubject(subject1)
            .createsNewFolderFromDropDownMenu(USER_FOLDER)
            .createsFilterFromStatusLine(USER_FOLDER, lock.firstAcc().getSelfEmail());
        user.defaultSteps().shouldBeOnUrl(containsString("#setup/filters-create?id="))
            .shouldContainValue(onFiltersCreationPage().setupFiltersCreate()
                .blockCreateConditions().conditionsList().get(0).inputCondition(), lock.firstAcc().getSelfEmail())
            .shouldSeeCheckBoxesInState(true, onFiltersCreationPage().setupFiltersCreate().blockSelectAction()
                .moveToFolderCheckBox())
            .shouldContainText(onFiltersCreationPage().setupFiltersCreate().blockSelectAction()
                .selectFolderDropdown(), USER_FOLDER);
    }
}

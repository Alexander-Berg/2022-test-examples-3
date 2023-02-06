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
import ru.yandex.autotests.innerpochta.steps.AllureStepStorage;
import ru.yandex.autotests.innerpochta.tests.BaseTest;
import ru.yandex.autotests.innerpochta.util.FeaturesConst;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;

import java.io.IOException;

import static ru.yandex.autotests.innerpochta.rules.ClearAccRule.clearAcc;

/**
 * Created with IntelliJ IDEA.
 * User: arttimofeev
 * Date: 07.09.12
 * Time: 15:47
 */

@Aqua.Test
@Title("Тест на создание фильтра с несколькими действиями")
@Features(FeaturesConst.FILTERS)
@Tag(FeaturesConst.FILTERS)
@Stories(FeaturesConst.GENERAL)
public class FiltersStoryCreateNewFilterWithMultipleActionsTest extends BaseTest {

    private static final String ADDRESS = "sendtotestmail@yandex.ru";

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
    public void logIn(){
        user.loginSteps().forAcc(lock.firstAcc()).logins(QuickFragments.SETTINGS_FILTERS);
        user.filtersSteps().clicksOnCreateNewFilter();
    }

    @Test
    @Title("Тест на создание фильтра с несколькими действиями")
    @TestCaseId("1313")
    public void testCreateFilterWithMultipleActions() {
        String ifCondition = user.filtersSteps().chooseRandomIfConditionForNewFilter();
        user.defaultSteps().turnTrue(onFiltersCreationPage().setupFiltersCreate().blockSelectAction().markAsCheckBox())
            .turnTrue(onFiltersCreationPage().setupFiltersCreate().blockSelectAction().markAsReadCheckBox());
        user.filtersSteps().chooseToNotifyAddress(ADDRESS)
            .replyWithText("text")
            .chooseMoveToFolderAction()
            .submitsFilter(lock.firstAcc())
            .shouldSeeSelectedConditionInFilter(ifCondition)
            .shouldSeeSelectedActionInFilter(
                "— автоматический ответ «text»",
                "— уведомить по адресу «" + ADDRESS + "»",
                "— пометить письмо как прочитанное",
                "— пометить письмо меткой «Важные»",
                "— переместить письмо в папку «Отправленные»"
            );
    }
}

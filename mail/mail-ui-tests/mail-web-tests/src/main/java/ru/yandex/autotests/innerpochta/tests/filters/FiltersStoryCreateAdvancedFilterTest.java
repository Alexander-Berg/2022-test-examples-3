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
 * Date: 19.09.12
 * Time: 19:24
 */
@Aqua.Test
@Title("Тест на создание фильтра с большим количеством условий")
@Stories(FeaturesConst.GENERAL)
@Features(FeaturesConst.FILTERS)
@Tag(FeaturesConst.FILTERS)
public class FiltersStoryCreateAdvancedFilterTest extends BaseTest {

    private static final String ACTION = "— переместить письмо в папку «Отправленные»";
    private static final String CONDITION_PATTERN = "Если\nзаголовок «sender» не содержит «text7»\n" +
        "%s\n«Название вложения» не содержит «text6»\n" +
        "%s\n«Тело письма» содержит «text5»\n" +
        "%s\n«Тема» содержит «text4»\n" +
        "%s\n«Копия» не совпадает c «text3»\n" +
        "%s\n«Кому» не совпадает c «text2»\n" +
        "%s\n«Кому или копия» совпадает c «text1»\n" +
        "%s\n«От кого» совпадает c «text0»";

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
        user.loginSteps().forAcc(lock.firstAcc()).logins(QuickFragments.SETTINGS_FILTERS);
    }

    @Test
    @Title("Тест на создание фильтра с большим количеством условий «ИЛИ»")
    @TestCaseId("1300")
    public void testCreateAdvancedFilterWithOrLogic() {
        user.defaultSteps().shouldSee(onFiltersOverview().createNewFilterButton())
            .clicksOn(onFiltersOverview().createNewFilterButton());
        user.filtersSteps().createsAdvancedConditionsForFilter()
            .selectsLogicOfFilter(0)
            .submitsFilter(lock.firstAcc())
            .shouldSeeSelectedConditionInFilter(
                String.format(CONDITION_PATTERN, "или", "или", "или", "или", "или", "или", "или")
            )
            .shouldSeeSelectedActionInFilter(ACTION);
    }

    @Test
    @Title("Тест на создание фильтра с большим количеством условий «И»")
    @TestCaseId("1299")
    public void testCreateAdvancedFilterWithAndLogic() {
        user.defaultSteps().shouldSee(onFiltersOverview().createNewFilterButton())
            .clicksOn(onFiltersOverview().createNewFilterButton());
        user.filtersSteps().createsAdvancedConditionsForFilter()
            .selectsLogicOfFilter(1)
            .submitsFilter(lock.firstAcc())
            .shouldSeeSelectedConditionInFilter(
                String.format(CONDITION_PATTERN, "и", "и", "и", "и", "и", "и", "и")
            )
            .shouldSeeSelectedActionInFilter(ACTION);
    }
}

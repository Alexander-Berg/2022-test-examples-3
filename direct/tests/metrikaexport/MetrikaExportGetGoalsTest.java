package ru.yandex.autotests.direct.intapi.java.tests.metrikaexport;

import java.util.List;

import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.intapi.java.core.DirectRule;
import ru.yandex.autotests.direct.intapi.java.features.TestFeatures;
import ru.yandex.autotests.direct.intapi.java.features.tags.Tags;
import ru.yandex.autotests.direct.utils.tags.TagDictionary;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Issue;
import ru.yandex.qatools.allure.annotations.Stories;

import static org.hamcrest.Matchers.greaterThan;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;

@Aqua.Test
@Description("metrika-export/retargeting-goals")
@Stories(TestFeatures.Metrika.GOALS)
@Features(TestFeatures.METRIKA)
@Tag(Tags.METRIKA)
@Tag(TagDictionary.TRUNK)
@Issue("DIRECT-91807")
public class MetrikaExportGetGoalsTest {

    public static final Long GOAL_DIVISOR = 500_000L;

    @Rule
    public DirectRule directRule = DirectRule.defaultRule();

    private List<Long> results;

    @Before
    public void setUp() {
        results = directRule.intapiSteps().metrikaExportControllerSteps().getRetargetingGoals(GOAL_DIVISOR, 0L);
    }

    @Test
    public void getGoalsReturnsNotEmptyList() {
        assertThat("Получили не пустой список целей", results, Matchers.hasSize(greaterThan(0)));
    }

    @Test
    public void eachGoalIsCorrect() {
        assertThat("Все id целей корректные", results, Matchers.everyItem(greaterThan(0L)));
    }
}

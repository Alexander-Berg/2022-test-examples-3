package ru.yandex.autotests.direct.intapi.java.tests.metrika;

import java.util.List;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import org.hamcrest.TypeSafeDiagnosingMatcher;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.intapi.java.core.DirectRule;
import ru.yandex.autotests.direct.intapi.java.features.TestFeatures;
import ru.yandex.autotests.direct.intapi.java.features.tags.Tags;
import ru.yandex.autotests.direct.intapi.models.MetrikaCountersResult;
import ru.yandex.autotests.direct.utils.tags.TagDictionary;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Issue;
import ru.yandex.qatools.allure.annotations.Stories;

import static org.hamcrest.Matchers.greaterThan;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;

@Aqua.Test
@ru.yandex.qatools.allure.annotations.Description("metrika/metrika-counters")
@Stories(TestFeatures.Metrika.COUNTERS)
@Features(TestFeatures.METRIKA)
@Tag(Tags.METRIKA)
@Tag(TagDictionary.TRUNK)
@Tag("DIRECT-64729")
@Issue("DIRECT-64729")
public class MetrikaGetCountersTest {

    public static final Long COUNTER_DIVISOR = 500_000L;

    @Rule
    public DirectRule directRule = DirectRule.defaultRule();

    private List<MetrikaCountersResult> results;

    private Matcher<MetrikaCountersResult> counterCorrectMatcher =
            new TypeSafeDiagnosingMatcher<MetrikaCountersResult>() {
                @Override
                protected boolean matchesSafely(MetrikaCountersResult item, Description mismatchDescription) {
                    if (item == null) {
                        return false;
                    }
                    Matcher<Long> idMatcher = greaterThan(0L);
                    return idMatcher.matches(item.getOrderId()) &&
                            idMatcher.matches(item.getCampaignId()) &&
                            idMatcher.matches(item.getCounterId());
                }

                @Override
                public void describeTo(Description description) {
                    description.appendText("correct counter");
                }
            };

    @Before
    public void setUp() {
        results = directRule.intapiSteps().metrikaControllerSteps().getCampCounters(COUNTER_DIVISOR, 0L);
    }

    @Test
    public void getCountersReturnsNotEmptyList() {
        assertThat("Получили не пустой список счетчиков", results, Matchers.hasSize(greaterThan(0)));
    }

    @Test
    public void eachGoalIsCorrect() {
        assertThat("Все счетчики корректные", results, Matchers.everyItem(counterCorrectMatcher));
    }
}

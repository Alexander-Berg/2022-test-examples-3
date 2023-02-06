package ru.yandex.market.api.opinion;

import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;
import ru.yandex.market.api.domain.v2.opinion.ModelOpinionV2;
import ru.yandex.market.api.integration.UnitTestBase;
import ru.yandex.market.api.matchers.FactMatcher;
import ru.yandex.market.api.util.ResourceHelpers;

import java.util.Date;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

public class ModelOpinionV2JsonParserTest extends UnitTestBase {

    private ModelOpinionV2JsonParser parser;

    @Before
    public void setUp() throws Exception {
        parser = new ModelOpinionV2JsonParser();
    }

    @Test
    public void testParserOpinion() {
        ModelOpinionV2 opinion = parse("model-opinion.json");

        assertEquals(1, opinion.getId());
        assertEquals(10495456L, (long)opinion.getModel().getId());

        assertEquals("Текст отзыва", opinion.getText());
        assertEquals("Тест плюсы", opinion.getPros());
        assertEquals("Тест минусы", opinion.getCons());

        assertEquals(new Date(946670461000L), opinion.getDate());

        assertEquals(5, opinion.getAgreeCount());
        assertEquals(1, opinion.getDisagreeCount());

        assertEquals(4, (int) opinion.getGrade());

        assertEquals(UsageTime.FEW_WEEKS, opinion.getUsageTime());

        assertEquals(359587186, opinion.getAuthorUid());

        assertEquals(143, (int) opinion.getRegionId());

        assertEquals(Visibility.NAME, opinion.getVisibility());

        assertEquals(true, opinion.getVerifiedBuyer());
    }

    @Test
    public void testSetHiddenVisibilityIfNotSpecified(){
        ModelOpinionV2 opinion = parse("model-opinion-without-visibility.json");
        assertEquals(Visibility.HIDDEN, opinion.getVisibility());
    }

    @Test
    public void testEmptyId(){
        ModelOpinionV2 opinion = parse("model-opinion-without-id.json");
        assertEquals(0, opinion.getId());
    }

    @Test
    public void testFactors() {
        ModelOpinionV2 opinion = parse("model-opinion-with-facts.json");
        assertThat(
            opinion.getFacts(),
            Matchers.containsInAnyOrder(
                FactMatcher.facts(
                    FactMatcher.id(742),
                    FactMatcher.value(5)
                ),
                FactMatcher.facts(
                    FactMatcher.id(743),
                    FactMatcher.value(3)
                )
            )
        );
    }

    private ModelOpinionV2 parse(String filename) {
        return parser.parse(ResourceHelpers.getResource(filename));
    }
}

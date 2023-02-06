package ru.yandex.market.api.opinion;

import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;
import ru.yandex.market.api.domain.v2.opinion.ShopOpinionV2;
import ru.yandex.market.api.integration.UnitTestBase;
import ru.yandex.market.api.matchers.FactMatcher;
import ru.yandex.market.api.util.ResourceHelpers;

import static org.junit.Assert.*;

public class ShopOpinionV2JsonParserTest extends UnitTestBase {

    private ShopOpinionV2JsonParser parser;

    @Before
    public void setUp() throws Exception {
        parser = new ShopOpinionV2JsonParser();
    }

    @Test
    public void testShopInfoParsing(){
        ShopOpinionV2 opinion = parser.parse(ResourceHelpers.getResource("shop-opinion.json"));

        assertEquals(3828, opinion.getShopId());
        assertEquals("21817163", opinion.getShopOrderId());
        assertEquals(Delivery.INSTORE, opinion.getDelivery());
        assertEquals(188214432L, opinion.getAuthorUid());
        assertEquals("opinion body", opinion.getText());
        assertEquals(Visibility.HIDDEN, opinion.getVisibility());
        assertEquals(Problem.UNRESOLVED, opinion.getProblem());
    }

    @Test
    public void testParseShopWithoutProblem() {
        ShopOpinionV2 opinion = parse("shop-opinion2.json");
        assertNull(opinion.getProblem());
    }

    @Test
    public void testParseShopOpinionWithResolvedProblem() {
        ShopOpinionV2 opinion = parse("shop-opinion3.json");

        assertEquals(Problem.RESOLVED, opinion.getProblem());
    }

    @Test
    public void testParseShopOpinionWithInvalidDelivery() {
        ShopOpinionV2 opinion = parse("shop-opinion3.json");
        assertNull(opinion.getDelivery());
    }

    @Test
    public void testModerationState() {
        ShopOpinionV2 opinion = parse("shop-opinion-with-moderation-state.json");
        assertEquals(ModerationState.APPROVED.name(), opinion.getState());
    }

    @Test
    public void testFactors() {
        ShopOpinionV2 opinion = parse("shop-opinion-with-facts.json");
        assertThat(
            opinion.getFacts(),
            Matchers.containsInAnyOrder(
                FactMatcher.shopFacts(
                    FactMatcher.id(4),
                    FactMatcher.value(5)
                ),
                FactMatcher.shopFacts(
                    FactMatcher.id(5),
                    FactMatcher.value(2)
                )
            )
        );
    }

    private ShopOpinionV2 parse(String filename) {
        return parser.parse(ResourceHelpers.getResource(filename));
    }
}

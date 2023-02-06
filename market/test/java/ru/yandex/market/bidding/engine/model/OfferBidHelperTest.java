package ru.yandex.market.bidding.engine.model;

import java.util.ArrayList;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import ru.yandex.market.bidding.model.GoalPlace;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class OfferBidHelperTest {

    private static Integer zeroIfNull(Integer i) {
        return i == null ? 0 : i;
    }

    @Before
    public void setUp() throws Exception {

    }

    @After
    public void tearDown() throws Exception {

    }

    @Test
    public void test_decodeGoalPlace_should_createGoalWithLimitedMaxValues_when_passedFlagsIsHuge() throws Exception {
        GoalPlace goalPlace = OfferBidHelper.decodeGoalPlace(0xFFFFFFFF);
        assertEquals(OfferBidHelper.GOAL_MASK, goalPlace.card.intValue());
        assertEquals(OfferBidHelper.GOAL_MASK, goalPlace.parallel.intValue());
        assertEquals(OfferBidHelper.GOAL_MASK, goalPlace.cardCpa.intValue());
    }

    @Test
    public void test_encodeGoalPlace_should_resetGoalFlagsBits_when_passedGoalIsNull() throws Exception {
        for (int i = 0; i < OfferBidHelper.ALL_GOALS_MASK; i++) {
            short bits = (short) OfferBidHelper.encodeGoalPlace(i, null);
            assertNull(OfferBidHelper.decodeGoalPlace(bits));
        }
    }

    @Test
    public void testEncodeDecodeGoalPlace() throws Exception {

        List<GoalPlace> combinations = new ArrayList<>();
        for (int i = 0; i <= OfferBidHelper.GOAL_MASK; i++) {
            for (int j = 0; j <= OfferBidHelper.GOAL_MASK; j++) {
                for (int k = 0; k <= OfferBidHelper.GOAL_MASK; k++) {
                    combinations.add(new GoalPlace(i, j, k));
                }
            }
        }

        for (GoalPlace place : combinations) {
            for (int i = 0; i <= OfferBidHelper.ALL_GOALS_MASK; i++) {
                short bits = (short) OfferBidHelper.encodeGoalPlace(i, place);
                GoalPlace actual = OfferBidHelper.decodeGoalPlace(bits);
                if (actual == null) {
                    actual = new GoalPlace(0, 0, 0);
                }
                assertEquals(i + ":" + place.parallel + ":" + place.card + ":" + place.cardCpa, place.parallel, zeroIfNull(actual.parallel));
                assertEquals(i + ":" + place.parallel + ":" + place.card + ":" + place.cardCpa, place.card, zeroIfNull(actual.card));
                assertEquals(i + ":" + place.parallel + ":" + place.card + ":" + place.cardCpa, place.cardCpa, zeroIfNull(actual.cardCpa));

            }
        }
    }
}
package ru.yandex.travel.hotels.common.partners.travelline;

import java.io.IOException;
import java.nio.charset.Charset;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.io.Resources;
import org.junit.Test;

import ru.yandex.travel.hotels.common.partners.base.exceptions.PartnerException;
import ru.yandex.travel.hotels.common.partners.travelline.model.Currency;
import ru.yandex.travel.hotels.common.partners.travelline.model.Placement;
import ru.yandex.travel.hotels.common.partners.travelline.model.RoomStay;
import ru.yandex.travel.hotels.common.refunds.RefundRule;
import ru.yandex.travel.hotels.common.refunds.RefundRules;
import ru.yandex.travel.hotels.common.refunds.RefundType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

public class TravellineRefundRulesBuilderTests {

    private static final ObjectMapper mapper = DefaultTravellineClient.createObjectMapper();

    @Test
    public void testSingleFullPenalty() throws IOException {
        RoomStay roomStay = loadRoomStay("singleFullUnlimitedPenalty");
        RoomStay.RatePlanRef ratePlan = roomStay.getRatePlans().get(0);
        int totalPrice = 4000;
        RefundRules rules = TravellineRefundRulesBuilder.build(ratePlan.getCancelPenaltyGroup(), null,
                ratePlan.getCode(), roomStay.getStayDates(), ZoneOffset.UTC, totalPrice);

        assertThat(rules.isRefundable()).isFalse();
        List<RefundRule> rulesList = rules.getRules();
        assertThat(rulesList.size()).isEqualTo(1);
        assertThat(rulesList.get(0).getType()).isEqualTo(RefundType.NON_REFUNDABLE);
        assertThat(rulesList.get(0).getStartsAt()).isNull();
        assertThat(rulesList.get(0).getEndsAt()).isNull();
    }

    @Test
    public void testSinglePenalty() throws IOException {
        RoomStay roomStay = loadRoomStay("singleUnlimitedPenalty");
        RoomStay.RatePlanRef ratePlan = roomStay.getRatePlans().get(0);
        int totalPrice = 4000;
        RefundRules rules = TravellineRefundRulesBuilder.build(ratePlan.getCancelPenaltyGroup(), null,
                ratePlan.getCode(), roomStay.getStayDates(), ZoneOffset.UTC, totalPrice);

        assertThat(rules.isRefundable()).isTrue();
        List<RefundRule> list = rules.getRules();
        assertThat(list.size()).isEqualTo(2);
        assertThat(list.get(0).getType()).isEqualTo(RefundType.REFUNDABLE_WITH_PENALTY);
        assertThat(list.get(0).getStartsAt()).isNull();
        assertThat(list.get(0).getEndsAt()).isEqualTo(Instant.parse("2025-01-12T12:30:00Z"));
        assertThat(list.get(0).getPenalty().getNumberStripped()).isEqualByComparingTo("2000.0");
        assertThat(list.get(0).getPenalty().getCurrency().getCurrencyCode()).isEqualTo(Currency.RUB.getValue());

        assertThat(list.get(1).getType()).isEqualTo(RefundType.NON_REFUNDABLE);
        assertThat(list.get(1).getStartsAt()).isEqualTo(Instant.parse("2025-01-12T12:30:00Z"));
        assertThat(list.get(1).getEndsAt()).isNull();
    }

    @Test
    public void testNightsPenalty() throws IOException {
        RoomStay roomStay = loadRoomStay("nightsPenalty");
        List<Placement> placements = List.of(Placement.builder()
                .rates(roomStay.getPlacementRates().get(0).getRates())
                .build());
        RoomStay.RatePlanRef ratePlan = roomStay.getRatePlans().get(0);
        int totalPrice = 6000;
        RefundRules rules = TravellineRefundRulesBuilder.build(ratePlan.getCancelPenaltyGroup(),
                placements, ratePlan.getCode(), roomStay.getStayDates(), ZoneOffset.UTC, totalPrice);

        assertThat(rules.isRefundable()).isTrue();
        List<RefundRule> list = rules.getRules();
        assertThat(list.size()).isEqualTo(4);

        assertThat(list.get(0).getType()).isEqualTo(RefundType.FULLY_REFUNDABLE);
        assertThat(list.get(0).getStartsAt()).isNull();
        assertThat(list.get(0).getEndsAt()).isEqualTo(Instant.parse("2025-01-05T14:00:00Z"));

        assertThat(list.get(1).getType()).isEqualTo(RefundType.REFUNDABLE_WITH_PENALTY);
        assertThat(list.get(1).getStartsAt()).isEqualTo(Instant.parse("2025-01-05T14:00:00Z"));
        assertThat(list.get(1).getEndsAt()).isEqualTo(Instant.parse("2025-01-11T14:00:00Z"));
        assertThat(list.get(1).getPenalty().getNumberStripped()).isEqualByComparingTo("3000.0");
        assertThat(list.get(1).getPenalty().getCurrency().getCurrencyCode()).isEqualTo(Currency.RUB.getValue());

        assertThat(list.get(2).getType()).isEqualTo(RefundType.REFUNDABLE_WITH_PENALTY);
        assertThat(list.get(2).getStartsAt()).isEqualTo("2025-01-11T14:00:00Z");
        assertThat(list.get(2).getEndsAt()).isEqualTo("2025-01-12T12:30:00Z");
        assertThat(list.get(2).getPenalty().getNumberStripped()).isEqualByComparingTo("3800.0");
        assertThat(list.get(2).getPenalty().getCurrency().getCurrencyCode()).isEqualTo(Currency.RUB.getValue());

        assertThat(list.get(3).getType()).isEqualTo(RefundType.NON_REFUNDABLE);
        assertThat(list.get(3).getStartsAt()).isEqualTo("2025-01-12T12:30:00Z");
        assertThat(list.get(3).getEndsAt()).isNull();
    }

    @Test
    public void testFirstNightPenalty() throws IOException {
        RoomStay roomStay = loadRoomStay("firstNightPenalty");
        List<Placement> placements = List.of(Placement.builder()
                .rates(roomStay.getPlacementRates().get(0).getRates())
                .build());
        RoomStay.RatePlanRef ratePlan = roomStay.getRatePlans().get(0);
        int totalPrice = 6000;
        RefundRules rules = TravellineRefundRulesBuilder.build(ratePlan.getCancelPenaltyGroup(),
               placements, ratePlan.getCode(), roomStay.getStayDates(), ZoneOffset.UTC, totalPrice);

        assertThat(rules.isRefundable()).isTrue();
        List<RefundRule> list = rules.getRules();
        assertThat(list.size()).isEqualTo(4);

        assertThat(list.get(0).getType()).isEqualTo(RefundType.FULLY_REFUNDABLE);
        assertThat(list.get(0).getStartsAt()).isNull();
        assertThat(list.get(0).getEndsAt()).isEqualTo("2025-01-05T14:00:00Z");

        assertThat(list.get(1).getType()).isEqualTo(RefundType.REFUNDABLE_WITH_PENALTY);
        assertThat(list.get(1).getStartsAt()).isEqualTo("2025-01-05T14:00:00Z");
        assertThat(list.get(1).getEndsAt()).isEqualTo("2025-01-11T14:00:00Z");
        assertThat(list.get(1).getPenalty().getNumberStripped()).isEqualByComparingTo("3000.0");
        assertThat(list.get(1).getPenalty().getCurrency().getCurrencyCode()).isEqualTo(Currency.RUB.getValue());

        assertThat(list.get(2).getType()).isEqualTo(RefundType.REFUNDABLE_WITH_PENALTY);
        assertThat(list.get(2).getStartsAt()).isEqualTo("2025-01-11T14:00:00Z");
        assertThat(list.get(2).getEndsAt()).isEqualTo("2025-01-12T12:30:00Z");
        assertThat(list.get(2).getPenalty().getNumberStripped()).isEqualByComparingTo("3200.00");
        assertThat(list.get(2).getPenalty().getCurrency().getCurrencyCode()).isEqualTo(Currency.RUB.getValue());

        assertThat(list.get(3).getType()).isEqualTo(RefundType.NON_REFUNDABLE);
        assertThat(list.get(3).getStartsAt()).isEqualTo("2025-01-12T12:30:00Z");
        assertThat(list.get(3).getEndsAt()).isNull();
    }

    @Test
    public void testDoubleSomePenalty() throws IOException {
        RoomStay roomStay = loadRoomStay("doubleSomePenalty");
        RoomStay.RatePlanRef ratePlan = roomStay.getRatePlans().get(0);
        int totalPrice = 4000;
        RefundRules rules = TravellineRefundRulesBuilder.build(ratePlan.getCancelPenaltyGroup(), null,
                ratePlan.getCode(), roomStay.getStayDates(), ZoneOffset.UTC, totalPrice);

        assertThat(rules.isRefundable()).isTrue();
        List<RefundRule> list = rules.getRules();
        assertThat(list.size()).isEqualTo(3);

        assertThat(list.get(0).getType()).isEqualTo(RefundType.FULLY_REFUNDABLE);
        assertThat(list.get(0).getStartsAt()).isNull();
        assertThat(list.get(0).getEndsAt()).isEqualTo(Instant.parse("2025-01-05T14:00:00Z"));

        assertThat(list.get(1).getType()).isEqualTo(RefundType.REFUNDABLE_WITH_PENALTY);
        assertThat(list.get(1).getStartsAt()).isEqualTo(Instant.parse("2025-01-05T14:00:00Z"));
        assertThat(list.get(1).getEndsAt()).isEqualTo(Instant.parse("2025-01-12T12:30:00Z"));
        assertThat(list.get(1).getPenalty().getNumberStripped()).isEqualByComparingTo("2000");
        assertThat(list.get(1).getPenalty().getCurrency().getCurrencyCode()).isEqualTo(Currency.RUB.getValue());

        assertThat(list.get(2).getType()).isEqualTo(RefundType.NON_REFUNDABLE);
        assertThat(list.get(2).getStartsAt()).isEqualTo(Instant.parse("2025-01-12T12:30:00Z"));
        assertThat(list.get(2).getEndsAt()).isNull();
    }

    @Test
    public void testPenaltyAfterStartDate() throws IOException {
        RoomStay roomStay = loadRoomStay("penaltyAfterStartDate");
        RoomStay.RatePlanRef ratePlan = roomStay.getRatePlans().get(0);
        int totalPrice = 4000;
        RefundRules rules = TravellineRefundRulesBuilder.build(ratePlan.getCancelPenaltyGroup(), null,
                ratePlan.getCode(), roomStay.getStayDates(), ZoneOffset.UTC, totalPrice);

        assertThat(rules.isRefundable()).isTrue();
        List<RefundRule> penalties = rules.getRules();
        assertThat(penalties.size()).isEqualTo(2);
        assertThat(penalties.get(0).getType()).isEqualTo(RefundType.FULLY_REFUNDABLE);
        assertThat(penalties.get(0).getStartsAt()).isNull();
        assertThat(penalties.get(0).getEndsAt()).isEqualTo("2025-01-10T14:00:00Z");

        assertThat(penalties.get(1).getType()).isEqualTo(RefundType.NON_REFUNDABLE);
        assertThat(penalties.get(1).getStartsAt()).isEqualTo("2025-01-10T14:00:00Z");
        assertThat(penalties.get(1).getEndsAt()).isNull();
    }

    @Test
    public void testZeroPenalty() throws IOException {
        RoomStay roomStay = loadRoomStay("zeroPenalty");
        RoomStay.RatePlanRef ratePlan = roomStay.getRatePlans().get(0);
        int totalPrice = 4000;
        RefundRules rules = TravellineRefundRulesBuilder.build(ratePlan.getCancelPenaltyGroup(), null,
                ratePlan.getCode(), roomStay.getStayDates(), ZoneOffset.UTC, totalPrice);

        assertThat(rules.isRefundable()).isTrue();
        List<RefundRule> list = rules.getRules();
        assertThat(list.size()).isEqualTo(2);
        assertThat(list.get(0).getType()).isEqualTo(RefundType.FULLY_REFUNDABLE);
        assertThat(list.get(0).getStartsAt()).isNull();
        assertThat(list.get(0).getEndsAt()).isEqualTo("2025-01-12T14:00:00Z");

        assertThat(list.get(1).getType()).isEqualTo(RefundType.NON_REFUNDABLE);
        assertThat(list.get(1).getStartsAt()).isEqualTo("2025-01-12T14:00:00Z");
        assertThat(list.get(1).getEndsAt()).isNull();
    }

    @Test
    public void testSingleLimitedPenalty() throws IOException {
        RoomStay roomStay = loadRoomStay("singleLimitedPenalty");
        RoomStay.RatePlanRef ratePlan = roomStay.getRatePlans().get(0);
        int totalPrice = 4000;
        RefundRules rules = TravellineRefundRulesBuilder.build(ratePlan.getCancelPenaltyGroup(), null,
                ratePlan.getCode(), roomStay.getStayDates(), ZoneOffset.UTC, totalPrice);

        assertThat(rules.isRefundable()).isTrue();
        List<RefundRule> list = rules.getRules();
        assertThat(list.size()).isEqualTo(3);
        assertThat(list.get(0).getType()).isEqualTo(RefundType.FULLY_REFUNDABLE);
        assertThat(list.get(0).getStartsAt()).isNull();
        assertThat(list.get(0).getEndsAt()).isEqualTo("2025-01-11T19:00:00Z");

        assertThat(list.get(1).getType()).isEqualTo(RefundType.REFUNDABLE_WITH_PENALTY);
        assertThat(list.get(1).getStartsAt()).isEqualTo("2025-01-11T19:00:00Z");
        assertThat(list.get(1).getEndsAt()).isEqualTo("2025-01-12T14:00:00Z");
        assertThat(list.get(1).getPenalty().getNumberStripped()).isEqualByComparingTo("2000");
        assertThat(list.get(1).getPenalty().getCurrency().getCurrencyCode()).isEqualTo(Currency.RUB.getValue());

        assertThat(list.get(2).getType()).isEqualTo(RefundType.NON_REFUNDABLE);
        assertThat(list.get(2).getStartsAt()).isEqualTo("2025-01-12T14:00:00Z");
        assertThat(list.get(2).getEndsAt()).isNull();
        assertThat(list.get(2).getPenalty()).isNull();
    }

    @Test
    public void testPenaltyInFuture() throws IOException {
        RoomStay roomStay = loadRoomStay("penaltyInFuture");
        RoomStay.RatePlanRef ratePlan = roomStay.getRatePlans().get(0);
        int totalPrice = 4000;
        RefundRules rules = TravellineRefundRulesBuilder.build(ratePlan.getCancelPenaltyGroup(), null,
                ratePlan.getCode(), roomStay.getStayDates(), ZoneOffset.UTC, totalPrice);

        assertThat(rules.isRefundable()).isTrue();
        List<RefundRule> penalties = rules.getRules();
        assertThat(penalties.size()).isEqualTo(3);
        assertThat(penalties.get(0).getType()).isEqualTo(RefundType.FULLY_REFUNDABLE);
        assertThat(penalties.get(0).getStartsAt()).isNull();
        assertThat(penalties.get(0).getEndsAt()).isEqualTo("2025-01-05T14:00:00Z");

        assertThat(penalties.get(1).getType()).isEqualTo(RefundType.REFUNDABLE_WITH_PENALTY);
        assertThat(penalties.get(1).getStartsAt()).isEqualTo("2025-01-05T14:00:00Z");
        assertThat(penalties.get(1).getEndsAt()).isEqualTo("2025-01-10T14:00:00Z");
        assertThat(penalties.get(1).getPenalty().getNumberStripped()).isEqualByComparingTo("1000");
        assertThat(penalties.get(1).getPenalty().getCurrency().getCurrencyCode()).isEqualTo(Currency.RUB.getValue());

        assertThat(penalties.get(2).getType()).isEqualTo(RefundType.NON_REFUNDABLE);
        assertThat(penalties.get(2).getStartsAt()).isEqualTo("2025-01-10T14:00:00Z");
        assertThat(penalties.get(2).getEndsAt()).isNull();
        assertThat(penalties.get(2).getPenalty()).isNull();
    }

    @Test
    public void testPenaltiesWithGap() throws IOException {
        RoomStay roomStay = loadRoomStay("penaltiesWithGap");
        RoomStay.RatePlanRef ratePlan = roomStay.getRatePlans().get(0);
        int totalPrice = 4000;
        try {
            TravellineRefundRulesBuilder.build(ratePlan.getCancelPenaltyGroup(), null, ratePlan.getCode(),
                    roomStay.getStayDates(), ZoneOffset.UTC, totalPrice);
            fail("Helper method should have failed");
        } catch (Exception e) {
            assertThat(e.getClass()).isEqualTo(PartnerException.class);
            assertThat(e.getMessage()).isEqualTo("Rate plan 208490: Gap between intervals found from " +
                    "2025-01-10T14:00:00Z" +
                    " till 2025-01-11T14:00:00Z");
        }
    }

    @Test
    public void testOverlappingPenalties() throws IOException {
        RoomStay roomStay = loadRoomStay("overlappingPenalties");
        RoomStay.RatePlanRef ratePlan = roomStay.getRatePlans().get(0);
        int totalPrice = 4000;
        try {
            TravellineRefundRulesBuilder.build(ratePlan.getCancelPenaltyGroup(), null, ratePlan.getCode(),
                    roomStay.getStayDates(), ZoneOffset.UTC, totalPrice);
            fail("Helper method should have failed");
        } catch (Exception e) {
            assertThat(e.getClass()).isEqualTo(PartnerException.class);
            assertThat(e.getMessage()).isEqualTo("Rate plan 208490: Intervals overlap from 2025-01-10T14:00:00Z till " +
                    "2025-01-11T14:00:00Z");
        }
    }

    @Test
    public void testTwoEmptyStarts() throws IOException {
        RoomStay roomStay = loadRoomStay("twoEmptyStarts");
        RoomStay.RatePlanRef ratePlan = roomStay.getRatePlans().get(0);
        int totalPrice = 4000;
        try {
            TravellineRefundRulesBuilder.build(ratePlan.getCancelPenaltyGroup(), null, ratePlan.getCode(),
                    roomStay.getStayDates(), ZoneOffset.UTC, totalPrice);
            fail("Helper method should have failed");
        } catch (Exception e) {
            assertThat(e.getClass()).isEqualTo(PartnerException.class);
            assertThat(e.getMessage()).isEqualTo("Rate plan 208490: More than 1 interval with empty start are found");
        }
    }

    @Test
    public void testTwoEmptyEnds() throws IOException {
        RoomStay roomStay = loadRoomStay("twoEmptyEnds");
        RoomStay.RatePlanRef ratePlan = roomStay.getRatePlans().get(0);
        int totalPrice = 4000;
        try {
            TravellineRefundRulesBuilder.build(ratePlan.getCancelPenaltyGroup(), null, ratePlan.getCode(),
                    roomStay.getStayDates(), ZoneOffset.UTC, totalPrice);
            fail("Helper method should have failed");
        } catch (Exception e) {
            assertThat(e.getClass()).isEqualTo(PartnerException.class);
            assertThat(e.getMessage()).isEqualTo("Rate plan 208490: More than 1 interval with empty end are found");
        }
    }

    @Test
    public void testWrongPriceOrder() throws IOException {
        RoomStay roomStay = loadRoomStay("wrongPriceOrder");
        RoomStay.RatePlanRef ratePlan = roomStay.getRatePlans().get(0);
        int totalPrice = 4000;
        try {
            TravellineRefundRulesBuilder.build(ratePlan.getCancelPenaltyGroup(), null, ratePlan.getCode(),
                    roomStay.getStayDates(), ZoneOffset.UTC, totalPrice);
            fail("Helper method should have failed");
        } catch (Exception e) {
            assertThat(e.getClass()).isEqualTo(PartnerException.class);
            assertThat(e.getMessage()).isEqualTo("Rate plan 208490: previous penalty amount 4000 is greater then next" +
                    " penalty amount 3600.0");
        }
    }

    @Test
    public void testGapAfterLastPenalty() throws IOException {
        RoomStay roomStay = loadRoomStay("gapAfterLastPenalty");
        RoomStay.RatePlanRef ratePlan = roomStay.getRatePlans().get(0);
        int totalPrice = 4000;
        try {
            RefundRules rules = TravellineRefundRulesBuilder.build(ratePlan.getCancelPenaltyGroup(), null,
                    ratePlan.getCode(), roomStay.getStayDates(), ZoneOffset.UTC, totalPrice);
            fail("Helper method should have failed");
        } catch (Exception e) {
            assertThat(e.getClass()).isEqualTo(PartnerException.class);
            assertThat(e.getMessage()).isEqualTo("Rate plan 208490: Gap between intervals found from " +
                    "2025-01-11T14:00:00Z" +
                    " till 2025-01-12T14:00:00Z");
        }
    }

    @Test
    public void testRuleEndMatchesCheckin() throws IOException {
        RoomStay roomStay = loadRoomStay("rightBorderMatches");
        RoomStay.RatePlanRef ratePlan = roomStay.getRatePlans().get(0);
        int totalPrice = 9000;
        RefundRules rules = TravellineRefundRulesBuilder.build(ratePlan.getCancelPenaltyGroup(), null,
                ratePlan.getCode(), roomStay.getStayDates(), ZoneOffset.UTC, totalPrice);
        assertThat(rules.getRules()).hasSize(2);
        assertThat(rules.getRules().get(0).getType()).isEqualTo(RefundType.FULLY_REFUNDABLE);
        assertThat(rules.getRules().get(1).getType()).isEqualTo(RefundType.NON_REFUNDABLE);
    }

    private RoomStay loadRoomStay(String resourceName) throws IOException {
        if (!resourceName.endsWith(".json")) {
            resourceName += ".json";
        }
        String data = Resources.toString(Resources.getResource("travelline/" + resourceName), Charset.defaultCharset());
        return mapper.readerFor(RoomStay.class).readValue(data);
    }
}

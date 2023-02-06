package ru.yandex.travel.hotels.common.partners.booking;

import java.io.IOException;
import java.nio.charset.Charset;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.io.Resources;
import org.junit.Test;

import ru.yandex.travel.hotels.common.partners.booking.model.Block;
import ru.yandex.travel.hotels.common.partners.booking.utils.BookingRefundRulesBuilder;
import ru.yandex.travel.hotels.common.refunds.RefundRule;
import ru.yandex.travel.hotels.common.refunds.RefundRules;
import ru.yandex.travel.hotels.common.refunds.RefundType;

import static org.assertj.core.api.Assertions.assertThat;

public class BookingRefundRulesBuilderTests {

    private static final ObjectMapper mapper = DefaultBookingClient.createObjectMapper();
    private static final LocalDateTime checkInDate = LocalDate.of(2020, 9, 11).atStartOfDay();
    private static final LocalDateTime checkOutDate = LocalDate.of(2020, 9, 12).atStartOfDay();

    @Test
    public void testNoPenalties() throws IOException {
        Block block = loadBlock("noCancellations");
        RefundRules rules = BookingRefundRulesBuilder.build(block, checkInDate, checkOutDate, 4000);
        assertThat(rules.isRefundable()).isFalse();
        List<RefundRule> rulesList = rules.getRules();
        assertThat(rulesList.size()).isEqualTo(0);
    }

    @Test
    public void testRealCancellationInfo() throws IOException {
        Block block = loadBlock("realCancellationInfo");
        RefundRules rules = BookingRefundRulesBuilder.build(block, checkInDate, checkOutDate, 4000);

        assertThat(rules.isRefundable()).isTrue();
        List<RefundRule> rulesList = rules.getRules();
        assertThat(rulesList.size()).isEqualTo(2);
        assertThat(rulesList.get(0).getType()).isEqualTo(RefundType.FULLY_REFUNDABLE);
        assertThat(rulesList.get(0).getStartsAt()).isNull();
        assertThat(rulesList.get(0).getEndsAt()).isEqualTo(Instant.parse("2020-09-10T14:00:00Z"));

        assertThat(rulesList.get(1).getType()).isEqualTo(RefundType.REFUNDABLE_WITH_PENALTY);
        assertThat(rulesList.get(1).getPenalty().getNumberStripped()).isEqualByComparingTo("3000.0");
        assertThat(rulesList.get(1).getStartsAt()).isEqualTo(Instant.parse("2020-09-10T14:00:00Z"));
        assertThat(rulesList.get(1).getEndsAt()).isNull();
    }

    @Test
    public void testPartialCancellation() throws IOException {
        Block block = loadBlock("partialCancellation");
        RefundRules rules = BookingRefundRulesBuilder.build(block, checkInDate, checkOutDate, 4000);

        assertThat(rules.isRefundable()).isTrue();
        List<RefundRule> rulesList = rules.getRules();
        assertThat(rulesList.size()).isEqualTo(3);
        assertThat(rulesList.get(0).getType()).isEqualTo(RefundType.FULLY_REFUNDABLE);
        assertThat(rulesList.get(0).getStartsAt()).isNull();
        assertThat(rulesList.get(0).getEndsAt()).isEqualTo(Instant.parse("2020-09-08T22:00:00Z"));

        assertThat(rulesList.get(1).getType()).isEqualTo(RefundType.REFUNDABLE_WITH_PENALTY);
        assertThat(rulesList.get(1).getStartsAt()).isEqualTo(Instant.parse("2020-09-08T22:00:00Z"));
        assertThat(rulesList.get(1).getEndsAt()).isEqualTo(Instant.parse("2020-09-10T14:00:00Z"));
        assertThat(rulesList.get(1).getPenalty().getNumberStripped()).isEqualByComparingTo("1000.0");

        assertThat(rulesList.get(2).getType()).isEqualTo(RefundType.REFUNDABLE_WITH_PENALTY);
        assertThat(rulesList.get(2).getPenalty().getNumberStripped()).isEqualByComparingTo("3000.0");
        assertThat(rulesList.get(2).getStartsAt()).isEqualTo(Instant.parse("2020-09-10T14:00:00Z"));
        assertThat(rulesList.get(2).getEndsAt()).isNull();
    }

    @Test
    public void testPartialAndNoCancellation() throws IOException {
        Block block = loadBlock("partialAndNoCancellation");
        RefundRules rules = BookingRefundRulesBuilder.build(block, checkInDate, checkOutDate, 4000);

        assertThat(rules.isRefundable()).isTrue();
        List<RefundRule> rulesList = rules.getRules();
        assertThat(rulesList.size()).isEqualTo(3);
        assertThat(rulesList.get(0).getType()).isEqualTo(RefundType.FULLY_REFUNDABLE);
        assertThat(rulesList.get(0).getStartsAt()).isNull();
        assertThat(rulesList.get(0).getEndsAt()).isEqualTo(Instant.parse("2020-09-08T22:00:00Z"));

        assertThat(rulesList.get(1).getType()).isEqualTo(RefundType.REFUNDABLE_WITH_PENALTY);
        assertThat(rulesList.get(1).getStartsAt()).isEqualTo(Instant.parse("2020-09-08T22:00:00Z"));
        assertThat(rulesList.get(1).getEndsAt()).isEqualTo(Instant.parse("2020-09-10T14:00:00Z"));
        assertThat(rulesList.get(1).getPenalty().getNumberStripped()).isEqualByComparingTo("1000.0");

        assertThat(rulesList.get(2).getType()).isEqualTo(RefundType.NON_REFUNDABLE);
        assertThat(rulesList.get(2).getStartsAt()).isEqualTo(Instant.parse("2020-09-10T14:00:00Z"));
        assertThat(rulesList.get(2).getEndsAt()).isNull();
    }

    private Block loadBlock(String resourceName) throws IOException {
        if (!resourceName.endsWith(".json")) {
            resourceName += ".json";
        }
        String data = Resources.toString(Resources.getResource("booking/" + resourceName), Charset.defaultCharset());
        return mapper.readerFor(Block.class).readValue(data);
    }
}

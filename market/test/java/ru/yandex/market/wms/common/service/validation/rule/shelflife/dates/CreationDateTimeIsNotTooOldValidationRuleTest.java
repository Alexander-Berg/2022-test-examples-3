package ru.yandex.market.wms.common.service.validation.rule.shelflife.dates;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ru.yandex.market.wms.common.pojo.ShelfLifeDates;

import static org.junit.jupiter.api.Assertions.assertThrows;

public class CreationDateTimeIsNotTooOldValidationRuleTest {

    private CreationDateTimeIsNotTooOldValidationRule rule;

    @BeforeEach
    public void init() {
        Clock clock = Clock.fixed(Instant.parse("2020-04-18T12:00:00.000Z"), ZoneOffset.UTC);
        rule = new CreationDateTimeIsNotTooOldValidationRule(clock);
    }

    @Test
    public void correctIfShelfLifeDatesIsNotSpecified() {
        rule.validate(null);
    }

    @Test
    public void correctIfCreationDateIsNotSpecified() {
        rule.validate(new ShelfLifeDates(null, Instant.parse("2020-04-19T12:00:00.000Z")));
    }

    @Test
    public void correctIfExpirationDateIsNotSpecified() {
        rule.validate(new ShelfLifeDates(Instant.parse("2020-04-19T12:00:00.000Z"), null));
    }

    @Test
    public void correctIfBothDatesAreNotSpecified() {
        rule.validate(new ShelfLifeDates(null, null));
    }

    @Test
    public void correctIfCreationDateIsNotTooOld() {
        rule.validate(new ShelfLifeDates(Instant.parse("2020-04-17T12:00:00.000Z"),
                Instant.parse("2020-04-19T12:00:00.000Z")));
    }

    @Test
    public void incorrectIfCreationDateIsTooOld() {
        assertThrows(Exception.class, () ->
                rule.validate(new ShelfLifeDates(Instant.parse("1999-04-19T12:00:00.000Z"),
                        Instant.parse("2000-04-20T12:00:00.000Z"))));
    }
}

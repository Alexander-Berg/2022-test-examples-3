package ru.yandex.market.wms.common.service.validation.rule.shelflife.dates;

import java.time.Instant;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ru.yandex.market.wms.common.pojo.ShelfLifeDates;

import static org.junit.jupiter.api.Assertions.assertThrows;

public class ShelfLifeIsNotTooBigValidationRuleTest {

    private ShelfLifeIsNotTooBigValidationRule rule;

    @BeforeEach
    public void init() {
        rule = new ShelfLifeIsNotTooBigValidationRule();
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
    public void correctIfShelfLifeIsNotTooBig() {
        rule.validate(new ShelfLifeDates(Instant.parse("2020-04-17T12:00:00.000Z"),
                Instant.parse("2020-04-19T12:00:00.000Z")));
    }

    @Test
    public void incorrectIfShelfLifeIsTooBig() {
        assertThrows(Exception.class, () ->
                rule.validate(new ShelfLifeDates(Instant.parse("2020-04-19T12:00:00.000Z"),
                        Instant.parse("2030-04-20T12:00:00.000Z"))));
    }
}

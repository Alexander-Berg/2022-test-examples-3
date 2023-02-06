package ru.yandex.market.logistic.gateway.service.converter.fulfillment;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.junit.Before;
import org.junit.Test;

import ru.yandex.market.logistic.gateway.BaseTest;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.DateTime;

public class DateTimeConverterTest extends BaseTest {
    private DateTimeConverter dateTimeConverter;

    @Before
    public void setUp() {
        dateTimeConverter = new DateTimeConverter();
    }

    @Test
    public void toLocalDate() {
        LocalDateTime expected = LocalDateTime.MAX;
        DateTime dateTime = DateTime.fromLocalDateTime(expected);
        LocalDate result = dateTimeConverter.convertToLocalDate(dateTime);
        assertions.assertThat(result).isEqualTo(expected.toLocalDate());
    }
}

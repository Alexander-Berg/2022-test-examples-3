package ru.yandex.market.api.partner.view;

import java.time.LocalDate;

import org.junit.Assert;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.equalTo;

/**
 * @author zoom
 */
public class ResponseFormatterTest {

    @Test
    public void shouldFormatLocalDate() {
        Assert.assertThat(ResponseFormatter.DATE.format(LocalDate.of(2016, 5, 6)), equalTo("06-05-2016"));
    }
}
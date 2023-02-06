package ru.yandex.travel.orders.services;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.travel.orders.services.NotificationHelper.normalizeFreeFormatPhone;

public class NotificationHelperTest {
    @Test
    public void testFormatMoney() {
        var m = BigDecimal.valueOf(100120, 2);
        var s = NotificationHelper.formatMoney(m);
        assertThat(s).isEqualTo("1001.20");

        m = BigDecimal.valueOf(200100, 2);
        s = NotificationHelper.formatMoney(m);
        assertThat(s).isEqualTo("2001");

        m = BigDecimal.valueOf(3001234, 3);
        s = NotificationHelper.formatMoney(m);
        assertThat(s).isEqualTo("3001.23");

        m = BigDecimal.valueOf(40011, 1);
        s = NotificationHelper.formatMoney(m);
        assertThat(s).isEqualTo("4001.10");
    }

    @Test
    public void testHumanDate() {
        int currentYear = LocalDateTime.now().getYear();
        LocalDateTime dt = LocalDateTime.of(currentYear, 5, 8, 10, 15);
        String s = NotificationHelper.humanDate(dt);
        assertThat(s).isEqualTo("08 мая");

        int nextYear = currentYear + 1;
        dt = LocalDateTime.of(nextYear, 5, 8, 10, 15);
        s = NotificationHelper.humanDate(dt);
        assertThat(s).isEqualTo("08 мая " + nextYear);
    }

    @Test
    public void testNormalizeFreeFormatPhone() {
        // simple
        assertThat(normalizeFreeFormatPhone("tel.: +007 495 739-70-00")).isEqualTo("+0074957397000");
        assertThat(normalizeFreeFormatPhone("tel.: +007 495 739, ext. 1234")).isEqualTo("+0074957391234");
        assertThat(normalizeFreeFormatPhone("тел.: +7 495 739-3 доб. 321")).isEqualTo("+74957393321");
        assertThat(normalizeFreeFormatPhone("тел.: 8 495 739-37-77")).isEqualTo("84957393777");
        assertThat(normalizeFreeFormatPhone("195 739 доб 37-77")).isEqualTo("1957393777");
        assertThat(normalizeFreeFormatPhone("195 739 доб 37-77")).isEqualTo("1957393777");
        assertThat(normalizeFreeFormatPhone("+7+495+739+37+77")).isEqualTo("+74957393777");
        // double phone
        assertThat(normalizeFreeFormatPhone("+7 4957393777;+74957391234")).isEqualTo("+74957393777");
        assertThat(normalizeFreeFormatPhone("84957393777:+74957391234")).isEqualTo("84957393777");
        assertThat(normalizeFreeFormatPhone("5-23-25; +7 495 739 12 34")).isEqualTo("+74957391234");
        assertThat(normalizeFreeFormatPhone("8 495 739-37-77, 8 495 739 12 34")).isEqualTo("84957393777");
        assertThat(normalizeFreeFormatPhone("+74957393777 84957391234")).isEqualTo("+74957393777");
        assertThat(normalizeFreeFormatPhone(" +374957393777/+74957391234")).isEqualTo("+374957393777");
        assertThat(normalizeFreeFormatPhone("+74957393777--+74957391234")).isEqualTo("+74957393777");
        assertThat(normalizeFreeFormatPhone(" +74957393777.84957391234")).isEqualTo("+74957393777");
    }
}

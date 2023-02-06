package ru.yandex.market.checkout.pushapi.client.util;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ru.yandex.market.checkout.checkouter.jackson.CheckouterDateFormats;
import ru.yandex.market.checkout.pushapi.client.util.test.XmlTestUtil;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class CheckoutDateFormatTest {

    private CheckoutDateFormat checkoutDateFormat = new CheckoutDateFormat();
    private String dateStr;
    private Date date;

    @BeforeEach
    public void setUp() throws Exception {
        dateStr = "20-05-2013";
        date = XmlTestUtil.date("2013-05-20");
    }

    @Test
    public void testReturnsNullIfNullableAndDateStrIsNull() throws Exception {
        assertNull(checkoutDateFormat.parse(null, true));
    }

    @Test
    public void testThrowsRuntimeExceptionIfNotNullableAndDateStrIsNull() {
        Assertions.assertThrows(RuntimeException.class, () -> {
            checkoutDateFormat.parse(null, false);
        });
    }

    @Test
    public void testReturnsParsedDate() throws Exception {
        assertEquals(date, checkoutDateFormat.parse(dateStr, false));
        assertEquals(date, checkoutDateFormat.parse(dateStr, true));
    }

    @Test
    public void testThrowsRuntimeExceptionIfDateStrsDoesntMatchExpectedFormat() {
        Assertions.assertThrows(RuntimeException.class, () -> {
            checkoutDateFormat.parse("2013-05-20", false);
        });
    }

    @Test
    public void testCreateShortDate() throws ParseException {
        Date longDate = checkoutDateFormat.createLongDateFormat().parse("12-12-2021 08:16:45");
        SimpleDateFormat dateFormat = new SimpleDateFormat(CheckouterDateFormats.DATE_FORMAT);
        assertEquals(dateFormat.parse("12-12-2021") , checkoutDateFormat.createShortDate(longDate));
    }
}

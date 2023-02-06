package ru.yandex.market.checkout.pushapi.client.util.test;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public final class PushApiTestHelper {

    private PushApiTestHelper() {
        throw new UnsupportedOperationException();
    }

    public static Date createLongDate(String dateStr) {
        try {
            return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(dateStr);
        } catch (ParseException ex) {
            throw new RuntimeException(ex.getMessage(), ex);
        }
    }
}

package ru.yandex.autotests.direct.httpclient.data;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;

/**
 * @author Roman Kuhta (kuhtich@yandex-team.ru)
 */
public class CSRFToken {
    public static final String KEY = "csrf_token";
    public static final CSRFToken EMPTY = new CSRFToken("");
    private final String value;


    public CSRFToken(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public NameValuePair asPair() {
        return new BasicNameValuePair(KEY, getValue());
    }

    @Override
    public String toString() {
        return value;
    }
}

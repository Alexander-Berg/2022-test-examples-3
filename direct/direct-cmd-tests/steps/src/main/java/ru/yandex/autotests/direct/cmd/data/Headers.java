package ru.yandex.autotests.direct.cmd.data;

import org.apache.http.Header;
import org.apache.http.message.BasicHeader;

public class Headers {

    public static final Header ACCEPT_JSON_HEADER =
            new BasicHeader("Accept","application/json, text/javascript, */*; q=0.01");

    public static final Header X_REQUESTED_WITH_HEADER =
            new BasicHeader("X-Requested-With","XMLHttpRequest");
}

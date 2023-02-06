package ru.yandex.autotests.direct.httpclient.data;

import org.apache.http.Header;
import org.apache.http.message.BasicHeader;

/**
 * Created by shmykov on 03.10.14.
 */
public class Headers {

    public static final Header ACCEPT_JSON_HEADER =
            new BasicHeader("Accept","application/json, text/javascript, */*; q=0.01");
}

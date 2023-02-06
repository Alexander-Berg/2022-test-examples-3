package ru.yandex.autotests.direct.httpclient.core;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.StatusLine;
import ru.yandex.autotests.direct.httpclient.data.CSRFToken;
import ru.yandex.autotests.httpclient.lite.core.BackEndResponse;

import java.util.regex.Pattern;

/**
 * @author Roman Kuhta (kuhtich@yandex-team.ru)
 */
public class DirectResponse extends BackEndResponse {
    private static CSRFToken lastToken = null;

    public DirectResponse(StatusLine statusLine, HttpEntity httpEntity, Header[] headers) {
        super(statusLine, httpEntity, headers);
    }

    public CSRFToken getCSRFToken() {
        Pattern pattern = Pattern.compile("csrf_token([= :'\"&]|(%3D)|(quot;))+.+?[ \"'&]");
        java.util.regex.Matcher matcher = pattern.matcher(getResponseContent().asString());
        if (matcher.find()) {
            return new CSRFToken(matcher.group().replace("csrf_token", "").replaceAll("([= :'\"&]|(%3D)|(quot;))?", ""));
        }
        return CSRFToken.EMPTY;
    }

    public static CSRFToken getLastToken() {
        return lastToken;
    }

    public static void setLastToken(CSRFToken token) {
        lastToken = token;
    }
}

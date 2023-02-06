package ru.yandex.autotests.direct.web.util.testinterfaces.helpers;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.util.List;

import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;

import ru.yandex.autotests.direct.web.util.DirectWebError;

/**
 * Created with IntelliJ IDEA.
 * User: buhter
 * Date: 13.05.13
 * Time: 14:58
 * Класс для работы с параметрами урлов
 */
public class URLParametersHelper {
    public static String getUrlParameterByName(String url, String parameterName){
        try {
            List<NameValuePair> args = URLEncodedUtils.parse(new URI(url),
                    Charset.defaultCharset().displayName());
            for (NameValuePair arg:args){
                if (arg.getName().equals(parameterName)){
                    return arg.getValue();
                }
            }
            throw new DirectWebError("Couldn't find parameter '" + parameterName + "' in url '" + url + "'");
        } catch (URISyntaxException e) {
            throw new DirectWebError("Got URISyntaxException while retrieving parameter '" + parameterName +
                    "' in url '" + url + "'");
        }

    }
}

package ru.yandex.autotests.direct.cmd.steps.base;

import org.apache.http.Consts;
import org.apache.http.Header;
import org.apache.http.HttpHeaders;
import org.apache.http.NameValuePair;
import org.apache.http.StatusLine;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.util.Asserts;
import org.json.JSONException;
import org.json.JSONObject;

import ru.yandex.autotests.direct.cmd.data.redirect.RedirectResponse;
import ru.yandex.autotests.httpclientlite.HttpClientLiteParserException;
import ru.yandex.autotests.httpclientlite.core.Response;
import ru.yandex.autotests.httpclientlite.core.ResponseContent;
import ru.yandex.autotests.httpclientlite.core.response.AbstractResponseParser;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public class RedirectParser extends AbstractResponseParser {

    private static final String CAMPAIGN = "campaign";

    private static Header findLocationHeader(Response response) {
        String headerName = HttpHeaders.LOCATION;
        Header header = findHeader(response, headerName);
        Asserts.notNull(header, headerName + " header");
        return header;
    }

    private static Header findHeader(Response response, String headerName) {
        for (Header header : response.getHeaders()) {
            if (headerName.equals(header.getName())) {
                return header;
            }
        }
        return null;
    }

    private static Map<String, String> pairsToMap(List<NameValuePair> pairs) {
        Map<String, String> map = new HashMap<>();
        for (NameValuePair pair : pairs) {
            map.put(pair.getName(), pair.getValue());
        }
        return map;
    }

    public static Map<String, String> getUrlParams(String url) {
        return pairsToMap(URLEncodedUtils.parse(url, Consts.UTF_8, '?', '&', '#'));
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T actualParse(Response response, Class<T> contentClass) {

        ResponseContent content = response.getResponseContent();

        try {
            checkResponseCodeInner(response.getStatusLine());
        } catch (HttpClientLiteParserException e) {
            String error = e.getMessage();
            try {
                JSONObject obj = new JSONObject(content.asString());

                if (obj.has(CAMPAIGN) && obj.getJSONObject(CAMPAIGN).has("error")) {
                    error = obj.getJSONObject(CAMPAIGN).get("error") + " (" + error + ")";
                } else {
                    error = "Error: " + error;
                }
            } catch (JSONException ignore) {
            }

            throw new HttpClientLiteParserException(error);
        }

        Header header = findLocationHeader(response);
        Map<String, String> params = getUrlParams(header.getValue());
        return (T) new RedirectResponse(header.getValue(), params, content);
    }

    @Override
    protected List<Class<?>> getSupportedReturnTypes() {
        return Collections.singletonList(RedirectResponse.class);
    }

    @Override
    protected Pattern getStatusCodePattern() {
        return Pattern.compile("3\\d{2}");
    }

    @Override
    protected void checkResponseCode(StatusLine statusLine) {
        // пусто, проверим код ответа позже
    }
}

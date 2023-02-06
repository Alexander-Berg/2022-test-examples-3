package ru.yandex.market.api.listener.expectations;

import org.apache.http.NameValuePair;
import ru.yandex.market.api.util.ApiCollections;
import ru.yandex.market.api.util.Urls;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author dimkarp93
 */
public class HttpRequestUtils {
    public static Map<String, List<String>> getParametersFromUri(URI uri) {

        Map<String, List<String>> result = new HashMap<>();
        for (NameValuePair pair : Urls.builder(uri).getQueryParams()) {
            String name = pair.getName();
            String value = pair.getValue();
            appendParameter(result, name, value);
        }
        return result;
    }

    private static void appendParameter(Map<String, List<String>> result, String name, String value) {
        List<String> values = result.get(name);
        if (ApiCollections.isEmpty(values)) {
            result.put(name, new ArrayList<>());
        }
        values = result.get(name);
        values.add(value);
    }
}


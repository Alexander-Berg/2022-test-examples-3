package ru.yandex.market.api.util.httpclient.spi;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import com.google.common.base.MoreObjects;
import com.google.common.collect.Iterables;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URIBuilder;

import ru.yandex.market.api.util.Urls;

/**
 * Вспомогательные методы
 */
public class MockHelper {


    /**
     * Делает последовательность параметров в url стабильным (в одинаковом порядке)
     *
     * @param url
     * @param ignoredParams
     * @return
     */
    public static String normalizeUrl(String url, Collection<String> ignoredParams) {
        URIBuilder builder = Urls.builder(url);

        List<NameValuePair> params = builder.getQueryParams();

        Iterables.removeIf(params, param -> ignoredParams.contains(param.getName()));

        Collections.sort(params, (o1, o2) -> {
            if (o1 == o2) {
                return 0;
            }
            int v = o1.getName().compareTo(o2.getName());
            if (0 != v) {
                return v;
            }

            return o1.getValue().compareTo(o2.getValue());
        });

        if (params.isEmpty()) {
            builder.clearParameters();
        } else {
            builder.setParameters(params);
        }

        return Urls.toString(builder);
    }

    public static String key(String method,
                             String md5,
                             String testName,
                             String url,
                             Collection<String> ignoreParamNames,
                             Integer invocationNumber) {
        return method + ":" +
            MoreObjects.firstNonNull(md5, "") + ":" +
            MoreObjects.firstNonNull(testName, "") + ":" +
            MoreObjects.firstNonNull(invocationNumber, "") + ":" +
            normalizeUrl(url, ignoreParamNames);
    }
}

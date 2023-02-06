package ru.yandex.market.api.listener.domain;


import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import ru.yandex.market.api.util.ApiCollections;

import java.util.Collection;
import java.util.Map;

/**
 * @author dimkarp93
 */
public class HttpHeaders {
    private final Multimap<String, String> headers;

    public HttpHeaders() {
        this(null);
    }

    public HttpHeaders(Multimap<String, String> headers) {
        if (null != headers) {
            this.headers = headers;
        } else {
            this.headers = ArrayListMultimap.create();
        }
    }

    private Collection<String> get(String name, boolean ignoreCase) {
        if (!ignoreCase) {
            return headers.get(name);
        }

        for (Map.Entry<String, Collection<String>> header: headers.asMap().entrySet()) {
            String headerName = header.getKey();
            Collection<String> headerValues = header.getValue();
            if (headerName.equalsIgnoreCase(name)) {
                return headerValues;
            }
        }

        return null;
    }

    public String get(String name) {
        Collection<String> found = get(name, true);
        return ApiCollections.isEmpty(found) ? null : found.iterator().next();
    }


    //TODO нужен для совместимости сигнатур
    @Deprecated
    public void add(String name, String value) {
        headers.put(name, value);
    }

    public void put(String name, String value) {
        headers.put(name, value);
    }

    //TODO нужен для совместимости сигнатур
    @Deprecated
    public void setAll(HttpHeaders other) {
        headers.putAll(other.headers);
    }

    public void putAll(HttpHeaders other) {
        headers.putAll(other.headers);
    }

    public boolean contains(String name, String value, boolean ignoreCase) {
        Collection<String> found = get(name, ignoreCase);
        if (ApiCollections.isEmpty(found)) {
            return false;
        }

        if (!ignoreCase) {
            return found.contains(value);
        }

        return found.stream().anyMatch(value::equalsIgnoreCase);
    }

    public int size() {
        return headers.size();
    }

    public Collection<Map.Entry<String, String>> entries() {
        return headers.entries();
    }

    public static HttpHeaders combine(HttpHeaders a, HttpHeaders b) {
        HttpHeaders result = new HttpHeaders(a.headers);
        result.setAll(b);
        return result;
    }

}

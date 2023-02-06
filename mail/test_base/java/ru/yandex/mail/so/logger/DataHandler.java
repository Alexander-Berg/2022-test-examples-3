package ru.yandex.mail.so.logger;

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.UnaryOperator;

import org.apache.http.HttpEntity;
import org.apache.http.HttpStatus;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.FileEntity;
import org.apache.http.entity.StringEntity;

import ru.yandex.http.test.HttpResource;
import ru.yandex.http.test.StaticHttpResource;
import ru.yandex.parser.uri.UriParser;
import ru.yandex.test.util.StringChecker;

public interface DataHandler {

    void add(final String uri, final HttpResource resource);

    Map<String, HttpResource> data();

    default UnaryOperator<String> uriPreprocessor() {
        return uri -> new UriParser(uri).toString();
    }

    default String findNearestUri(final String inputUri) {
        String uri = uriPreprocessor().apply(inputUri);
        String minDiff = null;

        if (data().size() <= 0) {
            return null;
        }
        for (String registered : data().keySet()) {
            String diff = StringChecker.compare(registered, uri);
            if (diff == null) {
                continue;
            }
            if (minDiff == null || diff.length() < minDiff.length()) {
                minDiff = diff;
            }
        }
        return minDiff;
    }

    default HttpResource findResource(final String inputUri) {
        String uri = uriPreprocessor().apply(inputUri);
        HttpResource res = data().get(uri);
        if (res == null) {
            for (Map.Entry<String, HttpResource> entry : data().entrySet()) {
                String key = entry.getKey();
                if (key.charAt(key.length() - 1) == '*') {
                    key = key.substring(0, key.length() - 1);
                    if (uri.startsWith(key)) {
                        res = entry.getValue();
                        break;
                    }
                }
            }
        }
        return res;
    }

    default void add(final String uri, final int status) {
        add(uri, status, (HttpEntity) null);
    }

    default void add(final String uri, final String body) {
        add(uri, HttpStatus.SC_OK, body);
    }

    default void add(final String uri, final String body, final ContentType contentType) {
        add(uri, new StringEntity(body, contentType));
    }

    default void add(final String uri, final File file) {
        add(uri, file, SpLogger.TEXT_PLAIN);
    }

    default void add(final String uri, final File file, final ContentType contentType) {
        add(uri, new FileEntity(file, contentType));
    }

    default void add(final String uri, final HttpEntity entity) {
        add(uri, HttpStatus.SC_OK, entity);
    }

    default void add(final String uri, final int statusCode, final HttpEntity entity) {
        add(uri, new StaticHttpResource(statusCode, entity));
    }

    default void add(final String uri, final int statusCode, final String body) {
        add(uri, statusCode, new StringEntity(body, SpLogger.TEXT_PLAIN));
    }

    @SuppressWarnings("unused")
    default int accessCount(final String uri) {
        HttpResource res = findResource(uri);
        if (res == null) {
            return 0;
        } else {
            return res.accessCount();
        }
    }

    @SuppressWarnings("unused")
    default List<Throwable> exceptions(final String uri) {
        HttpResource res = findResource(uri);
        if (res == null) {
            return Collections.emptyList();
        } else {
            return res.exceptions();
        }
    }
}

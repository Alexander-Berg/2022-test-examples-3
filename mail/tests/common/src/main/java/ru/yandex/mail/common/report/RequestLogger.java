package ru.yandex.mail.common.report;

import ch.lambdaj.function.convert.StringConverter;
import io.restassured.http.Header;
import io.restassured.response.Response;
import io.restassured.specification.FilterableRequestSpecification;

import static java.lang.String.format;
import static ru.yandex.autotests.plugins.testpers.html.common.HtmlUtils.bold;

abstract class RequestLogger {
    abstract String log(FilterableRequestSpecification requestSpec, Response response, String responseBody);

    static StringConverter<Header> headers() {
        return from -> format("%s: %s", bold(from.getName()), from.getValue().replaceAll("\"", "{q}"));
    }

    static StringConverter<Header> headersToElliptics() {
        return from -> format("%s: %s", from.getName(), from.getValue());
    }
}

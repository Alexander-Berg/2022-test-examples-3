package ru.yandex.mail.common.report;

import io.restassured.response.Response;
import io.restassured.specification.FilterableRequestSpecification;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import static ch.lambdaj.collection.LambdaCollections.with;
import static com.google.common.base.Joiner.on;
import static java.lang.String.format;
import static org.apache.commons.lang3.StringUtils.isEmpty;
import static ru.yandex.autotests.innerpochta.IfThenElseHelper.ifCond;
import static ru.yandex.autotests.innerpochta.html.RAHeader.convertMapToString;

class LocalLogger extends RequestLogger {
    @Override
    public String log(FilterableRequestSpecification requestSpec, Response response, String responseBody) {
        String href = pathToFile(requestSpec, new ArrayList<Pair<String, String>>(){{
            add(new ImmutablePair<>("REQUEST:", requestSpec.getURI()));
            add(new ImmutablePair<>("HEADERS:", on("\n").join(with(requestSpec.getHeaders()).convert(headersToElliptics()))));
        }});

        String hrefResp = pathToFile(requestSpec, new ArrayList<Pair<String, String>>(){{
            add(new ImmutablePair<>("RESPONSE:", responseBody));
            add(new ImmutablePair<>("REQPATH:", format("%s %s", requestSpec.getMethod(), requestSpec.getURI())));
            add(new ImmutablePair<>("STATUS:", response.statusLine()));
            add(new ImmutablePair<>("REQ HEADERS:", on("\n").join(with(requestSpec.getHeaders()).convert(headersToElliptics()))));
            add(new ImmutablePair<>("HEADERS:", on("\n").join(with(response.headers()).convert(headersToElliptics()))));
        }});

        String formParams = on("&").skipNulls().join(
                ifCond("POST".equals(requestSpec.getURI()))
                        .thenReturn(convertMapToString(requestSpec.getRequestParams(), "&"))
                        .elseReturn(null),
                ifCond("POST".equals(requestSpec.getURI()))
                        .thenReturn(convertMapToString(requestSpec.getFormParams(), "&"))
                        .elseReturn(null)
        );

        return on("").join(
                format("%s: ", requestSpec.getMethod()),
                requestSpec.getURI(),
                ifCond(isEmpty(formParams)).thenReturn("").elseReturn(format(" [ %s ]", formParams)),
                ifCond(isEmpty(href)).thenReturn("").elseReturn(format("%n[ REQ: %s ]", href)),
                format("%n[ RESP: <%s> %s ]%n", response.statusLine(), hrefResp)
        );
    }

    private String pathToFile(FilterableRequestSpecification requestSpec, List<Pair<String, String>> entries) {
        try {
            File lDir = new File("responses");
            lDir.mkdirs();

            String reqId = requestSpec.getHeaders().get("X-Request-Id").getValue();

            File file;

            do {
                String time = String.valueOf((System.currentTimeMillis())/1000);
                URI uri = URI.create(requestSpec.getURI());
                String name = reqId + "_" + uri.getHost() + "_" + uri.getPath().replace("/", "") + "_" + time;
                file = new File(lDir, name);
            } while (!file.createNewFile());

            FileUtils.writeStringToFile(file, on("\n").join(with(entries)), "UTF-8");
            return file.getAbsolutePath();
        } catch (IOException e) {
            throw new RuntimeException("", e);
        }
    }
}

package ru.yandex.mail.common.report;

import io.restassured.response.Response;
import io.restassured.specification.FilterableRequestSpecification;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.http.client.utils.URIBuilder;
import ru.yandex.autotests.plugins.testpers.html.common.Code;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

import static ch.lambdaj.collection.LambdaCollections.with;
import static com.google.common.base.Joiner.on;
import static com.google.common.collect.Lists.newArrayList;
import static java.lang.String.format;
import static org.apache.commons.lang3.StringUtils.isEmpty;
import static ru.yandex.autotests.innerpochta.IfThenElseHelper.ifCond;
import static ru.yandex.autotests.innerpochta.html.RAHeader.header;
import static ru.yandex.autotests.plugins.testpers.html.common.Code.codeBlock;
import static ru.yandex.autotests.plugins.testpers.html.common.HtmlUtils.bold;
import static ru.yandex.autotests.plugins.testpers.html.common.HtmlUtils.html;
import static ru.yandex.autotests.plugins.testpers.html.common.HtmlUtils.htmlNoEscape;
import static ru.yandex.autotests.plugins.testpers.html.common.HtmlUtils.link;
import static ru.yandex.autotests.plugins.testpers.html.common.HtmlUtils.modalLink;
import static ru.yandex.autotests.plugins.testpers.html.common.HtmlUtils.pre;
import static ru.yandex.autotests.plugins.testpers.html.common.HtmlUtils.tab;
import static ru.yandex.autotests.plugins.testpers.html.common.HtmlUtils.tabs;
import static ru.yandex.autotests.plugins.testpers.html.common.ResponseRender.blocks;
import static ru.yandex.qatools.elliptics.ElClient.elliptics;

class HtmlLogger extends RequestLogger {
    private String urlInElliptics(String comment, String resp, Code... blocks) {
        if (isEmpty(resp)) {
            return "";
        }
        List<Code> blocksOfCode = newArrayList(blocks);
        blocksOfCode.add(codeBlock(comment, resp).format(true));

        String html = htmlNoEscape(blocks(blocksOfCode));

        String url = elliptics().path(this.getClass()).randomize()
                .name(this.getClass().getSimpleName() + ".htm")
                .put(html).get().url();
        try {
            return new URIBuilder(URI.create(url)).setScheme("https").build().toString();
        } catch (URISyntaxException e) {
            return url;
        }
    }

    private String urlForRequest(FilterableRequestSpecification requestSpec) {
        return urlInElliptics("REQUEST:", requestSpec.getURI(),
                codeBlock("HEADERS:", on("\n").join(with(requestSpec.getHeaders()).convert(headersToElliptics())
                )));
    }


    private String urlForResponse(FilterableRequestSpecification requestSpec,
                                  Response response, String responseBody) {
        return urlInElliptics("RESPONSE:", responseBody,
                codeBlock("REQPATH:", format("%s %s", requestSpec.getMethod(), requestSpec.getURI())),
                codeBlock("STATUS:", response.statusLine()),
                codeBlock("REQ HEADERS:", on("\n").join(with(requestSpec.getHeaders()).convert(headersToElliptics()))),
                codeBlock("HEADERS:", on("\n").join(with(response.headers()).convert(headersToElliptics())))
        );
    }

    @Override
    public String log(FilterableRequestSpecification requestSpec, Response response, String body) {
        String href = urlForRequest(requestSpec);
        String hrefResp = urlForResponse(requestSpec, response, body);
        String id = RandomStringUtils.randomAlphanumeric(5);
        String idResp = RandomStringUtils.randomAlphanumeric(5);

        return tabs(
                tab(requestSpec.getMethod(), on("\n").join(
                        pre(link(requestSpec.getURI(),
                                bold(requestSpec.getMethod()))
                                + ": "
                                + requestSpec.getURI()),
                        on("").skipNulls().join(
                                ifCond("POST".equals(requestSpec.getMethod()))
                                        .thenReturn(html(header("REQUEST PARAMS:\n", requestSpec.getRequestParams())))
                                        .elseReturn(null),
                                ifCond("POST".equals(requestSpec.getMethod()))
                                        .thenReturn(html(header("FORM PARAMS:\n", requestSpec.getFormParams())))
                                        .elseReturn(null)
                        )
                )),
                tab("BODY", on("\n").join(
                        ifCond(isEmpty(href)).thenReturn("").elseReturn(html(header("BODY: ", modalLink(id, "REQUEST")))),
                        ifCond(isEmpty(hrefResp)).thenReturn("").elseReturn(html(header("BODY: ", modalLink(idResp, "RESPONSE"))))
                )),
                tab("REQ-INFO", on("\n").join(
                        html(header("HEADERS:\n", on("\n").join(with(requestSpec.getHeaders()).convert(headers())))),
                        html(header("COOKIES:\n", on("\n").join(requestSpec.getCookies())))
                ))
        );
    }
}

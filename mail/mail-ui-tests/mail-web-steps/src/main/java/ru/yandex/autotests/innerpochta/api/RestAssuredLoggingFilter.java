package ru.yandex.autotests.innerpochta.api;

import io.restassured.builder.ResponseBuilder;
import io.restassured.filter.FilterContext;
import io.restassured.filter.OrderedFilter;
import io.restassured.http.Cookie;
import io.restassured.http.Cookies;
import io.restassured.http.Method;
import io.restassured.internal.RestAssuredResponseImpl;
import io.restassured.response.Response;
import io.restassured.specification.FilterableRequestSpecification;
import io.restassured.specification.FilterableResponseSpecification;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.Validate;
import org.apache.http.client.utils.URIBuilder;
import org.apache.log4j.Level;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import ru.yandex.autotests.innerpochta.LoggerProperties;
import ru.yandex.autotests.innerpochta.filter.CurlFormatter;
import ru.yandex.autotests.innerpochta.filter.DefaultReqFormatter;
import ru.yandex.autotests.innerpochta.filter.DefaultRespFormatter;
import ru.yandex.autotests.plugins.testpers.html.common.Code;
import ru.yandex.qatools.allure.annotations.Step;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.lang.String.format;
import static java.lang.String.join;
import static java.lang.String.valueOf;
import static java.util.Arrays.asList;
import static java.util.stream.Collectors.joining;
import static org.apache.commons.lang3.StringUtils.isEmpty;
import static org.hamcrest.Matchers.instanceOf;
import static ru.yandex.autotests.innerpochta.IfThenElseHelper.ifCond;
import static ru.yandex.autotests.innerpochta.StreamHelper.streamOf;
import static ru.yandex.autotests.innerpochta.converters.Converters.headersConverter;
import static ru.yandex.autotests.innerpochta.converters.Converters.headersToEllipticsConverter;
import static ru.yandex.autotests.innerpochta.html.RAHeader.convertMapToString;
import static ru.yandex.autotests.innerpochta.html.RAHeader.header;
import static ru.yandex.autotests.plugins.testpers.html.common.Code.codeBlock;
import static ru.yandex.autotests.plugins.testpers.html.common.HtmlUtils.bold;
import static ru.yandex.autotests.plugins.testpers.html.common.HtmlUtils.html;
import static ru.yandex.autotests.plugins.testpers.html.common.HtmlUtils.htmlNoEscape;
import static ru.yandex.autotests.plugins.testpers.html.common.HtmlUtils.link;
import static ru.yandex.autotests.plugins.testpers.html.common.HtmlUtils.modalFrame;
import static ru.yandex.autotests.plugins.testpers.html.common.HtmlUtils.modalLink;
import static ru.yandex.autotests.plugins.testpers.html.common.HtmlUtils.pre;
import static ru.yandex.autotests.plugins.testpers.html.common.HtmlUtils.tab;
import static ru.yandex.autotests.plugins.testpers.html.common.HtmlUtils.tabs;
import static ru.yandex.autotests.plugins.testpers.html.common.ResponseRender.blocks;
import static ru.yandex.qatools.elliptics.ElClient.elliptics;

public class RestAssuredLoggingFilter implements OrderedFilter {

    private Logger logger = LogManager.getLogger(this.getClass());

    private Level priority;

    private String comment = "";

    private boolean prettify = true;

    private boolean common = false;

    private DefaultRespFormatter respFormatter = new DefaultRespFormatter();

    private DefaultReqFormatter reqFormatter = new DefaultReqFormatter();


    public static RestAssuredLoggingFilter log() {
        return new RestAssuredLoggingFilter(Level.INFO);
    }

    public static RestAssuredLoggingFilter log(Level priority) {
        return new RestAssuredLoggingFilter(priority);
    }

    public RestAssuredLoggingFilter(Level priority) {
        this.priority = priority;
    }

    public RestAssuredLoggingFilter pretty(boolean prettify) {
        this.prettify = prettify;
        return this;
    }

    public RestAssuredLoggingFilter logger(Logger logger) {
        this.logger = logger;
        return this;
    }

    @Step("{0}")
    public RestAssuredLoggingFilter comment(String comment) {
        this.comment = comment;
        return this;
    }

    /**
     * Общий ли фильтр - т.е. используется ли для логирования по-умолчанию
     *
     * @param common - true, если общий
     * @return this
     */
    public RestAssuredLoggingFilter common(boolean common) {
        this.common = common;
        return this;
    }


    public RestAssuredLoggingFilter respFormatter(DefaultRespFormatter formatter) {
        this.respFormatter = Validate.notNull(formatter, "Формирователь тела ответа не может быть NULL");
        return this;
    }


    public RestAssuredLoggingFilter reqFormatter(DefaultReqFormatter formatter) {
        this.reqFormatter = Validate.notNull(formatter, "Формирователь тела запроса не может быть NULL");
        return this;
    }


    @Override
    public Response filter(FilterableRequestSpecification requestSpec, FilterableResponseSpecification responseSpec,
                           FilterContext ctx) {

        // Если есть еще такие фильтры, общим не фильтруем
        // FIXME: если 2 общих, вообще никто не логирует
        if (common && requestSpec.getDefinedFilters().stream().filter(raFilter ->
                instanceOf(RestAssuredLoggingFilter.class).matches(raFilter)).count() > 1) {
            return ctx.next(requestSpec, responseSpec);
        }

        boolean logged = false;
        try {
            Response response = ctx.next(requestSpec, responseSpec);
            byte[] respNonPretty = response.asByteArray();

            String respPretty = null;
            try {
                respPretty = respFormatter.format(response, prettify, respNonPretty);
            } catch (IOException e) {
                logger.error("Can't format response! Skipping...", e);
            }

            if (new LoggerProperties().isLocalDebug()) {
                String log = localLogRequest(requestSpec, response, respPretty);
                logger.log(priority, log);
            } else {
                String log = logRequest(requestSpec, response, respPretty);
                logger.log(priority, log);
            }

            logged = true;
            return cloneResponseIfNeeded(response, respNonPretty);
        } finally {
            if (!logged) {
                logger.error(format("FAILED TO LOG: %s: %s",
                        requestSpec.getMethod(),
                        requestSpec.getURI()));
            }
        }
    }

    private String localLogRequest(FilterableRequestSpecification requestSpec, Response response, String responseBody) {
        String href = urlForRequest(requestSpec);
        String hrefResp = urlForResponse(requestSpec, response, responseBody);

        String formParams = Stream.of(
                ifCond(Method.POST.name().equals(requestSpec.getMethod()))
                        .thenReturn(convertMapToString(requestSpec.getRequestParams(), "&"))
                        .elseReturn(null),
                ifCond(Method.POST.name().equals(requestSpec.getMethod()))
                        .thenReturn(convertMapToString(requestSpec.getFormParams(), "&"))
                        .elseReturn(null)
        ).filter(Objects::nonNull).collect(joining("&"));

        return join("",
                ifCond(isEmpty(comment)).thenReturn("").elseReturn(format("[ %s ]: ", comment)),
                format("%s: ", requestSpec.getMethod()),
                requestSpec.getURI(),
                ifCond(isEmpty(formParams)).thenReturn("").elseReturn(format(" [ %s ]", formParams)),
                ifCond(isEmpty(href)).thenReturn("").elseReturn(format("%n[ REQ: %s ]", href)),
                format("%n[ RESP: <%s> %s ]%n", response.statusLine(), hrefResp)
        );
    }

    private String logRequest(FilterableRequestSpecification requestSpec, Response response, String responseBody) {
        String href = urlForRequest(requestSpec);
        String hrefResp = urlForResponse(requestSpec, response, responseBody);

        String id = RandomStringUtils.randomAlphanumeric(5);
        String idResp = RandomStringUtils.randomAlphanumeric(5);

        String log = tabs(
                tab("COMMENT", comment),
                tab(requestSpec.getMethod(), join("\n",
                        pre(link(requestSpec.getURI(),
                                bold(requestSpec.getMethod()))
                                + ": "
                                + requestSpec.getURI()),
                        Stream.of(
                                ifCond(Method.POST.name().equals(requestSpec.getMethod()))
                                        .thenReturn(html(header("REQUEST PARAMS:\n", requestSpec.getRequestParams())))
                                        .elseReturn(null),
                                ifCond(Method.POST.name().equals(requestSpec.getMethod()))
                                        .thenReturn(html(header("FORM PARAMS:\n", requestSpec.getFormParams())))
                                        .elseReturn(null)
                        ).filter(Objects::nonNull).collect(joining())
                )),
                tab("BODY", join("\n",
                        ifCond(isEmpty(href)).thenReturn("")
                                .elseReturn(html(header("BODY: ", modalLink(id, "REQUEST")))),
                        ifCond(isEmpty(hrefResp)).thenReturn("")
                                .elseReturn(html(header("BODY: ", modalLink(idResp, "RESPONSE"))))
                )),
                tab("REQ-INFO", join("\n",
                        html(header("HEADERS:\n", headersConverter(requestSpec.getHeaders(), "\n"))),
                        html(header("COOKIES:\n", streamOf(requestSpec.getCookies())
                                .map(Cookie::toString).collect(joining("\n"))))
                )),
                tab("RESP-INFO", join("\n",
                        html(header("STATUS: ", response.statusLine())),
                        html(header("HEADERS:\n", headersConverter(response.getHeaders(), "\n"))),
                        html(header("COOKIES:\n", response.cookies())))
                ),
                tab("cURL", pre(new CurlFormatter(requestSpec).asCurlString()))
        );

        return Stream.of(
                log,
                ifCond(isEmpty(href)).thenReturn(null).elseReturn(modalFrame(href, id)),
                ifCond(isEmpty(hrefResp)).thenReturn(null).elseReturn(modalFrame(hrefResp, idResp))
        ).filter(Objects::nonNull).collect(joining());
    }

    private String prettyReq(FilterableRequestSpecification requestSpec) {
        return reqFormatter.format(requestSpec);
    }


    public String urlForRequest(FilterableRequestSpecification requestSpec) {
        return urlInElliptics("REQUEST:", prettyReq(requestSpec),
                codeBlock("HEADERS:", headersToEllipticsConverter(requestSpec.getHeaders(), "\n")));
    }


    public String urlForResponse(FilterableRequestSpecification requestSpec, Response response, String responseBody) {
        return urlInElliptics("RESPONSE:", responseBody,
                codeBlock("REQPATH:", format("%s %s", requestSpec.getMethod(), requestSpec.getURI())),
                codeBlock("STATUS:", response.statusLine()),
                codeBlock("REQ HEADERS:", headersToEllipticsConverter(requestSpec.getHeaders(), "\n")),
                codeBlock("REQ FORM:",
                        Stream.concat(
                                requestSpec.getFormParams().entrySet().stream(),
                                requestSpec.getRequestParams().entrySet().stream()
                        ).map(param -> format("%s=%s", param.getKey(), valueOf(param.getValue())))
                                .collect(joining("\n"))
                ),
                codeBlock("HEADERS:", headersToEllipticsConverter(response.getHeaders(), "\n")),
                codeBlock("COOKIES:", cookiesToEllipticsConverter(requestSpec.getCookies(), "\n"))
        );
    }


    public String urlInElliptics(String comment, String resp, Code... blocks) {
        if (isEmpty(resp)) {
            return "";
        }
        List<Code> blocksOfCode = new ArrayList<>(asList(blocks));
        blocksOfCode.add(codeBlock(comment, resp).format(prettify));

        String html = htmlNoEscape(blocks(blocksOfCode));

        if (new LoggerProperties().isLocalDebug()) {
            try {
                File lDir = new File("target/site/testpers-api-report-plugin/logs/resp");
                lDir.mkdirs();
                File fileWithDebugInfo = File.createTempFile(RandomStringUtils.randomAlphanumeric(10), ".htm", lDir);
                FileUtils.writeStringToFile(fileWithDebugInfo, html, "UTF-8");
                return fileWithDebugInfo.getAbsolutePath();
            } catch (IOException e) {
                throw new RuntimeException("", e);
            }
        }

        //TODO отключение подсветки синтаксиса
        String url = elliptics().path(this.getClass()).randomize()
                .name(this.getClass().getSimpleName() + ".htm")
                .put(html).get().url();
        try {
            return new URIBuilder(URI.create(url)).setScheme("https").build().toString();
        } catch (URISyntaxException e) {
            return url;
        }
    }

    private Response cloneResponseIfNeeded(Response response, byte[] responseBytes) {
        if (responseBytes != null && response instanceof RestAssuredResponseImpl
                && !((RestAssuredResponseImpl) response).getHasExpectations()) {
            final Response build = new ResponseBuilder().clone(response).setBody(responseBytes).build();
            ((RestAssuredResponseImpl) build).setHasExpectations(true);
            return build;
        }
        return response;
    }

    private String cookiesToEllipticsConverter(Cookies cookies, String separator) {
        return streamOf(cookies)
            .map(
                cookie -> format("%s=%s", cookie.getName(), cookie.getValue().replaceAll("\"", "{q}"))
            )
            .collect(Collectors.joining(separator));
    }

    @Override
    public int getOrder() {
        return LOWEST_PRECEDENCE;
    }
}


package ru.yandex.autotests.testpers.mail.mon.misc;

import com.google.common.base.Charsets;
import com.jayway.restassured.RestAssured;
import com.jayway.restassured.filter.Filter;
import com.jayway.restassured.filter.FilterContext;
import com.jayway.restassured.http.ContentType;
import com.jayway.restassured.internal.http.URIBuilder;
import com.jayway.restassured.response.Response;
import com.jayway.restassured.specification.FilterableRequestSpecification;
import com.jayway.restassured.specification.FilterableResponseSpecification;
import org.apache.commons.lang3.Validate;
import org.junit.rules.ExternalResource;
import ru.yandex.autotests.lib.junit.rules.login.Credentials;
import ru.yandex.autotests.lib.junit.rules.passport.PassportRequest;
import ru.yandex.autotests.lib.junit.rules.passport.PassportRule;
import ru.yandex.autotests.lib.junit.rules.passport.RequestExecutor;

import javax.ws.rs.core.UriBuilder;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.HashMap;

import static com.jayway.restassured.RestAssured.given;
import static com.jayway.restassured.config.RestAssuredConfig.config;
import static org.apache.commons.lang3.Validate.notNull;

/**
 * User: lanwen
 * Date: 04.02.15
 * Time: 13:22
 */
public class RestAssuredAuthRule extends ExternalResource implements Filter {

    private Credentials acc;
    private Response resp;

    private RestAssuredAuthRule() {
    }

    public static RestAssuredAuthRule auth() {
        return new RestAssuredAuthRule();
    }

    @Override
    protected void before() throws Throwable {
        RestAssured.config = config().encoderConfig(config().getEncoderConfig().defaultContentCharset(Charsets.UTF_8));
        login();
    }

    public RestAssuredAuthRule login() {
        PassportRule passport = new PassportRule(new RequestExecutor() {
            @Override
            public void request(URL url) {
                try {
                    resp = given()
                            .contentType(ContentType.URLENC)
                            .content(url.getQuery())
                            .post(new URIBuilder(URI.create(url.toString()), false, 
                                    RestAssured.config.getEncoderConfig()).setQuery(new HashMap<>()).toURI());
                } catch (URISyntaxException e) {
                    throw new RuntimeException("Bad URI in login req", e);
                }
            }

            @Override
            public void request(PassportRequest passportRequest) {
                request(passportRequest.useAuthService(false).rawUrl());
            }

            @Override
            public String getCookie(String cookieName) {
                return resp.getCookie(cookieName);
            }
        });

        if (acc != null) {
            passport.withCredentials(acc);
        }

        passport.login();
        return this;           
    }

    public RestAssuredAuthRule withAcc(Account acc) {
        this.acc = acc;
        return this;
    }

    @Override
    public Response filter(FilterableRequestSpecification reqSpec, FilterableResponseSpecification respSpec, FilterContext ctx) {
        notNull(resp, "Не было попытки авторизации для вызова фильтра");
        reqSpec.cookies(resp.cookies());
        return ctx.next(reqSpec, respSpec);
    }
}

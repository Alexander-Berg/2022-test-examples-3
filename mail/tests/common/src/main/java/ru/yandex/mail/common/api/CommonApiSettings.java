package ru.yandex.mail.common.api;

import java.util.function.Function;
import java.util.function.Supplier;

import io.restassured.builder.RequestSpecBuilder;
import io.restassured.config.DecoderConfig;
import io.restassured.config.EncoderConfig;
import io.restassured.config.ObjectMapperConfig;
import io.restassured.config.RestAssuredConfig;
import io.restassured.http.Cookies;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import io.restassured.specification.ResponseSpecification;
import ru.yandex.mail.common.report.RestAssuredLoggingFilter;

import static io.restassured.RestAssured.given;
import static io.restassured.mapper.ObjectMapperType.GSON;
import static org.apache.log4j.Logger.getLogger;
import static ru.yandex.mail.common.properties.CoreProperties.props;


public class CommonApiSettings {
    private static final String clientType = "type";
    private static final String clientVersion = "version";

    public static RequestSpecification baseWmiSpec(String uri, String xRequestId) {

        RequestSpecBuilder spec = new RequestSpecBuilder()
                .setConfig(RestAssuredConfig.config()
                        .objectMapperConfig(ObjectMapperConfig.objectMapperConfig().defaultObjectMapperType(GSON))
                        .encoderConfig(EncoderConfig.encoderConfig().defaultContentCharset("UTF-8"))
                        .decoderConfig(DecoderConfig.decoderConfig().defaultContentCharset("UTF-8")))
                .setBaseUri(uri)
                .addHeader(Headers.REQUEST_ID, xRequestId);

        return given()
                .filter(new RestAssuredLoggingFilter(getLogger(CommonApiSettings.class), props().isLocalDebug()))
                .spec(spec.build());
    }

    public static Function<Response, Response> shouldBe(ResponseSpecification respSpec){
        return response -> response.then().spec(respSpec).extract().response();
    }

    public static Supplier<RequestSpecBuilder> requestSpecBuilder(RequestTraits traits) {
        return () -> new RequestSpecBuilder().addRequestSpecification(baseWmiSpec(traits.getUrl(), traits.getXRequestId()))
                .setBaseUri(traits.getUrl())
                .addHeader(Headers.CLIENT_TYPE, clientType)
                .addHeader(Headers.CLIENT_VERSION, clientVersion)
                .addHeader(Headers.SERVICE_TICKET, traits.getServiceTicket());
    }

    public static Supplier<RequestSpecBuilder> requestSpecBuilder(RequestTraits traits, Cookies cookies) {
        return () -> new RequestSpecBuilder().addRequestSpecification(baseWmiSpec(traits.getUrl(), traits.getXRequestId()))
                .setBaseUri(traits.getUrl())
                .addHeader(Headers.CLIENT_TYPE, clientType)
                .addHeader(Headers.CLIENT_VERSION, clientVersion)
                .addHeader(Headers.SERVICE_TICKET, traits.getServiceTicket())
                .addCookies(cookies);
    }
}

package ru.yandex.market.logistics.logistics4shops.utils;

import javax.annotation.ParametersAreNonnullByDefault;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import lombok.experimental.UtilityClass;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.util.MultiValueMap;

import ru.yandex.market.logistics.test.integration.matchers.JsonMatcher;
import ru.yandex.market.logistics.test.integration.matchers.XmlMatcher;

import static org.hamcrest.Matchers.equalTo;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.extractFileContent;

@UtilityClass
@ParametersAreNonnullByDefault
@SuppressWarnings("HideUtilityClassConstructor")
public class RestAssuredFactory {

    public void assertGetSuccess(String url, String responsePath) {
        RestAssured
            .get(url)
            .then()
            .statusCode(HttpStatus.OK.value())
            .contentType(ContentType.JSON)
            .body(new JsonMatcher(extractFileContent(responsePath)));
    }

    public void assertGetSuccess(String url, String responsePath, MultiValueMap<String, String> params) {
        RestAssured
            .given()
            .params(params)
            .get(url)
            .then()
            .statusCode(HttpStatus.OK.value())
            .contentType(ContentType.JSON)
            .body(new JsonMatcher(extractFileContent(responsePath)));
    }

    public void assertGetSuccess(String url, String responsePath, String parameterName, Object... parameterValues) {
        RestAssured
            .given()
            .param(parameterName, parameterValues)
            .get(url)
            .then()
            .statusCode(HttpStatus.OK.value())
            .contentType(ContentType.JSON)
            .body(new JsonMatcher(extractFileContent(responsePath)));
    }

    public void assertGetNotFound(String url, String errorMessage) {
        RestAssured
            .get(url)
            .then()
            .statusCode(HttpStatus.NOT_FOUND.value())
            .body("message", equalTo(errorMessage));
    }

    public void assertPostXml(String url, String bodyFilePath, String responsePath) {
        RestAssured.given()
            .contentType(MediaType.TEXT_XML_VALUE)
            .body(extractFileContent(bodyFilePath))
            .post(url)
            .then()
            .statusCode(HttpStatus.OK.value())
            .contentType(MediaType.TEXT_XML_VALUE)
            .body(new XmlMatcher(extractFileContent(responsePath)));
    }

    public void assertGetXml(String url, String responsePath) {
        RestAssured
            .get(url)
            .then()
            .statusCode(HttpStatus.OK.value())
            .contentType(MediaType.TEXT_XML_VALUE)
            .body(new XmlMatcher(extractFileContent(responsePath)));
    }
}

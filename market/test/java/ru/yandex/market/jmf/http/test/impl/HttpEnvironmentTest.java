package ru.yandex.market.jmf.http.test.impl;

import java.util.function.Function;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import ru.yandex.market.jmf.http.Http;
import ru.yandex.market.jmf.http.HttpResponse;
import ru.yandex.market.jmf.http.test.ResponseBuilder;

public class HttpEnvironmentTest {

    private final HttpEnvironment environment = new HttpEnvironment();

    @Test
    public void raiseExceptionOnRequestWhenNoExpectationIsSet() {
        var runtimeException = Assertions.assertThrows(RuntimeException.class,
                () -> environment.execute(Http.get(), "http://example.com"));
        Assertions.assertEquals("Unknown host", runtimeException.getMessage());
    }

    @Test
    public void successfulGetRequest() {
        environment.when(HttpRequest.get("https://yandex.ru")).then(hasResponse(b -> b.body("good")));
        assertOkWithBody("good",
                environment.execute(Http.get(), "https://yandex.ru"));
    }

    @Test
    public void schemaMismatch() {
        environment.when(HttpRequest.get("https://yandex.ru")).then(hasResponse());
        var runtimeException = Assertions.assertThrows(RuntimeException.class,
                () -> environment.execute(Http.get(), "http://yandex.ru"));
        Assertions.assertEquals("Unknown host", runtimeException.getMessage());
    }

    @Test
    public void notFoundOnMethodMismatch() {
        environment.when(HttpRequest.get("https://yandex.ru")).then(hasResponse());
        assertNotFound(environment.execute(Http.post(), "https://yandex.ru"));
    }

    @Test
    public void notFoundWhenPathDiffers() {
        environment.when(HttpRequest.get("https://yandex.ru/alisa")).then(hasResponse());
        assertNotFound(environment.execute(Http.get(), "https://yandex.ru"));
    }

    @Test
    public void notFoundWhenParamDiffers() {
        environment.when(HttpRequest.get("https://yandex.ru/alisa")).then(hasResponse());
        assertNotFound(environment.execute(Http.get(), "https://yandex.ru/alisa?lang=fn"));
    }

    @Test
    public void matchingWithParamsIsNotSupported() {
        // Кажется, это баг!
        environment.when(HttpRequest.get("https://translate.yandex.ru/?from=ru")).then(hasResponse(b -> b.body("ok")));
        assertNotFound(environment.execute(Http.get(), "https://translate.yandex.ru/?from=ru"));
    }

    @Test
    public void sameHostAndDifferentPaths() {
        environment.when(HttpRequest.get("https://yandex.ru/alisa/ru")).then(hasResponse(b -> b.body("здрасьте")));
        environment.when(HttpRequest.get("https://yandex.ru/alisa/fn")).then(hasResponse(b -> b.body("päivää")));

        assertOkWithBody("päivää",
                environment.execute(Http.get(), "https://yandex.ru/alisa/fn"));
        assertOkWithBody("здрасьте",
                environment.execute(Http.get(), "https://yandex.ru/alisa/ru"));
    }

    /**
     * Если указать два разных ожидания на один и тот же путь, будет использоваться только последний из них
     */
    @Test
    public void lastExpectationIsAlwaysUsed() {
        environment.when(HttpRequest.get("http://my.com")).then(hasResponse(b -> b.body("first")));
        environment.when(HttpRequest.get("http://my.com")).then(hasResponse(b -> b.body("second")));
        assertOkWithBody("second", environment.execute(Http.get(), "http://my.com"));

        environment.when(HttpRequest.get("http://my.com")).then(hasResponse(b -> b.body("third")));
        assertOkWithBody("third", environment.execute(Http.get(), "http://my.com"));
    }

    private HttpResponse hasResponse() {
        return hasResponse(Function.identity());
    }

    private HttpResponse hasResponse(Function<ResponseBuilder, ResponseBuilder> builderSpec) {
        return builderSpec.apply(ResponseBuilder.newBuilder()).build();
    }

    private void assertOkWithBody(String body, HttpResponse httpResponse) {
        Assertions.assertTrue(httpResponse.getHttpStatus().is2xxSuccessful());
        Assertions.assertEquals(body, httpResponse.getBodyAsString());
    }

    private void assertNotFound(HttpResponse httpResponse) {
        Assertions.assertTrue(httpResponse.getHttpStatus().is4xxClientError());
        Assertions.assertEquals("", httpResponse.getBodyAsString());
    }
}

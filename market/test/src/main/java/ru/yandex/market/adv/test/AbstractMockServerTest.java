package ru.yandex.market.adv.test;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import javax.annotation.Nonnull;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.mockserver.client.MockServerClient;
import org.mockserver.matchers.MatchType;
import org.mockserver.model.Body;
import org.mockserver.model.MediaType;
import org.mockserver.verify.VerificationTimes;

import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;
import static org.mockserver.model.JsonBody.json;

/**
 * Общий наследник для всех тестовых классов, в которых требуется поднятие {@link MockServerClient}.
 * Date: 14.10.2021
 * Project: adv-content-manager
 *
 * @author alexminakov
 */
@RequiredArgsConstructor
public abstract class AbstractMockServerTest extends AbstractTest {

    protected final MockServerClient server;

    @BeforeEach
    public void init() {
        server.reset();
    }

    /**
     * Добавляем mock на конкретный путь запроса до mock-server.
     *
     * @param method          http метод
     * @param path            путь запроса
     * @param requestFile     файл с json запросом
     * @param queryParameters параметры запроса
     * @param responseCode    код ответа
     * @param responseFile    файл с json ответом
     */
    @SuppressWarnings("unused")
    protected void mockServerPath(
            @Nonnull String method,
            @Nonnull String path,
            String requestFile,
            @Nonnull Map<String, List<String>> queryParameters,
            int responseCode,
            String responseFile
    ) {
        mockServerPath(
                method,
                path,
                () -> requestFile == null
                        ? null
                        : json(loadFile(requestFile), StandardCharsets.UTF_8, MatchType.STRICT),
                queryParameters,
                responseCode,
                responseFile,
                MediaType.APPLICATION_JSON_UTF_8
        );
    }

    /**
     * Добавляем mock на конкретный путь запроса до mock-server.
     *
     * @param method              http метод
     * @param path                путь запроса
     * @param requestBody         тело запроса
     * @param queryParameters     параметры запроса
     * @param responseCode        код ответа
     * @param responseFile        файл с json ответом
     * @param responseContentType тип контента ответа
     */
    @SuppressWarnings("SameParameterValue")
    protected void mockServerPath(
            @Nonnull String method,
            @Nonnull String path,
            @Nonnull Supplier<Body<?>> requestBody,
            @Nonnull Map<String, List<String>> queryParameters,
            int responseCode,
            String responseFile,
            MediaType responseContentType
    ) {
        server
                .when(request()
                        .withMethod(method)
                        .withPath(path)
                        .withQueryStringParameters(queryParameters)
                        .withBody(requestBody.get())
                )
                .respond(response()
                        .withStatusCode(responseCode)
                        .withContentType(responseContentType)
                        .withBody(responseFile == null ? null : loadFile(responseFile), StandardCharsets.UTF_8)
                );
    }

    /**
     * Проверяет, что запрос был отправлен
     * @param method      http метод
     * @param path        путь запроса
     * @param requestFile тело запроса
     */
    protected void mockServerVerify(@Nonnull String method, @Nonnull String path, String requestFile) {
        server.verify(
                request()
                        .withMethod(method)
                        .withPath(path)
                        .withBody(
                                requestFile == null
                                        ? null
                                        : json(loadFile(requestFile), StandardCharsets.UTF_8, MatchType.STRICT)
                        ),
                VerificationTimes.once()
        );
    }
}

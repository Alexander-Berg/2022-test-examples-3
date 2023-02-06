package ru.yandex.direct.test.utils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.function.Function;
import java.util.function.Predicate;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import okhttp3.mockwebserver.MockWebServer;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.entity.ContentType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ru.yandex.direct.utils.JsonUtils;

/**
 * Тестовый веб сервер.
 * Пример использования:
 * <p>
 * <pre>{@code
 *     @Rule
 *     public MockedHttpWebServer server = new MockedHttpWebServer("http://ya.ru/test.cgi?table_name=SSPInfo",
 *              "[]", ContentType.APPLICATION_JSON);
 *
 *     private String serverUrl = server.getServerURL();
 * }</pre>
 */
public abstract class MockedHttpWebServerBase {
    protected static final Logger logger = LoggerFactory.getLogger(MockedHttpWebServerBase.class);

    protected final Map<String, List<PredicateResponsePair>> pathToPredicateAndResponse = new HashMap<>();
    protected final Map<String, List<String>> pathToRequestBody = new HashMap<>();
    protected final ContentType contentType;
    protected MockWebServer server;

    /**
     * @param requestPath  путь HTTP-запроса вместе с query-частью (например /test.cgi?table_name=SomeTable)
     * @param responseBody тело ответа
     */
    public MockedHttpWebServerBase(String requestPath, String responseBody) {
        this(requestPath, responseBody, ContentType.TEXT_HTML);
    }

    /**
     * @param requestPath  путь HTTP-запроса вместе с query-частью (например /test.cgi?table_name=SomeTable)
     * @param responseBody тело ответа
     * @param contentType  Значение заговолка Content-Type ответа сервера
     */
    public MockedHttpWebServerBase(String requestPath, String responseBody, ContentType contentType) {
        this(Collections.singletonMap(requestPath, responseBody), contentType);
    }

    /**
     * @param contentType Значение заговолка Content-Type ответа сервера
     */
    public MockedHttpWebServerBase(ContentType contentType) {
        this(Collections.emptyMap(), contentType);
    }

    /**
     * @param requestToResponse путь HTTP-запроса вместе с query-частью (например /test.cgi?table_name=SomeTable) в
     *                          соответствии с телом ответа
     * @param contentType       Значение заговолка Content-Type ответа сервера
     */
    public MockedHttpWebServerBase(Map<String, String> requestToResponse, ContentType contentType) {
        for (Map.Entry<String, String> entry : requestToResponse.entrySet()) {
            addResponse(entry.getKey(), entry.getValue());
        }
        this.contentType = contentType;
    }

    /**
     * Добавить тестовому серверу ответ по пути [serverUrl] + [requestPath].
     * Ответ отдается при любом запросе: GET, POST с любым телом и пр.
     *
     * @param requestPath  путь до документа на сервере (вместе с query-частью)
     * @param responseBody ответ сервера
     */
    public void addResponse(String requestPath, String responseBody) {
        pathToPredicateAndResponse.put(
                requestPath,
                Collections.singletonList(new PredicateResponsePair(s -> true, (request) -> responseBody)));
    }

    /**
     * Добавить тестовому серверу ответ по пути [serverUrl] + [requestPath].
     * Ответ отдается при любом запросе: GET, POST с любым телом и пр.
     *
     * @param requestPath  путь до документа на сервере (вместе с query-частью)
     * @param responseBody ответ сервера
     */
    public void addResponse(String requestPath, Function<String, String> responseBody) {
        pathToPredicateAndResponse.put(
                requestPath,
                Collections.singletonList(new PredicateResponsePair(s -> true, responseBody)));
    }

    /**
     * Добавить тестовому серверу ответ по пути [serverUrl] + [requestPath]. Ответ будет отдан, только если метод
     * Predicate.test() вернет true. Один requestPath может отдавать более одного тела ответа, если для него задано
     * несколько условий
     *
     * @param requestPath  путь до документа на сервере (вместе с query-частью)
     * @param predicate    условие выдачи конкретного ответа, на вход предикату подается тело запроса
     * @param responseBody ответ сервера
     */
    public void addResponse(String requestPath, Predicate<String> predicate, String responseBody) {
        List<PredicateResponsePair> pairs;
        if (pathToPredicateAndResponse.containsKey(requestPath)) {
            pairs = pathToPredicateAndResponse.get(requestPath);
        } else {
            pairs = new ArrayList<>();
            pathToPredicateAndResponse.put(requestPath, pairs);
        }
        pairs.add(new PredicateResponsePair(predicate, (request) -> responseBody));
    }

    /**
     * Удалить все варианты ответа сервера и все сохраненные запросы
     */
    public void clear() {
        pathToPredicateAndResponse.clear();
        pathToRequestBody.clear();
    }


    /**
     * Считать из конфига ответы тестовому серверу для POST-запросов
     * Конфиг в формате JSON
     * {requests: [ {url: , body: , response: }, ...]}
     *
     * @param config путь до конфига
     */
    public void addPostResponsesFromConfig(String config) {
        ObjectMapper mapper = JsonUtils.getObjectMapper();
        Config httpCfg = ConfigFactory.load(config);
        for (Config req : httpCfg.getConfigList("requests")) {
            addResponse(req.getString("url"),
                    body -> {
                        try {
                            return mapper.readTree(req.getString("body"))
                                    .equals(mapper.readTree(body));
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    },
                    req.getString("response"));
        }
    }

    public String getServerURL() {
        return StringUtils.stripEnd(server.url("").toString(), "/"); //  Всегда возвращает путь без последнего слеша
    }

    public String getHostName() {
        return server.getHostName();
    }

    public int getPort() {
        return server.getPort();
    }

    protected synchronized void addRequestBody(String requestPath, String requestBody) {
        List<String> bodies;
        if (pathToRequestBody.containsKey(requestPath)) {
            bodies = pathToRequestBody.get(requestPath);
        } else {
            bodies = new ArrayList<>();
            pathToRequestBody.put(requestPath, bodies);
        }
        bodies.add(requestBody);
    }

    /**
     * Получить список тел запроса, сделанных по заданному пути. Если запросов не было возбуждается исключение
     */
    public List<String> getRequests(String requestPath) {
        List<String> bodies = pathToRequestBody.get(requestPath);
        if (bodies == null) {
            throw new NoSuchElementException("No requests for path " + requestPath + " found");
        }
        return bodies;
    }

    /**
     * Получить тело запроса, сделанного по заданному пути. Тело отдается только если запрос по этому пути был только
     * один, в остальных случаях возбуждается исключение
     */
    public String getRequest(String requestPath) {
        List<String> bodies = getRequests(requestPath);
        if (bodies.size() != 1) {
            throw new IllegalStateException("There are more than one request for path " + requestPath);
        }
        return bodies.get(0);
    }

    /**
     * Получить количество запросов, сделанных по заданному пути.
     */
    public int getRequestNum(String requestPath) {
        List<String> bodies = pathToRequestBody.get(requestPath);
        return bodies != null ? bodies.size() : 0;
    }

    protected class PredicateResponsePair {
        protected final Predicate<String> predicate;
        protected final Function<String, String> responseBody;

        private PredicateResponsePair(Predicate<String> predicate, Function<String, String> responseBody) {
            this.predicate = predicate;
            this.responseBody = responseBody;
        }
    }
}
